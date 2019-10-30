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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.ModelOnlyResourceDefinition;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.PropertiesAttributeDefinition;
import org.jboss.as.controller.SimpleAttributeDefinition;
import org.jboss.as.controller.access.management.SensitiveTargetAccessConstraintDefinition;
import org.jboss.as.controller.operations.validation.EnumValidator;
import org.jboss.as.controller.operations.validation.IntRangeValidator;
import org.jboss.as.messaging.AttributeMarshallers;
import org.jboss.as.messaging.CommonAttributes;
import org.jboss.as.messaging.MessagingExtension;
import org.jboss.dmr.ModelNode;

/**
 * @author Jeff Mesnil (c) 2012 Red Hat Inc.
 */
public class JMSBridgeDefinition extends ModelOnlyResourceDefinition {

    public static final PathElement PATH = PathElement.pathElement(CommonAttributes.JMS_BRIDGE);

    public static final String SOURCE = "source";
    public static final String TARGET = "target";
    public static final String CONTEXT = "context";

    public static final SimpleAttributeDefinition MODULE = create("module", STRING)
            .setRequired(false)
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
            .setRequired(false)
            .setAllowExpression(true)
            .setXmlName("user")
            .addAccessConstraint(SensitiveTargetAccessConstraintDefinition.CREDENTIAL)
            .addAccessConstraint(MESSAGING_SECURITY_DEF)
            .build();
    public static final SimpleAttributeDefinition SOURCE_PASSWORD = create("source-password", STRING)
            .setRequired(false)
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
            .setRequired(false)
            .setAllowExpression(true)
            .setXmlName("user")
            .addAccessConstraint(SensitiveTargetAccessConstraintDefinition.CREDENTIAL)
            .addAccessConstraint(MESSAGING_SECURITY_DEF)
            .build();

    public static final SimpleAttributeDefinition TARGET_PASSWORD = create("target-password", STRING)
            .setRequired(false)
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
            .setValidator(new EnumValidator<>(QualityOfServiceMode.class, false, false))
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
            .setRequired(false)
            .setAllowExpression(true)
            .build();
    public static final SimpleAttributeDefinition CLIENT_ID = create("client-id", STRING)
            .setRequired(false)
            .setAllowExpression(true)
            .build();
    public static final SimpleAttributeDefinition ADD_MESSAGE_ID_IN_HEADER = create("add-messageID-in-header", BOOLEAN)
            .setRequired(false)
            .setDefaultValue(new ModelNode().set(false))
            .setAllowExpression(true)
            .build();

    public static final AttributeDefinition[] JMS_BRIDGE_ATTRIBUTES = {
            MODULE,
            QUALITY_OF_SERVICE,
            FAILURE_RETRY_INTERVAL, MAX_RETRIES,
            MAX_BATCH_SIZE, MAX_BATCH_TIME,
            CommonAttributes.SELECTOR,
            SUBSCRIPTION_NAME, CLIENT_ID,
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

    private static AttributeDefinition[] getAllAttributes() {
        List<AttributeDefinition> allAttributes = new ArrayList<>();
        allAttributes.addAll(Arrays.asList(JMS_BRIDGE_ATTRIBUTES));
        allAttributes.addAll(Arrays.asList(JMS_SOURCE_ATTRIBUTES));
        allAttributes.addAll(Arrays.asList(JMS_TARGET_ATTRIBUTES));
        return allAttributes.toArray(new AttributeDefinition[allAttributes.size()]);
    }

    public static final JMSBridgeDefinition INSTANCE = new JMSBridgeDefinition();

    private JMSBridgeDefinition() {
        super(PATH,
                MessagingExtension.getResourceDescriptionResolver(CommonAttributes.JMS_BRIDGE),
                getAllAttributes());
        setDeprecated(MessagingExtension.DEPRECATED_SINCE);
    }

    private enum QualityOfServiceMode {
        AT_MOST_ONCE(0),
        DUPLICATES_OK(1),
        ONCE_AND_ONLY_ONCE(2);

        private final int value;

        QualityOfServiceMode(int value) {
            this.value = value;
        }
    }
}
