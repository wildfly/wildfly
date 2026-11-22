/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.clustering.cluster.singleton;

import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.time.Duration;
import java.util.Arrays;
import java.util.Set;

import jakarta.servlet.http.HttpServletResponse;

import org.apache.http.Header;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.awaitility.Awaitility;
import org.awaitility.core.ConditionFactory;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.OperateOnDeployment;
import org.jboss.arquillian.container.test.api.TargetsContainer;
import org.jboss.arquillian.junit5.ArquillianExtension;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.as.test.clustering.ClusterTestUtil;
import org.jboss.as.test.clustering.TopologyChangeListenerUtil;
import org.jboss.as.test.clustering.cluster.AbstractClusteringTestCase;
import org.jboss.as.test.clustering.cluster.singleton.partition.PartitionServlet;
import org.jboss.as.test.clustering.cluster.singleton.partition.SingletonServiceActivator;
import org.jboss.as.test.clustering.cluster.singleton.service.ElectedMemberRegistry;
import org.jboss.as.test.clustering.cluster.singleton.service.ElectedMemberServiceServlet;
import org.jboss.as.test.http.util.TestHttpClientUtils;
import org.jboss.as.test.shared.TimeoutUtil;
import org.jboss.msc.service.ServiceActivator;
import org.jboss.msc.service.ServiceName;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

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
@ExtendWith(ArquillianExtension.class)
public class SingletonPartitionTestCase extends AbstractClusteringTestCase {
    private static final ConditionFactory AWAIT = Awaitility.await().atMost(TimeoutUtil.adjust(Duration.ofSeconds(15))).pollInterval(Duration.ofMillis(100)).pollInSameThread();

    // maximum time in ms to wait for cluster topology change in case the injected merge event fails for some reason
    private static final Duration TOPOLOGY_CHANGE_TIMEOUT = Duration.ofMillis(TimeoutUtil.adjust(150_000));
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
        war.addClasses(ElectedMemberServiceServlet.class, ElectedMemberRegistry.class);
        war.addAsServiceProvider(ServiceActivator.class, SingletonServiceActivator.class);
        ClusterTestUtil.addTopologyListenerDependencies(war);
        war.setManifest(new StringAsset("Manifest-Version: 1.0\nDependencies: org.jgroups, org.infinispan.core\n"));
        return war;
    }

    @Test
    void singletonService(
            @ArquillianResource() @OperateOnDeployment(DEPLOYMENT_1) URL baseURL1,
            @ArquillianResource() @OperateOnDeployment(DEPLOYMENT_2) URL baseURL2) throws Exception {

        // URLs look like "http://IP:PORT/singleton/service"
        URI serviceANode1Uri = ElectedMemberServiceServlet.createURI(baseURL1, SingletonServiceActivator.SERVICE_A_NAME);
        URI serviceANode2Uri = ElectedMemberServiceServlet.createURI(baseURL2, SingletonServiceActivator.SERVICE_A_NAME);

        URI serviceBNode1Uri = ElectedMemberServiceServlet.createURI(baseURL1, SingletonServiceActivator.SERVICE_B_NAME);
        URI serviceBNode2Uri = ElectedMemberServiceServlet.createURI(baseURL2, SingletonServiceActivator.SERVICE_B_NAME);

        log.trace("URLs are:\n" + serviceANode1Uri
                + "\n" + serviceANode2Uri
                + "\n" + serviceBNode1Uri
                + "\n" + serviceBNode2Uri);

        try (CloseableHttpClient client = TestHttpClientUtils.promiscuousCookieHttpClient()) {

            // 1. Begin with no partitions, both services should have a single provider

            waitForView(client, baseURL1, NODE_1_2);
            waitForView(client, baseURL2, NODE_1_2);
            Thread.sleep(SERVICE_TIMEOUT);

            // check service A
            checkSingletonNode(client, baseURL1, SingletonServiceActivator.SERVICE_A_NAME, SingletonServiceActivator.SERVICE_A_PREFERRED_NODE);
            checkSingletonNode(client, baseURL2, SingletonServiceActivator.SERVICE_A_NAME, SingletonServiceActivator.SERVICE_A_PREFERRED_NODE);
            // check service B
            checkSingletonNode(client, baseURL1, SingletonServiceActivator.SERVICE_B_NAME, SingletonServiceActivator.SERVICE_B_PREFERRED_NODE);
            checkSingletonNode(client, baseURL2, SingletonServiceActivator.SERVICE_B_NAME, SingletonServiceActivator.SERVICE_B_PREFERRED_NODE);

            // 2. Simulate network partition; each having its own provider
            partition(client, true, baseURL1, baseURL2);
            waitForView(client, baseURL1, Set.of(NODE_1));
            waitForView(client, baseURL2, Set.of(NODE_2));
            Thread.sleep(SERVICE_TIMEOUT);

            // check service A
            checkSingletonNode(client, baseURL1, SingletonServiceActivator.SERVICE_A_NAME, NODE_1);
            checkSingletonNode(client, baseURL2, SingletonServiceActivator.SERVICE_A_NAME, NODE_2);
            // check service B
            checkSingletonNode(client, baseURL1, SingletonServiceActivator.SERVICE_B_NAME, NODE_1);
            checkSingletonNode(client, baseURL2, SingletonServiceActivator.SERVICE_B_NAME, NODE_2);

            // 3. Stop partitioning, merge, each service is supposed to have a single provider again.

            partition(client, false, baseURL1, baseURL2);
            waitForView(client, baseURL1, NODE_1_2);
            waitForView(client, baseURL2, NODE_1_2);
            Thread.sleep(SERVICE_TIMEOUT);

            // check service A
            checkSingletonNode(client, baseURL1, SingletonServiceActivator.SERVICE_A_NAME, SingletonServiceActivator.SERVICE_A_PREFERRED_NODE);
            checkSingletonNode(client, baseURL2, SingletonServiceActivator.SERVICE_A_NAME, SingletonServiceActivator.SERVICE_A_PREFERRED_NODE);
            // check service B
            checkSingletonNode(client, baseURL1, SingletonServiceActivator.SERVICE_B_NAME, SingletonServiceActivator.SERVICE_B_PREFERRED_NODE);
            checkSingletonNode(client, baseURL2, SingletonServiceActivator.SERVICE_B_NAME, SingletonServiceActivator.SERVICE_B_PREFERRED_NODE);

            // 4. Simulate network partition again, each node should start the service again. This verifies WFLY-4748.

            partition(client, true, baseURL1, baseURL2);
            waitForView(client, baseURL1, Set.of(NODE_1));
            waitForView(client, baseURL2, Set.of(NODE_2));
            Thread.sleep(SERVICE_TIMEOUT);

            // check service A
            checkSingletonNode(client, baseURL1, SingletonServiceActivator.SERVICE_A_NAME, NODE_1);
            checkSingletonNode(client, baseURL2, SingletonServiceActivator.SERVICE_A_NAME, NODE_2);
            // check service B
            checkSingletonNode(client, baseURL1, SingletonServiceActivator.SERVICE_B_NAME, NODE_1);
            checkSingletonNode(client, baseURL2, SingletonServiceActivator.SERVICE_B_NAME, NODE_2);
        }
    }

    @AfterEach
    @Override
    public void afterTestMethod() throws Exception {
        super.afterTestMethod();

        // Stop the container to ensure there aren't any remnants since the test operates on a live JGroups channels
        stop(NODE_1_2);
    }

    private static void checkSingletonNode(CloseableHttpClient client, URL baseURL, ServiceName serviceName, String expectedProviderNode) throws URISyntaxException {
        URI uri = (expectedProviderNode != null) ? ElectedMemberServiceServlet.createURI(baseURL, serviceName, expectedProviderNode) : ElectedMemberServiceServlet.createURI(baseURL, serviceName);
        AWAIT.untilAsserted(() -> {
            try (CloseableHttpResponse response = client.execute(new HttpGet(uri))) {
                assertEquals(HttpServletResponse.SC_OK, response.getStatusLine().getStatusCode());
                Header header = response.getFirstHeader(ElectedMemberServiceServlet.ELECTED_MEMBER_HEADER);
                if (header != null) {
                    assertEquals(expectedProviderNode, header.getValue(), "Expected different provider node");
                } else {
                    assertNull(expectedProviderNode, "Unexpected provider node");
                }
            }
        });
    }

    private static void waitForView(CloseableHttpClient client, URL baseURL, Set<String> topology) throws IOException, URISyntaxException {
        TopologyChangeListenerUtil.establishTopology(client, baseURL, CONTAINER, "default", topology, TOPOLOGY_CHANGE_TIMEOUT);
    }

    private static void partition(CloseableHttpClient client, boolean partition, URL... baseURIs) {
        Arrays.stream(baseURIs).parallel().forEach(baseURI -> {
            try (CloseableHttpResponse response = client.execute(new HttpGet(PartitionServlet.createURI(baseURI, partition)))) {
                assertEquals(HttpServletResponse.SC_OK, response.getStatusLine().getStatusCode());
            } catch (IOException | URISyntaxException e) {
                fail(e);
            }
        });
    }
}
