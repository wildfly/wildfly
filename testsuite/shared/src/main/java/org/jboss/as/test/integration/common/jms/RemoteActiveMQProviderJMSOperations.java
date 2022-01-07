/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2014, Red Hat, Inc., and individual contributors
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
package org.jboss.as.test.integration.common.jms;

import org.jboss.as.arquillian.container.ManagementClient;
import org.jboss.as.controller.client.ModelControllerClient;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.Property;

import java.util.Map;

import static org.jboss.as.controller.client.helpers.ClientConstants.ADD;
import static org.jboss.as.controller.client.helpers.ClientConstants.REMOVE_OPERATION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.NAME;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SUBSYSTEM;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.VALUE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.WRITE_ATTRIBUTE_OPERATION;
import static org.jboss.as.test.integration.common.jms.JMSOperationsProvider.execute;

import javax.jms.Connection;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.Queue;
import javax.jms.QueueRequestor;
import javax.jms.QueueSession;
import javax.jms.Session;
import org.apache.activemq.artemis.api.core.RoutingType;
import org.apache.activemq.artemis.api.core.management.ResourceNames;
import org.apache.activemq.artemis.api.jms.ActiveMQJMSClient;
import org.apache.activemq.artemis.jms.client.ActiveMQConnectionFactory;
import org.jboss.as.controller.PathAddress;

/**
 * An implementation of JMSOperations for Apache ActiveMQ 6.
 *
 * @author <a href="http://jmesnil.net/">Jeff Mesnil</a> (c) 2014 Red Hat inc.
 */
public class RemoteActiveMQProviderJMSOperations implements JMSOperations {

    private final ModelControllerClient client;

    public RemoteActiveMQProviderJMSOperations(ModelControllerClient client) {
        this.client = client;
    }

    public RemoteActiveMQProviderJMSOperations(ManagementClient client) {
        this.client = client.getControllerClient();
    }

    private static final ModelNode subsystemAddress = new ModelNode();

    static {
        subsystemAddress.add("subsystem", "messaging-activemq");
    }

    private static final ModelNode serverAddress = new ModelNode();

    static {
        serverAddress.add("subsystem", "messaging-activemq");
        serverAddress.add("server", "default");
    }

    @Override
    public ModelControllerClient getControllerClient() {
        return client;
    }

    @Override
    public ModelNode getServerAddress() {
        return subsystemAddress.clone();
    }

    @Override
    public ModelNode getSubsystemAddress() {
        return subsystemAddress.clone();
    }

    @Override
    public String getProviderName() {
        return "activemq";
    }

    @Override
    public void createJmsQueue(String queueName, String jndiName) {
        createJmsQueue(queueName, jndiName, new ModelNode());
    }

    @Override
    public void createJmsQueue(String queueName, String jndiName, ModelNode attributes) {
        ModelNode address = getServerAddress()
                .add("external-jms-queue", queueName);
        attributes.get("entries").add(jndiName);
        executeOperation(address, ADD, attributes);
        createRemoteQueue("jms.queue." + queueName);
    }

    @Override
    public void createJmsTopic(String topicName, String jndiName) {
        createJmsTopic(topicName, jndiName, new ModelNode());
    }

    @Override
    public void createJmsTopic(String topicName, String jndiName, ModelNode attributes) {
        ModelNode address = getServerAddress()
                .add("external-jms-topic", topicName);
        attributes.get("entries").add(jndiName);
        executeOperation(address, ADD, attributes);
        createRemoteTopic("jms.topic." + topicName);
    }

    @Override
    public void removeJmsQueue(String queueName) {
        ModelNode address = getServerAddress()
                .add("external-jms-queue", queueName);
        executeOperation(address, REMOVE_OPERATION, null);
        deleteRemoteQueue("jms.queue." + queueName);
    }

    @Override
    public void removeJmsTopic(String topicName) {
        ModelNode address = getServerAddress()
                .add("external-jms-topic", topicName);
        executeOperation(address, REMOVE_OPERATION, null);
        deleteRemoteTopic("jms.topic." + topicName);
    }

    @Override
    public void addJmsConnectionFactory(String name, String jndiName, ModelNode attributes) {
        ModelNode address = getServerAddress()
                .add("connection-factory", name);
        attributes.get("entries").add(jndiName);
        executeOperation(address, ADD, attributes);
    }

    @Override
    public void removeJmsConnectionFactory(String name) {
        ModelNode address = getServerAddress()
                .add("connection-factory", name);
        executeOperation(address, REMOVE_OPERATION, null);
    }

    @Override
    public void addJmsExternalConnectionFactory(String name, String jndiName, ModelNode attributes) {
        ModelNode address = getSubsystemAddress()
                .add("connection-factory", name);
        attributes.get("entries").add(jndiName);
        executeOperation(address, ADD, attributes);
    }

    @Override
    public void removeJmsExternalConnectionFactory(String name) {
        ModelNode address = getSubsystemAddress()
                .add("connection-factory", name);
        executeOperation(address, REMOVE_OPERATION, null);
    }

    @Override
    public void addJmsBridge(String name, ModelNode attributes) {
        ModelNode address = new ModelNode();
        address.add("subsystem", "messaging-activemq");
        address.add("jms-bridge", name);
        executeOperation(address, ADD, attributes);
    }

    @Override
    public void removeJmsBridge(String name) {
        ModelNode address = new ModelNode();
        address.add("subsystem", "messaging-activemq");
        address.add("jms-bridge", name);
        executeOperation(address, REMOVE_OPERATION, null);
    }

    @Override
    public void addCoreBridge(String name, ModelNode attributes) {
        ModelNode address = getServerAddress();
        address.add("bridge", name);
        executeOperation(address, ADD, attributes);
    }

    @Override
    public void removeCoreBridge(String name) {
        ModelNode address = getServerAddress();
        address.add("bridge", name);
        executeOperation(address, REMOVE_OPERATION, null);
    }

    @Override
    public void addCoreQueue(String queueName, String queueAddress, boolean durable, String routing) {
        ModelNode address = getServerAddress()
                .add("queue", queueName);
        ModelNode attributes = new ModelNode();
        attributes.get("queue-address").set(queueAddress);
        attributes.get("durable").set(durable);
        attributes.get("routing-type").set(routing);
        executeOperation(address, ADD, attributes);
    }

    @Override
    public void removeCoreQueue(String queueName) {
        ModelNode address = getServerAddress()
                .add("queue", queueName);
        executeOperation(address, REMOVE_OPERATION, null);
    }

    @Override
    public void createRemoteAcceptor(String name, String socketBinding, Map<String, String> params) {
        ModelNode model = getServerAddress().add("remote-acceptor", name);
        ModelNode attributes = new ModelNode();
        attributes.get("socket-binding").set(socketBinding);
        if (params != null) {
            for (String key : params.keySet()) {
                model.get("params").add(key, params.get(key));
            }
        }
        executeOperation(model, ADD, attributes);
    }

    @Override
    public void removeRemoteAcceptor(String name) {
        ModelNode model = getServerAddress().add("remote-acceptor", name);
        executeOperation(model, REMOVE_OPERATION, null);
    }

    @Override
    public void close() {
        // no-op
        // DO NOT close the management client. Whoever passed it into the constructor should close it
    }

    private void executeOperation(final ModelNode address, final String opName, ModelNode attributes) {
        final ModelNode operation = new ModelNode();
        operation.get(OP).set(opName);
        operation.get(OP_ADDR).set(address);
        if (attributes != null) {
            for (Property property : attributes.asPropertyList()) {
                operation.get(property.getName()).set(property.getValue());
            }
        }
        try {
            execute(client, operation);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void setSystemProperties(String destination, String resourceAdapter) {
        final ModelNode enableSubstitutionOp = new ModelNode();
        enableSubstitutionOp.get(OP_ADDR).set(SUBSYSTEM, "ee");
        enableSubstitutionOp.get(OP).set(WRITE_ATTRIBUTE_OPERATION);
        enableSubstitutionOp.get(NAME).set("annotation-property-replacement");
        enableSubstitutionOp.get(VALUE).set(true);

        final ModelNode setDestinationOp = new ModelNode();
        setDestinationOp.get(OP).set(ADD);
        setDestinationOp.get(OP_ADDR).add("system-property", "destination");
        setDestinationOp.get("value").set(destination);
        final ModelNode setResourceAdapterOp = new ModelNode();
        setResourceAdapterOp.get(OP).set(ADD);
        setResourceAdapterOp.get(OP_ADDR).add("system-property", "resource.adapter");
        setResourceAdapterOp.get("value").set(resourceAdapter);

        try {
            execute(client, enableSubstitutionOp);
            execute(client, setDestinationOp);
            execute(client, setResourceAdapterOp);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void removeSystemProperties() {
        final ModelNode removeDestinationOp = new ModelNode();
        removeDestinationOp.get(OP).set("remove");
        removeDestinationOp.get(OP_ADDR).add("system-property", "destination");
        final ModelNode removeResourceAdapterOp = new ModelNode();
        removeResourceAdapterOp.get(OP).set("remove");
        removeResourceAdapterOp.get(OP_ADDR).add("system-property", "resource.adapter");

        final ModelNode disableSubstitutionOp = new ModelNode();
        disableSubstitutionOp.get(OP_ADDR).set(SUBSYSTEM, "ee");
        disableSubstitutionOp.get(OP).set(WRITE_ATTRIBUTE_OPERATION);
        disableSubstitutionOp.get(NAME).set("annotation-property-replacement");
        disableSubstitutionOp.get(VALUE).set(false);

        try {
            execute(client, removeDestinationOp);
            execute(client, removeResourceAdapterOp);
            execute(client, disableSubstitutionOp);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void addHttpConnector(String connectorName, String socketBinding, String endpoint, Map<String, String> parameters) {
        ModelNode address = getServerAddress().add("http-connector", connectorName);

        ModelNode attributes = new ModelNode();
        attributes.get("socket-binding").set(socketBinding);
        attributes.get("endpoint").set(endpoint);
        if (parameters != null && parameters.size() > 0) {
            ModelNode params = attributes.get("params").setEmptyList();
            for (Map.Entry<String, String> param : parameters.entrySet()) {
                params.add(param.getKey(), param.getValue());
            }
        }
        executeOperation(address, ADD, attributes);
    }

    @Override
    public void removeHttpConnector(String connectorName) {
        ModelNode address = getServerAddress()
                .add("http-connector", connectorName);
        executeOperation(address, REMOVE_OPERATION, null);
    }

    @Override
    public void addExternalHttpConnector(String connectorName, String socketBinding, String endpoint) {
        ModelNode address = getSubsystemAddress()
                .add("http-connector", connectorName);
        ModelNode attributes = new ModelNode();
        attributes.get("socket-binding").set(socketBinding);
        attributes.get("endpoint").set(endpoint);
        executeOperation(address, ADD, attributes);
    }

    @Override
    public void addExternalRemoteConnector(String name, String socketBinding) {
        ModelNode address = getSubsystemAddress()
                .add("remote-connector", name);
        ModelNode attributes = new ModelNode();
        attributes.get("socket-binding").set(socketBinding);
        executeOperation(address, ADD, attributes);
    }

    @Override
    public void removeExternalHttpConnector(String connectorName) {
        ModelNode address = getSubsystemAddress()
                .add("http-connector", connectorName);
        executeOperation(address, REMOVE_OPERATION, null);
    }

    @Override
    public void removeExternalRemoteConnector(String connectorName) {
        ModelNode address = getSubsystemAddress()
                .add("remote-connector", connectorName);
        executeOperation(address, REMOVE_OPERATION, null);
    }

    @Override
    public void enableMessagingTraces() {
        final ModelNode attributes = new ModelNode();
        attributes.get("level").set("TRACE");
        ModelNode address = PathAddress.parseCLIStyleAddress("/subsystem=logging/logger=org.wildfly.extension.messaging-activemq").toModelNode();
        try {
            executeOperation(address, REMOVE_OPERATION, null);
        } catch (Exception e) {
        }
        executeOperation(address, ADD, attributes);

        address = PathAddress.parseCLIStyleAddress("/subsystem=logging/logger=org.apache.activemq.artemis").toModelNode();
        try {
            executeOperation(address, REMOVE_OPERATION, null);
        } catch (Exception e) {
        }
        executeOperation(address, ADD, attributes);
    }

    @Override
    public void createRemoteConnector(String name, String socketBinding, Map<String, String> params) {
        ModelNode address = PathAddress.parseCLIStyleAddress(" /subsystem=messaging-activemq/server=default/remote-connector=" + name).toModelNode();
        ModelNode attributes = new ModelNode();
        attributes.get("socket-binding").set(socketBinding);
        if (params != null) {
            for (String key : params.keySet()) {
                attributes.get("params").add(key, params.get(key));
            }
        }
        try {
            executeOperation(address, REMOVE_OPERATION, null);
        } catch (Exception e) {
        }
        executeOperation(address, ADD, attributes);
    }

    @Override
    public void createSocketBinding(String name, String interfaceName, int port) {
        String interfaceValue = interfaceName == null || interfaceName.isEmpty() ? "public" : interfaceName;
        ModelNode address = PathAddress.parseCLIStyleAddress("/socket-binding-group=standard-sockets/socket-binding=" + name).toModelNode();
        ModelNode attributes = new ModelNode();
        attributes.get("interface").set(interfaceValue);
        attributes.get("port").set(port);
        try {
            executeOperation(address, REMOVE_OPERATION, null);
        } catch (Exception e) {
        }
        executeOperation(address, ADD, attributes);
    }

    private void createRemoteQueue(String queueName) {
        ActiveMQConnectionFactory cf = new ActiveMQConnectionFactory("tcp://localhost:61616", "guest", "guest");
        try (Connection connection = cf.createConnection();
                Session session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE)) {
            connection.start();
            Queue managementQueue = ActiveMQJMSClient.createQueue("activemq.management");
            QueueRequestor requestor = new QueueRequestor((QueueSession) session, managementQueue);
            Message m = session.createMessage();
            org.apache.activemq.artemis.api.jms.management.JMSManagementHelper.putOperationInvocation(m, ResourceNames.BROKER, "createQueue", queueName, queueName, true, RoutingType.ANYCAST.name());
            Message reply = requestor.request(m);
            System.out.println("Creating queue " + queueName + " returned " + reply);
            if (!reply.getBooleanProperty("_AMQ_OperationSucceeded")) {
                String body = reply.getBody(String.class);
                if (!destinationAlreadyExist(body)) {
                    System.out.println("Creation of queue " + queueName + " has failed because of " + body);
                }
            }
        } catch (JMSException ex) {
            ex.printStackTrace();
            throw new JMSOperationsException(ex);
        }
        System.out.println("Queue " + queueName + " has been created");
    }

    private void deleteRemoteQueue(String queueName) {
        ActiveMQConnectionFactory cf = new ActiveMQConnectionFactory("tcp://localhost:61616", "guest", "guest");
        try (Connection connection = cf.createConnection();
                Session session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE)) {
            connection.start();
            Queue managementQueue = ActiveMQJMSClient.createQueue("activemq.management");
            QueueRequestor requestor = new QueueRequestor((QueueSession) session, managementQueue);
            Message m = session.createMessage();
            org.apache.activemq.artemis.api.jms.management.JMSManagementHelper.putOperationInvocation(m, ResourceNames.BROKER, "destroyQueue", queueName, true, true);
            Message reply = requestor.request(m);
            System.out.println("Deleting queue " + queueName + " returned " + reply);
            if (!reply.getBooleanProperty("_AMQ_OperationSucceeded")) {
                String body = reply.getBody(String.class);
                System.out.println("Deleting of queue " + queueName + " has failed because of " + body);
            }
        } catch (JMSException ex) {
            ex.printStackTrace();
            throw new JMSOperationsException(ex);
        }
        System.out.println("Queue " + queueName + " has been deleted");
    }

    private boolean destinationAlreadyExist(String body) {
        return body.contains("AMQ119019") || body.contains("AMQ119018") || body.contains("AMQ229019") || body.contains("AMQ229018");
    }

    private void createRemoteTopic(String topicName) {
        ActiveMQConnectionFactory cf = new ActiveMQConnectionFactory("tcp://localhost:61616", "guest", "guest");
        try (Connection connection = cf.createConnection();
                Session session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE)) {
            connection.start();
            Queue managementQueue = ActiveMQJMSClient.createQueue("activemq.management");
            QueueRequestor requestor = new QueueRequestor((QueueSession) session, managementQueue);
            Message m = session.createMessage();
            org.apache.activemq.artemis.api.jms.management.JMSManagementHelper.putOperationInvocation(m, ResourceNames.BROKER, "createAddress", topicName, RoutingType.MULTICAST.name());
            Message reply = requestor.request(m);
            System.out.println("Creating topic " + topicName + " returned " + reply);
            if (!reply.getBooleanProperty("_AMQ_OperationSucceeded")) {
                String body = reply.getBody(String.class);
                if (!destinationAlreadyExist(body)) {
                    System.out.println("Creation of topic " + topicName + " has failed because of " + body);
                    throw new JMSOperationsException("Creation of topic " + topicName + " has failed because of " + body);
                }
            }
        } catch (JMSException ex) {
            ex.printStackTrace();
            throw new JMSOperationsException(ex);
        }
        System.out.println("Topic " + topicName + " has been created");
    }

    private void deleteRemoteTopic(String topicName) {
        ActiveMQConnectionFactory cf = new ActiveMQConnectionFactory("tcp://localhost:61616", "guest", "guest");
        try (Connection connection = cf.createConnection();
                Session session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE)) {
            connection.start();
            Queue managementQueue = ActiveMQJMSClient.createQueue("activemq.management");
            QueueRequestor requestor = new QueueRequestor((QueueSession) session, managementQueue);
            Message m = session.createMessage();
            org.apache.activemq.artemis.api.jms.management.JMSManagementHelper.putOperationInvocation(m, ResourceNames.BROKER, "deleteAddress", topicName, true);
            Message reply = requestor.request(m);
            System.out.println("Deleting topic " + topicName + " returned " + reply);
            if (!reply.getBooleanProperty("_AMQ_OperationSucceeded")) {
                String body = reply.getBody(String.class);
                System.out.println("Deleting of topic " + topicName + " has failed because of " + body);
            }
        } catch (JMSException ex) {
            ex.printStackTrace();
            throw new JMSOperationsException(ex);
        }

        System.out.println("Topic " + topicName + " has been deleted");
    }

    @Override
    public boolean isRemoteBroker() {
        return true;
    }

    @Override
    public void disableSecurity() {
        final ModelNode operation = new ModelNode();
        operation.get(OP).set(WRITE_ATTRIBUTE_OPERATION);
        operation.get(OP_ADDR).set(getServerAddress());
        operation.get(NAME).set("security-enabled");
        operation.get(VALUE).set(false);

        try {
            execute(client, operation);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void enableSecurity() {
        final ModelNode operation = new ModelNode();
        operation.get(OP).set(WRITE_ATTRIBUTE_OPERATION);
        operation.get(OP_ADDR).set(getServerAddress());
        operation.get(NAME).set("security-enabled");
        operation.get(VALUE).set(true);

        try {
            execute(client, operation);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

}
