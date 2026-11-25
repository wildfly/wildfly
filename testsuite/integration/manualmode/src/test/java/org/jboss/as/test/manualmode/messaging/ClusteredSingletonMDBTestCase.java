/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.manualmode.messaging;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import javax.naming.InitialContext;
import javax.naming.NamingException;

import org.jboss.arquillian.container.test.api.Deployer;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.container.test.api.TargetsContainer;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.as.controller.client.ModelControllerClient;
import org.jboss.as.controller.client.helpers.Operations;
import org.jboss.as.test.integration.common.jms.JMSOperations;
import org.jboss.as.test.integration.common.jms.JMSOperationsProvider;
import org.jboss.as.test.manualmode.messaging.deployment.ClusteredSingletonQueueMDB;
import org.jboss.as.test.manualmode.messaging.ha.AbstractMessagingHATestCase;
import org.jboss.as.test.shared.TimeoutUtil;
import org.jboss.dmr.ModelNode;
import org.jboss.logging.Logger;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Tests that a clustered singleton MDB is active on exactly one cluster node at a time,
 * and that when the active node stops, another node takes over message delivery.
 * <p>
 * The test uses two standalone-full-ha.xml servers configured as an Artemis messaging
 * cluster with a delivery group. It verifies both the {@code delivery-active} management
 * attribute and actual message consumption.
 */
@RunWith(Arquillian.class)
@RunAsClient
public class ClusteredSingletonMDBTestCase extends AbstractMessagingHATestCase {

    private static final Logger log = Logger.getLogger(ClusteredSingletonMDBTestCase.class);

    static final String MDB_DEPLOYMENT_SERVER1 = "clustered-singleton-mdb-server1";
    static final String MDB_DEPLOYMENT_SERVER2 = "clustered-singleton-mdb-server2";
    static final String MDB_JAR_NAME = "clustered-singleton-mdb.jar";
    // Must match the name= attribute on @MessageDriven in ClusteredSingletonQueueMDB
    static final String MDB_NAME = "ClusteredSingletonQueueMDB";

    // Must match <d:group> in jboss-ejb3.xml and the @MessageDriven deliveryGroup attribute
    static final String DELIVERY_GROUP = "my-mdb-delivery-group";
    static final String QUEUE_NAME = "ClusteredSingletonMDBQueue";
    // Must match the destinationLookup activationConfig property in ClusteredSingletonQueueMDB
    static final String QUEUE_JNDI_LOCAL = "java:jboss/jms/queue/" + QUEUE_NAME;
    // Exported JNDI name registered so the remote test client can reach the queue
    static final String QUEUE_JNDI_EXPORTED = "java:jboss/exported/jms/queue/" + QUEUE_NAME;
    // Remote JNDI lookup name: strip java:jboss/exported/ prefix
    static final String QUEUE_JNDI_REMOTE = "jms/queue/" + QUEUE_NAME;

    @ArquillianResource
    private Deployer deployer;

    @Deployment(name = MDB_DEPLOYMENT_SERVER1, managed = false, testable = false)
    @TargetsContainer(SERVER1)
    public static JavaArchive createMDBDeploymentForServer1() {
        return createMDBJar();
    }

    @Deployment(name = MDB_DEPLOYMENT_SERVER2, managed = false, testable = false)
    @TargetsContainer(SERVER2)
    public static JavaArchive createMDBDeploymentForServer2() {
        return createMDBJar();
    }

    private static JavaArchive createMDBJar() {
        return ShrinkWrap.create(JavaArchive.class, MDB_JAR_NAME)
                .addClass(ClusteredSingletonQueueMDB.class)
                .addAsManifestResource(ClusteredSingletonMDBTestCase.class.getPackage(), "jboss-ejb3.xml", "jboss-ejb3.xml");
    }

    @Override
    protected void setUpServer1(ModelControllerClient client) throws Exception {
        configureServer(client);
    }

    @Override
    protected void setUpServer2(ModelControllerClient client) throws Exception {
        configureServer(client);
    }

    @Test
    public void testClusteredSingletonMDBFailoverOnShutdown() throws Exception {
        deployer.deploy(MDB_DEPLOYMENT_SERVER1);
        deployer.deploy(MDB_DEPLOYMENT_SERVER2);
        try {
            boolean[] active;
            try (ModelControllerClient client1 = createClient1();
                 ModelControllerClient client2 = createClient2()) {
                active = waitForSingletonElection(client1, client2);
                log.infof("After deployment: server1 delivery-active=%b, server2 delivery-active=%b", active[0], active[1]);
                assertTrue("Singleton election timed out: neither node became delivery-active within timeout",
                        active[0] || active[1]);
                assertFalse("Singleton constraint violated: both nodes are simultaneously delivery-active",
                        active[0] && active[1]);
            }

            // Verify the active singleton node actually processes a message
            InitialContext activeCtx = active[0] ? createJNDIContextFromServer1() : createJNDIContextFromServer2();
            try {
                sendMessage(activeCtx, QUEUE_JNDI_REMOTE, "singleton-delivery-test");
            } finally {
                activeCtx.close();
            }
            try (ModelControllerClient activeClient = active[0] ? createClient1() : createClient2()) {
                assertTrue("Active singleton MDB must consume the sent message",
                        waitForQueueEmpty(activeClient, TimeoutUtil.adjust(10000)));
            }

            performFailoverAndVerify(active[0]);
        } finally {
            try { deployer.undeploy(MDB_DEPLOYMENT_SERVER2); } catch (Exception ignored) {}
            try { deployer.undeploy(MDB_DEPLOYMENT_SERVER1); } catch (Exception ignored) {}
        }
    }

    @Test
    public void testClusteredSingletonMDBWithInactiveDeliveryGroup() throws Exception {
        try (ModelControllerClient client1 = createClient1();
             ModelControllerClient client2 = createClient2()) {
            setDeliveryGroupActive(client1, false);
            setDeliveryGroupActive(client2, false);
        }

        deployer.deploy(MDB_DEPLOYMENT_SERVER1);
        deployer.deploy(MDB_DEPLOYMENT_SERVER2);
        try {
            try (ModelControllerClient client1 = createClient1();
                 ModelControllerClient client2 = createClient2()) {
                assertTrue("Neither node should be delivery-active when the delivery group is inactive",
                        waitForBothInactive(client1, client2, ACTIVATION_TIMEOUT));

                // Send a message while no MDB is active — it must stay on the queue
                InitialContext ctx = createJNDIContextFromServer1();
                try {
                    sendMessage(ctx, QUEUE_JNDI_REMOTE, "queued-while-inactive");
                } finally {
                    ctx.close();
                }
                try (ModelControllerClient queueClient = createClient1()) {
                    assertFalse("Message should not be consumed when delivery group is inactive",
                            waitForQueueEmpty(queueClient, TimeoutUtil.adjust(3000)));
                }

                // Activate the delivery group on server1 only
                setDeliveryGroupActive(client1, true);
                assertTrue("Server1 should become delivery-active after its delivery group is activated",
                        waitForDeliveryActive(client1, true, ACTIVATION_TIMEOUT));
                assertTrue("Server2 must remain delivery-inactive (singleton active on server1)",
                        waitForDeliveryInactive(client2, TimeoutUtil.adjust(5000)));

                // The previously queued message must now be consumed by server1's MDB
                try (ModelControllerClient queueClient = createClient1()) {
                    assertTrue("Server1 MDB must consume the previously queued message",
                            waitForQueueEmpty(queueClient, TimeoutUtil.adjust(10000)));
                }
            }
        } finally {
            try { deployer.undeploy(MDB_DEPLOYMENT_SERVER2); } catch (Exception ignored) {}
            try { deployer.undeploy(MDB_DEPLOYMENT_SERVER1); } catch (Exception ignored) {}
        }
    }

    // Stops the singleton-master container and verifies that the surviving node takes over delivery.
    private void performFailoverAndVerify(boolean server1WasMaster) throws Exception {
        String stoppingServer = server1WasMaster ? SERVER1 : SERVER2;
        String survivorLabel = server1WasMaster ? "Server2" : "Server1";
        log.infof("%s is the singleton master, stopping it", stoppingServer);
        container.stop(stoppingServer);
        try (ModelControllerClient survivorClient = server1WasMaster ? createClient2() : createClient1()) {
            assertTrue(survivorLabel + " must become delivery-active after " + stoppingServer + " stops",
                    waitForDeliveryActive(survivorClient, true, ACTIVATION_TIMEOUT));
            InitialContext survivorCtx = createJNDIContextForSurvivor(server1WasMaster);
            try {
                sendMessage(survivorCtx, QUEUE_JNDI_REMOTE, "post-failover-test");
            } finally {
                survivorCtx.close();
            }
            assertTrue(survivorLabel + " MDB must consume messages after becoming singleton master",
                    waitForQueueEmpty(survivorClient, TimeoutUtil.adjust(10000)));
        }
    }

    private static InitialContext createJNDIContextForSurvivor(boolean server1WasMaster) throws NamingException {
        return server1WasMaster ? createJNDIContextFromServer2() : createJNDIContextFromServer1();
    }

    private void configureServer(ModelControllerClient client) throws Exception {
        execute(client, Operations.createWriteAttributeOperation(
                new ModelNode().add("subsystem", "messaging-activemq").add("server", "default"),
                "cluster-password", "password"));

        ModelNode addDeliveryGroup = Operations.createAddOperation(
                new ModelNode().add("subsystem", "ejb3").add("mdb-delivery-group", DELIVERY_GROUP));
        addDeliveryGroup.get("active").set(true);
        execute(client, addDeliveryGroup);

        JMSOperations jmsOps = JMSOperationsProvider.getInstance(client);
        try {
            ModelNode queueAttrs = new ModelNode();
            queueAttrs.get("entries").add(QUEUE_JNDI_LOCAL);
            jmsOps.createJmsQueue(QUEUE_NAME, QUEUE_JNDI_EXPORTED, queueAttrs);
        } finally {
            jmsOps.close();
        }
    }

    private void setDeliveryGroupActive(ModelControllerClient client, boolean active) throws Exception {
        execute(client, Operations.createWriteAttributeOperation(
                new ModelNode().add("subsystem", "ejb3").add("mdb-delivery-group", DELIVERY_GROUP),
                "active", active));
    }

    private boolean isMDBDeliveryActive(ModelControllerClient client) throws Exception {
        ModelNode op = Operations.createReadAttributeOperation(
                new ModelNode().add("deployment", MDB_JAR_NAME)
                               .add("subsystem", "ejb3")
                               .add("message-driven-bean", MDB_NAME),
                "delivery-active");
        return execute(client, op).asBoolean();
    }

    private boolean[] waitForSingletonElection(ModelControllerClient client1, ModelControllerClient client2)
            throws Exception {
        long start = System.currentTimeMillis();
        do {
            try {
                boolean s1 = isMDBDeliveryActive(client1);
                boolean s2 = isMDBDeliveryActive(client2);
                 if ((s1 ^ s2) || (s1 && s2 && isMDBDeliveryActive(client1))) {
                    return new boolean[]{s1, s2};
                }
            } catch (Exception ignored) {
                // transient failures during cluster formation
            }
            Thread.sleep(TimeoutUtil.adjust(1000));
        } while (System.currentTimeMillis() - start < ACTIVATION_TIMEOUT);
        return new boolean[]{tryGetDeliveryActive(client1), tryGetDeliveryActive(client2)};
    }

    private boolean waitForBothInactive(ModelControllerClient client1, ModelControllerClient client2,
            long timeoutMs) throws Exception {
        long start = System.currentTimeMillis();
        do {
            try {
                if (!isMDBDeliveryActive(client1) && !isMDBDeliveryActive(client2)) {
                    return true;
                }
            } catch (Exception ignored) {
                // transient failures during cluster formation or MDB deployment
            }
            Thread.sleep(TimeoutUtil.adjust(1000));
        } while (System.currentTimeMillis() - start < timeoutMs);
        return false;
    }

    private boolean waitForDeliveryActive(ModelControllerClient client, boolean expected, long timeoutMs)
            throws Exception {
        long start = System.currentTimeMillis();
        do {
            try {
                if (isMDBDeliveryActive(client) == expected) {
                    return true;
                }
            } catch (Exception ignored) {
                // transient failures during cluster formation or MDB deployment
            }
            Thread.sleep(1000);
        } while (System.currentTimeMillis() - start < timeoutMs);
        return tryGetDeliveryActive(client) == expected;
    }

    private boolean waitForDeliveryInactive(ModelControllerClient client, long timeoutMs) throws Exception {
        return waitForDeliveryActive(client, false, timeoutMs);
    }

    private boolean tryGetDeliveryActive(ModelControllerClient client) {
        try {
            return isMDBDeliveryActive(client);
        } catch (Exception e) {
            log.warnf(e, "Failed to read delivery-active attribute");
            return false;
        }
    }

    private boolean waitForQueueEmpty(ModelControllerClient client, long timeoutMs) throws Exception {
        ModelNode addr = new ModelNode()
                .add("subsystem", "messaging-activemq")
                .add("server", "default")
                .add("jms-queue", QUEUE_NAME);
        long start = System.currentTimeMillis();
        do {
            try {
                if (execute(client, Operations.createReadAttributeOperation(addr, "message-count")).asLong() == 0) {
                    return true;
                }
            } catch (Exception ignored) {
                // queue not yet visible
            }
            Thread.sleep(500);
        } while (System.currentTimeMillis() - start < timeoutMs);
        return false;
    }
}
