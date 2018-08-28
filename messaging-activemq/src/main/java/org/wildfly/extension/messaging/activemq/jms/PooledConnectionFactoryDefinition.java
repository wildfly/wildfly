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

package org.wildfly.extension.messaging.activemq.jms;

import static java.lang.System.arraycopy;
import static org.wildfly.extension.messaging.activemq.jms.ConnectionFactoryAttribute.getDefinitions;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.jboss.as.controller.AbstractAttributeDefinitionBuilder;
import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.PersistentResourceDefinition;
import org.jboss.as.controller.PrimitiveListAttributeDefinition;
import org.jboss.as.controller.ReloadRequiredWriteAttributeHandler;
import org.jboss.as.controller.SimpleAttributeDefinition;
import org.jboss.as.controller.SimpleAttributeDefinitionBuilder;
import org.jboss.as.controller.SimpleListAttributeDefinition;
import org.jboss.as.controller.SimpleMapAttributeDefinition;
import org.jboss.as.controller.registry.AttributeAccess;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.wildfly.extension.messaging.activemq.CommonAttributes;
import org.wildfly.extension.messaging.activemq.MessagingExtension;
import org.wildfly.extension.messaging.activemq.jms.ConnectionFactoryAttributes.Common;
import org.wildfly.extension.messaging.activemq.jms.ConnectionFactoryAttributes.Pooled;

/**
 * JMS pooled Connection Factory resource definition.
 *
 * @author <a href="http://jmesnil.net">Jeff Mesnil</a> (c) 2012 Red Hat Inc.
 */
public class PooledConnectionFactoryDefinition extends PersistentResourceDefinition {

    // the generation of the Pooled CF attributes is a bit ugly but it is with purpose:
    // * factorize the attributes which are common between the regular CF and the pooled CF
    // * keep in a single place the subtle differences (e.g. different default values for reconnect-attempts between
    //   the regular and pooled CF
    private static ConnectionFactoryAttribute[] define(ConnectionFactoryAttribute[] specific, ConnectionFactoryAttribute... common) {
        int size = common.length + specific.length;
        ConnectionFactoryAttribute[] result = new ConnectionFactoryAttribute[size];
        arraycopy(specific, 0, result, 0, specific.length);
        for (int i = 0; i < common.length; i++) {
            ConnectionFactoryAttribute attr = common[i];
            AttributeDefinition definition = attr.getDefinition();

            ConnectionFactoryAttribute newAttr;
            // replace the reconnect-attempts attribute to use a different default value for pooled CF
            if (definition == Common.RECONNECT_ATTEMPTS) {
                AttributeDefinition copy = copy(Pooled.RECONNECT_ATTEMPTS, AttributeAccess.Flag.RESTART_ALL_SERVICES);
                newAttr = ConnectionFactoryAttribute.create(copy, Pooled.RECONNECT_ATTEMPTS_PROP_NAME, true);
            } else {
                AttributeDefinition copy = copy(definition, AttributeAccess.Flag.RESTART_ALL_SERVICES);
                newAttr = ConnectionFactoryAttribute.create(copy, attr.getPropertyName(), attr.isResourceAdapterProperty(), attr.getConfigType());
            }
            result[specific.length + i] = newAttr;
        }
        return result;
    }

    private static AttributeDefinition copy(AttributeDefinition attribute, AttributeAccess.Flag flag) {
        AbstractAttributeDefinitionBuilder builder;
        if (attribute instanceof  SimpleListAttributeDefinition) {
            builder =  new SimpleListAttributeDefinition.Builder((SimpleListAttributeDefinition)attribute);
        } else if (attribute instanceof  SimpleMapAttributeDefinition) {
            builder = new SimpleMapAttributeDefinition.Builder((SimpleMapAttributeDefinition)attribute);
        } else if (attribute instanceof PrimitiveListAttributeDefinition) {
            builder = new PrimitiveListAttributeDefinition.Builder((PrimitiveListAttributeDefinition)attribute);
        } else {
            builder = new SimpleAttributeDefinitionBuilder((SimpleAttributeDefinition)attribute);
        }
        builder.setFlags(flag);
        return builder.build();
    }

    public static final ConnectionFactoryAttribute[] ATTRIBUTES = define(Pooled.ATTRIBUTES, Common.ATTRIBUTES);

    public static Map<String, ConnectionFactoryAttribute> getAttributesMap() {
        Map<String, ConnectionFactoryAttribute> attrs = new HashMap<String, ConnectionFactoryAttribute>(ATTRIBUTES.length);
        for (ConnectionFactoryAttribute attribute : ATTRIBUTES) {
            attrs.put(attribute.getDefinition().getName(), attribute);
        }
        return attrs;
    }

    private final boolean deployed;

    public static final PooledConnectionFactoryDefinition INSTANCE = new PooledConnectionFactoryDefinition(false);

    public static final PooledConnectionFactoryDefinition DEPLOYMENT_INSTANCE = new PooledConnectionFactoryDefinition(true);

    public PooledConnectionFactoryDefinition(final boolean deployed) {
        super(MessagingExtension.POOLED_CONNECTION_FACTORY_PATH, MessagingExtension.getResourceDescriptionResolver(CommonAttributes.POOLED_CONNECTION_FACTORY),
                PooledConnectionFactoryAdd.INSTANCE,
                PooledConnectionFactoryRemove.INSTANCE);
        this.deployed = deployed;
    }

    @Override
    public Collection<AttributeDefinition> getAttributes() {
        return Arrays.asList(getDefinitions(ATTRIBUTES));
    }

    @Override
    public void registerAttributes(ManagementResourceRegistration registry) {
        AttributeDefinition[] definitions = getDefinitions(ATTRIBUTES);
        ReloadRequiredWriteAttributeHandler reloadRequiredWriteAttributeHandler = new ReloadRequiredWriteAttributeHandler(definitions);
        for (AttributeDefinition attr : definitions) {
            if (!attr.getFlags().contains(AttributeAccess.Flag.STORAGE_RUNTIME)) {
                if (deployed) {
                    registry.registerReadOnlyAttribute(attr, PooledConnectionFactoryConfigurationRuntimeHandler.INSTANCE);
                } else {
                    registry.registerReadWriteAttribute(attr, null, reloadRequiredWriteAttributeHandler);
                }
            }
        }
    }
}
