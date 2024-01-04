/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.extension.messaging.activemq.jms;

import org.jboss.as.controller.AttributeDefinition;
import org.wildfly.extension.messaging.activemq._private.MessagingLogger;

/**
 * A wrapper for pooled CF attributes with additional parameters required
 * to setup the ActiveMQ RA.
 *
 * @author <a href="http://jmesnil.net/">Jeff Mesnil</a> (c) 2012 Red Hat Inc.
 */
public class ConnectionFactoryAttribute {
    enum ConfigType {
        INBOUND, OUTBOUND;
    }

    private final AttributeDefinition attributeDefinition;
    private String propertyName;
    private final boolean resourceAdapterProperty;
    private ConfigType configType;

    public static ConnectionFactoryAttribute create(final AttributeDefinition attributeDefinition, final String propertyName, boolean resourceAdapterProperty) {
        return new ConnectionFactoryAttribute(attributeDefinition, propertyName, resourceAdapterProperty, null);
    }

    public static ConnectionFactoryAttribute create(final AttributeDefinition attributeDefinition, final String propertyName, boolean resourceAdapterProperty, ConfigType inboundConfig) {
        return new ConnectionFactoryAttribute(attributeDefinition, propertyName, resourceAdapterProperty, inboundConfig);
    }


    public static AttributeDefinition[] getDefinitions(final ConnectionFactoryAttribute... attrs) {
        AttributeDefinition[] definitions = new AttributeDefinition[attrs.length];
        for (int i = 0; i < attrs.length; i++) {
            ConnectionFactoryAttribute attr = attrs[i];
            definitions[i] = attr.getDefinition();
        }
        return definitions;
    }

    private ConnectionFactoryAttribute(final AttributeDefinition attributeDefinition, final String propertyName, boolean resourceAdapterProperty, ConfigType configType) {
        this.attributeDefinition = attributeDefinition;
        this.propertyName = propertyName;
        this.resourceAdapterProperty = resourceAdapterProperty;
        this.configType = configType;
    }

    public String getClassType() {
        switch (attributeDefinition.getType()) {
            case BOOLEAN:
                return Boolean.class.getName();
            case DOUBLE:
            case BIG_DECIMAL:
                return Double.class.getName();
            case LONG:
                return Long.class.getName();
            case INT:
                return Integer.class.getName();
            case STRING:
            case  LIST:
                return String.class.getName();
            default:
                throw MessagingLogger.ROOT_LOGGER.invalidAttributeType(attributeDefinition.getName(), attributeDefinition.getType());

        }
    }

    public String getPropertyName() {
        return propertyName;
    }

    public AttributeDefinition getDefinition() {
        return attributeDefinition;
    }

    public boolean isResourceAdapterProperty() {
        return resourceAdapterProperty;
    }

    public ConfigType getConfigType() {
        return configType;
    }
}