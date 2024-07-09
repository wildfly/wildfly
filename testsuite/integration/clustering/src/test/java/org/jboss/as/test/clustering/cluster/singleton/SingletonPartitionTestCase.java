/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.clustering.cluster.singleton;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Arrays;

import jakarta.servlet.http.HttpServletResponse;

import org.apache.http.Header;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.utils.HttpClientUtils;
import org.apache.http.impl.client.CloseableHttpClient;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.OperateOnDeployment;
import org.jboss.arquillian.container.test.api.TargetsContainer;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.as.test.clustering.ClusterHttpClientUtil;
import org.jboss.as.test.clustering.ClusterTestUtil;
import org.jboss.as.test.clustering.cluster.AbstractClusteringTestCase;
import org.jboss.as.test.clustering.cluster.singleton.partition.PartitionServlet;
import org.jboss.as.test.clustering.cluster.singleton.partition.SingletonServiceActivator;
import org.jboss.as.test.clustering.cluster.singleton.service.NodeServiceExecutorRegistry;
import org.jboss.as.test.clustering.cluster.singleton.service.NodeServiceServlet;
import org.jboss.as.test.clustering.cluster.singleton.service.SingletonElectionListenerService;
import org.jboss.as.test.http.util.TestHttpClientUtils;
import org.jboss.as.test.shared.TimeoutUtil;
import org.jboss.msc.service.ServiceActivator;
import org.jboss.msc.service.ServiceName;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Test case for BZ-1190029 and https://issues.jboss.org/browse/WFLY-4748.
 * <p/>
 * This test creates a cluster of two nodes running two singleton services. Communication between the clusters is then
 * disabled (by inserting DISCARD protocol) so the cluster splits, and then enabled again so the two partitions merge.
 * After the merge, each service is supposed to have only one provider.
 *
 * @author Tomas Hofman
 * @author Radoslav Husar
 */
@RunWith(Arquillian.class)
public class SingletonPartitionTestCase extends AbstractClusteringTestCase {

    // maximum time in ms to wait for cluster topology change in case the injected merge event fails for some reason
    private static final long TOPOLOGY_CHANGE_TIMEOUT = TimeoutUtil.adjust(150_000);
    // it takes a little extra time after merge for the singleton service to migrate
    private static final int SERVICE_TIMEOUT = TimeoutUtil.adjust(5_000);
    private static final String CONTAINER = "server";

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

    private static Archive<?> createDeployment() {
        WebArchive war = ShrinkWrap.create(WebArchive.class, SingletonPartitionTestCase.class.getSimpleName() + ".war");
        war.addPackage(SingletonServiceActivator.class.getPackage());
        war.addClasses(NodeServiceServlet.class, SingletonElectionListenerService.class, NodeServiceExecutorRegistry.class);
        war.addAsServiceProvider(ServiceActivator.class, SingletonServiceActivator.class);
        ClusterTestUtil.addTopologyListenerDependencies(war);
        war.setManifest(new StringAsset("Manifest-Version: 1.0\nDependencies: org.wildfly.service, org.jgroups, org.infinispan.core\n"));
        return war;
    }

    @Test
    public void testSingletonService(
            @ArquillianResource() @OperateOnDeployment(DEPLOYMENT_1) URL baseURL1,
            @ArquillianResource() @OperateOnDeployment(DEPLOYMENT_2) URL baseURL2) throws Exception {

        // URLs look like "http://IP:PORT/singleton/service"
        URI serviceANode1Uri = NodeServiceServlet.createURI(baseURL1, SingletonServiceActivator.SERVICE_A_NAME);
        URI serviceANode2Uri = NodeServiceServlet.createURI(baseURL2, SingletonServiceActivator.SERVICE_A_NAME);

        URI serviceBNode1Uri = NodeServiceServlet.createURI(baseURL1, SingletonServiceActivator.SERVICE_B_NAME);
        URI serviceBNode2Uri = NodeServiceServlet.createURI(baseURL2, SingletonServiceActivator.SERVICE_B_NAME);

        log.trace("URLs are:\n" + serviceANode1Uri
                + "\n" + serviceANode2Uri
                + "\n" + serviceBNode1Uri
                + "\n" + serviceBNode2Uri);

        // 1. Begin with no partitions, both services should have a single provider

        waitForView(baseURL1, NODE_1, NODE_2);
        waitForView(baseURL2, NODE_1, NODE_2);
        Thread.sleep(SERVICE_TIMEOUT);


        // check service A
        checkSingletonNode(baseURL1, SingletonServiceActivator.SERVICE_A_NAME, SingletonServiceActivator.SERVICE_A_PREFERRED_NODE);
        checkSingletonNode(baseURL2, SingletonServiceActivator.SERVICE_A_NAME, SingletonServiceActivator.SERVICE_A_PREFERRED_NODE);
        // check service B
        checkSingletonNode(baseURL1, SingletonServiceActivator.SERVICE_B_NAME, SingletonServiceActivator.SERVICE_B_PREFERRED_NODE);
        checkSingletonNode(baseURL2, SingletonServiceActivator.SERVICE_B_NAME, SingletonServiceActivator.SERVICE_B_PREFERRED_NODE);


        // 2. Simulate network partition; each having it's own provider

        partition(true, baseURL1, baseURL2);
        waitForView(baseURL1, NODE_1);
        waitForView(baseURL2, NODE_2);
        Thread.sleep(SERVICE_TIMEOUT);

        // check service A
        checkSingletonNode(baseURL1, SingletonServiceActivator.SERVICE_A_NAME, NODE_1);
        checkSingletonNode(baseURL2, SingletonServiceActivator.SERVICE_A_NAME, NODE_2);
        // check service B
        checkSingletonNode(baseURL1, SingletonServiceActivator.SERVICE_B_NAME, NODE_1);
        checkSingletonNode(baseURL2, SingletonServiceActivator.SERVICE_B_NAME, NODE_2);


        // 3. Stop partitioning, merge, each service is supposed to have a single provider again.

        partition(false, baseURL1, baseURL2);
        waitForView(baseURL1, NODE_1, NODE_2);
        waitForView(baseURL2, NODE_1, NODE_2);
        Thread.sleep(SERVICE_TIMEOUT);

        // check service A
        checkSingletonNode(baseURL1, SingletonServiceActivator.SERVICE_A_NAME, SingletonServiceActivator.SERVICE_A_PREFERRED_NODE);
        checkSingletonNode(baseURL2, SingletonServiceActivator.SERVICE_A_NAME, SingletonServiceActivator.SERVICE_A_PREFERRED_NODE);
        // check service B
        checkSingletonNode(baseURL1, SingletonServiceActivator.SERVICE_B_NAME, SingletonServiceActivator.SERVICE_B_PREFERRED_NODE);
        checkSingletonNode(baseURL2, SingletonServiceActivator.SERVICE_B_NAME, SingletonServiceActivator.SERVICE_B_PREFERRED_NODE);


        // 4. Simulate network partition again, each node should start the service again. This verifies WFLY-4748.

        partition(true, baseURL1, baseURL2);
        waitForView(baseURL1, NODE_1);
        waitForView(baseURL2, NODE_2);
        Thread.sleep(SERVICE_TIMEOUT);

        // check service A
        checkSingletonNode(baseURL1, SingletonServiceActivator.SERVICE_A_NAME, NODE_1);
        checkSingletonNode(baseURL2, SingletonServiceActivator.SERVICE_A_NAME, NODE_2);
        // check service B
        checkSingletonNode(baseURL1, SingletonServiceActivator.SERVICE_B_NAME, NODE_1);
        checkSingletonNode(baseURL2, SingletonServiceActivator.SERVICE_B_NAME, NODE_2);
    }

    @Override
    public void afterTestMethod() throws Exception {
        super.afterTestMethod();

        // Stop the container to ensure there aren't any remnants since the test operates on a live JGroups channels
        stop(TWO_NODES);
    }

    private static void checkSingletonNode(URL baseURL, ServiceName serviceName, String expectedProviderNode) throws IOException, URISyntaxException {
        URI uri = (expectedProviderNode != null) ? NodeServiceServlet.createURI(baseURL, serviceName, expectedProviderNode) : NodeServiceServlet.createURI(baseURL, serviceName);
        try (CloseableHttpClient client = TestHttpClientUtils.promiscuousCookieHttpClient()) {
            HttpResponse response = client.execute(new HttpGet(uri));
            try {
                Assert.assertEquals(HttpServletResponse.SC_OK, response.getStatusLine().getStatusCode());
                Header header = response.getFirstHeader("node");
                if (header != null) {
                    Assert.assertEquals("Expected different provider node", expectedProviderNode, header.getValue());
                } else {
                    Assert.assertNull("Unexpected provider node", expectedProviderNode);
                }
            } finally {
                HttpClientUtils.closeQuietly(response);
            }
        }
    }

    private static void waitForView(URL baseURL, String... members) throws IOException, URISyntaxException {
        ClusterHttpClientUtil.establishTopology(baseURL, CONTAINER, "default", TOPOLOGY_CHANGE_TIMEOUT, members);
    }

    private static void partition(boolean partition, URL... baseURIs) {
        Arrays.stream(baseURIs).parallel().forEach(baseURI -> {
            try (CloseableHttpClient client = TestHttpClientUtils.promiscuousCookieHttpClient()) {
                HttpResponse response = client.execute(new HttpGet(PartitionServlet.createURI(baseURI, partition)));
                try {
                    Assert.assertEquals(HttpServletResponse.SC_OK, response.getStatusLine().getStatusCode());
                } finally {
                    HttpClientUtils.closeQuietly(response);
                }
            } catch (Exception ignored) {
            }
        });
    }
}
