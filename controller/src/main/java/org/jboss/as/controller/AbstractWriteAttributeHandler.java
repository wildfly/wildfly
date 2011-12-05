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

import static org.jboss.as.controller.ControllerLogger.MGMT_OP_LOGGER;
import static org.jboss.as.controller.ControllerMessages.MESSAGES;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.NAME;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.VALUE;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.controller.operations.validation.ParameterValidator;
import org.jboss.as.controller.operations.validation.ParametersValidator;
import org.jboss.as.controller.operations.validation.StringLengthValidator;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.dmr.ModelNode;

/**
 * Abstract handler for the write aspect of a
 * {@link ManagementResourceRegistration#registerReadWriteAttribute(AttributeDefinition, OperationStepHandler, OperationStepHandler) read-write attribute}.
 *
 * @param <T> the type of an object that, if stored by the
 * {@link AbstractWriteAttributeHandler#applyUpdateToRuntime(OperationContext, ModelNode, String, ModelNode, ModelNode, HandbackHolder)}
 * implementation, will be passed to
 * {@link AbstractWriteAttributeHandler#revertUpdateToRuntime(OperationContext, ModelNode, String, ModelNode, ModelNode, Object)}.
 *
 * @author Brian Stansberry (c) 2011 Red Hat Inc.
 */
public abstract class AbstractWriteAttributeHandler<T> implements OperationStepHandler {

    private final ParametersValidator nameValidator = new ParametersValidator();

    private final ParameterValidator unresolvedValueValidator;
    private final ParameterValidator resolvedValueValidator;
    private final Map<String, AttributeDefinition> attributeDefinitions;

    protected AbstractWriteAttributeHandler() {
        this(null, null);
    }

    protected AbstractWriteAttributeHandler(final ParameterValidator validator) {
        this(validator, validator);
    }

    protected AbstractWriteAttributeHandler(final AttributeDefinition... definitions) {
        assert definitions != null : MESSAGES.nullVar("definitions").getLocalizedMessage();
        attributeDefinitions = new HashMap<String, AttributeDefinition>();
        for (AttributeDefinition def : definitions) {
            attributeDefinitions.put(def.getName(), def);
        }
        this.unresolvedValueValidator = null;
        this.resolvedValueValidator = null;
    }

    protected AbstractWriteAttributeHandler(final ParameterValidator unresolvedValidator, final ParameterValidator resolvedValidator) {

        this.nameValidator.registerValidator(NAME, new StringLengthValidator(1));
        this.unresolvedValueValidator = unresolvedValidator;
        this.resolvedValueValidator = resolvedValidator;
        this.attributeDefinitions = null;
    }

    @Override
    public void execute(OperationContext context, ModelNode operation) throws OperationFailedException {

        nameValidator.validate(operation);
        final String attributeName = operation.require(NAME).asString();
        // Don't require VALUE. Let the validator decide if it's bothered by an undefined value
        final ModelNode newValue = operation.hasDefined(VALUE) ? operation.get(VALUE) : new ModelNode();
        validateUnresolvedValue(attributeName, newValue);
        final ModelNode submodel = context.readResourceForUpdate(PathAddress.EMPTY_ADDRESS).getModel();
        final ModelNode currentValue = submodel.get(attributeName).clone();

        final AttributeDefinition attributeDefinition = getAttributeDefinition(attributeName);
        if (attributeDefinition != null) {
            final ModelNode syntheticOp = new ModelNode();
            syntheticOp.get(attributeName).set(newValue);
            attributeDefinition.validateAndSet(syntheticOp, submodel);
        } else {
            submodel.get(attributeName).set(newValue);
        }

        if (requiresRuntime(context)) {
            context.addStep(new OperationStepHandler() {
                @Override
                public void execute(OperationContext context, ModelNode operation) throws OperationFailedException {
                    ModelNode resolvedValue = attributeDefinition != null ? attributeDefinition.resolveModelAttribute(context, submodel) : newValue.resolve();
                    validateResolvedValue(attributeName, newValue);
                    HandbackHolder<T> handback = new HandbackHolder<T>();
                    boolean restartRequired = applyUpdateToRuntime(context, operation, attributeName, resolvedValue, currentValue, handback);
                    if (restartRequired) {
                        context.reloadRequired();
                    }

                    if (context.completeStep() != OperationContext.ResultAction.KEEP) {
                        ModelNode valueToRestore = currentValue.resolve();
                        try {
                            revertUpdateToRuntime(context, operation, attributeName, valueToRestore, resolvedValue, handback.handback);
                        } catch (Exception e) {
                            MGMT_OP_LOGGER.errorRevertingOperation(e, getClass().getSimpleName(),
                                    operation.require(ModelDescriptionConstants.OP).asString(),
                                    PathAddress.pathAddress(operation.get(ModelDescriptionConstants.OP_ADDR)));
                        }
                        if (restartRequired) {
                            context.revertReloadRequired();
                        }
                    }
                }
            }, OperationContext.Stage.RUNTIME);
        }

        context.completeStep();
    }



    /**
     * Hook to allow subclasses to make runtime changes to effect the attribute value change.
     *
     * @param context the context of the operation
     * @param operation the operation
     * @param attributeName the name of the attribute being modified
     * @param resolvedValue the new value for the attribute, after {@link ModelNode#resolve()} has been called on it
     * @param currentValue the existing value for the attribute
     * @param handbackHolder holder for an arbitrary object to pass to
     *             {@link #revertUpdateToRuntime(OperationContext, ModelNode, String, ModelNode, ModelNode, Object)} if
     *             the operation needs to be rolled back
     *
     * @return {@code true} if the server requires restart to effect the attribute
     *         value change; {@code false} if not
     */
    protected abstract boolean applyUpdateToRuntime(OperationContext context, ModelNode operation, String attributeName,
                                                    ModelNode resolvedValue, ModelNode currentValue, HandbackHolder<T> handbackHolder)
            throws OperationFailedException;

    /**
     * Hook to allow subclasses to revert runtime changes made in
     * {@link #applyUpdateToRuntime(OperationContext, ModelNode, String, ModelNode, ModelNode, HandbackHolder)}.
     *
     * @param context the context of the operation
     * @param operation the operation
     * @param attributeName the name of the attribute being modified
     * @param valueToRestore the previous value for the attribute, before this operation was executed
     * @param valueToRevert the new value for the attribute that should be reverted
     * @param handback an object, if any, passed in to the {@code handbackHolder} by the {@code applyUpdateToRuntime}
     *                 implementation
     */
    protected abstract void revertUpdateToRuntime(OperationContext context, ModelNode operation, String attributeName,
                                                  ModelNode valueToRestore, ModelNode valueToRevert, T handback)
            throws OperationFailedException;

    /**
     * If an unresolved value validator was passed to the constructor, uses it to validate the value.
     * Subclasses can alter this behavior.
     *
     * @param attributeName the name of the attribute being updated
     * @param unresolvedValue the unresolved value
     */
    protected void validateUnresolvedValue(final String attributeName, final ModelNode unresolvedValue) throws OperationFailedException {
        if (unresolvedValueValidator != null) {
            unresolvedValueValidator.validateParameter(VALUE, unresolvedValue);
        }
    }

    /**
     * If a resolved value validator was passed to the constructor, uses it to validate the value.
     * Subclasses can alter this behavior.
     *
     * @param attributeName the name of the attribute being updated
     * @param resolvedValue the resolved value
     */
    protected void validateResolvedValue(final String attributeName, final ModelNode resolvedValue) throws OperationFailedException {
        if (resolvedValueValidator != null) {
            resolvedValueValidator.validateResolvedParameter(VALUE, resolvedValue);
        }
    }


    /**
     * Gets whether a {@link OperationContext.Stage#RUNTIME} handler should be added. This default implementation
     * returns {@code true} if the {@link OperationContext#getType() context type} is {@link OperationContext.Type#SERVER}
     * and {@link OperationContext#isBooting() context.isBooting()} returns {@code false}.
     *
     * @param context operation context
     * @return {@code true} if a runtime stage handler should be added; {@code false} otherwise.
     */
    protected boolean requiresRuntime(OperationContext context) {
        return context.getType() == OperationContext.Type.SERVER && !context.isBooting();
    }

    protected AttributeDefinition getAttributeDefinition(final String attributeName) {
        return attributeDefinitions == null ? null : attributeDefinitions.get(attributeName);
    }

    /**
     * Holder subclasses can use to pass an object between
     * {@link AbstractWriteAttributeHandler#applyUpdateToRuntime(OperationContext, ModelNode, String, ModelNode, ModelNode, HandbackHolder)}
     * and {@link AbstractWriteAttributeHandler#revertUpdateToRuntime(OperationContext, ModelNode, String, ModelNode, ModelNode, Object)}.
     * Typically that object would encapsulate some state useful in reverting the runtime update.
     *
     * @param <T> the type of the object being passed
     */
    public static class HandbackHolder<T> {
        private T handback;

        /**
         * Store an object for use in
         * {@link AbstractWriteAttributeHandler#revertUpdateToRuntime(OperationContext, ModelNode, String, ModelNode, ModelNode, Object)}.
         *
         * @param handback the object
         */
        public void setHandback(final T handback) {
            this.handback = handback;
        }

    }
}
