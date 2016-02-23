/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2015, Red Hat, Inc., and individual contributors
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
package org.jboss.as.test.clustering.tunnel.singleton;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;

import javax.servlet.http.HttpServletResponse;

import org.apache.http.Header;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.utils.HttpClientUtils;
import org.apache.http.impl.client.CloseableHttpClient;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.OperateOnDeployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.container.test.api.TargetsContainer;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.as.test.clustering.ClusterHttpClientUtil;
import org.jboss.as.test.clustering.ClusterTestUtil;
import org.jboss.as.test.clustering.cluster.ClusterAbstractTestCase;
import org.jboss.as.test.clustering.cluster.singleton.service.MyService;
import org.jboss.as.test.clustering.cluster.singleton.service.MyServiceServlet;
import org.jboss.msc.service.ServiceActivator;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.jgroups.stack.GossipRouter;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Test case for BZ-1190029.
 * <p/>
 * This test creates a cluster of two nodes running two singleton services. Communication between the clusters is then
 * disabled, so the cluster splits, and then enabled again so the two partition merges. After the merge, each service
 * is supposed to have only one provider.
 *
 * @author Tomas Hofman
 */
@RunWith(Arquillian.class)
@RunAsClient
public class SingletonTunnelTestCase extends ClusterAbstractTestCase {

    private static final long TOPOLOGY_CHANGE_TIMEOUT = 75000; // maximum time in ms to wait for cluster topology change
    private static final String CONTAINER = "singleton";

    @Deployment(name = DEPLOYMENT_1, managed = false, testable = false)
    @TargetsContainer(CONTAINER_1)
    public static Archive<?> deployment0() {
        return createDeployment();
    }

    @Deployment(name = DEPLOYMENT_2, managed = false, testable = false)
    @TargetsContainer(CONTAINER_2)
    public static Archive<?> deployment1() {
        return createDeployment();
    }

    private static GossipRouter gossipRouter = null;

    private static Archive<?> createDeployment() {
        WebArchive war = ShrinkWrap.create(WebArchive.class, "singleton.war");
        war.addPackage(MyService.class.getPackage());
        war.addClass(SingletonServiceActivator.class);
        war.addAsServiceProvider(ServiceActivator.class, SingletonServiceActivator.class);
        ClusterTestUtil.addTopologyListenerDependencies(war);
        return war;
    }

    @Override
    @Before
    @RunAsClient
    public void beforeTestMethod() {
        startGossipRouter();
        super.beforeTestMethod();
    }

    @Override
    @Before
    @RunAsClient
    public void afterTestMethod() {
        super.afterTestMethod();
        stopGossipRouter();
    }

    @Test
    public void testSingletonService(
            @ArquillianResource() @OperateOnDeployment(DEPLOYMENT_1) URL baseURL1,
            @ArquillianResource() @OperateOnDeployment(DEPLOYMENT_2) URL baseURL2)
            throws IOException, URISyntaxException {

        // URLs look like "http://IP:PORT/singleton/service"
        URI serviceANode1Uri = MyServiceServlet.createURI(baseURL1, SingletonServiceActivator.SERVICE_A_NAME);
        URI serviceANode2Uri = MyServiceServlet.createURI(baseURL2, SingletonServiceActivator.SERVICE_A_NAME);

        URI serviceBNode1Uri = MyServiceServlet.createURI(baseURL1, SingletonServiceActivator.SERVICE_B_NAME);
        URI serviceBNode2Uri = MyServiceServlet.createURI(baseURL2, SingletonServiceActivator.SERVICE_B_NAME);

        log.info("URLs are:\n" + serviceANode1Uri
                + "\n" + serviceANode2Uri
                + "\n" + serviceBNode1Uri
                + "\n" + serviceBNode2Uri);

        // 1. Gossip router is started, both services should have a single provider

        waitForView(baseURL1, NODE_1, NODE_2);

        // check service A
        checkSingletonNode(serviceANode1Uri, SingletonServiceActivator.SERVICE_A_PREFERRED_NODE);
        checkSingletonNode(serviceANode2Uri, SingletonServiceActivator.SERVICE_A_PREFERRED_NODE);
        // check service B
        checkSingletonNode(serviceBNode1Uri, SingletonServiceActivator.SERVICE_B_PREFERRED_NODE);
        checkSingletonNode(serviceBNode2Uri, SingletonServiceActivator.SERVICE_B_PREFERRED_NODE);


        // 2. Stop gossip router, cluster splits and each partition should have it's own provider

        stopGossipRouter();
        log.info("Waiting for establishing a view.");

        waitForView(baseURL1, NODE_1);
        waitForView(baseURL2, NODE_2);

        // check service A
        checkSingletonNode(serviceANode1Uri, NODE_1);
        checkSingletonNode(serviceANode2Uri, NODE_2);
        // check service B
        checkSingletonNode(serviceBNode1Uri, NODE_1);
        checkSingletonNode(serviceBNode2Uri, NODE_2);


        // 3. Start gossip router. After merge, each service is supposed to have a single provider again.

        startGossipRouter();

        waitForView(baseURL1, NODE_1, NODE_2);

        // check service A
        checkSingletonNode(serviceANode1Uri, SingletonServiceActivator.SERVICE_A_PREFERRED_NODE);
        checkSingletonNode(serviceANode2Uri, SingletonServiceActivator.SERVICE_A_PREFERRED_NODE);
        // check service B
        checkSingletonNode(serviceBNode1Uri, SingletonServiceActivator.SERVICE_B_PREFERRED_NODE);
        checkSingletonNode(serviceBNode2Uri, SingletonServiceActivator.SERVICE_B_PREFERRED_NODE);


        // 4. Stop gossip router again. Each node should start the service again. This verifies WFLY-4748.

        stopGossipRouter();

        waitForView(baseURL1, NODE_1);
        waitForView(baseURL2, NODE_2);

        // check service A
        checkSingletonNode(serviceANode1Uri, NODE_1);
        checkSingletonNode(serviceANode2Uri, NODE_2);
        // check service B
        checkSingletonNode(serviceBNode1Uri, NODE_1);
        checkSingletonNode(serviceBNode2Uri, NODE_2);
    }

    private static void checkSingletonNode(URI serviceUri, String expectedProviderNode) throws IOException {
        String node = getSingletonNode(serviceUri);
        Assert.assertEquals("Expected different provider node.", expectedProviderNode, node);
    }

    private static String getSingletonNode(URI serviceUri) throws IOException {
        CloseableHttpClient client = org.jboss.as.test.http.util.TestHttpClientUtils.promiscuousCookieHttpClient();
        HttpResponse response = client.execute(new HttpGet(serviceUri));
        try {
            Assert.assertEquals(HttpServletResponse.SC_OK, response.getStatusLine().getStatusCode());
            Header header = response.getFirstHeader("node");
            Assert.assertNotNull("No provider detected.", header);
            return header.getValue();
        } finally {
            HttpClientUtils.closeQuietly(response);
            HttpClientUtils.closeQuietly(client);
        }
    }

    private static void waitForView(URL baseURL, String... members) throws IOException, URISyntaxException {
        ClusterHttpClientUtil.establishTopology(baseURL, CONTAINER, "default", TOPOLOGY_CHANGE_TIMEOUT, members);
        try {
            Thread.sleep(5000); // it takes a little extra time after merge for the singleton service to migrate
        } catch (InterruptedException e) {
            Assert.fail("Interrupted.");
        }
    }

    private static void startGossipRouter() {
        log.info("Starting gossip router.");
        try {
            String address = System.getProperty("node0");
            if (address == null || address.trim().isEmpty()) {
                address = "127.0.0.1";
            }
            if (gossipRouter == null) {
                log.info("Assigning address " + address + " to gossip router.");
                gossipRouter = new GossipRouter(address, 12001);

            }
            gossipRouter.start();
        } catch (Exception e) {
            log.error("Caught exception: ", e);
            Assert.fail("Couldn't start GossipRouter: " + e.getMessage());
        }
    }

    private static void stopGossipRouter() {
        log.info("Stopping gossip router.");
        if (gossipRouter != null) {
            gossipRouter.stop();
        }
    }
}
