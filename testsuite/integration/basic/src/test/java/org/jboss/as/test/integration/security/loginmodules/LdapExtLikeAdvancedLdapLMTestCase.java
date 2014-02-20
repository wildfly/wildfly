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

import static org.junit.Assert.*;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.naming.Context;
import javax.security.auth.login.LoginException;

import org.apache.commons.lang.StringUtils;
import org.apache.http.client.ClientProtocolException;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.OperateOnDeployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.as.arquillian.api.ServerSetup;
import org.jboss.as.arquillian.api.ServerSetupTask;
import org.jboss.as.test.categories.CommonCriteria;
import org.jboss.as.test.integration.security.common.AbstractSecurityDomainsServerSetupTask;
import org.jboss.as.test.integration.security.common.Utils;
import org.jboss.as.test.integration.security.common.config.SecurityDomain;
import org.jboss.as.test.integration.security.common.config.SecurityModule;
import org.jboss.as.test.integration.security.common.servlets.PrincipalPrintingServlet;
import org.jboss.as.test.integration.security.common.servlets.RolePrintingServlet;
import org.jboss.logging.Logger;
import org.jboss.security.negotiation.AdvancedLdapLoginModule;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;

/**
 * An {@link AdvancedLdapLoginModule} test, which includes testing referrals. This testcase doesn't contain Kerberos tests, such
 * ones are located in org.jboss.as.test.integration.security.loginmodules.negotiation package.
 * <p>
 * The test data used in this testcase comes from {@link LdapExtLoginModuleTestCase}.
 * 
 * @author Josef Cacek
 */
@RunWith(Arquillian.class)
@ServerSetup({ LdapExtLDAPServerSetupTask.SystemPropertiesSetup.class, LdapExtLDAPServerSetupTask.class,
        LdapExtLikeAdvancedLdapLMTestCase.SecurityDomainsSetup.class })
@RunAsClient
@Category(CommonCriteria.class)
public class LdapExtLikeAdvancedLdapLMTestCase {

    /** The SECURITY_DOMAIN_NAME_PREFIX */
    public static final String SECURITY_DOMAIN_NAME_PREFIX = "test-";

    private static Logger LOGGER = Logger.getLogger(LdapExtLikeAdvancedLdapLMTestCase.class);

    private static final String DEP1 = "DEP1";
    private static final String DEP2 = "DEP2";
    private static final String DEP2_THROW = "DEP2-throw";
    private static final String DEP3 = "DEP3";
    private static final String DEP4 = "DEP4";
    private static final String DEP4_DIRECT = "DEP4-direct";
    private static final String DEP5 = "DEP5";

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
     * Test case for Example 1.
     * 
     * @throws Exception
     */
    @Test
    @OperateOnDeployment(DEP1)
    @Ignore("WFLY-808 - referrals don't work when they reference to another LDAP instance")
    public void test1(@ArquillianResource URL webAppURL) throws Exception {
        testDeployment(webAppURL, "jduke", "TheDuke", "Echo", "Admin");
    }

    /**
     * Test case for Example 2.
     * 
     * @throws Exception
     */
    @Test
    @OperateOnDeployment(DEP2)
    public void test2(@ArquillianResource URL webAppURL) throws Exception {
        // JBPAPP-10173 - ExtendedLdap LM would contain also "jduke"
        testDeployment(webAppURL, "jduke", "TheDuke", "Echo");
    }

    @Test
    @OperateOnDeployment(DEP2_THROW)
    @Ignore("WFLY-808 - referrals don't work when they reference to another LDAP instance")
    public void test2throw(@ArquillianResource URL webAppURL) throws Exception {
        // JBPAPP-10173 - ExtendedLdap LM would contain also "jduke"
        testDeployment(webAppURL, "jduke", "TheDuke", "Echo");
    }

    /**
     * Test case for Example 3.
     * 
     * @throws Exception
     */
    @Test
    @OperateOnDeployment(DEP3)
    @Ignore("AS7-5737 - referrals don't work when they reference to another LDAP instance")
    public void test3(@ArquillianResource URL webAppURL) throws Exception {
        testDeployment(webAppURL, "Java Duke", "TheDuke", "Echo", "Admin");
    }

    /**
     * Test case for Example 4.
     * 
     * @throws Exception
     */
    @Test
    @OperateOnDeployment(DEP4)
    public void test4(@ArquillianResource URL webAppURL) throws Exception {
        // JBPAPP-10173 - ExtendedLdap LM would contain also "R1", "R2", "R3"
        // recursion in AdvancedLdapLoginModule is enabled only if the roleAttributeIsDN module option is true. This is not
        // required in LdapExtLogiModule.
        testDeployment(webAppURL, "Java Duke", "RG/2", "R5");
    }

    /**
     * Test case for Example 4 (direct).
     * 
     * @throws Exception
     */
    @Test
    @OperateOnDeployment(DEP4_DIRECT)
    public void test4_direct(@ArquillianResource URL webAppURL) throws Exception {
        testDeployment(webAppURL, "jduke", "RG/2");
    }

    /**
     * Test case for Example 5.
     * 
     * @throws Exception
     */
    @Test
    @OperateOnDeployment(DEP5)
    public void test5(@ArquillianResource URL webAppURL) throws Exception {
        testDeployment(webAppURL, "jduke", "R1");
    }

    // Private methods -------------------------------------------------------

    /**
     * Tests role assignment for given deployment (web-app URL).
     */
    private void testDeployment(URL webAppURL, String username, String... assignedRoles) throws MalformedURLException,
            ClientProtocolException, IOException, URISyntaxException, LoginException {
        final URL rolesPrintingURL = new URL(webAppURL.toExternalForm() + RolePrintingServlet.SERVLET_PATH.substring(1) + "?"
                + LdapExtLDAPServerSetupTask.QUERY_ROLES);
        final String rolesResponse = Utils.makeCallWithBasicAuthn(rolesPrintingURL, username, "theduke", 200);

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
        final String principal = Utils.makeCallWithBasicAuthn(principalPrintingURL, username, "theduke", 200);
        assertEquals("Unexpected Principal name", username, principal);
    }

    /**
     * Creates a {@link WebArchive} for given security domain.
     * 
     * @param securityDomainName
     * @return
     */
    private static WebArchive createWar(String securityDomainName) {
        LOGGER.info("Start deployment for security-domain " + securityDomainName);
        final WebArchive war = ShrinkWrap.create(WebArchive.class, securityDomainName + ".war");
        war.addClasses(RolePrintingServlet.class, PrincipalPrintingServlet.class);
        war.addAsWebInfResource(LdapExtLoginModuleTestCase.class.getPackage(), LdapExtLoginModuleTestCase.class.getSimpleName()
                + "-web.xml", "web.xml");
        war.addAsWebInfResource(Utils.getJBossWebXmlAsset(securityDomainName), "jboss-web.xml");
        war.addAsManifestResource(Utils.getJBossDeploymentStructure("org.jboss.security.negotiation"),
                "jboss-deployment-structure.xml");
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug(war.toString(true));
        }
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
            final String lmClassName = AdvancedLdapLoginModule.class.getName();
            final SecurityDomain sd1 = new SecurityDomain.Builder()
                    .name(SECURITY_DOMAIN_NAME_PREFIX + DEP1)
                    .loginModules(
                            new SecurityModule.Builder()
                                    .name(lmClassName)
                                    .options(getCommonOptions())
                                    .putOption(Context.REFERRAL, "follow")
                                    .putOption("baseCtxDN", "ou=People,dc=jboss,dc=org")
                                    .putOption("java.naming.provider.url",
                                            "ldap://" + secondaryTestAddress + ":" + LdapExtLDAPServerSetupTask.LDAP_PORT)
                                    .putOption("baseFilter", "(uid={0})").putOption("rolesCtxDN", "ou=Roles,dc=jboss,dc=org")
                                    .putOption("roleFilter", "(|(objectClass=referral)(member={1}))")
                                    .putOption("roleAttributeID", "cn").build()) //
                    .build();
            final SecurityModule.Builder sd2LoginModuleBuilder = new SecurityModule.Builder()
                    .name(lmClassName)
                    .options(getCommonOptions())
                    .putOption(Context.REFERRAL, "ignore")
                    .putOption("java.naming.provider.url",
                            "ldap://" + secondaryTestAddress + ":" + LdapExtLDAPServerSetupTask.LDAP_PORT)
                    .putOption("baseCtxDN", "ou=People,o=example2,dc=jboss,dc=org").putOption("baseFilter", "(uid={0})")
                    .putOption("rolesCtxDN", "ou=Roles,o=example2,dc=jboss,dc=org")
                    .putOption("roleFilter", "(|(objectClass=referral)(cn={0}))").putOption("roleAttributeID", "description")
                    .putOption("roleAttributeIsDN", "true").putOption("roleNameAttributeID", "cn");
            final SecurityDomain sd2 = new SecurityDomain.Builder().name(SECURITY_DOMAIN_NAME_PREFIX + DEP2)
                    .loginModules(sd2LoginModuleBuilder.build()).build();
            sd2LoginModuleBuilder.putOption(Context.REFERRAL, "throw");
            final SecurityDomain sd2throw = new SecurityDomain.Builder().name(SECURITY_DOMAIN_NAME_PREFIX + DEP2_THROW)
                    .loginModules(sd2LoginModuleBuilder.build()).build();
            final SecurityDomain sd3 = new SecurityDomain.Builder()
                    .name(SECURITY_DOMAIN_NAME_PREFIX + DEP3)
                    .loginModules(
                            new SecurityModule.Builder()
                                    .name(lmClassName)
                                    .options(getCommonOptions())
                                    .putOption(Context.REFERRAL, "follow")
                                    .putOption("java.naming.provider.url",
                                            "ldaps://" + secondaryTestAddress + ":" + LdapExtLDAPServerSetupTask.LDAPS_PORT)
                                    .putOption("baseCtxDN", "ou=People,o=example3,dc=jboss,dc=org")
                                    .putOption("baseFilter", "(cn={0})")
                                    .putOption("rolesCtxDN", "ou=Roles,o=example3,dc=jboss,dc=org")
                                    .putOption("roleFilter", "(|(objectClass=referral)(member={1}))")
                                    .putOption("roleAttributeID", "cn").build()) //
                    .build();
            final SecurityDomain sd4 = new SecurityDomain.Builder()
                    .name(SECURITY_DOMAIN_NAME_PREFIX + DEP4)
                    .loginModules(
                            new SecurityModule.Builder()
                                    .name(lmClassName)
                                    .options(getCommonOptions())
                                    .putOption(Context.REFERRAL, "ignore")
                                    .putOption("java.naming.provider.url",
                                            "ldaps://" + secondaryTestAddress + ":" + LdapExtLDAPServerSetupTask.LDAPS_PORT)
                                    .putOption("baseCtxDN", "ou=People,o=example4,dc=jboss,dc=org")
                                    .putOption("baseFilter", "(employeeNumber={0})")
                                    .putOption("rolesCtxDN", "ou=Roles,o=example4,dc=jboss,dc=org")
                                    .putOption("roleFilter", "(|(objectClass=referral)(member={1}))")
                                    .putOption("roleAttributeID", "cn").putOption("recurseRoles", "true").build()) //
                    .build();
            final SecurityDomain sd4_direct = new SecurityDomain.Builder()
                    .name(SECURITY_DOMAIN_NAME_PREFIX + DEP4_DIRECT)
                    .loginModules(
                            new SecurityModule.Builder()
                                    .name(lmClassName)
                                    .options(getCommonOptions())
                                    .putOption(Context.REFERRAL, "ignore")
                                    .putOption("java.naming.provider.url",
                                            "ldap://" + secondaryTestAddress + ":" + LdapExtLDAPServerSetupTask.LDAP_PORT)
                                    .putOption("baseCtxDN", "ou=People,o=example4,dc=jboss,dc=org")
                                    .putOption("baseFilter", "(uid={0})").putOption("roleAttributeIsDN", "true")
                                    .putOption("roleNameAttributeID", "cn").putOption("roleAttributeID", "description")
                                    .putOption("recurseRoles", "true").build()) //
                    .build();
            final SecurityDomain sd5 = new SecurityDomain.Builder()
                    .name(SECURITY_DOMAIN_NAME_PREFIX + DEP5)
                    .loginModules(
                            new SecurityModule.Builder()
                                    .name(lmClassName)
                                    .options(getCommonOptions())
                                    .putOption(Context.REFERRAL, "throw")
                                    .putOption("java.naming.provider.url",
                                            "ldap://" + secondaryTestAddress + ":" + LdapExtLDAPServerSetupTask.LDAP_PORT) //
                                    .putOption("baseCtxDN", "ou=People,o=example5,dc=jboss,dc=org") //
                                    .putOption("baseFilter", "(uid={0})") //
                                    .putOption("rolesCtxDN", "ou=People,o=example5,dc=jboss,dc=org") //
                                    .putOption("roleFilter", "(uid={0})") //
                                    .putOption("roleAttributeID", "employeeNumber").build()) //
                    .build();
            return new SecurityDomain[] { sd1, sd2, sd2throw, sd3, sd4, sd4_direct, sd5 };
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
