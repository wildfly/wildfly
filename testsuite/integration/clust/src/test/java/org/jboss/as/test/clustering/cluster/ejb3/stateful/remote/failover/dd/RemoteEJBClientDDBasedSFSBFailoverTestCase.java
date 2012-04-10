/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2012, Red Hat, Inc., and individual contributors
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

package org.jboss.as.test.clustering.cluster.ejb3.stateful.remote.failover.dd;

import org.jboss.arquillian.container.test.api.*;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;

import static org.jboss.as.test.clustering.ClusteringTestConstants.*;

import org.jboss.as.test.clustering.EJBClientContextSelector;
import org.jboss.as.test.clustering.EJBDirectory;
import org.jboss.as.test.clustering.NodeNameGetter;
import org.jboss.as.test.clustering.RemoteEJBDirectory;
import org.jboss.as.test.clustering.cluster.ejb3.stateful.remote.failover.CounterResult;
import org.jboss.as.test.clustering.cluster.ejb3.stateful.remote.failover.RemoteCounter;
import org.jboss.ejb.client.ContextSelector;
import org.jboss.ejb.client.EJBClientContext;
import org.jboss.logging.Logger;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Tests that invocations, from a remote EJB client, on a stateful session bean marked as clustered via jboss-ejb3.xml,
 * failover to other node(s) in cases like a node going down
 *
 * @author Jaikiran Pai
 */
@RunWith(Arquillian.class)
@RunAsClient
public class RemoteEJBClientDDBasedSFSBFailoverTestCase {
    private static final Logger logger = Logger.getLogger(RemoteEJBClientDDBasedSFSBFailoverTestCase.class);

    private static final String MODULE_NAME = "remote-ejb-client-dd-based-stateful-bean-failover-test";

    private static EJBDirectory context;

    @ArquillianResource
    private ContainerController container;

    @ArquillianResource
    private Deployer deployer;


    @Deployment(name = DEPLOYMENT_1, managed = false, testable = false)
    @TargetsContainer(CONTAINER_1)
    public static Archive<?> createDeploymentForContainer1() {
        return createDeployment();
    }

    @Deployment(name = DEPLOYMENT_2, managed = false, testable = false)
    @TargetsContainer(CONTAINER_2)
    public static Archive<?> createDeploymentForContainer2() {
        return createDeployment();
    }

    private static Archive<?> createDeployment() {
        final JavaArchive ejbJar = ShrinkWrap.create(JavaArchive.class, MODULE_NAME + ".jar");
        ejbJar.addPackage(DDBasedClusteredSFSB.class.getPackage());
        ejbJar.addClasses(RemoteCounter.class, CounterResult.class, NodeNameGetter.class);
        ejbJar.addAsManifestResource("cluster/ejb3/stateful/failover/dd/jboss-ejb3.xml", "jboss-ejb3.xml");
        logger.info(ejbJar.toString(true));
        return ejbJar;
    }

    @BeforeClass
    public static void beforeClass() throws Exception {
        context = new RemoteEJBDirectory(MODULE_NAME);
    }

    /**
     * Starts 2 nodes with the clustered beans deployed on each node. Invokes a (deployment descriptor based) clustered SFSB a few times.
     * Then stops a node from among the cluster (the one which received the last invocation) and continues invoking
     * on the same SFSB. These subsequent invocations are expected to failover to the other node and also have the
     * correct state of the SFSB.
     *
     * @throws Exception
     */
    @Test
    public void testFailoverFromRemoteClientWhenOneNodeGoesDown() throws Exception {
        // Container is unmanaged, so start it ourselves
        this.container.start(CONTAINER_1);
        // deploy to container1
        this.deployer.deploy(DEPLOYMENT_1);

        // start the other container too
        this.container.start(CONTAINER_2);
        this.deployer.deploy(DEPLOYMENT_2);

        final ContextSelector<EJBClientContext> previousSelector = EJBClientContextSelector.setup("cluster/ejb3/stateful/failover/sfsb-failover-jboss-ejb-client.properties");
        boolean container1Stopped = false;
        boolean container2Stopped = false;
        try {
            final RemoteCounter remoteCounter = context.lookupStateful(DDBasedClusteredSFSB.class, RemoteCounter.class);
            // invoke on the bean a few times
            final int NUM_TIMES = 25;
            for (int i = 0; i < NUM_TIMES; i++) {
                final CounterResult result = remoteCounter.increment();
                logger.info("Counter incremented to " + result.getCount() + " on node " + result.getNodeName());
            }
            final CounterResult result = remoteCounter.getCount();
            Assert.assertNotNull("Result from remote stateful counter was null", result);
            Assert.assertEquals("Unexpected count from remote counter", NUM_TIMES, result.getCount());

            // shutdown the node on which the previous invocation happened
            final int totalCountBeforeShuttingDownANode = result.getCount();
            final String previousInvocationNodeName = result.getNodeName();
            // the value is configured in arquillian.xml of the project
            if (previousInvocationNodeName.equals(NODE_1)) {
                this.deployer.undeploy(DEPLOYMENT_1);
                this.container.stop(CONTAINER_1);
                container1Stopped = true;
            } else {
                this.deployer.undeploy(DEPLOYMENT_2);
                this.container.stop(CONTAINER_2);
                container2Stopped = true;
            }
            // invoke again
            CounterResult resultAfterShuttingDownANode = remoteCounter.increment();
            Assert.assertNotNull("Result from remote stateful counter, after shutting down a node was null", resultAfterShuttingDownANode);
            Assert.assertEquals("Unexpected count from remote counter, after shutting down a node", totalCountBeforeShuttingDownANode + 1, resultAfterShuttingDownANode.getCount());
            Assert.assertFalse("Result was received from an unexpected node, after shutting down a node", previousInvocationNodeName.equals(resultAfterShuttingDownANode.getNodeName()));

            // repeat invocations
            final int countBeforeDecrementing = resultAfterShuttingDownANode.getCount();
            final String aliveNode = resultAfterShuttingDownANode.getNodeName();
            for (int i = NUM_TIMES; i > 0; i--) {
                resultAfterShuttingDownANode = remoteCounter.decrement();
                Assert.assertNotNull("Result from remote stateful counter, after shutting down a node was null", resultAfterShuttingDownANode);
                Assert.assertEquals("Result was received from an unexpected node, after shutting down a node", aliveNode, resultAfterShuttingDownANode.getNodeName());
                logger.info("Counter decremented to " + resultAfterShuttingDownANode.getCount() + " on node " + resultAfterShuttingDownANode.getNodeName());
            }
            final CounterResult finalResult = remoteCounter.getCount();
            Assert.assertNotNull("Result from remote stateful counter, after shutting down a node was null", finalResult);
            final int finalCount = finalResult.getCount();
            final String finalNodeName = finalResult.getNodeName();
            Assert.assertEquals("Result was received from an unexpected node, after shutting down a node", aliveNode, finalNodeName);
            Assert.assertEquals("Unexpected count from remote counter, after shutting down a node", countBeforeDecrementing - NUM_TIMES, finalCount);
        } finally {
            // reset the selector
            if (previousSelector != null) {
                EJBClientContext.setSelector(previousSelector);
            }
            // shutdown the containers
            if (!container1Stopped) {
                this.deployer.undeploy(DEPLOYMENT_1);
                this.container.stop(CONTAINER_1);
            }

            if (!container2Stopped) {
                this.deployer.undeploy(DEPLOYMENT_2);
                this.container.stop(CONTAINER_2);
            }
        }
    }
}
