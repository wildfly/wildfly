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
package org.jboss.as.test.manualmode.web.ssl;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.COMPOSITE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.PORT;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.PROTOCOL;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SOCKET_BINDING;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SOCKET_BINDING_GROUP;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.STEPS;
import static org.jboss.as.test.integration.management.util.ModelUtil.createOpNode;
import static org.jboss.as.test.integration.security.common.Utils.makeCallWithHttpClient;
import static org.jboss.as.test.integration.security.common.SSLTruststoreUtil.HTTPS_PORT_VERIFY_FALSE;
import static org.jboss.as.test.integration.security.common.SSLTruststoreUtil.HTTPS_PORT_VERIFY_TRUE;
import static org.jboss.as.test.integration.security.common.SSLTruststoreUtil.HTTPS_PORT_VERIFY_WANT;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.io.File;
import java.net.MalformedURLException;
import java.net.SocketException;
import java.net.URL;
import java.util.Locale;

import javax.net.ssl.SSLHandshakeException;
import javax.net.ssl.SSLPeerUnverifiedException;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.SystemUtils;
import org.apache.http.client.HttpClient;
import org.jboss.arquillian.container.test.api.ContainerController;
import org.jboss.arquillian.container.test.api.Deployer;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.junit.InSequence;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.as.arquillian.container.ManagementClient;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.client.ModelControllerClient;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.controller.operations.common.Util;
import org.jboss.as.test.categories.CommonCriteria;
import org.jboss.as.test.integration.security.common.AbstractSecurityDomainsServerSetupTask;
import org.jboss.as.test.integration.security.common.AddRoleLoginModule;
import org.jboss.as.test.integration.security.common.SSLTruststoreUtil;
import org.jboss.as.test.integration.security.common.SecurityTestConstants;
import org.jboss.as.test.integration.security.common.SecurityTraceLoggingServerSetupTask;
import org.jboss.as.test.integration.security.common.Utils;
import org.jboss.as.test.integration.security.common.config.JSSE;
import org.jboss.as.test.integration.security.common.config.SecureStore;
import org.jboss.as.test.integration.security.common.config.SecurityDomain;
import org.jboss.as.test.integration.security.common.config.SecurityModule;
import org.jboss.as.test.integration.security.common.servlets.PrincipalPrintingServlet;
import org.jboss.as.test.integration.security.common.servlets.SimpleSecuredServlet;
import org.jboss.as.test.integration.security.common.servlets.SimpleServlet;
import org.jboss.as.test.shared.TestSuiteEnvironment;
import org.jboss.dmr.ModelNode;
import org.jboss.logging.Logger;
import org.jboss.security.auth.spi.BaseCertLoginModule;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Assume;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;

/**
 * Testing https connection to Web Connector with configured two-way SSL.
 * HTTP client has set client keystore with valid/invalid certificate, which is used for
 * authentication to management interface. Result of authentication depends on whether client
 * certificate is accepted in server truststore. HTTP client uses client truststore with accepted
 * server certificate to authenticate server identity.
 *
 * Keystores and truststores have valid certificates until 25 Octover 2033.
 *
 * @author Filip Bogyai
 * @author Josef cacek
 */

@RunWith(Arquillian.class)
@RunAsClient
@Category(CommonCriteria.class)
public class HTTPSWebConnectorTestCase {

    private static final String STANDARD_SOCKETS = "standard-sockets";

    private static final String HTTPS = "https";

    public static final int HTTPS_PORT = 8444;

    private static Logger LOGGER = Logger.getLogger(HTTPSWebConnectorTestCase.class);

    private static SecurityTraceLoggingServerSetupTask TRACE_SECURITY = new SecurityTraceLoggingServerSetupTask();

    private static final File WORK_DIR = new File("https-workdir");
    public static final File SERVER_KEYSTORE_FILE = new File(WORK_DIR, SecurityTestConstants.SERVER_KEYSTORE);
    public static final File SERVER_TRUSTSTORE_FILE = new File(WORK_DIR, SecurityTestConstants.SERVER_TRUSTSTORE);
    public static final File CLIENT_KEYSTORE_FILE = new File(WORK_DIR, SecurityTestConstants.CLIENT_KEYSTORE);
    public static final File CLIENT_TRUSTSTORE_FILE = new File(WORK_DIR, SecurityTestConstants.CLIENT_TRUSTSTORE);
    public static final File UNTRUSTED_KEYSTORE_FILE = new File(WORK_DIR, SecurityTestConstants.UNTRUSTED_KEYSTORE);

    private static final String HTTPS_NAME_VERIFY_NOT_REQUESTED = "https-verify-not-requested";
    private static final String HTTPS_NAME_VERIFY_REQUESTED = "https-verify-requested";
    private static final String HTTPS_NAME_VERIFY_REQUIRED = "https-verify-required";

    private static final String HTTPS_REALM = "httpsRealm";
    private static final String APP_CONTEXT = HTTPS;
    private static final String SECURED_SERVLET_WITH_SESSION = SimpleSecuredServlet.SERVLET_PATH + "?"
            + SimpleSecuredServlet.CREATE_SESSION_PARAM + "=true";

    private static final String SECURITY_DOMAIN_CERT = "client cert domain";
    private static final String SECURITY_DOMAIN_JSSE = "jsse_!@#$%^&*()_domain";

    public static final String CONTAINER = "default-jbossas";

    @ArquillianResource
    private static ContainerController containerController;

    @ArquillianResource
    private Deployer deployer;

    @Deployment(name = APP_CONTEXT, testable = false, managed = false)
    public static WebArchive deployment() {
        LOGGER.trace("Start deployment " + APP_CONTEXT);
        final WebArchive war = ShrinkWrap.create(WebArchive.class, APP_CONTEXT + ".war");
        war.addClasses(AddRoleLoginModule.class, SimpleServlet.class, SimpleSecuredServlet.class,
                PrincipalPrintingServlet.class);
        war.addAsWebInfResource(HTTPSWebConnectorTestCase.class.getPackage(), "web.xml", "web.xml");
        war.addAsWebInfResource(HTTPSWebConnectorTestCase.class.getPackage(), "jboss-web.xml", "jboss-web.xml");
        return war;
    }

    @Test
    @InSequence(-1)
    public void startAndSetupContainer() throws Exception {

        LOGGER.trace("*** starting server");
        containerController.start(CONTAINER);
        ModelControllerClient client = TestSuiteEnvironment.getModelControllerClient();
        ManagementClient managementClient = new ManagementClient(client, TestSuiteEnvironment.getServerAddress(),
                TestSuiteEnvironment.getServerPort(), "remote+http");

        LOGGER.trace("*** will configure server now");
        serverSetup(managementClient);

        deployer.deploy(APP_CONTEXT);
    }

    /**
     * @test.tsfi tsfi.keystore.file
     * @test.tsfi tsfi.truststore.file
     * @test.objective Testing default HTTPs connector with verify-client attribute set to "false". The CLIENT-CERT
     *                 authentication (BaseCertLoginModule) is configured for this test. Trusted client is allowed to access
     *                 both secured/unsecured resource. Untrusted client can only access unprotected resources.
     * @test.expectedResult Trusted client has access to protected and unprotected resources. Untrusted client has only access
     *                      to unprotected resources.
     * @throws Exception
     */
    @Test
    @InSequence(1)
    public void testNonVerifyingConnector() throws Exception {

        Assume.assumeFalse(SystemUtils.IS_JAVA_1_6 && SystemUtils.JAVA_VENDOR.toUpperCase(Locale.ENGLISH).contains("IBM"));

        final URL printPrincipalUrl = getServletUrl(HTTPS_PORT_VERIFY_FALSE, PrincipalPrintingServlet.SERVLET_PATH);
        final URL securedUrl = getServletUrl(HTTPS_PORT_VERIFY_FALSE, SECURED_SERVLET_WITH_SESSION);
        final URL unsecuredUrl = getServletUrl(HTTPS_PORT_VERIFY_FALSE, SimpleServlet.SERVLET_PATH);

        final HttpClient httpClient = getHttpClient(CLIENT_KEYSTORE_FILE);
        final HttpClient httpClientUntrusted = getHttpClient(UNTRUSTED_KEYSTORE_FILE);

        try {
            makeCallWithHttpClient(printPrincipalUrl, httpClient, HttpServletResponse.SC_FORBIDDEN);

            String responseBody = makeCallWithHttpClient(securedUrl, httpClient, HttpServletResponse.SC_OK);
            assertEquals("Secured page was not reached", SimpleSecuredServlet.RESPONSE_BODY, responseBody);

            String principal = makeCallWithHttpClient(printPrincipalUrl, httpClient, HttpServletResponse.SC_OK);
            assertEquals("Unexpected principal", "cn=client", principal.toLowerCase());

            responseBody = makeCallWithHttpClient(unsecuredUrl, httpClientUntrusted, HttpServletResponse.SC_OK);
            assertEquals("Secured page was not reached", SimpleServlet.RESPONSE_BODY, responseBody);

            try {
                makeCallWithHttpClient(securedUrl, httpClientUntrusted, HttpServletResponse.SC_FORBIDDEN);
            } catch (SSLHandshakeException e) {
                // OK
            } catch (java.net.SocketException se) {
                // OK - on windows usually fails with this one
            }
        } finally {
            httpClient.getConnectionManager().shutdown();
            httpClientUntrusted.getConnectionManager().shutdown();
        }
    }

    /**
     * @test.tsfi tsfi.keystore.file
     * @test.tsfi tsfi.truststore.file
     * @test.objective Testing default HTTPs connector with verify-client attribute set to "want". The CLIENT-CERT
     *                 authentication (BaseCertLoginModule) is configured for this test. Trusted client is allowed to access
     *                 both secured/unsecured resource. Untrusted client can only access unprotected resources.
     * @test.expectedResult Trusted client has access to protected and unprotected resources. Untrusted client has only access
     *                      to unprotected resources.
     * @throws Exception
     */
    @Test
    @InSequence(1)
    public void testWantVerifyConnector() throws Exception {

        Assume.assumeFalse(SystemUtils.IS_JAVA_1_6 && SystemUtils.JAVA_VENDOR.toUpperCase(Locale.ENGLISH).contains("IBM"));

        final URL printPrincipalUrl = getServletUrl(HTTPS_PORT_VERIFY_WANT, PrincipalPrintingServlet.SERVLET_PATH);
        final URL securedUrl = getServletUrl(HTTPS_PORT_VERIFY_WANT, SECURED_SERVLET_WITH_SESSION);
        final URL unsecuredUrl = getServletUrl(HTTPS_PORT_VERIFY_WANT, SimpleServlet.SERVLET_PATH);

        final HttpClient httpClient = getHttpClient(CLIENT_KEYSTORE_FILE);
        final HttpClient httpClientUntrusted = getHttpClient(UNTRUSTED_KEYSTORE_FILE);

        try {
            makeCallWithHttpClient(printPrincipalUrl, httpClientUntrusted, HttpServletResponse.SC_FORBIDDEN);

            final String principal = makeCallWithHttpClient(printPrincipalUrl, httpClient, HttpServletResponse.SC_OK);
            assertEquals("Unexpected principal", "cn=client", principal.toLowerCase());

            String responseBody = makeCallWithHttpClient(unsecuredUrl, httpClient, HttpServletResponse.SC_OK);
            assertEquals("Unsecured page was not reached", SimpleSecuredServlet.RESPONSE_BODY, responseBody);
            responseBody = makeCallWithHttpClient(securedUrl, httpClient, HttpServletResponse.SC_OK);
            assertEquals("Secured page was not reached", SimpleSecuredServlet.RESPONSE_BODY, responseBody);

            responseBody = makeCallWithHttpClient(unsecuredUrl, httpClientUntrusted, HttpServletResponse.SC_OK);
            assertEquals("Unsecured page was not reached", SimpleServlet.RESPONSE_BODY, responseBody);
            makeCallWithHttpClient(securedUrl, httpClientUntrusted, HttpServletResponse.SC_FORBIDDEN);

        } finally {
            httpClient.getConnectionManager().shutdown();
            httpClientUntrusted.getConnectionManager().shutdown();
        }
    }

    /**
     * @test.tsfi tsfi.keystore.file
     * @test.tsfi tsfi.truststore.file
     * @test.objective Testing default HTTPs connector with verify-client attribute set to "true". The CLIENT-CERT
     *                 authentication (BaseCertLoginModule) is configured for this test. Trusted client is allowed to access
     *                 both secured/unsecured resource. Untrusted client is not allowed to access anything.
     * @test.expectedResult Trusted client has access to protected and unprotected resources. Untrusted client can't access
     *                      anything.
     * @throws Exception
     */
    @Test
    @InSequence(1)
    public void testVerifyingConnector() throws Exception {
        final HttpClient httpClient = getHttpClient(CLIENT_KEYSTORE_FILE);
        final HttpClient httpClientUntrusted = getHttpClient(UNTRUSTED_KEYSTORE_FILE);
        try {
            final URL printPrincipalUrl = getServletUrl(HTTPS_PORT_VERIFY_TRUE, PrincipalPrintingServlet.SERVLET_PATH);
            final URL securedUrl = getServletUrl(HTTPS_PORT_VERIFY_TRUE, SECURED_SERVLET_WITH_SESSION);
            final URL unsecuredUrl = getServletUrl(HTTPS_PORT_VERIFY_TRUE, SimpleServlet.SERVLET_PATH);

            String principal = makeCallWithHttpClient(printPrincipalUrl, httpClient, HttpServletResponse.SC_OK);
            assertEquals("Unexpected principal", "cn=client", principal.toLowerCase());

            String responseBody = makeCallWithHttpClient(securedUrl, httpClient, HttpServletResponse.SC_OK);
            assertEquals("Secured page was not reached", SimpleSecuredServlet.RESPONSE_BODY, responseBody);

            try {
                makeCallWithHttpClient(unsecuredUrl, httpClientUntrusted, HttpServletResponse.SC_FORBIDDEN);
                fail("Untrusted client should not be authenticated.");
            } catch (SSLHandshakeException |SSLPeerUnverifiedException | SocketException e) {
                //depending on the OS and the version of HTTP client in use any one of these exceptions may be thrown
                //in particular the SocketException gets thrown on Windows
                // OK
            }

            try {
                makeCallWithHttpClient(printPrincipalUrl, httpClientUntrusted, HttpServletResponse.SC_FORBIDDEN);
                fail("Untrusted client should not be authenticated.");
            } catch (SSLHandshakeException |SSLPeerUnverifiedException | SocketException e) {
                // OK
            }

            try {
                makeCallWithHttpClient(securedUrl, httpClientUntrusted, HttpServletResponse.SC_FORBIDDEN);
                fail("Untrusted client should not be authenticated.");
            } catch (SSLHandshakeException |SSLPeerUnverifiedException | SocketException e) {
                // OK
            }

        } finally {
            httpClient.getConnectionManager().shutdown();
            httpClientUntrusted.getConnectionManager().shutdown();
        }
    }

    @Test
    @InSequence(3)
    public void stopContainer() throws Exception {
        deployer.undeploy(APP_CONTEXT);
        final ModelControllerClient client = TestSuiteEnvironment.getModelControllerClient();
        final ManagementClient managementClient = new ManagementClient(client, TestSuiteEnvironment.getServerAddress(),
                TestSuiteEnvironment.getServerPort(), "remote+http");
        LOGGER.trace("*** reseting test configuration");
        serverTearDown(managementClient);

        LOGGER.trace("*** stopping container");
        containerController.stop(CONTAINER);
    }

    private URL getServletUrl(int connectorPort, String servletPath) throws MalformedURLException {
        return new URL(HTTPS, TestSuiteEnvironment.getServerAddress(), connectorPort, "/" + APP_CONTEXT + servletPath);
    }

    private static HttpClient getHttpClient(File keystoreFile) {
        return SSLTruststoreUtil.getHttpClientWithSSL(keystoreFile, SecurityTestConstants.KEYSTORE_PASSWORD,
                CLIENT_TRUSTSTORE_FILE, SecurityTestConstants.KEYSTORE_PASSWORD);
    }

    private void serverSetup(ManagementClient managementClient) throws Exception {
        FileUtils.deleteDirectory(WORK_DIR);
        WORK_DIR.mkdirs();
        Utils.createKeyMaterial(WORK_DIR);

        // Uncomment if TRACE logging is necessary. Don't leave it on all the time; CI resources aren't free.
        //TRACE_SECURITY.setup(managementClient, null);

        SecurityDomainsSetup.INSTANCE.setup(managementClient, null);

        final ModelControllerClient client = managementClient.getControllerClient();

        // add new HTTPS_REALM with SSL
        ModelNode operation = createOpNode("core-service=management/security-realm=" + HTTPS_REALM, ModelDescriptionConstants.ADD);
        Utils.applyUpdate(operation, client);

        operation = createOpNode("core-service=management/security-realm=" + HTTPS_REALM + "/authentication=truststore",
                ModelDescriptionConstants.ADD);
        operation.get("keystore-path").set(SERVER_TRUSTSTORE_FILE.getAbsolutePath());
        operation.get("keystore-password").set(SecurityTestConstants.KEYSTORE_PASSWORD);
        Utils.applyUpdate(operation, client);

        operation = createOpNode("core-service=management/security-realm=" + HTTPS_REALM + "/server-identity=ssl",
                ModelDescriptionConstants.ADD);
        operation.get(PROTOCOL).set("TLSv1");
        operation.get("keystore-path").set(SERVER_KEYSTORE_FILE.getAbsolutePath());
        operation.get("keystore-password").set(SecurityTestConstants.KEYSTORE_PASSWORD);
        // operation.get("alias").set("management");
        Utils.applyUpdate(operation, client);

        LOGGER.trace("*** restarting server");
        containerController.stop(CONTAINER);
        containerController.start(CONTAINER);

        addHttpsConnector("NOT_REQUESTED", HTTPS_NAME_VERIFY_NOT_REQUESTED, HTTPS_PORT_VERIFY_FALSE, client);
        addHttpsConnector("REQUESTED", HTTPS_NAME_VERIFY_REQUESTED, HTTPS_PORT_VERIFY_WANT, client);
        addHttpsConnector("REQUIRED", HTTPS_NAME_VERIFY_REQUIRED, HTTPS_PORT_VERIFY_TRUE, client);

    }

    private void addHttpsConnector(String verifyClient, String httpsName, int httpsPort, ModelControllerClient client)
            throws Exception {
        final ModelNode compositeOp = Util.createOperation(COMPOSITE, PathAddress.EMPTY_ADDRESS);
        final ModelNode steps = compositeOp.get(STEPS);

        // /socket-binding-group=standard-sockets/socket-binding=NAME:add(port=PORT)
        ModelNode op = Util.createAddOperation(PathAddress.pathAddress(SOCKET_BINDING_GROUP, STANDARD_SOCKETS).append(
                SOCKET_BINDING, httpsName));
        op.get(PORT).set(httpsPort);
        steps.add(op);

        ModelNode operation = createOpNode("subsystem=undertow/server=default-server/https-listener=" + httpsName,
                ModelDescriptionConstants.ADD);
        operation.get("socket-binding").set(httpsName);
        operation.get("security-realm").set(HTTPS_REALM);
        operation.get("verify-client").set(verifyClient);

        steps.add(operation);

        Utils.applyUpdate(compositeOp, client);
    }

    private void serverTearDown(ManagementClient managementClient) throws Exception {

        // delete test security domains
        SecurityDomainsSetup.INSTANCE.tearDown(managementClient, null);

        final ModelControllerClient client = managementClient.getControllerClient();

        // delete https web connectors
        rmHttpsConnector(HTTPS_NAME_VERIFY_NOT_REQUESTED, client);
        rmHttpsConnector(HTTPS_NAME_VERIFY_REQUESTED, client);
        rmHttpsConnector(HTTPS_NAME_VERIFY_REQUIRED, client);

        ModelNode operation = createOpNode("core-service=management/security-realm=" + HTTPS_REALM, ModelDescriptionConstants.REMOVE);
        Utils.applyUpdate(operation, client);

        FileUtils.deleteDirectory(WORK_DIR);
        // Uncomment if TRACE logging is necessary. Don't leave it on all the time; CI resources aren't free.
        //TRACE_SECURITY.tearDown(managementClient, null);
    }

    private void rmHttpsConnector(String httpsName, ModelControllerClient client) throws Exception {
        final ModelNode compositeOp = Util.createOperation(COMPOSITE, PathAddress.EMPTY_ADDRESS);
        final ModelNode steps = compositeOp.get(STEPS);

        ModelNode operation = createOpNode("subsystem=undertow/server=default-server/https-listener=" + httpsName,
                ModelDescriptionConstants.REMOVE);
        steps.add(operation);

        steps.add(Util.createRemoveOperation(PathAddress.pathAddress(SOCKET_BINDING_GROUP, STANDARD_SOCKETS).append(
                SOCKET_BINDING, httpsName)));

        Utils.applyUpdate(compositeOp, client);
    }

    /**
     * Security domains configuration for this test.
     */
    private static class SecurityDomainsSetup extends AbstractSecurityDomainsServerSetupTask {

        private static final SecurityDomainsSetup INSTANCE = new SecurityDomainsSetup();

        @Override
        protected SecurityDomain[] getSecurityDomains() throws Exception {
            final SecurityDomain sd = new SecurityDomain.Builder()
                    .name(SECURITY_DOMAIN_CERT)
                    .loginModules(
                            new SecurityModule.Builder().name(BaseCertLoginModule.class.getName())
                                    .putOption("securityDomain", SECURITY_DOMAIN_JSSE)
                                    .putOption("password-stacking", "useFirstPass").build(),
                            new SecurityModule.Builder().name(AddRoleLoginModule.class.getName()).flag("optional")
                                    .putOption("password-stacking", "useFirstPass")
                                    .putOption("roleName", SimpleSecuredServlet.ALLOWED_ROLE).build()) //
                    .build();
            final SecurityDomain sdJsse = new SecurityDomain.Builder()
                    .name(SECURITY_DOMAIN_JSSE)
                    .jsse(new JSSE.Builder().trustStore(
                            new SecureStore.Builder().type("JKS").url(SERVER_TRUSTSTORE_FILE.toURI().toURL())
                                    .password(SecurityTestConstants.KEYSTORE_PASSWORD).build()) //
                            .build()) //
                    .build();

            return new SecurityDomain[] { sdJsse, sd };
        }
    }
}
