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
package org.jboss.as.test.manualmode.web.ssl;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.COMPOSITE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.PROTOCOL;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SOCKET_BINDING;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SSL;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.STEPS;
import static org.junit.Assert.assertEquals;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Locale;

import javax.servlet.http.HttpServletResponse;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.SystemUtils;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.util.EntityUtils;
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
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.PORT;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SOCKET_BINDING_GROUP;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SUBSYSTEM;
import org.jboss.as.controller.operations.common.Util;
import org.jboss.as.test.categories.CommonCriteria;
import org.jboss.as.test.integration.security.common.AddRoleLoginModule;
import org.jboss.as.test.integration.security.common.SSLTruststoreUtil;
import org.jboss.as.test.integration.security.common.SecurityTestConstants;
import org.jboss.as.test.integration.security.common.SecurityTraceLoggingServerSetupTask;
import org.jboss.as.test.integration.security.common.Utils;
import org.jboss.as.test.integration.security.common.servlets.PrincipalPrintingServlet;
import org.jboss.as.test.integration.security.common.servlets.SimpleReverseProxyDisplayServlet;
import org.jboss.as.test.integration.security.common.servlets.SimpleSecureTransportServlet;
import org.jboss.as.test.integration.security.common.servlets.SimpleSecuredServlet;
import org.jboss.as.test.integration.security.common.servlets.SimpleServlet;
import org.jboss.as.test.shared.TestSuiteEnvironment;
import org.jboss.as.web.Constants;
import org.jboss.dmr.ModelNode;
import org.jboss.logging.Logger;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Assume;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;

/**
 * Testing https connection to Web Connector with configured two-way SSL. HTTP client has set client keystore with valid/invalid
 * certificate, which is used for authentication to management interface. Result of authentication depends on whether client
 * certificate is accepted in server truststore. HTTP client uses client truststore with accepted server certificate to
 * authenticate server identity.
 *
 * Keystores and truststores have valid certificates until 25 October 2033.
 *
 * @author Filip Bogyai
 * @author Josef cacek
 */
@RunWith(Arquillian.class)
@RunAsClient
@Category(CommonCriteria.class)
public class HTTPWebConnectorRedirectTestCase {

    private static final String STANDARD_SOCKETS = "standard-sockets";

    private static final String CONNECTOR = "connector";

    private static final String WEB = "web";

    private static final String HTTPS = "https";

    private static Logger LOGGER = Logger.getLogger(HTTPWebConnectorRedirectTestCase.class);

    private static SecurityTraceLoggingServerSetupTask TRACE_SECURITY = new SecurityTraceLoggingServerSetupTask();

    private static final File WORK_DIR = new File("https-workdir");
    public static final File SERVER_KEYSTORE_FILE = new File(WORK_DIR, SecurityTestConstants.SERVER_KEYSTORE);
    public static final File SERVER_TRUSTSTORE_FILE = new File(WORK_DIR, SecurityTestConstants.SERVER_TRUSTSTORE);
    public static final File CLIENT_KEYSTORE_FILE = new File(WORK_DIR, SecurityTestConstants.CLIENT_KEYSTORE);
    public static final File CLIENT_TRUSTSTORE_FILE = new File(WORK_DIR, SecurityTestConstants.CLIENT_TRUSTSTORE);
    public static final File UNTRUSTED_KEYSTORE_FILE = new File(WORK_DIR, SecurityTestConstants.UNTRUSTED_KEYSTORE);

    private static final String TEST_SSL_CONNECTOR = "ssl-test";
    private static final String TEST_REVERSE_PROXY_NAME = "reverse-proxy";

    private static final int HTTPS_PORT = 18443;

    protected static final int[] HTTPS_PORTS = {HTTPS_PORT};

    private static final String APP_CONTEXT = HTTPS;

    public static final String CONTAINER = "default-jbossas";

    @ArquillianResource
    private static ContainerController containerController;

    @ArquillianResource
    private Deployer deployer;

    @Deployment(name = APP_CONTEXT, testable = false, managed = false)
    public static WebArchive deployment() {
        LOGGER.info("Start deployment " + APP_CONTEXT);
        final WebArchive war = ShrinkWrap.create(WebArchive.class, APP_CONTEXT + ".war");
        war.addClasses(AddRoleLoginModule.class, SimpleServlet.class, SimpleSecuredServlet.class,
                SimpleReverseProxyDisplayServlet.class, SimpleSecureTransportServlet.class, PrincipalPrintingServlet.class);
        war.addAsWebInfResource(HTTPWebConnectorRedirectTestCase.class.getPackage(), "web_constraints.xml", "web.xml");
        return war;
    }

    @Test
    @InSequence(-1)
    public void startAndSetupContainer() throws Exception {

        LOGGER.info("*** starting server");
        containerController.start(CONTAINER);
        ModelControllerClient client = TestSuiteEnvironment.getModelControllerClient();
        ManagementClient managementClient = new ManagementClient(client, TestSuiteEnvironment.getServerAddress(),
                TestSuiteEnvironment.getServerPort());

        LOGGER.info("*** will configure server now");
        serverSetup(managementClient);

        LOGGER.info("*** restarting server");
        containerController.stop(CONTAINER);
        containerController.start(CONTAINER);
        deployer.deploy(APP_CONTEXT);
    }

    /**
     * @test.tsfi tsfi.port.https
     * @test.tsfi tsfi.keystore.file
     * @test.tsfi tsfi.truststore.file
     * @test.objective Testing default HTTPs connector with configured CLIENT-CERT authentication (BaseCertLoginModule is used).
     * Trusted client is allowed to access both secured/unsecured resource. Untrusted client can only access unprotected
     * resources.
     * @test.expectedResult Trusted client has access to protected and unprotected resources. Untrusted client has only access
     * to unprotected resources.
     * @throws Exception
     */
    @Test
    @InSequence(1)
    public void testDefaultConnector() throws Exception {
        Assume.assumeFalse(SystemUtils.IS_JAVA_1_6 && SystemUtils.JAVA_VENDOR.toUpperCase(Locale.ENGLISH).contains("IBM"));
        final URL reverseUrl = getServletUrl(8080, SimpleReverseProxyDisplayServlet.SERVLET_PATH);
        final URL securedTransportUrl = getSecureServletUrl(8443, SimpleSecureTransportServlet.SERVLET_PATH);
        final URL testSecuredTransportUrl = getSecureServletUrl(HTTPS_PORT, SimpleSecureTransportServlet.SERVLET_PATH);
        final URL testRedirectUrl = getServletUrl(8080, SimpleSecureTransportServlet.SERVLET_PATH);
        final HttpClient httpClient = getHttpClient(CLIENT_KEYSTORE_FILE);
        try {
            String responseBody = makeCall(reverseUrl, httpClient, HttpServletResponse.SC_OK);
            assertEquals("Secured page was not reached", "{\"server name\": \"" + TestSuiteEnvironment.getServerAddress() + "\", \"server port\" : 8080}", responseBody);
            responseBody = makeCall(securedTransportUrl, httpClient, HttpServletResponse.SC_OK);
            assertEquals("Secured page was not reached", SimpleServlet.RESPONSE_BODY, responseBody);
            responseBody = makeCall(testSecuredTransportUrl, httpClient, HttpServletResponse.SC_OK);
            assertEquals("Secured page was not reached", SimpleServlet.RESPONSE_BODY, responseBody);
            responseBody = makeCall(testRedirectUrl, httpClient, HttpServletResponse.SC_OK);
            assertEquals("Secured page was not reached", SimpleServlet.RESPONSE_BODY, responseBody);
        } finally {
            httpClient.getConnectionManager().shutdown();
        }
    }

    @Test
    @InSequence(3)
    public void stopContainer() throws Exception {
        deployer.undeploy(APP_CONTEXT);
        final ModelControllerClient client = TestSuiteEnvironment.getModelControllerClient();
        final ManagementClient managementClient = new ManagementClient(client, TestSuiteEnvironment.getServerAddress(),
                TestSuiteEnvironment.getServerPort());
        LOGGER.info("*** reseting test configuration");
        serverTearDown(managementClient);

        LOGGER.info("*** stopping container");
        containerController.stop(CONTAINER);
    }

    private URL getSecureServletUrl(int connectorPort, String servletPath) throws MalformedURLException {
        return new URL(HTTPS, TestSuiteEnvironment.getServerAddress(), connectorPort, "/" + APP_CONTEXT + servletPath);
    }

    private URL getServletUrl(int connectorPort, String servletPath) throws MalformedURLException {
        return new URL("http", TestSuiteEnvironment.getServerAddress(), connectorPort, "/" + APP_CONTEXT + servletPath);
    }

    /**
     * Requests given URL and checks if the returned HTTP status code is the expected one. Returns HTTP response body
     */
    private static String makeCall(URL url, HttpClient httpClient, int expectedStatusCode) throws ClientProtocolException,
            IOException, URISyntaxException {
        String httpResponseBody = null;
        HttpGet httpGet = new HttpGet(url.toURI());
        HttpResponse response = httpClient.execute(httpGet);
        int statusCode = response.getStatusLine().getStatusCode();
        LOGGER.info("Request to: " + url + " responds: " + statusCode);

        assertEquals("Unexpected status code", expectedStatusCode, statusCode);

        HttpEntity entity = response.getEntity();
        if (entity != null) {
            httpResponseBody = EntityUtils.toString(response.getEntity());
            EntityUtils.consume(entity);
        }

        return httpResponseBody;
    }

    private static HttpClient getHttpClient(File keystoreFile) {
        return SSLTruststoreUtil.getHttpClientWithSSL(keystoreFile, SecurityTestConstants.KEYSTORE_PASSWORD,
                CLIENT_TRUSTSTORE_FILE, SecurityTestConstants.KEYSTORE_PASSWORD);
    }

    private void serverSetup(ManagementClient managementClient) throws Exception {
        FileUtils.deleteDirectory(WORK_DIR);
        WORK_DIR.mkdirs();
        Utils.createKeyMaterial(WORK_DIR);

        TRACE_SECURITY.setup(managementClient, null);

        final ModelControllerClient client = managementClient.getControllerClient();

        final ModelNode compositeOp = Util.createOperation(COMPOSITE, PathAddress.EMPTY_ADDRESS);
        final ModelNode steps = compositeOp.get(STEPS);
        final PathAddress connectorAddress = PathAddress.pathAddress(SUBSYSTEM, WEB).append(CONNECTOR, HTTPS);
        ModelNode op = Util.createAddOperation(connectorAddress);
        op.get(SOCKET_BINDING).set(HTTPS);
        op.get(PROTOCOL).set("HTTP/1.1");
        op.get("scheme").set(HTTPS);
        op.get("secure").set(true);
        steps.add(op);

        op = Util.createAddOperation(connectorAddress.append(SSL, "configuration"));
        op.get("certificate-key-file").set(SERVER_KEYSTORE_FILE.getAbsolutePath());
        op.get("password").set(SecurityTestConstants.KEYSTORE_PASSWORD);
        op.get("ca-certificate-file").set(SERVER_TRUSTSTORE_FILE.getAbsolutePath());
        steps.add(op);
        Utils.applyUpdate(compositeOp, client);

        addHttpsConnector(TEST_SSL_CONNECTOR, HTTPS_PORT, client);

    }

    private void addHttpsConnector(String httpsName, int httpsPort, ModelControllerClient client)
            throws Exception {
        final ModelNode compositeOp = Util.createOperation(COMPOSITE, PathAddress.EMPTY_ADDRESS);
        final ModelNode steps = compositeOp.get(STEPS);

        // /socket-binding-group=standard-sockets/socket-binding=NAME:add(port=PORT)
        ModelNode op = Util.createAddOperation(PathAddress.pathAddress(SOCKET_BINDING_GROUP, STANDARD_SOCKETS).append(
                SOCKET_BINDING, httpsName));
        op.get(PORT).set(httpsPort);
        steps.add(op);

        // /socket-binding-group=standard-sockets/remote-destination-outbound-socket-binding=reverse-proxy:add(host=jboss.redhat.com,port=80) {allow-resource-service-restart=true}
//        op = Util.createAddOperation(PathAddress.pathAddress().append(SOCKET_BINDING_GROUP, STANDARD_SOCKETS)
//                .append(REMOTE_DESTINATION_OUTBOUND_SOCKET_BINDING, TEST_REVERSE_PROXY_NAME));
//        op.get(HOST).set("www.redhat.com");
//        op.get(PORT).set(80);
//        steps.add(op);

        final PathAddress connectorAddress = PathAddress.pathAddress(SUBSYSTEM, WEB).append(CONNECTOR, httpsName);
        op = Util.createAddOperation(connectorAddress);
        op.get(SOCKET_BINDING).set(httpsName);
        op.get(PROTOCOL).set("HTTP/1.1");
        op.get("scheme").set(HTTPS);
        op.get("secure").set(true);
        steps.add(op);

        op = Util.createAddOperation(connectorAddress.append(SSL, "configuration"));
        op.get("name").set(httpsName);
        op.get("certificate-key-file").set(SERVER_KEYSTORE_FILE.getAbsolutePath());
        op.get("password").set(SecurityTestConstants.KEYSTORE_PASSWORD);
        op.get("ca-certificate-file").set(SERVER_TRUSTSTORE_FILE.getAbsolutePath());
        steps.add(op);

        final PathAddress httpConnectorAddress = PathAddress.pathAddress(SUBSYSTEM, WEB).append(CONNECTOR, "http");
        op = Util.getWriteAttributeOperation(httpConnectorAddress.toModelNode(), Constants.REDIRECT_BINDING, httpsName);
        steps.add(op);

//        op = Util.getWriteAttributeOperation(httpConnectorAddress.toModelNode(), Constants.PROXY_BINDING, TEST_REVERSE_PROXY_NAME);
//        steps.add(op);

        Utils.applyUpdate(compositeOp, client);
    }

    private void serverTearDown(ManagementClient managementClient) throws Exception {

        final ModelControllerClient client = managementClient.getControllerClient();

        // delete https web connectors
        rmHttpsConnector(TEST_SSL_CONNECTOR, client);

        Utils.applyUpdate(Util.createRemoveOperation(PathAddress.pathAddress(SUBSYSTEM, WEB).append(CONNECTOR, HTTPS)), client);

        FileUtils.deleteDirectory(WORK_DIR);
        TRACE_SECURITY.tearDown(managementClient, null);
    }

    private void rmHttpsConnector(String httpsName, ModelControllerClient client) throws Exception {
        final ModelNode compositeOp = Util.createOperation(COMPOSITE, PathAddress.EMPTY_ADDRESS);
        final ModelNode steps = compositeOp.get(STEPS);

        steps.add(Util.createRemoveOperation(PathAddress.pathAddress(SUBSYSTEM, WEB).append(CONNECTOR, httpsName)));
        // Don't rollback when the AS detects the war needs the module
        steps.add(Util.createRemoveOperation(PathAddress.pathAddress(SOCKET_BINDING_GROUP, STANDARD_SOCKETS).append(
                SOCKET_BINDING, httpsName)));
        final PathAddress httpConnectorAddress = PathAddress.pathAddress(SUBSYSTEM, WEB).append(CONNECTOR, "http");
        steps.add(Util.getUndefineAttributeOperation(httpConnectorAddress, Constants.REDIRECT_BINDING));

        Utils.applyUpdate(compositeOp, client);
    }
}
