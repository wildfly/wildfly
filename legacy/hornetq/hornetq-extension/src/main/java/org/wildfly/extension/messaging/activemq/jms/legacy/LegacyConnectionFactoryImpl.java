/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.extension.messaging.activemq.jms.legacy;

import static org.hornetq.api.jms.JMSFactoryType.CF;
import static org.hornetq.api.jms.JMSFactoryType.QUEUE_CF;
import static org.hornetq.api.jms.JMSFactoryType.QUEUE_XA_CF;
import static org.hornetq.api.jms.JMSFactoryType.TOPIC_CF;
import static org.hornetq.api.jms.JMSFactoryType.TOPIC_XA_CF;
import static org.hornetq.api.jms.JMSFactoryType.XA_CF;
import static org.wildfly.extension.messaging.activemq.CommonAttributes.CALL_FAILOVER_TIMEOUT;
import static org.wildfly.extension.messaging.activemq.CommonAttributes.CALL_TIMEOUT;
import static org.wildfly.extension.messaging.activemq.CommonAttributes.CLIENT_ID;
import static org.wildfly.extension.messaging.activemq.jms.legacy.LegacyConnectionFactoryDefinition.AUTO_GROUP;
import static org.wildfly.extension.messaging.activemq.jms.legacy.LegacyConnectionFactoryDefinition.BLOCK_ON_ACKNOWLEDGE;
import static org.wildfly.extension.messaging.activemq.jms.legacy.LegacyConnectionFactoryDefinition.BLOCK_ON_DURABLE_SEND;
import static org.wildfly.extension.messaging.activemq.jms.legacy.LegacyConnectionFactoryDefinition.BLOCK_ON_NON_DURABLE_SEND;
import static org.wildfly.extension.messaging.activemq.jms.legacy.LegacyConnectionFactoryDefinition.CACHE_LARGE_MESSAGE_CLIENT;
import static org.wildfly.extension.messaging.activemq.jms.legacy.LegacyConnectionFactoryDefinition.CLIENT_FAILURE_CHECK_PERIOD;
import static org.wildfly.extension.messaging.activemq.jms.legacy.LegacyConnectionFactoryDefinition.COMPRESS_LARGE_MESSAGES;
import static org.wildfly.extension.messaging.activemq.jms.legacy.LegacyConnectionFactoryDefinition.CONFIRMATION_WINDOW_SIZE;
import static org.wildfly.extension.messaging.activemq.jms.legacy.LegacyConnectionFactoryDefinition.CONNECTION_LOAD_BALANCING_CLASS_NAME;
import static org.wildfly.extension.messaging.activemq.jms.legacy.LegacyConnectionFactoryDefinition.CONNECTION_TTL;
import static org.wildfly.extension.messaging.activemq.jms.legacy.LegacyConnectionFactoryDefinition.CONSUMER_MAX_RATE;
import static org.wildfly.extension.messaging.activemq.jms.legacy.LegacyConnectionFactoryDefinition.CONSUMER_WINDOW_SIZE;
import static org.wildfly.extension.messaging.activemq.jms.legacy.LegacyConnectionFactoryDefinition.DUPS_OK_BATCH_SIZE;
import static org.wildfly.extension.messaging.activemq.jms.legacy.LegacyConnectionFactoryDefinition.FAILOVER_ON_INITIAL_CONNECTION;
import static org.wildfly.extension.messaging.activemq.jms.legacy.LegacyConnectionFactoryDefinition.GROUP_ID;
import static org.wildfly.extension.messaging.activemq.jms.legacy.LegacyConnectionFactoryDefinition.HornetQConnectionFactoryType.GENERIC;
import static org.wildfly.extension.messaging.activemq.jms.legacy.LegacyConnectionFactoryDefinition.HornetQConnectionFactoryType.QUEUE;
import static org.wildfly.extension.messaging.activemq.jms.legacy.LegacyConnectionFactoryDefinition.HornetQConnectionFactoryType.TOPIC;
import static org.wildfly.extension.messaging.activemq.jms.legacy.LegacyConnectionFactoryDefinition.HornetQConnectionFactoryType.XA_GENERIC;
import static org.wildfly.extension.messaging.activemq.jms.legacy.LegacyConnectionFactoryDefinition.HornetQConnectionFactoryType.XA_QUEUE;
import static org.wildfly.extension.messaging.activemq.jms.legacy.LegacyConnectionFactoryDefinition.HornetQConnectionFactoryType.XA_TOPIC;
import static org.wildfly.extension.messaging.activemq.jms.legacy.LegacyConnectionFactoryDefinition.INITIAL_CONNECT_ATTEMPTS;
import static org.wildfly.extension.messaging.activemq.jms.legacy.LegacyConnectionFactoryDefinition.INITIAL_MESSAGE_PACKET_SIZE;
import static org.wildfly.extension.messaging.activemq.jms.legacy.LegacyConnectionFactoryDefinition.MAX_RETRY_INTERVAL;
import static org.wildfly.extension.messaging.activemq.jms.legacy.LegacyConnectionFactoryDefinition.MIN_LARGE_MESSAGE_SIZE;
import static org.wildfly.extension.messaging.activemq.jms.legacy.LegacyConnectionFactoryDefinition.PRE_ACKNOWLEDGE;
import static org.wildfly.extension.messaging.activemq.jms.legacy.LegacyConnectionFactoryDefinition.PRODUCER_MAX_RATE;
import static org.wildfly.extension.messaging.activemq.jms.legacy.LegacyConnectionFactoryDefinition.PRODUCER_WINDOW_SIZE;
import static org.wildfly.extension.messaging.activemq.jms.legacy.LegacyConnectionFactoryDefinition.RECONNECT_ATTEMPTS;
import static org.wildfly.extension.messaging.activemq.jms.legacy.LegacyConnectionFactoryDefinition.RETRY_INTERVAL;
import static org.wildfly.extension.messaging.activemq.jms.legacy.LegacyConnectionFactoryDefinition.RETRY_INTERVAL_MULTIPLIER;
import static org.wildfly.extension.messaging.activemq.jms.legacy.LegacyConnectionFactoryDefinition.SCHEDULED_THREAD_POOL_MAX_SIZE;
import static org.wildfly.extension.messaging.activemq.jms.legacy.LegacyConnectionFactoryDefinition.THREAD_POOL_MAX_SIZE;
import static org.wildfly.extension.messaging.activemq.jms.legacy.LegacyConnectionFactoryDefinition.TRANSACTION_BATCH_SIZE;
import static org.wildfly.extension.messaging.activemq.jms.legacy.LegacyConnectionFactoryDefinition.USE_GLOBAL_POOLS;

import jakarta.jms.ConnectionFactory;
import jakarta.jms.Queue;
import jakarta.jms.Topic;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.activemq.artemis.api.config.ActiveMQDefaultConfiguration;
import org.apache.activemq.artemis.core.server.ActiveMQServer;
import org.hornetq.api.config.HornetQDefaultConfiguration;
import org.hornetq.api.core.BroadcastEndpointFactoryConfiguration;
import org.hornetq.api.core.DiscoveryGroupConfiguration;
import org.hornetq.api.core.TransportConfiguration;
import org.hornetq.api.core.UDPBroadcastGroupConfiguration;
import org.hornetq.api.jms.HornetQJMSClient;
import org.hornetq.api.jms.JMSFactoryType;
import org.hornetq.core.remoting.impl.netty.TransportConstants;
import org.hornetq.jms.client.HornetQConnectionFactory;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.dmr.ModelNode;
import org.jboss.msc.service.ServiceTarget;
import org.jboss.msc.service.StartException;
import org.wildfly.extension.messaging.activemq.BinderServiceUtil;
import org.wildfly.extension.messaging.activemq._private.MessagingLogger;

/**
 *
 * @author Emmanuel Hugonnet (c) 2024 Red Hat, Inc.
 */
public class LegacyConnectionFactoryImpl implements LegacyConnectionFactory {

    /**
     * Map ActiveMQ parameters key (using CameCalse convention) to HornetQ parameter keys (using lisp-case convention)
     */
    private static final Map<String, String> PARAM_KEY_MAPPING = new HashMap<>();

    static {
        PARAM_KEY_MAPPING.put(
                org.apache.activemq.artemis.core.remoting.impl.netty.TransportConstants.SSL_ENABLED_PROP_NAME,
                TransportConstants.SSL_ENABLED_PROP_NAME);
        PARAM_KEY_MAPPING.put(
                org.apache.activemq.artemis.core.remoting.impl.netty.TransportConstants.HTTP_ENABLED_PROP_NAME,
                TransportConstants.HTTP_ENABLED_PROP_NAME);
        PARAM_KEY_MAPPING.put(
                org.apache.activemq.artemis.core.remoting.impl.netty.TransportConstants.HTTP_CLIENT_IDLE_PROP_NAME,
                TransportConstants.HTTP_CLIENT_IDLE_PROP_NAME);
        PARAM_KEY_MAPPING.put(
                org.apache.activemq.artemis.core.remoting.impl.netty.TransportConstants.HTTP_CLIENT_IDLE_SCAN_PERIOD,
                TransportConstants.HTTP_CLIENT_IDLE_SCAN_PERIOD);
        PARAM_KEY_MAPPING.put(
                org.apache.activemq.artemis.core.remoting.impl.netty.TransportConstants.HTTP_REQUIRES_SESSION_ID,
                TransportConstants.HTTP_REQUIRES_SESSION_ID);
        PARAM_KEY_MAPPING.put(
                org.apache.activemq.artemis.core.remoting.impl.netty.TransportConstants.HTTP_UPGRADE_ENABLED_PROP_NAME,
                TransportConstants.HTTP_UPGRADE_ENABLED_PROP_NAME);
        PARAM_KEY_MAPPING.put(
                org.apache.activemq.artemis.core.remoting.impl.netty.TransportConstants.HTTP_UPGRADE_ENDPOINT_PROP_NAME,
                TransportConstants.HTTP_UPGRADE_ENDPOINT_PROP_NAME);
        PARAM_KEY_MAPPING.put(
                org.apache.activemq.artemis.core.remoting.impl.netty.TransportConstants.USE_SERVLET_PROP_NAME,
                TransportConstants.USE_SERVLET_PROP_NAME);
        PARAM_KEY_MAPPING.put(
                org.apache.activemq.artemis.core.remoting.impl.netty.TransportConstants.SERVLET_PATH,
                TransportConstants.SERVLET_PATH);
        PARAM_KEY_MAPPING.put(
                org.apache.activemq.artemis.core.remoting.impl.netty.TransportConstants.USE_NIO_PROP_NAME,
                TransportConstants.USE_NIO_PROP_NAME);
        PARAM_KEY_MAPPING.put(
                org.apache.activemq.artemis.core.remoting.impl.netty.TransportConstants.USE_NIO_GLOBAL_WORKER_POOL_PROP_NAME,
                TransportConstants.USE_NIO_GLOBAL_WORKER_POOL_PROP_NAME);
        PARAM_KEY_MAPPING.put(
                org.apache.activemq.artemis.core.remoting.impl.netty.TransportConstants.LOCAL_ADDRESS_PROP_NAME,
                TransportConstants.LOCAL_ADDRESS_PROP_NAME);
        PARAM_KEY_MAPPING.put(
                org.apache.activemq.artemis.core.remoting.impl.netty.TransportConstants.KEYSTORE_PROVIDER_PROP_NAME,
                TransportConstants.KEYSTORE_PROVIDER_PROP_NAME);
        PARAM_KEY_MAPPING.put(
                org.apache.activemq.artemis.core.remoting.impl.netty.TransportConstants.KEYSTORE_PATH_PROP_NAME,
                TransportConstants.KEYSTORE_PATH_PROP_NAME);
        PARAM_KEY_MAPPING.put(
                org.apache.activemq.artemis.core.remoting.impl.netty.TransportConstants.KEYSTORE_PASSWORD_PROP_NAME,
                TransportConstants.KEYSTORE_PASSWORD_PROP_NAME);
        PARAM_KEY_MAPPING.put(
                org.apache.activemq.artemis.core.remoting.impl.netty.TransportConstants.TRUSTSTORE_PROVIDER_PROP_NAME,
                TransportConstants.TRUSTSTORE_PROVIDER_PROP_NAME);
        PARAM_KEY_MAPPING.put(
                org.apache.activemq.artemis.core.remoting.impl.netty.TransportConstants.TRUSTSTORE_PATH_PROP_NAME,
                TransportConstants.TRUSTSTORE_PATH_PROP_NAME);
        PARAM_KEY_MAPPING.put(
                org.apache.activemq.artemis.core.remoting.impl.netty.TransportConstants.TRUSTSTORE_PASSWORD_PROP_NAME,
                TransportConstants.TRUSTSTORE_PASSWORD_PROP_NAME);
        PARAM_KEY_MAPPING.put(
                org.apache.activemq.artemis.core.remoting.impl.netty.TransportConstants.ENABLED_CIPHER_SUITES_PROP_NAME,
                TransportConstants.ENABLED_CIPHER_SUITES_PROP_NAME);
        PARAM_KEY_MAPPING.put(
                org.apache.activemq.artemis.core.remoting.impl.netty.TransportConstants.ENABLED_PROTOCOLS_PROP_NAME,
                TransportConstants.ENABLED_PROTOCOLS_PROP_NAME);
        PARAM_KEY_MAPPING.put(
                org.apache.activemq.artemis.core.remoting.impl.netty.TransportConstants.TCP_NODELAY_PROPNAME,
                TransportConstants.TCP_NODELAY_PROPNAME);
        PARAM_KEY_MAPPING.put(
                org.apache.activemq.artemis.core.remoting.impl.netty.TransportConstants.TCP_SENDBUFFER_SIZE_PROPNAME,
                TransportConstants.TCP_SENDBUFFER_SIZE_PROPNAME);
        PARAM_KEY_MAPPING.put(
                org.apache.activemq.artemis.core.remoting.impl.netty.TransportConstants.TCP_RECEIVEBUFFER_SIZE_PROPNAME,
                TransportConstants.TCP_RECEIVEBUFFER_SIZE_PROPNAME);
        PARAM_KEY_MAPPING.put(
                org.apache.activemq.artemis.core.remoting.impl.netty.TransportConstants.NIO_REMOTING_THREADS_PROPNAME,
                TransportConstants.NIO_REMOTING_THREADS_PROPNAME);
        PARAM_KEY_MAPPING.put(
                org.apache.activemq.artemis.core.remoting.impl.netty.TransportConstants.BATCH_DELAY,
                TransportConstants.BATCH_DELAY);
        PARAM_KEY_MAPPING.put(
                org.apache.activemq.artemis.core.remoting.impl.netty.TransportConstants.NIO_REMOTING_THREADS_PROPNAME,
                TransportConstants.NIO_REMOTING_THREADS_PROPNAME);
        PARAM_KEY_MAPPING.put(
                ActiveMQDefaultConfiguration.getPropMaskPassword(),
                HornetQDefaultConfiguration.getPropMaskPassword());
        PARAM_KEY_MAPPING.put(
                ActiveMQDefaultConfiguration.getPropPasswordCodec(),
                HornetQDefaultConfiguration.getPropPasswordCodec());
        PARAM_KEY_MAPPING.put(
                org.apache.activemq.artemis.core.remoting.impl.netty.TransportConstants.NETTY_CONNECT_TIMEOUT,
                TransportConstants.NETTY_CONNECT_TIMEOUT);
    }

    @Override
    public ConnectionFactory createLegacyConnectionFactory(OperationContext context, ModelNode model) throws OperationFailedException {

        boolean ha = LegacyConnectionFactoryDefinition.HA.resolveModelAttribute(context, model).asBoolean();
        String factoryTypeStr = LegacyConnectionFactoryDefinition.FACTORY_TYPE.resolveModelAttribute(context, model).asString();
        JMSFactoryType factoryType = getType(LegacyConnectionFactoryDefinition.HornetQConnectionFactoryType.valueOf(factoryTypeStr));

        final HornetQConnectionFactory incompleteCF;
        if (ha) {
            incompleteCF = HornetQJMSClient.createConnectionFactoryWithHA(factoryType);
        } else {
            incompleteCF = HornetQJMSClient.createConnectionFactoryWithoutHA(factoryType);
        }

        incompleteCF.setAutoGroup(AUTO_GROUP.resolveModelAttribute(context, model).asBoolean());
        incompleteCF.setBlockOnAcknowledge(BLOCK_ON_ACKNOWLEDGE.resolveModelAttribute(context, model).asBoolean());
        incompleteCF.setBlockOnDurableSend(BLOCK_ON_DURABLE_SEND.resolveModelAttribute(context, model).asBoolean());
        incompleteCF.setBlockOnNonDurableSend(BLOCK_ON_NON_DURABLE_SEND.resolveModelAttribute(context, model).asBoolean());
        incompleteCF.setCacheLargeMessagesClient(CACHE_LARGE_MESSAGE_CLIENT.resolveModelAttribute(context, model).asBoolean());
        incompleteCF.setCallFailoverTimeout(CALL_FAILOVER_TIMEOUT.resolveModelAttribute(context, model).asLong());
        incompleteCF.setCallTimeout(CALL_TIMEOUT.resolveModelAttribute(context, model).asLong());
        incompleteCF.setClientFailureCheckPeriod(CLIENT_FAILURE_CHECK_PERIOD.resolveModelAttribute(context, model).asLong());
        final ModelNode clientID = CLIENT_ID.resolveModelAttribute(context, model);
        if (clientID.isDefined()) {
            incompleteCF.setClientID(clientID.asString());
        }
        incompleteCF.setCompressLargeMessage(COMPRESS_LARGE_MESSAGES.resolveModelAttribute(context, model).asBoolean());
        incompleteCF.setConfirmationWindowSize(CONFIRMATION_WINDOW_SIZE.resolveModelAttribute(context, model).asInt());

        final ModelNode connectionLoadBalancingClassName = CONNECTION_LOAD_BALANCING_CLASS_NAME.resolveModelAttribute(context, model);
        if (connectionLoadBalancingClassName.isDefined()) {
            incompleteCF.setConnectionLoadBalancingPolicyClassName(connectionLoadBalancingClassName.asString());
        }

        incompleteCF.setConnectionTTL(CONNECTION_TTL.resolveModelAttribute(context, model).asLong());
        incompleteCF.setConsumerMaxRate(CONSUMER_MAX_RATE.resolveModelAttribute(context, model).asInt());
        incompleteCF.setConsumerWindowSize(CONSUMER_WINDOW_SIZE.resolveModelAttribute(context, model).asInt());
        incompleteCF.setConfirmationWindowSize(CONFIRMATION_WINDOW_SIZE.resolveModelAttribute(context, model).asInt());
        incompleteCF.setDupsOKBatchSize(DUPS_OK_BATCH_SIZE.resolveModelAttribute(context, model).asInt());
        incompleteCF.setFailoverOnInitialConnection(FAILOVER_ON_INITIAL_CONNECTION.resolveModelAttribute(context, model).asBoolean());
        final ModelNode groupID = GROUP_ID.resolveModelAttribute(context, model);
        if (groupID.isDefined()) {
            incompleteCF.setGroupID(groupID.asString());
        }
        incompleteCF.setInitialConnectAttempts(INITIAL_CONNECT_ATTEMPTS.resolveModelAttribute(context, model).asInt());
        incompleteCF.setInitialMessagePacketSize(INITIAL_MESSAGE_PACKET_SIZE.resolveModelAttribute(context, model).asInt());
        incompleteCF.setMaxRetryInterval(MAX_RETRY_INTERVAL.resolveModelAttribute(context, model).asLong());
        incompleteCF.setMinLargeMessageSize(MIN_LARGE_MESSAGE_SIZE.resolveModelAttribute(context, model).asInt());
        incompleteCF.setPreAcknowledge(PRE_ACKNOWLEDGE.resolveModelAttribute(context, model).asBoolean());
        incompleteCF.setProducerMaxRate(PRODUCER_MAX_RATE.resolveModelAttribute(context, model).asInt());
        incompleteCF.setProducerWindowSize(PRODUCER_WINDOW_SIZE.resolveModelAttribute(context, model).asInt());
        incompleteCF.setReconnectAttempts(RECONNECT_ATTEMPTS.resolveModelAttribute(context, model).asInt());
        incompleteCF.setRetryInterval(RETRY_INTERVAL.resolveModelAttribute(context, model).asLong());
        incompleteCF.setRetryIntervalMultiplier(RETRY_INTERVAL_MULTIPLIER.resolveModelAttribute(context, model).asDouble());
        incompleteCF.setScheduledThreadPoolMaxSize(SCHEDULED_THREAD_POOL_MAX_SIZE.resolveModelAttribute(context, model).asInt());
        incompleteCF.setThreadPoolMaxSize(THREAD_POOL_MAX_SIZE.resolveModelAttribute(context, model).asInt());
        incompleteCF.setTransactionBatchSize(TRANSACTION_BATCH_SIZE.resolveModelAttribute(context, model).asInt());
        incompleteCF.setUseGlobalPools(USE_GLOBAL_POOLS.resolveModelAttribute(context, model).asBoolean());
        return incompleteCF;
    }

    @Override
    public ConnectionFactory completeConnectionFactory(ActiveMQServer activeMQServer, ConnectionFactory toBecompletedConnectionFactory, String discoveryGroupName, List<String> connectors) throws StartException {
        HornetQConnectionFactory uncompletedConnectionFactory = (HornetQConnectionFactory) toBecompletedConnectionFactory;
        DiscoveryGroupConfiguration discoveryGroupConfiguration = null;
        if (discoveryGroupName != null) {
            if (activeMQServer.getConfiguration().getDiscoveryGroupConfigurations().containsKey(discoveryGroupName)) {
                discoveryGroupConfiguration = translateDiscoveryGroupConfiguration(activeMQServer.getConfiguration().getDiscoveryGroupConfigurations().get(discoveryGroupName));
            } else {
                throw MessagingLogger.ROOT_LOGGER.discoveryGroupIsNotDefined(discoveryGroupName);
            }
        }
        TransportConfiguration[] transportConfigurations = translateTransportGroupConfigurations(activeMQServer.getConfiguration().getConnectorConfigurations(), connectors);
        JMSFactoryType factoryType = JMSFactoryType.valueOf(uncompletedConnectionFactory.getFactoryType());
        HornetQConnectionFactory connectionFactory;
        if (uncompletedConnectionFactory.isHA()) {

            if (discoveryGroupConfiguration != null) {
                connectionFactory = HornetQJMSClient.createConnectionFactoryWithHA(discoveryGroupConfiguration, factoryType);
            } else {
                connectionFactory = HornetQJMSClient.createConnectionFactoryWithHA(factoryType, transportConfigurations);
            }
        } else {
            if (discoveryGroupConfiguration != null) {
                connectionFactory = HornetQJMSClient.createConnectionFactoryWithoutHA(discoveryGroupConfiguration, factoryType);
            } else {
                connectionFactory = HornetQJMSClient.createConnectionFactoryWithoutHA(factoryType, transportConfigurations);
            }
        }

        connectionFactory.setAutoGroup(uncompletedConnectionFactory.isAutoGroup());
        connectionFactory.setBlockOnAcknowledge(uncompletedConnectionFactory.isBlockOnAcknowledge());
        connectionFactory.setBlockOnDurableSend(uncompletedConnectionFactory.isBlockOnDurableSend());
        connectionFactory.setBlockOnNonDurableSend(uncompletedConnectionFactory.isBlockOnNonDurableSend());
        connectionFactory.setCacheLargeMessagesClient(uncompletedConnectionFactory.isCacheLargeMessagesClient());
        connectionFactory.setCallFailoverTimeout(uncompletedConnectionFactory.getCallFailoverTimeout());
        connectionFactory.setCallTimeout(uncompletedConnectionFactory.getCallTimeout());
        connectionFactory.setClientFailureCheckPeriod(uncompletedConnectionFactory.getClientFailureCheckPeriod());
        connectionFactory.setClientID(uncompletedConnectionFactory.getClientID());
        connectionFactory.setCompressLargeMessage(uncompletedConnectionFactory.isCompressLargeMessage());
        connectionFactory.setConfirmationWindowSize(uncompletedConnectionFactory.getConfirmationWindowSize());
        connectionFactory.setConnectionLoadBalancingPolicyClassName(uncompletedConnectionFactory.getConnectionLoadBalancingPolicyClassName());
        connectionFactory.setConnectionTTL(uncompletedConnectionFactory.getConnectionTTL());
        connectionFactory.setConsumerMaxRate(uncompletedConnectionFactory.getConsumerMaxRate());
        connectionFactory.setConsumerWindowSize(uncompletedConnectionFactory.getConsumerWindowSize());
        connectionFactory.setConfirmationWindowSize(uncompletedConnectionFactory.getConfirmationWindowSize());
        connectionFactory.setDupsOKBatchSize(uncompletedConnectionFactory.getDupsOKBatchSize());
        connectionFactory.setFailoverOnInitialConnection(uncompletedConnectionFactory.isFailoverOnInitialConnection());
        connectionFactory.setGroupID(uncompletedConnectionFactory.getGroupID());
        connectionFactory.setInitialConnectAttempts(uncompletedConnectionFactory.getInitialConnectAttempts());
        connectionFactory.setInitialMessagePacketSize(uncompletedConnectionFactory.getInitialMessagePacketSize());
        connectionFactory.setMaxRetryInterval(uncompletedConnectionFactory.getMaxRetryInterval());
        connectionFactory.setMinLargeMessageSize(uncompletedConnectionFactory.getMinLargeMessageSize());
        connectionFactory.setPreAcknowledge(uncompletedConnectionFactory.isPreAcknowledge());
        connectionFactory.setProducerMaxRate(uncompletedConnectionFactory.getProducerMaxRate());
        connectionFactory.setProducerWindowSize(uncompletedConnectionFactory.getProducerWindowSize());
        connectionFactory.setReconnectAttempts(uncompletedConnectionFactory.getReconnectAttempts());
        connectionFactory.setRetryInterval(uncompletedConnectionFactory.getRetryInterval());
        connectionFactory.setRetryIntervalMultiplier(uncompletedConnectionFactory.getRetryIntervalMultiplier());
        connectionFactory.setScheduledThreadPoolMaxSize(uncompletedConnectionFactory.getScheduledThreadPoolMaxSize());
        connectionFactory.setThreadPoolMaxSize(uncompletedConnectionFactory.getThreadPoolMaxSize());
        connectionFactory.setTransactionBatchSize(uncompletedConnectionFactory.getTransactionBatchSize());
        connectionFactory.setUseGlobalPools(uncompletedConnectionFactory.isUseGlobalPools());
        return connectionFactory;
    }

    private DiscoveryGroupConfiguration translateDiscoveryGroupConfiguration(org.apache.activemq.artemis.api.core.DiscoveryGroupConfiguration newDiscoveryGroupConfiguration) throws StartException {
        org.apache.activemq.artemis.api.core.BroadcastEndpointFactory newBroadcastEndpointFactory = newDiscoveryGroupConfiguration.getBroadcastEndpointFactory();
        BroadcastEndpointFactoryConfiguration legacyBroadcastEndpointFactory;

        if (newBroadcastEndpointFactory instanceof org.apache.activemq.artemis.api.core.UDPBroadcastEndpointFactory) {
            org.apache.activemq.artemis.api.core.UDPBroadcastEndpointFactory factory = (org.apache.activemq.artemis.api.core.UDPBroadcastEndpointFactory) newBroadcastEndpointFactory;
            legacyBroadcastEndpointFactory = new UDPBroadcastGroupConfiguration(
                    factory.getGroupAddress(),
                    factory.getGroupPort(),
                    factory.getLocalBindAddress(),
                    factory.getLocalBindPort());
        } else if (newBroadcastEndpointFactory instanceof org.apache.activemq.artemis.api.core.ChannelBroadcastEndpointFactory) {
            org.apache.activemq.artemis.api.core.ChannelBroadcastEndpointFactory factory = (org.apache.activemq.artemis.api.core.ChannelBroadcastEndpointFactory) newBroadcastEndpointFactory;
            legacyBroadcastEndpointFactory = new org.hornetq.api.core.JGroupsBroadcastGroupConfiguration(
                    factory.getChannel(),
                    factory.getChannelName());
        } else {
            throw MessagingLogger.ROOT_LOGGER.unsupportedBroadcastGroupConfigurationForLegacy(newBroadcastEndpointFactory.getClass().getName());
        }

        return new DiscoveryGroupConfiguration(newDiscoveryGroupConfiguration.getName(),
                newDiscoveryGroupConfiguration.getRefreshTimeout(),
                newDiscoveryGroupConfiguration.getDiscoveryInitialWaitTimeout(),
                legacyBroadcastEndpointFactory);
    }

    private TransportConfiguration[] translateTransportGroupConfigurations(Map<String, org.apache.activemq.artemis.api.core.TransportConfiguration> connectorConfigurations, List<String> connectors) throws StartException {
        List<TransportConfiguration> legacyConnectorConfigurations = new ArrayList<>();

        for (String connectorName : connectors) {
            org.apache.activemq.artemis.api.core.TransportConfiguration newTransportConfiguration = connectorConfigurations.get(connectorName);
            String legacyFactoryClassName = translateFactoryClassName(newTransportConfiguration.getFactoryClassName());
            Map legacyParams = translateParams(newTransportConfiguration.getParams());
            TransportConfiguration legacyTransportConfiguration = new TransportConfiguration(
                    legacyFactoryClassName,
                    legacyParams,
                    newTransportConfiguration.getName());

            legacyConnectorConfigurations.add(legacyTransportConfiguration);
        }

        return legacyConnectorConfigurations.toArray(new TransportConfiguration[legacyConnectorConfigurations.size()]);
    }

    private String translateFactoryClassName(String newFactoryClassName) throws StartException {
        if (newFactoryClassName.equals(org.apache.activemq.artemis.core.remoting.impl.netty.NettyConnectorFactory.class.getName())) {
            return org.hornetq.core.remoting.impl.netty.NettyConnectorFactory.class.getName();
        } else {
            throw MessagingLogger.ROOT_LOGGER.unsupportedConnectorFactoryForLegacy(newFactoryClassName.getClass().getName());
        }
    }

    private Map translateParams(Map<String, Object> newParams) {
        Map<String, Object> legacyParams = new HashMap<>();

        for (Map.Entry<String, Object> newEntry : newParams.entrySet()) {
            String newKey = newEntry.getKey();
            Object value = newEntry.getValue();
            String legacyKey = PARAM_KEY_MAPPING.getOrDefault(newKey, newKey);
            if (org.apache.activemq.artemis.core.remoting.impl.netty.TransportConstants.ACTIVEMQ_SERVER_NAME.equals(legacyKey)) {
                // property specific to ActiveMQ that can not be mapped to HornetQ
                continue;
            }
            legacyParams.put(legacyKey, value);
        }
        return legacyParams;
    }

    private JMSFactoryType getType(LegacyConnectionFactoryDefinition.HornetQConnectionFactoryType type) {
        switch (type) {
            case GENERIC:
                return CF;
            case TOPIC:
                return TOPIC_CF;
            case QUEUE:
                return QUEUE_CF;
            case XA_GENERIC:
                return XA_CF;
            case XA_QUEUE:
                return QUEUE_XA_CF;
            case XA_TOPIC:
                return TOPIC_XA_CF;
        }
        return CF;
    }

    @Override
    public void createQueue(ServiceTarget serviceTarget, String name, List<String> legacyEntries) {
        Queue legacyQueue = HornetQJMSClient.createQueue(name);
        for (String legacyEntry : legacyEntries) {
            BinderServiceUtil.installBinderService(serviceTarget, legacyEntry, legacyQueue);
        }
    }

    @Override
    public void createTopic(ServiceTarget serviceTarget, String name, List<String> legacyEntries) {
        Topic legacyTopic = HornetQJMSClient.createTopic(name);
        for (String legacyEntry : legacyEntries) {
            BinderServiceUtil.installBinderService(serviceTarget, legacyEntry, legacyTopic);
        }
    }

}
