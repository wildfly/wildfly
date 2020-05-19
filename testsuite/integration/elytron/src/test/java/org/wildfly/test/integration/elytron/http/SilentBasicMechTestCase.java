/*
 * Copyright 2019 Red Hat, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.wildfly.test.integration.elytron.http;

import static org.apache.http.HttpStatus.SC_OK;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.net.URI;

import org.apache.http.HttpStatus;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.auth.BasicScheme;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.as.arquillian.api.ServerSetup;
import org.jboss.as.test.integration.security.common.Utils;
import org.jboss.as.test.integration.security.common.servlets.SimpleServlet;
import org.jboss.as.test.integration.web.sso.LogoutServlet;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.wildfly.test.security.common.elytron.MechanismConfiguration;

/**
 * Test of silent BASIC HTTP mechanism.
 *
 * Basic authentication in silent mode will send a challenge only if the request
 * contained authorization header, otherwise it is assumed another method will
 * send the challenge. This behaviour will allow to combine basic auth with form
 * auth, so human users will use form based auth and programmatic clients can
 * use basic authentication to log in.
 *
 */
@RunWith(Arquillian.class)
@RunAsClient
@ServerSetup({ SilentBasicMechTestCase.ServerSetup.class})
public class SilentBasicMechTestCase extends FormMechTestBase {

    private static final String FORBIDDEN_CONTENT = "Forbidden";
    private static final String NAME = SilentBasicMechTestCase.class.getSimpleName();
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
                .addAsWebInfResource(SilentBasicMechTestCase.class.getPackage(), NAME + "-web.xml", "web.xml");
    }

    static class ServerSetup extends AbstractMechTestBase.ServerSetup {
        @Override
        protected boolean useAuthenticationFactory() {
            return false;
        }

        @Override protected MechanismConfiguration getMechanismConfiguration() {
            return null;
        }

    }

    @Test
    public void testBasicWithCredentialSuccess() throws Exception {
        HttpGet request = new HttpGet(new URI(url.toExternalForm() + "role1"));
        UsernamePasswordCredentials credentials = new UsernamePasswordCredentials("user1", "password1");

        CredentialsProvider credsProvider = new BasicCredentialsProvider();
        credsProvider.setCredentials(AuthScope.ANY, credentials);

        try (CloseableHttpClient httpClient = HttpClients.custom().setDefaultCredentialsProvider(credsProvider).build()) {
            request.addHeader(new BasicScheme().authenticate(credentials, request, null));
            try (CloseableHttpResponse response = httpClient.execute(request)) {
                int statusCode = response.getStatusLine().getStatusCode();
                assertEquals("Unexpected status code in HTTP response.", SC_OK, statusCode);
                assertEquals("Unexpected content of HTTP response.", SimpleServlet.RESPONSE_BODY, EntityUtils.toString(response.getEntity()));
            }
        }
    }

    @Test
    public void testInsufficientRole() throws Exception {
        HttpGet request = new HttpGet(new URI(url.toExternalForm() + "role2"));
        UsernamePasswordCredentials credentials = new UsernamePasswordCredentials("user1", "password1");

        CredentialsProvider credsProvider = new BasicCredentialsProvider();
        credsProvider.setCredentials(AuthScope.ANY, credentials);

        try (CloseableHttpClient httpClient = HttpClients.custom()
                .setDefaultCredentialsProvider(credsProvider).build()) {
            request.addHeader(new BasicScheme().authenticate(credentials, request, null));
            try (CloseableHttpResponse response = httpClient.execute(request)) {
                int statusCode = response.getStatusLine().getStatusCode();
                assertEquals("Unexpected status code in HTTP response.", HttpStatus.SC_FORBIDDEN, statusCode);
                assertTrue("Unexpected content of HTTP response.", EntityUtils.toString(response.getEntity()).contains(FORBIDDEN_CONTENT));
            }
        }
    }

    @Override
    @Test
    public void testInvalidPrincipal() throws Exception {
        HttpGet request = new HttpGet(new URI(url.toExternalForm() + "role1"));
        UsernamePasswordCredentials credentials = new UsernamePasswordCredentials("user1wrong", "password1");

        CredentialsProvider credsProvider = new BasicCredentialsProvider();
        credsProvider.setCredentials(AuthScope.ANY, credentials);

        try (CloseableHttpClient httpClient = HttpClients.custom()
                .setDefaultCredentialsProvider(credsProvider).build()) {
            request.addHeader(new BasicScheme().authenticate(credentials, request, null));
            try (CloseableHttpResponse response = httpClient.execute(request)) {
                int statusCode = response.getStatusLine().getStatusCode();
                assertEquals("Unexpected status code in HTTP response.", HttpStatus.SC_UNAUTHORIZED, statusCode);
                assertEquals("Unexpected content of HTTP response.", LOGIN_PAGE_CONTENT, EntityUtils.toString(response.getEntity()));
            }
        }
    }

    @Override
    @Test
    public void testInvalidCredential() throws Exception {
        HttpGet request = new HttpGet(new URI(url.toExternalForm() + "role1"));
        UsernamePasswordCredentials credentials = new UsernamePasswordCredentials("user1", "password1wrong");

        CredentialsProvider credsProvider = new BasicCredentialsProvider();
        credsProvider.setCredentials(AuthScope.ANY, credentials);

        try (CloseableHttpClient httpClient = HttpClients.custom()
                .setDefaultCredentialsProvider(credsProvider).build()) {
            request.addHeader(new BasicScheme().authenticate(credentials, request, null));
            try (CloseableHttpResponse response = httpClient.execute(request)) {
                int statusCode = response.getStatusLine().getStatusCode();
                assertEquals("Unexpected status code in HTTP response.", HttpStatus.SC_UNAUTHORIZED, statusCode);
                assertEquals("Unexpected content of HTTP response.", LOGIN_PAGE_CONTENT, EntityUtils.toString(response.getEntity()));
            }
        }
    }

}
