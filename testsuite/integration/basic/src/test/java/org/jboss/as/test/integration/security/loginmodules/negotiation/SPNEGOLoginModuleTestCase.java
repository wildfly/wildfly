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

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.security.Principal;
import java.security.Security;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.security.auth.login.Configuration;
import javax.security.auth.login.LoginException;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.SystemUtils;
import org.apache.commons.lang.text.StrSubstitutor;
import org.apache.directory.api.ldap.model.entry.DefaultEntry;
import org.apache.directory.api.ldap.model.ldif.LdifEntry;
import org.apache.directory.api.ldap.model.ldif.LdifReader;
import org.apache.directory.api.ldap.model.schema.SchemaManager;
import org.apache.directory.server.annotations.CreateKdcServer;
import org.apache.directory.server.annotations.CreateTransport;
import org.apache.directory.server.core.annotations.ContextEntry;
import org.apache.directory.server.core.annotations.CreateDS;
import org.apache.directory.server.core.annotations.CreateIndex;
import org.apache.directory.server.core.annotations.CreatePartition;
import org.apache.directory.server.core.api.DirectoryService;
import org.apache.directory.server.core.factory.DSAnnotationProcessor;
import org.apache.directory.server.core.kerberos.KeyDerivationInterceptor;
import org.apache.directory.server.factory.ServerAnnotationProcessor;
import org.apache.directory.server.kerberos.kdc.KdcServer;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.OperateOnDeployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.as.arquillian.api.ServerSetup;
import org.jboss.as.arquillian.api.ServerSetupTask;
import org.jboss.as.arquillian.container.ManagementClient;
import org.jboss.as.network.NetworkUtils;
import org.jboss.as.test.integration.security.common.AbstractSecurityDomainsServerSetupTask;
import org.jboss.as.test.integration.security.common.AbstractSystemPropertiesServerSetupTask;
import org.jboss.as.test.integration.security.common.KDCServerAnnotationProcessor;
import org.jboss.as.test.integration.security.common.Krb5LoginConfiguration;
import org.jboss.as.test.integration.security.common.Utils;
import org.jboss.as.test.integration.security.common.config.SecurityDomain;
import org.jboss.as.test.integration.security.common.config.SecurityModule;
import org.jboss.as.test.integration.security.common.servlets.SimpleSecuredServlet;
import org.jboss.as.test.integration.security.common.servlets.SimpleServlet;
import org.jboss.as.test.integration.security.loginmodules.LdapExtLoginModuleTestCase;
import org.jboss.as.test.integration.security.xacml.CustomXACMLAuthorizationModule;
import org.jboss.logging.Logger;
import org.jboss.security.SecurityConstants;
import org.jboss.security.negotiation.NegotiationAuthenticator;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Ignore;
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
        GSSTestServer.class, //
        SPNEGOLoginModuleTestCase.SecurityDomainsSetup.class })
@RunAsClient
@Ignore("AS7-6796 - Undertow SPNEGO")
public class SPNEGOLoginModuleTestCase {

    private static Logger LOGGER = Logger.getLogger(SPNEGOLoginModuleTestCase.class);

    /** The WEBAPP_NAME */
    private static final String WEBAPP_NAME = "kerberos-login-module";
    private static final String WEBAPP_NAME_FALLBACK = "kerberos-test-form-fallback";

    /** The TRUE */
    private static final String TRUE = Boolean.TRUE.toString();

    @ArquillianResource
    ManagementClient mgmtClient;

    // Public methods --------------------------------------------------------

    /**
     * Creates {@link WebArchive}.
     *
     * @return
     */
    @Deployment(name = "WEB")
    public static WebArchive deployment() {
        LOGGER.debug("Web deployment");
        return createWebApp(WEBAPP_NAME, "web-spnego-authn.xml", "SPNEGO");
    }

    /**
     * Creates {@link WebArchive}.
     *
     * @return
     */
    @Deployment(name = "WEB-FORM")
    public static WebArchive deploymentWebFormFallback() {
        LOGGER.debug("Web deployment with FORM fallback");
        final WebArchive war = createWebApp(WEBAPP_NAME_FALLBACK, "web-spnego-form-fallback.xml", "SPNEGO-with-fallback");
        war.addAsWebResource(LdapExtLoginModuleTestCase.class.getPackage(), "error.jsp", "error.jsp");
        war.addAsWebResource(LdapExtLoginModuleTestCase.class.getPackage(), "login.jsp", "login.jsp");
        war.addAsResource(new StringAsset("jduke@JBOSS.ORG=fallback\nhnelson@JBOSS.ORG=terces"), "fallback-users.properties");
        war.addAsResource(EmptyAsset.INSTANCE, "fallback-roles.properties");
        return war;
    }

    /**
     * Creates {@link JavaArchive} for testing the {@link CustomXACMLAuthorizationModule}.
     *
     * @return
     * @throws IOException
     * @throws IllegalArgumentException
     */
    @Deployment(name = "EJB")
    public static JavaArchive deploymentEjb() throws IllegalArgumentException, IOException {
        final JavaArchive jar = ShrinkWrap
                .create(JavaArchive.class, "ejb-spnego.jar")
                .addClasses(SimpleSession.class, SimpleStatelessSessionBean.class)
                .addAsManifestResource(
                        new StringAsset(
                                "<jboss:ejb-jar xmlns:jboss='http://www.jboss.com/xml/ns/javaee' xmlns='http://java.sun.com/xml/ns/javaee' xmlns:s='urn:security' version='3.1' impl-version='2.0'>"
                                        + "<assembly-descriptor><s:security>"
                                        + "<ejb-name>*</ejb-name><s:security-domain>SPNEGO</s:security-domain>"
                                        + "</s:security></assembly-descriptor>" //
                                        + "</jboss:ejb-jar>"), "jboss-ejb3.xml");
        jar.addAsManifestResource(Utils.getJBossDeploymentStructure("org.jboss.security.negotiation"),
                "jboss-deployment-structure.xml");
        LOGGER.info(jar.toString(true));
        return jar;
    }

    /**
     * Correct login.
     *
     * @throws Exception
     */
    @Test
    @OperateOnDeployment("WEB")
    public void testAuthn(@ArquillianResource URL webAppURL) throws Exception {
        final URI servletUri = getServletURI(webAppURL, SimpleSecuredServlet.SERVLET_PATH);
        LOGGER.info("Testing successful authentication " + servletUri);
        final String responseBody = Utils.makeCallWithKerberosAuthn(servletUri, "jduke", "theduke", HttpServletResponse.SC_OK);
        assertEquals("Unexpected response body", SimpleSecuredServlet.RESPONSE_BODY, responseBody);
    }

    /**
     * Incorrect login.
     *
     * @throws Exception
     */
    @Test
    @OperateOnDeployment("WEB")
    public void testUnsuccessfulAuthn(@ArquillianResource URL webAppURL) throws Exception {
        final URI servletUri = getServletURI(webAppURL, SimpleSecuredServlet.SERVLET_PATH);
        LOGGER.info("Testing failed authentication " + servletUri);
        try {
            Utils.makeCallWithKerberosAuthn(servletUri, "jduke", "the%", HttpServletResponse.SC_OK);
            fail();
        } catch (LoginException e) {
            //OK
        }
        try {
            Utils.makeCallWithKerberosAuthn(servletUri, "jd%", "theduke", HttpServletResponse.SC_OK);
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
    @OperateOnDeployment("WEB")
    public void testUnsuccessfulAuthz(@ArquillianResource URL webAppURL) throws Exception {
        final URI servletUri = getServletURI(webAppURL, SimpleSecuredServlet.SERVLET_PATH);
        LOGGER.info("Testing correct authentication, but failed authorization " + servletUri);
        Utils.makeCallWithKerberosAuthn(servletUri, "hnelson", "secret", HttpServletResponse.SC_FORBIDDEN);
    }

    /**
     * Unsecured request.
     *
     * @throws Exception
     */
    @Test
    @OperateOnDeployment("WEB")
    public void testUnsecured(@ArquillianResource URL webAppURL) throws Exception {
        final URI servletUri = getServletURI(webAppURL, SimpleServlet.SERVLET_PATH);
        LOGGER.info("Testing access to unprotected resource " + servletUri);
        final String responseBody = Utils.makeCallWithKerberosAuthn(servletUri, null, null, HttpServletResponse.SC_OK);
        assertEquals("Unexpected response body.", SimpleServlet.RESPONSE_BODY, responseBody);
    }

    /**
     * Tests identity propagation by requesting {@link PropagateIdentityServlet}.
     *
     * @throws Exception
     */
    @Test
    @OperateOnDeployment("WEB")
    public void testIdentityPropagation(@ArquillianResource URL webAppURL) throws Exception {
        final URI servletUri = getServletURI(webAppURL, PropagateIdentityServlet.SERVLET_PATH);
        LOGGER.info("Testing identity propagation " + servletUri);
        final String responseBody = Utils.makeCallWithKerberosAuthn(servletUri, "jduke", "theduke", HttpServletResponse.SC_OK);
        assertEquals("Unexpected response body.", "jduke@JBOSS.ORG", responseBody);
    }

    /**
     * Tests web SPNEGO authentication with FORM method fallback.
     *
     * @throws Exception
     */
    @Test
    @OperateOnDeployment("WEB-FORM")
    public void testFormFallback(@ArquillianResource URL webAppURL) throws Exception {
        final URI servletUri = getServletURI(webAppURL, SimpleSecuredServlet.SERVLET_PATH);
        LOGGER.info("Testing fallback to FORM authentication. " + servletUri);

        LOGGER.info("Testing successful SPNEGO authentication");
        String responseBody = Utils.makeCallWithKerberosAuthn(servletUri, "jduke", "theduke", HttpServletResponse.SC_OK);
        assertEquals("Unexpected response body", SimpleSecuredServlet.RESPONSE_BODY, responseBody);

        LOGGER.info("Testing successful FORM authentication");
        responseBody = Utils.makeHttpCallWoSPNEGO(webAppURL.toExternalForm(), SimpleSecuredServlet.SERVLET_PATH,
                "jduke@JBOSS.ORG", "fallback", HttpServletResponse.SC_OK);
        assertEquals("Unexpected response body", SimpleSecuredServlet.RESPONSE_BODY, responseBody);

        LOGGER.info("Testing FORM fallback");
        responseBody = Utils.makeHttpCallWithFallback(webAppURL.toExternalForm(), SimpleSecuredServlet.SERVLET_PATH,
                "jduke@JBOSS.ORG", "fallback", HttpServletResponse.SC_OK);
        assertEquals("Unexpected response body", SimpleSecuredServlet.RESPONSE_BODY, responseBody);
    }

    /**
     * Tests EJB authentication using SPNEGO.
     *
     * @throws Exception
     */
    @Test
    @OperateOnDeployment("EJB")
    @Ignore("AS7-4874 EJB3 in AS7 doesn't support Kerberos authentication")
    public void testEjbAccess() throws Exception {
        LOGGER.info("Testing EJB3 access.");

        final Configuration tmpConfig = Configuration.getConfiguration();

        Configuration
                .setConfiguration(new Krb5LoginConfiguration("hnelson", Krb5ConfServerSetupTask.HNELSON_KEYTAB_FILE, false));
        Context ctx = getRemoteContext();
        SimpleSession session = (SimpleSession) ctx.lookup("ejb:/ejb-spnego//SimpleStatelessSessionBean!"
                + SimpleSession.class.getName());
        Principal principal = session.invokeRegularMethod();
        ctx.close();
        assertEquals("User's principal name doesn't match.", "hnelson@JBOSS.ORG", principal.getName());

        Configuration.setConfiguration(new Krb5LoginConfiguration("jduke", Krb5ConfServerSetupTask.JDUKE_KEYTAB_FILE, false));
        ctx = getRemoteContext();
        session = (SimpleSession) ctx.lookup("ejb:/ejb-spnego//SimpleStatelessSessionBean!" + SimpleSession.class.getName());
        principal = session.invokeAdministrativeMethod();
        ctx.close();
        assertEquals("User's principal name doesn't match.", "jduke@JBOSS.ORG", principal.getName());

        Configuration.setConfiguration(tmpConfig);
    }

    // Private methods -------------------------------------------------------

    private static WebArchive createWebApp(final String webAppName, final String webXmlFilename, final String securityDomain) {
        final WebArchive war = ShrinkWrap.create(WebArchive.class, webAppName + ".war");
        war.addClasses(SimpleSecuredServlet.class, SimpleServlet.class, PropagateIdentityServlet.class, GSSTestClient.class,
                GSSTestConstants.class);
        war.addAsWebInfResource(SPNEGOLoginModuleTestCase.class.getPackage(), webXmlFilename, "web.xml");
        war.addAsWebInfResource(Utils.getJBossWebXmlAsset(securityDomain, NegotiationAuthenticator.class.getName()),
                "jboss-web.xml");
        war.addAsManifestResource(
                Utils.getJBossDeploymentStructure("org.jboss.security.negotiation", "org.apache.commons.lang"),
                "jboss-deployment-structure.xml");
        LOGGER.info(war.toString(true));
        return war;
    }

    private Context getRemoteContext() throws Exception {
        final Properties env = new Properties();
        //        env.put(Context.INITIAL_CONTEXT_FACTORY, org.jboss.naming.remote.client.InitialContextFactory.class.getName());
        //        env.put(Context.PROVIDER_URL, mgmtClient.getRemoteEjbURL().toString());
        //        env.put("jboss.naming.client.connect.options.org.xnio.Options.SASL_POLICY_NOPLAINTEXT", "false");
        //        env.put("jboss.naming.client.security.callback.handler.class", CallbackHandler.class.getName());
        env.put(Context.URL_PKG_PREFIXES, "org.jboss.ejb.client.naming");
        return new InitialContext(env);
    }

    /**
     * Constructs URI for given servlet path.
     *
     * @param servletPath
     * @return
     * @throws URISyntaxException
     */
    private URI getServletURI(final URL webAppURL, final String servletPath) throws URISyntaxException {
        return Utils.getServletURI(webAppURL, servletPath, mgmtClient, true);
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
    @CreateKdcServer(primaryRealm = "JBOSS.ORG",
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
        private boolean removeBouncyCastle = false;

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
            try {
                if(Security.getProvider(BouncyCastleProvider.PROVIDER_NAME) == null) {
                    Security.addProvider(new BouncyCastleProvider());
                    removeBouncyCastle = true;
                }
            } catch(SecurityException ex) {
                LOGGER.warn("Cannot register BouncyCastleProvider", ex);
            }
            directoryService = DSAnnotationProcessor.getDirectoryService();
            final String hostname = Utils.getCannonicalHost(managementClient);
            final Map<String, String> map = new HashMap<String, String>();
            map.put("hostname", NetworkUtils.formatPossibleIpv6Address(hostname));
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
            if(removeBouncyCastle) {
                try {
                    Security.removeProvider(BouncyCastleProvider.PROVIDER_NAME);
                } catch(SecurityException ex) {
                    LOGGER.warn("Cannot deregister BouncyCastleProvider", ex);
                }
            }
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
            kerberosModuleBuilder.putOption("principal",
                    "HTTP/" + NetworkUtils.formatPossibleIpv6Address(Utils.getCannonicalHost(managementClient)) + "@JBOSS.ORG") //
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
                                    .putOption("jduke@JBOSS.ORG", "Admin,Users,JBossAdmin,TestRole").build())//
                    .build();

            final SecurityDomain spnegoWithFallback = new SecurityDomain.Builder()
                    .name("SPNEGO-with-fallback")
                    .loginModules(new SecurityModule.Builder().name("SPNEGO") //
                            .putOption("password-stacking", "useFirstPass") //
                            .putOption("serverSecurityDomain", "host") //
                            .putOption("usernamePasswordDomain", "FORM-as-fallback") //
                            .build())
                    .mappingModules(
                            new SecurityModule.Builder().name("SimpleRoles")
                                    .putOption("jduke@JBOSS.ORG", "Admin,Users,JBossAdmin,TestRole").build())//
                    .build();

            final SecurityDomain formFallbackDomain = new SecurityDomain.Builder().name("FORM-as-fallback")
                    .loginModules(new SecurityModule.Builder().name("UsersRoles") //
                            .putOption("usersProperties", "fallback-users.properties") //
                            .putOption("rolesProperties", "fallback-roles.properties") //
                            .build()).build();
            return new SecurityDomain[] { hostDomain, spnegoDomain, spnegoWithFallback, formFallbackDomain };
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
            map.put("java.security.krb5.conf", Krb5ConfServerSetupTask.getKrb5ConfFullPath());
            map.put("sun.security.krb5.debug", TRUE);
            map.put(SecurityConstants.DISABLE_SECDOMAIN_OPTION, TRUE);
            return mapToSystemProperties(map);
        }

    }

}
