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

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.controller.operations.validation.ParametersValidator;
import org.jboss.as.controller.registry.AttributeAccess;
import org.jboss.as.controller.registry.Resource;
import org.jboss.dmr.ModelNode;

/**
 * Abstract handler for the write aspect of a
 * {@link org.jboss.as.controller.registry.ManagementResourceRegistration#registerReadWriteAttribute(AttributeDefinition, OperationStepHandler, OperationStepHandler) read-write attribute}.
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

    private final Map<String, AttributeDefinition> attributeDefinitions;

    protected AbstractWriteAttributeHandler(final AttributeDefinition... definitions) {
        assert definitions != null : MESSAGES.nullVar("definitions").getLocalizedMessage();
        attributeDefinitions = new HashMap<String, AttributeDefinition>();
        for (AttributeDefinition def : definitions) {
            attributeDefinitions.put(def.getName(), def);
        }

    }

    protected AbstractWriteAttributeHandler(final Collection<AttributeDefinition> definitions) {
        this(definitions.toArray(new AttributeDefinition[definitions.size()]));
    }

    @Override
    public void execute(OperationContext context, ModelNode operation) throws OperationFailedException {

        nameValidator.validate(operation);
        final String attributeName = operation.require(NAME).asString();
        // Don't require VALUE. Let the validator decide if it's bothered by an undefined value
        ModelNode newValue = operation.hasDefined(VALUE) ? operation.get(VALUE) : new ModelNode();
        validateUnresolvedValue(attributeName, newValue);
        final Resource resource = context.readResourceForUpdate(PathAddress.EMPTY_ADDRESS);
        final ModelNode submodel = resource.getModel();
        final ModelNode currentValue = submodel.get(attributeName).clone();

        final AttributeDefinition attributeDefinition = getAttributeDefinition(attributeName);
        final ModelNode defaultValue;
        if (attributeDefinition != null) {
            defaultValue = attributeDefinition.getDefaultValue();
            final ModelNode syntheticOp = new ModelNode();
            syntheticOp.get(attributeName).set(newValue);
            attributeDefinition.validateAndSet(syntheticOp, submodel);
            newValue = submodel.get(attributeName);
        } else {
            defaultValue = null;
            submodel.get(attributeName).set(newValue);
        }

        finishModelStage(context, operation, attributeName, newValue, currentValue, resource);

        if (requiresRuntime(context)) {
            final ModelNode updatedValue = newValue;
            context.addStep(new OperationStepHandler() {
                @Override
                public void execute(OperationContext context, ModelNode operation) throws OperationFailedException {
                    final ModelNode resolvedValue = attributeDefinition != null ? attributeDefinition.resolveModelAttribute(context, submodel) : updatedValue.resolve();
                    validateResolvedValue(attributeName, updatedValue);
                    final HandbackHolder<T> handback = new HandbackHolder<T>();
                    final boolean reloadRequired = applyUpdateToRuntime(context, operation, attributeName, resolvedValue, currentValue, handback);
                    if (reloadRequired) {
                        if (attributeDefinition != null && attributeDefinition.getFlags().contains(AttributeAccess.Flag.RESTART_JVM)){
                            context.restartRequired();
                        }else{
                            context.reloadRequired();
                        }
                    }

                    context.completeStep(new OperationContext.RollbackHandler() {
                        @Override
                        public void handleRollback(OperationContext context, ModelNode operation) {
                            ModelNode valueToRestore = currentValue.resolve();
                            if (valueToRestore.isDefined() == false && defaultValue != null) {
                                valueToRestore = defaultValue;
                            }
                            try {
                                revertUpdateToRuntime(context, operation, attributeName, valueToRestore, resolvedValue, handback.handback);
                            } catch (Exception e) {
                                MGMT_OP_LOGGER.errorRevertingOperation(e, getClass().getSimpleName(),
                                        operation.require(ModelDescriptionConstants.OP).asString(),
                                        PathAddress.pathAddress(operation.get(ModelDescriptionConstants.OP_ADDR)));
                            }
                            if (reloadRequired) {
                                if (attributeDefinition != null && attributeDefinition.getFlags().contains(AttributeAccess.Flag.RESTART_JVM)) {
                                    context.revertRestartRequired();
                                } else {
                                    context.revertReloadRequired();
                                }

                            }
                        }
                    });
                }
            }, OperationContext.Stage.RUNTIME);
        }

        context.stepCompleted();
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
     * @return {@code true} if the server requires reload to effect the attribute
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
     * Does nothing. Subclasses can alter this behavior.
     *
     * @param attributeName the name of the attribute being updated
     * @param unresolvedValue the unresolved value
     *
     * @deprecated the {@link AttributeDefinition} provided to the constructor will validate
     */
    @Deprecated
    protected void validateUnresolvedValue(final String attributeName, final ModelNode unresolvedValue) throws OperationFailedException {
        // TODO remove
    }

    /**
     * Does nothing. Subclasses can alter this behavior.
     *
     * @param attributeName the name of the attribute being updated
     * @param resolvedValue the resolved value
     * @deprecated the {@link AttributeDefinition} provided to the constructor will validate
     */
    @Deprecated
    protected void validateResolvedValue(final String attributeName, final ModelNode resolvedValue) throws OperationFailedException {
        // TODO remove
    }

    /**
     * Hook to allow subclasses to do any final {@link OperationContext.Stage#MODEL} processing following the
     * application of the new attribute value. This default implementation calls
     * {@link #validateUpdatedModel(OperationContext, Resource)}.
     * <p>
     * <strong>NOTE:</strong> Implementations must not call
     * {@link OperationContext#completeStep(OperationContext.ResultHandler)} or any of its variants. The method that
     * calls this one handles step completion.
     * </p>
     *
     *
     * @param context the operation context
     * @param operation the operation
     * @param attributeName the name of the attribute being modified
     * @param newValue the new value for the attribute
     * @param oldValue the previous value for the attribute
     * @param model the updated model resource
     * @throws OperationFailedException
     */
    protected void finishModelStage(final OperationContext context, final ModelNode operation, String attributeName,
                                    ModelNode newValue, ModelNode oldValue, final Resource model) throws OperationFailedException {
        validateUpdatedModel(context, model);
    }

    /**
     * Hook to allow subclasses to validate the model following the application of the new attribute value.
     * This default implementation does nothing.
     *
     * @param context the operation context
     * @param model the updated model resource
     * @throws OperationFailedException
     */
    protected void validateUpdatedModel(final OperationContext context, final Resource model) throws OperationFailedException {
        // default impl does nothing
    }


    /**
     * Gets whether a {@link OperationContext.Stage#RUNTIME} handler should be added. This default implementation
     * returns true if the process is a {@link OperationContext#isNormalServer() normal server} and the process
     * is not {@link OperationContext#isBooting() booting}. The rationale for the latter check is if the process is
     * booting, the resource being modified will have been added as a previous step in the same context, and
     * the Stage.RUNTIME handling for that add will see a model the reflects the changes made by this handler and
     * will apply them to the runtime.
     *
     * @param context operation context
     * @return {@code true} if a runtime stage handler should be added; {@code false} otherwise.
     */
    protected boolean requiresRuntime(OperationContext context) {
        return context.isNormalServer() && !context.isBooting();
    }

    /**
     * Gets the {@link AttributeDefinition} provided to the constructor (if present) whose
     * {@link AttributeDefinition#getName() name} matches the given {@code attributeName}.
     *
     * @param attributeName the attribute name
     * @return the attribute definition, or {@code null} if no matching definition is found
     */
    protected AttributeDefinition getAttributeDefinition(final String attributeName) {
        return attributeDefinitions == null ? null : attributeDefinitions.get(attributeName);
    }

    /**
     * Holder subclasses can use to pass an object between
     * {@link AbstractWriteAttributeHandler#applyUpdateToRuntime(OperationContext, ModelNode, String, ModelNode, ModelNode, HandbackHolder)}
     * and {@link AbstractWriteAttributeHandler#revertUpdateToRuntime(OperationContext, ModelNode, String, ModelNode, ModelNode, Object)}.
     * Typically that object would encapsulate some data useful in reverting the runtime update.
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
