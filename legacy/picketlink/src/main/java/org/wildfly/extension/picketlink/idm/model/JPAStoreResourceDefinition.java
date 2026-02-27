/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.extension.picketlink.idm.model;

import org.jboss.as.controller.ModuleIdentifierUtil;
import org.jboss.as.controller.SimpleAttributeDefinition;
import org.jboss.as.controller.SimpleAttributeDefinitionBuilder;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;
import org.wildfly.extension.picketlink.common.model.ModelElement;
import org.wildfly.extension.picketlink.common.model.validator.ModelValidationStepHandler;
import org.wildfly.extension.picketlink.common.model.validator.NotEmptyResourceValidationStepHandler;
import org.wildfly.extension.picketlink.common.model.validator.RequiredChildValidationStepHandler;

/**
 * @author <a href="mailto:psilva@redhat.com">Pedro Silva</a>
 * @since Mar 16, 2012
 */
public class JPAStoreResourceDefinition extends AbstractIdentityStoreResourceDefinition {

    public static final SimpleAttributeDefinition DATA_SOURCE = new SimpleAttributeDefinitionBuilder(ModelElement.JPA_STORE_DATASOURCE.getName(), ModelType.STRING, true)
        .setAllowExpression(true)
        .setAlternatives(
            ModelElement.JPA_STORE_ENTITY_MANAGER_FACTORY.getName(),
            ModelElement.JPA_STORE_ENTITY_MODULE_UNIT_NAME.getName())
        .build();
    public static final SimpleAttributeDefinition ENTITY_MODULE = new SimpleAttributeDefinitionBuilder(ModelElement.JPA_STORE_ENTITY_MODULE.getName(), ModelType.STRING, true)
        .setAllowExpression(true)
        .setCorrector(ModuleIdentifierUtil.MODULE_NAME_CORRECTOR)
        .build();
    public static final SimpleAttributeDefinition ENTITY_MODULE_UNIT_NAME = new SimpleAttributeDefinitionBuilder(ModelElement.JPA_STORE_ENTITY_MODULE_UNIT_NAME.getName(), ModelType.STRING, true)
        .setDefaultValue(new ModelNode("identity"))
        .setAllowExpression(true)
        .setAlternatives(
            ModelElement.JPA_STORE_ENTITY_MANAGER_FACTORY.getName(),
            ModelElement.JPA_STORE_DATASOURCE.getName())
        .setRequires(ModelElement.JPA_STORE_ENTITY_MODULE_UNIT_NAME.getName())
        .build();
    public static final SimpleAttributeDefinition ENTITY_MANAGER_FACTORY = new SimpleAttributeDefinitionBuilder(ModelElement.JPA_STORE_ENTITY_MANAGER_FACTORY.getName(), ModelType.STRING, true)
        .setAllowExpression(true)
        .setAlternatives(
            ModelElement.JPA_STORE_DATASOURCE.getName(),
            ModelElement.JPA_STORE_ENTITY_MODULE_UNIT_NAME.getName())
        .build();
    public static final JPAStoreResourceDefinition INSTANCE = new JPAStoreResourceDefinition(DATA_SOURCE, ENTITY_MODULE, ENTITY_MODULE_UNIT_NAME, ENTITY_MANAGER_FACTORY, SUPPORT_CREDENTIAL, SUPPORT_ATTRIBUTE);

    private JPAStoreResourceDefinition(SimpleAttributeDefinition... attributes) {
        super(ModelElement.JPA_STORE, getModelValidators(), attributes);
    }

    @Override
    public void registerChildren(ManagementResourceRegistration resourceRegistration) {
        addChildResourceDefinition(SupportedTypesResourceDefinition.INSTANCE, resourceRegistration);
        addChildResourceDefinition(CredentialHandlerResourceDefinition.INSTANCE, resourceRegistration);
    }

    private static ModelValidationStepHandler[] getModelValidators() {
        return new ModelValidationStepHandler[] {
            NotEmptyResourceValidationStepHandler.INSTANCE,
            new RequiredChildValidationStepHandler(ModelElement.SUPPORTED_TYPES)
        };
    }
}
