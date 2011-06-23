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

package org.jboss.as.server.operations;

import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.operations.global.WriteAttributeHandlers.WriteAttributeOperationHandler;
import org.jboss.as.controller.operations.validation.ParameterValidator;
import org.jboss.dmr.ModelNode;

/**
 * Abstract superclass for write-attribute operation handlers that run on the
 * server.
 * <p/>
 * TODO Get ServerOperationContext and BootOperationHandler concepts into
 * the controller module so the same handlers can apply to domain/host attributes.
 *
 * @author Brian Stansberry
 */
public abstract class ServerWriteAttributeOperationHandler extends WriteAttributeOperationHandler {

    private final ParameterValidator resolvedValueValidator;

    /**
     * Creates a handler that doesn't validate values.
     */
    public ServerWriteAttributeOperationHandler() {
        this(null, null);
    }

    /**
     * Creates a handler that users the given {@code valueValidator}
     * to validate values before applying them to the model.
     *
     * @param valueValidator the validator to use to validate the value. May be {@code null}
     */
    public ServerWriteAttributeOperationHandler(ParameterValidator valueValidator) {
        this(valueValidator, null);
    }

    /**
     * Creates a handler that users the given {@code valueValidator}
     * to validate values before applying them to the model, and a separate
     * validator to validate the {@link ModelNode#resolve() resolved value} it
     * after it has been applied to the mode.
     * <p/>
     * Typically if this constructor is used the {@code valueValidator} would
     * allow expressions, while the {@code resolvedValueValidator} would not.
     *
     * @param valueValidator         the validator to use to validate the value before application to the model. May be {@code null}     *
     * @param resolvedValueValidator the validator to use to validate the value before application to the model. May be {@code null}
     */
    public ServerWriteAttributeOperationHandler(ParameterValidator valueValidator, ParameterValidator resolvedValueValidator) {
        super(valueValidator);
        this.resolvedValueValidator = resolvedValueValidator;
    }

    @Override
    protected void modelChanged(final OperationContext context, final ModelNode operation,
                                final String attributeName, final ModelNode newValue, final ModelNode currentValue) throws OperationFailedException {

        boolean restartRequired = false;
        if (context.getType() == OperationContext.Type.SERVER) {
            validateResolvedValue(attributeName, newValue);
            ModelNode resolvedValue = newValue.isDefined() ? newValue.resolve() : newValue;
            restartRequired = applyUpdateToRuntime(context, operation, attributeName, resolvedValue, currentValue);
            if (restartRequired) {
                context.reloadRequired();
            }
        }

        if (context.completeStep() != OperationContext.ResultAction.KEEP && restartRequired) {
            context.revertReloadRequired();
        }
    }

    /**
     * If a resolved value validator was passed to the constructor, uses it to validate the value.
     * Subclasses can alter this behavior.
     */
    protected void validateResolvedValue(String name, ModelNode value) throws OperationFailedException {
        if (resolvedValueValidator != null) {
            resolvedValueValidator.validateParameter(name, value.resolve());
        }
    }


    /**
     * Hook to allow subclasses to make runtime changes to effect the attribute value change. Runtime changes
     * should be implemented by calling {@link org.jboss.as.controller.OperationContext#addStep(org.jboss.as.controller.OperationStepHandler, org.jboss.as.controller.OperationContext.Stage) adding a new step}
     * with {@link org.jboss.as.controller.OperationContext.Stage#RUNTIME}.
     * <p>
     * This default implementation simply returns {@code false}.
     * </p>
     *
     * @param context the context of the operation
     * @param operation the operation
     * @param attributeName the name of the attribute being modified
     * @param newValue the new value for the attribute
     * @param currentValue the existing value for the attribute
     *
     * @return {@code true} if the server requires restart to effect the attribute
     *         value change; {@code false} if not
     */
    protected boolean applyUpdateToRuntime(final OperationContext context, final ModelNode operation,
                                           final String attributeName, final ModelNode newValue, final ModelNode currentValue) throws OperationFailedException {
        return false;
    }


}
