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
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.STEPS;
import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Locale;

import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang.SystemUtils;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
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
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.HOST;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.PORT;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.PROTOCOL;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.REMOTE_DESTINATION_OUTBOUND_SOCKET_BINDING;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SOCKET_BINDING;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SOCKET_BINDING_GROUP;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SUBSYSTEM;
import org.jboss.as.controller.operations.common.Util;
import org.jboss.as.test.categories.CommonCriteria;
import org.jboss.as.test.integration.security.common.AddRoleLoginModule;
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
public class HTTPWebConnectorProxyConfTestCase {

    private static final String STANDARD_SOCKETS = "standard-sockets";

    private static final String CONNECTOR = "connector";

    private static final String WEB = "web";

    private static Logger LOGGER = Logger.getLogger(HTTPWebConnectorProxyConfTestCase.class);

    private static final String TEST_REVERSE_PROXY_NAME = "reverse-proxy";

    private static final String APP_CONTEXT = "https";

    public static final String CONTAINER = "default-jbossas";

    private static final String PROXY_NAME = "www.redhat.com";

    private static final String TEST_CONNECTOR_NAME = "manual-http-test";
    private static final int TEST_CONNECTOR_PORT = 8090;

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
        war.addAsWebInfResource(HTTPWebConnectorProxyConfTestCase.class.getPackage(), "web_constraints.xml", "web.xml");
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
        final URL reverseUrl = getServletUrl(TEST_CONNECTOR_PORT, SimpleReverseProxyDisplayServlet.SERVLET_PATH);
        final HttpClient httpClient = getHttpClient();
        try {
            String responseBody = makeCall(reverseUrl, httpClient, HttpServletResponse.SC_OK);
            assertEquals("Secured page was not reached", "{\"server name\": \"" + PROXY_NAME + "\", \"server port\" : 80}", responseBody);
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

    private static HttpClient getHttpClient() {
        return new DefaultHttpClient();
    }

    private void serverSetup(ManagementClient managementClient) throws Exception {
        final ModelControllerClient client = managementClient.getControllerClient();
        addHttpConnector(TEST_CONNECTOR_NAME, TEST_CONNECTOR_PORT, client);

    }

    private void addHttpConnector(String connectorName, int httpPort, ModelControllerClient client)
            throws Exception {
        final ModelNode compositeOp = Util.createOperation(COMPOSITE, PathAddress.EMPTY_ADDRESS);
        final ModelNode steps = compositeOp.get(STEPS);

        // /socket-binding-group=standard-sockets/socket-binding=NAME:add(port=PORT)
        ModelNode op = Util.createAddOperation(PathAddress.pathAddress(SOCKET_BINDING_GROUP, STANDARD_SOCKETS).append(
                SOCKET_BINDING, connectorName));
        op.get(PORT).set(httpPort);
        steps.add(op);

        // /socket-binding-group=standard-sockets/remote-destination-outbound-socket-binding=reverse-proxy:add(host=www.redhat.com,port=80) {allow-resource-service-restart=true}
        op = Util.createAddOperation(PathAddress.pathAddress().append(SOCKET_BINDING_GROUP, STANDARD_SOCKETS)
                .append(REMOTE_DESTINATION_OUTBOUND_SOCKET_BINDING, TEST_REVERSE_PROXY_NAME));
        op.get(HOST).set(PROXY_NAME);
        op.get(PORT).set(80);
        steps.add(op);

        final PathAddress connectorAddress = PathAddress.pathAddress(SUBSYSTEM, WEB).append(CONNECTOR, connectorName);
        op = Util.createAddOperation(connectorAddress);
        op.get(SOCKET_BINDING).set(connectorName);
        op.get(PROTOCOL).set("HTTP/1.1");
        op.get("scheme").set("http");
        op.get(Constants.PROXY_BINDING).set(TEST_REVERSE_PROXY_NAME);
        steps.add(op);

        Utils.applyUpdate(compositeOp, client);
    }

    private void serverTearDown(ManagementClient managementClient) throws Exception {

        final ModelControllerClient client = managementClient.getControllerClient();
        final PathAddress httpConnectorAddress = PathAddress.pathAddress(SUBSYSTEM, WEB).append(CONNECTOR, TEST_CONNECTOR_NAME);
        Utils.applyUpdate(Util.createRemoveOperation(httpConnectorAddress), client);
        Utils.applyUpdate(Util.createRemoveOperation(PathAddress.pathAddress(SOCKET_BINDING_GROUP, STANDARD_SOCKETS).append(
                REMOTE_DESTINATION_OUTBOUND_SOCKET_BINDING, TEST_REVERSE_PROXY_NAME)), client);
    }
}
