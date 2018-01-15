/*
 * Copyright 2016 Red Hat, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.wildfly.test.manual.elytron.seccontext;

import static java.nio.file.StandardCopyOption.REPLACE_EXISTING;
import static java.util.concurrent.TimeUnit.SECONDS;
import static javax.servlet.http.HttpServletResponse.SC_OK;
import static javax.servlet.http.HttpServletResponse.SC_UNAUTHORIZED;
import static org.hamcrest.CoreMatchers.allOf;
import static org.hamcrest.CoreMatchers.anyOf;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.startsWith;
import static org.jboss.as.test.shared.integration.ejb.security.PermissionUtils.createPermissionsXmlAsset;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.wildfly.test.manual.elytron.seccontext.SeccontextUtil.JAR_ENTRY_EJB;
import static org.wildfly.test.manual.elytron.seccontext.SeccontextUtil.SERVER1;
import static org.wildfly.test.manual.elytron.seccontext.SeccontextUtil.SERVER2;
import static org.wildfly.test.manual.elytron.seccontext.SeccontextUtil.WAR_ENTRY_SERVLET_BASIC;
import static org.wildfly.test.manual.elytron.seccontext.SeccontextUtil.WAR_ENTRY_SERVLET_BEARER_TOKEN;
import static org.wildfly.test.manual.elytron.seccontext.SeccontextUtil.WAR_ENTRY_SERVLET_FORM;
import static org.wildfly.test.manual.elytron.seccontext.SeccontextUtil.WAR_WHOAMI;

import java.io.BufferedWriter;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;
import java.net.SocketPermission;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.Base64.Encoder;
import java.util.List;
import java.util.concurrent.Callable;
import org.apache.commons.io.IOUtils;
import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;
import org.jboss.arquillian.container.test.api.ContainerController;
import org.jboss.arquillian.container.test.api.Deployer;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.container.test.api.TargetsContainer;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.as.cli.CommandContext;
import org.jboss.as.cli.CommandLineException;
import org.jboss.as.controller.client.ModelControllerClient;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.controller.operations.common.Util;
import org.jboss.as.network.NetworkUtils;
import org.jboss.as.test.integration.domain.management.util.DomainTestUtils;
import org.jboss.as.test.integration.management.util.CLIOpResult;
import org.jboss.as.test.integration.management.util.CLITestUtil;
import org.jboss.as.test.integration.management.util.MgmtOperationException;
import org.jboss.as.test.integration.management.util.ServerReload;
import org.jboss.as.test.integration.security.common.SecurityTestConstants;
import org.jboss.as.test.integration.security.common.Utils;
import org.jboss.as.test.shared.TestSuiteEnvironment;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;
import org.jboss.logging.Logger;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Before;
import org.junit.runner.RunWith;
import org.wildfly.security.permission.ElytronPermission;

/**
 * Tests for testing (re)authentication and security identity propagation between servers. Test scenarios use following
 * configuration.
 *
 * <h3>Given</h3>
 * <pre>
 * EJBs used for testing:
 * - WhoAmIBean & WhoAmIBeanSFSB - protected (whoami, admin and no-server2-identity roles are allowed), just returns caller principal
 * - EntryBean & EntryBeanSFSB - protected (entry, admin and no-server2-identity roles are allowed), configures identity propagation and calls a remote WhoAmIBean
 *
 * Servlets used for testing:
 * - WhoAmIServlet - protected (servlet and admin roles are allowed) - just returns name of the incoming user name
 * - EntryServlet - protected (servlet and admin roles are allowed) - configures identity propagation and calls a remote WhoAmIBean
 *
 * Deployments used for testing:
 * - entry-ejb.jar (EntryBean & EntryBeanSFSB)
 * - whoami.war (WhoAmIBean & WhoAmIBeanSFSB, WhoAmIServlet)
 * - entry-servlet-basic.war (EntryServlet, WhoAmIServlet) - authentication mechanism BASIC
 * - entry-servlet-form.war (EntryServlet, WhoAmIServlet) - authentication mechanism FORM
 * - entry-servlet-bearer.war (EntryServlet, WhoAmIServlet) - authentication mechanism BEARER_TOKEN
 *
 * Servers started and configured for context propagation scenarios:
 * - seccontext-server1 (standalone-ha.xml)
 *   * entry-ejb.jar
 *   * entry-servlet-basic.war
 *   * entry-servlet-form.war
 *   * entry-servlet-bearer.war
 *   * first-server-chain.war
 * - seccontext-server2 (standalone.xml)
 *   * whoami.war
 *
 * Users used for testing (username==password==role):
 * - entry
 * - whoami
 * - servlet
 * - admin
 * - server (has configured additional permission - RunAsPrincipalPermission)
 * - another-server (used for server chain scenarios)
 * - server-norunas
 * - no-server2-identity
 * </pre>
 *
 * @see ReAuthnType reauthentication types
 * @author Josef Cacek
 */
@RunWith(Arquillian.class)
@RunAsClient
public abstract class AbstractSecurityContextPropagationTestBase {

    private static final Logger LOGGER = Logger.getLogger(AbstractSecurityContextPropagationTestBase.class);

    protected static final ServerHolder server1 = new ServerHolder(SERVER1, TestSuiteEnvironment.getServerAddress(), 0);
    protected static final ServerHolder server2 = new ServerHolder(SERVER2, TestSuiteEnvironment.getServerAddressNode1(), 100);

    private static final Package PACKAGE = AbstractSecurityContextPropagationTestBase.class.getPackage();

    private static final Encoder B64_ENCODER = Base64.getUrlEncoder().withoutPadding();
    private static final String JWT_HEADER_B64 = B64_ENCODER
            .encodeToString("{\"alg\":\"none\",\"typ\":\"JWT\"}".getBytes(StandardCharsets.UTF_8));

    @ArquillianResource
    private static volatile ContainerController containerController;

    @ArquillianResource
    private static volatile Deployer deployer;

    /**
     * Creates deployment with Entry bean - to be placed on the first server.
     */
    @Deployment(name = JAR_ENTRY_EJB, managed = false, testable = false)
    @TargetsContainer(SERVER1)
    public static Archive<?> createEntryBeanDeployment() {
        return ShrinkWrap.create(JavaArchive.class, JAR_ENTRY_EJB + ".jar")
                .addClasses(EntryBean.class, EntryBeanSFSB.class, Entry.class, WhoAmI.class, ReAuthnType.class,
                        SeccontextUtil.class, CallAnotherBeanInfo.class)
                .addAsManifestResource(createPermissionsXmlAsset(new ElytronPermission("authenticate"),
                        new ElytronPermission("getPrivateCredentials"), new ElytronPermission("getSecurityDomain"),
                        new SocketPermission(TestSuiteEnvironment.getServerAddressNode1() + ":8180", "connect,resolve")),
                        "permissions.xml")
                .addAsManifestResource(Utils.getJBossEjb3XmlAsset("seccontext-entry"), "jboss-ejb3.xml");
    }

    /**
     * Creates deployment with Entry servlet and BASIC authentication.
     */
    @Deployment(name = WAR_ENTRY_SERVLET_BASIC, managed = false, testable = false)
    @TargetsContainer(SERVER1)
    public static Archive<?> createEntryServletBasicAuthnDeployment() {
        return createEntryServletDeploymentBase(WAR_ENTRY_SERVLET_BASIC)
                .addAsWebInfResource(new StringAsset(SecurityTestConstants.WEB_XML_BASIC_AUTHN), "web.xml");
    }

    /**
     * Creates deployment with Entry servlet and FORM authentication.
     */
    @Deployment(name = WAR_ENTRY_SERVLET_FORM, managed = false, testable = false)
    @TargetsContainer(SERVER1)
    public static Archive<?> createEntryServletFormAuthnDeployment() {
        return createEntryServletDeploymentBase(WAR_ENTRY_SERVLET_FORM)
                .addAsWebInfResource(PACKAGE, "web-form-authn.xml", "web.xml")
                .addAsWebResource(PACKAGE, "login.html", "login.html").addAsWebResource(PACKAGE, "error.html", "error.html");
    }

    /**
     * Creates deployment with Entry servlet and BEARER authentication.
     */
    @Deployment(name = WAR_ENTRY_SERVLET_BEARER_TOKEN, managed = false, testable = false)
    @TargetsContainer(SERVER1)
    public static Archive<?> createEntryServletBearerAuthnDeployment() {
        return createEntryServletDeploymentBase(WAR_ENTRY_SERVLET_BEARER_TOKEN).addAsWebInfResource(PACKAGE,
                "web-token-authn.xml", "web.xml");
    }

    /**
     * Creates deployment with WhoAmI bean and servlet - to be placed on the second server.
     */
    @Deployment(name = WAR_WHOAMI, managed = false, testable = false)
    @TargetsContainer(SERVER2)
    public static Archive<?> createEjbClientDeployment() {
        return ShrinkWrap.create(WebArchive.class, WAR_WHOAMI + ".war")
                .addClasses(WhoAmIBean.class, WhoAmIBeanSFSB.class, WhoAmI.class, WhoAmIServlet.class, Server2Exception.class)
                .addAsWebInfResource(Utils.getJBossWebXmlAsset("seccontext-web"), "jboss-web.xml")
                .addAsWebInfResource(new StringAsset(SecurityTestConstants.WEB_XML_BASIC_AUTHN), "web.xml")
                .addAsWebInfResource(Utils.getJBossEjb3XmlAsset("seccontext-whoami"), "jboss-ejb3.xml");
    }

    /**
     * Start servers (if not yet started) and if it's the first execution it sets configuration of test servers and deploys test
     * applications.
     */
    @Before
    public void before() throws CommandLineException, IOException, MgmtOperationException {
        setupServer1();
        setupServer2();
    }

    /**
     * Shut down servers.
     */
    @AfterClass
    public static void afterClass() throws IOException {
        server1.shutDown();
        server2.shutDown();
    }

    /**
     * Setup seccontext-server1.
     */
    protected void setupServer1() throws CommandLineException, IOException, MgmtOperationException {
        server1.resetContainerConfiguration(new ServerConfigurationBuilder()
                .withDeployments(JAR_ENTRY_EJB, WAR_ENTRY_SERVLET_BASIC, WAR_ENTRY_SERVLET_FORM,
                        WAR_ENTRY_SERVLET_BEARER_TOKEN)
                .build());
    }

    /**
     * Setup seccontext-server2.
     */
    protected void setupServer2() throws CommandLineException, IOException, MgmtOperationException {
        server2.resetContainerConfiguration(new ServerConfigurationBuilder()
                .withDeployments(WAR_WHOAMI)
                .build());
    }

    /**
     * Returns true if the stateful Entry bean variant should be used by the tests. False otherwise.
     */
    protected abstract boolean isEntryStateful();

    /**
     * Returns true if the stateful WhoAmI bean variant should be used by the tests. False otherwise.
     */
    protected abstract boolean isWhoAmIStateful();

    /**
     * Do HTTP GET request with given client.
     *
     * @param httpClient
     * @param url
     * @param expectedStatus expected status coe
     * @return response body
     */
    protected String doHttpRequest(final CloseableHttpClient httpClient, final URL url, int expectedStatus)
            throws URISyntaxException, IOException, ClientProtocolException, UnsupportedEncodingException {

        return doHttpRequestFormAuthn(httpClient, url, false, null, null, expectedStatus);
    }

    /**
     * Do HTTP request using given client with possible FORM authentication.
     *
     * @param httpClient client instance
     * @param url URL to make request to
     * @param loginFormExpected flag which says if login (FORM) is expected, if true username and password arguments are used to
     *        login.
     * @param username user to fill into the login form
     * @param password password to fill into the login form
     * @param expectedStatus expected status code
     * @return response body
     */
    protected String doHttpRequestFormAuthn(final CloseableHttpClient httpClient, final URL url, boolean loginFormExpected,
            String username, String password, int expectedStatus)
            throws URISyntaxException, IOException, ClientProtocolException, UnsupportedEncodingException {

        HttpGet httpGet = new HttpGet(url.toURI());

        HttpResponse response = httpClient.execute(httpGet);

        HttpEntity entity = response.getEntity();
        assertNotNull(entity);
        String responseBody = EntityUtils.toString(entity);
        if (loginFormExpected) {
            assertThat("Login page was expected", responseBody, containsString("j_security_check"));
            assertEquals("HTTP OK response for login page was expected", SC_OK, response.getStatusLine().getStatusCode());

            // We should now login with the user name and password
            HttpPost httpPost = new HttpPost(
                    server1.getApplicationHttpUrl() + "/" + WAR_ENTRY_SERVLET_FORM + "/j_security_check");

            List<NameValuePair> nvps = new ArrayList<NameValuePair>();
            nvps.add(new BasicNameValuePair("j_username", username));
            nvps.add(new BasicNameValuePair("j_password", password));

            httpPost.setEntity(new UrlEncodedFormEntity(nvps, "UTF-8"));

            response = httpClient.execute(httpPost);
            entity = response.getEntity();
            assertNotNull(entity);
            responseBody = EntityUtils.toString(entity);
        } else {
            assertThat("Login page was not expected", responseBody, not(containsString("j_security_check")));
        }
        assertEquals("Unexpected status code", expectedStatus, response.getStatusLine().getStatusCode());
        return responseBody;
    }

    /**
     * Do HTTP request using given client with BEARER_TOKEN authentication. The implementation makes 2 calls - the first without
     * Authorization header provided (just to check response code and WWW-Authenticate header value), the second with
     * Authorization header.
     *
     * @param httpClient client instance
     * @param url URL to make request to
     * @param token bearer token
     * @param expectedStatus expected status code
     * @return response body
     */
    protected String doHttpRequestTokenAuthn(final CloseableHttpClient httpClient, final URL url, String token,
            int expectedStatusCode)
            throws URISyntaxException, IOException, ClientProtocolException, UnsupportedEncodingException {

        final HttpGet httpGet = new HttpGet(url.toURI());

        HttpResponse response = httpClient.execute(httpGet);
        assertEquals("Unexpected HTTP response status code.", SC_UNAUTHORIZED, response.getStatusLine().getStatusCode());
        Header[] authenticateHeaders = response.getHeaders("WWW-Authenticate");
        assertTrue("Expected WWW-Authenticate header was not present in the HTTP response",
                authenticateHeaders != null && authenticateHeaders.length > 0);
        boolean bearerAuthnHeaderFound = false;
        for (Header header : authenticateHeaders) {
            final String headerVal = header.getValue();
            if (headerVal != null && headerVal.startsWith("Bearer")) {
                bearerAuthnHeaderFound = true;
                break;
            }
        }
        assertTrue("WWW-Authenticate response header didn't request expected Bearer token authentication",
                bearerAuthnHeaderFound);
        HttpEntity entity = response.getEntity();
        if (entity != null)
            EntityUtils.consume(entity);

        httpGet.addHeader("Authorization", "Bearer " + token);
        response = httpClient.execute(httpGet);
        assertEquals("Unexpected status code returned after the authentication.", expectedStatusCode,
                response.getStatusLine().getStatusCode());
        return EntityUtils.toString(response.getEntity());
    }

    /**
     * Creates callable for executing {@link Entry#doubleWhoAmI(String, String, ReAuthnType, String)} as "whoami" user.
     *
     * @param type reauthentication reauthentication type used within the doubleWhoAmI
     * @return Callable
     */
    protected Callable<String[]> getDoubleWhoAmICallable(final ReAuthnType type) {
        return getDoubleWhoAmICallable(type, "whoami", "whoami");
    }

    /**
     * Creates callable for executing {@link Entry#doubleWhoAmI(String, String, ReAuthnType, String)} as "whoami" user.
     *
     * @param type reauthentication reauthentication type used within the doubleWhoAmI
     * @param authzName - authorization name
     * @return Callable
     */
    protected Callable<String[]> getDoubleWhoAmICallable(final ReAuthnType type, final String authzName) {
        return getDoubleWhoAmICallable(type, "whoami", "whoami", authzName);
    }

    /**
     * Creates a callable for executing {@link Entry#doubleWhoAmI(String, String, ReAuthnType, String)} as given user.
     *
     * @param type reauthentication re-authentication type used within the doubleWhoAmI
     * @param username
     * @param password
     * @return
     */
    protected Callable<String[]> getDoubleWhoAmICallable(final ReAuthnType type, final String username, final String password) {
        return getDoubleWhoAmICallable(type, username, password, null);
    }

    /**
     * Creates a callable for executing {@link Entry#doubleWhoAmI(CallAnotherBeanInfo)} as given user.
     *
     * @param type reauthentication re-authentication type used within the doubleWhoAmI
     * @param username
     * @param password
     * @param authzName - authorization name
     * @return
     */
    protected Callable<String[]> getDoubleWhoAmICallable(final ReAuthnType type, final String username, final String password,
            final String authzName) {

        return () -> {
            final Entry bean = SeccontextUtil.lookup(
                    SeccontextUtil.getRemoteEjbName(JAR_ENTRY_EJB, "EntryBean", Entry.class.getName(), isEntryStateful()),
                    server1.getApplicationRemotingUrl());
            final String server2Url = server2.getApplicationRemotingUrl();
            return bean.doubleWhoAmI(new CallAnotherBeanInfo.Builder()
                    .username(username)
                    .password(password)
                    .authzName(authzName)
                    .type(type)
                    .providerUrl(server2Url)
                    .statefullWhoAmI(isWhoAmIStateful())
                    .build());
        };
    }

    /**
     * Creates a callable for executing {@link Entry#whoAmIAndIllegalStateException(CallAnotherBeanInfo)} as given user.
     *
     * @param type reauthentication re-authentication type used within the doubleWhoAmI
     * @param username
     * @param password
     * @return
     */
    protected Callable<String[]> getWhoAmIAndIllegalStateExceptionCallable(final ReAuthnType type, final String username,
            final String password) {

        return () -> {
            final Entry bean = SeccontextUtil.lookup(
                    SeccontextUtil.getRemoteEjbName(JAR_ENTRY_EJB, "EntryBean", Entry.class.getName(), isEntryStateful()),
                    server1.getApplicationRemotingUrl());
            final String server2Url = server2.getApplicationRemotingUrl();
            return bean.whoAmIAndIllegalStateException(new CallAnotherBeanInfo.Builder()
                    .username(username)
                    .password(password)
                    .type(type)
                    .providerUrl(server2Url)
                    .statefullWhoAmI(isWhoAmIStateful())
                    .build());
        };
    }

    /**
     * Creates a callable for executing {@link Entry#whoAmIAndServer2Exception(CallAnotherBeanInfo)} as given user.
     *
     * @param type reauthentication re-authentication type used within the doubleWhoAmI
     * @param username
     * @param password
     * @return
     */
    protected Callable<String[]> getWhoAmIAndServer2ExceptionCallable(final ReAuthnType type, final String username,
            final String password) {

        return () -> {
            final Entry bean = SeccontextUtil.lookup(
                    SeccontextUtil.getRemoteEjbName(JAR_ENTRY_EJB, "EntryBean", Entry.class.getName(), isEntryStateful()),
                    server1.getApplicationRemotingUrl());
            final String server2Url = server2.getApplicationRemotingUrl();
            return bean.whoAmIAndServer2Exception(new CallAnotherBeanInfo.Builder()
                    .username(username)
                    .password(password)
                    .type(type)
                    .providerUrl(server2Url)
                    .statefullWhoAmI(isWhoAmIStateful())
                    .build());
        };
    }

    protected Callable<String> getEjbToServletCallable(final ReAuthnType type, final String username, final String password) {
        return () -> {
            final Entry bean = SeccontextUtil.lookup(
                    SeccontextUtil.getRemoteEjbName(JAR_ENTRY_EJB, "EntryBean", Entry.class.getName(), isEntryStateful()),
                    server1.getApplicationRemotingUrl());
            final String servletUrl = server2.getApplicationHttpUrl() + "/" + WAR_WHOAMI + WhoAmIServlet.SERVLET_PATH;
            return bean.readUrl(username, password, type, new URL(servletUrl));
        };
    }

    protected static org.hamcrest.Matcher<java.lang.String> isEjbAuthenticationError() {
        // different behavior for stateless and stateful beans
        // is reported under https://issues.jboss.org/browse/JBEAP-12439
        return anyOf(startsWith("javax.ejb.NoSuchEJBException: EJBCLIENT000079"),
                startsWith("javax.naming.CommunicationException: EJBCLIENT000062"), containsString("JBREM000308"),
                containsString("javax.security.sasl.SaslException: Authentication failed"));
    }

    protected static org.hamcrest.Matcher<java.lang.String> isExpectedIllegalStateException() {
        return containsString("EJBException: java.lang.IllegalStateException: Expected IllegalStateException");
    }

    protected static org.hamcrest.Matcher<java.lang.String> isClassNotFoundException_Server2Exception() {
        return allOf(startsWith("javax.ejb.EJBException"),
                containsString("ClassNotFoundException: org.wildfly.test.manual.elytron.seccontext.Server2Exception"));
    }

    protected static org.hamcrest.Matcher<java.lang.String> isEvidenceVerificationError() {
        return startsWith("java.lang.SecurityException: ELY01151");
    }

    protected static org.hamcrest.Matcher<java.lang.String> isEjbAccessException() {
        return startsWith("javax.ejb.EJBAccessException");
    }

    protected String createJwtToken(String userName) {
        String jwtPayload = String.format("{" //
                + "\"iss\": \"issuer.wildfly.org\"," //
                + "\"sub\": \"elytron@wildfly.org\"," //
                + "\"exp\": 2051222399," //
                + "\"aud\": \"%1$s\"," //
                + "\"groups\": [\"%1$s\"]" //
                + "}", userName);
        return JWT_HEADER_B64 + "." + B64_ENCODER.encodeToString(jwtPayload.getBytes(StandardCharsets.UTF_8)) + ".";
    }

    protected URL getEntryServletUrl(String warName, String username, String password, ReAuthnType type) throws IOException {
        return getEntryServletUrl(warName, username, password, null, type);
    }

    protected URL getEntryServletUrl(String warName, String username, String password, String authzName, ReAuthnType type) throws IOException {
        final StringBuilder sb = new StringBuilder(server1.getApplicationHttpUrl() + "/" + warName + EntryServlet.SERVLET_PATH);
        addQueryParam(sb, EntryServlet.PARAM_USERNAME, username);
        addQueryParam(sb, EntryServlet.PARAM_PASSWORD, password);
        addQueryParam(sb, EntryServlet.PARAM_AUTHZ_NAME, authzName);
        addQueryParam(sb, EntryServlet.PARAM_STATEFULL, String.valueOf(isWhoAmIStateful()));
        addQueryParam(sb, EntryServlet.PARAM_CREATE_SESSION, String.valueOf(true));
        addQueryParam(sb, EntryServlet.PARAM_REAUTHN_TYPE, type.name());
        addQueryParam(sb, EntryServlet.PARAM_PROVIDER_URL, server2.getApplicationRemotingUrl());
        return new URL(sb.toString());
    }

    private static void addQueryParam(StringBuilder sb, String paramName, String paramValue) {
        final String encodedPair = Utils.encodeQueryParam(paramName, paramValue);
        if (encodedPair != null) {
            sb.append(sb.indexOf("?") < 0 ? "?" : "&").append(encodedPair);
        }
    }

    /**
     * Creates deployment base with Entry servlet. It doesn't contain web.xml and related resources if needed (e.g. login page).
     */
    private static WebArchive createEntryServletDeploymentBase(String name) {
        return ShrinkWrap.create(WebArchive.class, name + ".war")
                .addClasses(EntryServlet.class, WhoAmIServlet.class, WhoAmI.class, ReAuthnType.class, SeccontextUtil.class)
                .addAsManifestResource(createPermissionsXmlAsset(new ElytronPermission("authenticate"),
                        new ElytronPermission("getPrivateCredentials"), new ElytronPermission("getSecurityDomain"),
                        new SocketPermission(TestSuiteEnvironment.getServerAddressNode1() + ":8180", "connect,resolve")),
                        "permissions.xml")
                .addAsWebInfResource(Utils.getJBossWebXmlAsset("seccontext-web"), "jboss-web.xml");
    }

    protected static class ServerHolder {
        private final String name;
        private final String host;
        private final int portOffset;
        private volatile ModelControllerClient client;
        private volatile CommandContext commandCtx;
        private volatile ByteArrayOutputStream consoleOut = new ByteArrayOutputStream();
        private final List<String> deployments = new ArrayList<>();
        private String jbossServerConfigDir;
        private Path propertyFile;

        private volatile String snapshot;

        public ServerHolder(String name, String host, int portOffset) {
            this.name = name;
            this.host = host;
            this.portOffset = portOffset;
        }

        public void resetContainerConfiguration(ServerConfiguration config)
                throws CommandLineException, IOException, MgmtOperationException {
            if (!containerController.isStarted(name)) {
                containerController.start(name);
                client = ModelControllerClient.Factory.create(host, getManagementPort());
                commandCtx = CLITestUtil.getCommandContext(host, getManagementPort(), null, consoleOut, -1);
                commandCtx.connectController();
                readSnapshot();
                jbossServerConfigDir = readJbossServerConfigDir();

                if (snapshot == null) {
                    // configure each server just once
                    takeSnapshot();
                    createPropertyFile(config.getAdditionalUsers());
                    final File cliFIle = File.createTempFile("seccontext-", ".cli");
                    try (FileOutputStream fos = new FileOutputStream(cliFIle)) {
                        IOUtils.copy(
                                AbstractSecurityContextPropagationTestBase.class.getResourceAsStream("seccontext-setup.cli"),
                                fos);
                    }
                    addCliCommands(cliFIle, config.getCliCommands());
                    runBatch(cliFIle);
                    switchJGroupsToTcpping();
                    cliFIle.delete();
                    reload();

                    deployments.addAll(config.getDeployments());
                    for (String deployment : config.getDeployments()) {
                        deployer.deploy(deployment);
                    }
                }
            }
        }

        public void shutDown() throws IOException {
            if (containerController.isStarted(name)) {
                // deployer.undeploy(name);
                for (String deployment : deployments) {
                    deployer.undeploy(deployment);
                }
                commandCtx.terminateSession();
                client.close();
                containerController.stop(name);
                reloadFromSnapshot();
                Files.deleteIfExists(propertyFile);
            }
        }

        public int getManagementPort() {
            return 9990 + portOffset;
        }

        public int getApplicationPort() {
            return 8080 + portOffset;
        }

        public String getApplicationHttpUrl() throws IOException {
            return "http://" + NetworkUtils.formatPossibleIpv6Address(host) + ":" + getApplicationPort();
        }

        public String getApplicationRemotingUrl() throws IOException {
            return "remote+" + getApplicationHttpUrl();
        }

        /**
         * Sends command line to CLI.
         *
         * @param line specifies the command line.
         * @param ignoreError if set to false, asserts that handling the line did not result in a
         *        {@link org.jboss.as.cli.CommandLineException}.
         *
         * @return true if the CLI is in a non-error state following handling the line
         */
        public boolean sendLine(String line, boolean ignoreError) {
            consoleOut.reset();
            if (ignoreError) {
                commandCtx.handleSafe(line);
                return commandCtx.getExitCode() == 0;
            } else {
                try {
                    commandCtx.handle(line);
                } catch (CommandLineException e) {
                    StringWriter stackTrace = new StringWriter();
                    e.printStackTrace(new PrintWriter(stackTrace));
                    Assert.fail(String.format("Failed to execute line '%s'%n%s", line, stackTrace.toString()));
                }
            }
            return true;
        }

        /**
         * Runs given CLI script file as a batch.
         *
         * @param batchFile CLI file to run in batch
         * @return true if CLI returns Success
         */
        public boolean runBatch(File batchFile) throws IOException {
            sendLine("run-batch --file=\"" + batchFile.getAbsolutePath() + "\" -v", false);
            if (consoleOut.size() <= 0) {
                return false;
            }
            return new CLIOpResult(ModelNode.fromStream(new ByteArrayInputStream(consoleOut.toByteArray())))
                    .isIsOutcomeSuccess();
        }

        /**
         * Switch JGroups subsystem (if present) from using UDP multicast to TCPPING discovery protocol.
         */
        private void switchJGroupsToTcpping() throws IOException {
            consoleOut.reset();
            try {
                commandCtx.handle("if outcome==success of /subsystem=jgroups:read-resource()");
                // TODO This command is deprecated
                commandCtx.handle(String.format(
                        "/subsystem=jgroups/stack=tcp/protocol=TCPPING:add(add-index=0, properties={initial_hosts=\"%1$s[7600],%1$s[9600]\",port_range=0})",
                        Utils.stripSquareBrackets(host)));
                commandCtx.handle("/subsystem=jgroups/stack=tcp/protocol=MPING:remove");
                commandCtx.handle("/subsystem=jgroups/channel=ee:write-attribute(name=stack,value=tcp)");
                commandCtx.handle("end-if");
            } catch (CommandLineException e) {
                LOGGER.error("Command line error occured during JGroups reconfiguration", e);
            } finally {
                LOGGER.debugf("Output of JGroups reconfiguration (switch to TCPPING): %s",
                        new String(consoleOut.toByteArray()));
            }
        }

        private void takeSnapshot() throws IOException, MgmtOperationException {
            DomainTestUtils.executeForResult(Util.createOperation("take-snapshot", null), client);
            readSnapshot();
        }

        private void readSnapshot() throws IOException, MgmtOperationException {
            ModelNode namesNode = DomainTestUtils.executeForResult(Util.createOperation("list-snapshots", null), client)
                    .get("names");
            if (namesNode == null || namesNode.getType() != ModelType.LIST) {
                throw new IllegalStateException("Unexpected return value from :list-snaphot operation: " + namesNode);
            }
            List<ModelNode> snapshots = namesNode.asList();
            if (!snapshots.isEmpty()) {
                snapshot = namesNode.get(snapshots.size() - 1).asString();
            }
        }

        private void reloadFromSnapshot() throws IOException {
            if (snapshot != null) {
                File snapshotFile = new File(jbossServerConfigDir + File.separator + "standalone_xml_history" + File.separator
                        + "snapshot" + File.separator + snapshot);
                String standaloneName = snapshot.replaceAll("\\d", "").replaceFirst("-", "");
                File standaloneFile = new File(jbossServerConfigDir + File.separator + standaloneName);
                Files.copy(snapshotFile.toPath(), standaloneFile.toPath(), REPLACE_EXISTING);
                snapshotFile.delete();
            }
        }

        private void reload() {
            ModelNode operation = Util.createOperation("reload", null);
            ServerReload.executeReloadAndWaitForCompletion(client, operation, (int) SECONDS.toMillis(90), host,
                    getManagementPort());
        }

        /**
         * Create single property file with users and/or roles in standalone server config directory. It will be used for
         * property-realm configuration (see {@code seccontext-setup.cli} script)
         */
        private void createPropertyFile(List<String> additionalUsers) throws IOException {
            String configDirPath = jbossServerConfigDir != null ? jbossServerConfigDir : readJbossServerConfigDir();
            List<String> users = new ArrayList<>();
            users.add("admin");
            users.add("servlet");
            users.add("entry");
            users.add("whoami");
            users.add("server");
            users.add("server-norunas");
            users.add("authz");
            users.addAll(additionalUsers);
            String[] usersArr = new String[users.size()];
            usersArr = users.toArray(usersArr);
            propertyFile = Paths.get(configDirPath, "seccontext.properties");
            Files.write(propertyFile, Utils.createUsersFromRoles(usersArr).getBytes(StandardCharsets.ISO_8859_1));
        }

        private String readJbossServerConfigDir() throws IOException {
            sendLine("/core-service=platform-mbean/type=runtime:read-attribute(name=system-properties)", false);
            assertTrue(consoleOut.size() > 0);
            ModelNode node = ModelNode.fromStream(new ByteArrayInputStream(consoleOut.toByteArray()));
            return node.get(ModelDescriptionConstants.RESULT).get("jboss.server.config.dir").asString();
        }

        private void addCliCommands(File file, List<String> commands) throws IOException {
            try (BufferedWriter bw = new BufferedWriter(new FileWriter(file, true))) {
                for (String command : commands) {
                    bw.append(command);
                }
            }

        }
    }

    protected static class ServerConfiguration {

        private final List<String> deployments;
        private final List<String> additionalUsers;
        private final List<String> cliCommands;

        private ServerConfiguration(ServerConfigurationBuilder builder) {
            this.deployments = builder.deployments;
            this.additionalUsers = builder.additionalUsers;
            this.cliCommands = builder.cliCommands;
        }

        public List<String> getDeployments() {
            return deployments;
        }

        public List<String> getAdditionalUsers() {
            return additionalUsers;
        }

        public List<String> getCliCommands() {
            return cliCommands;
        }

    }

    protected static class ServerConfigurationBuilder {

        private List<String> deployments = new ArrayList<>();
        private List<String> additionalUsers = new ArrayList<>();
        private List<String> cliCommands = new ArrayList<>();

        public ServerConfigurationBuilder withDeployments(String... deployments) {
            this.deployments.addAll(Arrays.asList(deployments));
            return this;
        }

        public ServerConfigurationBuilder withAdditionalUsers(String... additionalUsers) {
            this.additionalUsers.addAll(Arrays.asList(additionalUsers));
            return this;
        }

        public ServerConfigurationBuilder withCliCommands(String... cliCommands) {
            this.cliCommands.addAll(Arrays.asList(cliCommands));
            return this;
        }

        public ServerConfiguration build() {
            return new ServerConfiguration(this);
        }

    }
}
