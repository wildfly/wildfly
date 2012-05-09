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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.net.URI;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

import javax.security.auth.login.LoginException;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.io.FileUtils;
import org.apache.directory.server.annotations.CreateLdapServer;
import org.apache.directory.server.annotations.CreateTransport;
import org.apache.directory.server.annotations.SaslMechanism;
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
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.as.arquillian.api.ServerSetup;
import org.jboss.as.arquillian.api.ServerSetupTask;
import org.jboss.as.arquillian.container.ManagementClient;
import org.jboss.as.test.integration.security.common.AbstractSecurityDomainStackServerSetupTask;
import org.jboss.as.test.integration.security.common.AbstractSystemPropertiesServerSetupTask;
import org.jboss.as.test.integration.security.common.ExtCreateKdcServer;
import org.jboss.as.test.integration.security.common.KDCServerAnnotationProcessor;
import org.jboss.as.test.integration.security.common.Utils;
import org.jboss.as.test.integration.security.loginmodules.common.servlets.AbstractLoginModuleTestServlet;
import org.jboss.as.test.integration.security.loginmodules.common.servlets.SecuredLdapLoginModuleTestServlet;
import org.jboss.as.test.integration.security.loginmodules.common.servlets.SimpleUnsecuredServlet;
import org.jboss.logging.Logger;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Basic Negotiation login module (SPNEGOLoginModule) tests.
 * 
 * @author Josef Cacek
 */
@RunWith(Arquillian.class)
@ServerSetup({ SPNEGOLoginModuleTestCase.KerberosSystemPropertiesSetupTask.class, //
        SPNEGOLoginModuleTestCase.KDCServerSetupTask.class, //
        SPNEGOLoginModuleTestCase.HostSecurityDomainSetup.class, //
        SPNEGOLoginModuleTestCase.SPNEGOSecurityDomainSetup.class })
@RunAsClient
public class SPNEGOLoginModuleTestCase {

    private static Logger LOGGER = Logger.getLogger(SPNEGOLoginModuleTestCase.class);

    /** The KRB5_CONF_PATH */
    private static final String KRB5_CONF_PATH = SPNEGOLoginModuleTestCase.class.getResource("krb5.conf").getFile();
    /** The TRUE */
    private static final String TRUE = Boolean.TRUE.toString();

    @ArquillianResource
    URL webAppURL;

    // Public methods --------------------------------------------------------

    /**
     * Creates {@link WebArchive}.
     * 
     * @return
     */
    @Deployment
    public static WebArchive deployment() {
        LOGGER.info("start deployment");
        final WebArchive war = ShrinkWrap.create(WebArchive.class, "kerberos-login-module.war");
        war.addClasses(SecuredLdapLoginModuleTestServlet.class, SimpleUnsecuredServlet.class,
                AbstractLoginModuleTestServlet.class);
        war.addAsWebInfResource(SPNEGOLoginModuleTestCase.class.getPackage(), "web-spnego-authn.xml", "web.xml");
        war.addAsWebInfResource(new StringAsset("<jboss-web>" + //
                "<security-domain>SPNEGO</security-domain>" + //
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
     * Correct login.
     * 
     * @throws Exception
     */
    @Test
    public void testAuthn() throws Exception {
        LOGGER.info("Testing successfull authentication");
        final URI servletUri = new URI(webAppURL.toExternalForm() + SecuredLdapLoginModuleTestServlet.SERVLET_PATH.substring(1));
        final String responseBody = Utils.makeCallWithKerberosAuthn(servletUri, "jduke", "theduke", KRB5_CONF_PATH,
                HttpServletResponse.SC_OK);
        assertEquals("Unexpected response body", SecuredLdapLoginModuleTestServlet.RESPONSE_BODY, responseBody);
    }

    /**
     * Incorrect login.
     * 
     * @throws Exception
     */
    @Test
    public void testUnsucessfulAuthn() throws Exception {
        LOGGER.info("Testing failed authentication");
        final URI servletUri = new URI(webAppURL.toExternalForm() + SecuredLdapLoginModuleTestServlet.SERVLET_PATH.substring(1));
        try {
            Utils.makeCallWithKerberosAuthn(servletUri, "jduke", "the%", KRB5_CONF_PATH, HttpServletResponse.SC_OK);
            fail();
        } catch (LoginException e) {
            //OK
        }
        try {
            Utils.makeCallWithKerberosAuthn(servletUri, "jd%", "theduke", KRB5_CONF_PATH, HttpServletResponse.SC_OK);
            fail();
        } catch (LoginException e) {
            //OK
        }
    }

    /**
     * Correct login, but without permissions.
     * 
     * @throws Exception
     */
    @Test
    public void testUnsucessfulAuthz() throws Exception {
        LOGGER.info("Testing correct authentication, but failed authorization.");
        final URI servletUri = new URI(webAppURL.toExternalForm() + SecuredLdapLoginModuleTestServlet.SERVLET_PATH.substring(1));
        Utils.makeCallWithKerberosAuthn(servletUri, "hnelson", "secret", KRB5_CONF_PATH, HttpServletResponse.SC_FORBIDDEN);
    }

    /**
     * Unsecured request.
     * 
     * @throws Exception
     */
    @Test
    public void testUnsecured() throws Exception {
        LOGGER.info("Testing access to unprotected resource.");
        final URI servletUri = new URI(webAppURL.toExternalForm() + SimpleUnsecuredServlet.SERVLET_PATH.substring(1));
        final String responseBody = Utils.makeCallWithKerberosAuthn(servletUri, null, null, KRB5_CONF_PATH,
                HttpServletResponse.SC_OK);
        assertEquals("Unexpected response body.", SimpleUnsecuredServlet.RESPONSE_BODY, responseBody);
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
            @CreateTransport( protocol = "LDAP",  port = 10389, address = "0.0.0.0" ), 
            @CreateTransport( protocol = "LDAPS", port = 10636, address = "0.0.0.0" ) 
        },
        saslHost="localhost",
        saslPrincipal="ldap/localhost@JBOSS.ORG",
        saslMechanisms = 
        {
            @SaslMechanism( name=SupportedSaslMechanisms.PLAIN, implClass=PlainMechanismHandler.class ),
            @SaslMechanism( name=SupportedSaslMechanisms.CRAM_MD5, implClass=CramMd5MechanismHandler.class),
            @SaslMechanism( name=SupportedSaslMechanisms.DIGEST_MD5, implClass=DigestMd5MechanismHandler.class),
            @SaslMechanism( name=SupportedSaslMechanisms.GSSAPI, implClass=GssapiMechanismHandler.class),
            @SaslMechanism( name=SupportedSaslMechanisms.NTLM, implClass=NtlmMechanismHandler.class),
            @SaslMechanism( name=SupportedSaslMechanisms.GSS_SPNEGO, implClass=NtlmMechanismHandler.class)
        })            
    @ExtCreateKdcServer(primaryRealm = "JBOSS.ORG",
        kdcPrincipal = "krbtgt/JBOSS.ORG@JBOSS.ORG",
        searchBaseDn = "dc=jboss,dc=org",
        transports = 
        { 
            @CreateTransport(protocol = "TCP", port = 6088),
            @CreateTransport(protocol = "UDP", port = 6088)
        })
    //@formatter:on
    static class KDCServerSetupTask implements ServerSetupTask {

        private DirectoryService directoryService;
        private LdapServer ldapServer;
        private KdcServer kdcServer;

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
                        SPNEGOLoginModuleTestCase.class.getResourceAsStream("SPNEGOLoginModuleTestCase.ldif"))) {
                    directoryService.getAdminSession().add(new DefaultEntry(schemaManager, ldifEntry.getEntry()));
                }
            } catch (Exception e) {
                e.printStackTrace();
                throw e;
            }
            ldapServer = ServerAnnotationProcessor.getLdapServer(directoryService);
            kdcServer = KDCServerAnnotationProcessor.getKdcServer(directoryService, 1024);
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
            kdcServer.stop();
            ldapServer.stop();
            directoryService.shutdown();
            FileUtils.deleteDirectory(directoryService.getInstanceLayout().getInstanceDirectory());
        }

    }

    /**
     * A security-domain configuration class with "Kerberos" login module - it's used for JBoss Application server
     * authentication against the KDC.
     */
    static class HostSecurityDomainSetup extends AbstractSecurityDomainStackServerSetupTask {

        /**
         * @see org.jboss.as.test.integration.security.common.AbstractSecurityDomainStackServerSetupTask#getLoginModuleConfigurations()
         */
        @Override
        protected SecurityModuleConfiguration[] getLoginModuleConfigurations() {
            SecurityModuleConfiguration loginModule = new AbstractSecurityModuleConfiguration() {

                /**
                 * @see org.jboss.as.test.integration.security.common.AbstractSecurityDomainStackServerSetupTask.SecurityModuleConfiguration#getName()
                 */
                public String getName() {
                    return "Kerberos";
                }

                /**
                 * @see org.jboss.as.test.integration.security.common.AbstractSecurityDomainStackServerSetupTask.AbstractSecurityModuleConfiguration#getOptions()
                 */
                @Override
                public Map<String, String> getOptions() {
                    Map<String, String> moduleOptions = new HashMap<String, String>();
                    moduleOptions.put("storeKey", TRUE);
                    moduleOptions.put("refreshKrb5Config", TRUE);
                    moduleOptions.put("useKeyTab", TRUE);
                    moduleOptions.put("principal", "HTTP/localhost@JBOSS.ORG");
                    moduleOptions.put("keyTab", SPNEGOLoginModuleTestCase.class.getResource("http-localhost.keytab").getFile());
                    moduleOptions.put("doNotPrompt", TRUE);
                    moduleOptions.put("debug", TRUE);
                    return moduleOptions;
                }

            };
            return new SecurityModuleConfiguration[] { loginModule };
        }

        /**
         * @see org.jboss.as.test.integration.security.common.AbstractSecurityDomainStackServerSetupTask#getSecurityDomainName()
         */
        @Override
        protected String getSecurityDomainName() {
            return "host";
        }

    }

    /**
     * A security-domain configuration class with SPNEGO login module and SimpleRoles mapping-module.
     */
    static class SPNEGOSecurityDomainSetup extends AbstractSecurityDomainStackServerSetupTask {

        /**
         * @see org.jboss.as.test.integration.security.common.AbstractSecurityDomainStackServerSetupTask#getSecurityDomainName()
         */
        @Override
        protected String getSecurityDomainName() {
            return "SPNEGO";
        }

        /**
         * @see org.jboss.as.test.integration.security.common.AbstractSecurityDomainStackServerSetupTask#getLoginModuleConfigurations()
         */
        @Override
        protected SecurityModuleConfiguration[] getLoginModuleConfigurations() {
            SecurityModuleConfiguration loginModule = new AbstractSecurityModuleConfiguration() {

                public String getName() {
                    return "SPNEGO";
                }

                @Override
                public Map<String, String> getOptions() {
                    Map<String, String> moduleOptions = new HashMap<String, String>();
                    moduleOptions.put("password-stacking", "useFirstPass");
                    moduleOptions.put("serverSecurityDomain", "host");
                    return moduleOptions;
                }

            };
            return new SecurityModuleConfiguration[] { loginModule };
        }

        /**
         * @see org.jboss.as.test.integration.security.common.AbstractSecurityDomainStackServerSetupTask#getMappingModuleConfigurations()
         */
        @Override
        protected SecurityModuleConfiguration[] getMappingModuleConfigurations() {
            SecurityModuleConfiguration mappingModule = new AbstractSecurityModuleConfiguration() {

                public String getName() {
                    return "SimpleRoles";
                }

                @Override
                public Map<String, String> getOptions() {
                    Map<String, String> moduleOptions = new HashMap<String, String>();
                    moduleOptions.put("jduke@JBOSS.ORG", "Admin,Users,JBossAdmin");
                    return moduleOptions;
                }

            };
            return new SecurityModuleConfiguration[] { mappingModule };
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
            return mapToSystemProperties(map);
        }

    }

}
