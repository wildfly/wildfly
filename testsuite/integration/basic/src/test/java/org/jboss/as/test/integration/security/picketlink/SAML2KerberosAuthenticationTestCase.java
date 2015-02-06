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

package org.jboss.as.test.integration.security.picketlink;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.hasItem;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.Assert.assertThat;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import javax.naming.Context;
import javax.security.auth.Subject;
import javax.security.auth.login.Configuration;
import javax.security.auth.login.LoginContext;
import javax.security.auth.login.LoginException;
import javax.servlet.http.HttpServletResponse;

import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpHeaders;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.auth.AuthScope;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.params.AuthPolicy;
import org.apache.http.client.params.ClientPNames;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpParams;
import org.apache.http.util.EntityUtils;
import org.hamcrest.Matcher;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.OperateOnDeployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.as.arquillian.api.ServerSetup;
import org.jboss.as.arquillian.api.ServerSetupTask;
import org.jboss.as.arquillian.container.ManagementClient;
import org.jboss.as.network.NetworkUtils;
import org.jboss.as.security.Constants;
import org.jboss.as.test.integration.security.common.AbstractKrb5ConfServerSetupTask;
import org.jboss.as.test.integration.security.common.AbstractSecurityDomainsServerSetupTask;
import org.jboss.as.test.integration.security.common.Krb5LoginConfiguration;
import org.jboss.as.test.integration.security.common.NullHCCredentials;
import org.jboss.as.test.integration.security.common.Utils;
import org.jboss.as.test.integration.security.common.config.SecurityDomain;
import org.jboss.as.test.integration.security.common.config.SecurityModule;
import org.jboss.as.test.integration.security.common.negotiation.JBossNegotiateSchemeFactory;
import org.jboss.as.test.integration.security.common.negotiation.KerberosTestUtils;
import org.jboss.as.test.integration.security.common.servlets.PrincipalPrintingServlet;
import org.jboss.as.test.integration.security.common.servlets.RolePrintingServlet;
import org.jboss.logging.Logger;
import org.jboss.security.auth.callback.UsernamePasswordHandler;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Tests for integration of the IDP and Kerberos.
 *
 * @author Hynek Mlnarik
 */
@RunWith(Arquillian.class)
@ServerSetup({ KerberosServerSetupTask.Krb5ConfServerSetupTask.class, KerberosServerSetupTask.SystemPropertiesSetup.class,
        KerberosServerSetupTask.class, SAML2KerberosAuthenticationTestCase.SecurityDomainsSetup.class })
@RunAsClient
@Ignore("AS7-6796 - Undertow SPNEGO")
public class SAML2KerberosAuthenticationTestCase {

    private static final String SERVICE_PROVIDER_NAME = "SP_DEPLOYMENT";
    private static final String IDENTITY_PROVIDER_NAME = "IDP_DEPLOYMENT";

    private static final String SP_DEPLOYMENT_NAME = "test-" + SERVICE_PROVIDER_NAME;
    private static final String IDP_DEPLOYMENT_NAME = "idp-test-" + SERVICE_PROVIDER_NAME;

    private static final String SERVICE_PROVIDER_REALM = "spRealm";
    private static final String IDENTITY_PROVIDER_REALM = IDP_DEPLOYMENT_NAME;

    private static final Logger LOGGER = Logger.getLogger(SAML2KerberosAuthenticationTestCase.class);

    private static final String PICKETLINK_MODULE_NAME = "org.picketlink";
    private static final String JBOSS_NEGOTIATION_MODULE_NAME = "org.jboss.security.negotiation";

    private static final String DUKE_PASSWORD = "theduke";

    @ArquillianResource
    ManagementClient mgmtClient;

    private static void consumeResponse(final HttpResponse response) {
        HttpEntity entity = response.getEntity();
        EntityUtils.consumeQuietly(entity);
    }

    // Public methods --------------------------------------------------------

    /**
     * Skip unsupported/unstable/buggy Kerberos configurations.
     */
    @BeforeClass
    public static void beforeClass() {
        KerberosTestUtils.assumeKerberosAuthenticationSupported(null);
    }

    /**
     * Creates a {@link WebArchive} for given security domain.
     * 
     * @return
     */
    @Deployment(name = SERVICE_PROVIDER_NAME)
    public static WebArchive createSpWar() {
        LOGGER.info("Creating deployment for " + SP_DEPLOYMENT_NAME);

        final WebArchive war = ShrinkWrap.create(WebArchive.class, SP_DEPLOYMENT_NAME + ".war");
        war.addClasses(RolePrintingServlet.class, PrincipalPrintingServlet.class);
        war.addAsWebInfResource(SAML2KerberosAuthenticationTestCase.class.getPackage(),
                SAML2KerberosAuthenticationTestCase.class.getSimpleName() + "-web.xml", "web.xml");

        war.addAsWebInfResource(Utils.getJBossWebXmlAsset(SERVICE_PROVIDER_REALM,
                "org.picketlink.identity.federation.bindings.tomcat.sp.ServiceProviderAuthenticator"), "jboss-web.xml");

        war.addAsManifestResource(Utils.getJBossDeploymentStructure(PICKETLINK_MODULE_NAME, JBOSS_NEGOTIATION_MODULE_NAME),
                "jboss-deployment-structure.xml");
        war.addAsWebInfResource(
                new StringAsset(PicketLinkTestBase.propertiesReplacer("picketlink-sp.xml", SP_DEPLOYMENT_NAME, "REDIRECT",
                        IDP_DEPLOYMENT_NAME)), "picketlink.xml");

        war.add(new StringAsset("Welcome to deployment: " + SP_DEPLOYMENT_NAME), "index.jsp");

        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug(war.toString(true));
        }

        return war;
    }

    /**
     * Creates a {@link WebArchive} for given security domain.
     * 
     * @return
     */
    @Deployment(name = IDENTITY_PROVIDER_NAME)
    public static WebArchive createIdpWar() {
        LOGGER.info("Creating deployment for " + IDP_DEPLOYMENT_NAME);
        final WebArchive war = ShrinkWrap.create(WebArchive.class, IDP_DEPLOYMENT_NAME + ".war");
        war.addAsWebInfResource(SAML2KerberosAuthenticationTestCase.class.getPackage(),
                SAML2KerberosAuthenticationTestCase.class.getSimpleName() + "-idp-web.xml", "web.xml");

        war.addAsWebInfResource(Utils.getJBossWebXmlAsset(IDP_DEPLOYMENT_NAME,
                "org.jboss.security.negotiation.NegotiationAuthenticator",
                "org.picketlink.identity.federation.bindings.tomcat.idp.IDPWebBrowserSSOValve"), "jboss-web.xml");

        war.addAsManifestResource(Utils.getJBossDeploymentStructure(PICKETLINK_MODULE_NAME, JBOSS_NEGOTIATION_MODULE_NAME),
                "jboss-deployment-structure.xml");
        war.addAsWebInfResource(
                new StringAsset(PicketLinkTestBase.propertiesReplacer("picketlink-idp.xml", IDP_DEPLOYMENT_NAME, "",
                        IDP_DEPLOYMENT_NAME)), "picketlink.xml");
        war.addAsWebResource(SAML2KerberosAuthenticationTestCase.class.getPackage(), "error.jsp", "error.jsp");
        war.addAsWebResource(SAML2KerberosAuthenticationTestCase.class.getPackage(), "login.jsp", "login.jsp");
        war.add(new StringAsset("Welcome to IdP"), "index.jsp");
        war.add(new StringAsset("Welcome to IdP hosted"), "hosted/index.jsp");

        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug(war.toString(true));
        }

        return war;
    }

    /**
     * Test for SPNEGO working.
     *
     * @throws Exception
     */
    @Test
    @OperateOnDeployment(SERVICE_PROVIDER_NAME)
    public void testNegotiateHttpHeader(@ArquillianResource URL webAppURL,
            @ArquillianResource @OperateOnDeployment(IDENTITY_PROVIDER_NAME) URL idpURL) throws Exception {
        DefaultHttpClient httpClient = new DefaultHttpClient();
        try {
            final HttpGet httpGet = new HttpGet(webAppURL.toURI());
            final HttpResponse response = httpClient.execute(httpGet);

            assertThat("Unexpected status code.", response.getStatusLine().getStatusCode(),
                    equalTo(HttpServletResponse.SC_UNAUTHORIZED));

            final Header[] authnHeaders = response.getHeaders("WWW-Authenticate");
            assertThat("WWW-Authenticate header is present", authnHeaders, notNullValue());
            assertThat("WWW-Authenticate header is non-empty", authnHeaders.length, not(equalTo(0)));

            final Set<? super String> authnHeaderValues = new HashSet<String>();
            for (final Header header : authnHeaders) {
                authnHeaderValues.add(header.getValue());
            }

            Matcher<String> matcherContainsString = containsString("Negotiate");
            Matcher<Iterable<? super String>> matcherAnyContainsNegotiate = hasItem(matcherContainsString);
            assertThat("WWW-Authenticate [Negotiate] header is missing", authnHeaderValues, matcherAnyContainsNegotiate);

            consumeResponse(response);
        } finally {
            // When HttpClient instance is no longer needed,
            // shut down the connection manager to ensure
            // immediate deallocation of all system resources
            httpClient.getConnectionManager().shutdown();
        }
    }

    /**
     * Test roles for jduke user.
     * 
     * @throws Exception
     */
    @Test
    @OperateOnDeployment(SERVICE_PROVIDER_NAME)
    public void testJDukeRoles(@ArquillianResource URL webAppURL,
            @ArquillianResource @OperateOnDeployment(IDENTITY_PROVIDER_NAME) URL idpURL) throws Exception {
        final URI rolesPrintingURL = new URI(webAppURL.toExternalForm() + RolePrintingServlet.SERVLET_PATH.substring(1)
                + "?test=testDeploymentViaKerberos&" + KerberosServerSetupTask.QUERY_ROLES);

        String responseBody = makeCallWithKerberosAuthn(rolesPrintingURL, idpURL.toURI(), "jduke", DUKE_PASSWORD);

        final List<String> assignedRolesList = Arrays.asList(new String[] { "TheDuke", "Echo", "Admin" });
        for (String role : KerberosServerSetupTask.ROLE_NAMES) {
            if (assignedRolesList.contains(role)) {
                assertThat("Missing role assignment", responseBody, containsString("," + role + ","));
            } else {
                assertThat("Unexpected role assignment", responseBody, not(containsString("," + role + ",")));
            }
        }
    }

    /**
     * Test principal for jduke user.
     *
     * @throws Exception
     */
    @Test
    @OperateOnDeployment(SERVICE_PROVIDER_NAME)
    public void testJDukePrincipal(@ArquillianResource URL webAppURL,
            @ArquillianResource @OperateOnDeployment(IDENTITY_PROVIDER_NAME) URL idpURL) throws Exception {
        final String cannonicalHost = Utils.getCannonicalHost(mgmtClient);
        final URI principalPrintingURL = new URI(webAppURL.toExternalForm()
                + PrincipalPrintingServlet.SERVLET_PATH.substring(1) + "?test=testDeploymentViaKerberos");
        String responseBody = makeCallWithKerberosAuthn(principalPrintingURL,
                Utils.replaceHost(idpURL.toURI(), cannonicalHost), "jduke", DUKE_PASSWORD);

        assertThat("Unexpected principal", responseBody, equalTo("jduke"));
    }

    // Private methods -------------------------------------------------------

    /**
     * Returns response body for the given URL request as a String. It also checks if the returned HTTP status code is the
     * expected one. If the server returns {@link HttpServletResponse#SC_UNAUTHORIZED} and an username is provided, then the
     * given user is authenticated against Kerberos and a new request is executed under the new subject.
     *
     * @param uri URI to which the request should be made
     * @param user Username
     * @param pass Password
     * @return HTTP response body
     * @throws IOException
     * @throws URISyntaxException
     * @throws PrivilegedActionException
     * @throws LoginException
     */
    public static String makeCallWithKerberosAuthn(URI uri, URI idpUri, final String user, final String pass)
            throws IOException, URISyntaxException,
            PrivilegedActionException, LoginException {
        
        final String canonicalHost = Utils.getDefaultHost(true);
        uri = Utils.replaceHost(uri, canonicalHost);
        idpUri = Utils.replaceHost(idpUri, canonicalHost);

        LOGGER.info("Making call to: " + uri);
        LOGGER.info("Expected IDP: " + idpUri);

        // Use our custom configuration to avoid reliance on external config
        Configuration.setConfiguration(new Krb5LoginConfiguration());

        // 1. Authenticate to Kerberos.
        final LoginContext lc = new LoginContext(Utils.class.getName(), new UsernamePasswordHandler(user, pass));
        lc.login();

        // 2. Perform the work as authenticated Subject.
        final String responseBody = Subject.doAs(lc.getSubject(), new HttpGetInKerberos(uri, idpUri));
        lc.logout();
        return responseBody;
    }

    // Inner classes ------------------------------------------------------

    /**
     * A {@link ServerSetupTask} instance which creates security domains for this test case.
     * 
     * @author Hynek Mlnarik
     */
    static class SecurityDomainsSetup extends AbstractSecurityDomainsServerSetupTask {

        private static final String SERVER_SECURITY_DOMAIN = "host";

        /**
         * Returns SecurityDomains configuration for this testcase.
         * 
         * @see org.jboss.as.test.integration.security.common.AbstractSecurityDomainsServerSetupTask#getSecurityDomains()
         */
        @Override
        protected SecurityDomain[] getSecurityDomains() {
            List<SecurityDomain> res = new LinkedList<SecurityDomain>();

            // Add host security domain
            res.add(new SecurityDomain.Builder()
                    .name(SERVER_SECURITY_DOMAIN)
                    .cacheType("default")
                    .loginModules(
                            new SecurityModule.Builder()
                                    .name(Krb5LoginConfiguration.getLoginModule())
                                    .flag(Constants.REQUIRED)
                                    .options(
                                            Krb5LoginConfiguration.getOptions(
                                                    KerberosServerSetupTask.getHttpServicePrincipal(managementClient),
                                                    AbstractKrb5ConfServerSetupTask.HTTP_KEYTAB_FILE, true)).build()).build());

            // Add IdP security domain
            res.add(new SecurityDomain.Builder()
                    .name(IDENTITY_PROVIDER_REALM)
                    .loginModules(
                            new SecurityModule.Builder()
                                    // Login module used for password negotiation
                                    .name("SPNEGO").flag(Constants.REQUISITE).putOption("password-stacking", "useFirstPass")
                                    .putOption("serverSecurityDomain", SERVER_SECURITY_DOMAIN)
                                    .putOption("removeRealmFromPrincipal", "true").build(),

                            new SecurityModule.Builder()
                                    // Login module used for role retrieval
                                    .name("org.jboss.security.auth.spi.LdapExtLoginModule")
                                    .flag(Constants.REQUIRED)
                                    .putOption("password-stacking", "useFirstPass")
                                    .putOption(
                                            Context.PROVIDER_URL,
                                            "ldap://"
                                                    + NetworkUtils.formatPossibleIpv6Address(Utils
                                                            .getCannonicalHost(managementClient)) + ":"
                                                    + KerberosServerSetupTask.LDAP_PORT)
                                    .putOption("baseCtxDN", "ou=People,dc=jboss,dc=org").putOption("baseFilter", "(uid={0})")
                                    .putOption("rolesCtxDN", "ou=Roles,dc=jboss,dc=org")
                                    .putOption("roleFilter", "(|(objectClass=referral)(member={1}))")
                                    .putOption("roleAttributeID", "cn").putOption("referralUserAttributeIDToCheck", "member")
                                    .putOption("bindDN", KerberosServerSetupTask.SECURITY_PRINCIPAL)
                                    .putOption("bindCredential", KerberosServerSetupTask.SECURITY_CREDENTIALS)
                                    .putOption(Context.INITIAL_CONTEXT_FACTORY, "com.sun.jndi.ldap.LdapCtxFactory")
                                    .putOption(Context.SECURITY_AUTHENTICATION, "simple").putOption(Context.REFERRAL, "follow")
                                    .putOption("throwValidateError", "true").putOption("roleRecursion", "5").build()).build());

            // Add SP security domain
            res.add(new SecurityDomain.Builder()
                    .name(SERVICE_PROVIDER_REALM)
                    .loginModules(
                            new SecurityModule.Builder()
                                    .name("org.picketlink.identity.federation.bindings.jboss.auth.SAML2LoginModule")
                                    .flag(Constants.REQUIRED).build()).build());

            return res.toArray(new SecurityDomain[0]);
        }
    }

    /**
     * Class which is intended to be run in context of a Kerberos-authenticated user, to test the http authentication via IdP.
     */
    private static class HttpGetInKerberos implements PrivilegedExceptionAction<String> {

        private final URI uri;
        private final URI idpUri;

        /**
         * Initializes the instance.
         * 
         * @param uri URI of the web application
         * @param idpUri URI of the respective identity provider
         */
        public HttpGetInKerberos(URI uri, URI idpUri) {
            this.uri = uri;
            this.idpUri = idpUri;
        }

        /**
         * Performs authentication via IdP and retrieves the document body from the {@link #uri}.
         * 
         * @return Body of the response retrieved from {@link #uri}
         * @throws Exception
         */
        @Override
        public String run() throws Exception {
            final DefaultHttpClient httpClient = new DefaultHttpClient();
            httpClient.getAuthSchemes().register(AuthPolicy.SPNEGO, new JBossNegotiateSchemeFactory(true));
            httpClient.getCredentialsProvider().setCredentials(new AuthScope(null, -1, null), new NullHCCredentials());

            final HttpParams doNotRedirect = new BasicHttpParams();
            doNotRedirect.setParameter(ClientPNames.HANDLE_REDIRECTS, false);
            doNotRedirect.setParameter(ClientPNames.HANDLE_AUTHENTICATION, true);

            final HttpParams doRedirect = new BasicHttpParams();
            doRedirect.setParameter(ClientPNames.HANDLE_AUTHENTICATION, true);
            doRedirect.setParameter(ClientPNames.HANDLE_REDIRECTS, true);

            try {
                // 1. Login to IdP
                HttpGet initialIdpHttpGet = new HttpGet(this.idpUri); // GET /idp-test-DEP1
                initialIdpHttpGet.setParams(doRedirect);
                HttpResponse response = httpClient.execute(initialIdpHttpGet);
                assertThat("Unexpected status code when expecting successfull kerberos authentication", response
                        .getStatusLine().getStatusCode(), equalTo(HttpStatus.SC_OK));
                consumeResponse(response);

                // 2. Do the work, manually do the redirect
                HttpGet initialHttpGet = new HttpGet(this.uri); // GET /test-DEP1/printRoles?role=TheDuke2&role=...
                initialHttpGet.setParams(doNotRedirect);
                response = httpClient.execute(initialHttpGet);
                assertThat("Unexpected status code when expecting redirect to IdP", response.getStatusLine().getStatusCode(),
                        equalTo(HttpStatus.SC_MOVED_TEMPORARILY));
                String initialHttpGetRedirect = response.getFirstHeader(HttpHeaders.LOCATION).getValue();
                consumeResponse(response);

                HttpGet idpHttpGet = new HttpGet(initialHttpGetRedirect); // GET /idp-test-DEP1/?SAMLRequest=jZLfT4MwEMf.....
                idpHttpGet.setParams(doNotRedirect);
                response = httpClient.execute(idpHttpGet);
                assertThat("Unexpected status code when expecting redirect from SP with SAML request", response.getStatusLine()
                        .getStatusCode(), equalTo(HttpStatus.SC_MOVED_TEMPORARILY));
                String idpHttpGetRedirect = response.getFirstHeader(HttpHeaders.LOCATION).getValue();
                consumeResponse(response);

                HttpGet idpHttpGetRedirectForAuth = new HttpGet(idpHttpGetRedirect); // GET
                                                                                     // /idp-test-DEP1/?SAMLRequest=jZLfT4MwEMf.....,
                                                                                     // Authorization: Negotiate
                idpHttpGetRedirectForAuth.setParams(doNotRedirect);
                response = httpClient.execute(idpHttpGetRedirectForAuth);
                assertThat("Unexpected status code when expecting redirect from IdP with SAML response", response
                        .getStatusLine().getStatusCode(), equalTo(HttpStatus.SC_MOVED_TEMPORARILY));
                String idpHttpGetRedirectAuth = response.getFirstHeader(HttpHeaders.LOCATION).getValue();
                consumeResponse(response);

                HttpGet spHttpGet = new HttpGet(idpHttpGetRedirectAuth); // GET /test-DEP1/?SAMLResponse=...
                spHttpGet.setParams(doNotRedirect);
                response = httpClient.execute(spHttpGet);
                assertThat("Unexpected status code when expecting succesfull authentication to the SP", response
                        .getStatusLine().getStatusCode(), equalTo(HttpStatus.SC_OK));
                return EntityUtils.toString(response.getEntity());
            } finally {
                // When HttpClient instance is no longer needed,
                // shut down the connection manager to ensure
                // immediate deallocation of all system resources
                httpClient.getConnectionManager().shutdown();
            }
        }
    }

}
