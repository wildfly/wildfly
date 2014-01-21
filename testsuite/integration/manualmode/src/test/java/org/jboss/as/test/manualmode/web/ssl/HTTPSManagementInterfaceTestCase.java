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

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.NAME;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.VALUE;
import static org.jboss.as.test.integration.management.util.ModelUtil.createOpNode;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;

import org.apache.commons.io.FileUtils;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.util.EntityUtils;
import org.jboss.arquillian.container.test.api.ContainerController;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.junit.InSequence;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.as.arquillian.container.ManagementClient;
import org.jboss.as.controller.client.ModelControllerClient;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.test.categories.CommonCriteria;
import org.jboss.as.test.integration.security.common.AbstractSecurityRealmsServerSetupTask;
import org.jboss.as.test.integration.security.common.SecurityTestConstants;
import org.jboss.as.test.integration.security.common.Utils;
import org.jboss.as.test.integration.security.common.config.realm.Authentication;
import org.jboss.as.test.integration.security.common.config.realm.RealmKeystore;
import org.jboss.as.test.integration.security.common.config.realm.SecurityRealm;
import org.jboss.as.test.integration.security.common.config.realm.ServerIdentity;
import org.jboss.as.test.shared.TestSuiteEnvironment;
import org.jboss.dmr.ModelNode;
import org.jboss.logging.Logger;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;

/**
 * Testing https connection to HTTP Management interface with configured two-way SSL. 
 * HTTP client has set client keystore with valid/invalid certificate, which is used for
 * authentication to management interface. Result of authentication depends on whether client
 * certificate is accepted in server truststore. HTTP client uses client truststore with accepted
 * server certificate to authenticate server identity.
 * 
 * Keystores and truststores have valid certificates until 25 October 2033.
 * 
 * @author Filip Bogyai
 * @author Josef Cacek
 */

@RunWith(Arquillian.class)
@RunAsClient
@Category(CommonCriteria.class)
public class HTTPSManagementInterfaceTestCase {

    private static Logger LOGGER = Logger.getLogger(HTTPSManagementInterfaceTestCase.class);

    private static final File WORK_DIR = new File("mgmt-if-workdir");
    public static final File SERVER_KEYSTORE_FILE = new File(WORK_DIR, SecurityTestConstants.SERVER_KEYSTORE);
    public static final File SERVER_TRUSTSTORE_FILE = new File(WORK_DIR, SecurityTestConstants.SERVER_TRUSTSTORE);
    public static final File CLIENT_KEYSTORE_FILE = new File(WORK_DIR, SecurityTestConstants.CLIENT_KEYSTORE);
    public static final File CLIENT_TRUSTSTORE_FILE = new File(WORK_DIR, SecurityTestConstants.CLIENT_TRUSTSTORE);
    public static final File UNTRUSTED_KEYSTORE_FILE = new File(WORK_DIR, SecurityTestConstants.UNTRUSTED_KEYSTORE);
    public static final File MGMT_USERS_FILE = new File(WORK_DIR, "mgmt-users.properties");

    private static final ManagementWebRealmSetup managementWebRealmSetup = new ManagementWebRealmSetup();
    private static final String MANAGEMENT_WEB_REALM = "ManagementWebRealm";
    public static final int MGMT_PORT = 9990;
    public static final int MGMT_SECURED_PORT = 9443;
    private static final String MGMT_CTX = "/management";
    private static final String ADMIN = "admin";
    private static final String ADMIN_PASS = "secret_1";

    public static final String CONTAINER = "default-jbossas";

    @ArquillianResource
    private static ContainerController containerController;

    @Test
    @InSequence(-1)
    public void startAndSetupContainer() throws Exception {

        LOGGER.info("*** starting server");
        containerController.start(CONTAINER);
        ModelControllerClient client = TestSuiteEnvironment.getModelControllerClient();
        ManagementClient managementClient = new ManagementClient(client, TestSuiteEnvironment.getServerAddress(),
                TestSuiteEnvironment.getServerPort());

        LOGGER.info("*** will configure server now");
        createKeyMaterial();
        managementWebRealmSetup.setup(managementClient, CONTAINER);
        serverSetup(managementClient);

        // To apply new security realm settings for http interface reload of
        // server is required
        LOGGER.info("*** restarting server");
        containerController.stop(CONTAINER);
        containerController.start(CONTAINER);
    }

    /**
     * @test.tsfi tsfi.port.management.http
     * @test.tsfi tsfi.app.web.admin.console
     * @test.tsfi tsfi.keystore.file
     * @test.tsfi tsfi.truststore.file
     * @test.objective Testing authentication over management-http port. Test with user "admin" who has right password and right
     *                 role to login into management web interface. Also provides check for web administration console
     *                 authentication, which goes through /management context.
     * 
     * @test.expectedResult Management web console page is successfully reached, and test finishes without exception.
     * 
     * @throws ClientProtocolException, IOException, URISyntaxException
     */
    @Test
    @InSequence(1)
    public void testHTTP() throws ClientProtocolException, IOException, URISyntaxException {

        final ModelControllerClient client = TestSuiteEnvironment.getModelControllerClient();
        final ManagementClient managementClient = new ManagementClient(client, TestSuiteEnvironment.getServerAddress(),
                TestSuiteEnvironment.getServerPort());

        DefaultHttpClient httpClient = new DefaultHttpClient();
        URL mgmtURL = new URL("http", managementClient.getMgmtAddress(), MGMT_PORT, MGMT_CTX);

        String responseBody = makeCall(mgmtURL, httpClient, 401);
        assertFalse("Management index page was reached", responseBody.contains("management-major-version"));

        final UsernamePasswordCredentials credentials = new UsernamePasswordCredentials(ADMIN, ADMIN_PASS);
        httpClient.getCredentialsProvider().setCredentials(new AuthScope(AuthScope.ANY), credentials);

        responseBody = makeCall(mgmtURL, httpClient, 200);
        assertTrue("Management index page was not reached", responseBody.contains("management-major-version"));
    }

    /**
     * @test.tsfi tsfi.port.management.https
     * @test.tsfi tsfi.app.web.admin.console
     * @test.tsfi tsfi.keystore.file
     * @test.tsfi tsfi.truststore.file
     * @test.objective Testing authentication over management-https port. Test with user who has right/wrong certificate 
     *                 to login into management web interface. Also provides check for web administration console
     *                 authentication, which goes through /management context.
     * 
     * @test.expectedResult Management web console page is successfully reached, and test finishes without exception.
     * 
     * @throws ClientProtocolException, IOException, URISyntaxException
     */
    @Test
    @InSequence(2)
    public void testHTTPS() throws Exception {

        final ModelControllerClient client = TestSuiteEnvironment.getModelControllerClient();
        final ManagementClient managementClient = new ManagementClient(client, TestSuiteEnvironment.getServerAddress(),
                TestSuiteEnvironment.getServerPort());

        final HttpClient httpClient = getHttpClient(CLIENT_KEYSTORE_FILE);
        final HttpClient httpClientUntrusted = getHttpClient(UNTRUSTED_KEYSTORE_FILE);

        URL mgmtURL = new URL("https", managementClient.getMgmtAddress(), MGMT_SECURED_PORT, MGMT_CTX);

        String responseBody = makeCall(mgmtURL, httpClientUntrusted, 401);
        assertFalse("Management index page was reached", responseBody.contains("management-major-version"));

        responseBody = makeCall(mgmtURL, httpClient, 200);
        assertTrue("Management index page was not reached", responseBody.contains("management-major-version"));

    }

    @Test
    @InSequence(3)
    public void stopContainer() throws Exception {

        final ModelControllerClient client = TestSuiteEnvironment.getModelControllerClient();
        final ManagementClient managementClient = new ManagementClient(client, TestSuiteEnvironment.getServerAddress(),
                TestSuiteEnvironment.getServerPort());
        LOGGER.info("*** reseting test configuration");
        serverTearDown(managementClient);
        managementWebRealmSetup.tearDown(managementClient, CONTAINER);

        LOGGER.info("*** stopping container");
        containerController.stop(CONTAINER);
    }

    /**
     * Requests given URL and checks if the returned HTTP status code is the expected one. Returns HTTP response body
     * 
     * @param URL url to which the request should be made
     * @param DefaultHttpClient httpClient to test multiple access
     * @param expectedStatusCode expected status code returned from the requested server
     * @return HTTP response body
     * @throws ClientProtocolException
     * @throws IOException
     * @throws URISyntaxException
     */
    public static String makeCall(URL url, HttpClient httpClient, int expectedStatusCode)
            throws ClientProtocolException, IOException, URISyntaxException {

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
        return SSLTruststoreUtil.getHttpClientWithSSL(keystoreFile, SecurityTestConstants.KEYSTORE_PASSWORD, CLIENT_TRUSTSTORE_FILE,
                SecurityTestConstants.KEYSTORE_PASSWORD);
    }

    static class ManagementWebRealmSetup extends AbstractSecurityRealmsServerSetupTask {

        @Override
        protected SecurityRealm[] getSecurityRealms() throws Exception {
            final ServerIdentity serverIdentity = new ServerIdentity.Builder().ssl(
                    new RealmKeystore.Builder().keystorePassword(SecurityTestConstants.KEYSTORE_PASSWORD)
                            .keystorePath(SERVER_KEYSTORE_FILE.getAbsolutePath()).build()).build();
            final Authentication authentication = new Authentication.Builder().truststore(
                    new RealmKeystore.Builder().keystorePassword(SecurityTestConstants.KEYSTORE_PASSWORD)
                            .keystorePath(SERVER_TRUSTSTORE_FILE.getAbsolutePath()).build()).build();
            final SecurityRealm realm = new SecurityRealm.Builder().name(MANAGEMENT_WEB_REALM).serverIdentity(serverIdentity)
                    .authentication(authentication).build();
            return new SecurityRealm[] { realm };
        }
    }

    private void createKeyMaterial() throws IOException {

        FileUtils.deleteDirectory(WORK_DIR);
        WORK_DIR.mkdirs();
        Utils.createKeyMaterial(WORK_DIR);
        FileUtils.write(MGMT_USERS_FILE, ADMIN + "=" + ADMIN_PASS);
    }

    private void serverSetup(ManagementClient managementClient) throws Exception {

        final ModelControllerClient client = managementClient.getControllerClient();

        ModelNode operation = createOpNode("core-service=management/security-realm=" + MANAGEMENT_WEB_REALM + "/authentication=properties",
                ModelDescriptionConstants.ADD);
        operation.get("path").set(MGMT_USERS_FILE.getAbsolutePath());
        operation.get("plain-text").set(true);
        Utils.applyUpdate(operation, client);

        // change security-realm for native management interface
        operation = createOpNode("core-service=management/management-interface=http-interface",
                ModelDescriptionConstants.WRITE_ATTRIBUTE_OPERATION);
        operation.get(NAME).set("security-realm");
        operation.get(VALUE).set(MANAGEMENT_WEB_REALM);
        Utils.applyUpdate(operation, client);

        // add https connector to management interface
        operation = createOpNode("core-service=management/management-interface=http-interface",
                ModelDescriptionConstants.WRITE_ATTRIBUTE_OPERATION);
        operation.get(NAME).set("secure-socket-binding");
        operation.get(VALUE).set("management-https");
        Utils.applyUpdate(operation, client);

    }

    private void serverTearDown(ManagementClient managementClient) throws Exception {

        ModelNode operation = createOpNode("core-service=management/management-interface=http-interface",
                ModelDescriptionConstants.UNDEFINE_ATTRIBUTE_OPERATION);
        operation.get(NAME).set("secure-socket-binding");
        managementClient.getControllerClient().execute(operation);

        operation = createOpNode("core-service=management/management-interface=http-interface",
                ModelDescriptionConstants.WRITE_ATTRIBUTE_OPERATION);
        operation.get(NAME).set("security-realm");
        operation.get(VALUE).set("ManagementRealm");
        managementClient.getControllerClient().execute(operation);

        FileUtils.deleteDirectory(WORK_DIR);
    }

}
