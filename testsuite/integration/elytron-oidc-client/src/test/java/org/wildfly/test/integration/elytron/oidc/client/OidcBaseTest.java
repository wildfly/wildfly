/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2021, Red Hat, Inc., and individual contributors
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

package org.wildfly.test.integration.elytron.oidc.client;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assume.assumeTrue;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.LinkedList;
import java.util.List;

import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.CookieStore;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.utils.HttpClientUtils;
import org.apache.http.impl.client.BasicCookieStore;
import org.apache.http.impl.client.BasicResponseHandler;
import org.apache.http.impl.client.LaxRedirectStrategy;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.protocol.BasicHttpContext;
import org.apache.http.protocol.HttpContext;
import org.jboss.arquillian.container.test.api.OperateOnDeployment;
import org.jboss.as.arquillian.api.ServerSetupTask;
import org.jboss.as.arquillian.container.ManagementClient;
import org.jboss.as.test.http.util.TestHttpClientUtils;
import org.jboss.as.test.integration.security.common.servlets.SimpleSecuredServlet;
import org.jboss.as.test.integration.security.common.servlets.SimpleServlet;
import org.jboss.as.test.shared.TestSuiteEnvironment;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.junit.Test;
import org.keycloak.representations.idm.RealmRepresentation;
import org.testcontainers.DockerClientFactory;
import org.wildfly.security.jose.util.JsonSerialization;

import io.restassured.RestAssured;

/**
 * Tests for the OpenID Connect authentication mechanism.
 *
 * @author <a href="mailto:fjuma@redhat.com">Farah Juma</a>
 */
public abstract class OidcBaseTest {

    public static final String CLIENT_SECRET = "secret";
    public static final String OIDC_WITHOUT_SUBSYSTEM_CONFIG_WEB_XML = "web.xml";
    public static KeycloakContainer KEYCLOAK_CONTAINER;
    public static final String TEST_REALM = "WildFly";
    private static final String KEYCLOAK_USERNAME = "username";
    private static final String KEYCLOAK_PASSWORD = "password";
    public static final int CLIENT_PORT = TestSuiteEnvironment.getHttpPort();
    public static final String CLIENT_HOST_NAME = TestSuiteEnvironment.getHttpAddress();
    public static final String PROVIDER_URL_APP = "ProviderUrlOidcApp";
    public static final String AUTH_SERVER_URL_APP = "AuthServerUrlOidcApp";
    public static final String WRONG_PROVIDER_URL_APP = "WrongProviderUrlOidcApp";
    public static final String WRONG_SECRET_APP = "WrongSecretOidcApp";

    private static boolean isDockerAvailable() {
        try {
            DockerClientFactory.instance().client();
            return true;
        } catch (Throwable ex) {
            return false;
        }
    }

    public static void sendRealmCreationRequest(RealmRepresentation realm) {
        try {
            RestAssured
                    .given()
                    .auth().oauth2(KeycloakConfiguration.getAdminAccessToken(KEYCLOAK_CONTAINER.getAuthServerUrl()))
                    .contentType("application/json")
                    .body(JsonSerialization.writeValueAsBytes(realm))
                    .when()
                    .post(KEYCLOAK_CONTAINER.getAuthServerUrl() + "/admin/realms").then()
                    .statusCode(201);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    @OperateOnDeployment(PROVIDER_URL_APP)
    public void testWrongPasswordWithProviderUrl() throws Exception {
        loginToApp(PROVIDER_URL_APP, org.wildfly.test.integration.elytron.oidc.client.KeycloakConfiguration.ALICE, "WRONG_PASSWORD", HttpURLConnection.HTTP_OK, "Invalid username or password");
    }

    @Test
    @OperateOnDeployment(PROVIDER_URL_APP)
    public void testSucessfulAuthenticationWithProviderUrl() throws Exception {
        loginToApp(PROVIDER_URL_APP, org.wildfly.test.integration.elytron.oidc.client.KeycloakConfiguration.ALICE, org.wildfly.test.integration.elytron.oidc.client.KeycloakConfiguration.ALICE_PASSWORD, HttpURLConnection.HTTP_OK, SimpleServlet.RESPONSE_BODY);
    }

    @Test
    @OperateOnDeployment(PROVIDER_URL_APP)
    public void testWrongRoleWithProviderUrl() throws Exception {
        loginToApp(PROVIDER_URL_APP, org.wildfly.test.integration.elytron.oidc.client.KeycloakConfiguration.BOB, org.wildfly.test.integration.elytron.oidc.client.KeycloakConfiguration.BOB_PASSWORD, HttpURLConnection.HTTP_FORBIDDEN, null);
    }

    @Test
    @OperateOnDeployment(AUTH_SERVER_URL_APP)
    public void testWrongPasswordWithAuthServerUrl() throws Exception {
        loginToApp(AUTH_SERVER_URL_APP, org.wildfly.test.integration.elytron.oidc.client.KeycloakConfiguration.ALICE, "WRONG_PASSWORD", HttpURLConnection.HTTP_OK, "Invalid username or password");
    }

    @Test
    @OperateOnDeployment(AUTH_SERVER_URL_APP)
    public void testSucessfulAuthenticationWithAuthServerUrl() throws Exception {
        loginToApp(AUTH_SERVER_URL_APP, org.wildfly.test.integration.elytron.oidc.client.KeycloakConfiguration.ALICE, org.wildfly.test.integration.elytron.oidc.client.KeycloakConfiguration.ALICE_PASSWORD, HttpURLConnection.HTTP_OK, SimpleServlet.RESPONSE_BODY);
    }

    @Test
    @OperateOnDeployment(AUTH_SERVER_URL_APP)
    public void testWrongRoleWithAuthServerUrl() throws Exception {
        loginToApp(AUTH_SERVER_URL_APP, org.wildfly.test.integration.elytron.oidc.client.KeycloakConfiguration.BOB, org.wildfly.test.integration.elytron.oidc.client.KeycloakConfiguration.BOB_PASSWORD, HttpURLConnection.HTTP_FORBIDDEN, null);
    }

    @Test
    @OperateOnDeployment(WRONG_PROVIDER_URL_APP)
    public void testWrongProviderUrl() throws Exception {
        loginToApp(WRONG_PROVIDER_URL_APP, org.wildfly.test.integration.elytron.oidc.client.KeycloakConfiguration.ALICE, org.wildfly.test.integration.elytron.oidc.client.KeycloakConfiguration.ALICE_PASSWORD, -1, null, false);
    }

    @Test
    @OperateOnDeployment(WRONG_SECRET_APP)
    public void testWrongClientSecret() throws Exception {
        loginToApp(WRONG_SECRET_APP, org.wildfly.test.integration.elytron.oidc.client.KeycloakConfiguration.ALICE, org.wildfly.test.integration.elytron.oidc.client.KeycloakConfiguration.ALICE_PASSWORD, HttpURLConnection.HTTP_FORBIDDEN, null);
    }

    public static void loginToApp(String appName, String username, String password, int expectedStatusCode, String expectedText) throws Exception {
        loginToApp(appName, username, password, expectedStatusCode, expectedText, true);
    }

    public static void loginToApp(String appName, String username, String password, int expectedStatusCode, String expectedText, boolean loginToKeycloak) throws Exception {
        final URI requestUri = new URL("http", TestSuiteEnvironment.getHttpAddress(), TestSuiteEnvironment.getHttpPort(),
                "/" + appName + SimpleSecuredServlet.SERVLET_PATH).toURI();
        CookieStore store = new BasicCookieStore();
        HttpClient httpClient = TestHttpClientUtils.promiscuousCookieHttpClientBuilder()
                .setDefaultCookieStore(store)
                .setRedirectStrategy(new LaxRedirectStrategy())
                .build();
        HttpGet getMethod = new HttpGet(requestUri);
        HttpContext context = new BasicHttpContext();
        HttpResponse response = httpClient.execute(getMethod, context);
        try {
            int statusCode = response.getStatusLine().getStatusCode();
            if (loginToKeycloak) {
                assertTrue("Expected code == OK but got " + statusCode + " for request=" + requestUri, statusCode == HttpURLConnection.HTTP_OK);
                Form keycloakLoginForm = new Form(response);
                HttpResponse afterLoginClickResponse = simulateClickingOnButton(httpClient, keycloakLoginForm, username, password, "Sign In");
                afterLoginClickResponse.getEntity().getContent();
                assertEquals(expectedStatusCode, afterLoginClickResponse.getStatusLine().getStatusCode());
                if (expectedText != null) {
                    String responseString = new BasicResponseHandler().handleResponse(afterLoginClickResponse);
                    assertTrue(responseString.contains(expectedText));
                }
            } else {
                assertTrue("Expected code == FORBIDDEN but got " + statusCode + " for request=" + requestUri, statusCode == HttpURLConnection.HTTP_FORBIDDEN);
            }
        } finally {
            HttpClientUtils.closeQuietly(response);
        }
    }

    public static class KeycloakSetup implements ServerSetupTask {

        @Override
        public void setup(ManagementClient managementClient, String containerId) throws Exception {
            assumeTrue("Docker isn't available, OIDC tests will be skipped", isDockerAvailable());
            KEYCLOAK_CONTAINER = new KeycloakContainer();
            KEYCLOAK_CONTAINER.start();
        }

        public void tearDown(ManagementClient managementClient, String containerId) throws Exception {
            if (KEYCLOAK_CONTAINER != null) {
                KEYCLOAK_CONTAINER.stop();
            }
        }
    }

    private static HttpResponse simulateClickingOnButton(HttpClient client, Form form, String username, String password, String buttonValue) throws IOException {
        final URL url = new URL(form.getAction());
        final HttpPost request = new HttpPost(url.toString());
        final List<NameValuePair> params = new LinkedList<>();
        for (Input input : form.getInputFields()) {
            if (input.type == Input.Type.HIDDEN ||
                    (input.type == Input.Type.SUBMIT && input.getValue().equals(buttonValue))) {
                params.add(new BasicNameValuePair(input.getName(), input.getValue()));
            } else if (input.getName().equals(KEYCLOAK_USERNAME)) {
                params.add(new BasicNameValuePair(input.getName(), username));
            } else if (input.getName().equals(KEYCLOAK_PASSWORD)) {
                params.add(new BasicNameValuePair(input.getName(), password));
            }
        }
        request.setEntity(new UrlEncodedFormEntity(params, StandardCharsets.UTF_8));
        return client.execute(request);
    }

    public static final class Form {

        static final String
                NAME = "name",
                VALUE = "value",
                INPUT = "input",
                TYPE = "type",
                ACTION = "action",
                FORM = "form";

        final HttpResponse response;
        final String action;
        final List<Input> inputFields = new LinkedList<>();

        public Form(HttpResponse response) throws IOException {
            this.response = response;
            final String responseString = new BasicResponseHandler().handleResponse(response);
            final Document doc = Jsoup.parse(responseString);
            final Element form = doc.select(FORM).first();
            this.action = form.attr(ACTION);
            for (Element input : form.select(INPUT)) {
                Input.Type type = null;
                switch (input.attr(TYPE)) {
                    case "submit":
                        type = Input.Type.SUBMIT;
                        break;
                    case "hidden":
                        type = Input.Type.HIDDEN;
                        break;
                }
                inputFields.add(new Input(input.attr(NAME), input.attr(VALUE), type));
            }
        }

        public String getAction() {
            return action;
        }

        public List<Input> getInputFields() {
            return inputFields;
        }
    }

    private static final class Input {

        final String name, value;
        final Input.Type type;

        public Input(String name, String value, Input.Type type) {
            this.name = name;
            this.value = value;
            this.type = type;
        }

        public String getName() {
            return name;
        }

        public String getValue() {
            return value;
        }

        public enum Type {
            HIDDEN, SUBMIT
        }
    }
}
