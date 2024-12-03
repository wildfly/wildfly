/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.ee.subsystem;

import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.OperationStepHandler;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.ReloadRequiredWriteAttributeHandler;
import org.jboss.as.controller.ServiceRemoveStepHandler;
import org.jboss.as.controller.SimpleAttributeDefinition;
import org.jboss.as.controller.SimpleAttributeDefinitionBuilder;
import org.jboss.as.controller.SimpleResourceDefinition;
import org.jboss.as.controller.capability.RuntimeCapability;
import org.jboss.as.controller.descriptions.ResourceDescriptionResolver;
import org.jboss.as.controller.descriptions.StandardResourceDescriptionResolver;
import org.jboss.as.controller.registry.AttributeAccess;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.as.ee.concurrent.WildFlyContextService;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;

/**
 * @author Eduardo Martins
 */
public class ContextServiceResourceDefinition extends SimpleResourceDefinition {

    /**
     * the resource definition's dynamic runtime capability
     */
    public static final RuntimeCapability<Void> CAPABILITY = RuntimeCapability.Builder.of("org.wildfly.ee.concurrent.context.service", true, WildFlyContextService.class)
            .build();

    public static final String JNDI_NAME = "jndi-name";
    public static final String USE_TRANSACTION_SETUP_PROVIDER = "use-transaction-setup-provider";

    public static final SimpleAttributeDefinition JNDI_NAME_AD =
            new SimpleAttributeDefinitionBuilder(JNDI_NAME, ModelType.STRING, false)
                    .setAllowExpression(true)
                    .setFlags(AttributeAccess.Flag.RESTART_RESOURCE_SERVICES)
                    .build();

    public static final SimpleAttributeDefinition USE_TRANSACTION_SETUP_PROVIDER_AD =
            new SimpleAttributeDefinitionBuilder(USE_TRANSACTION_SETUP_PROVIDER, ModelType.BOOLEAN, true)
                    .setAllowExpression(true)
                    .setDefaultValue(ModelNode.FALSE)
                    .setFlags(AttributeAccess.Flag.RESTART_RESOURCE_SERVICES)
                    .setDeprecated(EESubsystemModel.Version.v6_0_0)
                    .build();

    static final SimpleAttributeDefinition[] ATTRIBUTES = {JNDI_NAME_AD, USE_TRANSACTION_SETUP_PROVIDER_AD};

    private static final ResourceDescriptionResolver RESOLVER = new StandardResourceDescriptionResolver(EESubsystemModel.CONTEXT_SERVICE, EeExtension.class.getPackage().getName() + ".LocalDescriptions", EeExtension.class.getClassLoader(), true, true);

    public ContextServiceResourceDefinition() {
        super(new SimpleResourceDefinition.Parameters(PathElement.pathElement(EESubsystemModel.CONTEXT_SERVICE), RESOLVER)
                .setAddHandler(ContextServiceAdd.INSTANCE)
                .setRemoveHandler(new ServiceRemoveStepHandler(ContextServiceAdd.INSTANCE))
                .addCapabilities(CAPABILITY));
    }

    @Override
    public void registerAttributes(ManagementResourceRegistration resourceRegistration) {
        OperationStepHandler writeHandler = new ReloadRequiredWriteAttributeHandler(ATTRIBUTES);
        for (AttributeDefinition attr : ATTRIBUTES) {
            resourceRegistration.registerReadWriteAttribute(attr, null, writeHandler);
        }
    }
}
