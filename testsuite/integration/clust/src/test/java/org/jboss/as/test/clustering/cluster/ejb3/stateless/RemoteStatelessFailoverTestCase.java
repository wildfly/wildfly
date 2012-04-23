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

package org.jboss.as.test.clustering.cluster.ejb3.stateless;

import static org.jboss.as.test.clustering.ClusteringTestConstants.CONTAINERS;
import static org.jboss.as.test.clustering.ClusteringTestConstants.CONTAINER_1;
import static org.jboss.as.test.clustering.ClusteringTestConstants.CONTAINER_2;
import static org.jboss.as.test.clustering.ClusteringTestConstants.DEPLOYMENTS;
import static org.jboss.as.test.clustering.ClusteringTestConstants.DEPLOYMENT_1;
import static org.jboss.as.test.clustering.ClusteringTestConstants.DEPLOYMENT_2;
import static org.jboss.as.test.clustering.ClusteringTestConstants.GRACE_TIME_TO_MEMBERSHIP_CHANGE;
import static org.jboss.as.test.clustering.ClusteringTestConstants.NODES;
import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import org.jboss.arquillian.container.test.api.ContainerController;
import org.jboss.arquillian.container.test.api.Deployer;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.container.test.api.TargetsContainer;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.junit.InSequence;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.as.test.clustering.EJBClientContextSelector;
import org.jboss.as.test.clustering.EJBDirectory;
import org.jboss.as.test.clustering.NodeNameGetter;
import org.jboss.as.test.clustering.RemoteEJBDirectory;
import org.jboss.as.test.clustering.cluster.ClusterAbstractTestCase;
import org.jboss.as.test.clustering.cluster.ejb3.stateless.bean.Stateless;
import org.jboss.as.test.clustering.cluster.ejb3.stateless.bean.StatelessBean;
import org.jboss.ejb.client.ContextSelector;
import org.jboss.ejb.client.EJBClientContext;
import org.jboss.logging.Logger;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * @author Paul Ferraro
 * @author Ondrej Chaloupka
 * @version Oct 2012
 */
@RunWith(Arquillian.class)
@RunAsClient
public class RemoteStatelessFailoverTestCase extends ClusterAbstractTestCase {
    private static final Logger log = Logger.getLogger(RemoteStatelessFailoverTestCase.class);
    private static final String MODULE_NAME = "remote-ejb-client-stateless-bean-failover-test";
    private static final String CLIENT_PROPERTIES = "cluster/ejb3/stateless/jboss-ejb-client.properties";
    private static EJBDirectory context;

    private static final Integer PORT_2 = 4547;
    private static final String HOST_2 = System.getProperty("node1");
    private static final String REMOTE_PORT_PROPERTY_NAME = "remote.connection.default.port";
    private static final String REMOTE_HOST_PROPERTY_NAME = "remote.connection.default.host";

    private boolean[] deployed = new boolean[] { false, false };
    private boolean[] started = new boolean[] { false, false };

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
        ejbJar.addPackage(StatelessBean.class.getPackage());
        ejbJar.addClass(NodeNameGetter.class);
        log.info(ejbJar.toString(true));
        return ejbJar;
    }

    @BeforeClass
    public static void beforeClass() throws Exception {
        context = new RemoteEJBDirectory(MODULE_NAME);
    }

    @Override
    protected void setUp() {
        stop(CONTAINER_2);

        // Make sure only one container is running now.
        start(CONTAINER_1);
        deploy(DEPLOYMENT_1);
    }

    @Test
    @InSequence(1)
    public void testFailoverOnStop() throws Exception {

        // In case something went wrong.
        start(CONTAINER_1);
        deploy(DEPLOYMENT_1);

        final ContextSelector<EJBClientContext> selector = EJBClientContextSelector.setup(CLIENT_PROPERTIES);

        try {
            Stateless bean = context.lookupStateless(StatelessBean.class, Stateless.class);

            assertEquals(NODES[0], bean.getNodeName());

            start(CONTAINER_2);
            deploy(DEPLOYMENT_2);

            // Allow ample time for topology change to propagate to client
            Thread.sleep(GRACE_TIME_TO_MEMBERSHIP_CHANGE);

            List<String> results = new ArrayList<String>(10);
            for (int i = 0; i < 10; ++i) {
                results.add(bean.getNodeName());
            }

            for (int i = 0; i < NODES.length; ++i) {
                int frequency = Collections.frequency(results, NODES[i]);
                Assert.assertTrue(String.valueOf(frequency), frequency > 0);
            }

            stop(CONTAINER_1);

            assertEquals(NODES[1], bean.getNodeName());
        } finally {
            // reset the selector
            if (selector != null) {
                EJBClientContext.setSelector(selector);
            }
            // shutdown the 2nd container
            stop(CONTAINER_2);
        }
    }

    @Test
    @InSequence(2)
    public void testFailoverOnUndeploy() throws Exception {

        // In case the previous test failed.
        start(CONTAINER_1);
        deploy(DEPLOYMENT_1);

        final ContextSelector<EJBClientContext> selector = EJBClientContextSelector.setup(CLIENT_PROPERTIES);

        try {
            Stateless bean = context.lookupStateless(StatelessBean.class, Stateless.class);

            assertEquals(NODES[0], bean.getNodeName());

            start(CONTAINER_2);
            deploy(DEPLOYMENT_2); // TODO: Should be still deployed.

            // Allow ample time for topology change to propagate to client
            Thread.sleep(GRACE_TIME_TO_MEMBERSHIP_CHANGE);

            List<String> results = new ArrayList<String>(10);
            for (int i = 0; i < 10; ++i) {
                results.add(bean.getNodeName());
            }

            for (int i = 0; i < NODES.length; ++i) {
                int frequency = Collections.frequency(results, NODES[i]);
                Assert.assertTrue(String.valueOf(frequency), frequency > 0);
            }

            undeploy(DEPLOYMENT_1);

            assertEquals(NODES[1], bean.getNodeName());
        } finally {
            // reset the selector
            if (selector != null) {
                EJBClientContext.setSelector(selector);
            }

            // Keep containers running
        }
    }

    /**
     * Basic load balance testing. A random distribution is used amongst nodes for client now.
     */
    @Test
    @InSequence(3)
    public void testLoadBalance() throws Exception {

        // In case the previous test failed.
        start(CONTAINER_1);
        deploy(DEPLOYMENT_1);
        start(CONTAINER_2);
        deploy(DEPLOYMENT_2);

        final ContextSelector<EJBClientContext> previousSelector = EJBClientContextSelector.setup(CLIENT_PROPERTIES);

        int numberOfServers = 2;
        int numberOfCalls = 50;
        // there will be at least 20% of calls processed by all servers
        double serversProcessedAtLeast = 0.2;

        try {
            Stateless bean = context.lookupStateless(StatelessBean.class, Stateless.class);

            String node = bean.getNodeName();
            log.info("Node called : " + node);

            validateBalancing(bean, numberOfCalls, numberOfServers, serversProcessedAtLeast);

            Properties contextChangeProperties = new Properties();
            contextChangeProperties.put(REMOTE_PORT_PROPERTY_NAME, PORT_2.toString());
            contextChangeProperties.put(REMOTE_HOST_PROPERTY_NAME, HOST_2.toString());
            EJBClientContextSelector.setup(CLIENT_PROPERTIES, contextChangeProperties);

            bean = context.lookupStateless(StatelessBean.class, Stateless.class);
            node = bean.getNodeName();
            log.info("Node called : " + node);

            validateBalancing(bean, numberOfCalls, numberOfServers, serversProcessedAtLeast);

            stop(CONTAINER_1);
            node = bean.getNodeName();
            log.info("Node called : " + node);

            start(CONTAINER_1);
            node = bean.getNodeName();
            log.info("Node called : " + node);

            validateBalancing(bean, numberOfCalls, numberOfServers, serversProcessedAtLeast);
        } finally {
            // reset the selector
            if (previousSelector != null) {
                EJBClientContext.setSelector(previousSelector);
            }
        }
    }

    /**
     * Method calls the bean function getNodeName() {numCalls} times and checks whether all servers processed at least part of calls.
     * The necessary number of processed calls by each server is {minPercentage} of the number of all calls.
     */
    private void validateBalancing(Stateless bean, int numCalls, int expectedServers, double minPercentage) {
        List<String> results = new ArrayList<String>(numCalls);
        for (int i = 0; i < numCalls; i++) {
            results.add(bean.getNodeName());
        }

        Set<String> entries = new HashSet<String>();
        entries.addAll(results);

        Assert.assertEquals(expectedServers, entries.size());

        double minCalls = minPercentage * numCalls;
        for (String entry: entries) {
            int frequency = Collections.frequency(results, entry);
            Assert.assertTrue(Integer.toString(frequency), frequency >= minCalls);
        }
        System.out.println(String.format("All %d servers processed at least %f of calls", expectedServers, minCalls));
    }

}
