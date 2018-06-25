/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2018, Red Hat, Inc., and individual contributors
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

package org.wildfly.test.integration.elytron.http;

import static org.apache.http.HttpStatus.SC_FORBIDDEN;
import static org.apache.http.HttpStatus.SC_MOVED_TEMPORARILY;
import static org.apache.http.HttpStatus.SC_OK;
import static org.jboss.as.test.integration.security.common.servlets.SimpleServlet.RESPONSE_BODY;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;

import org.apache.http.Header;
import org.apache.http.NameValuePair;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.as.test.integration.security.common.Utils;
import org.jboss.as.test.integration.security.common.servlets.SimpleServlet;
import org.jboss.as.test.integration.web.sso.LogoutServlet;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Test;

/**
 * Test of FORM HTTP mechanism.
 *
 * @author Jan Kalina
 */
abstract class FormMechTestCase extends AbstractMechTestBase {

    private static final String NAME = FormMechTestCase.class.getSimpleName();
    private static final String LOGIN_PAGE_CONTENT = "LOGINPAGE";
    private static final String ERROR_PAGE_CONTENT = "ERRORPAGE";

    @Deployment(testable = false)
    public static WebArchive createDeployment() {
        return ShrinkWrap.create(WebArchive.class, NAME + ".war")
                .addClasses(SimpleServlet.class)
                .addClasses(LogoutServlet.class)
                .addAsWebInfResource(Utils.getJBossWebXmlAsset(APP_DOMAIN), "jboss-web.xml")
                .addAsWebResource(new StringAsset(LOGIN_PAGE_CONTENT), "login.html")
                .addAsWebResource(new StringAsset(ERROR_PAGE_CONTENT), "error.html")
                .addAsWebInfResource(FormMechTestCase.class.getPackage(), NAME + "-web.xml", "web.xml");
    }

    @Test
    @Override
    public void testUnauthorized() throws Exception {
        HttpGet request = new HttpGet(new URI(url.toExternalForm() + "role1"));
        HttpClientContext context = HttpClientContext.create();

        try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
            try (CloseableHttpResponse response = httpClient.execute(request, context)) {
                int statusCode = response.getStatusLine().getStatusCode();
                assertEquals("Unexpected status code in HTTP response.", SC_OK, statusCode);
                assertEquals("Unexpected content of HTTP response.", LOGIN_PAGE_CONTENT, EntityUtils.toString(response.getEntity()));
            }
        }
    }

    @Test
    public void testLoginPage() throws Exception {
        HttpGet request = new HttpGet(new URI(url.toExternalForm() + "login.html"));
        HttpClientContext context = HttpClientContext.create();

        try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
            try (CloseableHttpResponse response = httpClient.execute(request, context)) {
                int statusCode = response.getStatusLine().getStatusCode();
                assertEquals("Unexpected status code in HTTP response.", SC_OK, statusCode);
                assertEquals("Unexpected content of HTTP response.", LOGIN_PAGE_CONTENT, EntityUtils.toString(response.getEntity()));
            }
        }
    }

    @Test
    public void testCorrectWorkflow() throws Exception {
        try (CloseableHttpClient httpClient = HttpClientBuilder.create().disableRedirectHandling().build()) {
            // unauthorized - login form should be shown
            HttpGet request1 = new HttpGet(new URI(url.toExternalForm() + "role1"));
            try (CloseableHttpResponse response = httpClient.execute(request1)) {
                int statusCode = response.getStatusLine().getStatusCode();
                assertEquals("Unexpected status code in HTTP response.", SC_OK, statusCode);
                assertEquals("Unexpected content of HTTP response.", LOGIN_PAGE_CONTENT, EntityUtils.toString(response.getEntity()));
            }

            // logging-in
            HttpPost request2 = new HttpPost(new URI(url.toExternalForm() + "j_security_check"));
            List<NameValuePair> params = new ArrayList<NameValuePair>();
            params.add(new BasicNameValuePair("j_username", "user1"));
            params.add(new BasicNameValuePair("j_password", "password1"));
            request2.setEntity(new UrlEncodedFormEntity(params));
            try (CloseableHttpResponse response = httpClient.execute(request2)) {
                int statusCode = response.getStatusLine().getStatusCode();
                Header[] locations = response.getHeaders("Location");
                assertEquals("Unexpected status code in HTTP response.", SC_MOVED_TEMPORARILY, statusCode);
                assertEquals("Missing redirect in HTTP response.", 1, locations.length);
                assertEquals("Unexpected redirect in HTTP response.", url.toExternalForm() + "role1", locations[0].getValue());
            }

            // should be logged now
            HttpGet request3 = new HttpGet(new URI(url.toExternalForm() + "role1"));
            try (CloseableHttpResponse response = httpClient.execute(request3)) {
                int statusCode = response.getStatusLine().getStatusCode();
                assertEquals("Unexpected status code in HTTP response.", SC_OK, statusCode);
                assertEquals("Unexpected content of HTTP response.", RESPONSE_BODY, EntityUtils.toString(response.getEntity()));
            }

            // but no role2
            HttpGet request4 = new HttpGet(new URI(url.toExternalForm() + "role2"));
            try (CloseableHttpResponse response = httpClient.execute(request4)) {
                int statusCode = response.getStatusLine().getStatusCode();
                assertEquals("Unexpected status code in HTTP response.", SC_FORBIDDEN, statusCode);
                assertNotEquals("Unexpected content of HTTP response.", RESPONSE_BODY, EntityUtils.toString(response.getEntity()));
            }

            // try to log-out
            HttpGet request5 = new HttpGet(new URI(url.toExternalForm() + "logout"));
            try (CloseableHttpResponse response = httpClient.execute(request5)) {
                int statusCode = response.getStatusLine().getStatusCode();
                Header[] locations = response.getHeaders("Location");
                assertEquals("Unexpected status code in HTTP response.", SC_MOVED_TEMPORARILY, statusCode);
                assertEquals("Missing redirect in HTTP response.", 1, locations.length);
                assertEquals("Unexpected redirect in HTTP response.", url.toExternalForm() + "index.html", locations[0].getValue());
            }

            // should be logged-out again
            HttpGet request6 = new HttpGet(new URI(url.toExternalForm() + "role1"));
            try (CloseableHttpResponse response = httpClient.execute(request6)) {
                int statusCode = response.getStatusLine().getStatusCode();
                assertEquals("Unexpected status code in HTTP response.", SC_OK, statusCode);
                assertEquals("Unexpected content of HTTP response.", LOGIN_PAGE_CONTENT, EntityUtils.toString(response.getEntity()));
            }
        }
    }

    @Test
    public void testInvalidPrincipal() throws Exception {
        try (CloseableHttpClient httpClient = HttpClientBuilder.create().disableRedirectHandling().build()) {
            HttpPost request = new HttpPost(new URI(url.toExternalForm() + "j_security_check"));
            List<NameValuePair> params = new ArrayList<NameValuePair>();
            params.add(new BasicNameValuePair("j_username", "user1wrong"));
            params.add(new BasicNameValuePair("j_password", "password1"));
            request.setEntity(new UrlEncodedFormEntity(params));
            try (CloseableHttpResponse response = httpClient.execute(request)) {
                int statusCode = response.getStatusLine().getStatusCode();
                assertEquals("Unexpected status code in HTTP response.", SC_OK, statusCode);
                assertEquals("Unexpected content of HTTP response.", ERROR_PAGE_CONTENT, EntityUtils.toString(response.getEntity()));
            }
        }
    }

    @Test
    public void testInvalidCredential() throws Exception {
        try (CloseableHttpClient httpClient = HttpClientBuilder.create().disableRedirectHandling().build()) {
            HttpPost request = new HttpPost(new URI(url.toExternalForm() + "j_security_check"));
            List<NameValuePair> params = new ArrayList<NameValuePair>();
            params.add(new BasicNameValuePair("j_username", "user1"));
            params.add(new BasicNameValuePair("j_password", "password1wrong"));
            request.setEntity(new UrlEncodedFormEntity(params));
            try (CloseableHttpResponse response = httpClient.execute(request)) {
                int statusCode = response.getStatusLine().getStatusCode();
                assertEquals("Unexpected status code in HTTP response.", SC_OK, statusCode);
                assertEquals("Unexpected content of HTTP response.", ERROR_PAGE_CONTENT, EntityUtils.toString(response.getEntity()));
            }
        }
    }

}
