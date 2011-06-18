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

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ADD;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.BYTES;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.COMPOSITE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.CONTENT;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.DEPLOYMENT;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.HASH;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.INPUT_STREAM_INDEX;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OPERATION_HEADERS;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.STEPS;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.URL;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.jboss.as.controller.NewOperationContext;
import org.jboss.as.controller.NewProxyController;
import org.jboss.as.controller.NewStepHandler;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.client.Operation;
import org.jboss.as.controller.registry.ModelNodeRegistration;
import org.jboss.as.domain.controller.LocalHostControllerInfo;
import org.jboss.as.domain.controller.operations.deployment.DeploymentFullReplaceHandler;
import org.jboss.as.domain.controller.operations.deployment.DeploymentUploadUtil;
import org.jboss.as.domain.controller.operations.deployment.NewDeploymentUploadUtil;
import org.jboss.dmr.ModelNode;

/**
 * Coordinates the overall execution of an operation on behalf of the domain.
 *
 * @author Brian Stansberry (c) 2011 Red Hat Inc.
 */
public class OperationCoordinatorStepHandler implements NewStepHandler {

    private final LocalHostControllerInfo localHostControllerInfo;
    private final Map<String, NewProxyController> hostProxies;
    private final OperationSlaveStepHandler localSlaveHandler;
    private volatile ExecutorService executorService;

    OperationCoordinatorStepHandler(final LocalHostControllerInfo localHostControllerInfo,
                                    final Map<String, NewProxyController> hostProxies, OperationSlaveStepHandler localSlaveHandler) {
        this.localHostControllerInfo = localHostControllerInfo;
        this.hostProxies = hostProxies;
        this.localSlaveHandler = localSlaveHandler;
    }

    @Override
    public void execute(NewOperationContext context, ModelNode operation) throws OperationFailedException {

        // Determine routing
        ModelNodeRegistration opRegistry = context.getModelNodeRegistration();
        OperationRouting routing = OperationRouting.determineRouting(operation, localHostControllerInfo, opRegistry);

        if (!localHostControllerInfo.isMasterDomainController()
                && (routing.isRouteToMaster() || !routing.isLocalOnly(localHostControllerInfo.getLocalHostName()))) {
            // We cannot handle this ourselves
            routetoMasterDomainController(context, operation);
        }
        else if (routing.getSingleHost() != null && !localHostControllerInfo.getLocalHostName().equals(routing.getSingleHost())) {
            // Possibly a two step operation, but not coordinated by this host. Execute direct and let the remote HC
            // coordinate any two step process (if there is one)
            executeDirect(context, operation);
        }
        else if (!routing.isTwoStep()) {
            // It's a domain level op (probably a read) that does not require bringing in other hosts or servers
            executeDirect(context, operation);
        }
        else {
            // Else we are responsible for coordinating a two-phase op
            // -- apply to DomainController models across domain and then push to servers
            executeTwoPhaseOperation(context, operation, routing);
        }


    }

    public void setExecutorService(ExecutorService executorService) {
        this.executorService = executorService;
    }

    private ExecutorService getExecutorService() {
        return executorService == null ? Executors.newSingleThreadExecutor() : executorService;
    }

    private void routetoMasterDomainController(NewOperationContext context, ModelNode operation) {
        // System.out.println("------ route to master ");
        // Per discussion on 2011/03/07, routing requests from a slave to the
        // master may overly complicate the security infrastructure. Therefore,
        // the ability to do this is being disabled until it's clear that it's
        // not a problem
        context.getFailureDescription().set(String.format("Operation %s for address %s can only handled by the " +
                "master Domain Controller; this host is not the master Domain Controller",
                operation.get(OP).asString(), PathAddress.pathAddress(operation.get(OP_ADDR))));
        context.completeStep();
    }

    /**
     * Directly handles the op in the standard way the default prepare step handler would
     * @param context the operation execution context
     * @param operation the operation
     * @throws OperationFailedException
     */
    private void executeDirect(NewOperationContext context, ModelNode operation) throws OperationFailedException {
        final String operationName =  operation.require(OP).asString();
        final NewStepHandler stepHandler = context.getModelNodeRegistration().getOperationHandler(PathAddress.EMPTY_ADDRESS, operationName);
        if(stepHandler != null) {
            context.addStep(stepHandler, NewOperationContext.Stage.MODEL);
        } else {
            context.getFailureDescription().set(String.format("No handler for operation %s at address %s", operationName, PathAddress.pathAddress(operation.get(OP_ADDR))));
        }
        context.completeStep();
    }

    private void executeTwoPhaseOperation(NewOperationContext context, ModelNode operation, OperationRouting routing) throws OperationFailedException {

        DomainOperationContext overallContext = new DomainOperationContext(localHostControllerInfo);

        final ModelNode slaveOp = operation.clone();
        // Hackalicious approach to not streaming content to all the slaves
        storeDeploymentContent(slaveOp, context);
        slaveOp.get(OPERATION_HEADERS, PrepareStepHandler.EXECUTE_FOR_COORDINATOR).set(true);
        slaveOp.protect();

        // If necessary, execute locally first. This gets all of the Stage.MODEL, Stage.RUNTIME, Stage.VERIFY
        // steps registered. A failure in those will prevent the rest of the steps below executing
        String localHostName = localHostControllerInfo.getLocalHostName();
        if (routing.isLocalCallNeeded(localHostName)) {
            ModelNode localResponse = new ModelNode();
            overallContext.setCoordinatorResult(localResponse);
            localSlaveHandler.addSteps(context, slaveOp, localResponse, false);
        }

        if (localHostControllerInfo.isMasterDomainController()) {
            // Add steps to invoke on the HC for each relevant slave
            Set<String> remoteHosts = new HashSet<String>(routing.getHosts());
            boolean global = remoteHosts.size() == 0;
            remoteHosts.remove(localHostName);

            if (remoteHosts.size() > 0 || global) {
                // Lock the controller to ensure there are no topology changes mid-op.
                // This assumes registering/unregistering a remote proxy will involve an op and hence will block
                // TODO perhaps expose a direct OperationContext method to do this
                context.getServiceRegistry(true);

                if (global) {
                    remoteHosts.addAll(hostProxies.keySet());
                }

                Map<String, NewProxyController> remoteProxies = new HashMap<String, NewProxyController>();
                for (String host : remoteHosts) {
                    NewProxyController proxy = hostProxies.get(host);
                    if (proxy != null) {
                        remoteProxies.put(host, proxy);
                    } else if (!global) {
                        throw new OperationFailedException(new ModelNode().set(String.format("Operation targets host %s but that host is not registered", host)));
                    }
                }

                context.addStep(new ModelNode(), slaveOp, new DomainSlaveHandler(remoteProxies, overallContext, executorService), NewOperationContext.Stage.DOMAIN);

            }
        }

        // Finally, the step to formulate and execute the 2nd phase rollout plan
        context.addStep(new DomainRolloutStepHandler(overallContext, getExecutorService()), NewOperationContext.Stage.DOMAIN);

        // Always add the step to assemble the final result
        context.addStep(new DomainResultHandler(overallContext), NewOperationContext.Stage.DOMAIN);

        context.completeStep();
    }

    private void storeDeploymentContent(ModelNode opNode, NewOperationContext context) throws OperationFailedException {

        try {
            // A pretty painful hack. We analyze the operation for operations that include deployment content attachments; if found
            // we store the content and replace the attachments

            PathAddress address = PathAddress.pathAddress(opNode.get(OP_ADDR));
            if (address.size() == 0) {
                String opName = opNode.get(OP).asString();
                if (DeploymentFullReplaceHandler.OPERATION_NAME.equals(opName) && hasStorableContent(opNode)) {
                    byte[] hash = NewDeploymentUploadUtil.storeDeploymentContent(context, opNode, localHostControllerInfo.getContentRepository());
                    opNode.remove(INPUT_STREAM_INDEX);
                    opNode.get(HASH).set(hash);
                }
                else if (COMPOSITE.equals(opName) && opNode.hasDefined(STEPS)){
                    // Check the steps
                    for (ModelNode childOp : opNode.get(STEPS).asList()) {
                        storeDeploymentContent(childOp, context);
                    }
                }
            }
            else if (address.size() == 1 && DEPLOYMENT.equals(address.getElement(0).getKey())
                    && ADD.equals(opNode.get(OP).asString()) && hasStorableContent(opNode)) {
                byte[] hash = NewDeploymentUploadUtil.storeDeploymentContent(context, opNode, localHostControllerInfo.getContentRepository());
                opNode.remove(INPUT_STREAM_INDEX);
                opNode.get(HASH).set(hash);
            }
        } catch (IOException ioe) {
            throw new OperationFailedException(new ModelNode().set(String.format("Caught IOException storing deployment content -- %s", ioe)));
        }
    }

    private boolean hasStorableContent(ModelNode operation) {
        if (!operation.hasDefined(CONTENT)) {
            final ModelNode content = operation.require(CONTENT);
            for (ModelNode item : content.asList()) {
                if (hasValidContentAdditionParameterDefined(item)) {
                    return true;
                }
            }
        }
        return false;
    }

    private static final List<String> CONTENT_ADDITION_PARAMETERS = Arrays.asList(INPUT_STREAM_INDEX, BYTES, URL);


    private static boolean hasValidContentAdditionParameterDefined(ModelNode item) {
        for (String s : CONTENT_ADDITION_PARAMETERS) {
            if (item.hasDefined(s)) {
                return true;
            }
        }
        return false;
    }

}
