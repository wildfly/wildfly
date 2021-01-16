/*
 * Copyright 2018 Red Hat, Inc.
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
package org.wildfly.extension.messaging.activemq.jms;

import java.util.Arrays;
import java.util.Collections;
import java.util.Map;
import java.util.function.Supplier;
import javax.jms.ConnectionFactory;
import org.apache.activemq.artemis.api.core.DiscoveryGroupConfiguration;
import org.apache.activemq.artemis.api.core.TransportConfiguration;
import org.apache.activemq.artemis.api.jms.ActiveMQJMSClient;
import org.apache.activemq.artemis.api.jms.JMSFactoryType;
import org.apache.activemq.artemis.jms.client.ActiveMQConnectionFactory;
import org.apache.activemq.artemis.jms.server.config.ConnectionFactoryConfiguration;
import org.jboss.as.network.ManagedBinding;
import org.jboss.as.network.OutboundSocketBinding;
import org.jboss.as.network.SocketBinding;
import org.jboss.msc.service.Service;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;
import org.wildfly.clustering.spi.dispatcher.CommandDispatcherFactory;
import org.wildfly.extension.messaging.activemq.JGroupsDiscoveryGroupAdd;
import org.wildfly.extension.messaging.activemq.SocketDiscoveryGroupAdd;
import org.wildfly.extension.messaging.activemq.TransportConfigOperationHandlers;
import org.wildfly.extension.messaging.activemq.logging.MessagingLogger;

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
    private final Map<String, Supplier<SocketBinding>> groupBindings;
    // mapping between the {discovery}-groups and the cluster names they use
    private final Map<String, String> clusterNames;
    // mapping between the {discovery}-groups and the command dispatcher factory they use
    private final Map<String, Supplier<CommandDispatcherFactory>> commandDispatcherFactories;
    private ActiveMQConnectionFactory factory;
    private final ConnectionFactoryConfiguration config;

    ExternalConnectionFactoryService(DiscoveryGroupConfiguration groupConfiguration,
            Map<String, Supplier<CommandDispatcherFactory>> commandDispatcherFactories,
            Map<String, Supplier<SocketBinding>> groupBindings, Map<String, String> clusterNames, JMSFactoryType type, boolean ha, boolean enable1Prefixes, ConnectionFactoryConfiguration config) {
        this(ha, enable1Prefixes, type, groupConfiguration, Collections.emptyMap(), Collections.emptyMap(),commandDispatcherFactories, groupBindings, clusterNames, null, config);
    }

    ExternalConnectionFactoryService(TransportConfiguration[] connectors, Map<String, Supplier<SocketBinding>> socketBindings,
            Map<String, Supplier<OutboundSocketBinding>> outboundSocketBindings, JMSFactoryType type, boolean ha, boolean enable1Prefixes, ConnectionFactoryConfiguration config) {
        this(ha, enable1Prefixes, type, null, socketBindings, outboundSocketBindings, Collections.emptyMap(), Collections.emptyMap(), Collections.emptyMap(), connectors, config);
    }

    private ExternalConnectionFactoryService(boolean ha,
            boolean enable1Prefixes,
            JMSFactoryType type,
            DiscoveryGroupConfiguration groupConfiguration,
            Map<String, Supplier<SocketBinding>> socketBindings,
            Map<String, Supplier<OutboundSocketBinding>> outboundSocketBindings,
            Map<String, Supplier<CommandDispatcherFactory>> commandDispatcherFactories,
            Map<String, Supplier<SocketBinding>> groupBindings,
            Map<String, String> clusterNames,
            TransportConfiguration[] connectors,
            ConnectionFactoryConfiguration config) {
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
                    CommandDispatcherFactory commandDispatcherFactory = commandDispatcherFactories.get(key).get();
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
