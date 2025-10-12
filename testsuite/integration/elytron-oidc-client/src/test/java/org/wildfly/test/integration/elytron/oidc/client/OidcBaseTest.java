/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.test.integration.elytron.oidc.client;

import static org.apache.http.HttpStatus.SC_OK;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SYSTEM_PROPERTY;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.VALUE;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assume.assumeTrue;
import static org.wildfly.security.http.oidc.Oidc.OIDC_SCOPE;
import static org.wildfly.security.http.oidc.Oidc.AuthenticationRequestFormat.OAUTH2;
import static org.wildfly.security.http.oidc.Oidc.AuthenticationRequestFormat.REQUEST;
import static org.wildfly.security.http.oidc.Oidc.AuthenticationRequestFormat.REQUEST_URI;
import static org.wildfly.test.integration.elytron.oidc.client.KeycloakConfiguration.ALLOWED_ORIGIN;

import java.io.File;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
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
import org.apache.http.util.EntityUtils;
import org.jboss.as.arquillian.api.ServerSetupTask;
import org.jboss.as.arquillian.container.ManagementClient;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.operations.common.Util;
import org.jboss.as.test.http.util.TestHttpClientUtils;
import org.jboss.as.test.integration.management.ManagementOperations;
import org.jboss.as.test.integration.security.common.servlets.SimpleSecuredServlet;
import org.jboss.as.test.integration.security.common.servlets.SimpleServlet;
import org.jboss.as.test.shared.ManagementServerSetupTask;
import org.jboss.as.test.shared.TestSuiteEnvironment;
import org.jboss.as.test.shared.TimeoutUtil;
import org.jboss.as.test.shared.util.AssumeTestGroupUtil;
import org.jboss.dmr.ModelNode;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.junit.BeforeClass;
import org.keycloak.representations.idm.RealmRepresentation;
import org.wildfly.common.iteration.CodePointIterator;
import org.wildfly.security.jose.util.JsonSerialization;
import org.wildfly.test.integration.elytron.oidc.client.deployment.OidcWithDeploymentConfigTest;
import org.wildfly.test.integration.elytron.oidc.client.subsystem.SimpleServletWithScope;

import io.restassured.RestAssured;

/**
 * Tests for the OpenID Connect authentication mechanism.
 *
 * @author <a href="mailto:fjuma@redhat.com">Farah Juma</a>
 */
public abstract class OidcBaseTest {

    public static final String CLIENT_SECRET = "longerclientsecretthatisstleast256bitslong";
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
    public static final String FORM_WITH_OIDC_EAR_APP = "FormWithOidcApp";
    public static final String FORM_WITH_OIDC_OIDC_APP = "oidc";
    public static final String DIRECT_ACCESS_GRANT_ENABLED_CLIENT = "DirectAccessGrantEnabledClient";
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
    public static final String OPENID_SCOPE_APP = "OpenIDScopeApp";
    public static final String INVALID_SCOPE_APP = "InvalidScopeApp";
    public static final String SINGLE_SCOPE_APP = "SingleScopeApp";
    public static final String MULTIPLE_SCOPE_APP = "MultipleScopeApp";
    public static final String OAUTH2_REQUEST_METHOD_APP = "OAuth2RequestApp";
    public static final String PLAINTEXT_REQUEST_APP = "PlainTextRequestApp";
    public static final String PLAINTEXT_REQUEST_URI_APP = "PlainTextRequestUriApp";
    public static final String PLAINTEXT_ENCRYPTED_REQUEST_APP = "PlainTextEncryptedRequestApp";
    public static final String PLAINTEXT_ENCRYPTED_REQUEST_URI_APP = "PlainTextEncryptedRequestUriApp";
    public static final String RSA_SIGNED_REQUEST_APP = "RsaSignedRequestApp";
    public static final String RSA_SIGNED_AND_ENCRYPTED_REQUEST_APP = "RSASignedAndEncryptedRequestApp";
    public static final String SIGNED_AND_ENCRYPTED_REQUEST_URI_APP = "SignedAndEncryptedRequestUriApp";
    public static final String PS_SIGNED_RSA_ENCRYPTED_REQUEST_APP = "PsSignedAndRsaEncryptedRequestApp";
    public static final String INVALID_SIGNATURE_ALGORITHM_APP = "InvalidSignatureAlgorithmApp";
    public static final String PS_SIGNED_REQUEST_URI_APP = "PsSignedRequestUriApp";
    public static final String MISSING_SECRET_APP = "MissingSecretApp";
    public static final String FORM_USER="user1";
    public static final String FORM_PASSWORD="password1";
    protected static final String ERROR_PAGE_CONTENT = "Error!";

    // Avoid problem on windows with path
    public static final String USERS_PATH = new File(
            OidcWithDeploymentConfigTest.class.getResource("users.properties").getFile()).getAbsolutePath()
            .replace("\\", "/");
    public static final String ROLES_PATH = new File(
            OidcWithDeploymentConfigTest.class.getResource("roles.properties").getFile()).getAbsolutePath()
            .replace("\\", "/");
    public static final String ORIGINAL_USERS_PATH = "application-users.properties";
    public static final String ORIGINAL_ROLES_PATH = "application-roles.properties";
    public static final String RELATIVE_TO = "jboss.server.config.dir";

    private static final long REALM_CREATION_TIMEOUT = TimeoutUtil.adjust(20000);

    private enum BearerAuthType {
        BEARER,
        QUERY_PARAM,
        BASIC
    }

    public static void sendRealmCreationRequest(RealmRepresentation realm) {
        long timeout = System.currentTimeMillis() + REALM_CREATION_TIMEOUT;
        while (true) {
            try {
                String adminAccessToken = KeycloakConfiguration.getAdminAccessToken(KEYCLOAK_CONTAINER.getAuthServerUrl());
                assertNotNull(adminAccessToken);
                RestAssured
                        .given()
                        .auth().oauth2(adminAccessToken)
                        .contentType("application/json")
                        .body(JsonSerialization.writeValueAsBytes(realm))
                        .when()
                        .post(KEYCLOAK_CONTAINER.getAuthServerUrl() + "/admin/realms").then()
                        .statusCode(201);
                return;
            } catch (IOException | AssertionError e) {
                if (System.currentTimeMillis() >= timeout) {
                    // Time's up; throw on the failure
                    if (e instanceof IOException) {
                        throw new RuntimeException(e);
                    }
                    throw (AssertionError) e;
                }
                try {
                    //noinspection BusyWait
                    Thread.sleep(100);
                } catch (InterruptedException ex) {
                    Thread.currentThread().interrupt();
                    throw new RuntimeException(ex);
                }
            }
        }
    }

    @BeforeClass
    public static void checkDockerAvailability() {
        assumeTrue("Docker isn't available, OIDC tests will be skipped", AssumeTestGroupUtil.isDockerAvailable());
    }

    public void testWrongPasswordWithProviderUrl() throws Exception {
        loginToApp(PROVIDER_URL_APP, KeycloakConfiguration.ALICE, "WRONG_PASSWORD", HttpURLConnection.HTTP_OK, "Invalid username or password");
    }

    public void testSuccessfulAuthenticationWithProviderUrl() throws Exception {
        loginToApp(PROVIDER_URL_APP, KeycloakConfiguration.ALICE, KeycloakConfiguration.ALICE_PASSWORD, HttpURLConnection.HTTP_OK, SimpleServlet.RESPONSE_BODY);
    }

    public void testWrongRoleWithProviderUrl() throws Exception {
        loginToApp(PROVIDER_URL_APP, KeycloakConfiguration.BOB, KeycloakConfiguration.BOB_PASSWORD, HttpURLConnection.HTTP_FORBIDDEN, null);
    }

    public void testWrongPasswordWithAuthServerUrl() throws Exception {
        loginToApp(AUTH_SERVER_URL_APP, KeycloakConfiguration.ALICE, "WRONG_PASSWORD", HttpURLConnection.HTTP_OK, "Invalid username or password");
    }

    public void testSuccessfulAuthenticationWithAuthServerUrl() throws Exception {
        loginToApp(AUTH_SERVER_URL_APP, KeycloakConfiguration.ALICE, KeycloakConfiguration.ALICE_PASSWORD, HttpURLConnection.HTTP_OK, SimpleServlet.RESPONSE_BODY);
    }

    public void testWrongRoleWithAuthServerUrl() throws Exception {
        loginToApp(AUTH_SERVER_URL_APP, KeycloakConfiguration.BOB, KeycloakConfiguration.BOB_PASSWORD, HttpURLConnection.HTTP_FORBIDDEN, null);
    }

    public void testWrongProviderUrl() throws Exception {
        loginToApp(WRONG_PROVIDER_URL_APP, KeycloakConfiguration.ALICE, KeycloakConfiguration.ALICE_PASSWORD, -1, null, false);
    }

    public void testWrongClientSecret() throws Exception {
        loginToApp(WRONG_SECRET_APP, KeycloakConfiguration.ALICE, KeycloakConfiguration.ALICE_PASSWORD, HttpURLConnection.HTTP_FORBIDDEN, null);
    }

    /**
     * Tests that use a bearer token.
     */

    public void testSuccessfulBearerOnlyAuthenticationWithAuthServerUrl() throws Exception {
        performBearerAuthentication(BEARER_ONLY_AUTH_SERVER_URL_APP, KeycloakConfiguration.ALICE, KeycloakConfiguration.ALICE_PASSWORD,
                SimpleServlet.RESPONSE_BODY, null, BearerAuthType.BEARER, DIRECT_ACCESS_GRANT_ENABLED_CLIENT, CLIENT_SECRET);
    }

    public void testSuccessfulBearerOnlyAuthenticationWithProviderUrl() throws Exception {
        performBearerAuthentication(BEARER_ONLY_PROVIDER_URL_APP, KeycloakConfiguration.ALICE, KeycloakConfiguration.ALICE_PASSWORD,
                SimpleServlet.RESPONSE_BODY, null, BearerAuthType.BEARER, DIRECT_ACCESS_GRANT_ENABLED_CLIENT, CLIENT_SECRET);
    }

    public void testWrongToken() throws Exception {
        String wrongToken = "eyJhbGciOiJSUzI1NiIsInR5cCIgOiAiSldUIiwia2lkIiA6ICJrNmhQYTdHdmdrajdFdlhLeFAtRjFLZkNSUk85Q3kwNC04YzFqTERWOXNrIn0.eyJleHAiOjE2NTc2NjExODksImlhdCI6MTY1NzY2MTEyOSwianRpIjoiZThiZGQ3MWItYTA2OC00Mjc3LTkyY2UtZWJkYmU2MDVkMzBhIiwiaXNzIjoiaHR0cDovL2xvY2FsaG9zdDo4MDgwL3JlYWxtcy9tYXN0ZXIiLCJhdWQiOlsibXlyZWFsbS1yZWFsbSIsIm1hc3Rlci1yZWFsbSIsImFjY291bnQiXSwic3ViIjoiZTliOGE2OWItM2RlNy00ZDYzLWFjYmItMmYyNTRhMDM1MjVkIiwidHlwIjoiQmVhcmVyIiwiYXpwIjoidGVzdC13ZWJhcHAiLCJzZXNzaW9uX3N0YXRlIjoiMTQ1OTdhMmUtOGM1Ni00YzkwLWI3NjAtZWFjYzczNWU1Zjc1IiwiYWNyIjoiMSIsInJlYWxtX2FjY2VzcyI6eyJyb2xlcyI6WyJjcmVhdGUtcmVhbG0iLCJkZWZhdWx0LXJvbGVzLW1hc3RlciIsIm9mZmxpbmVfYWNjZXNzIiwiYWRtaW4iLCJ1bWFfYXV0aG9yaXphdGlvbiIsInVzZXIiXX0sInJlc291cmNlX2FjY2VzcyI6eyJteXJlYWxtLXJlYWxtIjp7InJvbGVzIjpbInZpZXctcmVhbG0iLCJ2aWV3LWlkZW50aXR5LXByb3ZpZGVycyIsIm1hbmFnZS1pZGVudGl0eS1wcm92aWRlcnMiLCJpbXBlcnNvbmF0aW9uIiwiY3JlYXRlLWNsaWVudCIsIm1hbmFnZS11c2VycyIsInF1ZXJ5LXJlYWxtcyIsInZpZXctYXV0aG9yaXphdGlvbiIsInF1ZXJ5LWNsaWVudHMiLCJxdWVyeS11c2VycyIsIm1hbmFnZS1ldmVudHMiLCJtYW5hZ2UtcmVhbG0iLCJ2aWV3LWV2ZW50cyIsInZpZXctdXNlcnMiLCJ2aWV3LWNsaWVudHMiLCJtYW5hZ2UtYXV0aG9yaXphdGlvbiIsIm1hbmFnZS1jbGllbnRzIiwicXVlcnktZ3JvdXBzIl19LCJtYXN0ZXItcmVhbG0iOnsicm9sZXMiOlsidmlldy1yZWFsbSIsInZpZXctaWRlbnRpdHktcHJvdmlkZXJzIiwibWFuYWdlLWlkZW50aXR5LXByb3ZpZGVycyIsImltcGVyc29uYXRpb24iLCJjcmVhdGUtY2xpZW50IiwibWFuYWdlLXVzZXJzIiwicXVlcnktcmVhbG1zIiwidmlldy1hdXRob3JpemF0aW9uIiwicXVlcnktY2xpZW50cyIsInF1ZXJ5LXVzZXJzIiwibWFuYWdlLWV2ZW50cyIsIm1hbmFnZS1yZWFsbSIsInZpZXctZXZlbnRzIiwidmlldy11c2VycyIsInZpZXctY2xpZW50cyIsIm1hbmFnZS1hdXRob3JpemF0aW9uIiwibWFuYWdlLWNsaWVudHMiLCJxdWVyeS1ncm91cHMiXX0sImFjY291bnQiOnsicm9sZXMiOlsibWFuYWdlLWFjY291bnQiLCJtYW5hZ2UtYWNjb3VudC1saW5rcyIsInZpZXctcHJvZmlsZSJdfX0sInNjb3BlIjoiZW1haWwgcHJvZmlsZSIsInNpZCI6IjE0NTk3YTJlLThjNTYtNGM5MC1iNzYwLWVhY2M3MzVlNWY3NSIsImVtYWlsX3ZlcmlmaWVkIjpmYWxzZSwicHJlZmVycmVkX3VzZXJuYW1lIjoiYWxpY2UifQ.hVj6SG-aTcDYhifdljpiBcz4ShCHej3h_4-82rgX0s_oJ-En68Cqt-_DgJLtMdr6dW_gQFFCPYBJfEGvZ8L6b_TwzbdLxyrQrKTOpeG0KJ8VAFlbWum9B1vvES_sav1Gj1sQHlV621EaLISYz7pnknuQEvrB7liJFRRjN9SH30AsAJy6nmKTDHGZ6Eegkveqd_7POaKfsHS3Z0-SGyL5GClXv9yZ1l5Y4VH-rrMUztLPCFH5bJ319-m-7sgizvV-C2EcM37XVAtPRVQbJNRW0wVmLEJKMuLYVnjS1Wn5eU_qnBvVMEaENNG3TzNd6b4YmxMFHFf9tnkb3wkDzdrRTA";
        performBearerAuthentication(BEARER_ONLY_PROVIDER_URL_APP, KeycloakConfiguration.ALICE, KeycloakConfiguration.ALICE_PASSWORD,
                SimpleServlet.RESPONSE_BODY, wrongToken, BearerAuthType.BEARER);
    }

    public void testInvalidToken() throws Exception {
        performBearerAuthentication(BEARER_ONLY_PROVIDER_URL_APP, KeycloakConfiguration.ALICE, KeycloakConfiguration.ALICE_PASSWORD,
                SimpleServlet.RESPONSE_BODY, "INVALID_TOKEN", BearerAuthType.BEARER);
    }

    public void testNoTokenProvidedWithAuthServerUrl() throws Exception {
        accessAppWithoutToken(BEARER_ONLY_AUTH_SERVER_URL_APP, false, true, TEST_REALM);
    }

    public void testNoTokenProvidedWithProviderUrl() throws Exception {
        accessAppWithoutToken(BEARER_ONLY_PROVIDER_URL_APP, false, true);
    }

    public void testTokenProvidedBearerOnlyNotSet() throws Exception {
        performBearerAuthentication(PROVIDER_URL_APP, KeycloakConfiguration.ALICE, KeycloakConfiguration.ALICE_PASSWORD,
                SimpleServlet.RESPONSE_BODY, null, BearerAuthType.BEARER, DIRECT_ACCESS_GRANT_ENABLED_CLIENT, CLIENT_SECRET);
    }

    public void testTokenNotProvidedBearerOnlyNotSet() throws Exception {
        // ensure the regular OIDC flow takes place
        accessAppWithoutToken(PROVIDER_URL_APP, false, false,null, HttpURLConnection.HTTP_OK, SimpleServlet.RESPONSE_BODY);
    }

    /**
     * Tests that pass the bearer token to use via an access_token query param.
     */

    public void testValidTokenViaQueryParameter() throws Exception {
        performBearerAuthentication(BEARER_ONLY_PROVIDER_URL_APP, KeycloakConfiguration.ALICE, KeycloakConfiguration.ALICE_PASSWORD,
                SimpleServlet.RESPONSE_BODY, null, BearerAuthType.QUERY_PARAM, DIRECT_ACCESS_GRANT_ENABLED_CLIENT, CLIENT_SECRET);
    }

    public void testWrongTokenViaQueryParameter() throws Exception {
        String wrongToken = "eyJhbGciOiJSUzI1NiIsInR5cCIgOiAiSldUIiwia2lkIiA6ICJrNmhQYTdHdmdrajdFdlhLeFAtRjFLZkNSUk85Q3kwNC04YzFqTERWOXNrIn0.eyJleHAiOjE2NTc2NjExODksImlhdCI6MTY1NzY2MTEyOSwianRpIjoiZThiZGQ3MWItYTA2OC00Mjc3LTkyY2UtZWJkYmU2MDVkMzBhIiwiaXNzIjoiaHR0cDovL2xvY2FsaG9zdDo4MDgwL3JlYWxtcy9tYXN0ZXIiLCJhdWQiOlsibXlyZWFsbS1yZWFsbSIsIm1hc3Rlci1yZWFsbSIsImFjY291bnQiXSwic3ViIjoiZTliOGE2OWItM2RlNy00ZDYzLWFjYmItMmYyNTRhMDM1MjVkIiwidHlwIjoiQmVhcmVyIiwiYXpwIjoidGVzdC13ZWJhcHAiLCJzZXNzaW9uX3N0YXRlIjoiMTQ1OTdhMmUtOGM1Ni00YzkwLWI3NjAtZWFjYzczNWU1Zjc1IiwiYWNyIjoiMSIsInJlYWxtX2FjY2VzcyI6eyJyb2xlcyI6WyJjcmVhdGUtcmVhbG0iLCJkZWZhdWx0LXJvbGVzLW1hc3RlciIsIm9mZmxpbmVfYWNjZXNzIiwiYWRtaW4iLCJ1bWFfYXV0aG9yaXphdGlvbiIsInVzZXIiXX0sInJlc291cmNlX2FjY2VzcyI6eyJteXJlYWxtLXJlYWxtIjp7InJvbGVzIjpbInZpZXctcmVhbG0iLCJ2aWV3LWlkZW50aXR5LXByb3ZpZGVycyIsIm1hbmFnZS1pZGVudGl0eS1wcm92aWRlcnMiLCJpbXBlcnNvbmF0aW9uIiwiY3JlYXRlLWNsaWVudCIsIm1hbmFnZS11c2VycyIsInF1ZXJ5LXJlYWxtcyIsInZpZXctYXV0aG9yaXphdGlvbiIsInF1ZXJ5LWNsaWVudHMiLCJxdWVyeS11c2VycyIsIm1hbmFnZS1ldmVudHMiLCJtYW5hZ2UtcmVhbG0iLCJ2aWV3LWV2ZW50cyIsInZpZXctdXNlcnMiLCJ2aWV3LWNsaWVudHMiLCJtYW5hZ2UtYXV0aG9yaXphdGlvbiIsIm1hbmFnZS1jbGllbnRzIiwicXVlcnktZ3JvdXBzIl19LCJtYXN0ZXItcmVhbG0iOnsicm9sZXMiOlsidmlldy1yZWFsbSIsInZpZXctaWRlbnRpdHktcHJvdmlkZXJzIiwibWFuYWdlLWlkZW50aXR5LXByb3ZpZGVycyIsImltcGVyc29uYXRpb24iLCJjcmVhdGUtY2xpZW50IiwibWFuYWdlLXVzZXJzIiwicXVlcnktcmVhbG1zIiwidmlldy1hdXRob3JpemF0aW9uIiwicXVlcnktY2xpZW50cyIsInF1ZXJ5LXVzZXJzIiwibWFuYWdlLWV2ZW50cyIsIm1hbmFnZS1yZWFsbSIsInZpZXctZXZlbnRzIiwidmlldy11c2VycyIsInZpZXctY2xpZW50cyIsIm1hbmFnZS1hdXRob3JpemF0aW9uIiwibWFuYWdlLWNsaWVudHMiLCJxdWVyeS1ncm91cHMiXX0sImFjY291bnQiOnsicm9sZXMiOlsibWFuYWdlLWFjY291bnQiLCJtYW5hZ2UtYWNjb3VudC1saW5rcyIsInZpZXctcHJvZmlsZSJdfX0sInNjb3BlIjoiZW1haWwgcHJvZmlsZSIsInNpZCI6IjE0NTk3YTJlLThjNTYtNGM5MC1iNzYwLWVhY2M3MzVlNWY3NSIsImVtYWlsX3ZlcmlmaWVkIjpmYWxzZSwicHJlZmVycmVkX3VzZXJuYW1lIjoiYWxpY2UifQ.hVj6SG-aTcDYhifdljpiBcz4ShCHej3h_4-82rgX0s_oJ-En68Cqt-_DgJLtMdr6dW_gQFFCPYBJfEGvZ8L6b_TwzbdLxyrQrKTOpeG0KJ8VAFlbWum9B1vvES_sav1Gj1sQHlV621EaLISYz7pnknuQEvrB7liJFRRjN9SH30AsAJy6nmKTDHGZ6Eegkveqd_7POaKfsHS3Z0-SGyL5GClXv9yZ1l5Y4VH-rrMUztLPCFH5bJ319-m-7sgizvV-C2EcM37XVAtPRVQbJNRW0wVmLEJKMuLYVnjS1Wn5eU_qnBvVMEaENNG3TzNd6b4YmxMFHFf9tnkb3wkDzdrRTA";
        performBearerAuthentication(BEARER_ONLY_PROVIDER_URL_APP, KeycloakConfiguration.ALICE, KeycloakConfiguration.ALICE_PASSWORD,
                SimpleServlet.RESPONSE_BODY, wrongToken, BearerAuthType.QUERY_PARAM);
    }

    public void testInvalidTokenViaQueryParameter() throws Exception {
        performBearerAuthentication(BEARER_ONLY_PROVIDER_URL_APP, KeycloakConfiguration.ALICE, KeycloakConfiguration.ALICE_PASSWORD,
                SimpleServlet.RESPONSE_BODY, "INVALID_TOKEN", BearerAuthType.QUERY_PARAM);
    }

    /**
     * Tests that rely on obtaining the bearer token to use from credentials obtained from basic auth.
     */

    public void testBasicAuthenticationWithoutEnableBasicAuthSet() throws Exception {
        accessAppWithoutToken(BEARER_ONLY_PROVIDER_URL_APP, true, true, KeycloakConfiguration.ALICE, KeycloakConfiguration.ALICE_PASSWORD);
    }

    public void testBasicAuthenticationWithoutEnableBasicAuthSetAndWithoutBearerOnlySet() throws Exception {
        // ensure the regular OIDC flow takes place
        accessAppWithoutToken(PROVIDER_URL_APP, true, false, null, HttpURLConnection.HTTP_OK, SimpleServlet.RESPONSE_BODY);
    }

    public void testValidCredentialsBasicAuthentication() throws Exception {
        performBearerAuthentication(BASIC_AUTH_PROVIDER_URL_APP, KeycloakConfiguration.ALICE, KeycloakConfiguration.ALICE_PASSWORD,
                SimpleServlet.RESPONSE_BODY, null, BearerAuthType.BASIC);
    }

    public void testInvalidCredentialsBasicAuthentication() throws Exception {
        accessAppWithoutToken(BASIC_AUTH_PROVIDER_URL_APP, true, true, KeycloakConfiguration.ALICE, WRONG_PASSWORD);
    }

    /**
     * Tests that simulate CORS preflight requests.
     */

    public void testCorsRequestWithEnableCors() throws Exception {
        performBearerAuthenticationWithCors(CORS_PROVIDER_URL_APP, KeycloakConfiguration.ALICE, KeycloakConfiguration.ALICE_PASSWORD,
                SimpleServlet.RESPONSE_BODY, null, CORS_CLIENT, CLIENT_SECRET, ALLOWED_ORIGIN, true);
    }

    public void testCorsRequestWithEnableCorsWithWrongToken() throws Exception {
        String wrongToken = "eyJhbGciOiJSUzI1NiIsInR5cCIgOiAiSldUIiwia2lkIiA6ICJrNmhQYTdHdmdrajdFdlhLeFAtRjFLZkNSUk85Q3kwNC04YzFqTERWOXNrIn0.eyJleHAiOjE2NTc2NjExODksImlhdCI6MTY1NzY2MTEyOSwianRpIjoiZThiZGQ3MWItYTA2OC00Mjc3LTkyY2UtZWJkYmU2MDVkMzBhIiwiaXNzIjoiaHR0cDovL2xvY2FsaG9zdDo4MDgwL3JlYWxtcy9tYXN0ZXIiLCJhdWQiOlsibXlyZWFsbS1yZWFsbSIsIm1hc3Rlci1yZWFsbSIsImFjY291bnQiXSwic3ViIjoiZTliOGE2OWItM2RlNy00ZDYzLWFjYmItMmYyNTRhMDM1MjVkIiwidHlwIjoiQmVhcmVyIiwiYXpwIjoidGVzdC13ZWJhcHAiLCJzZXNzaW9uX3N0YXRlIjoiMTQ1OTdhMmUtOGM1Ni00YzkwLWI3NjAtZWFjYzczNWU1Zjc1IiwiYWNyIjoiMSIsInJlYWxtX2FjY2VzcyI6eyJyb2xlcyI6WyJjcmVhdGUtcmVhbG0iLCJkZWZhdWx0LXJvbGVzLW1hc3RlciIsIm9mZmxpbmVfYWNjZXNzIiwiYWRtaW4iLCJ1bWFfYXV0aG9yaXphdGlvbiIsInVzZXIiXX0sInJlc291cmNlX2FjY2VzcyI6eyJteXJlYWxtLXJlYWxtIjp7InJvbGVzIjpbInZpZXctcmVhbG0iLCJ2aWV3LWlkZW50aXR5LXByb3ZpZGVycyIsIm1hbmFnZS1pZGVudGl0eS1wcm92aWRlcnMiLCJpbXBlcnNvbmF0aW9uIiwiY3JlYXRlLWNsaWVudCIsIm1hbmFnZS11c2VycyIsInF1ZXJ5LXJlYWxtcyIsInZpZXctYXV0aG9yaXphdGlvbiIsInF1ZXJ5LWNsaWVudHMiLCJxdWVyeS11c2VycyIsIm1hbmFnZS1ldmVudHMiLCJtYW5hZ2UtcmVhbG0iLCJ2aWV3LWV2ZW50cyIsInZpZXctdXNlcnMiLCJ2aWV3LWNsaWVudHMiLCJtYW5hZ2UtYXV0aG9yaXphdGlvbiIsIm1hbmFnZS1jbGllbnRzIiwicXVlcnktZ3JvdXBzIl19LCJtYXN0ZXItcmVhbG0iOnsicm9sZXMiOlsidmlldy1yZWFsbSIsInZpZXctaWRlbnRpdHktcHJvdmlkZXJzIiwibWFuYWdlLWlkZW50aXR5LXByb3ZpZGVycyIsImltcGVyc29uYXRpb24iLCJjcmVhdGUtY2xpZW50IiwibWFuYWdlLXVzZXJzIiwicXVlcnktcmVhbG1zIiwidmlldy1hdXRob3JpemF0aW9uIiwicXVlcnktY2xpZW50cyIsInF1ZXJ5LXVzZXJzIiwibWFuYWdlLWV2ZW50cyIsIm1hbmFnZS1yZWFsbSIsInZpZXctZXZlbnRzIiwidmlldy11c2VycyIsInZpZXctY2xpZW50cyIsIm1hbmFnZS1hdXRob3JpemF0aW9uIiwibWFuYWdlLWNsaWVudHMiLCJxdWVyeS1ncm91cHMiXX0sImFjY291bnQiOnsicm9sZXMiOlsibWFuYWdlLWFjY291bnQiLCJtYW5hZ2UtYWNjb3VudC1saW5rcyIsInZpZXctcHJvZmlsZSJdfX0sInNjb3BlIjoiZW1haWwgcHJvZmlsZSIsInNpZCI6IjE0NTk3YTJlLThjNTYtNGM5MC1iNzYwLWVhY2M3MzVlNWY3NSIsImVtYWlsX3ZlcmlmaWVkIjpmYWxzZSwicHJlZmVycmVkX3VzZXJuYW1lIjoiYWxpY2UifQ.hVj6SG-aTcDYhifdljpiBcz4ShCHej3h_4-82rgX0s_oJ-En68Cqt-_DgJLtMdr6dW_gQFFCPYBJfEGvZ8L6b_TwzbdLxyrQrKTOpeG0KJ8VAFlbWum9B1vvES_sav1Gj1sQHlV621EaLISYz7pnknuQEvrB7liJFRRjN9SH30AsAJy6nmKTDHGZ6Eegkveqd_7POaKfsHS3Z0-SGyL5GClXv9yZ1l5Y4VH-rrMUztLPCFH5bJ319-m-7sgizvV-C2EcM37XVAtPRVQbJNRW0wVmLEJKMuLYVnjS1Wn5eU_qnBvVMEaENNG3TzNd6b4YmxMFHFf9tnkb3wkDzdrRTA";
        performBearerAuthenticationWithCors(CORS_PROVIDER_URL_APP, KeycloakConfiguration.ALICE, KeycloakConfiguration.ALICE_PASSWORD,
                SimpleServlet.RESPONSE_BODY, wrongToken, CORS_CLIENT, CLIENT_SECRET, ALLOWED_ORIGIN, true);
    }

    public void testCorsRequestWithEnableCorsWithInvalidToken() throws Exception {
        performBearerAuthenticationWithCors(CORS_PROVIDER_URL_APP, KeycloakConfiguration.ALICE, KeycloakConfiguration.ALICE_PASSWORD,
                SimpleServlet.RESPONSE_BODY, "INVALID_TOKEN", CORS_CLIENT, CLIENT_SECRET, ALLOWED_ORIGIN, true);
    }

    public void testCorsRequestWithEnableCorsWithInvalidOrigin() throws Exception {
        performBearerAuthenticationWithCors(CORS_PROVIDER_URL_APP, KeycloakConfiguration.ALICE, KeycloakConfiguration.ALICE_PASSWORD,
                SimpleServlet.RESPONSE_BODY, null, CORS_CLIENT, CLIENT_SECRET, "http://invalidorigin", true);
    }

    public void testCorsRequestWithoutEnableCors() throws Exception {
        performBearerAuthenticationWithCors(BEARER_ONLY_PROVIDER_URL_APP, KeycloakConfiguration.ALICE, KeycloakConfiguration.ALICE_PASSWORD,
                SimpleServlet.RESPONSE_BODY, null, CORS_CLIENT, CLIENT_SECRET, ALLOWED_ORIGIN, false);
    }

    /**
     * Tests that use different scope values to request access to claims values.
     */

    public void testOpenIDScope() throws Exception {
        String expectedScope = OIDC_SCOPE;
        loginToApp(KeycloakConfiguration.ALICE, KeycloakConfiguration.ALICE_PASSWORD, HttpURLConnection.HTTP_OK, SimpleServlet.RESPONSE_BODY, true,
                new URL("http", TestSuiteEnvironment.getHttpAddress(), TestSuiteEnvironment.getHttpPort(),
                        "/" + OPENID_SCOPE_APP + SimpleServletWithScope.SERVLET_PATH).toURI(), expectedScope, false);
    }

    public void testSingleScope() throws Exception {
        String expectedScope = OIDC_SCOPE + "+profile";
        loginToApp(KeycloakConfiguration.ALICE, KeycloakConfiguration.ALICE_PASSWORD, HttpURLConnection.HTTP_OK, SimpleServlet.RESPONSE_BODY, true,
                new URL("http", TestSuiteEnvironment.getHttpAddress(), TestSuiteEnvironment.getHttpPort(),
                        "/" + SINGLE_SCOPE_APP + SimpleServletWithScope.SERVLET_PATH).toURI(), expectedScope, false);
    }


    public void testMultipleScope() throws Exception {
        String expectedScope = OIDC_SCOPE + "+phone+profile+microprofile-jwt+email";
        loginToApp(KeycloakConfiguration.ALICE, KeycloakConfiguration.ALICE_PASSWORD, HttpURLConnection.HTTP_OK, SimpleServlet.RESPONSE_BODY, true,
                new URL("http", TestSuiteEnvironment.getHttpAddress(), TestSuiteEnvironment.getHttpPort(),
                        "/" + MULTIPLE_SCOPE_APP + SimpleServletWithScope.SERVLET_PATH).toURI(), expectedScope, false);
    }

    public void testInvalidScope() throws Exception {
        String expectedScope = OIDC_SCOPE + "+INVALID_SCOPE";
        loginToApp(KeycloakConfiguration.ALICE, KeycloakConfiguration.ALICE_PASSWORD, HttpURLConnection.HTTP_OK, SimpleServlet.RESPONSE_BODY, false,
                new URL("http", TestSuiteEnvironment.getHttpAddress(), TestSuiteEnvironment.getHttpPort(),
                        "/" + INVALID_SCOPE_APP + SimpleServletWithScope.SERVLET_PATH).toURI(), expectedScope, true);
    }

    /**
    * Tests that use authentication-request-format to send request objects using request and request_uri
    **/

    public void testOpenIDWithOauth2Request() throws Exception {
        loginToApp(KeycloakConfiguration.ALICE, KeycloakConfiguration.ALICE_PASSWORD, HttpURLConnection.HTTP_OK, SimpleServlet.RESPONSE_BODY, true,
                new URL("http", TestSuiteEnvironment.getHttpAddress(), TestSuiteEnvironment.getHttpPort(),
                        "/" + OAUTH2_REQUEST_METHOD_APP + SimpleSecuredServlet.SERVLET_PATH).toURI(), null, false, OAUTH2.getValue());
    }

    public void testOpenIDWithPlainTextRequest() throws Exception {
        loginToApp(KeycloakConfiguration.ALICE, KeycloakConfiguration.ALICE_PASSWORD, HttpURLConnection.HTTP_OK, SimpleServlet.RESPONSE_BODY, true,
                new URL("http", TestSuiteEnvironment.getHttpAddress(), TestSuiteEnvironment.getHttpPort(),
                        "/" + PLAINTEXT_REQUEST_APP + SimpleSecuredServlet.SERVLET_PATH).toURI(), null, false, REQUEST.getValue());
    }

    public void testOpenIDWithPlainTextRequestUri() throws Exception {
        loginToApp(KeycloakConfiguration.ALICE, KeycloakConfiguration.ALICE_PASSWORD, HttpURLConnection.HTTP_OK, SimpleServlet.RESPONSE_BODY, true,
                new URL("http", TestSuiteEnvironment.getHttpAddress(), TestSuiteEnvironment.getHttpPort(),
                        "/" + PLAINTEXT_REQUEST_URI_APP + SimpleSecuredServlet.SERVLET_PATH).toURI(), null, false, REQUEST_URI.getValue());
    }

    public void testOpenIDWithPlainTextEncryptedRequest() throws Exception {
        loginToApp(KeycloakConfiguration.ALICE, KeycloakConfiguration.ALICE_PASSWORD, HttpURLConnection.HTTP_OK, SimpleServlet.RESPONSE_BODY, true,
                new URL("http", TestSuiteEnvironment.getHttpAddress(), TestSuiteEnvironment.getHttpPort(),
                        "/" + PLAINTEXT_ENCRYPTED_REQUEST_APP + SimpleSecuredServlet.SERVLET_PATH).toURI(), null, false, REQUEST.getValue());
    }

    public void testOpenIDWithPlainTextEncryptedRequestUri() throws Exception {
        loginToApp(KeycloakConfiguration.ALICE, KeycloakConfiguration.ALICE_PASSWORD, HttpURLConnection.HTTP_OK, SimpleServlet.RESPONSE_BODY, true,
                new URL("http", TestSuiteEnvironment.getHttpAddress(), TestSuiteEnvironment.getHttpPort(),
                        "/" + PLAINTEXT_ENCRYPTED_REQUEST_URI_APP + SimpleSecuredServlet.SERVLET_PATH).toURI(), null, false, REQUEST_URI.getValue());
    }

    public void testOpenIDWithRsaSignedRequest() throws Exception {
        loginToApp(KeycloakConfiguration.ALICE, KeycloakConfiguration.ALICE_PASSWORD, HttpURLConnection.HTTP_OK, SimpleServlet.RESPONSE_BODY, true,
                new URL("http", TestSuiteEnvironment.getHttpAddress(), TestSuiteEnvironment.getHttpPort(),
                        "/" + RSA_SIGNED_REQUEST_APP + SimpleSecuredServlet.SERVLET_PATH).toURI(), null, false, REQUEST.getValue());
    }

    public void testOpenIDWithRsaSignedAndEncryptedRequest() throws Exception {
        loginToApp(KeycloakConfiguration.ALICE, KeycloakConfiguration.ALICE_PASSWORD, HttpURLConnection.HTTP_OK, SimpleServlet.RESPONSE_BODY, true,
                new URL("http", TestSuiteEnvironment.getHttpAddress(), TestSuiteEnvironment.getHttpPort(),
                        "/" + RSA_SIGNED_AND_ENCRYPTED_REQUEST_APP + SimpleSecuredServlet.SERVLET_PATH).toURI(), null, false, REQUEST.getValue());
    }

    public void testOpenIDWithSignedAndEncryptedRequestUri() throws Exception {
        loginToApp(KeycloakConfiguration.ALICE, KeycloakConfiguration.ALICE_PASSWORD, HttpURLConnection.HTTP_OK, SimpleServlet.RESPONSE_BODY, true,
                new URL("http", TestSuiteEnvironment.getHttpAddress(), TestSuiteEnvironment.getHttpPort(),
                        "/" + SIGNED_AND_ENCRYPTED_REQUEST_URI_APP + SimpleSecuredServlet.SERVLET_PATH).toURI(), null, false, REQUEST_URI.getValue());
    }

    public void testOpenIDWithPsSignedRequestUri() throws Exception {
        loginToApp(KeycloakConfiguration.ALICE, KeycloakConfiguration.ALICE_PASSWORD, HttpURLConnection.HTTP_OK, SimpleServlet.RESPONSE_BODY, true,
                new URL("http", TestSuiteEnvironment.getHttpAddress(), TestSuiteEnvironment.getHttpPort(),
                        "/" + PS_SIGNED_REQUEST_URI_APP + SimpleSecuredServlet.SERVLET_PATH).toURI(), null, false, REQUEST_URI.getValue());
    }

    public void testOpenIDWithPsSignedAndRsaEncryptedRequest() throws Exception {
        loginToApp(KeycloakConfiguration.ALICE, KeycloakConfiguration.ALICE_PASSWORD, HttpURLConnection.HTTP_OK, SimpleServlet.RESPONSE_BODY, true,
                new URL("http", TestSuiteEnvironment.getHttpAddress(), TestSuiteEnvironment.getHttpPort(),
                        "/" + PS_SIGNED_RSA_ENCRYPTED_REQUEST_APP + SimpleSecuredServlet.SERVLET_PATH).toURI(), null, false, REQUEST.getValue());
    }

    public void testOpenIDWithInvalidSigningAlgorithm() throws Exception {
        testRequestObjectInvalidConfiguration(new URL("http", TestSuiteEnvironment.getHttpAddress(), TestSuiteEnvironment.getHttpPort(),
                        "/" + INVALID_SIGNATURE_ALGORITHM_APP + SimpleSecuredServlet.SERVLET_PATH).toURI(), true);
    }

    public void testOpenIDWithMissingSecretHmacSigningAlgorithm() throws Exception {
        //Expected to fail since the client secret is needed to sign the JWT
        testRequestObjectInvalidConfiguration(new URL("http", TestSuiteEnvironment.getHttpAddress(), TestSuiteEnvironment.getHttpPort(),
                "/" + MISSING_SECRET_APP + SimpleSecuredServlet.SERVLET_PATH).toURI(), true);
    }

    public void testFormWithOidc() throws Exception {
        // oidc login
        // EAR declares context-root to be oidc
        loginToApp(FORM_WITH_OIDC_OIDC_APP,
                KeycloakConfiguration.ALICE,
                KeycloakConfiguration.ALICE_PASSWORD,
                HttpURLConnection.HTTP_OK, SimpleServlet.RESPONSE_BODY);

        // login with Form wfly user acct
        testFormCredentials();
    }
    private void testFormCredentials() throws Exception {
        URI requestUri = new URI("http://"+CLIENT_HOST_NAME+":"+CLIENT_PORT
                +"/form"+"/"+SimpleSecuredServlet.class.getSimpleName()
                +"/j_security_check");
        HttpClient httpClient = HttpClients.createDefault();
        HttpPost getMethod = new HttpPost(requestUri);

        List<NameValuePair> nvps = new ArrayList<NameValuePair>();
        nvps.add(new BasicNameValuePair("j_username", FORM_USER));
        nvps.add(new BasicNameValuePair("j_password", FORM_PASSWORD));

        getMethod.setEntity(new UrlEncodedFormEntity(nvps, StandardCharsets.UTF_8));

        HttpResponse response = httpClient.execute(getMethod);
        int statusCode = response.getStatusLine().getStatusCode();
        assertEquals("Expected code == OK but got " + statusCode +
                " for request=" + requestUri, HttpURLConnection.HTTP_MOVED_TEMP, statusCode);
    }

    public void testInvalidFormWithOidcCredentials() throws Exception {
        // login with Form wfly user acct
        testInvalidFormCredentials();
        // oidc login
        // EAR declares context-root to be oidc
        loginToApp(FORM_WITH_OIDC_OIDC_APP,
                KeycloakConfiguration.ALICE,
                "WRONG_PASSWORD", HttpURLConnection.HTTP_OK, "Invalid username or password");
    }
    public void testInvalidFormCredentials() throws Exception {
        URI requestUri = new URI("http://"+CLIENT_HOST_NAME+":"+CLIENT_PORT
                +"/form"+"/"+SimpleSecuredServlet.class.getSimpleName()
                +"/j_security_check");
        HttpClient httpClient = HttpClients.createDefault();
        HttpPost getMethod = new HttpPost(requestUri);

        List<NameValuePair> nvps = new ArrayList<NameValuePair>();
        nvps.add(new BasicNameValuePair("j_username", "Not"+FORM_USER));
        nvps.add(new BasicNameValuePair("j_password", "Not"+FORM_PASSWORD));

        getMethod.setEntity(new UrlEncodedFormEntity(nvps, StandardCharsets.UTF_8));

        HttpResponse response = httpClient.execute(getMethod);
        int statusCode = response.getStatusLine().getStatusCode();
        assertEquals("For request=" + requestUri +"  Unexpected status code in HTTP response.",
                SC_OK,  statusCode );
        String errorMsg = EntityUtils.toString(response.getEntity());
        assertTrue("Expected HTTP response to contain " + ERROR_PAGE_CONTENT
                + "  response msg is: " + errorMsg, errorMsg.contains(ERROR_PAGE_CONTENT));
    }

    public static void loginToApp(String appName, String username, String password, int expectedStatusCode, String expectedText) throws Exception {
        loginToApp(username, password, expectedStatusCode, expectedText, true,
                new URL("http", TestSuiteEnvironment.getHttpAddress(), TestSuiteEnvironment.getHttpPort(),
                "/" + appName + SimpleSecuredServlet.SERVLET_PATH).toURI());
    }

    public static void loginToApp(String appName, String username, String password, int expectedStatusCode, String expectedText, URI requestUri) throws Exception {
        loginToApp(username, password, expectedStatusCode, expectedText, true, requestUri);
    }

    public static void loginToApp(String appName, String username, String password, int expectedStatusCode, String expectedText, boolean loginToKeycloak) throws Exception {
        loginToApp(username, password, expectedStatusCode, expectedText, loginToKeycloak, new URL("http", TestSuiteEnvironment.getHttpAddress(), TestSuiteEnvironment.getHttpPort(),
                "/" + appName + SimpleSecuredServlet.SERVLET_PATH).toURI());
    }

    public static void loginToApp(String username, String password, int expectedStatusCode, String expectedText, boolean loginToKeycloak, URI requestUri) throws Exception {
        loginToApp(username, password, expectedStatusCode, expectedText, loginToKeycloak, requestUri, null, false);
    }

    public static void loginToApp(String username, String password, int expectedStatusCode, String expectedText, boolean loginToKeycloak, URI requestUri, String expectedScope, boolean checkInvalidScope) throws Exception {
        loginToApp(username, password, expectedStatusCode, expectedText, loginToKeycloak, requestUri, expectedScope, checkInvalidScope, null);
    }

    public static void loginToApp(String username, String password, int expectedStatusCode, String expectedText, boolean loginToKeycloak, URI requestUri, String expectedScope, boolean checkInvalidScope, String requestMethod) throws Exception {
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
                assertEquals("Expected code == OK but got " + statusCode + " for request=" + requestUri, HttpURLConnection.HTTP_OK, statusCode);
                Form keycloakLoginForm = new Form(response);
                HttpResponse afterLoginClickResponse = simulateClickingOnButton(httpClient, keycloakLoginForm, username, password, "Sign In");
                afterLoginClickResponse.getEntity().getContent();
                assertEquals(expectedStatusCode, afterLoginClickResponse.getStatusLine().getStatusCode());
                if (expectedText != null) {
                    String responseString = new BasicResponseHandler().handleResponse(afterLoginClickResponse);
                    assertTrue("Unexpected result " + responseString, responseString.contains(expectedText));
                    if (expectedScope != null) {
                        assertTrue(context.toString().contains("scope=" + expectedScope));
                        if (expectedScope.contains("profile")) {
                            assertTrue(responseString.contains("profile: " + KeycloakConfiguration.ALICE_FIRST_NAME + " " + KeycloakConfiguration.ALICE_LAST_NAME));
                        }
                        if (expectedScope.contains("email")) {
                            assertTrue(responseString.contains("email: " + KeycloakConfiguration.ALICE_EMAIL_VERIFIED));
                        }
                        if (expectedScope.contains("microprofile-jwt")) {
                            assertTrue(responseString.contains("microprofile-jwt: [" + KeycloakConfiguration.JBOSS_ADMIN_ROLE + ", " + KeycloakConfiguration.USER_ROLE + "]"));
                        }
                    }
                }

                if (requestMethod != null && requestMethod.equals(REQUEST_URI.getValue())) {
                    assertFalse(context.toString().contains("scope=" + OIDC_SCOPE + "+phone+profile+email")); // additional scope values should be inside the request object
                    assertTrue(context.toString().contains("scope=" + OIDC_SCOPE));
                    assertTrue(context.toString().contains("request_uri="));
                } else if (requestMethod != null && requestMethod.equals(REQUEST.getValue())) {
                    assertFalse(context.toString().contains("scope=" + OIDC_SCOPE + "+phone+profile+email")); // additional scope values should be inside the request object
                    assertTrue(context.toString().contains("scope=" + OIDC_SCOPE));
                    assertTrue(context.toString().contains("request="));
                }
            } else if (checkInvalidScope) {
                assertTrue(context.toString().contains("error_description=Invalid+scopes"));
                assertEquals("Expected code == BAD REQUEST but got " + statusCode + " for request=" + requestUri, HttpURLConnection.HTTP_BAD_REQUEST, statusCode);
            } else {
                assertEquals("Expected code == FORBIDDEN but got " + statusCode + " for request=" + requestUri, HttpURLConnection.HTTP_FORBIDDEN, statusCode);
            }
        } finally {
            HttpClientUtils.closeQuietly(response);
        }
    }

    private void testRequestObjectInvalidConfiguration(URI requestUri, boolean expectFailure) throws Exception {
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
            if (expectFailure) {
                assertEquals("Expected code == HTTP_INTERNAL_ERROR but got " + statusCode + " for request=" + requestUri, HttpURLConnection.HTTP_INTERNAL_ERROR, statusCode);
            } else {
                assertEquals("Expected code == OK but got " + statusCode + " for request=" + requestUri, HttpURLConnection.HTTP_OK, statusCode);
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
                assertEquals("Expected code == UNAUTHORIZED but got " + statusCode + " for request=" + requestUri, HttpURLConnection.HTTP_UNAUTHORIZED, statusCode);
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
                    assertEquals("Expected code == UNAUTHORIZED but got " + statusCode + " for request=" + requestUri, HttpURLConnection.HTTP_UNAUTHORIZED, statusCode);
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
                assertEquals("Expected code == UNAUTHORIZED but got " + statusCode + " for request=" + requestUri, HttpURLConnection.HTTP_UNAUTHORIZED, statusCode);
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
                assertEquals("Expected code == OK but got " + statusCode + " for request=" + requestUri, HttpURLConnection.HTTP_OK, statusCode);
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
            assumeTrue("Docker isn't available, OIDC tests will be skipped", AssumeTestGroupUtil.isDockerAvailable());
            KEYCLOAK_CONTAINER = new KeycloakContainer();
            KEYCLOAK_CONTAINER.start();
        }

        public void tearDown(ManagementClient managementClient, String containerId) throws Exception {
            if (KEYCLOAK_CONTAINER != null) {
                KEYCLOAK_CONTAINER.stop();
            }
        }
    }

    public static HttpResponse simulateClickingOnButton(HttpClient client, Form form, String username, String password, String buttonValue) throws IOException {
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

    protected static <T extends OidcBaseTest> void addSystemProperty(ManagementClient client, Class<T> clazz) throws Exception {
        ModelNode add = Util.createAddOperation(PathAddress.pathAddress(SYSTEM_PROPERTY, OidcBaseTest.class.getName()));
        add.get(VALUE).set(clazz.getName());
        ManagementOperations.executeOperation(client.getControllerClient(), add);
    }

    public static class WildFlyServerSetupTask extends ManagementServerSetupTask {
        public WildFlyServerSetupTask() {
            super(createContainerConfigurationBuilder()
                    .setupScript(createScriptBuilder()
                            .startBatch()
                            .add("/subsystem=elytron/properties-realm=ApplicationRealm:write-attribute(name=users-properties.path, value=\"%s\")",
                                    USERS_PATH)
                            .add("/subsystem=elytron/properties-realm=ApplicationRealm:write-attribute(name=users-properties.plain-text, value=true)")
                            .add("/subsystem=elytron/properties-realm=ApplicationRealm:undefine-attribute(name=users-properties.relative-to)")
                            .add("/subsystem=elytron/properties-realm=ApplicationRealm:write-attribute(name=groups-properties.path, value=\"%s\")",
                                    ROLES_PATH)
                            .add("/subsystem=elytron/properties-realm=ApplicationRealm:undefine-attribute(name=groups-properties.relative-to)")
                            .endBatch()
                            .build())
                    .tearDownScript(createScriptBuilder()
                            .startBatch()
                            .add("/subsystem=elytron/properties-realm=ApplicationRealm:write-attribute(name=users-properties.path, value=\"%s\")",
                                    ORIGINAL_USERS_PATH)
                            .add("/subsystem=elytron/properties-realm=ApplicationRealm:write-attribute(name=users-properties.relative-to, value=\"%s\")",
                                    RELATIVE_TO)
                            .add("/subsystem=elytron/properties-realm=ApplicationRealm:undefine-attribute(name=users-properties.plain-text)")
                            .add("/subsystem=elytron/properties-realm=ApplicationRealm:write-attribute(name=groups-properties.path, value=\"%s\")",
                                    ORIGINAL_ROLES_PATH)
                            .add("/subsystem=elytron/properties-realm=ApplicationRealm:write-attribute(name=groups-properties.relative-to, value=\"%s\")",
                                    RELATIVE_TO)
                            .endBatch()
                            .build())
                    .build());
        }
    }
}
