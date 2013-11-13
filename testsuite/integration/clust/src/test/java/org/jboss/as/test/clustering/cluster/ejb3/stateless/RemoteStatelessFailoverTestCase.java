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

import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Properties;
import java.util.Set;

import javax.naming.NamingException;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.container.test.api.TargetsContainer;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.as.test.clustering.EJBClientContextSelector;
import org.jboss.as.test.clustering.EJBDirectory;
import org.jboss.as.test.clustering.NodeNameGetter;
import org.jboss.as.test.clustering.RemoteEJBDirectory;
import org.jboss.as.test.clustering.cluster.ClusterAbstractTestCase;
import org.jboss.as.test.clustering.cluster.ejb3.stateless.bean.Stateless;
import org.jboss.as.test.clustering.cluster.ejb3.stateless.bean.StatelessBean;
import org.jboss.ejb.client.EJBClientContext;
import org.jboss.logging.Logger;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.AfterClass;
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
@Ignore("WFLY-2298")
public class RemoteStatelessFailoverTestCase extends ClusterAbstractTestCase {
    private static final Logger log = Logger.getLogger(RemoteStatelessFailoverTestCase.class);
    private static final String MODULE_NAME = "remote-ejb-client-stateless-bean-failover-test";
    private static final String CLIENT_PROPERTIES = "cluster/ejb3/stateless/jboss-ejb-client.properties";
    private static EJBDirectory context;

    // TODO: remove hardcoded stuff :-(
    private static final Integer PORT_2 = 8180;
    private static final String HOST_2 = System.getProperty("node1");
    private static final String REMOTE_PORT_PROPERTY_NAME = "remote.connection.default.port";
    private static final String REMOTE_HOST_PROPERTY_NAME = "remote.connection.default.host";

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
    public static void beforeClass() throws NamingException {
        context = new RemoteEJBDirectory(MODULE_NAME);
    }

    @AfterClass
    public static void destroy() throws NamingException {
        context.close();
    }

    @Test
    public void testFailoverOnStop() throws Exception {

        stop(CONTAINER_2);

        EJBClientContextSelector.setup(CLIENT_PROPERTIES);

        try {
            Stateless bean = context.lookupStateless(StatelessBean.class, Stateless.class);

            assertEquals(NODES[0], bean.getNodeName());

            start(CONTAINER_2);

            List<String> results = new ArrayList<String>(10);
            for (int i = 0; i < 10; ++i) {
                results.add(bean.getNodeName());
            }

            for (String NODE : NODES) {
                int frequency = Collections.frequency(results, NODE);
                Assert.assertTrue(String.valueOf(frequency), frequency > 0);
            }

            stop(CONTAINER_1);

            assertEquals(NODES[1], bean.getNodeName());
        } finally {
            EJBClientContext.getCurrent().close();
        }
    }

    @Test
    public void testFailoverOnUndeploy() throws Exception {

        undeploy(DEPLOYMENT_2);

        EJBClientContextSelector.setup(CLIENT_PROPERTIES);

        try {
            Stateless bean = context.lookupStateless(StatelessBean.class, Stateless.class);

            assertEquals(NODES[0], bean.getNodeName());

            deploy(DEPLOYMENT_2);

            List<String> results = new ArrayList<String>(10);
            for (int i = 0; i < 10; ++i) {
                results.add(bean.getNodeName());
            }

            for (String NODE : NODES) {
                int frequency = Collections.frequency(results, NODE);
                Assert.assertTrue(String.valueOf(frequency), frequency > 0);
            }

            undeploy(DEPLOYMENT_1);

            assertEquals(NODES[1], bean.getNodeName());
        } finally {
            EJBClientContext.getCurrent().close();
        }
    }

    /**
     * Basic load balance testing. A random distribution is used amongst nodes for client now.
     */
    @Test
    public void testLoadBalance() throws Exception {

        EJBClientContextSelector.setup(CLIENT_PROPERTIES);

        int numberOfServers = 2;
        int numberOfCalls = 50;
        // there will be at least 20% of calls processed by all servers
        double serversProcessedAtLeast = 0.2;

        try {
            Stateless bean = context.lookupStateless(StatelessBean.class, Stateless.class);

            String node = bean.getNodeName();
            log.info("Node called: " + node);

            validateBalancing(bean, numberOfCalls, numberOfServers, serversProcessedAtLeast);

            Properties contextChangeProperties = new Properties();
            contextChangeProperties.put(REMOTE_PORT_PROPERTY_NAME, PORT_2.toString());
            contextChangeProperties.put(REMOTE_HOST_PROPERTY_NAME, HOST_2.toString());
            EJBClientContextSelector.setup(CLIENT_PROPERTIES, contextChangeProperties);

            bean = context.lookupStateless(StatelessBean.class, Stateless.class);
            node = bean.getNodeName();
            log.info("Node called: " + node);

            validateBalancing(bean, numberOfCalls, numberOfServers, serversProcessedAtLeast);

            stop(CONTAINER_1);

            node = bean.getNodeName();
            log.info("Node called: " + node);

            start(CONTAINER_1);

            node = bean.getNodeName();
            log.info("Node called: " + node);

            validateBalancing(bean, numberOfCalls, numberOfServers, serversProcessedAtLeast);
        } finally {
            EJBClientContext.getCurrent().close();
        }
    }

    /**
     * Method calls the bean function getNodeName() {numCalls} times and checks whether all servers processed at least part of calls.
     * The necessary number of processed calls by each server is {minPercentage} of the number of all calls.
     */
    private static void validateBalancing(Stateless bean, int numCalls, int expectedServers, double minPercentage) {
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
        log.info(String.format("All %d servers processed at least %f of calls", expectedServers, minCalls));
    }
}
