/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2011, Red Hat, Inc., and individual contributors
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

package org.jboss.as.test.integration.jaxb.unit;

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
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.as.test.integration.jaxb.JAXBUsageServlet;
import org.jboss.as.test.integration.jaxb.bindings.Items;
import org.jboss.as.test.integration.jaxb.bindings.ObjectFactory;
import org.jboss.as.test.integration.jaxb.bindings.PurchaseOrderType;
import org.jboss.as.test.integration.jaxb.bindings.USAddress;
import org.jboss.as.test.shared.SecurityManagerFailure;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Tests that JAXB is usable from a WAR
 *
 * @author Jason T. Greene
 */
@RunWith(Arquillian.class)
@RunAsClient
public class JAXBUsageTestCase {

    private static final String WEB_APP_CONTEXT = "jaxb-webapp";

    @ArquillianResource
    private URL url;

    /**
     * Create a .ear, containing a web application (without any JSF constructs) and also the xerces jar in the .ear/lib
     *
     * @return
     */
    @Deployment(name = "app", testable = false)
    public static WebArchive createDeployment() {
        final WebArchive war = ShrinkWrap.create(WebArchive.class, WEB_APP_CONTEXT + ".war");
        war.addClasses(JAXBUsageServlet.class, Items.class, ObjectFactory.class, PurchaseOrderType.class, USAddress.class);
        return war;
    }

    @BeforeClass
    public static void beforeClass() {
        SecurityManagerFailure.thisTestIsFailingUnderSM("WFLY-6192");
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
        Assert.assertEquals("Unexpected status code", 200, statusCode);
        final HttpEntity entity = response.getEntity();
        Assert.assertNotNull("Response message from servlet was null", entity);
        final String responseMessage = EntityUtils.toString(entity);
        Assert.assertEquals("Wrong return value", "Mill Valley", responseMessage.trim());
    }
}
