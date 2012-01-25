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
import java.lang.Exception;
import java.net.InetAddress;

import javax.jms.MessageProducer;
import javax.jms.Topic;
import javax.jms.TopicConnection;
import javax.jms.TopicSession;

import junit.framework.Assert;
import org.hornetq.api.core.TransportConfiguration;
import org.hornetq.api.jms.HornetQJMSClient;
import org.hornetq.api.jms.JMSFactoryType;
import org.hornetq.core.remoting.impl.netty.NettyConnectorFactory;
import org.hornetq.jms.client.HornetQConnectionFactory;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.as.arquillian.container.Authentication;
import org.jboss.as.controller.client.ModelControllerClient;
import org.jboss.as.controller.client.OperationBuilder;
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
 * Tests the management API for JMS topics.
 *
 * @author Brian Stansberry (c) 2011 Red Hat Inc.
 */
@RunAsClient()
@RunWith(Arquillian.class)
//@Ignore("Ignore failing tests")
public class JMSTopicManagementTestCase {

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

    private TopicConnection conn;
    private Topic topic;
    private TopicSession session;

    private TopicConnection consumerConn;
    private TopicSession consumerSession;

    @Before
    public void addTopic() throws Exception {

        count++;
        final String jndiName = getTopicJndiName();
        adminSupport.createJmsTopic(getTopicName(), jndiName);

        TransportConfiguration transportConfiguration =
                     new TransportConfiguration(NettyConnectorFactory.class.getName());
        HornetQConnectionFactory cf = HornetQJMSClient.createConnectionFactoryWithoutHA(JMSFactoryType.CF, transportConfiguration);
        cf.setClientID("sender");
        conn = cf.createTopicConnection();
        conn.start();
        topic = HornetQJMSClient.createTopic(getTopicName());
        session = conn.createTopicSession(false, TopicSession.AUTO_ACKNOWLEDGE);

        cf = HornetQJMSClient.createConnectionFactoryWithoutHA(JMSFactoryType.CF, transportConfiguration);
        cf.setClientID("consumer");
        consumerConn = cf.createTopicConnection();
        consumerConn.start();
        consumerSession = consumerConn.createTopicSession(false, TopicSession.AUTO_ACKNOWLEDGE);
    }

    @Before
    public void addSecuritySettings() throws Exception
    {
        final ModelControllerClient client = ModelControllerClient.Factory.create(InetAddress.getByName("localhost"), 9999, Authentication.getCallbackHandler());
        //subsystem=messaging/hornetq-server=default/security-setting=#/role=guest:write-attribute(name=create-durable-queue, value=TRUE)
        ModelNode op = new ModelNode();
        op.get("operation").set("write-attribute");
        op.get("address").add("subsystem", "messaging");
        op.get("address").add("hornetq-server", "default");
        op.get("address").add("security-setting", "#");
        op.get("address").add("role", "guest");
        op.get("name").set("create-durable-queue");
        op.get("value").set(true);
        applyUpdate(op, client);

        op = new ModelNode();
        op.get("operation").set("write-attribute");
        op.get("address").add("subsystem", "messaging");
        op.get("address").add("hornetq-server", "default");
        op.get("address").add("security-setting", "#");
        op.get("address").add("role", "guest");
        op.get("name").set("delete-durable-queue");
        op.get("value").set(true);
        applyUpdate(op, client);
    }

    @After
    public void removeTopic() throws Exception {

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

        adminSupport.removeJmsTopic(getTopicName());
    }

    @After
    public void removeSecuritySetting() throws Exception
    {
        final ModelControllerClient client = ModelControllerClient.Factory.create(InetAddress.getByName("localhost"), 9999, Authentication.getCallbackHandler());
        //subsystem=messaging/hornetq-server=default/security-setting=#/role=guest:write-attribute(name=create-durable-queue, value=FALSE)
        ModelNode op = new ModelNode();
        op.get("operation").set("write-attribute");
        op.get("address").add("subsystem", "messaging");
        op.get("address").add("hornetq-server", "default");
        op.get("address").add("security-setting", "#");
        op.get("address").add("role", "guest");
        op.get("name").set("create-durable-queue");
        op.get("value").set(false);
        applyUpdate(op, client);

        op = new ModelNode();
        op.get("operation").set("write-attribute");
        op.get("address").add("subsystem", "messaging");
        op.get("address").add("hornetq-server", "default");
        op.get("address").add("security-setting", "#");
        op.get("address").add("role", "guest");
        op.get("name").set("delete-durable-queue");
        op.get("value").set(false);
        applyUpdate(op, client);
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
        System.out.println(result);


        ModelNode operation = getTopicOperation("list-messages-for-subscription");
        operation.get("queue-name").set(subscriber.get("queueName"));

        result = execute(operation, true);
        Assert.assertTrue(result.isDefined());
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
        Assert.assertTrue(result.isDefined());
        Assert.assertEquals(2, result.asInt());
    }

    @Test
    public void testListAllSubscriptions() throws Exception {
        session.createDurableSubscriber(topic, "testListAllSubscriptions", "foo=bar", false);
        session.createSubscriber(topic);

        final ModelNode result = execute(getTopicOperation("list-all-subscriptions"), true);
        Assert.assertTrue(result.isDefined());
        Assert.assertEquals(2, result.asList().size());
    }

    @Test
    public void testListAllSubscriptionsAsJSON() throws Exception {
        session.createDurableSubscriber(topic, "testListAllSubscriptionsAsJSON", "foo=bar", false);
        session.createSubscriber(topic);

        final ModelNode result = execute(getTopicOperation("list-all-subscriptions-as-json"), true);
        Assert.assertTrue(result.isDefined());
        Assert.assertEquals(ModelType.STRING, result.getType());
    }

    @Test
    public void testListDurableSubscriptions() throws Exception {
        session.createDurableSubscriber(topic, "testListDurableSubscriptions", "foo=bar", false);
        session.createSubscriber(topic);

        final ModelNode result = execute(getTopicOperation("list-durable-subscriptions"), true);
        Assert.assertTrue(result.isDefined());
        Assert.assertEquals(1, result.asList().size());
    }

    @Test
    public void testListDurableSubscriptionsAsJSON() throws Exception {
        session.createDurableSubscriber(topic, "testListDurableSubscriptionsAsJSON", "foo=bar", false);
        session.createSubscriber(topic);

        final ModelNode result = execute(getTopicOperation("list-durable-subscriptions-as-json"), true);
        Assert.assertTrue(result.isDefined());
        Assert.assertEquals(ModelType.STRING, result.getType());
    }

    @Test
    public void testListNonDurableSubscriptions() throws Exception {
        session.createDurableSubscriber(topic, "testListNonDurableSubscriptions", "foo=bar", false);
        session.createSubscriber(topic, "foo=bar", false);

        final ModelNode result = execute(getTopicOperation("list-non-durable-subscriptions"), true);
        Assert.assertTrue(result.isDefined());
        Assert.assertEquals(1, result.asList().size());
    }

    @Test
    public void testListNonDurableSubscriptionsAsJSON() throws Exception {
        session.createDurableSubscriber(topic, "testListNonDurableSubscriptionsAsJSON", "foo=bar", false);
        session.createSubscriber(topic, "foo=bar", false);

        final ModelNode result = execute(getTopicOperation("list-non-durable-subscriptions-as-json"), true);
        Assert.assertTrue(result.isDefined());
        Assert.assertEquals(ModelType.STRING, result.getType());
    }

    @Test
    public void testDropDurableSubscription() throws Exception {
        consumerSession.createDurableSubscriber(topic, "testDropDurableSubscription", "foo=bar", false);
        consumerSession.close();
        consumerSession = null;

        ModelNode result = execute(getTopicOperation("list-durable-subscriptions"), true);
        Assert.assertTrue(result.isDefined());
        Assert.assertEquals(1, result.asList().size());

        ModelNode op = getTopicOperation("drop-durable-subscription");
        op.get("client-id").set("consumer");
        op.get("subscription-name").set("testDropDurableSubscription");
        result = execute(op, true);
        Assert.assertFalse(result.isDefined());

        result = execute(getTopicOperation("list-durable-subscriptions"), true);
        Assert.assertTrue(result.isDefined());
        Assert.assertEquals(0, result.asList().size());
    }

    @Test
    public void testDropAllSubscription() throws Exception {
        consumerSession.createDurableSubscriber(topic, "testDropAllSubscription", "foo=bar", false);
        consumerSession.createDurableSubscriber(topic, "testDropAllSubscription2", null, false);
        consumerSession.close();
        consumerSession = null;

        ModelNode result = execute(getTopicOperation("list-all-subscriptions"), true);
        Assert.assertTrue(result.isDefined());
        Assert.assertEquals(2, result.asList().size());

        result = execute(getTopicOperation("drop-all-subscriptions"), true);
        Assert.assertFalse(result.isDefined());

        result = execute(getTopicOperation("list-all-subscriptions"), true);
        Assert.assertTrue(result.isDefined());
        Assert.assertEquals(0, result.asList().size());
    }

    @Test
    public void testAddJndi() throws Exception {
        ModelNode op = getTopicOperation("add-jndi");
        op.get("jndi-binding").set("topic/added" + count);

        ModelNode result = execute(op, true);
        Assert.assertFalse(result.isDefined());

        op = getTopicOperation("read-attribute");
        op.get("name").set("entries");
        result = execute(op, true);
        Assert.assertTrue(result.isDefined());
        for (ModelNode binding : result.asList()) {
            if (binding.asString().equals("topic/added" + count))
                return;
        }
        Assert.fail("topic/added" + count + " was not found");
    }

    private ModelNode getTopicOperation(String operationName) {
        final ModelNode address = new ModelNode();
        address.add("subsystem", "messaging");
        address.add("hornetq-server", "default");
        address.add("jms-topic", getTopicName());
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

    private String getTopicName() {
        return getClass().getSimpleName() + count;
    }

    private String getTopicJndiName() {
        return "topic/" + getTopicName();
    }

   static void applyUpdate(ModelNode update, final ModelControllerClient client) throws IOException {
        ModelNode result = client.execute(new OperationBuilder(update).build());
        if (result.hasDefined("outcome") && "success".equals(result.get("outcome").asString())) {
            if (result.hasDefined("result")) {
                System.out.println(result.get("result"));
            }
        }
        else if (result.hasDefined("failure-description")){
            throw new RuntimeException(result.get("failure-description").toString());
        }
        else {
            throw new RuntimeException("Operation not successful; outcome = " + result.get("outcome"));
        }
    }
}
