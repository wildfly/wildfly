/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.extension.picketlink.federation.model.handlers;

import static org.wildfly.extension.picketlink.logging.PicketLinkLogger.ROOT_LOGGER;

import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.ModelOnlyWriteAttributeHandler;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.OperationStepHandler;
import org.jboss.as.controller.SimpleAttributeDefinition;
import org.jboss.as.controller.SimpleAttributeDefinitionBuilder;
import org.jboss.as.controller.operations.validation.EnumValidator;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;
import org.wildfly.extension.picketlink.common.model.ModelElement;
import org.wildfly.extension.picketlink.common.model.validator.UniqueTypeValidationStepHandler;
import org.wildfly.extension.picketlink.federation.model.AbstractFederationResourceDefinition;

/**
 * @author <a href="mailto:psilva@redhat.com">Pedro Silva</a>
 * @since Mar 16, 2012
 */
public class HandlerResourceDefinition extends AbstractFederationResourceDefinition {

    public static final SimpleAttributeDefinition CLASS_NAME = new SimpleAttributeDefinitionBuilder(ModelElement.COMMON_CLASS_NAME.getName(), ModelType.STRING, true)
        .setAllowExpression(true)
        .setAlternatives(ModelElement.COMMON_CODE.getName())
        .build();

    public static final SimpleAttributeDefinition CODE = new SimpleAttributeDefinitionBuilder(ModelElement.COMMON_CODE.getName(), ModelType.STRING, true)
        .setValidator(EnumValidator.create(HandlerTypeEnum.class))
        .setAllowExpression(true)
        .setAlternatives(ModelElement.COMMON_CLASS_NAME.getName())
        .build();

    public static final HandlerResourceDefinition INSTANCE = new HandlerResourceDefinition();

    private HandlerResourceDefinition() {
        super(ModelElement.COMMON_HANDLER, HandlerAddHandler.INSTANCE, CLASS_NAME, CODE);
    }

    @Override
    public void registerChildren(ManagementResourceRegistration resourceRegistration) {
        addChildResourceDefinition(HandlerParameterResourceDefinition.INSTANCE, resourceRegistration);
    }

    public static String getHandlerType(OperationContext context, ModelNode elementNode) throws OperationFailedException {
        ModelNode classNameNode = CLASS_NAME.resolveModelAttribute(context, elementNode);
        ModelNode codeNode = CODE.resolveModelAttribute(context, elementNode);

        if (classNameNode.isDefined()) {
            return classNameNode.asString();
        } else if (codeNode.isDefined()) {
            return HandlerTypeEnum.forType(codeNode.asString());
        } else {
            throw ROOT_LOGGER.federationHandlerTypeNotProvided();
        }
    }

    @Override
    protected OperationStepHandler createAttributeWriterHandler() {
        return new ModelOnlyWriteAttributeHandler(getAttributes().toArray(new AttributeDefinition[0])) {
                @Override
                public void execute(OperationContext context, ModelNode operation) throws OperationFailedException {
                    context.addStep(new UniqueTypeValidationStepHandler(ModelElement.COMMON_HANDLER) {
                        @Override
                        protected String getType(OperationContext context, ModelNode model) throws OperationFailedException {
                        return getHandlerType(context, model);
                    }
                }, OperationContext.Stage.MODEL);
                super.execute(context, operation);
            }
        };
    }
}
