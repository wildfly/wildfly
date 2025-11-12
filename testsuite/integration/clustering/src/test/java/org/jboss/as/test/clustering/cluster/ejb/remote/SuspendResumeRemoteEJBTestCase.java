/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.clustering.cluster.ejb.remote;

import org.apache.commons.lang3.RandomUtils;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.TargetsContainer;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.junit.InSequence;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.as.arquillian.container.ManagementClient;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.operations.common.Util;
import org.jboss.as.test.clustering.cluster.AbstractClusteringTestCase;
import org.jboss.as.test.clustering.cluster.ejb.remote.bean.Heartbeat;
import org.jboss.as.test.clustering.cluster.ejb.remote.bean.HeartbeatBean;
import org.jboss.as.test.clustering.cluster.ejb.remote.bean.Result;
import org.jboss.as.test.clustering.ejb.EJBDirectory;
import org.jboss.as.test.clustering.ejb.RemoteEJBDirectory;
import org.jboss.as.test.shared.PermissionUtils;
import org.jboss.dmr.ModelNode;
import org.jboss.logging.Logger;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.Map;
import java.util.PropertyPermission;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SUCCESS;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OUTCOME;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.RESULT;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.NAME;

/**
 * Test for WFLY-13871.
 * <p>
 * This test will set up an EJB client interacting with a cluster of two nodes and verify that when
 * one of the nodes is suspended, invocations no longer are sent to the suspended node; and that when the node is resumed,
 * invocations return to the resumed node.
 *
 * @author Richard Achmatowicz
 */
@RunWith(Arquillian.class)
public class SuspendResumeRemoteEJBTestCase extends AbstractClusteringTestCase {

    static final Logger LOGGER = Logger.getLogger(SuspendResumeRemoteEJBTestCase.class);
    private static final String MODULE_NAME = SuspendResumeRemoteEJBTestCase.class.getSimpleName();

    @Deployment(name = DEPLOYMENT_1, managed = false, testable = false)
    @TargetsContainer(NODE_1)
    public static Archive<?> createDeploymentForContainer1() {
        return createDeployment();
    }

    @Deployment(name = DEPLOYMENT_2, managed = false, testable = false)
    @TargetsContainer(NODE_2)
    public static Archive<?> createDeploymentForContainer2() {
        return createDeployment();
    }

    private static Archive<?> createDeployment() {
        return ShrinkWrap.create(JavaArchive.class, MODULE_NAME + ".jar")
                .addPackage(EJBDirectory.class.getPackage())
                .addClasses(Result.class, Heartbeat.class, HeartbeatBean.class, RandomUtils.class)
                .addAsManifestResource(PermissionUtils.createPermissionsXmlAsset(new PropertyPermission(NODE_NAME_PROPERTY, "read")), "permissions.xml")
                ;
    }

    @ArquillianResource
    @TargetsContainer(NODE_1)
    private ManagementClient client1;

    @ArquillianResource
    @TargetsContainer(NODE_2)
    private ManagementClient client2;

    private final Map<String, ManagementClient> clients = new TreeMap<>();

    static final int INVOCATION_LOOP_TIMES = 10;
    static final int SUSPEND_RESUME_LOOP_TIMES = 10;

    // time between invocations
    static final long INV_WAIT_DURATION_MSECS = 10;
    // time that the server remains suspended (or resumed) during continuous invocations
    static final long SUSPEND_RESUME_DURATION_MSECS = 1000;

    // the set of nodes available, according to suspend/resume
    private final Set<String> nodesAvailable = new TreeSet<>(NODE_1_2);

    @Before
    public void initialiseNodesAvailable() {
        this.clients.put(NODE_1, this.client1);
        this.clients.put(NODE_2, this.client2);
    }

    @After
    public void resumeAll() {
        for (String node : NODE_1_2) {
            if (!this.nodesAvailable.contains(node)) {
                try {
                    this.resumeServer(node);
                } catch (IOException e) {
                    LOGGER.warn(e.getLocalizedMessage(), e);
                }
            }
        }
    }

    /**
     * This test checks that suspending and then resuming the server during invocation results in correct behaviour
     * in the case that the proxy is created before the server is suspended.
     * <p>
     * The test assertion is checked after each invocation result, and verifies that no invocation is sent to a suspended node.
     */
    @Test
    @InSequence(1)
    public void testSuspendResumeAfterProxyInit() {
        LOGGER.info("testSuspendResumeAfterProxyInit() - start");
        try (EJBDirectory directory = new RemoteEJBDirectory(MODULE_NAME)) {
            Heartbeat bean = directory.lookupStateless(HeartbeatBean.class, Heartbeat.class);

            for (int i = 1; i < INVOCATION_LOOP_TIMES; i++) {
                performInvocation(bean);
            }

            suspendTheServer(NODE_1);

            for (int i = 1; i < INVOCATION_LOOP_TIMES; i++) {
                performInvocation(bean);
            }

            resumeTheServer(NODE_1);

            for (int i = 1; i < INVOCATION_LOOP_TIMES; i++) {
                performInvocation(bean);
            }
        } catch (Exception e) {
            LOGGER.info("Caught exception! e = " + e.getMessage());
            Assert.fail("Test failed with exception: e = " + e.getMessage());
        } finally {
            LOGGER.info("testSuspendResumeAfterProxyInit() - end");
        }
    }

    /**
     * This test checks that suspending and then resuming the server during invocation results in correct behaviour
     * in the case that the proxy is created after the server is suspended.
     * <p>
     * The test assertion is checked after each invocation result, and verifies that no invocation is sent to a suspended node.
     */
    @Test
    @InSequence(2)
    public void testSuspendResumeBeforeProxyInit() {
        LOGGER.info("testSuspendResumeBeforeProxyInit() - start");
        try (EJBDirectory directory = new RemoteEJBDirectory(MODULE_NAME)) {

            suspendTheServer(NODE_1);

            Heartbeat bean = directory.lookupStateless(HeartbeatBean.class, Heartbeat.class);

            for (int i = 1; i < INVOCATION_LOOP_TIMES; i++) {
                performInvocation(bean);
            }

            resumeTheServer(NODE_1);

            for (int i = 1; i < INVOCATION_LOOP_TIMES; i++) {
                performInvocation(bean);
            }
        } catch (Exception e) {
            LOGGER.info("Caught exception! e = " + e.getMessage());
            Assert.fail("Test failed with exception: e = " + e.getMessage());
        } finally {
            LOGGER.info("testSuspendResumeBeforeProxyInit() - end");
        }
    }

    /**
     * This test checks that suspending and then resuming the server during invocation results in correct behaviour
     * in the case that the proxy is created after the server is suspended.
     * <p>
     * The test assertion is checked after each invocation result, and verifies that no invocation is sent to a suspended node.
     */
    @Test
    @InSequence(3)
    public void testSuspendResumeContinuous() {
        LOGGER.info("testSuspendResumeContinuous() - start");
        try (EJBDirectory directory = new RemoteEJBDirectory(MODULE_NAME)) {

            Heartbeat bean = directory.lookupStateless(HeartbeatBean.class, Heartbeat.class);
            ContinuousInvoker continuousInvoker = new ContinuousInvoker(bean);

            Thread thread = new Thread(continuousInvoker);
            LOGGER.info("Starting the invoker ...");
            thread.start();
            try {
                for (int i = 1; i < SUSPEND_RESUME_LOOP_TIMES ; i++) {
                    // suspend and then resume each server in turn while invocations happen
                    Thread.sleep(SUSPEND_RESUME_DURATION_MSECS);

                    suspendTheServer(NODE_1);

                    Thread.sleep(SUSPEND_RESUME_DURATION_MSECS);

                    resumeTheServer(NODE_1);

                    // suspend and then resume each server in turn while invocations happen
                    Thread.sleep(SUSPEND_RESUME_DURATION_MSECS);

                    suspendTheServer(NODE_2);

                    Thread.sleep(SUSPEND_RESUME_DURATION_MSECS);

                    resumeTheServer(NODE_2);

                    Thread.sleep(SUSPEND_RESUME_DURATION_MSECS);
                }
            } finally {
                thread.interrupt();
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } catch (Exception e) {
            LOGGER.info("Caught exception! e = " + e.getMessage());
            Assert.fail("Test failed with exception: e = " + e.getMessage());
        } finally {
            LOGGER.info("testSuspendResumeContinuous() - end");
        }
    }


    /**
     * Helper class that performs invocations on a bean once per second until stopInvoking() is called.
     */
    private class ContinuousInvoker implements Runnable {
        private final Heartbeat bean;

        public ContinuousInvoker(Heartbeat bean) {
            this.bean = bean;
        }

        @Override
        public void run() {
            try {
                while (!Thread.currentThread().isInterrupted()) {
                    performInvocation(this.bean);
                }
            } catch (Throwable e) {
                LOGGER.info("ContinuousInvoker: caught exception while performing invocation: e = " + e.getMessage());
            }
        }
    }

    private void performInvocation(Heartbeat bean) {
        Result<Date> result = bean.pulse();

        LOGGER.info("invoked pulse(), result: node = " + result.getNode() + ", value = " + result.getValue());
        Assert.assertTrue(this.nodesAvailable.contains(result.getNode()));

        try {
            Thread.sleep(INV_WAIT_DURATION_MSECS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }


    // helper methods to determine server state

    private void suspendTheServer(String node) throws IOException {
        LOGGER.info("=================== SUSPEND  " + node + " ===========================");
        if (suspendServer(node)) {
            this.nodesAvailable.remove(node);
        } else {
            throw new IllegalStateException("Failed to suspend server");
        }
    }

    private void resumeTheServer(String node) throws IOException {
        LOGGER.info("=================== RESUME  " + node + " ===========================");
        if (resumeServer(node)) {
            this.nodesAvailable.add(node);
        } else {
            throw new IllegalStateException("Failed to resume server");
        }
    }

    /**
     * Suspend a server by calling the management operation ":suspend"
     *
     * @param node the node to be suspended
     * @return true if the operation succeeded
     */
    private boolean suspendServer(String node) throws IOException {
        return this.suspendResume(node, Util.createOperation("suspend", PathAddress.EMPTY_ADDRESS), "SUSPENDED");
    }

    /**
     * Resume a server by calling the management operation ":suspend"
     *
     * @param node the node to be suspended
     * @return true if the operation succeeded
     */
    private boolean resumeServer(String node) throws IOException {
        return this.suspendResume(node, Util.createOperation("resume", PathAddress.EMPTY_ADDRESS), "RUNNING");
    }

    private boolean suspendResume(String node, ModelNode operation, String targetState) throws IOException {
        ModelNode result = this.clients.get(node).getControllerClient().execute(operation);
        Assert.assertEquals(SUCCESS, result.get(OUTCOME).asString());

        // Do not return until suspend state reaches target value, or timeout
        ModelNode queryOperation = Util.createOperation("read-attribute", PathAddress.EMPTY_ADDRESS);
        queryOperation.get(NAME).set("suspend-state");
        Instant start = Instant.now();
        Duration maxDuration = Duration.ofSeconds(15);
        String state = this.getSuspendState(node);
        Instant now = Instant.now();
        try {
            while (!targetState.equals(state) && Duration.between(start, now).compareTo(maxDuration) < 0) {
                Thread.sleep(10);
                state = this.getSuspendState(node);
                now = Instant.now();
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        LOGGER.infof("Target state = %s, actual state = %s, duration = %s", targetState, state, Duration.between(start, now));
        return targetState.equals(state);
    }

    /**
     * Check if a server is suspended by reading the server attribute "suspend-state"
     *
     * @param node the node to be checked
     * @return server suspend state
     */
    private String getSuspendState(String node) throws IOException {
        ModelNode op = Util.createOperation("read-attribute", PathAddress.EMPTY_ADDRESS);
        op.get(NAME).set("suspend-state");
        ModelNode result = this.clients.get(node).getControllerClient().execute(op);
        Assert.assertEquals(SUCCESS, result.get(OUTCOME).asString());
        return result.get(RESULT).asString();
    }
}
