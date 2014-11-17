/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2013, Red Hat, Inc., and individual contributors
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

import static java.util.UUID.randomUUID;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ADD;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.FAILED;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.FAILURE_DESCRIPTION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.INCLUDE_RUNTIME;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.NAME;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OUTCOME;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.READ_ATTRIBUTE_OPERATION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.READ_RESOURCE_OPERATION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.RESULT;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;

import org.jboss.arquillian.container.test.api.ContainerController;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.as.controller.client.ModelControllerClient;
import org.jboss.as.test.shared.TestSuiteEnvironment;
import org.jboss.as.test.shared.TimeoutUtil;
import org.jboss.dmr.ModelNode;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.wildfly.test.api.Authentication;

/**
 *
 * @author <a href="http://jmesnil.net/">Jeff Mesnil</a> (c) 2013 Red Hat, inc.
 */
@RunWith(Arquillian.class)
@RunAsClient
public class HornetQBackupActivationTestCase {

    // maximum time for HornetQ activation to detect node failover/failback
    private static int ACTIVATION_TIMEOUT = 10000;
    // maximum time to reload a server
    private static int RELOAD_TIMEOUT = 10000;

    public static final String LIVE_SERVER = "jbossas-messaging-live";
    public static final String BACKUP_SERVER = "jbossas-messaging-backup";

    @ArquillianResource
    private static ContainerController container;

    private ModelControllerClient liveClient;

    private ModelControllerClient backupClient;

    @Before
    public void initServer() throws Exception {
        container.start(LIVE_SERVER);
        container.start(BACKUP_SERVER);
        liveClient = createLiveClient();
        backupClient = createBackupClient();
    }

    @After
    public void closeServer() throws Exception {
        if (container.isStarted(BACKUP_SERVER)) {
            container.stop(BACKUP_SERVER);
        }
        if (container.isStarted(LIVE_SERVER)) {
            container.stop(LIVE_SERVER);
        }
        liveClient = null;
        backupClient = null;
    }

    private static ModelControllerClient createLiveClient() {
        return TestSuiteEnvironment.getModelControllerClient();
    }

    private static ModelControllerClient createBackupClient() throws UnknownHostException {
        return ModelControllerClient.Factory.create(InetAddress.getByName(TestSuiteEnvironment.getServerAddress()),
                TestSuiteEnvironment.getServerPort() + 100,
                Authentication.getCallbackHandler());
    }

    @Test
    public void testBackupActivation() throws Exception {
        checkHornetQServerStartedAndActiveAttributes(liveClient, true, true);
        checkHornetQServerStartedAndActiveAttributes(backupClient, true, false);

        final String queueName = randomUUID().toString();
        addQueue(backupClient, queueName);
        final String jmsQueueName = randomUUID().toString();
        addJMSQueue(backupClient, jmsQueueName);
        final String jmsTopicName = randomUUID().toString();
        addJMSTopic(backupClient, jmsTopicName);

        checkQueue(backupClient, queueName, false);
        checkJMSQueue(backupClient, jmsQueueName, false);
        checkJMSTopic(backupClient, jmsTopicName, false);
        checkConnectionFactory(backupClient, false);

        System.out.println("===================");
        System.out.println("STOP LIVE SERVER...");
        System.out.println("===================");
        // shutdown live server
        container.stop(LIVE_SERVER);
        // let some time for the backup to detect the failure
        waitForHornetQServerActivation(backupClient, true, TimeoutUtil.adjust(ACTIVATION_TIMEOUT));

        checkHornetQServerStartedAndActiveAttributes(backupClient, true, true);
        checkQueue(backupClient, queueName, true);
        checkJMSQueue(backupClient, jmsQueueName, true);
        checkJMSTopic(backupClient, jmsTopicName, true);
        checkConnectionFactory(backupClient, true);

        System.out.println("====================");
        System.out.println("START LIVE SERVER...");
        System.out.println("====================");
        // restart the live server
        container.start(LIVE_SERVER);
        // let some time for the backup to detect the live node and failback
        waitForHornetQServerActivation(liveClient, true, TimeoutUtil.adjust(ACTIVATION_TIMEOUT));
        checkHornetQServerStartedAndActiveAttributes(liveClient, true, true);

        // let some time for the backup to detect the live node and failback
        waitForHornetQServerActivation(backupClient, false, TimeoutUtil.adjust(ACTIVATION_TIMEOUT));
        // backup server has been restarted in passive mode
        checkHornetQServerStartedAndActiveAttributes(backupClient, true, false);

        checkQueue(backupClient, queueName, false);
        checkJMSQueue(backupClient, jmsQueueName, false);
        checkJMSTopic(backupClient, jmsTopicName, false);
        checkConnectionFactory(backupClient, false);

        System.out.println("=============================");
        System.out.println("RETURN TO NORMAL OPERATION...");
        System.out.println("=============================");

        // https://issues.jboss.org/browse/WFLY-1710
        // set the boolean to true to verify that there are no
        // XA recovery warnings anymore
        if (false) {
            Thread.sleep(36000);

            System.out.println("=============================");
            System.out.println("DONE...");
            System.out.println("=============================");
        }
    }

    // https://issues.jboss.org/browse/AS7-6840
    @Test
    public void testBackupFailoverAfterFailback() throws Exception {
        checkHornetQServerStartedAndActiveAttributes(liveClient, true, true);
        checkHornetQServerStartedAndActiveAttributes(backupClient, true, false);

        final String queueName = randomUUID().toString();
        addQueue(backupClient, queueName);
        final String jmsQueueName = randomUUID().toString();
        addJMSQueue(backupClient, jmsQueueName);
        final String jmsTopicName = randomUUID().toString();
        addJMSTopic(backupClient, jmsTopicName);

        checkQueue(backupClient, queueName, false);
        checkJMSQueue(backupClient, jmsQueueName, false);
        checkJMSTopic(backupClient, jmsTopicName, false);
        checkConnectionFactory(backupClient, false);

        System.out.println("===================");
        System.out.println("STOP LIVE SERVER...");
        System.out.println("===================");
        // shutdown live server
        container.stop(LIVE_SERVER);

        // let some time for the backup to detect the failure
        waitForHornetQServerActivation(backupClient, true, TimeoutUtil.adjust(ACTIVATION_TIMEOUT));
        checkHornetQServerStartedAndActiveAttributes(backupClient, true, true);

        System.out.println("====================");
        System.out.println("START LIVE SERVER...");
        System.out.println("====================");
        // restart the live server
        container.start(LIVE_SERVER);

        // let some time for the backup to detect the live node and failback
        waitForHornetQServerActivation(liveClient, true, TimeoutUtil.adjust(ACTIVATION_TIMEOUT));
        checkHornetQServerStartedAndActiveAttributes(liveClient, true, true);
        waitForHornetQServerActivation(backupClient, false, TimeoutUtil.adjust(ACTIVATION_TIMEOUT));
        checkHornetQServerStartedAndActiveAttributes(backupClient, true, false);

        System.out.println("==============================");
        System.out.println("STOP LIVE SERVER A 2ND TIME...");
        System.out.println("==============================");
        // shutdown live servera 2nd time
        container.stop(LIVE_SERVER);

        // let some time for the backup to detect the failure
        waitForHornetQServerActivation(backupClient, true, TimeoutUtil.adjust(ACTIVATION_TIMEOUT));
        checkHornetQServerStartedAndActiveAttributes(backupClient, true, true);
    }

    // https://issues.jboss.org/browse/AS7-6881
    @Test
    public void testPassiveBackupReload() throws Exception {
        checkHornetQServerStartedAndActiveAttributes(liveClient, true, true);
        checkHornetQServerStartedAndActiveAttributes(backupClient, true, false);

        reload(backupClient);
        // let some time for the server to reload
        waitForBackupServerToReload(TimeoutUtil.adjust(RELOAD_TIMEOUT));
        waitForHornetQServerActivation(backupClient, false, TimeoutUtil.adjust(ACTIVATION_TIMEOUT));

        checkHornetQServerStartedAndActiveAttributes(liveClient, true, true);
        checkHornetQServerStartedAndActiveAttributes(backupClient, true, false);
    }

    @Test
    public void testActiveBackupReload() throws Exception {
        checkHornetQServerStartedAndActiveAttributes(liveClient, true, true);
        checkHornetQServerStartedAndActiveAttributes(backupClient, true, false);

        // shutdown live server
        container.stop(LIVE_SERVER);
        // let some time for the backup to detect the failure
        waitForHornetQServerActivation(backupClient, true, TimeoutUtil.adjust(ACTIVATION_TIMEOUT));
        checkHornetQServerStartedAndActiveAttributes(backupClient, true, true);

        reload(backupClient);
        // let some time for the server to reload
        waitForBackupServerToReload(TimeoutUtil.adjust(RELOAD_TIMEOUT));
        waitForHornetQServerActivation(backupClient, false, TimeoutUtil.adjust(ACTIVATION_TIMEOUT));

        // !! reloading an active backup server will make it passive again !!
        checkHornetQServerStartedAndActiveAttributes(backupClient, true, false);
    }

    // https://issues.jboss.org/browse/AS7-6881
    @Test
    public void testLiveReload() throws Exception {
        checkHornetQServerStartedAndActiveAttributes(liveClient, true, true);
        checkHornetQServerStartedAndActiveAttributes(backupClient, true, false);

        reload(liveClient);
        // let some time for the server to reload
        waitForLiveServerToReload(TimeoutUtil.adjust(RELOAD_TIMEOUT));
        waitForHornetQServerActivation(liveClient, true, TimeoutUtil.adjust(ACTIVATION_TIMEOUT));
        // let some time for the backup server to failback
        waitForHornetQServerActivation(backupClient, false, TimeoutUtil.adjust(ACTIVATION_TIMEOUT));

        checkHornetQServerStartedAndActiveAttributes(liveClient, true, true);
        checkHornetQServerStartedAndActiveAttributes(backupClient, true, false);
    }

    private void reload(ModelControllerClient client) throws IOException {
        ModelNode operation = new ModelNode();
        operation.get(OP_ADDR).setEmptyList();
        operation.get(OP).set("reload");
        try {
            execute(client, operation);
        } catch(IOException e) {
            final Throwable cause = e.getCause();
            if (!(cause instanceof ExecutionException) && !(cause instanceof CancellationException)) {
                throw e;
            } // else ignore, this might happen if the channel gets closed before we got the response
        }
    }

    private static void waitForHornetQServerActivation(ModelControllerClient client, boolean expectedActive, int timeout) throws IOException {
        long start = System.currentTimeMillis();
        long now;
        do {
            ModelNode operation = new ModelNode();
            operation.get(OP_ADDR).add("subsystem", "messaging");
            operation.get(OP_ADDR).add("hornetq-server", "default");
            operation.get(OP).set(READ_RESOURCE_OPERATION);
            operation.get(INCLUDE_RUNTIME).set(true);
            try {
                ModelNode result = execute(client, operation);
                boolean started = result.get(RESULT, "started").asBoolean();
                boolean active = result.get(RESULT, "active").asBoolean();
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
        } while (now - start < timeout);

        fail("Server did not become active in the imparted time.");
    }

    private void waitForBackupServerToReload(int timeout) throws Exception {
        // FIXME use the CLI high-level reload operation that blocks instead of
        // fiddling with timeouts...
        // leave some time to have the server starts its reload process and change
        // its server-starte from running.
        Thread.sleep(TimeoutUtil.adjust(500));
        long start = System.currentTimeMillis();
        long now;
        do {
            backupClient.close();
            backupClient = createBackupClient();
            ModelNode operation = new ModelNode();
            operation.get(OP_ADDR).setEmptyList();
            operation.get(OP).set(READ_ATTRIBUTE_OPERATION);
            operation.get(NAME).set("server-state");
            try {
                ModelNode result = execute(backupClient, operation);
                boolean normal = "running".equals(result.get(RESULT).asString());
                if (normal) {
                    return;
                }
            } catch (Exception e) {
            }
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
            }
            now = System.currentTimeMillis();
        } while (now - start < timeout);

        fail("Backup Server did not reload in the imparted time.");
    }

    private void waitForLiveServerToReload(int timeout) throws Exception {
        // FIXME use the CLI high-level reload operation that blocks instead of
        // fiddling with timeouts...
        // leave some time to have the server starts its reload process and change
        // its server-starte from running.
        Thread.sleep(TimeoutUtil.adjust(500));
        long start = System.currentTimeMillis();
        long now;
        do {
            liveClient.close();
            liveClient = createLiveClient();
            ModelNode operation = new ModelNode();
            operation.get(OP_ADDR).setEmptyList();
            operation.get(OP).set(READ_ATTRIBUTE_OPERATION);
            operation.get(NAME).set("server-state");
            try {
                ModelNode result = execute(liveClient, operation);
                boolean normal = "running".equals(result.get(RESULT).asString());
                if (normal) {
                    return;
                }
            } catch (Exception e) {
            }
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
            }
            now = System.currentTimeMillis();
        } while (now - start < timeout);

        fail("Live Server did not reload in the imparted time.");
    }

    private static void addQueue(ModelControllerClient client, String queueName) throws IOException {
        ModelNode operation = new ModelNode();
        operation.get(OP_ADDR).add("subsystem", "messaging");
        operation.get(OP_ADDR).add("hornetq-server", "default");
        operation.get(OP_ADDR).add("queue", queueName);
        operation.get(OP).set(ADD);
        operation.get("queue-address").set(queueName);
        execute(client, operation);
    }

    private static void addJMSQueue(ModelControllerClient client, String jmsQueueName) throws IOException {
        ModelNode operation = new ModelNode();
        operation.get(OP_ADDR).add("subsystem", "messaging");
        operation.get(OP_ADDR).add("hornetq-server", "default");
        operation.get(OP_ADDR).add("jms-queue", jmsQueueName);
        operation.get(OP).set(ADD);
        operation.get("entries").setEmptyList();
        operation.get("entries").add("java:jboss/exported/jms/" + jmsQueueName);
        execute(client, operation);
    }

    private static void addJMSTopic(ModelControllerClient client, String jmsTopicName) throws IOException {
        ModelNode operation = new ModelNode();
        operation.get(OP_ADDR).add("subsystem", "messaging");
        operation.get(OP_ADDR).add("hornetq-server", "default");
        operation.get(OP_ADDR).add("jms-topic", jmsTopicName);
        operation.get(OP).set(ADD);
        operation.get("entries").setEmptyList();
        operation.get("entries").add("java:jboss/exported/jms/" + jmsTopicName);
        execute(client, operation);
    }

    private static void checkConnectionFactory(ModelControllerClient client, boolean active) throws Exception {
        ModelNode operation = new ModelNode();
        operation.get(OP_ADDR).add("subsystem", "messaging");
        operation.get(OP_ADDR).add("hornetq-server", "default");
        operation.get(OP_ADDR).add("connection-factory", "RemoteConnectionFactory");
        operation.get(OP).set(READ_RESOURCE_OPERATION);
        operation.get(INCLUDE_RUNTIME).set(true);
        ModelNode result = execute(client, operation);
        // initial-message-packet-size is a runtime attribute. if the server is passive, it returns undefined
        assertEquals(result.toJSONString(true), active, result.get(RESULT, "initial-message-packet-size").isDefined());

        // runtime operation
        operation.get(OP).set("add-jndi");
        operation.get("jndi-binding").set("java:jboss/exported/jms/" + randomUUID().toString());
        if (active) {
            execute(client, operation);
        } else {
            executeWithFailure(client, operation);
        }
    }

    private static void checkQueue(ModelControllerClient client, String queueName, boolean active) throws Exception {
        ModelNode address = new ModelNode();
        address.add("subsystem", "messaging");
        address.add("hornetq-server", "default");
        address.add("queue", queueName);
        checkQueue0(client, address, "id", active);
    }

    private static void checkJMSQueue(ModelControllerClient client, String jmsQueueName, boolean active) throws Exception {
        ModelNode address = new ModelNode();
        address.add("subsystem", "messaging");
        address.add("hornetq-server", "default");
        address.add("jms-queue", jmsQueueName);
        checkQueue0(client, address, "queue-address", active);
    }

    private static void checkQueue0(ModelControllerClient client, ModelNode address, String runtimeAttributeName, boolean active) throws Exception {
        ModelNode operation = new ModelNode();
        operation.get(OP_ADDR).set(address);
        operation.get(OP).set(READ_RESOURCE_OPERATION);
        operation.get(INCLUDE_RUNTIME).set(true);
        ModelNode result = execute(client, operation);
        assertEquals(result.toJSONString(true), active, result.get(RESULT, runtimeAttributeName).isDefined());

        // runtime operation
        operation.get(OP).set("list-messages");
        if (active) {
            execute(client, operation);
        } else {
            executeWithFailure(client, operation);
        }
    }

    private static void checkJMSTopic(ModelControllerClient client, String jmsTopicName, boolean active) throws Exception {
        ModelNode operation = new ModelNode();
        operation.get(OP_ADDR).add("subsystem", "messaging");
        operation.get(OP_ADDR).add("hornetq-server", "default");
        operation.get(OP_ADDR).add("jms-topic", jmsTopicName);
        operation.get(OP).set(READ_RESOURCE_OPERATION);
        operation.get(INCLUDE_RUNTIME).set(true);
        ModelNode result = execute(client, operation);
        assertEquals(result.toJSONString(true), active, result.get(RESULT, "topic-address").isDefined());

        // runtime operation
        operation.get(OP).set("list-all-subscriptions");
        if (active) {
            execute(client, operation);
        } else {
            executeWithFailure(client, operation);
        }
    }

    public static void checkHornetQServerStartedAndActiveAttributes(ModelControllerClient client, boolean expectedStarted, boolean  expectedActive) throws IOException {
        ModelNode operation = new ModelNode();
        operation.get(OP_ADDR).add("subsystem", "messaging");
        operation.get(OP_ADDR).add("hornetq-server", "default");
        operation.get(OP).set(READ_RESOURCE_OPERATION);
        operation.get(INCLUDE_RUNTIME).set(true);
        ModelNode result = execute(client, operation);
        assertEquals(expectedStarted, result.get(RESULT, "started").asBoolean());
        assertEquals(expectedActive, result.get(RESULT, "active").asBoolean());
    }

    private static ModelNode execute(ModelControllerClient client, ModelNode operation) throws IOException {
        ModelNode result = client.execute(operation);
        return result;
    }

    private static void executeWithFailure(ModelControllerClient client, ModelNode operation) throws IOException {
        ModelNode result = client.execute(operation);
        assertEquals(result.toJSONString(true), FAILED, result.get(OUTCOME).asString());
        assertTrue(result.toJSONString(true), result.get(FAILURE_DESCRIPTION).asString().contains("WFLYMSG0066"));
        assertFalse(result.has(RESULT));
    }

}
