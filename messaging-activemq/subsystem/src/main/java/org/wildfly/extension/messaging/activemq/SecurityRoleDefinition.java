/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.extension.messaging.activemq;

import static org.jboss.as.controller.registry.AttributeAccess.Flag.RESTART_NONE;
import static org.jboss.dmr.ModelType.BOOLEAN;
import static org.jboss.dmr.ModelType.STRING;

import java.util.Collection;
import java.util.Collections;

import org.apache.activemq.artemis.core.security.Role;
import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.ObjectTypeAttributeDefinition;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.PersistentResourceDefinition;
import org.jboss.as.controller.SimpleAttributeDefinition;
import org.jboss.as.controller.SimpleAttributeDefinitionBuilder;
import org.jboss.as.controller.registry.AttributeAccess;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.dmr.ModelNode;

/**
 * Security role resource definition
 *
 * @author <a href="http://jmesnil.net">Jeff Mesnil</a> (c) 2012 Red Hat Inc.
 */
public class SecurityRoleDefinition extends PersistentResourceDefinition {

    public static ObjectTypeAttributeDefinition getObjectTypeAttributeDefinition() {
        // add the role name as an attribute of the object type
        SimpleAttributeDefinition[] attrs = new SimpleAttributeDefinition[ATTRIBUTES.length + 1];
        attrs[0] = NAME;
        System.arraycopy(ATTRIBUTES, 0, attrs, 1, ATTRIBUTES.length);
        return ObjectTypeAttributeDefinition.Builder.of(CommonAttributes.ROLE, attrs).build();
    }

    private static SimpleAttributeDefinition create(final String name) {
        return SimpleAttributeDefinitionBuilder.create(name, BOOLEAN)
                .setDefaultValue(ModelNode.FALSE)
                .setRequired(false)
                .setFlags(RESTART_NONE)
                .build();
    }

    static final SimpleAttributeDefinition SEND = create("send");
    static final SimpleAttributeDefinition CONSUME = create("consume");
    static final SimpleAttributeDefinition CREATE_DURABLE_QUEUE = create("create-durable-queue");
    static final SimpleAttributeDefinition DELETE_DURABLE_QUEUE = create("delete-durable-queue");
    static final SimpleAttributeDefinition CREATE_NON_DURABLE_QUEUE = create("create-non-durable-queue");
    static final SimpleAttributeDefinition DELETE_NON_DURABLE_QUEUE = create("delete-non-durable-queue");
    static final SimpleAttributeDefinition MANAGE = SimpleAttributeDefinitionBuilder.create("manage", BOOLEAN)
            .setDefaultValue(ModelNode.FALSE)
            .setRequired(false)
            .setFlags(RESTART_NONE)
            .addAccessConstraint(MessagingExtension.MESSAGING_MANAGEMENT_SENSITIVE_TARGET)
            .build();
    static final SimpleAttributeDefinition BROWSE = SimpleAttributeDefinitionBuilder.create("browse", BOOLEAN)
            .setDefaultValue(ModelNode.FALSE)
            .setRequired(false)
            .setFlags(RESTART_NONE)
            .setStorageRuntime()
            .addAccessConstraint(MessagingExtension.MESSAGING_MANAGEMENT_SENSITIVE_TARGET)
            .build();
    static final SimpleAttributeDefinition CREATE_ADDRESS = SimpleAttributeDefinitionBuilder.create("create-address", BOOLEAN)
            .setDefaultValue(ModelNode.FALSE)
            .setRequired(false)
            .setFlags(RESTART_NONE)
            .setStorageRuntime()
            .addAccessConstraint(MessagingExtension.MESSAGING_MANAGEMENT_SENSITIVE_TARGET)
            .build();
    static final SimpleAttributeDefinition DELETE_ADDRESS = SimpleAttributeDefinitionBuilder.create("delete-address", BOOLEAN)
            .setDefaultValue(ModelNode.FALSE)
            .setRequired(false)
            .setFlags(RESTART_NONE)
            .setStorageRuntime()
            .addAccessConstraint(MessagingExtension.MESSAGING_MANAGEMENT_SENSITIVE_TARGET)
            .build();

    static final SimpleAttributeDefinition[] ATTRIBUTES = {
        SEND,
        CONSUME,
        CREATE_DURABLE_QUEUE,
        DELETE_DURABLE_QUEUE,
        CREATE_NON_DURABLE_QUEUE,
        DELETE_NON_DURABLE_QUEUE,
        MANAGE
    };

    static final SimpleAttributeDefinition NAME = SimpleAttributeDefinitionBuilder.create("name", STRING)
            .build();

    private final boolean runtimeOnly;

    static Role transform(final OperationContext context, final String name, final ModelNode node) throws OperationFailedException {
        final boolean send = SEND.resolveModelAttribute(context, node).asBoolean();
        final boolean consume = CONSUME.resolveModelAttribute(context, node).asBoolean();
        final boolean createDurableQueue = CREATE_DURABLE_QUEUE.resolveModelAttribute(context, node).asBoolean();
        final boolean deleteDurableQueue = DELETE_DURABLE_QUEUE.resolveModelAttribute(context, node).asBoolean();
        final boolean createNonDurableQueue = CREATE_NON_DURABLE_QUEUE.resolveModelAttribute(context, node).asBoolean();
        final boolean deleteNonDurableQueue = DELETE_NON_DURABLE_QUEUE.resolveModelAttribute(context, node).asBoolean();
        final boolean manage = MANAGE.resolveModelAttribute(context, node).asBoolean();
        return new Role(name, send, consume, createDurableQueue, deleteDurableQueue, createNonDurableQueue,
                deleteNonDurableQueue, manage, consume, createDurableQueue || createNonDurableQueue,
                deleteDurableQueue || deleteNonDurableQueue);
    }

    SecurityRoleDefinition(final boolean runtimeOnly) {
        super(MessagingExtension.ROLE_PATH,
                MessagingExtension.getResourceDescriptionResolver(CommonAttributes.SECURITY_ROLE),
                runtimeOnly ? null : SecurityRoleAdd.INSTANCE,
                runtimeOnly ? null : SecurityRoleRemove.INSTANCE,
                runtimeOnly);
        this.runtimeOnly = runtimeOnly;
    }

    @Override
    public Collection<AttributeDefinition> getAttributes() {
        return Collections.emptyList();
    }

    @Override
    public void registerAttributes(ManagementResourceRegistration registry) {
        super.registerAttributes(registry);

        if (runtimeOnly) {
            for (SimpleAttributeDefinition attr : ATTRIBUTES) {
                AttributeDefinition readOnlyAttr = SimpleAttributeDefinitionBuilder.create(attr)
                        .setStorageRuntime()
                        .build();
                registry.registerReadOnlyAttribute(readOnlyAttr, SecurityRoleReadAttributeHandler.INSTANCE);
            }
            registry.registerReadOnlyAttribute(BROWSE, SecurityRoleReadAttributeHandler.INSTANCE);
            registry.registerReadOnlyAttribute(CREATE_ADDRESS, SecurityRoleReadAttributeHandler.INSTANCE);
            registry.registerReadOnlyAttribute(DELETE_ADDRESS, SecurityRoleReadAttributeHandler.INSTANCE);
        } else {
            for (AttributeDefinition attr : ATTRIBUTES) {
                if (!attr.getFlags().contains(AttributeAccess.Flag.STORAGE_RUNTIME)) {
                    registry.registerReadWriteAttribute(attr, null, SecurityRoleAttributeHandler.INSTANCE);
                }
            }
        }
    }
}
