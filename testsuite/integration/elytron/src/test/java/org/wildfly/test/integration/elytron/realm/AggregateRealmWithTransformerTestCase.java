/*
 * Copyright 2019 Red Hat, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.wildfly.test.integration.elytron.realm;

import org.apache.http.NameValuePair;
import org.apache.http.client.utils.URLEncodedUtils;
import org.apache.http.message.BasicNameValuePair;
import org.hamcrest.MatcherAssert;
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
import org.wildfly.security.permission.ElytronPermission;
import org.wildfly.test.security.common.AbstractElytronSetupTask;
import org.wildfly.test.security.common.elytron.AggregateSecurityRealm;
import org.wildfly.test.security.common.elytron.ConfigurableElement;
import org.wildfly.test.security.common.elytron.FileSystemRealm;
import org.wildfly.test.security.common.elytron.PropertiesRealm;
import org.wildfly.test.security.common.elytron.RegexPrincipalTransformer;
import org.wildfly.test.security.common.elytron.SimpleSecurityDomain;
import org.wildfly.test.security.common.elytron.UserWithAttributeValues;
import org.wildfly.test.security.common.elytron.servlet.AttributePrintingServlet;
import org.wildfly.test.undertow.common.UndertowApplicationSecurityDomain;

import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;

import static jakarta.servlet.http.HttpServletResponse.SC_FORBIDDEN;
import static jakarta.servlet.http.HttpServletResponse.SC_OK;
import static org.hamcrest.CoreMatchers.is;


import static org.jboss.as.test.shared.integration.ejb.security.PermissionUtils.createPermissionsXmlAsset;
import static org.wildfly.test.integration.elytron.realm.AggregateRealmUtil.assertAttribute;
import static org.wildfly.test.integration.elytron.realm.AggregateRealmUtil.assertAuthenticationFailed;
import static org.wildfly.test.integration.elytron.realm.AggregateRealmUtil.assertAuthenticationSuccess;
import static org.wildfly.test.integration.elytron.realm.AggregateRealmUtil.assertInRole;
import static org.wildfly.test.integration.elytron.realm.AggregateRealmUtil.assertNoRoleAssigned;
import static org.wildfly.test.integration.elytron.realm.AggregateRealmUtil.assertNotInRole;
import static org.wildfly.test.integration.elytron.realm.AggregateRealmUtil.getAttributes;
import static org.wildfly.test.integration.elytron.realm.AggregateRealmUtil.prepareRolesPrintingURL;

/**
 * Test scenarios about Elytron Aggregate Realm usage with Principal transformer
 *
 * Most of scenarios are tested for two variants:
 * * Same type of aggregated realms
 * ** Authentication realm uses the same type of realm as authorization realm. Specifically, all realms are filesystem-realm.
 * ** These tests have "_sameTypeRealm" suffix
 * * Different type of aggregated realms
 * ** Authentication realm uses different type of realm then authorization realm. Specifically, authentication realm
 *    is properties-realm and authorization realm is filesystem-realm.
 * ** These tests have "_differentTypeRealm" suffix
 *
 * All these scenarios use aggregated-realm with principal transformer. User A is transformed to user B by principal transformer.
 * Example of the configuration:
 * /subsystem=elytron/regex-principal-transformer=custom-pt:add(pattern=Auser,replacement=Buser)
 * /subsystem=elytron/aggregate-realm=custom-aggregate-realm:add(authentication-realm=elytron-authn-properties-realm,authorization-realm=elytron-authz-realm,principal-transformer=custom-pt)
 *
 * https://issues.jboss.org/browse/WFLY-12202
 * https://issues.jboss.org/browse/WFCORE-4496
 * https://issues.jboss.org/browse/ELY-1829
 */
@RunWith(Arquillian.class)
@RunAsClient
@ServerSetup({AggregateRealmWithTransformerTestCase.SetupTask.class})
public class AggregateRealmWithTransformerTestCase {

    private static final String AGGREGATE_REALM_SAME_TYPE_NAME = "elytron-aggregate-realm-same-type";
    private static final String AGGREGATE_REALM_DIFFERENT_TYPE_NAME = "elytron-aggregate-realm-different-type";
    private static final String AGGREGATE_REALM_ATTRIBUTES_NAME = "elytron-aggregate-realm-attributes";

    private static final String FILESYSTEM_REALM_1_AUTHN_NAME = "elytron-authn-filesystem-realm";
    private static final String PROPERTIES_REALM_AUTHN_NAME = "elytron-authn-properties-realm";
    private static final String FILESYSTEM_REALM_2_AUTHZ_NAME = "elytron-authz-filesystem-realm-1";
    private static final String FILESYSTEM_REALM_3_AUTHZ_NAME = "elytron-authz-filesystem-realm-2";

    private static final String PRINCIPAL_TRANSFORMER = "elytron-custom-principal-transformer";
    private static final String REPLACE_STRING = "AUser";
    private static final String REPLACE_BY = "BUser";

    private static final String A_USER_WITH_ALL = "AUserWithRolesWithAttributesWithPassInAuthz";
    private static final String B_USER_WITH_ALL = "BUserWithRolesWithAttributesWithPassInAuthz";

    private static final String A_USER_WITHOUT_PASS_IN_AUTHZ = "AUserWithoutPassInAuthz";
    private static final String B_USER_WITHOUT_PASS_IN_AUTHZ = "BUserWithoutPassInAuthz";

    private static final String A_USERS_WITHOUT_ROLES_IN_AUTHZ = "AUsersWithoutRolesInAuthz";
    private static final String B_USERS_WITHOUT_ROLES_IN_AUTHZ = "BUsersWithoutRolesInAuthz";
    private static final String A_USER_A_WITHOUT_ROLES_IN_AUTHZ = "AUserAWithoutRolesInAuthz";
    private static final String B_USER_A_WITHOUT_ROLES_IN_AUTHZ = "BUserAWithoutRolesInAuthz";
    private static final String A_USER_B_WITHOUT_ROLES_IN_AUTHZ = "AUserBWithoutRolesInAuthz";
    private static final String B_USER_B_WITHOUT_ROLES_IN_AUTHZ = "BUserBWithoutRolesInAuthz";

    private static final String A_USER_WITHOUT_B_USER_IN_ANY_REALM = "AUserWithoutBUserInAnyRealm";
    private static final String NON_DEFINED_B_USER_WITHOUT_B_USER_IN_ANY_REALM = "BUserWithoutBUserInAnyRealm";
    private static final String A_USER_WITHOUT_B_USER_IN_AUTHZ = "AUserWithoutBUserInAuthz";
    private static final String B_USER_WITHOUT_B_USER_IN_AUTHZ = "BUserWithoutBUserInAuthz";

    private static final String A_1_PASSWORD = "password1";
    private static final String A_2_PASSWORD = "password2";
    private static final String A_3_PASSWORD = "password3";
    private static final String B_1_PASSWORD = "password4";
    private static final String B_2_PASSWORD = "password5";
    private static final String B_3_PASSWORD = "password6";

    public static final String ROLE_1 = "Group1";
    public static final String ROLE_2 = "Group2";
    public static final String ROLE_3 = "Group3";
    public static final String ROLE_4 = "Group4";
    public static final String ROLE_5 = "Group5";
    public static final String ROLE_6 = "Group6";

    private static final String[] ALL_TESTED_ROLES = {ROLE_1, ROLE_2, ROLE_3, ROLE_4, ROLE_5, ROLE_6};

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
        war.addClasses(RoleServlets.class, RoleServlets.Role1Servlet.class, RoleServlets.Role2Servlet.class,
                RoleServlets.Role3Servlet.class, RoleServlets.Role4Servlet.class, RoleServlets.Role5Servlet.class,
                RoleServlets.Role6Servlet.class);
        war.addAsWebInfResource(AggregateRealmWithTransformerTestCase.class.getPackage(), "aggregate-realm-with-transformer-web.xml", "web.xml");
        war.addAsWebInfResource(Utils.getJBossWebXmlAsset(name), "jboss-web.xml");
        return war;
    }

    @Deployment(name = AGGREGATE_REALM_ATTRIBUTES_NAME)
    public static WebArchive deploymentAttributeAggregation() {
        final WebArchive war = ShrinkWrap.create(WebArchive.class, AGGREGATE_REALM_ATTRIBUTES_NAME + ".war");
        war.addClasses(AttributePrintingServlet.class, RolePrintingServlet.class);
        war.addClasses(RoleServlets.class, RoleServlets.Role1Servlet.class, RoleServlets.Role2Servlet.class,
                RoleServlets.Role3Servlet.class, RoleServlets.Role4Servlet.class, RoleServlets.Role5Servlet.class,
                RoleServlets.Role6Servlet.class);
        war.addAsWebInfResource(AggregateRealmWithTransformerTestCase.class.getPackage(), "aggregate-realm-with-transformer-web.xml", "web.xml");
        war.addAsWebInfResource(Utils.getJBossWebXmlAsset(AGGREGATE_REALM_ATTRIBUTES_NAME), "jboss-web.xml");
        war.addAsManifestResource(createPermissionsXmlAsset(
                new ElytronPermission("getSecurityDomain")),
                "permissions.xml");
        return war;
    }

    /**
     * User A is in Group1 group in authentication realm, user B is in Group2 group in authentication realm.
     * User A is in Group3 group in authorization realm, user B is in Group4 group in authorization realm.
     * User A with correct password is authenticated and authorized for Group4 role only.
     */
    @Test
    @OperateOnDeployment(AGGREGATE_REALM_SAME_TYPE_NAME)
    public void userWithOneRole_userInBothRealm_sameTypeRealm(@ArquillianResource URL webAppURL) throws Exception {
        userWithOneRole_userInBothRealm(webAppURL);
    }
    @Test
    @OperateOnDeployment(AGGREGATE_REALM_DIFFERENT_TYPE_NAME)
    public void userWithOneRole_userInBothRealm_differentTypeRealm(@ArquillianResource URL webAppURL) throws Exception {
        userWithOneRole_userInBothRealm(webAppURL);
    }
    private void userWithOneRole_userInBothRealm(URL webAppURL) throws Exception {
        testAssignedRoles(webAppURL, A_USER_WITH_ALL, A_1_PASSWORD, ROLE_4);
        testAssignedRoles(webAppURL, B_USER_WITH_ALL, B_1_PASSWORD, ROLE_4);
        checkRoleServlets(webAppURL.toExternalForm(), A_USER_WITH_ALL, A_1_PASSWORD);
        checkRoleServlets(webAppURL.toExternalForm(), B_USER_WITH_ALL, B_1_PASSWORD);
    }

    /**
     * Realm used for authorization defines different passwords for the users than realm for authentication
     * User A with correct username but with password for A from authorization-realm is not authenticated
     * User A with correct username but with password for B from authorization-realm is not authenticated
     * User A with correct username but with password for B from authentication-realm is not authenticated
     * User A with correct username and with password for A from authentication-realm is authenticated
     */
    @Test
    @OperateOnDeployment(AGGREGATE_REALM_SAME_TYPE_NAME)
    public void principal_differentPasswords_userInBothRealm_sameTypeRealm(@ArquillianResource URL webAppURL) throws Exception {
        principal_differentPasswords_userInBothRealm(webAppURL);
    }
    @Test
    @OperateOnDeployment(AGGREGATE_REALM_DIFFERENT_TYPE_NAME)
    public void principal_differentPasswords_userInBothRealm_differentTypeRealm(@ArquillianResource URL webAppURL) throws Exception {
        principal_differentPasswords_userInBothRealm(webAppURL);
    }
    private void principal_differentPasswords_userInBothRealm(URL webAppURL) throws Exception {
        // User A
        assertAuthenticationSuccess(webAppURL, A_USER_WITH_ALL, A_1_PASSWORD, QUERY_ROLES);
        assertAuthenticationFailed(webAppURL, A_USER_WITH_ALL, A_2_PASSWORD, QUERY_ROLES);
        assertAuthenticationFailed(webAppURL, A_USER_WITH_ALL, B_1_PASSWORD, QUERY_ROLES);
        assertAuthenticationFailed(webAppURL, A_USER_WITH_ALL, B_2_PASSWORD, QUERY_ROLES);
        // User B
        assertAuthenticationFailed(webAppURL, B_USER_WITH_ALL, A_1_PASSWORD, QUERY_ROLES);
        assertAuthenticationFailed(webAppURL, B_USER_WITH_ALL, A_2_PASSWORD, QUERY_ROLES);
        assertAuthenticationSuccess(webAppURL, B_USER_WITH_ALL, B_1_PASSWORD, QUERY_ROLES);
        assertAuthenticationFailed(webAppURL, B_USER_WITH_ALL, B_2_PASSWORD, QUERY_ROLES);
    }

    /**
     * Realm for authorization does not include user's (both A and B) password
     * User A with correct username and password is authenticated
     */
    @Test
    @OperateOnDeployment(AGGREGATE_REALM_SAME_TYPE_NAME)
    public void principal_correctPassword_userOnlyInAuthzRealm_sameTypeRealm(@ArquillianResource URL webAppURL) throws Exception {
        principal_correctPassword_userOnlyInAuthzRealm(webAppURL);
    }
    @Test
    @OperateOnDeployment(AGGREGATE_REALM_DIFFERENT_TYPE_NAME)
    public void principal_correctPassword_userOnlyInAuthzRealm_differentTypeRealm(@ArquillianResource URL webAppURL) throws Exception {
        principal_correctPassword_userOnlyInAuthzRealm(webAppURL);
    }
    private void principal_correctPassword_userOnlyInAuthzRealm(URL webAppURL) throws Exception {
        // Roles
        testAssignedRoles(webAppURL, A_USER_WITHOUT_PASS_IN_AUTHZ, A_1_PASSWORD, ROLE_4);
        testAssignedRoles(webAppURL, B_USER_WITHOUT_PASS_IN_AUTHZ, B_1_PASSWORD, ROLE_4);
        checkRoleServlets(webAppURL.toExternalForm(), A_USER_WITHOUT_PASS_IN_AUTHZ, A_1_PASSWORD);
        checkRoleServlets(webAppURL.toExternalForm(), B_USER_WITHOUT_PASS_IN_AUTHZ, B_1_PASSWORD);
        // User A
        assertAuthenticationSuccess(webAppURL, A_USER_WITHOUT_PASS_IN_AUTHZ, A_1_PASSWORD, QUERY_ROLES);
        assertAuthenticationFailed(webAppURL, A_USER_WITHOUT_PASS_IN_AUTHZ, A_2_PASSWORD, QUERY_ROLES);
        assertAuthenticationFailed(webAppURL, A_USER_WITHOUT_PASS_IN_AUTHZ, B_1_PASSWORD, QUERY_ROLES);
        assertAuthenticationFailed(webAppURL, A_USER_WITHOUT_PASS_IN_AUTHZ, B_2_PASSWORD, QUERY_ROLES);
        // User B
        assertAuthenticationFailed(webAppURL, B_USER_WITHOUT_PASS_IN_AUTHZ, A_1_PASSWORD, QUERY_ROLES);
        assertAuthenticationFailed(webAppURL, B_USER_WITHOUT_PASS_IN_AUTHZ, A_2_PASSWORD, QUERY_ROLES);
        assertAuthenticationSuccess(webAppURL, B_USER_WITHOUT_PASS_IN_AUTHZ, B_1_PASSWORD, QUERY_ROLES);
        assertAuthenticationFailed(webAppURL, B_USER_WITHOUT_PASS_IN_AUTHZ, B_2_PASSWORD, QUERY_ROLES);
    }

    /**
     * No roles in authorization realm. User with correct password is authenticated but not authorized.
     */
    @Test
    @OperateOnDeployment(AGGREGATE_REALM_SAME_TYPE_NAME)
    public void principal_usersWithNoRoles_userInBothRealm_sameTypeRealm(@ArquillianResource URL webAppURL) throws Exception {
        principal_usersWithNoRoles_userInBothRealm(webAppURL);
    }
    @Test
    @OperateOnDeployment(AGGREGATE_REALM_DIFFERENT_TYPE_NAME)
    public void principal_usersWithNoRoles_userInBothRealm_differentTypeRealm(@ArquillianResource URL webAppURL) throws Exception {
        principal_usersWithNoRoles_userInBothRealm(webAppURL);
    }
    private void principal_usersWithNoRoles_userInBothRealm(URL webAppURL) throws Exception {
        // User A
        assertNoRoleAssigned(webAppURL, A_USERS_WITHOUT_ROLES_IN_AUTHZ, A_1_PASSWORD, QUERY_ROLES);
        assertAuthenticationFailed(webAppURL, A_USERS_WITHOUT_ROLES_IN_AUTHZ, A_2_PASSWORD, QUERY_ROLES);
        assertAuthenticationFailed(webAppURL, A_USERS_WITHOUT_ROLES_IN_AUTHZ, B_1_PASSWORD, QUERY_ROLES);
        assertAuthenticationFailed(webAppURL, A_USERS_WITHOUT_ROLES_IN_AUTHZ, B_2_PASSWORD, QUERY_ROLES);
        // User B
        assertAuthenticationFailed(webAppURL, B_USERS_WITHOUT_ROLES_IN_AUTHZ, A_1_PASSWORD, QUERY_ROLES);
        assertAuthenticationFailed(webAppURL, B_USERS_WITHOUT_ROLES_IN_AUTHZ, A_2_PASSWORD, QUERY_ROLES);
        assertNoRoleAssigned(webAppURL, B_USERS_WITHOUT_ROLES_IN_AUTHZ, B_1_PASSWORD, QUERY_ROLES);
        assertAuthenticationFailed(webAppURL, B_USERS_WITHOUT_ROLES_IN_AUTHZ, B_2_PASSWORD, QUERY_ROLES);
    }

    /**
     * No roles for user B in authorization realm.
     * One role for user A in authorization realm.
     * User A with correct password is authenticated but not authorized.
     */
    @Test
    @OperateOnDeployment(AGGREGATE_REALM_SAME_TYPE_NAME)
    public void principal_userBWithNoRoles_userInBothRealm_sameTypeRealm(@ArquillianResource URL webAppURL) throws Exception {
        principal_userBWithNoRoles_userInBothRealm(webAppURL);
    }
    @Test
    @OperateOnDeployment(AGGREGATE_REALM_DIFFERENT_TYPE_NAME)
    public void principal_userBWithNoRoles_userInBothRealm_differentTypeRealm(@ArquillianResource URL webAppURL) throws Exception {
        principal_userBWithNoRoles_userInBothRealm(webAppURL);
    }
    private void principal_userBWithNoRoles_userInBothRealm(URL webAppURL) throws Exception {
        // User A
        assertNoRoleAssigned(webAppURL, A_USER_B_WITHOUT_ROLES_IN_AUTHZ, A_1_PASSWORD, QUERY_ROLES);
        assertAuthenticationFailed(webAppURL, A_USER_B_WITHOUT_ROLES_IN_AUTHZ, A_2_PASSWORD, QUERY_ROLES);
        assertAuthenticationFailed(webAppURL, A_USER_B_WITHOUT_ROLES_IN_AUTHZ, B_1_PASSWORD, QUERY_ROLES);
        assertAuthenticationFailed(webAppURL, A_USER_B_WITHOUT_ROLES_IN_AUTHZ, B_2_PASSWORD, QUERY_ROLES);
        // User B
        assertAuthenticationFailed(webAppURL, B_USER_B_WITHOUT_ROLES_IN_AUTHZ, A_1_PASSWORD, QUERY_ROLES);
        assertAuthenticationFailed(webAppURL, B_USER_B_WITHOUT_ROLES_IN_AUTHZ, A_2_PASSWORD, QUERY_ROLES);
        assertNoRoleAssigned(webAppURL, B_USER_B_WITHOUT_ROLES_IN_AUTHZ, B_1_PASSWORD, QUERY_ROLES);
        assertAuthenticationFailed(webAppURL, B_USER_B_WITHOUT_ROLES_IN_AUTHZ, B_2_PASSWORD, QUERY_ROLES);
    }

    /**
     * No roles for user A in authorization realm.
     * One role for user B in authorization realm.
     * User A with correct password is authenticated and authorized.
     */
    @Test
    @OperateOnDeployment(AGGREGATE_REALM_SAME_TYPE_NAME)
    public void principal_userAWithNoRoles_userInBothRealm_sameTypeRealm(@ArquillianResource URL webAppURL) throws Exception {
        principal_userAWithNoRoles_userInBothRealm(webAppURL);
    }
    @Test
    @OperateOnDeployment(AGGREGATE_REALM_DIFFERENT_TYPE_NAME)
    public void principal_userAWithNoRoles_userInBothRealm_differentTypeRealm(@ArquillianResource URL webAppURL) throws Exception {
        principal_userAWithNoRoles_userInBothRealm(webAppURL);
    }
    private void principal_userAWithNoRoles_userInBothRealm(URL webAppURL) throws Exception {
        // Roles
        testAssignedRoles(webAppURL, A_USER_A_WITHOUT_ROLES_IN_AUTHZ, A_1_PASSWORD, ROLE_4);
        testAssignedRoles(webAppURL, B_USER_A_WITHOUT_ROLES_IN_AUTHZ, B_1_PASSWORD, ROLE_4);
        checkRoleServlets(webAppURL.toExternalForm(), A_USER_A_WITHOUT_ROLES_IN_AUTHZ, A_1_PASSWORD);
        checkRoleServlets(webAppURL.toExternalForm(), B_USER_A_WITHOUT_ROLES_IN_AUTHZ, B_1_PASSWORD);
        // User A
        assertAuthenticationSuccess(webAppURL, A_USER_A_WITHOUT_ROLES_IN_AUTHZ, A_1_PASSWORD, QUERY_ROLES);
        assertAuthenticationFailed(webAppURL, A_USER_A_WITHOUT_ROLES_IN_AUTHZ, A_2_PASSWORD, QUERY_ROLES);
        assertAuthenticationFailed(webAppURL, A_USER_A_WITHOUT_ROLES_IN_AUTHZ, B_1_PASSWORD, QUERY_ROLES);
        assertAuthenticationFailed(webAppURL, A_USER_A_WITHOUT_ROLES_IN_AUTHZ, B_2_PASSWORD, QUERY_ROLES);
        // User B
        assertAuthenticationFailed(webAppURL, B_USER_A_WITHOUT_ROLES_IN_AUTHZ, A_1_PASSWORD, QUERY_ROLES);
        assertAuthenticationFailed(webAppURL, B_USER_A_WITHOUT_ROLES_IN_AUTHZ, A_2_PASSWORD, QUERY_ROLES);
        assertAuthenticationSuccess(webAppURL, B_USER_A_WITHOUT_ROLES_IN_AUTHZ, B_1_PASSWORD, QUERY_ROLES);
        assertAuthenticationFailed(webAppURL, B_USER_A_WITHOUT_ROLES_IN_AUTHZ, B_2_PASSWORD, QUERY_ROLES);
    }

    /**
     * B is not defined in authorization realm, but B is defined in authentication realm.
     * User A is authenticated with correct password.
     * User with correct password is authenticated but not authorized.
     */
    @Test
    @OperateOnDeployment(AGGREGATE_REALM_SAME_TYPE_NAME)
    public void principal_transformedUserIsNotDefinedInAuthorizationRealm_sameTypeRealm(@ArquillianResource URL webAppURL) throws Exception {
        principal_transformedUserIsNotDefinedInAuthorizationRealm(webAppURL);
    }
    @Test
    @OperateOnDeployment(AGGREGATE_REALM_DIFFERENT_TYPE_NAME)
    public void principal_transformedUserIsNotDefinedInAuthorizationRealm_differentTypeRealm(@ArquillianResource URL webAppURL) throws Exception {
        principal_transformedUserIsNotDefinedInAuthorizationRealm(webAppURL);
    }
    private void principal_transformedUserIsNotDefinedInAuthorizationRealm(URL webAppURL) throws Exception {
        // User A
        assertNoRoleAssigned(webAppURL, A_USER_WITHOUT_B_USER_IN_AUTHZ, A_1_PASSWORD, QUERY_ROLES);
        assertAuthenticationFailed(webAppURL, A_USER_WITHOUT_B_USER_IN_AUTHZ, A_2_PASSWORD, QUERY_ROLES);
        assertAuthenticationFailed(webAppURL, A_USER_WITHOUT_B_USER_IN_AUTHZ, B_1_PASSWORD, QUERY_ROLES);
        assertAuthenticationFailed(webAppURL, A_USER_WITHOUT_B_USER_IN_AUTHZ, B_2_PASSWORD, QUERY_ROLES);
        // User B
        assertAuthenticationFailed(webAppURL, B_USER_WITHOUT_B_USER_IN_AUTHZ, A_1_PASSWORD, QUERY_ROLES);
        assertAuthenticationFailed(webAppURL, B_USER_WITHOUT_B_USER_IN_AUTHZ, A_2_PASSWORD, QUERY_ROLES);
        assertNoRoleAssigned(webAppURL, B_USER_WITHOUT_B_USER_IN_AUTHZ, B_1_PASSWORD, QUERY_ROLES);
        assertAuthenticationFailed(webAppURL, B_USER_WITHOUT_B_USER_IN_AUTHZ, B_2_PASSWORD, QUERY_ROLES);
    }

    /**
     * B is not defined in both authorization realm and authentication realm
     * User A is authenticated with correct password.
     * User with correct password is authenticated but not authorized
     */
    @Test
    @OperateOnDeployment(AGGREGATE_REALM_SAME_TYPE_NAME)
    public void principal_transformedUserIsNotDefinedInAnyRealm_sameTypeRealm(@ArquillianResource URL webAppURL) throws Exception {
        principal_transformedUserIsNotDefinedInAnyRealm(webAppURL);
    }
    @Test
    @OperateOnDeployment(AGGREGATE_REALM_DIFFERENT_TYPE_NAME)
    public void principal_transformedUserIsNotDefinedInAnyRealm_differentTypeRealm(@ArquillianResource URL webAppURL) throws Exception {
        principal_transformedUserIsNotDefinedInAnyRealm(webAppURL);
    }
    private void principal_transformedUserIsNotDefinedInAnyRealm(URL webAppURL) throws Exception {
        // User A
        assertNoRoleAssigned(webAppURL, A_USER_WITHOUT_B_USER_IN_ANY_REALM, A_1_PASSWORD, QUERY_ROLES);
        assertAuthenticationFailed(webAppURL, A_USER_WITHOUT_B_USER_IN_ANY_REALM, A_2_PASSWORD, QUERY_ROLES);
        assertAuthenticationFailed(webAppURL, A_USER_WITHOUT_B_USER_IN_ANY_REALM, B_1_PASSWORD, QUERY_ROLES);
        assertAuthenticationFailed(webAppURL, A_USER_WITHOUT_B_USER_IN_ANY_REALM, B_2_PASSWORD, QUERY_ROLES);
        // User B
        assertAuthenticationFailed(webAppURL, NON_DEFINED_B_USER_WITHOUT_B_USER_IN_ANY_REALM, A_1_PASSWORD, QUERY_ROLES);
        assertAuthenticationFailed(webAppURL, NON_DEFINED_B_USER_WITHOUT_B_USER_IN_ANY_REALM, A_2_PASSWORD, QUERY_ROLES);
        assertAuthenticationFailed(webAppURL, NON_DEFINED_B_USER_WITHOUT_B_USER_IN_ANY_REALM, B_1_PASSWORD, QUERY_ROLES);
        assertAuthenticationFailed(webAppURL, NON_DEFINED_B_USER_WITHOUT_B_USER_IN_ANY_REALM, B_2_PASSWORD, QUERY_ROLES);
    }

    /**
     * User A with correct username and password is authenticated, user A has attributes of B user from both authorization realms.
     * A user doesn't have any attribute of A user from any authorization realm.
     * All authorization realms are filesystem-realms (same approach as original tests from AggregateRealmTestCase)
     */
    @Test
    @OperateOnDeployment(AGGREGATE_REALM_ATTRIBUTES_NAME)
    public void principal_twoAuthzRealms(@ArquillianResource URL webAppURL) throws Exception {
        // roles
        testAssignedRoles(webAppURL, A_USER_WITH_ALL, A_1_PASSWORD, ROLE_4);
        testAssignedRoles(webAppURL, B_USER_WITH_ALL, B_1_PASSWORD, ROLE_4);
        checkRoleServlets(webAppURL.toExternalForm(), A_USER_WITH_ALL, A_1_PASSWORD);
        checkRoleServlets(webAppURL.toExternalForm(), B_USER_WITH_ALL, B_1_PASSWORD);
        // User A authentication
        assertAuthenticationSuccess(webAppURL, A_USER_WITH_ALL, A_1_PASSWORD, QUERY_ROLES);
        assertAuthenticationFailed(webAppURL, A_USER_WITH_ALL, A_2_PASSWORD, QUERY_ROLES);
        assertAuthenticationFailed(webAppURL, A_USER_WITH_ALL, B_1_PASSWORD, QUERY_ROLES);
        assertAuthenticationFailed(webAppURL, A_USER_WITH_ALL, B_2_PASSWORD, QUERY_ROLES);
        // User B authentication
        assertAuthenticationFailed(webAppURL, B_USER_WITH_ALL, A_1_PASSWORD, QUERY_ROLES);
        assertAuthenticationFailed(webAppURL, B_USER_WITH_ALL, A_2_PASSWORD, QUERY_ROLES);
        assertAuthenticationSuccess(webAppURL, B_USER_WITH_ALL, B_1_PASSWORD, QUERY_ROLES);
        assertAuthenticationFailed(webAppURL, B_USER_WITH_ALL, B_2_PASSWORD, QUERY_ROLES);
        // check attributes
        assertAttributes(webAppURL, A_USER_WITH_ALL, A_1_PASSWORD);
        assertAttributes(webAppURL, B_USER_WITH_ALL, B_1_PASSWORD);
    }

    private void checkRoleServlets(String webAppURL, String user, String password) throws Exception {
        Utils.makeCallWithBasicAuthn(new URL(webAppURL + ROLE_1), user, password, SC_FORBIDDEN);
        Utils.makeCallWithBasicAuthn(new URL(webAppURL + ROLE_2), user, password, SC_FORBIDDEN);
        Utils.makeCallWithBasicAuthn(new URL(webAppURL + ROLE_3), user, password, SC_FORBIDDEN);
        Utils.makeCallWithBasicAuthn(new URL(webAppURL + ROLE_4), user, password, SC_OK);
        Utils.makeCallWithBasicAuthn(new URL(webAppURL + ROLE_5), user, password, SC_FORBIDDEN);
        Utils.makeCallWithBasicAuthn(new URL(webAppURL + ROLE_6), user, password, SC_FORBIDDEN);
    }

    private void assertAttributes(URL webAppURL, String user, String password) throws Exception {
        Properties properties = getAttributes(webAppURL, user, password);
        MatcherAssert.assertThat("Properties count", properties.size(), is(3));
        assertAttribute(properties, "groups", ROLE_4);
        assertAttribute(properties, "Attribute1", "3", "4");
        assertAttribute(properties, "Attribute2", "7", "8");
    }

    private void testAssignedRoles(URL webAppURL, String username, String password, String... assignedRoles) throws Exception {
        final URL rolesPrintingURL = prepareRolesPrintingURL(webAppURL, QUERY_ROLES);
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

    static class SetupTask extends AbstractElytronSetupTask {
        @Override
        protected ConfigurableElement[] getConfigurableElements() {
            ArrayList<ConfigurableElement> configurableElements = new ArrayList<>();

            // prepare principal transcormer
            configurableElements.add(RegexPrincipalTransformer.builder(PRINCIPAL_TRANSFORMER)
                    .withPattern(REPLACE_STRING)
                    .withReplacement(REPLACE_BY)
                    .build());

            // properties-realm realm for authentication
            configurableElements.add(PropertiesRealm.builder()
                    .withName(PROPERTIES_REALM_AUTHN_NAME)
                    .withUser(UserWithAttributeValues.builder()
                            .withName(A_USER_WITH_ALL)
                            .withPassword(A_1_PASSWORD)
                            .withValues(ROLE_1)
                            .build())
                    .withUser(UserWithAttributeValues.builder()
                            .withName(B_USER_WITH_ALL)
                            .withPassword(B_1_PASSWORD)
                            .withValues(ROLE_2)
                            .build())
                    // for principal_correctPassword_userOnlyInAuthzRealm_sameTypeRealm()
                    //     and principal_correctPassword_userOnlyInAuthzRealm_differentTypeRealm()
                    .withUser(UserWithAttributeValues.builder()
                            .withName(A_USER_WITHOUT_PASS_IN_AUTHZ)
                            .withPassword(A_1_PASSWORD)
                            .withValues(ROLE_1)
                            .build())
                    .withUser(UserWithAttributeValues.builder()
                            .withName(B_USER_WITHOUT_PASS_IN_AUTHZ)
                            .withPassword(B_1_PASSWORD)
                            .withValues(ROLE_2)
                            .build())
                    // for principal_usersWithNoRoles_userInBothRealm_sameTypeRealm()
                    //     and principal_usersWithNoRoles_userInBothRealm_differentTypeRealm()
                    .withUser(UserWithAttributeValues.builder()
                            .withName(A_USERS_WITHOUT_ROLES_IN_AUTHZ)
                            .withPassword(A_1_PASSWORD)
                            .withValues(ROLE_1)
                            .build())
                    .withUser(UserWithAttributeValues.builder()
                            .withName(B_USERS_WITHOUT_ROLES_IN_AUTHZ)
                            .withPassword(B_1_PASSWORD)
                            .withValues(ROLE_2)
                            .build())
                    // for principal_userAWithNoRoles_userInBothRealm_sameTypeRealm()
                    //     and principal_userAWithNoRoles_userInBothRealm_differentTypeRealm()
                    .withUser(UserWithAttributeValues.builder()
                            .withName(A_USER_A_WITHOUT_ROLES_IN_AUTHZ)
                            .withPassword(A_1_PASSWORD)
                            .withValues(ROLE_1)
                            .build())
                    .withUser(UserWithAttributeValues.builder()
                            .withName(B_USER_A_WITHOUT_ROLES_IN_AUTHZ)
                            .withPassword(B_1_PASSWORD)
                            .withValues(ROLE_2)
                            .build())
                    // for principal_userBWithNoRoles_userInBothRealm_sameTypeRealm()
                    //     and principal_userBWithNoRoles_userInBothRealm_differentTypeRealm()
                    .withUser(UserWithAttributeValues.builder()
                            .withName(A_USER_B_WITHOUT_ROLES_IN_AUTHZ)
                            .withPassword(A_1_PASSWORD)
                            .withValues(ROLE_1)
                            .build())
                    .withUser(UserWithAttributeValues.builder()
                            .withName(B_USER_B_WITHOUT_ROLES_IN_AUTHZ)
                            .withPassword(B_1_PASSWORD)
                            .withValues(ROLE_2)
                            .build())
                    // for principal_transformedUserIsNotDefinedInAuthorizationRealm_sameTypeRealm()
                    //     and principal_transformedUserIsNotDefinedInAuthorizationRealm_differentTypeRealm()
                    .withUser(UserWithAttributeValues.builder()
                            .withName(A_USER_WITHOUT_B_USER_IN_AUTHZ)
                            .withPassword(A_1_PASSWORD)
                            .withValues(ROLE_1)
                            .build())
                    .withUser(UserWithAttributeValues.builder()
                            .withName(B_USER_WITHOUT_B_USER_IN_AUTHZ)
                            .withPassword(B_1_PASSWORD)
                            .withValues(ROLE_2)
                            .build())
                    // for principal_transformedUserIsNotDefinedInAnyRealm_sameTypeRealm()
                    //     and principal_transformedUserIsNotDefinedInAnyRealm_differentTypeRealm()
                    .withUser(UserWithAttributeValues.builder()
                            .withName(A_USER_WITHOUT_B_USER_IN_ANY_REALM)
                            .withPassword(A_1_PASSWORD)
                            .withValues(ROLE_1)
                            .build())
                    .build());

            // filesystem-realm realm for authentication
            configurableElements.add(FileSystemRealm.builder()
                    .withName(FILESYSTEM_REALM_1_AUTHN_NAME)
                    .withUser(UserWithAttributeValues.builder()
                            .withName(A_USER_WITH_ALL)
                            .withPassword(A_1_PASSWORD)
                            .withValues(ROLE_1)
                            .build())
                    .withUser(UserWithAttributeValues.builder()
                            .withName(B_USER_WITH_ALL)
                            .withPassword(B_1_PASSWORD)
                            .withValues(ROLE_2)
                            .build())
                    // for principal_correctPassword_userOnlyInAuthzRealm_sameTypeRealm()
                    //     and principal_correctPassword_userOnlyInAuthzRealm_differentTypeRealm()
                    .withUser(UserWithAttributeValues.builder()
                            .withName(A_USER_WITHOUT_PASS_IN_AUTHZ)
                            .withPassword(A_1_PASSWORD)
                            .withValues(ROLE_1)
                            .build())
                    .withUser(UserWithAttributeValues.builder()
                            .withName(B_USER_WITHOUT_PASS_IN_AUTHZ)
                            .withPassword(B_1_PASSWORD)
                            .withValues(ROLE_2)
                            .build())
                    // for principal_usersWithNoRoles_userInBothRealm_sameTypeRealm()
                    //     and principal_usersWithNoRoles_userInBothRealm_differentTypeRealm()
                    .withUser(UserWithAttributeValues.builder()
                            .withName(A_USERS_WITHOUT_ROLES_IN_AUTHZ)
                            .withPassword(A_1_PASSWORD)
                            .withValues(ROLE_1)
                            .build())
                    .withUser(UserWithAttributeValues.builder()
                            .withName(B_USERS_WITHOUT_ROLES_IN_AUTHZ)
                            .withPassword(B_1_PASSWORD)
                            .withValues(ROLE_2)
                            .build())
                    // for principal_userAWithNoRoles_userInBothRealm_sameTypeRealm()
                    //     and principal_userAWithNoRoles_userInBothRealm_differentTypeRealm()
                    .withUser(UserWithAttributeValues.builder()
                            .withName(A_USER_A_WITHOUT_ROLES_IN_AUTHZ)
                            .withPassword(A_1_PASSWORD)
                            .withValues(ROLE_1)
                            .build())
                    .withUser(UserWithAttributeValues.builder()
                            .withName(B_USER_A_WITHOUT_ROLES_IN_AUTHZ)
                            .withPassword(B_1_PASSWORD)
                            .withValues(ROLE_2)
                            .build())
                    // for principal_userBWithNoRoles_userInBothRealm_sameTypeRealm()
                    //     and principal_userBWithNoRoles_userInBothRealm_differentTypeRealm()
                    .withUser(UserWithAttributeValues.builder()
                            .withName(A_USER_B_WITHOUT_ROLES_IN_AUTHZ)
                            .withPassword(A_1_PASSWORD)
                            .withValues(ROLE_1)
                            .build())
                    .withUser(UserWithAttributeValues.builder()
                            .withName(B_USER_B_WITHOUT_ROLES_IN_AUTHZ)
                            .withPassword(B_1_PASSWORD)
                            .withValues(ROLE_2)
                            .build())
                    // for principal_transformedUserIsNotDefinedInAuthorizationRealm_sameTypeRealm()
                    //     and principal_transformedUserIsNotDefinedInAuthorizationRealm_differentTypeRealm()
                    .withUser(UserWithAttributeValues.builder()
                            .withName(A_USER_WITHOUT_B_USER_IN_AUTHZ)
                            .withPassword(A_1_PASSWORD)
                            .withValues(ROLE_1)
                            .build())
                    .withUser(UserWithAttributeValues.builder()
                            .withName(B_USER_WITHOUT_B_USER_IN_AUTHZ)
                            .withPassword(B_1_PASSWORD)
                            .withValues(ROLE_2)
                            .build())
                    // for principal_transformedUserIsNotDefinedInAnyRealm_sameTypeRealm()
                    //     and principal_transformedUserIsNotDefinedInAnyRealm_differentTypeRealm()
                    .withUser(UserWithAttributeValues.builder()
                            .withName(A_USER_WITHOUT_B_USER_IN_ANY_REALM)
                            .withPassword(A_1_PASSWORD)
                            .withValues(ROLE_1)
                            .build())
                    .build());

            // filesystem-realm realm for authorization
            configurableElements.add(FileSystemRealm.builder()
                    .withName(FILESYSTEM_REALM_2_AUTHZ_NAME)
                    .withUser(UserWithAttributeValues.builder()
                            .withName(A_USER_WITH_ALL)
                            .withPassword(A_2_PASSWORD)
                            .withValues(ROLE_3)
                            .build())
                    .withUser(UserWithAttributeValues.builder()
                            .withName(B_USER_WITH_ALL)
                            .withPassword(B_2_PASSWORD)
                            .withValues(ROLE_4)
                            .build())
                    // for principal_correctPassword_userOnlyInAuthzRealm_sameTypeRealm()
                    //     and principal_correctPassword_userOnlyInAuthzRealm_differentTypeRealm()
                    .withUser(UserWithAttributeValues.builder()
                            .withName(A_USER_WITHOUT_PASS_IN_AUTHZ)
                            .withValues(ROLE_3)
                            .build())
                    .withUser(UserWithAttributeValues.builder()
                            .withName(B_USER_WITHOUT_PASS_IN_AUTHZ)
                            .withValues(ROLE_4)
                            .build())
                    // for principal_usersWithNoRoles_userInBothRealm_sameTypeRealm()
                    //     and principal_usersWithNoRoles_userInBothRealm_differentTypeRealm()
                    .withUser(UserWithAttributeValues.builder()
                            .withName(A_USERS_WITHOUT_ROLES_IN_AUTHZ)
                            .withPassword(A_2_PASSWORD)
                            .build())
                    .withUser(UserWithAttributeValues.builder()
                            .withName(B_USERS_WITHOUT_ROLES_IN_AUTHZ)
                            .withPassword(B_2_PASSWORD)
                            .build())
                    // for principal_userAWithNoRoles_userInBothRealm_sameTypeRealm()
                    //     and principal_userAWithNoRoles_userInBothRealm_differentTypeRealm()
                    .withUser(UserWithAttributeValues.builder()
                            .withName(A_USER_A_WITHOUT_ROLES_IN_AUTHZ)
                            .withPassword(A_2_PASSWORD)
                            .build())
                    .withUser(UserWithAttributeValues.builder()
                            .withName(B_USER_A_WITHOUT_ROLES_IN_AUTHZ)
                            .withPassword(B_2_PASSWORD)
                            .withValues(ROLE_4)
                            .build())
                    // for principal_userBWithNoRoles_userInBothRealm_sameTypeRealm()
                    //     and principal_userBWithNoRoles_userInBothRealm_differentTypeRealm()
                    .withUser(UserWithAttributeValues.builder()
                            .withName(A_USER_B_WITHOUT_ROLES_IN_AUTHZ)
                            .withPassword(A_2_PASSWORD)
                            .withValues(ROLE_3)
                            .build())
                    .withUser(UserWithAttributeValues.builder()
                            .withName(B_USER_B_WITHOUT_ROLES_IN_AUTHZ)
                            .withPassword(B_2_PASSWORD)
                            .build())
                    .build());
            configurableElements.add(new AggregateRealmUtil.CustomFSAttributes(FILESYSTEM_REALM_2_AUTHZ_NAME, A_USER_WITH_ALL, "Attribute1", "1", "2"));
            configurableElements.add(new AggregateRealmUtil.CustomFSAttributes(FILESYSTEM_REALM_2_AUTHZ_NAME, B_USER_WITH_ALL, "Attribute1", "3", "4"));

            // second file-system realm for authorization, used in attribute tests
            configurableElements.add(FileSystemRealm.builder()
                    .withName(FILESYSTEM_REALM_3_AUTHZ_NAME)
                    .withUser(UserWithAttributeValues.builder()
                            .withName(A_USER_WITH_ALL)
                            .withPassword(A_3_PASSWORD)
                            .withValues(ROLE_5)
                            .build())
                    .withUser(UserWithAttributeValues.builder()
                            .withName(B_USER_WITH_ALL)
                            .withPassword(B_3_PASSWORD)
                            .withValues(ROLE_6)
                            .build())
                    .build());
            configurableElements.add(new AggregateRealmUtil.CustomFSAttributes(FILESYSTEM_REALM_3_AUTHZ_NAME, A_USER_WITH_ALL, "Attribute2", "5", "6"));
            configurableElements.add(new AggregateRealmUtil.CustomFSAttributes(FILESYSTEM_REALM_3_AUTHZ_NAME, B_USER_WITH_ALL, "Attribute2", "7", "8"));

            // aggregate security realms
            configurableElements.add(AggregateSecurityRealm.builder(AGGREGATE_REALM_SAME_TYPE_NAME)
                    .withAuthenticationRealm(FILESYSTEM_REALM_1_AUTHN_NAME)
                    .withAuthorizationRealm(FILESYSTEM_REALM_2_AUTHZ_NAME)
                    .withPrincipalTransformer(PRINCIPAL_TRANSFORMER)
                    .build());
            configurableElements.add(AggregateSecurityRealm.builder(AGGREGATE_REALM_DIFFERENT_TYPE_NAME)
                    .withAuthenticationRealm(PROPERTIES_REALM_AUTHN_NAME)
                    .withAuthorizationRealm(FILESYSTEM_REALM_2_AUTHZ_NAME)
                    .withPrincipalTransformer(PRINCIPAL_TRANSFORMER)
                    .build());
            configurableElements.add(AggregateSecurityRealm.builder(AGGREGATE_REALM_ATTRIBUTES_NAME)
                    .withAuthenticationRealm(FILESYSTEM_REALM_1_AUTHN_NAME)
                    .withAuthorizationRealms(FILESYSTEM_REALM_2_AUTHZ_NAME, FILESYSTEM_REALM_3_AUTHZ_NAME)
                    .withPrincipalTransformer(PRINCIPAL_TRANSFORMER)
                    .build());

            // SimpleSecurityDomain
            configurableElements.add(SimpleSecurityDomain.builder()
                    .withName(AGGREGATE_REALM_SAME_TYPE_NAME)
                    .withDefaultRealm(AGGREGATE_REALM_SAME_TYPE_NAME)
                    .withPermissionMapper("default-permission-mapper")
                    .withRealms(SimpleSecurityDomain.SecurityDomainRealm.builder()
                            .withRealm(AGGREGATE_REALM_SAME_TYPE_NAME)
                            .withRoleDecoder("groups-to-roles")
                            .build())
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
            configurableElements.add(SimpleSecurityDomain.builder()
                    .withName(AGGREGATE_REALM_ATTRIBUTES_NAME)
                    .withDefaultRealm(AGGREGATE_REALM_ATTRIBUTES_NAME)
                    .withPermissionMapper("default-permission-mapper")
                    .withRealms(SimpleSecurityDomain.SecurityDomainRealm.builder()
                            .withRealm(AGGREGATE_REALM_ATTRIBUTES_NAME)
                            .withRoleDecoder("groups-to-roles")
                            .build())
                    .build());

            // Undertow application security domain
            configurableElements.add(UndertowApplicationSecurityDomain.builder()
                              .withName(AGGREGATE_REALM_SAME_TYPE_NAME)
                    .withSecurityDomain(AGGREGATE_REALM_SAME_TYPE_NAME)
                    .build());
            configurableElements.add(UndertowApplicationSecurityDomain.builder()
                              .withName(AGGREGATE_REALM_DIFFERENT_TYPE_NAME)
                    .withSecurityDomain(AGGREGATE_REALM_DIFFERENT_TYPE_NAME)
                    .build());
            configurableElements.add(UndertowApplicationSecurityDomain.builder()
                              .withName(AGGREGATE_REALM_ATTRIBUTES_NAME)
                    .withSecurityDomain(AGGREGATE_REALM_ATTRIBUTES_NAME)
                    .build());

            return configurableElements.toArray(new ConfigurableElement[configurableElements.size()]);
        }
    }
}
