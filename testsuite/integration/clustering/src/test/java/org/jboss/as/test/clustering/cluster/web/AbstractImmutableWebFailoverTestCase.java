/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.clustering.cluster.web;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

import jakarta.servlet.http.HttpServletResponse;

import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpHead;
import org.apache.http.impl.client.CloseableHttpClient;
import org.jboss.arquillian.container.test.api.OperateOnDeployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.as.test.clustering.ClusterHttpClientUtil;
import org.jboss.as.test.clustering.cluster.AbstractClusteringTestCase;
import org.jboss.as.test.clustering.single.web.SimpleServlet;
import org.jboss.as.test.http.util.TestHttpClientUtils;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Validates behavior of immutable session attributes.
 *
 * @author Paul Ferraro
 */
@RunWith(Arquillian.class)
public abstract class AbstractImmutableWebFailoverTestCase extends AbstractClusteringTestCase {

    private final String deploymentName;

    protected AbstractImmutableWebFailoverTestCase(String deploymentName) {
        super(NODE_1_2_3);

        this.deploymentName = deploymentName;
    }

    @Test
    public void test(
            @ArquillianResource(SimpleServlet.class) @OperateOnDeployment(DEPLOYMENT_1) URL baseURL1,
            @ArquillianResource(SimpleServlet.class) @OperateOnDeployment(DEPLOYMENT_2) URL baseURL2,
            @ArquillianResource(SimpleServlet.class) @OperateOnDeployment(DEPLOYMENT_3) URL baseURL3) throws Exception {
        this.testGracefulUndeployFailover(baseURL1, baseURL2, baseURL3);
        this.testGracefulSimpleFailover(baseURL1, baseURL2, baseURL3);
    }

    protected void testGracefulSimpleFailover(URL baseURL1, URL baseURL2, URL baseURL3) throws Exception {
        this.testFailover(new GracefulRestartLifecycle(), baseURL1, baseURL2, baseURL3);
    }

    protected void testGracefulUndeployFailover(URL baseURL1, URL baseURL2, URL baseURL3) throws Exception {
        this.testFailover(new RedeployLifecycle(), baseURL1, baseURL2, baseURL3);
    }

    private void testFailover(Lifecycle lifecycle, URL baseURL1, URL baseURL2, URL baseURL3) throws Exception {
        URI uri1 = SimpleServlet.createURI(baseURL1);
        URI uri2 = SimpleServlet.createURI(baseURL2);
        URI uri3 = SimpleServlet.createURI(baseURL3);

        this.establishTopology(baseURL1, NODE_1_2_3);

        AtomicReference<String> sessionId = new AtomicReference<>();

        try (CloseableHttpClient client = TestHttpClientUtils.promiscuousCookieHttpClient()) {
            try (CloseableHttpResponse response = client.execute(new HttpGet(uri1))) {
                Assert.assertEquals(HttpServletResponse.SC_OK, response.getStatusLine().getStatusCode());
                sessionId.setPlain(response.getFirstHeader(SimpleServlet.SESSION_ID_HEADER).getValue());
                Assert.assertEquals(1, Integer.parseInt(response.getFirstHeader(SimpleServlet.VALUE_HEADER).getValue()));
                Map.Entry<String, String> entry = parseSessionRoute(response);
                Assert.assertNotNull(entry);
                Assert.assertEquals(sessionId.getPlain(), entry.getKey());
                Assert.assertEquals(NODE_1, entry.getValue());
            }

            // Were we to request the same session on the same server, there is a chance that the transaction for the next request will coalesce with the previous one.
            // To ensure the initial transaction completes, request the same session on another server.
            try (CloseableHttpResponse response = client.execute(new HttpHead(uri3))) {
                Assert.assertEquals(HttpServletResponse.SC_OK, response.getStatusLine().getStatusCode());
                Assert.assertEquals(sessionId.getPlain(), response.getFirstHeader(SimpleServlet.SESSION_ID_HEADER).getValue());
                Map.Entry<String, String> entry = parseSessionRoute(response);
                Assert.assertNotNull(entry);
                Assert.assertEquals(sessionId.getPlain(), entry.getKey());
                Assert.assertEquals(NODE_3, entry.getValue());
            }

            // The following requests will read and mutate a session attribute
            // Since the attribute was configured to be immutable, no operation on the session attributes cache entry will be triggered.
            // We can verify this by checking that such updates are not visible between servers

            try (CloseableHttpResponse response = client.execute(new HttpGet(uri1))) {
                Assert.assertEquals(HttpServletResponse.SC_OK, response.getStatusLine().getStatusCode());
                Assert.assertEquals(sessionId.getPlain(), response.getFirstHeader(SimpleServlet.SESSION_ID_HEADER).getValue());
                Assert.assertEquals(2, Integer.parseInt(response.getFirstHeader(SimpleServlet.VALUE_HEADER).getValue()));
                Map.Entry<String, String> entry = parseSessionRoute(response);
                Assert.assertNotNull(entry);
                Assert.assertEquals(sessionId.getPlain(), entry.getKey());
                Assert.assertEquals(NODE_1, entry.getValue());
            }

            try (CloseableHttpResponse response = client.execute(new HttpGet(uri2))) {
                Assert.assertEquals(HttpServletResponse.SC_OK, response.getStatusLine().getStatusCode());
                // Because session attribute is defined to be immutable, the previous updates should be lost
                Assert.assertEquals(sessionId.getPlain(), response.getFirstHeader(SimpleServlet.SESSION_ID_HEADER).getValue());
                Assert.assertEquals(2, Integer.parseInt(response.getFirstHeader(SimpleServlet.VALUE_HEADER).getValue()));
                Map.Entry<String, String> entry = parseSessionRoute(response);
                Assert.assertNotNull(entry);
                Assert.assertEquals(sessionId.getPlain(), entry.getKey());
                Assert.assertEquals(NODE_2, entry.getValue());
            }

            try (CloseableHttpResponse response = client.execute(new HttpGet(uri2))) {
                Assert.assertEquals(HttpServletResponse.SC_OK, response.getStatusLine().getStatusCode());
                Assert.assertEquals(sessionId.getPlain(), response.getFirstHeader(SimpleServlet.SESSION_ID_HEADER).getValue());
                Assert.assertEquals(3, Integer.parseInt(response.getFirstHeader(SimpleServlet.VALUE_HEADER).getValue()));
                Map.Entry<String, String> entry = parseSessionRoute(response);
                // Ensure routing is not changed on 2nd query
                Assert.assertNull(entry);
            }

            try (CloseableHttpResponse response = client.execute(new HttpGet(uri3))) {
                Assert.assertEquals(HttpServletResponse.SC_OK, response.getStatusLine().getStatusCode());
                // Because session attribute is defined to be immutable, the previous updates should be lost
                Assert.assertEquals(sessionId.getPlain(), response.getFirstHeader(SimpleServlet.SESSION_ID_HEADER).getValue());
                Assert.assertEquals(2, Integer.parseInt(response.getFirstHeader(SimpleServlet.VALUE_HEADER).getValue()));
                Map.Entry<String, String> entry = parseSessionRoute(response);
                Assert.assertNotNull(entry);
                Assert.assertEquals(sessionId.getPlain(), entry.getKey());
                Assert.assertEquals(NODE_3, entry.getValue());
            }

            try (CloseableHttpResponse response = client.execute(new HttpGet(uri3))) {
                Assert.assertEquals(HttpServletResponse.SC_OK, response.getStatusLine().getStatusCode());
                Assert.assertEquals(sessionId.getPlain(), response.getFirstHeader(SimpleServlet.SESSION_ID_HEADER).getValue());
                Assert.assertEquals(3, Integer.parseInt(response.getFirstHeader(SimpleServlet.VALUE_HEADER).getValue()));
                Map.Entry<String, String> entry = parseSessionRoute(response);
                // Ensure routing is not changed on 2nd query
                Assert.assertNull(entry);
            }
        }
    }

    private void establishTopology(URL baseURL, Set<String> topology) throws URISyntaxException, IOException, InterruptedException {
        ClusterHttpClientUtil.establishTopology(baseURL, "web", this.deploymentName, topology);

        // TODO we should be able to speed this up by observing changes in the routing registry
        // prevents failing assertions when topology information is expected, e.g.:
        //java.lang.AssertionError: expected null, but was:<bpAmUlICzeXMtFiOJvTiOCASbXNivRdHTIlQC00c=node-2>
        Thread.sleep(GRACE_TIME_TOPOLOGY_CHANGE);
    }
}
