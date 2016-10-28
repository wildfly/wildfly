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

import static javax.jms.Session.AUTO_ACKNOWLEDGE;
import static org.jboss.as.controller.operations.common.Util.getEmptyOperation;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.io.StringReader;
import java.util.HashSet;
import java.util.Set;

import javax.jms.Connection;
import javax.jms.ConnectionFactory;
import javax.jms.Message;
import javax.jms.MessageConsumer;
import javax.jms.MessageProducer;
import javax.jms.Queue;
import javax.jms.Session;
import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonReader;
import javax.naming.Context;

import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.as.arquillian.api.ContainerResource;
import org.jboss.as.arquillian.container.ManagementClient;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.operations.common.Util;
import org.jboss.as.test.integration.common.jms.JMSOperations;
import org.jboss.as.test.integration.common.jms.JMSOperationsProvider;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;
import org.jboss.logging.Logger;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Tests the management API for JMS queues.
 *
 * @author Brian Stansberry (c) 2011 Red Hat Inc.
 */
@RunAsClient()
@RunWith(Arquillian.class)
public class JMSQueueManagementTestCase {
    private static final Logger LOGGER = Logger.getLogger(JMSQueueManagementTestCase.class);

    private static final String EXPORTED_PREFIX = "java:jboss/exported/";

    private static final long SAMPLE_PERIOD = 1001;

    private static long count = System.currentTimeMillis();

    @ContainerResource
    private Context remoteContext;

    @ContainerResource
    private ManagementClient managementClient;

    private JMSOperations adminSupport;

    private Connection conn;
    private Queue queue;
    private Queue otherQueue;
    private Session session;

    private Connection consumerConn;
    private Session consumerSession;

    @Before
    public void addQueues() throws Exception {

        adminSupport = JMSOperationsProvider.getInstance(managementClient.getControllerClient());

        count++;
        adminSupport.createJmsQueue(getQueueName(), EXPORTED_PREFIX + getQueueJndiName());
        adminSupport.createJmsQueue(getOtherQueueName(), EXPORTED_PREFIX + getOtherQueueJndiName());

        ConnectionFactory cf = (ConnectionFactory) remoteContext.lookup("jms/RemoteConnectionFactory");
        assertNotNull(cf);
        conn = cf.createConnection("guest", "guest");
        conn.start();
        queue = (Queue) remoteContext.lookup(getQueueJndiName());
        otherQueue = (Queue) remoteContext.lookup(getOtherQueueJndiName());
        session = conn.createSession(false, AUTO_ACKNOWLEDGE);
    }

    @After
    public void removeQueues() throws Exception {

        if (conn != null) {
            conn.stop();
        }
        if (session != null) {
            session.close();
        }
        if (conn != null) {
            conn.close();
        }

        if (consumerConn != null) {
            consumerConn.stop();
        }
        if (consumerSession != null) {
            consumerSession.close();
        }
        if (consumerConn != null) {
            consumerConn.close();
        }

        if (adminSupport != null) {
            adminSupport.removeJmsQueue(getQueueName());
            adminSupport.removeJmsQueue(getOtherQueueName());
            adminSupport.close();
        }
    }


    private void enableStatistics(boolean enabled) throws IOException {

        if (enabled) {
            ModelNode enableStatistics = Util.getWriteAttributeOperation(adminSupport.getServerAddress(), "statistics-enabled", new ModelNode(enabled));
            ModelNode accelerateSamplingPeriod = Util.getWriteAttributeOperation(adminSupport.getServerAddress(), "message-counter-sample-period", new ModelNode(SAMPLE_PERIOD));

            execute(enableStatistics, true);
            execute(accelerateSamplingPeriod, true);
        } else {
            ModelNode undefineStatisticsEnabled = Util.getUndefineAttributeOperation(PathAddress.pathAddress(adminSupport.getServerAddress()), "statistics-enabled");
            ModelNode undefineMessageCounterSamplePeriod = Util.getUndefineAttributeOperation(PathAddress.pathAddress(adminSupport.getServerAddress()), "message-counter-sample-period");

            execute(undefineStatisticsEnabled, true);
            execute(undefineMessageCounterSamplePeriod, true);
        }
    }

    private static JsonObject fromString(String string) {
        try (JsonReader reader = Json.createReader(new StringReader(string))) {
            return reader.readObject();
        }
    }

    @Test
    public void testListAndCountMessages() throws Exception {

        MessageProducer producer = session.createProducer(queue);
        producer.send(session.createTextMessage("A"));
        producer.send(session.createTextMessage("B"));

        ModelNode result = execute(getQueueOperation("list-messages"), true);
        Assert.assertTrue(result.isDefined());
        Assert.assertEquals(2, result.asList().size());

        result = execute(getQueueOperation("count-messages"), true);
        Assert.assertTrue(result.isDefined());
        Assert.assertEquals(2, result.asInt());
    }

    @Test
    public void testMessageCounters() throws Exception {

        try {
            enableStatistics(true);

            MessageProducer producer = session.createProducer(queue);
            producer.send(session.createTextMessage("A"));
            producer.send(session.createTextMessage("B"));


            // wait for 2 sample periods to let the counters be updated.
            checkMessageCounters(2, 2);

            ModelNode result = execute(getQueueOperation("list-message-counter-as-html"), true);
            Assert.assertTrue(result.isDefined());
            Assert.assertEquals(ModelType.STRING, result.getType());

            result = execute(getQueueOperation("list-message-counter-history-as-json"), true);
            Assert.assertTrue(result.isDefined());
            Assert.assertEquals(ModelType.STRING, result.getType());

            result = execute(getQueueOperation("list-message-counter-history-as-html"), true);
            Assert.assertTrue(result.isDefined());
            Assert.assertEquals(ModelType.STRING, result.getType());

            result = execute(getQueueOperation("reset-message-counter"), true);
            Assert.assertFalse(result.isDefined());

            // check that the messageCountDelta has been reset to 0 after invoking "reset-message-counter"
            checkMessageCounters(2, 0);

            result = execute(getQueueOperation("list-message-counter-history-as-json"), true);
            Assert.assertTrue(result.isDefined());
            Assert.assertEquals(ModelType.STRING, result.getType());
        } finally {
            enableStatistics(false);
        }
    }

    /**
     * Given that message counters are sampled, we fetched them several time (with a period shorter than the sample period)
     * to make the test pass faster.
     */
    private void checkMessageCounters(int expectedMessageCount, int expectedMessageCountDelta) throws Exception {
        long start = System.currentTimeMillis();
        long now;
        JsonObject messageCounters;
        do {
            Thread.sleep((long) (SAMPLE_PERIOD / 2.0));
            ModelNode result = execute(getQueueOperation("list-message-counter-as-json"), true);
            Assert.assertTrue(result.isDefined());
            Assert.assertEquals(ModelType.STRING, result.getType());
            messageCounters = fromString(result.asString());
            int actualMessageCount = messageCounters.getInt("messageCount");
            int actualMessageCountDelta = messageCounters.getInt("messageCountDelta");
            if (actualMessageCount == expectedMessageCount && actualMessageCountDelta == expectedMessageCountDelta) {
                // got correct counters
                return;
            }
            now = System.currentTimeMillis();
        } while (now - start < (2.0 * SAMPLE_PERIOD));
        // after twice the sample period, the assertions must always be true
        Assert.assertEquals(messageCounters.toString(), expectedMessageCount, messageCounters.getInt("messageCount"));
        Assert.assertEquals(messageCounters.toString(), expectedMessageCountDelta, messageCounters.getInt("messageCountDelta"));

    }

    @Test
    public void testPauseAndResume() throws Exception {

        final ModelNode readAttr = getQueueOperation("read-attribute");
        readAttr.get("name").set("paused");

        ModelNode result = execute(readAttr, true);
        Assert.assertTrue(result.isDefined());
        Assert.assertFalse(result.asBoolean());

        result = execute(getQueueOperation("pause"), true);
        Assert.assertFalse(result.isDefined());

        result = execute(readAttr, true);
        Assert.assertTrue(result.isDefined());
        Assert.assertTrue(result.asBoolean());

        result = execute(getQueueOperation("resume"), true);
        Assert.assertFalse(result.isDefined());

        result = execute(readAttr, true);
        Assert.assertTrue(result.isDefined());
        Assert.assertFalse(result.asBoolean());
    }

//    @org.junit.Ignore("AS7-2480")
    @Test
    public void testMessageRemoval() throws Exception {

        MessageProducer producer = session.createProducer(queue);
        Message msgA = session.createTextMessage("A");
        producer.send(msgA);
        producer.send(session.createTextMessage("B"));
        producer.send(session.createTextMessage("C"));

        final ModelNode op = getQueueOperation("remove-message");
        op.get("message-id").set(msgA.getJMSMessageID());

        ModelNode result = execute(op, true);
        Assert.assertTrue(result.isDefined());
        Assert.assertTrue(result.asBoolean());

        result = execute(getQueueOperation("count-messages"), true);
        Assert.assertTrue(result.isDefined());
        Assert.assertEquals(2, result.asInt());

        result = execute(getQueueOperation("remove-messages"), true);
        Assert.assertTrue(result.isDefined());
        Assert.assertEquals(2, result.asInt());

        result = execute(getQueueOperation("count-messages"), true);
        Assert.assertTrue(result.isDefined());
        Assert.assertEquals(0, result.asInt());

    }

//    @org.junit.Ignore("AS7-2480")
    @Test
    public void testMessageMovement() throws Exception {

        MessageProducer producer = session.createProducer(queue);
        Message msgA = session.createTextMessage("A");
        producer.send(msgA);
        producer.send(session.createTextMessage("B"));
        producer.send(session.createTextMessage("C"));

        ModelNode op = getQueueOperation("move-message");
        op.get("message-id").set(msgA.getJMSMessageID());
        op.get("other-queue-name").set(getOtherQueueName());

        ModelNode result = execute(op, true);
        Assert.assertTrue(result.isDefined());
        Assert.assertTrue(result.asBoolean());

        result = execute(getQueueOperation("count-messages"), true);
        Assert.assertTrue(result.isDefined());
        Assert.assertEquals(2, result.asInt());

        op = getQueueOperation("move-messages");
        op.get("other-queue-name").set(getOtherQueueName());

        result = execute(op, true);
        Assert.assertTrue(result.isDefined());
        Assert.assertEquals(2, result.asInt());

        result = execute(getQueueOperation("count-messages"), true);
        Assert.assertTrue(result.isDefined());
        Assert.assertEquals(0, result.asInt());

    }

    @Test
    public void testChangeMessagePriority() throws Exception {

        MessageProducer producer = session.createProducer(queue);
        Message msgA = session.createTextMessage("A");
        producer.send(msgA);
        producer.send(session.createTextMessage("B"));
        producer.send(session.createTextMessage("C"));

        Set<Integer> priorities = new HashSet<Integer>();
        ModelNode result = execute(getQueueOperation("list-messages"), true);
        Assert.assertEquals(3, result.asInt());
        for (ModelNode node : result.asList()) {
            priorities.add(node.get("JMSPriority").asInt());
        }
        int newPriority = -1;
        for (int i = 0; i < 10; i++) {
            if (!priorities.contains(i)) {
                newPriority = i;
                break;
            }
        }

        ModelNode op = getQueueOperation("change-message-priority");
        op.get("message-id").set(msgA.getJMSMessageID());
        op.get("new-priority").set(newPriority);

        result = execute(op, true);
        Assert.assertTrue(result.isDefined());
        Assert.assertTrue(result.asBoolean());

        result = execute(getQueueOperation("list-messages"), true);
        boolean found = false;
        for (ModelNode node : result.asList()) {
            if (msgA.getJMSMessageID().equals(node.get("JMSMessageID").asString())) {
                Assert.assertEquals(newPriority, node.get("JMSPriority").asInt());
                found = true;
                break;
            }
        }
        Assert.assertTrue(found);

        op = getQueueOperation("change-messages-priority");
        op.get("new-priority").set(newPriority);

        result = execute(op, true);
        Assert.assertTrue(result.isDefined());
        Assert.assertTrue(result.asInt() > 1 && result.asInt() < 4);

        result = execute(getQueueOperation("list-messages"), true);
        for (ModelNode node : result.asList()) {
            Assert.assertEquals(newPriority, node.get("JMSPriority").asInt());
        }

    }

    @Test
    public void testListConsumers() throws Exception {
        ConnectionFactory cf = (ConnectionFactory) remoteContext.lookup("jms/RemoteConnectionFactory");
        consumerConn = cf.createConnection("guest", "guest");
        consumerConn.start();
        consumerSession = consumerConn.createSession(false, AUTO_ACKNOWLEDGE);

        ModelNode result = execute(getQueueOperation("list-consumers-as-json"), true);
        Assert.assertTrue(result.isDefined());
        Assert.assertEquals(ModelType.STRING, result.getType());
    }

    @Test
    public void testAddJndi() throws Exception {
        ModelNode op = getQueueOperation("add-jndi");
        op.get("jndi-binding").set("queue/added" + count);

        ModelNode result = execute(op, true);
        Assert.assertFalse(result.isDefined());

        op = getQueueOperation("read-attribute");
        op.get("name").set("entries");
        result = execute(op, true);
        Assert.assertTrue(result.isDefined());
        for (ModelNode binding : result.asList()) {
            if (binding.asString().equals("queue/added" + count))
                return;
        }
        fail("queue/added" + count + " was not found");
    }

    @Test
    public void testRemoveJndi() throws Exception {
        String jndiName = "queue/added" + count;
        ModelNode op = getQueueOperation("add-jndi");
        op.get("jndi-binding").set(jndiName);

        ModelNode result = execute(op, true);
        Assert.assertFalse(result.isDefined());

        op = getQueueOperation("remove-jndi");
        op.get("jndi-binding").set(jndiName);
        result = execute(op, true);
        Assert.assertFalse(result.isDefined());

        op = getQueueOperation("read-attribute");
        op.get("name").set("entries");
        result = execute(op, true);
        Assert.assertTrue(result.isDefined());
        for (ModelNode binding : result.asList()) {
            if (binding.asString().equals(jndiName)) {
                Assert.fail("found " + jndiName + " while it must be removed");
            }
        }
    }

    @Test
    public void testRemoveLastJndi() throws Exception {
        ModelNode op = getQueueOperation("remove-jndi");
        op.get("jndi-binding").set(EXPORTED_PREFIX + getQueueJndiName());

        // removing the last jndi name must generate a failure
        execute(op, false);

        op = getQueueOperation("read-attribute");
        op.get("name").set("entries");
        ModelNode result = execute(op, true);
        Assert.assertTrue(result.isDefined());
        for (ModelNode binding : result.asList()) {
            if (binding.asString().equals(EXPORTED_PREFIX + getQueueJndiName()))
                return;
        }
        Assert.fail(getQueueJndiName() + " was not found");
    }

    private ModelNode getQueueOperation(String operationName) {
        final ModelNode address = adminSupport.getServerAddress().add("jms-queue", getQueueName());
        return getEmptyOperation(operationName, address);
    }

    private ModelNode execute(final ModelNode op, final boolean expectSuccess) throws IOException {
        ModelNode response = managementClient.getControllerClient().execute(op);
        final String outcome = response.get("outcome").asString();
        if (expectSuccess) {
            if (!"success".equals(outcome)) {
                LOGGER.trace(response);
            }
            Assert.assertEquals("success", outcome);
            return response.get("result");
        } else {
            if ("success".equals(outcome)) {
                LOGGER.trace(response);
            }
            Assert.assertEquals("failed", outcome);
            return response.get("failure-description");
        }
    }

    @Test
    public void removeJMSQueueRemovesAllMessages() throws Exception {
        MessageProducer producer = session.createProducer(queue);
        producer.send(session.createTextMessage("A"));

        MessageConsumer consumer = session.createConsumer(queue);

        ModelNode result = execute(getQueueOperation("count-messages"), true);
        Assert.assertTrue(result.isDefined());
        Assert.assertEquals(1, result.asInt());

        // remove the queue
        adminSupport.removeJmsQueue(getQueueName());
        // add the queue back
        adminSupport.createJmsQueue(getQueueName(), EXPORTED_PREFIX + getQueueJndiName());

        result = execute(getQueueOperation("count-messages"), true);
        Assert.assertTrue(result.isDefined());
        Assert.assertEquals(0, result.asInt());
    }

    private String getQueueName() {
        return getClass().getSimpleName() + count;
    }

    private String getQueueJndiName() {
        return "queue/" + getQueueName();
    }

    private String getOtherQueueName() {
        return getClass().getSimpleName() + "other" + count;
    }

    private String getOtherQueueJndiName() {
        return "queue/" + getOtherQueueName();
    }
}
