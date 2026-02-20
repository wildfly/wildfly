/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.integration.messaging.jms.deployment;

import static org.jboss.as.test.integration.messaging.jms.deployment.BaseConnectionPerSessionMDB.QUEUE_LOOKUP;
import static org.jboss.as.test.integration.messaging.jms.deployment.BaseConnectionPerSessionMDB.QUEUE_NAME;
import static org.jboss.as.test.integration.messaging.jms.deployment.BaseConnectionPerSessionMDB.SINGLE_CONNECTION_SYSTEM_PROPERTY_NAME;
import static org.jboss.shrinkwrap.api.ShrinkWrap.create;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import javax.naming.Context;
import javax.naming.InitialContext;
import java.io.StringReader;
import java.time.Duration;
import java.util.HashSet;
import java.util.Properties;
import java.util.Set;
import java.util.UUID;

import jakarta.jms.ConnectionFactory;
import jakarta.jms.Destination;
import jakarta.jms.JMSConsumer;
import jakarta.jms.JMSContext;
import jakarta.jms.Queue;
import jakarta.json.Json;
import jakarta.json.JsonArray;
import jakarta.json.JsonObject;
import org.awaitility.Awaitility;
import org.jboss.arquillian.container.test.api.Deployer;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.as.arquillian.api.ServerSetup;
import org.jboss.as.arquillian.api.ServerSetupTask;
import org.jboss.as.arquillian.container.ManagementClient;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.client.helpers.Operations;
import org.jboss.as.controller.operations.common.Util;
import org.jboss.as.test.integration.common.jms.JMSOperations;
import org.jboss.as.test.integration.common.jms.JMSOperationsProvider;
import org.jboss.as.test.integration.management.base.ContainerResourceMgmtTestBase;
import org.jboss.as.test.shared.TestSuiteEnvironment;
import org.jboss.as.test.shared.TimeoutUtil;
import org.jboss.dmr.ModelNode;
import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.wildfly.naming.client.WildFlyInitialContextFactory;

/**
 * Tests for singleConnection activation config property with various configurations.
 */
@RunAsClient()
@RunWith(Arquillian.class)
@ServerSetup(SingleConnectionActivationPropertyTestCase.MessagingResourcesSetupTask.class)
public class SingleConnectionActivationPropertyTestCase extends ContainerResourceMgmtTestBase {

    private static final String SINGLE_CONNECTION_MDB = "singleConnectionPerSessionMdb";
    private static final String CONNECTION_PER_SESSION_MDB = "connectionPerSessionMdb";
    private static final String SINGLE_CONNECTION_WITH_EXPRESSION_MDB = "singleConnectionWithExpressionMDB";

    @ArquillianResource
    Deployer deployer;

    private final ModelNode listConsumersOnQueueOperation = Util.createOperation("list-consumers-as-json",
            PathAddress.pathAddress("subsystem", "messaging-activemq")
                    .append("server", "default")
                    .append("jms-queue", QUEUE_NAME));

    static class MessagingResourcesSetupTask implements ServerSetupTask {

        @Override
        public void setup(ManagementClient managementClient, String containerId) throws Exception {
            JMSOperations jmsOperations = JMSOperationsProvider.getInstance(managementClient.getControllerClient());
            jmsOperations.createJmsQueue(QUEUE_NAME, QUEUE_LOOKUP);
            executeOperation(managementClient, Operations.createWriteAttributeOperation(
                    new ModelNode().add("subsystem", "ee"), "annotation-property-replacement", true));
        }

        @Override
        public void tearDown(ManagementClient managementClient, String containerId) throws Exception {
            JMSOperations jmsOperations = JMSOperationsProvider.getInstance(managementClient.getControllerClient());
            jmsOperations.removeJmsQueue(QUEUE_NAME);
            executeOperation(managementClient, Operations.createWriteAttributeOperation(
                    new ModelNode().add("subsystem", "ee"), "annotation-property-replacement", false));
        }
    }

    @Deployment(name = SINGLE_CONNECTION_MDB, testable = false, managed = false)
    public static JavaArchive createSingleConnectionMdb() {
        return create(JavaArchive.class, SINGLE_CONNECTION_MDB + ".jar")
                .addClasses(SingleConnectionPerSessionMDB.class, BaseConnectionPerSessionMDB.class)
                .addAsManifestResource(EmptyAsset.INSTANCE, "beans.xml");
    }

    @Deployment(name = CONNECTION_PER_SESSION_MDB, testable = false, managed = false)
    public static JavaArchive createConnectionPerSessionMdb() {
        return create(JavaArchive.class, CONNECTION_PER_SESSION_MDB + ".jar")
                .addClasses(ConnectionPerSessionMDB.class, BaseConnectionPerSessionMDB.class)
                .addAsManifestResource(EmptyAsset.INSTANCE, "beans.xml");
    }

    @Deployment(name = SINGLE_CONNECTION_WITH_EXPRESSION_MDB, testable = false, managed = false)
    public static JavaArchive createSingleConnectionExpressionMdb() {
        return create(JavaArchive.class, SINGLE_CONNECTION_WITH_EXPRESSION_MDB + ".jar")
                .addClasses(SingleConnectionWithExpressionMDB.class, BaseConnectionPerSessionMDB.class)
                .addAsManifestResource(EmptyAsset.INSTANCE, "beans.xml");
    }

    /**
     * Tests that an MDB with singleConnection=true creates just single connection during
     * deploy, undeploy, and redeploy lifecycle.
     */
    @Test
    public void testMdbRedeploymentWithSingleConnection() throws Exception {
        try {
            deployer.deploy(SINGLE_CONNECTION_MDB);
            Awaitility.await("MDB deployment should create exactly 1 connection with singleConnection=true")
                    .atMost(Duration.ofMillis(TimeoutUtil.adjust(5000)))
                    .until(() -> countConnectionsOnQueue() == 1);
            assertEquals("Number of JMS sessions on queue does not match.", 15, countSessionsOnQueue());
            sendAndReceiveMessage();

            // Undeploy the MDB
            deployer.undeploy(SINGLE_CONNECTION_MDB);
            Awaitility.await("There must be no connection when MDB is undeployed.")
                    .atMost(Duration.ofMillis(TimeoutUtil.adjust(5000)))
                    .until(() -> countConnectionsOnQueue() == 0);
            assertEquals("Number of JMS sessions on queue does not match.", 0, countSessionsOnQueue());

            // Redeploy the MDB
            deployer.deploy(SINGLE_CONNECTION_MDB);
            Awaitility.await("MDB deployment should create exactly 1 connection with singleConnection=true")
                    .atMost(Duration.ofMillis(TimeoutUtil.adjust(5000)))
                    .until(() -> countConnectionsOnQueue() == 1);
            assertEquals("Number of JMS sessions on queue does not match.", 15, countSessionsOnQueue());
            sendAndReceiveMessage();
        } finally {
            deployer.undeploy(SINGLE_CONNECTION_MDB);
        }
    }

    /**
     * Tests that an MDB with singleConnection=false creates one connection per session (15 connections
     * for maxSession=15).
     */
    @Test
    public void testSingleConnectionFalse() throws Exception {
        try {
            deployer.deploy(CONNECTION_PER_SESSION_MDB);
            Awaitility.await("MDB deployment should create exactly 15 connections with singleConnection=false")
                    .atMost(Duration.ofMillis(TimeoutUtil.adjust(5000)))
                    .until(() -> countConnectionsOnQueue() == 15);
            assertEquals("Number of JMS sessions on queue does not match.", 15, countSessionsOnQueue());
            sendAndReceiveMessage();
        } finally {
            deployer.undeploy(CONNECTION_PER_SESSION_MDB);
        }
    }

    /**
     * Tests that the singleConnection property supports expression resolution with a default value.
     * When the system property is not set, ${mdb.single.connection:true} resolves to true (1 connection).
     */
    @Test
    public void testExpressionSupportForSingleConnectionNotSet() throws Exception {
        try {
            deployer.deploy(SINGLE_CONNECTION_WITH_EXPRESSION_MDB);
            Awaitility.await("MDB deployment should create exactly 1 connection.")
                    .atMost(Duration.ofMillis(TimeoutUtil.adjust(5000)))
                    .until(() -> countConnectionsOnQueue() == 1);
            assertEquals("Number of JMS sessions on queue does not match.", 15, countSessionsOnQueue());
            sendAndReceiveMessage();
        } finally {
            deployer.undeploy(SINGLE_CONNECTION_WITH_EXPRESSION_MDB);
        }
    }

    /**
     * Tests that the singleConnection property expression resolves to true when system property
     * is explicitly set to "true", resulting in 1 shared connection.
     */
    @Test
    public void testExpressionSupportForSingleConnectionTrue() throws Exception {
        addSystemProperty(SINGLE_CONNECTION_SYSTEM_PROPERTY_NAME, "true");
        try {
            deployer.deploy(SINGLE_CONNECTION_WITH_EXPRESSION_MDB);
            Awaitility.await("MDB deployment should create exactly 1 connection.")
                    .atMost(Duration.ofMillis(TimeoutUtil.adjust(5000)))
                    .until(() -> countConnectionsOnQueue() == 1);
            assertEquals("Number of JMS sessions on queue does not match.", 15, countSessionsOnQueue());
            sendAndReceiveMessage();
        } finally {
            deployer.undeploy(SINGLE_CONNECTION_WITH_EXPRESSION_MDB);
            removeSystemProperty(SINGLE_CONNECTION_SYSTEM_PROPERTY_NAME);
        }
    }

    /**
     * Tests that the singleConnection property expression resolves to false when system property
     * is explicitly set to "false", resulting in 15 separate connections (one per session).
     */
    @Test
    public void testExpressionSupportForSingleConnectionFalse() throws Exception {
        addSystemProperty(SINGLE_CONNECTION_SYSTEM_PROPERTY_NAME, "false");
        try {
            deployer.deploy(SINGLE_CONNECTION_WITH_EXPRESSION_MDB);
            Awaitility.await("MDB deployment should create exactly 15 connections.")
                    .atMost(Duration.ofMillis(TimeoutUtil.adjust(5000)))
                    .until(() -> countConnectionsOnQueue() == 15);
            assertEquals("Number of JMS sessions on queue does not match.", 15, countSessionsOnQueue());
            sendAndReceiveMessage();
        } finally {
            deployer.undeploy(SINGLE_CONNECTION_WITH_EXPRESSION_MDB);
            removeSystemProperty(SINGLE_CONNECTION_SYSTEM_PROPERTY_NAME);
        }
    }

    /**
     * Counts the number of connections with consumer on queue
     */
    private int countConnectionsOnQueue() throws Exception {
        ModelNode result = executeOperation(listConsumersOnQueueOperation);
        Set<String> connectionSet = new HashSet<>();
        JsonArray jsonArray = Json.createReader(new StringReader(result.asString())).readArray();
        for (int i = 0; i < jsonArray.size(); i++) {
            JsonObject obj = jsonArray.getJsonObject(i);
            connectionSet.add(obj.getString("connectionID"));
        }
        return connectionSet.size();
    }

    /**
     * Counts the number of JMS sessions with consumer on queue
     */
    private int countSessionsOnQueue() throws Exception {
        ModelNode result = executeOperation(listConsumersOnQueueOperation);
        Set<String> sessionSet = new HashSet<>();
        JsonArray jsonArray = Json.createReader(new StringReader(result.asString())).readArray();
        for (int i = 0; i < jsonArray.size(); i++) {
            JsonObject obj = jsonArray.getJsonObject(i);
            sessionSet.add(obj.getString("sessionID"));
        }
        return sessionSet.size();
    }

    private void addSystemProperty(String propertyName, String value) throws Exception {
        ModelNode addSystemProperty = Operations.createAddOperation(
                new ModelNode().add("system-property", propertyName));
        addSystemProperty.get("value").set(value);
        executeOperation(addSystemProperty);
    }

    private void removeSystemProperty(String propertyName) throws Exception {
        ModelNode removeSystemProperty = Operations.createRemoveOperation(
                new ModelNode().add("system-property", propertyName));
        executeOperation(removeSystemProperty);
    }

    private void sendAndReceiveMessage() throws Exception {
        final Properties env = new Properties();
        env.put(Context.INITIAL_CONTEXT_FACTORY, WildFlyInitialContextFactory.class.getName());
        env.put(Context.PROVIDER_URL, "remote+http://" + TestSuiteEnvironment.getServerAddress() + ":" + TestSuiteEnvironment.getHttpPort());
        Context jndiContext = null;
        JMSContext jmsContext = null;
        try {
            jndiContext = new InitialContext(env);
            ConnectionFactory connectionFactory = (ConnectionFactory) jndiContext.lookup("jms/RemoteConnectionFactory");
            jmsContext = connectionFactory.createContext("guest", "guest");
            Queue queue = jmsContext.createQueue(QUEUE_NAME);
            Destination replyTo = jmsContext.createTemporaryQueue();
            JMSConsumer consumer = jmsContext.createConsumer(replyTo);
            String text = UUID.randomUUID().toString();
            jmsContext.createProducer()
                    .setJMSReplyTo(replyTo)
                    .send(queue, text);
            String reply = consumer.receiveBody(String.class, TimeoutUtil.adjust(5000));
            assertNotNull(reply);
            assertEquals(text, reply);
        } finally {
            if (jndiContext != null) {
                jndiContext.close();
            }
            if (jmsContext != null) {
                jmsContext.close();
            }
        }
    }
}
