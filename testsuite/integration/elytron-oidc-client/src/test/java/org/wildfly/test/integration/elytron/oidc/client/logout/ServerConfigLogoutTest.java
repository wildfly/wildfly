/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.test.integration.elytron.oidc.client.logout;

import static org.junit.Assume.assumeTrue;
import static org.wildfly.test.integration.elytron.oidc.client.logout.Constants.BACK_CHANNEL_LOGOUT_APP;
import static org.wildfly.test.integration.elytron.oidc.client.logout.Constants.FRONT_CHANNEL_LOGOUT_APP;
import static org.wildfly.test.integration.elytron.oidc.client.logout.Constants.RP_INITIATED_LOGOUT_APP;
import static org.wildfly.test.integration.elytron.oidc.client.logout.Constants.SIGN_IN_TO_YOUR_ACCOUNT;
import static org.wildfly.test.integration.elytron.oidc.client.logout.Constants.WEB_XML;
import static org.wildfly.test.integration.elytron.oidc.client.logout.Constants.YOU_ARE_LOGGED_OUT;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.http.client.CookieStore;
import org.apache.http.client.HttpClient;
import org.apache.http.impl.client.BasicCookieStore;
import org.apache.http.impl.client.LaxRedirectStrategy;

import org.jboss.arquillian.container.test.api.Deployer;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.as.arquillian.api.ServerSetup;
import org.jboss.as.arquillian.container.ManagementClient;
import org.jboss.as.test.integration.security.common.servlets.SimpleServlet;
import org.jboss.as.test.integration.security.common.servlets.SimpleSecuredServlet;
import org.jboss.as.test.http.util.TestHttpClientUtils;
import org.jboss.as.test.shared.util.AssumeTestGroupUtil;
import org.jboss.as.version.Stability;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;

import org.junit.Ignore;
import org.wildfly.test.integration.elytron.oidc.client.KeycloakConfiguration;
import org.wildfly.test.stabilitylevel.StabilityServerSetupSnapshotRestoreTasks;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

/*  Test OIDC logout.  Logout configuration attributes
    are passed to Elytron via oidc.json file attributes.
 */
@RunWith(Arquillian.class)
@RunAsClient
@ServerSetup({ ServerConfigLogoutTest.PreviewStabilitySetupTask.class,
        EnvSetupUtils.KeycloakAndSubsystemSetup.class,
        EnvSetupUtils.WildFlyServerSetupTask.class})
public class ServerConfigLogoutTest extends LoginLogoutBasics {

    @ArquillianResource
    protected static Deployer deployer;

    @BeforeClass
    public static void checkDockerAvailability() {
        assumeTrue("Docker isn't available, OIDC tests will be skipped", AssumeTestGroupUtil.isDockerAvailable());
    }

    @Before
    public void createHttpClient() {
        CookieStore store = new BasicCookieStore();
        HttpClient httpClient = TestHttpClientUtils.promiscuousCookieHttpClientBuilder()
                .setDefaultCookieStore(store)
                .setRedirectStrategy(new LaxRedirectStrategy())
                .build();
        super.setHttpClient(httpClient);
    }

    public ServerConfigLogoutTest() {
        super(Stability.PREVIEW);
    }

    //-------------- test configuration data ---------------
    private static final String POST_LOGOUT_PATH_VALUE = "http://"
            + EnvSetupUtils.CLIENT_HOST_NAME + ":"
            + EnvSetupUtils.CLIENT_PORT + "/" + RP_INITIATED_LOGOUT_APP
            + SimplePostLogoutServlet.POST_LOGOUT_PATH;

    private static final String BACK_CHANNEL_LOGOUT_URL = "http://"
            + EnvSetupUtils.HOST_TESTCONTAINERS_INTERNAL + ":"
            + EnvSetupUtils.CLIENT_PORT + "/" + BACK_CHANNEL_LOGOUT_APP
            + SimpleSecuredServlet.SERVLET_PATH + Constants.LOGOUT_CALLBACK_PATH_VALUE;

    private static final String BACK_CHANNEL_LOGOUT_APP_TWO = BACK_CHANNEL_LOGOUT_APP+"_TWO";
    private static final String BACK_CHANNEL_LOGOUT_URL_TWO = "http://"
            + EnvSetupUtils.HOST_TESTCONTAINERS_INTERNAL + ":"
            + EnvSetupUtils.CLIENT_PORT + "/" + BACK_CHANNEL_LOGOUT_APP_TWO
            + SimpleSecuredServlet.SERVLET_PATH + Constants.LOGOUT_CALLBACK_PATH_VALUE;

    private static final String FRONT_CHANNEL_LOGOUT_URL = "http://"
            + EnvSetupUtils.HOST_TESTCONTAINERS_INTERNAL + ":"
            + EnvSetupUtils.CLIENT_PORT + "/" + FRONT_CHANNEL_LOGOUT_APP
            + SimpleSecuredServlet.SERVLET_PATH + Constants.LOGOUT_CALLBACK_PATH_VALUE;

    // These are the oidc logout URL paths that are registered with Keycloak.
    // The path of the URL must be the same as the system properties registered above.
    private static Map<String, LoginLogoutBasics.LogoutChannelPaths> APP_LOGOUT;
    static {
        APP_LOGOUT= new HashMap<String, LoginLogoutBasics.LogoutChannelPaths>();
        APP_LOGOUT.put(RP_INITIATED_LOGOUT_APP, new LoginLogoutBasics.LogoutChannelPaths(
                null,null, List.of(POST_LOGOUT_PATH_VALUE)) );
        APP_LOGOUT.put(BACK_CHANNEL_LOGOUT_APP, new LoginLogoutBasics.LogoutChannelPaths(
                BACK_CHANNEL_LOGOUT_URL,null, null) );
        APP_LOGOUT.put(BACK_CHANNEL_LOGOUT_APP_TWO, new LoginLogoutBasics.LogoutChannelPaths(
                BACK_CHANNEL_LOGOUT_URL_TWO,null, null) );
        APP_LOGOUT.put(FRONT_CHANNEL_LOGOUT_APP, new LoginLogoutBasics.LogoutChannelPaths(
                null,FRONT_CHANNEL_LOGOUT_URL, null) );
        EnvSetupUtils.KeycloakAndSubsystemSetup.setLogoutUrlPaths(APP_LOGOUT);
        EnvSetupUtils.KeycloakAndSubsystemSetup.setOidcServerConfig(true);
    }

    // These are the application names registered as Keycloak clients.
    // The name corresponds to each WAR file declared and deployed in
    // OidcLogoutSystemPropertiesAppsSetUp
    private static Map<String, KeycloakConfiguration.ClientAppType> APP_NAMES;
    static {
        APP_NAMES = new HashMap<>();
        APP_NAMES.put(RP_INITIATED_LOGOUT_APP, KeycloakConfiguration.ClientAppType.OIDC_CLIENT);
        APP_NAMES.put(BACK_CHANNEL_LOGOUT_APP, KeycloakConfiguration.ClientAppType.OIDC_CLIENT);
        APP_NAMES.put(BACK_CHANNEL_LOGOUT_APP_TWO, KeycloakConfiguration.ClientAppType.OIDC_CLIENT);
        APP_NAMES.put(FRONT_CHANNEL_LOGOUT_APP, KeycloakConfiguration.ClientAppType.OIDC_CLIENT);
        EnvSetupUtils.KeycloakAndSubsystemSetup.setKeycloakClients(APP_NAMES);
    }

    //-------------- Test components ---------------------

    private static final Package packageName = ServerConfigLogoutTest.class.getPackage();

    @Deployment(name = RP_INITIATED_LOGOUT_APP, managed = false, testable = false)
    public static WebArchive createRpInitiatedAuthServerUrlDeployment() {
        return ShrinkWrap.create(WebArchive.class, RP_INITIATED_LOGOUT_APP + ".war")
                .addClasses(SimpleServlet.class)
                .addClasses(SimpleSecuredServlet.class)
                .addClasses(SimplePostLogoutServlet.class)
                .addAsWebInfResource(packageName, WEB_XML, "web.xml")
                ;
    }

    @Deployment(name = BACK_CHANNEL_LOGOUT_APP, managed = false, testable = false)
    public static WebArchive createBackChannelAuthServerUrlDeployment() {
        WebArchive war =  ShrinkWrap.create(WebArchive.class, BACK_CHANNEL_LOGOUT_APP + ".war")
                .addClasses(SimpleServlet.class)
                .addClasses(SimpleSecuredServlet.class)
                .addAsWebInfResource(packageName, WEB_XML, "web.xml")
                ;
        return war;
    }

    @Deployment(name = BACK_CHANNEL_LOGOUT_APP_TWO, managed = false, testable = false)
    public static WebArchive createBackChannelAuthServerUrlDeploymentTwo() {
        WebArchive war =  ShrinkWrap.create(WebArchive.class, BACK_CHANNEL_LOGOUT_APP_TWO + ".war")
                .addClasses(SimpleServlet.class)
                .addClasses(SimpleSecuredServlet.class)
                .addAsWebInfResource(packageName, WEB_XML, "web.xml")
                ;
        return war;
    }

    @Deployment(name = FRONT_CHANNEL_LOGOUT_APP, managed = false, testable = false)
    public static WebArchive createFrontChannelAuthServerUrlDeployment() {
        return ShrinkWrap.create(WebArchive.class, FRONT_CHANNEL_LOGOUT_APP + ".war")
                .addClasses(SimpleServlet.class)
                .addClasses(SimpleSecuredServlet.class)
                .addAsWebInfResource(packageName, WEB_XML, "web.xml")
                ;
    }

    @Test
    //  Test checks that RPInitiated Logout can be completed
    //  via a GET to the OP.
    public void testRpInitiatedLogout() throws Exception {
        try {
            deployer.deploy(RP_INITIATED_LOGOUT_APP);

            loginToApp(RP_INITIATED_LOGOUT_APP);
            assertUserLoggedIn(RP_INITIATED_LOGOUT_APP, SimpleServlet.RESPONSE_BODY);
            logoutOfKeycloak(RP_INITIATED_LOGOUT_APP, SimplePostLogoutServlet.RESPONSE_BODY);
            Thread.sleep(2000);  // slow server adjustment
            assertUserLoggedOut(RP_INITIATED_LOGOUT_APP, SIGN_IN_TO_YOUR_ACCOUNT);

        } finally {
            deployer.undeploy(RP_INITIATED_LOGOUT_APP);
        }
    }

    @Test
    //  Test checks that back channel Logout can be completed.
    public void testBackChannelLogout() throws Exception {
        try {
            deployer.deploy(BACK_CHANNEL_LOGOUT_APP);

            loginToApp(BACK_CHANNEL_LOGOUT_APP);
            assertUserLoggedIn(BACK_CHANNEL_LOGOUT_APP, SimpleServlet.RESPONSE_BODY);
            logoutOfKeycloak(BACK_CHANNEL_LOGOUT_APP, YOU_ARE_LOGGED_OUT);
            Thread.sleep(2000);  // slow server adjustment
            assertUserLoggedOut(BACK_CHANNEL_LOGOUT_APP, SIGN_IN_TO_YOUR_ACCOUNT);

        } finally {
            deployer.undeploy(BACK_CHANNEL_LOGOUT_APP);
        }
    }

    @Ignore // todo waiting on Selenium support for wfly
    @Test
    //  Test checks that front channel Logout can be completed.
    public void testFrontChannelLogout() throws Exception {
        try {
            deployer.deploy(FRONT_CHANNEL_LOGOUT_APP);

            loginToApp(FRONT_CHANNEL_LOGOUT_APP);
            assertUserLoggedIn(FRONT_CHANNEL_LOGOUT_APP, SimpleServlet.RESPONSE_BODY);
            logoutOfKeycloak(FRONT_CHANNEL_LOGOUT_APP, "You are logging out from following apps");
            assertUserLoggedOut(FRONT_CHANNEL_LOGOUT_APP, SimpleServlet.RESPONSE_BODY);

        } finally {
            deployer.undeploy(FRONT_CHANNEL_LOGOUT_APP);
        }
    }

    @Test
    // Test checks that back channel Logout can be completed
    // when user logged in to 2 apps
    public void testBackChannelLogoutTwo() throws Exception {

        try {
            deployer.deploy(BACK_CHANNEL_LOGOUT_APP_TWO);
            deployer.deploy(BACK_CHANNEL_LOGOUT_APP);
            loginToApp(BACK_CHANNEL_LOGOUT_APP);
            Thread.sleep(3000);  // slow server adjustment
            loginToApp(BACK_CHANNEL_LOGOUT_APP_TWO);

            assertUserLoggedIn(BACK_CHANNEL_LOGOUT_APP, "GOOD");
            assertUserLoggedIn(BACK_CHANNEL_LOGOUT_APP_TWO, "GOOD");

            Thread.sleep(1500);  // slow server adjustment
            logoutOfKeycloak(BACK_CHANNEL_LOGOUT_APP, YOU_ARE_LOGGED_OUT);
            Thread.sleep(1000);  // slow server adjustment
            assertUserLoggedOut(BACK_CHANNEL_LOGOUT_APP, SIGN_IN_TO_YOUR_ACCOUNT);
            assertUserLoggedOut(BACK_CHANNEL_LOGOUT_APP_TWO, SIGN_IN_TO_YOUR_ACCOUNT);

        } finally {
            deployer.undeploy(BACK_CHANNEL_LOGOUT_APP_TWO);
            deployer.undeploy(BACK_CHANNEL_LOGOUT_APP);
        }
    }


    //-------------- Server Setup -------------------------
    public static class PreviewStabilitySetupTask extends StabilityServerSetupSnapshotRestoreTasks.Preview {
        @Override
        protected void doSetup(ManagementClient managementClient) throws Exception {
            // Write a system property so the model gets stored with a lower stability level.
            // This is to make sure we can reload back to the higher level from the snapshot
            LoginLogoutBasics.addSystemProperty(managementClient, ServerConfigLogoutTest.class);
        }
    }
}
