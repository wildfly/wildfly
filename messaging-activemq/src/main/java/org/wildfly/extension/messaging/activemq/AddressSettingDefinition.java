/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2012, Red Hat, Inc., and individual contributors
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

package org.wildfly.extension.messaging.activemq;

import static org.jboss.as.controller.SimpleAttributeDefinitionBuilder.create;
import static org.jboss.as.controller.client.helpers.MeasurementUnit.BYTES;
import static org.jboss.as.controller.client.helpers.MeasurementUnit.DAYS;
import static org.jboss.as.controller.client.helpers.MeasurementUnit.MILLISECONDS;
import static org.jboss.as.controller.client.helpers.MeasurementUnit.SECONDS;
import static org.wildfly.extension.messaging.activemq.CommonAttributes.DEAD_LETTER_ADDRESS;
import static org.wildfly.extension.messaging.activemq.CommonAttributes.EXPIRY_ADDRESS;
import static org.wildfly.extension.messaging.activemq.MessagingExtension.VERSION_3_0_0;

import java.util.Arrays;
import java.util.Collection;

import org.apache.activemq.artemis.core.settings.impl.AddressFullMessagePolicy;
import org.apache.activemq.artemis.core.settings.impl.AddressSettings;
import org.apache.activemq.artemis.core.settings.impl.SlowConsumerPolicy;
import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.PersistentResourceDefinition;
import org.jboss.as.controller.SimpleAttributeDefinition;
import org.jboss.as.controller.operations.validation.EnumValidator;
import org.jboss.as.controller.registry.AttributeAccess;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;

/**
 * Address setting resource definition
 *
 * @author <a href="http://jmesnil.net">Jeff Mesnil</a> (c) 2012 Red Hat Inc.
 */
public class AddressSettingDefinition extends PersistentResourceDefinition {

    public static final SimpleAttributeDefinition AUTO_CREATE_ADDRESSES = create("auto-create-addresses", ModelType.BOOLEAN)
            .setDefaultValue(new ModelNode(true))
            .setRequired(false)
            .setAllowExpression(true)
            .build();

    public static final SimpleAttributeDefinition AUTO_DELETE_ADDRESSES = create("auto-delete-addresses", ModelType.BOOLEAN)
            .setDefaultValue(new ModelNode(true))
            .setRequired(false)
            .setAllowExpression(true)
            .build();

    // Property exists in Artemis 2 but is no longer honoured
    @Deprecated
    public static final SimpleAttributeDefinition AUTO_CREATE_JMS_QUEUES = create("auto-create-jms-queues", ModelType.BOOLEAN)
            .setDefaultValue(new ModelNode(false))
            .setRequired(false)
            .setAllowExpression(true)
            .setDeprecated(VERSION_3_0_0)
            .build();

    // Property exists in Artemis 2 but is no longer honoured
    @Deprecated
    public static final SimpleAttributeDefinition AUTO_DELETE_JMS_QUEUES = create("auto-delete-jms-queues", ModelType.BOOLEAN)
            .setDefaultValue(new ModelNode(false))
            .setRequired(false)
            .setAllowExpression(true)
            .setDeprecated(VERSION_3_0_0)
            .build();

    public static final SimpleAttributeDefinition AUTO_CREATE_QUEUES = create("auto-create-queues", ModelType.BOOLEAN)
            .setDefaultValue(new ModelNode(false))
            .setRequired(false)
            .setAllowExpression(true)
            .build();

    public static final SimpleAttributeDefinition AUTO_DELETE_QUEUES = create("auto-delete-queues", ModelType.BOOLEAN)
            .setDefaultValue(new ModelNode(false))
            .setRequired(false)
            .setAllowExpression(true)
            .build();

    public static final SimpleAttributeDefinition ADDRESS_FULL_MESSAGE_POLICY = create("address-full-policy", ModelType.STRING)
            .setDefaultValue(new ModelNode(AddressSettings.DEFAULT_ADDRESS_FULL_MESSAGE_POLICY.toString()))
            .setValidator(new EnumValidator<>(AddressFullMessagePolicy.class, true, false))
            .setRequired(false)
            .setAllowExpression(true)
            .build();

    public static final SimpleAttributeDefinition EXPIRY_DELAY = create("expiry-delay", ModelType.LONG)
            .setDefaultValue(new ModelNode(AddressSettings.DEFAULT_EXPIRY_DELAY))
            .setMeasurementUnit(MILLISECONDS)
            .setRequired(false)
            .setAllowExpression(true)
            .build();

    public static final SimpleAttributeDefinition LAST_VALUE_QUEUE = create("last-value-queue", ModelType.BOOLEAN)
            .setDefaultValue(new ModelNode(AddressSettings.DEFAULT_LAST_VALUE_QUEUE))
            .setRequired(false)
            .setAllowExpression(true)
            .build();

    public static final SimpleAttributeDefinition MAX_DELIVERY_ATTEMPTS = create("max-delivery-attempts", ModelType.INT)
            .setDefaultValue(new ModelNode(AddressSettings.DEFAULT_MAX_DELIVERY_ATTEMPTS))
            .setRequired(false)
            .setAllowExpression(true)
            .build();

    public static final SimpleAttributeDefinition MAX_REDELIVERY_DELAY = create("max-redelivery-delay", ModelType.LONG)
            .setDefaultValue(new ModelNode(AddressSettings.DEFAULT_REDELIVER_DELAY * 10))
            .setMeasurementUnit(MILLISECONDS)
            .setRequired(false)
            .setAllowExpression(true)
            .build();

    public static final SimpleAttributeDefinition MAX_SIZE_BYTES = create("max-size-bytes", ModelType.LONG)
            .setDefaultValue(new ModelNode(AddressSettings.DEFAULT_MAX_SIZE_BYTES))
            .setMeasurementUnit(BYTES)
            .setRequired(false)
            .setAllowExpression(true)
            .build();

    public static final SimpleAttributeDefinition MESSAGE_COUNTER_HISTORY_DAY_LIMIT = create("message-counter-history-day-limit", ModelType.INT)
            .setDefaultValue(new ModelNode(AddressSettings.DEFAULT_MESSAGE_COUNTER_HISTORY_DAY_LIMIT))
            .setMeasurementUnit(DAYS)
            .setRequired(false)
            .setAllowExpression(true)
            .build();

    public static final SimpleAttributeDefinition PAGE_MAX_CACHE_SIZE = create("page-max-cache-size", ModelType.INT)
            .setDefaultValue(new ModelNode(AddressSettings.DEFAULT_PAGE_MAX_CACHE))
            .setRequired(false)
            .setAllowExpression(true)
            .build();

    public static final SimpleAttributeDefinition PAGE_SIZE_BYTES = create("page-size-bytes", ModelType.LONG)
            .setDefaultValue(new ModelNode(AddressSettings.DEFAULT_PAGE_SIZE))
            .setMeasurementUnit(BYTES)
            .setRequired(false)
            .setAllowExpression(true)
            .build();

    public static final SimpleAttributeDefinition REDELIVERY_DELAY = create("redelivery-delay", ModelType.LONG)
            .setDefaultValue(new ModelNode(AddressSettings.DEFAULT_REDELIVER_DELAY))
            .setMeasurementUnit(MILLISECONDS)
            .setRequired(false)
            .setAllowExpression(true)
            .build();

    public static final SimpleAttributeDefinition REDELIVERY_MULTIPLIER = create("redelivery-multiplier", ModelType.DOUBLE)
            .setDefaultValue(new ModelNode(AddressSettings.DEFAULT_REDELIVER_MULTIPLIER))
            .setRequired(false)
            .setAllowExpression(true)
            .build();

    public static final SimpleAttributeDefinition REDISTRIBUTION_DELAY = create("redistribution-delay", ModelType.LONG)
            .setDefaultValue(new ModelNode(AddressSettings.DEFAULT_REDISTRIBUTION_DELAY))
            .setMeasurementUnit(MILLISECONDS)
            .setRequired(false)
            .setAllowExpression(true)
            .build();

    public static final SimpleAttributeDefinition SEND_TO_DLA_ON_NO_ROUTE = create("send-to-dla-on-no-route", ModelType.BOOLEAN)
            .setDefaultValue(new ModelNode(AddressSettings.DEFAULT_SEND_TO_DLA_ON_NO_ROUTE))
            .setRequired(false)
            .setAllowExpression(true)
            .build();

    public static final SimpleAttributeDefinition SLOW_CONSUMER_CHECK_PERIOD = create("slow-consumer-check-period", ModelType.LONG)
            .setDefaultValue(new ModelNode(AddressSettings.DEFAULT_SLOW_CONSUMER_CHECK_PERIOD))
            .setMeasurementUnit(SECONDS)
            .setRequired(false)
            .setAllowExpression(true)
            .build();

    public static final SimpleAttributeDefinition SLOW_CONSUMER_POLICY = create("slow-consumer-policy", ModelType.STRING)
            .setDefaultValue(new ModelNode(AddressSettings.DEFAULT_SLOW_CONSUMER_POLICY.toString()))
            .setValidator(new EnumValidator<>(SlowConsumerPolicy.class, true, true))
            .setRequired(false)
            .setAllowExpression(true)
            .build();

    public static final SimpleAttributeDefinition SLOW_CONSUMER_THRESHOLD = create("slow-consumer-threshold", ModelType.LONG)
            .setDefaultValue(new ModelNode(AddressSettings.DEFAULT_SLOW_CONSUMER_THRESHOLD))
            .setRequired(false)
            .setAllowExpression(true)
            .build();


    /**
     * Attributes are defined in the <em>same order than in the XSD schema</em>
     */
    static final AttributeDefinition[] ATTRIBUTES = new SimpleAttributeDefinition[] {
            DEAD_LETTER_ADDRESS,
            EXPIRY_ADDRESS,
            EXPIRY_DELAY,
            REDELIVERY_DELAY,
            REDELIVERY_MULTIPLIER,
            MAX_DELIVERY_ATTEMPTS,
            MAX_REDELIVERY_DELAY,
            MAX_SIZE_BYTES,
            PAGE_SIZE_BYTES,
            PAGE_MAX_CACHE_SIZE,
            ADDRESS_FULL_MESSAGE_POLICY,
            MESSAGE_COUNTER_HISTORY_DAY_LIMIT,
            LAST_VALUE_QUEUE,
            REDISTRIBUTION_DELAY,
            SEND_TO_DLA_ON_NO_ROUTE,
            SLOW_CONSUMER_CHECK_PERIOD,
            SLOW_CONSUMER_POLICY,
            SLOW_CONSUMER_THRESHOLD,
            AUTO_CREATE_JMS_QUEUES,
            AUTO_DELETE_JMS_QUEUES,
            AUTO_CREATE_ADDRESSES,
            AUTO_DELETE_ADDRESSES,
            AUTO_CREATE_QUEUES,
            AUTO_DELETE_QUEUES
    };

    static final AddressSettingDefinition INSTANCE = new AddressSettingDefinition();

    private AddressSettingDefinition() {
        super(MessagingExtension.ADDRESS_SETTING_PATH,
                MessagingExtension.getResourceDescriptionResolver(CommonAttributes.ADDRESS_SETTING),
                AddressSettingAdd.INSTANCE,
                AddressSettingRemove.INSTANCE);
    }


    @Override
    public void registerAttributes(ManagementResourceRegistration registry) {
        for (AttributeDefinition attr : ATTRIBUTES) {
            if (!attr.getFlags().contains(AttributeAccess.Flag.STORAGE_RUNTIME)) {
                registry.registerReadWriteAttribute(attr, null, AddressSettingsWriteHandler.INSTANCE);
            }
        }
    }

    @Override
    public Collection<AttributeDefinition> getAttributes() {
        return Arrays.asList(ATTRIBUTES);
    }
}
