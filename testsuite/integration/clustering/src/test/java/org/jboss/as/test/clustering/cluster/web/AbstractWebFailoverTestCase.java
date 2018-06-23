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
package org.jboss.as.test.clustering.cluster.web;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Map;

import javax.servlet.http.HttpServletResponse;

import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.utils.HttpClientUtils;
import org.apache.http.impl.client.CloseableHttpClient;
import org.infinispan.configuration.cache.CacheMode;
import org.infinispan.transaction.TransactionMode;
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
 * Test that HTTP session failover on shutdown and undeploy works.
 *
 * @author Radoslav Husar
 */
@RunWith(Arquillian.class)
public abstract class AbstractWebFailoverTestCase extends AbstractClusteringTestCase {

    private final String deploymentName;
    private final CacheMode cacheMode;
    private final Runnable nonTxWait;

    protected AbstractWebFailoverTestCase(String deploymentName, TransactionMode transactionMode) {
        this(deploymentName, CacheMode.DIST_SYNC, transactionMode);
    }

    protected AbstractWebFailoverTestCase(String deploymentName, CacheMode cacheMode, TransactionMode transactionMode) {
        super(THREE_NODES);

        this.deploymentName = deploymentName;
        this.cacheMode = cacheMode;
        this.nonTxWait = () -> {
            // If the cache is non-transactional, we need to wait for that replication to finish, otherwise the read can be stale
            if (!transactionMode.isTransactional()) {
                try {
                    Thread.sleep(GRACE_TIME_TO_REPLICATE);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
        };
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
        this.testFailover(new RestartLifecycle(), baseURL1, baseURL2, baseURL3);
    }

    protected void testGracefulUndeployFailover(URL baseURL1, URL baseURL2, URL baseURL3) throws Exception {
        this.testFailover(new RedeployLifecycle(), baseURL1, baseURL2, baseURL3);
    }

    private void testFailover(Lifecycle lifecycle, URL baseURL1, URL baseURL2, URL baseURL3) throws Exception {
        URI uri1 = SimpleServlet.createURI(baseURL1);
        URI uri2 = SimpleServlet.createURI(baseURL2);
        URI uri3 = SimpleServlet.createURI(baseURL3);

        this.establishTopology(baseURL1, THREE_NODES);
        int value = 1;
        // In case updated route information is received, it must be different from the last route
        String lastOwner;

        try (CloseableHttpClient client = TestHttpClientUtils.promiscuousCookieHttpClient()) {
            HttpResponse response = client.execute(new HttpGet(uri1));
            try {
                Assert.assertEquals(HttpServletResponse.SC_OK, response.getStatusLine().getStatusCode());
                Assert.assertEquals(value++, Integer.parseInt(response.getFirstHeader(SimpleServlet.VALUE_HEADER).getValue()));
                Map.Entry<String, String> entry = parseSessionRoute(response);
                Assert.assertNotNull(entry);
                Assert.assertEquals(NODE_1, entry.getValue());
                lastOwner = entry.getValue();
                Assert.assertEquals(entry.getKey(), response.getFirstHeader(SimpleServlet.SESSION_ID_HEADER).getValue());
            } finally {
                HttpClientUtils.closeQuietly(response);
            }

            this.nonTxWait.run();

            response = client.execute(new HttpGet(uri1));
            try {
                Assert.assertEquals(HttpServletResponse.SC_OK, response.getStatusLine().getStatusCode());
                Assert.assertEquals(value++, Integer.parseInt(response.getFirstHeader(SimpleServlet.VALUE_HEADER).getValue()));
                Map.Entry<String, String> entry = parseSessionRoute(response);
                // Ensure routing is not changed on 2nd query
                Assert.assertNull(entry);
            } finally {
                HttpClientUtils.closeQuietly(response);
            }

            lifecycle.stop(NODE_1);

            this.establishTopology(baseURL2, NODE_2, NODE_3);

            // node2
            response = client.execute(new HttpGet(uri2));
            try {
                Assert.assertEquals(HttpServletResponse.SC_OK, response.getStatusLine().getStatusCode());
                Assert.assertEquals(value++, Integer.parseInt(response.getFirstHeader(SimpleServlet.VALUE_HEADER).getValue()));
                Map.Entry<String, String> entry = parseSessionRoute(response);
                // After topology change, the session will have to be re-routed to either of the 2 remaining nodes
                Assert.assertNotNull(entry);
                Assert.assertNotEquals(lastOwner, entry.getValue());
                lastOwner = entry.getValue();
                Assert.assertEquals(entry.getKey(), response.getFirstHeader(SimpleServlet.SESSION_ID_HEADER).getValue());
            } finally {
                HttpClientUtils.closeQuietly(response);
            }

            this.nonTxWait.run();

            response = client.execute(new HttpGet(uri2));
            try {
                Assert.assertEquals(HttpServletResponse.SC_OK, response.getStatusLine().getStatusCode());
                Assert.assertEquals(value++, Integer.parseInt(response.getFirstHeader(SimpleServlet.VALUE_HEADER).getValue()));
                Map.Entry<String, String> entry = parseSessionRoute(response);
                // Ensure routing is not changed on subsequent query
                Assert.assertNull(entry);
            } finally {
                HttpClientUtils.closeQuietly(response);
            }

            this.nonTxWait.run();

            // node3
            response = client.execute(new HttpGet(uri3));
            try {
                Assert.assertEquals(HttpServletResponse.SC_OK, response.getStatusLine().getStatusCode());
                Assert.assertEquals(value++, Integer.parseInt(response.getFirstHeader(SimpleServlet.VALUE_HEADER).getValue()));
                Map.Entry<String, String> entry = parseSessionRoute(response);
                if (cacheMode.isInvalidation()) {
                    Assert.assertNotNull(entry);
                    Assert.assertEquals(NODE_3, entry.getValue());
                } else {
                    Assert.assertNull(entry);
                }
            } finally {
                HttpClientUtils.closeQuietly(response);
            }

            this.nonTxWait.run();

            response = client.execute(new HttpGet(uri3));
            try {
                Assert.assertEquals(HttpServletResponse.SC_OK, response.getStatusLine().getStatusCode());
                Assert.assertEquals(value++, Integer.parseInt(response.getFirstHeader(SimpleServlet.VALUE_HEADER).getValue()));
                Map.Entry<String, String> entry = parseSessionRoute(response);
                Assert.assertNull(entry);
            } finally {
                HttpClientUtils.closeQuietly(response);
            }

            lifecycle.start(NODE_1);

            this.establishTopology(baseURL2, THREE_NODES);

            response = client.execute(new HttpGet(uri2));
            try {
                Assert.assertEquals(HttpServletResponse.SC_OK, response.getStatusLine().getStatusCode());
                Assert.assertEquals(value++, Integer.parseInt(response.getFirstHeader(SimpleServlet.VALUE_HEADER).getValue()));
                Map.Entry<String, String> entry = parseSessionRoute(response);
                if (cacheMode.isInvalidation()) {
                    Assert.assertNotNull(entry);
                    Assert.assertEquals(NODE_2, entry.getValue());
                } else if (entry != null) {
                    Assert.assertNotEquals(lastOwner, entry.getValue());
                    lastOwner = entry.getValue();
                    Assert.assertEquals(entry.getKey(), response.getFirstHeader(SimpleServlet.SESSION_ID_HEADER).getValue());
                }
            } finally {
                HttpClientUtils.closeQuietly(response);
            }

            this.nonTxWait.run();

            response = client.execute(new HttpGet(uri2));
            try {
                Assert.assertEquals(HttpServletResponse.SC_OK, response.getStatusLine().getStatusCode());
                Assert.assertEquals(value++, Integer.parseInt(response.getFirstHeader(SimpleServlet.VALUE_HEADER).getValue()));
                Map.Entry<String, String> entry = parseSessionRoute(response);
                Assert.assertNull(entry);
            } finally {
                HttpClientUtils.closeQuietly(response);
            }

            this.nonTxWait.run();

            response = client.execute(new HttpGet(uri3));
            try {
                Assert.assertEquals(HttpServletResponse.SC_OK, response.getStatusLine().getStatusCode());
                Assert.assertEquals(value++, Integer.parseInt(response.getFirstHeader(SimpleServlet.VALUE_HEADER).getValue()));
                Map.Entry<String, String> entry = parseSessionRoute(response);
                if (cacheMode.isInvalidation()) {
                    Assert.assertNotNull(entry);
                    Assert.assertEquals(NODE_3, entry.getValue());
                } else {
                    Assert.assertNull(entry);
                }
            } finally {
                HttpClientUtils.closeQuietly(response);
            }

            lifecycle.stop(NODE_2);

            this.establishTopology(baseURL1, NODE_1, NODE_3);

            response = client.execute(new HttpGet(uri1));
            try {
                Assert.assertEquals(HttpServletResponse.SC_OK, response.getStatusLine().getStatusCode());
                Assert.assertEquals(value++, Integer.parseInt(response.getFirstHeader(SimpleServlet.VALUE_HEADER).getValue()));
                Map.Entry<String, String> entry = parseSessionRoute(response);
                if (entry != null) {
                    Assert.assertNotEquals(lastOwner, entry.getValue());
                    lastOwner = entry.getValue();
                    Assert.assertEquals(entry.getKey(), response.getFirstHeader(SimpleServlet.SESSION_ID_HEADER).getValue());
                }
            } finally {
                HttpClientUtils.closeQuietly(response);
            }

            this.nonTxWait.run();

            response = client.execute(new HttpGet(uri1));
            try {
                Assert.assertEquals(HttpServletResponse.SC_OK, response.getStatusLine().getStatusCode());
                Assert.assertEquals(value++, Integer.parseInt(response.getFirstHeader(SimpleServlet.VALUE_HEADER).getValue()));
                Map.Entry<String, String> entry = parseSessionRoute(response);
                if (entry != null) {
                    Assert.assertNotEquals(lastOwner, entry.getValue());
                    lastOwner = entry.getValue();
                    Assert.assertEquals(entry.getKey(), response.getFirstHeader(SimpleServlet.SESSION_ID_HEADER).getValue());
                }
            } finally {
                HttpClientUtils.closeQuietly(response);
            }

            this.nonTxWait.run();

            response = client.execute(new HttpGet(uri3));
            try {
                Assert.assertEquals(HttpServletResponse.SC_OK, response.getStatusLine().getStatusCode());
                Assert.assertEquals(value++, Integer.parseInt(response.getFirstHeader(SimpleServlet.VALUE_HEADER).getValue()));
                Map.Entry<String, String> entry = parseSessionRoute(response);
                if (cacheMode.isInvalidation()) {
                    Assert.assertNotNull(entry);
                    Assert.assertEquals(NODE_3, entry.getValue());
                } else {
                    if (entry != null) {
                        Assert.assertNotEquals(lastOwner, entry.getValue());
                        lastOwner = entry.getValue();
                        Assert.assertEquals(entry.getKey(), response.getFirstHeader(SimpleServlet.SESSION_ID_HEADER).getValue());
                    }
                }
            } finally {
                HttpClientUtils.closeQuietly(response);
            }

            this.nonTxWait.run();

            response = client.execute(new HttpGet(uri3));
            try {
                Assert.assertEquals(HttpServletResponse.SC_OK, response.getStatusLine().getStatusCode());
                Assert.assertEquals(value++, Integer.parseInt(response.getFirstHeader(SimpleServlet.VALUE_HEADER).getValue()));
                Map.Entry<String, String> entry = parseSessionRoute(response);
                if (entry != null) {
                    Assert.assertNotEquals(lastOwner, entry.getValue());
                    lastOwner = entry.getValue();
                    Assert.assertEquals(entry.getKey(), response.getFirstHeader(SimpleServlet.SESSION_ID_HEADER).getValue());
                }
            } finally {
                HttpClientUtils.closeQuietly(response);
            }

            lifecycle.start(NODE_2);

            this.establishTopology(baseURL1, THREE_NODES);

            this.nonTxWait.run();

            response = client.execute(new HttpGet(uri1));
            try {
                Assert.assertEquals(HttpServletResponse.SC_OK, response.getStatusLine().getStatusCode());
                Assert.assertEquals(value++, Integer.parseInt(response.getFirstHeader(SimpleServlet.VALUE_HEADER).getValue()));
                Map.Entry<String, String> entry = parseSessionRoute(response);
                if (cacheMode.isInvalidation()) {
                    Assert.assertNotNull(entry);
                    Assert.assertEquals(NODE_1, entry.getValue());
                } else if (entry != null) {
                    Assert.assertNotEquals(lastOwner, entry.getValue());
                    Assert.assertEquals(entry.getKey(), response.getFirstHeader(SimpleServlet.SESSION_ID_HEADER).getValue());
                }
            } finally {
                HttpClientUtils.closeQuietly(response);
            }

            this.nonTxWait.run();

            response = client.execute(new HttpGet(uri1));
            try {
                Assert.assertEquals(HttpServletResponse.SC_OK, response.getStatusLine().getStatusCode());
                Assert.assertEquals(value, Integer.parseInt(response.getFirstHeader(SimpleServlet.VALUE_HEADER).getValue()));
                Map.Entry<String, String> entry = parseSessionRoute(response);
                if (entry != null) {
                    Assert.assertNotEquals(lastOwner, entry.getValue());
                    lastOwner = entry.getValue();
                    Assert.assertEquals(entry.getKey(), response.getFirstHeader(SimpleServlet.SESSION_ID_HEADER).getValue());
                }
            } finally {
                HttpClientUtils.closeQuietly(response);
            }
        }
    }

    private void establishTopology(URL baseURL, String... nodes) throws URISyntaxException, IOException, InterruptedException {
        ClusterHttpClientUtil.establishTopology(baseURL, "web", this.deploymentName, nodes);

        // TODO we should be able to speed this up by observing changes in the routing registry
        // prevents failing assertions when topology information is expected, e.g.:
        //java.lang.AssertionError: expected null, but was:<bpAmUlICzeXMtFiOJvTiOCASbXNivRdHTIlQC00c=node-2>
        Thread.sleep(GRACE_TIME_TOPOLOGY_CHANGE);
    }
}
