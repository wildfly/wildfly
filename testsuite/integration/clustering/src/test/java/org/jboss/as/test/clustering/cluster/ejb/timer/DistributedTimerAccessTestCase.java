/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.clustering.cluster.ejb.timer;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

import org.apache.http.HttpEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.OperateOnDeployment;
import org.jboss.arquillian.container.test.api.TargetsContainer;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.as.test.clustering.cluster.AbstractClusteringTestCase;
import org.jboss.as.test.clustering.cluster.ejb.timer.beans.TimerServiceBean;
import org.jboss.as.test.clustering.cluster.ejb.timer.servlet.TimerServletEndpoint;
import org.jboss.as.test.http.util.TestHttpClientUtils;
import org.jboss.logging.Logger;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.github.dockerjava.zerodep.shaded.org.apache.hc.client5.http.ClientProtocolException;

import static org.awaitility.Awaitility.await;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.collection.ArrayMatching.arrayContainingInAnyOrder;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import jakarta.servlet.http.HttpServletResponse;

@RunWith(Arquillian.class)
public class DistributedTimerAccessTestCase extends AbstractClusteringTestCase {

    private static final Logger log = Logger.getLogger(DistributedTimerAccessTestCase.class);

    public static final String DEPLOYMENT = "TimerApp";

    @Deployment(name = DEPLOYMENT_1, managed = false, testable = false)
    @TargetsContainer(NODE_1)
    public static Archive<?> deployment0() {
        return createArchive();
    }

    @Deployment(name = DEPLOYMENT_2, managed = false, testable = false)
    @TargetsContainer(NODE_2)
    public static Archive<?> deployment1() {
        return createArchive();
    }

    protected static WebArchive createArchive() {
        return ShrinkWrap.create(WebArchive.class, DEPLOYMENT + ".war").addClass(TimerServletEndpoint.class)
                .addClass(TimerServiceBean.class);
    }

    @Test
    public void testDistributeCacheTimerAccess(@ArquillianResource @OperateOnDeployment(DEPLOYMENT_1) URI node1URL,
            @ArquillianResource @OperateOnDeployment(DEPLOYMENT_2) URI node2URL)
            throws InterruptedException, URISyntaxException {

        final int numberOfTimers = 5;
        List<URI> uris = List.of(node1URL, node2URL);
        List<URI> httpEndpoints = new ArrayList<>();
        uris.stream().map(uri -> uri.resolve(TimerServletEndpoint.ENDPOINT)).forEach(httpEndpoints::add);

        try (CloseableHttpClient httpClient = TestHttpClientUtils.promiscuousCookieHttpClient()) {

            List<String> expectedTimers = new ArrayList<>();
            for (int j = 0; j < httpEndpoints.size(); j++) {
                URI endpoint = httpEndpoints.get(j);
                for (int i = 0; i < numberOfTimers; i++) {
                    String value = String.valueOf(j * numberOfTimers + i);
                    expectedTimers.add(value);
                    boolean isTimerCreated = createTimer(httpClient, endpoint, value);
                    assertTrue("uri not getting status " + endpoint, isTimerCreated);
                }
            }

            await().atMost(Duration.ofSeconds(15)).untilAsserted(() -> {
                List<String> currentTimers = new ArrayList<>();
                for (int j = 0; j < httpEndpoints.size(); j++) {
                    currentTimers.addAll(getTimers(httpClient, httpEndpoints.get(j)));
                }
                assertThat(currentTimers.toArray(String[]::new), arrayContainingInAnyOrder(expectedTimers.toArray(String[]::new)));
            });
        } catch (IOException e) {
            fail("an exception happend");
        }

    }

    private List<String> getTimers(CloseableHttpClient httpClient, URI uri) throws ClientProtocolException, IOException {
        HttpGet get = new HttpGet(uri);
        try (CloseableHttpResponse response = httpClient.execute(get)) {
            if (HttpServletResponse.SC_OK == response.getStatusLine().getStatusCode()) {
                String content = new String(response.getEntity().getContent().readAllBytes());
                return List.of(content.split("\n"));
            }
            throw new IOException("failed to get timers");
        }
    }

    private boolean createTimer(CloseableHttpClient httpClient, URI uri, String info)
            throws ClientProtocolException, IOException {
        HttpPost post = new HttpPost(uri);
        HttpEntity entity = new StringEntity(info, ContentType.APPLICATION_JSON);
        post.setEntity(entity);
        try (CloseableHttpResponse response = httpClient.execute(post)) {
            return HttpServletResponse.SC_OK == response.getStatusLine().getStatusCode();
        }
    }
}
