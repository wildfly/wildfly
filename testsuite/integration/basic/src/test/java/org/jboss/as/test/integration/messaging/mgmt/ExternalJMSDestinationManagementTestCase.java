/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2011, Red Hat, Inc., and individual contributors
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
package org.jboss.as.test.integration.messaging.mgmt;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import jakarta.jms.Destination;
import jakarta.jms.JMSConsumer;
import jakarta.jms.JMSContext;
import jakarta.jms.JMSException;
import jakarta.jms.TextMessage;
import javax.naming.Context;
import org.apache.activemq.artemis.jms.client.ActiveMQJMSConnectionFactory;

import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.as.arquillian.api.ContainerResource;
import org.jboss.as.arquillian.container.ManagementClient;
import org.jboss.as.controller.client.helpers.Operations;
import org.jboss.as.test.integration.common.jms.JMSOperations;
import org.jboss.as.test.integration.common.jms.JMSOperationsProvider;
import org.jboss.as.test.shared.TimeoutUtil;
import org.jboss.dmr.ModelNode;
import org.jboss.logging.Logger;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Tests the management API for Jakarta Messaging queues.
 *
 * @author Brian Stansberry (c) 2011 Red Hat Inc.
 */
@RunAsClient()
@RunWith(Arquillian.class)
public class ExternalJMSDestinationManagementTestCase {

    private static final Logger LOGGER = Logger.getLogger(ExternalJMSDestinationManagementTestCase.class);

    public static final String LEGACY_QUEUE_NAME = "myExternalLegacyQueue";
    public static final String LEGACY_TOPIC_NAME = "myExternalLegacyTopic";
    public static final String QUEUE_NAME = "myExternalQueue";
    public static final String TOPIC_NAME = "myExternalTopic";
    private static final String CONNECTION_FACTORY_JNDI_NAME = "java:jboss/exported/jms/TestConnectionFactory";
    private static Set<String> initialQueues = Collections.emptySet();

    @ContainerResource
    private ManagementClient managementClient;

    @ContainerResource
    private Context remoteContext;

    @Before
    public void prepareDestinations() throws Exception {
        if (initialQueues == null || initialQueues.isEmpty()) {
            initialQueues = listRuntimeQueues(managementClient);
        }
        JMSOperations adminSupport = JMSOperationsProvider.getInstance(managementClient.getControllerClient());
        adminSupport.addCoreQueue(QUEUE_NAME, QUEUE_NAME, true, "ANYCAST");
        adminSupport.addCoreQueue("jms.queue." + LEGACY_QUEUE_NAME, "jms.queue." + LEGACY_QUEUE_NAME, true, "ANYCAST");
        adminSupport.addCoreQueue(TOPIC_NAME, TOPIC_NAME, true, "MULTICAST");
        adminSupport.addCoreQueue("jms.topic." + LEGACY_TOPIC_NAME, "jms.topic." + LEGACY_TOPIC_NAME, true, "MULTICAST");
        ModelNode op = Operations.createAddOperation(adminSupport.getSubsystemAddress().add("external-jms-topic", TOPIC_NAME));
        op.get("enable-amq1-prefix").set("false");
        op.get("entries").add("java:jboss/exported/jms/topic/" + TOPIC_NAME);
        managementClient.getControllerClient().execute(op);
        op = Operations.createAddOperation(adminSupport.getSubsystemAddress().add("external-jms-topic", LEGACY_TOPIC_NAME));
        op.get("enable-amq1-prefix").set("true");
        op.get("entries").add("java:jboss/exported/jms/topic/" + LEGACY_TOPIC_NAME);
        managementClient.getControllerClient().execute(op);
        op = Operations.createAddOperation(adminSupport.getSubsystemAddress().add("external-jms-queue", QUEUE_NAME));
        op.get("enable-amq1-prefix").set("false");
        op.get("entries").add("java:jboss/exported/jms/queue/" + QUEUE_NAME);
        managementClient.getControllerClient().execute(op);
        op = Operations.createAddOperation(adminSupport.getSubsystemAddress().add("external-jms-queue", LEGACY_QUEUE_NAME));
        op.get("enable-amq1-prefix").set("true");
        op.get("entries").add("java:jboss/exported/jms/queue/" + LEGACY_QUEUE_NAME);
        managementClient.getControllerClient().execute(op);
        ModelNode attr = new ModelNode();
        if (adminSupport.isRemoteBroker()) {
            adminSupport.addExternalRemoteConnector("remote-broker-connector", "messaging-activemq");
            attr.get("connectors").add("remote-broker-connector");
        } else {
            adminSupport.addExternalHttpConnector("http-test-connector", "http", "http-acceptor");
            attr.get("connectors").add("http-test-connector");
        }
        adminSupport.addJmsExternalConnectionFactory("TestConnectionFactory", CONNECTION_FACTORY_JNDI_NAME, attr);
        adminSupport.close();
    }

    Set<String> listRuntimeQueues(org.jboss.as.arquillian.container.ManagementClient managementClient) throws IOException {
        JMSOperations ops = JMSOperationsProvider.getInstance(managementClient.getControllerClient());
        ModelNode op = Operations.createOperation("read-children-names", ops.getServerAddress());
        op.get("child-type").set("runtime-queue");
        ModelNode result = Operations.readResult(managementClient.getControllerClient().execute(op));
        List<ModelNode> runtimeQueues = result.asList();
        return runtimeQueues.stream().map(ModelNode::asString).collect(Collectors.toSet());
    }

    @Test
    public void testCreatedDestinations() throws Exception {
        ActiveMQJMSConnectionFactory cf = (ActiveMQJMSConnectionFactory) remoteContext.lookup("jms/TestConnectionFactory");
        Assert.assertNotNull(cf);
        Destination myExternalQueue = (Destination) remoteContext.lookup("jms/queue/" + QUEUE_NAME);
        Assert.assertNotNull(myExternalQueue);
        Destination myLegacyExternalQueue = (Destination) remoteContext.lookup("jms/queue/" + LEGACY_QUEUE_NAME);
        Assert.assertNotNull(myLegacyExternalQueue);
        Destination myExternalTopic = (Destination) remoteContext.lookup("jms/topic/" + TOPIC_NAME);
        Assert.assertNotNull(myExternalTopic);
        Destination myLegacyExternalTopic = (Destination) remoteContext.lookup("jms/topic/" + LEGACY_TOPIC_NAME);
        Assert.assertNotNull(myLegacyExternalTopic);
        try (JMSContext jmsContext = cf.createContext("guest", "guest", JMSContext.AUTO_ACKNOWLEDGE)) {
            checkJMSDestination(jmsContext, myLegacyExternalQueue);
            checkJMSDestination(jmsContext, myLegacyExternalTopic);
        }
        cf = (ActiveMQJMSConnectionFactory) remoteContext.lookup("jms/TestConnectionFactory");
        cf.setEnable1xPrefixes(false);
        try (JMSContext jmsContext = cf.createContext("guest", "guest", JMSContext.AUTO_ACKNOWLEDGE)) {
            checkJMSDestination(jmsContext, myExternalQueue);
            checkJMSDestination(jmsContext, myExternalTopic);
        }
        JMSOperations ops = JMSOperationsProvider.getInstance(managementClient.getControllerClient());
        if (!ops.isRemoteBroker()) {
            ModelNode op = Operations.createOperation("read-children-names", ops.getServerAddress());
            op.get("child-type").set("runtime-queue");
            List<ModelNode> runtimeQueues = Operations.readResult(managementClient.getControllerClient().execute(op)).asList();
            Set<String> queues = runtimeQueues.stream().map(ModelNode::asString).collect(Collectors.toSet());
            Assert.assertEquals(initialQueues.size() + 4, queues.size());
            Assert.assertTrue("We should have myExternalQueue queue", queues.contains(QUEUE_NAME));
            Assert.assertTrue("We should have myExternalQueue queue", queues.contains("jms.queue." + LEGACY_QUEUE_NAME));
            queues.removeAll(initialQueues);
            queues.remove(QUEUE_NAME);
            queues.remove("jms.queue." + LEGACY_QUEUE_NAME);
            checkRuntimeQueue(ops, QUEUE_NAME, "ANYCAST", QUEUE_NAME);
            checkRuntimeQueue(ops, "jms.queue." + LEGACY_QUEUE_NAME, "ANYCAST", "jms.queue." + LEGACY_QUEUE_NAME);
            for (String topicId : queues) {
                checkRuntimeTopic(ops, topicId);
            }
        }
    }

    private void checkJMSDestination(JMSContext jmsContext, Destination destination) throws JMSException {
        String message = "Sending message to " + destination.toString();
        try (JMSConsumer consumer = jmsContext.createConsumer(destination)) {
            jmsContext.createProducer().send(destination, jmsContext.createTextMessage(message));
            TextMessage msg = (TextMessage) consumer.receive(TimeoutUtil.adjust(5000));
            Assert.assertNotNull(msg);
            Assert.assertEquals(message, msg.getText());
        }
    }

    private void checkRuntimeQueue(JMSOperations ops, String name, String expectedRoutingType, String expectedQueueAddress) throws IOException {
        ModelNode op = Operations.createReadResourceOperation(ops.getServerAddress().add("runtime-queue", name), false);
        op.get("include-runtime").set(true);
        op.get("include-defaults").set(true);
        ModelNode result = Operations.readResult(managementClient.getControllerClient().execute(op));
        Assert.assertEquals("ANYCAST", result.require("routing-type").asString());
        Assert.assertEquals(expectedQueueAddress, result.require("queue-address").asString());
    }

    private void checkRuntimeTopic(JMSOperations ops, String topicId) throws IOException {
        ModelNode op = Operations.createReadResourceOperation(ops.getServerAddress().add("runtime-queue", topicId), false);
        op.get("include-runtime").set(true);
        op.get("include-defaults").set(true);
        ModelNode result = Operations.readResult(managementClient.getControllerClient().execute(op));
        String address = result.require("queue-address").asString();
        Assert.assertTrue(address.equals("jms.topic." + LEGACY_TOPIC_NAME) || address.equals(TOPIC_NAME));
        Assert.assertEquals("MULTICAST", result.require("routing-type").asString());
    }

    @After
    public void removeQueues() throws Exception {
        JMSOperations adminSupport = JMSOperationsProvider.getInstance(managementClient.getControllerClient());
        adminSupport.removeCoreQueue(QUEUE_NAME);
        adminSupport.removeCoreQueue("jms.queue." + LEGACY_QUEUE_NAME);
        adminSupport.removeCoreQueue(TOPIC_NAME);
        adminSupport.removeCoreQueue("jms.topic." + LEGACY_TOPIC_NAME);
        ModelNode op = Operations.createRemoveOperation(adminSupport.getSubsystemAddress().add("external-jms-topic", TOPIC_NAME));
        managementClient.getControllerClient().execute(op);
        op = Operations.createRemoveOperation(adminSupport.getSubsystemAddress().add("external-jms-topic", LEGACY_TOPIC_NAME));
        managementClient.getControllerClient().execute(op);
        op = Operations.createRemoveOperation(adminSupport.getSubsystemAddress().add("external-jms-queue", QUEUE_NAME));
        managementClient.getControllerClient().execute(op);
        op = Operations.createRemoveOperation(adminSupport.getSubsystemAddress().add("external-jms-queue", LEGACY_QUEUE_NAME));
        managementClient.getControllerClient().execute(op);
        adminSupport.removeJmsExternalConnectionFactory("TestConnectionFactory");
        if (adminSupport.isRemoteBroker()) {
            adminSupport.removeExternalRemoteConnector("remote-broker-connector");
        } else {
            adminSupport.removeExternalHttpConnector("http-test-connector");
        }
        adminSupport.close();
    }

}
