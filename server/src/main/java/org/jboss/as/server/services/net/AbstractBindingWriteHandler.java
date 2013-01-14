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

package org.jboss.as.server.services.net;

import org.jboss.as.controller.AbstractWriteAttributeHandler;
import org.jboss.as.controller.AttributeDefinition;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;

import java.net.UnknownHostException;

import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.PathElement;
import org.jboss.as.network.SocketBinding;
import org.jboss.dmr.ModelNode;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceName;

/**
 * Basic {@code OperationHandler} triggering a 'requireRestart' if a binding attribute is
 * changed and the service binding is bound.
 *
 * @author Emanuel Muckenhuber
 */
abstract class AbstractBindingWriteHandler extends AbstractWriteAttributeHandler<AbstractBindingWriteHandler.RollbackInfo> {

    private static final ServiceName SOCKET_BINDING = SocketBinding.JBOSS_BINDING_NAME;
    public AbstractBindingWriteHandler(AttributeDefinition... definitions) {
        super(definitions);
    }

    /**
     * Handle the actual runtime change.
     *
     * @param operation      the original operation
     * @param attributeName  the attribute name
     * @param attributeValue the new attribute value
     * @param binding        the resolved socket binding
     * @throws OperationFailedException
     */
    abstract void handleRuntimeChange(final ModelNode operation, final String attributeName, final ModelNode attributeValue, final SocketBinding binding) throws OperationFailedException;

    /**
     * Handle the actual runtime change.
     *
     * @param operation      the original operation
     * @param attributeName  the attribute name
     * @param previousValue  the attribute value before the change
     * @param binding        the resolved socket binding
     * @throws OperationFailedException
     */
    abstract void handleRuntimeRollback(final ModelNode operation, final String attributeName, final ModelNode previousValue, final SocketBinding binding);

    /**
     * Indicates if a change requires a reload, regardless of whether the socket-binding was bound or not.
     *
     * @return {@code true} if a reload is required, {@code false} otherwise
     */
    protected boolean requiresRestart() {
        return false;
    }

    @Override
    protected boolean applyUpdateToRuntime(final OperationContext context, final ModelNode operation, final String attributeName, final ModelNode resolvedValue,
                                           final ModelNode currentValue, final HandbackHolder<RollbackInfo> handbackHolder) throws OperationFailedException {

        final PathAddress address = PathAddress.pathAddress(operation.get(OP_ADDR));
        final PathElement element = address.getLastElement();
        final String bindingName = element.getValue();
        final ModelNode bindingModel = context.readResource(PathAddress.EMPTY_ADDRESS).getModel();
        final ServiceController<?> controller = context.getServiceRegistry(true).getRequiredService(SOCKET_BINDING.append(bindingName));
        final SocketBinding binding = controller.getState() == ServiceController.State.UP ? SocketBinding.class.cast(controller.getValue()) : null;
        final boolean bound = binding != null && binding.isBound();
        if (binding == null) {
            // existing is not started, so can't update it. Instead reinstall the service
            handleBindingReinstall(context, bindingName, bindingModel);
        } else if (bound) {
            // Cannot edit bound sockets
            return true;
        } else {
            handleRuntimeChange(operation, attributeName, resolvedValue, binding);
        }
        handbackHolder.setHandback(new RollbackInfo(bindingName, bindingModel, binding));
        return requiresRestart();
    }

    @Override
    protected void revertUpdateToRuntime(final OperationContext context, final ModelNode operation, final String attributeName, final ModelNode valueToRestore,
                                         final ModelNode valueToRevert, final RollbackInfo handback) throws OperationFailedException {
        if(handback != null) {
            // in case the binding wasn't installed
            if(handback.revertBinding()) {
                revertBindingReinstall(context, handback.bindingName, handback.bindingModel, attributeName, valueToRevert);
            } else {
                handleRuntimeRollback(operation, attributeName, valueToRevert, handback.binding);
            }
        }
    }

    private void handleBindingReinstall(OperationContext context, String bindingName, ModelNode bindingModel) throws OperationFailedException {
        context.removeService(SOCKET_BINDING.append(bindingName));
        try {
            BindingAddHandler.installBindingService(context, bindingModel, bindingName, null);
        } catch (UnknownHostException e) {
            throw new OperationFailedException(new ModelNode().set(e.toString()));
        }
    }

    private void revertBindingReinstall(OperationContext context, String bindingName, ModelNode bindingModel,
                                        String attributeName, ModelNode previousValue) {
        context.removeService(SOCKET_BINDING.append(bindingName));
        ModelNode unresolvedConfig = bindingModel.clone();
        unresolvedConfig.get(attributeName).set(previousValue);
        try {
            BindingAddHandler.installBindingService(context, unresolvedConfig, bindingName, null);
        } catch (Exception e) {
            // Bizarro, as we installed the service before
            throw new RuntimeException(e);
        }
    }

    static class RollbackInfo {

        private final String bindingName;
        private final ModelNode bindingModel;
        private final SocketBinding binding;

        RollbackInfo(String bindingName, ModelNode bindingModel, SocketBinding binding) {
            this.bindingName = bindingName;
            this.bindingModel = bindingModel;
            this.binding = binding;
        }

        boolean revertBinding() {
            return binding == null;
        }

    }

}
