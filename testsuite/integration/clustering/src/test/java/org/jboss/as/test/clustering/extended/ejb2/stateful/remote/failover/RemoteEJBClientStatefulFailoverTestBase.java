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

package org.jboss.as.test.clustering.extended.ejb2.stateful.remote.failover;

import javax.naming.NamingException;

import org.jboss.as.test.clustering.EJBClientContextSelector;
import org.jboss.as.test.clustering.cluster.ClusterAbstractTestCase;
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
public abstract class RemoteEJBClientStatefulFailoverTestBase extends ClusterAbstractTestCase {
    private static final Logger log = Logger.getLogger(RemoteEJBClientStatefulFailoverTestBase.class);

    protected static final String PROPERTIES_FILE = "cluster/ejb3/stateful/failover/sfsb-failover-jboss-ejb-client.properties";
    protected static final String ARCHIVE_NAME = "ejb2-failover-test";
    protected static final String ARCHIVE_NAME_SINGLE = ARCHIVE_NAME + "-single";

    protected static EJBDirectory singletonDirectory;
    protected static EJBDirectory directory;

    protected static Archive<?> createDeploymentSingleton() {
        final JavaArchive jar = ShrinkWrap.create(JavaArchive.class, ARCHIVE_NAME_SINGLE + ".jar");
        jar.addClasses(CounterSingleton.class, CounterSingletonRemote.class);
        return jar;
    }

    @BeforeClass
    public static void beforeClass() throws NamingException {
        directory = new RemoteEJBDirectory(ARCHIVE_NAME);
        singletonDirectory = new RemoteEJBDirectory(ARCHIVE_NAME_SINGLE);
    }

    @AfterClass
    public static void destroy() throws NamingException {
        directory.close();
        singletonDirectory.close();
    }

    @Override
    public void beforeTestMethod() {
        start(CONTAINERS);
        deploy(DEPLOYMENT_HELPERS);
        deploy(DEPLOYMENTS);
    }

    @Override
    public void afterTestMethod() {
        super.afterTestMethod();
        undeploy(DEPLOYMENT_HELPERS);
    }

    /**
     * Starts 2 nodes with the clustered beans deployed on each node. Invokes a clustered SFSB a few times.
     * Then stops a node from among the cluster (the one which received the last invocation) and continues invoking
     * on the same SFSB. These subsequent invocations are expected to failover to the other node and also have the
     * correct state of the SFSB.
     *
     * @throws Exception
     */
    public abstract void testFailoverFromRemoteClientWhenOneNodeGoesDown() throws Exception;

    /**
     * Same as above, but application gets undeployed while the server keeps running.
     *
     * @throws Exception
     */
    public abstract void testFailoverFromRemoteClientWhenOneNodeUndeploys() throws Exception;

    /**
     * Implementation of defined abstract tests above.
     */
    protected void failoverFromRemoteClient(boolean undeployOnly) throws Exception {
        // TODO Elytron: Once support for legacy EJB properties has been added back, actually set the EJB properties
        // that should be used for this test using PROPERTIES_FILE and ensure the EJB client context is reset
        // to its original state at the end of the test
        EJBClientContextSelector.setup(PROPERTIES_FILE);


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
                stop(CONTAINER_1);
            }
        } else {
            if (undeployOnly) {
                deployer.undeploy(DEPLOYMENT_2);
                deployer.undeploy(DEPLOYMENT_HELPER_2);
            } else {
                stop(CONTAINER_2);
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
