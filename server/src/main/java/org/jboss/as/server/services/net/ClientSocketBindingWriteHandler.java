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

import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.OperationStepHandler;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.operations.global.WriteAttributeHandlers;
import org.jboss.as.controller.operations.validation.ParameterValidator;
import org.jboss.as.network.ClientSocketBinding;
import org.jboss.dmr.ModelNode;
import org.jboss.msc.service.ServiceController;

import java.net.UnknownHostException;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.VALUE;

/**
 * @author Jaikiran Pai
 */
class ClientSocketBindingWriteHandler extends WriteAttributeHandlers.WriteAttributeOperationHandler {

    private final ParameterValidator resolvedValueValidator;
    private final boolean remoteDestination;

    ClientSocketBindingWriteHandler(ParameterValidator valueValidator, ParameterValidator resolvedValueValidator,
                                              final boolean remoteDestination) {
        super(valueValidator);
        this.resolvedValueValidator = resolvedValueValidator;
        this.remoteDestination = remoteDestination;
    }

    /**
     * Indicating whether the operation requires a restart irrespective of
     * the runtime state.
     *
     * @return
     */
    protected boolean requiresRestart() {
        return false;
    }

    @Override
    protected void modelChanged(final OperationContext context, final ModelNode operation,
                                final String attributeName, final ModelNode newValue, final ModelNode currentValue) throws OperationFailedException {

        final boolean restartRequired = requiresRestart();
        boolean setReload = false;
        if (context.getType() == OperationContext.Type.SERVER) {
            if (restartRequired) {
                context.reloadRequired();
                setReload = true;
            } else {
                context.addStep(new OperationStepHandler() {
                    public void execute(OperationContext context, ModelNode operation) throws OperationFailedException {
                        final ModelNode resolvedValue = newValue.isDefined() ? newValue.resolve() : newValue;
                        if (resolvedValueValidator != null) {
                            resolvedValueValidator.validateResolvedParameter(VALUE, resolvedValue);
                        }
                        final PathAddress address = PathAddress.pathAddress(operation.get(OP_ADDR));
                        final PathElement element = address.getLastElement();
                        final String bindingName = element.getValue();
                        final ModelNode bindingModel = context.readResource(PathAddress.EMPTY_ADDRESS).getModel();
                        final ServiceController<?> controller = context.getServiceRegistry(true).getRequiredService(ClientSocketBinding.CLIENT_SOCKET_BINDING_BASE_SERVICE_NAME.append(bindingName));
                        final ClientSocketBinding binding = controller.getState() == ServiceController.State.UP ? ClientSocketBinding.class.cast(controller.getValue()) : null;
                        final boolean bound = binding != null && binding.isBound();
                        if (binding == null) {
                            // existing is not started, so can't "update" it. Instead reinstall the service
                            handleBindingReinstall(context, bindingName, bindingModel);
                        } else {
                            // We don't allow runtime changes without a context reload for client socket bindings
                            // since any services which might have already injected/depended on the client
                            // socket binding service would have use the (now stale) attributes.
                            context.reloadRequired();
                        }
                        if (context.completeStep() != OperationContext.ResultAction.KEEP) {
                            if (binding == null) {
                                // Back to the old service
                                revertBindingReinstall(context, bindingName, bindingModel, attributeName, currentValue);
                            } else {
                                context.revertReloadRequired();
                            }
                        }
                    }
                }, OperationContext.Stage.RUNTIME);
            }
        }
        if (context.completeStep() != OperationContext.ResultAction.KEEP && setReload) {
            context.revertReloadRequired();
        }
    }

    private void handleBindingReinstall(OperationContext context, String bindingName, ModelNode bindingModel) throws OperationFailedException {
        context.removeService(ClientSocketBinding.CLIENT_SOCKET_BINDING_BASE_SERVICE_NAME.append(bindingName));
        try {
            if (remoteDestination) {
                RemoteDestinationClientSocketBindingAddHandler.installClientSocketBindingService(context, bindingModel, bindingName);
            } else {
                LocalDestinationClientSocketBindingAddHandler.installClientSocketBindingService(context, bindingModel, bindingName);
            }
        } catch (UnknownHostException e) {
            throw new OperationFailedException(new ModelNode().set(e.getLocalizedMessage()));
        }
    }

    private void revertBindingReinstall(OperationContext context, String bindingName, ModelNode bindingModel,
                                        String attributeName, ModelNode previousValue) {

        context.removeService(ClientSocketBinding.CLIENT_SOCKET_BINDING_BASE_SERVICE_NAME.append(bindingName));
        final ModelNode unresolvedConfig = bindingModel.clone();
        unresolvedConfig.get(attributeName).set(previousValue);
        try {
            if (remoteDestination) {
                RemoteDestinationClientSocketBindingAddHandler.installClientSocketBindingService(context, unresolvedConfig, bindingName);
            } else {
                LocalDestinationClientSocketBindingAddHandler.installClientSocketBindingService(context, unresolvedConfig, bindingName);
            }
        } catch (Exception e) {
            // Bizarro, as we installed the service before
            throw new RuntimeException(e);
        }
    }

}
