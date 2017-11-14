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

import static org.jboss.as.test.integration.security.common.Utils.assertHttpHeader;
import static org.jboss.as.test.integration.security.common.negotiation.KerberosTestUtils.OID_DUMMY;
import static org.jboss.as.test.integration.security.common.negotiation.KerberosTestUtils.OID_KERBEROS_V5;
import static org.jboss.as.test.integration.security.common.negotiation.KerberosTestUtils.OID_KERBEROS_V5_LEGACY;
import static org.jboss.as.test.integration.security.common.negotiation.KerberosTestUtils.OID_NTLM;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.SocketPermission;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.security.PrivilegedActionException;
import java.security.Security;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.PropertyPermission;

import javax.security.auth.kerberos.ServicePermission;
import javax.security.auth.login.LoginException;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
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
import org.apache.directory.server.kerberos.kdc.KdcServer;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.util.EntityUtils;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.ietf.jgss.GSSException;
import org.ietf.jgss.GSSManager;
import org.ietf.jgss.GSSName;
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
import org.jboss.as.test.integration.security.common.CoreUtils;
import org.jboss.as.test.integration.security.common.KDCServerAnnotationProcessor;
import org.jboss.as.test.integration.security.common.Utils;
import org.jboss.as.test.integration.security.common.config.SecurityDomain;
import org.jboss.as.test.integration.security.common.config.SecurityModule;
import org.jboss.as.test.integration.security.common.negotiation.KerberosTestUtils;
import org.jboss.as.test.integration.security.common.servlets.SimpleSecuredServlet;
import org.jboss.as.test.integration.security.common.servlets.SimpleServlet;
import org.jboss.as.test.integration.security.loginmodules.LdapExtLoginModuleTestCase;
import org.jboss.as.test.shared.TestSuiteEnvironment;
import org.jboss.as.test.shared.integration.ejb.security.PermissionUtils;
import org.jboss.logging.Logger;
import org.jboss.security.SecurityConstants;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Basic Negotiation login module (SPNEGOLoginModule) tests.
 * <p>
 * Some of the tests check the negotiation workflow if it fits the following RFCs:
 * </p>
 * <ul>
 * <li><a href="https://tools.ietf.org/html/rfc2743">RFC-2743 - GSS-API</a></li>
 * <li><a href="https://tools.ietf.org/html/rfc4120">RFC-4120 The Kerberos Network Authentication Service (V5)</a></li>
 * <li><a href="https://tools.ietf.org/html/rfc4121">RFC-4121 The Kerberos Version 5 GSS-API</a></li>
 * <li><a href="https://tools.ietf.org/html/rfc4178">RFC-4178 SPNEGO</a></li>
 * <li><a href="https://tools.ietf.org/html/rfc4559">RFC-4559 SPNEGO-based Kerberos and NTLM HTTP Authentication in Microsoft
 * Windows</a></li>
 * </ul>
 *
 * @author Josef Cacek
 */
@RunWith(Arquillian.class)
@ServerSetup({//SecurityTraceLoggingServerSetupTask.class, // Uncomment if TRACE logging is necessary. Don't leave it on all the time; CI resources aren't free.
        Krb5ConfServerSetupTask.class, //
        SPNEGOLoginModuleTestCase.KerberosSystemPropertiesSetupTask.class, //
        SPNEGOLoginModuleTestCase.KDCServerSetupTask.class, //
        GSSTestServer.class, //
        SPNEGOLoginModuleTestCase.SecurityDomainsSetup.class})
@RunAsClient
public class SPNEGOLoginModuleTestCase {

    private static Logger LOGGER = Logger.getLogger(SPNEGOLoginModuleTestCase.class);

    /**
     * The WEBAPP_NAME
     */
    private static final String WEBAPP_NAME = "kerberos-login-module";
    private static final String WEBAPP_NAME_FALLBACK = "kerberos-test-form-fallback";

    private static final String HEADER_WWW_AUTHENTICATE = "WWW-Authenticate";
    private static final String HEADER_AUTHORIZATION = "Authorization";

    private static final String HEADER_VAL_SELECT_KERBEROS_MECH = "Negotiate oRQwEqADCgEBoQsGCSqGSIb3EgECAg==";

    private static final byte[] DUMMY_TOKEN = "Ahoj, svete!".getBytes(StandardCharsets.UTF_8);

    /**
     * The TRUE
     */
    private static final String TRUE = Boolean.TRUE.toString();

    @ArquillianResource
    ManagementClient mgmtClient;

    // Public methods --------------------------------------------------------

    @Before
    public void before() {
        KerberosTestUtils.assumeKerberosAuthenticationSupported();
    }

    /**
     * Creates {@link WebArchive}.
     *
     * @return
     */
    @Deployment(name = "WEB", testable = false)
    public static WebArchive deployment() {
        LOGGER.debug("Web deployment");
        final WebArchive war = createWebApp(WEBAPP_NAME, "web-spnego-authn.xml", "SPNEGO");
        war.addAsManifestResource(PermissionUtils.createPermissionsXmlAsset(
                // Permissions for PropagateIdentityServlet to get delegation credentials DelegationCredentialContext.getDelegCredential()
                new RuntimePermission("org.jboss.security.negotiation.getDelegCredential"),
                // Permissions for PropagateIdentityServlet to read properties
                new PropertyPermission(GSSTestConstants.PROPERTY_PORT, "read"),
                new PropertyPermission(GSSTestConstants.PROPERTY_PRINCIPAL, "read"),
                new PropertyPermission(GSSTestConstants.PROPERTY_PASSWORD, "read"),
                // Permissions for GSSTestClient to connect to GSSTestServer
                new SocketPermission(TestSuiteEnvironment.getServerAddress(), "resolve,connect"),
                new SocketPermission(CoreUtils.getCannonicalHost(TestSuiteEnvironment.getServerAddress()), "resolve,connect"),
                // Permissions for GSSTestClient to initiate gss context
                new ServicePermission(GSSTestConstants.PRINCIPAL, "initiate"),
                new ServicePermission("krbtgt/JBOSS.ORG@JBOSS.ORG", "initiate")),
                "permissions.xml");
        return war;
    }

    /**
     * Creates {@link WebArchive}.
     *
     * @return
     */
    @Deployment(name = "WEB-FORM", testable = false)
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
     * Correct login.
     *
     * @throws Exception
     */
    @Test
    @OperateOnDeployment("WEB")
    public void testAuthn(@ArquillianResource URL webAppURL) throws Exception {
        final URI servletUri = getServletURI(webAppURL, SimpleSecuredServlet.SERVLET_PATH);

        LOGGER.trace("Testing successful authentication " + servletUri);
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

        LOGGER.trace("Testing failed authentication " + servletUri);
        try {
            Utils.makeCallWithKerberosAuthn(servletUri, "jduke", "the%", HttpServletResponse.SC_OK);
            fail();
        } catch (LoginException e) {
            // OK
        }
        try {
            Utils.makeCallWithKerberosAuthn(servletUri, "jd%", "theduke", HttpServletResponse.SC_OK);
            fail();
        } catch (LoginException e) {
            // OK
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

        LOGGER.trace("Testing correct authentication, but failed authorization " + servletUri);
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

        LOGGER.trace("Testing access to unprotected resource " + servletUri);
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
        KerberosTestUtils.assumeKerberosAuthenticationSupported();
        final URI servletUri = getServletURI(webAppURL, PropagateIdentityServlet.SERVLET_PATH);

        LOGGER.trace("Testing identity propagation " + servletUri);
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
        KerberosTestUtils.assumeKerberosAuthenticationSupported();
        final URI servletUri = getServletURI(webAppURL, SimpleSecuredServlet.SERVLET_PATH);

        LOGGER.trace("Testing fallback to FORM authentication. " + servletUri);

        LOGGER.trace("Testing successful SPNEGO authentication");
        String responseBody = Utils.makeCallWithKerberosAuthn(servletUri, "jduke", "theduke", HttpServletResponse.SC_OK);
        assertEquals("Unexpected response body", SimpleSecuredServlet.RESPONSE_BODY, responseBody);

        LOGGER.trace("Testing successful FORM authentication");
        responseBody = Utils.makeHttpCallWoSPNEGO(webAppURL.toExternalForm(), SimpleSecuredServlet.SERVLET_PATH,
                "jduke@JBOSS.ORG", "fallback", HttpServletResponse.SC_OK);
        assertEquals("Unexpected response body", SimpleSecuredServlet.RESPONSE_BODY, responseBody);

        LOGGER.trace("Testing FORM fallback");
        responseBody = Utils.makeHttpCallWithFallback(webAppURL.toExternalForm(), SimpleSecuredServlet.SERVLET_PATH,
                "jduke@JBOSS.ORG", "fallback", HttpServletResponse.SC_OK);
        assertEquals("Unexpected response body", SimpleSecuredServlet.RESPONSE_BODY, responseBody);
    }

    /**
     * SPNEGO simple scenario - only kerberos mechanism is provided with valid token.
     */
    @Test
    @OperateOnDeployment("WEB")
    public void testSimpleSpnegoWorkflow(@ArquillianResource URL webAppURL) throws Exception {
        KerberosTestUtils.assumeKerberosAuthenticationSupported();
        final URI uri = getServletURI(webAppURL, SimpleSecuredServlet.SERVLET_PATH);
        final String[] mechTypes = new String[]{OID_KERBEROS_V5};
        assertSpnegoWorkflow(uri, mechTypes, createNewKerberosTicketForHttp(uri), null, false, true);
    }

    /**
     * SPNEGO simple scenario - more mechanismTypes is provided but the Kerberos mechanism is most preferable one and it has a
     * valid token.
     */
    @Test
    @OperateOnDeployment("WEB")
    public void testMoreMechTypesSpnegoWorkflow(@ArquillianResource URL webAppURL) throws Exception {
        KerberosTestUtils.assumeKerberosAuthenticationSupported();
        final URI uri = getServletURI(webAppURL, SimpleSecuredServlet.SERVLET_PATH);
        final String[] mechTypes = new String[]{OID_KERBEROS_V5, OID_DUMMY, OID_KERBEROS_V5_LEGACY};
        assertSpnegoWorkflow(uri, mechTypes, createNewKerberosTicketForHttp(uri), null, false, true);
    }

    /**
     * SPNEGO continuation scenario - more mechanismTypes is provided and the Kerberos mechanism is not the most preferable one.
     * Client provides valid token in the second round.
     */
    @Test
    @OperateOnDeployment("WEB")
    public void testContSpnegoWorkflow(@ArquillianResource URL webAppURL) throws Exception {
        final URI uri = getServletURI(webAppURL, SimpleSecuredServlet.SERVLET_PATH);
        final String[] mechTypes = new String[]{OID_DUMMY, OID_KERBEROS_V5_LEGACY, OID_KERBEROS_V5};
        assertSpnegoWorkflow(uri, mechTypes, DUMMY_TOKEN, createNewKerberosTicketForHttp(uri), true, true);
    }

    /**
     * SPNEGO continuation scenario - Kerberos mechanisms are provided as mechanismTypes. The Legacy (aka Microsoft) mechanism
     * is provided as the first one and we expect the server will not accept it and it'll ask the token for the standard
     * Kerberos mechanism OID. Client provides valid token in both rounds.
     */
    @Test
    @OperateOnDeployment("WEB")
    public void testLegacyKerberosSpnegoWorkflow(@ArquillianResource URL webAppURL) throws Exception {
        final URI uri = getServletURI(webAppURL, SimpleSecuredServlet.SERVLET_PATH);
        final String[] mechTypes = new String[]{OID_KERBEROS_V5_LEGACY, OID_KERBEROS_V5};
        final byte[] kerberosToken = createNewKerberosTicketForHttp(uri);
        assertSpnegoWorkflow(uri, mechTypes, kerberosToken, kerberosToken, false, true);
    }

    /**
     * SPNEGO simple scenario - more mechanismTypes is provided but the Kerberos mechanism is not listed as the supported one.
     */
    @Test
    @OperateOnDeployment("WEB")
    public void testNoKerberosSpnegoWorkflow(@ArquillianResource URL webAppURL) throws Exception {
        final URI uri = getServletURI(webAppURL, SimpleSecuredServlet.SERVLET_PATH);
        final String[] mechTypes = new String[]{OID_DUMMY, OID_NTLM};
        assertSpnegoWorkflow(uri, mechTypes, DUMMY_TOKEN, null, false, false);
    }

    /**
     * SPNEGO continuation scenario - more mechanismTypes is provided and the Kerberos mechanism is not the most preferable one.
     * Client provides invalid token in the second round.
     */
    @Test
    @OperateOnDeployment("WEB")
    public void testContInvalidKerberosSpnegoWorkflow(@ArquillianResource URL webAppURL) throws Exception {
        URI uri = getServletURI(webAppURL, SimpleSecuredServlet.SERVLET_PATH);
        final String[] mechTypes = new String[]{OID_DUMMY, OID_KERBEROS_V5};
        assertSpnegoWorkflow(uri, mechTypes, DUMMY_TOKEN, DUMMY_TOKEN, true, false);
    }

    /**
     * SPNEGO simple scenario - more mechanismTypes is provided and the Kerberos mechanism is the most preferable one. Client
     * provides invalid token in the first round.
     */
    @Test
    @OperateOnDeployment("WEB")
    @Ignore("JBEAP-4114")
    public void testInvalidKerberosSpnegoWorkflow(@ArquillianResource URL webAppURL) throws Exception {
        final URI uri = getServletURI(webAppURL, SimpleSecuredServlet.SERVLET_PATH);
        final String[] mechTypes = new String[]{OID_KERBEROS_V5, OID_DUMMY};
        assertSpnegoWorkflow(uri, mechTypes, DUMMY_TOKEN, null, false, false);
    }

    /**
     * Kerberos simple scenario. Client provides a valid Kerberos token (without SPNEGO envelope) in the first round. See
     * <a href="https://tools.ietf.org/html/rfc4121">RFC-4121</a>.
     */
    @Test
    @OperateOnDeployment("WEB")
    public void testPlainKerberosWorkflow(@ArquillianResource URL webAppURL) throws Exception {
        final URI uri = getServletURI(webAppURL, SimpleSecuredServlet.SERVLET_PATH);
        final byte[] kerberosToken = createNewKerberosTicketForHttp(uri);
        try (CloseableHttpClient httpClient = HttpClientBuilder.create().build()) {
            final HttpGet httpGet = new HttpGet(uri);
            HttpResponse response = httpClient.execute(httpGet);
            assertEquals(HttpServletResponse.SC_UNAUTHORIZED, response.getStatusLine().getStatusCode());
            assertHttpHeader(response, HEADER_WWW_AUTHENTICATE, "Negotiate");
            EntityUtils.consume(response.getEntity());
            httpGet.setHeader(HEADER_AUTHORIZATION, "Negotiate " + Base64.getEncoder().encodeToString(kerberosToken));
            response = httpClient.execute(httpGet);
            if (LOGGER.isTraceEnabled()) {
                LOGGER.trace("Negotiate response in HTTP header:\n" + KerberosTestUtils.dumpNegotiateHeader(response));
            }
            assertEquals(HttpServletResponse.SC_OK, response.getStatusLine().getStatusCode());
            assertEquals("Unexpected response body", SimpleSecuredServlet.RESPONSE_BODY,
                    EntityUtils.toString(response.getEntity()));
        }
    }

    // Private methods -------------------------------------------------------

    private static WebArchive createWebApp(final String webAppName, final String webXmlFilename, final String securityDomain) {
        final WebArchive war = ShrinkWrap.create(WebArchive.class, webAppName + ".war");
        war.addClasses(SimpleSecuredServlet.class, SimpleServlet.class, PropagateIdentityServlet.class, GSSTestClient.class,
                GSSTestConstants.class);
        war.addAsWebInfResource(SPNEGOLoginModuleTestCase.class.getPackage(), webXmlFilename, "web.xml");
        war.addAsWebInfResource(Utils.getJBossWebXmlAsset(securityDomain), "jboss-web.xml");
        war.addAsManifestResource(
                Utils.getJBossDeploymentStructure("org.jboss.security.negotiation", "org.apache.commons.lang"),
                "jboss-deployment-structure.xml");
        return war;
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

    /**
     * Create a new Kerberos ticket for HTTP service. The ticket should be newly generated for every test to avoid the
     * "ticket reply errors".
     *
     * @param uri servlet URI (used to retrieve hostname)
     * @return ASN.1 (DER) encoded Kerberos key for HTTP service
     */
    private byte[] createNewKerberosTicketForHttp(URI uri)
            throws GSSException, MalformedURLException, LoginException, PrivilegedActionException {
        final GSSName serverName = GSSManager.getInstance().createName("HTTP@" + uri.getHost(), GSSName.NT_HOSTBASED_SERVICE);
        return Utils.createKerberosTicketForServer("jduke", "theduke", serverName);
    }

    /**
     * Implements testing of SPNEGO authentication workflow with configurable parameters such supported mechanisms, tokens,
     * expected continuation and checking responses.
     *
     * @param uri                  test URI which is protected by SPNEGO/Kerberos authentication.
     * @param mechTypesOids        array of supported mechanisms by the client in decreasing preference order (favorite choice first)
     * @param initMechToken        initial token (optimistic mechanism token) - token for the first of supported mechanisms
     * @param responseToken        token which is used in the second round when the server requests using Kerberos as a mechanism for
     *                             authentication
     * @param continuationExpected flag which says that we expect server to require the second round of authentication (i.e.
     *                             server asks to send Kerberos token)
     * @param successExpected      flag which says that we expect the authentication finishes with success (in the first or second
     *                             round - which depends on the continuationExpected param)
     */
    private void assertSpnegoWorkflow(URI uri, final String[] mechTypesOids, final byte[] initMechToken,
                                      final byte[] responseToken, boolean continuationExpected, boolean successExpected)
            throws IOException {
        try (CloseableHttpClient httpClient = HttpClientBuilder.create().build()) {
            final HttpGet httpGet = new HttpGet(uri);
            HttpResponse response = httpClient.execute(httpGet);
            assertEquals(HttpServletResponse.SC_UNAUTHORIZED, response.getStatusLine().getStatusCode());
            assertHttpHeader(response, HEADER_WWW_AUTHENTICATE, "Negotiate");
            EntityUtils.consume(response.getEntity());

            byte[] spnegoInitToken = KerberosTestUtils.generateSpnegoTokenInit(initMechToken, mechTypesOids);
            httpGet.setHeader(HEADER_AUTHORIZATION, "Negotiate " + Base64.getEncoder().encodeToString(spnegoInitToken));

            response = httpClient.execute(httpGet);
            if (LOGGER.isTraceEnabled()) {
                LOGGER.trace("Negotiate response in HTTP header:\n" + KerberosTestUtils.dumpNegotiateHeader(response));
            }

            if (continuationExpected) {
                assertEquals(HttpServletResponse.SC_UNAUTHORIZED, response.getStatusLine().getStatusCode());
                // Assume that the server selects Kerberos v5 mechanism - OID: '1 2 840 113554 1 2 2'
                assertHttpHeader(response, HEADER_WWW_AUTHENTICATE, HEADER_VAL_SELECT_KERBEROS_MECH);
                EntityUtils.consume(response.getEntity());

                byte[] spnegoRespToken = KerberosTestUtils.generateSpnegoTokenResp(responseToken);
                httpGet.setHeader(HEADER_AUTHORIZATION, "Negotiate " + Base64.getEncoder().encodeToString(spnegoRespToken));
                response = httpClient.execute(httpGet);
                if (LOGGER.isTraceEnabled()) {
                    LOGGER.trace("Negotiate response in HTTP header:\n" + KerberosTestUtils.dumpNegotiateHeader(response));
                }
            }
            if (successExpected) {
                assertEquals(HttpServletResponse.SC_OK, response.getStatusLine().getStatusCode());
                assertEquals("Unexpected response body", SimpleSecuredServlet.RESPONSE_BODY,
                        EntityUtils.toString(response.getEntity()));
            } else {
                assertEquals(HttpServletResponse.SC_UNAUTHORIZED, response.getStatusLine().getStatusCode());
                assertHttpHeader(response, HEADER_WWW_AUTHENTICATE,
                        // if this is the first round we expect the REJECTED response.
                        "Negotiate" + (continuationExpected ? "" : " oQcwBaADCgEC"));
            }
        }
    }

    // Embedded classes ------------------------------------------------------

    /**
     * A server setup task which configures and starts Kerberos KDC server.
     */
    //@formatter:off
    @CreateDS(
            name = "JBossDS-SPNEGOLoginModuleTestCase",
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
         * java.lang.String)
         */
        @Override
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
            final String hostname = Utils.getCannonicalHost(managementClient);
            final Map<String, String> map = new HashMap<String, String>();
            map.put("hostname", NetworkUtils.formatPossibleIpv6Address(hostname));
            final String ldifContent = StrSubstitutor.replace(
                    IOUtils.toString(
                            SPNEGOLoginModuleTestCase.class.getResourceAsStream(SPNEGOLoginModuleTestCase.class.getSimpleName()
                                    + ".ldif"), "UTF-8"), map);
            LOGGER.trace(ldifContent);
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
         * java.lang.String)
         */
        @Override
        public void tearDown(ManagementClient managementClient, String containerId) throws Exception {
            kdcServer.stop();
            directoryService.shutdown();
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
            if (Utils.IBM_JDK) {
                // http://www.ibm.com/developerworks/java/jdk/security/60/secguides/jgssDocs/api/com/ibm/security/auth/module/Krb5LoginModule.html
                // http://publib.boulder.ibm.com/infocenter/iseries/v5r3/index.jsp?topic=%2Frzaha%2Frzahajgssusejaas20.htm
                // TODO Handle class name on AS side?
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
            final String host = NetworkUtils.formatPossibleIpv6Address(Utils.getCannonicalHost(managementClient));
            kerberosModuleBuilder.putOption("principal", "HTTP/" + host + "@JBOSS.ORG"); //
                    //.putOption("debug", Boolean.FALSE.toString());
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
            return new SecurityDomain[]{hostDomain, spnegoDomain, spnegoWithFallback, formFallbackDomain};
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
            //map.put("sun.security.krb5.debug", TRUE);
            map.put(SecurityConstants.DISABLE_SECDOMAIN_OPTION, TRUE);
            return mapToSystemProperties(map);
        }

    }

}
