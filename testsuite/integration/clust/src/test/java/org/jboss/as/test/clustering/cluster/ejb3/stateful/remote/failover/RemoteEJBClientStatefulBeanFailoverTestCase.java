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

package org.jboss.as.test.clustering.cluster.ejb3.stateful.remote.failover;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.container.test.api.TargetsContainer;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.junit.InSequence;

import static org.jboss.as.test.clustering.ClusteringTestConstants.*;

import org.jboss.as.test.clustering.EJBClientContextSelector;
import org.jboss.as.test.clustering.EJBDirectory;
import org.jboss.as.test.clustering.NodeNameGetter;
import org.jboss.as.test.clustering.RemoteEJBDirectory;
import org.jboss.as.test.clustering.cluster.ClusterAbstractTestCase;
import org.jboss.ejb.client.ContextSelector;
import org.jboss.ejb.client.EJBClientContext;
import org.jboss.logging.Logger;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Tests that invocations on a clustered stateful session bean from a remote EJB client, failover to
 * other node(s) in cases like a node going down
 * <p/>
 * This test also replicates some decorated CDI beans, to make sure they are replicated correctly.
 *
 * @author Jaikiran Pai
 * @author Radoslav Husar
 * @version Oct 2012
 */
@RunWith(Arquillian.class)
@RunAsClient
public class RemoteEJBClientStatefulBeanFailoverTestCase extends ClusterAbstractTestCase {

    private static final Logger logger = Logger.getLogger(RemoteEJBClientStatefulBeanFailoverTestCase.class);

    private static final String MODULE_NAME = "remote-ejb-client-stateful-bean-failover-test";

    private static EJBDirectory context;

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
        // CounterBean package but don't include the tests
        ejbJar.addClasses(CDIDecorator.class, ClientSFSBRemote.class, DestructionCounterSingleton.class,
                NodeNameRetriever.class, CDIManagedBean.class, CounterBean.class, DecoratorInterface.class,
                NodeNameSFSB.class, ClientSFSB.class, CounterResult.class, DestructionCounterRemote.class,
                RemoteCounter.class);
        ejbJar.addClass(NodeNameGetter.class);
        ejbJar.addAsManifestResource(new StringAsset("<beans>" +
                "<decorators><class>" + CDIDecorator.class.getName() + "</class></decorators>" +
                "</beans>"), "beans.xml");
        return ejbJar;
    }

    @BeforeClass
    public static void setupEjbContext() throws Exception {
        context = new RemoteEJBDirectory(MODULE_NAME);
    }

    @Override
    protected void setUp() {
        super.setUp();

        // Also deploy
        deploy(DEPLOYMENTS);
    }


    /**
     * Starts 2 nodes with the clustered beans deployed on each node. Invokes a clustered SFSB a few times.
     * Then stops a node from among the cluster (the one which received the last invocation) and continues invoking
     * on the same SFSB. These subsequent invocations are expected to failover to the other node and also have the
     * correct state of the SFSB.
     *
     * @throws Exception
     */
    @Test
    @InSequence(1)
    public void testRemoteClientFailoverOnShutdown() throws Exception {
        this.remoteClientFailover(false);
    }

    /**
     * Same as above, but application gets undeployed while the server keeps running.
     *
     * @throws Exception
     */
    @Test
    @InSequence(2)
    public void testRemoteClientFailoverOnUndeploy() throws Exception {
        this.remoteClientFailover(true);
    }

    private void remoteClientFailover(boolean undeployOnly) throws Exception {
        //DEBUG
        //stop(CONTAINERS);
        //start(CONTAINERS);

        final ContextSelector<EJBClientContext> previousSelector = EJBClientContextSelector.setup("cluster/ejb3/stateful/failover/sfsb-failover-jboss-ejb-client.properties");
        boolean container1Stopped = false;
        boolean container2Stopped = false;
        try {
            final RemoteCounter remoteCounter = context.lookupStateful(CounterBean.class, RemoteCounter.class);
            final DestructionCounterRemote destructionCounter = context.lookupSingleton(DestructionCounterSingleton.class, DestructionCounterRemote.class);
            // invoke on the bean a few times
            final int NUM_TIMES = 25;
            for (int i = 0; i < NUM_TIMES; i++) {
                final CounterResult result = remoteCounter.increment();
                logger.info("Counter incremented to " + result.getCount() + " on node " + result.getNodeName());
            }
            final CounterResult result = remoteCounter.getCount();
            Assert.assertNotNull("Result from remote stateful counter was null", result);
            Assert.assertEquals("Unexpected count from remote counter", NUM_TIMES, result.getCount());
            Assert.assertEquals("Nothing should have been destroyed yet", 0, destructionCounter.getCDIDestructionCount());
            Assert.assertEquals("Nothing should have been destroyed yet", 0, destructionCounter.getSFSBDestructionCount());

            // shutdown the node on which the previous invocation happened
            final int totalCountBeforeShuttingDownANode = result.getCount();
            final String previousInvocationNodeName = result.getNodeName();
            // the value is configured in arquillian.xml of the project
            if (previousInvocationNodeName.equals(NODE_1)) {
                if (undeployOnly) {
                    undeploy(DEPLOYMENT_1);
                } else {
                    stop(CONTAINER_1);
                }
                container1Stopped = true;
            } else {
                if (undeployOnly) {
                    undeploy(DEPLOYMENT_2);
                } else {
                    stop(CONTAINER_2);
                }
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


            Assert.assertEquals("Nothing should have been destroyed yet", 0, destructionCounter.getCDIDestructionCount());
            Assert.assertEquals("Nothing should have been destroyed yet", 0, destructionCounter.getSFSBDestructionCount());
            remoteCounter.remove();
            Assert.assertEquals("CDI bean was not destroyed", 1, destructionCounter.getCDIDestructionCount());
            Assert.assertEquals("SFSB was not destroyed", 1, destructionCounter.getSFSBDestructionCount());


        } finally {
            // reset the selector
            if (previousSelector != null) {
                EJBClientContext.setSelector(previousSelector);
            }

            // Restore deployment for shutdown test
            if (container1Stopped) {
                if (undeployOnly) {
                    undeploy(DEPLOYMENT_2);
                    deploy(DEPLOYMENTS); // This test required redeployment :-(
                } else {
                    undeploy(DEPLOYMENT_2);
                    start(CONTAINER_1);
                    deploy(DEPLOYMENTS);
                }
            }

            if (container2Stopped) {
                if (undeployOnly) {
                    undeploy(DEPLOYMENT_1);
                    deploy(DEPLOYMENTS); // This test required redeployment :-(
                } else {
                    undeploy(DEPLOYMENT_2);
                    start(CONTAINER_2);
                    deploy(DEPLOYMENTS);
                }
            }
        }
    }
}
