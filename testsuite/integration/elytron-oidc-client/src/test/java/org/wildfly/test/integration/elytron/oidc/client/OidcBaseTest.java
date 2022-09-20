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
import static org.wildfly.test.integration.elytron.oidc.client.KeycloakConfiguration.ALLOWED_ORIGIN;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.LinkedList;
import java.util.List;

import org.apache.http.Header;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.CookieStore;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpOptions;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.utils.HttpClientUtils;
import org.apache.http.impl.client.BasicCookieStore;
import org.apache.http.impl.client.BasicResponseHandler;
import org.apache.http.impl.client.HttpClients;
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
import org.junit.BeforeClass;
import org.junit.Test;
import org.keycloak.representations.idm.RealmRepresentation;
import org.testcontainers.DockerClientFactory;
import org.wildfly.common.iteration.CodePointIterator;
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
    public static final String DIRECT_ACCCESS_GRANT_ENABLED_CLIENT = "DirectAccessGrantEnabledClient";
    public static final String BEARER_ONLY_AUTH_SERVER_URL_APP = "AuthServerUrlBearerOnlyApp";
    public static final String BEARER_ONLY_PROVIDER_URL_APP = "ProviderUrlBearerOnlyApp";
    public static final String BASIC_AUTH_PROVIDER_URL_APP = "BasicAuthProviderUrlApp";
    public static final String CORS_PROVIDER_URL_APP = "CorsApp";
    private static final String WRONG_PASSWORD = "WRONG_PASSWORD";
    private static final String ACCESS_CONTROL_ALLOW_ORIGIN = "Access-Control-Allow-Origin";
    private static final String ACCESS_CONTROL_ALLOW_CREDENTIALS = "Access-Control-Allow-Credentials";
    private static final String ACCESS_CONTROL_ALLOW_METHODS = "Access-Control-Allow-Methods";
    private static final String ACCESS_CONTROL_ALLOW_HEADERS = "Access-Control-Allow-Headers";
    static final String ORIGIN = "Origin";
    static final String ACCESS_CONTROL_REQUEST_METHOD = "Access-Control-Request-Method";
    static final String ACCESS_CONTROL_REQUEST_HEADERS = "Access-Control-Request-Headers";
    public static final String CORS_CLIENT = "CorsClient";

    private enum BearerAuthType {
        BEARER,
        QUERY_PARAM,
        BASIC
    }

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

    @BeforeClass
    public static void checkDockerAvailability() {
        assumeTrue("Docker isn't available, OIDC tests will be skipped", isDockerAvailable());
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

    /**
     * Tests that use a bearer token.
     */

    @Test
    @OperateOnDeployment(BEARER_ONLY_AUTH_SERVER_URL_APP)
    public void testSucessfulBearerOnlyAuthenticationWithAuthServerUrl() throws Exception {
        performBearerAuthentication(BEARER_ONLY_AUTH_SERVER_URL_APP, KeycloakConfiguration.ALICE, KeycloakConfiguration.ALICE_PASSWORD,
                SimpleServlet.RESPONSE_BODY, null, BearerAuthType.BEARER, DIRECT_ACCCESS_GRANT_ENABLED_CLIENT, CLIENT_SECRET);
    }

    @Test
    @OperateOnDeployment(BEARER_ONLY_PROVIDER_URL_APP)
    public void testSucessfulBearerOnlyAuthenticationWithProviderUrl() throws Exception {
        performBearerAuthentication(BEARER_ONLY_PROVIDER_URL_APP, KeycloakConfiguration.ALICE, KeycloakConfiguration.ALICE_PASSWORD,
                SimpleServlet.RESPONSE_BODY, null, BearerAuthType.BEARER, DIRECT_ACCCESS_GRANT_ENABLED_CLIENT, CLIENT_SECRET);
    }

    @Test
    @OperateOnDeployment(BEARER_ONLY_PROVIDER_URL_APP)
    public void testWrongToken() throws Exception {
        String wrongToken = "eyJhbGciOiJSUzI1NiIsInR5cCIgOiAiSldUIiwia2lkIiA6ICJrNmhQYTdHdmdrajdFdlhLeFAtRjFLZkNSUk85Q3kwNC04YzFqTERWOXNrIn0.eyJleHAiOjE2NTc2NjExODksImlhdCI6MTY1NzY2MTEyOSwianRpIjoiZThiZGQ3MWItYTA2OC00Mjc3LTkyY2UtZWJkYmU2MDVkMzBhIiwiaXNzIjoiaHR0cDovL2xvY2FsaG9zdDo4MDgwL3JlYWxtcy9tYXN0ZXIiLCJhdWQiOlsibXlyZWFsbS1yZWFsbSIsIm1hc3Rlci1yZWFsbSIsImFjY291bnQiXSwic3ViIjoiZTliOGE2OWItM2RlNy00ZDYzLWFjYmItMmYyNTRhMDM1MjVkIiwidHlwIjoiQmVhcmVyIiwiYXpwIjoidGVzdC13ZWJhcHAiLCJzZXNzaW9uX3N0YXRlIjoiMTQ1OTdhMmUtOGM1Ni00YzkwLWI3NjAtZWFjYzczNWU1Zjc1IiwiYWNyIjoiMSIsInJlYWxtX2FjY2VzcyI6eyJyb2xlcyI6WyJjcmVhdGUtcmVhbG0iLCJkZWZhdWx0LXJvbGVzLW1hc3RlciIsIm9mZmxpbmVfYWNjZXNzIiwiYWRtaW4iLCJ1bWFfYXV0aG9yaXphdGlvbiIsInVzZXIiXX0sInJlc291cmNlX2FjY2VzcyI6eyJteXJlYWxtLXJlYWxtIjp7InJvbGVzIjpbInZpZXctcmVhbG0iLCJ2aWV3LWlkZW50aXR5LXByb3ZpZGVycyIsIm1hbmFnZS1pZGVudGl0eS1wcm92aWRlcnMiLCJpbXBlcnNvbmF0aW9uIiwiY3JlYXRlLWNsaWVudCIsIm1hbmFnZS11c2VycyIsInF1ZXJ5LXJlYWxtcyIsInZpZXctYXV0aG9yaXphdGlvbiIsInF1ZXJ5LWNsaWVudHMiLCJxdWVyeS11c2VycyIsIm1hbmFnZS1ldmVudHMiLCJtYW5hZ2UtcmVhbG0iLCJ2aWV3LWV2ZW50cyIsInZpZXctdXNlcnMiLCJ2aWV3LWNsaWVudHMiLCJtYW5hZ2UtYXV0aG9yaXphdGlvbiIsIm1hbmFnZS1jbGllbnRzIiwicXVlcnktZ3JvdXBzIl19LCJtYXN0ZXItcmVhbG0iOnsicm9sZXMiOlsidmlldy1yZWFsbSIsInZpZXctaWRlbnRpdHktcHJvdmlkZXJzIiwibWFuYWdlLWlkZW50aXR5LXByb3ZpZGVycyIsImltcGVyc29uYXRpb24iLCJjcmVhdGUtY2xpZW50IiwibWFuYWdlLXVzZXJzIiwicXVlcnktcmVhbG1zIiwidmlldy1hdXRob3JpemF0aW9uIiwicXVlcnktY2xpZW50cyIsInF1ZXJ5LXVzZXJzIiwibWFuYWdlLWV2ZW50cyIsIm1hbmFnZS1yZWFsbSIsInZpZXctZXZlbnRzIiwidmlldy11c2VycyIsInZpZXctY2xpZW50cyIsIm1hbmFnZS1hdXRob3JpemF0aW9uIiwibWFuYWdlLWNsaWVudHMiLCJxdWVyeS1ncm91cHMiXX0sImFjY291bnQiOnsicm9sZXMiOlsibWFuYWdlLWFjY291bnQiLCJtYW5hZ2UtYWNjb3VudC1saW5rcyIsInZpZXctcHJvZmlsZSJdfX0sInNjb3BlIjoiZW1haWwgcHJvZmlsZSIsInNpZCI6IjE0NTk3YTJlLThjNTYtNGM5MC1iNzYwLWVhY2M3MzVlNWY3NSIsImVtYWlsX3ZlcmlmaWVkIjpmYWxzZSwicHJlZmVycmVkX3VzZXJuYW1lIjoiYWxpY2UifQ.hVj6SG-aTcDYhifdljpiBcz4ShCHej3h_4-82rgX0s_oJ-En68Cqt-_DgJLtMdr6dW_gQFFCPYBJfEGvZ8L6b_TwzbdLxyrQrKTOpeG0KJ8VAFlbWum9B1vvES_sav1Gj1sQHlV621EaLISYz7pnknuQEvrB7liJFRRjN9SH30AsAJy6nmKTDHGZ6Eegkveqd_7POaKfsHS3Z0-SGyL5GClXv9yZ1l5Y4VH-rrMUztLPCFH5bJ319-m-7sgizvV-C2EcM37XVAtPRVQbJNRW0wVmLEJKMuLYVnjS1Wn5eU_qnBvVMEaENNG3TzNd6b4YmxMFHFf9tnkb3wkDzdrRTA";
        performBearerAuthentication(BEARER_ONLY_PROVIDER_URL_APP, KeycloakConfiguration.ALICE, KeycloakConfiguration.ALICE_PASSWORD,
                SimpleServlet.RESPONSE_BODY, wrongToken, BearerAuthType.BEARER);
    }

    @Test
    @OperateOnDeployment(BEARER_ONLY_PROVIDER_URL_APP)
    public void testInvalidToken() throws Exception {
        performBearerAuthentication(BEARER_ONLY_PROVIDER_URL_APP, KeycloakConfiguration.ALICE, KeycloakConfiguration.ALICE_PASSWORD,
                SimpleServlet.RESPONSE_BODY, "INVALID_TOKEN", BearerAuthType.BEARER);
    }

    @Test
    @OperateOnDeployment(BEARER_ONLY_AUTH_SERVER_URL_APP)
    public void testNoTokenProvidedWithAuthServerUrl() throws Exception {
        accessAppWithoutToken(BEARER_ONLY_AUTH_SERVER_URL_APP, false, true, TEST_REALM);
    }

    @Test
    @OperateOnDeployment(BEARER_ONLY_PROVIDER_URL_APP)
    public void testNoTokenProvidedWithProviderUrl() throws Exception {
        accessAppWithoutToken(BEARER_ONLY_PROVIDER_URL_APP, false, true);
    }

    @Test
    @OperateOnDeployment(PROVIDER_URL_APP)
    public void testTokenProvidedBearerOnlyNotSet() throws Exception {
        performBearerAuthentication(PROVIDER_URL_APP, KeycloakConfiguration.ALICE, KeycloakConfiguration.ALICE_PASSWORD,
                SimpleServlet.RESPONSE_BODY, null, BearerAuthType.BEARER, DIRECT_ACCCESS_GRANT_ENABLED_CLIENT, CLIENT_SECRET);
    }

    @Test
    @OperateOnDeployment(PROVIDER_URL_APP)
    public void testTokenNotProvidedBearerOnlyNotSet() throws Exception {
        // ensure the regular OIDC flow takes place
        accessAppWithoutToken(PROVIDER_URL_APP, false, false,null, HttpURLConnection.HTTP_OK, SimpleServlet.RESPONSE_BODY);
    }

    /**
     * Tests that pass the bearer token to use via an access_token query param.
     */

    @Test
    @OperateOnDeployment(BEARER_ONLY_PROVIDER_URL_APP)
    public void testValidTokenViaQueryParameter() throws Exception {
        performBearerAuthentication(BEARER_ONLY_PROVIDER_URL_APP, KeycloakConfiguration.ALICE, KeycloakConfiguration.ALICE_PASSWORD,
                SimpleServlet.RESPONSE_BODY, null, BearerAuthType.QUERY_PARAM, DIRECT_ACCCESS_GRANT_ENABLED_CLIENT, CLIENT_SECRET);
    }

    @Test
    @OperateOnDeployment(BEARER_ONLY_PROVIDER_URL_APP)
    public void testWrongTokenViaQueryParameter() throws Exception {
        String wrongToken = "eyJhbGciOiJSUzI1NiIsInR5cCIgOiAiSldUIiwia2lkIiA6ICJrNmhQYTdHdmdrajdFdlhLeFAtRjFLZkNSUk85Q3kwNC04YzFqTERWOXNrIn0.eyJleHAiOjE2NTc2NjExODksImlhdCI6MTY1NzY2MTEyOSwianRpIjoiZThiZGQ3MWItYTA2OC00Mjc3LTkyY2UtZWJkYmU2MDVkMzBhIiwiaXNzIjoiaHR0cDovL2xvY2FsaG9zdDo4MDgwL3JlYWxtcy9tYXN0ZXIiLCJhdWQiOlsibXlyZWFsbS1yZWFsbSIsIm1hc3Rlci1yZWFsbSIsImFjY291bnQiXSwic3ViIjoiZTliOGE2OWItM2RlNy00ZDYzLWFjYmItMmYyNTRhMDM1MjVkIiwidHlwIjoiQmVhcmVyIiwiYXpwIjoidGVzdC13ZWJhcHAiLCJzZXNzaW9uX3N0YXRlIjoiMTQ1OTdhMmUtOGM1Ni00YzkwLWI3NjAtZWFjYzczNWU1Zjc1IiwiYWNyIjoiMSIsInJlYWxtX2FjY2VzcyI6eyJyb2xlcyI6WyJjcmVhdGUtcmVhbG0iLCJkZWZhdWx0LXJvbGVzLW1hc3RlciIsIm9mZmxpbmVfYWNjZXNzIiwiYWRtaW4iLCJ1bWFfYXV0aG9yaXphdGlvbiIsInVzZXIiXX0sInJlc291cmNlX2FjY2VzcyI6eyJteXJlYWxtLXJlYWxtIjp7InJvbGVzIjpbInZpZXctcmVhbG0iLCJ2aWV3LWlkZW50aXR5LXByb3ZpZGVycyIsIm1hbmFnZS1pZGVudGl0eS1wcm92aWRlcnMiLCJpbXBlcnNvbmF0aW9uIiwiY3JlYXRlLWNsaWVudCIsIm1hbmFnZS11c2VycyIsInF1ZXJ5LXJlYWxtcyIsInZpZXctYXV0aG9yaXphdGlvbiIsInF1ZXJ5LWNsaWVudHMiLCJxdWVyeS11c2VycyIsIm1hbmFnZS1ldmVudHMiLCJtYW5hZ2UtcmVhbG0iLCJ2aWV3LWV2ZW50cyIsInZpZXctdXNlcnMiLCJ2aWV3LWNsaWVudHMiLCJtYW5hZ2UtYXV0aG9yaXphdGlvbiIsIm1hbmFnZS1jbGllbnRzIiwicXVlcnktZ3JvdXBzIl19LCJtYXN0ZXItcmVhbG0iOnsicm9sZXMiOlsidmlldy1yZWFsbSIsInZpZXctaWRlbnRpdHktcHJvdmlkZXJzIiwibWFuYWdlLWlkZW50aXR5LXByb3ZpZGVycyIsImltcGVyc29uYXRpb24iLCJjcmVhdGUtY2xpZW50IiwibWFuYWdlLXVzZXJzIiwicXVlcnktcmVhbG1zIiwidmlldy1hdXRob3JpemF0aW9uIiwicXVlcnktY2xpZW50cyIsInF1ZXJ5LXVzZXJzIiwibWFuYWdlLWV2ZW50cyIsIm1hbmFnZS1yZWFsbSIsInZpZXctZXZlbnRzIiwidmlldy11c2VycyIsInZpZXctY2xpZW50cyIsIm1hbmFnZS1hdXRob3JpemF0aW9uIiwibWFuYWdlLWNsaWVudHMiLCJxdWVyeS1ncm91cHMiXX0sImFjY291bnQiOnsicm9sZXMiOlsibWFuYWdlLWFjY291bnQiLCJtYW5hZ2UtYWNjb3VudC1saW5rcyIsInZpZXctcHJvZmlsZSJdfX0sInNjb3BlIjoiZW1haWwgcHJvZmlsZSIsInNpZCI6IjE0NTk3YTJlLThjNTYtNGM5MC1iNzYwLWVhY2M3MzVlNWY3NSIsImVtYWlsX3ZlcmlmaWVkIjpmYWxzZSwicHJlZmVycmVkX3VzZXJuYW1lIjoiYWxpY2UifQ.hVj6SG-aTcDYhifdljpiBcz4ShCHej3h_4-82rgX0s_oJ-En68Cqt-_DgJLtMdr6dW_gQFFCPYBJfEGvZ8L6b_TwzbdLxyrQrKTOpeG0KJ8VAFlbWum9B1vvES_sav1Gj1sQHlV621EaLISYz7pnknuQEvrB7liJFRRjN9SH30AsAJy6nmKTDHGZ6Eegkveqd_7POaKfsHS3Z0-SGyL5GClXv9yZ1l5Y4VH-rrMUztLPCFH5bJ319-m-7sgizvV-C2EcM37XVAtPRVQbJNRW0wVmLEJKMuLYVnjS1Wn5eU_qnBvVMEaENNG3TzNd6b4YmxMFHFf9tnkb3wkDzdrRTA";
        performBearerAuthentication(BEARER_ONLY_PROVIDER_URL_APP, KeycloakConfiguration.ALICE, KeycloakConfiguration.ALICE_PASSWORD,
                SimpleServlet.RESPONSE_BODY, wrongToken, BearerAuthType.QUERY_PARAM);
    }

    @Test
    @OperateOnDeployment(BEARER_ONLY_PROVIDER_URL_APP)
    public void testInvalidTokenViaQueryParameter() throws Exception {
        performBearerAuthentication(BEARER_ONLY_PROVIDER_URL_APP, KeycloakConfiguration.ALICE, KeycloakConfiguration.ALICE_PASSWORD,
                SimpleServlet.RESPONSE_BODY, "INVALID_TOKEN", BearerAuthType.QUERY_PARAM);
    }

    /**
     * Tests that rely on obtaining the bearer token to use from credentials obtained from basic auth.
     */

    @Test
    @OperateOnDeployment(BEARER_ONLY_PROVIDER_URL_APP)
    public void testBasicAuthenticationWithoutEnableBasicAuthSet() throws Exception {
        accessAppWithoutToken(BEARER_ONLY_PROVIDER_URL_APP, true, true, KeycloakConfiguration.ALICE, KeycloakConfiguration.ALICE_PASSWORD);
    }

    @Test
    @OperateOnDeployment(PROVIDER_URL_APP)
    public void testBasicAuthenticationWithoutEnableBasicAuthSetAndWithoutBearerOnlySet() throws Exception {
        // ensure the regular OIDC flow takes place
        accessAppWithoutToken(PROVIDER_URL_APP, true, false, null, HttpURLConnection.HTTP_OK, SimpleServlet.RESPONSE_BODY);
    }

    @Test
    @OperateOnDeployment(BASIC_AUTH_PROVIDER_URL_APP)
    public void testValidCredentialsBasicAuthentication() throws Exception {
        performBearerAuthentication(BASIC_AUTH_PROVIDER_URL_APP, KeycloakConfiguration.ALICE, KeycloakConfiguration.ALICE_PASSWORD,
                SimpleServlet.RESPONSE_BODY, null, BearerAuthType.BASIC);
    }

    @Test
    @OperateOnDeployment(BASIC_AUTH_PROVIDER_URL_APP)
    public void testInvalidCredentialsBasicAuthentication() throws Exception {
        accessAppWithoutToken(BASIC_AUTH_PROVIDER_URL_APP, true, true, KeycloakConfiguration.ALICE, WRONG_PASSWORD);
    }

    /**
     * Tests that simulate CORS preflight requests.
     */

    @Test
    @OperateOnDeployment(CORS_PROVIDER_URL_APP)
    public void testCorsRequestWithEnableCors() throws Exception {
        performBearerAuthenticationWithCors(CORS_PROVIDER_URL_APP, KeycloakConfiguration.ALICE, KeycloakConfiguration.ALICE_PASSWORD,
                SimpleServlet.RESPONSE_BODY, null, CORS_CLIENT, CLIENT_SECRET, ALLOWED_ORIGIN, true);
    }

    @Test
    @OperateOnDeployment(CORS_PROVIDER_URL_APP)
    public void testCorsRequestWithEnableCorsWithWrongToken() throws Exception {
        String wrongToken = "eyJhbGciOiJSUzI1NiIsInR5cCIgOiAiSldUIiwia2lkIiA6ICJrNmhQYTdHdmdrajdFdlhLeFAtRjFLZkNSUk85Q3kwNC04YzFqTERWOXNrIn0.eyJleHAiOjE2NTc2NjExODksImlhdCI6MTY1NzY2MTEyOSwianRpIjoiZThiZGQ3MWItYTA2OC00Mjc3LTkyY2UtZWJkYmU2MDVkMzBhIiwiaXNzIjoiaHR0cDovL2xvY2FsaG9zdDo4MDgwL3JlYWxtcy9tYXN0ZXIiLCJhdWQiOlsibXlyZWFsbS1yZWFsbSIsIm1hc3Rlci1yZWFsbSIsImFjY291bnQiXSwic3ViIjoiZTliOGE2OWItM2RlNy00ZDYzLWFjYmItMmYyNTRhMDM1MjVkIiwidHlwIjoiQmVhcmVyIiwiYXpwIjoidGVzdC13ZWJhcHAiLCJzZXNzaW9uX3N0YXRlIjoiMTQ1OTdhMmUtOGM1Ni00YzkwLWI3NjAtZWFjYzczNWU1Zjc1IiwiYWNyIjoiMSIsInJlYWxtX2FjY2VzcyI6eyJyb2xlcyI6WyJjcmVhdGUtcmVhbG0iLCJkZWZhdWx0LXJvbGVzLW1hc3RlciIsIm9mZmxpbmVfYWNjZXNzIiwiYWRtaW4iLCJ1bWFfYXV0aG9yaXphdGlvbiIsInVzZXIiXX0sInJlc291cmNlX2FjY2VzcyI6eyJteXJlYWxtLXJlYWxtIjp7InJvbGVzIjpbInZpZXctcmVhbG0iLCJ2aWV3LWlkZW50aXR5LXByb3ZpZGVycyIsIm1hbmFnZS1pZGVudGl0eS1wcm92aWRlcnMiLCJpbXBlcnNvbmF0aW9uIiwiY3JlYXRlLWNsaWVudCIsIm1hbmFnZS11c2VycyIsInF1ZXJ5LXJlYWxtcyIsInZpZXctYXV0aG9yaXphdGlvbiIsInF1ZXJ5LWNsaWVudHMiLCJxdWVyeS11c2VycyIsIm1hbmFnZS1ldmVudHMiLCJtYW5hZ2UtcmVhbG0iLCJ2aWV3LWV2ZW50cyIsInZpZXctdXNlcnMiLCJ2aWV3LWNsaWVudHMiLCJtYW5hZ2UtYXV0aG9yaXphdGlvbiIsIm1hbmFnZS1jbGllbnRzIiwicXVlcnktZ3JvdXBzIl19LCJtYXN0ZXItcmVhbG0iOnsicm9sZXMiOlsidmlldy1yZWFsbSIsInZpZXctaWRlbnRpdHktcHJvdmlkZXJzIiwibWFuYWdlLWlkZW50aXR5LXByb3ZpZGVycyIsImltcGVyc29uYXRpb24iLCJjcmVhdGUtY2xpZW50IiwibWFuYWdlLXVzZXJzIiwicXVlcnktcmVhbG1zIiwidmlldy1hdXRob3JpemF0aW9uIiwicXVlcnktY2xpZW50cyIsInF1ZXJ5LXVzZXJzIiwibWFuYWdlLWV2ZW50cyIsIm1hbmFnZS1yZWFsbSIsInZpZXctZXZlbnRzIiwidmlldy11c2VycyIsInZpZXctY2xpZW50cyIsIm1hbmFnZS1hdXRob3JpemF0aW9uIiwibWFuYWdlLWNsaWVudHMiLCJxdWVyeS1ncm91cHMiXX0sImFjY291bnQiOnsicm9sZXMiOlsibWFuYWdlLWFjY291bnQiLCJtYW5hZ2UtYWNjb3VudC1saW5rcyIsInZpZXctcHJvZmlsZSJdfX0sInNjb3BlIjoiZW1haWwgcHJvZmlsZSIsInNpZCI6IjE0NTk3YTJlLThjNTYtNGM5MC1iNzYwLWVhY2M3MzVlNWY3NSIsImVtYWlsX3ZlcmlmaWVkIjpmYWxzZSwicHJlZmVycmVkX3VzZXJuYW1lIjoiYWxpY2UifQ.hVj6SG-aTcDYhifdljpiBcz4ShCHej3h_4-82rgX0s_oJ-En68Cqt-_DgJLtMdr6dW_gQFFCPYBJfEGvZ8L6b_TwzbdLxyrQrKTOpeG0KJ8VAFlbWum9B1vvES_sav1Gj1sQHlV621EaLISYz7pnknuQEvrB7liJFRRjN9SH30AsAJy6nmKTDHGZ6Eegkveqd_7POaKfsHS3Z0-SGyL5GClXv9yZ1l5Y4VH-rrMUztLPCFH5bJ319-m-7sgizvV-C2EcM37XVAtPRVQbJNRW0wVmLEJKMuLYVnjS1Wn5eU_qnBvVMEaENNG3TzNd6b4YmxMFHFf9tnkb3wkDzdrRTA";
        performBearerAuthenticationWithCors(CORS_PROVIDER_URL_APP, KeycloakConfiguration.ALICE, KeycloakConfiguration.ALICE_PASSWORD,
                SimpleServlet.RESPONSE_BODY, wrongToken, CORS_CLIENT, CLIENT_SECRET, ALLOWED_ORIGIN, true);
    }

    @Test
    @OperateOnDeployment(CORS_PROVIDER_URL_APP)
    public void testCorsRequestWithEnableCorsWithInvalidToken() throws Exception {
        performBearerAuthenticationWithCors(CORS_PROVIDER_URL_APP, KeycloakConfiguration.ALICE, KeycloakConfiguration.ALICE_PASSWORD,
                SimpleServlet.RESPONSE_BODY, "INVALID_TOKEN", CORS_CLIENT, CLIENT_SECRET, ALLOWED_ORIGIN, true);
    }

    @Test
    @OperateOnDeployment(CORS_PROVIDER_URL_APP)
    public void testCorsRequestWithEnableCorsWithInvalidOrigin() throws Exception {
        performBearerAuthenticationWithCors(CORS_PROVIDER_URL_APP, KeycloakConfiguration.ALICE, KeycloakConfiguration.ALICE_PASSWORD,
                SimpleServlet.RESPONSE_BODY, null, CORS_CLIENT, CLIENT_SECRET, "http://invalidorigin", true);
    }

    @Test
    @OperateOnDeployment(BEARER_ONLY_PROVIDER_URL_APP)
    public void testCorsRequestWithoutEnableCors() throws Exception {
        performBearerAuthenticationWithCors(BEARER_ONLY_PROVIDER_URL_APP, KeycloakConfiguration.ALICE, KeycloakConfiguration.ALICE_PASSWORD,
                SimpleServlet.RESPONSE_BODY, null, CORS_CLIENT, CLIENT_SECRET, ALLOWED_ORIGIN, false);
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

    private void performBearerAuthentication(String appName, String username, String password,
                                             String clientPageText, String bearerToken, BearerAuthType bearerAuthType) throws Exception {
        performBearerAuthentication(appName, username, password, clientPageText, bearerToken, bearerAuthType, null, null);

    }

    private void performBearerAuthentication(String appName, String username, String password,
                                             String clientPageText, String bearerToken, BearerAuthType bearerAuthType,
                                             String clientId, String clientSecret) throws Exception {
        HttpClient httpClient = HttpClients.createDefault();
        HttpGet getMethod;
        HttpContext context = new BasicHttpContext();
        HttpResponse response;
        URI requestUri;
        switch (bearerAuthType) {
            case QUERY_PARAM:
                if (bearerToken == null) {
                    // obtain a bearer token and then try accessing the endpoint with a query param specified
                    requestUri = new URL("http", TestSuiteEnvironment.getHttpAddress(), TestSuiteEnvironment.getHttpPort(),
                            "/" + appName + SimpleSecuredServlet.SERVLET_PATH + "?access_token="
                                    + KeycloakConfiguration.getAccessToken(KEYCLOAK_CONTAINER.getAuthServerUrl(), TEST_REALM, username,
                                    password, clientId, clientSecret)).toURI();
                } else {
                    // try accessing the endpoint with the given bearer token specified using a query param
                    requestUri = new URL("http", TestSuiteEnvironment.getHttpAddress(), TestSuiteEnvironment.getHttpPort(),
                            "/" + appName + SimpleSecuredServlet.SERVLET_PATH + "?access_token=" + bearerToken).toURI();
                }
                getMethod = new HttpGet(requestUri);
                break;
            case BASIC:
                requestUri = new URL("http", TestSuiteEnvironment.getHttpAddress(), TestSuiteEnvironment.getHttpPort(),
                        "/" + appName + SimpleSecuredServlet.SERVLET_PATH).toURI();
                getMethod = new HttpGet(requestUri);
                getMethod.addHeader("Authorization", "Basic " + CodePointIterator.ofString(username + ":" + password).asUtf8().base64Encode().drainToString());
                break;
            default: // BEARER
                requestUri = new URL("http", TestSuiteEnvironment.getHttpAddress(), TestSuiteEnvironment.getHttpPort(),
                        "/" + appName + SimpleSecuredServlet.SERVLET_PATH).toURI();
                getMethod = new HttpGet(requestUri);
                if (bearerToken == null) {
                    // obtain a bearer token and then try accessing the endpoint with the Authorization header specified
                    getMethod.addHeader("Authorization", "Bearer " + KeycloakConfiguration.getAccessToken(KEYCLOAK_CONTAINER.getAuthServerUrl(), TEST_REALM, username,
                            password, clientId, clientSecret));
                } else {
                    // try accessing the endpoint with the given bearer token specified using the Authorization header
                    getMethod.addHeader("Authorization", "Bearer " + bearerToken);
                }
                break;
        }
        response = httpClient.execute(getMethod, context);
        try {
            int statusCode = response.getStatusLine().getStatusCode();
            if (bearerToken == null) {
                assertEquals(HttpURLConnection.HTTP_OK, statusCode);
                String responseString = new BasicResponseHandler().handleResponse(response);
                assertTrue(responseString.contains(clientPageText));
            } else {
                assertTrue("Expected code == UNAUTHORIZED but got " + statusCode + " for request=" + requestUri, statusCode == HttpURLConnection.HTTP_UNAUTHORIZED);
            }
        } finally {
            HttpClientUtils.closeQuietly(response);
        }
    }

    private void performBearerAuthenticationWithCors(String appName, String username, String password,
                                                     String clientPageText, String bearerToken,
                                                     String clientId, String clientSecret, String originHeader, boolean corsEnabled) throws Exception {
        URI requestUri = new URL("http", TestSuiteEnvironment.getHttpAddress(), TestSuiteEnvironment.getHttpPort(),
                "/" + appName + SimpleSecuredServlet.SERVLET_PATH).toURI();
        HttpClient httpClient = HttpClients.createDefault();
        HttpOptions optionsMethod = new HttpOptions(requestUri);
        HttpContext context = new BasicHttpContext();
        HttpResponse response;
        optionsMethod.addHeader(ORIGIN, originHeader);
        optionsMethod.addHeader(ACCESS_CONTROL_REQUEST_HEADERS, "authorization");
        optionsMethod.addHeader(ACCESS_CONTROL_REQUEST_METHOD, "GET");
        response = httpClient.execute(optionsMethod, context);

        try {

            int statusCode = response.getStatusLine().getStatusCode();
            if (corsEnabled) {
                assertEquals(HttpURLConnection.HTTP_OK, statusCode);
                assertTrue(Boolean.valueOf(response.getFirstHeader(ACCESS_CONTROL_ALLOW_CREDENTIALS).getValue()));
                assertEquals("authorization", response.getFirstHeader(ACCESS_CONTROL_ALLOW_HEADERS).getValue());
                assertEquals("GET", response.getFirstHeader(ACCESS_CONTROL_ALLOW_METHODS).getValue());
                assertEquals(originHeader, response.getFirstHeader(ACCESS_CONTROL_ALLOW_ORIGIN).getValue());


                HttpGet getMethod = new HttpGet(requestUri);
                getMethod.addHeader(ORIGIN, originHeader);
                if (bearerToken == null) {
                    // obtain a bearer token and then try accessing the endpoint with the Authorization header specified
                    getMethod.addHeader("Authorization", "Bearer " + KeycloakConfiguration.getAccessToken(KEYCLOAK_CONTAINER.getAuthServerUrl(), TEST_REALM, username,
                            password, clientId, clientSecret));
                } else {
                    // try accessing the endpoint with the given bearer token specified using the Authorization header
                    getMethod.addHeader("Authorization", "Bearer " + bearerToken);
                }

                response = httpClient.execute(getMethod, context);

                statusCode = response.getStatusLine().getStatusCode();
                if (bearerToken == null) {
                    if (originHeader.equals(ALLOWED_ORIGIN)) {
                        assertEquals(HttpURLConnection.HTTP_OK, statusCode);
                        String responseString = new BasicResponseHandler().handleResponse(response);
                        assertTrue(responseString.contains(clientPageText));
                    } else {
                        assertEquals(HttpURLConnection.HTTP_FORBIDDEN, statusCode);
                    }
                } else {
                    assertTrue("Expected code == UNAUTHORIZED but got " + statusCode + " for request=" + requestUri, statusCode == HttpURLConnection.HTTP_UNAUTHORIZED);
                }
            } else {
                assertEquals(HttpURLConnection.HTTP_UNAUTHORIZED, statusCode);
                Header authenticateHeader = response.getFirstHeader("WWW-Authenticate");
                assertEquals("Bearer", authenticateHeader.getValue());
            }
        } finally {
            HttpClientUtils.closeQuietly(response);
        }
    }

    private void accessAppWithoutToken(String appName, boolean includeBasicHeader, boolean bearerOnlyOrEnableBasicConfigured) throws Exception {
        accessAppWithoutToken(appName, includeBasicHeader, bearerOnlyOrEnableBasicConfigured, null);
    }

    private void accessAppWithoutToken(String appName, boolean includeBasicHeader, boolean bearerOnlyOrEnableBasicConfigured, String realm) throws Exception {
        accessAppWithoutToken(appName, includeBasicHeader, bearerOnlyOrEnableBasicConfigured, realm, -1, null);
    }

    private void accessAppWithoutToken(String appName, boolean includeBasicHeader, boolean bearerOnlyOrEnableBasicConfigured, String realm, int expectedStatusCode, String expectedText) throws Exception {
        accessAppWithoutToken(appName, includeBasicHeader, bearerOnlyOrEnableBasicConfigured, realm, expectedStatusCode, expectedText, null, null);
    }

    private void accessAppWithoutToken(String appName, boolean includeBasicHeader, boolean bearerOnlyOrEnableBasicConfigured, String username, String password) throws Exception {
        accessAppWithoutToken(appName, includeBasicHeader, bearerOnlyOrEnableBasicConfigured, null, -1, null, username, password);
    }

    private void accessAppWithoutToken(String appName, boolean includeBasicHeader, boolean bearerOnlyOrEnableBasicConfigured, String realm, int expectedStatusCode, String expectedText,
                                       String username, String password) throws Exception {
        final URI requestUri = new URL("http", TestSuiteEnvironment.getHttpAddress(), TestSuiteEnvironment.getHttpPort(),
                "/" + appName + SimpleSecuredServlet.SERVLET_PATH).toURI();
        CookieStore store = new BasicCookieStore();
        HttpClient httpClient = TestHttpClientUtils.promiscuousCookieHttpClientBuilder()
                .setDefaultCookieStore(store)
                .setRedirectStrategy(new LaxRedirectStrategy())
                .build();
        HttpGet getMethod = new HttpGet(requestUri);
        if (includeBasicHeader) {
            getMethod.addHeader("Authorization", "Basic " + CodePointIterator.ofString(username + ":" + password).asUtf8().base64Encode().drainToString());
        }
        HttpContext context = new BasicHttpContext();

        HttpResponse response = httpClient.execute(getMethod, context);
        try {
            int statusCode = response.getStatusLine().getStatusCode();
            if (bearerOnlyOrEnableBasicConfigured) {
                assertTrue("Expected code == UNAUTHORIZED but got " + statusCode + " for request=" + requestUri, statusCode == HttpURLConnection.HTTP_UNAUTHORIZED);
                Header authenticateHeader = response.getFirstHeader("WWW-Authenticate");
                String authenticateValue = authenticateHeader.getValue();
                if (password != null && password.equals(WRONG_PASSWORD)) {
                    assertTrue(authenticateValue.startsWith("Bearer error=\"" + "no_token" + "\""));
                    assertTrue(authenticateValue.contains("error_description"));
                    assertTrue(authenticateValue.contains(String.valueOf(HttpURLConnection.HTTP_UNAUTHORIZED)));
                } else if (realm != null) {
                    assertEquals("Bearer realm=\"" + TEST_REALM + "\"", authenticateValue);
                } else {
                    assertEquals("Bearer", authenticateValue);
                }
            } else {
                // no token provided and bearer-only is not configured, should end up in the OIDC flow
                assertTrue("Expected code == OK but got " + statusCode + " for request=" + requestUri, statusCode == HttpURLConnection.HTTP_OK);
                Form keycloakLoginForm = new Form(response);
                HttpResponse afterLoginClickResponse = simulateClickingOnButton(httpClient, keycloakLoginForm,
                        KeycloakConfiguration.ALICE, KeycloakConfiguration.ALICE_PASSWORD, "Sign In");
                afterLoginClickResponse.getEntity().getContent();
                assertEquals(expectedStatusCode, afterLoginClickResponse.getStatusLine().getStatusCode());
                if (expectedText != null) {
                    String responseString = new BasicResponseHandler().handleResponse(afterLoginClickResponse);
                    assertTrue(responseString.contains(expectedText));
                }
            }
        } finally {
            HttpClientUtils.closeQuietly(response);
        }
    }

    public static class KeycloakSetup implements ServerSetupTask {

        @Override
        public void setup(ManagementClient managementClient, String containerId) throws Exception {
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
