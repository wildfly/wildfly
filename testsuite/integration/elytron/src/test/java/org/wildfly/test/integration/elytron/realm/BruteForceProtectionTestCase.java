/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.test.integration.elytron.realm;

import static jakarta.servlet.http.HttpServletResponse.SC_OK;
import static jakarta.servlet.http.HttpServletResponse.SC_UNAUTHORIZED;

import java.net.MalformedURLException;
import java.net.URL;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.as.arquillian.api.ServerSetup;
import org.jboss.as.arquillian.api.ServerSetupTask;
import org.jboss.as.arquillian.container.ManagementClient;
import org.jboss.as.test.integration.management.util.CLIWrapper;
import org.jboss.as.test.integration.security.common.Utils;
import org.jboss.as.test.integration.security.common.servlets.SimpleSecuredServlet;
import org.jboss.as.test.integration.security.common.servlets.SimpleServlet;
import org.jboss.as.test.shared.ServerReload;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.wildfly.test.stabilitylevel.StabilityServerSetupSnapshotRestoreTasks;

/**
 * Integration test that verifies the {@code brute-force-protection} management model attribute is
 * applied to a security realm and takes effect at runtime.
 *
 * <p>The test configures a filesystem realm with {@code max-failed-attempts=3}. It then makes 3
 * failed authentication attempts against one user account, confirms that account is subsequently
 * locked out (further attempts are rejected even with the correct password), and confirms that a
 * second account on the same realm is unaffected. This proves the model-level configuration is
 * honoured rather than the library default of 10 failed attempts.
 *
 * <p>No waits or sleeps are required because the test does not exercise lockout expiry.
 *
 * <p>The {@code brute-force-protection} attribute requires community stability, so the first setup
 * task reloads the server to that level (and skips the test entirely if community is not a
 * supported stability level for the running server).
 */
@RunWith(Arquillian.class)
@RunAsClient
@ServerSetup({
    StabilityServerSetupSnapshotRestoreTasks.Community.class,
    BruteForceProtectionTestCase.SetUpTask.class
})
public class BruteForceProtectionTestCase {

    private static final String DEPLOYMENT = "bruteForceProtection";

    /** Account that will be subjected to repeated failed attempts. */
    private static final String LOCKED_USER   = "lockedUser";
    /** Account that must remain accessible throughout the test. */
    private static final String INNOCENT_USER = "innocentUser";
    private static final String CORRECT_PASSWORD = "password1!";
    private static final String WRONG_PASSWORD   = "wrongPassword";

    /**
     * Number of failed attempts configured in the model. Must be lower than the library default
     * (10) so that a failure here proves the model attribute was applied, not the default.
     */
    private static final int MAX_FAILED_ATTEMPTS = 3;

    @Deployment(name = DEPLOYMENT)
    public static WebArchive createDeployment() {
        return ShrinkWrap.create(WebArchive.class, DEPLOYMENT + ".war")
                .addClasses(SimpleServlet.class, SimpleSecuredServlet.class)
                .addAsWebInfResource(BruteForceProtectionTestCase.class.getPackage(),
                        "filesystem-realm-web.xml", "web.xml")
                .addAsWebInfResource(Utils.getJBossWebXmlAsset(DEPLOYMENT), "jboss-web.xml");
    }

    /**
     * Verifies that the {@code brute-force-protection} model attribute is applied at runtime.
     *
     * <p>After {@code MAX_FAILED_ATTEMPTS} bad-password attempts the targeted account is locked
     * out: a subsequent attempt with the correct password is still rejected. A second account on
     * the same realm must remain accessible throughout, confirming that lockout is per-identity
     * and that the model-configured threshold (3) rather than the library default (10) was used.
     */
    @Test
    public void testBruteForceProtectionConfiguration(@ArquillianResource URL webAppURL) throws Exception {
        URL url = securedUrl(webAppURL);

        // Exhaust the configured failure allowance for lockedUser.
        for (int i = 0; i < MAX_FAILED_ATTEMPTS; i++) {
            Utils.makeCallWithBasicAuthn(url, LOCKED_USER, WRONG_PASSWORD, SC_UNAUTHORIZED);
        }

        // lockedUser is now locked out — correct password is rejected.
        Utils.makeCallWithBasicAuthn(url, LOCKED_USER, CORRECT_PASSWORD, SC_UNAUTHORIZED);

        // innocentUser on the same realm is unaffected and must still authenticate successfully.
        Utils.makeCallWithBasicAuthn(url, INNOCENT_USER, CORRECT_PASSWORD, SC_OK);
    }

    private URL securedUrl(URL base) throws MalformedURLException {
        return new URL(base.toExternalForm() + SimpleSecuredServlet.SERVLET_PATH.substring(1));
    }

    // -------------------------------------------------------------------------

    static class SetUpTask implements ServerSetupTask {

        private static final String REALM_NAME   = "bfpTestRealm";
        private static final String DOMAIN_NAME  = "bfpTestDomain";
        private static final String HTTP_FACTORY = "bfpTestHttpFactory";
        private static final String ROLE_DECODER = "bfp-roles";
        private static final String PREDEFINED_HTTP_SERVER_MECHANISM_FACTORY = "global";

        @Override
        public void setup(ManagementClient managementClient, String containerId) throws Exception {
            try (CLIWrapper cli = new CLIWrapper(true)) {
                // Create a filesystem realm and populate two user accounts
                cli.sendLine(String.format(
                        "/subsystem=elytron/filesystem-realm=%s:add(path=bfp-realm,relative-to=jboss.server.data.dir)",
                        REALM_NAME));
                addUser(cli, LOCKED_USER);
                addUser(cli, INNOCENT_USER);

                // Apply brute-force-protection with max-failed-attempts well below the library
                // default of 10, proving the model attribute is what drives the behaviour.
                cli.sendLine(String.format(
                        "/subsystem=elytron/filesystem-realm=%s:write-attribute(" +
                        "name=brute-force-protection," +
                        "value={enabled=true,max-failed-attempts=%d})",
                        REALM_NAME, MAX_FAILED_ATTEMPTS));

                cli.sendLine(String.format(
                        "/subsystem=elytron/simple-role-decoder=%s:add(attribute=Roles)",
                        ROLE_DECODER));
                cli.sendLine(String.format(
                        "/subsystem=elytron/security-domain=%1$s:add(" +
                        "realms=[{realm=%2$s,role-decoder=%3$s}]," +
                        "default-realm=%2$s," +
                        "permission-mapper=default-permission-mapper)",
                        DOMAIN_NAME, REALM_NAME, ROLE_DECODER));
                cli.sendLine(String.format(
                        "/subsystem=elytron/http-authentication-factory=%1$s:add(" +
                        "http-server-mechanism-factory=%2$s," +
                        "security-domain=%3$s," +
                        "mechanism-configurations=[{mechanism-name=BASIC," +
                        "mechanism-realm-configurations=[{realm-name=\"%1$s\"}]}])",
                        HTTP_FACTORY, PREDEFINED_HTTP_SERVER_MECHANISM_FACTORY, DOMAIN_NAME));
                cli.sendLine(String.format(
                        "/subsystem=undertow/application-security-domain=%s:add(http-authentication-factory=%s)",
                        DEPLOYMENT, HTTP_FACTORY));
            }
            ServerReload.reloadIfRequired(managementClient);
        }

        @Override
        public void tearDown(ManagementClient managementClient, String containerId) throws Exception {
            // Cleanup is handled by the snapshot restore in StabilityServerSetupSnapshotRestoreTasks.
        }

        private void addUser(CLIWrapper cli, String username) throws Exception {
            cli.sendLine(String.format(
                    "/subsystem=elytron/filesystem-realm=%s:add-identity(identity=%s)",
                    REALM_NAME, username));
            cli.sendLine(String.format(
                    "/subsystem=elytron/filesystem-realm=%s:set-password(identity=%s,clear={password=\"%s\"})",
                    REALM_NAME, username, CORRECT_PASSWORD));
            cli.sendLine(String.format(
                    "/subsystem=elytron/filesystem-realm=%s:add-identity-attribute(identity=%s,name=Roles,value=[JBossAdmin])",
                    REALM_NAME, username));
        }
    }
}
