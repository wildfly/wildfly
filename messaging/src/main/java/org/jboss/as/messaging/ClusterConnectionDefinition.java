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

import static org.hornetq.core.config.impl.ConfigurationImpl.DEFAULT_CLUSTER_CONNECTION_TTL;
import static org.hornetq.core.config.impl.ConfigurationImpl.DEFAULT_CLUSTER_FAILURE_CHECK_PERIOD;
import static org.hornetq.core.config.impl.ConfigurationImpl.DEFAULT_CLUSTER_MAX_RETRY_INTERVAL;
import static org.hornetq.core.config.impl.ConfigurationImpl.DEFAULT_CLUSTER_RECONNECT_ATTEMPTS;
import static org.hornetq.core.config.impl.ConfigurationImpl.DEFAULT_CLUSTER_RETRY_INTERVAL_MULTIPLIER;
import static org.jboss.as.controller.SimpleAttributeDefinitionBuilder.create;
import static org.jboss.as.controller.client.helpers.MeasurementUnit.MILLISECONDS;
import static org.jboss.as.controller.registry.AttributeAccess.Flag.RESTART_ALL_SERVICES;
import static org.jboss.as.controller.registry.AttributeAccess.Flag.STORAGE_RUNTIME;
import static org.jboss.as.messaging.CommonAttributes.CONNECTOR_REF_STRING;
import static org.jboss.as.messaging.CommonAttributes.STATIC_CONNECTORS;
import static org.jboss.dmr.ModelType.BIG_DECIMAL;
import static org.jboss.dmr.ModelType.BOOLEAN;
import static org.jboss.dmr.ModelType.INT;
import static org.jboss.dmr.ModelType.LONG;
import static org.jboss.dmr.ModelType.OBJECT;
import static org.jboss.dmr.ModelType.STRING;

import java.util.EnumSet;

import org.hornetq.core.config.impl.ConfigurationImpl;
import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.PrimitiveListAttributeDefinition;
import org.jboss.as.controller.SimpleAttributeDefinition;
import org.jboss.as.controller.SimpleOperationDefinition;
import org.jboss.as.controller.SimpleOperationDefinitionBuilder;
import org.jboss.as.controller.SimpleResourceDefinition;
import org.jboss.as.controller.operations.validation.StringLengthValidator;
import org.jboss.as.controller.registry.AttributeAccess;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.as.controller.registry.OperationEntry;
import org.jboss.dmr.ModelNode;


/**
 * Cluster connection resource definition
 *
 * @author <a href="http://jmesnil.net">Jeff Mesnil</a> (c) 2012 Red Hat Inc.
 */
public class ClusterConnectionDefinition extends SimpleResourceDefinition {

    public static final PathElement PATH = PathElement.pathElement(CommonAttributes.CLUSTER_CONNECTION);

    public static final String GET_NODES = "get-nodes";

    // we keep the operation for backwards compatibility but it duplicates the "static-connectors" writable attribute
    @Deprecated
    public static final String GET_STATIC_CONNECTORS_AS_JSON = "get-static-connectors-as-json";

    public static final String[] OPERATIONS = {GET_NODES, GET_STATIC_CONNECTORS_AS_JSON};

    private final boolean registerRuntimeOnly;

    public static final SimpleAttributeDefinition ADDRESS = create("cluster-connection-address", STRING)
            .setXmlName(CommonAttributes.ADDRESS)
            .setDefaultValue(null)
            .setFlags(RESTART_ALL_SERVICES)
            .build();

    public static final SimpleAttributeDefinition ALLOW_DIRECT_CONNECTIONS_ONLY = create("allow-direct-connections-only", BOOLEAN)
            .setDefaultValue(new ModelNode().set(false))
            .setAllowNull(true)
            .setFlags(RESTART_ALL_SERVICES)
            .build();

    public static final SimpleAttributeDefinition CHECK_PERIOD = create("check-period", LONG)
            .setDefaultValue(new ModelNode().set(DEFAULT_CLUSTER_FAILURE_CHECK_PERIOD))
            .setAllowNull(true)
            .setMeasurementUnit(MILLISECONDS)
            .setFlags(RESTART_ALL_SERVICES)
            .build();

    public static final SimpleAttributeDefinition CONNECTION_TTL = create("connection-ttl", LONG)
            .setDefaultValue(new ModelNode().set(DEFAULT_CLUSTER_CONNECTION_TTL))
            .setMeasurementUnit(MILLISECONDS)
            .setAllowNull(true)
            .setFlags(RESTART_ALL_SERVICES)
            .build();

    public static final SimpleAttributeDefinition CONNECTOR_REF = create("connector-ref", STRING)
            .setFlags(RESTART_ALL_SERVICES)
            .build();

    public static final PrimitiveListAttributeDefinition CONNECTOR_REFS = PrimitiveListAttributeDefinition.Builder.of(STATIC_CONNECTORS, STRING)
            .setAllowNull(true)
            .setValidator(new StringLengthValidator(1))
            .setXmlName(CONNECTOR_REF_STRING)
            .setAttributeMarshaller(new AttributeMarshallers.WrappedListAttributeMarshaller(null))
            .setAlternatives(CommonAttributes.DISCOVERY_GROUP_NAME)
            .setRestartAllServices()
            .build();

    public static final SimpleAttributeDefinition DISCOVERY_GROUP_NAME = create(CommonAttributes.DISCOVERY_GROUP_NAME, STRING)
            .setAllowNull(true)
            .setAlternatives(STATIC_CONNECTORS)
            .setAttributeMarshaller(AttributeMarshallers.DISCOVERY_GROUP_MARSHALLER)
            .setRestartAllServices()
            .build();

    public static final SimpleAttributeDefinition FORWARD_WHEN_NO_CONSUMERS = create("forward-when-no-consumers", BOOLEAN)
            .setDefaultValue(new ModelNode().set(ConfigurationImpl.DEFAULT_CLUSTER_FORWARD_WHEN_NO_CONSUMERS))
            .setAllowNull(true)
            .setFlags(RESTART_ALL_SERVICES)
            .build();

    public static final SimpleAttributeDefinition MAX_HOPS = create("max-hops", INT)
            .setDefaultValue(new ModelNode().set(ConfigurationImpl.DEFAULT_CLUSTER_MAX_HOPS))
            .setAllowNull(true)
            .setFlags(RESTART_ALL_SERVICES)
            .build();

    public static final SimpleAttributeDefinition MAX_RETRY_INTERVAL = create("max-retry-interval", LONG)
            .setDefaultValue(new ModelNode().set(DEFAULT_CLUSTER_MAX_RETRY_INTERVAL))
            .setAllowNull(true)
            .setMeasurementUnit(MILLISECONDS)
            .setFlags(RESTART_ALL_SERVICES)
            .build();

    public static final SimpleAttributeDefinition RETRY_INTERVAL = create("retry-interval", LONG)
            .setDefaultValue(new ModelNode().set(ConfigurationImpl.DEFAULT_CLUSTER_RETRY_INTERVAL))
            .setMeasurementUnit(MILLISECONDS)
            .setAllowNull(true)
            .setFlags(RESTART_ALL_SERVICES)
            .build();

    public static final SimpleAttributeDefinition RECONNECT_ATTEMPTS = create("reconnect-attempts", INT)
            .setDefaultValue(new ModelNode().set(DEFAULT_CLUSTER_RECONNECT_ATTEMPTS))
            .setAllowNull(true)
            .setFlags(RESTART_ALL_SERVICES)
            .build();

    public static final SimpleAttributeDefinition RETRY_INTERVAL_MULTIPLIER = create("retry-interval-multiplier", BIG_DECIMAL)
            .setDefaultValue(new ModelNode().set(DEFAULT_CLUSTER_RETRY_INTERVAL_MULTIPLIER))
            .setAllowNull(true)
            .setFlags(RESTART_ALL_SERVICES)
            .build();

    public static final SimpleAttributeDefinition USE_DUPLICATE_DETECTION = create("use-duplicate-detection", BOOLEAN)
            .setDefaultValue(new ModelNode().set(ConfigurationImpl.DEFAULT_CLUSTER_DUPLICATE_DETECTION))
            .setAllowNull(true)
            .setFlags(RESTART_ALL_SERVICES)
            .build();

    public static final AttributeDefinition[] ATTRIBUTES = {
            ADDRESS, CONNECTOR_REF,
            CHECK_PERIOD,
            CONNECTION_TTL,
            CommonAttributes.MIN_LARGE_MESSAGE_SIZE,
            CommonAttributes.CALL_TIMEOUT,
            CommonAttributes.CALL_FAILOVER_TIMEOUT,
            RETRY_INTERVAL, RETRY_INTERVAL_MULTIPLIER, MAX_RETRY_INTERVAL,
            RECONNECT_ATTEMPTS, USE_DUPLICATE_DETECTION,
            FORWARD_WHEN_NO_CONSUMERS, MAX_HOPS,
            CommonAttributes.BRIDGE_CONFIRMATION_WINDOW_SIZE,
            CONNECTOR_REFS,
            ALLOW_DIRECT_CONNECTIONS_ONLY,
            DISCOVERY_GROUP_NAME,
    };

    public static final SimpleAttributeDefinition NODE_ID = create("node-id", STRING)
            .setFlags(STORAGE_RUNTIME)
            .build();

    public static final SimpleAttributeDefinition TOPOLOGY = create("topology", STRING)
            .setFlags(STORAGE_RUNTIME)
            .build();

    public static final AttributeDefinition[] READONLY_ATTRIBUTES = {
            TOPOLOGY,
            NODE_ID
    };

    public ClusterConnectionDefinition(final boolean registerRuntimeOnly) {
        super(PATH,
                MessagingExtension.getResourceDescriptionResolver(CommonAttributes.CLUSTER_CONNECTION),
                ClusterConnectionAdd.INSTANCE,
                ClusterConnectionRemove.INSTANCE);
        this.registerRuntimeOnly = registerRuntimeOnly;
    }

    @Override
    public void registerAttributes(ManagementResourceRegistration registry) {
        super.registerAttributes(registry);
        for (AttributeDefinition attr : ATTRIBUTES) {
            if (registerRuntimeOnly || !attr.getFlags().contains(AttributeAccess.Flag.STORAGE_RUNTIME)) {
                registry.registerReadWriteAttribute(attr, null, ClusterConnectionWriteAttributeHandler.INSTANCE);
            }
        }

        if (registerRuntimeOnly) {
            ClusterConnectionControlHandler.INSTANCE.registerAttributes(registry);

            for (AttributeDefinition attr : READONLY_ATTRIBUTES) {
                registry.registerReadOnlyAttribute(attr, ClusterConnectionControlHandler.INSTANCE);
            }
        }
    }

    @Override
    public void registerOperations(ManagementResourceRegistration registry) {
        if (registerRuntimeOnly) {
            ClusterConnectionControlHandler.INSTANCE.registerOperations(registry, getResourceDescriptionResolver());

            final EnumSet<OperationEntry.Flag> flags = EnumSet.of(OperationEntry.Flag.READ_ONLY, OperationEntry.Flag.RUNTIME_ONLY);
            SimpleOperationDefinition getNodesDef = new SimpleOperationDefinitionBuilder(ClusterConnectionDefinition.GET_NODES, getResourceDescriptionResolver())
                    .withFlags(flags)
                    .setReplyType(OBJECT)
                    .setReplyValueType(STRING)
                    .build();
            registry.registerOperationHandler(getNodesDef, ClusterConnectionControlHandler.INSTANCE);
            SimpleOperationDefinition getStaticConnectorsAsJson = new SimpleOperationDefinitionBuilder(ClusterConnectionDefinition.GET_STATIC_CONNECTORS_AS_JSON, getResourceDescriptionResolver())
                    .withFlags(flags)
                    .setReplyType(STRING)
                    .build();
            registry.registerOperationHandler(getStaticConnectorsAsJson, ClusterConnectionControlHandler.INSTANCE);

        }

        super.registerOperations(registry);
    }
}
