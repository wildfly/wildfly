/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2015, Red Hat, Inc., and individual contributors
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

package org.wildfly.extension.messaging.activemq.jms.legacy;

import static org.wildfly.extension.messaging.activemq.CommonAttributes.LEGACY;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.jms.ConnectionFactory;

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
import org.jboss.msc.service.Service;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceTarget;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;
import org.jboss.msc.value.InjectedValue;
import org.wildfly.extension.messaging.activemq.ActiveMQActivationService;
import org.wildfly.extension.messaging.activemq.jms.JMSServices;
import org.wildfly.extension.messaging.activemq.logging.MessagingLogger;

/**
 * @author <a href="http://jmesnil.net/">Jeff Mesnil</a> (c) 2015 Red Hat inc.
 */
public class LegacyConnectionFactoryService implements Service<ConnectionFactory> {

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

    private final InjectedValue<ActiveMQServer> injectedActiveMQServer = new InjectedValue<ActiveMQServer>();
    private final HornetQConnectionFactory uncompletedConnectionFactory;
    private final String discoveryGroupName;
    private final List<String> connectors;

    private HornetQConnectionFactory connectionFactory;

    public LegacyConnectionFactoryService(HornetQConnectionFactory uncompletedConnectionFactory, String discoveryGroupName, List<String> connectors) {

        this.uncompletedConnectionFactory = uncompletedConnectionFactory;
        this.discoveryGroupName = discoveryGroupName;
        this.connectors = connectors;
    }

    @Override
    public void start(StartContext context) throws StartException {
        ActiveMQServer activeMQServer = injectedActiveMQServer.getValue();

        DiscoveryGroupConfiguration discoveryGroupConfiguration = null;
        if (discoveryGroupName != null) {
            if (activeMQServer.getConfiguration().getDiscoveryGroupConfigurations().keySet().contains(discoveryGroupName)) {
                discoveryGroupConfiguration = translateDiscoveryGroupConfiguration(activeMQServer.getConfiguration().getDiscoveryGroupConfigurations().get(discoveryGroupName));
            } else {
                throw MessagingLogger.ROOT_LOGGER.discoveryGroupIsNotDefined(discoveryGroupName);
            }
        }
        TransportConfiguration[] transportConfigurations = translateTransportGroupConfigurations(activeMQServer.getConfiguration().getConnectorConfigurations(), connectors);
        JMSFactoryType factoryType = JMSFactoryType.valueOf(uncompletedConnectionFactory.getFactoryType());

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
    }

    @Override
    public void stop(StopContext context) {
        connectionFactory = null;
    }

    @Override
    public ConnectionFactory getValue() throws IllegalStateException, IllegalArgumentException {
        return connectionFactory;
    }

    public static LegacyConnectionFactoryService installService(final String name,
                                                                final ServiceName activeMQServerServiceName,
                                                                final ServiceTarget serviceTarget,
                                                                final HornetQConnectionFactory uncompletedConnectionFactory,
                                                                final String discoveryGroupName,
                                                                final List<String> connectors) {
        final LegacyConnectionFactoryService service = new LegacyConnectionFactoryService(uncompletedConnectionFactory, discoveryGroupName, connectors);
        final ServiceName serviceName = JMSServices.getConnectionFactoryBaseServiceName(activeMQServerServiceName).append(LEGACY, name);

        serviceTarget.addService(serviceName, service)
                .addDependency(ActiveMQActivationService.getServiceName(activeMQServerServiceName))
                .addDependency(activeMQServerServiceName, ActiveMQServer.class, service.injectedActiveMQServer)
                .setInitialMode(ServiceController.Mode.PASSIVE)
                .install();
        return service;
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
        List<org.hornetq.api.core.TransportConfiguration> legacyConnectorConfigurations = new ArrayList<>();

        for (String connectorName : connectors) {
            org.apache.activemq.artemis.api.core.TransportConfiguration newTransportConfiguration = connectorConfigurations.get(connectorName);
            String legacyFactoryClassName = translateFactoryClassName(newTransportConfiguration.getFactoryClassName());
            Map legacyParams = translateParams(newTransportConfiguration.getParams());
            org.hornetq.api.core.TransportConfiguration legacyTransportConfiguration = new org.hornetq.api.core.TransportConfiguration(
                    legacyFactoryClassName,
                    legacyParams,
                    newTransportConfiguration.getName());

            legacyConnectorConfigurations.add(legacyTransportConfiguration);
        }

        return legacyConnectorConfigurations.toArray(new org.hornetq.api.core.TransportConfiguration[legacyConnectorConfigurations.size()]);
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
}
