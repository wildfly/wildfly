/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2013, Red Hat, Inc., and individual contributors
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
package org.jboss.as.test.manualmode.security;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;

import javax.servlet.http.HttpServletResponse;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
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
import org.apache.directory.server.ldap.handlers.ssl.LdapsInitializer;
import org.apache.directory.server.protocol.shared.transport.TcpTransport;
import org.apache.directory.server.protocol.shared.transport.Transport;
import org.jboss.arquillian.container.test.api.ContainerController;
import org.jboss.arquillian.container.test.api.Deployer;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.OperateOnDeployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.junit.InSequence;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.as.arquillian.api.ServerSetupTask;
import org.jboss.as.arquillian.container.ManagementClient;
import org.jboss.as.controller.client.ModelControllerClient;
import org.jboss.as.test.integration.security.common.AbstractSecurityDomainsServerSetupTask;
import org.jboss.as.test.integration.security.common.AbstractSecurityRealmsServerSetupTask;
import org.jboss.as.test.integration.security.common.AbstractSystemPropertiesServerSetupTask;
import org.jboss.as.test.integration.security.common.ManagedCreateLdapServer;
import org.jboss.as.test.integration.security.common.ManagedCreateTransport;
import org.jboss.as.test.integration.security.common.Utils;
import org.jboss.as.test.integration.security.common.config.SecurityDomain;
import org.jboss.as.test.integration.security.common.config.SecurityModule;
import org.jboss.as.test.integration.security.common.config.SecurityModule.Builder;
import org.jboss.as.test.integration.security.common.config.realm.Authentication;
import org.jboss.as.test.integration.security.common.config.realm.Authorization;
import org.jboss.as.test.integration.security.common.config.realm.LdapAuthentication;
import org.jboss.as.test.integration.security.common.config.realm.RealmKeystore;
import org.jboss.as.test.integration.security.common.config.realm.SecurityRealm;
import org.jboss.as.test.integration.security.common.config.realm.ServerIdentity;
import org.jboss.as.test.shared.TestSuiteEnvironment;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * A testcase which tests a SecurityRealm used as a SSL configuration source for LDAPs.<br/>
 * This test uses a simple re-implementation of ApacheDS {@link LdapsInitializer} class, which enables to set our own
 * TrustManager and ask for client authentication.<br/>
 * Test scenario:
 * <ol>
 * <li>start container</li>
 * <li>Start LDAP server with LDAPs protocol - use {@link TrustAndStoreTrustManager} as a TrustManager for incoming connections.
 * </li>
 * <li>configure security realms and LDAP outbound connection (one security realm with SSL configuration, ldap-connection using
 * the first security realm, second security realm with LDAP authentication using the new LDAP connection)</li>
 * <li>configure security domain, which uses RealmDirect login module pointing on the new security realm with LDAP authn</li>
 * <li>deploy protected web application, which uses security domain from the last step</li>
 * <li>test access to the web-app</li>
 * <li>test if the server certificate configured in the security realm was used for client authentication on LDAP server side
 * (use {@link TrustAndStoreTrustManager#isSubjectInClientCertChain(String)})</li>
 * <li>undo the changes</li>
 * </ol>
 *
 * @author Josef Cacek
 */
@RunWith(Arquillian.class)
@RunAsClient
public class OutboundLdapConnectionTestCase {

    private static final String KEYSTORE_PASSWORD = "123456";
    private static final String KEYSTORE_FILENAME_LDAPS = "ldaps.keystore";
    private static final String KEYSTORE_FILENAME_JBAS = "jbas.keystore";
    private static final String TRUSTSTORE_FILENAME_JBAS = "jbas.truststore";
    private static final File KEYSTORE_FILE_LDAPS = new File(KEYSTORE_FILENAME_LDAPS);
    private static final File KEYSTORE_FILE_JBAS = new File(KEYSTORE_FILENAME_JBAS);
    private static final File TRUSTSTORE_FILE_JBAS = new File(TRUSTSTORE_FILENAME_JBAS);

    private static final int LDAPS_PORT = 10636;

    private static final String CONTAINER = "default-jbossas";

    private static final String TEST_FILE = "test.txt";
    private static final String TEST_FILE_CONTENT = "OK";

    private static final String LDAPS_AUTHN_REALM = "ldaps-authn-realm";
    private static final String LDAPS_AUTHN_SD = "ldaps-authn-sd";
    private static final String SSL_CONF_REALM = "ssl-conf-realm";
    private static final String LDAPS_CONNECTION = "test-ldaps";

    private static final String LDAPS_AUTHN_REALM_NO_SSL = "ldaps-authn-realm-no-ssl";
    private static final String LDAPS_AUTHN_SD_NO_SSL = "ldaps-authn-sd-no-ssl";

    private static final String SECURITY_CREDENTIALS = "secret";
    private static final String SECURITY_PRINCIPAL = "uid=admin,ou=system";

    private final LDAPServerSetupTask ldapsSetup = new LDAPServerSetupTask();
    private final SecurityRealmsSetup realmsSetup = new SecurityRealmsSetup();
    private final SecurityDomainsSetup domainsSetup = new SecurityDomainsSetup();
    private final SystemPropertiesSetup systemPropertiesSetup = new SystemPropertiesSetup();
    private static boolean serverConfigured = false;

    @ArquillianResource
    private static ContainerController containerController;

    @ArquillianResource
    Deployer deployer;

    @BeforeClass
    public static void beforeTests() throws Exception {
        GenerateLdapConnectionStores.setUpKeyStores();
    }

    @AfterClass
    public static void afterTests() {
        GenerateLdapConnectionStores.removeKeyStores();
    }

    @Before
    public void initializeRoleConfiguration() throws Exception {
        if (containerController.isStarted(CONTAINER) && !serverConfigured) {
            final ModelControllerClient client = TestSuiteEnvironment.getModelControllerClient();
            ManagementClient mgmtClient = new ManagementClient(client, TestSuiteEnvironment.getServerAddress(),
                    TestSuiteEnvironment.getServerPort(), "remote+http");
            prepareServer(mgmtClient);
        }
    }

    @After
    public void restoreRoleConfiguration() throws Exception {
        if (serverConfigured && containerController.isStarted(CONTAINER)) {
            final ModelControllerClient client = TestSuiteEnvironment.getModelControllerClient();
            ManagementClient mgmtClient = new ManagementClient(client, TestSuiteEnvironment.getServerAddress(),
                    TestSuiteEnvironment.getServerPort(), "remote+http");
            cleanUpServer(mgmtClient);
        }
    }

    public void prepareServer(ManagementClient mgmtClient) throws Exception {
        serverConfigured = true;
        createTempKS(KEYSTORE_FILENAME_LDAPS, KEYSTORE_FILE_LDAPS);
        createTempKS(KEYSTORE_FILENAME_JBAS, KEYSTORE_FILE_JBAS);
        createTempKS(TRUSTSTORE_FILENAME_JBAS, TRUSTSTORE_FILE_JBAS);
        ldapsSetup.startLdapServer();
        realmsSetup.setup(mgmtClient, CONTAINER);
        domainsSetup.setup(mgmtClient, CONTAINER);
        systemPropertiesSetup.setup(mgmtClient, CONTAINER);
    }

    private void createTempKS(final String keystoreFilename, final File keystoreFile) throws IOException {
        try (InputStream is = getClass().getResourceAsStream(keystoreFilename);
                FileOutputStream fos = new FileOutputStream(keystoreFile)) {
            IOUtils.copy(is, fos);
        }
    }

    public void cleanUpServer(ManagementClient mgmtClient) throws Exception {
        KEYSTORE_FILE_LDAPS.delete();
        KEYSTORE_FILE_JBAS.delete();
        TRUSTSTORE_FILE_JBAS.delete();
        realmsSetup.tearDown(mgmtClient, CONTAINER);
        ldapsSetup.shutdownLdapServer();
        domainsSetup.tearDown(mgmtClient, CONTAINER);
        systemPropertiesSetup.tearDown(mgmtClient, CONTAINER);
    }

    @Deployment(name = LDAPS_AUTHN_SD, managed = false, testable = false)
    public static WebArchive deployment() {
        return createDeployment(LDAPS_AUTHN_SD);
    }

    @Deployment(name = LDAPS_AUTHN_SD_NO_SSL, managed = false, testable = false)
    public static WebArchive deploymentNoSsl() {
        return createDeployment(LDAPS_AUTHN_SD_NO_SSL);
    }

    @Test
    @InSequence(-2)
    public void startContainer() throws Exception {
        containerController.start(CONTAINER);
    }

    @Test
    @InSequence(0)
    @OperateOnDeployment(LDAPS_AUTHN_SD)
    public void test(@ArquillianResource ManagementClient mgmtClient) throws Exception {
        final SslCertChainRecorder recorder = ldapsSetup.recorder;
        try {
            deployer.deploy(LDAPS_AUTHN_SD);
            deployer.deploy(LDAPS_AUTHN_SD_NO_SSL);

            final URL appUrlNoSsl = new URL(mgmtClient.getWebUri().toString() + "/" + LDAPS_AUTHN_SD_NO_SSL + "/" + TEST_FILE);
            Utils.makeCallWithBasicAuthn(appUrlNoSsl, "jduke", "theduke", HttpServletResponse.SC_UNAUTHORIZED);
            assertEquals("Number of connections with CN=JBAS client cert", 0, recorder.countCerts("CN=JBAS"));

            final URL appUrl = new URL(mgmtClient.getWebUri().toString() + "/" + LDAPS_AUTHN_SD + "/" + TEST_FILE);
            final String resp = Utils.makeCallWithBasicAuthn(appUrl, "jduke", "theduke", HttpServletResponse.SC_OK);
            assertEquals(TEST_FILE_CONTENT, resp);
            assertEquals("Number of connections with CN=JBAS client cert", 1, recorder.countCerts("CN=JBAS"));
        } finally {
            deployer.undeploy(LDAPS_AUTHN_SD);
            deployer.undeploy(LDAPS_AUTHN_SD_NO_SSL);
            recorder.clear();
        }

    }

    @Test
    @InSequence(2)
    public void stopContainer() throws Exception {
        containerController.stop(CONTAINER);
    }
    // Private methods -------------------------------------------------------

    /**
     * Creates test application for this TestCase.
     *
     * @param securityDomain security domain name, also used as an application name
     * @return
     */
    private static WebArchive createDeployment(final String securityDomain) {
        final WebArchive war = ShrinkWrap.create(WebArchive.class, securityDomain + ".war");
        war.add(new StringAsset(TEST_FILE_CONTENT), TEST_FILE);
        war.addAsWebInfResource(OutboundLdapConnectionTestCase.class.getPackage(), OutboundLdapConnectionTestCase.class
                .getSimpleName() + "-web.xml", "web.xml");
        war.addAsWebInfResource(Utils.getJBossWebXmlAsset(securityDomain), "jboss-web.xml");
        war.addAsResource(new StringAsset("jduke=Admin"), "roles.properties");
        return war;
    }

    // Embedded classes ------------------------------------------------------
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
            final Builder realmDirectLMBuilder = new SecurityModule.Builder().name("RealmDirect");
            final SecurityModule mappingModule = new SecurityModule.Builder().name("SimpleRoles").putOption("jduke", "Admin")
                    .build();

            final SecurityDomain sd1 = new SecurityDomain.Builder().name(LDAPS_AUTHN_SD)
                    .loginModules(realmDirectLMBuilder.putOption("realm", LDAPS_AUTHN_REALM).build())
                    .mappingModules(mappingModule).build();
            final SecurityDomain sd2 = new SecurityDomain.Builder().name(LDAPS_AUTHN_SD_NO_SSL)
                    .loginModules(realmDirectLMBuilder.putOption("realm", LDAPS_AUTHN_REALM_NO_SSL).build())
                    .mappingModules(mappingModule).build();
            return new SecurityDomain[]{sd1, sd2};
        }
    }

    /**
     * A {@link ServerSetupTask} instance which creates security realms for this test case.
     *
     * @author Josef Cacek
     */
    static class SecurityRealmsSetup extends AbstractSecurityRealmsServerSetupTask {

        /**
         * Returns SecurityRealms configuration for this testcase.
         */
        @Override
        protected SecurityRealm[] getSecurityRealms() {
            final RealmKeystore.Builder keyStoreBuilder = new RealmKeystore.Builder().keystorePassword(KEYSTORE_PASSWORD);
            final String ldapsUrl = "ldaps://" + Utils.getSecondaryTestAddress(managementClient) + ":" + LDAPS_PORT;

            final SecurityRealm sslConfRealm = new SecurityRealm.Builder().name(SSL_CONF_REALM).authentication(
                    new Authentication.Builder().truststore(keyStoreBuilder.keystorePath(TRUSTSTORE_FILE_JBAS.getAbsolutePath())
                    .build()).build()).serverIdentity(new ServerIdentity.Builder().ssl(
                    keyStoreBuilder.keystorePath(KEYSTORE_FILE_JBAS.getAbsolutePath()).build()).build()).build();
            final SecurityRealm ldapsAuthRealm = new SecurityRealm.Builder().name(LDAPS_AUTHN_REALM).authentication(
                    new Authentication.Builder().ldap(new LdapAuthentication.Builder()
                    // shared attributes
                    .connection(LDAPS_CONNECTION)
                    // ldap-connection
                    .url(ldapsUrl).searchDn(SECURITY_PRINCIPAL).searchCredential(SECURITY_CREDENTIALS).securityRealm(SSL_CONF_REALM)
                    // ldap authentication
                    .baseDn("ou=People,dc=jboss,dc=org").recursive(Boolean.TRUE).usernameAttribute("uid").build()).build())
                    .authorization(new Authorization.Builder().path("application-roles.properties")
                    .relativeTo("jboss.server.config.dir").build()).build();
            return new SecurityRealm[]{sslConfRealm, ldapsAuthRealm};
        }
    }

    /**
     * This setup task disables hostname verification truststore file.
     */
    static class SystemPropertiesSetup extends AbstractSystemPropertiesServerSetupTask {

        /**
         * @see org.jboss.as.test.integration.security.common.AbstractSystemPropertiesServerSetupTask#getSystemProperties()
         */
        @Override
        protected SystemProperty[] getSystemProperties() {
            return new SystemProperty[]{new DefaultSystemProperty("com.sun.jndi.ldap.object.disableEndpointIdentification","")};
        }
    }

    /**
     * A server setup task which configures and starts LDAP server.
     */
    //@formatter:off
    @CreateDS(
            name = "JBossDS-OutboundLdapConnectionTestCase",
            factory = org.jboss.as.test.integration.ldap.InMemoryDirectoryServiceFactory.class,
            partitions = {
                @CreatePartition(
                name = "jboss",
                suffix = "dc=jboss,dc=org",
                contextEntry = @ContextEntry(
                entryLdif = "dn: dc=jboss,dc=org\n"
                + "dc: jboss\n"
                + "objectClass: top\n"
                + "objectClass: domain\n\n"),
                indexes = {
                    @CreateIndex(attribute = "objectClass"),
                    @CreateIndex(attribute = "dc"),
                    @CreateIndex(attribute = "ou")
                }
                )
            },
            additionalInterceptors = {KeyDerivationInterceptor.class})
    @CreateLdapServer(
            transports = {
                @CreateTransport(protocol = "LDAPS", port = LDAPS_PORT, address = "0.0.0.0")
            },
            certificatePassword = KEYSTORE_PASSWORD)
    //@formatter:on
    static class LDAPServerSetupTask {

        private DirectoryService directoryService;
        private LdapServer ldapServer;
        private final SslCertChainRecorder recorder = new SslCertChainRecorder();

        /**
         * Creates directory services, starts LDAP server and KDCServer.
         */
        public void startLdapServer() throws Exception {
            LdapsInitializer.setAndLockRecorder(recorder);
            directoryService = DSAnnotationProcessor.getDirectoryService();
            final SchemaManager schemaManager = directoryService.getSchemaManager();
            try (LdifReader ldifReader = new LdifReader(OutboundLdapConnectionTestCase.class.getResourceAsStream(
                    "OutboundLdapConnectionTestCase.ldif"))) {
                for (LdifEntry ldifEntry : ldifReader) {
                    directoryService.getAdminSession().add(new DefaultEntry(schemaManager, ldifEntry.getEntry()));
                }
            }
            final ManagedCreateLdapServer createLdapServer = new ManagedCreateLdapServer(
                    (CreateLdapServer) AnnotationUtils.getInstance(CreateLdapServer.class));
            createLdapServer.setKeyStore(KEYSTORE_FILE_LDAPS.getAbsolutePath());
            fixTransportAddress(createLdapServer, StringUtils.strip(TestSuiteEnvironment.getSecondaryTestAddress(false)));
            ldapServer = ServerAnnotationProcessor.instantiateLdapServer(createLdapServer, directoryService);

            /* set setWantClientAuth(true) manually as there is no way to do this via annotation */
            Transport[] transports = ldapServer.getTransports();
            assertTrue("The LDAP server configured via annotations should have just one transport", transports.length == 1);
            final TcpTransport transport = (TcpTransport) transports[0];
            transport.setWantClientAuth(true);
            TcpTransport newTransport = new InitializedTcpTransport(transport);
            ldapServer.setTransports(newTransport);

            assertEquals(ldapServer.getCertificatePassword(),KEYSTORE_PASSWORD);
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
         */
        public void shutdownLdapServer() throws Exception {
            ldapServer.stop();
            directoryService.shutdown();
            FileUtils.deleteDirectory(directoryService.getInstanceLayout().getInstanceDirectory());
            LdapsInitializer.unsetAndUnlockRecorder();
        }
    }
}
