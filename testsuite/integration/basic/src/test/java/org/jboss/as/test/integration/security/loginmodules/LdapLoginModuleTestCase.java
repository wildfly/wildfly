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
import java.io.FileOutputStream;
import java.net.URL;
import java.security.Security;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.directory.api.ldap.model.entry.DefaultEntry;
import org.apache.directory.api.ldap.model.ldif.LdifEntry;
import org.apache.directory.api.ldap.model.ldif.LdifReader;
import org.apache.directory.api.ldap.model.schema.SchemaManager;
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
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.OperateOnDeployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.as.arquillian.api.ServerSetup;
import org.jboss.as.arquillian.api.ServerSetupTask;
import org.jboss.as.arquillian.container.ManagementClient;
import org.jboss.as.security.Constants;
import org.jboss.as.test.categories.CommonCriteria;
import org.jboss.as.test.integration.security.common.AbstractSecurityDomainsServerSetupTask;
import org.jboss.as.test.integration.security.common.AbstractSystemPropertiesServerSetupTask;
import org.jboss.as.test.integration.security.common.ManagedCreateLdapServer;
import org.jboss.as.test.integration.security.common.Utils;
import org.jboss.as.test.integration.security.common.config.SecurityDomain;
import org.jboss.as.test.integration.security.common.config.SecurityModule;
import org.jboss.as.test.integration.security.common.servlets.SimpleSecuredServlet;
import org.jboss.as.test.integration.security.common.servlets.SimpleServlet;
import org.jboss.logging.Logger;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;

/**
 * A LdapLoginModuleTestCase.
 *
 * @author Josef Cacek
 */
@RunWith(Arquillian.class)
@ServerSetup({LdapLoginModuleTestCase.SystemPropertiesSetup.class, LdapLoginModuleTestCase.LDAPServerSetupTask.class,
        LdapLoginModuleTestCase.SecurityDomainsSetup.class})
@RunAsClient
@Category(CommonCriteria.class)
public class LdapLoginModuleTestCase {

    private static Logger LOGGER = Logger.getLogger(LdapLoginModuleTestCase.class);

    private static final String SECURITY_DOMAIN_LDAP = "test-ldap";
    private static final String SECURITY_DOMAIN_LDAPS = "test-ldaps";

    private static final String KEYSTORE_FILENAME = "ldaps.jks";
    private static final File KEYSTORE_FILE = new File(KEYSTORE_FILENAME);
    private static final int LDAP_PORT = 10389;
    private static final int LDAPS_PORT = 10636;

    private static final String INITIAL_CONTEXT_FACTORY = "com.sun.jndi.ldap.LdapCtxFactory";
    private static final String SECURITY_AUTHENTICATION = "simple";
    private static final String SECURITY_CREDENTIALS = "secret";
    private static final String SECURITY_PRINCIPAL = "uid=admin,ou=system";

    @ArquillianResource
    ManagementClient mgmtClient;

    // Public methods --------------------------------------------------------

    /**
     * Creates {@link WebArchive} with the {@link OKServlet}.
     *
     * @return
     */
    @Deployment(name = SECURITY_DOMAIN_LDAP, testable = false)
    public static WebArchive deploymentLdap() {
        return createWar(SECURITY_DOMAIN_LDAP);
    }

    /**
     * Creates {@link WebArchive} with the {@link OKServlet}.
     *
     * @return
     */
    @Deployment(name = SECURITY_DOMAIN_LDAPS, testable = false)
    public static WebArchive deploymentLdaps() {
        return createWar(SECURITY_DOMAIN_LDAPS);
    }

    /**
     * Test ldap protocol.
     *
     * @throws Exception
     */
    @Test
    @OperateOnDeployment(SECURITY_DOMAIN_LDAP)
    public void testLdap(@ArquillianResource URL webAppURL) throws Exception {
        testAccess(webAppURL);
    }

    /**
     * Test ldaps protocol.
     *
     * @throws Exception
     */
    @Test
    @OperateOnDeployment(SECURITY_DOMAIN_LDAPS)
    public void testLdaps(@ArquillianResource URL webAppURL) throws Exception {
        testAccess(webAppURL);
    }

    // Private methods -------------------------------------------------------

    /**
     * Tests access to the given web application URL.
     *
     * @param webAppURL
     * @throws Exception
     */
    private void testAccess(@ArquillianResource URL webAppURL) throws Exception {
        final URL servletURL = new URL(webAppURL.toExternalForm() + SimpleSecuredServlet.SERVLET_PATH.substring(1));
        LOGGER.trace("Testing successful authentication - " + servletURL);
        assertEquals("Expected response body doesn't match the returned one.", SimpleSecuredServlet.RESPONSE_BODY,
                Utils.makeCallWithBasicAuthn(servletURL, "jduke", "theduke", 200));

        LOGGER.trace("Testing failed authentication - " + servletURL);
        Utils.makeCallWithBasicAuthn(servletURL, "anil", "theduke", 401);
        Utils.makeCallWithBasicAuthn(servletURL, "jduke", "anil", 401);
        Utils.makeCallWithBasicAuthn(servletURL, "anil", "anil", 401);

        LOGGER.trace("Testing failed authorization - " + servletURL);
        Utils.makeCallWithBasicAuthn(servletURL, "tester", "password", 403);

        final URL unprotectedURL = new URL(webAppURL.toExternalForm() + SimpleServlet.SERVLET_PATH.substring(1));
        LOGGER.trace("Testing access to unprotected resource - " + unprotectedURL);
        assertEquals("Expected response body doesn't match the returned one.", SimpleServlet.RESPONSE_BODY,
                Utils.makeCallWithBasicAuthn(unprotectedURL, null, null, 200));
    }

    /**
     * Creates Web application for given security domain testing.
     *
     * @param securityDomain
     * @return
     */
    private static WebArchive createWar(final String securityDomain) {
        LOGGER.trace("Creating deployment: " + securityDomain);
        final WebArchive war = ShrinkWrap.create(WebArchive.class, securityDomain + ".war");
        war.addClasses(SimpleSecuredServlet.class, SimpleServlet.class);
        war.addAsWebInfResource(LdapLoginModuleTestCase.class.getPackage(), "web-basic-authn.xml", "web.xml");
        war.addAsWebInfResource(new StringAsset("<jboss-web><security-domain>" + securityDomain
                + "</security-domain></jboss-web>"), "jboss-web.xml");
        return war;
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
            return new SystemProperty[]{new DefaultSystemProperty("javax.net.ssl.trustStore", KEYSTORE_FILE.getAbsolutePath())};
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
        protected SecurityDomain[] getSecurityDomains() throws Exception {
            Map<String, String> moduleOptions = new HashMap<String, String>();

            // InitialContextFactory implementation class name. This defaults to the Sun LDAP provider implementation
            // com.sun.jndi.ldap.LdapCtxFactory.
            moduleOptions.put("java.naming.factory.initial", INITIAL_CONTEXT_FACTORY);

            // LDAP URL for the LDAP server.
            moduleOptions.put("java.naming.provider.url", "ldap://" + Utils.getSecondaryTestAddress(managementClient) + ":"
                    + LDAP_PORT);

            // Security level to use. This defaults to simple.
            moduleOptions.put("java.naming.security.authentication", SECURITY_AUTHENTICATION);

            // Transport protocol to use for secure access, such as, SSL.
            // moduleOptions.put("java.naming.security.protocol","");

            // Principal for authenticating the caller to the service. This is built from other properties as described below.
            moduleOptions.put("java.naming.security.principal", SECURITY_PRINCIPAL);

            // Authentication scheme to use. For example, hashed password, clear-text password, key, certificate, and so on.
            moduleOptions.put("java.naming.security.credentials", SECURITY_CREDENTIALS);

            // Prefix added to the username to form the user distinguished name. See principalDNSuffix for more info.
            moduleOptions.put("principalDNPrefix", "uid=");

            // Suffix added to the username when forming the user distinguished name. This is useful if you prompt a user for a
            // username and you don't want the user to have to enter the fully distinguished name. Using this property and
            // principalDNSuffix the userDN will be formed as principalDNPrefix + username + principalDNSuffix
            moduleOptions.put("principalDNSuffix", ",ou=People,dc=jboss,dc=org");

            // Value that indicates the credential should be obtained as an opaque Object using the
            // org.jboss.security.auth.callback.ObjectCallback type of Callback rather than as a char[] password using a JAAS
            // PasswordCallback. This allows for passing non-char[] credential information to the LDAP server. The available
            // values are true and false.
            // moduleOptions.put("useObjectCredential","");

            // Fixed, distinguished name to the context to search for user roles.
            moduleOptions.put("rolesCtxDN", "ou=Roles,dc=jboss,dc=org");

            // Name of an attribute in the user object that contains the distinguished name to the context to search for user
            // roles. This differs from rolesCtxDN in that the context to search for a user's roles can be unique for each user.
            // moduleOptions.put("userRolesCtxDNAttributeName","");

            // Name of the attribute containing the user roles. If not specified, this defaults to roles.
            // moduleOptions.put("roleAttributeID","");

            // Flag indicating whether the roleAttributeID contains the fully distinguished name of a role object, or the role
            // name. The role name is taken from the value of the roleNameAttributeId attribute of the context name by the
            // distinguished name.
            // If true, the role attribute represents the distinguished name of a role object. If false, the role name is taken
            // from the value of roleAttributeID. The default is false.
            // Note: In certain directory schemas (e.g., MS ActiveDirectory), role attributes in the user object are stored as
            // DNs to role objects instead of simple names. For implementations that use this schema type, roleAttributeIsDN
            // must be set to true.
            moduleOptions.put("roleAttributeIsDN", "false");

            // Name of the attribute containing the user roles. If not specified, this defaults to roles.
            moduleOptions.put("roleAttributeID", "cn");

            // Name of the attribute in the object containing the user roles that corresponds to the userid. This is used to
            // locate the user roles. If not specified this defaults to uid.
            moduleOptions.put("uidAttributeID", "member");

            // Flag that specifies whether the search for user roles should match on the user's fully distinguished name. If
            // true, the full userDN is used as the match value. If false, only the username is used as the match value against
            // the uidAttributeName attribute. The default value is false.
            moduleOptions.put("matchOnUserDN", "true");

            // A flag indicating if empty (length 0) passwords should be passed to the LDAP server. An empty password is treated
            // as an anonymous login by some LDAP servers and this may not be a desirable feature. To reject empty passwords,
            // set this to false. If set to true, the LDAP server will validate the empty password. The default is true.
            // moduleOptions.put("allowEmptyPasswords","");

            final SecurityDomain sdLdap = new SecurityDomain.Builder()
                    .name(SECURITY_DOMAIN_LDAP)
                    .loginModules(
                            new SecurityModule.Builder().name("Ldap").flag(Constants.SUFFICIENT).options(moduleOptions).build())
                    .build();

            moduleOptions.put("java.naming.provider.url", "ldaps://" + Utils.getSecondaryTestAddress(managementClient) + ":"
                    + LDAPS_PORT);
            final SecurityDomain sdLdaps = new SecurityDomain.Builder()
                    .name(SECURITY_DOMAIN_LDAPS)
                    .loginModules(
                            new SecurityModule.Builder().name("Ldap").flag(Constants.SUFFICIENT).options(moduleOptions).build())
                    .build();

            return new SecurityDomain[]{sdLdap, sdLdaps};
        }

    }

    /**
     * A server setup task which configures and starts LDAP server.
     */
    //@formatter:off
    @CreateDS(
            name = "JBossDS-LdapLoginModuleTestCase",
            factory = org.jboss.as.test.integration.ldap.InMemoryDirectoryServiceFactory.class,
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
                                                            "objectClass: domain\n\n"),
                                    indexes =
                                            {
                                                    @CreateIndex(attribute = "objectClass"),
                                                    @CreateIndex(attribute = "dc"),
                                                    @CreateIndex(attribute = "ou")
                                            })
                    },
            additionalInterceptors = {KeyDerivationInterceptor.class})
    @CreateLdapServer(
            transports =
                    {
                            @CreateTransport(protocol = "LDAP", port = LDAP_PORT, address = "0.0.0.0"),
                            @CreateTransport(protocol = "LDAPS", port = LDAPS_PORT, address = "0.0.0.0")
                    },
//        keyStore="localhost-ldap.jks",
            certificatePassword = "secret")
    //@formatter:on
    static class LDAPServerSetupTask implements ServerSetupTask {

        private DirectoryService directoryService;
        private LdapServer ldapServer;
        private boolean removeBouncyCastle = false;

        /**
         * Creates directory services, starts LDAP server and KDCServer
         *
         * @param managementClient
         * @param containerId
         * @throws Exception
         * @see org.jboss.as.arquillian.api.ServerSetupTask#setup(org.jboss.as.arquillian.container.ManagementClient,
         * java.lang.String)
         */
        public void setup(ManagementClient managementClient, String containerId) throws Exception {
            try {
                if (Security.getProvider(BouncyCastleProvider.PROVIDER_NAME) == null) {
                    Security.addProvider(new BouncyCastleProvider());
                    removeBouncyCastle = true;
                }
            } catch (SecurityException ex) {
                LOGGER.warn("Cannot register BouncyCastleProvider", ex);
            }
            directoryService = DSAnnotationProcessor.getDirectoryService();
            final SchemaManager schemaManager = directoryService.getSchemaManager();
            try {
                for (LdifEntry ldifEntry : new LdifReader(
                        LdapLoginModuleTestCase.class.getResourceAsStream(LdapLoginModuleTestCase.class.getSimpleName()
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
            Utils.fixApacheDSTransportAddress(createLdapServer, Utils.getSecondaryTestAddress(managementClient, false));
            ldapServer = ServerAnnotationProcessor.instantiateLdapServer(createLdapServer, directoryService);
            ldapServer.start();
        }

        /**
         * Stops LDAP server and KDCServer and shuts down the directory service.
         *
         * @param managementClient
         * @param containerId
         * @throws Exception
         * @see org.jboss.as.arquillian.api.ServerSetupTask#tearDown(org.jboss.as.arquillian.container.ManagementClient,
         * java.lang.String)
         */
        public void tearDown(ManagementClient managementClient, String containerId) throws Exception {
            ldapServer.stop();
            directoryService.shutdown();
            KEYSTORE_FILE.delete();
            FileUtils.deleteDirectory(directoryService.getInstanceLayout().getInstanceDirectory());
            if (removeBouncyCastle) {
                try {
                    Security.removeProvider(BouncyCastleProvider.PROVIDER_NAME);
                } catch (SecurityException ex) {
                    LOGGER.warn("Cannot deregister BouncyCastleProvider", ex);
                }
            }
        }
    }

}
