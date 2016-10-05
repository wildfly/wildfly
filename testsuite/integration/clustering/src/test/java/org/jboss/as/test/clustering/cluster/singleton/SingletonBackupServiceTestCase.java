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
package org.jboss.as.test.clustering.cluster.singleton;

import static org.jboss.as.test.shared.integration.ejb.security.PermissionUtils.createPermissionsXmlAsset;

import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;

import javax.servlet.http.HttpServletResponse;

import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.utils.HttpClientUtils;
import org.apache.http.impl.client.CloseableHttpClient;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.OperateOnDeployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.container.test.api.TargetsContainer;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.as.server.security.ServerPermission;
import org.jboss.as.test.clustering.cluster.ClusterAbstractTestCase;
import org.jboss.as.test.clustering.cluster.singleton.service.ValueServiceActivator;
import org.jboss.as.test.clustering.cluster.singleton.service.ValueServiceServlet;
import org.jboss.as.test.http.util.TestHttpClientUtils;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(Arquillian.class)
@RunAsClient
public class SingletonBackupServiceTestCase extends ClusterAbstractTestCase {

    @Deployment(name = DEPLOYMENT_1, managed = false, testable = false)
    @TargetsContainer(CONTAINER_1)
    public static Archive<?> deployment0() {
        return createDeployment();
    }

    @Deployment(name = DEPLOYMENT_2, managed = false, testable = false)
    @TargetsContainer(CONTAINER_2)
    public static Archive<?> deployment1() {
        return createDeployment();
    }

    private static Archive<?> createDeployment() {
        WebArchive war = ShrinkWrap.create(WebArchive.class, "singleton.war");
        war.addPackage(ValueServiceServlet.class.getPackage());
        war.setManifest(new StringAsset("Manifest-Version: 1.0\nDependencies: org.jboss.as.server\n"));
        war.addAsServiceProvider(org.jboss.msc.service.ServiceActivator.class, ValueServiceActivator.class);
        war.addAsManifestResource(createPermissionsXmlAsset(
                new RuntimePermission("getClassLoader"), // See org.jboss.as.server.deployment.service.ServiceActivatorProcessor#deploy()
                new ServerPermission("useServiceRegistry"), // See org.jboss.as.server.deployment.service.SecuredServiceRegistry
                new ServerPermission("getCurrentServiceContainer")
        ), "permissions.xml");
        return war;
    }

    @Test
    public void testSingletonService(
            @ArquillianResource(ValueServiceServlet.class) @OperateOnDeployment(DEPLOYMENT_1) URL baseURL1,
            @ArquillianResource(ValueServiceServlet.class) @OperateOnDeployment(DEPLOYMENT_2) URL baseURL2)
            throws IOException, URISyntaxException {

        // Needed to be able to inject ArquillianResource
        stop(CONTAINER_2);

        try (CloseableHttpClient client = TestHttpClientUtils.promiscuousCookieHttpClient()) {
            HttpResponse response = client.execute(new HttpGet(ValueServiceServlet.createURI(baseURL1, ValueServiceActivator.SERVICE_NAME)));
            try {
                Assert.assertEquals(HttpServletResponse.SC_OK, response.getStatusLine().getStatusCode());
                Assert.assertEquals("true", response.getFirstHeader(ValueServiceServlet.PRIMARY_HEADER).getValue());
            } finally {
                HttpClientUtils.closeQuietly(response);
            }

            start(CONTAINER_2);

            response = client.execute(new HttpGet(ValueServiceServlet.createURI(baseURL1, ValueServiceActivator.SERVICE_NAME)));
            try {
                Assert.assertEquals(HttpServletResponse.SC_OK, response.getStatusLine().getStatusCode());
                Assert.assertEquals("true", response.getFirstHeader(ValueServiceServlet.PRIMARY_HEADER).getValue());
            } finally {
                HttpClientUtils.closeQuietly(response);
            }

            response = client.execute(new HttpGet(ValueServiceServlet.createURI(baseURL2, ValueServiceActivator.SERVICE_NAME)));
            try {
                Assert.assertEquals(HttpServletResponse.SC_OK, response.getStatusLine().getStatusCode());
                Assert.assertEquals("false", response.getFirstHeader(ValueServiceServlet.PRIMARY_HEADER).getValue());
            } finally {
                HttpClientUtils.closeQuietly(response);
            }

            stop(CONTAINER_2);

            response = client.execute(new HttpGet(ValueServiceServlet.createURI(baseURL1, ValueServiceActivator.SERVICE_NAME)));
            try {
                Assert.assertEquals(HttpServletResponse.SC_OK, response.getStatusLine().getStatusCode());
                Assert.assertEquals("true", response.getFirstHeader(ValueServiceServlet.PRIMARY_HEADER).getValue());
            } finally {
                HttpClientUtils.closeQuietly(response);
            }

            start(CONTAINER_2);

            response = client.execute(new HttpGet(ValueServiceServlet.createURI(baseURL1, ValueServiceActivator.SERVICE_NAME)));
            try {
                Assert.assertEquals(HttpServletResponse.SC_OK, response.getStatusLine().getStatusCode());
                Assert.assertEquals("true", response.getFirstHeader(ValueServiceServlet.PRIMARY_HEADER).getValue());
            } finally {
                HttpClientUtils.closeQuietly(response);
            }

            response = client.execute(new HttpGet(ValueServiceServlet.createURI(baseURL2, ValueServiceActivator.SERVICE_NAME)));
            try {
                Assert.assertEquals(HttpServletResponse.SC_OK, response.getStatusLine().getStatusCode());
                Assert.assertEquals("false", response.getFirstHeader(ValueServiceServlet.PRIMARY_HEADER).getValue());
            } finally {
                HttpClientUtils.closeQuietly(response);
            }

            stop(CONTAINER_1);

            response = client.execute(new HttpGet(ValueServiceServlet.createURI(baseURL2, ValueServiceActivator.SERVICE_NAME)));
            try {
                Assert.assertEquals(HttpServletResponse.SC_OK, response.getStatusLine().getStatusCode());
                Assert.assertEquals("true", response.getFirstHeader(ValueServiceServlet.PRIMARY_HEADER).getValue());
            } finally {
                HttpClientUtils.closeQuietly(response);
            }

            start(CONTAINER_1);

            response = client.execute(new HttpGet(ValueServiceServlet.createURI(baseURL1, ValueServiceActivator.SERVICE_NAME)));
            try {
                Assert.assertEquals(HttpServletResponse.SC_OK, response.getStatusLine().getStatusCode());
                Assert.assertEquals("false", response.getFirstHeader(ValueServiceServlet.PRIMARY_HEADER).getValue());
            } finally {
                HttpClientUtils.closeQuietly(response);
            }

            response = client.execute(new HttpGet(ValueServiceServlet.createURI(baseURL2, ValueServiceActivator.SERVICE_NAME)));
            try {
                Assert.assertEquals(HttpServletResponse.SC_OK, response.getStatusLine().getStatusCode());
                Assert.assertEquals("true", response.getFirstHeader(ValueServiceServlet.PRIMARY_HEADER).getValue());
            } finally {
                HttpClientUtils.closeQuietly(response);
            }
        }
    }
}
