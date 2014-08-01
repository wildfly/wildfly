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
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.utils.HttpClientUtils;
import org.jboss.arquillian.container.test.api.OperateOnDeployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.as.test.clustering.ClusterHttpClientUtil;
import org.jboss.as.test.clustering.cluster.ClusterAbstractTestCase;
import org.jboss.as.test.clustering.single.web.SimpleServlet;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Test that HTTP session failover on shutdown and undeploy works.
 *
 * @author Radoslav Husar
 * @version Oct 2012
 */
@RunWith(Arquillian.class)
@RunAsClient
public abstract class ClusteredWebFailoverAbstractCase extends ClusterAbstractTestCase {

    protected abstract String getDeploymentName();

    /**
     * Test simple graceful shutdown failover:
     * <p/>
     * 1/ Start 2 containers and deploy <distributable/> webapp.
     * 2/ Query first container creating a web session.
     * 3/ Shutdown first container.
     * 4/ Query second container verifying sessions got replicated.
     * 5/ Bring up the first container.
     * 6/ Query first container verifying that updated sessions replicated back.
     *
     * @throws IOException
     * @throws InterruptedException
     * @throws URISyntaxException
     */
    @Test
    public void testGracefulSimpleFailover(
            @ArquillianResource(SimpleServlet.class) @OperateOnDeployment(DEPLOYMENT_1) URL baseURL1,
            @ArquillianResource(SimpleServlet.class) @OperateOnDeployment(DEPLOYMENT_2) URL baseURL2)
            throws IOException, InterruptedException, URISyntaxException {
        testFailover(new RestartLifecycle(), baseURL1, baseURL2);
    }

    /**
     * Test simple undeploy failover:
     * <p/>
     * 1/ Start 2 containers and deploy <distributable/> webapp.
     * 2/ Query first container creating a web session.
     * 3/ Undeploy application from the first container.
     * 4/ Query second container verifying sessions got replicated.
     * 5/ Redeploy application to the first container.
     * 6/ Query first container verifying that updated sessions replicated back.
     *
     * @throws IOException
     * @throws InterruptedException
     * @throws URISyntaxException
     */
    @Test
    public void testGracefulUndeployFailover(
            @ArquillianResource(SimpleServlet.class) @OperateOnDeployment(DEPLOYMENT_1) URL baseURL1,
            @ArquillianResource(SimpleServlet.class) @OperateOnDeployment(DEPLOYMENT_2) URL baseURL2)
            throws IOException, URISyntaxException {
        testFailover(new RedeployLifecycle(), baseURL1, baseURL2);
    }

    private void testFailover(Lifecycle lifecycle, URL baseURL1, URL baseURL2) throws IOException, URISyntaxException {
        HttpClient client = org.jboss.as.test.http.util.HttpClientUtils.relaxedCookieHttpClient();

        URI uri1 = SimpleServlet.createURI(baseURL1);
        URI uri2 = SimpleServlet.createURI(baseURL2);

        this.establishTopology(baseURL1, NODES);

        try {
            HttpResponse response = client.execute(new HttpGet(uri1));
            try {
                Assert.assertEquals(HttpServletResponse.SC_OK, response.getStatusLine().getStatusCode());
                Assert.assertEquals(1, Integer.parseInt(response.getFirstHeader(SimpleServlet.VALUE_HEADER).getValue()));
                Map.Entry<String, String> entry = parseSessionRoute(response);
                Assert.assertEquals(NODE_1, entry.getValue());
                Assert.assertEquals(entry.getKey(), response.getFirstHeader(SimpleServlet.SESSION_ID_HEADER).getValue());
            } finally {
                HttpClientUtils.closeQuietly(response);
            }

            // Let's do this twice to have more debug info if failover is slow.
            response = client.execute(new HttpGet(uri1));
            try {
                Assert.assertEquals(HttpServletResponse.SC_OK, response.getStatusLine().getStatusCode());
                Assert.assertEquals(2, Integer.parseInt(response.getFirstHeader(SimpleServlet.VALUE_HEADER).getValue()));
                Map.Entry<String, String> entry = parseSessionRoute(response);
                Assert.assertNull(entry);
            } finally {
                HttpClientUtils.closeQuietly(response);
            }

            // Gracefully shutdown the 1st container.
            lifecycle.stop(NODE_1);

            this.establishTopology(baseURL2, NODE_2);

            // Now check on the 2nd server

            // Note that this DOES rely on the fact that both servers are running on the "same" domain,
            // which is '127.0.0.0'. Otherwise you will have to spoof cookies. @Rado

            response = client.execute(new HttpGet(uri2));
            try {
                Assert.assertEquals(HttpServletResponse.SC_OK, response.getStatusLine().getStatusCode());
                Assert.assertEquals("Session failed to replicate after container 1 was shutdown.", 3, Integer.parseInt(response.getFirstHeader(SimpleServlet.VALUE_HEADER).getValue()));
                Map.Entry<String, String> entry = parseSessionRoute(response);
                Assert.assertEquals(NODE_2, entry.getValue());
                Assert.assertEquals(entry.getKey(), response.getFirstHeader(SimpleServlet.SESSION_ID_HEADER).getValue());
            } finally {
                HttpClientUtils.closeQuietly(response);
            }

            // Let's do one more check.
            response = client.execute(new HttpGet(uri2));
            try {
                Assert.assertEquals(HttpServletResponse.SC_OK, response.getStatusLine().getStatusCode());
                Assert.assertEquals(4, Integer.parseInt(response.getFirstHeader(SimpleServlet.VALUE_HEADER).getValue()));
                Map.Entry<String, String> entry = parseSessionRoute(response);
                Assert.assertNull(entry);
            } finally {
                HttpClientUtils.closeQuietly(response);
            }

            lifecycle.start(NODE_1);

            this.establishTopology(baseURL2, NODES);

            response = client.execute(new HttpGet(uri2));
            try {
                Assert.assertEquals(HttpServletResponse.SC_OK, response.getStatusLine().getStatusCode());
                Assert.assertEquals("Session failed to replicate after container 1 was brough up.", 5, Integer.parseInt(response.getFirstHeader(SimpleServlet.VALUE_HEADER).getValue()));
                Map.Entry<String, String> entry = parseSessionRoute(response);
                Assert.assertNull(entry);
            } finally {
                HttpClientUtils.closeQuietly(response);
            }

            // Let's do this twice to have more debug info if failover is slow.
            response = client.execute(new HttpGet(uri2));
            try {
                Assert.assertEquals(HttpServletResponse.SC_OK, response.getStatusLine().getStatusCode());
                Assert.assertEquals(6, Integer.parseInt(response.getFirstHeader(SimpleServlet.VALUE_HEADER).getValue()));
                Map.Entry<String, String> entry = parseSessionRoute(response);
                Assert.assertNull(entry);
            } finally {
                HttpClientUtils.closeQuietly(response);
            }

            // Gracefully shutdown the 1st container.
            lifecycle.stop(NODE_2);

            this.establishTopology(baseURL1, NODE_1);

            // Now check on the 2nd server

            // Note that this DOES rely on the fact that both servers are running on the "same" domain,
            // which is '127.0.0.0'. Otherwise you will have to spoof cookies. @Rado

            response = client.execute(new HttpGet(uri1));
            try {
                Assert.assertEquals(HttpServletResponse.SC_OK, response.getStatusLine().getStatusCode());
                Assert.assertEquals("Session failed to replicate after container 1 was shutdown.", 7, Integer.parseInt(response.getFirstHeader(SimpleServlet.VALUE_HEADER).getValue()));
                Map.Entry<String, String> entry = parseSessionRoute(response);
                Assert.assertEquals(NODE_1, entry.getValue());
                Assert.assertEquals(entry.getKey(), response.getFirstHeader(SimpleServlet.SESSION_ID_HEADER).getValue());
            } finally {
                HttpClientUtils.closeQuietly(response);
            }

            // Let's do one more check.
            response = client.execute(new HttpGet(uri1));
            try {
                Assert.assertEquals(HttpServletResponse.SC_OK, response.getStatusLine().getStatusCode());
                Assert.assertEquals(8, Integer.parseInt(response.getFirstHeader(SimpleServlet.VALUE_HEADER).getValue()));
                Map.Entry<String, String> entry = parseSessionRoute(response);
                Assert.assertNull(entry);
            } finally {
                HttpClientUtils.closeQuietly(response);
            }

            lifecycle.start(NODE_2);

            this.establishTopology(baseURL1, NODES);

            response = client.execute(new HttpGet(uri1));
            try {
                Assert.assertEquals(HttpServletResponse.SC_OK, response.getStatusLine().getStatusCode());
                Assert.assertEquals("Session failed to replicate after container 1 was brough up.", 9, Integer.parseInt(response.getFirstHeader(SimpleServlet.VALUE_HEADER).getValue()));
                Map.Entry<String, String> entry = parseSessionRoute(response);
                Assert.assertNull(entry);
            } finally {
                HttpClientUtils.closeQuietly(response);
            }

            // Let's do this twice to have more debug info if failover is slow.
            response = client.execute(new HttpGet(uri1));
            try {
                Assert.assertEquals(HttpServletResponse.SC_OK, response.getStatusLine().getStatusCode());
                Assert.assertEquals(10, Integer.parseInt(response.getFirstHeader(SimpleServlet.VALUE_HEADER).getValue()));
                Map.Entry<String, String> entry = parseSessionRoute(response);
                Assert.assertNull(entry);
            } finally {
                HttpClientUtils.closeQuietly(response);
            }
        } finally {
            HttpClientUtils.closeQuietly(client);
        }
    }

    private void establishTopology(URL baseURL, String... nodes) throws URISyntaxException, IOException {
        ClusterHttpClientUtil.establishTopology(baseURL, "web", this.getDeploymentName(), nodes);
    }
}
