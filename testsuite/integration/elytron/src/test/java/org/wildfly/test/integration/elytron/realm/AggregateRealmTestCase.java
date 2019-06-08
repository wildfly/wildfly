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

import static javax.servlet.http.HttpServletResponse.SC_FORBIDDEN;
import static javax.servlet.http.HttpServletResponse.SC_OK;
import static javax.servlet.http.HttpServletResponse.SC_UNAUTHORIZED;
import static org.junit.Assert.fail;

import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

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
import org.jboss.as.test.integration.security.common.Utils;
import org.jboss.as.test.integration.security.common.servlets.RolePrintingServlet;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.wildfly.test.security.common.AbstractElytronSetupTask;
import org.wildfly.test.security.common.elytron.AggregateSecurityRealm;
import org.wildfly.test.security.common.elytron.ConfigurableElement;
import org.wildfly.test.security.common.elytron.FileSystemRealm;
import org.wildfly.test.security.common.elytron.PropertiesRealm;
import org.wildfly.test.security.common.elytron.SimpleSecurityDomain;
import org.wildfly.test.security.common.elytron.UserWithAttributeValues;
import org.wildfly.test.undertow.common.UndertowApplicationSecurityDomain;

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
        QUERY_ROLES = URLEncodedUtils.format(qparams, StandardCharsets.UTF_8);
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

    static class SetupTask extends AbstractElytronSetupTask {

        private static final String PROPERTIES_REALM_AUTHN_NAME = "elytron-authn-properties-realm";
        private static final String PROPERTIES_REALM_AUTHZ_NAME = "elytron-authz-properties-realm";
        private static final String FILESYSTEM_REALM_AUTHN_NAME = "elytron-authn-filesystem-realm";

        @Override
        protected ConfigurableElement[] getConfigurableElements() {
            ArrayList<ConfigurableElement> configurableElements = new ArrayList<>();
            configurableElements.add(PropertiesRealm.builder()
                    .withName(PROPERTIES_REALM_AUTHN_NAME)
                    .withUser(UserWithAttributeValues.builder()
                            .withName(USER_WITHOUT_ROLE)
                            .withPassword(CORRECT_PASSWORD)
                            .build())
                    .withUser(UserWithAttributeValues.builder()
                            .withName(USER_WITH_ONE_ROLE)
                            .withPassword(CORRECT_PASSWORD)
                            .build())
                    .withUser(UserWithAttributeValues.builder()
                            .withName(USER_WITH_TWO_ROLES)
                            .withPassword(CORRECT_PASSWORD)
                            .build())
                    .withUser(UserWithAttributeValues.builder()
                            .withName(USER_WITH_DIFFERENT_ROLE_IN_DIFFERENT_REALM)
                            .withPassword(CORRECT_PASSWORD)
                            .build())
                    .withUser(UserWithAttributeValues.builder()
                            .withName(USER_WITH_DIFFERENT_ROLE_IN_DIFFERENT_REALM)
                            .withPassword(CORRECT_PASSWORD)
                            .withValues(ROLE_USER)
                            .build())
                    .withUser(UserWithAttributeValues.builder()
                            .withName(USER_ONLY_IN_AUTHORIZATION)
                            .withPassword(CORRECT_PASSWORD)
                            .build())
                    .build());
            configurableElements.add(PropertiesRealm.builder()
                    .withName(PROPERTIES_REALM_AUTHZ_NAME)
                    .withUser(UserWithAttributeValues.builder()
                            .withName(USER_WITHOUT_ROLE)
                            .withPassword(AUTHORIZATION_REALM_PASSWORD)
                            .build())
                    .withUser(UserWithAttributeValues.builder()
                            .withName(USER_WITH_ONE_ROLE)
                            .withPassword(AUTHORIZATION_REALM_PASSWORD)
                            .withValues(ROLE_USER)
                            .build())
                    .withUser(UserWithAttributeValues.builder()
                            .withName(USER_WITH_TWO_ROLES)
                            .withPassword(AUTHORIZATION_REALM_PASSWORD)
                            .withValues(ROLE_USER, ROLE_ADMIN)
                            .build())
                    .withUser(UserWithAttributeValues.builder()
                            .withName(USER_WITH_DIFFERENT_ROLE_IN_DIFFERENT_REALM)
                            .withPassword(AUTHORIZATION_REALM_PASSWORD)
                            .withValues(ROLE_ADMIN)
                            .build())
                    .withUser(UserWithAttributeValues.builder()
                            .withName(USER_ONLY_IN_AUTHORIZATION)
                            .withValues(ROLE_USER)
                            .build())
                    .build());
            configurableElements.add(FileSystemRealm.builder()
                    .withName(FILESYSTEM_REALM_AUTHN_NAME)
                    .withUser(UserWithAttributeValues.builder()
                            .withName(USER_WITHOUT_ROLE)
                            .withPassword(CORRECT_PASSWORD)
                            .build())
                    .withUser(UserWithAttributeValues.builder()
                            .withName(USER_WITH_ONE_ROLE)
                            .withPassword(CORRECT_PASSWORD)
                            .build())
                    .withUser(UserWithAttributeValues.builder()
                            .withName(USER_WITH_TWO_ROLES)
                            .withPassword(CORRECT_PASSWORD)
                            .build())
                    .withUser(UserWithAttributeValues.builder()
                            .withName(USER_WITH_DIFFERENT_ROLE_IN_DIFFERENT_REALM)
                            .withPassword(CORRECT_PASSWORD)
                            .withValues(ROLE_USER)
                            .build())
                    .withUser(UserWithAttributeValues.builder()
                            .withName(USER_ONLY_IN_AUTHORIZATION)
                            .withPassword(CORRECT_PASSWORD)
                            .build())
                    .build());
            configurableElements.add(AggregateSecurityRealm.builder(AGGREGATE_REALM_SAME_TYPE_NAME)
                    .withAuthenticationRealm(PROPERTIES_REALM_AUTHN_NAME)
                    .withAuthorizationRealm(PROPERTIES_REALM_AUTHZ_NAME)
                    .build());
            configurableElements.add(SimpleSecurityDomain.builder()
                    .withName(AGGREGATE_REALM_SAME_TYPE_NAME)
                    .withDefaultRealm(AGGREGATE_REALM_SAME_TYPE_NAME)
                    .withPermissionMapper("default-permission-mapper")
                    .withRealms(SimpleSecurityDomain.SecurityDomainRealm.builder()
                            .withRealm(AGGREGATE_REALM_SAME_TYPE_NAME)
                            .withRoleDecoder("groups-to-roles")
                            .build())
                    .build());
            configurableElements.add(AggregateSecurityRealm.builder(AGGREGATE_REALM_DIFFERENT_TYPE_NAME)
                    .withAuthenticationRealm(FILESYSTEM_REALM_AUTHN_NAME)
                    .withAuthorizationRealm(PROPERTIES_REALM_AUTHZ_NAME)
                    .build());
            configurableElements.add(SimpleSecurityDomain.builder()
                    .withName(AGGREGATE_REALM_DIFFERENT_TYPE_NAME)
                    .withDefaultRealm(AGGREGATE_REALM_DIFFERENT_TYPE_NAME)
                    .withPermissionMapper("default-permission-mapper")
                    .withRealms(SimpleSecurityDomain.SecurityDomainRealm.builder()
                            .withRealm(AGGREGATE_REALM_DIFFERENT_TYPE_NAME)
                            .withRoleDecoder("groups-to-roles")
                            .build())
                    .build());
            configurableElements.add(UndertowApplicationSecurityDomain.builder()
                              .withName(AGGREGATE_REALM_SAME_TYPE_NAME)
                    .withSecurityDomain(AGGREGATE_REALM_SAME_TYPE_NAME)
                    .build());
            configurableElements.add(UndertowApplicationSecurityDomain.builder()
                              .withName(AGGREGATE_REALM_DIFFERENT_TYPE_NAME)
                    .withSecurityDomain(AGGREGATE_REALM_DIFFERENT_TYPE_NAME)
                    .build());
            return configurableElements.toArray(new ConfigurableElement[configurableElements.size()]);
        }
    }

}
