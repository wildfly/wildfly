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

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.EXECUTE_FOR_COORDINATOR;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OPERATION_HEADERS;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;

import java.util.Map;

import org.jboss.as.controller.ControllerMessages;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.OperationStepHandler;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.ProxyController;
import org.jboss.as.controller.extension.ExtensionRegistry;
import org.jboss.as.controller.registry.ImmutableManagementResourceRegistration;
import org.jboss.as.controller.registry.Resource;
import org.jboss.as.domain.controller.LocalHostControllerInfo;
import org.jboss.as.domain.controller.operations.ApplyMissingDomainModelResourcesHandler;
import org.jboss.as.host.controller.ignored.IgnoredDomainResourceRegistry;
import org.jboss.as.host.controller.mgmt.DomainControllerRuntimeIgnoreTransformationRegistry;
import org.jboss.dmr.ModelNode;

/**
 * Performs the host specific overall execution of an operation on a slave, on behalf of the domain controller.
 *
 * @author Brian Stansberry (c) 2011 Red Hat Inc.
 */
public class OperationSlaveStepHandler {

    private final LocalHostControllerInfo localHostControllerInfo;
    private final Map<String, ProxyController> serverProxies;
    private final IgnoredDomainResourceRegistry ignoredDomainResourceRegistry;
    private final ExtensionRegistry extensionRegistry;
    private volatile ApplyMissingDomainModelResourcesHandler applyMissingDomainModelResourcesHandler;

    OperationSlaveStepHandler(final LocalHostControllerInfo localHostControllerInfo, Map<String, ProxyController> serverProxies,
                              IgnoredDomainResourceRegistry ignoredDomainResourceRegistry, ExtensionRegistry extensionRegistry) {
        this.localHostControllerInfo = localHostControllerInfo;
        this.serverProxies = serverProxies;
        this.ignoredDomainResourceRegistry = ignoredDomainResourceRegistry;
        this.extensionRegistry = extensionRegistry;
    }

    void intialize(ApplyMissingDomainModelResourcesHandler applyMissingDomainModelResourcesHandler) {
        this.applyMissingDomainModelResourcesHandler = applyMissingDomainModelResourcesHandler;
    }

    void execute(OperationContext context, ModelNode operation) throws OperationFailedException {

        ModelNode headers = operation.get(OPERATION_HEADERS);
        headers.remove(EXECUTE_FOR_COORDINATOR);
        final ModelNode missingResources = operation.get(OPERATION_HEADERS).remove(DomainControllerRuntimeIgnoreTransformationRegistry.MISSING_DOMAIN_RESOURCES);

        if (headers.hasDefined(DomainControllerLockIdUtils.DOMAIN_CONTROLLER_LOCK_ID)) {
            int id = headers.remove(DomainControllerLockIdUtils.DOMAIN_CONTROLLER_LOCK_ID).asInt();
            context.attach(DomainControllerLockIdUtils.DOMAIN_CONTROLLER_LOCK_ID_ATTACHMENT, id);
        }

        final HostControllerExecutionSupport hostControllerExecutionSupport = addSteps(context, operation, null, true);

        //Add the missing resources step first
        if (missingResources != null) {
            ModelNode applyMissingResourcesOp = ApplyMissingDomainModelResourcesHandler.createPiggyBackedMissingDataOperation(missingResources);
            context.addStep(applyMissingResourcesOp, applyMissingDomainModelResourcesHandler, OperationContext.Stage.MODEL, true);
        }



        // In case the actual operation fails make sure the result still gets formatted
        context.completeStep(new OperationContext.RollbackHandler() {
            @Override
            public void handleRollback(OperationContext context, ModelNode operation) {
                if (hostControllerExecutionSupport.getDomainOperation() != null) {
                    final ModelNode domainResult = hostControllerExecutionSupport.getFormattedDomainResult(context.getResult());
                    context.getResult().set(domainResult);
                }
            }
        });
    }

    HostControllerExecutionSupport addSteps(final OperationContext context, final ModelNode operation, final ModelNode response, final boolean recordResponse) throws OperationFailedException {
        final PathAddress originalAddress = PathAddress.pathAddress(operation.get(OP_ADDR));
        final ImmutableManagementResourceRegistration originalRegistration = context.getResourceRegistration();

        final HostControllerExecutionSupport hostControllerExecutionSupport =
                HostControllerExecutionSupport.Factory.create(operation, localHostControllerInfo.getLocalHostName(),
                        new LazyDomainModelProvider(context), ignoredDomainResourceRegistry, !localHostControllerInfo.isMasterDomainController() && localHostControllerInfo.isRemoteDomainControllerIgnoreUnaffectedConfiguration(),
                        extensionRegistry);
        ModelNode domainOp = hostControllerExecutionSupport.getDomainOperation();
        if (domainOp != null) {
            // Only require an existing registration if the domain op is not ignored
            if (originalRegistration == null) {
                throw new OperationFailedException(new ModelNode(ControllerMessages.MESSAGES.noSuchResourceType(originalAddress)));
            }
            addBasicStep(context, domainOp);
        }

        ServerOperationResolver resolver = new ServerOperationResolver(localHostControllerInfo.getLocalHostName(), serverProxies);
        ServerOperationsResolverHandler sorh = new ServerOperationsResolverHandler(
                resolver, hostControllerExecutionSupport, originalAddress, originalRegistration, response);
        context.addStep(sorh, OperationContext.Stage.DOMAIN);

        return hostControllerExecutionSupport;
    }

    /**
     * Directly handles the op in the standard way the default prepare step handler would
     * @param context the operation execution context
     * @param operation the operation
     * @throws OperationFailedException if no handler is registered for the operation
     */
    private void addBasicStep(OperationContext context, ModelNode operation) throws OperationFailedException {
        final String operationName = operation.require(OP).asString();

        final OperationStepHandler stepHandler = context.getResourceRegistration().getOperationHandler(PathAddress.EMPTY_ADDRESS, operationName);
        if(stepHandler != null) {
            context.addStep(operation, stepHandler, OperationContext.Stage.MODEL);
        } else {
            throw new OperationFailedException(new ModelNode(ControllerMessages.MESSAGES.noHandlerForOperation(operationName, PathAddress.pathAddress(operation.get(OP_ADDR)))));
        }
    }

    /** Lazily provides a copy of the domain model */
    private static class LazyDomainModelProvider implements HostControllerExecutionSupport.DomainModelProvider {
        private final OperationContext context;
        private Resource domainModelResource;

        private LazyDomainModelProvider(OperationContext context) {
            this.context = context;
        }

        public Resource getDomainModel() {
            if (domainModelResource == null) {
                domainModelResource = context.readResourceFromRoot(PathAddress.EMPTY_ADDRESS, true);
            }
            return domainModelResource;
        }
    }
}
