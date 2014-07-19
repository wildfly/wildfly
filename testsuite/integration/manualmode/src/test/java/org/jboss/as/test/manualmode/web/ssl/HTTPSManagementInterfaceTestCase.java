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

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.not;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.NAME;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.VALUE;
import static org.jboss.as.test.integration.management.util.CustomCLIExecutor.MANAGEMENT_HTTPS_PORT;
import static org.jboss.as.test.integration.management.util.CustomCLIExecutor.MANAGEMENT_HTTP_PORT;
import static org.jboss.as.test.integration.management.util.CustomCLIExecutor.MANAGEMENT_NATIVE_PORT;
import static org.jboss.as.test.integration.management.util.CustomCLIExecutor.NATIVE_CONTROLLER;
import static org.jboss.as.test.integration.management.util.CustomCLIExecutor.waitForServerToReload;
import static org.jboss.as.test.integration.management.util.ModelUtil.createOpNode;
import static org.jboss.as.test.integration.security.common.Utils.makeCallWithHttpClient;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.File;
import java.io.IOException;
import java.net.InetAddress;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.UnknownHostException;

import javax.net.ssl.SSLHandshakeException;
import javax.net.ssl.SSLPeerUnverifiedException;

import org.apache.commons.io.FileUtils;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.impl.client.DefaultHttpClient;
import org.jboss.arquillian.container.test.api.ContainerController;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.junit.InSequence;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.as.arquillian.container.ManagementClient;
import org.jboss.as.controller.client.ModelControllerClient;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.test.categories.CommonCriteria;
import org.jboss.as.test.integration.management.util.CustomCLIExecutor;
import org.jboss.as.test.integration.security.common.AbstractSecurityRealmsServerSetupTask;
import org.jboss.as.test.integration.security.common.SSLTruststoreUtil;
import org.jboss.as.test.integration.security.common.SecurityTestConstants;
import org.jboss.as.test.integration.security.common.Utils;
import org.jboss.as.test.integration.security.common.config.realm.Authentication;
import org.jboss.as.test.integration.security.common.config.realm.RealmKeystore;
import org.jboss.as.test.integration.security.common.config.realm.SecurityRealm;
import org.jboss.as.test.integration.security.common.config.realm.ServerIdentity;
import org.jboss.as.test.shared.TestSuiteEnvironment;
import org.jboss.as.test.shared.integration.ejb.security.CallbackHandler;
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
 * Keystores and truststores have valid certificates until 25 Octover 2033.
 *
 * @author Filip Bogyai
 * @author Josef Cacek
 */

@RunWith(Arquillian.class)
@RunAsClient
@Category(CommonCriteria.class)
public class HTTPSManagementInterfaceTestCase {

    public static Logger LOGGER = Logger.getLogger(HTTPSManagementInterfaceTestCase.class);

    private static final File WORK_DIR = new File("mgmt-if-workdir");
    public static final File SERVER_KEYSTORE_FILE = new File(WORK_DIR, SecurityTestConstants.SERVER_KEYSTORE);
    public static final File SERVER_TRUSTSTORE_FILE = new File(WORK_DIR, SecurityTestConstants.SERVER_TRUSTSTORE);
    public static final File CLIENT_KEYSTORE_FILE = new File(WORK_DIR, SecurityTestConstants.CLIENT_KEYSTORE);
    public static final File CLIENT_TRUSTSTORE_FILE = new File(WORK_DIR, SecurityTestConstants.CLIENT_TRUSTSTORE);
    public static final File UNTRUSTED_KEYSTORE_FILE = new File(WORK_DIR, SecurityTestConstants.UNTRUSTED_KEYSTORE);

    private static final String RELOAD = "reload";
    private static final int MAX_RELOAD_TIME = 20000;

    private static final ManagementWebRealmSetup managementNativeRealmSetup = new ManagementWebRealmSetup();
    private static final String MANAGEMENT_WEB_REALM = "ManagementWebRealm";
    private static final String MGMT_CTX = "/management";
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
                TestSuiteEnvironment.getServerPort(), "http-remoting");

        LOGGER.info("*** will configure server now");
        serverSetup(managementClient);
        managementNativeRealmSetup.setup(managementClient, CONTAINER);
        managementClient.close();

        LOGGER.info("*** reloading server");
        // To apply new security realm settings for http interface reload of
        // server is required
        CustomCLIExecutor.execute(null, RELOAD, NATIVE_CONTROLLER);
        waitForServerToReload(MAX_RELOAD_TIME, NATIVE_CONTROLLER);

    }

    /**
     * @test.tsfi tsfi.port.management.http
     * @test.tsfi tsfi.app.web.admin.console
     * @test.tsfi tsfi.keystore.file
     * @test.tsfi tsfi.truststore.file
     * @test.objective Testing authentication over management-http port. Test with user who has right/wrong certificate
     *                 to login into management web interface. Also provides check for web administration console
     *                 authentication, which goes through /management context.
     *
     * @test.expectedResult Management web console page is successfully reached, and test finishes without exception.
     *
     * @throws ClientProtocolException, IOException, URISyntaxException
     */
    @Test
    @InSequence(1)
    public void testHTTP() throws ClientProtocolException, IOException, URISyntaxException {

        DefaultHttpClient httpClient = new DefaultHttpClient();
        URL mgmtURL = new URL("http", TestSuiteEnvironment.getServerAddress(), MANAGEMENT_HTTP_PORT, MGMT_CTX);

        try {
            String responseBody = makeCallWithHttpClient(mgmtURL, httpClient, 401);
            assertThat("Management index page was reached", responseBody, not(containsString("management-major-version")));
            fail("Untrusted client should not be authenticated.");
        } catch (SSLHandshakeException e) {
            // OK
        }

        final HttpClient trustedHttpClient = getHttpClient(CLIENT_KEYSTORE_FILE);
        String responseBody = makeCallWithHttpClient(mgmtURL, trustedHttpClient, 200);
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

        final HttpClient httpClient = getHttpClient(CLIENT_KEYSTORE_FILE);
        final HttpClient httpClientUntrusted = getHttpClient(UNTRUSTED_KEYSTORE_FILE);

        URL mgmtURL = new URL("https", TestSuiteEnvironment.getServerAddress(), MANAGEMENT_HTTPS_PORT, MGMT_CTX);
        try {
            String responseBody = makeCallWithHttpClient(mgmtURL, httpClientUntrusted, 401);
            assertThat("Management index page was reached", responseBody, not(containsString("management-major-version")));
        } catch (SSLHandshakeException e) {
            // OK
        }

        String responseBody = makeCallWithHttpClient(mgmtURL, httpClient, 200);
        assertTrue("Management index page was not reached", responseBody.contains("management-major-version"));

    }

    @Test
    @InSequence(3)
    public void stopContainer() throws Exception {

        LOGGER.info("*** reseting test configuration");
        ModelControllerClient client = getNativeModelControllerClient();
        ManagementClient managementClient = new ManagementClient(client, TestSuiteEnvironment.getServerAddress(),
                MANAGEMENT_NATIVE_PORT , "remoting");

        resetHttpInterfaceConfiguration(client);

        // reload to apply changes
        CustomCLIExecutor.execute(null, RELOAD, NATIVE_CONTROLLER);
        waitForServerToReload(MAX_RELOAD_TIME, NATIVE_CONTROLLER);

        serverTearDown(managementClient);
        managementNativeRealmSetup.tearDown(managementClient, CONTAINER);

        LOGGER.info("*** stopping container");
        containerController.stop(CONTAINER);
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

    private void serverSetup(ManagementClient managementClient) throws Exception {

     // create key and trust stores with imported certificates from opposing sides
        FileUtils.deleteDirectory(WORK_DIR);
        WORK_DIR.mkdirs();
        Utils.createKeyMaterial(WORK_DIR);

        final ModelControllerClient client = managementClient.getControllerClient();

        // change security-realm for http management interface
        ModelNode operation = createOpNode("core-service=management/management-interface=http-interface",
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

        // create native interface to control server while http interface will be secured
        operation = createOpNode("core-service=management/management-interface=native-interface", ModelDescriptionConstants.ADD);
        operation.get("security-realm").set("ManagementRealm");
        operation.get("interface").set("management");
        operation.get("port").set(MANAGEMENT_NATIVE_PORT);
        Utils.applyUpdate(operation, client);
    }

    private void serverTearDown(ManagementClient managementClient) throws Exception {

        final ModelControllerClient client = managementClient.getControllerClient();

        ModelNode operation = createOpNode("core-service=management/management-interface=native-interface",
                ModelDescriptionConstants.REMOVE);
        Utils.applyUpdate(operation, client);

        FileUtils.deleteDirectory(WORK_DIR);
    }

    private void resetHttpInterfaceConfiguration(ModelControllerClient client) throws Exception {

        // change back security realm for http management interface
        ModelNode operation = createOpNode("core-service=management/management-interface=http-interface",
                ModelDescriptionConstants.WRITE_ATTRIBUTE_OPERATION);
        operation.get(NAME).set("security-realm");
        operation.get(VALUE).set("ManagementRealm");
        Utils.applyUpdate(operation, client);

        // undefine secure socket binding from http interface
        operation = createOpNode("core-service=management/management-interface=http-interface",
                ModelDescriptionConstants.UNDEFINE_ATTRIBUTE_OPERATION);
        operation.get(NAME).set("secure-socket-binding");
        Utils.applyUpdate(operation, client);
    }

    private ModelControllerClient getNativeModelControllerClient(){

        ModelControllerClient client = null;
        try {
            client = ModelControllerClient.Factory.create("remote", InetAddress.getByName(TestSuiteEnvironment.getServerAddress()),
                    MANAGEMENT_NATIVE_PORT, new CallbackHandler());
        } catch (UnknownHostException e) {
            throw new RuntimeException(e);
        }
        return client;
    }

}
