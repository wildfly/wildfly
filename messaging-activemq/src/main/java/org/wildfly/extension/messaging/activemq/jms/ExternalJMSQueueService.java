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


import static org.wildfly.extension.messaging.activemq.logging.MessagingLogger.ROOT_LOGGER;

import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.Queue;
import javax.jms.QueueConnection;
import javax.jms.QueueConnectionFactory;
import javax.jms.QueueRequestor;
import javax.jms.QueueSession;
import javax.jms.Session;
import javax.naming.NamingException;
import org.apache.activemq.artemis.api.core.RoutingType;
import org.apache.activemq.artemis.api.core.management.ResourceNames;
import org.apache.activemq.artemis.api.jms.ActiveMQJMSClient;
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

/**
 * Service responsible for creating and destroying a client {@code javax.jms.Queue}.
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

    public ExternalJMSQueueService(final String queueName) {
        this.queueName = queueName;
        this.config = null;
    }

    private ExternalJMSQueueService(final DestinationConfiguration config) {
        this.queueName = config.getName();
        this.config = config;
    }

    @Override
    public synchronized void start(final StartContext context) throws StartException {
        NamingStore namingStore = namingStoreInjector.getOptionalValue();
        if(namingStore!= null) {
            Queue managementQueue = config.getManagementQueue();
            final NamingContext storeBaseContext = new NamingContext(namingStore, null);
            try {
                QueueConnectionFactory cf = (QueueConnectionFactory) storeBaseContext.lookup(pcfInjector.getValue().getBindInfo().getAbsoluteJndiName());
                try (QueueConnection connection = config.createQueueConnection(cf)) {
                    QueueSession session = connection.createQueueSession(false, Session.AUTO_ACKNOWLEDGE);
                    connection.start();
                    QueueRequestor requestor = new QueueRequestor(session, managementQueue);
                    Message m = session.createMessage();
                    if (config.getSelector() != null && !config.getSelector().isEmpty()) {
                        org.apache.activemq.artemis.api.jms.management.JMSManagementHelper.putOperationInvocation(m, ResourceNames.BROKER, "createQueue", JMS_QUEUE_PREFIX + queueName, JMS_QUEUE_PREFIX + queueName, config.getSelector(), config.isDurable(), RoutingType.ANYCAST.name());
                    } else {
                        org.apache.activemq.artemis.api.jms.management.JMSManagementHelper.putOperationInvocation(m, ResourceNames.BROKER, "createQueue", JMS_QUEUE_PREFIX + queueName, JMS_QUEUE_PREFIX + queueName, config.isDurable(), RoutingType.ANYCAST.name());
                    }
                    Message reply = requestor.request(m);
                    ROOT_LOGGER.debugf("Creating queue %s returned %s", JMS_QUEUE_PREFIX + queueName, reply);
                    if(!reply.getBooleanProperty("_AMQ_OperationSucceeded")) {
                        throw ROOT_LOGGER.remoteDestinationCreationFailed(JMS_QUEUE_PREFIX + queueName, reply.getBody(String.class));
                    }
                    ROOT_LOGGER.debugf("Queue %s has been created", JMS_QUEUE_PREFIX + queueName);
                }
            } catch (NamingException | JMSException ex) {
                    throw new StartException(ex);
            } finally {
                try {
                    storeBaseContext.close();
                } catch (NamingException ex) {
                    throw new StartException(ex);
                }
            }
        }
        queue = ActiveMQJMSClient.createQueue(JMS_QUEUE_PREFIX + queueName);
    }

    @Override
    public synchronized void stop(final StopContext context) {
        NamingStore namingStore = namingStoreInjector.getOptionalValue();
        if(namingStore!= null) {
            Queue managementQueue = config.getManagementQueue();
            final NamingContext storeBaseContext = new NamingContext(namingStore, null);
            try {
                QueueConnectionFactory cf = (QueueConnectionFactory) storeBaseContext.lookup(pcfInjector.getValue().getBindInfo().getAbsoluteJndiName());
                try (QueueConnection connection = config.createQueueConnection(cf)) {
                    QueueSession session = connection.createQueueSession(false, Session.AUTO_ACKNOWLEDGE);
                    connection.start();
                    QueueRequestor requestor = new QueueRequestor(session, managementQueue);
                    Message m = session.createMessage();
                    org.apache.activemq.artemis.api.jms.management.JMSManagementHelper.putOperationInvocation(m, ResourceNames.BROKER, "destroyQueue", JMS_QUEUE_PREFIX + queueName, true, true);
                    Message reply = requestor.request(m);
                    ROOT_LOGGER.debugf("Deleting queue %s returned %s", JMS_QUEUE_PREFIX + queueName, reply);
                    if(!reply.getBooleanProperty("_AMQ_OperationSucceeded")) {
                        throw ROOT_LOGGER.remoteDestinationDeletionFailed(JMS_QUEUE_PREFIX + queueName, reply.getBody(String.class));
                    }
                    ROOT_LOGGER.debugf("Queue %s has been deleted", JMS_QUEUE_PREFIX + queueName);
                }
            } catch (NamingException | JMSException ex) {
                    throw new RuntimeException(ex);
            } finally {
                try {
                    storeBaseContext.close();
                } catch (NamingException ex) {
                    throw new RuntimeException(ex);
                }
            }
        }
        queue = null;
    }

    @Override
    public Queue getValue() throws IllegalStateException, IllegalArgumentException {
        return queue;
    }

    public static Service<Queue> installService(final String name, final ServiceTarget serviceTarget, final ServiceName serviceName) {
        final ExternalJMSQueueService service = new ExternalJMSQueueService(name);
        final ServiceBuilder<Queue> serviceBuilder = serviceTarget.addService(serviceName, service);
        serviceBuilder.install();
        return service;
    }

    public static Service<Queue> installRuntimeQueueService(final DestinationConfiguration config, final ServiceTarget serviceTarget, final ServiceName pcf) {
        final ExternalJMSQueueService service = new ExternalJMSQueueService(config);
        final ServiceBuilder<Queue> serviceBuilder = serviceTarget.addService(config.getDestinationServiceName(), service);
        serviceBuilder.addDependency(NamingService.SERVICE_NAME, NamingStore.class, service.namingStoreInjector);
        serviceBuilder.addDependency(pcf, ExternalPooledConnectionFactoryService.class, service.pcfInjector);
        serviceBuilder.addDependencies(ConnectorServices.RESOURCE_ADAPTER_SERVICE_PREFIX.append(config.getResourceAdapter()));
        serviceBuilder.install();
        return service;
    }
}
