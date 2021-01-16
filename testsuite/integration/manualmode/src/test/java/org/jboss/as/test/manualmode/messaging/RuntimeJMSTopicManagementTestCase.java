/*
 * Copyright 2020 JBoss by Red Hat.
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
package org.jboss.as.test.manualmode.messaging;

import static javax.jms.JMSContext.AUTO_ACKNOWLEDGE;
import static org.jboss.as.controller.client.helpers.ClientConstants.NAME;
import static org.jboss.as.controller.client.helpers.ClientConstants.VALUE;
import static org.jboss.as.controller.client.helpers.ClientConstants.WRITE_ATTRIBUTE_OPERATION;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.net.UnknownHostException;
import java.util.Properties;
import javax.jms.Connection;
import javax.jms.ConnectionFactory;
import javax.jms.JMSException;
import javax.jms.MessageProducer;
import javax.jms.Session;
import javax.jms.TextMessage;
import javax.jms.Topic;
import javax.jms.TopicSubscriber;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import org.jboss.arquillian.container.test.api.ContainerController;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.as.arquillian.container.ManagementClient;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.client.ModelControllerClient;
import org.jboss.as.controller.client.helpers.ClientConstants;
import org.jboss.as.controller.client.helpers.Operations;
import org.jboss.as.test.integration.common.jms.JMSOperations;
import org.jboss.as.test.integration.common.jms.JMSOperationsProvider;
import org.jboss.as.test.shared.TestSuiteEnvironment;
import org.jboss.as.test.shared.TimeoutUtil;
import org.jboss.dmr.ModelNode;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 *
 * @author Emmanuel Hugonnet (c) 2020 Red Hat, Inc.
 */
@RunAsClient()
@RunWith(Arquillian.class)
public class RuntimeJMSTopicManagementTestCase {

    private static final String EXPORTED_PREFIX = "java:jboss/exported/";

    private static long count = System.currentTimeMillis();

    private static final String DEFAULT_FULL_JBOSSAS = "default-full-jbossas";

    @ArquillianResource
    protected static ContainerController container;

    /**
     * Scenario:
     *  - start the server
     *  - pause the topic
     *  - send a message to it and ensure that the consumer don't get it
     *  - restart the server
     *  - the topic should still be paused as its state was persisted
     *  - the consumer shouldn't get the message
     *  - resume the topic
     *  - the consumer should get the message.
     * @throws IOException
     * @throws NamingException
     * @throws JMSException
     * @throws InterruptedException
     */
    @Test
    public void testPauseAndResumePersisted() throws IOException, NamingException, JMSException, InterruptedException {
        count = System.currentTimeMillis();
        if (!container.isStarted(DEFAULT_FULL_JBOSSAS)) {
            container.start(DEFAULT_FULL_JBOSSAS);
        }
        InitialContext remoteContext = createJNDIContext();
        ManagementClient managementClient = createManagementClient();
        JMSOperations adminSupport = JMSOperationsProvider.getInstance(managementClient.getControllerClient());
        adminSupport.createJmsTopic(getTopicName(), EXPORTED_PREFIX + getTopicJndiName());
        addSecuritySettings(adminSupport);
        assertFalse("Topic should be running", isTopicPaused(managementClient));
        ConnectionFactory cf = (ConnectionFactory) remoteContext.lookup("jms/RemoteConnectionFactory");
        Topic topic = (Topic) remoteContext.lookup(getTopicJndiName());

        final String subscriptionName = "pauseJMSTopicPersisted";
        try (Connection conn = cf.createConnection("guest", "guest")) {
            conn.setClientID("sender");
            try (Session session = conn.createSession(false, AUTO_ACKNOWLEDGE)) {
                conn.start();
                try (Connection consumerConn = cf.createConnection("guest", "guest")) {
                    consumerConn.setClientID("consumer");
                    try (Session consumerSession = consumerConn.createSession(false, AUTO_ACKNOWLEDGE)) {
                        consumerConn.start();
                        TopicSubscriber consumer = consumerSession.createDurableSubscriber(topic, subscriptionName);
                        pauseTopic(managementClient, true);
                        MessageProducer producer = session.createProducer(topic);
                        producer.send(session.createTextMessage("A"));

                        TextMessage message = (TextMessage) consumer.receive(TimeoutUtil.adjust(500));
                        Assert.assertNull("The message was received by the consumer, this is wrong as the connection is paused", message);
                        Assert.assertEquals(1, countMessageSubscriptions(managementClient, consumerConn.getClientID(), subscriptionName));
                    }
                }
            }
        }
        adminSupport.close();
        managementClient.close();
        remoteContext.close();
        container.stop(DEFAULT_FULL_JBOSSAS);

        container.start(DEFAULT_FULL_JBOSSAS);
        remoteContext = createJNDIContext();
        managementClient = createManagementClient();
        assertTrue("Topic should be paused", isTopicPaused(managementClient));
        cf = (ConnectionFactory) remoteContext.lookup("jms/RemoteConnectionFactory");
        topic = (Topic) remoteContext.lookup(getTopicJndiName());
        try (Connection consumerConn = cf.createConnection("guest", "guest")) {
            consumerConn.setClientID("consumer");
            try (Session consumerSession = consumerConn.createSession(false, AUTO_ACKNOWLEDGE);) {
                consumerConn.start();
                TopicSubscriber consumer = consumerSession.createDurableSubscriber(topic, subscriptionName);
                TextMessage message = (TextMessage) consumer.receive(TimeoutUtil.adjust(500));
                Assert.assertNull("The message was received by the consumer, this is wrong as the connection is paused", message);
                Assert.assertEquals(1, countMessageSubscriptions(managementClient, consumerConn.getClientID(), subscriptionName));
                resumeTopic(managementClient);
                assertFalse("Topic should be running", isTopicPaused(managementClient));
                message = (TextMessage) consumer.receive(TimeoutUtil.adjust(500));
                Assert.assertNotNull("The message was not received by the consumer, this is wrong as the connection is resumed", message);
                Assert.assertEquals("A", message.getText());
                Thread.sleep(TimeoutUtil.adjust(500));
                Assert.assertEquals(0, countMessageSubscriptions(managementClient, consumerConn.getClientID(), subscriptionName));
            }
        }
        adminSupport = JMSOperationsProvider.getInstance(managementClient.getControllerClient());
        adminSupport.removeJmsTopic(getTopicName());
        adminSupport.close();
        managementClient.close();
        remoteContext.close();
        container.stop(DEFAULT_FULL_JBOSSAS);
    }

    /**
     * Scenario:
     *  - start the server
     *  - pause the topic
     *  - send a message to it and ensure that the consumer don't get it
     *  - restart the server
     *  - the topic should resume as its state wasn't persisted
     *  - the consumer should get the message.
     * @throws IOException
     * @throws NamingException
     * @throws JMSException
     * @throws InterruptedException
     */
    @Test
    public void testPauseAndResume() throws IOException, NamingException, JMSException, InterruptedException {
        count = System.currentTimeMillis();
        if (!container.isStarted(DEFAULT_FULL_JBOSSAS)) {
            container.start(DEFAULT_FULL_JBOSSAS);
        }
        InitialContext remoteContext = createJNDIContext();
        ManagementClient managementClient = createManagementClient();
        JMSOperations adminSupport = JMSOperationsProvider.getInstance(managementClient.getControllerClient());
        adminSupport.createJmsTopic(getTopicName(), EXPORTED_PREFIX + getTopicJndiName());
        addSecuritySettings(adminSupport);
        assertFalse("Topic should be running", isTopicPaused(managementClient));
        ConnectionFactory cf = (ConnectionFactory) remoteContext.lookup("jms/RemoteConnectionFactory");
        Topic topic = (Topic) remoteContext.lookup(getTopicJndiName());

        final String subscriptionName = "pauseJMSTopic";
        try (Connection conn = cf.createConnection("guest", "guest")) {
            conn.setClientID("sender");
            try (Session session = conn.createSession(false, AUTO_ACKNOWLEDGE)) {
                conn.start();
                try (Connection consumerConn = cf.createConnection("guest", "guest")) {
                    consumerConn.setClientID("consumer");
                    try (Session consumerSession = consumerConn.createSession(false, AUTO_ACKNOWLEDGE)) {
                        consumerConn.start();
                        TopicSubscriber consumer = consumerSession.createDurableSubscriber(topic, subscriptionName);
                        pauseTopic(managementClient, false);
                        MessageProducer producer = session.createProducer(topic);
                        producer.send(session.createTextMessage("A"));

                        TextMessage message = (TextMessage) consumer.receive(TimeoutUtil.adjust(500));
                        Assert.assertNull("The message was received by the consumer, this is wrong as the connection is paused", message);
                        Assert.assertEquals(1, countMessageSubscriptions(managementClient, consumerConn.getClientID(), subscriptionName));
                        consumer.close();
                        producer.close();
                    }
                }
            }
        }
        adminSupport.close();
        managementClient.close();
        remoteContext.close();
        container.stop(DEFAULT_FULL_JBOSSAS);

        container.start(DEFAULT_FULL_JBOSSAS);
        remoteContext = createJNDIContext();
        managementClient = createManagementClient();
        assertFalse("Topic should be running", isTopicPaused(managementClient));
        cf = (ConnectionFactory) remoteContext.lookup("jms/RemoteConnectionFactory");
        topic = (Topic) remoteContext.lookup(getTopicJndiName());
        try (Connection consumerConn = cf.createConnection("guest", "guest")) {
            consumerConn.setClientID("consumer");
            try (Session consumerSession = consumerConn.createSession(false, AUTO_ACKNOWLEDGE);) {
                consumerConn.start();
                TopicSubscriber consumer = consumerSession.createDurableSubscriber(topic, subscriptionName);
                TextMessage message = (TextMessage) consumer.receive(TimeoutUtil.adjust(500));
                Assert.assertNotNull("The message was not received by the consumer, this is wrong as the connection is resumed", message);
                Assert.assertEquals("A", message.getText());
                Thread.sleep(TimeoutUtil.adjust(500));
                Assert.assertEquals(0, countMessageSubscriptions(managementClient, consumerConn.getClientID(), subscriptionName));
            }
        }
        adminSupport = JMSOperationsProvider.getInstance(managementClient.getControllerClient());
        adminSupport.removeJmsTopic(getTopicName());
        adminSupport.close();
        managementClient.close();
        remoteContext.close();
        container.stop(DEFAULT_FULL_JBOSSAS);
    }

    private ModelNode getTopicAddress() {
        return PathAddress.parseCLIStyleAddress("/subsystem=messaging-activemq/server=default/jms-topic=" + getTopicName()).toModelNode();
    }

    private String getTopicName() {
        return getClass().getSimpleName() + count;
    }

    private String getTopicJndiName() {
        return "topic/" + getTopicName();
    }

    private ModelNode pauseTopic(ManagementClient managementClient, boolean persist) throws IOException {
        ModelNode operation = Operations.createOperation("pause", getTopicAddress());
        operation.get("persist").set(persist);
        return execute(managementClient.getControllerClient(), operation, true);
    }

    private int countMessageSubscriptions(ManagementClient managementClient, String clientID, String subscriptionName) throws IOException {
        ModelNode operation = Operations.createOperation("count-messages-for-subscription", getTopicAddress());
        operation.get("client-id").set(clientID);
        operation.get("subscription-name").set(subscriptionName);
        return execute(managementClient.getControllerClient(), operation, true).asInt();
    }

    private ModelNode resumeTopic(ManagementClient managementClient) throws IOException {
        return execute(managementClient.getControllerClient(), Operations.createOperation("resume", getTopicAddress()), true);
    }

    private ModelNode execute(final ModelControllerClient client, final ModelNode op, final boolean expectSuccess) throws IOException {
        ModelNode response = client.execute(op);
        if (expectSuccess) {
            assertTrue(response.toJSONString(true), Operations.isSuccessfulOutcome(response));
            return Operations.readResult(response);
        }
        assertEquals("failed", response.get("outcome").asString());
        return response.get("failure-description");
    }

    private boolean isTopicPaused(ManagementClient managementClient) throws IOException {
        return execute(managementClient.getControllerClient(), Operations.createReadAttributeOperation(getTopicAddress(), "paused"), true).asBoolean();
    }

    private void addSecuritySettings(JMSOperations adminSupport) throws IOException {
        // <jms server address>/security-setting=#/role=guest:write-attribute(name=create-durable-queue, value=TRUE)
        ModelNode address = adminSupport.getServerAddress()
                .add("security-setting", "#")
                .add("role", "guest");

        ModelNode op = new ModelNode();
        op.get(ClientConstants.OP).set(WRITE_ATTRIBUTE_OPERATION);
        op.get(ClientConstants.OP_ADDR).set(address);
        op.get(NAME).set("create-durable-queue");
        op.get(VALUE).set(true);
        execute(adminSupport.getControllerClient(), op, true);

        op = new ModelNode();
        op.get(ClientConstants.OP).set(WRITE_ATTRIBUTE_OPERATION);
        op.get(ClientConstants.OP_ADDR).set(address);
        op.get(NAME).set("delete-durable-queue");
        op.get(VALUE).set(true);
        execute(adminSupport.getControllerClient(), op, true);
    }

    private static ManagementClient createManagementClient() throws UnknownHostException {
        return new ManagementClient(
                TestSuiteEnvironment.getModelControllerClient(),
                TestSuiteEnvironment.formatPossibleIpv6Address(TestSuiteEnvironment.getServerAddress()),
                TestSuiteEnvironment.getServerPort(),
                "remote+http");
    }

    protected static InitialContext createJNDIContext() throws NamingException {
        final Properties env = new Properties();
        env.put(Context.INITIAL_CONTEXT_FACTORY, "org.jboss.naming.remote.client.InitialContextFactory");
        String ipAdddress = TestSuiteEnvironment.formatPossibleIpv6Address(TestSuiteEnvironment.getServerAddress());
        env.put(Context.PROVIDER_URL, System.getProperty(Context.PROVIDER_URL, "remote+http://" + ipAdddress + ":8080"));
        env.put(Context.SECURITY_PRINCIPAL, "guest");
        env.put(Context.SECURITY_CREDENTIALS, "guest");
        return new InitialContext(env);
    }

}
