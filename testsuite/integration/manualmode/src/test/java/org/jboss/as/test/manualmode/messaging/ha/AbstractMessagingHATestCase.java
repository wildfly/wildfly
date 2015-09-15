/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2015, Red Hat, Inc., and individual contributors
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

package org.jboss.as.test.manualmode.messaging.ha;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ADD;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.FAILED;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.FAILURE_DESCRIPTION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.INCLUDE_RUNTIME;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OUTCOME;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.READ_RESOURCE_OPERATION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.RECURSIVE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.RESULT;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SUCCESS;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.WRITE_ATTRIBUTE_OPERATION;
import static org.jboss.as.test.shared.ServerReload.executeReloadAndWaitForCompletion;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.File;
import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Properties;
import java.util.UUID;

import javax.jms.ConnectionFactory;
import javax.jms.Destination;
import javax.jms.JMSConsumer;
import javax.jms.JMSContext;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;

import org.apache.activemq.artemis.api.config.ActiveMQDefaultConfiguration;
import org.jboss.arquillian.container.test.api.ContainerController;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.as.controller.client.ModelControllerClient;
import org.jboss.as.test.integration.common.jms.JMSOperations;
import org.jboss.as.test.shared.ServerReload;
import org.jboss.as.test.shared.TestSuiteEnvironment;
import org.jboss.as.test.shared.TimeoutUtil;
import org.jboss.dmr.ModelNode;
import org.jboss.logging.Logger;
import org.junit.After;
import org.junit.Before;
import org.junit.runner.RunWith;
import org.wildfly.test.api.Authentication;

/**
 * @author <a href="http://jmesnil.net/">Jeff Mesnil</a> (c) 2015 Red Hat inc.
 */
@RunWith(Arquillian.class)
@RunAsClient
public abstract class AbstractMessagingHATestCase {

    public static final String SERVER1 = "jbossas-messaging-ha-server1";
    public static final String SERVER2 = "jbossas-messaging-ha-server2";

    // maximum time for HornetQ activation to detect node failover/failback
    protected static int ACTIVATION_TIMEOUT = 15000;
    // leave some time before restarting a server to have other nodes expire its information
    protected static long EXPIRATION_TIME = ActiveMQDefaultConfiguration.getDefaultBroadcastRefreshTimeout() + 2000;

    protected static final Logger logger = Logger.getLogger(AbstractMessagingHATestCase.class);

    private String snapshotForServer1;
    private String snapshotForServer2;

    @ArquillianResource
    protected static ContainerController container;

    protected static ModelControllerClient createClient1() {
        return TestSuiteEnvironment.getModelControllerClient();
    }

    protected static ModelControllerClient createClient2() throws UnknownHostException {
        return ModelControllerClient.Factory.create(InetAddress.getByName(TestSuiteEnvironment.getServerAddress()),
                TestSuiteEnvironment.getServerPort() + 100,
                Authentication.getCallbackHandler());
    }

    private static String takeSnapshot(ModelControllerClient client) throws Exception {
        ModelNode operation = new ModelNode();
        operation.get(OP).set("take-snapshot");
        ModelNode result = execute(client, operation);
        String snapshot = result.asString();
        return snapshot;
    }

    protected static void waitForHornetQServerActivation(JMSOperations operations, boolean expectedActive) throws IOException {
        long start = System.currentTimeMillis();
        long now;
        do {
            ModelNode operation = new ModelNode();
            operation.get(OP_ADDR).set(operations.getServerAddress());
            operation.get(OP).set(READ_RESOURCE_OPERATION);
            operation.get(INCLUDE_RUNTIME).set(true);
            operation.get(RECURSIVE).set(false);
            try {
                ModelNode result = execute(operations.getControllerClient(), operation);
                boolean started = result.get("started").asBoolean();
                boolean active = result.get("active").asBoolean();
                logger.info("active=" + active + ", started=" + started);
                if (started && expectedActive == active) {
                    // leave some time to the hornetq children resources to be installed after the server is activated
                    Thread.sleep(TimeoutUtil.adjust(500));

                    return;
                }
            } catch (Exception e) {
            }
            try {
                Thread.sleep(ACTIVATION_TIMEOUT / 3);
            } catch (InterruptedException e) {
            }
            now = System.currentTimeMillis();
        } while (now - start < ACTIVATION_TIMEOUT);

        fail(expectedActive ? "Server did not become active in the imparted time." : "Server remained active after the imparted time");
    }

    protected static void checkHornetQServerStartedAndActiveAttributes(JMSOperations operations, boolean expectedStarted, boolean expectedActive) throws Exception {
        ModelNode operation = new ModelNode();
        ModelNode address = operations.getServerAddress();
        operation.get(OP_ADDR).set(address);
        operation.get(OP).set(READ_RESOURCE_OPERATION);
        operation.get(INCLUDE_RUNTIME).set(true);
        operation.get(RECURSIVE).set(false);
        ModelNode result = execute(operations.getControllerClient(), operation);
        assertEquals(expectedStarted, result.get("started").asBoolean());
        assertEquals(expectedActive, result.get("active").asBoolean());
    }

    protected static InitialContext createJNDIContextFromServer1() throws NamingException {
        String address = TestSuiteEnvironment.formatPossibleIpv6Address(TestSuiteEnvironment.getServerAddress());

        final Properties env = new Properties();
        env.put(Context.INITIAL_CONTEXT_FACTORY, "org.jboss.naming.remote.client.InitialContextFactory");
        env.put(Context.PROVIDER_URL, System.getProperty(Context.PROVIDER_URL, "http-remoting://" + address + ":8080"));
        env.put(Context.SECURITY_PRINCIPAL, "guest");
        env.put(Context.SECURITY_CREDENTIALS, "guest");
        return new InitialContext(env);
    }

    protected static  InitialContext createJNDIContextFromServer2() throws NamingException {
        String address = TestSuiteEnvironment.formatPossibleIpv6Address(TestSuiteEnvironment.getServerAddress());

        final Properties env = new Properties();
        env.put(Context.INITIAL_CONTEXT_FACTORY, "org.jboss.naming.remote.client.InitialContextFactory");
        env.put(Context.PROVIDER_URL, System.getProperty(Context.PROVIDER_URL, "http-remoting://" + address + ":8180"));
        env.put(Context.SECURITY_PRINCIPAL, "guest");
        env.put(Context.SECURITY_CREDENTIALS, "guest");
        return new InitialContext(env);
    }

    protected static  void sendMessage(Context ctx, String destinationLookup, String text) throws NamingException {
        ConnectionFactory cf = (ConnectionFactory) ctx.lookup("jms/RemoteConnectionFactory");
        assertNotNull(cf);
        Destination destination = (Destination) ctx.lookup(destinationLookup);
        assertNotNull(destination);

        try (JMSContext context = cf.createContext("guest", "guest")) {
            context.createProducer().send(destination, text);
        }
    }

    protected static  void receiveMessage(Context ctx, String destinationLookup, String expectedText) throws NamingException {
        ConnectionFactory cf = (ConnectionFactory) ctx.lookup("jms/RemoteConnectionFactory");
        assertNotNull(cf);
        Destination destination = (Destination) ctx.lookup(destinationLookup);
        assertNotNull(destination);

        try (JMSContext context = cf.createContext("guest", "guest")) {
            JMSConsumer consumer = context.createConsumer(destination);
            String text = consumer.receiveBody(String.class, 5000);
            assertNotNull(text);
            assertEquals(expectedText, text);
        }
    }

    protected static  void sendAndReceiveMessage(Context ctx, String destinationLookup) throws NamingException {
        String text = "message sent and received from the same server " + UUID.randomUUID().toString();
        sendMessage(ctx, destinationLookup, text);
        receiveMessage(ctx, destinationLookup, text);
    }

    protected static void checkJMSQueue(JMSOperations operations, String jmsQueueName, boolean active) throws Exception {
        ModelNode address = operations.getServerAddress().add("jms-queue", jmsQueueName);
        checkQueue0(operations.getControllerClient(), address, "queue-address", active);
    }

    protected static void checkQueue0(ModelControllerClient client, ModelNode address, String runtimeAttributeName, boolean active) throws Exception {
        ModelNode operation = new ModelNode();
        operation.get(OP_ADDR).set(address);
        operation.get(OP).set(READ_RESOURCE_OPERATION);
        operation.get(INCLUDE_RUNTIME).set(true);
       // ModelNode result = execute(client, operation);
       // System.out.println(runtimeAttributeName + " = " + result.get(runtimeAttributeName));
        //assertEquals(result.toJSONString(true), active, result.get(runtimeAttributeName).isDefined());

        // runtime operation
        operation.get(OP).set("list-messages");
        if (active) {
            execute(client, operation);
        } else {
            executeWithFailure(client, operation);
        }
    }

    private void restoreSnapshot(String snapshot) {
        logger.info("snapshot = " + snapshot);
        File snapshotFile = new File(snapshot);
        File configurationDir = snapshotFile.getParentFile().getParentFile().getParentFile();
        logger.info("configurationDir = " + configurationDir);
        File standaloneConfiguration = new File(configurationDir, "standalone-full-ha.xml");
        snapshotFile.renameTo(standaloneConfiguration);
    }

    protected static ModelNode execute(ModelControllerClient client, ModelNode operation) throws Exception {
        return execute(client, operation, false);
    }

    protected static ModelNode execute(ModelControllerClient client, ModelNode operation, boolean log) throws Exception {
        if (log) {
            logger.info("operation = " + operation);
        }
        ModelNode response = client.execute(operation);
        if (log) {
            logger.info("response = " + response);
        }
        boolean success = SUCCESS.equals(response.get(OUTCOME).asString());
        if (success) {
            return response.get(RESULT);
        }
        throw new Exception("Operation failed");
    }

    protected static void executeWithFailure(ModelControllerClient client, ModelNode operation) throws IOException {
        ModelNode result = client.execute(operation);
        assertEquals(result.toJSONString(true), FAILED, result.get(OUTCOME).asString());
        assertTrue(result.toJSONString(true), result.get(FAILURE_DESCRIPTION).asString().contains("WFLYMSGAMQ0066"));
        assertFalse(result.has(RESULT));
    }

    @Before
    public void setUp() throws Exception {

        // start server1 and reload it in admin-only
        container.start(SERVER1);
        ModelControllerClient client1 = createClient1();
        snapshotForServer1 = takeSnapshot(client1);
        executeReloadAndWaitForCompletionOfServer1(client1, true);
        client1 = createClient1();

        // start server2 and reload it in admin-only
        container.start(SERVER2);
        ModelControllerClient client2 = createClient2();
        snapshotForServer2 = takeSnapshot(client2);
        executeReloadAndWaitForCompletionOfServer2(client2, true);
        client2 = createClient2();

        // setup both servers
        try {
            info("Setup Server #1");
            setUpServer1(client1);
            info("Setup Server #2");
            setUpServer2(client2);
        } catch (Exception e) {
            tearDown();
            throw e;
        }

        // reload server1 in normal mode
        info("Reload Server #1...");
        executeReloadAndWaitForCompletionOfServer1(client1, false);
        info("Server #1 reloaded");
        client1 = createClient1();

        // reload server2  in normal mode
        info("Reload Server #2...");
        executeReloadAndWaitForCompletionOfServer2(client2, false);
        info("Server #2 reloaded");
        client2 = createClient2();

        // both servers are started and configured
        assertTrue(container.isStarted(SERVER1));
        client1.close();
        assertTrue(container.isStarted(SERVER2));
        client2.close();
    }

    protected abstract void setUpServer1(ModelControllerClient client) throws Exception;
    protected abstract void setUpServer2(ModelControllerClient client) throws Exception;

    @After
    public void tearDown() throws Exception {
        try {
            if (container.isStarted(SERVER2)) {
                container.stop(SERVER2);
            }
        } finally {
            restoreSnapshot(snapshotForServer2);
        }

        try {
            if (container.isStarted(SERVER1)) {
                container.stop(SERVER1);
            }
        } finally {
            restoreSnapshot(snapshotForServer1);
        }
    }

    private void executeReloadAndWaitForCompletionOfServer1(ModelControllerClient initialClient, boolean adminOnly) throws Exception {
        executeReloadAndWaitForCompletion(initialClient, adminOnly);
        initialClient.close();
    }

    private void executeReloadAndWaitForCompletionOfServer2(ModelControllerClient initialClient, boolean adminOnly) throws Exception {
        executeReloadAndWaitForCompletion(initialClient, ServerReload.TIMEOUT,
                adminOnly,
                TestSuiteEnvironment.getServerAddress(),
                TestSuiteEnvironment.getServerPort() + 100);
        initialClient.close();
    }

    protected void addDebug(ModelControllerClient client, String server) throws Exception {
        // /subsystem=logging/pattern-formatter=COLOR-PATTERN:write-attribute(name=pattern, value="<server> %d{yyyy-MM-dd HH:mm:ss,SSS} %-5p [%c] (%t) %s%e%n")
        // /subsystem=logging/console-handler=DEBUGGER:add(level=DEBUG, named-formatter=COLOR-PATTERN)

        ModelNode configureFormatterOp = new ModelNode();
        configureFormatterOp.get(OP_ADDR).add("subsystem", "logging");
        configureFormatterOp.get(OP_ADDR).add("pattern-formatter", "COLOR-PATTERN");
        configureFormatterOp.get(OP).set(WRITE_ATTRIBUTE_OPERATION);
        configureFormatterOp.get("name").set("pattern");
        configureFormatterOp.get("value").set("[" + server + "] %d{yyyy-MM-dd HH:mm:ss,SSS} %-5p [%c] (%t) %s%e%n");
        execute(client, configureFormatterOp, true);

        ModelNode addHandlerOp = new ModelNode();
        addHandlerOp.get(OP_ADDR).add("subsystem", "logging");
        addHandlerOp.get(OP_ADDR).add("console-handler", "DEBUGGER");
        addHandlerOp.get(OP).set(ADD);
        addHandlerOp.get("level").set("DEBUG");
        addHandlerOp.get("named-formatter").set("COLOR-PATTERN");
        execute(client, addHandlerOp, true);

        ModelNode addLoggerOp = new ModelNode();
        addLoggerOp.get(OP_ADDR).add("subsystem", "logging");
        addLoggerOp.get(OP_ADDR).add("logger", "org.apache.activemq.artemis");
        addLoggerOp.get(OP).set(ADD);
        addLoggerOp.get("level").set("DEBUG");
        addLoggerOp.get("handlers").add("DEBUGGER");
        execute(client, addLoggerOp, true);
    }

    protected void info(String message) {
        logger.info("\n\n===================\n" + message + "\n===================\n\n");
    }
}
