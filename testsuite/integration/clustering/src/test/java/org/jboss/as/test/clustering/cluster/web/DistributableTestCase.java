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
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import javax.servlet.http.HttpServletResponse;

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.utils.HttpClientUtils;
import org.apache.http.impl.client.CloseableHttpClient;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.OperateOnDeployment;
import org.jboss.arquillian.container.test.api.TargetsContainer;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.as.test.clustering.cluster.AbstractClusteringTestCase;
import org.jboss.as.test.clustering.single.web.Mutable;
import org.jboss.as.test.clustering.single.web.SimpleServlet;
import org.jboss.as.test.http.util.TestHttpClientUtils;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Validate that <distributable/> works for a two-node cluster.
 *
 * @author Paul Ferraro
 * @author Radoslav Husar
 */
@RunWith(Arquillian.class)
public class DistributableTestCase extends AbstractClusteringTestCase {

    private static final String MODULE_NAME = DistributableTestCase.class.getSimpleName();

    private static final int REQUEST_DURATION = 10000;

    @Deployment(name = DEPLOYMENT_1, managed = false, testable = false)
    @TargetsContainer(NODE_1)
    public static Archive<?> deployment1() {
        return getDeployment();
    }

    @Deployment(name = DEPLOYMENT_2, managed = false, testable = false)
    @TargetsContainer(NODE_2)
    public static Archive<?> deployment2() {
        return getDeployment();
    }

    private static Archive<?> getDeployment() {
        WebArchive war = ShrinkWrap.create(WebArchive.class, MODULE_NAME + ".war");
        war.addClasses(SimpleServlet.class, Mutable.class);
        war.setWebXML(DistributableTestCase.class.getPackage(), "web.xml");
        return war;
    }

    @Test
    @OperateOnDeployment(DEPLOYMENT_1)
    public void testSerialized(@ArquillianResource(SimpleServlet.class) URL baseURL) throws IOException, URISyntaxException {

        // returns the URL of the deployment (http://127.0.0.1:8180/distributable)
        URI uri = SimpleServlet.createURI(baseURL);

        try (CloseableHttpClient client = TestHttpClientUtils.promiscuousCookieHttpClient()) {
            HttpResponse response = client.execute(new HttpGet(uri));
            try {
                Assert.assertEquals(HttpServletResponse.SC_OK, response.getStatusLine().getStatusCode());
                Assert.assertEquals(1, Integer.parseInt(response.getFirstHeader("value").getValue()));
                Assert.assertFalse(Boolean.valueOf(response.getFirstHeader("serialized").getValue()));
            } finally {
                HttpClientUtils.closeQuietly(response);
            }

            response = client.execute(new HttpGet(uri));
            try {
                Assert.assertEquals(HttpServletResponse.SC_OK, response.getStatusLine().getStatusCode());
                Assert.assertEquals(2, Integer.parseInt(response.getFirstHeader("value").getValue()));
                // This won't be true unless we have somewhere to which to replicate
                Assert.assertTrue(Boolean.valueOf(response.getFirstHeader("serialized").getValue()));
            } finally {
                HttpClientUtils.closeQuietly(response);
            }
        }
    }

    @Test
    @OperateOnDeployment(DEPLOYMENT_2) // For change, operate on the 2nd deployment first
    public void testSessionReplication(
            @ArquillianResource(SimpleServlet.class) @OperateOnDeployment(DEPLOYMENT_1) URL baseURL1,
            @ArquillianResource(SimpleServlet.class) @OperateOnDeployment(DEPLOYMENT_2) URL baseURL2)
            throws IOException, URISyntaxException, InterruptedException {

        URI url1 = SimpleServlet.createURI(baseURL1);
        URI url2 = SimpleServlet.createURI(baseURL2);

        try (CloseableHttpClient client = TestHttpClientUtils.promiscuousCookieHttpClient()) {
            HttpResponse response = client.execute(new HttpGet(url1));
            try {
                Assert.assertEquals(HttpServletResponse.SC_OK, response.getStatusLine().getStatusCode());
                Assert.assertEquals(1, Integer.parseInt(response.getFirstHeader("value").getValue()));
            } finally {
                HttpClientUtils.closeQuietly(response);
            }

            // Lets do this twice to have more debug info if failover is slow.
            response = client.execute(new HttpGet(url1));
            try {
                Assert.assertEquals(HttpServletResponse.SC_OK, response.getStatusLine().getStatusCode());
                Assert.assertEquals(2, Integer.parseInt(response.getFirstHeader("value").getValue()));
            } finally {
                HttpClientUtils.closeQuietly(response);
            }

            // Lets wait for the session to replicate
            Thread.sleep(GRACE_TIME_TO_REPLICATE);

            // Now check on the 2nd server
            response = client.execute(new HttpGet(url2));
            try {
                Assert.assertEquals(HttpServletResponse.SC_OK, response.getStatusLine().getStatusCode());
                Assert.assertEquals(3, Integer.parseInt(response.getFirstHeader("value").getValue()));
            } finally {
                HttpClientUtils.closeQuietly(response);
            }

            // Lets do one more check.
            response = client.execute(new HttpGet(url2));
            try {
                Assert.assertEquals(HttpServletResponse.SC_OK, response.getStatusLine().getStatusCode());
                Assert.assertEquals(4, Integer.parseInt(response.getFirstHeader("value").getValue()));
            } finally {
                HttpClientUtils.closeQuietly(response);
            }
        }
    }

    /**
     * Test that a session is gracefully served when a clustered application is undeployed.
     */
    @Test
    public void testGracefulServeOnUndeploy(
            @ArquillianResource(SimpleServlet.class) @OperateOnDeployment(DEPLOYMENT_1) URL baseURL1)
            throws Exception {
        this.testGracefulServe(baseURL1, new RedeployLifecycle());
    }

    /**
     * Test that a session is gracefully served when clustered AS instanced is shutdown.
     */
    @Test
    public void testGracefulServeOnShutdown(
            @ArquillianResource(SimpleServlet.class) @OperateOnDeployment(DEPLOYMENT_1) URL baseURL1)
            throws Exception {
        this.testGracefulServe(baseURL1, new RestartLifecycle());
    }

    private void testGracefulServe(URL baseURL, Lifecycle lifecycle) throws URISyntaxException, IOException, InterruptedException {

        try (CloseableHttpClient client = TestHttpClientUtils.promiscuousCookieHttpClient()) {
            URI uri = SimpleServlet.createURI(baseURL);

            // Make sure a normal request will succeed
            HttpResponse response = client.execute(new HttpGet(uri));
            try {
                Assert.assertEquals(HttpServletResponse.SC_OK, response.getStatusLine().getStatusCode());
            } finally {
                HttpClientUtils.closeQuietly(response);
            }

            // Send a long request - in parallel
            URI longRunningURI = SimpleServlet.createURI(baseURL, REQUEST_DURATION);
            ExecutorService executor = Executors.newSingleThreadExecutor();
            Future<HttpResponse> future = executor.submit(new RequestTask(client, longRunningURI));

            // Make sure long request has started
            Thread.sleep(1000);

            lifecycle.stop(NODE_1);

            // Get result of long request
            // This request should succeed since it initiated before server shutdown
            try {
                response = future.get();
                try {
                    Assert.assertEquals("Request should succeed since it initiated before undeply or shutdown.",
                            HttpServletResponse.SC_OK, response.getStatusLine().getStatusCode());
                } finally {
                    HttpClientUtils.closeQuietly(response);
                }
            } catch (ExecutionException e) {
                e.printStackTrace(System.err);
                Assert.fail(e.getCause().getMessage());
            }
        }
    }

    /**
     * Request task to request a long running URL and then undeploy / shutdown the server.
     */
    private class RequestTask implements Callable<HttpResponse> {

        private final HttpClient client;
        private final URI uri;

        RequestTask(HttpClient client, URI uri) {
            this.client = client;
            this.uri = uri;
        }

        @Override
        public HttpResponse call() throws Exception {
            return this.client.execute(new HttpGet(this.uri));
        }
    }
}
