/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2012, Red Hat, Inc., and individual contributors
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
package org.jboss.as.test.integration.security.loginmodules;

import static org.jboss.as.test.integration.security.common.BasicVaultServerSetupTask.ATTRIBUTE_NAME;
import static org.jboss.as.test.integration.security.common.BasicVaultServerSetupTask.VAULT_BLOCK;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.naming.Context;
import javax.security.auth.login.LoginException;

import org.apache.commons.lang.StringUtils;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.OperateOnDeployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.as.arquillian.api.ServerSetup;
import org.jboss.as.arquillian.api.ServerSetupTask;
import org.jboss.as.test.categories.CommonCriteria;
import org.jboss.as.test.integration.security.common.AbstractSecurityDomainsServerSetupTask;
import org.jboss.as.test.integration.security.common.BasicVaultServerSetupTask;
import org.jboss.as.test.integration.security.common.Utils;
import org.jboss.as.test.integration.security.common.config.SecurityDomain;
import org.jboss.as.test.integration.security.common.config.SecurityModule;
import org.jboss.as.test.integration.security.common.servlets.PrincipalPrintingServlet;
import org.jboss.as.test.integration.security.common.servlets.RolePrintingServlet;
import org.jboss.logging.Logger;
import org.jboss.security.auth.spi.LdapExtLoginModule;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;

/**
 * Tests for {@link LdapExtLoginModule}. It's based on examples from https://community.jboss.org/wiki/LdapExtLoginModule and it
 * includes also tests for LDAP referrals handling.
 *
 * @author Josef Cacek
 */
@RunWith(Arquillian.class)
@ServerSetup({BasicVaultServerSetupTask.class, LdapExtLDAPServerSetupTask.SystemPropertiesSetup.class,
        LdapExtLDAPServerSetupTask.class,
        LdapExtLoginModuleTestCase.SecurityDomainsSetup.class})
@RunAsClient
@Category(CommonCriteria.class)
public class LdapExtLoginModuleTestCase {

    /**
     * The SECURITY_DOMAIN_NAME_PREFIX
     */
    public static final String SECURITY_DOMAIN_NAME_PREFIX = "test-";

    private static Logger LOGGER = Logger.getLogger(LdapExtLoginModuleTestCase.class);

    private static final String DEP1 = "DEP1";
    private static final String DEP2 = "DEP2";
    private static final String DEP2_THROW = "DEP2-throw";
    private static final String DEP3 = "DEP3";
    private static final String DEP4 = "DEP4";
    private static final String DEP4_DIRECT = "DEP4-direct";
    private static final String DEP5 = "DEP5";
    private static final String DEP6 = "DEP6";
    private static final String DEP7 = "DEP7";

    // Public methods --------------------------------------------------------

    /**
     * Creates {@link WebArchive} for {@link #test1(URL)}.
     *
     * @return
     */
    @Deployment(name = DEP1)
    public static WebArchive deployment1() {
        return createWar(SECURITY_DOMAIN_NAME_PREFIX + DEP1);
    }

    /**
     * Creates {@link WebArchive} for {@link #test2(URL)}.
     *
     * @return
     */
    @Deployment(name = DEP2)
    public static WebArchive deployment2() {
        return createWar(SECURITY_DOMAIN_NAME_PREFIX + DEP2);
    }

    /**
     * Creates {@link WebArchive} for {@link #test2throw(URL)}.
     *
     * @return
     */
    @Deployment(name = DEP2_THROW)
    public static WebArchive deployment2throw() {
        return createWar(SECURITY_DOMAIN_NAME_PREFIX + DEP2_THROW);
    }

    /**
     * Creates {@link WebArchive} for {@link #test3(URL)}.
     *
     * @return
     */
    @Deployment(name = DEP3)
    public static WebArchive deployment3() {
        return createWar(SECURITY_DOMAIN_NAME_PREFIX + DEP3);
    }

    /**
     * Creates {@link WebArchive} for {@link #test4(URL)}.
     *
     * @return
     */
    @Deployment(name = DEP4)
    public static WebArchive deployment4() {
        return createWar(SECURITY_DOMAIN_NAME_PREFIX + DEP4);
    }

    /**
     * Creates {@link WebArchive} for {@link #test4_direct(URL)}.
     *
     * @return
     */
    @Deployment(name = DEP4_DIRECT)
    public static WebArchive deployment4_direct() {
        return createWar(SECURITY_DOMAIN_NAME_PREFIX + DEP4_DIRECT);
    }

    /**
     * Creates {@link WebArchive} for {@link #test5(URL)}.
     *
     * @return
     */
    @Deployment(name = DEP5)
    public static WebArchive deployment5() {
        return createWar(SECURITY_DOMAIN_NAME_PREFIX + DEP5);
    }

    /**
     * Creates {@link WebArchive} for {@link #test6(URL)}.
     *
     * @return
     */
    @Deployment(name = DEP6)
    public static WebArchive deployment6() {
        return createWar(SECURITY_DOMAIN_NAME_PREFIX + DEP6);
    }

    /**
     * Creates {@link WebArchive} for {@link #test1(URL)}.
     *
     * @return
     */
    @Deployment(name = DEP7)
    public static WebArchive deployment7() {
        return createWar(SECURITY_DOMAIN_NAME_PREFIX + DEP7);
    }

    /**
     * Test case for Example 1.
     *
     * @throws Exception
     */
    @Test
    @OperateOnDeployment(DEP1)
    public void test1(@ArquillianResource URL webAppURL) throws Exception {
        testDeployment(webAppURL, "jduke", "theduke", "TheDuke", "Echo", "Admin");
        // referral authenticated user
        testDeployment(webAppURL, "mmcfly", "sugarless", "Admin");
    }

    /**
     * Test case for Example 2.
     *
     * @throws Exception
     */
    @Test
    @OperateOnDeployment(DEP2)
    public void test2(@ArquillianResource URL webAppURL) throws Exception {
        testDeployment(webAppURL, "jduke", "theduke", "TheDuke", "Echo", "jduke");
    }

    @Test
    @OperateOnDeployment(DEP2_THROW)
    public void test2throw(@ArquillianResource URL webAppURL) throws Exception {
        testDeployment(webAppURL, "jduke", "theduke", "TheDuke", "Echo", "jduke");
    }

    /**
     * Test case for Example 3.
     *
     * @throws Exception
     */
    @Test
    @OperateOnDeployment(DEP3)
    public void test3(@ArquillianResource URL webAppURL) throws Exception {
        testDeployment(webAppURL, "Java Duke", "theduke", "TheDuke", "Echo", "Admin");
        // referral authenticated user
        testDeployment(webAppURL, "Biff Tannen", "almanac", "RX", "Admin");
    }

    /**
     * Test case for Example 4.
     *
     * @throws Exception
     */
    @Test
    @OperateOnDeployment(DEP4)
    public void test4(@ArquillianResource URL webAppURL) throws Exception {
        testDeployment(webAppURL, "Java Duke", "theduke", "RG/2", "R1", "R2", "R3", "R5");
    }

    /**
     * Test case for Example 4.
     *
     * @throws Exception
     */
    @Test
    @OperateOnDeployment(DEP4_DIRECT)
    public void test4_direct(@ArquillianResource URL webAppURL) throws Exception {
        testDeployment(webAppURL, "jduke", "theduke", "jduke", "RG/2");
    }

    /**
     * Test case for Example 5.
     *
     * @throws Exception
     */
    @Test
    @OperateOnDeployment(DEP5)
    public void test5(@ArquillianResource URL webAppURL) throws Exception {
        testDeployment(webAppURL, "jduke", "theduke", "R1");
        // referral authenticated user
        testDeployment(webAppURL, "ebrown", "atomic", "ebrown-1");
    }

    /**
     * Test case for Example 6.
     *
     * @throws Exception
     */
    @Test
    @OperateOnDeployment(DEP6)
    public void test6(@ArquillianResource URL webAppURL) throws Exception {
        testDeployment(webAppURL, "jduke", "theduke", "Admin");
    }

    /**
     * Test case for Example 1. With name stripping #1
     *
     * @throws Exception
     */
    @Test
    @OperateOnDeployment(DEP7)
    public void test7(@ArquillianResource URL webAppURL) throws Exception {
        testDeployment(webAppURL, "cn=jduke,ou=JBoss,o=Red Hat", "theduke", "TheDuke", "Echo", "Admin");
        // referral authenticated user
        testDeployment(webAppURL, "mmcfly", "sugarless", "Admin");
    }

    /**
     * Test case for Example 1. With name stripping #2
     *
     * @throws Exception
     */
    @Test
    @OperateOnDeployment(DEP7)
    public void test8(@ArquillianResource URL webAppURL) throws Exception {
        testDeployment(webAppURL, "garbage,cn=jduke,ou=JBoss,o=Red Hat", "theduke", "TheDuke", "Echo", "Admin");
        // referral authenticated user
        testDeployment(webAppURL, "mmcfly", "sugarless", "Admin");
    }

    /**
     * Test case for Example 1. With name stripping #3
     *
     * @throws Exception
     */
    @Test
    @OperateOnDeployment(DEP7)
    public void test9(@ArquillianResource URL webAppURL) throws Exception {
        testDeployment(webAppURL, "jduke,ou=JBoss,o=Red Hat", "theduke", "TheDuke", "Echo", "Admin");
        // referral authenticated user
        testDeployment(webAppURL, "mmcfly", "sugarless", "Admin");
    }

    /**
     * Test case for Example 1. With name stripping #4
     *
     * @throws Exception
     */
    @Test
    @OperateOnDeployment(DEP7)
    public void test10(@ArquillianResource URL webAppURL) throws Exception {
        testDeployment(webAppURL, "jduke", "theduke", "TheDuke", "Echo", "Admin");
        // referral authenticated user
        testDeployment(webAppURL, "mmcfly", "sugarless", "Admin");
    }
    // Private methods -------------------------------------------------------

    /**
     * Tests role assignment for given deployment (web-app URL).
     */
    private void testDeployment(URL webAppURL, String username, String password, String... assignedRoles) throws
            IOException, URISyntaxException, LoginException {
        final URL rolesPrintingURL = new URL(webAppURL.toExternalForm() + RolePrintingServlet.SERVLET_PATH.substring(1) + "?"
                + LdapExtLDAPServerSetupTask.QUERY_ROLES);
        final String rolesResponse = Utils.makeCallWithBasicAuthn(rolesPrintingURL, username, password, 200);

        final List<String> assignedRolesList = Arrays.asList(assignedRoles);

        for (String role : LdapExtLDAPServerSetupTask.ROLE_NAMES) {
            if (assignedRolesList.contains(role)) {
                assertInRole(rolesResponse, role);
            } else {
                assertNotInRole(rolesResponse, role);
            }
        }
        final URL principalPrintingURL = new URL(webAppURL.toExternalForm()
                + PrincipalPrintingServlet.SERVLET_PATH.substring(1) + "?" + LdapExtLDAPServerSetupTask.QUERY_ROLES);
        final String principal = Utils.makeCallWithBasicAuthn(principalPrintingURL, username, password, 200);
        assertEquals("Unexpected Principal name", username, principal);
    }

    /**
     * Creates a {@link WebArchive} for given security domain.
     *
     * @param securityDomainName
     * @return
     */
    private static WebArchive createWar(String securityDomainName) {
        final WebArchive war = ShrinkWrap.create(WebArchive.class, securityDomainName + ".war");
        war.addClasses(RolePrintingServlet.class, PrincipalPrintingServlet.class);
        war.addAsWebInfResource(LdapExtLoginModuleTestCase.class.getPackage(), LdapExtLoginModuleTestCase.class.getSimpleName()
                + "-web.xml", "web.xml");
        war.addAsWebInfResource(Utils.getJBossWebXmlAsset(securityDomainName), "jboss-web.xml");
        return war;
    }

    /**
     * Asserts, the role list returned from the {@link RolePrintingServlet} contains the given role.
     *
     * @param rolePrintResponse
     * @param role
     */
    private void assertInRole(final String rolePrintResponse, String role) {
        if (!StringUtils.contains(rolePrintResponse, "," + role + ",")) {
            fail("Missing role '" + role + "' assignment");
        }
    }

    /**
     * Asserts, the role list returned from the {@link RolePrintingServlet} doesn't contain the given role.
     *
     * @param rolePrintResponse
     * @param role
     */
    private void assertNotInRole(final String rolePrintResponse, String role) {
        if (StringUtils.contains(rolePrintResponse, "," + role + ",")) {
            fail("Unexpected role '" + role + "' assignment");
        }
    }

    // Inner classes ------------------------------------------------------

    /**
     * A {@link ServerSetupTask} instance which creates security domains for this test case.
     *
     * @author Josef Cacek
     */
    static class SecurityDomainsSetup extends AbstractSecurityDomainsServerSetupTask {

        /**
         * Returns SecurityDomains configuration for this testcase.
         *
         * @see org.jboss.as.test.integration.security.common.AbstractSecurityDomainsServerSetupTask#getSecurityDomains()
         */
        @Override
        protected SecurityDomain[] getSecurityDomains() {
            final String secondaryTestAddress = Utils.getSecondaryTestAddress(managementClient);
            final String ldapUrl = "ldap://" + secondaryTestAddress + ":"
                    + org.jboss.as.test.integration.security.loginmodules.LdapExtLDAPServerSetupTask.LDAP_PORT;
            final String ldapsUrl = "ldaps://" + secondaryTestAddress + ":"
                    + org.jboss.as.test.integration.security.loginmodules.LdapExtLDAPServerSetupTask.LDAPS_PORT;
            final SecurityDomain sd1 = new SecurityDomain.Builder()
                    .name(SECURITY_DOMAIN_NAME_PREFIX + DEP1)
                    .loginModules(
                            new SecurityModule.Builder()
                                    .name("org.jboss.security.auth.spi.LdapExtLoginModule")
                                    .options(getCommonOptions())
                                    .putOption(Context.REFERRAL, "follow")
                                    .putOption("baseCtxDN", "ou=People,dc=jboss,dc=org")
                                    .putOption("java.naming.provider.url", ldapUrl)
                                    .putOption("baseFilter", "(|(objectClass=referral)(uid={0}))")
                                    .putOption("rolesCtxDN", "ou=Roles,dc=jboss,dc=org")
                                    .putOption("roleFilter", "(|(objectClass=referral)(member={1}))")
                                    .putOption("roleAttributeID", "cn")
                                    .putOption("referralUserAttributeIDToCheck", "member")
                                    .build()) //
                    .build();
            final SecurityModule.Builder sd2LoginModuleBuilder = new SecurityModule.Builder()
                    .name("LdapExtended")
                    .options(getCommonOptions())
                    .putOption(Context.REFERRAL, "ignore")
                    .putOption("java.naming.provider.url", ldapsUrl)
                    .putOption("baseCtxDN", "ou=People,o=example2,dc=jboss,dc=org")
                    .putOption("baseFilter", "(uid={0})")
                    .putOption("rolesCtxDN", "ou=Roles,o=example2,dc=jboss,dc=org")
                    .putOption("roleFilter", "(|(objectClass=referral)(cn={0}))")
                    .putOption("roleAttributeID", "description")
                    .putOption("roleAttributeIsDN", "true")
                    .putOption("roleNameAttributeID", "cn")
                    .putOption("roleRecursion", "0");
            final SecurityDomain sd2 = new SecurityDomain.Builder().name(SECURITY_DOMAIN_NAME_PREFIX + DEP2)
                    .loginModules(sd2LoginModuleBuilder.build()).build();
            sd2LoginModuleBuilder.putOption(Context.REFERRAL, "throw")
                    .putOption("referralUserAttributeIDToCheck", "member");
            final SecurityDomain sd2throw = new SecurityDomain.Builder().name(SECURITY_DOMAIN_NAME_PREFIX + DEP2_THROW)
                    .loginModules(sd2LoginModuleBuilder.build()).build();
            final SecurityDomain sd3 = new SecurityDomain.Builder()
                    .name(SECURITY_DOMAIN_NAME_PREFIX + DEP3)
                    .loginModules(
                            new SecurityModule.Builder()
                                    .name(LdapExtLoginModule.class.getName())
                                    .options(getCommonOptions())
                                    .putOption(Context.REFERRAL, "follow")
                                    .putOption("java.naming.provider.url", ldapsUrl)
                                    .putOption("baseCtxDN", "ou=People,o=example3,dc=jboss,dc=org")
                                    .putOption("baseFilter", "(|(objectClass=referral)(cn={0}))")
                                    .putOption("rolesCtxDN", "ou=Roles,o=example3,dc=jboss,dc=org")
                                    .putOption("roleFilter", "(|(objectClass=referral)(member={1}))")
                                    .putOption("roleAttributeID", "cn").putOption("roleRecursion", "0")
                                    .putOption("referralUserAttributeIDToCheck", "member")
                                    .build()) //
                    .build();
            final SecurityDomain sd4 = new SecurityDomain.Builder()
                    .name(SECURITY_DOMAIN_NAME_PREFIX + DEP4)
                    .loginModules(
                            new SecurityModule.Builder()
                                    .name(LdapExtLoginModule.class.getName())
                                    .options(getCommonOptions())
                                    .putOption(Context.REFERRAL, "ignore")
                                    .putOption("java.naming.provider.url", ldapsUrl)
                                    //
                                    .putOption("baseCtxDN", "ou=People,o=example4,dc=jboss,dc=org")
                                    .putOption("baseFilter", "(employeeNumber={0})")
                                    .putOption("rolesCtxDN", "ou=Roles,o=example4,dc=jboss,dc=org")
                                    .putOption("roleFilter", "(|(objectClass=referral)(member={1}))")
                                    .putOption("roleAttributeID", "cn").putOption("roleRecursion", "1").build()) //
                    .build();
            final SecurityDomain sd4_direct = new SecurityDomain.Builder()
                    .name(SECURITY_DOMAIN_NAME_PREFIX + DEP4_DIRECT)
                    .loginModules(
                            new SecurityModule.Builder()
                                    .name(LdapExtLoginModule.class.getName())
                                    .options(getCommonOptions())
                                    .putOption(Context.REFERRAL, "ignore")
                                    .putOption("java.naming.provider.url", ldapUrl)
                                    .putOption("baseCtxDN", "o=example4,dc=jboss,dc=org") //
                                    .putOption("baseFilter", "(uid={0})") //
                                    .putOption("rolesCtxDN", "o=example4,dc=jboss,dc=org") //
                                    .putOption("roleFilter", "(cn={0})") //
                                    .putOption("roleAttributeIsDN", "true") //
                                    .putOption("roleAttributeID", "description") //
                                    .putOption("roleNameAttributeID", "cn") //
                                    .putOption("roleRecursion", "5").build()) //
                    .build();
            final SecurityDomain sd5 = new SecurityDomain.Builder()
                    .name(SECURITY_DOMAIN_NAME_PREFIX + DEP5)
                    .loginModules(
                            new SecurityModule.Builder()
                                    .name(LdapExtLoginModule.class.getName())
                                    .options(getCommonOptions())
                                    .putOption(Context.REFERRAL, "throw")
                                    .putOption("java.naming.provider.url", ldapUrl)
                                    .putOption("baseCtxDN", "ou=People,o=example5,dc=jboss,dc=org") //
                                    .putOption("baseFilter", "(|(objectClass=referral)(uid={0}))") //
                                    .putOption("rolesCtxDN", "ou=People,o=example5,dc=jboss,dc=org") //
                                    .putOption("roleFilter", "(|(objectClass=referral)(uid={0}))") //
                                    .putOption("roleAttributeID", "employeeNumber")
                                    .putOption("referralUserAttributeIDToCheck", "uid").build())
                    .build();

            final SecurityDomain sd6 = new SecurityDomain.Builder()
                    .name(SECURITY_DOMAIN_NAME_PREFIX + DEP6)
                    .loginModules(
                            new SecurityModule.Builder().name("org.jboss.security.auth.spi.LdapExtLoginModule")
                                    .options(getCommonOptions()) //
                                    .putOption("bindDN", "uid=sa,o=example6,dc=jboss,dc=org")
                                    .putOption("bindCredential", "VAULT::" + VAULT_BLOCK + "::" + ATTRIBUTE_NAME + "::1")
                                    .putOption("java.naming.provider.url", ldapUrl)
                                    .putOption("baseCtxDN", "o=example6,dc=jboss,dc=org")
                                    .putOption("baseFilter", "(uid={0})")
                                    .putOption("rolesCtxDN", "o=example6,dc=jboss,dc=org")
                                    .putOption("roleFilter", "(member={1})")
                                    .putOption("roleAttributeID", "cn")
                                    .build()) //
                    .build();
            final SecurityDomain sd7 = new SecurityDomain.Builder()
                    .name(SECURITY_DOMAIN_NAME_PREFIX + DEP7)
                    .loginModules(
                            new SecurityModule.Builder()
                                    .name("org.jboss.security.auth.spi.LdapExtLoginModule")
                                    .options(getCommonOptions())
                                    .putOption(Context.REFERRAL, "follow")
                                    .putOption("baseCtxDN", "ou=People,dc=jboss,dc=org")
                                    .putOption("java.naming.provider.url", ldapUrl)
                                    .putOption("baseFilter", "(|(objectClass=referral)(uid={0}))")
                                    .putOption("rolesCtxDN", "ou=Roles,dc=jboss,dc=org")
                                    .putOption("roleFilter", "(|(objectClass=referral)(member={1}))")
                                    .putOption("roleAttributeID", "cn")
                                    .putOption("referralUserAttributeIDToCheck", "member")
                                    .putOption("parseUsername", "true")
                                    .putOption("usernameBeginString", "cn=")
                                    .putOption("usernameEndString", ",")
                                    .build()) //
                    .build();
            return new SecurityDomain[]{sd1, sd2, sd2throw, sd3, sd4, sd4_direct, sd5, sd6, sd7};
        }

        private Map<String, String> getCommonOptions() {
            final Map<String, String> moduleOptions = new HashMap<String, String>();
            moduleOptions.put(Context.INITIAL_CONTEXT_FACTORY, "com.sun.jndi.ldap.LdapCtxFactory");
            moduleOptions.put(Context.SECURITY_AUTHENTICATION, "simple");
            moduleOptions.put("bindDN", LdapExtLDAPServerSetupTask.SECURITY_PRINCIPAL);
            moduleOptions.put("bindCredential", LdapExtLDAPServerSetupTask.SECURITY_CREDENTIALS);
            moduleOptions.put("throwValidateError", "true");
            return moduleOptions;
        }
    }

}
