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
package org.jboss.as.controller;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;

import java.util.NoSuchElementException;

import org.jboss.as.controller.operations.common.Util;
import org.jboss.as.controller.registry.Resource;
import org.jboss.dmr.ModelNode;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceName;

/**
 * Simple remove handler that, if allowed, restarts a parent resource when a child is removed.
 * Otherwise the server is put into a forced reload.
 *
 * @author Jason T. Greene
 */
public abstract class RestartParentResourceHandlerBase implements OperationStepHandler {
    private final String parentKeyName;

    protected RestartParentResourceHandlerBase(String parentKeyName) {
        this.parentKeyName = parentKeyName;
    }

    @Override
    public void execute(OperationContext context, ModelNode operation) throws OperationFailedException {

        // Do the simple model part
        updateModel(context, operation);

        if (!context.isBooting() && requiresRuntime(context)) {
            context.addStep(new OperationStepHandler() {
                @Override
                public void execute(OperationContext context, ModelNode operation) throws OperationFailedException {

                    PathAddress address = getParentAddress(PathAddress.pathAddress(operation.require(OP_ADDR)));
                    ServiceName serviceName = getParentServiceName(address);
                    final ServiceController<?> service = serviceName != null ?
                            context.getServiceRegistry(false).getService(serviceName) : null;

                    ModelNode parentModel = null;
                    boolean servicesRestarted = false;
                    final boolean reloadRequired = service != null && !isResourceServiceRestartAllowed(context, service);
                    if (reloadRequired) {
                        context.reloadRequired();
                    } else if (service != null ) {
                        parentModel = getModel(context, address);
                        if (parentModel != null && context.markResourceRestarted(address, this)) {
                            removeServices(context, serviceName, parentModel);
                            final ServiceVerificationHandler verificationHandler = new ServiceVerificationHandler();
                            recreateParentService(context, address, parentModel, verificationHandler);
                            context.addStep(verificationHandler, OperationContext.Stage.VERIFY);
                            servicesRestarted = true;
                        }
                    } // else  No parent service, nothing to do

                    // If we restarted services, keep the model that drove the new services so we can
                    // revert the change on rollback
                    final ModelNode invalidatedParentModel = servicesRestarted ? parentModel : null;

                    context.completeStep(new OperationContext.RollbackHandler() {
                        @Override
                        public void handleRollback(OperationContext context, ModelNode operation) {
                            if (reloadRequired) {
                                context.revertReloadRequired();
                            } else if (invalidatedParentModel != null) {
                                recoverServices(context, operation, invalidatedParentModel);
                            }
                        }
                    });
                }
            }, OperationContext.Stage.RUNTIME);
        }
        context.completeStep(OperationContext.RollbackHandler.NOOP_ROLLBACK_HANDLER);
    }

    /**
     * Gets whether this operation needs to update the runtime. The default implementation returns {@code true}
     * if {@link OperationContext#getProcessType() the process type} is not {@link ProcessType#HOST_CONTROLLER}.
     *
     * @param context the operation context
     * @return {@code true} if the operation should update the runtime; {@code false} if it only updates the configuration
     *         model
     */
    protected boolean requiresRuntime(OperationContext context) {
        return context.getProcessType() != ProcessType.HOST_CONTROLLER;
    }

    /**
     * Gets whether a restart of the parent resource's services is allowed. This default implementation
     * checks whether {@link OperationContext#isResourceServiceRestartAllowed() the context allows resource service restarts};
     * subclasses could also check the state of the {@code service}.
     *
     * @param context the operation context
     * @param service the parent service
     * @return {@code true} if a restart is allowed; {@code false}
     */
    protected boolean isResourceServiceRestartAllowed(final OperationContext context, final ServiceController<?> service) {
        return context.isResourceServiceRestartAllowed();
    }

    /**
     * Removes services. This default implementation simply
     * {@link OperationContext#removeService(ServiceController) instructs the context to remove the parentService}.
     * Subclasses could use the provided {@code parentModel} to identify and remove other services.
     *
     * @param context the operation context
     * @param parentService the name of the parent service
     * @param parentModel the model associated with the parent resource, including nodes for any child resources
     *
     * @throws OperationFailedException if there is a problem removing the services
     */
    protected void removeServices(final OperationContext context, final ServiceName parentService, final ModelNode parentModel) throws OperationFailedException {
        context.removeService(parentService);
    }

    /**
     * Performs the update to the persistent configuration model.
     *
     * @param context the operation context
     * @param operation  the operation
     * @throws OperationFailedException if there is a problem updating the model
     */
    protected abstract void updateModel(final OperationContext context, final ModelNode operation) throws OperationFailedException;

    /**
     * Recreate the parent service(s) using the given model.
     *
     * @param context the operation context
     * @param parentAddress the address of the parent resource
     * @param parentModel the current configuration model for the parent resource and its children
     * @param verificationHandler handler for verifying newly installed services
     *
     * @throws OperationFailedException if there is a problem installing the services
     */
    protected abstract void recreateParentService(OperationContext context, PathAddress parentAddress, ModelNode parentModel, ServiceVerificationHandler verificationHandler) throws OperationFailedException;

    /**
     * Gets the name of the parent service.
     *
     * @param parentAddress the address of the parent resource
     * @return  the service name
     */
    protected abstract ServiceName getParentServiceName(PathAddress parentAddress);


    protected PathAddress getParentAddress(PathAddress address) {
        return Util.getParentAddressByKey(address, parentKeyName);
    }

    private void recoverServices(final OperationContext context, final ModelNode operation, final ModelNode invalidatedParentModel) {
        PathAddress address = getParentAddress(PathAddress.pathAddress(operation.require(OP_ADDR)));
        ServiceName serviceName = getParentServiceName(address);

        ModelNode parentModel = getOriginalModel(context, address);
        if (parentModel != null && context.revertResourceRestarted(address, this)) {
            try {
                removeServices(context, serviceName, invalidatedParentModel);
                recreateParentService(context, address, parentModel, null);
            } catch (OperationFailedException e) {
                throw ControllerMessages.MESSAGES.failedToRecoverServices(e);
            }
        }
    }

    private ModelNode getModel(OperationContext ctx, PathAddress address) {
        try {
            Resource resource = ctx.readResourceFromRoot(address);
            return Resource.Tools.readModel(resource);
        } catch (NoSuchElementException e) {
            return null;
        }
    }

    private ModelNode getOriginalModel(OperationContext ctx, PathAddress address) {
        try {
            Resource resource = ctx.getOriginalRootResource().navigate(address);
            return Resource.Tools.readModel(resource);
        } catch (NoSuchElementException e) {
            return null;
        }
    }
}
