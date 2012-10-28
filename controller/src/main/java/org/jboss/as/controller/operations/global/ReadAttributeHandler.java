/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2012, Red Hat, Inc., and individual contributors
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

package org.jboss.as.controller.operations.global;

import static org.jboss.as.controller.ControllerMessages.MESSAGES;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ATTRIBUTES;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.DEFAULT;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.READ_ATTRIBUTE_OPERATION;

import java.util.Locale;
import java.util.Set;

import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationDefinition;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.OperationStepHandler;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.SimpleOperationDefinitionBuilder;
import org.jboss.as.controller.descriptions.DescriptionProvider;
import org.jboss.as.controller.descriptions.common.ControllerResolver;
import org.jboss.as.controller.operations.validation.ModelTypeValidator;
import org.jboss.as.controller.operations.validation.ParametersValidator;
import org.jboss.as.controller.operations.validation.StringLengthValidator;
import org.jboss.as.controller.registry.AttributeAccess;
import org.jboss.as.controller.registry.ImmutableManagementResourceRegistration;
import org.jboss.as.controller.registry.Resource;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;

/**
 * {@link org.jboss.as.controller.OperationStepHandler} reading a single attribute at the given operation address.
 * The required request parameter "name" represents the attribute name.
 *
 * @author <a href="kabir.khan@jboss.com">Kabir Khan</a>
 */
public class ReadAttributeHandler extends GlobalOperationHandlers.AbstractMultiTargetHandler implements OperationStepHandler {

    public static final OperationDefinition DEFINITION = new SimpleOperationDefinitionBuilder(READ_ATTRIBUTE_OPERATION, ControllerResolver.getResolver("global"))
            .setParameters(GlobalOperationHandlers.NAME, GlobalOperationHandlers.INCLUDE_DEFAULTS)
            .setReadOnly()
            .setRuntimeOnly()
            .setReplyType(ModelType.OBJECT)
            .build();

    public static final OperationStepHandler INSTANCE = new ReadAttributeHandler();

    private ParametersValidator validator = new ParametersValidator();

    public ReadAttributeHandler() {
        validator.registerValidator(GlobalOperationHandlers.NAME.getName(), new StringLengthValidator(1));
        validator.registerValidator(GlobalOperationHandlers.INCLUDE_DEFAULTS.getName(), new ModelTypeValidator(ModelType.BOOLEAN, true));
    }

    @Override
    void doExecute(OperationContext context, ModelNode operation) throws OperationFailedException {
        validator.validate(operation);
        final String attributeName = operation.require(GlobalOperationHandlers.NAME.getName()).asString();
        final boolean defaults = operation.get(GlobalOperationHandlers.INCLUDE_DEFAULTS.getName()).asBoolean(true);

        final ImmutableManagementResourceRegistration registry = context.getResourceRegistration();
        final AttributeAccess attributeAccess = registry.getAttributeAccess(PathAddress.EMPTY_ADDRESS, attributeName);


        if (attributeAccess == null) {
            final Set<String> children = context.getResourceRegistration().getChildNames(PathAddress.EMPTY_ADDRESS);
            if (children.contains(attributeName)) {
                throw new OperationFailedException(new ModelNode().set(MESSAGES.attributeRegisteredOnResource(attributeName, operation.get(OP_ADDR))));
            } else {
                final Resource resource = context.readResource(PathAddress.EMPTY_ADDRESS, false);
                final ModelNode subModel = resource.getModel();
                if (subModel.hasDefined(attributeName)) {
                    final ModelNode result = subModel.get(attributeName);
                    context.getResult().set(result);
                } else {
                    // No defined value in the model. See if we should reply with a default from the metadata,
                    // reply with undefined, or fail because it's a non-existent attribute name
                    final ModelNode nodeDescription = getNodeDescription(registry, context, operation);
                    if (defaults && nodeDescription.get(ATTRIBUTES).hasDefined(attributeName) &&
                            nodeDescription.get(ATTRIBUTES, attributeName).hasDefined(DEFAULT)) {
                        final ModelNode result = nodeDescription.get(ATTRIBUTES, attributeName, DEFAULT);
                        context.getResult().set(result);
                    } else if (subModel.has(attributeName) || nodeDescription.get(ATTRIBUTES).has(attributeName)) {
                        // model had no defined value, but we treat its existence in the model or the metadata
                        // as proof that it's a legit attribute name
                        context.getResult(); // this initializes the "result" to ModelType.UNDEFINED
                    } else {
                        throw new OperationFailedException(new ModelNode().set(MESSAGES.unknownAttribute(attributeName)));
                    }
                }
            }
            // Complete the step for the unregistered attribute case
            context.stepCompleted();
        } else if (attributeAccess.getReadHandler() == null) {
            final Resource resource = context.readResource(PathAddress.EMPTY_ADDRESS, false);
            final ModelNode subModel = resource.getModel();
            // We know the attribute name is legit as it's in the registry, so this case is simpler
            if (subModel.hasDefined(attributeName) || !defaults) {
                final ModelNode result = subModel.get(attributeName);
                context.getResult().set(result);
            } else {
                // It wasn't in the model, but user wants a default value from metadata if there is one
                final ModelNode nodeDescription = getNodeDescription(registry, context, operation);
                if (nodeDescription.get(ATTRIBUTES).hasDefined(attributeName) &&
                        nodeDescription.get(ATTRIBUTES, attributeName).hasDefined(DEFAULT)) {
                    final ModelNode result = nodeDescription.get(ATTRIBUTES, attributeName, DEFAULT);
                    context.getResult().set(result);
                } else {
                    context.getResult(); // this initializes the "result" to ModelType.UNDEFINED
                }
            }
            // Complete the step for the "registered attribute but default read handler" case
            context.stepCompleted();
        } else {
            OperationStepHandler handler = attributeAccess.getReadHandler();
            ClassLoader oldTccl = SecurityActions.setThreadContextClassLoader(handler.getClass());
            try {
                handler.execute(context, operation);
            } finally {
                SecurityActions.setThreadContextClassLoader(oldTccl);
            }
            // no context.completeStep() here as that's the read handler's job
        }
    }

    private ModelNode getNodeDescription(ImmutableManagementResourceRegistration registry, OperationContext context, ModelNode operation) throws OperationFailedException {
        final DescriptionProvider descriptionProvider = registry.getModelDescription(PathAddress.EMPTY_ADDRESS);
        final Locale locale = GlobalOperationHandlers.getLocale(context, operation);
        return descriptionProvider.getModelDescription(locale);
    }

}
