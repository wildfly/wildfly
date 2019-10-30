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

package org.jboss.as.messaging;

import static org.jboss.as.controller.SimpleAttributeDefinitionBuilder.create;
import static org.jboss.as.controller.client.helpers.MeasurementUnit.BYTES;
import static org.jboss.as.controller.client.helpers.MeasurementUnit.DAYS;
import static org.jboss.as.controller.client.helpers.MeasurementUnit.MILLISECONDS;
import static org.jboss.as.controller.client.helpers.MeasurementUnit.MINUTES;
import static org.jboss.as.messaging.CommonAttributes.DEAD_LETTER_ADDRESS;
import static org.jboss.as.messaging.CommonAttributes.EXPIRY_ADDRESS;

import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.ModelOnlyAddStepHandler;
import org.jboss.as.controller.ModelOnlyResourceDefinition;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.SimpleAttributeDefinition;
import org.jboss.as.controller.operations.validation.EnumValidator;
import org.jboss.as.controller.registry.Resource;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;

/**
 * Address setting resource definition
 *
 * @author <a href="http://jmesnil.net">Jeff Mesnil</a> (c) 2012 Red Hat Inc.
 */
public class AddressSettingDefinition extends ModelOnlyResourceDefinition {

    public static final PathElement PATH = PathElement.pathElement(CommonAttributes.ADDRESS_SETTING);

    public static final SimpleAttributeDefinition ADDRESS_FULL_MESSAGE_POLICY = create("address-full-policy", ModelType.STRING)
            .setDefaultValue(new ModelNode(AddressFullMessagePolicy.PAGE.toString()))
            .setValidator(new EnumValidator<>(AddressFullMessagePolicy.class, true, false))
            .setRequired(false)
            .setAllowExpression(true)
            .build();

    public static final SimpleAttributeDefinition EXPIRY_DELAY = create("expiry-delay", ModelType.LONG)
            .setDefaultValue(new ModelNode(-1L))
            .setMeasurementUnit(MILLISECONDS)
            .setRequired(false)
            .setAllowExpression(true)
            .build();

    public static final SimpleAttributeDefinition LAST_VALUE_QUEUE = create("last-value-queue", ModelType.BOOLEAN)
            .setDefaultValue(new ModelNode(false))
            .setRequired(false)
            .setAllowExpression(true)
            .build();

    public static final SimpleAttributeDefinition MAX_DELIVERY_ATTEMPTS = create("max-delivery-attempts", ModelType.INT)
            .setDefaultValue(new ModelNode(10))
            .setRequired(false)
            .setAllowExpression(true)
            .build();

    public static final SimpleAttributeDefinition MAX_REDELIVERY_DELAY = create("max-redelivery-delay", ModelType.LONG)
            .setDefaultValue(new ModelNode(0L))
            .setMeasurementUnit(MILLISECONDS)
            .setRequired(false)
            .setAllowExpression(true)
            .build();

    public static final SimpleAttributeDefinition MAX_SIZE_BYTES = create("max-size-bytes", ModelType.LONG)
            .setDefaultValue(new ModelNode(-1L))
            .setMeasurementUnit(BYTES)
            .setRequired(false)
            .setAllowExpression(true)
            .build();

    public static final SimpleAttributeDefinition MESSAGE_COUNTER_HISTORY_DAY_LIMIT = create("message-counter-history-day-limit", ModelType.INT)
            .setDefaultValue(new ModelNode(0))
            .setMeasurementUnit(DAYS)
            .setRequired(false)
            .setAllowExpression(true)
            .build();

    public static final SimpleAttributeDefinition PAGE_MAX_CACHE_SIZE = create("page-max-cache-size", ModelType.INT)
            .setDefaultValue(new ModelNode(5))
            .setRequired(false)
            .setAllowExpression(true)
            .build();

    public static final SimpleAttributeDefinition PAGE_SIZE_BYTES = create("page-size-bytes", ModelType.LONG)
            .setDefaultValue(new ModelNode(10485760L))
            .setMeasurementUnit(BYTES)
            .setRequired(false)
            .setAllowExpression(true)
            .build();

    public static final SimpleAttributeDefinition REDELIVERY_DELAY = create("redelivery-delay", ModelType.LONG)
            .setDefaultValue(new ModelNode(0L))
            .setMeasurementUnit(MILLISECONDS)
            .setRequired(false)
            .setAllowExpression(true)
            .build();

    public static final SimpleAttributeDefinition REDELIVERY_MULTIPLIER = create("redelivery-multiplier", ModelType.DOUBLE)
            .setDefaultValue(new ModelNode(1.0))
            .setRequired(false)
            .setAllowExpression(true)
            .build();

    public static final SimpleAttributeDefinition REDISTRIBUTION_DELAY = create("redistribution-delay", ModelType.LONG)
            .setDefaultValue(new ModelNode(-1L))
            .setMeasurementUnit(MILLISECONDS)
            .setRequired(false)
            .setAllowExpression(true)
            .build();

    public static final SimpleAttributeDefinition SEND_TO_DLA_ON_NO_ROUTE = create("send-to-dla-on-no-route", ModelType.BOOLEAN)
            .setDefaultValue(new ModelNode(false))
            .setRequired(false)
            .setAllowExpression(true)
            .build();

    public static final SimpleAttributeDefinition SLOW_CONSUMER_CHECK_PERIOD = create("slow-consumer-check-period", ModelType.LONG)
            .setDefaultValue(new ModelNode(-1L))
            .setMeasurementUnit(MINUTES)
            .setRequired(false)
            .setAllowExpression(true)
            .build();

    public static final SimpleAttributeDefinition SLOW_CONSUMER_POLICY = create("slow-consumer-policy", ModelType.STRING)
            .setDefaultValue(new ModelNode(SlowConsumerPolicy.NOTIFY.toString()))
            .setValidator(new EnumValidator<>(SlowConsumerPolicy.class, true, true))
            .setRequired(false)
            .setAllowExpression(true)
            .build();

    public static final SimpleAttributeDefinition SLOW_CONSUMER_THRESHOLD = create("slow-consumer-threshold", ModelType.LONG)
            .setDefaultValue(new ModelNode(5L))
            .setRequired(false)
            .setAllowExpression(true)
            .build();

    public static final AttributeDefinition[] ATTRIBUTES_WITH_EXPRESSION_ALLOWED_IN_1_2_0 = new AttributeDefinition[]{ DEAD_LETTER_ADDRESS,
            EXPIRY_ADDRESS, REDELIVERY_DELAY, MAX_DELIVERY_ATTEMPTS, MAX_SIZE_BYTES,
            PAGE_SIZE_BYTES, PAGE_MAX_CACHE_SIZE, ADDRESS_FULL_MESSAGE_POLICY, MESSAGE_COUNTER_HISTORY_DAY_LIMIT,
            LAST_VALUE_QUEUE, REDISTRIBUTION_DELAY, SEND_TO_DLA_ON_NO_ROUTE
    };

    /**
     * Attributes are defined in the <em>same order than in the XSD schema</em>
     */
    static final SimpleAttributeDefinition[] ATTRIBUTES = new SimpleAttributeDefinition[] {
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
        SLOW_CONSUMER_THRESHOLD
    };

    static final AddressSettingDefinition INSTANCE = new AddressSettingDefinition();

    public AddressSettingDefinition() {
        super(PATH,
                MessagingExtension.getResourceDescriptionResolver(CommonAttributes.ADDRESS_SETTING),
                new ModelOnlyAddStepHandler(ATTRIBUTES) {
                    @Override
                    protected void populateModel(OperationContext context, ModelNode operation, Resource resource) throws OperationFailedException {
                        super.populateModel(context, operation, resource);
                        context.addStep(AddressSettingsValidator.ADD_VALIDATOR, OperationContext.Stage.MODEL);
                    }
                },
                ATTRIBUTES);
        setDeprecated(MessagingExtension.DEPRECATED_SINCE);
    }

    // copied from HornetQ to avoid import HornetQ artifacts just to define attribute constants and enum validator
    private enum AddressFullMessagePolicy
    {
        DROP, PAGE, BLOCK, FAIL;
    }

    private enum SlowConsumerPolicy
    {
        KILL, NOTIFY;
    }
}
