/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2013, Red Hat, Inc., and individual contributors
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

package org.wildfly.extension.picketlink.idm.model;

import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.SimpleAttributeDefinition;
import org.jboss.as.controller.SimpleAttributeDefinitionBuilder;
import org.jboss.as.controller.operations.validation.EnumValidator;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;
import org.wildfly.extension.picketlink.common.model.ModelElement;
import org.wildfly.extension.picketlink.common.model.validator.ModelValidationStepHandler;
import org.wildfly.extension.picketlink.common.model.validator.UniqueTypeValidationStepHandler;

import static org.wildfly.extension.picketlink.logging.PicketLinkLogger.ROOT_LOGGER;

/**
 * @author <a href="mailto:psilva@redhat.com">Pedro Silva</a>
 * @since Mar 16, 2012
 */
public class SupportedTypeResourceDefinition extends AbstractIDMResourceDefinition {

    public static final SimpleAttributeDefinition CLASS_NAME = new SimpleAttributeDefinitionBuilder(ModelElement.COMMON_CLASS_NAME.getName(), ModelType.STRING, true)
        .setAllowExpression(true)
        .setAlternatives(ModelElement.COMMON_CODE.getName())
        .build();
    public static final SimpleAttributeDefinition CODE = new SimpleAttributeDefinitionBuilder(ModelElement.COMMON_CODE.getName(), ModelType.STRING, true)
        .setValidator(new EnumValidator<AttributedTypeEnum>(AttributedTypeEnum.class, true, true))
        .setAllowExpression(true)
        .setAlternatives(ModelElement.COMMON_CLASS_NAME.getName())
        .build();
    public static final SimpleAttributeDefinition MODULE = new SimpleAttributeDefinitionBuilder(ModelElement.COMMON_MODULE.getName(), ModelType.STRING, true)
        .setAllowExpression(true)
        .setRequires(ModelElement.COMMON_CLASS_NAME.getName())
        .build();
    public static final SupportedTypeResourceDefinition INSTANCE = new SupportedTypeResourceDefinition(CLASS_NAME, CODE, MODULE);

    private SupportedTypeResourceDefinition(SimpleAttributeDefinition... attributes) {
        super(ModelElement.SUPPORTED_TYPE, new IDMConfigAddStepHandler(
            getModelValidators(), attributes), attributes);
    }

    private static ModelValidationStepHandler[] getModelValidators() {
        return new ModelValidationStepHandler[] {
            new UniqueTypeValidationStepHandler(ModelElement.SUPPORTED_TYPE) {
                @Override
                protected String getType(OperationContext context, ModelNode model) throws OperationFailedException {
                    return getSupportedType(context, model);
                }
            }
        };
    }

    @Override
    protected void doRegisterModelWriteAttributeHandler(OperationContext context, ModelNode operation) {
        for (ModelValidationStepHandler validator : getModelValidators()) {
            context.addStep(validator, OperationContext.Stage.MODEL);
        }
    }

    private static String getSupportedType(OperationContext context, ModelNode elementNode) throws OperationFailedException {
        ModelNode classNameNode = CLASS_NAME.resolveModelAttribute(context, elementNode);
        ModelNode codeNode = CODE.resolveModelAttribute(context, elementNode);

        if (classNameNode.isDefined()) {
            return classNameNode.asString();
        } else if (codeNode.isDefined()) {
            return AttributedTypeEnum.forType(codeNode.asString());
        } else {
            throw ROOT_LOGGER.idmNoSupportedTypesDefined();
        }
    }
}
