/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
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

import jakarta.servlet.http.HttpServletResponse;

import static org.junit.jupiter.api.Assertions.*;

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.utils.HttpClientUtils;
import org.apache.http.impl.client.CloseableHttpClient;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.OperateOnDeployment;
import org.jboss.arquillian.container.test.api.TargetsContainer;
import org.jboss.arquillian.junit5.ArquillianExtension;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.as.test.clustering.cluster.AbstractClusteringTestCase;
import org.jboss.as.test.clustering.single.web.Mutable;
import org.jboss.as.test.clustering.single.web.SimpleServlet;
import org.jboss.as.test.http.util.TestHttpClientUtils;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

/**
 * Validate that <distributable/> works for a two-node cluster.
 *
 * @author Paul Ferraro
 * @author Radoslav Husar
 */
@ExtendWith(ArquillianExtension.class)
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
    void serialized(@ArquillianResource(SimpleServlet.class) URL baseURL) throws Exception {

        // returns the URL of the deployment (http://127.0.0.1:8180/distributable)
        URI uri = SimpleServlet.createURI(baseURL);

        try (CloseableHttpClient client = TestHttpClientUtils.promiscuousCookieHttpClient()) {
            HttpResponse response = client.execute(new HttpGet(uri));
            try {
                assertEquals(HttpServletResponse.SC_OK, response.getStatusLine().getStatusCode());
                assertEquals(1, Integer.parseInt(response.getFirstHeader("value").getValue()));
                assertFalse(Boolean.valueOf(response.getFirstHeader("serialized").getValue()));
            } finally {
                HttpClientUtils.closeQuietly(response);
            }

            response = client.execute(new HttpGet(uri));
            try {
                assertEquals(HttpServletResponse.SC_OK, response.getStatusLine().getStatusCode());
                assertEquals(2, Integer.parseInt(response.getFirstHeader("value").getValue()));
                // This won't be true unless we have somewhere to which to replicate
                assertTrue(Boolean.valueOf(response.getFirstHeader("serialized").getValue()));
            } finally {
                HttpClientUtils.closeQuietly(response);
            }
        }
    }

    // For change, operate on the 2nd deployment first
    @Test
    @OperateOnDeployment(DEPLOYMENT_2)
    void sessionReplication(
            @ArquillianResource(SimpleServlet.class) @OperateOnDeployment(DEPLOYMENT_1) URL baseURL1,
            @ArquillianResource(SimpleServlet.class) @OperateOnDeployment(DEPLOYMENT_2) URL baseURL2)
            throws Exception {

        URI url1 = SimpleServlet.createURI(baseURL1);
        URI url2 = SimpleServlet.createURI(baseURL2);

        try (CloseableHttpClient client = TestHttpClientUtils.promiscuousCookieHttpClient()) {
            HttpResponse response = client.execute(new HttpGet(url1));
            try {
                assertEquals(HttpServletResponse.SC_OK, response.getStatusLine().getStatusCode());
                assertEquals(1, Integer.parseInt(response.getFirstHeader("value").getValue()));
            } finally {
                HttpClientUtils.closeQuietly(response);
            }

            // Lets do this twice to have more debug info if failover is slow.
            response = client.execute(new HttpGet(url1));
            try {
                assertEquals(HttpServletResponse.SC_OK, response.getStatusLine().getStatusCode());
                assertEquals(2, Integer.parseInt(response.getFirstHeader("value").getValue()));
            } finally {
                HttpClientUtils.closeQuietly(response);
            }

            // Lets wait for the session to replicate
            Thread.sleep(GRACE_TIME_TO_REPLICATE);

            // Now check on the 2nd server
            response = client.execute(new HttpGet(url2));
            try {
                assertEquals(HttpServletResponse.SC_OK, response.getStatusLine().getStatusCode());
                assertEquals(3, Integer.parseInt(response.getFirstHeader("value").getValue()));
            } finally {
                HttpClientUtils.closeQuietly(response);
            }

            // Lets do one more check.
            response = client.execute(new HttpGet(url2));
            try {
                assertEquals(HttpServletResponse.SC_OK, response.getStatusLine().getStatusCode());
                assertEquals(4, Integer.parseInt(response.getFirstHeader("value").getValue()));
            } finally {
                HttpClientUtils.closeQuietly(response);
            }
        }
    }

    /**
     * Test that a session is gracefully served when a clustered application is undeployed.
     */
    @Test
    void gracefulServeOnUndeploy(
            @ArquillianResource(SimpleServlet.class) @OperateOnDeployment(DEPLOYMENT_1) URL baseURL1)
            throws Exception {
        this.testGracefulServe(baseURL1, new RedeployLifecycle());
    }

    /**
     * Test that a session is gracefully served when clustered AS instanced is shutdown.
     */
    @Test
    void gracefulServeOnShutdown(
            @ArquillianResource(SimpleServlet.class) @OperateOnDeployment(DEPLOYMENT_1) URL baseURL1)
            throws Exception {
        this.testGracefulServe(baseURL1, new GracefulRestartLifecycle());
    }

    private void testGracefulServe(URL baseURL, Lifecycle lifecycle) throws URISyntaxException, IOException, InterruptedException {

        try (CloseableHttpClient client = TestHttpClientUtils.promiscuousCookieHttpClient()) {
            URI uri = SimpleServlet.createURI(baseURL);

            // Make sure a normal request will succeed
            HttpResponse response = client.execute(new HttpGet(uri));
            try {
                assertEquals(HttpServletResponse.SC_OK, response.getStatusLine().getStatusCode());
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
                    assertEquals(HttpServletResponse.SC_OK,
                            response.getStatusLine().getStatusCode(), "Request should succeed since it initiated before undeply or shutdown.");
                } finally {
                    HttpClientUtils.closeQuietly(response);
                }
            } catch (ExecutionException e) {
                e.printStackTrace(System.err);
                fail(e.getCause().getMessage());
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
