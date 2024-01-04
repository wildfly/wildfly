/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.extension.messaging.activemq;

import static org.jboss.as.controller.SimpleAttributeDefinitionBuilder.create;
import static org.jboss.as.controller.client.helpers.MeasurementUnit.MILLISECONDS;
import static org.jboss.as.controller.registry.AttributeAccess.Flag.STORAGE_RUNTIME;
import static org.jboss.dmr.ModelType.LONG;
import static org.jboss.dmr.ModelType.STRING;

import java.util.Arrays;
import java.util.Collection;

import org.apache.activemq.artemis.api.config.ActiveMQDefaultConfiguration;
import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.PersistentResourceDefinition;
import org.jboss.as.controller.SimpleAttributeDefinition;
import org.jboss.as.controller.operations.validation.EnumValidator;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.dmr.ModelNode;

/**
 * Grouping handler resource definition
 *
 * @author <a href="http://jmesnil.net">Jeff Mesnil</a> (c) 2012 Red Hat Inc.
 */
public class GroupingHandlerDefinition extends PersistentResourceDefinition {

    public static final SimpleAttributeDefinition GROUPING_HANDLER_ADDRESS = create("grouping-handler-address", STRING)
            .setXmlName(CommonAttributes.ADDRESS)
            .setAllowExpression(true)
            .setRestartAllServices()
            .build();

    /**
     * @see ActiveMQDefaultConfiguration#getDefaultGroupingHandlerTimeout
     */
    public static final SimpleAttributeDefinition TIMEOUT = create("timeout", LONG)
            .setDefaultValue(new ModelNode(5000L))
            .setMeasurementUnit(MILLISECONDS)
            .setRequired(false)
            .setAllowExpression(true)
            .setRestartAllServices()
            .build();

    /**
     * @see ActiveMQDefaultConfiguration#getDefaultGroupingHandlerGroupTimeout
     */
    public static final SimpleAttributeDefinition GROUP_TIMEOUT = create("group-timeout", LONG)
            // FIXME Cast to a long until Artemis type is fixed
            .setDefaultValue(new ModelNode(-1L))
            .setMeasurementUnit(MILLISECONDS)
            .setRequired(false)
            .setAllowExpression(true)
            .setValidator(InfiniteOrPositiveValidators.LONG_INSTANCE)
            .setRestartAllServices()
            .build();

    /**
     * @see ActiveMQDefaultConfiguration#getDefaultGroupingHandlerReaperPeriod
     */
    public static final SimpleAttributeDefinition REAPER_PERIOD = create("reaper-period", LONG)
            .setDefaultValue(new ModelNode(30000L))
            .setMeasurementUnit(MILLISECONDS)
            .setRequired(false)
            .setAllowExpression(true)
            .setRestartAllServices()
            .build();

    public static final SimpleAttributeDefinition TYPE = create("type", STRING)
            .setAllowExpression(true)
            .setValidator(EnumValidator.create(Type.class))
            .setRestartAllServices()
            .build();

    public static final AttributeDefinition[] ATTRIBUTES = { TYPE, GROUPING_HANDLER_ADDRESS, TIMEOUT, GROUP_TIMEOUT, REAPER_PERIOD };

    GroupingHandlerDefinition() {
        super(MessagingExtension.GROUPING_HANDLER_PATH,
                MessagingExtension.getResourceDescriptionResolver(CommonAttributes.GROUPING_HANDLER),
                GroupingHandlerAdd.INSTANCE,
                GroupingHandlerRemove.INSTANCE);
    }

    @Override
    public Collection<AttributeDefinition> getAttributes() {
        return Arrays.asList(ATTRIBUTES);
    }

    @Override
    public void registerAttributes(ManagementResourceRegistration registry) {
        for (AttributeDefinition attr : ATTRIBUTES) {
            if (!attr.getFlags().contains(STORAGE_RUNTIME)) {
                registry.registerReadWriteAttribute(attr, null, GroupingHandlerWriteAttributeHandler.INSTANCE);
            }
        }
    }

    private enum Type {
        LOCAL, REMOTE;
    }
}