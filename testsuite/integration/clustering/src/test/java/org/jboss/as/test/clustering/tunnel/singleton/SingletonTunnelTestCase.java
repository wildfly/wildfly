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
package org.jboss.as.test.clustering.tunnel.singleton;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Arrays;

import javax.servlet.http.HttpServletResponse;

import org.apache.http.HttpResponse;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.utils.HttpClientUtils;
import org.apache.http.impl.client.DefaultHttpClient;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.OperateOnDeployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.container.test.api.TargetsContainer;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.as.test.clustering.CurrentTopology;
import org.jboss.as.test.clustering.CurrentTopologyBean;
import org.jboss.as.test.clustering.CurrentTopologyServlet;
import org.jboss.as.test.clustering.cluster.ClusterAbstractTestCase;
import org.jboss.as.test.clustering.cluster.singleton.service.MyService;
import org.jboss.as.test.clustering.cluster.singleton.service.MyServiceActivator;
import org.jboss.as.test.clustering.cluster.singleton.service.MyServiceServlet;
import org.jboss.as.test.clustering.tunnel.singleton.service.MyOtherService;
import org.jboss.as.test.clustering.tunnel.singleton.service.MyOtherServiceActivator;
import org.jboss.as.test.clustering.tunnel.singleton.service.MyOtherServiceServlet;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.StringAsset;
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
 */
@RunWith(Arquillian.class)
@RunAsClient
public class SingletonTunnelTestCase extends ClusterAbstractTestCase {

    private static final long CLUSTER_TOPOLOGY_VIEW_CHANGE_TIMEOUT = 75000; // maximum time in ms to wait for cluster topology change
    private static final String CLUSTER_NAME = "singleton";

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
        war.addPackage(MyOtherService.class.getPackage());
        war.addClasses(CurrentTopology.class, CurrentTopologyBean.class, CurrentTopologyServlet.class);
        war.setManifest(new StringAsset("Manifest-Version: 1.0\nDependencies: org.jboss.msc, org.jboss.as.clustering.common, org.infinispan, org.jboss.as.server, org.jboss.marshalling, org.jgroups\n"));
        war.addAsServiceProvider(org.jboss.msc.service.ServiceActivator.class, MyServiceActivator.class, MyOtherServiceActivator.class);
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
            throws IOException, InterruptedException, URISyntaxException {

        // URLs look like "http://IP:PORT/singleton/service"
        URI myServiceUri1 = MyServiceServlet.createURI(baseURL1, MyService.DEFAULT_SERVICE_NAME);
        URI myServiceUri2 = MyServiceServlet.createURI(baseURL2, MyService.DEFAULT_SERVICE_NAME);

        URI myOtherServiceUri1 = MyOtherServiceServlet.createURI(baseURL1);
        URI myOtherServiceUri2 = MyOtherServiceServlet.createURI(baseURL2);

        log.info("URLs are: " + myServiceUri1 + ", " + myServiceUri2);

        // 1. - Gossip router is started, each service should have a single provider

        waitForView(baseURL1, NODE_1, NODE_2);

        // check MyService

        checkSingletonNode(myServiceUri1, MyServiceActivator.PREFERRED_NODE);
        checkSingletonNode(myServiceUri2, MyServiceActivator.PREFERRED_NODE);

        // check MyOtherService

        checkSingletonNode(myOtherServiceUri1, MyOtherServiceActivator.PREFERRED_NODE);
        checkSingletonNode(myOtherServiceUri2, MyOtherServiceActivator.PREFERRED_NODE);


        // 2. - Stop gossip router, cluster splits and each partition should have it's own provider

        stopGossipRouter();
        log.info("Waiting for establishing a view.");

        waitForView(baseURL1, NODE_1);
        waitForView(baseURL2, NODE_2);

        // check MyService

        checkSingletonNode(myServiceUri1, NODE_1);
        checkSingletonNode(myServiceUri2, NODE_2);

        // check MyOtherService

        checkSingletonNode(myOtherServiceUri1, NODE_1);
        checkSingletonNode(myOtherServiceUri2, NODE_2);


        // 3. - Start gossip router again. After merge, each service is supposed to have a single provider.

        startGossipRouter();

        waitForView(baseURL1, NODE_1, NODE_2);

        String provider1;
        String provider2;

        // check MyService

        provider1 = getSingletonNode(myServiceUri1);
        provider2 = getSingletonNode(myServiceUri2);

        Assert.assertEquals("Partition merge failed - two singletons detected for service MyService.", provider2, provider1);

        // check MyOtherService

        provider1 = getSingletonNode(myOtherServiceUri1);
        provider2 = getSingletonNode(myOtherServiceUri2);

        Assert.assertEquals("Partition merge failed - two singletons detected for service MyOtherService.", provider1, provider2);
    }

    private void checkSingletonNode(URI serviceUri, String expectedNode) throws IOException {
        String node = getSingletonNode(serviceUri);
        Assert.assertEquals(expectedNode, node);
    }

    private String getSingletonNode(URI serviceUri) throws IOException {
        DefaultHttpClient client = org.jboss.as.test.http.util.HttpClientUtils.relaxedCookieHttpClient();
        HttpResponse response = client.execute(new HttpGet(serviceUri));
        try {
            Assert.assertEquals(HttpServletResponse.SC_OK, response.getStatusLine().getStatusCode());
            return response.getFirstHeader("node").getValue();
        } finally {
            HttpClientUtils.closeQuietly(response);
            HttpClientUtils.closeQuietly(client);
        }
    }

    private void waitForView(URL baseURL, String... members) throws IOException, URISyntaxException {
        URI uri = CurrentTopologyServlet.createURI(baseURL, CLUSTER_NAME);
        String[] currentMembers;

        Arrays.sort(members);

        log.info("Waiting for view " + Arrays.toString(members));
        long start = System.currentTimeMillis();
        while (true) {
            String response = readUri(uri);
            currentMembers = response.split(",");
            Arrays.sort(currentMembers);

            if (Arrays.equals(currentMembers, members)) {
                log.info("View " + Arrays.toString(members) + " formed in " + (System.currentTimeMillis() - start) + " ms.");
                break;
            }

            if (System.currentTimeMillis() - start > CLUSTER_TOPOLOGY_VIEW_CHANGE_TIMEOUT) {
                Assert.fail("Cluster topology change timed out. Current members: {" + Arrays.toString(currentMembers)
                        + "} Wanted members: {" + Arrays.toString(members) + "}");
            }

            try {
                Thread.sleep(5000);
            } catch (InterruptedException e) {
                Assert.fail("Interrupted.");
            }
        }
    }

    private String readUri(URI uri) throws IOException {
        DefaultHttpClient client = org.jboss.as.test.http.util.HttpClientUtils.relaxedCookieHttpClient();
        try {
            CloseableHttpResponse response = client.execute(new HttpGet(uri));
            Assert.assertEquals(200, response.getStatusLine().getStatusCode());

            // read content
            StringBuilder sb = new StringBuilder();
            try (BufferedReader br = new BufferedReader(new InputStreamReader(response.getEntity().getContent()), 4096)) {
                String line;
                while ((line = br.readLine()) != null) {
                    sb.append(line);
                }
            }
            return sb.toString();
        } finally {
            HttpClientUtils.closeQuietly(client);
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
                gossipRouter = new GossipRouter(12001, address, false);

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
