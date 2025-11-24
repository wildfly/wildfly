/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.clustering.single.infinispan.xml;

import static org.jboss.as.test.shared.PermissionUtils.createPermissionsXmlAsset;

import java.io.FilePermission;
import java.io.IOException;
import java.lang.reflect.ReflectPermission;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.PropertyPermission;
import javax.management.MBeanServerPermission;

import jakarta.servlet.http.HttpServletResponse;

import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.utils.HttpClientUtils;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.util.EntityUtils;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.as.test.clustering.single.infinispan.xml.deployment.InfinispanXMLConfigurationServlet;
import org.jboss.as.test.clustering.single.web.SimpleServlet;
import org.jboss.as.test.http.util.TestHttpClientUtils;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.jboss.shrinkwrap.descriptor.api.Descriptors;
import org.jboss.shrinkwrap.descriptor.api.spec.se.manifest.ManifestDescriptor;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Tests that Infinispan can be configured via an infinispan.xml file packaged directly in WEB-INF/classes.
 *
 * @author Radoslav Husar
 * @see <a href="https://issues.redhat.com/browse/WFLY-21156">WFLY-21156</a>
 */
@RunWith(Arquillian.class)
@RunAsClient
public class InfinispanXMLConfigurationTestCase {

    private static final String MODULE_NAME = InfinispanXMLConfigurationTestCase.class.getSimpleName();


    @Deployment(testable = false)
    public static Archive<?> deployment() {
        return ShrinkWrap
                .create(WebArchive.class, MODULE_NAME + ".war")
                .addPackage(InfinispanXMLConfigurationServlet.class.getPackage())
                .addAsResource(InfinispanXMLConfigurationServlet.class.getPackage(), "infinispan.xml", "infinispan.xml")
                .setWebXML(SimpleServlet.class.getPackage(), "web.xml")
                .setManifest(new StringAsset(Descriptors.create(ManifestDescriptor.class).attribute("Dependencies", "org.infinispan").exportAsString()))
                .addAsManifestResource(createPermissionsXmlAsset(
                        // n.b. Infinispan swallows this exception when parsing the XML configuration file and fails cryptically if permission is missing
                        new FilePermission("<<ALL FILES>>", "read,write,delete"),
                        new MBeanServerPermission("findMBeanServer"),
                        new PropertyPermission("*", "read,write"),
                        new ReflectPermission("suppressAccessChecks"),
                        new RuntimePermission("accessDeclaredMembers"),
                        new RuntimePermission("getClassLoader"),
                        new RuntimePermission("setContextClassLoader")
                ), "permissions.xml");
    }

    @Test
    public void testInfinispanXmlConfiguration(@ArquillianResource URL baseURL) throws IOException, URISyntaxException {
        URI uri = InfinispanXMLConfigurationServlet.createURI(baseURL);

        try (CloseableHttpClient client = TestHttpClientUtils.promiscuousCookieHttpClient()) {
            HttpResponse response = client.execute(new HttpGet(uri));
            try {
                Assert.assertEquals(HttpServletResponse.SC_OK, response.getStatusLine().getStatusCode());
                String content = EntityUtils.toString(response.getEntity()).trim();
                Assert.assertEquals("", content);
            } finally {
                HttpClientUtils.closeQuietly(response);
            }
        }
    }
}
