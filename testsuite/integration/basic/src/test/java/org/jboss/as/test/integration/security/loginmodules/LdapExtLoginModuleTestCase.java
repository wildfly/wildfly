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
import static org.junit.Assert.fail;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.directory.server.annotations.CreateLdapServer;
import org.apache.directory.server.annotations.CreateTransport;
import org.apache.directory.server.core.annotations.AnnotationUtils;
import org.apache.directory.server.core.annotations.ContextEntry;
import org.apache.directory.server.core.annotations.CreateDS;
import org.apache.directory.server.core.annotations.CreateIndex;
import org.apache.directory.server.core.annotations.CreatePartition;
import org.apache.directory.server.core.api.DirectoryService;
import org.apache.directory.server.core.factory.DSAnnotationProcessor;
import org.apache.directory.server.core.kerberos.KeyDerivationInterceptor;
import org.apache.directory.server.factory.ServerAnnotationProcessor;
import org.apache.directory.server.ldap.LdapServer;
import org.apache.directory.shared.ldap.model.entry.DefaultEntry;
import org.apache.directory.shared.ldap.model.ldif.LdifEntry;
import org.apache.directory.shared.ldap.model.ldif.LdifReader;
import org.apache.directory.shared.ldap.model.schema.SchemaManager;
import org.apache.http.NameValuePair;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.utils.URLEncodedUtils;
import org.apache.http.message.BasicNameValuePair;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.OperateOnDeployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.as.arquillian.api.ServerSetup;
import org.jboss.as.arquillian.api.ServerSetupTask;
import org.jboss.as.arquillian.container.ManagementClient;
import org.jboss.as.test.integration.security.common.AbstractSecurityDomainsServerSetupTask;
import org.jboss.as.test.integration.security.common.AbstractSystemPropertiesServerSetupTask;
import org.jboss.as.test.integration.security.common.ManagedCreateLdapServer;
import org.jboss.as.test.integration.security.common.ManagedCreateTransport;
import org.jboss.as.test.integration.security.common.Utils;
import org.jboss.as.test.integration.security.common.config.SecurityDomain;
import org.jboss.as.test.integration.security.common.config.SecurityModule;
import org.jboss.as.test.integration.security.loginmodules.common.servlets.PrincipalPrintingServlet;
import org.jboss.as.test.integration.security.loginmodules.common.servlets.RolePrintingServlet;
import org.jboss.logging.Logger;
import org.jboss.security.auth.spi.LdapExtLoginModule;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * A LdapLoginModuleTestCase, based on examples from https://community.jboss.org/wiki/LdapExtLoginModule
 * 
 * @author Josef Cacek
 */
@RunWith(Arquillian.class)
@ServerSetup({ LdapExtLoginModuleTestCase.SystemPropertiesSetup.class, LdapExtLoginModuleTestCase.LDAPServerSetupTask.class,
        LdapExtLoginModuleTestCase.SecurityDomainsSetup.class })
@RunAsClient
public class LdapExtLoginModuleTestCase {

    /** The SECURITY_DOMAIN_NAME_PREFIX */
    public static final String SECURITY_DOMAIN_NAME_PREFIX = "test-";

    private static Logger LOGGER = Logger.getLogger(LdapExtLoginModuleTestCase.class);

    private static final String KEYSTORE_FILENAME = "ldaps.jks";
    private static final File KEYSTORE_FILE = new File(KEYSTORE_FILENAME);
    private static final int LDAP_PORT = 10389;
    private static final int LDAPS_PORT = 10636;

    private static final String SECURITY_CREDENTIALS = "secret";
    private static final String SECURITY_PRINCIPAL = "uid=admin,ou=system";

    private static final String DEP1 = "DEP1";
    private static final String DEP2 = "DEP2";
    private static final String DEP3 = "DEP3";
    private static final String DEP4 = "DEP4";

    private static final String[] ROLE_NAMES = { "TheDuke", "Echo", "TheDuke2", "Echo2", "JBossAdmin", "jduke", "jduke2",
            "RG1", "RG2", "RG3", "R1", "R2", "R3", "R4", "R5", "Roles" };

    private static final String QUERY_ROLES;
    static {
        final List<NameValuePair> qparams = new ArrayList<NameValuePair>();
        for (final String role : ROLE_NAMES) {
            qparams.add(new BasicNameValuePair(RolePrintingServlet.PARAM_ROLE_NAME, role));
        }
        QUERY_ROLES = URLEncodedUtils.format(qparams, "UTF-8");
    }

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
     * Creates {@link WebArchive} for {@link #test3(URL)}.
     * 
     * @return
     */
    @Deployment(name = DEP4)
    public static WebArchive deployment4() {
        return createWar(SECURITY_DOMAIN_NAME_PREFIX + DEP4);
    }

    /**
     * Test case for Example 1.
     * 
     * @throws Exception
     */
    @Test
    @OperateOnDeployment(DEP1)
    public void test1(@ArquillianResource URL webAppURL) throws Exception {
        testDeployment(webAppURL, "jduke", false);
    }

    /**
     * Test case for Example 2.
     * 
     * @throws Exception
     */
    @Test
    @OperateOnDeployment(DEP2)
    public void test2(@ArquillianResource URL webAppURL) throws Exception {
        testDeployment(webAppURL, "jduke", true);
    }

    /**
     * Test case for Example 3.
     * 
     * @throws Exception
     */
    @Test
    @OperateOnDeployment(DEP3)
    public void test3(@ArquillianResource URL webAppURL) throws Exception {
        testDeployment(webAppURL, "Java Duke", false);
    }

    /**
     * Test case for Example 4.
     * 
     * @throws Exception
     */
    @Test
    @OperateOnDeployment(DEP4)
    public void test4(@ArquillianResource URL webAppURL) throws Exception {
        final URL rolesPrintingURL = new URL(webAppURL.toExternalForm() + RolePrintingServlet.SERVLET_PATH.substring(1) + "?"
                + QUERY_ROLES);
        final String userName = "Java Duke";
        final String rolesResponse = Utils.makeCallWithBasicAuthn(rolesPrintingURL, userName, "theduke", 200);

        assertNotInRole(rolesResponse, "jduke");
        assertNotInRole(rolesResponse, "Java Duke");
        assertNotInRole(rolesResponse, "Roles");
        assertNotInRole(rolesResponse, "JBossAdmin");
        assertNotInRole(rolesResponse, "R4");
        //assigned roles
        assertInRole(rolesResponse, "RG2");
        assertInRole(rolesResponse, "R1");
        assertInRole(rolesResponse, "R2");
        assertInRole(rolesResponse, "R3");
        assertInRole(rolesResponse, "R5");

        final URL principalPrintingURL = new URL(webAppURL.toExternalForm()
                + PrincipalPrintingServlet.SERVLET_PATH.substring(1) + "?" + QUERY_ROLES);
        final String principal = Utils.makeCallWithBasicAuthn(principalPrintingURL, userName, "theduke", 200);
        assertEquals("Unexpected Principal name", userName, principal);
    }

    // Private methods -------------------------------------------------------

    /**
     * Tests role assignment.
     * 
     * @param webAppURL
     * @throws MalformedURLException
     * @throws ClientProtocolException
     * @throws IOException
     * @throws URISyntaxException
     */
    private void testDeployment(URL webAppURL, String username, boolean hasJdukeRole) throws MalformedURLException,
            ClientProtocolException, IOException, URISyntaxException {
        final URL rolesPrintingURL = new URL(webAppURL.toExternalForm() + RolePrintingServlet.SERVLET_PATH.substring(1) + "?"
                + QUERY_ROLES);
        final String rolesResponse = Utils.makeCallWithBasicAuthn(rolesPrintingURL, username, "theduke", 200);

        if (hasJdukeRole) {
            assertInRole(rolesResponse, "jduke");
        } else {
            assertNotInRole(rolesResponse, "jduke");
        }
        assertNotInRole(rolesResponse, "Java Duke");
        assertNotInRole(rolesResponse, "Roles");
        assertNotInRole(rolesResponse, "JBossAdmin");
        assertNotInRole(rolesResponse, "TheDuke2");
        assertNotInRole(rolesResponse, "Echo2");

        //assigned roles
        assertInRole(rolesResponse, "TheDuke");
        assertInRole(rolesResponse, "Echo");

        final URL principalPrintingURL = new URL(webAppURL.toExternalForm()
                + PrincipalPrintingServlet.SERVLET_PATH.substring(1) + "?" + QUERY_ROLES);
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
        war.addAsWebInfResource(new StringAsset("<jboss-web><security-domain>" + securityDomainName
                + "</security-domain></jboss-web>"), "jboss-web.xml");
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

    // Embedded classes ------------------------------------------------------

    /**
     * This setup task sets truststore file.
     */
    static class SystemPropertiesSetup extends AbstractSystemPropertiesServerSetupTask {

        /**
         * @see org.jboss.as.test.integration.security.common.AbstractSystemPropertiesServerSetupTask#getSystemProperties()
         */
        @Override
        protected SystemProperty[] getSystemProperties() {
            return new SystemProperty[] { new DefaultSystemProperty("javax.net.ssl.trustStore", KEYSTORE_FILE.getAbsolutePath()) };
        }
    }

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
            final SecurityDomain sd1 = new SecurityDomain.Builder()
                    .name(SECURITY_DOMAIN_NAME_PREFIX + DEP1)
                    .loginModules(
                            new SecurityModule.Builder().name("org.jboss.security.auth.spi.LdapExtLoginModule")
                                    .options(getCommonOptions()).putOption("baseCtxDN", "ou=People,dc=jboss,dc=org")
                                    .putOption("java.naming.provider.url", "ldap://" + secondaryTestAddress + ":" + LDAP_PORT)
                                    .putOption("baseFilter", "(uid={0})").putOption("rolesCtxDN", "ou=Roles,dc=jboss,dc=org")
                                    .putOption("roleFilter", "(member={1})").putOption("roleAttributeID", "cn").build()) //
                    .build();
            final SecurityDomain sd2 = new SecurityDomain.Builder()
                    .name(SECURITY_DOMAIN_NAME_PREFIX + DEP2)
                    .loginModules(
                            new SecurityModule.Builder().name("LdapExtended").options(getCommonOptions())
                                    .putOption("java.naming.provider.url", "ldap://" + secondaryTestAddress + ":" + LDAP_PORT)
                                    .putOption("baseCtxDN", "ou=People,o=example2,dc=jboss,dc=org")
                                    .putOption("baseFilter", "(uid={0})")
                                    .putOption("rolesCtxDN", "ou=Roles,o=example2,dc=jboss,dc=org")
                                    .putOption("roleFilter", "(cn={0})").putOption("roleAttributeID", "description")
                                    .putOption("roleAttributeIsDN", "true").putOption("roleNameAttributeID", "cn")
                                    .putOption("roleRecursion", "0").build()) //
                    .build();
            final SecurityDomain sd3 = new SecurityDomain.Builder()
                    .name(SECURITY_DOMAIN_NAME_PREFIX + DEP3)
                    .loginModules(
                            new SecurityModule.Builder()
                                    .name(LdapExtLoginModule.class.getName())
                                    .options(getCommonOptions())
                                    .putOption("java.naming.provider.url", "ldaps://" + secondaryTestAddress + ":" + LDAPS_PORT)
                                    .putOption("baseCtxDN", "ou=People,o=example3,dc=jboss,dc=org")
                                    .putOption("baseFilter", "(cn={0})")
                                    .putOption("rolesCtxDN", "ou=Roles,o=example3,dc=jboss,dc=org")
                                    .putOption("roleFilter", "(member={1})").putOption("roleAttributeID", "cn")
                                    .putOption("roleRecursion", "0").build()) //
                    .build();
            final SecurityDomain sd4 = new SecurityDomain.Builder()
                    .name(SECURITY_DOMAIN_NAME_PREFIX + DEP4)
                    .loginModules(
                            new SecurityModule.Builder()
                                    .name(LdapExtLoginModule.class.getName())
                                    .options(getCommonOptions())
                                    .putOption("java.naming.provider.url", "ldaps://" + secondaryTestAddress + ":" + LDAPS_PORT)
                                    .putOption("baseCtxDN", "ou=People,o=example4,dc=jboss,dc=org")
                                    .putOption("baseFilter", "(cn={0})")
                                    .putOption("rolesCtxDN", "ou=Roles,o=example4,dc=jboss,dc=org")
                                    .putOption("roleFilter", "(member={1})").putOption("roleAttributeID", "cn")
                                    .putOption("roleRecursion", "1").build()) //
                    .build();
            return new SecurityDomain[] { sd1, sd2, sd3, sd4 };
        }

        private Map<String, String> getCommonOptions() {
            final Map<String, String> moduleOptions = new HashMap<String, String>();
            moduleOptions.put("java.naming.factory.initial", "com.sun.jndi.ldap.LdapCtxFactory");
            moduleOptions.put("java.naming.security.authentication", "simple");
            moduleOptions.put("bindDN", SECURITY_PRINCIPAL);
            moduleOptions.put("bindCredential", SECURITY_CREDENTIALS);
            return moduleOptions;
        }
    }

    /**
     * A server setup task which configures and starts LDAP server.
     */
    //@formatter:off
    @CreateDS( 
        name = "JBossDS",
        partitions =
        {
            @CreatePartition(
                name = "jboss",
                suffix = "dc=jboss,dc=org",
                contextEntry = @ContextEntry( 
                    entryLdif =
                        "dn: dc=jboss,dc=org\n" +
                        "dc: jboss\n" +
                        "objectClass: top\n" +
                        "objectClass: domain\n\n" ),
                indexes = 
                {
                    @CreateIndex( attribute = "objectClass" ),
                    @CreateIndex( attribute = "dc" ),
                    @CreateIndex( attribute = "ou" )
                })
        },
        additionalInterceptors = { KeyDerivationInterceptor.class })
    @CreateLdapServer ( 
        transports = 
        {
            @CreateTransport( protocol = "LDAP",  port = LDAP_PORT), 
            @CreateTransport( protocol = "LDAPS", port = LDAPS_PORT) 
        },
//        keyStore="ldaps.jks",
        certificatePassword="secret")            
    //@formatter:on
    static class LDAPServerSetupTask implements ServerSetupTask {

        private DirectoryService directoryService;
        private LdapServer ldapServer;

        /**
         * Creates directory services, starts LDAP server and KDCServer
         * 
         * @param managementClient
         * @param containerId
         * @throws Exception
         * @see org.jboss.as.arquillian.api.ServerSetupTask#setup(org.jboss.as.arquillian.container.ManagementClient,
         *      java.lang.String)
         */
        public void setup(ManagementClient managementClient, String containerId) throws Exception {
            directoryService = DSAnnotationProcessor.getDirectoryService();
            final SchemaManager schemaManager = directoryService.getSchemaManager();
            try {
                for (LdifEntry ldifEntry : new LdifReader(
                        LdapExtLoginModuleTestCase.class.getResourceAsStream(LdapExtLoginModuleTestCase.class.getSimpleName()
                                + ".ldif"))) {
                    directoryService.getAdminSession().add(new DefaultEntry(schemaManager, ldifEntry.getEntry()));
                }
            } catch (Exception e) {
                e.printStackTrace();
                throw e;
            }
            final ManagedCreateLdapServer createLdapServer = new ManagedCreateLdapServer(
                    (CreateLdapServer) AnnotationUtils.getInstance(CreateLdapServer.class));
            FileOutputStream fos = new FileOutputStream(KEYSTORE_FILE);
            IOUtils.copy(getClass().getResourceAsStream(KEYSTORE_FILENAME), fos);
            fos.close();
            createLdapServer.setKeyStore(KEYSTORE_FILE.getAbsolutePath());
            fixTransportAddress(createLdapServer, StringUtils.strip(Utils.getSecondaryTestAddress(managementClient), "[]"));
            ldapServer = ServerAnnotationProcessor.instantiateLdapServer(createLdapServer, directoryService);
            ldapServer.start();
        }

        /**
         * Fixes bind address in the CreateTransport annotation.
         * 
         * @param createLdapServer
         */
        private void fixTransportAddress(ManagedCreateLdapServer createLdapServer, String address) {
            final CreateTransport[] createTransports = createLdapServer.transports();
            for (int i = 0; i < createTransports.length; i++) {
                final ManagedCreateTransport mgCreateTransport = new ManagedCreateTransport(createTransports[i]);
                mgCreateTransport.setAddress(address);
                createTransports[i] = mgCreateTransport;
            }
        }

        /**
         * Stops LDAP server and KDCServer and shuts down the directory service.
         * 
         * @param managementClient
         * @param containerId
         * @throws Exception
         * @see org.jboss.as.arquillian.api.ServerSetupTask#tearDown(org.jboss.as.arquillian.container.ManagementClient,
         *      java.lang.String)
         */
        public void tearDown(ManagementClient managementClient, String containerId) throws Exception {
            ldapServer.stop();
            directoryService.shutdown();
            KEYSTORE_FILE.delete();
            FileUtils.deleteDirectory(directoryService.getInstanceLayout().getInstanceDirectory());
        }

    }

}
