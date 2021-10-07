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
import javax.jms.ConnectionFactory;
import javax.jms.JMSException;
import javax.jms.Queue;
import javax.jms.Topic;
import javax.naming.NamingException;
import org.apache.activemq.artemis.api.core.client.ClientSessionFactory;
import org.apache.activemq.artemis.api.core.client.ClusterTopologyListener;
import org.apache.activemq.artemis.api.core.client.ServerLocator;
import org.apache.activemq.artemis.api.core.client.TopologyMember;
import org.apache.activemq.artemis.api.jms.ActiveMQJMSClient;
import org.apache.activemq.artemis.core.client.impl.TopologyMemberImpl;
import org.apache.activemq.artemis.jms.client.ActiveMQConnectionFactory;
import org.apache.activemq.artemis.ra.ActiveMQRAConnectionFactory;
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
 * Service responsible for creating and destroying a client {@code javax.jms.Topic}.
 *
 * @author Emmanuel Hugonnet (c) 2018 Red Hat, inc.
 */
public class ExternalJMSTopicService implements Service<Topic> {
    static final String JMS_TOPIC_PREFIX = "jms.topic.";

    private final InjectedValue<NamingStore> namingStoreInjector = new InjectedValue<NamingStore>();
    private final InjectedValue<ExternalPooledConnectionFactoryService> pcfInjector = new InjectedValue<ExternalPooledConnectionFactoryService>();

    private Topic topic;
    private final String topicName;
    private final DestinationConfiguration config;
    private ClientSessionFactory sessionFactory;

    private ExternalJMSTopicService(String name) {
        this.topicName = JMS_TOPIC_PREFIX + name;
        this.config = null;
    }

    private ExternalJMSTopicService(final DestinationConfiguration config) {
        this.topicName = JMS_TOPIC_PREFIX + config.getName();
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
                    final String protocolManagerFactor = locator.getProtocolManagerFactory().getClass().getName();
                    sessionFactory = locator.createSessionFactory();
                    ClusterTopologyListener listener = new ClusterTopologyListener() {
                        @Override
                        public void nodeUP(TopologyMember member, boolean last) {
                            try (ActiveMQConnectionFactory factory = new ActiveMQConnectionFactory(false, member.getLive())) {
                                factory.setProtocolManagerFactoryStr(protocolManagerFactor);
                                MessagingLogger.ROOT_LOGGER.infof("Creating topic %s on node UP %s - %s", topicName, member.getNodeId(), member.getLive().toJson());
                                config.createTopic(factory, managementQueue, topicName);
                            } catch (JMSException | StartException ex) {
                                MessagingLogger.ROOT_LOGGER.errorf(ex, "Creating topic %s on node UP %s failed", topicName, member.getLive().toJson());
                                throw new RuntimeException(ex);
                            }
                            if (member.getBackup() != null) {
                                try (ActiveMQConnectionFactory factory = new ActiveMQConnectionFactory(false, member.getBackup())) {
                                    factory.setProtocolManagerFactoryStr(protocolManagerFactor);
                                    MessagingLogger.ROOT_LOGGER.infof("Creating topic %s on backup node UP %s - %s", topicName, member.getNodeId(), member.getBackup().toJson());
                                    config.createTopic(factory, managementQueue, topicName);
                                } catch (JMSException | StartException ex) {
                                    throw new RuntimeException(ex);
                                }
                            }
                        }

                        @Override
                        public void nodeDown(long eventUID, String nodeID) {}
                    };
                    locator.addClusterTopologyListener(listener);
                    Collection<TopologyMemberImpl> members = locator.getTopology().getMembers();
                    if (members == null || members.isEmpty()) {
                        config.createTopic(cf, managementQueue, topicName);
                    }
                } else {
                    config.createTopic(cf, managementQueue, topicName);
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
        topic = ActiveMQJMSClient.createTopic(topicName);
    }


    @Override
    public synchronized void stop(final StopContext context) {
        if(sessionFactory != null) {
            sessionFactory.close();
        }
    }

    @Override
    public Topic getValue() throws IllegalStateException {
        return topic;
    }

    public static ExternalJMSTopicService installService(final String name, final ServiceName serviceName, final ServiceTarget serviceTarget) {
        final ExternalJMSTopicService service = new ExternalJMSTopicService(name);
        final ServiceBuilder<Topic> serviceBuilder = serviceTarget.addService(serviceName, service);
        serviceBuilder.install();
        return service;
    }

    public static ExternalJMSTopicService installRuntimeTopicService(final DestinationConfiguration config, final ServiceTarget serviceTarget, final ServiceName pcf) {
        final ExternalJMSTopicService service = new ExternalJMSTopicService(config);
        final ServiceBuilder<Topic> serviceBuilder = serviceTarget.addService(config.getDestinationServiceName(), service);
        serviceBuilder.addDependency(NamingService.SERVICE_NAME, NamingStore.class, service.namingStoreInjector);
        serviceBuilder.addDependency(pcf, ExternalPooledConnectionFactoryService.class, service.pcfInjector);
        serviceBuilder.addDependencies(ConnectorServices.RESOURCE_ADAPTER_SERVICE_PREFIX.append(config.getResourceAdapter()));
        serviceBuilder.install();
        return service;
    }
}
