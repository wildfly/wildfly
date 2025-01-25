/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.security;

import org.jboss.as.controller.AbstractBoottimeAddStepHandler;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.PropertiesAttributeDefinition;
import org.jboss.as.controller.ReloadRequiredRemoveStepHandler;
import org.jboss.as.controller.ReloadRequiredWriteAttributeHandler;
import org.jboss.as.controller.SimpleAttributeDefinition;
import org.jboss.as.controller.SimpleAttributeDefinitionBuilder;
import org.jboss.as.controller.SimpleResourceDefinition;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;

/**
 * @author Jason T. Greene
 */
class VaultResourceDefinition extends SimpleResourceDefinition {

    public static final VaultResourceDefinition INSTANCE = new VaultResourceDefinition();

    public static final SimpleAttributeDefinition CODE = new SimpleAttributeDefinitionBuilder(Constants.CODE, ModelType.STRING, true)
                    .build();

    public static final PropertiesAttributeDefinition OPTIONS = new PropertiesAttributeDefinition.Builder(Constants.VAULT_OPTIONS, true)
            .setXmlName(Constants.VAULT_OPTION)
            .setAllowExpression(true)
            .build();


    private VaultResourceDefinition() {
        super(SecurityExtension.VAULT_PATH,
                SecurityExtension.getResourceDescriptionResolver(Constants.VAULT),
                VaultResourceDefinitionAdd.INSTANCE, ReloadRequiredRemoveStepHandler.INSTANCE);
        setDeprecated(SecurityExtension.DEPRECATED_SINCE);
    }

    public void registerAttributes(final ManagementResourceRegistration resourceRegistration) {
        resourceRegistration.registerReadWriteAttribute(OPTIONS, null, new ReloadRequiredWriteAttributeHandler(OPTIONS));
        resourceRegistration.registerReadWriteAttribute(CODE, null, new ReloadRequiredWriteAttributeHandler(CODE));
    }

    static class VaultResourceDefinitionAdd extends AbstractBoottimeAddStepHandler {
        static final VaultResourceDefinitionAdd INSTANCE = new VaultResourceDefinitionAdd();

        @Override
        protected void populateModel(ModelNode operation, ModelNode model) throws OperationFailedException {
            CODE.validateAndSet(operation, model);
            OPTIONS.validateAndSet(operation, model);
        }

    }

}
