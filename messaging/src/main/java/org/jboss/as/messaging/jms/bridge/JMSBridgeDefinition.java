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

package org.jboss.as.messaging.jms.bridge;

import static org.jboss.as.controller.SimpleAttributeDefinitionBuilder.create;
import static org.jboss.as.controller.client.helpers.MeasurementUnit.MILLISECONDS;
import static org.jboss.as.messaging.CommonAttributes.JMS_BRIDGE;
import static org.jboss.dmr.ModelType.BOOLEAN;
import static org.jboss.dmr.ModelType.INT;
import static org.jboss.dmr.ModelType.LONG;
import static org.jboss.dmr.ModelType.STRING;

import java.util.Locale;

import org.hornetq.jms.bridge.QualityOfServiceMode;
import org.jboss.as.controller.SimpleAttributeDefinition;
import org.jboss.as.controller.SimpleResourceDefinition;
import org.jboss.as.controller.descriptions.DescriptionProvider;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.controller.operations.validation.EnumValidator;
import org.jboss.as.controller.operations.validation.IntRangeValidator;
import org.jboss.as.controller.registry.AttributeAccess;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.as.messaging.CommonAttributes;
import org.jboss.as.messaging.MessagingDescriptions;
import org.jboss.as.messaging.MessagingExtension;
import org.jboss.as.messaging.jms.SelectorAttribute;
import org.jboss.dmr.ModelNode;

/**
 * @author Jeff Mesnil (c) 2012 Red Hat Inc.
 */
public class JMSBridgeDefinition extends SimpleResourceDefinition {

    public static final JMSBridgeDefinition INSTANCE = new JMSBridgeDefinition();

    public static final String SOURCE = "source";
    public static final String TARGET = "target";
    public static final String CONTEXT = "context";

    public static final String PAUSE = "pause";
    public static final String RESUME = "resume";

    public static final SimpleAttributeDefinition MODULE = create("module", STRING)
            .setAllowNull(true).
            build();
    public static final SimpleAttributeDefinition SOURCE_CONNECTION_FACTORY = new JNDIResourceAttributeDefinition("source-connection-factory", CommonAttributes.CONNECTION_FACTORY);
    public static final SimpleAttributeDefinition SOURCE_DESTINATION = new JNDIResourceAttributeDefinition("source-destination", CommonAttributes.DESTINATION);
    public static final SimpleAttributeDefinition SOURCE_USER = create("source-user", STRING)
            .setAllowNull(true)
            .setAllowExpression(true)
            .setXmlName("user")
            .build();
    public static final SimpleAttributeDefinition SOURCE_PASSWORD = create("source-password", STRING)
            .setAllowNull(true)
            .setAllowExpression(true)
            .setXmlName("password")
            .build();
    public static final SimpleAttributeDefinition SOURCE_CONTEXT = new JNDIContextAttributeDefinition("source-context", CONTEXT);

    public static final SimpleAttributeDefinition TARGET_CONNECTION_FACTORY = new JNDIResourceAttributeDefinition("target-connection-factory", CommonAttributes.CONNECTION_FACTORY);
    public static final SimpleAttributeDefinition TARGET_DESTINATION = new JNDIResourceAttributeDefinition("target-destination", CommonAttributes.DESTINATION);
    public static final SimpleAttributeDefinition TARGET_USER = create("target-user", STRING)
            .setAllowNull(true)
            .setAllowExpression(true)
            .setXmlName("user")
            .build();
    public static final SimpleAttributeDefinition TARGET_PASSWORD = create("target-password", STRING)
            .setAllowNull(true)
            .setAllowExpression(true)
            .setXmlName("password")
            .build();
    public static final SimpleAttributeDefinition TARGET_CONTEXT = new JNDIContextAttributeDefinition("target-context", CONTEXT);

    public static final SimpleAttributeDefinition QUALITY_OF_SERVICE = create("quality-of-service", STRING)
            .setValidator(new EnumValidator<QualityOfServiceMode>(QualityOfServiceMode.class, false, false))
            .build();
    public static final SimpleAttributeDefinition FAILURE_RETRY_INTERVAL = create("failure-retry-interval", LONG)
            .setMeasurementUnit(MILLISECONDS)
            .setValidator(InfiniteOrPositiveValidators.LONG_INSTANCE)
            .build();
    public static final SimpleAttributeDefinition MAX_RETRIES = create("max-retries", INT)
            .setValidator(InfiniteOrPositiveValidators.INT_INSTANCE)
            .build();
    public static final SimpleAttributeDefinition MAX_BATCH_SIZE = create("max-batch-size", INT)
            .setValidator(new IntRangeValidator(0, Integer.MAX_VALUE, false, false))
            .build();
    public static final SimpleAttributeDefinition MAX_BATCH_TIME = create("max-batch-time", LONG)
            .setMeasurementUnit(MILLISECONDS)
            .setValidator(InfiniteOrPositiveValidators.LONG_INSTANCE)
            .build();
    public static final SimpleAttributeDefinition SUBSCRIPTION_NAME = create("subscription-name", STRING)
            .setAllowNull(true)
            .build();
    public static final SimpleAttributeDefinition CLIENT_ID = create("client-id", STRING)
            .setAllowNull(true)
            .build();
    public static final SimpleAttributeDefinition ADD_MESSAGE_ID_IN_HEADER = create("add-messageID-in-header", BOOLEAN)
            .setAllowNull(true)
            .setDefaultValue(new ModelNode().set(false))
            .build();
    public static final SimpleAttributeDefinition STARTED = create(CommonAttributes.STARTED, BOOLEAN)
            .setFlags(AttributeAccess.Flag.STORAGE_RUNTIME)
            .build();
    public static final SimpleAttributeDefinition PAUSED = create(CommonAttributes.PAUSED, BOOLEAN)
            .setFlags(AttributeAccess.Flag.STORAGE_RUNTIME)
            .build();

    public static final SimpleAttributeDefinition[] JMS_BRIDGE_ATTRIBUTES = {
        MODULE,
        SelectorAttribute.SELECTOR,
        QUALITY_OF_SERVICE,
        FAILURE_RETRY_INTERVAL, MAX_RETRIES,
        MAX_BATCH_SIZE, MAX_BATCH_TIME,
        SUBSCRIPTION_NAME, CommonAttributes.CLIENT_ID,
        ADD_MESSAGE_ID_IN_HEADER
    };

    public static final SimpleAttributeDefinition[] JMS_SOURCE_ATTRIBUTES = {
        SOURCE_CONNECTION_FACTORY, SOURCE_DESTINATION,
        SOURCE_USER, SOURCE_PASSWORD,
        SOURCE_CONTEXT
    };

    public static final SimpleAttributeDefinition[] JMS_TARGET_ATTRIBUTES = {
        TARGET_CONNECTION_FACTORY, TARGET_DESTINATION,
        TARGET_USER, TARGET_PASSWORD,
        TARGET_CONTEXT
    };

    public static final SimpleAttributeDefinition[] READONLY_ATTRIBUTES = {
        STARTED, PAUSED
    };

    public static final String[] OPERATIONS = {
        ModelDescriptionConstants.START, ModelDescriptionConstants.STOP,
        PAUSE, RESUME
    };

    public JMSBridgeDefinition() {
        super(MessagingExtension.JMS_BRIDGE_PATH,
                MessagingExtension.getResourceDescriptionResolver(CommonAttributes.JMS_BRIDGE),
                JMSBridgeAdd.INSTANCE,
                JMSBridgeRemove.INSTANCE);
    }

    @Override
    public void registerAttributes(ManagementResourceRegistration registry) {
        super.registerAttributes(registry);
        for (SimpleAttributeDefinition attr : JMS_BRIDGE_ATTRIBUTES) {
            registry.registerReadWriteAttribute(attr, null, JMSBridgeWriteAttributeHandler.INSTANCE);
        }
        for (SimpleAttributeDefinition attr : JMS_SOURCE_ATTRIBUTES) {
            registry.registerReadWriteAttribute(attr, null, JMSBridgeWriteAttributeHandler.INSTANCE);
        }
        for (SimpleAttributeDefinition attr : JMS_TARGET_ATTRIBUTES) {
            registry.registerReadWriteAttribute(attr, null, JMSBridgeWriteAttributeHandler.INSTANCE);
        }
        for (SimpleAttributeDefinition attr : READONLY_ATTRIBUTES) {
            registry.registerReadOnlyAttribute(attr, JMSBridgeHandler.INSTANCE);
        }
    }

    @Override
    public void registerOperations(ManagementResourceRegistration registry) {
        super.registerOperations(registry);
        for (final String operationName: OPERATIONS) {
            registry.registerOperationHandler(operationName, JMSBridgeHandler.INSTANCE, new DescriptionProvider() {
                @Override
                public ModelNode getModelDescription(Locale locale) {
                    return MessagingDescriptions.getDescriptionOnlyOperation(locale, operationName, JMS_BRIDGE);
                }
            });
        }
    }

}
