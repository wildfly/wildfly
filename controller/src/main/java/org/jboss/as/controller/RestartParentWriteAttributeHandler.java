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
import org.jboss.as.controller.operations.validation.ParameterValidator;
import org.jboss.as.controller.registry.Resource;
import org.jboss.dmr.ModelNode;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceName;

/**
 * Simple {@link org.jboss.as.controller.AbstractWriteAttributeHandler} that, if allowed,
 * restarts a parent resource when a change is made. Otherwise the server is put into a forced reload.
 *
 * @author Jason T. Greene.
 */
public abstract class RestartParentWriteAttributeHandler extends AbstractWriteAttributeHandler<ModelNode> {
    private final String parentKeyName;

    public RestartParentWriteAttributeHandler(String parentKeyName) {
        this.parentKeyName = parentKeyName;
    }

    public RestartParentWriteAttributeHandler(String parentKeyName, ParameterValidator validator) {
        super(validator);
        this.parentKeyName = parentKeyName;

    }

    public RestartParentWriteAttributeHandler(String parentKeyName, AttributeDefinition... definitions) {
        super(definitions);
        this.parentKeyName = parentKeyName;
    }

    public RestartParentWriteAttributeHandler(String parentKeyName, ParameterValidator unresolvedValidator, ParameterValidator resolvedValidator) {
        super(unresolvedValidator, resolvedValidator);
        this.parentKeyName = parentKeyName;
    }

    @Override
    protected boolean applyUpdateToRuntime(OperationContext context, ModelNode operation, String attributeName, ModelNode resolvedValue, ModelNode currentValue, HandbackHolder<ModelNode> handbackHolder) throws OperationFailedException {

        PathAddress address = getParentAddress(PathAddress.pathAddress(operation.require(OP_ADDR)));
        ServiceName serviceName = getParentServiceName(address);
        ServiceController<?> service = serviceName != null ?
                             context.getServiceRegistry(false).getService(serviceName) : null;

        // No parent service, nothing to do
        if (service == null) {
            return false;
        }

        boolean restartServices = isResourceServiceRestartAllowed(context, service);

        if (restartServices) {
            ModelNode parentModel = getModel(context, address);
            if (parentModel != null && context.markResourceRestarted(address, this)) {
                removeServices(context, serviceName, parentModel);
                final ServiceVerificationHandler verificationHandler = new ServiceVerificationHandler();
                recreateParentService(context, address, parentModel, verificationHandler);
                context.addStep(verificationHandler, OperationContext.Stage.VERIFY);
                handbackHolder.setHandback(parentModel);
            }
        }

        // Fall back to server wide reload
        return !restartServices;
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

    protected abstract void recreateParentService(OperationContext context, PathAddress parentAddress, ModelNode parentModel, ServiceVerificationHandler verificationHandler) throws OperationFailedException;

    protected abstract ServiceName getParentServiceName(PathAddress parentAddress);


    protected PathAddress getParentAddress(PathAddress address) {
        return Util.getParentAddressByKey(address, parentKeyName);
    }


    @Override
    protected void revertUpdateToRuntime(OperationContext context, ModelNode operation, String attributeName, ModelNode valueToRestore, ModelNode resolvedValue, ModelNode invalidatedParentModel) throws OperationFailedException {

        if (invalidatedParentModel == null) {
            // We didn't restart services, so no need to revert
            return;
        }

        PathAddress address = getParentAddress(PathAddress.pathAddress(operation.require(OP_ADDR)));
        ServiceName serviceName = getParentServiceName(address);

        ModelNode parentModel = getOriginalModel(context, address);
        if (parentModel != null && context.revertResourceRestarted(address, this)) {
            removeServices(context, serviceName, invalidatedParentModel);
            recreateParentService(context, address, parentModel, null);
        }
    }

    private ModelNode getModel(OperationContext ctx, PathAddress address) {
        try {
            Resource resource = ctx.getRootResource().navigate(address);
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
