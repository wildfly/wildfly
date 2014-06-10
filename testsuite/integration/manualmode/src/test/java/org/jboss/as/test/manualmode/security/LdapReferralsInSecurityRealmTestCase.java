/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2014, Red Hat, Inc., and individual contributors
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

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.LDAP_CONNECTION;
import static org.junit.Assert.assertEquals;

import java.net.URL;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.HttpServletResponse;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.text.StrSubstitutor;
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
import org.apache.directory.server.factory.ServerAnnotationProcessor;
import org.apache.directory.server.ldap.LdapServer;
import org.jboss.arquillian.container.test.api.ContainerController;
import org.jboss.arquillian.container.test.api.Deployer;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.junit.InSequence;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.as.arquillian.api.ServerSetupTask;
import org.jboss.as.arquillian.container.ManagementClient;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.client.ModelControllerClient;
import org.jboss.as.controller.operations.common.Util;
import org.jboss.as.domain.management.CoreManagementResourceDefinition;
import org.jboss.as.network.NetworkUtils;
import org.jboss.as.test.integration.security.common.AbstractSecurityDomainsServerSetupTask;
import org.jboss.as.test.integration.security.common.AbstractSecurityRealmsServerSetupTask;
import org.jboss.as.test.integration.security.common.ManagedCreateLdapServer;
import org.jboss.as.test.integration.security.common.ManagedCreateTransport;
import org.jboss.as.test.integration.security.common.Utils;
import org.jboss.as.test.integration.security.common.config.SecurityDomain;
import org.jboss.as.test.integration.security.common.config.SecurityModule;
import org.jboss.as.test.integration.security.common.config.SecurityModule.Builder;
import org.jboss.as.test.integration.security.common.config.realm.Authentication;
import org.jboss.as.test.integration.security.common.config.realm.Authorization;
import org.jboss.as.test.integration.security.common.config.realm.LdapAuthentication;
import org.jboss.as.test.integration.security.common.config.realm.SecurityRealm;
import org.jboss.as.test.shared.TestSuiteEnvironment;
import org.jboss.dmr.ModelNode;
import org.jboss.logging.Logger;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.jboss.vfs.VFSUtils;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * A testcase which tests referral attribute in ldap outbound-connection using in security realm.
 * 
 * @author olukas
 * 
 */
@RunWith(Arquillian.class)
@RunAsClient
public class LdapReferralsInSecurityRealmTestCase {
    
    private static final String CONTAINER = "default-jbossas";
    
    private static final String DEPLOYMENT_BASIC = "deployment-basic";
    private static final String DEPLOYMENT_REFERRAL = "deployment-referral";
    private static final String LDAP_BASIC_REALM = "ldap-basic-realm";
    private static final String LDAP_REFERRAL_REALM = "ldap-referral-realm";
    private static final String LDAP_BASIC_DOMAIN = "ldap-basic-sd";
    private static final String LDAP_REFERRAL_DOMAIN = "ldap-referral-sd";
    private static final String LDAP_BASIC_CONNECTION_NAME = "test-ldap-basic";
    private static final String LDAP_REFERRAL_CONNECTION_NAME = "test-ldap-referral";

    private static final String SECURITY_CREDENTIALS = "secret";
    private static final String SECURITY_PRINCIPAL = "uid=admin,ou=system";
    private static final String PASSWORD = "password123";
    private static final String USER = "jduke";
    private static final String REFERRAL_USER = "referraluser";

    private static final String IGNORE = "IGNORE";
    private static final String FOLLOW = "FOLLOW";
    private static final String THROW = "THROW";

    private static final String TEST_FILE = "test.html";
    private static final String TEST_FILE_CONTENT = "OK";

    static final int LDAP_PORT = 10389;
    static final int LDAP_PORT2 = 11389;

    private final LDAPServerSetupTask ldapSetup = new LDAPServerSetupTask();
    private final LDAPServerSetupTask2 ldapSetup2 = new LDAPServerSetupTask2();
    private final SecurityRealmsSetup realmsSetup = new SecurityRealmsSetup();
    private final SecurityDomainsSetup domainsSetup = new SecurityDomainsSetup();

    private static Logger LOGGER = Logger.getLogger(LdapReferralsInSecurityRealmTestCase.class);
    
    private static ManagementClient managementClient;
    
    @ArquillianResource
    private ContainerController container;
    
    @ArquillianResource
    Deployer deployer;
    
    @Deployment(name = DEPLOYMENT_BASIC, managed = false, testable = false)
    public static WebArchive deployment1() {
        return createDeployment(DEPLOYMENT_BASIC, LDAP_BASIC_DOMAIN);
    }

    @Deployment(name = DEPLOYMENT_REFERRAL, managed = false, testable = false)
    public static WebArchive deployment2() {
        return createDeployment(DEPLOYMENT_REFERRAL, LDAP_REFERRAL_DOMAIN);
    }
    
    @Test
    @InSequence(1)
    public void startContainer() throws Exception {
        container.start(CONTAINER);
        final ModelControllerClient client = TestSuiteEnvironment.getModelControllerClient();
        managementClient = new ManagementClient(client, TestSuiteEnvironment.getServerAddress(),
                TestSuiteEnvironment.getServerPort());
        prepare(managementClient);
    }

    @Test
    @InSequence(2)
    public void testReferrals() throws Exception {
        try {
            deployer.deploy(DEPLOYMENT_BASIC);
            deployer.deploy(DEPLOYMENT_REFERRAL);
            final URL appBasicUrl = new URL(managementClient.getWebUri().toString() + "/" + DEPLOYMENT_BASIC + "/" + TEST_FILE);
            final URL appReferralUrl = new URL(managementClient.getWebUri().toString() + "/" + DEPLOYMENT_REFERRAL + "/"
                    + TEST_FILE);

            tryAuthenticationWithReferral(LDAP_BASIC_CONNECTION_NAME, IGNORE, appBasicUrl,
                    HttpServletResponse.SC_UNAUTHORIZED);

            tryAuthenticationWithReferral(LDAP_REFERRAL_CONNECTION_NAME, FOLLOW, appReferralUrl, HttpServletResponse.SC_OK);

            tryAuthenticationWithReferral(LDAP_REFERRAL_CONNECTION_NAME, THROW, appReferralUrl,
                    HttpServletResponse.SC_UNAUTHORIZED);

        } finally {
            deployer.undeploy(DEPLOYMENT_BASIC);
            deployer.undeploy(DEPLOYMENT_REFERRAL);
        }
    }

    @Test
    @InSequence(3)
    public void stopContainer() throws Exception {
        cleanUp(managementClient);
        container.stop(CONTAINER);
    }
    
    /**
     * Creates a {@link WebArchive} for given security domain.
     * 
     * @param securityDomainName
     * @return
     */
    private static WebArchive createDeployment(String name, String securityDomainName) {
        final WebArchive war = ShrinkWrap.create(WebArchive.class, name + ".war");
        war.add(new StringAsset(TEST_FILE_CONTENT), TEST_FILE);
        war.addAsWebInfResource(LdapReferralsInSecurityRealmTestCase.class.getPackage(),
                LdapReferralsInSecurityRealmTestCase.class.getSimpleName() + "-web.xml", "web.xml");
        war.addAsWebInfResource(Utils.getJBossWebXmlAsset(securityDomainName), "jboss-web.xml");
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug(war.toString(true));
        }
        return war;
    }

    /**
     * Start LDAPs and prepare server.
     * 
     * @param mgmtClient
     * @throws Exception
     */
    public void prepare(ManagementClient mgmtClient) throws Exception {
        ldapSetup.startLdapServer();
        ldapSetup2.startLdapServer();
        realmsSetup.setup(mgmtClient, CONTAINER);
        domainsSetup.setup(mgmtClient, CONTAINER);
    }

    /**
     * Stop LDAPs and clean up server.
     * 
     * @param mgmtClient
     * @throws Exception
     */
    public void cleanUp(ManagementClient mgmtClient) throws Exception {
        realmsSetup.tearDown(mgmtClient, CONTAINER);
        ldapSetup.shutdownLdapServer();
        ldapSetup2.shutdownLdapServer();
        domainsSetup.tearDown(mgmtClient, CONTAINER);
    }

    /**
     * Try to access secured appUrl. Using referral attribute in chosen ldapConnection.
     * 
     * @param ldapConnection used outbound-connection ldap
     * @param referral tested referral attribute
     * @param appUrl secured URL
     * @param expectedStatusForRef expected result of authentication
     * @throws Exception
     */
    private void tryAuthenticationWithReferral(String ldapConnection, String referral, URL appUrl, int expectedStatusForRef)
            throws Exception {
        setLdapReferrals(managementClient, ldapConnection, referral);
        String resp = Utils.makeCallWithBasicAuthn(appUrl, USER, PASSWORD, HttpServletResponse.SC_OK);
        assertEquals(TEST_FILE_CONTENT, resp);
        resp = Utils.makeCallWithBasicAuthn(appUrl, REFERRAL_USER, PASSWORD, expectedStatusForRef);
        if (expectedStatusForRef == HttpServletResponse.SC_OK) {
            assertEquals(TEST_FILE_CONTENT, resp);
        }
    }

    /**
     * Set new referral attribute for outbound-connection ldap.
     * 
     * @param mgmtClient
     * @param ldapConnection used outbound-connection ldap
     * @param value new value of referral attribute
     * @throws Exception
     */
    private void setLdapReferrals(ManagementClient mgmtClient, String ldapConnection, String value) throws Exception {
        ModelNode op = Util.getWriteAttributeOperation(
                PathAddress.pathAddress(CoreManagementResourceDefinition.PATH_ELEMENT,
                        PathElement.pathElement(LDAP_CONNECTION, ldapConnection)), "referrals", new ModelNode(value));
        mgmtClient.getControllerClient().execute(op);
    }

    static class SecurityDomainsSetup extends AbstractSecurityDomainsServerSetupTask {

        /**
         * Returns SecurityDomains configuration for this testcase.
         *
         * @see org.jboss.as.test.integration.security.common.AbstractSecurityDomainsServerSetupTask#getSecurityDomains()
         */
        @Override
        protected SecurityDomain[] getSecurityDomains() {
            final Builder realmDirectLMBuilder = new SecurityModule.Builder().name("RealmDirect");
            final SecurityModule mappingModule = new SecurityModule.Builder().name("SimpleRoles")
                    .putOption(REFERRAL_USER, "Admin").putOption(USER, "Admin").build();

            final SecurityDomain sd1 = new SecurityDomain.Builder().name(LDAP_BASIC_DOMAIN)
                    .loginModules(realmDirectLMBuilder.putOption("realm", LDAP_BASIC_REALM).build())
                    .mappingModules(mappingModule).build();

            final SecurityDomain sd2 = new SecurityDomain.Builder().name(LDAP_REFERRAL_DOMAIN)
                    .loginModules(realmDirectLMBuilder.putOption("realm", LDAP_REFERRAL_REALM).build())
                    .mappingModules(mappingModule).build();

            return new SecurityDomain[] { sd1, sd2 };
        }
    }
    
    /**
     * A {@link ServerSetupTask} instance which creates security realms for this test case.
     */
    static class SecurityRealmsSetup extends AbstractSecurityRealmsServerSetupTask {

        /**
         * Returns SecurityRealms configuration for this testcase.
         */
        @Override
        protected SecurityRealm[] getSecurityRealms() {
            final String ldapUrl = "ldap://" + Utils.getSecondaryTestAddress(managementClient) + ":" + LDAP_PORT;

            final SecurityRealm ldapReferralRealm = new SecurityRealm.Builder()
                    .name(LDAP_REFERRAL_REALM)
                    .authentication(
                            new Authentication.Builder().ldap(
                                    new LdapAuthentication.Builder()
                                            // shared attributes
                                            .connection(LDAP_REFERRAL_CONNECTION_NAME)
                                            // ldap-connection
                                            .url(ldapUrl).searchDn(SECURITY_PRINCIPAL).searchCredential(SECURITY_CREDENTIALS)
                                            // ldap authentication
                                            .baseDn("dc=jboss,dc=org").recursive(Boolean.TRUE)
                                            .advancedFilter("(|(objectClass=referral)(uid={0}))").build()).build())
                    .authorization(
                            new Authorization.Builder().path("application-roles.properties")
                                    .relativeTo("jboss.server.config.dir").build()).build();

            final SecurityRealm ldapBasicRealm = new SecurityRealm.Builder()
                    .name(LDAP_BASIC_REALM)
                    .authentication(new Authentication.Builder().ldap(new LdapAuthentication.Builder()
                            // shared attributes
                            .connection(LDAP_BASIC_CONNECTION_NAME)
                            // ldap-connection
                            .url(ldapUrl).searchDn(SECURITY_PRINCIPAL).searchCredential(SECURITY_CREDENTIALS)
                            // ldap authentication
                            .baseDn("dc=jboss,dc=org").recursive(Boolean.TRUE).usernameAttribute("uid").build()).build())
                    .authorization(
                            new Authorization.Builder().path("application-roles.properties")
                                    .relativeTo("jboss.server.config.dir").build()).build();

            return new SecurityRealm[] { ldapReferralRealm, ldapBasicRealm };
        }
    }

    /**
     * A server setup task which configures and starts LDAP server.
     */
    //@formatter:off
    @CreateDS(
            name = "JBossDS",
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
            })
    @CreateLdapServer(
            transports = {
                    @CreateTransport(protocol = "LDAP", port = LDAP_PORT, address = "0.0.0.0")
            })
    //@formatter:on
    static class LDAPServerSetupTask {

        private static DirectoryService directoryService;
        private static LdapServer ldapServer;

        /**
         * Creates directory services and starts LDAP server.
         */
        public void startLdapServer() throws Exception {
            final Map<String, String> map = new HashMap<String, String>();
            map.put("hostname", NetworkUtils.formatPossibleIpv6Address(Utils.getSecondaryTestAddress(managementClient, false)));
            map.put("ldapPort2", Integer.toString(LDAP_PORT2));
            directoryService = DSAnnotationProcessor.getDirectoryService();
            
            final String ldifContent = StrSubstitutor.replace(
                    IOUtils.toString(
                            LdapReferralsInSecurityRealmTestCase.class.getResourceAsStream("LdapReferralsInSecurityRealmTestCase1.ldif"), "UTF-8"), map);
            LOGGER.debug(ldifContent);
            
            final SchemaManager schemaManager = directoryService.getSchemaManager();

            LdifReader ldifReader = new LdifReader(IOUtils.toInputStream(ldifContent));
            try {
                for (LdifEntry ldifEntry : ldifReader) {
                    directoryService.getAdminSession().add(new DefaultEntry(schemaManager, ldifEntry.getEntry()));
                }
            } finally {
                VFSUtils.safeClose(ldifReader);
            }
            final ManagedCreateLdapServer createLdapServer = new ManagedCreateLdapServer(
                    (CreateLdapServer) AnnotationUtils.getInstance(CreateLdapServer.class));
            fixTransportAddress(createLdapServer, StringUtils.strip(TestSuiteEnvironment.getSecondaryTestAddress(false)));
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
         * Stops LDAP server and shuts down the directory service.
         */
        public void shutdownLdapServer() throws Exception {
            ldapServer.stop();
            directoryService.shutdown();
            FileUtils.deleteDirectory(directoryService.getInstanceLayout().getInstanceDirectory());
        }
    }

    
    /**
     * A server setup task which configures and starts LDAP server.
     */
    //@formatter:off
    @CreateDS(
            name = "JBossComDS",
            partitions = {
                @CreatePartition(
                name = "jboss",
                suffix = "dc=jboss,dc=com",
                contextEntry = @ContextEntry(
                entryLdif = "dn: dc=jboss,dc=com\n"
                + "dc: jboss\n"
                + "objectClass: top\n"
                + "objectClass: domain\n\n"),
                indexes = {
                    @CreateIndex(attribute = "objectClass"),
                    @CreateIndex(attribute = "dc"),
                    @CreateIndex(attribute = "ou")
                }
                )
            })
    @CreateLdapServer(
            transports = {
                    @CreateTransport(protocol = "LDAP", port = LDAP_PORT2, address = "0.0.0.0")
            })
    //@formatter:on
    static class LDAPServerSetupTask2 {

        private static DirectoryService directoryService;
        private static LdapServer ldapServer;

        /**
         * Creates directory services and starts LDAP server.
         */
        public void startLdapServer() throws Exception {
            directoryService = DSAnnotationProcessor.getDirectoryService();
            final SchemaManager schemaManager = directoryService.getSchemaManager();
            LdifReader ldifReader = new LdifReader(
                    LdapReferralsInSecurityRealmTestCase.class
                            .getResourceAsStream("LdapReferralsInSecurityRealmTestCase2.ldif"));
            try {
                for (LdifEntry ldifEntry : ldifReader) {
                    directoryService.getAdminSession().add(new DefaultEntry(schemaManager, ldifEntry.getEntry()));
                }
            } finally {
                VFSUtils.safeClose(ldifReader);
            }
            final ManagedCreateLdapServer createLdapServer = new ManagedCreateLdapServer(
                    (CreateLdapServer) AnnotationUtils.getInstance(CreateLdapServer.class));
            fixTransportAddress(createLdapServer, StringUtils.strip(TestSuiteEnvironment.getSecondaryTestAddress(false)));
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
         * Stops LDAP server and shuts down the directory service.
         */
        public void shutdownLdapServer() throws Exception {
            ldapServer.stop();
            directoryService.shutdown();
            FileUtils.deleteDirectory(directoryService.getInstanceLayout().getInstanceDirectory());
        }
    }
}
