/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.extension.messaging.activemq.jms;

import static org.wildfly.extension.messaging.activemq._private.MessagingLogger.ROOT_LOGGER;

import jakarta.jms.Connection;
import jakarta.jms.ConnectionFactory;
import jakarta.jms.JMSException;
import jakarta.jms.Message;
import jakarta.jms.Queue;
import jakarta.jms.QueueRequestor;
import jakarta.jms.QueueSession;
import jakarta.jms.Session;
import org.apache.activemq.artemis.api.core.RoutingType;
import org.apache.activemq.artemis.api.core.management.ResourceNames;
import org.apache.activemq.artemis.jms.client.ActiveMQDestination;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.StartException;

/**
 *
 * @author Emmanuel Hugonnet (c) 2018 Red Hat, inc.
 */
public class DestinationConfiguration {

    private final boolean durable;
    private final String selector;
    private final String name;
    private final String managementQueueAddress;
    private final String managementUsername;
    private final String managementPassword;
    private final String resourceAdapter;
    private final ServiceName destinationServiceName;

    public DestinationConfiguration(boolean durable, String selector, String name, String managementQueueAddress,
            String managementUsername, String managementPassword, String resourceAdapter, ServiceName destinationServiceName) {
        this.durable = durable;
        this.selector = selector;
        this.name = name;
        this.managementQueueAddress = managementQueueAddress;
        this.managementUsername = managementUsername;
        this.managementPassword = managementPassword;
        this.resourceAdapter = resourceAdapter;
        this.destinationServiceName = destinationServiceName;
    }

    public boolean isDurable() {
        return durable;
    }

    public String getSelector() {
        return selector;
    }

    public String getName() {
        return name;
    }

    public ServiceName getDestinationServiceName() {
        return destinationServiceName;
    }

    public String getResourceAdapter() {
        return resourceAdapter;
    }

    public Queue getManagementQueue() {
        return ActiveMQDestination.createQueue(this.managementQueueAddress);
    }

    private Connection createQueueConnection(ConnectionFactory cf) throws JMSException {
        if(this.managementUsername != null && !this.managementUsername.isEmpty()) {
            return cf.createConnection(managementUsername, managementPassword);
        }
        return cf.createConnection();
    }

    public void createQueue(ConnectionFactory cf, Queue managementQueue, String queueName) throws JMSException, StartException {
        try (Connection connection = createQueueConnection(cf);
            Session session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE)) {
            connection.start();
            QueueRequestor requestor = new QueueRequestor((QueueSession) session, managementQueue);
            Message m = session.createMessage();
            org.apache.activemq.artemis.api.jms.management.JMSManagementHelper.putOperationInvocation(m, ResourceNames.BROKER, "createAddress", queueName, RoutingType.ANYCAST.name());
            Message reply = requestor.request(m);
            ROOT_LOGGER.debugf("Creating address %s returned %s", queueName, reply);
            if (!reply.getBooleanProperty("_AMQ_OperationSucceeded")) {
                String body = reply.getBody(String.class);
                if (!destinationAlreadyExist(body)) {
                    throw ROOT_LOGGER.remoteDestinationCreationFailed(queueName, body);
                }
            }
            ROOT_LOGGER.debugf("Address %s has been created", queueName);
            m = session.createMessage();
            if (getSelector() != null && !getSelector().isEmpty()) {
                org.apache.activemq.artemis.api.jms.management.JMSManagementHelper.putOperationInvocation(m, ResourceNames.BROKER, "createQueue", queueName, queueName, getSelector(), isDurable(), RoutingType.ANYCAST.name());
            } else {
                org.apache.activemq.artemis.api.jms.management.JMSManagementHelper.putOperationInvocation(m, ResourceNames.BROKER, "createQueue", queueName, queueName, isDurable(), RoutingType.ANYCAST.name());
            }
            reply = requestor.request(m);
            ROOT_LOGGER.debugf("Creating queue %s returned %s", queueName, reply);
            requestor.close();
            if (!reply.getBooleanProperty("_AMQ_OperationSucceeded")) {
                String body = reply.getBody(String.class);
                if (!destinationAlreadyExist(body)) {
                    throw ROOT_LOGGER.remoteDestinationCreationFailed(queueName, body);
                }
            }
            ROOT_LOGGER.debugf("Queue %s has been created", queueName);
        }
    }

    private boolean destinationAlreadyExist(String body) {
        return body.contains("AMQ119019") || body.contains("AMQ119018") || body.contains("AMQ229019") || body.contains("AMQ229018") || body.contains("AMQ229204");
    }

    public void destroyQueue(ConnectionFactory cf, Queue managementQueue, String queueName) throws JMSException {
        try (Connection connection = createQueueConnection(cf);
            Session session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE)) {
            connection.start();
            QueueRequestor requestor = new QueueRequestor((QueueSession) session, managementQueue);
            Message m = session.createMessage();
            org.apache.activemq.artemis.api.jms.management.JMSManagementHelper.putOperationInvocation(m, ResourceNames.BROKER, "destroyQueue", queueName, true, true);
            Message reply = requestor.request(m);
            ROOT_LOGGER.debugf("Deleting queue %s returned %s", queueName, reply);
            requestor.close();
            if (!reply.getBooleanProperty("_AMQ_OperationSucceeded")) {
                throw ROOT_LOGGER.remoteDestinationDeletionFailed(queueName, reply.getBody(String.class));
            }
            ROOT_LOGGER.debugf("Queue %s has been deleted", queueName);
        }
    }

    public void createTopic(ConnectionFactory cf, Queue managementQueue, String topicName) throws JMSException, StartException {
        try (Connection connection = createQueueConnection(cf);
            Session session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE)) {
            connection.start();
            QueueRequestor requestor = new QueueRequestor((QueueSession) session, managementQueue);
            Message m = session.createMessage();
            org.apache.activemq.artemis.api.jms.management.JMSManagementHelper.putOperationInvocation(m, ResourceNames.BROKER, "createAddress", topicName, RoutingType.MULTICAST.name());
            Message reply = requestor.request(m);
            ROOT_LOGGER.infof("Creating topic %s returned %s", topicName, reply);
            requestor.close();
            if (!reply.getBooleanProperty("_AMQ_OperationSucceeded")) {
                String body = reply.getBody(String.class);
                if (!destinationAlreadyExist(body)) {
                    throw ROOT_LOGGER.remoteDestinationCreationFailed(topicName, body);
                }
            }
            ROOT_LOGGER.infof("Topic %s has been created", topicName);
        }
    }

    public void destroyTopic(ConnectionFactory cf, Queue managementQueue, String topicName) throws JMSException {
        try (Connection connection = createQueueConnection(cf);
            Session session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE)) {
            connection.start();
            QueueRequestor requestor = new QueueRequestor((QueueSession) session, managementQueue);
            Message m = session.createMessage();
            org.apache.activemq.artemis.api.jms.management.JMSManagementHelper.putOperationInvocation(m, ResourceNames.BROKER, "deleteAddress", topicName, true);
            Message reply = requestor.request(m);
            requestor.close();
            ROOT_LOGGER.debugf("Deleting topic " + topicName + " returned " + reply);
            if (!reply.getBooleanProperty("_AMQ_OperationSucceeded")) {
                throw ROOT_LOGGER.remoteDestinationDeletionFailed(topicName, reply.getBody(String.class));
            }
            ROOT_LOGGER.debugf("Topic %s has been deleted", topicName);
        }
    }
    public static class Builder {

        private boolean durable = false;
        private String selector = null;
        private String name;
        private String managementQueueAddress = "activemq.management";
        private String managementUsername = null;
        private String managementPassword = null;
        private String resourceAdapter;
        private ServiceName destinationServiceName;

        private Builder() {
        }

        public static Builder getInstance() {
            return new Builder();
        }

        public Builder setDurable(boolean durable) {
            this.durable = durable;
            return this;
        }

        public Builder setSelector(String selector) {
            this.selector = selector;
            return this;
        }

        public Builder setName(String name) {
            this.name = name;
            return this;
        }

        public Builder setDestinationServiceName(ServiceName destinationServiceName) {
            this.destinationServiceName = destinationServiceName;
            return this;
        }

        public Builder setResourceAdapter(String resourceAdapter) {
            this.resourceAdapter = resourceAdapter;
            return this;
        }

        public Builder setManagementQueueAddress(String managementQueueAddress) {
            this.managementQueueAddress = managementQueueAddress;
            return this;
        }

        public Builder setManagementUsername(String managementUsername) {
            this.managementUsername = managementUsername;
            return this;
        }

        public Builder setManagementPassword(String managementPassword) {
            this.managementPassword = managementPassword;
            return this;
        }

        public DestinationConfiguration build() {
            return new DestinationConfiguration(durable, selector, name, managementQueueAddress, managementUsername,
                    managementPassword,resourceAdapter, destinationServiceName);
        }

    }
}
