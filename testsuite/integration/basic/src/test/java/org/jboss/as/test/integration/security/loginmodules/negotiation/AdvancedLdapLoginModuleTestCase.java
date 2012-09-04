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
package org.jboss.as.test.integration.security.loginmodules.negotiation;

import static org.junit.Assert.*;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.security.PrivilegedActionException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.security.auth.login.LoginException;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.SystemUtils;
import org.apache.commons.lang.text.StrSubstitutor;
import org.apache.directory.server.annotations.CreateLdapServer;
import org.apache.directory.server.annotations.CreateTransport;
import org.apache.directory.server.annotations.SaslMechanism;
import org.apache.directory.server.core.annotations.AnnotationUtils;
import org.apache.directory.server.core.annotations.ContextEntry;
import org.apache.directory.server.core.annotations.CreateDS;
import org.apache.directory.server.core.annotations.CreateIndex;
import org.apache.directory.server.core.annotations.CreatePartition;
import org.apache.directory.server.core.api.DirectoryService;
import org.apache.directory.server.core.factory.DSAnnotationProcessor;
import org.apache.directory.server.core.kerberos.KeyDerivationInterceptor;
import org.apache.directory.server.factory.ServerAnnotationProcessor;
import org.apache.directory.server.kerberos.kdc.KdcServer;
import org.apache.directory.server.ldap.LdapServer;
import org.apache.directory.server.ldap.handlers.bind.cramMD5.CramMd5MechanismHandler;
import org.apache.directory.server.ldap.handlers.bind.digestMD5.DigestMd5MechanismHandler;
import org.apache.directory.server.ldap.handlers.bind.gssapi.GssapiMechanismHandler;
import org.apache.directory.server.ldap.handlers.bind.ntlm.NtlmMechanismHandler;
import org.apache.directory.server.ldap.handlers.bind.plain.PlainMechanismHandler;
import org.apache.directory.shared.ldap.model.constants.SupportedSaslMechanisms;
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
import org.jboss.as.test.integration.security.common.ExtCreateKdcServer;
import org.jboss.as.test.integration.security.common.KDCServerAnnotationProcessor;
import org.jboss.as.test.integration.security.common.ManagedCreateLdapServer;
import org.jboss.as.test.integration.security.common.ManagedCreateTransport;
import org.jboss.as.test.integration.security.common.Utils;
import org.jboss.as.test.integration.security.common.config.SecurityDomain;
import org.jboss.as.test.integration.security.common.config.SecurityModule;
import org.jboss.as.test.integration.security.loginmodules.common.servlets.PrincipalPrintingServlet;
import org.jboss.as.test.integration.security.loginmodules.common.servlets.RolePrintingServlet;
import org.jboss.as.test.integration.security.loginmodules.common.servlets.SimpleSecuredServlet;
import org.jboss.as.test.integration.security.loginmodules.common.servlets.SimpleServlet;
import org.jboss.logging.Logger;
import org.jboss.security.SecurityConstants;
import org.jboss.security.negotiation.AdvancedLdapLoginModule;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * JUnit testcase for AdvancedLdapLoginModule. It's slightly modified version of
 * {@link org.jboss.as.test.integration.security.loginmodules.LdapExtLoginModuleTestCase}.
 * 
 * @author Josef Cacek
 */
@RunWith(Arquillian.class)
@ServerSetup({ Krb5ConfServerSetupTask.class, // 
        AdvancedLdapLoginModuleTestCase.KerberosSystemPropertiesSetupTask.class, //
        AdvancedLdapLoginModuleTestCase.DirectoryServerSetupTask.class, //
        AdvancedLdapLoginModuleTestCase.SecurityDomainsSetup.class })
@RunAsClient
public class AdvancedLdapLoginModuleTestCase {
    private static Logger LOGGER = Logger.getLogger(AdvancedLdapLoginModuleTestCase.class);

    /** The SECURITY_DOMAIN_NAME_PREFIX */
    public static final String SECURITY_DOMAIN_NAME_PREFIX = "ldap-test-";

    /** The KRB5_CONF_PATH */
    private static final String KRB5_CONF_PATH = Krb5ConfServerSetupTask.getKrb5ConfFullPath();

    /** The TRUE */
    private static final String TRUE = Boolean.TRUE.toString();

    private static final int LDAP_PORT = 10389;

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
        testDeployment(webAppURL, "TheDuke", "Echo");
    }

    /**
     * Test case for Example 2.
     * 
     * @throws Exception
     */
    @Test
    @OperateOnDeployment(DEP2)
    public void test2(@ArquillianResource URL webAppURL) throws Exception {
        //JBPAPP-10173 - ExtendedLdap LM would contain also "jduke"
        testDeployment(webAppURL, "TheDuke", "Echo");
    }

    /**
     * Test case for Example 3.
     * 
     * @throws Exception
     */
    @Test
    @OperateOnDeployment(DEP3)
    public void test3(@ArquillianResource URL webAppURL) throws Exception {
        testDeployment(webAppURL, "TheDuke", "Echo");
    }

    /**
     * Test case for Example 4.
     * 
     * @throws Exception
     */
    @Test
    @OperateOnDeployment(DEP4)
    public void test4(@ArquillianResource URL webAppURL) throws Exception {
        //JBPAPP-10173 - ExtendedLdap LM would contain also "R1", "R2", "R3"
        //recursion in AdvancedLdapLoginModule is enabled only if the roleAttributeIsDN module option is true. This is not required in LdapExtLogiModule.
        testDeployment(webAppURL, "RG2", "R5");
    }

    // Private methods -------------------------------------------------------

    /**
     * Creates {@link WebArchive} (WAR) for given deployment name.
     * 
     * @param deploymentName
     * @return
     */
    private static WebArchive createWar(final String deploymentName) {
        LOGGER.info("Starting deployment " + deploymentName);

        final WebArchive war = ShrinkWrap.create(WebArchive.class, deploymentName + ".war");
        war.addClasses(SimpleSecuredServlet.class, SimpleServlet.class, RolePrintingServlet.class,
                PrincipalPrintingServlet.class);
        war.addAsWebInfResource(AdvancedLdapLoginModuleTestCase.class.getPackage(),
                AdvancedLdapLoginModuleTestCase.class.getSimpleName() + "-web.xml", "web.xml");
        war.addAsWebInfResource(new StringAsset("<jboss-web>" + //
                "<security-domain>" + deploymentName + "</security-domain>" + //
                "<valve><class-name>org.jboss.security.negotiation.NegotiationAuthenticator</class-name></valve>" + //                
                "</jboss-web>"), "jboss-web.xml");
        war.addAsManifestResource(new StringAsset("<jboss-deployment-structure><deployment><dependencies>" //
                + "<module name='org.jboss.security.negotiation'/>" //
                + "</dependencies></deployment></jboss-deployment-structure>"), //
                "jboss-deployment-structure.xml");

        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug(war.toString(true));
        }
        return war;
    }

    /**
     * Tests role assignment for given deployment (web-app URL).
     */
    private void testDeployment(URL webAppURL, String... assignedRoles) throws MalformedURLException, ClientProtocolException,
            IOException, URISyntaxException, LoginException, PrivilegedActionException {
        final URI rolesPrintingURI = new URI(webAppURL.toExternalForm() + RolePrintingServlet.SERVLET_PATH.substring(1) + "?"
                + QUERY_ROLES);
        final String rolesResponse = Utils.makeCallWithKerberosAuthn(rolesPrintingURI, "jduke", "theduke", KRB5_CONF_PATH, 200);
        final List<String> assignedRolesList = Arrays.asList(assignedRoles);

        for (String role : ROLE_NAMES) {
            if (assignedRolesList.contains(role)) {
                assertInRole(rolesResponse, role);
            } else {
                assertNotInRole(rolesResponse, role);
            }
        }
        final URI principalPrintingURI = new URI(webAppURL.toExternalForm()
                + PrincipalPrintingServlet.SERVLET_PATH.substring(1) + "?" + QUERY_ROLES);
        final String principal = Utils.makeCallWithKerberosAuthn(principalPrintingURI, "jduke", "theduke", KRB5_CONF_PATH, 200);
        assertEquals("Unexpected Principal name", "jduke@JBOSS.ORG", principal);
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
     * A server setup task which configures and starts Kerberos KDC server.
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
                @CreateTransport( protocol = "LDAP",  port = LDAP_PORT)
            },
            saslHost="localhost",
            saslPrincipal="ldap/localhost@JBOSS.ORG",
            saslMechanisms = 
            {
                @SaslMechanism( name=SupportedSaslMechanisms.PLAIN, implClass=PlainMechanismHandler.class ),
                @SaslMechanism( name=SupportedSaslMechanisms.CRAM_MD5, implClass=CramMd5MechanismHandler.class),
                @SaslMechanism( name= SupportedSaslMechanisms.DIGEST_MD5, implClass=DigestMd5MechanismHandler.class),
                @SaslMechanism( name=SupportedSaslMechanisms.GSSAPI, implClass=GssapiMechanismHandler.class),
                @SaslMechanism( name=SupportedSaslMechanisms.NTLM, implClass=NtlmMechanismHandler.class),
                @SaslMechanism( name=SupportedSaslMechanisms.GSS_SPNEGO, implClass=NtlmMechanismHandler.class)
            })
    @ExtCreateKdcServer(primaryRealm = "JBOSS.ORG",
        kdcPrincipal = "krbtgt/JBOSS.ORG@JBOSS.ORG",
        searchBaseDn = "dc=jboss,dc=org",
        transports = 
        {
            @CreateTransport(protocol = "UDP", port = 6088)
        })
    //@formatter:on
    static class DirectoryServerSetupTask implements ServerSetupTask {

        private DirectoryService directoryService;
        private KdcServer kdcServer;
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
            final String hostname = Utils.getCannonicalHost(managementClient);
            final Map<String, String> map = new HashMap<String, String>();
            map.put("hostname", hostname);
            final String ldifContent = StrSubstitutor.replace(
                    IOUtils.toString(
                            SPNEGOLoginModuleTestCase.class.getResourceAsStream(AdvancedLdapLoginModuleTestCase.class
                                    .getSimpleName() + ".ldif"), "UTF-8"), map);
            LOGGER.info(ldifContent);
            final SchemaManager schemaManager = directoryService.getSchemaManager();
            try {
                for (LdifEntry ldifEntry : new LdifReader(IOUtils.toInputStream(ldifContent))) {
                    directoryService.getAdminSession().add(new DefaultEntry(schemaManager, ldifEntry.getEntry()));
                }
            } catch (Exception e) {
                e.printStackTrace();
                throw e;
            }
            kdcServer = KDCServerAnnotationProcessor.getKdcServer(directoryService, 1024, hostname);
            final ManagedCreateLdapServer createLdapServer = new ManagedCreateLdapServer(
                    (CreateLdapServer) AnnotationUtils.getInstance(CreateLdapServer.class));
            fixTransportAddress(createLdapServer, StringUtils.strip(Utils.getSecondaryTestAddress(managementClient), "[]"));
            ldapServer = ServerAnnotationProcessor.instantiateLdapServer(createLdapServer, directoryService);
            ldapServer.setSearchBaseDn("dc=jboss,dc=org");
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
            kdcServer.stop();
            directoryService.shutdown();
            FileUtils.deleteDirectory(directoryService.getInstanceLayout().getInstanceDirectory());
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
            final SecurityModule.Builder kerberosModuleBuilder = new SecurityModule.Builder();
            if (SystemUtils.JAVA_VENDOR.startsWith("IBM")) {
                //http://www.ibm.com/developerworks/java/jdk/security/60/secguides/jgssDocs/api/com/ibm/security/auth/module/Krb5LoginModule.html
                //http://publib.boulder.ibm.com/infocenter/iseries/v5r3/index.jsp?topic=%2Frzaha%2Frzahajgssusejaas20.htm
                //TODO Handle class name on AS side?
                kerberosModuleBuilder.name("com.ibm.security.auth.module.Krb5LoginModule") // 
                        .putOption("useKeytab", Krb5ConfServerSetupTask.HTTP_KEYTAB_FILE.toURI().toString()) //
                        .putOption("credsType", "acceptor");
            } else {
                kerberosModuleBuilder.name("Kerberos") // 
                        .putOption("storeKey", TRUE) //
                        .putOption("refreshKrb5Config", TRUE) //
                        .putOption("useKeyTab", TRUE) //
                        .putOption("keyTab", Krb5ConfServerSetupTask.getKeyTabFullPath()) //
                        .putOption("doNotPrompt", TRUE);
            }
            kerberosModuleBuilder.putOption("principal", "HTTP/" + Utils.getCannonicalHost(managementClient) + "@JBOSS.ORG") //
                    .putOption("debug", TRUE);
            final SecurityDomain hostDomain = new SecurityDomain.Builder().name("host")
                    .loginModules(kerberosModuleBuilder.build()) //
                    .build();
            final SecurityModule spnegoLoginModule = new SecurityModule.Builder().name("SPNEGO")
                    .putOption("password-stacking", "useFirstPass").putOption("serverSecurityDomain", "host").build();
            final SecurityDomain sd1 = new SecurityDomain.Builder()
                    .name(SECURITY_DOMAIN_NAME_PREFIX + DEP1)
                    .loginModules(
                            spnegoLoginModule,
                            new SecurityModule.Builder().name(AdvancedLdapLoginModule.class.getName())
                                    .options(getCommonOptions()) //
                                    .putOption("baseCtxDN", "ou=Users,dc=jboss,dc=org") //
                                    .putOption("baseFilter", "(krb5PrincipalName={0})") //
                                    .putOption("rolesCtxDN", "ou=Roles,dc=jboss,dc=org") //
                                    .putOption("roleFilter", "(member={1})") //
                                    .putOption("roleAttributeID", "cn") //
                                    .build()) //
                    .build();
            final SecurityDomain sd2 = new SecurityDomain.Builder()
                    .name(SECURITY_DOMAIN_NAME_PREFIX + DEP2)
                    .loginModules(
                            spnegoLoginModule,
                            new SecurityModule.Builder().name("AdvancedLdap").options(getCommonOptions())
                                    .putOption("baseCtxDN", "ou=Users,dc=jboss,dc=org") //
                                    .putOption("baseFilter", "(krb5PrincipalName={0})") //
                                    .putOption("rolesCtxDN", "ou=Roles,o=example2,dc=jboss,dc=org") //
                                    .putOption("roleFilter", "(postalAddress={0})") //
                                    .putOption("roleAttributeID", "description") //
                                    .putOption("roleAttributeIsDN", TRUE) //
                                    .putOption("roleNameAttributeID", "cn") //
                                    //                                    .putOption("roleRecursion", "0") //
                                    .build()) //
                    .build();
            final SecurityDomain sd3 = new SecurityDomain.Builder()
                    .name(SECURITY_DOMAIN_NAME_PREFIX + DEP3)
                    .loginModules(
                            spnegoLoginModule,
                            new SecurityModule.Builder().name(AdvancedLdapLoginModule.class.getName())
                                    .options(getCommonOptions()) //
                                    .putOption("baseCtxDN", "ou=Users,dc=jboss,dc=org") //
                                    .putOption("baseFilter", "(mail={0})") //
                                    .putOption("rolesCtxDN", "ou=Roles,o=example3,dc=jboss,dc=org") //
                                    .putOption("roleFilter", "(member={1})") //
                                    .putOption("roleAttributeID", "cn") //
                                    //                                    .putOption("roleRecursion", "0") //
                                    .build()) //
                    .build();
            final SecurityDomain sd4 = new SecurityDomain.Builder()
                    .name(SECURITY_DOMAIN_NAME_PREFIX + DEP4)
                    .loginModules(
                            spnegoLoginModule,
                            new SecurityModule.Builder().name(AdvancedLdapLoginModule.class.getName())
                                    .options(getCommonOptions()) //
                                    .putOption("baseCtxDN", "ou=Users,dc=jboss,dc=org") //
                                    .putOption("baseFilter", "(mail={0})") //
                                    .putOption("rolesCtxDN", "ou=Roles,o=example4,dc=jboss,dc=org") //
                                    .putOption("roleFilter", "(member={1})") //
                                    .putOption("roleAttributeID", "cn") //
                                    .putOption("recurseRoles", TRUE) //
                                    .build()) //
                    .build();
            return new SecurityDomain[] { hostDomain, sd1, sd2, sd3, sd4 };
        }

        private Map<String, String> getCommonOptions() {
            final Map<String, String> moduleOptions = new HashMap<String, String>();
            moduleOptions.put("password-stacking", "useFirstPass");
            moduleOptions.put("bindAuthentication", "GSSAPI");
            moduleOptions.put("jaasSecurityDomain", "host");
            moduleOptions.put("java.naming.provider.url", "ldap://" + Utils.getSecondaryTestAddress(managementClient) + ":"
                    + LDAP_PORT);
            return moduleOptions;
        }
    }

    /**
     * A Kerberos system-properties server setup task. Sets path to a <code>krb5.conf</code> file and enables Kerberos debug
     * messages.
     * 
     * @author Josef Cacek
     */
    static class KerberosSystemPropertiesSetupTask extends AbstractSystemPropertiesServerSetupTask {

        /**
         * Returns "java.security.krb5.conf" and "sun.security.krb5.debug" properties.
         * 
         * @return Kerberos properties
         * @see org.jboss.as.test.integration.security.common.AbstractSystemPropertiesServerSetupTask#getSystemProperties()
         */
        @Override
        protected SystemProperty[] getSystemProperties() {
            final Map<String, String> map = new HashMap<String, String>();
            map.put("java.security.krb5.conf", KRB5_CONF_PATH);
            map.put("sun.security.krb5.debug", TRUE);
            map.put(SecurityConstants.DISABLE_SECDOMAIN_OPTION, TRUE);
            return mapToSystemProperties(map);
        }

    }
}
