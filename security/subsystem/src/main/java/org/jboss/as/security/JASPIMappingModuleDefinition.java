/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.security;

import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.SimpleAttributeDefinition;
import org.jboss.as.controller.SimpleAttributeDefinitionBuilder;
import org.jboss.as.controller.operations.validation.EnumValidator;
import org.jboss.as.controller.operations.validation.StringLengthValidator;
import org.jboss.dmr.ModelType;

/**
 * @author <a href="mailto:tomaz.cerar@redhat.com">Tomaz Cerar</a> (c) 2012 Red Hat Inc.
 */
class JASPIMappingModuleDefinition extends MappingModuleDefinition {

    static final SimpleAttributeDefinition LOGIN_MODULE_STACK_REF = new SimpleAttributeDefinitionBuilder(Constants.LOGIN_MODULE_STACK_REF, ModelType.STRING)
            .setRequired(false)
            .setValidator(new StringLengthValidator(1, true))
            .build();

    private static final SimpleAttributeDefinition FLAG = new SimpleAttributeDefinitionBuilder(Constants.FLAG, ModelType.STRING)
            .setRequired(false)
            .setValidator(EnumValidator.create(ModuleFlag.class))
            .setAllowExpression(true)
            .build();



    private static final AttributeDefinition[] ATTRIBUTES = {CODE, FLAG, LOGIN_MODULE_STACK_REF, MODULE_OPTIONS, LoginModuleResourceDefinition.MODULE};

    JASPIMappingModuleDefinition() {
        super(Constants.AUTH_MODULE);
    }

    @Override
    public AttributeDefinition[] getAttributes() {
        return ATTRIBUTES;
    }
}
