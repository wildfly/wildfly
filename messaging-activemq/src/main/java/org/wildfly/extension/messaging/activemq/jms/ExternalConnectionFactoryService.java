/*
 * Copyright 2018 JBoss by Red Hat.
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
import org.jboss.as.network.ManagedBinding;
import org.jboss.as.network.OutboundSocketBinding;
import org.jboss.as.network.SocketBinding;
import org.jboss.msc.service.Service;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;
import org.wildfly.clustering.dispatcher.CommandDispatcherFactory;
import org.wildfly.extension.messaging.activemq.DiscoveryGroupAdd;
import org.wildfly.extension.messaging.activemq.TransportConfigOperationHandlers;
import org.wildfly.extension.messaging.activemq.logging.MessagingLogger;

/**
 *
 * @author Emmanuel Hugonnet (c) 2018 Red Hat, inc.
 */
public class ExternalConnectionFactoryService implements Service<ConnectionFactory> {

    private final boolean ha;
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

    ExternalConnectionFactoryService(DiscoveryGroupConfiguration groupConfiguration,
            Map<String, Supplier<CommandDispatcherFactory>> commandDispatcherFactories,
            Map<String, Supplier<SocketBinding>> groupBindings, Map<String, String> clusterNames, JMSFactoryType type, boolean ha) {
        this(ha, type, groupConfiguration, Collections.emptyMap(), Collections.emptyMap(),commandDispatcherFactories, groupBindings, clusterNames, null);
    }

    ExternalConnectionFactoryService(TransportConfiguration[] connectors, Map<String, Supplier<SocketBinding>> socketBindings,
            Map<String, Supplier<OutboundSocketBinding>> outboundSocketBindings, JMSFactoryType type, boolean ha) {
        this(ha, type, null, socketBindings, outboundSocketBindings, Collections.emptyMap(), Collections.emptyMap(), Collections.emptyMap(), connectors);
    }

    private ExternalConnectionFactoryService(boolean ha,
            JMSFactoryType type,
            DiscoveryGroupConfiguration groupConfiguration,
            Map<String, Supplier<SocketBinding>> socketBindings,
            Map<String, Supplier<OutboundSocketBinding>> outboundSocketBindings,
            Map<String, Supplier<CommandDispatcherFactory>> commandDispatcherFactories,
            Map<String, Supplier<SocketBinding>> groupBindings,
            Map<String, String> clusterNames,
            TransportConfiguration[] connectors) {
        assert (connectors != null && connectors.length > 0) || groupConfiguration != null;
        this.ha = ha;
        this.type = type;
        this.groupConfiguration = groupConfiguration;
        this.connectors = connectors;
        this.socketBindings = socketBindings;
        this.outboundSocketBindings = outboundSocketBindings;
        this.clusterNames = clusterNames;
        this.commandDispatcherFactories = commandDispatcherFactories;
        this.groupBindings = groupBindings;
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
                    config = DiscoveryGroupAdd.createDiscoveryGroupConfiguration(name, groupConfiguration, commandDispatcherFactory, clusterName);
                } else {
                    final SocketBinding binding = groupBindings.get(key).get();
                    if (binding == null) {
                        throw MessagingLogger.ROOT_LOGGER.failedToFindDiscoverySocketBinding(name);
                    }
                    config = DiscoveryGroupAdd.createDiscoveryGroupConfiguration(name, groupConfiguration, binding);
                    binding.getSocketBindings().getNamedRegistry().registerBinding(ManagedBinding.Factory.createSimpleManagedBinding(binding));
                }
                if (ha) {
                    factory = ActiveMQJMSClient.createConnectionFactoryWithHA(config, type);
                } else {
                    factory = ActiveMQJMSClient.createConnectionFactoryWithoutHA(config, type);
                }
            }
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
