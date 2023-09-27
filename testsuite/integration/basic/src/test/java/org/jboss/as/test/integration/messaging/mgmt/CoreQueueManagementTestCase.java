/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.integration.messaging.mgmt;

import static java.util.UUID.randomUUID;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.INCLUDE_RUNTIME;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.util.HashMap;

import org.apache.activemq.artemis.api.core.TransportConfiguration;
import org.apache.activemq.artemis.api.core.client.ActiveMQClient;
import org.apache.activemq.artemis.api.core.client.ClientMessage;
import org.apache.activemq.artemis.api.core.client.ClientProducer;
import org.apache.activemq.artemis.api.core.client.ClientSession;
import org.apache.activemq.artemis.api.core.client.ClientSessionFactory;
import org.apache.activemq.artemis.api.core.client.ServerLocator;
import org.apache.activemq.artemis.core.remoting.impl.netty.NettyConnectorFactory;
import org.apache.activemq.artemis.core.remoting.impl.netty.TransportConstants;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.as.arquillian.api.ContainerResource;
import org.jboss.as.arquillian.container.ManagementClient;
import org.jboss.as.test.shared.TestSuiteEnvironment;
import org.jboss.as.test.shared.TimeoutUtil;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Tests the management API for Artemis core queues.
 *
 * @author Brian Stansberry (c) 2011 Red Hat Inc.
 */
@RunAsClient()
@RunWith(Arquillian.class)
public class CoreQueueManagementTestCase {

    private static long count = System.currentTimeMillis();

    private static final long TIMEOUT = TimeoutUtil.adjust(5000);

    @ContainerResource
    private ManagementClient managementClient;

    private ClientSessionFactory sessionFactory;
    private ClientSession session;
    private ClientSession consumerSession;

    @Before
    public void setup() throws Exception {

        count++;

        HashMap<String, Object> map = new HashMap<String, Object>();
        map.put("host", TestSuiteEnvironment.getServerAddress());
        map.put("port", 8080);
        map.put(TransportConstants.HTTP_UPGRADE_ENABLED_PROP_NAME, true);
        map.put(TransportConstants.HTTP_UPGRADE_ENDPOINT_PROP_NAME, "http-acceptor");
        TransportConfiguration transportConfiguration =
                new TransportConfiguration(NettyConnectorFactory.class.getName(), map);
        ServerLocator locator = ActiveMQClient.createServerLocatorWithoutHA(transportConfiguration);
        locator.setBlockOnDurableSend(true);
        locator.setBlockOnNonDurableSend(true);
        sessionFactory =  locator.createSessionFactory();

        session = sessionFactory.createSession("guest", "guest", false, true, true, false, 1);
        session.createQueue(getQueueName(), getQueueName(), false);
        session.createQueue(getOtherQueueName(), getOtherQueueName(), false);

        consumerSession = sessionFactory.createSession("guest", "guest", false, false, false, false, 1);
    }

    @After
    public void cleanup() throws Exception {

        if (consumerSession != null) {
            consumerSession.close();
        }

        if (session != null) {
            session.deleteQueue(getQueueName());
            session.deleteQueue(getOtherQueueName());
            session.close();
        }

        if (sessionFactory != null) {
            sessionFactory.cleanup();
            sessionFactory.close();
        }
    }

    @Test
    public void testReadResource() throws Exception {
        String address = randomUUID().toString();
        String queueName = randomUUID().toString();

        final ModelNode readQueueResourceOp = getQueueOperation("read-resource", queueName);

        final ModelNode readRuntimeQueueResourceOp = getRuntimeQueueOperation("read-resource", queueName);
        readRuntimeQueueResourceOp.get(INCLUDE_RUNTIME).set(true);

        // resource does not exist
        ModelNode result = execute(readQueueResourceOp, false);
        assertTrue(result.toJSONString(false), result.asString().contains("WFLYCTL0216"));
        result = execute(readRuntimeQueueResourceOp, false);
        assertTrue(result.toJSONString(false), result.asString().contains("WFLYCTL0216"));

        session.createQueue(address, queueName, false);

        // resource does not exist for core queue...
        result = execute(readQueueResourceOp, false);
        assertTrue(result.toJSONString(false), result.asString().contains("WFLYCTL0216"));
        // ... but it does for runtime-queue
        result = execute(readRuntimeQueueResourceOp, true);
        assertTrue(result.isDefined());
        assertEquals(address, result.get("queue-address").asString());

        session.deleteQueue(queueName);

        // resource no longer exists
        result = execute(readQueueResourceOp, false);
        assertTrue(result.toJSONString(false), result.asString().contains("WFLYCTL0216"));
        result = execute(readRuntimeQueueResourceOp, false);
        assertTrue(result.toJSONString(false), result.asString().contains("WFLYCTL0216"));
    }

    @Test
    public void testListAndCountMessages() throws Exception {
        ClientProducer producer = session.createProducer(getQueueName());
        ClientMessage message1 = session.createMessage(ClientMessage.TEXT_TYPE, false);
        message1.putStringProperty("hope", "message1");
        producer.send(message1);
        ClientMessage message2 = session.createMessage(ClientMessage.TEXT_TYPE, false);
        message2.putStringProperty("hope", "message2");
        producer.send(message2);

        listMessages(2);
        ModelNode result = execute(getQueueOperation("count-messages"), true);
        Assert.assertTrue(result.isDefined());
        Assert.assertEquals(2, result.asInt());
    }

    private ModelNode listMessages(int expectedSize) throws IOException, InterruptedException {
        final ModelNode listMessagesOperation = getQueueOperation("list-messages");
        long end = System.currentTimeMillis() + TIMEOUT;
        boolean passed = false;
        ModelNode result = null;
        while (end > System.currentTimeMillis()) {
            result = execute(listMessagesOperation, true);
            Assert.assertTrue(result.isDefined());
            passed = result.asList().size() == expectedSize;
            if (passed) {
                break;
            }
            Thread.sleep(100);
        }
        Assert.assertTrue("Here is what  we got instead of the " + expectedSize + " messages " + result, passed);
        return result;
    }

    @Test
    public void testMessageCounters() throws Exception {

        ClientProducer producer = session.createProducer(getQueueName());
        producer.send(session.createMessage(ClientMessage.TEXT_TYPE, false));
        producer.send(session.createMessage(ClientMessage.TEXT_TYPE, false));

        ModelNode result = execute(getQueueOperation("list-message-counter-as-json"), true);
        Assert.assertTrue(result.isDefined());
        Assert.assertEquals(ModelType.STRING, result.getType());

        result = execute(getQueueOperation("list-message-counter-as-html"), true);
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

        result = execute(getQueueOperation("list-message-counter-history-as-json"), true);
        Assert.assertTrue(result.isDefined());
        Assert.assertEquals(ModelType.STRING, result.getType());

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

        ClientProducer producer = session.createProducer(getQueueName());
        ClientMessage msgA = session.createMessage(ClientMessage.TEXT_TYPE, false);
        producer.send(msgA);
        producer.send(session.createMessage(ClientMessage.TEXT_TYPE, false));
        producer.send(session.createMessage(ClientMessage.TEXT_TYPE, false));

        final ModelNode op = getQueueOperation("remove-message");
        op.get("message-id").set(findMessageID());

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

    private long findMessageID() throws Exception {
        final ModelNode result = execute(getQueueOperation("list-messages"), true);
        return result.get(0).get("messageID").asLong();
    }

//    @org.junit.Ignore("AS7-2480")
    @Test
    public void testMessageMovement() throws Exception {

        ClientProducer producer = session.createProducer(getQueueName());
        ClientMessage msgA = session.createMessage(ClientMessage.TEXT_TYPE, false);
        producer.send(msgA);
        producer.send(session.createMessage(ClientMessage.TEXT_TYPE, false));
        producer.send(session.createMessage(ClientMessage.TEXT_TYPE, false));

        ModelNode op = getQueueOperation("move-message");
        op.get("message-id").set(findMessageID());
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

//    @org.junit.Ignore("AS7-2480")
    @Test
    public void testChangeMessagePriority() throws Exception {
        final int priority = 3;
        ClientProducer producer = session.createProducer(getQueueName());
        producer.send(session.createMessage(ClientMessage.TEXT_TYPE, false).setPriority((byte)priority));
        long id = findMessageID();
        producer.send(session.createMessage(ClientMessage.TEXT_TYPE, false).setPriority((byte)priority));
        producer.send(session.createMessage(ClientMessage.TEXT_TYPE, false).setPriority((byte)priority));

        ModelNode result = listMessages(3);
        for (ModelNode node : result.asList()) {
            Assert.assertEquals("Priority should be " + priority, priority, node.get("priority").asInt());
        }
        int newPriority = 5;
        ModelNode op = getQueueOperation("change-message-priority");
        op.get("message-id").set(id);
        op.get("new-priority").set(newPriority);

        result = execute(op, true);
        Assert.assertTrue(result.isDefined());
        Assert.assertTrue(result.asBoolean());

        result = listMessages(3);
        boolean found = false;
        for (ModelNode node : result.asList()) {
            if (id == node.get("messageID").asLong()) {
                Assert.assertEquals("Message should have the new priority", newPriority, node.get("priority").asInt());
                found = true;
            } else {
                Assert.assertEquals("Message should have the set priority", priority, node.get("priority").asInt());
            }
        }
        Assert.assertTrue("We should have found the updated message", found);

        op = getQueueOperation("change-messages-priority");
        op.get("new-priority").set(newPriority);
        result = execute(op, true);
        Assert.assertTrue(result.isDefined());
        Assert.assertTrue(result.asInt() > 1 && result.asInt() < 4);

        result = listMessages(3);
        for (ModelNode node : result.asList()) {
            Assert.assertEquals("Message should have the new priority", newPriority, node.get("priority").asInt());
        }

    }

    @Test
    public void testListConsumers() throws Exception {

        consumerSession.createConsumer(getQueueName());

        ModelNode result = execute(getQueueOperation("list-consumers-as-json"), true);
        Assert.assertTrue(result.isDefined());
        Assert.assertEquals(ModelType.STRING, result.getType());
    }

    private ModelNode getQueueOperation(String operationName, String queueName) {
        final ModelNode address = new ModelNode();
        address.add("subsystem", "messaging-activemq");
        address.add("server", "default");
        address.add("queue", queueName);
        return org.jboss.as.controller.operations.common.Util.getEmptyOperation(operationName, address);
    }

    private ModelNode getRuntimeQueueOperation(String operationName, String queueName) {
        final ModelNode address = new ModelNode();
        address.add("subsystem", "messaging-activemq");
        address.add("server", "default");
        address.add("runtime-queue", queueName);
        return org.jboss.as.controller.operations.common.Util.getEmptyOperation(operationName, address);
    }

    private ModelNode getQueueOperation(String operationName) {
        return getQueueOperation(operationName, getQueueName());
    }

    private ModelNode execute(final ModelNode op, final boolean expectSuccess) throws IOException {
        ModelNode response = managementClient.getControllerClient().execute(op);
        final String outcome = response.get("outcome").asString();
        if (expectSuccess) {
            Assert.assertEquals("success", outcome);
            return response.get("result");
        } else {
            Assert.assertEquals("failed", outcome);
            return response.get("failure-description");
        }
    }

    private String getQueueName() {
        return getClass().getSimpleName() + count;
    }

    private String getOtherQueueName() {
        return getClass().getSimpleName() + "other" + count;
    }
}
