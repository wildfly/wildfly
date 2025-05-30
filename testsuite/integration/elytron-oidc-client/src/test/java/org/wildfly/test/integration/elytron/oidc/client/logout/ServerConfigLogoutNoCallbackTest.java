/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.test.integration.elytron.oidc.client.logout;

import static org.junit.Assume.assumeTrue;
import static org.wildfly.test.integration.elytron.oidc.client.logout.Constants.BACK_CHANNEL_LOGOUT_APP;
import static org.wildfly.test.integration.elytron.oidc.client.logout.Constants.BACK_CHANNEL_LOGOUT_APP_TWO;
import static org.wildfly.test.integration.elytron.oidc.client.logout.Constants.FRONT_CHANNEL_LOGOUT_APP;
import static org.wildfly.test.integration.elytron.oidc.client.logout.Constants.NO_CALLBACK;
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

import com.gargoylesoftware.htmlunit.BrowserVersion;
import com.gargoylesoftware.htmlunit.WebClient;

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

import org.wildfly.test.integration.elytron.oidc.client.KeycloakConfiguration;
import org.wildfly.test.stabilitylevel.StabilityServerSetupSnapshotRestoreTasks;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.Ignore;

/*  Test OIDC logout.  Logout configuration attributes
    are passed to Elytron via oidc.json file attributes.
 */
@RunWith(Arquillian.class)
@RunAsClient
@ServerSetup({ ServerConfigLogoutNoCallbackTest.PreviewStabilitySetupTask.class,
        EnvSetupUtils.KeycloakAndSubsystemSetup.class,
        EnvSetupUtils.WildFlyServerSetupTask.class})
public class ServerConfigLogoutNoCallbackTest extends LoginLogoutBasics {

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

    public ServerConfigLogoutNoCallbackTest() {
        super(Stability.PREVIEW);
    }

    //-------------- test configuration data ---------------

    // These are the oidc logout URL paths that are registered with Keycloak.
    // The path of the URL must be the same as the system properties registered above.
    private static Map<String, LoginLogoutBasics.LogoutChannelPaths> APP_LOGOUT;
    static {
        APP_LOGOUT= new HashMap<String, LoginLogoutBasics.LogoutChannelPaths>();
        APP_LOGOUT.put(RP_INITIATED_LOGOUT_APP, new LoginLogoutBasics.LogoutChannelPaths(
                null,null, List.of(NO_CALLBACK)) );
        APP_LOGOUT.put(BACK_CHANNEL_LOGOUT_APP, new LoginLogoutBasics.LogoutChannelPaths(
                NO_CALLBACK,null, null) );
        APP_LOGOUT.put(BACK_CHANNEL_LOGOUT_APP_TWO, new LoginLogoutBasics.LogoutChannelPaths(
                NO_CALLBACK,null, null) );
        APP_LOGOUT.put(FRONT_CHANNEL_LOGOUT_APP, new LoginLogoutBasics.LogoutChannelPaths(
                null,NO_CALLBACK, null) );
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

    private static final Package packageName = ServerConfigLogoutNoCallbackTest.class.getPackage();

    @Deployment(name = RP_INITIATED_LOGOUT_APP, managed = false, testable = false)
    public static WebArchive createRpInitiatedAuthServerUrlDeployment() {
        return ShrinkWrap.create(WebArchive.class, RP_INITIATED_LOGOUT_APP + ".war")
                .addClasses(SimpleServlet.class)
                .addClasses(SimpleSecuredServlet.class)
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
                .addClasses(SecuredFrontChannelServlet.class)
                .addAsWebInfResource(packageName, WEB_XML, "web.xml")
                .addAsWebInfResource(packageName,
                        FRONT_CHANNEL_LOGOUT_APP+"-oidc.json", "oidc.json")
                ;
    }
    @Ignore // todo re-enable test when keycloak is returning logout token
    @Test
    //  Test checks that RPInitiated Logout can be completed
    //  via a GET to the OP.
    public void testRpInitiatedLogout() throws Exception {
        try {
            deployer.deploy(RP_INITIATED_LOGOUT_APP);

            loginToApp(RP_INITIATED_LOGOUT_APP);
            assertUserLoggedIn(RP_INITIATED_LOGOUT_APP, SimpleServlet.RESPONSE_BODY);
            logoutOfKeycloak(RP_INITIATED_LOGOUT_APP, YOU_ARE_LOGGED_OUT);
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
            assertUserLoggedOut(BACK_CHANNEL_LOGOUT_APP, SIGN_IN_TO_YOUR_ACCOUNT);

        } finally {
            deployer.undeploy(BACK_CHANNEL_LOGOUT_APP);
        }
    }

    @Test
    //  Test checks that front channel Logout can be completed.
    public void testFrontChannelLogout() throws Exception {
        try (WebClient webClient = new WebClient(BrowserVersion.CHROME)) {
            deployer.deploy(FRONT_CHANNEL_LOGOUT_APP);

            browserLoginToApp(webClient, FRONT_CHANNEL_LOGOUT_APP);
            browserAssertUserLoggedIn(webClient, FRONT_CHANNEL_LOGOUT_APP,
                    SecuredFrontChannelServlet.SERVLET_PATH);

            // logout
            browserLogoutOfKeycloak(webClient, FRONT_CHANNEL_LOGOUT_APP);
            browserAssertUserLoggedOut(webClient, FRONT_CHANNEL_LOGOUT_APP,
                    SIGN_IN_TO_YOUR_ACCOUNT);

            webClient.close();
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
            loginToApp(BACK_CHANNEL_LOGOUT_APP_TWO);

            assertUserLoggedIn(BACK_CHANNEL_LOGOUT_APP, "GOOD");
            assertUserLoggedIn(BACK_CHANNEL_LOGOUT_APP_TWO, "GOOD");

            logoutOfKeycloak(BACK_CHANNEL_LOGOUT_APP, YOU_ARE_LOGGED_OUT);
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
            LoginLogoutBasics.addSystemProperty(managementClient, ServerConfigLogoutNoCallbackTest.class);
        }
    }
}
