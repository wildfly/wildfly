/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2017, Red Hat Middleware LLC, and individual contributors
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
package org.wildfly.test.integration.elytron.realm;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import static javax.servlet.http.HttpServletResponse.SC_FORBIDDEN;
import static javax.servlet.http.HttpServletResponse.SC_OK;
import static javax.servlet.http.HttpServletResponse.SC_UNAUTHORIZED;
import org.apache.commons.io.FileUtils;
import org.apache.http.NameValuePair;
import org.apache.http.client.utils.URLEncodedUtils;
import org.apache.http.message.BasicNameValuePair;
import org.codehaus.plexus.util.StringUtils;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.OperateOnDeployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.as.arquillian.api.ServerSetup;
import org.jboss.as.arquillian.api.ServerSetupTask;
import org.jboss.as.arquillian.container.ManagementClient;
import org.jboss.as.test.integration.management.util.CLIWrapper;
import org.jboss.as.test.integration.security.common.Utils;
import static org.jboss.as.test.integration.security.common.Utils.createTemporaryFolder;
import org.jboss.as.test.integration.security.common.servlets.RolePrintingServlet;
import org.jboss.as.test.shared.CliUtils;
import static org.jboss.as.test.shared.CliUtils.asAbsolutePath;
import org.jboss.as.test.shared.ServerReload;
import org.jboss.crypto.CryptoUtil;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import static org.junit.Assert.fail;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Test case for Elytron Aggregate Realm.
 *
 * It tests two types of scenarios:
 * <ul>
 * <li>Both, authentication and authorization realm, is the same Elytron Realm type (both are Properties Realm for this test
 * case)</li>
 * <li>Both is the different Elytron Realm type (Filesystem Realm for authentication and Properties Realm for
 * authorization)</li>
 * </ul>
 *
 * Given: Secured application for printing roles secured by Aggregate Realm.<br>
 *
 * @author olukas
 */
@RunWith(Arquillian.class)
@RunAsClient
@ServerSetup({AggregateRealmTestCase.SetupTask.class})
public class AggregateRealmTestCase {

    private static final String CHARSET_UTF_8 = "UTF-8";

    private static final String AGGREGATE_REALM_SAME_TYPE_NAME = "elytron-aggregate-realm-same-type";
    private static final String AGGREGATE_REALM_DIFFERENT_TYPE_NAME = "elytron-aggregate-realm-different-type";

    private static final String USER_WITHOUT_ROLE = "userWithoutRole";
    private static final String USER_WITH_ONE_ROLE = "userWithOneRole";
    private static final String USER_WITH_TWO_ROLES = "userWithTwoRoles";
    private static final String USER_WITH_DIFFERENT_ROLE_IN_DIFFERENT_REALM = "userWithDifferentRoleInDifferentRealm";
    private static final String USER_ONLY_IN_AUTHORIZATION = "userOnlyInAuthorization";
    private static final String WRONG_USER = "wrongUser";

    private static final String CORRECT_PASSWORD = "password";
    private static final String AUTHORIZATION_REALM_PASSWORD = "passwordInAuthzRealm";
    private static final String WRONG_PASSWORD = "wrongPassword";
    private static final String EMPTY_PASSWORD = "";

    private static final String ROLE_ADMIN = "Admin";
    private static final String ROLE_USER = "User";

    private static final String[] ALL_TESTED_ROLES = {ROLE_ADMIN, ROLE_USER};

    static final String QUERY_ROLES;

    static {
        final List<NameValuePair> qparams = new ArrayList<>();
        for (final String role : ALL_TESTED_ROLES) {
            qparams.add(new BasicNameValuePair(RolePrintingServlet.PARAM_ROLE_NAME, role));
        }
        QUERY_ROLES = URLEncodedUtils.format(qparams, "UTF-8");
    }

    @Deployment(name = AGGREGATE_REALM_SAME_TYPE_NAME)
    public static WebArchive deploymentSameType() {
        return deployment(AGGREGATE_REALM_SAME_TYPE_NAME);
    }

    @Deployment(name = AGGREGATE_REALM_DIFFERENT_TYPE_NAME)
    public static WebArchive deploymentDifferentType() {
        return deployment(AGGREGATE_REALM_DIFFERENT_TYPE_NAME);
    }

    private static WebArchive deployment(String name) {
        final WebArchive war = ShrinkWrap.create(WebArchive.class, name + ".war");
        war.addClasses(RolePrintingServlet.class);
        war.addAsWebInfResource(AggregateRealmTestCase.class.getPackage(), "aggregate-realm-web.xml", "web.xml");
        war.addAsWebInfResource(Utils.getJBossWebXmlAsset(name), "jboss-web.xml");
        return war;
    }

    /**
     * Given: Authentication is provided by Elytron Properties Realm<br>
     * and authorization is provided by another Elytron Properties Realm<br>
     * and roles property file maps no roles for the user. <br>
     * When the user with correct username and password tries to authenticate, <br>
     * then authentication should succeed <br>
     * and authorization should fail - no roles should be assigned to the user (HTTP status 403 is returned).
     */
    @Test
    @OperateOnDeployment(AGGREGATE_REALM_SAME_TYPE_NAME)
    public void userWithNoRoles_userInBothRealm_sameTypeRealm(@ArquillianResource URL webAppURL) throws Exception {
        userWithNoRoles_userInBothRealm(webAppURL);
    }

    /**
     * Given: Authentication is provided by Elytron Properties Realm<br>
     * and authorization is provided by another Elytron Properties Realm<br>
     * and roles property file maps role 'User' for the user. <br>
     * When user with correct username and password tries to authenticate, <br>
     * then authentication should succeed <br>
     * and just role 'User' should be assigned to the user.
     */
    @Test
    @OperateOnDeployment(AGGREGATE_REALM_SAME_TYPE_NAME)
    public void userWithOneRole_userInBothRealm_sameTypeRealm(@ArquillianResource URL webAppURL) throws Exception {
        userWithOneRole_userInBothRealm(webAppURL);
    }

    /**
     * Given: Authentication is provided by Elytron Properties Realm<br>
     * and authorization is provided by another Elytron Properties Realm<br>
     * and roles property file maps roles 'User' and 'Admin' for the user. <br>
     * When user with correct username and password tries to authenticate, <br>
     * then authentication should succeed <br>
     * and just roles 'User' and 'Admin' should be assigned to the user.
     */
    @Test
    @OperateOnDeployment(AGGREGATE_REALM_SAME_TYPE_NAME)
    public void userWithTwoRoles_userInBothRealm_sameTypeRealm(@ArquillianResource URL webAppURL) throws Exception {
        userWithTwoRoles_userInBothRealm(webAppURL);
    }

    /**
     * Given: Authentication is provided by Elytron Properties Realm<br>
     * and authorization is provided by another Elytron Properties Realm.<br>
     * When the user with correct username but wrong password tries to authenticate, <br>
     * then authentication should fail (HTTP status 401 is returned).
     */
    @Test
    @OperateOnDeployment(AGGREGATE_REALM_SAME_TYPE_NAME)
    public void wrongPassword_userInBothRealm_sameTypeRealm(@ArquillianResource URL webAppURL) throws Exception {
        wrongPassword_userInBothRealm(webAppURL);
    }

    /**
     * Given: Authentication is provided by Elytron Properties Realm<br>
     * and authorization is provided by another Elytron Properties Realm.<br>
     * When the user with correct username but with empty password tries to authenticate, <br>
     * then authentication should fail (HTTP status 401 is returned).
     */
    @Test
    @OperateOnDeployment(AGGREGATE_REALM_SAME_TYPE_NAME)
    public void emptyPassword_userInBothRealm_sameTypeRealm(@ArquillianResource URL webAppURL) throws Exception {
        emptyPassword_userInBothRealm(webAppURL);
    }

    /**
     * Given: Authentication is provided by Elytron Properties Realm<br>
     * and authorization is provided by another Elytron Properties Realm<br>
     * and realm used for authorization define different password for the user then realm for authentication.<br>
     * When the user with correct username but with password from realm for authorization tries to authenticate, <br>
     * then authentication should fail (HTTP status 401 is returned).
     */
    @Test
    @OperateOnDeployment(AGGREGATE_REALM_SAME_TYPE_NAME)
    public void passwordFromAuthzRealm_userInBothRealm_sameTypeRealm(@ArquillianResource URL webAppURL) throws Exception {
        passwordFromAuthzRealm_userInBothRealm(webAppURL);
    }

    /**
     * Given: Authentication is provided by Elytron Properties Realm<br>
     * and authorization is provided by another Elytron Properties Realm.<br>
     * When non-exist user tries to authenticate, <br>
     * then authentication should fail (HTTP status 401 is returned).
     */
    @Test
    @OperateOnDeployment(AGGREGATE_REALM_SAME_TYPE_NAME)
    public void wrongUser_sameTypeRealm(@ArquillianResource URL webAppURL) throws Exception {
        wrongUser(webAppURL);
    }

    /**
     * Given: Authentication is provided by Elytron Properties Realm<br>
     * and authorization is provided by another Elytron Properties Realm<br>
     * and realm for authentication maps role 'User' for the user <br>
     * and realm for authorization maps role 'Admin' for the user. <br>
     * When user with correct username and password tries to authenticate, <br>
     * then authentication should succeed <br>
     * and just role 'Admin' should be assigned to the user.
     */
    @Test
    @OperateOnDeployment(AGGREGATE_REALM_SAME_TYPE_NAME)
    public void userWithDifferentRoleInDifferentRealm_sameTypeRealm(@ArquillianResource URL webAppURL) throws Exception {
        userWithDifferentRoleInDifferentRealm(webAppURL);
    }

    /**
     * Given: Authentication is provided by Elytron Properties Realm<br>
     * and authorization is provided by another Elytron Properties Realm<br>
     * and roles property file maps role 'User' for the user <br>
     * and realm for authorization does not include user's username in authentication store. <br>
     * When user with correct username and password tries to authenticate, <br>
     * then authentication should succeed <br>
     * and just role 'User' should be assigned to the user.
     */
    @Test
    @OperateOnDeployment(AGGREGATE_REALM_SAME_TYPE_NAME)
    public void correctPassword_userOnlyInAuthzRealm_sameTypeRealm(@ArquillianResource URL webAppURL) throws Exception {
        correctPassword_userOnlyInAuthzRealm(webAppURL);
    }

    /**
     * Given: Authentication is provided by Elytron Properties Realm<br>
     * and authorization is provided by another Elytron Properties Realm<br>
     * and realm for authorization does not include user's username in authentication store. <br>
     * When the user with correct username but wrong password tries to authenticate, <br>
     * then authentication should fail (HTTP status 401 is returned).
     */
    @Test
    @OperateOnDeployment(AGGREGATE_REALM_SAME_TYPE_NAME)
    public void wrongPassword_userOnlyInAuthzRealm_sameTypeRealm(@ArquillianResource URL webAppURL) throws Exception {
        wrongPassword_userOnlyInAuthzRealm(webAppURL);
    }

    /**
     * Given: Authentication is provided by Elytron Properties Realm<br>
     * and authorization is provided by another Elytron Properties Realm<br>
     * and realm for authorization does not include user's username in authentication store. <br>
     * When the user with correct username but with empty password tries to authenticate, <br>
     * then authentication should fail (HTTP status 401 is returned).
     */
    @Test
    @OperateOnDeployment(AGGREGATE_REALM_SAME_TYPE_NAME)
    public void emptyPassword_userOnlyInAuthzRealm_sameTypeRealm(@ArquillianResource URL webAppURL) throws Exception {
        emptyPassword_userOnlyInAuthzRealm(webAppURL);
    }

    /**
     * Given: Authentication is provided by Elytron Filesystem Realm<br>
     * and authorization is provided by another Elytron Properties Realm<br>
     * and roles property file maps no roles for the user. <br>
     * When the user with correct username and password tries to authenticate, <br>
     * then authentication should succeed <br>
     * and authorization should fail - no roles should be assigned to the user (HTTP status 403 is returned).
     */
    @Test
    @OperateOnDeployment(AGGREGATE_REALM_DIFFERENT_TYPE_NAME)
    public void userWithNoRoles_userInBothRealm_differentTypeRealm(@ArquillianResource URL webAppURL) throws Exception {
        userWithNoRoles_userInBothRealm(webAppURL);
    }

    /**
     * Given: Authentication is provided by Elytron Filesystem Realm<br>
     * and authorization is provided by another Elytron Properties Realm<br>
     * and roles property file maps role 'User' for the user. <br>
     * When user with correct username and password tries to authenticate, <br>
     * then authentication should succeed <br>
     * and just role 'User' should be assigned to the user.
     */
    @Test
    @OperateOnDeployment(AGGREGATE_REALM_DIFFERENT_TYPE_NAME)
    public void userWithOneRole_userInBothRealm_differentTypeRealm(@ArquillianResource URL webAppURL) throws Exception {
        userWithOneRole_userInBothRealm(webAppURL);
    }

    /**
     * Given: Authentication is provided by Elytron Filesystem Realm<br>
     * and authorization is provided by another Elytron Properties Realm<br>
     * and roles property file maps roles 'User' and 'Admin' for the user. <br>
     * When user with correct username and password tries to authenticate, <br>
     * then authentication should succeed <br>
     * and just roles 'User' and 'Admin' should be assigned to the user.
     */
    @Test
    @OperateOnDeployment(AGGREGATE_REALM_DIFFERENT_TYPE_NAME)
    public void userWithTwoRoles_userInBothRealm_differentTypeRealm(@ArquillianResource URL webAppURL) throws Exception {
        userWithTwoRoles_userInBothRealm(webAppURL);
    }

    /**
     * Given: Authentication is provided by Elytron Filesystem Realm<br>
     * and authorization is provided by another Elytron Properties Realm.<br>
     * When the user with correct username but wrong password tries to authenticate, <br>
     * then authentication should fail (HTTP status 401 is returned).
     */
    @Test
    @OperateOnDeployment(AGGREGATE_REALM_DIFFERENT_TYPE_NAME)
    public void wrongPassword_userInBothRealm_differentTypeRealm(@ArquillianResource URL webAppURL) throws Exception {
        wrongPassword_userInBothRealm(webAppURL);
    }

    /**
     * Given: Authentication is provided by Elytron Filesystem Realm<br>
     * and authorization is provided by another Elytron Properties Realm.<br>
     * When the user with correct username but with empty password tries to authenticate, <br>
     * then authentication should fail (HTTP status 401 is returned).
     */
    @Test
    @OperateOnDeployment(AGGREGATE_REALM_DIFFERENT_TYPE_NAME)
    public void emptyPassword_userInBothRealm_differentTypeRealm(@ArquillianResource URL webAppURL) throws Exception {
        emptyPassword_userInBothRealm(webAppURL);
    }

    /**
     * Given: Authentication is provided by Elytron Filesystem Realm<br>
     * and authorization is provided by another Elytron Properties Realm<br>
     * and realm used for authorization define different password for the user then realm for authentication.<br>
     * When the user with correct username but with password from realm for authorization tries to authenticate, <br>
     * then authentication should fail (HTTP status 401 is returned).
     */
    @Test
    @OperateOnDeployment(AGGREGATE_REALM_DIFFERENT_TYPE_NAME)
    public void passwordFromAuthzRealm_userInBothRealm_differentTypeRealm(@ArquillianResource URL webAppURL) throws Exception {
        passwordFromAuthzRealm_userInBothRealm(webAppURL);
    }

    /**
     * Given: Authentication is provided by Elytron Filesystem Realm<br>
     * and authorization is provided by another Elytron Properties Realm.<br>
     * When non-exist user tries to authenticate, <br>
     * then authentication should fail (HTTP status 401 is returned).
     */
    @Test
    @OperateOnDeployment(AGGREGATE_REALM_DIFFERENT_TYPE_NAME)
    public void wrongUser_differentTypeRealm(@ArquillianResource URL webAppURL) throws Exception {
        wrongUser(webAppURL);
    }

    /**
     * Given: Authentication is provided by Elytron Filesystem Realm<br>
     * and authorization is provided by another Elytron Properties Realm<br>
     * and realm for authentication maps role 'User' for the user <br>
     * and realm for authorization maps role 'Admin' for the user. <br>
     * When user with correct username and password tries to authenticate, <br>
     * then authentication should succeed <br>
     * and just role 'Admin' should be assigned to the user.
     */
    @Test
    @OperateOnDeployment(AGGREGATE_REALM_DIFFERENT_TYPE_NAME)
    public void userWithDifferentRoleInDifferentRealm_differentTypeRealm(@ArquillianResource URL webAppURL) throws Exception {
        userWithDifferentRoleInDifferentRealm(webAppURL);
    }

    /**
     * Given: Authentication is provided by Elytron Filesystem Realm<br>
     * and authorization is provided by another Elytron Properties Realm<br>
     * and roles property file maps role 'User' for the user <br>
     * and realm for authorization does not include user's username in authentication store. <br>
     * When user with correct username and password tries to authenticate, <br>
     * then authentication should succeed <br>
     * and just role 'User' should be assigned to the user.
     */
    @Test
    @OperateOnDeployment(AGGREGATE_REALM_DIFFERENT_TYPE_NAME)
    public void correctPassword_userOnlyInAuthzRealm_differentTypeRealm(@ArquillianResource URL webAppURL) throws Exception {
        correctPassword_userOnlyInAuthzRealm(webAppURL);
    }

    /**
     * Given: Authentication is provided by Elytron Filesystem Realm<br>
     * and authorization is provided by another Elytron Properties Realm<br>
     * and realm for authorization does not include user's username in authentication store. <br>
     * When the user with correct username but wrong password tries to authenticate, <br>
     * then authentication should fail (HTTP status 401 is returned).
     */
    @Test
    @OperateOnDeployment(AGGREGATE_REALM_DIFFERENT_TYPE_NAME)
    public void wrongPassword_userOnlyInAuthzRealm_differentTypeRealm(@ArquillianResource URL webAppURL) throws Exception {
        wrongPassword_userOnlyInAuthzRealm(webAppURL);
    }

    /**
     * Given: Authentication is provided by Elytron Filesystem Realm<br>
     * and authorization is provided by another Elytron Properties Realm<br>
     * and realm for authorization does not include user's username in authentication store. <br>
     * When the user with correct username but with empty password tries to authenticate, <br>
     * then authentication should fail (HTTP status 401 is returned).
     */
    @Test
    @OperateOnDeployment(AGGREGATE_REALM_DIFFERENT_TYPE_NAME)
    public void emptyPassword_userOnlyInAuthzRealm_differentTypeRealm(@ArquillianResource URL webAppURL) throws Exception {
        emptyPassword_userOnlyInAuthzRealm(webAppURL);
    }

    private void userWithNoRoles_userInBothRealm(URL webAppURL) throws Exception {
        assertNoRoleAssigned(webAppURL, USER_WITHOUT_ROLE, CORRECT_PASSWORD);
    }

    private void userWithOneRole_userInBothRealm(URL webAppURL) throws Exception {
        testAssignedRoles(webAppURL, USER_WITH_ONE_ROLE, CORRECT_PASSWORD, ROLE_USER);
    }

    private void userWithTwoRoles_userInBothRealm(URL webAppURL) throws Exception {
        testAssignedRoles(webAppURL, USER_WITH_TWO_ROLES, CORRECT_PASSWORD, ROLE_USER, ROLE_ADMIN);
    }

    private void wrongPassword_userInBothRealm(URL webAppURL) throws Exception {
        assertAuthenticationFailed(webAppURL, USER_WITH_ONE_ROLE, WRONG_PASSWORD);
    }

    private void emptyPassword_userInBothRealm(URL webAppURL) throws Exception {
        assertAuthenticationFailed(webAppURL, USER_WITH_ONE_ROLE, EMPTY_PASSWORD);
    }

    private void passwordFromAuthzRealm_userInBothRealm(URL webAppURL) throws Exception {
        assertAuthenticationFailed(webAppURL, USER_WITH_ONE_ROLE, AUTHORIZATION_REALM_PASSWORD);
    }

    private void wrongUser(URL webAppURL) throws Exception {
        assertAuthenticationFailed(webAppURL, WRONG_USER, CORRECT_PASSWORD);
    }

    private void userWithDifferentRoleInDifferentRealm(URL webAppURL) throws Exception {
        testAssignedRoles(webAppURL, USER_WITH_DIFFERENT_ROLE_IN_DIFFERENT_REALM, CORRECT_PASSWORD, ROLE_ADMIN);
    }

    private void correctPassword_userOnlyInAuthzRealm(URL webAppURL) throws Exception {
        testAssignedRoles(webAppURL, USER_ONLY_IN_AUTHORIZATION, CORRECT_PASSWORD, ROLE_USER);
    }

    private void wrongPassword_userOnlyInAuthzRealm(URL webAppURL) throws Exception {
        assertAuthenticationFailed(webAppURL, USER_ONLY_IN_AUTHORIZATION, WRONG_PASSWORD);
    }

    private void emptyPassword_userOnlyInAuthzRealm(URL webAppURL) throws Exception {
        assertAuthenticationFailed(webAppURL, USER_ONLY_IN_AUTHORIZATION, EMPTY_PASSWORD);
    }

    private void testAssignedRoles(URL webAppURL, String username, String password, String... assignedRoles) throws Exception {
        final URL rolesPrintingURL = prepareRolesPrintingURL(webAppURL);
        final String rolesResponse = Utils.makeCallWithBasicAuthn(rolesPrintingURL, username, password, SC_OK);

        final List<String> assignedRolesList = Arrays.asList(assignedRoles);

        for (String role : ALL_TESTED_ROLES) {
            if (assignedRolesList.contains(role)) {
                assertInRole(rolesResponse, role);
            } else {
                assertNotInRole(rolesResponse, role);
            }
        }
    }

    private void assertNoRoleAssigned(URL webAppURL, String username, String password) throws Exception {
        final URL rolesPrintingURL = prepareRolesPrintingURL(webAppURL);
        Utils.makeCallWithBasicAuthn(rolesPrintingURL, username, password, SC_FORBIDDEN);
    }

    private void assertAuthenticationFailed(URL webAppURL, String username, String password) throws Exception {
        final URL rolesPrintingURL = prepareRolesPrintingURL(webAppURL);
        Utils.makeCallWithBasicAuthn(rolesPrintingURL, username, password, SC_UNAUTHORIZED);
    }

    private URL prepareRolesPrintingURL(URL webAppURL) throws MalformedURLException {
        return new URL(webAppURL.toExternalForm() + RolePrintingServlet.SERVLET_PATH.substring(1) + "?" + QUERY_ROLES);
    }

    private void assertInRole(final String rolePrintResponse, String role) {
        if (!StringUtils.contains(rolePrintResponse, "," + role + ",")) {
            fail("Missing role '" + role + "' assignment");
        }
    }

    private void assertNotInRole(final String rolePrintResponse, String role) {
        if (StringUtils.contains(rolePrintResponse, "," + role + ",")) {
            fail("Unexpected role '" + role + "' assignment");
        }
    }

    static class SetupTask implements ServerSetupTask {

        private static final String PREDEFINED_HTTP_SERVER_MECHANISM_FACTORY = "global";

        private static final String PROPERTIES_REALM_AUTHN_NAME = "elytron-authn-properties-realm";
        private static final String PROPERTIES_REALM_AUTHZ_NAME = "elytron-authz-properties-realm";
        private static final String FILESYSTEM_REALM_AUTHN_NAME = "elytron-authn-filesystem-realm";

        private static final String USERS_AUTHN_REALM_FILENAME = "users-authn.properties";
        private static final String ROLES_AUTHN_REALM_FILENAME = "roles-authn.properties";
        private static final String USERS_AUTHZ_REALM_FILENAME = "users-authz.properties";
        private static final String ROLES_AUTHZ_REALM_FILENAME = "roles-authz.properties";
        private File usersAuthnRealmFile;
        private File rolesAuthnRealmFile;
        private File usersAuthzRealmFile;
        private File rolesAuthzRealmFile;
        private String fsRealmPath;

        private File tempFolder;

        @Override
        public void setup(ManagementClient mc, String string) throws Exception {
            tempFolder = createTemporaryFolder("ely-" + AggregateRealmTestCase.class.getSimpleName());
            String tempFolderAbsolutePath = tempFolder.getAbsolutePath();
            usersAuthnRealmFile = new File(tempFolderAbsolutePath, USERS_AUTHN_REALM_FILENAME);
            rolesAuthnRealmFile = new File(tempFolderAbsolutePath, ROLES_AUTHN_REALM_FILENAME);
            usersAuthzRealmFile = new File(tempFolderAbsolutePath, USERS_AUTHZ_REALM_FILENAME);
            rolesAuthzRealmFile = new File(tempFolderAbsolutePath, ROLES_AUTHZ_REALM_FILENAME);
            fsRealmPath = CliUtils.escapePath(tempFolderAbsolutePath + File.separator + "fs-realm-users");
            createPropertiesFiles();
            try (CLIWrapper cli = new CLIWrapper(true)) {
                cli.sendLine(String.format(
                        "/subsystem=elytron/properties-realm=%s:add(users-properties={path=%s},groups-properties={path=%s})",
                        PROPERTIES_REALM_AUTHN_NAME, asAbsolutePath(usersAuthnRealmFile),
                        asAbsolutePath(rolesAuthnRealmFile)));
                cli.sendLine(String.format(
                        "/subsystem=elytron/properties-realm=%s:add(users-properties={path=%s},groups-properties={path=%s})",
                        PROPERTIES_REALM_AUTHZ_NAME, asAbsolutePath(usersAuthzRealmFile),
                        asAbsolutePath(rolesAuthzRealmFile)));
                cli.sendLine(String.format(
                        "/subsystem=elytron/filesystem-realm=%s:add(path=%s)",
                        FILESYSTEM_REALM_AUTHN_NAME, fsRealmPath));
                addUserToFilesystemRealm(cli, USER_WITHOUT_ROLE, CORRECT_PASSWORD);
                addUserToFilesystemRealm(cli, USER_WITH_ONE_ROLE, CORRECT_PASSWORD);
                addUserToFilesystemRealm(cli, USER_WITH_TWO_ROLES, CORRECT_PASSWORD);
                addUserToFilesystemRealm(cli, USER_WITH_DIFFERENT_ROLE_IN_DIFFERENT_REALM, CORRECT_PASSWORD, ROLE_USER);
                addUserToFilesystemRealm(cli, USER_ONLY_IN_AUTHORIZATION, CORRECT_PASSWORD);
                addAggregateRealmAndRelatedResources(cli, AGGREGATE_REALM_SAME_TYPE_NAME, PROPERTIES_REALM_AUTHN_NAME,
                        PROPERTIES_REALM_AUTHZ_NAME);
                addAggregateRealmAndRelatedResources(cli, AGGREGATE_REALM_DIFFERENT_TYPE_NAME, FILESYSTEM_REALM_AUTHN_NAME,
                        PROPERTIES_REALM_AUTHZ_NAME);
            }
            ServerReload.reloadIfRequired(mc.getControllerClient());
        }

        @Override
        public void tearDown(ManagementClient mc, String string) throws Exception {
            try (CLIWrapper cli = new CLIWrapper(true)) {
                removeAggregateRealmAndRelatedResources(mc, cli, AGGREGATE_REALM_DIFFERENT_TYPE_NAME);
                removeAggregateRealmAndRelatedResources(mc, cli, AGGREGATE_REALM_SAME_TYPE_NAME);
                ServerReload.reloadIfRequired(mc.getControllerClient());
                cli.sendLine(String.format("/subsystem=elytron/filesystem-realm=%s:remove()", FILESYSTEM_REALM_AUTHN_NAME));
                cli.sendLine(String.format("/subsystem=elytron/properties-realm=%s:remove()", PROPERTIES_REALM_AUTHZ_NAME));
                cli.sendLine(String.format("/subsystem=elytron/properties-realm=%s:remove()", PROPERTIES_REALM_AUTHN_NAME));
                ServerReload.reloadIfRequired(mc.getControllerClient());
            } finally {
                removePropertiesFiles();
            }
        }

        private void createPropertiesFiles() throws IOException {
            createUsersProperties_authnRealm();
            createRolesProperties_authnRealm();
            createUsersProperties_authzRealm();
            createRolesProperties_authzRealm();
        }

        private void createUsersProperties_authnRealm() throws IOException {
            StringBuilder sb = new StringBuilder();
            sb.append("#$REALM_NAME=" + PROPERTIES_REALM_AUTHN_NAME + "$\n");
            sb.append(createPropertiesUserWithHashedPassword(USER_WITHOUT_ROLE, CORRECT_PASSWORD, PROPERTIES_REALM_AUTHN_NAME));
            sb.append(createPropertiesUserWithHashedPassword(USER_WITH_ONE_ROLE, CORRECT_PASSWORD, PROPERTIES_REALM_AUTHN_NAME));
            sb.append(createPropertiesUserWithHashedPassword(USER_WITH_TWO_ROLES, CORRECT_PASSWORD, PROPERTIES_REALM_AUTHN_NAME));
            sb.append(createPropertiesUserWithHashedPassword(USER_WITH_DIFFERENT_ROLE_IN_DIFFERENT_REALM, CORRECT_PASSWORD, PROPERTIES_REALM_AUTHN_NAME));
            sb.append(createPropertiesUserWithHashedPassword(USER_ONLY_IN_AUTHORIZATION, CORRECT_PASSWORD, PROPERTIES_REALM_AUTHN_NAME));
            FileUtils.writeStringToFile(usersAuthnRealmFile, sb.toString(), CHARSET_UTF_8);
        }

        private void createRolesProperties_authnRealm() throws IOException {
            StringBuilder sb = new StringBuilder();
            sb.append(USER_WITH_DIFFERENT_ROLE_IN_DIFFERENT_REALM + "=" + ROLE_USER + "\n");
            FileUtils.writeStringToFile(rolesAuthnRealmFile, sb.toString(), CHARSET_UTF_8);
        }

        private void createUsersProperties_authzRealm() throws IOException {
            StringBuilder sb = new StringBuilder();
            sb.append("#$REALM_NAME=" + PROPERTIES_REALM_AUTHZ_NAME + "$\n");
            sb.append(createPropertiesUserWithHashedPassword(USER_WITHOUT_ROLE, AUTHORIZATION_REALM_PASSWORD, PROPERTIES_REALM_AUTHZ_NAME));
            sb.append(createPropertiesUserWithHashedPassword(USER_WITH_ONE_ROLE, AUTHORIZATION_REALM_PASSWORD, PROPERTIES_REALM_AUTHZ_NAME));
            sb.append(createPropertiesUserWithHashedPassword(USER_WITH_TWO_ROLES, AUTHORIZATION_REALM_PASSWORD, PROPERTIES_REALM_AUTHZ_NAME));
            sb.append(createPropertiesUserWithHashedPassword(USER_WITH_DIFFERENT_ROLE_IN_DIFFERENT_REALM, AUTHORIZATION_REALM_PASSWORD, PROPERTIES_REALM_AUTHZ_NAME));
            FileUtils.writeStringToFile(usersAuthzRealmFile, sb.toString(), CHARSET_UTF_8);
        }

        private void createRolesProperties_authzRealm() throws IOException {
            StringBuilder sb = new StringBuilder();
            sb.append(USER_WITH_ONE_ROLE + "=" + ROLE_USER + "\n");
            sb.append(USER_WITH_TWO_ROLES + "=" + ROLE_USER + "," + ROLE_ADMIN + "\n");
            sb.append(USER_WITH_DIFFERENT_ROLE_IN_DIFFERENT_REALM + "=" + ROLE_ADMIN + "\n");
            sb.append(USER_ONLY_IN_AUTHORIZATION + "=" + ROLE_USER + "\n");
            FileUtils.writeStringToFile(rolesAuthzRealmFile, sb.toString(), CHARSET_UTF_8);
        }

        private String createPropertiesUserWithHashedPassword(String username, String password, String realmName) {
            return username + "=" + createHashedPassword(username, password, realmName) + "\n";
        }

        private String createHashedPassword(String username, String password, String realmName) {
            String clearTextPassword = username + ":" + realmName + ":" + password;
            String hashedPassword = CryptoUtil.createPasswordHash("MD5", "hex", null, null, clearTextPassword);
            return hashedPassword;
        }

        private void removePropertiesFiles() throws IOException {
            FileUtils.deleteQuietly(usersAuthnRealmFile);
            FileUtils.deleteQuietly(rolesAuthnRealmFile);
            FileUtils.deleteQuietly(usersAuthzRealmFile);
            FileUtils.deleteQuietly(rolesAuthzRealmFile);
            FileUtils.deleteDirectory(new File(fsRealmPath));
            FileUtils.deleteDirectory(tempFolder);
        }

        private void addUserToFilesystemRealm(CLIWrapper cli, String username, String password) throws Exception {
            addUserToFilesystemRealm(cli, username, password, null);
        }

        private void addUserToFilesystemRealm(CLIWrapper cli, String username, String password, String role)
                throws Exception {
            cli.sendLine(String.format("/subsystem=elytron/filesystem-realm=%s:add-identity(identity=%s)",
                    FILESYSTEM_REALM_AUTHN_NAME, username));
            cli.sendLine(String.format("/subsystem=elytron/filesystem-realm=%s:set-password(identity=%s, clear={password=\"%s\"})",
                    FILESYSTEM_REALM_AUTHN_NAME, username, password));
            if (role != null) {
                cli.sendLine(String.format("/subsystem=elytron/filesystem-realm=%s:add-identity-attribute(identity=%s, name=Roles, value=[\"%s\"])",
                        FILESYSTEM_REALM_AUTHN_NAME, username, role));
            }
        }

        private void addAggregateRealmAndRelatedResources(CLIWrapper cli, String name, String authnRealm, String authzRealm) {
            cli.sendLine(String.format(
                    "/subsystem=elytron/aggregate-realm=%s:add(authentication-realm=%s,authorization-realm=%s)",
                    name, authnRealm, authzRealm));
            cli.sendLine(String.format(
                    "/subsystem=elytron/security-domain=%1$s:add(realms=[{realm=%1$s,role-decoder=groups-to-roles}],default-realm=%1$s,permission-mapper=default-permission-mapper)",
                    name));
            cli.sendLine(String.format(
                    "/subsystem=elytron/http-authentication-factory=%1$s:add(http-server-mechanism-factory=%2$s,security-domain=%1$s,"
                    + "mechanism-configurations=[{mechanism-name=BASIC,mechanism-realm-configurations=[{realm-name=\"Some realm\"}]}])",
                    name, PREDEFINED_HTTP_SERVER_MECHANISM_FACTORY));
            cli.sendLine(String.format(
                    "/subsystem=undertow/application-security-domain=%1$s:add(http-authentication-factory=%1$s)",
                    name));
        }

        private void removeAggregateRealmAndRelatedResources(ManagementClient mc, CLIWrapper cli, String name) throws Exception {
            cli.sendLine(String.format("/subsystem=undertow/application-security-domain=%s:remove()", name));
            cli.sendLine(String.format("/subsystem=elytron/http-authentication-factory=%s:remove()", name));
            cli.sendLine(String.format("/subsystem=elytron/security-domain=%s:remove()", name));
            cli.sendLine(String.format("/subsystem=elytron/aggregate-realm=%s:remove()", name));
        }

    }
}
