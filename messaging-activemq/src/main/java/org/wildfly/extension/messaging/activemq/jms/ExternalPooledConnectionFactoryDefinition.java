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

import static org.wildfly.extension.messaging.activemq.AbstractTransportDefinition.CONNECTOR_CAPABILITY_NAME;
import static org.wildfly.extension.messaging.activemq.jms.ConnectionFactoryAttribute.getDefinitions;

import java.util.Arrays;
import java.util.Collection;

import org.jboss.as.controller.AbstractAttributeDefinitionBuilder;
import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.AttributeMarshaller;
import org.jboss.as.controller.AttributeParser;
import org.jboss.as.controller.PrimitiveListAttributeDefinition;
import org.jboss.as.controller.SimpleAttributeDefinition;
import org.jboss.as.controller.SimpleAttributeDefinitionBuilder;
import org.jboss.as.controller.SimpleListAttributeDefinition;
import org.jboss.as.controller.SimpleMapAttributeDefinition;
import org.jboss.as.controller.SimpleResourceDefinition;
import org.jboss.as.controller.StringListAttributeDefinition;
import org.jboss.as.controller.capability.RuntimeCapability;
import org.jboss.as.controller.registry.AttributeAccess;
import org.jboss.dmr.ModelNode;
import org.wildfly.extension.messaging.activemq.AbstractTransportDefinition;
import org.wildfly.extension.messaging.activemq.CommonAttributes;
import org.wildfly.extension.messaging.activemq.MessagingExtension;
import org.wildfly.extension.messaging.activemq.jms.ConnectionFactoryAttributes.Common;
import org.wildfly.extension.messaging.activemq.jms.ConnectionFactoryAttributes.Pooled;

/**
 * JMS external pooled Connection Factory resource definition.
 * By 'external' it means that this PCF is targeting an external broker.
 * @author Emmanuel Hugonnet (c) 2019 Red Hat, Inc.
 */
public class ExternalPooledConnectionFactoryDefinition extends PooledConnectionFactoryDefinition {

    static final String CAPABILITY_NAME = "org.wildfly.messaging.activemq.external.connection-factory";
    static final RuntimeCapability<Void> CAPABILITY = RuntimeCapability.Builder.of(CAPABILITY_NAME, true, ExternalConnectionFactoryService.class).
            build();

    // the generation of the Pooled CF attributes is a bit ugly but it is with purpose:
    // * factorize the attributes which are common between the regular CF and the pooled CF
    // * keep in a single place the subtle differences (e.g. different default values for reconnect-attempts between
    //   the regular and pooled CF
    private static ConnectionFactoryAttribute[] define(ConnectionFactoryAttribute[] specific, ConnectionFactoryAttribute... common) {
        int size = common.length + specific.length;
        ConnectionFactoryAttribute[] result = new ConnectionFactoryAttribute[size];
        for (int i = 0; i < specific.length; i++) {
            ConnectionFactoryAttribute attr = specific[i];
            AttributeDefinition definition = attr.getDefinition();
            if (definition == ConnectionFactoryAttributes.Pooled.INITIAL_CONNECT_ATTEMPTS) {
                result[i] = ConnectionFactoryAttribute.create(
                        SimpleAttributeDefinitionBuilder
                                .create(ConnectionFactoryAttributes.Pooled.INITIAL_CONNECT_ATTEMPTS)
                                .setDefaultValue(new ModelNode(-1))
                                .build(),
                        attr.getPropertyName(),
                        true);
            } else {
                result[i] = attr;
            }
        }
        for (int i = 0; i < common.length; i++) {
            ConnectionFactoryAttribute attr = common[i];
            AttributeDefinition definition = attr.getDefinition();

            ConnectionFactoryAttribute newAttr;
            // replace the reconnect-attempts attribute to use a different default value for pooled CF
            if (definition == Common.RECONNECT_ATTEMPTS) {
                AttributeDefinition copy = copy(Pooled.RECONNECT_ATTEMPTS, AttributeAccess.Flag.RESTART_ALL_SERVICES);
                newAttr = ConnectionFactoryAttribute.create(copy, Pooled.RECONNECT_ATTEMPTS_PROP_NAME, true);
            } else if (definition == CommonAttributes.HA) {
                newAttr = ConnectionFactoryAttribute.create(
                        SimpleAttributeDefinitionBuilder
                                .create(CommonAttributes.HA)
                                .setDefaultValue(ModelNode.TRUE)
                                .setFlags(AttributeAccess.Flag.RESTART_ALL_SERVICES)
                                .build(),
                        attr.getPropertyName(),
                        true);
            } else if (definition == Common.CONNECTORS) {
                StringListAttributeDefinition copy = new StringListAttributeDefinition.Builder(Common.CONNECTORS)
                        .setAlternatives(CommonAttributes.DISCOVERY_GROUP)
                        .setRequired(true)
                        .setAttributeParser(AttributeParser.STRING_LIST)
                        .setAttributeMarshaller(AttributeMarshaller.STRING_LIST)
                        .setCapabilityReference(new AbstractTransportDefinition.TransportCapabilityReferenceRecorder(CAPABILITY_NAME, CONNECTOR_CAPABILITY_NAME, true))
                        .setRestartAllServices()
                        .build();
                newAttr = ConnectionFactoryAttribute.create(copy, attr.getPropertyName(), attr.isResourceAdapterProperty(), attr.getConfigType());
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
        if (attribute instanceof SimpleListAttributeDefinition) {
            builder = new SimpleListAttributeDefinition.Builder((SimpleListAttributeDefinition) attribute);
        } else if (attribute instanceof SimpleMapAttributeDefinition) {
            builder = new SimpleMapAttributeDefinition.Builder((SimpleMapAttributeDefinition) attribute);
        } else if (attribute instanceof PrimitiveListAttributeDefinition) {
            builder = new PrimitiveListAttributeDefinition.Builder((PrimitiveListAttributeDefinition) attribute);
        } else {
            builder = new SimpleAttributeDefinitionBuilder((SimpleAttributeDefinition) attribute);
        }
        builder.setFlags(flag);
        return builder.build();
    }

    public static final ConnectionFactoryAttribute[] ATTRIBUTES = define(Pooled.ATTRIBUTES, Common.ATTRIBUTES);

    public static final ExternalPooledConnectionFactoryDefinition INSTANCE = new ExternalPooledConnectionFactoryDefinition(false);

    public static final ExternalPooledConnectionFactoryDefinition DEPLOYMENT_INSTANCE = new ExternalPooledConnectionFactoryDefinition(true);

    public ExternalPooledConnectionFactoryDefinition(final boolean deployed) {
        super(new SimpleResourceDefinition.Parameters(MessagingExtension.POOLED_CONNECTION_FACTORY_PATH, MessagingExtension.getResourceDescriptionResolver(CommonAttributes.POOLED_CONNECTION_FACTORY))
                .setAddHandler(ExternalPooledConnectionFactoryAdd.INSTANCE)
                .setRemoveHandler(ExternalPooledConnectionFactoryRemove.INSTANCE)
                .setCapabilities(CAPABILITY), deployed);
    }

    @Override
    public Collection<AttributeDefinition> getAttributes() {
        return Arrays.asList(getDefinitions(ATTRIBUTES));
    }
}
