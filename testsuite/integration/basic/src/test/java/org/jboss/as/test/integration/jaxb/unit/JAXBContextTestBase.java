/*
 * JBoss, Home of Professional Open Source
 * Copyright 2020 Red Hat, Inc., and individual contributors
 * as indicated by the @author tags.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jboss.as.test.integration.jaxb.unit;

import java.io.FilePermission;
import java.io.IOException;
import java.lang.reflect.ReflectPermission;
import java.net.URL;
import javax.xml.bind.JAXBContextFactory;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.util.EntityUtils;
import org.hamcrest.CoreMatchers;
import org.hamcrest.MatcherAssert;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.as.test.integration.jaxb.FakeJAXBContextFactory;
import org.jboss.as.test.integration.jaxb.JAXBContextServlet;
import org.jboss.as.test.integration.jaxb.JAXBUsageServlet;
import org.jboss.as.test.integration.jaxb.bindings.Items;
import org.jboss.as.test.integration.jaxb.bindings.ObjectFactory;
import org.jboss.as.test.integration.jaxb.bindings.PurchaseOrderType;
import org.jboss.as.test.integration.jaxb.bindings.USAddress;
import org.jboss.as.test.shared.integration.ejb.security.PermissionUtils;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Assert;

/**
 * <p>Base class for all the JAXB loading tests</p>
 *
 * @author rmartinc
 */
public abstract class JAXBContextTestBase {

    protected static final String WEB_APP_INTERNAL_CONTEXT = "jaxb-internal-webapp";
    protected static final String WEB_APP_CUSTOM_CONTEXT = "jaxb-custom-webapp";

    protected static final String JAXB_FACTORY_PROP_NAME = JAXBContextFactory.class.getName();
    protected static final String JAKARTA_FACTORY_PROP_NAME = JAXBContextFactory.class.getName().replaceFirst("javax.", "jakarta.");
    protected static final String DEFAULT_JAXB_FACTORY_CLASS;
    protected static final String DEFAULT_JAXB_CONTEXT_CLASS;
    protected static final String CUSTOM_JAXB_FACTORY_CLASS = FakeJAXBContextFactory.class.getName();
    protected static final String JAXB_PROPERTIES_FILE = "WEB-INF/classes/org/jboss/as/test/integration/jaxb/bindings/jaxb.properties";
    protected static final String SERVICES_FILE = "META-INF/services/" + JAXB_FACTORY_PROP_NAME;
    protected static final String PERMISSIONS_FILE = "META-INF/permissions.xml";

    static {
        boolean ee8 = System.getProperty("ts.ee9") == null;
        DEFAULT_JAXB_FACTORY_CLASS = ee8 ? "com.sun.xml.bind.v2.JAXBContextFactory" :"org.glassfish.jaxb.runtime.v2.JAXBContextFactory";
        DEFAULT_JAXB_CONTEXT_CLASS = ee8 ? "com.sun.xml.bind.v2.runtime.JAXBContextImpl" : "org.glassfish.jaxb.runtime.v2.runtime.JAXBContextImpl";
    }

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

    protected void testDefaultImplementation(URL url) throws IOException {
        // test the internal implementation is returned
        try (CloseableHttpClient httpClient = HttpClientBuilder.create().build()) {
            final String requestURL = url.toExternalForm() + JAXBContextServlet.URL_PATTERN;
            final HttpGet request = new HttpGet(requestURL);
            final HttpResponse response = httpClient.execute(request);
            int statusCode = response.getStatusLine().getStatusCode();
            Assert.assertEquals("Unexpected status code", 200, statusCode);
            final HttpEntity entity = response.getEntity();
            Assert.assertNotNull("Response message from servlet was null", entity);
            final String responseMessage = EntityUtils.toString(entity);
            MatcherAssert.assertThat(responseMessage, responseMessage,
                    CoreMatchers.containsString(DEFAULT_JAXB_CONTEXT_CLASS.replace('.', '/')));
        }
        // test it works
        try (CloseableHttpClient httpClient = HttpClientBuilder.create().build()) {
            final String requestURL = url.toExternalForm() + JAXBUsageServlet.URL_PATTERN;
            final HttpGet request = new HttpGet(requestURL);
            final HttpResponse response = httpClient.execute(request);
            int statusCode = response.getStatusLine().getStatusCode();
            Assert.assertEquals("Unexpected status code", 200, statusCode);
            final HttpEntity entity = response.getEntity();
            Assert.assertNotNull("Response message from servlet was null", entity);
            final String responseMessage = EntityUtils.toString(entity);
            Assert.assertEquals("Wrong return value", "Mill Valley", responseMessage.trim());
        }
    }

    protected void testCustomImplementation(URL url) throws IOException {
        // test the custom jaxb context is returned
        try (CloseableHttpClient httpClient = HttpClientBuilder.create().build()) {
            final String requestURL = url.toExternalForm() + JAXBContextServlet.URL_PATTERN;
            final HttpGet request = new HttpGet(requestURL);
            final HttpResponse response = httpClient.execute(request);
            int statusCode = response.getStatusLine().getStatusCode();
            Assert.assertEquals("Unexpected status code", 200, statusCode);
            final HttpEntity entity = response.getEntity();
            Assert.assertNotNull("Response message from servlet was null", entity);
            final String responseMessage = EntityUtils.toString(entity);
            Assert.assertEquals("Fake context is returned", "FakeJAXBContext", responseMessage);
        }
    }
}
