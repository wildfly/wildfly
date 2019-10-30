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

package org.jboss.as.test.integration.xerces.unit;

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
import org.jboss.as.test.integration.xerces.JSFManagedBean;
import org.jboss.as.test.integration.xerces.XercesUsageServlet;
import org.jboss.as.test.shared.integration.ejb.security.PermissionUtils;
import org.jboss.logging.Logger;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.EnterpriseArchive;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Tests that packaging the xerces jar within a deployment does not cause deployment or runtime failures in the application
 *
 * @author Jaikiran Pai
 */
@RunWith(Arquillian.class)
@RunAsClient
public class XercesUsageTestCase {

    private static final String WEB_APP_CONTEXT = "xerces-webapp";

    private static final String JSF_WEB_APP_CONTEXT = "xerces-jsf-webapp";

    private static final Logger logger = Logger.getLogger(XercesUsageTestCase.class);

    @ArquillianResource
    @OperateOnDeployment("app-without-jsf")
    private URL withoutJsf;

    @ArquillianResource
    @OperateOnDeployment("app-with-jsf")
    private URL withJsf;

    /**
     * Create a .ear, containing a web application (without any JSF constructs) and also the xerces jar in the .ear/lib
     *
     * @return
     */
    @Deployment(name = "app-without-jsf", testable = false)
    public static EnterpriseArchive createDeployment() {
        final WebArchive war = ShrinkWrap.create(WebArchive.class, WEB_APP_CONTEXT + ".war");
        war.addClasses(XercesUsageServlet.class);
        // add a dummy xml to parse
        war.addAsResource(XercesUsageServlet.class.getPackage(), "dummy.xml", "dummy.xml");

        final EnterpriseArchive ear = ShrinkWrap.create(EnterpriseArchive.class, "xerces-usage.ear");
        // add the .war
        ear.addAsModule(war);
        // add the xerces jar in the .ear/lib
        ear.addAsLibrary("xerces/xercesImpl.jar", "xercesImpl.jar");
        ear.addAsManifestResource(PermissionUtils.createPermissionsXmlAsset(
            new RuntimePermission("accessClassInPackage.org.apache.xerces.util"),
            new RuntimePermission("accessClassInPackage.org.apache.xerces.xni.grammars"),
            new RuntimePermission("accessClassInPackage.org.apache.xerces.impl.validation"),
            new RuntimePermission("accessClassInPackage.org.apache.xerces.impl.dtd"),
            new RuntimePermission("accessClassInPackage.org.apache.xerces.impl.*")
        ), "permissions.xml");

        return ear;
    }

    /**
     * Create a .ear, containing a JSF web application and also the xerces jar in the .ear/lib
     *
     * @return
     */
    @Deployment(name = "app-with-jsf", testable = false)
    public static EnterpriseArchive createJSFDeployment() {
        final WebArchive war = ShrinkWrap.create(WebArchive.class, JSF_WEB_APP_CONTEXT + ".war");
        war.addClasses(XercesUsageServlet.class, JSFManagedBean.class);
        // add a dummy xml to parse
        war.addAsResource(XercesUsageServlet.class.getPackage(), "dummy.xml", "dummy.xml");

        final EnterpriseArchive ear = ShrinkWrap.create(EnterpriseArchive.class, "xerces-usage-jsf.ear");
        // add the .war
        ear.addAsModule(war);
        // add the xerces jar in the .ear/lib
        ear.addAsLibrary("xerces/xercesImpl.jar", "xercesImpl.jar");
        ear.addAsManifestResource(PermissionUtils.createPermissionsXmlAsset(
            new RuntimePermission("accessClassInPackage.org.apache.xerces.util"),
            new RuntimePermission("accessClassInPackage.org.apache.xerces.impl.*"),
            new RuntimePermission("accessClassInPackage.org.apache.xerces.xni.grammars")
        ), "permissions.xml");

        return ear;
    }


    /**
     * Test that a non-JSF web application, with xerces jar packaged within the deployment, functions correctly
     * while using the packaged xerces API.
     *
     * @throws Exception
     */
    @OperateOnDeployment("app-without-jsf")
    @Test
    public void testXercesUsageInServletInNonJSFApp() throws Exception {
        final HttpClient httpClient = new DefaultHttpClient();
        final String xml = "dummy.xml";

        final String requestURL = withoutJsf.toExternalForm() + XercesUsageServlet.URL_PATTERN
                + "?" + XercesUsageServlet.XML_RESOURCE_NAME_PARAMETER + "=" + xml;
        final HttpGet request = new HttpGet(requestURL);
        final HttpResponse response = httpClient.execute(request);
        int statusCode = response.getStatusLine().getStatusCode();
        Assert.assertEquals("Unexpected status code", 200, statusCode);
        final HttpEntity entity = response.getEntity();
        Assert.assertNotNull("Response message from servlet was null", entity);
        final String responseMessage = EntityUtils.toString(entity);
        Assert.assertEquals("Unexpected echo message from servlet", XercesUsageServlet.SUCCESS_MESSAGE, responseMessage);
    }

    /**
     * Test that a JSF web application, with xerces jar packaged within the deployment, functions correctly while using
     * the packaged xerces API.
     *
     * @throws Exception
     */
    @OperateOnDeployment("app-with-jsf")
    @Test
    public void testXercesUsageInServletInJSFApp() throws Exception {
        final HttpClient httpClient = new DefaultHttpClient();
        final String xml = "dummy.xml";

        final String requestURL = withJsf.toExternalForm()+ XercesUsageServlet.URL_PATTERN
                + "?" + XercesUsageServlet.XML_RESOURCE_NAME_PARAMETER + "=" + xml;
        final HttpGet request = new HttpGet(requestURL);
        final HttpResponse response = httpClient.execute(request);
        int statusCode = response.getStatusLine().getStatusCode();
        Assert.assertEquals("Unexpected status code", 200, statusCode);
        final HttpEntity entity = response.getEntity();
        Assert.assertNotNull("Response message from servlet was null", entity);
        final String responseMessage = EntityUtils.toString(entity);
        Assert.assertEquals("Unexpected echo message from servlet", XercesUsageServlet.SUCCESS_MESSAGE, responseMessage);
    }


}
