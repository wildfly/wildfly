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

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.DOMAIN_UUID;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.EXECUTE_FOR_COORDINATOR;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OPERATION_HEADERS;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ROLLOUT_PLAN;
import static org.jboss.as.domain.controller.DomainControllerLogger.HOST_CONTROLLER_LOGGER;
import static org.jboss.as.domain.controller.DomainControllerMessages.MESSAGES;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.jboss.as.controller.AccessAuditContext;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.ProxyController;
import org.jboss.as.domain.controller.LocalHostControllerInfo;
import org.jboss.as.host.controller.mgmt.DomainControllerRuntimeIgnoreTransformationRegistry;
import org.jboss.as.repository.ContentRepository;
import org.jboss.dmr.ModelNode;

/**
 * Coordinates the overall execution of an operation on behalf of the domain.
 *
 * @author Brian Stansberry (c) 2011 Red Hat Inc.
 */
public class OperationCoordinatorStepHandler {

    private final LocalHostControllerInfo localHostControllerInfo;
    private final ContentRepository contentRepository;
    private final Map<String, ProxyController> hostProxies;
    private final Map<String, ProxyController> serverProxies;
    private final OperationSlaveStepHandler localSlaveHandler;
    private final DomainControllerRuntimeIgnoreTransformationRegistry runtimeIgnoreTransformationRegistry;
    private volatile ExecutorService executorService;

    OperationCoordinatorStepHandler(final LocalHostControllerInfo localHostControllerInfo,
                                    ContentRepository contentRepository,
                                    final Map<String, ProxyController> hostProxies,
                                    final Map<String, ProxyController> serverProxies,
                                    final OperationSlaveStepHandler localSlaveHandler,
                                    final DomainControllerRuntimeIgnoreTransformationRegistry runtimeIgnoreTransformationRegistry) {
        this.localHostControllerInfo = localHostControllerInfo;
        this.contentRepository = contentRepository;
        this.hostProxies = hostProxies;
        this.serverProxies = serverProxies;
        this.localSlaveHandler = localSlaveHandler;
        this.runtimeIgnoreTransformationRegistry = runtimeIgnoreTransformationRegistry;
    }

    void execute(OperationContext context, ModelNode operation) throws OperationFailedException {

        // Determine routing
        OperationRouting routing = OperationRouting.determineRouting(context, operation, localHostControllerInfo);

        if (!localHostControllerInfo.isMasterDomainController()
                && !routing.isLocalOnly(localHostControllerInfo.getLocalHostName())) {
            // We cannot handle this ourselves
            routeToMasterDomainController(context, operation);
        }
        else if (routing.getSingleHost() != null && !localHostControllerInfo.getLocalHostName().equals(routing.getSingleHost())) {
            if (HOST_CONTROLLER_LOGGER.isTraceEnabled()) {
                HOST_CONTROLLER_LOGGER.trace("Remote single host");
            }
            // This host is the master, but this op is addressed specifically to another host.
            // This is possibly a two step operation, but it's not coordinated by this host.
            // Execute direct (which will proxy the request to the intended HC) and let the remote HC coordinate
            // any two step process (if there is one)
            configureDomainUUID(operation);
            executeDirect(context, operation);
        }
        else if (!routing.isTwoStep()) {
            // It's a domain or host level op (probably a read) that does not require bringing in other hosts or servers
            executeDirect(context, operation);
        }
        else {
            // Else we are responsible for coordinating a two-phase op
            // -- domain level op: apply to HostController models across domain and then push to servers
            // -- host level op: apply to our model  and then push to servers
            executeTwoPhaseOperation(context, operation, routing);
        }
    }

    public void setExecutorService(ExecutorService executorService) {
        this.executorService = executorService;
    }

    private ExecutorService getExecutorService() {
        return executorService == null ? Executors.newSingleThreadExecutor() : executorService;
    }

    private void routeToMasterDomainController(OperationContext context, ModelNode operation) {
        // Per discussion on 2011/03/07, routing requests from a slave to the
        // master may overly complicate the security infrastructure. Therefore,
        // the ability to do this is being disabled until it's clear that it's
        // not a problem
        context.getFailureDescription().set(MESSAGES.masterDomainControllerOnlyOperation(operation.get(OP).asString(),
                PathAddress.pathAddress(operation.get(OP_ADDR))));
        context.stepCompleted();
    }

    /**
     * Directly handles the op in the standard way the default prepare step handler would
     * @param context the operation execution context
     * @param operation the operation
     * @throws OperationFailedException if there is no handler registered for the operation
     */
    private void executeDirect(OperationContext context, ModelNode operation) throws OperationFailedException {
        if (HOST_CONTROLLER_LOGGER.isTraceEnabled()) {
            HOST_CONTROLLER_LOGGER.tracef("%s executing direct", getClass().getSimpleName());
        }
        PrepareStepHandler.executeDirectOperation(context, operation);
    }

    private void executeTwoPhaseOperation(OperationContext context, ModelNode operation, OperationRouting routing) throws OperationFailedException {
        if (HOST_CONTROLLER_LOGGER.isTraceEnabled()) {
            HOST_CONTROLLER_LOGGER.trace("Executing two-phase");
        }

        configureDomainUUID(operation);

        DomainOperationContext overallContext = new DomainOperationContext(localHostControllerInfo);

        // Get a copy of the rollout plan so it doesn't get disrupted by any handlers
        ModelNode rolloutPlan = operation.hasDefined(OPERATION_HEADERS) && operation.get(OPERATION_HEADERS).has(ROLLOUT_PLAN)
            ? operation.get(OPERATION_HEADERS).remove(ROLLOUT_PLAN) : new ModelNode();

        // A stage that on the way out fixes up the result/failure description. On the way in it does nothing
        context.addStep(new DomainFinalResultHandler(overallContext), OperationContext.Stage.MODEL);

        final ModelNode slaveOp = operation.clone();
        slaveOp.get(OPERATION_HEADERS, EXECUTE_FOR_COORDINATOR).set(true);
        slaveOp.protect();

        // If necessary, execute locally first. This gets all of the Stage.MODEL, Stage.RUNTIME, Stage.VERIFY
        // steps registered. A failure in those will prevent the rest of the steps below executing
        String localHostName = localHostControllerInfo.getLocalHostName();
        if (routing.isLocalCallNeeded(localHostName)) {
            ModelNode localResponse = overallContext.getCoordinatorResult();
            localSlaveHandler.addSteps(context, slaveOp.clone(), localResponse, false);
        }

        if (localHostControllerInfo.isMasterDomainController()) {

            // Add steps to invoke on the HC for each relevant slave
            Set<String> remoteHosts = new HashSet<String>(routing.getHosts());
            boolean global = remoteHosts.size() == 0;
            remoteHosts.remove(localHostName);

            if (remoteHosts.size() > 0 || global) {
                // Lock the controller to ensure there are no topology changes mid-op.
                // This assumes registering/unregistering a remote proxy will involve an op and hence will block
                context.acquireControllerLock();

                if (global) {
                    remoteHosts.addAll(hostProxies.keySet());
                }

                Map<String, ProxyController> remoteProxies = new HashMap<String, ProxyController>();
                for (String host : remoteHosts) {
                    ProxyController proxy = hostProxies.get(host);
                    if (proxy != null) {
                        remoteProxies.put(host, proxy);
                    } else if (!global) {
                        throw MESSAGES.invalidOperationTargetHost(host);
                    }
                }

                context.addStep(slaveOp.clone(), new DomainSlaveHandler(remoteProxies, overallContext, runtimeIgnoreTransformationRegistry), OperationContext.Stage.DOMAIN);
            }
        }

        // Finally, the step to formulate and execute the 2nd phase rollout plan
        context.addStep(new DomainRolloutStepHandler(hostProxies, serverProxies, overallContext, rolloutPlan, getExecutorService()), OperationContext.Stage.DOMAIN);

        context.stepCompleted();
    }

    static void configureDomainUUID(ModelNode operation) {
        if (!operation.hasDefined(OPERATION_HEADERS) || !operation.get(OPERATION_HEADERS).hasDefined(DOMAIN_UUID)) {
            String domainUUID = UUID.randomUUID().toString();
            operation.get(OPERATION_HEADERS, DOMAIN_UUID).set(domainUUID);
            AccessAuditContext accessContext = SecurityActions.currentAccessAuditContext();
            if (accessContext != null) {
                accessContext.setDomainUuid(domainUUID);
            }
        }
    }

}
