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
import java.util.HashMap;
import java.util.Map;
import javax.jms.ConnectionFactory;
import org.apache.activemq.artemis.api.core.DiscoveryGroupConfiguration;
import org.apache.activemq.artemis.api.core.TransportConfiguration;
import org.apache.activemq.artemis.api.jms.ActiveMQJMSClient;
import org.apache.activemq.artemis.api.jms.JMSFactoryType;
import org.apache.activemq.artemis.jms.client.ActiveMQConnectionFactory;
import org.jboss.as.network.OutboundSocketBinding;
import org.jboss.as.network.SocketBinding;
import org.jboss.msc.inject.Injector;
import org.jboss.msc.inject.MapInjector;
import org.jboss.msc.service.Service;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;
import org.wildfly.extension.messaging.activemq.TransportConfigOperationHandlers;
import org.wildfly.extension.messaging.activemq.logging.MessagingLogger;

/**
 *
 * @author Emmanuel Hugonnet (c) 2018 Red Hat, inc.
 */
public class ClientConnectionFactoryService implements Service<ConnectionFactory> {

    private final boolean ha;
    private final DiscoveryGroupConfiguration groupConfiguration;
    private final TransportConfiguration[] connectors;
    private final JMSFactoryType type;
    private Map<String, SocketBinding> socketBindings = new HashMap<>();
    private Map<String, OutboundSocketBinding> outboundSocketBindings = new HashMap<>();
    private Map<String, SocketBinding> groupBindings = new HashMap<>();
    private ActiveMQConnectionFactory factory;

    ClientConnectionFactoryService(DiscoveryGroupConfiguration groupConfiguration, JMSFactoryType type, boolean ha) {
        this(ha, type, groupConfiguration, null);
    }

    ClientConnectionFactoryService(TransportConfiguration[] connectors,
            JMSFactoryType type,
            boolean ha) {
        this(ha, type, null, connectors);
    }

    private ClientConnectionFactoryService(boolean ha,
            JMSFactoryType type,
            DiscoveryGroupConfiguration groupConfiguration,
            TransportConfiguration[] connectors) {
        assert (connectors != null && connectors.length > 0) || groupConfiguration != null;
        this.ha = ha;
        this.type = type;
        this.groupConfiguration = groupConfiguration;
        this.connectors = connectors;
    }

    @Override
    public void start(StartContext context) throws StartException {
        try {
            if (connectors != null && connectors.length > 0) {
                TransportConfigOperationHandlers.processConnectorBindings(Arrays.asList(connectors), socketBindings, outboundSocketBindings);
                if(ha) {
                   factory = ActiveMQJMSClient.createConnectionFactoryWithHA(type, connectors);
                } else {
                    factory = ActiveMQJMSClient.createConnectionFactoryWithoutHA(type, connectors);
                }
            } else {
                 if(ha) {
                   factory = ActiveMQJMSClient.createConnectionFactoryWithHA(groupConfiguration, type);
                } else {
                    factory = ActiveMQJMSClient.createConnectionFactoryWithoutHA(groupConfiguration, type);
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

    Injector<SocketBinding> getSocketBindingInjector(String name) {
        return new MapInjector<String, SocketBinding>(socketBindings, name);
    }

    Injector<OutboundSocketBinding> getOutboundSocketBindingInjector(String name) {
        return new MapInjector<String, OutboundSocketBinding>(outboundSocketBindings, name);
    }

    Injector<SocketBinding> getGroupBindingInjector(String name) {
        return new MapInjector<String, SocketBinding>(groupBindings, name);
    }
}
