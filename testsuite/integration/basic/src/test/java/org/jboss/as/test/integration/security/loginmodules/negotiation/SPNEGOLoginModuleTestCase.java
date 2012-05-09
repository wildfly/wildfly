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
import java.net.URISyntaxException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

import javax.security.auth.login.LoginException;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.SystemUtils;
import org.apache.commons.lang.text.StrSubstitutor;
import org.apache.directory.server.annotations.CreateTransport;
import org.apache.directory.server.core.annotations.ContextEntry;
import org.apache.directory.server.core.annotations.CreateDS;
import org.apache.directory.server.core.annotations.CreateIndex;
import org.apache.directory.server.core.annotations.CreatePartition;
import org.apache.directory.server.core.api.DirectoryService;
import org.apache.directory.server.core.factory.DSAnnotationProcessor;
import org.apache.directory.server.core.kerberos.KeyDerivationInterceptor;
import org.apache.directory.server.kerberos.kdc.KdcServer;
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
import org.jboss.as.test.integration.security.common.AbstractSecurityDomainsServerSetupTask;
import org.jboss.as.test.integration.security.common.AbstractSystemPropertiesServerSetupTask;
import org.jboss.as.test.integration.security.common.ExtCreateKdcServer;
import org.jboss.as.test.integration.security.common.KDCServerAnnotationProcessor;
import org.jboss.as.test.integration.security.common.Utils;
import org.jboss.as.test.integration.security.common.config.SecurityDomain;
import org.jboss.as.test.integration.security.common.config.SecurityModule;
import org.jboss.as.test.integration.security.loginmodules.common.servlets.SimpleSecuredServlet;
import org.jboss.as.test.integration.security.loginmodules.common.servlets.SimpleServlet;
import org.jboss.logging.Logger;
import org.jboss.security.SecurityConstants;
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
@ServerSetup({ Krb5ConfServerSetupTask.class, // 
        SPNEGOLoginModuleTestCase.KerberosSystemPropertiesSetupTask.class, //
        SPNEGOLoginModuleTestCase.KDCServerSetupTask.class, //
        SPNEGOLoginModuleTestCase.SecurityDomainsSetup.class })
@RunAsClient
public class SPNEGOLoginModuleTestCase {

    private static Logger LOGGER = Logger.getLogger(SPNEGOLoginModuleTestCase.class);

    /** The WEBAPP_NAME */
    private static final String WEBAPP_NAME = "kerberos-login-module";

    /** The KRB5_CONF_PATH */
    private static final String KRB5_CONF_PATH = Krb5ConfServerSetupTask.getKrb5ConfFullPath();

    /** The TRUE */
    private static final String TRUE = Boolean.TRUE.toString();

    @ArquillianResource
    URL webAppURL;

    @ArquillianResource
    ManagementClient mgmtClient;

    // Public methods --------------------------------------------------------

    /**
     * Creates {@link WebArchive}.
     * 
     * @return
     */
    @Deployment
    public static WebArchive deployment() {
        LOGGER.info("start deployment");
        final WebArchive war = ShrinkWrap.create(WebArchive.class, WEBAPP_NAME + ".war");
        war.addClasses(SimpleSecuredServlet.class, SimpleServlet.class);
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
        final URI servletUri = getServletURI(SimpleSecuredServlet.SERVLET_PATH);
        LOGGER.info("Testing successfull authentication " + servletUri);
        final String responseBody = Utils.makeCallWithKerberosAuthn(servletUri, "jduke", "theduke", KRB5_CONF_PATH,
                HttpServletResponse.SC_OK);
        assertEquals("Unexpected response body", SimpleSecuredServlet.RESPONSE_BODY, responseBody);
    }

    /**
     * Incorrect login.
     * 
     * @throws Exception
     */
    @Test
    public void testUnsucessfulAuthn() throws Exception {
        final URI servletUri = getServletURI(SimpleSecuredServlet.SERVLET_PATH);
        LOGGER.info("Testing failed authentication " + servletUri);
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
        final URI servletUri = getServletURI(SimpleSecuredServlet.SERVLET_PATH);
        LOGGER.info("Testing correct authentication, but failed authorization " + servletUri);
        Utils.makeCallWithKerberosAuthn(servletUri, "hnelson", "secret", KRB5_CONF_PATH, HttpServletResponse.SC_FORBIDDEN);
    }

    /**
     * Unsecured request.
     * 
     * @throws Exception
     */
    @Test
    public void testUnsecured() throws Exception {
        final URI servletUri = getServletURI(SimpleServlet.SERVLET_PATH);
        LOGGER.info("Testing access to unprotected resource " + servletUri);
        final String responseBody = Utils.makeCallWithKerberosAuthn(servletUri, null, null, KRB5_CONF_PATH,
                HttpServletResponse.SC_OK);
        assertEquals("Unexpected response body.", SimpleServlet.RESPONSE_BODY, responseBody);
    }

    // Private methods -------------------------------------------------------

    /**
     * Constructs URI for given servlet path.
     * 
     * @param servletPath
     * @return
     * @throws URISyntaxException
     */
    private URI getServletURI(final String servletPath) throws URISyntaxException {
        return new URI(webAppURL.toExternalForm() + servletPath.substring(1));
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
    @ExtCreateKdcServer(primaryRealm = "JBOSS.ORG",
        kdcPrincipal = "krbtgt/JBOSS.ORG@JBOSS.ORG",
        searchBaseDn = "dc=jboss,dc=org",
        transports = 
        { 
            @CreateTransport(protocol = "UDP", port = 6088)
        })
    //@formatter:on
    static class KDCServerSetupTask implements ServerSetupTask {

        private DirectoryService directoryService;
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
            final String hostname = Utils.getCannonicalHost(managementClient);
            final Map<String, String> map = new HashMap<String, String>();
            map.put("hostname", hostname);
            final String ldifContent = StrSubstitutor.replace(
                    IOUtils.toString(
                            SPNEGOLoginModuleTestCase.class.getResourceAsStream(SPNEGOLoginModuleTestCase.class.getSimpleName()
                                    + ".ldif"), "UTF-8"), map);
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
                        .putOption("useKeytab", Krb5ConfServerSetupTask.getKeyTabFile().toURI().toString()) //
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
            final SecurityDomain spnegoDomain = new SecurityDomain.Builder()
                    .name("SPNEGO")
                    .loginModules(
                            new SecurityModule.Builder().name("SPNEGO").putOption("password-stacking", "useFirstPass")
                                    .putOption("serverSecurityDomain", "host").build()) //
                    .mappingModules(
                            new SecurityModule.Builder().name("SimpleRoles")
                                    .putOption("jduke@JBOSS.ORG", "Admin,Users,JBossAdmin").build())//
                    .build();
            return new SecurityDomain[] { hostDomain, spnegoDomain };
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
