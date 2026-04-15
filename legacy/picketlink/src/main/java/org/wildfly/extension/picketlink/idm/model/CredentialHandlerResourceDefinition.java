/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.extension.picketlink.idm.model;

import org.jboss.as.controller.ModuleIdentifierUtil;
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

/**
 * @author <a href="mailto:psilva@redhat.com">Pedro Silva</a>
 * @since Mar 16, 2012
 */
public class CredentialHandlerResourceDefinition extends AbstractIDMResourceDefinition {

    public static final SimpleAttributeDefinition CLASS_NAME = new SimpleAttributeDefinitionBuilder(ModelElement.COMMON_CLASS_NAME.getName(), ModelType.STRING, true)
       .setAllowExpression(true)
       .setAlternatives(ModelElement.COMMON_CODE.getName())
       .build();
    public static final SimpleAttributeDefinition CODE = new SimpleAttributeDefinitionBuilder(ModelElement.COMMON_CODE.getName(), ModelType.STRING, true)
        .setValidator(EnumValidator.create(CredentialTypeEnum.class))
        .setAllowExpression(true)
        .setAlternatives(ModelElement.COMMON_CLASS_NAME.getName())
        .build();
    public static final SimpleAttributeDefinition MODULE = new SimpleAttributeDefinitionBuilder(ModelElement.COMMON_MODULE.getName(), ModelType.STRING, true)
        .setAllowExpression(true)
        .setRequires(ModelElement.COMMON_CLASS_NAME.getName())
        .setCorrector(ModuleIdentifierUtil.MODULE_NAME_CORRECTOR)
        .build();
    public static final CredentialHandlerResourceDefinition INSTANCE = new CredentialHandlerResourceDefinition(CLASS_NAME, CODE, MODULE);

    private CredentialHandlerResourceDefinition(SimpleAttributeDefinition... attributes) {
        super(ModelElement.IDENTITY_STORE_CREDENTIAL_HANDLER, getModelValidators(), address -> address.getParent().getParent().getParent(),  attributes);
    }

    private static ModelValidationStepHandler[] getModelValidators() {
        return new ModelValidationStepHandler[] {
            new UniqueTypeValidationStepHandler(ModelElement.IDENTITY_STORE_CREDENTIAL_HANDLER) {
                @Override
                protected String getType(OperationContext context, ModelNode model) throws OperationFailedException {
                    return getCredentialType(context, model);
                }
            }
        };
    }

    private static String getCredentialType(OperationContext context, ModelNode elementNode) throws OperationFailedException {
        ModelNode classNameNode = CLASS_NAME.resolveModelAttribute(context, elementNode);
        ModelNode codeNode = CODE.resolveModelAttribute(context, elementNode);

        if (classNameNode.isDefined()) {
            return classNameNode.asString();
        }

        return CredentialTypeEnum.forType(codeNode.asString());
    }
}
