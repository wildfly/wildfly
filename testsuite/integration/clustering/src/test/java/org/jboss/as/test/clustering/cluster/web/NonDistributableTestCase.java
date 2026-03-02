/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.clustering.cluster.web;

import java.net.URI;
import java.net.URL;
import java.util.Map;

import jakarta.servlet.http.HttpServletResponse;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.utils.HttpClientUtils;
import org.apache.http.impl.client.CloseableHttpClient;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.OperateOnDeployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
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
 * Validate that non-distributable web applications play nicely with a load balancer using sticky sessions.
 *
 * @author Paul Ferraro
 */
@ExtendWith(ArquillianExtension.class)
@RunAsClient
public class NonDistributableTestCase extends AbstractClusteringTestCase {

    private static final String MODULE_NAME = NonDistributableTestCase.class.getSimpleName();

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
        return war;
    }

    @Test
    void test(@ArquillianResource(SimpleServlet.class) @OperateOnDeployment(DEPLOYMENT_1) URL baseURL1,
              @ArquillianResource(SimpleServlet.class) @OperateOnDeployment(DEPLOYMENT_2) URL baseURL2) throws Exception {
        URI uri1 = SimpleServlet.createURI(baseURL1);
        URI uri2 = SimpleServlet.createURI(baseURL2);

        try (CloseableHttpClient client = TestHttpClientUtils.promiscuousCookieHttpClient()) {
            HttpResponse response = client.execute(new HttpGet(uri1));
            try {
                assertEquals(HttpServletResponse.SC_OK, response.getStatusLine().getStatusCode());
                assertEquals(1, Integer.parseInt(response.getFirstHeader(SimpleServlet.VALUE_HEADER).getValue()));
                Map.Entry<String, String> entry = parseSessionRoute(response);
                // Session identifier should contain the route for this node
                assertEquals(NODE_1, entry.getValue());
                // Session identifier seen by servlet should *not* contain the route
                assertEquals(entry.getKey(), response.getFirstHeader(SimpleServlet.SESSION_ID_HEADER).getValue());
            } finally {
                HttpClientUtils.closeQuietly(response);
            }

            response = client.execute(new HttpGet(uri1));
            try {
                assertEquals(HttpServletResponse.SC_OK, response.getStatusLine().getStatusCode());
                assertEquals(2, Integer.parseInt(response.getFirstHeader(SimpleServlet.VALUE_HEADER).getValue()));
                Map.Entry<String, String> entry = parseSessionRoute(response);
                assertNull(entry);
            } finally {
                HttpClientUtils.closeQuietly(response);
            }

            response = client.execute(new HttpGet(uri2));
            try {
                assertEquals(HttpServletResponse.SC_OK, response.getStatusLine().getStatusCode());
                // Session should not be replicated
                assertEquals(1, Integer.parseInt(response.getFirstHeader(SimpleServlet.VALUE_HEADER).getValue()));
                Map.Entry<String, String> entry = parseSessionRoute(response);
                // Session identifier should contain the route for this node
                assertEquals(NODE_2, entry.getValue());
                // Session identifier seen by servlet should *not* contain the route
                assertEquals(entry.getKey(), response.getFirstHeader(SimpleServlet.SESSION_ID_HEADER).getValue());
            } finally {
                HttpClientUtils.closeQuietly(response);
            }

            response = client.execute(new HttpGet(uri2));
            try {
                assertEquals(HttpServletResponse.SC_OK, response.getStatusLine().getStatusCode());
                assertEquals(2, Integer.parseInt(response.getFirstHeader(SimpleServlet.VALUE_HEADER).getValue()));
                Map.Entry<String, String> entry = parseSessionRoute(response);
                assertNull(entry);
            } finally {
                HttpClientUtils.closeQuietly(response);
            }
        }
    }
}
