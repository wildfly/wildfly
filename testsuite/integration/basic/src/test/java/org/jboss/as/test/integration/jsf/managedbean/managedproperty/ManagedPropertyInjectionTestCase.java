/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.integration.jsf.managedbean.managedproperty;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.net.URL;
import java.nio.charset.StandardCharsets;

import org.apache.commons.io.IOUtils;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.utils.HttpClientUtils;
import org.apache.http.impl.client.DefaultHttpClient;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.OperateOnDeployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Test that Jakarta Server Faces managed properties are injected before @PostConstruct methods
 * are called and that beans for JSF implicit objects are injectable.
 *
 * @author Farah Juma
 * @author Brian Stansberry
 */
@RunWith(Arquillian.class)
@RunAsClient
public class ManagedPropertyInjectionTestCase {

    @ArquillianResource
    private URL url;

    /** Creates a war with beans annotated with @ManagedProperty but nothing to activate FacesServlet */
    @Deployment(name = "jsfmanagedproperty-cruft.war")
    public static Archive<?> cruftDeployment() {
        return baseDeployment("jsfmanagedproperty-cruft.war");
    }

    /** Creates a war equivalent to {@link #cruftDeployment()} but with a
     * faces-config.xml included in order to active the FacesServlet */
    @Deployment(name = "jsfmanagedproperty.war")
    public static Archive<?> accessibleDeployment() {
        final WebArchive war = baseDeployment("jsfmanagedproperty.war");
        war.addAsWebInfResource(InjectionTargetBean.class.getPackage(), "faces-config.xml", "faces-config.xml");
        return war;
    }

    private static WebArchive baseDeployment(String warName) {
        final WebArchive war = ShrinkWrap.create(WebArchive.class, warName);
        war.addClasses(InjectionTargetBean.class, GreetingBean.class);
        war.addAsWebResource(InjectionTargetBean.class.getPackage(), "managedproperties.xhtml", "managedproperties.xhtml");
        war.addAsWebResource(InjectionTargetBean.class.getPackage(), "index.html", "index.html");
        return war;
    }

    @Test
    @OperateOnDeployment("jsfmanagedproperty-cruft.war")
    public void testWithoutFacesServlet() throws IOException {
        DefaultHttpClient client = new DefaultHttpClient();
        try {
            // Confirm the war is accessible
            String relativePath = "index.html";
            executeRequest(client, relativePath, 200);
            // Confirm the FacesServlet is not accessible (sanity check that the deployment works as expected)
            relativePath = "managedproperties.jsf?testName=" + getClass().getSimpleName();
            executeRequest(client, relativePath, 404);
        } finally {
            HttpClientUtils.closeQuietly(client);
        }
    }

    @Test
    @OperateOnDeployment("jsfmanagedproperty.war")
    public void testWithFacesServlet() throws IOException {
        DefaultHttpClient client = new DefaultHttpClient();
        try {
            // Access the URL and validate injections
            String relativePath = "managedproperties.jsf?testName=" + getClass().getSimpleName();
            String responseString = executeRequest(client, relativePath, 200);
            checkKeyValue(responseString, "TestName", getClass().getSimpleName());
            checkKeyValue(responseString, "ContextPath", "/jsfmanagedproperty");
            checkKeyValue(responseString, "PostConstructCalled", "true");
            checkKeyValue(responseString, "GreetingBeanInjected", "true");
        } finally {
            HttpClientUtils.closeQuietly(client);
        }
    }

    private String executeRequest(HttpClient client, String relativePath, int expectedStatus) throws IOException {
        String requestUrl = url.toString() + relativePath;
        HttpGet getRequest = new HttpGet(requestUrl);
        HttpResponse response = client.execute(getRequest);
        try {
            String responseString = IOUtils.toString(response.getEntity().getContent(), StandardCharsets.UTF_8);
            assertEquals(requestUrl + "\n" + responseString, expectedStatus, response.getStatusLine().getStatusCode());
            return responseString;
        } finally {
            HttpClientUtils.closeQuietly(response);
        }
    }

    private static void checkKeyValue(String responseString, String key, String value) {
        assertTrue(key + " must be " + value + " in " + responseString, responseString.contains(key+":"+value));
    }
}
