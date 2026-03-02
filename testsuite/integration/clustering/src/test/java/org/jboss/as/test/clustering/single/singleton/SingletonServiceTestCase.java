/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.clustering.single.singleton;

import static org.jboss.as.test.clustering.cluster.AbstractClusteringTestCase.NODE_1;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.net.URI;
import java.net.URL;

import jakarta.servlet.http.HttpServletResponse;

import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.utils.HttpClientUtils;
import org.apache.http.impl.client.CloseableHttpClient;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit5.ArquillianExtension;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.as.test.clustering.cluster.singleton.service.NodeServiceActivator;
import org.jboss.as.test.clustering.cluster.singleton.service.NodeServiceServlet;
import org.jboss.as.test.http.util.TestHttpClientUtils;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

/**
 * Validates that a singleton service works in a non-clustered environment.
 * @author Paul Ferraro
 */
@ExtendWith(ArquillianExtension.class)
public class SingletonServiceTestCase {
    private static final String MODULE_NAME = SingletonServiceTestCase.class.getSimpleName();

    @Deployment(testable = false)
    public static Archive<?> deployment() {
        WebArchive war = ShrinkWrap.create(WebArchive.class, MODULE_NAME + ".war");
        war.addPackage(NodeServiceServlet.class.getPackage());
        war.setManifest(new StringAsset("Manifest-Version: 1.0\nDependencies: org.wildfly.service\n"));
        war.addAsServiceProvider(org.jboss.msc.service.ServiceActivator.class, NodeServiceActivator.class);
        return war;
    }

    @Test
    void singletonService(@ArquillianResource(NodeServiceServlet.class) URL baseURL) throws Exception {

        // URLs look like "http://IP:PORT/singleton/service"
        URI defaultURI = NodeServiceServlet.createURI(baseURL, NodeServiceActivator.DEFAULT_SERVICE_NAME);
        URI quorumURI = NodeServiceServlet.createURI(baseURL, NodeServiceActivator.QUORUM_SERVICE_NAME);

        try (CloseableHttpClient client = TestHttpClientUtils.promiscuousCookieHttpClient()) {
            HttpResponse response = client.execute(new HttpGet(defaultURI));
            try {
                assertEquals(HttpServletResponse.SC_OK, response.getStatusLine().getStatusCode());
                assertTrue(response.containsHeader(NodeServiceServlet.NODE_HEADER));
                assertEquals(NODE_1, response.getFirstHeader(NodeServiceServlet.NODE_HEADER).getValue());
            } finally {
                HttpClientUtils.closeQuietly(response);
            }

            // Service should be started regardless of whether a quorum was required.
            response = client.execute(new HttpGet(quorumURI));
            try {
                assertEquals(HttpServletResponse.SC_OK, response.getStatusLine().getStatusCode());
                assertTrue(response.containsHeader(NodeServiceServlet.NODE_HEADER));
                assertEquals(NODE_1, response.getFirstHeader(NodeServiceServlet.NODE_HEADER).getValue());
            } finally {
                HttpClientUtils.closeQuietly(response);
            }
        }
    }
}
