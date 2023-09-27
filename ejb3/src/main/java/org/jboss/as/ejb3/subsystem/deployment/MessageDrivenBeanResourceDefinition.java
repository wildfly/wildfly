/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.ejb3.subsystem.deployment;

import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.PropertiesAttributeDefinition;
import org.jboss.as.controller.SimpleAttributeDefinition;
import org.jboss.as.controller.SimpleAttributeDefinitionBuilder;
import org.jboss.as.controller.SimpleOperationDefinition;
import org.jboss.as.controller.SimpleOperationDefinitionBuilder;
import org.jboss.as.controller.registry.AttributeAccess;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;

/**
 * {@link org.jboss.as.controller.ResourceDefinition} for a {@link org.jboss.as.ejb3.component.messagedriven.MessageDrivenComponent}.
 *
 * @author Brian Stansberry (c) 2011 Red Hat Inc.
 */
public class MessageDrivenBeanResourceDefinition extends AbstractEJBComponentResourceDefinition {

    static final AttributeDefinition DELIVERY_ACTIVE = new SimpleAttributeDefinitionBuilder("delivery-active", ModelType.BOOLEAN, true)
            .setDefaultValue(ModelNode.TRUE)
            .setStorageRuntime()
            .build();

    static final SimpleAttributeDefinition MESSAGING_TYPE = new SimpleAttributeDefinitionBuilder("messaging-type", ModelType.STRING)
            .setFlags(AttributeAccess.Flag.STORAGE_RUNTIME)
            .build();

    static final SimpleAttributeDefinition MESSAGE_DESTINATION_TYPE = new SimpleAttributeDefinitionBuilder("message-destination-type", ModelType.STRING)
            .setFlags(AttributeAccess.Flag.STORAGE_RUNTIME)
            .build();

    static final SimpleAttributeDefinition MESSAGE_DESTINATION_LINK = new SimpleAttributeDefinitionBuilder("message-destination-link", ModelType.STRING)
            .setFlags(AttributeAccess.Flag.STORAGE_RUNTIME)
            .build();

    static final PropertiesAttributeDefinition ACTIVATION_CONFIG = new PropertiesAttributeDefinition.Builder("activation-config", true)
            .setFlags(AttributeAccess.Flag.STORAGE_RUNTIME)
            .build();

    static final String START_DELIVERY = "start-delivery";
    static final String STOP_DELIVERY = "stop-delivery";

    public MessageDrivenBeanResourceDefinition() {
        super(EJBComponentType.MESSAGE_DRIVEN);
    }

    @Override
    public void registerAttributes(ManagementResourceRegistration registry) {
        super.registerAttributes(registry);

        registry.registerReadOnlyAttribute(DELIVERY_ACTIVE, MessageDrivenBeanRuntimeHandler.INSTANCE);
        registry.registerReadOnlyAttribute(MESSAGING_TYPE, MessageDrivenBeanRuntimeHandler.INSTANCE);
        registry.registerReadOnlyAttribute(MESSAGE_DESTINATION_TYPE, MessageDrivenBeanRuntimeHandler.INSTANCE);
        registry.registerReadOnlyAttribute(MESSAGE_DESTINATION_LINK, MessageDrivenBeanRuntimeHandler.INSTANCE);
        registry.registerReadOnlyAttribute(ACTIVATION_CONFIG, MessageDrivenBeanRuntimeHandler.INSTANCE);
    }

    @Override
    public void registerOperations(ManagementResourceRegistration registry) {
        super.registerOperations(registry);

        final SimpleOperationDefinition startDelivery = new SimpleOperationDefinitionBuilder(START_DELIVERY, getResourceDescriptionResolver())
                .setRuntimeOnly()
                .build();
        registry.registerOperationHandler(startDelivery, MessageDrivenBeanRuntimeHandler.INSTANCE);
        final SimpleOperationDefinition stopDelivery = new SimpleOperationDefinitionBuilder(STOP_DELIVERY, getResourceDescriptionResolver())
                .setRuntimeOnly()
                .build();
        registry.registerOperationHandler(stopDelivery, MessageDrivenBeanRuntimeHandler.INSTANCE);
    }
}
