/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2021, Red Hat, Inc., and individual contributors
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
package org.jboss.as.test.manualmode.messaging;

import org.jboss.arquillian.container.test.api.ContainerController;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.as.arquillian.container.ManagementClient;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.test.integration.common.jms.JMSOperations;
import org.jboss.as.test.integration.common.jms.JMSOperationsProvider;
import org.jboss.as.test.integration.security.common.Utils;
import org.jboss.as.test.shared.TestSuiteEnvironment;
import org.jboss.as.test.shared.TimeoutUtil;
import org.jboss.dmr.ModelNode;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import jakarta.jms.Connection;
import jakarta.jms.ConnectionFactory;
import jakarta.jms.DeliveryMode;
import jakarta.jms.Message;
import jakarta.jms.MessageConsumer;
import jakarta.jms.MessageProducer;
import jakarta.jms.Queue;
import jakarta.jms.Session;
import jakarta.jms.TextMessage;
import jakarta.jms.Topic;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import java.io.IOException;
import java.util.Properties;
import java.util.UUID;

import static org.jboss.as.controller.client.helpers.ClientConstants.ADDRESS;
import static org.jboss.as.controller.client.helpers.ClientConstants.OP;
import static org.jboss.as.test.integration.management.util.ModelUtil.createOpNode;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

/**
 * Tests of setting up a core bridge from a queue to topic works.
 *
 * @author <a href="mailto:lgao@redhat.com">Lin Gao</a>
 */
@RunAsClient()
@RunWith(Arquillian.class)
public class CoreBridgeQueueToTopicTestCase {

    private static final String ROUTING_TYPE_PROPERTY = "org.wildfly.messaging.core.bridge.CoreBridge.routing-type";
    private static final String EXPORTED_PREFIX = "java:jboss/exported/";
    private static final String DEFAULT_FULL_JBOSSAS = "default-full-jbossas";
    private static final String REMOTE_CONNECTION_FACTORY_LOOKUP = "jms/RemoteConnectionFactory";
    private static final String SOURCE_QUEUE_NAME = "SourceQueue";
    private static final String SOURCE_QUEUE_LOOKUP = "jms/SourceQueue";
    private static final String TARGET_TOPIC_NAME = "TargetTopic";
    private static final String TARGET_TOPIC_LOOKUP = "jms/TargetTopic";
    private static final String BRIDGE_NAME = "CoreBridge";
    private static final String MESSAGE_TEXT = "Hello from Queue";

    @ArquillianResource
    protected static ContainerController container;

    private JMSOperations jmsOperations;
    private ManagementClient managementClient;

    @Before
    public void setUp() throws Exception {
        if (!container.isStarted(DEFAULT_FULL_JBOSSAS)) {
            container.start(DEFAULT_FULL_JBOSSAS);
        }
        managementClient = createManagementClient();
        jmsOperations = JMSOperationsProvider.getInstance(managementClient.getControllerClient());

        // create source and target topic and a bridge
        jmsOperations.createJmsQueue(SOURCE_QUEUE_NAME, EXPORTED_PREFIX + SOURCE_QUEUE_LOOKUP);
        removeMessages(false, SOURCE_QUEUE_NAME);
        jmsOperations.createJmsTopic(TARGET_TOPIC_NAME, EXPORTED_PREFIX + TARGET_TOPIC_LOOKUP);
        removeMessages(true, TARGET_TOPIC_NAME);
    }

    @After
    public void tearDown() throws Exception {
        // clean up queue, topic and bridge
        jmsOperations.removeJmsQueue(SOURCE_QUEUE_NAME);
        jmsOperations.removeJmsTopic(TARGET_TOPIC_NAME);
        if (jmsOperations != null) {
            jmsOperations.close();
        }
        if (managementClient != null) {
            managementClient.close();
        }
        container.stop(DEFAULT_FULL_JBOSSAS);
    }

    @Test
    public void testJMSMessageFromQueueToTopicFailure() throws Exception {
        try {
            createCoreBridge();
            TextMessage receivedMessage = getBridgedMessage("");
            assertNull("Expected message should be null because it won't pass message from queue to topic when routing-type is PASS", receivedMessage);
        } finally {
            jmsOperations.removeCoreBridge(BRIDGE_NAME);
        }
    }

    @Test
    public void testJMSMessageFromQueueToTopic() throws Exception {
        try {
            // set up system property to use STRIP routing type
            ModelNode operation = createOpNode("system-property=" + ROUTING_TYPE_PROPERTY, ModelDescriptionConstants.ADD);
            operation.get("value").set("STRIP");
            Utils.applyUpdate(operation, managementClient.getControllerClient());
            createCoreBridge();
            String messageSuffix = UUID.randomUUID().toString();
            TextMessage receivedMessage = getBridgedMessage(messageSuffix);
            assertNotNull("Expected message was not received", receivedMessage);
            assertEquals(createMessage(messageSuffix), receivedMessage.getText());
        } finally {
            ModelNode operation = createOpNode("system-property=" + ROUTING_TYPE_PROPERTY, ModelDescriptionConstants.REMOVE);
            Utils.applyUpdate(operation, managementClient.getControllerClient());
            jmsOperations.removeCoreBridge(BRIDGE_NAME);
        }
    }

    private String createMessage(String suffix) {
        return MESSAGE_TEXT + " ## " + suffix;
    }

    private TextMessage getBridgedMessage(String messageSuffix) throws Exception {
        InitialContext remoteContext = createJNDIContext();
        try {
            ConnectionFactory connectionFactory = (ConnectionFactory) remoteContext.lookup(REMOTE_CONNECTION_FACTORY_LOOKUP);
            Queue sourceQueue = (Queue) remoteContext.lookup(SOURCE_QUEUE_LOOKUP);
            Topic targetTopic = (Topic) remoteContext.lookup(TARGET_TOPIC_LOOKUP);
            try (Connection conn = connectionFactory.createConnection("guest", "guest");
                 Session session = conn.createSession(false, Session.AUTO_ACKNOWLEDGE);
                 MessageProducer producer = session.createProducer(sourceQueue);
                 MessageConsumer consumer = session.createConsumer(targetTopic)) {
                conn.start();
                Message message = session.createTextMessage(createMessage(messageSuffix));
                message.setJMSDeliveryMode(DeliveryMode.NON_PERSISTENT);
                producer.send(message);
                return (TextMessage) consumer.receive(TimeoutUtil.adjust(500));
            }
        } finally {
            remoteContext.close();
        }
    }

    private void createCoreBridge() {
        ModelNode bridgeAttributes = new ModelNode();
        bridgeAttributes.get("queue-name").set("jms.queue." + SOURCE_QUEUE_NAME);
        bridgeAttributes.get("forwarding-address").set("jms.topic." + TARGET_TOPIC_NAME);
        bridgeAttributes.get("static-connectors").add("in-vm");
        bridgeAttributes.get("use-duplicate-detection").set(false);
        bridgeAttributes.get("user").set("guest");
        bridgeAttributes.get("password").set("guest");
        jmsOperations.addCoreBridge(BRIDGE_NAME, bridgeAttributes);
    }

    private void removeMessages(boolean topic, String name) throws IOException {
        ModelNode operation = new ModelNode();
        String propertyName = topic ? "jms-topic" : "jms-queue";
        operation.get(ADDRESS).set(jmsOperations.getServerAddress().add(propertyName, name));
        operation.get(OP).set("remove-messages");
        jmsOperations.getControllerClient().execute(operation);
    }

    private static ManagementClient createManagementClient() {
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
