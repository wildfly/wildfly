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

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.VALUE;

import java.net.UnknownHostException;

import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.OperationStepHandler;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.operations.global.WriteAttributeHandlers;
import org.jboss.as.controller.operations.validation.ParameterValidator;
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
abstract class AbstractBindingWriteHandler extends WriteAttributeHandlers.WriteAttributeOperationHandler {

    private static final ServiceName SOCKET_BINDING = SocketBinding.JBOSS_BINDING_NAME;

    private final ParameterValidator resolvedValueValidator;

    protected AbstractBindingWriteHandler() {
        this(null, null);
    }

    protected AbstractBindingWriteHandler(ParameterValidator valueValidator) {
        this(valueValidator, valueValidator);
    }

    protected AbstractBindingWriteHandler(ParameterValidator valueValidator, ParameterValidator resolvedValueValidator) {
        super(valueValidator);
        this.resolvedValueValidator = resolvedValueValidator;
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

    @Override
    protected void modelChanged(final OperationContext context, final ModelNode operation,
                                final String attributeName, final ModelNode newValue, final ModelNode currentValue) throws OperationFailedException {

        final boolean restartRequired = requiresRestart();
        boolean setReload = false;
        if (requiresRuntime(context)) {
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
                        final ServiceController<?> controller = context.getServiceRegistry(true).getRequiredService(SOCKET_BINDING.append(bindingName));
                        final SocketBinding binding = controller.getState() == ServiceController.State.UP ? SocketBinding.class.cast(controller.getValue()) : null;
                        final boolean bound = binding != null && binding.isBound();
                        if (binding == null) {
                            // existing is not started, so can't update it. Instead reinstall the service
                            handleBindingReinstall(context, bindingName, bindingModel);
                        }
                        else if (bound) {
                            // Cannot edit bound sockets
                            context.reloadRequired();
                        } else {
                            handleRuntimeChange(operation, attributeName, resolvedValue, binding);
                        }

                        if (context.completeStep() != OperationContext.ResultAction.KEEP ) {
                            if (binding == null) {
                                // Back to the old service
                                revertBindingReinstall(context, bindingName, bindingModel, attributeName, currentValue);
                            } else if (bound) {
                                context.revertReloadRequired();
                            } else {
                                ModelNode resolvedPrevious = currentValue.isDefined() ? currentValue.resolve() : currentValue;
                                handleRuntimeRollback(operation, attributeName, resolvedPrevious, binding);
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

    protected boolean requiresRuntime(OperationContext context) {
        return context.getType() == OperationContext.Type.SERVER;
    }

    private void handleBindingReinstall(OperationContext context, String bindingName, ModelNode bindingModel) throws OperationFailedException {
        context.removeService(SOCKET_BINDING.append(bindingName));
        try {
            BindingAddHandler.installBindingService(context, bindingModel, bindingName);
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
            BindingAddHandler.installBindingService(context, unresolvedConfig, bindingName);
        } catch (Exception e) {
            // Bizarro, as we installed the service before
            throw new RuntimeException(e);
        }
    }

}
