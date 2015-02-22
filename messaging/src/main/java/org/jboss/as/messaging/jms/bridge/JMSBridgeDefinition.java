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
import static org.jboss.as.messaging.CommonAttributes.MESSAGING_SECURITY_DEF;
import static org.jboss.dmr.ModelType.BOOLEAN;
import static org.jboss.dmr.ModelType.INT;
import static org.jboss.dmr.ModelType.LONG;
import static org.jboss.dmr.ModelType.STRING;

import org.hornetq.jms.bridge.QualityOfServiceMode;
import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.PropertiesAttributeDefinition;
import org.jboss.as.controller.SimpleAttributeDefinition;
import org.jboss.as.controller.SimpleOperationDefinition;
import org.jboss.as.controller.SimpleResourceDefinition;
import org.jboss.as.controller.access.management.SensitiveTargetAccessConstraintDefinition;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.controller.operations.validation.EnumValidator;
import org.jboss.as.controller.operations.validation.IntRangeValidator;
import org.jboss.as.controller.registry.AttributeAccess;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.as.messaging.AttributeMarshallers;
import org.jboss.as.messaging.CommonAttributes;
import org.jboss.as.messaging.MessagingExtension;
import org.jboss.dmr.ModelNode;

/**
 * @author Jeff Mesnil (c) 2012 Red Hat Inc.
 */
public class JMSBridgeDefinition extends SimpleResourceDefinition {

    public static final PathElement PATH = PathElement.pathElement(CommonAttributes.JMS_BRIDGE);

    public static final JMSBridgeDefinition INSTANCE = new JMSBridgeDefinition();

    public static final String SOURCE = "source";
    public static final String TARGET = "target";
    public static final String CONTEXT = "context";

    public static final String PAUSE = "pause";
    public static final String RESUME = "resume";

    public static final SimpleAttributeDefinition MODULE = create("module", STRING)
            .setAllowNull(true)
            .build();

    public static final SimpleAttributeDefinition SOURCE_CONNECTION_FACTORY = create("source-connection-factory", STRING)
            .setXmlName(CommonAttributes.CONNECTION_FACTORY)
            .setAttributeMarshaller(AttributeMarshallers.JNDI_RESOURCE_MARSHALLER)
            .build();

    public static final SimpleAttributeDefinition SOURCE_DESTINATION = create("source-destination", STRING)
            .setXmlName(CommonAttributes.DESTINATION)
            .setAttributeMarshaller(AttributeMarshallers.JNDI_RESOURCE_MARSHALLER)
            .build();

    public static final SimpleAttributeDefinition SOURCE_USER = create("source-user", STRING)
            .setAllowNull(true)
            .setAllowExpression(true)
            .setXmlName("user")
            .addAccessConstraint(SensitiveTargetAccessConstraintDefinition.CREDENTIAL)
            .addAccessConstraint(MESSAGING_SECURITY_DEF)
            .build();
    public static final SimpleAttributeDefinition SOURCE_PASSWORD = create("source-password", STRING)
            .setAllowNull(true)
            .setAllowExpression(true)
            .setXmlName("password")
            .addAccessConstraint(SensitiveTargetAccessConstraintDefinition.CREDENTIAL)
            .addAccessConstraint(MESSAGING_SECURITY_DEF)
            .build();

    public static final PropertiesAttributeDefinition SOURCE_CONTEXT = new PropertiesAttributeDefinition.Builder("source-context", true)
            .setXmlName(CONTEXT)
            .setAllowExpression(true)
            .setAttributeMarshaller(AttributeMarshallers.JNDI_CONTEXT_MARSHALLER)
            .build();

    public static final SimpleAttributeDefinition TARGET_CONNECTION_FACTORY = create("target-connection-factory", STRING)
            .setXmlName(CommonAttributes.CONNECTION_FACTORY)
            .setAttributeMarshaller(AttributeMarshallers.JNDI_RESOURCE_MARSHALLER)
            .build();

    public static final SimpleAttributeDefinition TARGET_DESTINATION = create("target-destination", STRING)
            .setXmlName(CommonAttributes.DESTINATION)
            .setAttributeMarshaller(AttributeMarshallers.JNDI_RESOURCE_MARSHALLER)
            .build();

    public static final SimpleAttributeDefinition TARGET_USER = create("target-user", STRING)
            .setAllowNull(true)
            .setAllowExpression(true)
            .setXmlName("user")
            .addAccessConstraint(SensitiveTargetAccessConstraintDefinition.CREDENTIAL)
            .addAccessConstraint(MESSAGING_SECURITY_DEF)
            .build();

    public static final SimpleAttributeDefinition TARGET_PASSWORD = create("target-password", STRING)
            .setAllowNull(true)
            .setAllowExpression(true)
            .setXmlName("password")
            .addAccessConstraint(SensitiveTargetAccessConstraintDefinition.CREDENTIAL)
            .addAccessConstraint(MESSAGING_SECURITY_DEF)
            .build();

    public static final PropertiesAttributeDefinition TARGET_CONTEXT = new PropertiesAttributeDefinition.Builder("target-context", true)
            .setXmlName(CONTEXT)
            .setAllowExpression(true)
            .setAttributeMarshaller(AttributeMarshallers.JNDI_CONTEXT_MARSHALLER)
            .build();

    public static final SimpleAttributeDefinition QUALITY_OF_SERVICE = create("quality-of-service", STRING)
            .setValidator(new EnumValidator<QualityOfServiceMode>(QualityOfServiceMode.class, false, false))
            .setAllowExpression(true)
            .build();
    public static final SimpleAttributeDefinition FAILURE_RETRY_INTERVAL = create("failure-retry-interval", LONG)
            .setMeasurementUnit(MILLISECONDS)
            .setValidator(InfiniteOrPositiveValidators.LONG_INSTANCE)
            .setAllowExpression(true)
            .build();
    public static final SimpleAttributeDefinition MAX_RETRIES = create("max-retries", INT)
            .setValidator(InfiniteOrPositiveValidators.INT_INSTANCE)
            .setAllowExpression(true)
            .build();
    public static final SimpleAttributeDefinition MAX_BATCH_SIZE = create("max-batch-size", INT)
            .setValidator(new IntRangeValidator(0, Integer.MAX_VALUE, false, false))
            .setAllowExpression(true)
            .build();
    public static final SimpleAttributeDefinition MAX_BATCH_TIME = create("max-batch-time", LONG)
            .setMeasurementUnit(MILLISECONDS)
            .setValidator(InfiniteOrPositiveValidators.LONG_INSTANCE)
            .setAllowExpression(true)
            .build();
    public static final SimpleAttributeDefinition SUBSCRIPTION_NAME = create("subscription-name", STRING)
            .setAllowNull(true)
            .setAllowExpression(true)
            .build();
    public static final SimpleAttributeDefinition CLIENT_ID = create("client-id", STRING)
            .setAllowNull(true)
            .setAllowExpression(true)
            .build();
    public static final SimpleAttributeDefinition ADD_MESSAGE_ID_IN_HEADER = create("add-messageID-in-header", BOOLEAN)
            .setAllowNull(true)
            .setDefaultValue(new ModelNode().set(false))
            .setAllowExpression(true)
            .build();
    public static final SimpleAttributeDefinition STARTED = create(CommonAttributes.STARTED, BOOLEAN)
            .setFlags(AttributeAccess.Flag.STORAGE_RUNTIME)
            .build();

    public static final AttributeDefinition[] JMS_BRIDGE_ATTRIBUTES = {
            MODULE,
            QUALITY_OF_SERVICE,
            FAILURE_RETRY_INTERVAL, MAX_RETRIES,
            MAX_BATCH_SIZE, MAX_BATCH_TIME,
            CommonAttributes.SELECTOR,
            SUBSCRIPTION_NAME, CommonAttributes.CLIENT_ID,
            ADD_MESSAGE_ID_IN_HEADER
    };

    public static final AttributeDefinition[] JMS_SOURCE_ATTRIBUTES = {
            SOURCE_CONNECTION_FACTORY, SOURCE_DESTINATION,
            SOURCE_USER, SOURCE_PASSWORD,
            SOURCE_CONTEXT
    };

    public static final AttributeDefinition[] JMS_TARGET_ATTRIBUTES = {
            TARGET_CONNECTION_FACTORY, TARGET_DESTINATION,
            TARGET_USER, TARGET_PASSWORD,
            TARGET_CONTEXT
    };

    public static final AttributeDefinition[] READONLY_ATTRIBUTES = {
            STARTED, CommonAttributes.PAUSED
    };

    public static final String[] OPERATIONS = {
            ModelDescriptionConstants.START, ModelDescriptionConstants.STOP,
            PAUSE, RESUME
    };

    public JMSBridgeDefinition() {
        super(PATH,
                MessagingExtension.getResourceDescriptionResolver(CommonAttributes.JMS_BRIDGE),
                JMSBridgeAdd.INSTANCE,
                JMSBridgeRemove.INSTANCE);
        setDeprecated(MessagingExtension.DEPRECATED_SINCE);
    }

    @Override
    public void registerAttributes(ManagementResourceRegistration registry) {
        super.registerAttributes(registry);
        for (AttributeDefinition attr : JMS_BRIDGE_ATTRIBUTES) {
            registry.registerReadWriteAttribute(attr, null, JMSBridgeWriteAttributeHandler.INSTANCE);
        }
        for (AttributeDefinition attr : JMS_SOURCE_ATTRIBUTES) {
            registry.registerReadWriteAttribute(attr, null, JMSBridgeWriteAttributeHandler.INSTANCE);
        }
        for (AttributeDefinition attr : JMS_TARGET_ATTRIBUTES) {
            registry.registerReadWriteAttribute(attr, null, JMSBridgeWriteAttributeHandler.INSTANCE);
        }
        for (AttributeDefinition attr : READONLY_ATTRIBUTES) {
            registry.registerReadOnlyAttribute(attr, JMSBridgeHandler.INSTANCE);
        }
    }

    @Override
    public void registerOperations(ManagementResourceRegistration registry) {
        super.registerOperations(registry);
        for (final String operationName : OPERATIONS) {
            registry.registerOperationHandler(new SimpleOperationDefinition(operationName, getResourceDescriptionResolver()), JMSBridgeHandler.INSTANCE);
        }
    }

}
