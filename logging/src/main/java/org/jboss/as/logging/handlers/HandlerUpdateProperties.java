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

package org.jboss.as.logging.handlers;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;
import static org.jboss.as.logging.CommonAttributes.ENCODING;
import static org.jboss.as.logging.CommonAttributes.FILTER;
import static org.jboss.as.logging.CommonAttributes.FORMATTER;
import static org.jboss.as.logging.CommonAttributes.LEVEL;
import static org.jboss.as.logging.LoggingLogger.ROOT_LOGGER;
import static org.jboss.as.logging.LoggingMessages.MESSAGES;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Handler;

import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.OperationStepHandler;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.controller.registry.Resource;
import org.jboss.as.logging.util.LogServices;
import org.jboss.as.logging.util.ModelParser;
import org.jboss.dmr.ModelNode;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceRegistry;

/**
 * Parent operation responsible for updating the common attributes of logging handlers.
 *
 * @author John Bailey
 * @author <a href="mailto:jperkins@redhat.com">James R. Perkins</a>
 */
public abstract class HandlerUpdateProperties<T extends Handler> implements OperationStepHandler {
    public static final String OPERATION_NAME = "update-properties";

    private final Set<String> attributes;
    private final List<AttributeDefinition> attributeDefinitions;

    protected HandlerUpdateProperties(final AttributeDefinition... attributeDefinitions) {
        this.attributes = Collections.emptySet();
        this.attributes.addAll(attributes);
        this.attributeDefinitions = new ArrayList<AttributeDefinition>();
        this.attributeDefinitions.add(ENCODING);
        this.attributeDefinitions.add(FORMATTER);
        this.attributeDefinitions.add(LEVEL);
        this.attributeDefinitions.add(FILTER);
        Collections.addAll(this.attributeDefinitions, attributeDefinitions);
    }

    protected HandlerUpdateProperties(final String... attributes) {
        this.attributes = new HashSet<String>();
        Collections.addAll(this.attributes, attributes);
        this.attributeDefinitions = Collections.emptyList();
    }

    /**
     * {@inheritDoc
     */
    @Override
    public void execute(final OperationContext context, final ModelNode operation) throws OperationFailedException {
        final Resource resource = context.readResourceForUpdate(PathAddress.EMPTY_ADDRESS);
        final ModelNode originalModel = resource.getModel().clone();
        final ModelNode model = resource.getModel();
        updateModel(operation, model);

        if (requiresRuntime(context)) {
            context.addStep(new OperationStepHandler() {
                public void execute(OperationContext context, ModelNode operation) throws OperationFailedException {
                    final PathAddress address = PathAddress.pathAddress(operation.require(OP_ADDR));
                    final String name = address.getLastElement().getValue();
                    final ServiceRegistry serviceRegistry = context.getServiceRegistry(false);
                    @SuppressWarnings("unchecked")
                    final ServiceController<T> controller = (ServiceController<T>) serviceRegistry.getService(LogServices.handlerName(name));
                    if (controller != null) {
                        final T handler = controller.getValue();
                        final ModelNode level = LEVEL.resolveModelAttribute(context, model);
                        final ModelNode formatter = FORMATTER.resolveModelAttribute(context, model);
                        final ModelNode encoding = ENCODING.resolveModelAttribute(context, model);
                        final ModelNode filter = FILTER.resolveModelAttribute(context, model);

                        if (level.isDefined()) {
                            handler.setLevel(ModelParser.parseLevel(level));
                        }

                        if (formatter.isDefined()) {
                            FormatterSpec.fromModelNode(context, model).apply(handler);
                        }

                        if (encoding.isDefined()) {
                            try {
                                handler.setEncoding(encoding.asString());
                            } catch (UnsupportedEncodingException e) {
                                throw new OperationFailedException(e, new ModelNode().set(MESSAGES.failedToSetHandlerEncoding()));
                            }
                        }

                        if (filter.isDefined()) {
                            handler.setFilter(ModelParser.parseFilter(context, filter));
                        }

                        final boolean restartRequired = applyUpdateToRuntime(context, name, model, originalModel, handler);
                        // Copy the original defined values to the new model if the values are not defined.
                        copyOriginal(originalModel, model);

                        if (restartRequired) {
                            context.reloadRequired();
                        }

                        if (context.completeStep() != OperationContext.ResultAction.KEEP) {
                            try {
                                revertUpdateToRuntime(context, name, model, originalModel, handler);
                            } catch (Exception e) {
                                ROOT_LOGGER.errorRevertingOperation(e, getClass().getSimpleName(),
                                        operation.require(ModelDescriptionConstants.OP).asString(),
                                        PathAddress.pathAddress(operation.require(ModelDescriptionConstants.OP_ADDR)));
                            }
                            if (restartRequired) {
                                context.revertReloadRequired();
                            }
                        }
                    }

                }
            }, OperationContext.Stage.RUNTIME);
        }
        context.completeStep();
    }

    /**
     * Update the given node in the persistent configuration model based on the values in the given operation.
     *
     * @param operation the operation
     * @param model     persistent configuration model node that corresponds to the address of {@code operation}
     *
     * @throws OperationFailedException if {@code operation} is invalid or populating the model otherwise fails
     */
    protected final void updateModel(final ModelNode operation, final ModelNode model) throws OperationFailedException {
        for (AttributeDefinition attr : attributeDefinitions) {
            attr.validateAndSet(operation, model);
        }
        for (String attr : attributes) {
            copy(attr, operation, model);
        }
    }

    /**
     * Hook to allow subclasses to make runtime changes to effect the attribute value change.
     *
     * @param context       the context of the operation
     * @param model         the model
     * @param originalModel the original model
     * @param handler       the log handler
     *
     * @return {@code true} if the server requires restart to effect the attribute
     *         value change; {@code false} if not
     */
    protected abstract boolean applyUpdateToRuntime(OperationContext context, String handlerName, ModelNode model, ModelNode originalModel, T handler) throws OperationFailedException;

    /**
     * Hook to allow subclasses to revert runtime changes made in
     * {@link #applyUpdateToRuntime(OperationContext, String, ModelNode, ModelNode, T)}.
     *
     * @param context       the context of the operation
     * @param model         the model
     * @param originalModel the original model
     * @param handler       the log handler
     */
    protected abstract void revertUpdateToRuntime(OperationContext context, String handlerName, ModelNode model, ModelNode originalModel, T handler)
            throws OperationFailedException;


    /**
     * Gets whether a {@link OperationContext.Stage#RUNTIME} handler should be added.
     *
     * @param context operation context
     *
     * @return {@code true} if a runtime stage handler should be added; {@code false} otherwise.
     */
    protected boolean requiresRuntime(OperationContext context) {
        return context.isNormalServer() && !context.isBooting();
    }

    /**
     * Copies the attribute, represented by the {@code name} parameter, from one {@link ModelNode} to another if the
     * {@link ModelNode from} parameter has the attributed defined. If the attribute was not defined, nothing happens.
     *
     * @param name the name of the attribute to copy.
     * @param from the model node to copy the value from.
     * @param to   the model node to copy the value to.
     */
    protected void copy(final String name, final ModelNode from, final ModelNode to) {
        if (from.hasDefined(name)) {
            to.get(name).set(from.get(name));
        }
    }

    /**
     * Copies the original defined values that are not defined in the model to the model.
     *
     * @param originalModel the original model.
     * @param model         the new model.
     */
    private void copyOriginal(final ModelNode originalModel, final ModelNode model) {
        for (String key : originalModel.keys()) {
            if (originalModel.hasDefined(key) && !model.hasDefined(key)) {
                model.get(key).set(originalModel.get(key));
            }
        }
    }

    /**
     * Returns a collection of attributes used for the write attribute.
     *
     * @return a collection of attributes.
     */
    public final Collection<AttributeDefinition> getAttributes() {
        return Collections.unmodifiableCollection(attributeDefinitions);
    }
}
