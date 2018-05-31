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

import static org.junit.Assert.assertEquals;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import javax.naming.Context;
import javax.security.auth.login.LoginException;

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
import org.jboss.logging.Logger;
import org.jboss.security.auth.spi.LdapExtLoginModule;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.AfterClass;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;

/**
 * Tests for {@link LdapExtLoginModule} mechanism to cache passwords obtained from external sources.
 * It uses different options: {EXT} - non-cached, {EXTC[:timeout]} cached with optional expiration in milliseconds.
 *
 * @author Josef Cacek
 * @author Filip Bogyai
 */
@RunWith(Arquillian.class)
@ServerSetup({LdapExtLDAPServerSetupTask.SystemPropertiesSetup.class, LdapExtLDAPServerSetupTask.class,
        LdapExtPasswordCachingTestCase.SecurityDomainsSetup.class})
@RunAsClient
@Category(CommonCriteria.class)
public class LdapExtPasswordCachingTestCase {

    /**
     * The SECURITY_DOMAIN_NAME_PREFIX
     */
    public static final String SECURITY_DOMAIN_NAME_PREFIX = "test-";

    private static Logger LOGGER = Logger.getLogger(LdapExtPasswordCachingTestCase.class);
    private static final ExternalPasswordProvider passwordProvider = new ExternalPasswordProvider(System.getProperty("java.io.tmpdir") + File.separator + "tmp.password");

    private static final String DEP1 = "DEP1";
    private static final String DEP2 = "DEP2";
    private static final String DEP3 = "DEP3";
    private static final String EXT = "EXT";
    private static final String EXTC = "EXTC";
    private static final String EXTC500 = "EXTC:500";

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
     * Creates {@link WebArchive} for {@link #test3(URL)}.
     *
     * @return
     */
    @Deployment(name = DEP3)
    public static WebArchive deployment3() {
        return createWar(SECURITY_DOMAIN_NAME_PREFIX + DEP3);
    }

    /**
     * Test case for command option {EXT}..
     * Password should be called on every authentication request
     *
     * @throws Exception
     */
    @Test
    @OperateOnDeployment(DEP1)
    public void test1(@ArquillianResource URL webAppURL) throws Exception {

        passwordProvider.resetFileCounter();
        assertEquals("External counter is not reset", 0, passwordProvider.readFileCounter());
        checkPrincipal(webAppURL, "jduke");
        assertEquals("Password call 1", 1, passwordProvider.readFileCounter());

        checkPrincipal(webAppURL, "jduke");
        assertEquals("Password call 2", 2, passwordProvider.readFileCounter());

    }

    /**
     * Test case for command option {EXTC}..
     * Password should be stored after first authentication request
     *
     * @throws Exception
     */
    @Test
    @OperateOnDeployment(DEP2)
    public void test2(@ArquillianResource URL webAppURL) throws Exception {

        passwordProvider.resetFileCounter();
        assertEquals("External counter is not reset", 0, passwordProvider.readFileCounter());
        checkPrincipal(webAppURL, "jduke");
        assertEquals("Password call: 1", 1, passwordProvider.readFileCounter());

        checkPrincipal(webAppURL, "jduke");
        assertEquals("Password from cache: 1", 1, passwordProvider.readFileCounter());

        long WAIT = 800;
        sleep(WAIT);

        checkPrincipal(webAppURL, "jduke");
        assertEquals("Password from cache: 1", 1, passwordProvider.readFileCounter());
    }

    /**
     * Test case for command option  {EXTC[:expiration_in_millis]}
     * Password should be stored for [:expiration_in_millis] after authentication request
     * and then another call for password is needed
     * <p>
     * expiration_in_millis is set to 500 in test
     *
     * @throws Exception
     */
    @Test
    @OperateOnDeployment(DEP3)
    public void test3(@ArquillianResource URL webAppURL) throws Exception {

        passwordProvider.resetFileCounter();
        assertEquals("External counter is not reset", 0, passwordProvider.readFileCounter());
        checkPrincipal(webAppURL, "jduke");
        assertEquals("Password call: 1", 1, passwordProvider.readFileCounter());

        checkPrincipal(webAppURL, "jduke");
        assertEquals("Password from cache 1", 1, passwordProvider.readFileCounter());

        long WAIT = 800;
        sleep(WAIT);

        checkPrincipal(webAppURL, "jduke");
        assertEquals("Password call 2", 2, passwordProvider.readFileCounter());
        checkPrincipal(webAppURL, "jduke");
        assertEquals("Password from cache 2", 2, passwordProvider.readFileCounter());


    }

    // Private methods -------------------------------------------------------

    /**
     * Sleep the thread for given time, protected against spurious wakeups.
     */
    private void sleep(long millis) throws InterruptedException {
        long endTime = System.currentTimeMillis() + millis;
        long sleepTime = millis;
        while(sleepTime > 0) {
            Thread.sleep(sleepTime);
            sleepTime = endTime - System.currentTimeMillis();
        }
    }

    private void checkPrincipal(URL webAppURL, String username) throws
            IOException, URISyntaxException, LoginException {

        final URL principalPrintingURL = new URL(webAppURL.toExternalForm()
                + PrincipalPrintingServlet.SERVLET_PATH.substring(1));
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
        final WebArchive war = ShrinkWrap.create(WebArchive.class, securityDomainName + ".war");
        war.addClasses(PrincipalPrintingServlet.class);
        war.addAsWebInfResource(LdapExtLoginModuleTestCase.class.getPackage(), LdapExtLoginModuleTestCase.class.getSimpleName()
                + "-web.xml", "web.xml");
        war.addAsWebInfResource(Utils.getJBossWebXmlAsset(securityDomainName), "jboss-web.xml");
        return war;
    }

    // Inner classes ------------------------------------------------------

    /**
     * A {@link ServerSetupTask} instance which creates security domains for this test case.
     *
     * @author Josef Cacek
     * @author Filip Bogyai
     */
    static class SecurityDomainsSetup extends AbstractSecurityDomainsServerSetupTask {

        /**
         * Returns SecurityDomains configuration for this testcase.
         *
         * @see org.jboss.as.test.integration.security.common.AbstractSecurityDomainsServerSetupTask#getSecurityDomains()
         */
        @Override
        protected SecurityDomain[] getSecurityDomains() {

            final SecurityDomain sd1 = new SecurityDomain.Builder()
                    .name(SECURITY_DOMAIN_NAME_PREFIX + DEP1)
                    .loginModules(
                            new SecurityModule.Builder()
                                    .name(LdapExtLoginModule.class.getName())
                                    .options(getCommonOptions(buildExtCommand(EXT) + LdapExtLDAPServerSetupTask.SECURITY_CREDENTIALS))
                                    .build())
                    .build();
            final SecurityDomain sd2 = new SecurityDomain.Builder()
                    .name(SECURITY_DOMAIN_NAME_PREFIX + DEP2)
                    .loginModules(
                            new SecurityModule.Builder()
                                    .name("org.jboss.security.auth.spi.LdapExtLoginModule")
                                    .options(getCommonOptions(buildExtCommand(EXTC) + LdapExtLDAPServerSetupTask.SECURITY_CREDENTIALS))
                                    .build())
                    .build();
            final SecurityDomain sd3 = new SecurityDomain.Builder()
                    .name(SECURITY_DOMAIN_NAME_PREFIX + DEP3)
                    .loginModules(
                            new SecurityModule.Builder()
                                    .name("org.jboss.security.auth.spi.LdapExtLoginModule")
                                    .options(getCommonOptions(buildExtCommand(EXTC500) + LdapExtLDAPServerSetupTask.SECURITY_CREDENTIALS))
                                    .build())
                    .build();
            return new SecurityDomain[]{sd1, sd2, sd3};
        }

        // bindCredential uses extPasswordCommand to retrieve password
        private Map<String, String> getCommonOptions(String extPasswordCommand) {
            final String secondaryTestAddress = Utils.getSecondaryTestAddress(managementClient);

            final Map<String, String> moduleOptions = new HashMap<String, String>();
            moduleOptions.put(Context.INITIAL_CONTEXT_FACTORY, "com.sun.jndi.ldap.LdapCtxFactory");
            moduleOptions.put(Context.SECURITY_AUTHENTICATION, "simple");
            moduleOptions.put("bindDN", LdapExtLDAPServerSetupTask.SECURITY_PRINCIPAL);
            moduleOptions.put("bindCredential", extPasswordCommand);
            moduleOptions.put("throwValidateError", "true");
            moduleOptions.put(Context.REFERRAL, "follow");
            moduleOptions.put("baseCtxDN", "ou=People,dc=jboss,dc=org");
            moduleOptions.put(
                    "java.naming.provider.url",
                    "ldap://"
                            + secondaryTestAddress
                            + ":"
                            + org.jboss.as.test.integration.security.loginmodules.LdapExtLDAPServerSetupTask.LDAP_PORT);
            moduleOptions.put("baseFilter", "(uid={0})");
            moduleOptions.put("rolesCtxDN", "ou=Roles,dc=jboss,dc=org");
            moduleOptions.put("roleFilter", "(|(objectClass=referral)(member={1}))");
            moduleOptions.put("roleAttributeID", "cn");
            moduleOptions.put("referralUserAttributeIDToCheck", "member");
            return moduleOptions;
        }

        /**
         * Creates runnable command for bindCredential to retrieve password from external source
         *
         * @param extOption for command type EXT/EXTC[:expiration_in_millis]
         * @return command to run a class which will return password
         */
        private String buildExtCommand(String extOption) {
            // First check for java.exe or java as the binary
            File java = new File(System.getProperty("java.home"), "/bin/java");
            File javaExe = new File(System.getProperty("java.home"), "/bin/java.exe");
            String jre;
            if (java.exists()) { jre = java.getAbsolutePath(); } else { jre = javaExe.getAbsolutePath(); }
            // Build the command to run this jre
            String cmd = jre + " -cp " + ExternalPasswordProvider.class.getProtectionDomain().getCodeSource().getLocation().getPath()
                    + " org.jboss.as.test.integration.security.loginmodules.ExternalPasswordProvider " + passwordProvider.getCounterFile() + " ";

            return "{" + extOption + "}" + cmd;
        }
    }

    @AfterClass
    public static void cleanup() {
        passwordProvider.cleanup();
    }

}
