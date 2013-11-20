/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2011, Red Hat, Inc., and individual contributors
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

package org.jboss.as.test.clustering.extended.ejb2.stateless;

import static org.jboss.as.test.clustering.ClusteringTestConstants.CONTAINER_1;
import static org.jboss.as.test.clustering.ClusteringTestConstants.CONTAINER_2;
import static org.jboss.as.test.clustering.ClusteringTestConstants.DEPLOYMENT_1;
import static org.jboss.as.test.clustering.ClusteringTestConstants.DEPLOYMENT_2;
import static org.jboss.as.test.clustering.ClusteringTestConstants.NODES;
import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import javax.naming.NamingException;

import org.jboss.arquillian.container.test.api.ContainerController;
import org.jboss.arquillian.container.test.api.Deployer;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.container.test.api.TargetsContainer;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.as.test.clustering.EJBClientContextSelector;
import org.jboss.as.test.clustering.NodeNameGetter;
import org.jboss.as.test.clustering.ejb.EJBDirectory;
import org.jboss.as.test.clustering.ejb.RemoteEJBDirectory;
import org.jboss.ejb.client.ContextSelector;
import org.jboss.ejb.client.EJBClientContext;
import org.jboss.logging.Logger;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * EJB2 stateless bean - basic cluster tests - failover and load balancing.
 *
 * @author Paul Ferraro
 * @author Ondrej Chaloupka
 * @see org.jboss.as.test.clustering.cluster.ejb3.stateless.RemoteStatelessFailoverTestCase
 */
@RunWith(Arquillian.class)
@RunAsClient
public class RemoteStatelessFailoverTestCase {
    private static final Logger log = Logger.getLogger(RemoteStatelessFailoverTestCase.class);
    private static final String CLIENT_PROPERTIES = "cluster/ejb3/stateless/jboss-ejb-client.properties";
    private static EJBDirectory directoryAnnotation;
    private static EJBDirectory directoryDD;

    private static final String ARCHIVE_NAME = "stateless-ejb2-failover-test";
    private static final String ARCHIVE_NAME_DD = "stateless-ejb2-failover-dd-test";

    private static final Integer PORT_2 = 4547;
    private static final String HOST_2 = System.getProperty("node1");
    private static final String REMOTE_PORT_PROPERTY_NAME = "remote.connection.default.port";
    private static final String REMOTE_HOST_PROPERTY_NAME = "remote.connection.default.host";

    private static final String DEPLOYMENT_1_DD = DEPLOYMENT_1 + "-descriptor";
    private static final String DEPLOYMENT_2_DD = DEPLOYMENT_2 + "-descriptor";

    private static final Map<String, Boolean> deployed = new HashMap<String, Boolean>();
    private static final Map<String, Boolean> started = new HashMap<String, Boolean>();
    private static final Map<String, List<String>> container2deployment = new HashMap<String, List<String>>();

    @BeforeClass
    public static void init() throws NamingException {
        directoryAnnotation = new RemoteEJBDirectory(ARCHIVE_NAME);
        directoryDD = new RemoteEJBDirectory(ARCHIVE_NAME_DD);

        deployed.put(DEPLOYMENT_1, false);
        deployed.put(DEPLOYMENT_2, false);
        deployed.put(DEPLOYMENT_1_DD, false);
        deployed.put(DEPLOYMENT_2_DD, false);
        started.put(CONTAINER_1, false);
        started.put(CONTAINER_2, false);

        List<String> deployments1 = new ArrayList<String>();
        deployments1.add(DEPLOYMENT_1);
        deployments1.add(DEPLOYMENT_1_DD);
        container2deployment.put(CONTAINER_1, deployments1);
        List<String> deployments2 = new ArrayList<String>();
        deployments2.add(DEPLOYMENT_2);
        deployments2.add(DEPLOYMENT_2_DD);
        container2deployment.put(CONTAINER_2, deployments2);
    }

    @AfterClass
    public static void destroy() throws NamingException {
        directoryAnnotation.close();
        directoryDD.close();
    }

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

    @Deployment(name = DEPLOYMENT_1_DD, managed = false, testable = false)
    @TargetsContainer(CONTAINER_1)
    public static Archive<?> createDeploymentOnDescriptorForContainer1() {
        return createDeploymentOnDescriptor();
    }

    @Deployment(name = DEPLOYMENT_2_DD, managed = false, testable = false)
    @TargetsContainer(CONTAINER_2)
    public static Archive<?> createDeploymentOnDescriptorForContainer2() {
        return createDeploymentOnDescriptor();
    }

    private static Archive<?> createDeployment() {
        final JavaArchive jar = ShrinkWrap.create(JavaArchive.class, ARCHIVE_NAME + ".jar");
        jar.addClasses(StatelessBeanBase.class, StatelessBean.class, StatelessRemote.class, StatelessRemoteHome.class);
        jar.addClass(NodeNameGetter.class);
        log.info(jar.toString(true));
        return jar;
    }

    private static Archive<?> createDeploymentOnDescriptor() {
        final JavaArchive jar = ShrinkWrap.create(JavaArchive.class, ARCHIVE_NAME_DD + ".jar");
        jar.addClasses(StatelessBeanBase.class, StatelessBeanDD.class, StatelessRemote.class, StatelessRemoteHome.class);
        jar.addClass(NodeNameGetter.class);
        jar.addAsManifestResource(RemoteStatelessFailoverTestCase.class.getPackage(), "ejb-jar.xml", "ejb-jar.xml");
        jar.addAsManifestResource(RemoteStatelessFailoverTestCase.class.getPackage(), "jboss-ejb3.xml", "jboss-ejb3.xml");
        log.info(jar.toString(true));
        return jar;
    }

    @Test
    public void testFailoverOnStopAnnotatedBean() throws Exception {
        doFailover(true, directoryAnnotation, DEPLOYMENT_1, DEPLOYMENT_2);
    }

    @Test
    public void testFailoverOnStopBeanSpecifiedByDescriptor() throws Exception {
        doFailover(true, directoryDD, DEPLOYMENT_1_DD, DEPLOYMENT_2_DD);
    }

    @Test
    public void testFailoverOnUndeployAnnotatedBean() throws Exception {
        doFailover(false, directoryAnnotation, DEPLOYMENT_1, DEPLOYMENT_2);
    }

    @Test
    public void testFailoverOnUndeploySpecifiedByDescriptor() throws Exception {
        doFailover(false, directoryDD, DEPLOYMENT_1_DD, DEPLOYMENT_2_DD);
    }

    private void doFailover(boolean isStop, EJBDirectory directory, String deployment1, String deployment2) throws Exception {
        this.start(CONTAINER_1);
        this.deploy(CONTAINER_1, deployment1);

        final ContextSelector<EJBClientContext> selector = EJBClientContextSelector.setup(CLIENT_PROPERTIES);

        try {
            StatelessRemoteHome home = directory.lookupHome(StatelessBean.class, StatelessRemoteHome.class);
            StatelessRemote bean = home.create();

            assertEquals("The only " + NODES[0] + " is active. Bean had to be invoked on it but it wasn't.", NODES[0], bean.getNodeName());

            this.start(CONTAINER_2);
            this.deploy(CONTAINER_2, deployment2);

            if (isStop) {
                this.stop(CONTAINER_1);
            } else {
                this.undeploy(CONTAINER_1, deployment1);
            }

            assertEquals("Only " + NODES[1] + " is active. The bean had to be invoked on it but it wasn't.", NODES[1], bean.getNodeName());
        } finally {
            // reset the selector
            if (selector != null) {
                EJBClientContext.setSelector(selector);
            }
            // need to have the container started to undeploy deployment afterwards
            this.start(CONTAINER_1);
            // shutdown the containers
            undeployAll();
            shutdownAll();
        }
    }

    @Test
    public void testLoadbalanceAnnotatedBean() throws Exception {
        loadbalance(directoryAnnotation, DEPLOYMENT_1, DEPLOYMENT_2);
    }

    @Test
    public void testLoadbalanceSpecifiedByDescriptor() throws Exception {
        loadbalance(directoryDD, DEPLOYMENT_1_DD, DEPLOYMENT_2_DD);
    }

    /**
     * Basic load balance testing. A random distribution is used amongst nodes for client now.
     */
    private void loadbalance(EJBDirectory directory, String deployment1, String deployment2) throws Exception {
        this.start(CONTAINER_1);
        this.deploy(CONTAINER_1, deployment1);
        this.start(CONTAINER_2);
        this.deploy(CONTAINER_2, deployment2);

        final ContextSelector<EJBClientContext> previousSelector = EJBClientContextSelector.setup(CLIENT_PROPERTIES);

        int numberOfServers = 2;
        int numberOfCalls = 40;
        // there will be at least 20% of calls processed by all servers
        double serversProccessedAtLeast = 0.2;

        try {
            StatelessRemoteHome home = directory.lookupHome(StatelessBean.class, StatelessRemoteHome.class);
            StatelessRemote bean = home.create();

            String node = bean.getNodeName();
            log.info("Node called : " + node);

            validateBalancing(bean, numberOfCalls, numberOfServers, serversProccessedAtLeast);

            Properties contextChangeProperties = new Properties();
            contextChangeProperties.put(REMOTE_PORT_PROPERTY_NAME, PORT_2.toString());
            contextChangeProperties.put(REMOTE_HOST_PROPERTY_NAME, HOST_2.toString());
            EJBClientContextSelector.setup(CLIENT_PROPERTIES, contextChangeProperties);

            bean = home.create();
            node = bean.getNodeName();
            log.info("Node called : " + node);

            validateBalancing(bean, numberOfCalls, numberOfServers, serversProccessedAtLeast);
        } finally {
            // reset the selector
            if (previousSelector != null) {
                EJBClientContext.setSelector(previousSelector);
            }
            // undeploy&shutdown the containers
            undeployAll();
            shutdownAll();
        }
    }

    /**
     * Method calls the bean function getNodeName() {numCalls} times and checks whether all servers processed at least part of calls.
     * The necessary number of processed calls by each server is {minPercentage} of the number of all calls.
     */
    private static void validateBalancing(StatelessRemote bean, int numCalls, int expectedServers, double minPercentage) {
        Map<String, Integer> callCount = new HashMap<String, Integer>();
        int maxNumOfProcessedCalls = -1;
        int minNumOfProcessedCalls = Integer.MAX_VALUE;

        for (int i = 0; i < numCalls; i++) {
            String nodeName = bean.getNodeName();

            Integer count = callCount.get(nodeName);
            count = count == null ? 1 : ++count;
            callCount.put(nodeName, count);
        }
        Assert.assertEquals("It was running " + expectedServers + " servers but not all of them were used for loadbalancing.",
                expectedServers, callCount.size());

        for (Integer count : callCount.values()) {
            maxNumOfProcessedCalls = count > maxNumOfProcessedCalls ? count : maxNumOfProcessedCalls;
            minNumOfProcessedCalls = count < minNumOfProcessedCalls ? count : minNumOfProcessedCalls;
        }
        Assert.assertTrue("Minimal number of calls done to all servers have to be " + minPercentage * numCalls + " but was " + minNumOfProcessedCalls,
                minPercentage * numCalls <= minNumOfProcessedCalls);
        log.info("All " + expectedServers + " servers processed at least " + minNumOfProcessedCalls + " of calls");
    }

    private void undeployAll() {
        for (String container : container2deployment.keySet()) {
            for (String deployment : container2deployment.get(container)) {
                undeploy(container, deployment);
            }
        }
    }

    private void shutdownAll() {
        for (String container : container2deployment.keySet()) {
            stop(container);
        }
    }

    private void deploy(String container, String deployment) {
        if (started.get(container) && !deployed.get(deployment)) {
            this.deployer.deploy(deployment);
            deployed.put(deployment, true);
        }
    }

    private void undeploy(String container, String deployment) {
        if (started.get(container) && deployed.get(deployment)) {
            this.deployer.undeploy(deployment);
            deployed.put(deployment, false);
        }
    }

    private void start(String container) {
        if (!started.get(container)) {
            this.container.start(container);
            started.put(container, true);
        }
    }

    private void stop(String container) {
        if (started.get(container)) {
            this.container.stop(container);
            started.put(container, false);
        }
    }
}
