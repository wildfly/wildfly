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

import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.as.arquillian.container.ManagementClient;
import org.jboss.as.test.integration.common.jms.JMSOperations;
import org.jboss.as.test.integration.common.jms.JMSOperationsProvider;
import org.jboss.as.test.shared.TestSuiteEnvironment;
import org.jboss.byteman.agent.submit.Submit;
import org.jboss.dmr.ModelNode;
import org.jboss.logging.Logger;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.wildfly.core.testrunner.Server;
import org.wildfly.core.testrunner.ServerControl;
import org.wildfly.core.testrunner.ServerController;
import org.wildfly.core.testrunner.WildflyTestRunner;

import javax.inject.Inject;
import javax.jms.Connection;
import javax.jms.ConnectionFactory;
import javax.jms.DeliveryMode;
import javax.jms.Message;
import javax.jms.MessageConsumer;
import javax.jms.MessageProducer;
import javax.jms.Queue;
import javax.jms.Session;
import javax.jms.TextMessage;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import java.io.IOException;
import java.util.Collections;
import java.util.Properties;
import java.util.UUID;

import static org.jboss.as.controller.client.helpers.ClientConstants.ADDRESS;
import static org.jboss.as.controller.client.helpers.ClientConstants.OP;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/**
 * This test verifies that setting a call timeout on a core bridge takes an effect.
 *
 * @author Tomas Hofman
 */
@RunWith(WildflyTestRunner.class)
@RunAsClient
@ServerControl(manual = true)
public class CoreBridgeCallTimeoutTestCase {

    private static final Logger logger = Logger.getLogger(CoreBridgeCallTimeoutTestCase.class);

    private static final String SERVER_CONFIG_NAME = "standalone-full.xml";
    private static final String EXPORTED_PREFIX = "java:jboss/exported/";
    private static final String REMOTE_CONNECTION_FACTORY_LOOKUP = "jms/RemoteConnectionFactory";
    private static final String SOURCE_QUEUE_NAME = "SourceQueue";
    private static final String SOURCE_QUEUE_LOOKUP = "jms/SourceQueue";
    private static final String TARGET_QUEUE_NAME = "TargetQueue";
    private static final String TARGET_QUEUE_LOOKUP = "jms/TargetQueue";
    private static final String BRIDGE_NAME = "CoreBridge";
    private static final String MESSAGE_TEXT = "Hello world!";

    private static final String BYTEMAN_ADDRESS = System.getProperty("byteman.server.ipaddress", Submit.DEFAULT_ADDRESS);
    private static final Integer BYTEMAN_PORT = Integer.getInteger("byteman.server.port", Submit.DEFAULT_PORT);
    private static final Submit BYTEMAN = new Submit(BYTEMAN_ADDRESS, BYTEMAN_PORT);

    private static final String ORIGINAL_JVM_ARGS = System.getProperty("jvm.args", "");
    private static final String BASE_DIR = System.getProperty("basedir", "./");

    @Inject
    protected static ServerController container;

    private JMSOperations jmsOperations;
    private ManagementClient managementClient;

    @BeforeClass
    public static void startContainer() {
        System.out.println("jvm.args:" + System.getProperty("jvm.args"));
        System.out.println("basedir:" + System.getProperty("basedir"));
        System.setProperty("jvm.args", ORIGINAL_JVM_ARGS +
                " -Xmx512m -XX:MaxMetaspaceSize=256m " +
                "-Dorg.jboss.byteman.verbose " +
                "-Djboss.modules.system.pkgs=org.jboss.byteman " +
                "-Dorg.jboss.byteman.transform.all " +
                "-javaagent:" + BASE_DIR + "/target/lib/byteman.jar=listener:true,port:" + BYTEMAN_PORT + ",address:" + BYTEMAN_ADDRESS + ",policy:true");
        container.start(SERVER_CONFIG_NAME, Server.StartMode.NORMAL);
    }

    @AfterClass
    public static void stopContainer() {
        System.setProperty("jvm.args", ORIGINAL_JVM_ARGS);
        container.stop();
    }

    @Before
    public void setUp() throws IOException {
        managementClient = createManagementClient();
        jmsOperations = JMSOperationsProvider.getInstance(managementClient.getControllerClient());

        // create source and target queues and a bridge
        jmsOperations.createJmsQueue(SOURCE_QUEUE_NAME, EXPORTED_PREFIX + SOURCE_QUEUE_LOOKUP);
        removeMessagesFromQueue(SOURCE_QUEUE_NAME);
        jmsOperations.createJmsQueue(TARGET_QUEUE_NAME, EXPORTED_PREFIX + TARGET_QUEUE_LOOKUP);
        removeMessagesFromQueue(TARGET_QUEUE_NAME);
    }

    @After
    public void tearDown() {
        // clean up
        jmsOperations.removeJmsQueue(SOURCE_QUEUE_NAME);
        jmsOperations.removeJmsQueue(TARGET_QUEUE_NAME);
        if (jmsOperations != null) {
            jmsOperations.close();
        }
        if (managementClient != null) {
            managementClient.close();
        }
    }

    /**
     * Scenario:
     *
     * <ol>
     * <li>Create a bridge without a call-timeout.</li>
     * <li>Verify that messages are transferred.</li>
     * </ol>
     */
    @Test(timeout = 150000)
    public void testCoreBridge() throws Exception {
        Message receivedMessage = verifyCoreBridge(null); // no call timeout

        // assert that message was delivered
        assertNotNull("Expected message was not received", receivedMessage);
    }

    /**
     * Scenario:
     *
     * <ol>
     * <li>Create a bridge with call-timeout of 500 ms, and introduce delays to bridge calls via a byteman rule.</li>
     * <li>Verify that messages are not transferred.</li>
     * </ol>
     */
    @Test(timeout = 150000)
    public void testCallTimeoutWithFailure() throws Exception {
        try {
            insertBytemanRules(); // introduce 1 sec delay to bridge calls
            Message receivedMessage = verifyCoreBridge(500L);// set call timeout to 500 ms

            // assert that message was delivered
            assertNull("No message was expected to be transferred by the bridge", receivedMessage);
        } finally {
            deleteBytemanRules();
        }
    }

    private Message verifyCoreBridge(Long callTimeout) throws Exception {
        createCoreBridge(callTimeout);

        InitialContext remoteContext = createJNDIContext();
        ConnectionFactory connectionFactory = (ConnectionFactory) remoteContext.lookup(REMOTE_CONNECTION_FACTORY_LOOKUP);

        // lookup source and target queues
        Queue sourceQueue = (Queue) remoteContext.lookup(SOURCE_QUEUE_LOOKUP);
        Queue targetQueue = (Queue) remoteContext.lookup(TARGET_QUEUE_LOOKUP);

        try (Connection connection = connectionFactory.createConnection("guest", "guest")) {
            try (Session session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE)) {

                // send a message on the source queue
                MessageProducer producer = session.createProducer(sourceQueue);
                String text = MESSAGE_TEXT + " " + UUID.randomUUID().toString();
                Message message = session.createTextMessage(text);
                message.setJMSDeliveryMode(DeliveryMode.NON_PERSISTENT);
                producer.send(message);

                // receive a message from the target queue
                MessageConsumer consumer = session.createConsumer(targetQueue);
                connection.start();
                Message receivedMessage = consumer.receive(5000);

                // if some message was delivered, assert it's the same message which was sent
                if (receivedMessage != null) {
                    assertTrue(receivedMessage instanceof TextMessage);
                    assertEquals(text, ((TextMessage) receivedMessage).getText());
                }

                return receivedMessage;
            }
        } finally {
            jmsOperations.removeCoreBridge(BRIDGE_NAME);
            remoteContext.close();
        }
    }

    private void createCoreBridge(Long callTimeout) {
        ModelNode bridgeAttributes = new ModelNode();
        bridgeAttributes.get("queue-name").set("jms.queue." + SOURCE_QUEUE_NAME);
        bridgeAttributes.get("forwarding-address").set("jms.queue." + TARGET_QUEUE_NAME);
        bridgeAttributes.get("static-connectors").add("in-vm");
        bridgeAttributes.get("use-duplicate-detection").set(false);
        if (callTimeout != null) {
            bridgeAttributes.get("call-timeout").set(callTimeout);
        }
        bridgeAttributes.get("user").set("guest");
        bridgeAttributes.get("password").set("guest");
        jmsOperations.addCoreBridge(BRIDGE_NAME, bridgeAttributes);
    }

    private void removeMessagesFromQueue(String queueName) throws IOException {
        ModelNode operation = new ModelNode();
        operation.get(ADDRESS).set(jmsOperations.getServerAddress().add("jms-queue", queueName));
        operation.get(OP).set("remove-messages");
        jmsOperations.getControllerClient().execute(operation);
    }

    private static void insertBytemanRules() throws Exception {
        logger.info("Installing byteman rules.");
        BYTEMAN.addRulesFromResources(Collections.singletonList(
                CoreBridgeCallTimeoutTestCase.class.getClassLoader().getResourceAsStream("byteman/"
                        + CoreBridgeCallTimeoutTestCase.class.getSimpleName()
                        + ".btm")));
    }

    private static void deleteBytemanRules() throws Exception {
        logger.info("Removing byteman rules.");
        BYTEMAN.deleteAllRules();
    }

    private static ManagementClient createManagementClient() {
        return new ManagementClient(
                TestSuiteEnvironment.getModelControllerClient(),
                TestSuiteEnvironment.formatPossibleIpv6Address(TestSuiteEnvironment.getServerAddress()),
                TestSuiteEnvironment.getServerPort(),
                "remote+http");
    }

    private static InitialContext createJNDIContext() throws NamingException {
        final Properties env = new Properties();
        env.put(Context.INITIAL_CONTEXT_FACTORY, "org.jboss.naming.remote.client.InitialContextFactory");
        env.put(Context.PROVIDER_URL, "remote+http://" + TestSuiteEnvironment.getServerAddress() + ":8080");
        env.put(Context.SECURITY_PRINCIPAL, "guest");
        env.put(Context.SECURITY_CREDENTIALS, "guest");
        return new InitialContext(env);
    }

}
