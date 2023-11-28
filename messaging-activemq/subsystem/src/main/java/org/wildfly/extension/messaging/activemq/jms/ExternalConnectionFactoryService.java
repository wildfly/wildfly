/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.extension.messaging.activemq.jms;

import java.util.Arrays;
import java.util.Collections;
import java.util.Map;
import java.util.function.Supplier;
import jakarta.jms.ConnectionFactory;
import javax.net.ssl.SSLContext;
import org.apache.activemq.artemis.api.core.DiscoveryGroupConfiguration;
import org.apache.activemq.artemis.api.core.TransportConfiguration;
import org.apache.activemq.artemis.api.jms.ActiveMQJMSClient;
import org.apache.activemq.artemis.api.jms.JMSFactoryType;
import org.apache.activemq.artemis.jms.client.ActiveMQConnectionFactory;
import org.jboss.as.network.ManagedBinding;
import org.jboss.as.network.OutboundSocketBinding;
import org.jboss.as.network.SocketBinding;
import org.jboss.msc.service.Service;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;
import org.wildfly.extension.messaging.activemq.JGroupsDiscoveryGroupAdd;
import org.wildfly.extension.messaging.activemq.SocketDiscoveryGroupAdd;
import org.wildfly.extension.messaging.activemq.TransportConfigOperationHandlers;
import org.wildfly.extension.messaging.activemq.broadcast.BroadcastCommandDispatcherFactory;
import org.wildfly.extension.messaging.activemq._private.MessagingLogger;

/**
 *
 * @author Emmanuel Hugonnet (c) 2018 Red Hat, inc.
 */
public class ExternalConnectionFactoryService implements Service<ConnectionFactory> {

    private final boolean ha;
    private final boolean enable1Prefixes;
    private final DiscoveryGroupConfiguration groupConfiguration;
    private final TransportConfiguration[] connectors;
    private final JMSFactoryType type;

    private final Map<String, Supplier<SocketBinding>> socketBindings;
    private final Map<String, Supplier<OutboundSocketBinding>> outboundSocketBindings;
    private final Map<String, Supplier<SSLContext>> sslContexts;
    private final Map<String, Supplier<SocketBinding>> groupBindings;
    // mapping between the {discovery}-groups and the cluster names they use
    private final Map<String, String> clusterNames;
    // mapping between the {discovery}-groups and the command dispatcher factory they use
    private final Map<String, Supplier<BroadcastCommandDispatcherFactory>> commandDispatcherFactories;
    private ActiveMQConnectionFactory factory;
    private final ExternalConnectionFactoryConfiguration config;

    ExternalConnectionFactoryService(DiscoveryGroupConfiguration groupConfiguration,
            Map<String, Supplier<BroadcastCommandDispatcherFactory>> commandDispatcherFactories,
            Map<String, Supplier<SocketBinding>> groupBindings, Map<String, String> clusterNames, JMSFactoryType type, boolean ha, boolean enable1Prefixes, ExternalConnectionFactoryConfiguration config) {
        this(ha, enable1Prefixes, type, groupConfiguration, Collections.emptyMap(), Collections.emptyMap(),commandDispatcherFactories, groupBindings, Collections.emptyMap(), clusterNames, null, config);
    }

    ExternalConnectionFactoryService(TransportConfiguration[] connectors, Map<String, Supplier<SocketBinding>> socketBindings,
            Map<String, Supplier<OutboundSocketBinding>> outboundSocketBindings, Map<String, Supplier<SSLContext>> sslContexts, JMSFactoryType type, boolean ha, boolean enable1Prefixes, ExternalConnectionFactoryConfiguration config) {
        this(ha, enable1Prefixes, type, null, socketBindings, outboundSocketBindings, Collections.emptyMap(), Collections.emptyMap(), sslContexts, Collections.emptyMap(), connectors, config);
    }

    private ExternalConnectionFactoryService(boolean ha,
            boolean enable1Prefixes,
            JMSFactoryType type,
            DiscoveryGroupConfiguration groupConfiguration,
            Map<String, Supplier<SocketBinding>> socketBindings,
            Map<String, Supplier<OutboundSocketBinding>> outboundSocketBindings,
            Map<String, Supplier<BroadcastCommandDispatcherFactory>> commandDispatcherFactories,
            Map<String, Supplier<SocketBinding>> groupBindings,
            Map<String, Supplier<SSLContext>> sslContexts,
            Map<String, String> clusterNames,
            TransportConfiguration[] connectors,
            ExternalConnectionFactoryConfiguration config) {
        assert (connectors != null && connectors.length > 0) || groupConfiguration != null;
        this.ha = ha;
        this.enable1Prefixes = enable1Prefixes;
        this.type = type;
        this.groupConfiguration = groupConfiguration;
        this.connectors = connectors;
        this.socketBindings = socketBindings;
        this.outboundSocketBindings = outboundSocketBindings;
        this.clusterNames = clusterNames;
        this.commandDispatcherFactories = commandDispatcherFactories;
        this.groupBindings = groupBindings;
        this.sslContexts = sslContexts;
        this.config = config;
    }

    @Override
    public void start(StartContext context) throws StartException {
        try {
            if (connectors != null && connectors.length > 0) {
                TransportConfigOperationHandlers.processConnectorBindings(Arrays.asList(connectors), socketBindings, outboundSocketBindings);
                if (ha) {
                    factory = ActiveMQJMSClient.createConnectionFactoryWithHA(type, connectors);
                } else {
                    factory = ActiveMQJMSClient.createConnectionFactoryWithoutHA(type, connectors);
                }
            } else {
                final String name = groupConfiguration.getName();
                final String key = "discovery" + name;
                final DiscoveryGroupConfiguration config;
                if (commandDispatcherFactories.containsKey(key)) {
                    BroadcastCommandDispatcherFactory commandDispatcherFactory = commandDispatcherFactories.get(key).get();
                    String clusterName = clusterNames.get(key);
                    config = JGroupsDiscoveryGroupAdd.createDiscoveryGroupConfiguration(name, groupConfiguration, commandDispatcherFactory, clusterName);
                } else {
                    final SocketBinding binding = groupBindings.get(key).get();
                    if (binding == null) {
                        throw MessagingLogger.ROOT_LOGGER.failedToFindDiscoverySocketBinding(name);
                    }
                    config = SocketDiscoveryGroupAdd.createDiscoveryGroupConfiguration(name, groupConfiguration, binding);
                    binding.getSocketBindings().getNamedRegistry().registerBinding(ManagedBinding.Factory.createSimpleManagedBinding(binding));
                }
                if (ha) {
                    factory = ActiveMQJMSClient.createConnectionFactoryWithHA(config, type);
                } else {
                    factory = ActiveMQJMSClient.createConnectionFactoryWithoutHA(config, type);
                }
            }
            if(config != null) {
                factory.setAutoGroup(config.isAutoGroup());
                factory.setBlockOnAcknowledge(config.isBlockOnAcknowledge());
                factory.setBlockOnDurableSend(config.isBlockOnDurableSend());
                factory.setBlockOnNonDurableSend(config.isBlockOnNonDurableSend());
                factory.setCacheLargeMessagesClient(config.isCacheLargeMessagesClient());
                factory.setCallFailoverTimeout(config.getCallFailoverTimeout());
                factory.setCallTimeout(config.getCallTimeout());
                factory.setClientID(config.getClientID());
                factory.setClientFailureCheckPeriod(config.getClientFailureCheckPeriod());
                factory.setCompressLargeMessage(config.isCompressLargeMessages());
                factory.setConfirmationWindowSize(config.getConfirmationWindowSize());
                factory.setConnectionTTL(config.getConnectionTTL());
                factory.setConsumerMaxRate(config.getConsumerMaxRate());
                factory.setConsumerWindowSize(config.getConsumerWindowSize());
                factory.setDeserializationBlackList(config.getDeserializationBlackList());
                factory.setDeserializationWhiteList(config.getDeserializationWhiteList());
                factory.setDupsOKBatchSize(config.getDupsOKBatchSize());
                factory.setEnableSharedClientID(config.isEnableSharedClientID());
                factory.setFailoverOnInitialConnection(config.isFailoverOnInitialConnection());
                factory.setGroupID(config.getGroupID());
                factory.setInitialMessagePacketSize(config.getInitialMessagePacketSize());
                factory.setMaxRetryInterval(config.getMaxRetryInterval());
                factory.setMinLargeMessageSize(config.getMinLargeMessageSize());
                factory.setPreAcknowledge(config.isPreAcknowledge());
                factory.setProducerMaxRate(config.getProducerMaxRate());
                factory.setProducerWindowSize(config.getProducerWindowSize());
                factory.setProtocolManagerFactoryStr(config.getProtocolManagerFactoryStr());
                factory.setConnectionLoadBalancingPolicyClassName(config.getLoadBalancingPolicyClassName());
                factory.setReconnectAttempts(config.getReconnectAttempts());
                factory.setRetryInterval(config.getRetryInterval());
                factory.setRetryIntervalMultiplier(config.getRetryIntervalMultiplier());
                factory.setScheduledThreadPoolMaxSize(config.getScheduledThreadPoolMaxSize());
                factory.setThreadPoolMaxSize(config.getThreadPoolMaxSize());
                factory.setTransactionBatchSize(config.getTransactionBatchSize());
                factory.setUseGlobalPools(config.isUseGlobalPools());
                factory.setUseTopologyForLoadBalancing(config.getUseTopologyForLoadBalancing());
            }
            factory.setEnable1xPrefixes(enable1Prefixes);
        } catch (Throwable e) {
            throw MessagingLogger.ROOT_LOGGER.failedToCreate(e, "connection-factory");
        }
    }

    @Override
    public void stop(StopContext context) {
        try {
            factory.close();
        } catch (Throwable e) {
            MessagingLogger.ROOT_LOGGER.failedToDestroy("connection-factory", "");
        }
    }

    @Override
    public ConnectionFactory getValue() throws IllegalStateException, IllegalArgumentException {
        return factory;
    }
}
