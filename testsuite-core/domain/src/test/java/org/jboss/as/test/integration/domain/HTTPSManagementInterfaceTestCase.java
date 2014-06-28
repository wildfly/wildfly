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

package org.jboss.as.test.integration.domain;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.not;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.HOST;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.NAME;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.VALUE;
import static org.jboss.as.test.integration.management.util.CustomCLIExecutor.MANAGEMENT_HTTPS_PORT;
import static org.jboss.as.test.integration.management.util.CustomCLIExecutor.MANAGEMENT_HTTP_PORT;
import static org.jboss.as.test.integration.management.util.ModelUtil.createOpNode;
import static org.jboss.as.test.integration.security.common.CoreUtils.makeCallWithHttpClient;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.concurrent.TimeoutException;
import javax.net.ssl.SSLPeerUnverifiedException;

import org.apache.commons.io.FileUtils;
import org.apache.http.client.HttpClient;
import org.apache.http.impl.client.DefaultHttpClient;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.client.ModelControllerClient;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.test.integration.domain.management.util.DomainLifecycleUtil;
import org.jboss.as.test.integration.domain.management.util.DomainTestSupport;
import org.jboss.as.test.integration.domain.management.util.WildFlyManagedConfiguration;
import org.jboss.as.test.integration.security.common.AbstractBaseSecurityRealmsServerSetupTask;
import org.jboss.as.test.integration.security.common.CoreUtils;
import org.jboss.as.test.integration.security.common.SSLTruststoreUtil;
import org.jboss.as.test.integration.security.common.SecurityTestConstants;
import org.jboss.as.test.integration.security.common.config.realm.Authentication;
import org.jboss.as.test.integration.security.common.config.realm.RealmKeystore;
import org.jboss.as.test.integration.security.common.config.realm.SecurityRealm;
import org.jboss.as.test.integration.security.common.config.realm.ServerIdentity;
import org.jboss.dmr.ModelNode;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * Tests of managing a host controller using HTTPS.
 *
 * @author Brian Stansberry (c) 2014 Red Hat Inc.
 */
public class HTTPSManagementInterfaceTestCase {

    private static final File WORK_DIR = new File("target" + File.separatorChar + "https-mgmt-workdir");
    public static final File SERVER_KEYSTORE_FILE = new File(WORK_DIR, SecurityTestConstants.SERVER_KEYSTORE);
    public static final File SERVER_TRUSTSTORE_FILE = new File(WORK_DIR, SecurityTestConstants.SERVER_TRUSTSTORE);
    public static final File CLIENT_KEYSTORE_FILE = new File(WORK_DIR, SecurityTestConstants.CLIENT_KEYSTORE);
    public static final File CLIENT_TRUSTSTORE_FILE = new File(WORK_DIR, SecurityTestConstants.CLIENT_TRUSTSTORE);
    public static final File UNTRUSTED_KEYSTORE_FILE = new File(WORK_DIR, SecurityTestConstants.UNTRUSTED_KEYSTORE);

    private static final HttpManagementRealmSetup httpManagementRealmSetup = new HttpManagementRealmSetup();
    private static final String MANAGEMENT_WEB_REALM = "ManagementWebRealm";
    private static final String MGMT_CTX = "/management";
    public static final String CONTAINER = "default-jbossas";

    private static DomainTestSupport testSupport;
    private static DomainLifecycleUtil domainMasterLifecycleUtil;

    @BeforeClass
    public static void setupDomain() throws Exception {

        keyMaterialSetup();

        DomainTestSupport.Configuration configuration = DomainTestSupport.Configuration.create(
                HTTPSManagementInterfaceTestCase.class.getSimpleName(), "domain-configs/domain-minimal.xml", "host-configs/host-master-no-http.xml", null);
        WildFlyManagedConfiguration masterConfig = configuration.getMasterConfiguration();
        masterConfig.setAdminOnly(true);
        String args = masterConfig.getJavaVmArguments();
        args = args == null ? "" : args + " ";
        args = args + "-Djboss.test.host.slave.address=" + DomainTestSupport.slaveAddress;
        masterConfig.setJavaVmArguments(args);
        testSupport = DomainTestSupport.createAndStartSupport(configuration);
        domainMasterLifecycleUtil = testSupport.getDomainMasterLifecycleUtil();

        httpManagementRealmSetup.setup(domainMasterLifecycleUtil.getDomainClient(), CONTAINER);
    }

    @AfterClass
    public static void tearDownDomain() throws Exception {

        httpManagementRealmSetup.tearDown(domainMasterLifecycleUtil.getDomainClient(), CONTAINER);

        testSupport.stop();
        testSupport = null;
        domainMasterLifecycleUtil = null;

        FileUtils.deleteDirectory(WORK_DIR);
    }

    @Before
    public void addHttpInterface() throws Exception {

        ModelNode operation = createOpNode("host=master/core-service=management/management-interface=http-interface",
                ModelDescriptionConstants.ADD);
        operation.get("interface").set("management");
        operation.get("port").set(MANAGEMENT_HTTP_PORT);
        operation.get("security-realm").set(MANAGEMENT_WEB_REALM);
        operation.get("http-upgrade-enabled").set(true);
        operation.get("secure-port").set(MANAGEMENT_HTTPS_PORT);
        CoreUtils.applyUpdate(operation, domainMasterLifecycleUtil.getDomainClient());
    }

    @After
    public void removeHttpInterface() throws Exception {

        ModelNode operation = createOpNode("host=master/core-service=management/management-interface=http-interface",
                ModelDescriptionConstants.REMOVE);
        CoreUtils.applyUpdate(operation, domainMasterLifecycleUtil.getDomainClient());
    }

    /**
     * @throws Exception
     * @test.tsfi tsfi.port.management.http
     * @test.tsfi tsfi.app.web.admin.console
     * @test.tsfi tsfi.keystore.file
     * @test.tsfi tsfi.truststore.file
     * @test.objective Testing authentication over management-http port. Test with user who has right/wrong certificate
     * to login into management web interface. Also provides check for web administration console
     * authentication, which goes through /management context.
     * @test.expectedResult Management web console page is successfully reached, and test finishes without exception.
     */
    @Test
    public void testHTTP() throws Exception {
        httpTest(false);
    }

    /**
     * @throws Exception
     * @test.tsfi tsfi.port.management.http
     * @test.tsfi tsfi.app.web.admin.console
     * @test.tsfi tsfi.keystore.file
     * @test.tsfi tsfi.truststore.file
     * @test.objective Testing authentication over management-http port. Test with user who has right/wrong certificate
     * to login into management web interface. Also provides check for web administration console
     * authentication, which goes through /management context.
     * @test.expectedResult Management web console page is successfully reached, and test finishes without exception.
     */
    @Test
    public void testHTTPWithSecureInterface() throws Exception {
        httpTest(true);
    }

    private void httpTest(boolean addSecureInterface) throws Exception {

        if (addSecureInterface) {
            addSecureInterface();
        }
        reload();

        DefaultHttpClient httpClient = new DefaultHttpClient();
        URL mgmtURL = new URL("http", DomainTestSupport.masterAddress, MANAGEMENT_HTTP_PORT, MGMT_CTX);

        try {
            String responseBody = makeCallWithHttpClient(mgmtURL, httpClient, 401);
            assertThat("Management index page was reached", responseBody, not(containsString("management-major-version")));
            fail("Untrusted client should not be authenticated.");
        } catch (SSLPeerUnverifiedException e) {
            // OK
        }

        final HttpClient trustedHttpClient = getHttpClient(CLIENT_KEYSTORE_FILE);
        String responseBody = makeCallWithHttpClient(mgmtURL, trustedHttpClient, 200);
        assertTrue("Management index page was not reached", responseBody.contains("management-major-version"));
    }

    /**
     * @throws org.apache.http.client.ClientProtocolException, IOException, URISyntaxException
     * @test.tsfi tsfi.port.management.https
     * @test.tsfi tsfi.app.web.admin.console
     * @test.tsfi tsfi.keystore.file
     * @test.tsfi tsfi.truststore.file
     * @test.objective Testing authentication over management-https port. Test with user who has right/wrong certificate
     * to login into management web interface. Also provides check for web administration console
     * authentication, which goes through /management context.
     * @test.expectedResult Management web console page is successfully reached, and test finishes without exception.
     */
    @Test
    public void testHTTPS() throws Exception {
        httpsTest(false);
    }

    /**
     * @throws org.apache.http.client.ClientProtocolException, IOException, URISyntaxException
     * @test.tsfi tsfi.port.management.https
     * @test.tsfi tsfi.app.web.admin.console
     * @test.tsfi tsfi.keystore.file
     * @test.tsfi tsfi.truststore.file
     * @test.objective Testing authentication over management-https port. Test with user who has right/wrong certificate
     * to login into management web interface. Also provides check for web administration console
     * authentication, which goes through /management context.
     * @test.expectedResult Management web console page is successfully reached, and test finishes without exception.
     */
    @Test
    public void testHTTPSWithSecureInterface() throws Exception {
        httpsTest(true);
    }

    private void httpsTest(boolean addSecureInterface) throws Exception {

        if (addSecureInterface) {
            addSecureInterface();
        }
        reload();

        final HttpClient httpClient = getHttpClient(CLIENT_KEYSTORE_FILE);
        final HttpClient httpClientUntrusted = getHttpClient(UNTRUSTED_KEYSTORE_FILE);

        String address = addSecureInterface ? DomainTestSupport.slaveAddress : DomainTestSupport.masterAddress;
        URL mgmtURL = new URL("https", address, MANAGEMENT_HTTPS_PORT, MGMT_CTX);
        try {
            String responseBody = makeCallWithHttpClient(mgmtURL, httpClientUntrusted, 401);
            assertThat("Management index page was reached", responseBody, not(containsString("management-major-version")));
        } catch (SSLPeerUnverifiedException e) {
            // OK
        }

        String responseBody = makeCallWithHttpClient(mgmtURL, httpClient, 200);
        assertTrue("Management index page was not reached", responseBody.contains("management-major-version"));

    }

    @Test
    public void testHttpsRedirect() throws Exception {
        httpsRedirectTest(false);
    }

    @Test
    public void testNoHttpsRedirectWithSecureInterface() throws Exception {
        httpsRedirectTest(true);
    }

    private void httpsRedirectTest(boolean addSecureInterface) throws Exception {

        if (addSecureInterface) {
            addSecureInterface();
        }
        reload();

        final HttpClient httpClient = getHttpClient(CLIENT_KEYSTORE_FILE);

        URL mgmtURL = new URL("https", DomainTestSupport.masterAddress, MANAGEMENT_HTTP_PORT, MGMT_CTX);
        try {
            int expectedStatus = addSecureInterface ? 403 : 302;
            String responseBody = makeCallWithHttpClient(mgmtURL, httpClient, expectedStatus);
            assertThat("Management index page was reached", responseBody, not(containsString("management-major-version")));
        } catch (SSLPeerUnverifiedException e) {
            // OK
        }
    }

    private void addSecureInterface() throws Exception {

        ModelNode operation = createOpNode("host=master/core-service=management/management-interface=http-interface",
                ModelDescriptionConstants.WRITE_ATTRIBUTE_OPERATION);
        operation.get(NAME).set("secure-interface");
        operation.get(VALUE).set("secure-management");
        CoreUtils.applyUpdate(operation, domainMasterLifecycleUtil.getDomainClient());

    }

    private void reload() throws IOException, TimeoutException, InterruptedException {
        ModelNode op = new ModelNode();
        op.get(OP_ADDR).add(HOST, "master");
        op.get(OP).set("reload");
        op.get("admin-only").set(false);
        domainMasterLifecycleUtil.executeAwaitConnectionClosed(op);

        // Try to reconnect to the hc
        domainMasterLifecycleUtil.connect();
        domainMasterLifecycleUtil.awaitHostController(System.currentTimeMillis());

    }

    static class HttpManagementRealmSetup extends AbstractBaseSecurityRealmsServerSetupTask {

        // Overridden just to expose locally
        @Override
        protected void setup(ModelControllerClient modelControllerClient, String containerId) throws Exception {
            super.setup(modelControllerClient, containerId);
        }

        // Overridden just to expose locally
        @Override
        protected void tearDown(ModelControllerClient modelControllerClient, String containerId) throws Exception {
            super.tearDown(modelControllerClient, containerId);
        }

        @Override
        protected PathAddress getBaseAddress() {
            return PathAddress.pathAddress(PathElement.pathElement(HOST, "master"));
        }

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
            return new SecurityRealm[]{realm};
        }
    }

    private static void keyMaterialSetup() throws Exception {

        // create key and trust stores with imported certificates from opposing sides
        FileUtils.deleteDirectory(WORK_DIR);
        WORK_DIR.mkdirs();
        Assert.assertTrue(WORK_DIR.exists());
        Assert.assertTrue(WORK_DIR.isDirectory());
        CoreUtils.createKeyMaterial(WORK_DIR);
    }

    private static HttpClient getHttpClient(File keystoreFile) {
        return SSLTruststoreUtil.getHttpClientWithSSL(keystoreFile, SecurityTestConstants.KEYSTORE_PASSWORD, CLIENT_TRUSTSTORE_FILE,
                SecurityTestConstants.KEYSTORE_PASSWORD);
    }
}
