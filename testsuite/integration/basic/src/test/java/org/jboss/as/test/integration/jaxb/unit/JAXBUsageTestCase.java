/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.integration.jaxb.unit;

import java.io.FilePermission;
import java.lang.reflect.ReflectPermission;
import java.net.URL;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.util.EntityUtils;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.OperateOnDeployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit5.ArquillianExtension;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.as.test.integration.jaxb.JAXBUsageServlet;
import org.jboss.as.test.integration.jaxb.bindings.Items;
import org.jboss.as.test.integration.jaxb.bindings.ObjectFactory;
import org.jboss.as.test.integration.jaxb.bindings.PurchaseOrderType;
import org.jboss.as.test.integration.jaxb.bindings.USAddress;
import org.jboss.as.test.shared.PermissionUtils;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

/**
 * Tests that JAXB is usable from a WAR
 *
 * @author Jason T. Greene
 */
@ExtendWith(ArquillianExtension.class)
@RunAsClient
public class JAXBUsageTestCase {

    private static final String WEB_APP_CONTEXT = "jaxb-webapp";

    @ArquillianResource
    private URL url;

    /**
     * Create a .ear, containing a web application (without any Jakarta Server Faces constructs) and also the xerces jar in the .ear/lib
     *
     * @return
     */
    @Deployment(name = "app", testable = false)
    public static WebArchive createDeployment() {
        final WebArchive war = ShrinkWrap.create(WebArchive.class, WEB_APP_CONTEXT + ".war");
        war.addClasses(JAXBUsageServlet.class, Items.class, ObjectFactory.class, PurchaseOrderType.class, USAddress.class);
        war.add(PermissionUtils.createPermissionsXmlAsset(
                new RuntimePermission("accessDeclaredMembers"),
                new ReflectPermission("suppressAccessChecks"),
                new FilePermission(System.getProperty("jboss.inst") + "/standalone/tmp/-", "read")), "META-INF/permissions.xml");
        return war;
    }

    @OperateOnDeployment("app")
    @Test
    public void testJAXBServlet() throws Exception {
        final HttpClient httpClient = new DefaultHttpClient();
        final String xml = "dummy.xml";

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
