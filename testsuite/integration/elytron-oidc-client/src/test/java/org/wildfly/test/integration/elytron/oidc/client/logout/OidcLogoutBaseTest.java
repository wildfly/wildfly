/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.test.integration.elytron.oidc.client.logout;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SYSTEM_PROPERTY;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.VALUE;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assume.assumeTrue;

import static org.wildfly.test.integration.elytron.oidc.client.KeycloakConfiguration.ALICE;
import static org.wildfly.test.integration.elytron.oidc.client.KeycloakConfiguration.ALICE_PASSWORD;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

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
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.operations.common.Util;
import org.jboss.as.test.http.util.TestHttpClientUtils;
import org.jboss.as.test.integration.management.ManagementOperations;
import org.jboss.as.test.integration.security.common.servlets.SimpleSecuredServlet;
import org.jboss.as.test.integration.security.common.servlets.SimpleServlet;
import org.jboss.as.test.shared.ManagementServerSetupTask;
import org.jboss.as.test.shared.TestSuiteEnvironment;
import org.jboss.as.test.shared.util.AssumeTestGroupUtil;
import org.jboss.as.version.Stability;
import org.jboss.dmr.ModelNode;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.representations.idm.RealmRepresentation;
import org.wildfly.security.http.oidc.OidcClientConfiguration;
import org.wildfly.security.jose.util.JsonSerialization;
import org.wildfly.test.integration.elytron.oidc.client.KeycloakConfiguration;
import org.wildfly.test.integration.elytron.oidc.client.KeycloakContainer;

import io.restassured.RestAssured;

/**
 * Tests for the OpenID Connect logout types.
 */
public abstract class OidcLogoutBaseTest {

    private static HttpClient httpClient;

    @Before
    public void createHttpClient() {
        CookieStore store = new BasicCookieStore();
        httpClient = TestHttpClientUtils.promiscuousCookieHttpClientBuilder()
                .setDefaultCookieStore(store)
                .setRedirectStrategy(new LaxRedirectStrategy())
                .build();
    }

    @BeforeClass
    public static void checkDockerAvailability() {
        assumeTrue("Docker isn't available, OIDC tests will be skipped", AssumeTestGroupUtil.isDockerAvailable());
    }

    protected static OidcClientConfiguration config = new OidcClientConfiguration();

    public static final String CLIENT_SECRET = "longerclientsecretthatisstleast256bitslong";
    public static final String WEB_XML = "web.xml";
    public static KeycloakContainer KEYCLOAK_CONTAINER;
    public static final String TEST_REALM = "WildFly";
    private static final String KEYCLOAK_USERNAME = "username";
    private static final String KEYCLOAK_PASSWORD = "password";
    public static final int CLIENT_PORT = TestSuiteEnvironment.getHttpPort();
    // This name enables the Docker container (running keycloak) to make the
    // local machine's host accessible to keycloak.
    public static final String HOST_TESTCONTAINERS_INTERNAL = "host.testcontainers.internal";
    public static final String RP_INITIATED_LOGOUT_APP = "RpInitiatedLogoutApp";
    public static final String FRONT_CHANNEL_LOGOUT_APP = "FrontChannelLogoutApp";
    public static final String BACK_CHANNEL_LOGOUT_APP = "BackChannelLogoutApp";
    public static final String POST_LOGOUT_APP = "PostLogoutApp";

    private final Stability desiredStability;

    public OidcLogoutBaseTest(Stability desiredStability) {
        this.desiredStability = desiredStability;
    }

    @Test
    @OperateOnDeployment(RP_INITIATED_LOGOUT_APP)
    public void testRpInitiatedLogout() throws Exception {

        loginToApp(RP_INITIATED_LOGOUT_APP);
        assertUserLoggedIn(RP_INITIATED_LOGOUT_APP, "GOOD");
        logoutOfKeycloak(RP_INITIATED_LOGOUT_APP, "You are logged out");
        assertUserLoggedOut(RP_INITIATED_LOGOUT_APP, "GOOD");
    }

    @Test
    @OperateOnDeployment(FRONT_CHANNEL_LOGOUT_APP)
    public void testFrontChannelLogout() throws Exception {

        loginToApp(FRONT_CHANNEL_LOGOUT_APP);
        assertUserLoggedIn(FRONT_CHANNEL_LOGOUT_APP, "GOOD");
        logoutOfKeycloak(FRONT_CHANNEL_LOGOUT_APP, "You are logging out from following apps");
        assertUserLoggedOut(FRONT_CHANNEL_LOGOUT_APP, "GOOD");
    }

    @Test
    @OperateOnDeployment(BACK_CHANNEL_LOGOUT_APP)
    public void testBackChannelLogout() throws Exception {

        loginToApp(BACK_CHANNEL_LOGOUT_APP);
        assertUserLoggedIn(BACK_CHANNEL_LOGOUT_APP, "GOOD");
        logoutOfKeycloak(BACK_CHANNEL_LOGOUT_APP,"You are logged out");
        assertUserLoggedOut(BACK_CHANNEL_LOGOUT_APP, "Sign in to your account");
    }

    @Test
    @OperateOnDeployment(POST_LOGOUT_APP)
    public void testPostLogout() throws Exception {

        loginToApp(POST_LOGOUT_APP);
        assertUserLoggedIn(POST_LOGOUT_APP, "GOOD");
        logoutOfKeycloak(POST_LOGOUT_APP, "You are logged out");
        assertUserLoggedOut(POST_LOGOUT_APP, "GOOD");
    }

    @Test
    public void testBackChannelLogoutTwo() throws Exception {
        loginToApp(BACK_CHANNEL_LOGOUT_APP);
        loginToApp(FRONT_CHANNEL_LOGOUT_APP);
        assertUserLoggedIn(BACK_CHANNEL_LOGOUT_APP, "GOOD");
        assertUserLoggedIn(FRONT_CHANNEL_LOGOUT_APP, "GOOD");
        logoutOfKeycloak(BACK_CHANNEL_LOGOUT_APP,"You are logged out");
        assertUserLoggedOut(BACK_CHANNEL_LOGOUT_APP, "Sign in to your account");
        assertUserLoggedOut(FRONT_CHANNEL_LOGOUT_APP, "Sign in to your account");
    }

    private static URL generateURL(String appName) {
        try {
            return new URL("http", TestSuiteEnvironment.getHttpAddress(),
                    TestSuiteEnvironment.getHttpPort(),
                    "/" + appName + SimpleSecuredServlet.SERVLET_PATH);
        } catch (MalformedURLException e) {
            assertFalse(e.getMessage(), false);
        }
        return null;
    }

    public static void loginToApp(String appName) throws Exception {
        loginToApp(appName, ALICE, ALICE_PASSWORD, HttpURLConnection.HTTP_OK,
                SimpleServlet.RESPONSE_BODY);
    }

    public static void loginToApp(String appName,
                                  String username, String password, int expectedStatusCode, String expectedText) throws Exception {
        loginToApp(username, password, expectedStatusCode, expectedText, true,
                generateURL(appName).toURI());
    }

    public static void loginToApp(String username, String password,
                                  int expectedStatusCode, String expectedText,
                                  boolean loginToKeycloak, URI requestUri) throws Exception {

        HttpGet getMethod = new HttpGet(requestUri);
        HttpContext context = new BasicHttpContext();
        HttpResponse response = httpClient.execute(getMethod, context);
        try {
            int statusCode = response.getStatusLine().getStatusCode();
            if (loginToKeycloak) {
                assertTrue("Expected code == OK but got " + statusCode
                        + " for request=" + requestUri, statusCode == HttpURLConnection.HTTP_OK);
                Form keycloakLoginForm = new Form(response);
                HttpResponse afterLoginClickResponse = simulateClickingOnButton(httpClient,
                        keycloakLoginForm, username, password, "Sign In");

                afterLoginClickResponse.getEntity().getContent();
                assertEquals(expectedStatusCode, afterLoginClickResponse.getStatusLine().getStatusCode());

                if (expectedText != null) {
                    String responseString = new BasicResponseHandler().handleResponse(afterLoginClickResponse);
                    assertTrue("Unexpected result " + responseString, responseString.contains(expectedText));
                }
            }
            else {
                assertTrue("Expected code == FORBIDDEN but got " + statusCode + " for request=" + requestUri, statusCode == HttpURLConnection.HTTP_FORBIDDEN);
            }
        } finally {
            HttpClientUtils.closeQuietly(response);
        }
    }

    public static void logoutOfKeycloak(String appName, String expectedText) throws Exception {
        URI requestUri = new URL("http", TestSuiteEnvironment.getHttpAddress(),
                TestSuiteEnvironment.getHttpPort(),
                "/" + appName + SimpleSecuredServlet.SERVLET_PATH
                        + config.getLogoutPath()).toURI();
        logoutOfKeycloak(requestUri, HttpURLConnection.HTTP_OK, expectedText, true);
    }

    public static void logoutOfKeycloak(URI requestUri, int expectedStatusCode, String expectedText,
                                        boolean logoutFromKeycloak) throws Exception {

        HttpContext context = new BasicHttpContext();
        HttpResponse response = null;
        HttpGet getMethod = new HttpGet(requestUri);
        response = httpClient.execute(getMethod, context);

        try {
            int statusCode = response.getStatusLine().getStatusCode();
            if (logoutFromKeycloak) {
                assertTrue("Expected code == OK but got " + statusCode + " for request=" + requestUri, statusCode == HttpURLConnection.HTTP_OK);
                response.getEntity();
                String responseString = new BasicResponseHandler().handleResponse(response);
                assertTrue("Unexpected result " + expectedText, responseString.contains(expectedText));
            }
            else {
                assertTrue("Expected code == FORBIDDEN but got " + statusCode + " for request=" + requestUri, statusCode == HttpURLConnection.HTTP_FORBIDDEN);
            }
        } finally {
            HttpClientUtils.closeQuietly(response);
        }
    }

    public static void assertUserLoggedIn(String appName, String expectedText) throws Exception {
        accessPage(generateURL(appName).toURI(), HttpURLConnection.HTTP_OK, expectedText);
    }

    public static void assertUserLoggedOut(String appName, String expectedText) throws Exception {
        accessPage(generateURL(appName).toURI(), HttpURLConnection.HTTP_OK, expectedText);
    }

    public static void accessPage(URI requestUri, int expectedStatusCode,
                                     String expectedText) throws Exception {
        HttpContext context = new BasicHttpContext();
        HttpResponse response = null;
        HttpGet getMethod = new HttpGet(requestUri);
        response = httpClient.execute(getMethod, context);

        try {
            int statusCode = response.getStatusLine().getStatusCode();
            assertTrue("Expected code == " + expectedStatusCode + " but got "
                            + statusCode + " for request=" + requestUri,
                    statusCode == expectedStatusCode);
            response.getEntity();
            String responseString = new BasicResponseHandler().handleResponse(response);
            assertTrue("Unexpected result " + expectedText,
                    responseString.contains(expectedText));
        } finally {
            HttpClientUtils.closeQuietly(response);
        }
    }

    public static class KeycloakSetup implements ServerSetupTask {

        @Override
        public void setup(ManagementClient managementClient, String containerId) throws Exception {
            assumeTrue("Docker isn't available, OIDC tests will be skipped", AssumeTestGroupUtil.isDockerAvailable());
            // stmt required to enable container to have access to local
            // machine's port.
            org.testcontainers.Testcontainers.exposeHostPorts(8080);
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

    /* Data structure containing the URL path text to be registered with keycloak
       for logout support.
    */
    public static class LogoutChannelPaths {
        public String backChannelPath = null;
        public String frontChannelPath = null;
        public List<String> postLogoutRedirectPaths = null;

        public LogoutChannelPaths(final String backChannelPath,
                                 final String frontChannelPath,
                                 final List<String> postLogoutRedirectPaths) {
            this.backChannelPath = backChannelPath;
            this.frontChannelPath = frontChannelPath;
            this.postLogoutRedirectPaths = postLogoutRedirectPaths;
        }
    }

    protected static <T extends OidcLogoutBaseTest> void addSystemProperty(ManagementClient client, Class<T> clazz) throws Exception {
        ModelNode add = Util.createAddOperation(PathAddress.pathAddress(SYSTEM_PROPERTY, OidcLogoutBaseTest.class.getName()));
        add.get(VALUE).set(clazz.getName());
        ManagementOperations.executeOperation(client.getControllerClient(), add);
    }

    /*
        Class enables easy configuring of logging messages to server.log.
        It is being maintained for future debugging needs.
     */
    public static class WildFlyServerSetupTask extends ManagementServerSetupTask {
        public WildFlyServerSetupTask() {
            super(createContainerConfigurationBuilder()
                    .setupScript(createScriptBuilder()
                            .startBatch()
                            .add("/subsystem=logging/logger=org.wildfly.security.http.oidc:add()")
                            .add("/subsystem=logging/logger=org.wildfly.security.http.oidc:write-attribute(name=level, value=TRACE)")
                            .endBatch()
                            .build())
                    .tearDownScript(createScriptBuilder()
                            .startBatch()
                            .add("/subsystem=logging/logger=org.wildfly.security.http.oidc:remove()")
                            .endBatch()
                            .build())
                    .build());
        }
    }

    /* register rpInitiated, backchannel, frontchannel, postLogoutRedirectUris with Keycloak
     */
    public static void setOidcLogoutUrls(RealmRepresentation realm,
                                         Map<String, KeycloakConfiguration.ClientAppType> clientApps,
                                         Map<String, LogoutChannelPaths> appLogout) throws Exception {

        for (ClientRepresentation client : realm.getClients()) {
            KeycloakConfiguration.ClientAppType value = clientApps.get(client.getClientId());
            if (value == KeycloakConfiguration.ClientAppType.OIDC_CLIENT) {
                List<String> redirectUris = new ArrayList<>(client.getRedirectUris());
                String redirectUri = redirectUris.get(0);
                redirectUris.add("*");
                client.setRedirectUris(redirectUris);

                int indx = redirectUri.lastIndexOf("/*");
                String tmpRedirectUri = redirectUri.substring(0,indx);

                LogoutChannelPaths logoutChannelUrls = appLogout.get(client.getClientId());
                if (logoutChannelUrls != null) {
                    if (logoutChannelUrls.backChannelPath != null) {
                        KeycloakConfiguration.setBackchannelLogoutSessionRequired(
                                client,true);
                        KeycloakConfiguration.setBackchannelLogoutUrl(client,
                                tmpRedirectUri + logoutChannelUrls.backChannelPath);
                    }
                    if (logoutChannelUrls.frontChannelPath != null) {
                        KeycloakConfiguration.setBackchannelLogoutSessionRequired(
                                client,false);
                        KeycloakConfiguration.setFrontChannelLogoutSessionRequired(
                                client,true);
                        KeycloakConfiguration.setFrontChannelLogoutUrl(client,
                                tmpRedirectUri + logoutChannelUrls.frontChannelPath);
                    }
                    if (logoutChannelUrls.postLogoutRedirectPaths != null) {
                        List<String> tmpList = new ArrayList<>();
                        for (String s : logoutChannelUrls.postLogoutRedirectPaths) {
                            tmpList.add(tmpRedirectUri + s);
                        }
                        KeycloakConfiguration.setPostLogoutRedirectUris(client, tmpList);
                    }
                }
            }
        }
    }


    public static void sendRealmCreationRequest(RealmRepresentation realm) {
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
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /* This method retained for future debugging.  It can be helpful to
        review Keycloak's log file.

        To enable logging one must add stmt, withEnv("KC_LOG_LEVEL", "DEBUG"); ,
        to KeycloakContainer.configure()

        Add a call to this method after the login, logout action of interest.
     */
    private void dumpKeycloakLog() {

        String console = KEYCLOAK_CONTAINER.getLogs();
        String fileName = "/tmp/x-keycloak-logout.log";
        java.io.PrintWriter outLog = null;
        try {
            java.io.File file = new java.io.File(fileName);
            file.delete();
            outLog = new java.io.PrintWriter(fileName);
            outLog.println(console);
        } catch(Exception e) {
            System.out.println(e.getMessage());
        } finally {
            if (outLog != null) {
                outLog.close();
            }
        }
    }
}