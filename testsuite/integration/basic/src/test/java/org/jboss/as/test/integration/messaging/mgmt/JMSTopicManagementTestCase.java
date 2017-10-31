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
import static org.jboss.as.controller.client.helpers.ClientConstants.NAME;
import static org.jboss.as.controller.client.helpers.ClientConstants.VALUE;
import static org.jboss.as.controller.client.helpers.ClientConstants.WRITE_ATTRIBUTE_OPERATION;
import static org.jboss.as.controller.operations.common.Util.getEmptyOperation;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import javax.jms.Connection;
import javax.jms.ConnectionFactory;
import javax.jms.MessageProducer;
import javax.jms.Session;
import javax.jms.Topic;
import javax.jms.TopicSubscriber;
import javax.naming.Context;

import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.as.arquillian.api.ContainerResource;
import org.jboss.as.arquillian.container.ManagementClient;
import org.jboss.as.controller.client.ModelControllerClient;
import org.jboss.as.controller.client.OperationBuilder;
import org.jboss.as.controller.client.helpers.ClientConstants;
import org.jboss.as.test.integration.common.jms.JMSOperations;
import org.jboss.as.test.integration.common.jms.JMSOperationsProvider;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Tests the management API for JMS topics.
 *
 * @author Brian Stansberry (c) 2011 Red Hat Inc.
 */
@RunAsClient()
@RunWith(Arquillian.class)
public class JMSTopicManagementTestCase {

    private static final String EXPORTED_PREFIX = "java:jboss/exported/";

    private static long count = System.currentTimeMillis();
    private ConnectionFactory cf;

    @ContainerResource
    private ManagementClient managementClient;

    @ContainerResource
    private Context remoteContext;

    private JMSOperations adminSupport;

    private Connection conn;
    private Topic topic;
    private Session session;

    private Connection consumerConn;
    private Session consumerSession;

    @Before
    public void before() throws Exception {
        cf = (ConnectionFactory) remoteContext.lookup("jms/RemoteConnectionFactory");

        adminSupport = JMSOperationsProvider.getInstance(managementClient.getControllerClient());
        count++;

        adminSupport.createJmsTopic(getTopicName(), EXPORTED_PREFIX + getTopicJndiName());

        topic = (Topic) remoteContext.lookup(getTopicJndiName());

        conn = cf.createConnection("guest", "guest");
        conn.setClientID("sender");
        conn.start();
        session = conn.createSession(false, AUTO_ACKNOWLEDGE);

        consumerConn = cf.createConnection("guest", "guest");
        consumerConn.setClientID("consumer");
        consumerConn.start();
        consumerSession = consumerConn.createSession(false, AUTO_ACKNOWLEDGE);

        addSecuritySettings();
    }

    private void addSecuritySettings() throws Exception {
        // <jms server address>/security-setting=#/role=guest:write-attribute(name=create-durable-queue, value=TRUE)
        ModelNode address = adminSupport.getServerAddress()
                .add("security-setting", "#")
                .add("role", "guest");

        ModelNode op = new ModelNode();
        op.get(ClientConstants.OP).set(WRITE_ATTRIBUTE_OPERATION);
        op.get(ClientConstants.OP_ADDR).set(address);
        op.get(NAME).set("create-durable-queue");
        op.get(VALUE).set(true);
        applyUpdate(op, managementClient.getControllerClient());

        op = new ModelNode();
        op.get(ClientConstants.OP).set(WRITE_ATTRIBUTE_OPERATION);
        op.get(ClientConstants.OP_ADDR).set(address);
        op.get(NAME).set("delete-durable-queue");
        op.get(VALUE).set(true);
        applyUpdate(op, managementClient.getControllerClient());
    }

    @After
    public void after() throws Exception {

        removeSecuritySetting();

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
            adminSupport.removeJmsTopic(getTopicName());
            adminSupport.close();
        }
    }

    private void removeSecuritySetting() throws Exception {
        // <jms server address>/security-setting=#/role=guest:write-attribute(name=create-durable-queue, value=FALSE)
        ModelNode address = adminSupport.getServerAddress()
                .add("security-setting", "#")
                .add("role", "guest");

        ModelNode op = new ModelNode();
        op.get(ClientConstants.OP).set(WRITE_ATTRIBUTE_OPERATION);
        op.get(ClientConstants.OP_ADDR).set(address);
        op.get(NAME).set("create-durable-queue");
        op.get(VALUE).set(false);
        applyUpdate(op, managementClient.getControllerClient());

        op = new ModelNode();
        op.get(ClientConstants.OP).set(WRITE_ATTRIBUTE_OPERATION);
        op.get(ClientConstants.OP_ADDR).set(address);
        op.get(NAME).set("delete-durable-queue");
        op.get(VALUE).set(false);
        applyUpdate(op, managementClient.getControllerClient());
    }

    @Test
    public void testListMessagesForSubscription() throws Exception {
        consumerSession.createDurableSubscriber(topic, "testListMessagesForSubscription", null, false);
        consumerSession.close();
        consumerSession = null;

        MessageProducer producer = session.createProducer(topic);
        producer.send(session.createTextMessage("A"));
        producer.send(session.createTextMessage("B"));
//        session.commit();

        ModelNode result = execute(getTopicOperation("list-all-subscriptions"), true);
        final ModelNode subscriber = result.asList().get(0);
        //System.out.println(result);


        ModelNode operation = getTopicOperation("list-messages-for-subscription");
        operation.get("queue-name").set(subscriber.get("queueName"));

        result = execute(operation, true);
        assertTrue(result.isDefined());
        Assert.assertEquals(2, result.asList().size());

    }

    @Test
    public void testCountMessagesForSubscription() throws Exception {
        consumerSession.createDurableSubscriber(topic, "testCountMessagesForSubscription", null, false);
        consumerSession.close();
        consumerSession = null;
        MessageProducer producer = session.createProducer(topic);
        producer.send(session.createTextMessage("A"));
        producer.send(session.createTextMessage("B"));
//        session.commit();

        ModelNode result = execute(getTopicOperation("list-all-subscriptions"), true);
        final ModelNode subscriber = result.asList().get(0);


        ModelNode operation = getTopicOperation("count-messages-for-subscription");
        operation.get("client-id").set(subscriber.get("clientID"));
        operation.get("subscription-name").set("testCountMessagesForSubscription");

        result = execute(operation, true);
        assertTrue(result.isDefined());
        Assert.assertEquals(2, result.asInt());
    }

    @Test
    public void testListAllSubscriptions() throws Exception {
        session.createDurableSubscriber(topic, "testListAllSubscriptions", "foo=bar", false);
        session.createConsumer(topic);

        final ModelNode result = execute(getTopicOperation("list-all-subscriptions"), true);
        assertTrue(result.isDefined());
        Assert.assertEquals(2, result.asList().size());
    }

    @Test
    public void testListAllSubscriptionsAsJSON() throws Exception {
        session.createDurableSubscriber(topic, "testListAllSubscriptionsAsJSON", "foo=bar", false);
        session.createConsumer(topic);

        final ModelNode result = execute(getTopicOperation("list-all-subscriptions-as-json"), true);
        assertTrue(result.isDefined());
        Assert.assertEquals(ModelType.STRING, result.getType());
    }

    @Test
    public void testListDurableSubscriptions() throws Exception {
        session.createDurableSubscriber(topic, "testListDurableSubscriptions", "foo=bar", false);
        session.createConsumer(topic);

        final ModelNode result = execute(getTopicOperation("list-durable-subscriptions"), true);
        assertTrue(result.isDefined());
        Assert.assertEquals(1, result.asList().size());
    }

    @Test
    public void testListDurableSubscriptionsAsJSON() throws Exception {
        session.createDurableSubscriber(topic, "testListDurableSubscriptionsAsJSON", "foo=bar", false);
        session.createConsumer(topic);

        final ModelNode result = execute(getTopicOperation("list-durable-subscriptions-as-json"), true);
        assertTrue(result.isDefined());
        Assert.assertEquals(ModelType.STRING, result.getType());
    }

    @Test
    public void testListNonDurableSubscriptions() throws Exception {
        session.createDurableSubscriber(topic, "testListNonDurableSubscriptions", "foo=bar", false);
        session.createConsumer(topic, "foo=bar", false);

        final ModelNode result = execute(getTopicOperation("list-non-durable-subscriptions"), true);
        assertTrue(result.isDefined());
        Assert.assertEquals(1, result.asList().size());
    }

    @Test
    public void testListNonDurableSubscriptionsAsJSON() throws Exception {
        session.createDurableSubscriber(topic, "testListNonDurableSubscriptionsAsJSON", "foo=bar", false);
        session.createConsumer(topic, "foo=bar", false);

        final ModelNode result = execute(getTopicOperation("list-non-durable-subscriptions-as-json"), true);
        assertTrue(result.isDefined());
        Assert.assertEquals(ModelType.STRING, result.getType());
    }

    @Test
    public void testDropDurableSubscription() throws Exception {
        consumerSession.createDurableSubscriber(topic, "testDropDurableSubscription", "foo=bar", false);
        consumerSession.close();
        consumerSession = null;

        ModelNode result = execute(getTopicOperation("list-durable-subscriptions"), true);
        assertTrue(result.isDefined());
        Assert.assertEquals(1, result.asList().size());

        ModelNode op = getTopicOperation("drop-durable-subscription");
        op.get("client-id").set("consumer");
        op.get("subscription-name").set("testDropDurableSubscription");
        result = execute(op, true);
        Assert.assertFalse(result.isDefined());

        result = execute(getTopicOperation("list-durable-subscriptions"), true);
        assertTrue(result.isDefined());
        Assert.assertEquals(0, result.asList().size());
    }

    @Test
    public void testDropAllSubscription() throws Exception {
        consumerSession.createDurableSubscriber(topic, "testDropAllSubscription", "foo=bar", false);
        consumerSession.createDurableSubscriber(topic, "testDropAllSubscription2", null, false);
        consumerSession.close();
        consumerSession = null;

        ModelNode result = execute(getTopicOperation("list-all-subscriptions"), true);
        assertTrue(result.isDefined());
        Assert.assertEquals(2, result.asList().size());

        result = execute(getTopicOperation("drop-all-subscriptions"), true);
        Assert.assertFalse(result.isDefined());

        result = execute(getTopicOperation("list-all-subscriptions"), true);
        assertTrue(result.isDefined());
        Assert.assertEquals(0, result.asList().size());
    }

    @Test
    public void testAddJndi() throws Exception {
        String jndiName = "topic/added" + count;
        ModelNode op = getTopicOperation("add-jndi");
        op.get("jndi-binding").set(jndiName);

        ModelNode result = execute(op, true);
        Assert.assertFalse(result.isDefined());

        op = getTopicOperation("read-attribute");
        op.get("name").set("entries");
        result = execute(op, true);
        assertTrue(result.isDefined());
        for (ModelNode binding : result.asList()) {
            if (binding.asString().equals(jndiName)) { return; }
        }
        Assert.fail(jndiName + " was not found");
    }

    @Test
    public void testRemoveJndi() throws Exception {
        String jndiName = "topic/added" + count;
        ModelNode op = getTopicOperation("add-jndi");
        op.get("jndi-binding").set(jndiName);

        ModelNode result = execute(op, true);
        Assert.assertFalse(result.isDefined());

        op = getTopicOperation("remove-jndi");
        op.get("jndi-binding").set(jndiName);
        result = execute(op, true);
        Assert.assertFalse(result.isDefined());

        op = getTopicOperation("read-attribute");
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
        ModelNode op = getTopicOperation("remove-jndi");
        op.get("jndi-binding").set(EXPORTED_PREFIX + getTopicJndiName());

        // removing the last jndi name must generate a failure
        execute(op, false);

        op = getTopicOperation("read-attribute");
        op.get("name").set("entries");
        ModelNode result = execute(op, true);
        Assert.assertTrue(result.isDefined());
        for (ModelNode binding : result.asList()) {
            if (binding.asString().equals(EXPORTED_PREFIX + getTopicJndiName())) { return; }
        }
        Assert.fail(getTopicJndiName() + " was not found");
    }

    @Test
    public void removeJMSTopicRemovesAllMessages() throws Exception {

        // create a durable subscriber
        final String subscriptionName = "removeJMSTopicRemovesAllMessages";
        // stop the consumer connection to prevent eager consumption of messages
        consumerConn.stop();
        TopicSubscriber consumer = consumerSession.createDurableSubscriber(topic, subscriptionName);
        MessageProducer producer = session.createProducer(topic);
        producer.send(session.createTextMessage("A"));

        ModelNode operation = getTopicOperation("count-messages-for-subscription");
        operation.get("client-id").set(consumerConn.getClientID());
        operation.get("subscription-name").set(subscriptionName);

        ModelNode result = execute(operation, true);
        assertTrue(result.isDefined());
        Assert.assertEquals(1, result.asInt());

        // remove the topic
        adminSupport.removeJmsTopic(getTopicName());
        // add the topic
        adminSupport.createJmsTopic(getTopicName(), getTopicJndiName());
        // and recreate the durable subscriber to check all the messages have
        // been removed from the topic
        consumerSession.createDurableSubscriber(topic, subscriptionName);

        result = execute(operation, true);
        assertTrue(result.isDefined());
        Assert.assertEquals(0, result.asInt());
    }

    private ModelNode getTopicOperation(String operationName) {
        final ModelNode address = adminSupport.getServerAddress()
                .add("jms-topic", getTopicName());
        return getEmptyOperation(operationName, address);
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

    private String getTopicName() {
        return getClass().getSimpleName() + count;
    }

    private String getTopicJndiName() {
        return "topic/" + getTopicName();
    }

    static void applyUpdate(ModelNode update, final ModelControllerClient client) throws IOException {
        ModelNode result = client.execute(new OperationBuilder(update).build());
        if (result.hasDefined("outcome") && "success".equals(result.get("outcome").asString())) {
            /*if (result.hasDefined("result")) {
                System.out.println(result.get("result"));
            }*/
        } else if (result.hasDefined("failure-description")) {
            throw new RuntimeException(result.get("failure-description").toString());
        } else {
            throw new RuntimeException("Operation not successful; outcome = " + result.get("outcome"));
        }
    }
}
