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
import static org.jboss.as.controller.client.helpers.MeasurementUnit.MILLISECONDS;
import static org.jboss.dmr.ModelType.BIG_DECIMAL;
import static org.jboss.dmr.ModelType.BOOLEAN;
import static org.jboss.dmr.ModelType.INT;
import static org.jboss.dmr.ModelType.LONG;
import static org.jboss.dmr.ModelType.OBJECT;
import static org.jboss.dmr.ModelType.STRING;
import static org.wildfly.extension.messaging.activemq.CommonAttributes.STATIC_CONNECTORS;

import java.util.Arrays;
import java.util.Collection;

import org.apache.activemq.artemis.api.config.ActiveMQDefaultConfiguration;
import org.apache.activemq.artemis.core.server.cluster.impl.MessageLoadBalancingType;
import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.AttributeMarshaller;
import org.jboss.as.controller.AttributeParser;
import org.jboss.as.controller.PersistentResourceDefinition;
import org.jboss.as.controller.PrimitiveListAttributeDefinition;
import org.jboss.as.controller.ReloadRequiredWriteAttributeHandler;
import org.jboss.as.controller.SimpleAttributeDefinition;
import org.jboss.as.controller.SimpleOperationDefinition;
import org.jboss.as.controller.SimpleOperationDefinitionBuilder;
import org.jboss.as.controller.StringListAttributeDefinition;
import org.jboss.as.controller.operations.validation.EnumValidator;
import org.jboss.as.controller.operations.validation.StringLengthValidator;
import org.jboss.as.controller.registry.AttributeAccess;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.dmr.ModelNode;


/**
 * Cluster connection resource definition
 *
 * @author <a href="http://jmesnil.net">Jeff Mesnil</a> (c) 2012 Red Hat Inc.
 */
public class ClusterConnectionDefinition extends PersistentResourceDefinition {

    public static final String GET_NODES = "get-nodes";

    public static final SimpleAttributeDefinition ADDRESS = create("cluster-connection-address", STRING)
            .setXmlName(CommonAttributes.ADDRESS)
            .setDefaultValue(new ModelNode(""))
            .setRequired(false)
            // empty string is allowed to route *any* address to the cluster
            .setValidator(new StringLengthValidator(0))
            .setAllowExpression(true)
            .setRestartAllServices()
            .build();

    public static final SimpleAttributeDefinition ALLOW_DIRECT_CONNECTIONS_ONLY = create("allow-direct-connections-only", BOOLEAN)
            .setDefaultValue(new ModelNode(false))
            .setRequired(false)
            .setAllowExpression(true)
            .setRequires(STATIC_CONNECTORS)
            .setRestartAllServices()
            .build();

    public static final SimpleAttributeDefinition CALL_FAILOVER_TIMEOUT = create(CommonAttributes.CALL_FAILOVER_TIMEOUT)
            // cluster connection will wait forever during failover for a non-blocking call
            .setDefaultValue(new ModelNode(-1L))
            .setRestartAllServices()
            .build();

    public static final SimpleAttributeDefinition CHECK_PERIOD = create("check-period", LONG)
            .setDefaultValue(new ModelNode(ActiveMQDefaultConfiguration.getDefaultClusterFailureCheckPeriod()))
            .setRequired(false)
            .setAllowExpression(true)
            .setMeasurementUnit(MILLISECONDS)
            .setRestartAllServices()
            .build();

    public static final SimpleAttributeDefinition CONNECTION_TTL = create("connection-ttl", LONG)
            .setDefaultValue(new ModelNode(ActiveMQDefaultConfiguration.getDefaultClusterConnectionTtl()))
            .setMeasurementUnit(MILLISECONDS)
            .setRequired(false)
            .setAllowExpression(true)
            .setValidator(InfiniteOrPositiveValidators.LONG_INSTANCE)
            .setRestartAllServices()
            .build();

    public static final SimpleAttributeDefinition CONNECTOR_NAME = create("connector-name", STRING)
            .setRestartAllServices()
            .build();

    public static final PrimitiveListAttributeDefinition CONNECTOR_REFS = new StringListAttributeDefinition.Builder(STATIC_CONNECTORS)
            .setRequired(true)
            .setElementValidator(new StringLengthValidator(1))
            .setAttributeParser(AttributeParser.STRING_LIST)
            .setAttributeMarshaller(AttributeMarshaller.STRING_LIST)
            .setAlternatives(CommonAttributes.DISCOVERY_GROUP)
            .setRestartAllServices()
            .build();

    public static final SimpleAttributeDefinition DISCOVERY_GROUP_NAME = create(CommonAttributes.DISCOVERY_GROUP, STRING)
            .setRequired(true)
            .setAlternatives(CONNECTOR_REFS.getName())
            .setRestartAllServices()
            .build();

    // FIXME WFLY-4587 forward-when-no-consumers == true ? STRICT : ON_DEMAND
    public static final SimpleAttributeDefinition MESSAGE_LOAD_BALANCING_TYPE = create("message-load-balancing-type", STRING)
            .setDefaultValue(new ModelNode(ActiveMQDefaultConfiguration.getDefaultClusterMessageLoadBalancingType()))
            .setValidator(new EnumValidator<>(MessageLoadBalancingType.class, true, true))
            .setRequired(false)
            .setAllowExpression(true)
            .setRestartAllServices()
            .build();

    public static final SimpleAttributeDefinition INITIAL_CONNECT_ATTEMPTS = create("initial-connect-attempts", INT)
            .setDefaultValue(new ModelNode(ActiveMQDefaultConfiguration.getDefaultClusterInitialConnectAttempts()))
            .setRequired(false)
            .setAllowExpression(true)
            .setValidator(InfiniteOrPositiveValidators.INT_INSTANCE)
            .setRestartAllServices()
            .build();

    public static final SimpleAttributeDefinition MAX_HOPS = create("max-hops", INT)
            .setDefaultValue(new ModelNode(ActiveMQDefaultConfiguration.getDefaultClusterMaxHops()))
            .setRequired(false)
            .setAllowExpression(true)
            .setRestartAllServices()
            .build();

    public static final SimpleAttributeDefinition MAX_RETRY_INTERVAL = create("max-retry-interval", LONG)
            .setDefaultValue(new ModelNode(ActiveMQDefaultConfiguration.getDefaultClusterMaxRetryInterval()))
            .setRequired(false)
            .setAllowExpression(true)
            .setMeasurementUnit(MILLISECONDS)
            .setRestartAllServices()
            .build();

    public static final SimpleAttributeDefinition NOTIFICATION_ATTEMPTS = create("notification-attempts",INT)
            .setDefaultValue(new ModelNode(ActiveMQDefaultConfiguration.getDefaultClusterNotificationAttempts()))
            .setMeasurementUnit(MILLISECONDS)
            .setRequired(false)
            .setAllowExpression(true)
            .setRestartAllServices()
            .build();

    public static final SimpleAttributeDefinition NOTIFICATION_INTERVAL = create("notification-interval",LONG)
            .setDefaultValue(new ModelNode(ActiveMQDefaultConfiguration.getDefaultClusterNotificationInterval()))
            .setMeasurementUnit(MILLISECONDS)
            .setRequired(false)
            .setAllowExpression(true)
            .setRestartAllServices()
            .build();

    public static final SimpleAttributeDefinition PRODUCER_WINDOW_SIZE = create("producer-window-size", INT)
            .setDefaultValue(new ModelNode(ActiveMQDefaultConfiguration.getDefaultBridgeProducerWindowSize()))
            .setMeasurementUnit(BYTES)
            .setRequired(false)
            .setAllowExpression(true)
            .setValidator(InfiniteOrPositiveValidators.INT_INSTANCE)
            .setRestartAllServices()
            .build();

    public static final SimpleAttributeDefinition RETRY_INTERVAL = create("retry-interval", LONG)
            .setDefaultValue(new ModelNode(ActiveMQDefaultConfiguration.getDefaultClusterRetryInterval()))
            .setMeasurementUnit(MILLISECONDS)
            .setRequired(false)
            .setAllowExpression(true)
            .setRestartAllServices()
            .build();

    public static final SimpleAttributeDefinition RECONNECT_ATTEMPTS = create("reconnect-attempts", INT)
            .setDefaultValue(new ModelNode(ActiveMQDefaultConfiguration.getDefaultClusterReconnectAttempts()))
            .setRequired(false)
            .setAllowExpression(true)
            .setValidator(InfiniteOrPositiveValidators.INT_INSTANCE)
            .setRestartAllServices()
            .build();

    public static final SimpleAttributeDefinition RETRY_INTERVAL_MULTIPLIER = create("retry-interval-multiplier", BIG_DECIMAL)
            .setDefaultValue(new ModelNode(ActiveMQDefaultConfiguration.getDefaultClusterRetryIntervalMultiplier()))
            .setRequired(false)
            .setAllowExpression(true)
            .setRestartAllServices()
            .build();

    public static final SimpleAttributeDefinition USE_DUPLICATE_DETECTION = create("use-duplicate-detection", BOOLEAN)
            .setDefaultValue(new ModelNode(ActiveMQDefaultConfiguration.isDefaultClusterDuplicateDetection()))
            .setRequired(false)
            .setAllowExpression(true)
            .setRestartAllServices()
            .build();

    public static final AttributeDefinition[] ATTRIBUTES = {
            ADDRESS, CONNECTOR_NAME,
            CHECK_PERIOD,
            CONNECTION_TTL,
            CommonAttributes.MIN_LARGE_MESSAGE_SIZE,
            CommonAttributes.CALL_TIMEOUT,
            CALL_FAILOVER_TIMEOUT,
            RETRY_INTERVAL, RETRY_INTERVAL_MULTIPLIER, MAX_RETRY_INTERVAL,
            INITIAL_CONNECT_ATTEMPTS,
            RECONNECT_ATTEMPTS, USE_DUPLICATE_DETECTION,
            MESSAGE_LOAD_BALANCING_TYPE, MAX_HOPS,
            CommonAttributes.BRIDGE_CONFIRMATION_WINDOW_SIZE,
            PRODUCER_WINDOW_SIZE,
            NOTIFICATION_ATTEMPTS,
            NOTIFICATION_INTERVAL,
            CONNECTOR_REFS,
            ALLOW_DIRECT_CONNECTIONS_ONLY,
            DISCOVERY_GROUP_NAME,
    };

    public static final SimpleAttributeDefinition NODE_ID = create("node-id", STRING)
            .setStorageRuntime()
            .build();

    public static final SimpleAttributeDefinition TOPOLOGY = create("topology", STRING)
            .setStorageRuntime()
            .build();

    public static final AttributeDefinition[] READONLY_ATTRIBUTES = {
            TOPOLOGY,
            NODE_ID
    };

    private final boolean registerRuntimeOnly;

    ClusterConnectionDefinition(boolean registerRuntimeOnly) {
        super(MessagingExtension.CLUSTER_CONNECTION_PATH,
                MessagingExtension.getResourceDescriptionResolver(CommonAttributes.CLUSTER_CONNECTION),
                ClusterConnectionAdd.INSTANCE,
                ClusterConnectionRemove.INSTANCE);
        this.registerRuntimeOnly = registerRuntimeOnly;
    }

    @Override
    public Collection<AttributeDefinition> getAttributes() {
        return Arrays.asList(ATTRIBUTES);
    }

    @Override
    public void registerAttributes(ManagementResourceRegistration registry) {
        ReloadRequiredWriteAttributeHandler reloadRequiredWriteAttributeHandler = new ReloadRequiredWriteAttributeHandler(ATTRIBUTES);
        for (AttributeDefinition attr : ATTRIBUTES) {
            if (!attr.getFlags().contains(AttributeAccess.Flag.STORAGE_RUNTIME)) {
                registry.registerReadWriteAttribute(attr, null, reloadRequiredWriteAttributeHandler);
            }
        }

        ClusterConnectionControlHandler.INSTANCE.registerAttributes(registry);

        for (AttributeDefinition attr : READONLY_ATTRIBUTES) {
            registry.registerReadOnlyAttribute(attr, ClusterConnectionControlHandler.INSTANCE);
        }
    }

    @Override
    public void registerOperations(ManagementResourceRegistration registry) {

        super.registerOperations(registry);

        if (registerRuntimeOnly) {
            ClusterConnectionControlHandler.INSTANCE.registerOperations(registry, getResourceDescriptionResolver());

            SimpleOperationDefinition getNodesDef = new SimpleOperationDefinitionBuilder(ClusterConnectionDefinition.GET_NODES, getResourceDescriptionResolver())
                    .setReadOnly()
                    .setRuntimeOnly()
                    .setReplyType(OBJECT)
                    .setReplyValueType(STRING)
                    .build();
            registry.registerOperationHandler(getNodesDef, ClusterConnectionControlHandler.INSTANCE);
        }
    }
}
