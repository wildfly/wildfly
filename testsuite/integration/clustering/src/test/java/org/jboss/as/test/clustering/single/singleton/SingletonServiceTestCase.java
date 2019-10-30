/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2012, Red Hat, Inc., and individual contributors
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
package org.jboss.as.test.clustering.single.singleton;

import static org.jboss.as.test.clustering.cluster.AbstractClusteringTestCase.NODE_1;
import static org.jboss.as.test.shared.integration.ejb.security.PermissionUtils.createPermissionsXmlAsset;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;

import javax.servlet.http.HttpServletResponse;

import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.utils.HttpClientUtils;
import org.apache.http.impl.client.CloseableHttpClient;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.as.server.security.ServerPermission;
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

/**
 * Validates that a singleton service works in a non-clustered environment.
 * @author Paul Ferraro
 */
@RunWith(Arquillian.class)
public class SingletonServiceTestCase {
    private static final String MODULE_NAME = SingletonServiceTestCase.class.getSimpleName();

    @Deployment(testable = false)
    public static Archive<?> deployment() {
        WebArchive war = ShrinkWrap.create(WebArchive.class, MODULE_NAME + ".war");
        war.addPackage(NodeServiceServlet.class.getPackage());
        war.setManifest(new StringAsset("Manifest-Version: 1.0\nDependencies: org.jboss.as.server\n"));
        war.addAsManifestResource(createPermissionsXmlAsset(
                new RuntimePermission("getClassLoader"), // See org.jboss.as.server.deployment.service.ServiceActivatorProcessor#deploy()
                new ServerPermission("useServiceRegistry"), // See org.jboss.as.server.deployment.service.SecuredServiceRegistry
                new ServerPermission("getCurrentServiceContainer")
        ), "permissions.xml");
        war.addAsServiceProvider(org.jboss.msc.service.ServiceActivator.class, NodeServiceActivator.class);
        return war;
    }

    @Test
    public void testSingletonService(@ArquillianResource(NodeServiceServlet.class) URL baseURL) throws IOException, URISyntaxException {

        // URLs look like "http://IP:PORT/singleton/service"
        URI defaultURI = NodeServiceServlet.createURI(baseURL, NodeServiceActivator.DEFAULT_SERVICE_NAME);
        URI quorumURI = NodeServiceServlet.createURI(baseURL, NodeServiceActivator.QUORUM_SERVICE_NAME);

        try (CloseableHttpClient client = TestHttpClientUtils.promiscuousCookieHttpClient()) {
            HttpResponse response = client.execute(new HttpGet(defaultURI));
            try {
                Assert.assertEquals(HttpServletResponse.SC_OK, response.getStatusLine().getStatusCode());
                Assert.assertTrue(response.containsHeader(NodeServiceServlet.NODE_HEADER));
                Assert.assertEquals(NODE_1, response.getFirstHeader(NodeServiceServlet.NODE_HEADER).getValue());
            } finally {
                HttpClientUtils.closeQuietly(response);
            }

            // Service should be started regardless of whether a quorum was required.
            response = client.execute(new HttpGet(quorumURI));
            try {
                Assert.assertEquals(HttpServletResponse.SC_OK, response.getStatusLine().getStatusCode());
                Assert.assertTrue(response.containsHeader(NodeServiceServlet.NODE_HEADER));
                Assert.assertEquals(NODE_1, response.getFirstHeader(NodeServiceServlet.NODE_HEADER).getValue());
            } finally {
                HttpClientUtils.closeQuietly(response);
            }
        }
    }
}
