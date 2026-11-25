/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.test.integration.elytron.realm;

import static jakarta.servlet.http.HttpServletResponse.SC_FORBIDDEN;
import static jakarta.servlet.http.HttpServletResponse.SC_OK;
import static jakarta.servlet.http.HttpServletResponse.SC_UNAUTHORIZED;
import static org.junit.Assert.assertEquals;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.UUID;
import org.apache.commons.io.FileUtils;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.as.arquillian.api.ContainerResource;
import org.jboss.as.arquillian.api.ServerSetup;
import org.jboss.as.arquillian.api.ServerSetupTask;
import org.jboss.as.arquillian.container.ManagementClient;
import org.jboss.as.test.integration.management.util.CLIWrapper;
import org.jboss.as.test.integration.security.common.Utils;
import org.jboss.as.test.shared.ServerReload;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Tests Elytron caching-realm backed by filesystem-realm.
 *
 * Given: application secured by caching-realm backed by filesystem-realm
 * and caching-realm cache has maximum size 2.
 *
 * @author Ondrej Kotek
 */
@RunWith(Arquillian.class)
@RunAsClient
@ServerSetup({CachingFilesystemRealmTestCase.SetUpTask.class})
public class CachingFilesystemRealmTestCase {

    @ContainerResource
    protected ManagementClient managementClient;

    private static final String NAME = CachingFilesystemRealmTestCase.class.getSimpleName();
    private static final String DEPLOYMENT_NAME = "cachingFilesystemRealm";
    private static final String INDEX_PAGE_CONTENT = "index page content";
    private static final String SECURITY_DOMAIN = "securityDomain-" + NAME;
    private static final String CACHING_REALM = "cachingFileSystemRealm-" + NAME;
    private static final String FILESYSTEM_REALM = "fileSystemRealm-" + NAME;
    private static final String FILESYSTEM_REALM2 = "fileSystemRealm2-" + NAME;

    private static final String USER_A = "user-a";
    private static final String USER_B = "user-b";
    private static final String USER_C = "user-c";
    private static final String PASSWORD_A = "passwordA";
    private static final String PASSWORD_B = "passwordB";
    private static final String PASSWORD_C = "passwordC";

    private static final String TARGET_FOLDER = System.getProperty("project.build.directory", "target");
    // Initialized in setup() with a unique directory per test run to avoid issues with locked files from previous runs on Windows OS
    private static String filesystemRealmPath;

    @Deployment(name = DEPLOYMENT_NAME)
    public static WebArchive createDeployment() {
        final WebArchive war = ShrinkWrap.create(WebArchive.class, DEPLOYMENT_NAME + ".war");
        war.addAsWebInfResource(CachingFilesystemRealmTestCase.class.getPackage(), "caching-filesystem-realm-web.xml", "web.xml");
        war.addAsWebInfResource(Utils.getJBossWebXmlAsset(SECURITY_DOMAIN), "jboss-web.xml");
        war.add(new StringAsset(INDEX_PAGE_CONTENT), "index.html");
        return war;
    }

    @Before
    public void resetEnvironment() {
        clearCache();
        changeUserPassword(USER_A, PASSWORD_A);
    }

    /**
     * Given: client has successfully authenticated to application using USER_A credentials,
     * and password for USER_A is changed to "anotherPassword" on the file system.
     * When client does a new request using original USER_A credentials,
     * then authentication verification for the request should be successfully done against the cached identity
     * (any changes of USER_A on file system should be ignored).
     *
     * @param webAppUrl URL to application protected by caching-realm
     */
    @Test
    public void testIdentityPasswordIsCached(@ArquillianResource URL webAppUrl) throws URISyntaxException, Exception {
        // check basic authn is required
        Utils.makeCall(webAppUrl.toURI(), SC_UNAUTHORIZED);

        assertAccessAllowed(webAppUrl, USER_A, PASSWORD_A);

        changeUserPassword(USER_A, "anotherPassword");
        assertAccessAllowed(webAppUrl, USER_A, PASSWORD_A);

        clearCache();
        assertAccessUnauthorized(webAppUrl, USER_A, PASSWORD_A);
    }

    /**
     * Given: client has successfully authenticated to application using USER_A credentials,
     * and role for USER_A is changed to "anotherRole" on the file system.
     * When client does a new request to a resource that requires original role using USER_A credentials,
     * then authorization verification for the request should be successfully performed against the cached identity
     * (any changes of USER_A on the file system should be ignored).
     *
     * @param webAppUrl URL to application protected by caching-realm
     */
    @Test
    public void testIdentityAttributeIsCached(@ArquillianResource URL webAppUrl) {
        try {
            assertAccessAllowed(webAppUrl, USER_A, PASSWORD_A);

            changeUserRole(USER_A, "anotherRole");
            assertAccessAllowed(webAppUrl, USER_A, PASSWORD_A);

            clearCache();
            assertAccessForbidden(webAppUrl, USER_A, PASSWORD_A);
        } finally {
            changeUserRole(USER_A, "Admin");
        }
    }

    /**
     * Given: client has successfully authenticated to application using credentials in following order:
     * USER_A, USER_B, USER_C,
     * and password for USER_A is changed to "anotherPassword" on the file system.
     * When client does a new request using USER_A credentials,
     * then authentication verification for the request should fail because it is not performed against cached identity
     * (any changes of USER_A on the file system should be reflected).
     *
     * @param webAppUrl URL to application protected by caching-realm
     */
    @Test
    public void testJustLastUsedIdentitiesInLimitAreCached(@ArquillianResource URL webAppUrl) {
        assertAccessAllowed(webAppUrl, USER_A, PASSWORD_A);
        assertAccessAllowed(webAppUrl, USER_B, PASSWORD_B);
        assertAccessAllowed(webAppUrl, USER_C, PASSWORD_C);

        changeUserPassword(USER_A, "anotherPassword");
        assertAccessUnauthorized(webAppUrl, USER_A, PASSWORD_A);
        assertAccessAllowed(webAppUrl, USER_A, "anotherPassword");
    }

    private static void assertAccessAllowed(URL webAppUrl, String username, String password) {
        try {
            String response = Utils.makeCallWithBasicAuthn(webAppUrl, username, password, SC_OK);
            assertEquals("Response content does not match index page", response, INDEX_PAGE_CONTENT);
        } catch (IOException | URISyntaxException ex) {
            throw new IllegalStateException("Unable to call the web application", ex);
        }
    }

    private static void assertAccessUnauthorized(URL webAppUrl, String username, String password) {
        try {
            Utils.makeCallWithBasicAuthn(webAppUrl, username, password, SC_UNAUTHORIZED);
        } catch (IOException | URISyntaxException ex) {
            throw new IllegalStateException("Unable to call the web application", ex);
        }
    }

    private static void assertAccessForbidden(URL webAppUrl, String username, String password) {
        try {
            Utils.makeCallWithBasicAuthn(webAppUrl, username, password, SC_FORBIDDEN);
        } catch (IOException | URISyntaxException ex) {
            throw new IllegalStateException("Unable to call the web application", ex);
        }
    }

    private static void clearCache() {
        try (CLIWrapper cli = new CLIWrapper(true)) {
            cli.sendLine(String.format("/subsystem=elytron/caching-realm=%s:clear-cache", CACHING_REALM));
        } catch (Exception ex) {
            throw new IllegalStateException("Unable to clear cache of caching-realm", ex);
        }
    }

    // Password has to be changed through another filesystem-realm for the cache not to be updated.
    private static void changeUserPassword(String username, String password) {
        try (CLIWrapper cli = new CLIWrapper(true)) {
            cli.sendLine(String.format("/subsystem=elytron/filesystem-realm=%s:set-password(identity=%s, clear={password=\"%s\"})", FILESYSTEM_REALM2, username, password));
        } catch (Exception ex) {
            throw new IllegalStateException("Unable to change user password in filesystem-realm", ex);
        }
    }

    // Role has to be changed through another filesystem-realm for the cache not to be updated.
    private static void changeUserRole(String username, String role) {
        try (CLIWrapper cli = new CLIWrapper(true)) {
            cli.sendLine(String.format("/subsystem=elytron/filesystem-realm=%s:remove-identity-attribute(identity=%s, name=groups)", FILESYSTEM_REALM2, username));
            cli.sendLine(String.format("/subsystem=elytron/filesystem-realm=%s:add-identity-attribute(identity=%s, name=groups, value=[\"%s\"])", FILESYSTEM_REALM2, username, role));
        } catch (Exception ex) {
            throw new IllegalStateException("Unable to change user role in filesystem-realm", ex);
        }
    }

    static class SetUpTask implements ServerSetupTask {

        static final String HTTP_AUTH_FACTORY = "httpAuthFactory-" + NAME;

        @Override
        public void setup(ManagementClient managementClient, String s) throws Exception {
            filesystemRealmPath = TARGET_FOLDER + File.separator + NAME + "-" + UUID.randomUUID();
            configureServer();
            prepareFilesystemUsers();
        }

        @Override
        public void tearDown(ManagementClient managementClient, String s) throws Exception {
            try (CLIWrapper cli = new CLIWrapper(true)) {
                cli.sendLine(String.format("/subsystem=undertow/application-security-domain=%s:remove()", SECURITY_DOMAIN));
                cli.sendLine(String.format("/subsystem=elytron/http-authentication-factory=%s:remove()", HTTP_AUTH_FACTORY));
                cli.sendLine(String.format("/subsystem=elytron/security-domain=%s:remove()", SECURITY_DOMAIN));
                cli.sendLine(String.format("/subsystem=elytron/caching-realm=%s:remove()", CACHING_REALM));
                cli.sendLine(String.format("/subsystem=elytron/filesystem-realm=%s:remove()", FILESYSTEM_REALM));
                cli.sendLine(String.format("/subsystem=elytron/filesystem-realm=%s:remove()", FILESYSTEM_REALM2));
            }
            ServerReload.reloadIfRequired(managementClient);
            FileUtils.deleteDirectory(new File(filesystemRealmPath));
        }

        private static void configureServer() {
            try (CLIWrapper cli =  new CLIWrapper(true)) {
                cli.sendLine(String.format("/subsystem=elytron/filesystem-realm=%s:add(path=%s)", FILESYSTEM_REALM, filesystemRealmPath));
                // Passwords and attributes have to be changed through another filesystem-realm for the cache not to be updated
                cli.sendLine(String.format("/subsystem=elytron/filesystem-realm=%s:add(path=%s)", FILESYSTEM_REALM2, filesystemRealmPath));
                cli.sendLine(String.format("/subsystem=elytron/caching-realm=%s:add(realm=%s, maximum-entries=2)", CACHING_REALM, FILESYSTEM_REALM));
                cli.sendLine(String.format("/subsystem=elytron/security-domain=%1$s:add(realms=[{realm=%2$s, role-decoder=groups-to-roles}], "
                        + "default-realm=%2$s, permission-mapper=default-permission-mapper)", SECURITY_DOMAIN, CACHING_REALM));
                cli.sendLine(String.format("/subsystem=elytron/http-authentication-factory=%s:add(http-server-mechanism-factory=global, security-domain=%s, "
                        + "mechanism-configurations=[{mechanism-name=BASIC, mechanism-realm-configurations=[{realm-name=\"Some Realm\"}]}])",
                        HTTP_AUTH_FACTORY, SECURITY_DOMAIN));
                cli.sendLine(String.format("/subsystem=undertow/application-security-domain=%s:add(http-authentication-factory=%s)",
                        SECURITY_DOMAIN, HTTP_AUTH_FACTORY));
            } catch (Exception ex) {
                throw new IllegalStateException("Unable to configure server", ex);
            }
        }

        private static void prepareFilesystemUsers() {
            addUserToFilesystemRealm(USER_A, PASSWORD_A, "Admin");
            addUserToFilesystemRealm(USER_B, PASSWORD_B, "User");
            addUserToFilesystemRealm(USER_C, PASSWORD_C, "Admin");
        }

        private static void addUserToFilesystemRealm(String username, String password, String role) {
            try (CLIWrapper cli =  new CLIWrapper(true)) {
                cli.sendLine(String.format("/subsystem=elytron/filesystem-realm=%s:add-identity(identity=%s)", FILESYSTEM_REALM, username));
                cli.sendLine(String.format("/subsystem=elytron/filesystem-realm=%s:set-password(identity=%s, clear={password=\"%s\"})", FILESYSTEM_REALM, username, password));
                cli.sendLine(String.format("/subsystem=elytron/filesystem-realm=%s:add-identity-attribute(identity=%s, name=groups, value=[\"%s\"])", FILESYSTEM_REALM, username, role));
            } catch (Exception ex) {
                throw new IllegalStateException("Unable to add user to filesystem-realm", ex);
            }
        }

    }
}
