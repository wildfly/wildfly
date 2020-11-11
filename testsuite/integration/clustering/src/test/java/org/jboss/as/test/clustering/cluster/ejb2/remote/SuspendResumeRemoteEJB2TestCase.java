/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2017, Red Hat, Inc., and individual contributors
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

package org.jboss.as.test.clustering.cluster.ejb2.remote;

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
import org.jboss.as.test.clustering.cluster.ejb2.remote.bean.HeartbeatBeanBase;
import org.jboss.as.test.clustering.cluster.ejb2.remote.bean.HeartbeatRemote;
import org.jboss.as.test.clustering.cluster.ejb2.remote.bean.HeartbeatRemoteHome;
import org.jboss.as.test.clustering.cluster.ejb2.remote.bean.Result;
import org.jboss.as.test.clustering.cluster.ejb2.remote.bean.SlowHeartbeatBean;
import org.jboss.as.test.clustering.ejb.EJBDirectory;
import org.jboss.as.test.clustering.ejb.RemoteEJBDirectory;
import org.jboss.as.test.shared.integration.ejb.security.PermissionUtils;
import org.jboss.dmr.ModelNode;
import org.jboss.logging.Logger;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.IOException;
import java.util.Arrays;
import java.util.Date;
import java.util.HashSet;
import java.util.PropertyPermission;
import java.util.Set;

import static org.jboss.as.controller.client.helpers.ClientConstants.CONTROLLER_PROCESS_STATE_STARTING;
import static org.jboss.as.controller.client.helpers.ClientConstants.CONTROLLER_PROCESS_STATE_STOPPING;
import static org.jboss.as.controller.client.helpers.ClientConstants.RUNNING_STATE_SUSPENDED;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.NAME;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OUTCOME;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.RESULT;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SUCCESS;

/**
 * Test for WFLY-13871.
 *
 * This test will set up an EJB client interacting with a cluster of two nodes and verify that when
 * one of the nodes is suspended, invovations no longer are sent to the suspended node; and that when the node is resumed,
 * invocations return to the resumed node.
 *
 * @author Richard Achmatowicz
 */
@RunWith(Arquillian.class)
public class SuspendResumeRemoteEJB2TestCase extends AbstractClusteringTestCase {

    static final Logger LOGGER = Logger.getLogger(SuspendResumeRemoteEJB2TestCase.class);
    private static final String MODULE_NAME = SuspendResumeRemoteEJB2TestCase.class.getSimpleName();

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
                .addClasses(Result.class, HeartbeatRemote.class, HeartbeatRemoteHome.class, SlowHeartbeatBean.class, HeartbeatBeanBase.class, RandomUtils.class)
                .addAsManifestResource(PermissionUtils.createPermissionsXmlAsset(new PropertyPermission(NODE_NAME_PROPERTY, "read")), "permissions.xml")
                ;
    }

    @ArquillianResource
    @TargetsContainer(NODE_1)
    private ManagementClient client1;

    @ArquillianResource
    @TargetsContainer(NODE_2)
    private ManagementClient client2;

    final int INVOCATION_LOOP_TIMES = 5 ;
    final int SUSPEND_RESUME_LOOP_TIMES = 5 ;

    // time between invocations
    final long INV_WAIT_DURATION_MSECS = 10 ;
    // time that the server remains suspended (or resumed) during continuous invocations
    final long SUSPEND_RESUME_DURATION_MSECS = 1 * 1000 ;

    // the set of nodes available, according to suspend/resume
    Set<String> nodesAvailable = new HashSet<String>();


    @Before
    public void initialiseNodesAvailable() {
        nodesAvailable.addAll(Arrays.asList(NODE_1,NODE_2));
    }

    /**
     * This test checks that suspending and then resuming the server during invocation results in correct behaviour
     * in the case that the proxy is created before the server is suspended.
     *
     * The test assertion is checked after each invocation result, and verifies that no invocation is sent to a suspended node.
     *
     * @throws Exception
     */
    @Test
    @InSequence(1)
    public void testSuspendResumeAfterProxyInit() throws Exception {
        LOGGER.info("testSuspendResumeAfterProxyInit() - start");
        try (EJBDirectory directory = new RemoteEJBDirectory(MODULE_NAME)) {

            HeartbeatRemoteHome home = directory.lookupHome(SlowHeartbeatBean.class, HeartbeatRemoteHome.class);
            HeartbeatRemote bean = home.create();

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
        } catch(Exception e) {
            LOGGER.info("Caught exception! e = " + e.getMessage());
            Assert.fail("Test failed with exception: e = " + e.getMessage());
        } finally {
            LOGGER.info("testSuspendResumeAfterProxyInit() - end");
        }
    }

    /**
     * This test checks that suspending and then resuming the server during invocation results in correct behaviour
     * in the case that the proxy is created after the server is suspended.
     *
     *  The test assertion is checked after each invocation result, and verifies that no invocation is sent to a suspended node.
     *
     * @throws Exception
     */
    @Test
    @InSequence(2)
    public void testSuspendResumeBeforeProxyInit() throws Exception {
        LOGGER.info("testSuspendResumeBeforeProxyInit() - start");
        try (EJBDirectory directory = new RemoteEJBDirectory(MODULE_NAME)) {

            suspendTheServer(NODE_1);

            HeartbeatRemoteHome home = directory.lookupHome(SlowHeartbeatBean.class, HeartbeatRemoteHome.class);
            HeartbeatRemote bean = home.create();

            for (int i = 1; i < INVOCATION_LOOP_TIMES; i++) {
                performInvocation(bean);
            }

            resumeTheServer(NODE_1);

            for (int i = 1; i < INVOCATION_LOOP_TIMES; i++) {
                performInvocation(bean);
            }
        } catch(Exception e) {
            LOGGER.info("Caught exception! e = " + e.getMessage());
            Assert.fail("Test failed with exception: e = " + e.getMessage());
        } finally {
            LOGGER.info("testSuspendResumeBeforeProxyInit() - end");
        }
    }

    /**
     * This test checks that suspending and then resuming the server during invocation results in correct behaviour
     * in the case that the proxy is created after the server is suspended.
     *
     * The test assertion is checked after each invocation result, and verifies that no invocation is sent to a suspended node.
     *
     * @throws Exception
     */
    @Test
    @InSequence(3)
    public void testSuspendResumeContinuous() throws Exception {
        LOGGER.info("testSuspendResumeContinuous() - start");
        try (EJBDirectory directory = new RemoteEJBDirectory(MODULE_NAME)) {

            HeartbeatRemoteHome home = directory.lookupHome(SlowHeartbeatBean.class, HeartbeatRemoteHome.class);
            HeartbeatRemote bean = home.create();
            ContinuousInvoker continuousInvoker = new ContinuousInvoker(bean);

            Thread thread = new Thread(continuousInvoker);
            LOGGER.info("Starting the invoker ...");
            thread.start();

            for (int i = 0; i < SUSPEND_RESUME_LOOP_TIMES; i++) {
                // suspend and then resume each server in turn while invocations happen
                sleep(SUSPEND_RESUME_DURATION_MSECS);

                suspendTheServer(NODE_1);

                sleep(SUSPEND_RESUME_DURATION_MSECS);

                resumeTheServer(NODE_1);

                // suspend and then resume each server in turn while invocations happen
                sleep(SUSPEND_RESUME_DURATION_MSECS);

                suspendTheServer(NODE_2);

                sleep(SUSPEND_RESUME_DURATION_MSECS);

                resumeTheServer(NODE_2);
            }

            continuousInvoker.stopInvoking();
        }
        catch(Exception e) {
            LOGGER.info("Caught exception! e = " + e.getMessage());
            Assert.fail("Test failed with exception: e = " + e.getMessage());
        }
        LOGGER.info("testSuspendResumeContinuous() - end");
    }


    /**
     * Helper class that performs invocations on a bean once per second until stopInvoking() is called.
     */
    private class ContinuousInvoker implements Runnable {
        private boolean invoking = true ;
        HeartbeatRemote bean = null;

        public ContinuousInvoker(HeartbeatRemote bean) {
            this.bean = bean;
        }

        public synchronized void stopInvoking() {
            LOGGER.info("Stopping the invoker ...");
            this.invoking = false;
        }

        private synchronized boolean keepInvoking() {
            return this.invoking == true;
        }

        @Override
        public void run() {
            try {
                while (keepInvoking()) {
                    performInvocation(bean);
                }
            } catch (Exception e) {
                LOGGER.info("ContinousInvoker: caught exception while performing invocation: e = " + e.getMessage());
                throw e;
            }
        }
    }

    private void performInvocation(HeartbeatRemote bean) {
        Result<Date> result = null;
        try {
            result = bean.pulse();

            LOGGER.info("invoked pulse(), result: node = " + result.getNode() + ", value = " + result.getValue()) ;
            Assert.assertTrue(nodesAvailable.contains(result.getNode()));
            sleep(INV_WAIT_DURATION_MSECS);
        } catch (Exception e) {
            LOGGER.info("Exception caught while invoking pulse(): " + e.getMessage());
            throw e;
        }
    }


    // helper methods to determine server state

    private void suspendTheServer(String node) {
        LOGGER.info("=================== SUSPEND  " + node + " ===========================");
        if (suspendServer(node)) {
            nodesAvailable.remove(node);
        }
        LOGGER.info(isServerSuspended(node) ? node + " is suspended" : node + " is NOT suspended") ;
    }

    private void resumeTheServer(String node) {
        LOGGER.info("=================== RESUME  " + node + " ===========================");
        if (resumeServer(node)) {
            nodesAvailable.add(node);
        }
        LOGGER.info(isServerSuspended(node) ? node + " is suspended" : node + " is NOT suspended") ;
    }

    private boolean isServerRunning(String node) {
        try {
            ModelNode op = Util.createOperation("read-attribute", PathAddress.EMPTY_ADDRESS);
            op.get(NAME).set("server-state");
            ModelNode result = null;
            if (NODE_1.equals(node)) {
                result = client1.getControllerClient().execute(op);
            } else {
                result = client2.getControllerClient().execute(op);
            }
            return SUCCESS.equals(result.get(OUTCOME).asString())
                    && !CONTROLLER_PROCESS_STATE_STARTING.equals(result.get(RESULT).asString())
                    && !CONTROLLER_PROCESS_STATE_STOPPING.equals(result.get(RESULT).asString());
        } catch(IOException e) {
            return false;
        }
    }

    /**
     *  Suspend a server by calling the management operation ":suspend"
     *
     * @param node the node to be suspended
     * @return true if the operation succeeded
     */
    private boolean suspendServer(String node) {
        try {
            ModelNode op = Util.createOperation("suspend", PathAddress.EMPTY_ADDRESS);
            ModelNode result = null;
            if (NODE_1.equals(node)) {
                result = client1.getControllerClient().execute(op);
            } else {
                result = client2.getControllerClient().execute(op);
            }
            return SUCCESS.equals(result.get(OUTCOME).asString());
        } catch(IOException e) {
            return false;
        }
    }

    /**
     *  Resume a server by calling the management operation ":suspend"
     *
     * @param node the node to be suspended
     * @return true if the operation succeeded
     */
    private boolean resumeServer(String node) {
        try {
            ModelNode op = Util.createOperation("resume", PathAddress.EMPTY_ADDRESS);
            ModelNode result = null;
            if (NODE_1.equals(node)) {
                result = client1.getControllerClient().execute(op);
            } else {
                result = client2.getControllerClient().execute(op);
            }
            return SUCCESS.equals(result.get(OUTCOME).asString());
        } catch(IOException e) {
            return false;
        }
    }

    /**
     *  Check if a server is suspended by reading the server attribute "suspend-state"
     *
     * @param node the node to be checked
     * @return true if the server is suspended
     */
    private boolean isServerSuspended(String node) {
        try {
            ModelNode op = Util.createOperation("read-attribute", PathAddress.EMPTY_ADDRESS);
            op.get(NAME).set("suspend-state");
            ModelNode result = null;
            if (NODE_1.equals(node)) {
                result = client1.getControllerClient().execute(op);
            } else {
                result = client2.getControllerClient().execute(op);
            }
            return SUCCESS.equals(result.get(OUTCOME).asString())
                    && RUNNING_STATE_SUSPENDED.equalsIgnoreCase(result.get(RESULT).asString());
        } catch(IOException e) {
            return false;
        }
    }

    private void sleep(long ms) {
        try {
            LOGGER.info("Sleeping for " + ms + " ms ...");
            Thread.sleep( ms);
        } catch(InterruptedException ie) {
            // noop
        }
    }
}
