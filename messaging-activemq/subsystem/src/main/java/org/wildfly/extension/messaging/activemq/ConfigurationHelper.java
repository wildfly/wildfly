/*
 * Copyright 2019 JBoss by Red Hat.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.wildfly.extension.messaging.activemq;

import static org.wildfly.extension.messaging.activemq.BridgeAdd.createBridgeConfiguration;
import static org.wildfly.extension.messaging.activemq.CommonAttributes.DURABLE;
import static org.wildfly.extension.messaging.activemq.CommonAttributes.FILTER;
import static org.wildfly.extension.messaging.activemq.CommonAttributes.SELECTOR;
import static org.wildfly.extension.messaging.activemq.DivertAdd.createDivertConfiguration;
import static org.wildfly.extension.messaging.activemq.GroupingHandlerDefinition.GROUPING_HANDLER_ADDRESS;
import static org.wildfly.extension.messaging.activemq.GroupingHandlerDefinition.GROUP_TIMEOUT;
import static org.wildfly.extension.messaging.activemq.GroupingHandlerDefinition.REAPER_PERIOD;
import static org.wildfly.extension.messaging.activemq.GroupingHandlerDefinition.TIMEOUT;
import static org.wildfly.extension.messaging.activemq.QueueDefinition.DEFAULT_ROUTING_TYPE;
import static org.wildfly.extension.messaging.activemq.QueueDefinition.ROUTING_TYPE;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.apache.activemq.artemis.api.core.DiscoveryGroupConfiguration;
import org.apache.activemq.artemis.api.core.RoutingType;
import org.apache.activemq.artemis.api.core.SimpleString;
import org.apache.activemq.artemis.core.config.BridgeConfiguration;
import org.apache.activemq.artemis.core.config.ClusterConnectionConfiguration;
import org.apache.activemq.artemis.core.config.Configuration;
import org.apache.activemq.artemis.core.config.ConnectorServiceConfiguration;
import org.apache.activemq.artemis.core.config.CoreQueueConfiguration;
import org.apache.activemq.artemis.core.config.DivertConfiguration;
import org.apache.activemq.artemis.core.server.cluster.impl.MessageLoadBalancingType;
import org.apache.activemq.artemis.core.server.group.impl.GroupingHandlerConfiguration;
import org.apache.activemq.artemis.utils.SelectorTranslator;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.Property;

/**
 * Helper to create Artemis configuration.
 * @author Emmanuel Hugonnet (c) 2019 Red Hat, Inc.
 */
public class ConfigurationHelper {

    static void addQueueConfigurations(final OperationContext context, final Configuration configuration, final ModelNode model)  throws OperationFailedException {
        if (model.hasDefined(CommonAttributes.QUEUE)) {
            final List<CoreQueueConfiguration> configs = configuration.getQueueConfigurations();
            for (Property prop : model.get(CommonAttributes.QUEUE).asPropertyList()) {
                configs.add(createCoreQueueConfiguration(context, prop.getName(), prop.getValue()));
            }
        }
        if (model.hasDefined(CommonAttributes.JMS_QUEUE)) {
            final List<CoreQueueConfiguration> configs = configuration.getQueueConfigurations();
            for (Property prop : model.get(CommonAttributes.JMS_QUEUE).asPropertyList()) {
                configs.add(createJMSDestinationConfiguration(context, prop.getName(), prop.getValue()));
            }
        }
    }

    static CoreQueueConfiguration createCoreQueueConfiguration(final OperationContext context, String name, ModelNode model) throws OperationFailedException {
        final String queueAddress = QueueDefinition.ADDRESS.resolveModelAttribute(context, model).asString();
        final String filter = FILTER.resolveModelAttribute(context, model).asStringOrNull();
        final String routing;
        if(DEFAULT_ROUTING_TYPE != null && ! model.hasDefined(ROUTING_TYPE.getName())) {
            routing = RoutingType.valueOf(DEFAULT_ROUTING_TYPE.toUpperCase(Locale.ENGLISH)).toString();
        } else {
            routing = ROUTING_TYPE.resolveModelAttribute(context, model).asString();
        }
        final boolean durable = DURABLE.resolveModelAttribute(context, model).asBoolean();

        return new CoreQueueConfiguration()
                .setAddress(queueAddress)
                .setName(name)
                .setFilterString(filter)
                .setDurable(durable)
                .setRoutingType(RoutingType.valueOf(routing));
    }

    static CoreQueueConfiguration createJMSDestinationConfiguration(final OperationContext context, String name, ModelNode model) throws OperationFailedException {
        final String selector = SELECTOR.resolveModelAttribute(context, model).asStringOrNull();
        final boolean durable = DURABLE.resolveModelAttribute(context, model).asBoolean();
        final String destinationAddress = "jms.queue." + name;

        return new CoreQueueConfiguration()
                .setAddress(destinationAddress)
                .setName(destinationAddress)
                .setFilterString(SelectorTranslator.convertToActiveMQFilterString(selector))
                .setDurable(durable)
                .setRoutingType(RoutingType.ANYCAST);
    }

    static void addDivertConfigurations(final OperationContext context, final Configuration configuration, final ModelNode model)  throws OperationFailedException {
        if (model.hasDefined(CommonAttributes.DIVERT)) {
            final List<DivertConfiguration> configs = configuration.getDivertConfigurations();
            for (Property prop : model.get(CommonAttributes.DIVERT).asPropertyList()) {
                configs.add(createDivertConfiguration(context, prop.getName(), prop.getValue()));

            }
        }
    }

    static Map<String, DiscoveryGroupConfiguration> addDiscoveryGroupConfigurations(final OperationContext context, final ModelNode model) throws OperationFailedException {
        Map<String, DiscoveryGroupConfiguration> configs = new HashMap<>();
        if (model.hasDefined(CommonAttributes.JGROUPS_DISCOVERY_GROUP)) {
            for (Property prop : model.get(CommonAttributes.JGROUPS_DISCOVERY_GROUP).asPropertyList()) {
                configs.put(prop.getName(), new DiscoveryGroupConfiguration()
                        .setName(prop.getName())
                        .setRefreshTimeout(DiscoveryGroupDefinition.REFRESH_TIMEOUT.resolveModelAttribute(context, prop.getValue()).asLong())
                        .setDiscoveryInitialWaitTimeout(DiscoveryGroupDefinition.INITIAL_WAIT_TIMEOUT.resolveModelAttribute(context, prop.getValue()).asLong()));
            }
        }
        if (model.hasDefined(CommonAttributes.SOCKET_DISCOVERY_GROUP)) {
            for (Property prop : model.get(CommonAttributes.SOCKET_DISCOVERY_GROUP).asPropertyList()) {
                configs.put(prop.getName(), new DiscoveryGroupConfiguration()
                        .setName(prop.getName())
                        .setRefreshTimeout(DiscoveryGroupDefinition.REFRESH_TIMEOUT.resolveModelAttribute(context, prop.getValue()).asLong())
                        .setDiscoveryInitialWaitTimeout(DiscoveryGroupDefinition.INITIAL_WAIT_TIMEOUT.resolveModelAttribute(context, prop.getValue()).asLong()));
            }
        }
        return configs;
    }

    static void addBridgeConfigurations(final OperationContext context, final Configuration configuration, final ModelNode model)  throws OperationFailedException {
        if (model.hasDefined(CommonAttributes.BRIDGE)) {
            final List<BridgeConfiguration> configs = configuration.getBridgeConfigurations();
            for (Property prop : model.get(CommonAttributes.BRIDGE).asPropertyList()) {
                configs.add(createBridgeConfiguration(context, prop.getName(), prop.getValue()));
            }
        }
    }

    static void addGroupingHandlerConfiguration(final OperationContext context, final Configuration configuration, final ModelNode model) throws OperationFailedException {
        if (model.hasDefined(CommonAttributes.GROUPING_HANDLER)) {
            final Property prop = model.get(CommonAttributes.GROUPING_HANDLER).asProperty();
            final String name = prop.getName();
            final ModelNode node = prop.getValue();

            final GroupingHandlerConfiguration.TYPE type = GroupingHandlerConfiguration.TYPE.valueOf(GroupingHandlerDefinition.TYPE.resolveModelAttribute(context, node).asString());
            final String address = GROUPING_HANDLER_ADDRESS.resolveModelAttribute(context, node).asString();
            final int timeout = TIMEOUT.resolveModelAttribute(context, node).asInt();
            final long groupTimeout = GROUP_TIMEOUT.resolveModelAttribute(context, node).asLong();
            final long reaperPeriod = REAPER_PERIOD.resolveModelAttribute(context, node).asLong();
            final GroupingHandlerConfiguration conf = new GroupingHandlerConfiguration()
                    .setName(SimpleString.toSimpleString(name))
                    .setType(type)
                    .setAddress(SimpleString.toSimpleString(address))
                    .setTimeout(timeout)
                    .setGroupTimeout(groupTimeout)
                    .setReaperPeriod(reaperPeriod);
            configuration.setGroupingHandlerConfiguration(conf);
        }
    }

    static void addClusterConnectionConfigurations(final OperationContext context, final Configuration configuration, final ModelNode model)  throws OperationFailedException {
        if (model.hasDefined(CommonAttributes.CLUSTER_CONNECTION)) {
            final List<ClusterConnectionConfiguration> configs = configuration.getClusterConfigurations();
            for (Property prop : model.get(CommonAttributes.CLUSTER_CONNECTION).asPropertyList()) {
                configs.add(createClusterConnectionConfiguration(context, prop.getName(), prop.getValue()));

            }
        }
    }

    private static ClusterConnectionConfiguration createClusterConnectionConfiguration(final OperationContext context, final String name, final ModelNode model) throws OperationFailedException {
        final String address = ClusterConnectionDefinition.ADDRESS.resolveModelAttribute(context, model).asString();
        final String connectorName = ClusterConnectionDefinition.CONNECTOR_NAME.resolveModelAttribute(context, model).asString();
        final long retryInterval = ClusterConnectionDefinition.RETRY_INTERVAL.resolveModelAttribute(context, model).asLong();
        final boolean duplicateDetection = ClusterConnectionDefinition.USE_DUPLICATE_DETECTION.resolveModelAttribute(context, model).asBoolean();
        final long connectionTTL = ClusterConnectionDefinition.CONNECTION_TTL.resolveModelAttribute(context, model).asInt();
        final int initialConnectAttempts = ClusterConnectionDefinition.INITIAL_CONNECT_ATTEMPTS.resolveModelAttribute(context, model).asInt();
        final int reconnectAttempts = ClusterConnectionDefinition.RECONNECT_ATTEMPTS.resolveModelAttribute(context, model).asInt();
        final long maxRetryInterval = ClusterConnectionDefinition.MAX_RETRY_INTERVAL.resolveModelAttribute(context, model).asLong();
        final double retryIntervalMultiplier = ClusterConnectionDefinition.RETRY_INTERVAL_MULTIPLIER.resolveModelAttribute(context, model).asDouble();
        final long clientFailureCheckPeriod = ClusterConnectionDefinition.CHECK_PERIOD.resolveModelAttribute(context, model).asInt();
        final String messageLoadBalancingType = ClusterConnectionDefinition.MESSAGE_LOAD_BALANCING_TYPE.resolveModelAttribute(context, model).asString();

        final int maxHops = ClusterConnectionDefinition.MAX_HOPS.resolveModelAttribute(context, model).asInt();
        final int confirmationWindowSize = CommonAttributes.BRIDGE_CONFIRMATION_WINDOW_SIZE.resolveModelAttribute(context, model).asInt();
        final int producerWindowSize = ClusterConnectionDefinition.PRODUCER_WINDOW_SIZE.resolveModelAttribute(context, model).asInt();
        final ModelNode discoveryNode = ClusterConnectionDefinition.DISCOVERY_GROUP_NAME.resolveModelAttribute(context, model);
        final int minLargeMessageSize = CommonAttributes.MIN_LARGE_MESSAGE_SIZE.resolveModelAttribute(context, model).asInt();
        final long callTimeout = CommonAttributes.CALL_TIMEOUT.resolveModelAttribute(context, model).asLong();
        final long callFailoverTimeout = ClusterConnectionDefinition.CALL_FAILOVER_TIMEOUT.resolveModelAttribute(context, model).asLong();
        final long clusterNotificationInterval = ClusterConnectionDefinition.NOTIFICATION_INTERVAL.resolveModelAttribute(context, model).asLong();
        final int clusterNotificationAttempts = ClusterConnectionDefinition.NOTIFICATION_ATTEMPTS.resolveModelAttribute(context, model).asInt();

        ClusterConnectionConfiguration config = new ClusterConnectionConfiguration()
                .setName(name)
                .setAddress(address)
                .setConnectorName(connectorName)
                .setMinLargeMessageSize(minLargeMessageSize)
                .setClientFailureCheckPeriod(clientFailureCheckPeriod)
                .setConnectionTTL(connectionTTL)
                .setRetryInterval(retryInterval)
                .setRetryIntervalMultiplier(retryIntervalMultiplier)
                .setMaxRetryInterval(maxRetryInterval)
                .setInitialConnectAttempts(initialConnectAttempts)
                .setReconnectAttempts(reconnectAttempts)
                .setCallTimeout(callTimeout)
                .setCallFailoverTimeout(callFailoverTimeout)
                .setDuplicateDetection(duplicateDetection)
                .setMessageLoadBalancingType(MessageLoadBalancingType.valueOf(messageLoadBalancingType))
                .setMaxHops(maxHops)
                .setConfirmationWindowSize(confirmationWindowSize)
                .setProducerWindowSize(producerWindowSize)
                .setClusterNotificationInterval(clusterNotificationInterval)
                .setClusterNotificationAttempts(clusterNotificationAttempts);

        final String discoveryGroupName = discoveryNode.isDefined() ? discoveryNode.asString() : null;
        final List<String> staticConnectors = discoveryGroupName == null ? getStaticConnectors(model) : null;
        final boolean allowDirectOnly = ClusterConnectionDefinition.ALLOW_DIRECT_CONNECTIONS_ONLY.resolveModelAttribute(context, model).asBoolean();

        if (discoveryGroupName != null) {
            config.setDiscoveryGroupName(discoveryGroupName);
        } else {
            config.setStaticConnectors(staticConnectors)
                    .setAllowDirectConnectionsOnly(allowDirectOnly);
        }
        return config;
    }

    private static List<String> getStaticConnectors(ModelNode model) {
        if (!model.hasDefined(CommonAttributes.STATIC_CONNECTORS)) {
            return null;
        }

        List<String> result = new ArrayList<>();
        for (ModelNode connector : model.require(CommonAttributes.STATIC_CONNECTORS).asList()) {
            result.add(connector.asString());
        }
        return result;
    }

    static void addConnectorServiceConfigurations(final OperationContext context, final Configuration configuration, final ModelNode model)  throws OperationFailedException {
        if (model.hasDefined(CommonAttributes.CONNECTOR_SERVICE)) {
            final List<ConnectorServiceConfiguration> configs = configuration.getConnectorServiceConfigurations();
            for (Property prop : model.get(CommonAttributes.CONNECTOR_SERVICE).asPropertyList()) {
                configs.add(createConnectorServiceConfiguration(context, prop.getName(), prop.getValue()));
            }
        }
    }

    private static ConnectorServiceConfiguration createConnectorServiceConfiguration(final OperationContext context, final String name, final ModelNode model) throws OperationFailedException {
        final String factoryClass = CommonAttributes.FACTORY_CLASS.resolveModelAttribute(context, model).asString();
        Map<String, String> unwrappedParameters = CommonAttributes.PARAMS.unwrap(context, model);
        Map<String, Object> parameters = new HashMap<String, Object>(unwrappedParameters);
        return new ConnectorServiceConfiguration()
                .setFactoryClassName(factoryClass)
                .setParams(parameters)
                .setName(name);
    }
}
