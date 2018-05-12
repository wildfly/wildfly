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
 *
 */

package org.jboss.as.test.integration.xalan;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.util.EntityUtils;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.as.test.shared.maven.MavenUtil;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.EnterpriseArchive;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.net.URL;
import java.nio.file.Paths;
import java.util.List;

/**
 * A testcase to ensure that user deployments can package and use xalan library of their own
 */
@RunWith(Arquillian.class)
@RunAsClient
public class XalanUsageTestCase {

    private static final String WEB_APP_CONTEXT = "xalan-webapp";


    @ArquillianResource
    private URL appURL;

    @Deployment
    public static EnterpriseArchive createDeployment() throws Exception {
        final WebArchive war = ShrinkWrap.create(WebArchive.class, WEB_APP_CONTEXT + ".war");
        war.addClasses(XalanUsageServlet.class);
        // add a dummy xml to transform
        war.addAsResource(XalanUsageServlet.class.getPackage(), "dummy.xml", XalanUsageServlet.XML_RESOURCE_TO_TRANSFORM);
        war.addAsResource(XalanUsageServlet.class.getPackage(), "dummy.xsl", XalanUsageServlet.XSL_RESOURCE);

        final EnterpriseArchive ear = ShrinkWrap.create(EnterpriseArchive.class, "xalan-usage.ear");
        // add the .war
        ear.addAsModule(war);
        // add the xalan jar(s) in the .ear/lib
        final List<URL> xalanJars = MavenUtil.create(false).createMavenGavRecursiveURLs("xalan:xalan:2.7.2");
        if (xalanJars == null || xalanJars.isEmpty()) {
            throw new RuntimeException("Could not locate Xalan dependencies");
        }
        for (final URL xalanDep : xalanJars) {
            ear.addAsLibrary(xalanDep, Paths.get(xalanDep.toURI()).getFileName().toString());
        }
        return ear;
    }

    /**
     * Tests that a servlet within a user application can use the {@link javax.xml.transform.Transformer transform}
     * APIs when the application packages the xalan libraries of their own
     *
     * @throws Exception
     */
    @Test
    public void testXalanUsageInServlet() throws Exception {
        try (CloseableHttpClient httpClient = HttpClientBuilder.create().build()) {
            final String requestURL = appURL.toExternalForm() + XalanUsageServlet.URL_MAPPING_PATTERN;
            final HttpGet request = new HttpGet(requestURL);
            final HttpResponse response = httpClient.execute(request);
            int statusCode = response.getStatusLine().getStatusCode();
            Assert.assertEquals("Unexpected status code", 200, statusCode);
            final HttpEntity entity = response.getEntity();
            Assert.assertNotNull("Response message from servlet was null", entity);
            final String responseMessage = EntityUtils.toString(entity);
            Assert.assertEquals("Unexpected response message from servlet", XalanUsageServlet.SUCCESS_MESSAGE, responseMessage);
        }
    }
}
