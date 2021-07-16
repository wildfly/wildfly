/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2021, Red Hat, Inc., and individual contributors
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

package org.jboss.as.test.clustering.cluster.ejb.remote;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.TargetsContainer;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.as.arquillian.api.ServerSetup;
import org.jboss.as.test.clustering.cluster.AbstractClusteringTestCase;
import org.jboss.as.test.clustering.cluster.ejb.remote.bean.Incrementor;
import org.jboss.as.test.clustering.cluster.ejb.remote.bean.IncrementorBean;
import org.jboss.as.test.clustering.cluster.ejb.remote.bean.Result;
import org.jboss.as.test.clustering.cluster.ejb.remote.bean.StatefulIncrementorBean;
import org.jboss.as.test.clustering.cluster.ejb.remote.bean.StatelessIncrementorBean;
import org.jboss.as.test.clustering.ejb.EJBDirectory;
import org.jboss.as.test.clustering.ejb.RemoteEJBDirectory;
import org.jboss.as.test.shared.CLIServerSetupTask;
import org.jboss.ejb.client.Affinity;
import org.jboss.ejb.client.ClusterAffinity;
import org.jboss.ejb.client.EJBClient;
import org.jboss.ejb.client.EJBClientConnection;
import org.jboss.ejb.client.EJBClientContext;
import org.jboss.ejb.client.EJBIdentifier;
import org.jboss.ejb.client.NodeAffinity;
import org.jboss.ejb.client.StatelessEJBLocator;
import org.jboss.logging.Logger;
import org.jboss.as.test.shared.integration.ejb.security.PermissionUtils;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import javax.naming.Context;
import java.io.File;
import java.net.URL;
import java.util.Properties;
import java.util.PropertyPermission;

/**
 * Test EJB client invocations against two distinct, non-overlapping clusters.
 * This test relies on:
 * - configuration of two non-overlapping clusters, clusterA ={node1, node2} and clusterB={node3, node4}
 * - configuration of EJBClientContext with two configured connections, one per cluster
 * Invocation proxies are created using both EJBClient API and JNDI lookup.
 *
 * @author Richard Achmatowicz
 */
@RunWith(Arquillian.class)
@ServerSetup(RemoteEJBClientTwoClustersTestCase.ServerSetupTask.class)
public class RemoteEJBClientTwoClustersTestCase extends AbstractClusteringTestCase {

    private static final Logger logger = Logger.getLogger(RemoteEJBClientTwoClustersTestCase.class);
    private static final String CONFIGURATION_FILE_SYSTEM_PROPERTY_NAME = "wildfly.config.url";
    private static final String CONFIGURATION_FILE = "two-clusters-wildfly-config.xml";

    private static final String MODULE_NAME = RemoteEJBClientTwoClustersTestCase.class.getSimpleName();
    private static final Class STATEFUL_BEAN_CLASS = StatefulIncrementorBean.class;
    private static final Class STATELESS_BEAN_CLASS = StatelessIncrementorBean.class;
    private static final String STATEFUL_BEAN_NAME = StatefulIncrementorBean.class.getSimpleName();
    private static final String STATELESS_BEAN_NAME = StatelessIncrementorBean.class.getSimpleName();

    public RemoteEJBClientTwoClustersTestCase() {
        super(FOUR_NODES);
    }

    @Deployment(name = DEPLOYMENT_1, managed = false, testable = false)
    @TargetsContainer(NODE_1)
    public static Archive<?> deployment1() {
        return createDeployment();
    }

    @Deployment(name = DEPLOYMENT_2, managed = false, testable = false)
    @TargetsContainer(NODE_2)
    public static Archive<?> deployment2() {
        return createDeployment();
    }

    @Deployment(name = DEPLOYMENT_3, managed = false, testable = false)
    @TargetsContainer(NODE_3)
    public static Archive<?> deployment3() {
        return createDeployment();
    }

    @Deployment(name = DEPLOYMENT_4, managed = false, testable = false)
    @TargetsContainer(NODE_4)
    public static Archive<?> deployment4() {
        return createDeployment();
    }

    private static Archive<?> createDeployment() {
        return ShrinkWrap.create(JavaArchive.class, MODULE_NAME + ".jar")
                .addPackage(EJBDirectory.class.getPackage())
                .addClasses(Result.class, Incrementor.class, IncrementorBean.class, STATEFUL_BEAN_CLASS, STATELESS_BEAN_CLASS)
                .addAsManifestResource(PermissionUtils.createPermissionsXmlAsset(new PropertyPermission(NODE_NAME_PROPERTY, "read")), "permissions.xml")
                ;
    }

    /**
     * Set up the required EJBClientContext for the two clusters.
     * - we need at least one configured connection per cluster
     *
     * Because the Wildfly test suite runs surefire with <reuseForks>false</reuseForks>, this can cause the EJBClientContext
     * to be (staically) initialised in an earlier test execution. Rather than changing this setting and affecting all test cases,
     * we just set the EJBClientContext via the classloader default for ContextManager<EJBClientContext> to be
     * the context we need.
     *
     * @throws Exception
     */
    @BeforeClass
    public static void beforeClass() throws Exception {
        // make sure the desired configuration file is picked up
        ClassLoader cl = RemoteEJBClientTwoClustersTestCase.class.getClassLoader();
        URL resource = cl != null ? cl.getResource(CONFIGURATION_FILE) : ClassLoader.getSystemResource(CONFIGURATION_FILE);
        File file = new File(resource.toURI());
        System.setProperty(CONFIGURATION_FILE_SYSTEM_PROPERTY_NAME,file.getAbsolutePath());
    }

    @AfterClass
    public static void afterClass() throws Exception {
        // this test needs to run in its own JVM
        System.clearProperty(CONFIGURATION_FILE_SYSTEM_PROPERTY_NAME);
    }

    /**
     * Tests that EJBClient invocations to different clusters get expected results with EJBClient API-constructed proxies.
     * The two clusters are clusterA and clusterB and they both have the same module deployed.
     * Their cluster affinity values are the corresponding JChannel names, "clusterA" and "clusterB"
     */
    @Test
    public void testStatefulEJBCLientAPIBasedProxies() throws Exception {
        logger.info("Running a test against two clusters using stateful EJBClient-constructed proxies");
        logConfiguredConnections();

        // the EJBIdentifier identifies a bean in a module
        EJBIdentifier statefulIncrementorBean = new EJBIdentifier("", MODULE_NAME, STATEFUL_BEAN_NAME, "");
        // use the ClusterAffinity to constrain invocations to target a specific cluster of nodes only
        Affinity targetClusterA = new ClusterAffinity("clusterA");
        Affinity targetClusterB = new ClusterAffinity("clusterB");

        // try to invoke on clusterA
        try {
            // create the Locator for the session creation request, adding in the interface and affinity specification to the EJBIdentifier
            StatelessEJBLocator<Incrementor> preProxyBeanOnClusterA = new StatelessEJBLocator<Incrementor>(Incrementor.class, statefulIncrementorBean, targetClusterA);
            // create the session bean proxy
            Incrementor beanOnClusterA = EJBClient.createSessionProxy(preProxyBeanOnClusterA);

            Result<Integer> result = beanOnClusterA.increment();
            logger.info("Invocation on stateful EJBClient-based proxy for cluster A hit node " + result.getNode());
            Assert.assertTrue("Invocation on stateful EJBClient-based proxy hit wrong cluster", isMemberOfClusterA(result.getNode()));
            Assert.assertTrue("Invocation on stateful EJBClient-based proxy has wrong weak affinity", hasWeakAffinityToNodeInClusterA(beanOnClusterA));
        } catch (Exception e) {
            Assert.fail("Got exception invoking on stateful EJBClient-based proxy for clusterA: " + e.getMessage());
        }

        // try to invoke on clusterB
        try {
            // create the Locator for the session creation request
            StatelessEJBLocator<Incrementor> preProxyBeanOnClusterB = new StatelessEJBLocator<Incrementor>(Incrementor.class, statefulIncrementorBean, targetClusterB);
            // create the session bean proxy
            Incrementor beanOnClusterB = EJBClient.createSessionProxy(preProxyBeanOnClusterB);

            Result<Integer> result = beanOnClusterB.increment();
            logger.info("Invocation on stateful EJBClient-based proxy for cluster B hit node " + result.getNode());
            Assert.assertTrue("Invocation on stateful EJBClient-based proxy hit wrong cluster", isMemberOfClusterB(result.getNode()));
            Assert.assertTrue("Invocation on stateful EJBClient-based proxy has wrong weak affinity", hasWeakAffinityToNodeInClusterB(beanOnClusterB));
        } catch (Exception e) {
            Assert.fail("Got exception invoking on stateful EJBClient-based proxy for clusterB: " + e.getMessage());
        }
    }


    /**
     * Tests that EJBClient invocations to different clusters get expected results with JNDI-based proxies.
     * The two clusters are clusterA and clusterB and they both have the same module deployed.
     * Their cluster affinity values are the corresponding JChannel names, "clusterA" and "clusterB"
     */
    @Test
    public void testStatefulJNDIBasedProxies() throws Exception {
        logger.info("Running a test against two clusters using stateful proxies looked up via JNDI");
        logConfiguredConnections();

        // try to invoke on clusterA
        try (EJBDirectory directory = new RemoteEJBDirectory(MODULE_NAME, getJNDIEnvironmentForCluster("clusterA"))) {
            Incrementor beanOnClusterA = directory.lookupStateful(STATEFUL_BEAN_CLASS, Incrementor.class);
            Result<Integer> result = beanOnClusterA.increment();
            logger.info("Invocation on stateful JNDI-based proxy for cluster A hit node " + result.getNode());
            Assert.assertTrue("Invocation on stateful JNDI-based proxy hit wrong cluster", isMemberOfClusterA(result.getNode()));
            Assert.assertTrue("Invocation on stateful JNDI-based proxy has wrong weak affinity", hasWeakAffinityToNodeInClusterA(beanOnClusterA));
        } catch (Exception e) {
            Assert.fail("Got exception invoking on JNDI-based proxy for clusterA: " + e.getMessage());
        }

        // try to invoke on clusterB
        try (EJBDirectory directory = new RemoteEJBDirectory(MODULE_NAME, getJNDIEnvironmentForCluster("clusterB"))) {
            Incrementor beanOnClusterB = directory.lookupStateful(STATEFUL_BEAN_CLASS, Incrementor.class);
            Result<Integer> result = beanOnClusterB.increment();
            logger.info("Invocation on stateful JNDI-based proxy for cluster B hit node " + result.getNode());
            Assert.assertTrue("Invocation on stateful JNDI-based proxy hit wrong cluster", isMemberOfClusterB(result.getNode()));
            Assert.assertTrue("Invocation on stateful JNDI-based proxy has wrong weak affinity", hasWeakAffinityToNodeInClusterB(beanOnClusterB));
        } catch (Exception e) {
            Assert.fail("Got exception invoking on JNDI-based proxy for clusterB: " + e.getMessage());
        }
    }

    /**
     * Tests that EJBClient invocations to different clusters get expected results with EJBClient API-constructed proxies.
     * The two clusters are clusterA and clusterB and they both have the same module deployed.
     * Their cluster affinity values are the corresponding JChannel names, "clusterA" and "clusterB"
     */
    @Test
    public void testStatelessEJBCLientAPIBasedProxies() throws Exception {
        logger.info("Running a test against two clusters using EJBClient-constructed proxies");
        logConfiguredConnections();

        // the EJBIdentifier identifies a bean in a module
        EJBIdentifier statelessIncrementorBean = new EJBIdentifier("", MODULE_NAME, STATELESS_BEAN_NAME, "");
        // use the ClusterAffinity to constrain invocations to target a specific cluster of nodes only
        Affinity targetClusterA = new ClusterAffinity("clusterA");
        Affinity targetClusterB = new ClusterAffinity("clusterB");

        // try to invoke on clusterA
        try {
            // create the Locator for the session creation request, adding in the interface and affinity specification to the EJBIdentifier
            StatelessEJBLocator<Incrementor> preProxyBeanOnClusterA = new StatelessEJBLocator<Incrementor>(Incrementor.class, statelessIncrementorBean, targetClusterA);
            // create the session bean proxy
            Incrementor beanOnClusterA = EJBClient.createProxy(preProxyBeanOnClusterA);

            Result<Integer> result = beanOnClusterA.increment();
            logger.info("Invocation on stateless EJBClient-based proxy for cluster A hit node " + result.getNode());
            Assert.assertTrue("Invocation on stateless EJBClient-based proxy hit wrong cluster", isMemberOfClusterA(result.getNode()));
            Assert.assertTrue("Invocation on stateless EJBClient-based proxy has wrong weak affinity", hasWeakAffinityNONE(beanOnClusterA));
        } catch (Exception e) {
            Assert.fail("Got exception invoking on EJBClient-based proxy for clusterA: " + e.getMessage());
        }

        // try to invoke on clusterB
        try {
            // create the Locator for the session creation request
            StatelessEJBLocator<Incrementor> preProxyBeanOnClusterB = new StatelessEJBLocator<Incrementor>(Incrementor.class, statelessIncrementorBean, targetClusterB);
            // create the session bean proxy
            Incrementor beanOnClusterB = EJBClient.createProxy(preProxyBeanOnClusterB);

            Result<Integer> result = beanOnClusterB.increment();
            logger.info("Invocation on stateless EJBClient-based proxy for cluster B hit node " + result.getNode());
            Assert.assertTrue("Invocation on stateless EJBClient-based proxy hit wrong cluster", isMemberOfClusterB(result.getNode()));
            Assert.assertTrue("Invocation on stateless EJBClient-based proxy has wrong weak affinity", hasWeakAffinityNONE(beanOnClusterB));
        } catch (Exception e) {
            Assert.fail("Got exception invoking on EJBClient-based proxy for clusterB: " + e.getMessage());
        }
    }

    /**
     * Tests that EJBClient invocations to different clusters get expected results with JNDI-based proxies.
     * The two clusters are clusterA and clusterB and they both have the same module deployed.
     * Their cluster affinity values are the corresponding JChannel names, "clusterA" and "clusterB"
     */
    @Test
    public void testStatelessJNDIBasedProxies() throws Exception {
        logger.info("Running a test against two clusters using proxies looked up via JNDI");
        logConfiguredConnections();

        // try to invoke on clusterA
        try (EJBDirectory directory = new RemoteEJBDirectory(MODULE_NAME, getJNDIEnvironmentForCluster("clusterA"))) {
            Incrementor beanOnClusterA = directory.lookupStateless(STATELESS_BEAN_CLASS, Incrementor.class);
            Result<Integer> result = beanOnClusterA.increment();
            logger.info("Invocation on stateless JNDI-based proxy for cluster A hit node " + result.getNode());
            Assert.assertTrue("Invocation on stateless JNDI-based proxy hit wrong cluster", isMemberOfClusterA(result.getNode()));
            Assert.assertTrue("Invocation on stateless JNDI-based proxy has wrong weak affinity", hasWeakAffinityNONE(beanOnClusterA));
        } catch (Exception e) {
            Assert.fail("Got exception invoking on JNDI-based proxy for clusterA: " + e.getMessage());
        }

        // try to invoke on clusterB
        try (EJBDirectory directory = new RemoteEJBDirectory(MODULE_NAME, getJNDIEnvironmentForCluster("clusterB"))) {
            Incrementor beanOnClusterB = directory.lookupStateless(STATELESS_BEAN_CLASS, Incrementor.class);
            Result<Integer> result = beanOnClusterB.increment();
            logger.info("Invocation on stateless JNDI-based proxy for cluster B hit node " + result.getNode());
            Assert.assertTrue("Invocation on stateless JNDI-based proxy hit wrong cluster", isMemberOfClusterB(result.getNode()));
            Assert.assertTrue("Invocation on stateless JNDI-based proxy has wrong weak affinity", hasWeakAffinityNONE(beanOnClusterB));
        } catch (Exception e) {
            Assert.fail("Got exception invoking on JNDI-based proxy for clusterB: " + e.getMessage());
        }
    }

    private boolean isMemberOfClusterA(String nodeName) {
        return (NODE_1.equals(nodeName) || NODE_2.equals(nodeName));
    }
    private boolean isMemberOfClusterB(String nodeName) {
        return (NODE_3.equals(nodeName) || NODE_4.equals(nodeName));
    }

    private boolean hasWeakAffinityToNodeInClusterA(Incrementor proxy) {
        Assert.assertTrue("Proxy does not have weak affinity NodeAffinity", EJBClient.getWeakAffinity(proxy) instanceof NodeAffinity);
        String nodeName = ((NodeAffinity)EJBClient.getWeakAffinity(proxy)).getNodeName();
        return (NODE_1.equals(nodeName) || NODE_2.equals(nodeName));
    }

   private boolean hasWeakAffinityToNodeInClusterB(Incrementor proxy) {
        Assert.assertTrue("Proxy does not have weak affinity NodeAffinity", EJBClient.getWeakAffinity(proxy) instanceof NodeAffinity);
        String nodeName = ((NodeAffinity)EJBClient.getWeakAffinity(proxy)).getNodeName();
        return (NODE_3.equals(nodeName) || NODE_4.equals(nodeName));
    }

    private boolean hasWeakAffinityNONE(Incrementor proxy) {
        return EJBClient.getWeakAffinity(proxy).equals(Affinity.NONE);
    }

    private void logConfiguredConnections() {
        logger.info("Checking configured connections:");
        for (EJBClientConnection connection : EJBClientContext.getCurrent().getConfiguredConnections()) {
            logger.info("connection: " + connection.getDestination());
        }
    }

    /**
     * Set up the JNDI environment to allow SFSB creation on the correct cluster
     *
     * @param clusterName
     * @return
     */
    private Properties getJNDIEnvironmentForCluster(String clusterName) {
        Properties env = new Properties();
        env.setProperty(Context.INITIAL_CONTEXT_FACTORY, org.wildfly.naming.client.WildFlyInitialContextFactory.class.getName());
        env.setProperty("jboss.cluster-affinity", clusterName);
        return env;
    }

    /**
     * Setup server configurations to allow two independent clusters: clusterA = {node1, node2}, clusterB = {node3, node4}
     * This requires the following changes:
     * 1. keeping the two clusters separated at the JGroups level:
     *   - the server's JChannel managed by resource channel "ee" needs to have a different name in each cluster
     *   - the discovery protocol used by JGroups needs to use a different discovery address in each cluster
     * 2. providing distinct cluster identities used for ClusterAffinity processing which are provided by topology updates
     *   - for distributed caches over a single channel, the required ClusterAffinity value defaults to the JChannel name of the channel used
     */
    public static class ServerSetupTask extends CLIServerSetupTask {
        public ServerSetupTask() {
            this.builder
                    // clusterA nodes
                    .node(NODE_1, NODE_2)
                    // distinct JChannel name for each node in cluster A
                    .setup("/subsystem=jgroups/channel=ee:write-attribute(name=cluster,value=clusterA)")
                    .teardown("/subsystem=jgroups/channel=ee:write-attribute(name=cluster,value=ejb)")
                    .parent()
                    // clusterB nodes
                    .node(NODE_3, NODE_4)
                    // distinct JChannel name in cluster B for each node as well as unique discovery address
                    .setup("/subsystem=jgroups/channel=ee:write-attribute(name=cluster,value=clusterB)")
                    .setup("/socket-binding-group=standard-sockets/socket-binding=jgroups-mping:write-attribute(name=multicast-address,value=%s)", TESTSUITE_MCAST3)
                    .teardown("/socket-binding-group=standard-sockets/socket-binding=jgroups-mping:write-attribute(name=multicast-address,value=%s)", TESTSUITE_MCAST)
                    .teardown("/subsystem=jgroups/channel=ee:write-attribute(name=cluster,value=ejb)")
            ;
        }
    }
}
