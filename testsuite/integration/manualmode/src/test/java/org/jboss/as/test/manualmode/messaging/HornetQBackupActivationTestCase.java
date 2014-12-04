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
import org.jboss.as.test.integration.common.jms.DefaultHornetQProviderJMSOperations;
import org.jboss.as.test.integration.common.jms.JMSOperations;
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

    private JMSOperations liveJMSOperations;
    private JMSOperations backupJMSOperations;

    @Before
    public void initServer() throws Exception {
        container.start(LIVE_SERVER);
        container.start(BACKUP_SERVER);
        liveJMSOperations = new DefaultHornetQProviderJMSOperations(createLiveClient());
        backupJMSOperations = new DefaultHornetQProviderJMSOperations(createBackupClient());
    }

    @After
    public void closeServer() throws Exception {
        if (container.isStarted(BACKUP_SERVER)) {
            container.stop(BACKUP_SERVER);
        }
        if (container.isStarted(LIVE_SERVER)) {
            container.stop(LIVE_SERVER);
        }
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
        checkHornetQServerStartedAndActiveAttributes(liveJMSOperations, true, true);
        checkHornetQServerStartedAndActiveAttributes(backupJMSOperations, true, false);

        final String queueName = randomUUID().toString();
        addQueue(backupJMSOperations, queueName);
        final String jmsQueueName = randomUUID().toString();
        addJMSQueue(backupJMSOperations, jmsQueueName);
        final String jmsTopicName = randomUUID().toString();
        addJMSTopic(backupJMSOperations, jmsTopicName);

        checkQueue(backupJMSOperations, queueName, false);
        checkJMSQueue(backupJMSOperations, jmsQueueName, false);
        checkJMSTopic(backupJMSOperations, jmsTopicName, false);
        checkConnectionFactory(backupJMSOperations, false);

        System.out.println("===================");
        System.out.println("STOP LIVE SERVER...");
        System.out.println("===================");
        // shutdown live server
        container.stop(LIVE_SERVER);
        // let some time for the backup to detect the failure
        waitForHornetQServerActivation(backupJMSOperations, true, TimeoutUtil.adjust(ACTIVATION_TIMEOUT));

        checkHornetQServerStartedAndActiveAttributes(backupJMSOperations, true, true);
        checkQueue(backupJMSOperations, queueName, true);
        checkJMSQueue(backupJMSOperations, jmsQueueName, true);
        checkJMSTopic(backupJMSOperations, jmsTopicName, true);
        checkConnectionFactory(backupJMSOperations, true);

        System.out.println("====================");
        System.out.println("START LIVE SERVER...");
        System.out.println("====================");
        // restart the live server
        container.start(LIVE_SERVER);
        // let some time for the backup to detect the live node and failback
        waitForHornetQServerActivation(liveJMSOperations, true, TimeoutUtil.adjust(ACTIVATION_TIMEOUT));
        checkHornetQServerStartedAndActiveAttributes(liveJMSOperations, true, true);

        // let some time for the backup to detect the live node and failback
        waitForHornetQServerActivation(backupJMSOperations, false, TimeoutUtil.adjust(ACTIVATION_TIMEOUT));
        // backup server has been restarted in passive mode
        checkHornetQServerStartedAndActiveAttributes(backupJMSOperations, true, false);

        checkQueue(backupJMSOperations, queueName, false);
        checkJMSQueue(backupJMSOperations, jmsQueueName, false);
        checkJMSTopic(backupJMSOperations, jmsTopicName, false);
        checkConnectionFactory(backupJMSOperations, false);

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
        checkHornetQServerStartedAndActiveAttributes(liveJMSOperations, true, true);
        checkHornetQServerStartedAndActiveAttributes(backupJMSOperations, true, false);

        final String queueName = randomUUID().toString();
        addQueue(backupJMSOperations, queueName);
        final String jmsQueueName = randomUUID().toString();
        addJMSQueue(backupJMSOperations, jmsQueueName);
        final String jmsTopicName = randomUUID().toString();
        addJMSTopic(backupJMSOperations, jmsTopicName);

        checkQueue(backupJMSOperations, queueName, false);
        checkJMSQueue(backupJMSOperations, jmsQueueName, false);
        checkJMSTopic(backupJMSOperations, jmsTopicName, false);
        checkConnectionFactory(backupJMSOperations, false);

        System.out.println("===================");
        System.out.println("STOP LIVE SERVER...");
        System.out.println("===================");
        // shutdown live server
        container.stop(LIVE_SERVER);

        // let some time for the backup to detect the failure
        waitForHornetQServerActivation(backupJMSOperations, true, TimeoutUtil.adjust(ACTIVATION_TIMEOUT));
        checkHornetQServerStartedAndActiveAttributes(backupJMSOperations, true, true);

        System.out.println("====================");
        System.out.println("START LIVE SERVER...");
        System.out.println("====================");
        // restart the live server
        container.start(LIVE_SERVER);

        // let some time for the backup to detect the live node and failback
        waitForHornetQServerActivation(liveJMSOperations, true, TimeoutUtil.adjust(ACTIVATION_TIMEOUT));
        checkHornetQServerStartedAndActiveAttributes(liveJMSOperations, true, true);
        waitForHornetQServerActivation(backupJMSOperations, false, TimeoutUtil.adjust(ACTIVATION_TIMEOUT));
        checkHornetQServerStartedAndActiveAttributes(backupJMSOperations, true, false);

        System.out.println("==============================");
        System.out.println("STOP LIVE SERVER A 2ND TIME...");
        System.out.println("==============================");
        // shutdown live servera 2nd time
        container.stop(LIVE_SERVER);

        // let some time for the backup to detect the failure
        waitForHornetQServerActivation(backupJMSOperations, true, TimeoutUtil.adjust(ACTIVATION_TIMEOUT));
        checkHornetQServerStartedAndActiveAttributes(backupJMSOperations, true, true);
    }

    // https://issues.jboss.org/browse/AS7-6881
    @Test
    public void testPassiveBackupReload() throws Exception {
        checkHornetQServerStartedAndActiveAttributes(liveJMSOperations, true, true);
        checkHornetQServerStartedAndActiveAttributes(backupJMSOperations, true, false);

        reload(backupJMSOperations);
        // let some time for the server to reload
        waitForBackupServerToReload(TimeoutUtil.adjust(RELOAD_TIMEOUT));
        waitForHornetQServerActivation(backupJMSOperations, false, TimeoutUtil.adjust(ACTIVATION_TIMEOUT));

        checkHornetQServerStartedAndActiveAttributes(liveJMSOperations, true, true);
        checkHornetQServerStartedAndActiveAttributes(backupJMSOperations, true, false);
    }

    @Test
    public void testActiveBackupReload() throws Exception {
        checkHornetQServerStartedAndActiveAttributes(liveJMSOperations, true, true);
        checkHornetQServerStartedAndActiveAttributes(backupJMSOperations, true, false);

        // shutdown live server
        container.stop(LIVE_SERVER);
        // let some time for the backup to detect the failure
        waitForHornetQServerActivation(backupJMSOperations, true, TimeoutUtil.adjust(ACTIVATION_TIMEOUT));
        checkHornetQServerStartedAndActiveAttributes(backupJMSOperations, true, true);

        reload(backupJMSOperations);
        // let some time for the server to reload
        waitForBackupServerToReload(TimeoutUtil.adjust(RELOAD_TIMEOUT));
        waitForHornetQServerActivation(backupJMSOperations, false, TimeoutUtil.adjust(ACTIVATION_TIMEOUT));

        // !! reloading an active backup server will make it passive again !!
        checkHornetQServerStartedAndActiveAttributes(backupJMSOperations, true, false);
    }

    // https://issues.jboss.org/browse/AS7-6881
    @Test
    public void testLiveReload() throws Exception {
        checkHornetQServerStartedAndActiveAttributes(liveJMSOperations, true, true);
        checkHornetQServerStartedAndActiveAttributes(backupJMSOperations, true, false);

        reload(liveJMSOperations);
        // let some time for the server to reload
        waitForLiveServerToReload(TimeoutUtil.adjust(RELOAD_TIMEOUT));
        waitForHornetQServerActivation(liveJMSOperations, true, TimeoutUtil.adjust(ACTIVATION_TIMEOUT));
        // let some time for the backup server to failback
        waitForHornetQServerActivation(backupJMSOperations, false, TimeoutUtil.adjust(ACTIVATION_TIMEOUT));

        checkHornetQServerStartedAndActiveAttributes(liveJMSOperations, true, true);
        checkHornetQServerStartedAndActiveAttributes(backupJMSOperations, true, false);
    }

    private void reload(JMSOperations operations) throws IOException {
        ModelNode operation = new ModelNode();
        operation.get(OP_ADDR).setEmptyList();
        operation.get(OP).set("reload");
        try {
            execute(operations.getControllerClient(), operation);
        } catch(IOException e) {
            final Throwable cause = e.getCause();
            if (!(cause instanceof ExecutionException) && !(cause instanceof CancellationException)) {
                throw e;
            } // else ignore, this might happen if the channel gets closed before we got the response
        }
    }

    private void waitForHornetQServerActivation(JMSOperations operations, boolean expectedActive, int timeout) throws IOException {
        long start = System.currentTimeMillis();
        long now;
        do {
            ModelNode operation = new ModelNode();
            operation.get(OP_ADDR).set(operations.getServerAddress());
            operation.get(OP).set(READ_RESOURCE_OPERATION);
            operation.get(INCLUDE_RUNTIME).set(true);
            try {
                ModelNode result = execute(operations.getControllerClient(), operation);
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
            backupJMSOperations.getControllerClient().close();

            ModelControllerClient backupClient = createBackupClient();
            ModelNode operation = new ModelNode();
            operation.get(OP_ADDR).setEmptyList();
            operation.get(OP).set(READ_ATTRIBUTE_OPERATION);
            operation.get(NAME).set("server-state");
            try {
                ModelNode result = execute(backupClient, operation);
                boolean normal = "running".equals(result.get(RESULT).asString());
                if (normal) {
                    backupJMSOperations = new DefaultHornetQProviderJMSOperations(backupClient);
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
            liveJMSOperations.getControllerClient().close();

            ModelControllerClient liveClient = createLiveClient();
            ModelNode operation = new ModelNode();
            operation.get(OP_ADDR).setEmptyList();
            operation.get(OP).set(READ_ATTRIBUTE_OPERATION);
            operation.get(NAME).set("server-state");
            try {
                ModelNode result = execute(liveClient, operation);
                boolean normal = "running".equals(result.get(RESULT).asString());
                if (normal) {
                    liveJMSOperations = new DefaultHornetQProviderJMSOperations(liveClient);
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

    private void addQueue(JMSOperations operations, String queueName) throws IOException {
        operations.addCoreQueue(queueName, queueName, false);
    }

    private void addJMSQueue(JMSOperations operations, String jmsQueueName) throws IOException {
        operations.createJmsQueue(jmsQueueName, "java:jboss/exported/jms/" + jmsQueueName);
    }

    private void addJMSTopic(JMSOperations operations, String jmsTopicName) throws IOException {
        operations.createJmsTopic(jmsTopicName, "java:jboss/exported/jms/" + jmsTopicName);
    }

    private void checkConnectionFactory(JMSOperations operations, boolean active) throws Exception {
        ModelNode operation = new ModelNode();
        ModelNode address = operations.getServerAddress().add("connection-factory", "RemoteConnectionFactory");
        operation.get(OP_ADDR).set(address);
        operation.get(OP).set(READ_RESOURCE_OPERATION);
        operation.get(INCLUDE_RUNTIME).set(true);
        ModelNode result = execute(operations.getControllerClient(), operation);
        // initial-message-packet-size is a runtime attribute. if the server is passive, it returns undefined
        assertEquals(result.toJSONString(true), active, result.get(RESULT, "initial-message-packet-size").isDefined());

        // runtime operation
        operation.get(OP).set("add-jndi");
        operation.get("jndi-binding").set("java:jboss/exported/jms/" + randomUUID().toString());
        if (active) {
            execute(operations.getControllerClient(), operation);
        } else {
            executeWithFailure(operations.getControllerClient(), operation);
        }
    }

    private void checkQueue(JMSOperations operations, String queueName, boolean active) throws Exception {
        ModelNode address = operations.getServerAddress().add("queue", queueName);
        checkQueue0(operations.getControllerClient(), address, "id", active);
    }

    private void checkJMSQueue(JMSOperations operations, String jmsQueueName, boolean active) throws Exception {
        ModelNode address = operations.getServerAddress().add("jms-queue", jmsQueueName);
        checkQueue0(operations.getControllerClient(), address, "queue-address", active);
    }

    private void checkQueue0(ModelControllerClient client, ModelNode address, String runtimeAttributeName, boolean active) throws Exception {
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

    private void checkJMSTopic(JMSOperations operations, String jmsTopicName, boolean active) throws Exception {
        ModelNode operation = new ModelNode();
        ModelNode address = operations.getServerAddress().add("jms-topic", jmsTopicName);
        operation.get(OP_ADDR).set(address);
        operation.get(OP).set(READ_RESOURCE_OPERATION);
        operation.get(INCLUDE_RUNTIME).set(true);
        ModelNode result = execute(operations.getControllerClient(), operation);
        assertEquals(result.toJSONString(true), active, result.get(RESULT, "topic-address").isDefined());

        // runtime operation
        operation.get(OP).set("list-all-subscriptions");
        if (active) {
            execute(operations.getControllerClient(), operation);
        } else {
            executeWithFailure(operations.getControllerClient(), operation);
        }
    }

    public void checkHornetQServerStartedAndActiveAttributes(JMSOperations operations, boolean expectedStarted, boolean  expectedActive) throws IOException {
        ModelNode operation = new ModelNode();
        ModelNode address = operations.getServerAddress();
        operation.get(OP_ADDR).set(address);
        operation.get(OP).set(READ_RESOURCE_OPERATION);
        operation.get(INCLUDE_RUNTIME).set(true);
        System.out.println("operation = " + operation);
        ModelNode result = execute(operations.getControllerClient(), operation);
        assertEquals(expectedStarted, result.get(RESULT, "started").asBoolean());
        assertEquals(expectedActive, result.get(RESULT, "active").asBoolean());
    }

    private ModelNode execute(ModelControllerClient client, ModelNode operation) throws IOException {
        System.out.println("operation = " + operation);
        ModelNode result = client.execute(operation);
        return result;
    }

    private void executeWithFailure(ModelControllerClient client, ModelNode operation) throws IOException {
        ModelNode result = client.execute(operation);
        assertEquals(result.toJSONString(true), FAILED, result.get(OUTCOME).asString());
        assertTrue(result.toJSONString(true), result.get(FAILURE_DESCRIPTION).asString().contains("WFLYMSG0066"));
        assertFalse(result.has(RESULT));
    }

}
