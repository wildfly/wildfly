/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.clustering.cluster.infinispan;

import static org.jboss.as.test.shared.PermissionUtils.createPermissionsXmlAsset;
import static org.junit.jupiter.api.Assertions.*;

import java.lang.reflect.ReflectPermission;
import java.net.URL;
import javax.management.MBeanPermission;

import jakarta.servlet.http.HttpServletResponse;

import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.impl.client.CloseableHttpClient;
import org.infinispan.protostream.SerializationContextInitializer;
import org.jboss.arquillian.container.test.api.OperateOnDeployment;
import org.jboss.arquillian.junit5.ArquillianExtension;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.as.test.clustering.cluster.AbstractClusteringTestCase;
import org.jboss.as.test.clustering.cluster.infinispan.bean.Cache;
import org.jboss.as.test.clustering.cluster.infinispan.bean.InfinispanCacheSerializationContextInitializer;
import org.jboss.as.test.clustering.cluster.infinispan.servlet.InfinispanCacheServlet;
import org.jboss.as.test.http.util.TestHttpClientUtils;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

/**
 * Validates marshallability of application specific cache entries.
 * @author Paul Ferraro
 */
@ExtendWith(ArquillianExtension.class)
public abstract class AbstractCacheTestCase extends AbstractClusteringTestCase {

    protected static Archive<?> createDeployment(Class<? extends Cache> beanClass, String module) {
        WebArchive war = ShrinkWrap.create(WebArchive.class, beanClass.getSimpleName() + ".war");
        war.addPackage(InfinispanCacheServlet.class.getPackage());
        war.addPackage(Cache.class.getPackage());
        war.addClass(beanClass);
        war.addAsResource(new StringAsset(String.format("Manifest-Version: 1.0\nDependencies: %s, org.infinispan.commons, org.infinispan.protostream\n", module)), "META-INF/MANIFEST.MF");
        war.addAsServiceProvider(SerializationContextInitializer.class.getName(), InfinispanCacheSerializationContextInitializer.class.getName() + "Impl");
        war.addAsManifestResource(createPermissionsXmlAsset(
                new MBeanPermission("-#-[-]", "queryNames"),
                new MBeanPermission("org.infinispan.*[org.wildfly.clustering.infinispan:*,type=*]", "registerMBean"),
                new ReflectPermission("suppressAccessChecks"),
                new RuntimePermission("accessDeclaredMembers"),
                new RuntimePermission("getClassLoader")
        ), "permissions.xml");
        return war;
    }

    @Test
    public void test(@ArquillianResource @OperateOnDeployment(DEPLOYMENT_1) URL baseURL1, @ArquillianResource @OperateOnDeployment(DEPLOYMENT_2) URL baseURL2) throws Exception {
        System.out.println(InfinispanCacheServlet.createURI(baseURL1, "foo", "bar"));
        try (CloseableHttpClient client = TestHttpClientUtils.promiscuousCookieHttpClient()) {
            try (CloseableHttpResponse response = client.execute(new HttpPut(InfinispanCacheServlet.createURI(baseURL1, "foo", "bar")))) {
                assertEquals(HttpServletResponse.SC_OK, response.getStatusLine().getStatusCode());
                assertFalse(response.containsHeader(InfinispanCacheServlet.RESULT));
            }

            try (CloseableHttpResponse response = client.execute(new HttpGet(InfinispanCacheServlet.createURI(baseURL2, "foo")))) {
                assertEquals(HttpServletResponse.SC_OK, response.getStatusLine().getStatusCode());
                assertTrue(response.containsHeader(InfinispanCacheServlet.RESULT));
                assertEquals("bar", response.getFirstHeader(InfinispanCacheServlet.RESULT).getValue());
            }

            try (CloseableHttpResponse response = client.execute(new HttpDelete(InfinispanCacheServlet.createURI(baseURL1, "foo")))) {
                assertEquals(HttpServletResponse.SC_OK, response.getStatusLine().getStatusCode());
                assertTrue(response.containsHeader(InfinispanCacheServlet.RESULT));
                assertEquals("bar", response.getFirstHeader(InfinispanCacheServlet.RESULT).getValue());
            }

            try (CloseableHttpResponse response = client.execute(new HttpGet(InfinispanCacheServlet.createURI(baseURL2, "foo")))) {
                assertEquals(HttpServletResponse.SC_OK, response.getStatusLine().getStatusCode());
                assertFalse(response.containsHeader(InfinispanCacheServlet.RESULT));
            }
        }
    }
}
