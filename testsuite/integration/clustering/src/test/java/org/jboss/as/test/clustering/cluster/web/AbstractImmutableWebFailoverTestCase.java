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

import jakarta.servlet.http.HttpServletResponse;

import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.utils.HttpClientUtils;
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

        try (CloseableHttpClient client = TestHttpClientUtils.promiscuousCookieHttpClient()) {
            HttpResponse response = client.execute(new HttpGet(uri1));
            try {
                Assert.assertEquals(HttpServletResponse.SC_OK, response.getStatusLine().getStatusCode());
                Assert.assertEquals(1, Integer.parseInt(response.getFirstHeader(SimpleServlet.VALUE_HEADER).getValue()));
                Map.Entry<String, String> entry = parseSessionRoute(response);
                Assert.assertNotNull(entry);
                Assert.assertEquals(NODE_1, entry.getValue());
                Assert.assertEquals(entry.getKey(), response.getFirstHeader(SimpleServlet.SESSION_ID_HEADER).getValue());
            } finally {
                HttpClientUtils.closeQuietly(response);
            }

            response = client.execute(new HttpGet(uri1));
            try {
                Assert.assertEquals(HttpServletResponse.SC_OK, response.getStatusLine().getStatusCode());
                Assert.assertEquals(2, Integer.parseInt(response.getFirstHeader(SimpleServlet.VALUE_HEADER).getValue()));
                Map.Entry<String, String> entry = parseSessionRoute(response);
                // Ensure routing is not changed on 2nd query
                Assert.assertNull(entry);
            } finally {
                HttpClientUtils.closeQuietly(response);
            }

            response = client.execute(new HttpGet(uri2));
            try {
                Assert.assertEquals(HttpServletResponse.SC_OK, response.getStatusLine().getStatusCode());
                // Because session attribute is defined to be immutable, the previous updates should be lost
                Assert.assertEquals(2, Integer.parseInt(response.getFirstHeader(SimpleServlet.VALUE_HEADER).getValue()));
                Map.Entry<String, String> entry = parseSessionRoute(response);
                Assert.assertNotNull(entry);
                Assert.assertEquals(NODE_2, entry.getValue());
                Assert.assertEquals(entry.getKey(), response.getFirstHeader(SimpleServlet.SESSION_ID_HEADER).getValue());
            } finally {
                HttpClientUtils.closeQuietly(response);
            }

            response = client.execute(new HttpGet(uri2));
            try {
                Assert.assertEquals(HttpServletResponse.SC_OK, response.getStatusLine().getStatusCode());
                Assert.assertEquals(3, Integer.parseInt(response.getFirstHeader(SimpleServlet.VALUE_HEADER).getValue()));
                Map.Entry<String, String> entry = parseSessionRoute(response);
                // Ensure routing is not changed on 2nd query
                Assert.assertNull(entry);
            } finally {
                HttpClientUtils.closeQuietly(response);
            }

            response = client.execute(new HttpGet(uri3));
            try {
                Assert.assertEquals(HttpServletResponse.SC_OK, response.getStatusLine().getStatusCode());
                // Because session attribute is defined to be immutable, the previous updates should be lost
                Assert.assertEquals(2, Integer.parseInt(response.getFirstHeader(SimpleServlet.VALUE_HEADER).getValue()));
                Map.Entry<String, String> entry = parseSessionRoute(response);
                Assert.assertNotNull(entry);
                Assert.assertEquals(NODE_3, entry.getValue());
                Assert.assertEquals(entry.getKey(), response.getFirstHeader(SimpleServlet.SESSION_ID_HEADER).getValue());
            } finally {
                HttpClientUtils.closeQuietly(response);
            }

            response = client.execute(new HttpGet(uri3));
            try {
                Assert.assertEquals(HttpServletResponse.SC_OK, response.getStatusLine().getStatusCode());
                Assert.assertEquals(3, Integer.parseInt(response.getFirstHeader(SimpleServlet.VALUE_HEADER).getValue()));
                Map.Entry<String, String> entry = parseSessionRoute(response);
                // Ensure routing is not changed on 2nd query
                Assert.assertNull(entry);
            } finally {
                HttpClientUtils.closeQuietly(response);
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
