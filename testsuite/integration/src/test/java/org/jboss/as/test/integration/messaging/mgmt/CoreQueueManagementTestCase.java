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
import java.util.HashSet;
import java.util.Set;

import junit.framework.Assert;
import org.hornetq.api.core.TransportConfiguration;
import org.hornetq.api.core.client.ClientMessage;
import org.hornetq.api.core.client.ClientProducer;
import org.hornetq.api.core.client.ClientSession;
import org.hornetq.api.core.client.ClientSessionFactory;
import org.hornetq.api.core.client.HornetQClient;
import org.hornetq.api.core.client.ServerLocator;
import org.hornetq.core.remoting.impl.netty.NettyConnectorFactory;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.as.test.integration.common.JMSAdminOperations;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Tests the management API for HornetQ core queues.
 *
 * @author Brian Stansberry (c) 2011 Red Hat Inc.
 */
@RunAsClient()
@RunWith(Arquillian.class)
public class CoreQueueManagementTestCase {

    private static JMSAdminOperations adminSupport;
    private static long count = System.currentTimeMillis();

    @BeforeClass
    public static void connectManagmentClient() {
        adminSupport = new JMSAdminOperations();
    }

    @AfterClass
    public static void closeManagementClient() {
        if (adminSupport != null) {
            adminSupport.close();
        }
    }

    private ClientSession session;
    private ClientSession consumerSession;

    @Before
    public void addTopic() throws Exception {

        count++;

        TransportConfiguration transportConfiguration =
                     new TransportConfiguration(NettyConnectorFactory.class.getName());
        ServerLocator locator = HornetQClient.createServerLocatorWithoutHA(transportConfiguration);
        ClientSessionFactory factory =  locator.createSessionFactory();
        session = factory.createSession();
        session.createQueue(getQueueName(), getQueueName(), false);
        session.createQueue(getOtherQueueName(), getOtherQueueName(), false);
    }

    @After
    public void removeTopic() throws Exception {

        if (session != null) {
            session.close();
        }

        if (consumerSession != null) {
            consumerSession.close();
        }
    }

    @Test
    public void testListAndCountMessages() throws Exception {

        ClientProducer producer = session.createProducer(getQueueName());
        producer.send(session.createMessage(ClientMessage.TEXT_TYPE, false));
        producer.send(session.createMessage(ClientMessage.TEXT_TYPE, false));

        ModelNode result = execute(getQueueOperation("list-messages"), true);
        Assert.assertTrue(result.isDefined());
        Assert.assertEquals(2, result.asList().size());

        result = execute(getQueueOperation("count-messages"), true);
        Assert.assertTrue(result.isDefined());
        Assert.assertEquals(2, result.asInt());
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

    @Test
    public void testChangeMessagePriority() throws Exception {

        ClientProducer producer = session.createProducer(getQueueName());
        ClientMessage msgA = session.createMessage(ClientMessage.TEXT_TYPE, false);
        producer.send(msgA);
        producer.send(session.createMessage(ClientMessage.TEXT_TYPE, false));
        producer.send(session.createMessage(ClientMessage.TEXT_TYPE, false));

        Set<Integer> priorities = new HashSet<Integer>();
        ModelNode result = execute(getQueueOperation("list-messages"), true);
        Assert.assertEquals(3, result.asInt());
        for (ModelNode node : result.asList()) {
            priorities.add(node.get("priority").asInt());
        }
        int newPriority = -1;
        for (int i = 0; i < 10; i++) {
            if (!priorities.contains(i)) {
                newPriority = i;
                break;
            }
        }

        long id = findMessageID();
        ModelNode op = getQueueOperation("change-message-priority");
        op.get("message-id").set(id);
        op.get("new-priority").set(newPriority);

        result = execute(op, true);
        Assert.assertTrue(result.isDefined());
        Assert.assertTrue(result.asBoolean());

        result = execute(getQueueOperation("list-messages"), true);
        boolean found = false;
        for (ModelNode node : result.asList()) {
            if (id == node.get("messageID").asLong()) {
                Assert.assertEquals(newPriority, node.get("priority").asInt());
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
            Assert.assertEquals(newPriority, node.get("priority").asInt());
        }

    }

    @Test
    public void testListConsumers() throws Exception {

        TransportConfiguration transportConfiguration =
                     new TransportConfiguration(NettyConnectorFactory.class.getName());
        ServerLocator locator = HornetQClient.createServerLocatorWithoutHA(transportConfiguration);
        ClientSessionFactory factory =  locator.createSessionFactory();
        consumerSession = factory.createSession();
        session.createConsumer(getQueueName());

        ModelNode result = execute(getQueueOperation("list-consumers-as-json"), true);
        Assert.assertTrue(result.isDefined());
        Assert.assertEquals(ModelType.STRING, result.getType());
    }

    private ModelNode getQueueOperation(String operationName) {
        final ModelNode address = new ModelNode();
        address.add("subsystem", "messaging");
        address.add("hornetq-server", "default");
        address.add("queue", getQueueName());
        return org.jboss.as.controller.operations.common.Util.getEmptyOperation(operationName, address);
    }

    private ModelNode execute(final ModelNode op, final boolean expectSuccess) throws IOException {
        ModelNode response = adminSupport.getModelControllerClient().execute(op);
        final String outcome = response.get("outcome").asString();
        if (expectSuccess) {
            if (!"success".equals(outcome)) {
                System.out.println(response);
            }
            Assert.assertEquals("success", outcome);
            return response.get("result");
        } else {
            if ("success".equals(outcome)) {
                System.out.println(response);
            }
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
