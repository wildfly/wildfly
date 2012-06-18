/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2010, Red Hat, Inc., and individual contributors
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
package org.jboss.as.messaging.jms;

import static org.jboss.as.messaging.MessagingMessages.MESSAGES;

import org.jboss.as.controller.AttributeDefinition;

/**
 * A wrapper for pooled CF attributes with additional parameters required
 * to setup the HornetQ RA.
 *
 * @author <a href="http://jmesnil.net/">Jeff Mesnil</a> (c) 2012 Red Hat Inc.
 */
public class PooledConnectionFactoryAttribute {
        private final AttributeDefinition attributeDefinition;
        private String propertyName;
        private final boolean resourceAdapterProperty;

        public static PooledConnectionFactoryAttribute create(final AttributeDefinition attributeDefinition, final String propertyName, boolean resourceAdapterProperty) {
            return new PooledConnectionFactoryAttribute(attributeDefinition, propertyName, resourceAdapterProperty);
        }

        public static AttributeDefinition[] getDefinitions(final PooledConnectionFactoryAttribute... attrs) {
            AttributeDefinition[] definitions = new AttributeDefinition[attrs.length];
            for (int i = 0; i < attrs.length; i++) {
                PooledConnectionFactoryAttribute attr = attrs[i];
                definitions[i] = attr.getDefinition();
            }
            return definitions;
        }

        private PooledConnectionFactoryAttribute(final AttributeDefinition attributeDefinition, final String propertyName, boolean resourceAdapterProperty) {
            this.attributeDefinition = attributeDefinition;
            this.propertyName = propertyName;
            this.resourceAdapterProperty = resourceAdapterProperty;
        }

        public String getClassType() {
            switch (attributeDefinition.getType()) {
                case BOOLEAN:
                    return Boolean.class.getName();
                case BIG_DECIMAL:
                    return Double.class.getName();
                case LONG:
                    return Long.class.getName();
                case INT:
                    return Integer.class.getName();
                case STRING:
                    return String.class.getName();
                default:
                    throw MESSAGES.invalidAttributeType(attributeDefinition.getName(), attributeDefinition.getType());

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
    }