/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2019, Red Hat, Inc., and individual contributors
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

package org.jboss.as.test.clustering.cluster.ejb.remote.byteman;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.container.test.api.TargetsContainer;
import org.jboss.arquillian.extension.byteman.api.BMRules;
import org.jboss.arquillian.extension.byteman.api.BMRule;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.as.test.clustering.cluster.AbstractClusteringTestCase;
import org.jboss.as.test.clustering.cluster.ejb.remote.bean.Incrementor;
import org.jboss.as.test.clustering.cluster.ejb.remote.bean.IncrementorBean;
import org.jboss.as.test.clustering.cluster.ejb.remote.bean.Result;
import org.jboss.as.test.clustering.cluster.ejb.remote.bean.StatelessIncrementorBean;
import org.jboss.as.test.clustering.ejb.EJBDirectory;
import org.jboss.as.test.clustering.ejb.NamingEJBDirectory;
import org.jboss.as.test.clustering.ejb.RemoteEJBDirectory;
import org.jboss.as.test.shared.TimeoutUtil;
import org.jboss.as.test.shared.integration.ejb.security.PermissionUtils;
import org.jboss.ejb.client.ClusterNodeSelector;
import org.jboss.ejb.client.EJBClientConnection;
import org.jboss.ejb.client.EJBClientContext;
import org.jboss.ejb.protocol.remote.RemoteTransportProvider;
import org.jboss.logging.Logger;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

import javax.ejb.EJBException;
import javax.ejb.NoSuchEJBException;
import javax.naming.NamingException;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.PropertyPermission;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.stream.Collectors;

import static org.junit.Assert.assertNull;

/**
 *
 * Tests the ability of the the RemoteEJBDiscoveryProvider to detect the condition when a last node left in a cluster has crashed
 * and to remove that cluster from the discovered node registry (DNR).
 * The condition is as follows: if we get a ConnectException when trying to connect to a node X in cluster Y, and the DNR shows
 * X as being the only member of Y, then remove cluster Y from the DNR.
 *
 * This test implements a validation criterion which ensures that the following illegal scenario does not occur:
 * - start two cluster nodes A, B:              // membership = {A,B}
 * - shutdown A                                 // membership = {B}
 * - crash B                                    // membership = {B}
 * - start A                                    // membership = {A,B}
 * In this case, B is a member of the cluster (according to the DNR) but it has crashed.
 *
 * @author Richard Achmatowicz
 */
@RunWith(Arquillian.class)
public class LastNodeToLeaveRemoteEJBTestCase extends AbstractClusteringTestCase {

    public LastNodeToLeaveRemoteEJBTestCase() throws Exception {
        super(THREE_NODES);
    }

    static final Logger LOGGER = Logger.getLogger(LastNodeToLeaveRemoteEJBTestCase.class);
    private static final String MODULE_NAME = LastNodeToLeaveRemoteEJBTestCase.class.getSimpleName();

    private static final long INVOCATION_WAIT = TimeoutUtil.adjust(1000);
    private static final int THREADS = 2;

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

    @Deployment(name = DEPLOYMENT_3, managed = false, testable = false)
    @TargetsContainer(NODE_3)
    public static Archive<?> createDeploymentForContainer3() {
        return createDeployment();
    }

    private static Archive<?> createDeployment() {
        return ShrinkWrap.create(JavaArchive.class, MODULE_NAME + ".jar")
                .addPackage(EJBDirectory.class.getPackage())
                .addClasses(Result.class, Incrementor.class, IncrementorBean.class, StatelessIncrementorBean.class)
                .setManifest(new StringAsset("Manifest-Version: 1.0\nDependencies: org.infinispan\n"))
                .addAsManifestResource(PermissionUtils.createPermissionsXmlAsset(new PropertyPermission(NODE_NAME_PROPERTY, "read")), "permissions.xml")
                ;
    }

    public class CustomClusterNodeSelector implements ClusterNodeSelector {
        @Override
        public String selectNode(String clusterName, String[] connectedNodes, String[] totalAvailableNodes) {
            Set<String> connectedNodesSet = Arrays.asList(connectedNodes).stream().collect(Collectors.toSet());
            Set<String> totalNodesSet = Arrays.asList(totalAvailableNodes).stream().collect(Collectors.toSet());
            LOGGER.infof("Calling ClusterNodeSelector.selectNode(%s,%s,%s)", clusterName, connectedNodesSet, totalNodesSet);
            return ClusterNodeSelector.DEFAULT.selectNode(clusterName, connectedNodes, totalAvailableNodes);
        }
    }

    // Byteman rules to capture the DNR contents after each invocation
    @BMRules({
        @BMRule(name = "Set up results linkMap (SETUP)",
            targetClass = "org.jboss.ejb.protocol.remote.RemotingEJBDiscoveryProvider",
            targetMethod = "<init>",
            helper = "org.jboss.as.test.clustering.cluster.ejb.remote.byteman.LastNodeToLeaveTestHelper",
            targetLocation = "EXIT",
            condition = "debug(\" setting up the map \")",
            action = "createNodeListMap();"),

        @BMRule(name = "Track calls to start (COLLECT)",
            targetClass = "org.jboss.as.test.clustering.cluster.ejb.remote.byteman.LastNodeToLeaveRemoteEJBTestCase",
            targetMethod = "getStartedNodes",
            helper = "org.jboss.as.test.clustering.cluster.ejb.remote.byteman.LastNodeToLeaveTestHelper",
            targetLocation = "EXIT",
            binding = "startedNodes = $!;",
            condition = "debug(\"checking for started nodes\")",
            action = "updateStartedNodes(startedNodes);"),

        @BMRule(name = "Track calls to ClusterNodeSelector (COLLECT)",
            targetClass = "org.jboss.as.test.clustering.cluster.ejb.remote.byteman.LastNodeToLeaveRemoteEJBTestCase$CustomClusterNodeSelector",
            targetMethod = "selectNode",
            helper = "org.jboss.as.test.clustering.cluster.ejb.remote.byteman.LastNodeToLeaveTestHelper",
            binding = "clusterName : String = $1;connectedNodes : String[] = $2; totalAvailableNodes : String[] = $3;",
            condition = "debug(\"checking call to cluster node selector\")",
            action = "addConnectedNodesEntryForThread(clusterName, connectedNodes, totalAvailableNodes);"),

        @BMRule(name="Return test result to test case (RETURN)",
            targetClass = "org.jboss.as.test.clustering.cluster.ejb.remote.byteman.LastNodeToLeaveRemoteEJBTestCase",
            targetMethod = "getTestResult",
            helper = "org.jboss.as.test.clustering.cluster.ejb.remote.byteman.LastNodeToLeaveTestHelper",
            targetLocation = "ENTRY",
            condition = "debug(\"returning the result\")",
            action = "return getNodeListMap();")
    })
    @Test
    @RunAsClient
    public void testDNRContentsAfterLastNodeToLeave() throws Exception {

        List<Future<?>> futures = new ArrayList<>(THREADS);
        LOGGER.info("\n *** Starting test case test()\n");
        LOGGER.info("*** Started nodes = " + getStartedNodes());

        ExecutorService executorService = Executors.newFixedThreadPool(THREADS);
        for (int i = 0; i < THREADS; ++i) {
            // start a client thread
            Runnable task = () -> {
                LOGGER.infof("\n *** Starting test thread %s\n", Thread.currentThread().getName());
                EJBClientContext oldContext = null;
                try {
                    // install the correct EJB client context
                    oldContext = EJBClientContext.getContextManager().getGlobalDefault();
                    EJBClientContext newContext = createModifiedEJBClientContext(oldContext);
                    EJBClientContext.getContextManager().setThreadDefault(newContext);

                    // look up the IncrementorBean and repeatedly invoke on it
                    try (NamingEJBDirectory directory = new RemoteEJBDirectory(MODULE_NAME)) {
                        Incrementor bean = directory.lookupStateless(StatelessIncrementorBean.class, Incrementor.class);
                        LOGGER.info("\n +++ Looked up bean for thread " + Thread.currentThread().getName() + " \n");

                        while (!Thread.currentThread().isInterrupted()) {
                            try {
                                // invoke on the incrementor bean and note where it executes
                                LOGGER.info("\n +++ Thread " + Thread.currentThread().getName() + " invoking on bean...\n");

                                Result<Integer> result = bean.increment();
                                String target = result.getNode();
                                LOGGER.info("\n +++ Thread " + Thread.currentThread().getName() + " got result " + result.getValue() + " from node " + target + "\n");
                                Thread.sleep(INVOCATION_WAIT);
                            } catch (InterruptedException e) {
                                Thread.currentThread().interrupt();
                            } catch (NoSuchEJBException e) {
                                LOGGER.info("\n +++ Got NoSuchEJBException from node, skipping...\n");
                            }
                        }
                    } catch (NamingException | EJBException e) {
                        System.out.println("\n +++ Exception looking up bean for thread " + Thread.currentThread().getName() + " \n");
                        assertNull("Cause of EJBException has not been removed", e.getCause());
                    }
                    LOGGER.infof("\n *** Stopping test thread %s\n", Thread.currentThread().getName());
                } finally {
                    if (oldContext != null) {
                        EJBClientContext.getContextManager().setThreadDefault(null);
                    }
                }
            };
            futures.add(executorService.submit(task));
        }

        // Let the system stabilize
        Thread.sleep(GRACE_TIME_TO_REPLICATE);

        // now shutdown the entire cluster then start one node back up again, to check last node behaviour

        LOGGER.info("\n *** Stopping node " + NODE_3 + "\n");
        stop(NODE_3);
        LOGGER.info("*** Started nodes = " + getStartedNodes());
        // Let the system stabilize
        Thread.sleep(GRACE_TIME_TO_MEMBERSHIP_CHANGE);

        LOGGER.info("\n *** Stopping node " + NODE_2 + "\n");
        stop(NODE_2);
        LOGGER.info("*** Started nodes = " + getStartedNodes());
        // Let the system stabilize
        Thread.sleep(GRACE_TIME_TO_MEMBERSHIP_CHANGE);

        LOGGER.info("\n *** Stopping node " + NODE_1 + "\n");
        stop(NODE_1);
        LOGGER.info("*** Started nodes = " + getStartedNodes());
        // Let the system stabilize
        Thread.sleep(GRACE_TIME_TO_MEMBERSHIP_CHANGE);

        LOGGER.info("\n *** Starting node " + NODE_1 + "\n");
        start(NODE_1);
        LOGGER.info("*** Started nodes = " + getStartedNodes());
        // Let the system stabilize
        Thread.sleep(GRACE_TIME_TO_MEMBERSHIP_CHANGE);

        // stop the client
        for (Future<?> future : futures) {
            future.cancel(true);
        }

        executorService.shutdown();

        // get the test results for all threads from the rule
        Map<String, List<List<Set<String>>>> results = getTestResult();

        // validate the test
        for (Map.Entry<String, List<List<Set<String>>>> entry: results.entrySet()) {
            String thread = entry.getKey();
            LOGGER.infof("Collected data for thread: %s", thread);
            List<List<Set<String>>> nodeEntries = entry.getValue();
            for (List<Set<String>> nodeEntry : nodeEntries) {
                Set<String> startedNodes = nodeEntry.get(0);
                Set<String> connectedNodes = nodeEntry.get(1);
                Set<String> totalAvailableNodes = nodeEntry.get(2);
                LOGGER.infof("started nodes = %s, connected nodes = %s, total available nodes = %s", startedNodes, connectedNodes, totalAvailableNodes);

                Assert.assertTrue("Assertion violation: thread " + thread + " has stale nodes in discovered node registry(DNR): " +
                        " started = " + startedNodes + ", connected = " + connectedNodes + ", total available = " + totalAvailableNodes,
                        startedNodes.containsAll(connectedNodes) && startedNodes.containsAll(totalAvailableNodes));
            }
        }
        System.out.println("\n *** Stopping test case test() \n");
    }

    /*
     * Dummy method to allow returning Rule-collected results back to the test case for validation
     * Byteman will populate the return value when the method is called.
     */
    @SuppressWarnings("static-method")
    private Map<String, List<List<Set<String>>>> getTestResult() {
        // injected code will return the actual result
        return null;
    }

    /*
     * Method to allow determining the set of started nodes so that a Byteman rule
     * can keep track of them.
     */
    private Set<String> getStartedNodes() {
        List<String> nodes = Arrays.asList(THREE_NODES);
        Set<String> startedNodes = new HashSet<>();
        for (String node : nodes) {
            if (isStarted(node)) {
                startedNodes.add(node);
            }
        }
        return startedNodes;
    }


    /*
     * Create a sort-of copy of the current context with a different cluster node selector
     *
     * TODO: We need an easier way to create modified EJB client contexts
     */
    private EJBClientContext createModifiedEJBClientContext(EJBClientContext oldContext) {
        final EJBClientContext.Builder ejbClientBuilder = new EJBClientContext.Builder();
        // transport
        ejbClientBuilder.addTransportProvider(new RemoteTransportProvider());
        // configured connections
        for (EJBClientConnection connection : oldContext.getConfiguredConnections()) {
            EJBClientConnection.Builder builder = new EJBClientConnection.Builder();
            builder.setDestination(connection.getDestination());
            builder.setForDiscovery(connection.isForDiscovery());
            ejbClientBuilder.addClientConnection(builder.build());
        }
        // cluster node selector
        ejbClientBuilder.setClusterNodeSelector(new CustomClusterNodeSelector());
        return ejbClientBuilder.build();
    }
}
