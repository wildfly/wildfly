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

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.NAME;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.PROTOCOL;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.READ_ATTRIBUTE_OPERATION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.RESULT;
import static org.jboss.as.test.integration.management.util.ModelUtil.createOpNode;
import static org.jboss.as.test.integration.security.common.SSLTruststoreUtil.HTTPS_PORT;
import static org.jboss.as.test.integration.security.common.Utils.makeCallWithHttpClient;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Locale;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;

import javax.net.ssl.SSLHandshakeException;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.SystemUtils;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.util.EntityUtils;
import org.jboss.as.arquillian.api.ServerSetupTask;
import org.jboss.as.arquillian.container.ManagementClient;
import org.jboss.as.controller.client.ModelControllerClient;
import org.jboss.as.controller.client.helpers.ClientConstants;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.test.integration.security.common.SSLTruststoreUtil;
import org.jboss.as.test.integration.security.common.SecurityTestConstants;
import org.jboss.as.test.integration.security.common.SecurityTraceLoggingServerSetupTask;
import org.jboss.as.test.integration.security.common.Utils;
import org.jboss.as.test.integration.security.common.servlets.PrincipalPrintingServlet;
import org.jboss.as.test.integration.security.common.servlets.SimpleSecuredServlet;
import org.jboss.as.test.integration.security.common.servlets.SimpleServlet;
import org.jboss.as.test.shared.TestSuiteEnvironment;
import org.jboss.dmr.ModelNode;
import org.jboss.logging.Logger;
import org.junit.Assert;
import org.junit.Assume;
import org.xnio.IoUtils;

/**
 * Abstract class which serve as a base for CertificateLoginModule tests. It is
 * used for setting up server/client keystores, https connector and contains
 * useful methods for testing two-way SSL connection.
 *
 * @author Filip Bogyai
 */
public abstract class AbstractCertificateLoginModuleTestCase {

    private static Logger LOGGER = Logger.getLogger(AbstractCertificateLoginModuleTestCase.class);
    protected static SecurityTraceLoggingServerSetupTask TRACE_SECURITY = new SecurityTraceLoggingServerSetupTask();

    protected static final File WORK_DIR = new File("keystores-workdir");
    protected static final File SERVER_KEYSTORE_FILE = new File(WORK_DIR, SecurityTestConstants.SERVER_KEYSTORE);
    protected static final File SERVER_TRUSTSTORE_FILE = new File(WORK_DIR, SecurityTestConstants.SERVER_TRUSTSTORE);
    protected static final File CLIENT_KEYSTORE_FILE = new File(WORK_DIR, SecurityTestConstants.CLIENT_KEYSTORE);
    protected static final File CLIENT_TRUSTSTORE_FILE = new File(WORK_DIR, SecurityTestConstants.CLIENT_TRUSTSTORE);
    protected static final File UNTRUSTED_KEYSTORE_FILE = new File(WORK_DIR, SecurityTestConstants.UNTRUSTED_KEYSTORE);

    protected static final String HTTPS_REALM = "https_realm";
    protected static final String CONTAINER = "default-jbossas";
    protected static final String SECURED_SERVLET_WITH_SESSION = SimpleSecuredServlet.SERVLET_PATH + "?"
            + SimpleSecuredServlet.CREATE_SESSION_PARAM + "=true";

    /**
     * Testing access to HTTPS connector which have configured truststore with
     * trusted certificates. Client with trusted certificate is allowed to
     * access both secured/unsecured resource. Client with untrusted certificate
     * can only access unprotected resources.
     *
     * @throws Exception
     */
    public void testLoginWithCertificate(String appName) throws Exception {

        Assume.assumeFalse(SystemUtils.IS_JAVA_1_6 && SystemUtils.JAVA_VENDOR.toUpperCase(Locale.ENGLISH).contains("IBM"));

        final URL printPrincipalUrl = getServletUrl(HTTPS_PORT, appName, PrincipalPrintingServlet.SERVLET_PATH);
        final URL securedUrl = getServletUrl(HTTPS_PORT, appName, SECURED_SERVLET_WITH_SESSION);
        final URL unsecuredUrl = getServletUrl(HTTPS_PORT, appName, SimpleServlet.SERVLET_PATH);

        final HttpClient httpClient = getHttpsClient(CLIENT_KEYSTORE_FILE);
        final HttpClient httpClientUntrusted = getHttpsClient(UNTRUSTED_KEYSTORE_FILE);

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

    public URL getServletUrl(int connectorPort, String appName, String servletPath) throws MalformedURLException {
        return new URL("https", TestSuiteEnvironment.getServerAddress(), connectorPort, "/" + appName + servletPath);
    }

    /**
     * Requests given URL and checks if the returned HTTP status code is the
     * expected one. Returns HTTP response body
     */
    public static String makeCall(URL url, HttpClient httpClient, int expectedStatusCode) throws ClientProtocolException, IOException,
            URISyntaxException {
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

    public static HttpClient getHttpsClient(File keystoreFile) {
        return SSLTruststoreUtil.getHttpClientWithSSL(keystoreFile, SecurityTestConstants.KEYSTORE_PASSWORD, CLIENT_TRUSTSTORE_FILE,
                SecurityTestConstants.KEYSTORE_PASSWORD);
    }

    public void reloadServer(ModelControllerClient client, int timeout) throws Exception {
        executeReload(client);
        waitForLiveServerToReload(timeout);
    }

    private void executeReload(ModelControllerClient client) throws IOException {
        ModelNode operation = new ModelNode();
        operation.get(OP_ADDR).setEmptyList();
        operation.get(OP).set("reload");
        try {
            ModelNode result = client.execute(operation);
            Assert.assertEquals("success", result.get(ClientConstants.OUTCOME).asString());
        } catch (IOException e) {
            final Throwable cause = e.getCause();
            if (!(cause instanceof ExecutionException) && !(cause instanceof CancellationException)) {
                throw e;
            } // else ignore, this might happen if the channel gets closed before we got the response
        }
    }

    private void waitForLiveServerToReload(int timeout) throws Exception {
        long start = System.currentTimeMillis();
        ModelNode operation = new ModelNode();
        operation.get(OP_ADDR).setEmptyList();
        operation.get(OP).set(READ_ATTRIBUTE_OPERATION);
        operation.get(NAME).set("server-state");
        while (System.currentTimeMillis() - start < timeout) {
            ModelControllerClient liveClient = ModelControllerClient.Factory.create(TestSuiteEnvironment.getServerAddress(),
                    TestSuiteEnvironment.getServerPort());
            try {
                ModelNode result = liveClient.execute(operation);
                if ("running".equals(result.get(RESULT).asString())) {
                    return;
                }
            } catch (IOException e) {
            } finally {
                IoUtils.safeClose(liveClient);
            }
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
            }
        }
        fail("Live Server did not reload in the imparted time.");
    }

    static class HTTPSConnectorSetup implements ServerSetupTask {

        protected static final HTTPSConnectorSetup INSTANCE = new HTTPSConnectorSetup();

        @Override
        public void setup(ManagementClient managementClient, String containerId) throws Exception {

            FileUtils.deleteDirectory(WORK_DIR);
            WORK_DIR.mkdirs();
            Utils.createKeyMaterial(WORK_DIR);

            TRACE_SECURITY.setup(managementClient, null);

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
            Utils.applyUpdate(operation, client);

            operation = createOpNode("subsystem=undertow/server=default-server/https-listener=https", ModelDescriptionConstants.ADD);
            operation.get("socket-binding").set("https");
            operation.get("security-realm").set(HTTPS_REALM);
            Utils.applyUpdate(operation, client);
        }

        @Override
        public void tearDown(ManagementClient managementClient, String containerId) throws Exception {

            ModelNode operation = createOpNode("subsystem=undertow/server=default-server/https-listener=https",
                    ModelDescriptionConstants.REMOVE);
            Utils.applyUpdate(operation, managementClient.getControllerClient());

            operation = createOpNode("core-service=management/security-realm=" + HTTPS_REALM, ModelDescriptionConstants.REMOVE);
            Utils.applyUpdate(operation, managementClient.getControllerClient());

            FileUtils.deleteDirectory(WORK_DIR);
            TRACE_SECURITY.tearDown(managementClient, null);

        }
    }

}
