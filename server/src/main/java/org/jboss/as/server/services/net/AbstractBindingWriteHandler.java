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

import org.jboss.as.controller.NewOperationContext;
import org.jboss.as.controller.NewStepHandler;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.operations.validation.ParameterValidator;
import org.jboss.as.network.SocketBinding;
import org.jboss.as.server.operations.ServerWriteAttributeOperationHandler;
import org.jboss.dmr.ModelNode;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceName;

/**
 * Basic {@code OperationHandler} triggering a 'requireRestart' if a binding attribute is
 * changed and the service binding is bound.
 *
 * @author Emanuel Muckenhuber
 */
abstract class AbstractBindingWriteHandler extends ServerWriteAttributeOperationHandler {

    private static final ServiceName SOCKET_BINDING = SocketBinding.JBOSS_BINDING_NAME;

    protected AbstractBindingWriteHandler() {
        super();
    }

    protected AbstractBindingWriteHandler(ParameterValidator valueValidator) {
        super(valueValidator);
    }

    protected AbstractBindingWriteHandler(ParameterValidator valueValidator, ParameterValidator resolvedValueValidator) {
        super(valueValidator, resolvedValueValidator);
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
    protected void modelChanged(final NewOperationContext context, final ModelNode operation,
                                final String attributeName, final ModelNode newValue, final ModelNode currentValue) throws OperationFailedException {

        final boolean restartRequired = requiresRestart();
        boolean setReload = false;
        if (context.getType() == NewOperationContext.Type.SERVER) {
            if (restartRequired) {
                context.reloadRequired();
                setReload = true;
            } else {
                context.addStep(new NewStepHandler() {
                    public void execute(NewOperationContext context, ModelNode operation) throws OperationFailedException {
                        validateResolvedValue(attributeName, newValue);
                        final ModelNode resolvedValue = newValue.isDefined() ? newValue.resolve() : newValue;
                        final PathAddress address = PathAddress.pathAddress(operation.get(OP_ADDR));
                        final PathElement element = address.getLastElement();
                        final ServiceController<?> controller = context.getServiceRegistry(true).getRequiredService(SOCKET_BINDING.append(element.getValue()));
                        final SocketBinding binding = SocketBinding.class.cast(controller.getValue());
                        boolean mustRestart = binding.isBound();
                        if (mustRestart) {
                            context.reloadRequired();
                        } else {
                            handleRuntimeChange(operation, attributeName, resolvedValue, binding);
                        }

                        if (context.completeStep() != NewOperationContext.ResultAction.KEEP ) {
                            if (mustRestart) {
                                context.revertReloadRequired();
                            } else {
                                ModelNode resolvedPrevious = currentValue.isDefined() ? currentValue.resolve() : currentValue;
                                handleRuntimeRollback(operation, attributeName, resolvedPrevious, binding);
                            }
                        }
                    }
                }, NewOperationContext.Stage.RUNTIME);
            }
        }
        if (context.completeStep() != NewOperationContext.ResultAction.KEEP && setReload) {
            context.revertReloadRequired();
        }
    }

}
