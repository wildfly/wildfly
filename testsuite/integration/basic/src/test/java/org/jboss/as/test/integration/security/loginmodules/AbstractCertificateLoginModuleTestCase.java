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
package org.jboss.as.test.integration.security.loginmodules;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ALLOW_RESOURCE_SERVICE_RESTART;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.COMPOSITE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OPERATION_HEADERS;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.PROTOCOL;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SOCKET_BINDING;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SSL;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.STEPS;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SUBSYSTEM;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Locale;

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
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.operations.common.Util;
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
import org.junit.Assume;

/**
 * Abstract class which serve as a base for CertificateLoginModule tests. It us
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

        final URL printPrincipalUrl = getServletUrl(8443, appName, PrincipalPrintingServlet.SERVLET_PATH);
        final URL securedUrl = getServletUrl(8443, appName, SECURED_SERVLET_WITH_SESSION);
        final URL unsecuredUrl = getServletUrl(8443, appName, SimpleServlet.SERVLET_PATH);

        final HttpClient httpClient = getHttpsClient(CLIENT_KEYSTORE_FILE);
        final HttpClient httpClientUntrusted = getHttpsClient(UNTRUSTED_KEYSTORE_FILE);

        try {
            String responseBody = makeCall(securedUrl, httpClient, HttpServletResponse.SC_OK);
            assertEquals("Secured page was not reached", SimpleSecuredServlet.RESPONSE_BODY, responseBody);
            makeCall(securedUrl, httpClient, HttpServletResponse.SC_OK);

            String principal = makeCall(printPrincipalUrl, httpClient, HttpServletResponse.SC_OK);
            assertEquals("Unexpected principal", "cn=client", principal.toLowerCase());

            responseBody = makeCall(unsecuredUrl, httpClientUntrusted, HttpServletResponse.SC_OK);
            assertEquals("Secured page was not reached", SimpleServlet.RESPONSE_BODY, responseBody);

            try {
                makeCall(securedUrl, httpClientUntrusted, HttpServletResponse.SC_OK);
                fail("Untrusted client must not be able to access protected resource");
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

    static class HTTPSConnectorSetup implements ServerSetupTask {

        @Override
        public void setup(ManagementClient managementClient, String containerId) throws Exception {

            FileUtils.deleteDirectory(WORK_DIR);
            WORK_DIR.mkdirs();
            Utils.createKeyMaterial(WORK_DIR);

            TRACE_SECURITY.setup(managementClient, null);

            final ModelNode compositeOp = Util.createOperation(COMPOSITE, PathAddress.EMPTY_ADDRESS);
            final ModelNode steps = compositeOp.get(STEPS);
            final PathAddress connectorAddress = PathAddress.pathAddress(SUBSYSTEM, "web").append("connector", "https");
            ModelNode op = Util.createAddOperation(connectorAddress);
            op.get(SOCKET_BINDING).set("https");
            op.get(PROTOCOL).set("HTTP/1.1");
            op.get("scheme").set("https");
            op.get("secure").set(true);
            steps.add(op);

            op = Util.createAddOperation(connectorAddress.append(SSL, "configuration"));
            op.get("certificate-key-file").set(SERVER_KEYSTORE_FILE.getAbsolutePath());
            op.get("password").set(SecurityTestConstants.KEYSTORE_PASSWORD);
            op.get("ca-certificate-file").set(SERVER_TRUSTSTORE_FILE.getAbsolutePath());
            steps.add(op);
            Utils.applyUpdate(compositeOp, managementClient.getControllerClient());

        }

        @Override
        public void tearDown(ManagementClient managementClient, String containerId) throws Exception {

            ModelNode removeOperation = Util.createRemoveOperation(PathAddress.pathAddress(SUBSYSTEM, "web").append("connector", "https"));
            removeOperation.get(OPERATION_HEADERS, ALLOW_RESOURCE_SERVICE_RESTART).set(true);
            Utils.applyUpdate(removeOperation,
                    managementClient.getControllerClient());

            FileUtils.deleteDirectory(WORK_DIR);
            TRACE_SECURITY.tearDown(managementClient, null);

        }
    }

}
