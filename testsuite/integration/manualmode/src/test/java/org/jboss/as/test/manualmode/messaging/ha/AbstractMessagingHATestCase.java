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

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.FAILED;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.FAILURE_DESCRIPTION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OUTCOME;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.READ_RESOURCE_OPERATION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.RESULT;
import static org.jboss.as.test.shared.ServerReload.executeReloadAndWaitForCompletion;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.File;
import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Properties;
import java.util.UUID;

import javax.jms.ConnectionFactory;
import javax.jms.Destination;
import javax.jms.JMSConsumer;
import javax.jms.JMSContext;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;

import org.jboss.arquillian.container.test.api.ContainerController;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.as.controller.client.ModelControllerClient;
import org.jboss.as.controller.client.helpers.Operations;
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
    private static final Logger log = Logger.getLogger(AbstractMessagingHATestCase.class);
    private static final int OFFSET = 100;

    // maximum time for HornetQ activation to detect node failover/failback
    protected static int ACTIVATION_TIMEOUT = TimeoutUtil.adjust(30000);

    private String snapshotForServer1;
    private String snapshotForServer2;

    @ArquillianResource
    protected static ContainerController container;

    protected static ModelControllerClient createClient1() {
        return TestSuiteEnvironment.getModelControllerClient();
    }

    protected static ModelControllerClient createClient2() throws UnknownHostException {
        return ModelControllerClient.Factory.create(InetAddress.getByName(TestSuiteEnvironment.getServerAddressNode1()),
                TestSuiteEnvironment.getServerPort() + OFFSET,
                Authentication.getCallbackHandler());
    }

    private static String takeSnapshot(ModelControllerClient client) throws Exception {
        ModelNode operation = new ModelNode();
        operation.get(OP).set("take-snapshot");
        ModelNode result = execute(client, operation);
        return result.asString();
    }

    protected static void waitForHornetQServerActivation(JMSOperations operations, boolean expectedActive) throws IOException {
        long start = System.currentTimeMillis();
        long now;
        do {
            try {
                boolean started = isHornetQServerStarted(operations);
                boolean active = isHornetQServerActive(operations);
                if (started && expectedActive == active) {
                    // leave some time to the hornetq children resources to be installed after the server is activated
                    Thread.sleep(TimeoutUtil.adjust(500));

                    return;
                }
            } catch (Exception e) {
            }
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
            }
            now = System.currentTimeMillis();
        } while (now - start < ACTIVATION_TIMEOUT);

        fail("Server did not become active in the imparted time.");
    }

    protected static boolean isHornetQServerStarted(JMSOperations operations) throws Exception {
        ModelNode operation = Operations.createReadAttributeOperation(operations.getServerAddress(), "started");
        return execute(operations.getControllerClient(), operation).asBoolean();
    }

    protected static boolean isHornetQServerActive(JMSOperations operations) throws Exception {
        ModelNode operation = Operations.createReadAttributeOperation(operations.getServerAddress(), "active");
        return execute(operations.getControllerClient(), operation).asBoolean();
    }

    protected static void checkHornetQServerStartedAndActiveAttributes(JMSOperations operations, boolean expectedStarted, boolean expectedActive) throws Exception {
        assertEquals(expectedStarted, isHornetQServerStarted(operations));
        assertEquals(expectedActive, isHornetQServerActive(operations));
    }

    protected static InitialContext createJNDIContextFromServer1() throws NamingException {
        final Properties env = new Properties();
        env.put(Context.INITIAL_CONTEXT_FACTORY, "org.jboss.naming.remote.client.InitialContextFactory");
        String ipAdddress = TestSuiteEnvironment.getServerAddress("node0");
        env.put(Context.PROVIDER_URL, System.getProperty(Context.PROVIDER_URL, "remote+http://" + ipAdddress + ":" + TestSuiteEnvironment.getHttpPort()));
        env.put(Context.SECURITY_PRINCIPAL, "guest");
        env.put(Context.SECURITY_CREDENTIALS, "guest");
        return new InitialContext(env);
    }

    protected static InitialContext createJNDIContextFromServer2() throws NamingException {
        final Properties env = new Properties();
        env.put(Context.INITIAL_CONTEXT_FACTORY, "org.jboss.naming.remote.client.InitialContextFactory");
        String ipAdddress = TestSuiteEnvironment.getServerAddressNode1();
        env.put(Context.PROVIDER_URL, System.getProperty(Context.PROVIDER_URL, "remote+http://" + ipAdddress + ":" + (TestSuiteEnvironment.getHttpPort() + OFFSET)));
        env.put(Context.SECURITY_PRINCIPAL, "guest");
        env.put(Context.SECURITY_CREDENTIALS, "guest");
        return new InitialContext(env);
    }

    protected static void sendMessage(Context ctx, String destinationLookup, String text) throws NamingException {
        log.trace("Looking up for the RemoteConnectionFactory with " + ctx);
        ConnectionFactory cf = (ConnectionFactory) ctx.lookup("jms/RemoteConnectionFactory");
        assertNotNull(cf);
        log.trace("Looking up for the destination with " + ctx);
        Destination destination = (Destination) ctx.lookup(destinationLookup);
        assertNotNull(destination);

        try ( JMSContext context = cf.createContext("guest", "guest")) {
            context.createProducer().send(destination, text);
        }
    }

    protected static void receiveMessage(Context ctx, String destinationLookup, String expectedText) throws NamingException {
        log.trace("Looking up for the RemoteConnectionFactory with " + ctx);
        ConnectionFactory cf = (ConnectionFactory) ctx.lookup("jms/RemoteConnectionFactory");
        assertNotNull(cf);
        log.trace("Looking up for the destination with " + ctx);
        Destination destination = (Destination) ctx.lookup(destinationLookup);
        assertNotNull(destination);

        try ( JMSContext context = cf.createContext("guest", "guest");
              JMSConsumer consumer = context.createConsumer(destination)) {
            String text = consumer.receiveBody(String.class, TimeoutUtil.adjust(5000));
            assertNotNull(text);
            assertEquals(expectedText, text);
        }
    }

    protected static void sendAndReceiveMessage(Context ctx, String destinationLookup) throws NamingException {
        String text = UUID.randomUUID().toString();
        sendMessage(ctx, destinationLookup, text);
        receiveMessage(ctx, destinationLookup, text);
    }

    protected static void receiveNoMessage(Context ctx, String destinationLookup) throws NamingException {
        ConnectionFactory cf = (ConnectionFactory) ctx.lookup("jms/RemoteConnectionFactory");
        assertNotNull(cf);
        Destination destination = (Destination) ctx.lookup(destinationLookup);
        assertNotNull(destination);

        try ( JMSContext context = cf.createContext("guest", "guest");
              JMSConsumer consumer = context.createConsumer(destination)) {
            String text = consumer.receiveBody(String.class, TimeoutUtil.adjust(5000));
            assertNull(text);
        }

    }

    protected static void checkJMSQueue(JMSOperations operations, String jmsQueueName, boolean active) throws Exception {
        ModelNode address = operations.getServerAddress().add("jms-queue", jmsQueueName);
        checkQueue0(operations.getControllerClient(), address, "queue-address", active);
    }

    protected static void checkQueue0(ModelControllerClient client, ModelNode address, String runtimeAttributeName, boolean active) throws Exception {
        ModelNode operation = new ModelNode();
        operation.get(OP_ADDR).set(address);
        operation.get(OP).set(READ_RESOURCE_OPERATION);

        // runtime operation
        operation.get(OP).set("list-messages");
        if (active) {
            execute(client, operation);
        } else {
            executeWithFailure(client, operation);
        }
    }

    private void restoreSnapshot(String snapshot) throws IOException {
        Path snapshotFile = new File(snapshot).toPath();
        Path standaloneConfiguration = snapshotFile.getParent().getParent().getParent().resolve("standalone-full-ha.xml");
        Files.move(snapshotFile, standaloneConfiguration, StandardCopyOption.REPLACE_EXISTING);
    }

    protected static ModelNode execute(ModelControllerClient client, ModelNode operation) throws Exception {
        ModelNode response = client.execute(operation);
        boolean success = Operations.isSuccessfulOutcome(response);
        if (success) {
            return response.get(RESULT);
        }
        throw new Exception("Operation failed " + Operations.getFailureDescription(response));
    }

    protected static void executeWithFailure(ModelControllerClient client, ModelNode operation) throws IOException {
        ModelNode result = client.execute(operation);
        assertEquals(result.toJSONString(true), FAILED, result.get(OUTCOME).asString());
        assertTrue(result.toJSONString(true), result.get(FAILURE_DESCRIPTION).asString().contains("WFLYMSGAMQ0066"));
        assertFalse(result.has(RESULT));
    }

    @Before
    public void setUp() throws Exception {
        clearArtemisFiles();
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
            setUpServer1(client1);
            setUpServer2(client2);
        } catch (Exception e) {
            tearDown();
            throw e;
        }

        // reload server1 in normal mode
        executeReloadAndWaitForCompletionOfServer1(client1, false);
        client1 = createClient1();

        // reload server2  in normal mode
        executeReloadAndWaitForCompletionOfServer2(client2, false);
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
        if (container.isStarted(SERVER1)) {
            container.stop(SERVER1);
        }
        restoreSnapshot(snapshotForServer1);
        if (container.isStarted(SERVER2)) {
            container.stop(SERVER2);
        }
        restoreSnapshot(snapshotForServer2);
        clearArtemisFiles();
    }

    private void executeReloadAndWaitForCompletionOfServer1(ModelControllerClient initialClient, boolean adminOnly) throws Exception {
        executeReloadAndWaitForCompletion(initialClient, ServerReload.TIMEOUT,
                adminOnly,
                TestSuiteEnvironment.getServerAddress(),
                TestSuiteEnvironment.getServerPort());
    }

    private void executeReloadAndWaitForCompletionOfServer2(ModelControllerClient initialClient, boolean adminOnly) throws Exception {
        executeReloadAndWaitForCompletion(initialClient, ServerReload.TIMEOUT,
                adminOnly,
                TestSuiteEnvironment.getServerAddressNode1(),
                TestSuiteEnvironment.getServerPort() + OFFSET);
    }

    protected static void clearArtemisFiles() {
        File server1 = new File(SERVER1).toPath().resolve("standalone").resolve("data").resolve("activemq").toFile();
        deleteRecursive(server1);
        File server2 = new File(SERVER2).toPath().resolve("standalone").resolve("data").resolve("activemq").toFile();
        deleteRecursive(server2);
    }

    protected static void deleteRecursive(File file) {
        File[] files = file.listFiles();
        if(files != null) {
            File[] var2 = files;
            int var3 = files.length;

            for(int var4 = 0; var4 < var3; ++var4) {
                File f = var2[var4];
                deleteRecursive(f);
            }
        }
        file.delete();
    }
}
