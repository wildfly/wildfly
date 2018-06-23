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

package org.jboss.as.test.clustering.cluster.ejb2.stateful.failover;

import javax.naming.NamingException;

import org.jboss.as.test.clustering.cluster.AbstractClusteringTestCase;
import org.jboss.as.test.clustering.cluster.ejb2.stateful.failover.bean.shared.CounterRemote;
import org.jboss.as.test.clustering.cluster.ejb2.stateful.failover.bean.shared.CounterRemoteHome;
import org.jboss.as.test.clustering.cluster.ejb2.stateful.failover.bean.shared.CounterResult;
import org.jboss.as.test.clustering.cluster.ejb2.stateful.failover.bean.singleton.CounterSingleton;
import org.jboss.as.test.clustering.cluster.ejb2.stateful.failover.bean.singleton.CounterSingletonRemote;
import org.jboss.as.test.clustering.ejb.EJBDirectory;
import org.jboss.as.test.clustering.ejb.RemoteEJBDirectory;
import org.jboss.logging.Logger;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;

/**
 * @author Ondrej Chaloupka
 */
public abstract class RemoteEJBClientStatefulFailoverTestBase extends AbstractClusteringTestCase {
    private static final Logger log = Logger.getLogger(RemoteEJBClientStatefulFailoverTestBase.class);

    protected static final String MODULE_NAME = RemoteEJBClientStatefulFailoverTestBase.class.getSimpleName();
    protected static final String MODULE_NAME_SINGLE = MODULE_NAME + "-single";

    protected static EJBDirectory singletonDirectory;
    protected static EJBDirectory directory;

    protected static Archive<?> createDeploymentSingleton() {
        final JavaArchive jar = ShrinkWrap.create(JavaArchive.class, MODULE_NAME_SINGLE + ".jar");
        jar.addPackage(CounterSingletonRemote.class.getPackage());
        return jar;
    }

    public RemoteEJBClientStatefulFailoverTestBase() {
        super(TWO_NODES, new String[] { DEPLOYMENT_HELPER_1, DEPLOYMENT_HELPER_2, DEPLOYMENT_1, DEPLOYMENT_2 });
    }

    @Override
    public void afterTestMethod() {
        start(nodes);
        undeploy(TWO_DEPLOYMENTS);
        undeploy(TWO_DEPLOYMENT_HELPERS);
    }

    @BeforeClass
    public static void beforeClass() throws NamingException {
        directory = new RemoteEJBDirectory(MODULE_NAME);
        singletonDirectory = new RemoteEJBDirectory(MODULE_NAME_SINGLE);
    }

    @AfterClass
    public static void destroy() throws Exception {
        directory.close();
        singletonDirectory.close();
    }

    /**
     * Starts 2 nodes with the clustered beans deployed on each node. Invokes a clustered SFSB a few times.
     * Then stops a node from among the cluster (the one which received the last invocation) and continues invoking
     * on the same SFSB. These subsequent invocations are expected to failover to the other node and also have the
     * correct state of the SFSB.
     */
    public abstract void testFailoverFromRemoteClientWhenOneNodeGoesDown() throws Exception;

    /**
     * Same as above, but application gets undeployed while the server keeps running.
     */
    public abstract void testFailoverFromRemoteClientWhenOneNodeUndeploys() throws Exception;

    /**
     * Implementation of defined abstract tests above.
     */
    protected void failoverFromRemoteClient(boolean undeployOnly) throws Exception {
        CounterRemoteHome home = directory.lookupHome(CounterBean.class, CounterRemoteHome.class);
        CounterRemote remoteCounter = home.create();
        Assert.assertNotNull(remoteCounter);

        final CounterSingletonRemote destructionCounter = singletonDirectory.lookupSingleton(CounterSingleton.class, CounterSingletonRemote.class);
        destructionCounter.resetDestroyCount();

        // invoke on the bean a few times
        final int NUM_TIMES = 25;
        for (int i = 0; i < NUM_TIMES; i++) {
            final CounterResult result = remoteCounter.increment();
            log.trace("Counter incremented to " + result.getCount() + " on node " + result.getNodeName());
        }
        final CounterResult result = remoteCounter.getCount();
        Assert.assertNotNull("Result from remote stateful counter was null", result);
        Assert.assertEquals("Unexpected count from remote counter", NUM_TIMES, result.getCount());
        Assert.assertEquals("Nothing should have been destroyed yet", 0, destructionCounter.getDestroyCount());

        // shutdown the node on which the previous invocation happened
        final int totalCountBeforeShuttingDownANode = result.getCount();
        final String previousInvocationNodeName = result.getNodeName();
        // the value is configured in arquillian.xml of the project
        if (previousInvocationNodeName.equals(NODE_1)) {
            if (undeployOnly) {
                deployer.undeploy(DEPLOYMENT_1);
                deployer.undeploy(DEPLOYMENT_HELPER_1);
            } else {
                stop(NODE_1);
            }
        } else {
            if (undeployOnly) {
                deployer.undeploy(DEPLOYMENT_2);
                deployer.undeploy(DEPLOYMENT_HELPER_2);
            } else {
                stop(NODE_2);
            }
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
            log.trace("Counter decremented to " + resultAfterShuttingDownANode.getCount() + " on node " + resultAfterShuttingDownANode.getNodeName());
        }
        final CounterResult finalResult = remoteCounter.getCount();
        Assert.assertNotNull("Result from remote stateful counter, after shutting down a node was null", finalResult);
        final int finalCount = finalResult.getCount();
        final String finalNodeName = finalResult.getNodeName();
        Assert.assertEquals("Result was received from an unexpected node, after shutting down a node", aliveNode, finalNodeName);
        Assert.assertEquals("Unexpected count from remote counter, after shutting down a node", countBeforeDecrementing - NUM_TIMES, finalCount);


        Assert.assertEquals("Nothing should have been destroyed yet", 0, destructionCounter.getDestroyCount());
        remoteCounter.remove();
        Assert.assertEquals("SFSB was not destroyed", 1, destructionCounter.getDestroyCount());
    }
}
