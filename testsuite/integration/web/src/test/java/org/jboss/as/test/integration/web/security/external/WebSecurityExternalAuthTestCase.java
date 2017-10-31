/*
 * JBoss, Home of Professional Open Source.
 * Copyright (c) 2011, Red Hat, Inc., and individual contributors
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
package org.jboss.as.test.integration.web.security.external;

import static org.junit.Assert.assertEquals;

import java.net.URL;

import io.undertow.servlet.ServletExtension;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.StatusLine;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.as.arquillian.api.ServerSetup;
import org.jboss.as.test.categories.CommonCriteria;
import org.jboss.as.test.integration.web.security.SecuredServlet;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;

/**
 * Unit Test to test external custom login module.
 *
 * @author Anil Saldhana
 */
@RunWith(Arquillian.class)
@RunAsClient
@ServerSetup(ExternalAuthSecurityDomainSetup.class)
@Category(CommonCriteria.class)
public class WebSecurityExternalAuthTestCase {

    @Deployment
    public static WebArchive deployment() throws Exception {
        WebArchive war = ShrinkWrap.create(WebArchive.class, "web-secure-external.war");
        war.addClass(SecuredServlet.class);
        war.addClasses(UserHandler.class, UserHandlerExtension.class, ExternalLoginModule.class);

        war.addAsWebInfResource(WebSecurityExternalAuthTestCase.class.getPackage(), "jboss-web.xml", "jboss-web.xml");
        war.addAsWebInfResource(WebSecurityExternalAuthTestCase.class.getPackage(), "web.xml", "web.xml");

        war.addAsServiceProvider(ServletExtension.class, UserHandlerExtension.class);


        return war;
    }

    @ArquillianResource
    private URL url;

    protected void makeCall(String user, int expectedStatusCode) throws Exception {
        try (CloseableHttpClient httpClient = HttpClients.createDefault()) {

            HttpGet httpget = new HttpGet(url.toExternalForm() + "secured/");
            httpget.addHeader("User", user);

            HttpResponse response = httpClient.execute(httpget);
            HttpEntity entity = response.getEntity();

            StatusLine statusLine = response.getStatusLine();

            assertEquals(expectedStatusCode, statusLine.getStatusCode());
            EntityUtils.consume(entity);
        }
    }

    /**
     * Test with user 'anil' who has right role to be authenticated by our external login module.
     *
     * @throws Exception
     */
    @Test
    public void testSucessfulAuth() throws Exception {
        makeCall("anil", 200);
    }

    /**
     * <p>
     * Test with user "marcus" who does not have the right role to be authenticated by our external login module.
     * </p>
     * <p>
     * Should be a HTTP/403
     * </p>
     *
     * @throws Exception
     */
    @Test
    public void testUnsuccessfulAuth() throws Exception {
        makeCall("marcus", 403);
    }
}