/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.test.integration.elytron.oidc.client.multitenancy;

import static org.jboss.as.test.integration.management.util.ModelUtil.createOpNode;
import static org.jboss.as.test.shared.PermissionUtils.createPermissionsXmlAsset;
import static org.jboss.as.test.shared.util.AssumeTestGroupUtil.isDockerAvailable;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assume.assumeTrue;
import static org.wildfly.test.integration.elytron.oidc.client.KeycloakConfiguration.getRealmRepresentation;
import static org.wildfly.test.integration.elytron.oidc.client.KeycloakConfiguration.TENANT1_ENDPOINT;
import static org.wildfly.test.integration.elytron.oidc.client.KeycloakConfiguration.TENANT2_ENDPOINT;
import static org.wildfly.test.integration.elytron.oidc.client.KeycloakConfiguration.TENANT1_REALM;
import static org.wildfly.test.integration.elytron.oidc.client.KeycloakConfiguration.TENANT2_REALM;
import static org.wildfly.test.integration.elytron.oidc.client.KeycloakConfiguration.TENANT1_USER;
import static org.wildfly.test.integration.elytron.oidc.client.KeycloakConfiguration.TENANT1_PASSWORD;
import static org.wildfly.test.integration.elytron.oidc.client.KeycloakConfiguration.TENANT2_USER;
import static org.wildfly.test.integration.elytron.oidc.client.KeycloakConfiguration.TENANT2_PASSWORD;
import static org.wildfly.test.integration.elytron.oidc.client.KeycloakConfiguration.CHARLOTTE;
import static org.wildfly.test.integration.elytron.oidc.client.KeycloakConfiguration.CHARLOTTE_PASSWORD;
import static org.wildfly.test.integration.elytron.oidc.client.KeycloakConfiguration.DAN;
import static org.wildfly.test.integration.elytron.oidc.client.KeycloakConfiguration.DAN_PASSWORD;
import static org.wildfly.test.integration.elytron.oidc.client.OidcBaseTest.CLIENT_SECRET;
import static org.wildfly.test.integration.elytron.oidc.client.OidcBaseTest.CLIENT_HOST_NAME;
import static org.wildfly.test.integration.elytron.oidc.client.OidcBaseTest.CLIENT_PORT;
import static org.wildfly.test.integration.elytron.oidc.client.OidcBaseTest.KEYCLOAK_CONTAINER;

import java.io.FilePermission;
import java.lang.reflect.ReflectPermission;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.PropertyPermission;

import org.apache.http.HttpResponse;
import org.apache.http.client.CookieStore;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.utils.HttpClientUtils;
import org.apache.http.impl.client.BasicCookieStore;
import org.apache.http.impl.client.BasicResponseHandler;
import org.apache.http.impl.client.LaxRedirectStrategy;
import org.apache.http.protocol.BasicHttpContext;
import org.apache.http.protocol.HttpContext;
import org.jboss.arquillian.container.test.api.Deployer;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.junit.InSequence;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.as.arquillian.api.ServerSetup;
import org.jboss.as.arquillian.container.ManagementClient;
import org.jboss.as.controller.client.ModelControllerClient;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.test.http.util.TestHttpClientUtils;
import org.jboss.as.test.integration.security.common.Utils;
import org.jboss.as.test.shared.TestSuiteEnvironment;
import org.jboss.dmr.ModelNode;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.wildfly.test.integration.elytron.oidc.client.KeycloakConfiguration;
import org.wildfly.test.integration.elytron.oidc.client.OidcBaseTest;

import io.restassured.RestAssured;

/*****************************************************************************************************************************************
 * Tests for multi-tenancy.
 *
 * The tests below involve two tenants:
 * Tenant1: <APP>/tenant1
 * Tenant2: <APP>/tenant2
 *
 * Tenant1 is secured using the tenant1 Keycloak Realm which contains the following users:
 * tenant1_user
 * charlie
 * dan
 *
 * Tenant2 is secured using the tenant2 Keycloak Realm which contains the following users:
 * tenant2_user
 * charlie
 * dan
 *
 * The first set of tests will make use of Keycloak-specific OIDC configuration.
 * The second set of tests will make use of a provider-url in the OIDC configuration.
 *
 * @author <a href="mailto:fjuma@redhat.com">Farah Juma</a>
 *****************************************************************************************************************************************/
@RunWith(Arquillian.class)
@RunAsClient
@ServerSetup({ OidcWithMultiTenancyTest.KeycloakAndSystemPropertySetup.class })
public class OidcWithMultiTenancyTest {

    public static final String MULTI_TENANCY_AUTH_SERVER_URL_APP = "AuthServerUrlMultiTenancyApp";
    public static final String MULTI_TENANCY_PROVIDER_URL_APP = "ProviderUrlMultiTenancyApp";

    /** The String returned in the HTTP response body. */
    public static final String RESPONSE_BODY = "GOOD";

    private static final String OIDC_AUTH_SERVER_URL = "oidc.auth.server.url";
    private static final String OIDC_PROVIDER_URL_TENANT1 = "oidc.provider.url.tenant1";
    private static final String OIDC_PROVIDER_URL_TENANT2 = "oidc.provider.url.tenant2";

    private static final String MULTI_TENANCY_WITH_PROVIDER_URL_TENANT_1 = "ProviderUrlMultiTenancyApp-tenant1.json";
    private static final String MULTI_TENANCY_WITH_PROVIDER_URL_TENANT_2 = "ProviderUrlMultiTenancyApp-tenant2.json";
    private static final String MULTI_TENANCY_WITH_AUTH_SERVER_URL_TENANT_1 = "AuthServerUrlMultiTenancyApp-tenant1.json";
    private static final String MULTI_TENANCY_WITH_AUTH_SERVER_URL_TENANT_2 = "AuthServerUrlMultiTenancyApp-tenant2.json";
    private static final String MULTI_TENANCY_PROVIDER_URL_WEB_XML_FILE = "multi-tenancy-provider-url-web.xml";
    private static final String MULTI_TENANCY_AUTH_SERVER_URL_WEB_XML_FILE = "multi-tenancy-auth-server-url-web.xml";

    // setting a high number for the token lifespan so we can test that a valid token from tenant1 can't be used for tenant2
    private static final int ACCESS_TOKEN_LIFESPAN = 120;
    private static final int SESSION_MAX_LIFESPAN = 120;

    private static final Map<String, KeycloakConfiguration.ClientAppType> APP_NAMES = new HashMap<>();
    static {
        APP_NAMES.put(MULTI_TENANCY_PROVIDER_URL_APP, KeycloakConfiguration.ClientAppType.OIDC_CLIENT);
        APP_NAMES.put(MULTI_TENANCY_AUTH_SERVER_URL_APP, KeycloakConfiguration.ClientAppType.OIDC_CLIENT);
    }

    @ArquillianResource
    protected static Deployer deployer;

    @BeforeClass
    public static void checkDockerAvailability() {
        assumeTrue("Docker isn't available, OIDC tests will be skipped", isDockerAvailable());
    }

    @Deployment(name = MULTI_TENANCY_PROVIDER_URL_APP, managed = false, testable = false)
    public static WebArchive createMultiTenancyProviderUrlDeployment() {
        return ShrinkWrap.create(WebArchive.class, MULTI_TENANCY_PROVIDER_URL_APP + ".war")
                .addClasses(Tenant1Servlet.class)
                .addClasses(Tenant2Servlet.class)
                .addClasses(MultiTenantResolverProviderUrl.class)
                .addAsWebInfResource(OidcWithMultiTenancyTest.class.getPackage(), MULTI_TENANCY_PROVIDER_URL_WEB_XML_FILE, "web.xml")
                .addAsResource(OidcWithMultiTenancyTest.class.getPackage(), MULTI_TENANCY_WITH_PROVIDER_URL_TENANT_1)
                .addAsResource(OidcWithMultiTenancyTest.class.getPackage(), MULTI_TENANCY_WITH_PROVIDER_URL_TENANT_2)
                .addAsManifestResource(createPermissionsXmlAsset(
                                // needed for methods called by OidcClientConfigurationBuilder#loadOidcJsonConfiguration
                                new RuntimePermission("accessDeclaredMembers"),
                                new RuntimePermission("getClassLoader"),
                                new ReflectPermission("suppressAccessChecks"),
                                new PropertyPermission("*", "read, write"),
                                new FilePermission("<<ALL FILES>>", "read")
                        ),
                        "permissions.xml");
    }

    @Deployment(name = MULTI_TENANCY_AUTH_SERVER_URL_APP, managed = false, testable = false)
    public static WebArchive createMultiTenancyAuthServerUrlDeployment() {
        return ShrinkWrap.create(WebArchive.class, MULTI_TENANCY_AUTH_SERVER_URL_APP + ".war")
                .addClasses(Tenant1Servlet.class)
                .addClasses(Tenant2Servlet.class)
                .addClasses(MultiTenantResolverAuthServerUrl.class)
                .addAsWebInfResource(OidcWithMultiTenancyTest.class.getPackage(), MULTI_TENANCY_AUTH_SERVER_URL_WEB_XML_FILE, "web.xml")
                .addAsResource(OidcWithMultiTenancyTest.class.getPackage(), MULTI_TENANCY_WITH_AUTH_SERVER_URL_TENANT_1)
                .addAsResource(OidcWithMultiTenancyTest.class.getPackage(), MULTI_TENANCY_WITH_AUTH_SERVER_URL_TENANT_2)
                .addAsManifestResource(createPermissionsXmlAsset(
                                // needed for methods called by OidcClientConfigurationBuilder#loadOidcJsonConfiguration
                                new RuntimePermission("accessDeclaredMembers"),
                                new RuntimePermission("getClassLoader"),
                                new ReflectPermission("suppressAccessChecks"),
                                new PropertyPermission("*", "read, write"),
                                new FilePermission("<<ALL FILES>>", "read")
                        ),
                        "permissions.xml");
    }

    /**********************************************************
     * 1. Tests using Keycloak-specific OIDC configuration
     **********************************************************/

    /**
     * Test that logging into each tenant with a non-existing user fails.
     */
    @Test
    @InSequence(1)
    public void testNonExistingUserWithAuthServerUrl() throws Exception {
        deployer.deploy(MULTI_TENANCY_AUTH_SERVER_URL_APP);
        testNonExistingUserWithAuthServerUrl(TENANT2_USER, TENANT2_PASSWORD, TENANT1_ENDPOINT);
        testNonExistingUserWithAuthServerUrl(TENANT1_USER, TENANT1_PASSWORD, TENANT2_ENDPOINT);
        testNonExistingUserWithAuthServerUrl(KeycloakConfiguration.ALICE, KeycloakConfiguration.ALICE_PASSWORD, TENANT1_ENDPOINT);
    }

    /**
     * Test successfully logging into /tenant1 with the tenant1_user and successfully logging into /tenant2 with the tenant2_user.
     */
    @Test
    @InSequence(2)
    public void testSuccessfulAuthenticationWithAuthServerUrl() throws Exception {
        performTenantRequestWithAuthServerUrl(TENANT1_USER, TENANT1_PASSWORD, TENANT1_ENDPOINT, null, null);
        performTenantRequestWithAuthServerUrl(TENANT2_USER, TENANT2_PASSWORD, TENANT2_ENDPOINT, null, null);
    }

    /**
     * Test successfully logging into /tenant1 with the tenant1_user and then attempt to access /tenant1 again.
     * We should be able to access /tenant1 again without needing to log in again.
     *
     * Then test successfully logging into /tenant2 with the tenant2_user and then attempt to access /tenant2 again.
     * We should be able to access /tenant2 again without needing to log in again.
     */
    @Test
    @InSequence(3)
    public void testLoggedInUserWithAuthServerUrl() throws Exception {
        performTenantRequestWithAuthServerUrl(TENANT1_USER, TENANT1_PASSWORD, TENANT1_ENDPOINT, TENANT1_ENDPOINT, getClientPageTextForTenant(TENANT1_ENDPOINT));
        performTenantRequestWithAuthServerUrl(TENANT2_USER, TENANT2_PASSWORD, TENANT2_ENDPOINT, TENANT2_ENDPOINT, getClientPageTextForTenant(TENANT2_ENDPOINT));
    }

    /**
     * Test logging into /tenant1 with the tenant1_user and then attempt to access /tenant2.
     * We should be redirected to Keycloak to log in since the user's cached token isn't valid for
     * /tenant2. Attempting to log into /tenant2 with the tenant1_user should fail.
     *
     * Then test logging into /tenant2 with the tenant2_user and then attempt to access /tenant1.
     * We should be redirected to Keycloak to log in since the user's cached token isn't valid for
     * /tenant1. Attempting to log into /tenant1 with the tenant2_user should fail.
     */
    @Test
    @InSequence(4)
    public void testUnauthorizedAccessWithAuthServerUrl() throws Exception {
        performTenantRequestWithAuthServerUrl(TENANT1_USER, TENANT1_PASSWORD, TENANT1_ENDPOINT, TENANT2_ENDPOINT, "Invalid username or password");
        performTenantRequestWithAuthServerUrl(TENANT2_USER, TENANT2_PASSWORD, TENANT2_ENDPOINT, TENANT1_ENDPOINT, "Invalid username or password");
    }

    /**
     * Test logging into /tenant1 with a username that exists in both tenant realms and then attempt to access /tenant2.
     * We should be redirected to Keycloak to log in since the user's cached token isn't valid for /tenant2. Attempting
     * to log into /tenant2 with the same user should succeed.
     *
     * Test logging into /tenant2 with a username that exists in both tenant realms and then attempt to access /tenant1.
     * We should be redirected to Keycloak to log in since the user's cached token isn't valid for /tenant1. Attempting
     * to log into /tenant1 with the same user should succeed.
     */
    @Test
    @InSequence(5)
    public void testUnauthorizedAccessWithAuthServerUrlValidUser() throws Exception {
        try {
            performTenantRequestWithAuthServerUrl(CHARLOTTE, CHARLOTTE_PASSWORD, TENANT1_ENDPOINT, TENANT2_ENDPOINT, getClientPageTextForTenant(TENANT2_ENDPOINT));
            performTenantRequestWithAuthServerUrl(DAN, DAN_PASSWORD, TENANT2_ENDPOINT, TENANT1_ENDPOINT, getClientPageTextForTenant(TENANT1_ENDPOINT));
        } finally {
            deployer.undeploy(MULTI_TENANCY_AUTH_SERVER_URL_APP);
        }
    }

    /**********************************************************
     * 2. Tests using a provider-url in the OIDC configuration
     **********************************************************/

    /**
     * Test that logging into each tenant with a non-existing user fails.
     */
    @Test
    @InSequence(6)
    public void testNonExistingUserWithProviderUrl() throws Exception {
        deployer.deploy(MULTI_TENANCY_PROVIDER_URL_APP);
        testNonExistingUserWithProviderUrl(TENANT2_USER, TENANT2_PASSWORD, TENANT1_ENDPOINT);
        testNonExistingUserWithProviderUrl(TENANT1_USER, TENANT1_PASSWORD, TENANT2_ENDPOINT);
        testNonExistingUserWithProviderUrl(KeycloakConfiguration.ALICE, KeycloakConfiguration.ALICE_PASSWORD, TENANT1_ENDPOINT);
    }

    /**
     * Test successfully logging into /tenant1 with the tenant1_user and successfully logging into /tenant2 with the tenant2_user.
     */
    @Test
    @InSequence(7)
    public void testSuccessfulAuthenticationWithProviderUrl() throws Exception {
        performTenantRequestWithProviderUrl(TENANT1_USER, TENANT1_PASSWORD, TENANT1_ENDPOINT, null, null);
        performTenantRequestWithProviderUrl(TENANT2_USER, TENANT2_PASSWORD, TENANT2_ENDPOINT, null, null);
    }

    /**
     * Test successfully logging into /tenant1 with the tenant1_user and then attempt to access /tenant1 again.
     * We should be able to access /tenant1 again without needing to log in again.
     *
     * Then test successfully logging into /tenant2 with the tenant2_user and then attempt to access /tenant2 again.
     * We should be able to access /tenant2 again without needing to log in again.
     */
    @Test
    @InSequence(8)
    public void testLoggedInUserWithProviderUrl() throws Exception {
        performTenantRequestWithProviderUrl(TENANT1_USER, TENANT1_PASSWORD, TENANT1_ENDPOINT, TENANT1_ENDPOINT, getClientPageTextForTenant(TENANT1_ENDPOINT));
        performTenantRequestWithProviderUrl(TENANT2_USER, TENANT2_PASSWORD, TENANT2_ENDPOINT, TENANT2_ENDPOINT, getClientPageTextForTenant(TENANT2_ENDPOINT));
    }

    /**
     * Test logging into /tenant1 with the tenant1_user and then attempt to access /tenant2.
     * We should be redirected to Keycloak to log in since the user's cached token isn't valid for
     * /tenant2. Attempting to log into /tenant2 with the tenant1_user should fail.
     *
     * Then test logging into /tenant2 with the tenant2_user and then attempt to access /tenant1.
     * We should be redirected to Keycloak to log in since the user's cached token isn't valid for
     * /tenant1. Attempting to log into /tenant1 with the tenant2_user should fail.
     */
    @Test
    @InSequence(9)
    public void testUnauthorizedAccessWithProviderUrl() throws Exception {
        performTenantRequestWithProviderUrl(TENANT1_USER, TENANT1_PASSWORD, TENANT1_ENDPOINT, TENANT2_ENDPOINT, "Invalid username or password");
        performTenantRequestWithProviderUrl(TENANT2_USER, TENANT2_PASSWORD, TENANT2_ENDPOINT, TENANT1_ENDPOINT, "Invalid username or password");
    }

    /**
     * Test logging into /tenant1 with a username that exists in both tenant realms and then attempt to access /tenant2.
     * We should be redirected to Keycloak to log in since the user's cached token isn't valid for /tenant2. Attempting
     * to log into /tenant2 with the same user should succeed.
     *
     * Test logging into /tenant2 with a username that exists in both tenant realms and then attempt to access /tenant1.
     * We should be redirected to Keycloak to log in since the user's cached token isn't valid for /tenant1. Attempting
     * to log into /tenant1 with the same user should succeed.
     */
    @Test
    @InSequence(10)
    public void testUnauthorizedAccessWithProviderUrlValidUser() throws Exception {
        try {
            performTenantRequestWithProviderUrl(CHARLOTTE, CHARLOTTE_PASSWORD, TENANT1_ENDPOINT, TENANT2_ENDPOINT, getClientPageTextForTenant(TENANT2_ENDPOINT));
            performTenantRequestWithProviderUrl(DAN, DAN_PASSWORD, TENANT2_ENDPOINT, TENANT1_ENDPOINT, getClientPageTextForTenant(TENANT1_ENDPOINT));
        } finally {
            deployer.undeploy(MULTI_TENANCY_PROVIDER_URL_APP);
        }
    }

    private void testNonExistingUserWithAuthServerUrl(String username, String password, String tenant) throws Exception {
        testNonExistingUser(username, password, tenant, true);
    }

    private void testNonExistingUserWithProviderUrl(String username, String password, String tenant) throws Exception {
        testNonExistingUser(username, password, tenant, false);
    }

    private void testNonExistingUser(String username, String password, String tenant, boolean useAuthServerUrl) throws Exception {
        loginToTenantNonExistingUser(username, password, tenant, useAuthServerUrl);
    }

    private static void loginToTenantNonExistingUser(String username, String password, String tenant, boolean useAuthServerUrl) throws Exception {
        OidcBaseTest.loginToApp(username, password, HttpURLConnection.HTTP_OK, "Invalid username or password", true,
                new URL("http", TestSuiteEnvironment.getHttpAddress(), TestSuiteEnvironment.getHttpPort(),
                        "/" + getAppUrlForTenant(useAuthServerUrl, tenant)).toURI());
    }

    private void performTenantRequestWithAuthServerUrl(String username, String password, String tenant, String otherTenant, String expectedTextForOtherTenant) throws Exception {
        performTenantRequest(username, password, tenant, otherTenant, expectedTextForOtherTenant, true);
    }

    private void performTenantRequestWithProviderUrl(String username, String password, String tenant, String otherTenant, String expectedTextForOtherTenant) throws Exception {
        performTenantRequest(username, password, tenant, otherTenant, expectedTextForOtherTenant, false);
    }

    private void performTenantRequest(String username, String password, String tenant, String otherTenant, String expectedTextForOtherTenant, boolean useAuthServerUrl) throws Exception {
        loginToAppMultiTenancy(username, password, tenant, otherTenant, expectedTextForOtherTenant, useAuthServerUrl);
    }

    private static String getAppUrlForTenant(boolean useAuthServerUrl, String tenant) {
        String appName = useAuthServerUrl ? MULTI_TENANCY_AUTH_SERVER_URL_APP : MULTI_TENANCY_PROVIDER_URL_APP;
        return appName + tenant;
    }

    private static String getClientPageTextForTenant(String tenant) {
        return tenant + ":" + RESPONSE_BODY;
    }

    private static void loginToAppMultiTenancy(String username, String password, String tenant, String otherTenant, String expectedTextForOtherTenant, boolean useAuthServerUrl) throws Exception {
        int expectedStatusCode = HttpURLConnection.HTTP_OK;
        String expectedTextForTenant = getClientPageTextForTenant(tenant);
        URI requestUri = new URL("http", TestSuiteEnvironment.getHttpAddress(), TestSuiteEnvironment.getHttpPort(), "/" + getAppUrlForTenant(useAuthServerUrl, tenant)).toURI();

        CookieStore store = new BasicCookieStore();
        HttpClient httpClient = TestHttpClientUtils.promiscuousCookieHttpClientBuilder()
                .setDefaultCookieStore(store)
                .setRedirectStrategy(new LaxRedirectStrategy())
                .build();
        HttpGet getMethod = new HttpGet(requestUri);
        HttpContext context = new BasicHttpContext();

        // attempt to access the specified tenant, we should be redirected to Keycloak to log in
        HttpResponse response = httpClient.execute(getMethod, context);
        try {
            // log into Keycloak, we should be redirected back to the tenant upon successful authentication
            loginToKeycloakAndVerifyExpectedText(response, requestUri, httpClient, username, password, expectedTextForTenant);

            if (otherTenant != null) {
                URI otherTenantRequestUri = new URL("http", TestSuiteEnvironment.getHttpAddress(), TestSuiteEnvironment.getHttpPort(), "/" + getAppUrlForTenant(useAuthServerUrl, otherTenant)).toURI();

                // attempt to access the other tenant
                HttpGet otherTenantGetMethod = new HttpGet(otherTenantRequestUri);
                HttpResponse otherTenantResponse = httpClient.execute(otherTenantGetMethod, context);

                if (tenant.equals(otherTenant)) {
                    // accessing the same tenant as above, already logged in
                    String otherTenantResponseString = new BasicResponseHandler().handleResponse(otherTenantResponse);
                    assertEquals(expectedStatusCode, otherTenantResponse.getStatusLine().getStatusCode());
                    assertTrue("Unexpected result " + otherTenantResponseString, otherTenantResponseString.contains(expectedTextForOtherTenant));
                } else {
                    // accessing a different tenant, we should be redirected to Keycloak to log in
                    loginToKeycloakAndVerifyExpectedText(otherTenantResponse, otherTenantRequestUri, httpClient, username, password, expectedTextForOtherTenant);
                }
            }
        } finally {
            HttpClientUtils.closeQuietly(response);
        }
    }

    private static void loginToKeycloakAndVerifyExpectedText(HttpResponse response, URI requestUri, HttpClient httpClient,
                                                             String username, String password, String expectedText) throws Exception {
        int statusCode = response.getStatusLine().getStatusCode();
        assertTrue("Expected code == OK but got " + statusCode + " for request=" + requestUri, statusCode == HttpURLConnection.HTTP_OK);

        // log into Keycloak, we should be redirected back to the tenant upon successful authentication
        OidcBaseTest.Form keycloakLoginForm = new OidcBaseTest.Form(response);
        HttpResponse afterLoginClickResponse = OidcBaseTest.simulateClickingOnButton(httpClient, keycloakLoginForm, username, password, "Sign In");
        afterLoginClickResponse.getEntity().getContent();
        assertEquals(HttpURLConnection.HTTP_OK, afterLoginClickResponse.getStatusLine().getStatusCode());
        String responseString = new BasicResponseHandler().handleResponse(afterLoginClickResponse);
        assertTrue("Unexpected result " + responseString, responseString.contains(expectedText));
    }

    static class KeycloakAndSystemPropertySetup extends OidcBaseTest.KeycloakSetup {

        @Override
        public void setup(ManagementClient managementClient, String containerId) throws Exception {
            super.setup(managementClient, containerId);
            OidcBaseTest.sendRealmCreationRequest(getRealmRepresentation(TENANT1_REALM, CLIENT_SECRET, CLIENT_HOST_NAME, CLIENT_PORT, APP_NAMES, ACCESS_TOKEN_LIFESPAN, SESSION_MAX_LIFESPAN, true));
            OidcBaseTest.sendRealmCreationRequest(getRealmRepresentation(TENANT2_REALM, CLIENT_SECRET, CLIENT_HOST_NAME, CLIENT_PORT, APP_NAMES, ACCESS_TOKEN_LIFESPAN, SESSION_MAX_LIFESPAN, true));

            ModelControllerClient client = managementClient.getControllerClient();

            ModelNode operation = createOpNode("system-property=" + OIDC_AUTH_SERVER_URL, ModelDescriptionConstants.ADD);
            operation.get("value").set(KEYCLOAK_CONTAINER.getAuthServerUrl());
            Utils.applyUpdate(operation, client);

            operation = createOpNode("system-property=" + OIDC_PROVIDER_URL_TENANT1, ModelDescriptionConstants.ADD);
            operation.get("value").set(KEYCLOAK_CONTAINER.getAuthServerUrl() + "/realms/" + TENANT1_REALM);
            Utils.applyUpdate(operation, client);

            operation = createOpNode("system-property=" + OIDC_PROVIDER_URL_TENANT2, ModelDescriptionConstants.ADD);
            operation.get("value").set(KEYCLOAK_CONTAINER.getAuthServerUrl() + "/realms/" + TENANT2_REALM);
            Utils.applyUpdate(operation, client);
        }

        @Override
        public void tearDown(ManagementClient managementClient, String containerId) throws Exception {
            RestAssured
                    .given()
                    .auth().oauth2(KeycloakConfiguration.getAdminAccessToken(KEYCLOAK_CONTAINER.getAuthServerUrl()))
                    .when()
                    .delete(KEYCLOAK_CONTAINER.getAuthServerUrl() + "/admin/realms/" + TENANT1_REALM).then().statusCode(204);

            RestAssured
                    .given()
                    .auth().oauth2(KeycloakConfiguration.getAdminAccessToken(KEYCLOAK_CONTAINER.getAuthServerUrl()))
                    .when()
                    .delete(KEYCLOAK_CONTAINER.getAuthServerUrl() + "/admin/realms/" + TENANT2_REALM).then().statusCode(204);

            super.tearDown(managementClient, containerId);
            ModelControllerClient client = managementClient.getControllerClient();

            ModelNode operation = createOpNode("system-property=" + OIDC_AUTH_SERVER_URL, ModelDescriptionConstants.REMOVE);
            Utils.applyUpdate(operation, client);

            operation = createOpNode("system-property=" + OIDC_PROVIDER_URL_TENANT1, ModelDescriptionConstants.REMOVE);
            Utils.applyUpdate(operation, client);

            operation = createOpNode("system-property=" + OIDC_PROVIDER_URL_TENANT2, ModelDescriptionConstants.REMOVE);
            Utils.applyUpdate(operation, client);
        }
    }
}
