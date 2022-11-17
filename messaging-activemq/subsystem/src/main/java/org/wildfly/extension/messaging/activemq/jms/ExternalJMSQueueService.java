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


import java.util.Collection;
import jakarta.jms.ConnectionFactory;
import jakarta.jms.JMSException;
import jakarta.jms.Queue;
import javax.naming.NamingException;
import org.apache.activemq.artemis.api.core.client.ClientSessionFactory;
import org.apache.activemq.artemis.api.core.client.ClusterTopologyListener;
import org.apache.activemq.artemis.api.core.client.ServerLocator;
import org.apache.activemq.artemis.api.core.client.TopologyMember;
import org.apache.activemq.artemis.core.client.impl.TopologyMemberImpl;
import org.apache.activemq.artemis.jms.client.ActiveMQConnectionFactory;
import org.apache.activemq.artemis.jms.client.ActiveMQDestination;
import org.apache.activemq.artemis.ra.ActiveMQRAConnectionFactory;
import org.apache.activemq.artemis.spi.core.remoting.ClientProtocolManagerFactory;
import org.jboss.as.connector.util.ConnectorServices;
import org.jboss.as.naming.NamingContext;
import org.jboss.as.naming.NamingStore;
import org.jboss.as.naming.service.NamingService;

import org.jboss.msc.service.Service;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceTarget;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;
import org.jboss.msc.value.InjectedValue;
import org.wildfly.extension.messaging.activemq.logging.MessagingLogger;

/**
 * Service responsible for creating and destroying a client {@code jakarta.jms.Queue}.
 *
 * @author Emmanuel Hugonnet (c) 2018 Red Hat, inc.
 */
public class ExternalJMSQueueService implements Service<Queue> {

    static final String JMS_QUEUE_PREFIX = "jms.queue.";
    private final String queueName;
    private final DestinationConfiguration config;
    private final InjectedValue<NamingStore> namingStoreInjector = new InjectedValue<NamingStore>();
    private final InjectedValue<ExternalPooledConnectionFactoryService> pcfInjector = new InjectedValue<ExternalPooledConnectionFactoryService>();
    private Queue queue;
    private ClientSessionFactory sessionFactory;

    private ExternalJMSQueueService(final String queueName, final boolean enabledAMQ1Prefix) {
        this.queueName = enabledAMQ1Prefix ? JMS_QUEUE_PREFIX + queueName : queueName;
        this.config = null;
    }

    private ExternalJMSQueueService(final DestinationConfiguration config, final boolean enabledAMQ1Prefix) {
        this.queueName = enabledAMQ1Prefix ? JMS_QUEUE_PREFIX + config.getName() : config.getName();
        this.config = config;
    }

    @Override
    public synchronized void start(final StartContext context) throws StartException {
        NamingStore namingStore = namingStoreInjector.getOptionalValue();
        if(namingStore!= null) {
            final Queue managementQueue = config.getManagementQueue();
            final NamingContext storeBaseContext = new NamingContext(namingStore, null);
            try {
                ConnectionFactory cf = (ConnectionFactory) storeBaseContext.lookup(pcfInjector.getValue().getBindInfo().getAbsoluteJndiName());
                if (cf instanceof ActiveMQRAConnectionFactory) {
                    final ActiveMQRAConnectionFactory raCf = (ActiveMQRAConnectionFactory) cf;
                    final ServerLocator locator = raCf.getDefaultFactory().getServerLocator();
                    final ClientProtocolManagerFactory protocolManagerFactory = locator.getProtocolManagerFactory();
                    sessionFactory = locator.createSessionFactory();
                    ClusterTopologyListener listener = new ClusterTopologyListener() {
                        @Override
                        public void nodeUP(TopologyMember member, boolean last) {
                            try (ActiveMQConnectionFactory factory = new ActiveMQConnectionFactory(false, member.getLive())) {
                                factory.getServerLocator().setProtocolManagerFactory(protocolManagerFactory);
                                MessagingLogger.ROOT_LOGGER.infof("Creating queue %s on node UP %s - %s", queueName, member.getNodeId(), member.getLive().toString());
                                config.createQueue(factory, managementQueue, queueName);
                            } catch (JMSException | StartException ex) {
                                MessagingLogger.ROOT_LOGGER.errorf(ex, "Creating queue %s on node UP %s failed", queueName, member.getLive().toString());
                                throw new RuntimeException(ex);
                            }
                            if (member.getBackup() != null) {
                                try (ActiveMQConnectionFactory factory = new ActiveMQConnectionFactory(false, member.getBackup())) {
                                    factory.getServerLocator().setProtocolManagerFactory(protocolManagerFactory);
                                    MessagingLogger.ROOT_LOGGER.infof("Creating queue %s on backup node UP %s - %s", queueName, member.getNodeId(), member.getBackup().toString());
                                    config.createQueue(factory, managementQueue, queueName);
                                } catch (JMSException | StartException ex) {
                                    throw new RuntimeException(ex);
                                }
                            }
                        }

                        @Override
                        public void nodeDown(long eventUID, String nodeID) {}
                    };
                    Collection<TopologyMemberImpl> members = locator.getTopology().getMembers();
                    if (members == null || members.isEmpty() || members.size() == 1) {
                        config.createQueue(cf, managementQueue, queueName);
                    }
                    locator.addClusterTopologyListener(listener);
                } else {
                    config.createQueue(cf, managementQueue, queueName);
                }
            } catch (Exception ex) {
                MessagingLogger.ROOT_LOGGER.errorf(ex, "Error starting the external queue service %s", ex.getMessage());
                throw new StartException(ex);
            } finally {
                try {
                    storeBaseContext.close();
                } catch (NamingException ex) {
                    MessagingLogger.ROOT_LOGGER.tracef(ex, "Error closing the naming context %s", ex.getMessage());
                }
            }
        }
        queue = ActiveMQDestination.createQueue(queueName);
    }


    @Override
    public synchronized void stop(final StopContext context) {
        if(sessionFactory != null) {
            sessionFactory.close();
        }
    }

    @Override
    public Queue getValue() throws IllegalStateException, IllegalArgumentException {
        return queue;
    }

    public static Service<Queue> installService(final String name, final ServiceTarget serviceTarget, final ServiceName serviceName, final boolean enabledAMQ1Prefix) {
        final ExternalJMSQueueService service = new ExternalJMSQueueService(name, enabledAMQ1Prefix);
        final ServiceBuilder<Queue> serviceBuilder = serviceTarget.addService(serviceName, service);
        serviceBuilder.install();
        return service;
    }

    public static Service<Queue> installRuntimeQueueService(final DestinationConfiguration config, final ServiceTarget serviceTarget, final ServiceName pcf, final boolean enabledAMQ1Prefix) {
        final ExternalJMSQueueService service = new ExternalJMSQueueService(config, enabledAMQ1Prefix);
        final ServiceBuilder<Queue> serviceBuilder = serviceTarget.addService(config.getDestinationServiceName(), service);
        serviceBuilder.addDependency(NamingService.SERVICE_NAME, NamingStore.class, service.namingStoreInjector);
        serviceBuilder.addDependency(pcf, ExternalPooledConnectionFactoryService.class, service.pcfInjector);
        serviceBuilder.requires(ConnectorServices.RESOURCE_ADAPTER_SERVICE_PREFIX.append(config.getResourceAdapter()));
        serviceBuilder.install();
        return service;
    }

}
