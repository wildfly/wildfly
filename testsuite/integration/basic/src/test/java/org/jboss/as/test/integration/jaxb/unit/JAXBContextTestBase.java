/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.integration.jaxb.unit;

import java.io.FilePermission;
import java.io.IOException;
import java.lang.reflect.ReflectPermission;
import java.net.URL;

import jakarta.xml.bind.JAXBContextFactory;

import static org.assertj.core.api.Assertions.assertThat;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.util.EntityUtils;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.as.test.integration.jaxb.FakeJAXBContextFactory;
import org.jboss.as.test.integration.jaxb.JAXBContextServlet;
import org.jboss.as.test.integration.jaxb.JAXBUsageServlet;
import org.jboss.as.test.integration.jaxb.bindings.Items;
import org.jboss.as.test.integration.jaxb.bindings.ObjectFactory;
import org.jboss.as.test.integration.jaxb.bindings.PurchaseOrderType;
import org.jboss.as.test.integration.jaxb.bindings.USAddress;
import org.jboss.as.test.shared.PermissionUtils;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.jupiter.api.Assertions;

/**
 * <p>Base class for all the JAXB loading tests</p>
 *
 * @author rmartinc
 */
public abstract class JAXBContextTestBase {

    protected static final String WEB_APP_INTERNAL_CONTEXT = "jaxb-internal-webapp";
    protected static final String WEB_APP_CUSTOM_CONTEXT = "jaxb-custom-webapp";

    protected static final String JAKARTA_FACTORY_PROP_NAME = JAXBContextFactory.class.getName();
    protected static final String JAVAX_FACTORY_PROP_NAME = JAXBContextFactory.class.getName().replaceFirst("jakarta.", "javax.");
    protected static final String JAVAX_JAXB_FACTORY_CLASS = "com.sun.xml.bind.v2.JAXBContextFactory";
    protected static final String JAKARTA_JAXB_FACTORY_CLASS = "org.glassfish.jaxb.runtime.v2.JAXBContextFactory";
    protected static final String CUSTOM_JAXB_FACTORY_CLASS = FakeJAXBContextFactory.class.getName();
    protected static final String JAXB_PROPERTIES_FILE = "WEB-INF/classes/org/jboss/as/test/integration/jaxb/bindings/jaxb.properties";
    protected static final String SERVICES_FILE = "META-INF/services/" + JAKARTA_FACTORY_PROP_NAME;
    protected static final String PERMISSIONS_FILE = "META-INF/permissions.xml";

    @ArquillianResource
    protected URL url;


    public static WebArchive createInternalDeployment() {
        final WebArchive war = ShrinkWrap.create(WebArchive.class, WEB_APP_INTERNAL_CONTEXT + ".war");
        war.addClasses(JAXBContextServlet.class, JAXBUsageServlet.class, Items.class, ObjectFactory.class, PurchaseOrderType.class, USAddress.class);
        war.add(PermissionUtils.createPermissionsXmlAsset(
                new RuntimePermission("accessDeclaredMembers"),
                new ReflectPermission("suppressAccessChecks"),
                new FilePermission("<<ALL FILES>>", "read")), PERMISSIONS_FILE);
        return war;
    }

    public static WebArchive createCustomDeployment() {
        final WebArchive war = ShrinkWrap.create(WebArchive.class, WEB_APP_CUSTOM_CONTEXT + ".war");
        war.addClasses(JAXBContextServlet.class, Items.class, ObjectFactory.class, PurchaseOrderType.class, USAddress.class, FakeJAXBContextFactory.class);
        return war;
    }

    protected void testDeafultImplementation(URL url) throws IOException {
        // test the internal implementation is returned
        try (CloseableHttpClient httpClient = HttpClientBuilder.create().build()) {
            final String requestURL = url.toExternalForm() + JAXBContextServlet.URL_PATTERN;
            final HttpGet request = new HttpGet(requestURL);
            final HttpResponse response = httpClient.execute(request);
            int statusCode = response.getStatusLine().getStatusCode();
            Assertions.assertEquals(200, statusCode, "Unexpected status code");
            final HttpEntity entity = response.getEntity();
            Assertions.assertNotNull(entity, "Response message from servlet was null");
            final String responseMessage = EntityUtils.toString(entity);
            assertThat(responseMessage).containsAnyOf("/com/sun/xml/bind/v2/runtime/JAXBContextImpl", "/org/glassfish/jaxb/runtime/v2/runtime/JAXBContextImpl");
        }
        // test it works
        try (CloseableHttpClient httpClient = HttpClientBuilder.create().build()) {
            final String requestURL = url.toExternalForm() + JAXBUsageServlet.URL_PATTERN;
            final HttpGet request = new HttpGet(requestURL);
            final HttpResponse response = httpClient.execute(request);
            int statusCode = response.getStatusLine().getStatusCode();
            Assertions.assertEquals(200, statusCode, "Unexpected status code");
            final HttpEntity entity = response.getEntity();
            Assertions.assertNotNull(entity, "Response message from servlet was null");
            final String responseMessage = EntityUtils.toString(entity);
            Assertions.assertEquals("Mill Valley", responseMessage.trim(), "Wrong return value");
        }
    }

    protected void testCustomImplementation(URL url) throws IOException {
        // test the custom jaxb context is returned
        try (CloseableHttpClient httpClient = HttpClientBuilder.create().build()) {
            final String requestURL = url.toExternalForm() + JAXBContextServlet.URL_PATTERN;
            final HttpGet request = new HttpGet(requestURL);
            final HttpResponse response = httpClient.execute(request);
            int statusCode = response.getStatusLine().getStatusCode();
            Assertions.assertEquals(200, statusCode, "Unexpected status code");
            final HttpEntity entity = response.getEntity();
            Assertions.assertNotNull(entity, "Response message from servlet was null");
            final String responseMessage = EntityUtils.toString(entity);
            Assertions.assertEquals("FakeJAXBContext", responseMessage, "Fake context is returned");
        }
    }
}
