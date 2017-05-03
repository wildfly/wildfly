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
 */
package org.jboss.as.test.integration.web.security.form;

import static org.junit.Assert.assertEquals;

import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.StatusLine;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.cookie.Cookie;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;
import org.apache.log4j.Logger;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.as.test.integration.security.WebSecurityPasswordBasedBase;
import org.jboss.as.test.integration.web.security.SecuredServlet;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;

/**
 * An Abstract parent of the FORM based authentication tests. <br/>
 * <i>This class was introduced as a workaround for JBPAPP-9018 - {@link Class#getMethods()} method returns different
 * values in
 * Oracle JDK and IBM JDK.</i>
 *
 * @author Josef Cacek
 */
public abstract class AbstractWebSecurityFORMTestCase extends WebSecurityPasswordBasedBase {
    private static Logger LOGGER = Logger.getLogger(AbstractWebSecurityFORMTestCase.class);

    @ArquillianResource
    private URL url;

    // Protected methods -----------------------------------------------------

    protected static WebArchive prepareDeployment(final String jbossWebFileName) throws Exception {

        WebArchive war = ShrinkWrap.create(WebArchive.class, "web-secure.war");
        war.addClasses(SecuredServlet.class);

        war.addAsWebResource(WebSecurityFORMTestCase.class.getPackage(), "login.jsp", "login.jsp");
        war.addAsWebResource(WebSecurityFORMTestCase.class.getPackage(), "error.jsp", "error.jsp");

        war.addAsWebInfResource(WebSecurityFORMTestCase.class.getPackage(), jbossWebFileName, "jboss-web.xml");
        war.addAsWebInfResource(WebSecurityFORMTestCase.class.getPackage(), "web.xml", "web.xml");

        return war;
    }

    /**
     * Makes a HTTP request to the protected web application.
     *
     * @param user
     * @param pass
     * @param expectedStatusCode
     * @throws Exception
     * @see WebSecurityPasswordBasedBase#makeCall(java.lang.String, java.lang.String,
     * int)
     */
    @Override
    protected void makeCall(String user, String pass, int expectedStatusCode) throws Exception {
        DefaultHttpClient httpclient = new DefaultHttpClient();
        try {
            String req = url.toExternalForm() + "secured/";
            HttpGet httpget = new HttpGet(req);

            HttpResponse response = httpclient.execute(httpget);

            HttpEntity entity = response.getEntity();
            if (entity != null) {
                EntityUtils.consume(entity);
            }

            // We should get the Login Page
            StatusLine statusLine = response.getStatusLine();
            LOGGER.trace("Login form get: " + statusLine);
            assertEquals(200, statusLine.getStatusCode());

            LOGGER.trace("Initial set of cookies:");
            List<Cookie> cookies = httpclient.getCookieStore().getCookies();
            if (cookies.isEmpty()) {
                LOGGER.trace("None");
            } else {
                for (int i = 0; i < cookies.size(); i++) {
                    LOGGER.trace("- " + cookies.get(i).toString());
                }
            }
            req = url.toExternalForm() + "secured/j_security_check";
            // We should now login with the user name and password
            HttpPost httpPost = new HttpPost(req);

            List<NameValuePair> nvps = new ArrayList<NameValuePair>();
            nvps.add(new BasicNameValuePair("j_username", user));
            nvps.add(new BasicNameValuePair("j_password", pass));

            httpPost.setEntity(new UrlEncodedFormEntity(nvps, StandardCharsets.UTF_8));

            response = httpclient.execute(httpPost);
            entity = response.getEntity();
            if (entity != null) {
                EntityUtils.consume(entity);
            }

            statusLine = response.getStatusLine();

            // Post authentication - we have a 302
            assertEquals(302, statusLine.getStatusCode());
            Header locationHeader = response.getFirstHeader("Location");
            String location = locationHeader.getValue();

            HttpGet httpGet = new HttpGet(location);
            response = httpclient.execute(httpGet);

            entity = response.getEntity();
            if (entity != null) {
                EntityUtils.consume(entity);
            }

            LOGGER.trace("Post logon cookies:");
            cookies = httpclient.getCookieStore().getCookies();
            if (cookies.isEmpty()) {
                LOGGER.trace("None");
            } else {
                for (int i = 0; i < cookies.size(); i++) {
                    LOGGER.trace("- " + cookies.get(i).toString());
                }
            }

            // Either the authentication passed or failed based on the expected status code
            statusLine = response.getStatusLine();
            assertEquals(expectedStatusCode, statusLine.getStatusCode());
        } finally {
            // When HttpClient instance is no longer needed,
            // shut down the connection manager to ensure
            // immediate deallocation of all system resources
            httpclient.getConnectionManager().shutdown();
        }
    }
}
