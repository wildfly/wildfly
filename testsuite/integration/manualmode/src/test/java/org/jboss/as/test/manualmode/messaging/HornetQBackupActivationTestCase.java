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
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OUTCOME;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.READ_RESOURCE_OPERATION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.RESULT;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SUCCESS;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.net.InetAddress;

import org.jboss.arquillian.container.test.api.ContainerController;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.as.arquillian.container.Authentication;
import org.jboss.as.controller.client.ModelControllerClient;
import org.jboss.as.test.shared.TestSuiteEnvironment;
import org.jboss.as.test.shared.TimeoutUtil;
import org.jboss.dmr.ModelNode;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 *
 * @author <a href="http://jmesnil.net/">Jeff Mesnil</a> (c) 2013 Red Hat, inc.
 */
@RunWith(Arquillian.class)
@RunAsClient
public class HornetQBackupActivationTestCase {

    // maximum time for HornetQ activation to detect node failover/failback
    private static int ACTIVATION_TIMEOUT = 10000;

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
        liveClient = TestSuiteEnvironment.getModelControllerClient();
        backupClient = ModelControllerClient.Factory.create(InetAddress.getByName(TestSuiteEnvironment.getServerAddress()),
                TestSuiteEnvironment.getServerPort() + 100,
                Authentication.getCallbackHandler());
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

        // shutdown live server
        container.stop(LIVE_SERVER);
        // let some time for the backup to detect the failure
        waitForHornetQServerActivation(backupClient, true, TimeoutUtil.adjust(ACTIVATION_TIMEOUT));

        checkHornetQServerStartedAndActiveAttributes(backupClient, true, true);
        checkQueue(backupClient, queueName, true);
        checkJMSQueue(backupClient, jmsQueueName, true);
        checkJMSTopic(backupClient, jmsTopicName, true);
        checkConnectionFactory(backupClient, true);

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
            ModelNode result = execute(client, operation);
            boolean started = result.get(RESULT, "started").asBoolean();
            boolean active = result.get(RESULT, "active").asBoolean();
            if (started && expectedActive == active) {
                return;
            }
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
            }
            now = System.currentTimeMillis();
        } while (now - start < timeout);

        fail("Server did not become active in the imparted time.");

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
        System.out.println(operation.toJSONString(false) + "\n=>\n" + result.toJSONString(false));
        assertEquals(result.toJSONString(true), SUCCESS, result.get(OUTCOME).asString());
        return result;
    }

    private static void executeWithFailure(ModelControllerClient client, ModelNode operation) throws IOException {
        ModelNode result = client.execute(operation);
        System.out.println(operation.toJSONString(false) + "\n=>\n" + result.toJSONString(false));
        assertEquals(result.toJSONString(true), FAILED, result.get(OUTCOME).asString());
        assertTrue(result.toJSONString(true), result.get(FAILURE_DESCRIPTION).asString().contains("JBAS011678"));
        assertFalse(result.has(RESULT));
    }

}