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

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.CONCURRENT_GROUPS;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.FAILED;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.FAILURE_DESCRIPTION;
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
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SUCCESS;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicBoolean;

import org.jboss.as.controller.NewModelController;
import org.jboss.as.controller.NewOperationContext;
import org.jboss.as.controller.NewProxyController;
import org.jboss.as.controller.NewStepHandler;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.PathAddress;
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
public class DomainRolloutStepHandler implements NewStepHandler {

    private final DomainOperationContext domainOperationContext;
    private final Map<String, NewProxyController> hostProxies;
    private final Map<String, NewProxyController> serverProxies;
    private final ExecutorService executorService;
    private final ModelNode providedRolloutPlan;

    public DomainRolloutStepHandler(final Map<String, NewProxyController> hostProxies,
                                    final Map<String, NewProxyController> serverProxies,
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
    public void execute(final NewOperationContext context, final ModelNode operation) throws OperationFailedException {

        // Clean out any response that leaked in from a MODEL/RUNTIME/VERIFY stage hander,
        // as from now on any response comes from DomainResultHandler
        context.getResult().set(new ModelNode());

        // Confirm no host failures
        boolean pushToServers = !domainOperationContext.hasHostLevelFailures();
        if (pushToServers) {
            ModelNode ourResult = domainOperationContext.getCoordinatorResult();
            if (ourResult.has(FAILURE_DESCRIPTION)) {
                System.out.println("coordinator failed: " + ourResult);
                pushToServers = false;
                domainOperationContext.setCompleteRollback(true);
            } else {
                System.out.println("coordinator succeeded: " + ourResult);
                for (ModelNode hostResult : domainOperationContext.getHostControllerResults().values()) {
                    if (hostResult.has(FAILURE_DESCRIPTION)) {
                        System.out.println("host failed: " + hostResult);
                        pushToServers = false;
                        domainOperationContext.setCompleteRollback(true);
                        break;
                    }
                }
            }
        }  else System.out.println("Complete rollback");

        if (pushToServers) {
            domainOperationContext.setCompleteRollback(false);
            final Map<ServerIdentity, ProxyTask> tasks = new HashMap<ServerIdentity, ProxyTask>();
            try {
                pushToServers(context, tasks);
                context.completeStep();
            } finally {

                // Inform the remote hosts whether to commit or roll back their updates
                // Do this in parallel
                // TODO consider blocking until all return?
                boolean completeRollback = domainOperationContext.isCompleteRollback();
                for (Map.Entry<ServerIdentity, ProxyTask> entry : tasks.entrySet()) {
                    boolean rollback = completeRollback || domainOperationContext.isServerGroupRollback(entry.getKey().getServerGroupName());
                    entry.getValue().finalizeTransaction(!rollback);
                }
            }
        } else {
            context.completeStep();
        }
    }

    private void pushToServers(final NewOperationContext context, final Map<ServerIdentity, ProxyTask> tasks) throws OperationFailedException {

        final String localHostName = domainOperationContext.getLocalHostInfo().getLocalHostName();
        Map<String, ModelNode> hostResults = new HashMap<String, ModelNode>(domainOperationContext.getHostControllerResults());
        if (domainOperationContext.getCoordinatorResult().isDefined()) {
            hostResults.put(localHostName, domainOperationContext.getCoordinatorResult());
        }
        Map<String, Map<ServerIdentity, ModelNode>> opsByGroup = getOpsByGroup(hostResults);
        if (opsByGroup.size() > 0) {

            final ModelNode rolloutPlan = getRolloutPlan(this.providedRolloutPlan, opsByGroup);
            System.out.println("Rollout plan is " + rolloutPlan);
            final NewServerOperationExecutor operationExecutor = new NewServerOperationExecutor() {
                @Override
                public ModelNode executeServerOperation(ServerIdentity server, ModelNode operation) {
                    NewProxyController proxy = hostProxies.get(server.getHostName());
                    if (proxy == null) {
                        if (localHostName.equals(server.getHostName())) {
                            // Use our server proxies
                            proxy = serverProxies.get(server.getServerName());
                            if (proxy == null) {
                                System.out.println("No proxy for " + server);
                                return null;
                            }
                        }
                    }
                    // TODO this seems convoluted
                    ProxyTask task = new ProxyTask(server.getHostName(), operation, context, proxy);
                    tasks.put(server, task);

                    boolean interrupted = false;
                    Future<ModelNode> future = null;
                    ModelNode result = null;
                    try {
                        future = executorService.submit(task);
                        result = task.getResult();
                    } catch (InterruptedException e) {
                        interrupted = true;
                        result = new ModelNode();
                        result.get(OUTCOME).set(FAILED);
                        result.get(FAILURE_DESCRIPTION).set(String.format("Interrupted waiting for result from server %s", server));
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
            System.out.println("Rollout plan result is " + planResult);
            if (planResult == NewRolloutPlanController.Result.FAILED) {
                domainOperationContext.setCompleteRollback(true);
            }
        }
    }

    private Map<String, Map<ServerIdentity, ModelNode>> getOpsByGroup(Map<String, ModelNode> hostResults) {
        Map<String, Map<ServerIdentity, ModelNode>> result = new HashMap<String, Map<ServerIdentity, ModelNode>>();

        for (Map.Entry<String, ModelNode> entry : hostResults.entrySet()) {
            System.out.println("Result from host " + entry.getKey() + " is " + entry.getValue());
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
                        throw new OperationFailedException(new ModelNode().set(String.format("Invalid rollout plan. %s is not a valid child of node %s", series, IN_SERIES)));
                    }
                }
            }

            Set<String> groups = new HashSet<String>(opsByGroup.keySet());
            groups.removeAll(found);
            if (!groups.isEmpty()) {
                throw new OperationFailedException(new ModelNode().set(String.format("Invalid rollout plan. Plan operations affect server groups %s that are not reflected in the rollout plan", groups)));
            }
        }
        return rolloutPlan;
    }

    private void validateServerGroupPlan(Set<String> found, Property prop) throws OperationFailedException {
        if (!found.add(prop.getName())) {
            throw new OperationFailedException(new ModelNode().set(String.format("Invalid rollout plan. Server group %s appears more than once in the plan.", prop.getName())));
        }
        ModelNode plan = prop.getValue();
        if (plan.hasDefined(MAX_FAILURE_PERCENTAGE)) {
            if (plan.has(MAX_FAILED_SERVERS)) {
                plan.remove(MAX_FAILED_SERVERS);
            }
            int max = plan.get(MAX_FAILURE_PERCENTAGE).asInt();
            if (max < 0 || max > 100) {
                throw new OperationFailedException(new ModelNode().set(String.format("Invalid rollout plan. Server group %s has a %s value of %s; must be between 0 and 100.", prop.getName(), MAX_FAILURE_PERCENTAGE, max)));
            }
        }
        if (plan.hasDefined(MAX_FAILED_SERVERS)) {
            int max = plan.get(MAX_FAILED_SERVERS).asInt();
            if (max < 0) {
                throw new OperationFailedException(new ModelNode().set(String.format("Invalid rollout plan. Server group %s has a %s value of %s; cannot be less than 0.", prop.getName(), MAX_FAILED_SERVERS, max)));
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
}
