/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.security;

import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.ModuleIdentifierUtil;
import org.jboss.as.controller.ModelOnlyAddStepHandler;
import org.jboss.as.controller.ModelOnlyRemoveStepHandler;
import org.jboss.as.controller.ModelOnlyWriteAttributeHandler;
import org.jboss.as.controller.OperationStepHandler;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.PropertiesAttributeDefinition;
import org.jboss.as.controller.SimpleAttributeDefinition;
import org.jboss.as.controller.SimpleAttributeDefinitionBuilder;
import org.jboss.as.controller.SimpleResourceDefinition;
import org.jboss.as.controller.operations.validation.EnumValidator;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.dmr.ModelType;

/**
 * @author <a href="mailto:tomaz.cerar@redhat.com">Tomaz Cerar</a> (c) 2012 Red Hat Inc.
 */
class LoginModuleResourceDefinition extends SimpleResourceDefinition {
    static final SimpleAttributeDefinition CODE = new SimpleAttributeDefinitionBuilder(Constants.CODE, ModelType.STRING)
            .setRequired(true)
            .setMinSize(1)
            .build();

    static final SimpleAttributeDefinition FLAG = new SimpleAttributeDefinitionBuilder(Constants.FLAG, ModelType.STRING)
            .setRequired(true)
            .setAllowExpression(true)
            .setValidator(EnumValidator.create(ModuleFlag.class))
            .build();

    static final SimpleAttributeDefinition MODULE = new SimpleAttributeDefinitionBuilder(Constants.MODULE, ModelType.STRING)
            .setRequired(false)
            .setAllowExpression(false)
            .setMinSize(1)
            .setCorrector(ModuleIdentifierUtil.MODULE_NAME_CORRECTOR)
            .build();
    static final PropertiesAttributeDefinition MODULE_OPTIONS = new PropertiesAttributeDefinition.Builder(Constants.MODULE_OPTIONS, true)
            .setAllowExpression(true)
            .build();

    static final AttributeDefinition[] ATTRIBUTES = {CODE, FLAG, MODULE, MODULE_OPTIONS};

    LoginModuleResourceDefinition(final String key) {
        super(PathElement.pathElement(key),
                SecurityExtension.getResourceDescriptionResolver(Constants.LOGIN_MODULE_STACK, Constants.LOGIN_MODULES),
                new ModelOnlyAddStepHandler(ATTRIBUTES),
                ModelOnlyRemoveStepHandler.INSTANCE
        );
    }

    @Override
    public void registerAttributes(ManagementResourceRegistration resourceRegistration) {
        super.registerAttributes(resourceRegistration);
        OperationStepHandler writeHandler = new ModelOnlyWriteAttributeHandler(ATTRIBUTES);
        for (AttributeDefinition attribute : ATTRIBUTES) {
            resourceRegistration.registerReadWriteAttribute(attribute, null, writeHandler);
        }
    }
}
