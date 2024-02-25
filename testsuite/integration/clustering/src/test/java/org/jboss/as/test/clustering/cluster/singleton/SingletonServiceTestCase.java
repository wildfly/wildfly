/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.clustering.cluster.singleton;

import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;

import jakarta.servlet.http.HttpServletResponse;

import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.utils.HttpClientUtils;
import org.apache.http.impl.client.CloseableHttpClient;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.OperateOnDeployment;
import org.jboss.arquillian.container.test.api.TargetsContainer;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.as.test.clustering.cluster.AbstractClusteringTestCase;
import org.jboss.as.test.clustering.cluster.singleton.service.NodeServiceActivator;
import org.jboss.as.test.clustering.cluster.singleton.service.NodeServiceServlet;
import org.jboss.as.test.http.util.TestHttpClientUtils;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(Arquillian.class)
public class SingletonServiceTestCase extends AbstractClusteringTestCase {

    private static final String MODULE_NAME = SingletonServiceTestCase.class.getSimpleName();

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
        WebArchive war = ShrinkWrap.create(WebArchive.class, MODULE_NAME + ".war");
        war.addPackage(NodeServiceServlet.class.getPackage());
        war.addAsServiceProvider(org.jboss.msc.service.ServiceActivator.class, NodeServiceActivator.class);
        war.setManifest(new StringAsset("Manifest-Version: 1.0\nDependencies: org.wildfly.service\n"));
        return war;
    }

    @Test
    public void testSingletonService(
            @ArquillianResource(NodeServiceServlet.class) @OperateOnDeployment(DEPLOYMENT_1) URL baseURL1,
            @ArquillianResource(NodeServiceServlet.class) @OperateOnDeployment(DEPLOYMENT_2) URL baseURL2)
            throws IOException, URISyntaxException {

        // Needed to be able to inject ArquillianResource
        stop(NODE_2);

        try (CloseableHttpClient client = TestHttpClientUtils.promiscuousCookieHttpClient()) {
            HttpResponse response = client.execute(new HttpGet(NodeServiceServlet.createURI(baseURL1, NodeServiceActivator.DEFAULT_SERVICE_NAME, NODE_1)));
            try {
                Assert.assertEquals(HttpServletResponse.SC_OK, response.getStatusLine().getStatusCode());
                Assert.assertTrue(response.containsHeader(NodeServiceServlet.NODE_HEADER));
                Assert.assertEquals(NODE_1, response.getFirstHeader(NodeServiceServlet.NODE_HEADER).getValue());
            } finally {
                HttpClientUtils.closeQuietly(response);
            }

            response = client.execute(new HttpGet(NodeServiceServlet.createURI(baseURL1, NodeServiceActivator.QUORUM_SERVICE_NAME)));
            try {
                Assert.assertEquals(HttpServletResponse.SC_OK, response.getStatusLine().getStatusCode());
                Assert.assertFalse(response.containsHeader(NodeServiceServlet.NODE_HEADER));
            } finally {
                HttpClientUtils.closeQuietly(response);
            }

            start(NODE_2);

            response = client.execute(new HttpGet(NodeServiceServlet.createURI(baseURL1, NodeServiceActivator.DEFAULT_SERVICE_NAME, NODE_1)));
            try {
                Assert.assertEquals(HttpServletResponse.SC_OK, response.getStatusLine().getStatusCode());
                Assert.assertTrue(response.containsHeader(NodeServiceServlet.NODE_HEADER));
                Assert.assertEquals(NODE_2, response.getFirstHeader(NodeServiceServlet.NODE_HEADER).getValue());
            } finally {
                HttpClientUtils.closeQuietly(response);
            }

            response = client.execute(new HttpGet(NodeServiceServlet.createURI(baseURL1, NodeServiceActivator.QUORUM_SERVICE_NAME, NODE_2)));
            try {
                Assert.assertEquals(HttpServletResponse.SC_OK, response.getStatusLine().getStatusCode());
                Assert.assertTrue(response.containsHeader(NodeServiceServlet.NODE_HEADER));
                Assert.assertEquals(NODE_2, response.getFirstHeader(NodeServiceServlet.NODE_HEADER).getValue());
            } finally {
                HttpClientUtils.closeQuietly(response);
            }

            response = client.execute(new HttpGet(NodeServiceServlet.createURI(baseURL2, NodeServiceActivator.DEFAULT_SERVICE_NAME, NODE_2)));
            try {
                Assert.assertEquals(HttpServletResponse.SC_OK, response.getStatusLine().getStatusCode());
                Assert.assertTrue(response.containsHeader(NodeServiceServlet.NODE_HEADER));
                Assert.assertEquals(NODE_2, response.getFirstHeader(NodeServiceServlet.NODE_HEADER).getValue());
            } finally {
                HttpClientUtils.closeQuietly(response);
            }

            response = client.execute(new HttpGet(NodeServiceServlet.createURI(baseURL2, NodeServiceActivator.QUORUM_SERVICE_NAME, NODE_2)));
            try {
                Assert.assertEquals(HttpServletResponse.SC_OK, response.getStatusLine().getStatusCode());
                Assert.assertTrue(response.containsHeader(NodeServiceServlet.NODE_HEADER));
                Assert.assertEquals(NODE_2, response.getFirstHeader(NodeServiceServlet.NODE_HEADER).getValue());
            } finally {
                HttpClientUtils.closeQuietly(response);
            }

            stop(NODE_2);

            response = client.execute(new HttpGet(NodeServiceServlet.createURI(baseURL1, NodeServiceActivator.DEFAULT_SERVICE_NAME, NODE_1)));
            try {
                Assert.assertEquals(HttpServletResponse.SC_OK, response.getStatusLine().getStatusCode());
                Assert.assertTrue(response.containsHeader(NodeServiceServlet.NODE_HEADER));
                Assert.assertEquals(NODE_1, response.getFirstHeader(NodeServiceServlet.NODE_HEADER).getValue());
            } finally {
                HttpClientUtils.closeQuietly(response);
            }

            response = client.execute(new HttpGet(NodeServiceServlet.createURI(baseURL1, NodeServiceActivator.QUORUM_SERVICE_NAME)));
            try {
                Assert.assertEquals(HttpServletResponse.SC_OK, response.getStatusLine().getStatusCode());
                Assert.assertFalse(response.containsHeader(NodeServiceServlet.NODE_HEADER));
            } finally {
                HttpClientUtils.closeQuietly(response);
            }

            start(NODE_2);

            response = client.execute(new HttpGet(NodeServiceServlet.createURI(baseURL1, NodeServiceActivator.DEFAULT_SERVICE_NAME, NODE_2)));
            try {
                Assert.assertEquals(HttpServletResponse.SC_OK, response.getStatusLine().getStatusCode());
                Assert.assertTrue(response.containsHeader(NodeServiceServlet.NODE_HEADER));
                Assert.assertEquals(NODE_2, response.getFirstHeader(NodeServiceServlet.NODE_HEADER).getValue());
            } finally {
                HttpClientUtils.closeQuietly(response);
            }

            response = client.execute(new HttpGet(NodeServiceServlet.createURI(baseURL1, NodeServiceActivator.QUORUM_SERVICE_NAME, NODE_2)));
            try {
                Assert.assertEquals(HttpServletResponse.SC_OK, response.getStatusLine().getStatusCode());
                Assert.assertTrue(response.containsHeader(NodeServiceServlet.NODE_HEADER));
                Assert.assertEquals(NODE_2, response.getFirstHeader(NodeServiceServlet.NODE_HEADER).getValue());
            } finally {
                HttpClientUtils.closeQuietly(response);
            }

            response = client.execute(new HttpGet(NodeServiceServlet.createURI(baseURL2, NodeServiceActivator.DEFAULT_SERVICE_NAME, NODE_2)));
            try {
                Assert.assertEquals(HttpServletResponse.SC_OK, response.getStatusLine().getStatusCode());
                Assert.assertTrue(response.containsHeader(NodeServiceServlet.NODE_HEADER));
                Assert.assertEquals(NODE_2, response.getFirstHeader(NodeServiceServlet.NODE_HEADER).getValue());
            } finally {
                HttpClientUtils.closeQuietly(response);
            }

            response = client.execute(new HttpGet(NodeServiceServlet.createURI(baseURL2, NodeServiceActivator.QUORUM_SERVICE_NAME, NODE_2)));
            try {
                Assert.assertEquals(HttpServletResponse.SC_OK, response.getStatusLine().getStatusCode());
                Assert.assertTrue(response.containsHeader(NodeServiceServlet.NODE_HEADER));
                Assert.assertEquals(NODE_2, response.getFirstHeader(NodeServiceServlet.NODE_HEADER).getValue());
            } finally {
                HttpClientUtils.closeQuietly(response);
            }

            stop(NODE_1);

            response = client.execute(new HttpGet(NodeServiceServlet.createURI(baseURL2, NodeServiceActivator.DEFAULT_SERVICE_NAME, NODE_2)));
            try {
                Assert.assertEquals(HttpServletResponse.SC_OK, response.getStatusLine().getStatusCode());
                Assert.assertTrue(response.containsHeader(NodeServiceServlet.NODE_HEADER));
                Assert.assertEquals(NODE_2, response.getFirstHeader(NodeServiceServlet.NODE_HEADER).getValue());
            } finally {
                HttpClientUtils.closeQuietly(response);
            }

            response = client.execute(new HttpGet(NodeServiceServlet.createURI(baseURL2, NodeServiceActivator.QUORUM_SERVICE_NAME)));
            try {
                Assert.assertEquals(HttpServletResponse.SC_OK, response.getStatusLine().getStatusCode());
                Assert.assertFalse(response.containsHeader(NodeServiceServlet.NODE_HEADER));
            } finally {
                HttpClientUtils.closeQuietly(response);
            }

            start(NODE_1);

            response = client.execute(new HttpGet(NodeServiceServlet.createURI(baseURL1, NodeServiceActivator.DEFAULT_SERVICE_NAME, NODE_2)));
            try {
                Assert.assertEquals(HttpServletResponse.SC_OK, response.getStatusLine().getStatusCode());
                Assert.assertTrue(response.containsHeader(NodeServiceServlet.NODE_HEADER));
                Assert.assertEquals(NODE_2, response.getFirstHeader(NodeServiceServlet.NODE_HEADER).getValue());
            } finally {
                HttpClientUtils.closeQuietly(response);
            }

            response = client.execute(new HttpGet(NodeServiceServlet.createURI(baseURL1, NodeServiceActivator.QUORUM_SERVICE_NAME, NODE_2)));
            try {
                Assert.assertEquals(HttpServletResponse.SC_OK, response.getStatusLine().getStatusCode());
                Assert.assertTrue(response.containsHeader(NodeServiceServlet.NODE_HEADER));
                Assert.assertEquals(NODE_2, response.getFirstHeader(NodeServiceServlet.NODE_HEADER).getValue());
            } finally {
                HttpClientUtils.closeQuietly(response);
            }

            response = client.execute(new HttpGet(NodeServiceServlet.createURI(baseURL2, NodeServiceActivator.DEFAULT_SERVICE_NAME, NODE_2)));
            try {
                Assert.assertEquals(HttpServletResponse.SC_OK, response.getStatusLine().getStatusCode());
                Assert.assertTrue(response.containsHeader(NodeServiceServlet.NODE_HEADER));
                Assert.assertEquals(NODE_2, response.getFirstHeader(NodeServiceServlet.NODE_HEADER).getValue());
            } finally {
                HttpClientUtils.closeQuietly(response);
            }

            response = client.execute(new HttpGet(NodeServiceServlet.createURI(baseURL2, NodeServiceActivator.QUORUM_SERVICE_NAME, NODE_2)));
            try {
                Assert.assertEquals(HttpServletResponse.SC_OK, response.getStatusLine().getStatusCode());
                Assert.assertTrue(response.containsHeader(NodeServiceServlet.NODE_HEADER));
                Assert.assertEquals(NODE_2, response.getFirstHeader(NodeServiceServlet.NODE_HEADER).getValue());
            } finally {
                HttpClientUtils.closeQuietly(response);
            }
        }
    }
}
