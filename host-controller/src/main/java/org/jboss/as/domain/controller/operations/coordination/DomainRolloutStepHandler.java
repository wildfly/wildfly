/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2011, Red Hat, Inc., and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */

package org.jboss.as.domain.controller.operations.coordination;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.CANCELLED;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.CONCURRENT_GROUPS;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.DOMAIN_FAILURE_DESCRIPTION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.FAILED;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.FAILURE_DESCRIPTION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.HOST;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.HOST_FAILURE_DESCRIPTIONS;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.IN_SERIES;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.MAX_FAILED_SERVERS;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.MAX_FAILURE_PERCENTAGE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OUTCOME;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.RESULT;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ROLLBACK_ACROSS_GROUPS;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ROLLING_TO_SERVERS;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SERVERS;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SERVER_GROUP;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SERVER_OPERATIONS;
import static org.jboss.as.domain.controller.DomainControllerLogger.HOST_CONTROLLER_LOGGER;
import static org.jboss.as.domain.controller.DomainControllerMessages.MESSAGES;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.OperationStepHandler;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.ProxyController;
import org.jboss.as.domain.controller.ServerIdentity;
import org.jboss.as.domain.controller.plan.NewRolloutPlanController;
import org.jboss.as.domain.controller.plan.NewServerOperationExecutor;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.Property;

/**
 * Formulates a rollout plan, invokes the proxies to execute it on the servers.
 *
 * @author Brian Stansberry (c) 2011 Red Hat Inc.
 */
public class DomainRolloutStepHandler implements OperationStepHandler {

    private final DomainOperationContext domainOperationContext;
    private final Map<String, ProxyController> hostProxies;
    private final Map<String, ProxyController> serverProxies;
    private final ExecutorService executorService;
    private final ModelNode providedRolloutPlan;
    private final boolean trace = HOST_CONTROLLER_LOGGER.isTraceEnabled();

    public DomainRolloutStepHandler(final Map<String, ProxyController> hostProxies,
                                    final Map<String, ProxyController> serverProxies,
                                    final DomainOperationContext domainOperationContext,
                                    final ModelNode rolloutPlan,
                                    final ExecutorService executorService) {
        this.hostProxies = hostProxies;
        this.serverProxies = serverProxies;
        this.domainOperationContext = domainOperationContext;
        this.providedRolloutPlan = rolloutPlan;
        this.executorService = executorService;
    }

    @Override
    public void execute(final OperationContext context, final ModelNode operation) throws OperationFailedException {

        if (context.hasFailureDescription()) {
            // abort
            context.setRollbackOnly();
            context.completeStep();
            return;
        }

        // Confirm no host failures
        boolean pushToServers = !domainOperationContext.hasHostLevelFailures();
        if (pushToServers) {
            ModelNode ourResult = domainOperationContext.getCoordinatorResult();
            if (ourResult.has(FAILURE_DESCRIPTION)) {
                if (trace) {
                    HOST_CONTROLLER_LOGGER.tracef("coordinator failed: %s", ourResult);
                }
                pushToServers = false;
                domainOperationContext.setCompleteRollback(true);
            } else {
                if (trace) {
                    HOST_CONTROLLER_LOGGER.tracef("coordinator succeeded: %s", ourResult);
                }
                for (ModelNode hostResult : domainOperationContext.getHostControllerResults().values()) {
                    if (hostResult.has(FAILURE_DESCRIPTION)) {
                        if (trace) {
                            HOST_CONTROLLER_LOGGER.tracef("host failed: %s", hostResult);
                        }
                        pushToServers = false;
                        domainOperationContext.setCompleteRollback(true);
                        break;
                    }
                }
            }
        }

        if (pushToServers) {
            // We no longer roll back by default
            domainOperationContext.setCompleteRollback(false);

            final Map<ServerIdentity, ProxyTask> tasks = new HashMap<ServerIdentity, ProxyTask>();
            final Map<ServerIdentity, Future<ModelNode>> futures = new HashMap<ServerIdentity, Future<ModelNode>>();
            try {
                pushToServers(context, tasks, futures);
                context.completeStep();
            } finally {

                // Inform the remote hosts whether to commit or roll back their updates
                // Do them all before reading results so the commits/rollbacks can be executed in parallel
                boolean completeRollback = domainOperationContext.isCompleteRollback();
                for (Map.Entry<ServerIdentity, ProxyTask> entry : tasks.entrySet()) {
                    boolean rollback = completeRollback || domainOperationContext.isServerGroupRollback(entry.getKey().getServerGroupName());
                    entry.getValue().finalizeTransaction(!rollback);
                }
                // Now read the final values. This ensures the operations are committed on the remote servers
                // before we expose the servers to further requests
                boolean interrupted = false;
                try {
                    for (Map.Entry<ServerIdentity, Future<ModelNode>> entry : futures.entrySet()) {
                        Future<ModelNode> future = entry.getValue();
                        try {
                            ModelNode finalResult = future.isCancelled() ? getCancelledResult() : future.get();
                            domainOperationContext.addServerResult(entry.getKey(), finalResult);
                        } catch (InterruptedException e) {
                            interrupted = true;
                            HOST_CONTROLLER_LOGGER.interruptedAwaitingFinalResponse(entry.getKey().getServerName(), entry.getKey().getHostName());

                        } catch (ExecutionException e) {
                            HOST_CONTROLLER_LOGGER.caughtExceptionAwaitingFinalResponse(e.getCause(), entry.getKey().getServerName(), entry.getKey().getHostName());
                        }
                    }
                } finally {
                    if (interrupted) {
                        Thread.currentThread().interrupt();
                    }
                }
            }
        } else {
            // There were failures on hosts, so gather them up and report them
            reportHostFailures(context, operation);
            context.completeStep();
        }
    }

    private ModelNode getCancelledResult() {
        ModelNode cancelled = new ModelNode();
        cancelled.get(OUTCOME).set(CANCELLED);
        return cancelled;
    }

    private void pushToServers(final OperationContext context, final Map<ServerIdentity, ProxyTask> tasks,
                               final Map<ServerIdentity, Future<ModelNode>> futures) throws OperationFailedException {


        final String localHostName = domainOperationContext.getLocalHostInfo().getLocalHostName();
        Map<String, ModelNode> hostResults = new HashMap<String, ModelNode>(domainOperationContext.getHostControllerResults());
        if (domainOperationContext.getCoordinatorResult().isDefined()) {
            hostResults.put(localHostName, domainOperationContext.getCoordinatorResult());
        }
        Map<String, Map<ServerIdentity, ModelNode>> opsByGroup = getOpsByGroup(hostResults);
        if (opsByGroup.size() > 0) {

            final ModelNode rolloutPlan = getRolloutPlan(this.providedRolloutPlan, opsByGroup);

            if (trace) {
                HOST_CONTROLLER_LOGGER.tracef("Rollout plan is %s", rolloutPlan);
            }
            final NewServerOperationExecutor operationExecutor = new NewServerOperationExecutor() {
                @Override
                public ModelNode executeServerOperation(ServerIdentity server, ModelNode operation) {
                    ProxyController proxy = hostProxies.get(server.getHostName());
                    if (proxy == null) {
                        if (localHostName.equals(server.getHostName())) {
                            // Use our server proxies
                            proxy = serverProxies.get(server.getServerName());
                            if (proxy == null) {
                                if (trace) {
                                    HOST_CONTROLLER_LOGGER.tracef("No proxy for %s", server);
                                }
                                return null;
                            }
                        }
                    }

                    // TODO this seems a bit convoluted. It's already an executor service thread calling this method
                    // But now we use another thread to actually make the invocation, so the first can read
                    // the result and decide how it fits in the overall rollout plan
                    ProxyTask task = new ProxyTask(server.getHostName(), operation, context, proxy);
                    tasks.put(server, task);

                    boolean interrupted = false;
                    Future<ModelNode> future = null;
                    ModelNode result = null;
                    try {
                        future = executorService.submit(task);
                        result = task.getUncommittedResult();
                        futures.put(server, future);
                    } catch (Exception e) {
                        result = new ModelNode();
                        result.get(OUTCOME).set(FAILED);
                        if (e instanceof InterruptedException) {
                            result.get(FAILURE_DESCRIPTION).set(MESSAGES.interruptedAwaitingResultFromServer(server));
                            interrupted = true;
                        } else {
                            result.get(FAILURE_DESCRIPTION).set(MESSAGES.exceptionAwaitingResultFromServer(server, e.getMessage()));
                        }
                        task.cancel();
                        future.cancel(true);
                    } finally {
                        if (interrupted) {
                            Thread.currentThread().interrupt();
                        }
                    }
                    return result;
                }
            };

            NewRolloutPlanController rolloutPlanController = new NewRolloutPlanController(opsByGroup, rolloutPlan, domainOperationContext, operationExecutor, executorService);
            NewRolloutPlanController.Result planResult = rolloutPlanController.execute();
            if (trace) {
                HOST_CONTROLLER_LOGGER.tracef("Rollout plan result is %s", planResult);
            }
            if (planResult == NewRolloutPlanController.Result.FAILED) {
                domainOperationContext.setCompleteRollback(true);
                // AS7-801 -- we need to record a failure description here so the local host change gets aborted
                // Waiting to do it in the DomainFinalResultHandler on the way out is too late
                // Create the result node first so the server results will end up before the failure stuff
                context.getResult();
                context.getFailureDescription().set(MESSAGES.operationFailedOrRolledBack());
                domainOperationContext.setFailureReported(true);
            }
        }
    }

    private Map<String, Map<ServerIdentity, ModelNode>> getOpsByGroup(Map<String, ModelNode> hostResults) {
        Map<String, Map<ServerIdentity, ModelNode>> result = new HashMap<String, Map<ServerIdentity, ModelNode>>();

        for (Map.Entry<String, ModelNode> entry : hostResults.entrySet()) {
            if (trace) {
                HOST_CONTROLLER_LOGGER.tracef("1st phase result from host %s is %s", entry.getKey(), entry.getValue());
            }
            ModelNode hostResult = entry.getValue().get(RESULT);
            if (hostResult.hasDefined(SERVER_OPERATIONS)) {
                String host = entry.getKey();
                for (ModelNode item : hostResult.get(SERVER_OPERATIONS).asList()) {
                    ModelNode op = item.require(OP);
                    for (Property prop : item.require(SERVERS).asPropertyList()) {
                        String group = prop.getValue().asString();
                        Map<ServerIdentity, ModelNode> groupMap = result.get(group);
                        if (groupMap == null) {
                            groupMap = new HashMap<ServerIdentity, ModelNode>();
                            result.put(group, groupMap);
                        }
                        groupMap.put(new ServerIdentity(host, group, prop.getName()), op);
                    }
                }
            }
        }
        return result;
    }

    private ModelNode getRolloutPlan(ModelNode rolloutPlan, Map<String, Map<ServerIdentity, ModelNode>> opsByGroup) throws OperationFailedException {

        if (rolloutPlan == null || !rolloutPlan.isDefined()) {
            rolloutPlan = getDefaultRolloutPlan(opsByGroup);
        }
        else {
            // Validate that plan covers all groups
            Set<String> found = new HashSet<String>();
            if (rolloutPlan.hasDefined(IN_SERIES)) {
                for (ModelNode series : rolloutPlan.get(IN_SERIES).asList()) {
                    if (series.hasDefined(CONCURRENT_GROUPS)) {
                        for(Property prop : series.get(CONCURRENT_GROUPS).asPropertyList()) {
                            validateServerGroupPlan(found, prop);
                        }
                    }
                    else if (series.hasDefined(SERVER_GROUP)) {
                        Property prop = series.get(SERVER_GROUP).asProperty();
                        validateServerGroupPlan(found, prop);
                    }
                    else {
                        throw new OperationFailedException(new ModelNode().set(MESSAGES.invalidRolloutPlan(series, IN_SERIES)));
                    }
                }
            }

            Set<String> groups = new HashSet<String>(opsByGroup.keySet());
            groups.removeAll(found);
            if (!groups.isEmpty()) {
                throw new OperationFailedException(new ModelNode().set(MESSAGES.invalidRolloutPlan(groups)));
            }
        }
        return rolloutPlan;
    }

    private void validateServerGroupPlan(Set<String> found, Property prop) throws OperationFailedException {
        if (!found.add(prop.getName())) {
            throw new OperationFailedException(new ModelNode().set(MESSAGES.invalidRolloutPlanGroupAlreadyExists(prop.getName())));
        }
        ModelNode plan = prop.getValue();
        if (plan.hasDefined(MAX_FAILURE_PERCENTAGE)) {
            if (plan.has(MAX_FAILED_SERVERS)) {
                plan.remove(MAX_FAILED_SERVERS);
            }
            int max = plan.get(MAX_FAILURE_PERCENTAGE).asInt();
            if (max < 0 || max > 100) {
                throw new OperationFailedException(new ModelNode().set(MESSAGES.invalidRolloutPlanRange(prop.getName(), MAX_FAILURE_PERCENTAGE, max)));
            }
        }
        if (plan.hasDefined(MAX_FAILED_SERVERS)) {
            int max = plan.get(MAX_FAILED_SERVERS).asInt();
            if (max < 0) {
                throw new OperationFailedException(new ModelNode().set(MESSAGES.invalidRolloutPlanLess(prop.getName(), MAX_FAILED_SERVERS, max)));
            }
        }
    }

    private ModelNode getDefaultRolloutPlan(Map<String, Map<ServerIdentity, ModelNode>> opsByGroup) {
        ModelNode result = new ModelNode();
        if (opsByGroup.size() > 0) {
            ModelNode groups = result.get(IN_SERIES).add().get(CONCURRENT_GROUPS);

            ModelNode groupPlan = new ModelNode();
            groupPlan.get(ROLLING_TO_SERVERS).set(false);
            groupPlan.get(MAX_FAILED_SERVERS).set(0);

            for (String group : opsByGroup.keySet()) {
                groups.add(group, groupPlan);
            }
            result.get(ROLLBACK_ACROSS_GROUPS).set(true);
        }
        return result;
    }

    private void reportHostFailures(final OperationContext context, final ModelNode operation) {

        final boolean isDomain = isDomainOperation(operation);
        if (!collectDomainFailure(context, isDomain)) {
            collectHostFailures(context, isDomain);
        }
    }

    private boolean collectDomainFailure(OperationContext context, final boolean isDomain) {
        final ModelNode coordinator = domainOperationContext.getCoordinatorResult();
        ModelNode domainFailure = null;
        if (isDomain &&  coordinator != null && coordinator.has(FAILURE_DESCRIPTION)) {
            domainFailure = coordinator.hasDefined(FAILURE_DESCRIPTION) ? coordinator.get(FAILURE_DESCRIPTION) : new ModelNode().set(MESSAGES.unexplainedFailure());
        }
        if (domainFailure != null) {
            context.getFailureDescription().get(DOMAIN_FAILURE_DESCRIPTION).set(domainFailure);
            domainOperationContext.setFailureReported(true);
            return true;
        }
        return false;
    }

    private boolean collectHostFailures(final OperationContext context, final boolean isDomain) {
        ModelNode hostFailureResults = null;
        for (Map.Entry<String, ModelNode> entry : domainOperationContext.getHostControllerResults().entrySet()) {
            ModelNode hostResult = entry.getValue();
            if (hostResult.has(FAILURE_DESCRIPTION)) {
                if (hostFailureResults == null) {
                    hostFailureResults = new ModelNode();
                }
                final ModelNode desc = hostResult.hasDefined(FAILURE_DESCRIPTION) ? hostResult.get(FAILURE_DESCRIPTION) : new ModelNode().set(MESSAGES.unexplainedFailure());
                hostFailureResults.add(entry.getKey(), desc);
            }
        }

        final ModelNode coordinator = domainOperationContext.getCoordinatorResult();
        if (!isDomain && coordinator != null && coordinator.has(FAILURE_DESCRIPTION)) {
            if (hostFailureResults == null) {
                hostFailureResults = new ModelNode();
            }
            final ModelNode desc = coordinator.hasDefined(FAILURE_DESCRIPTION) ? coordinator.get(FAILURE_DESCRIPTION) : new ModelNode().set(MESSAGES.unexplainedFailure());
            hostFailureResults.add(domainOperationContext.getLocalHostInfo().getLocalHostName(), desc);
        }

        if (hostFailureResults != null) {
            context.getFailureDescription().get(HOST_FAILURE_DESCRIPTIONS).set(hostFailureResults);
            domainOperationContext.setFailureReported(true);
            return true;
        }
        return false;
    }

    private boolean isDomainOperation(final ModelNode operation) {
        final PathAddress address = PathAddress.pathAddress(operation.require(OP_ADDR));
        return address.size() == 0 || !address.getElement(0).getKey().equals(HOST);
    }
}
