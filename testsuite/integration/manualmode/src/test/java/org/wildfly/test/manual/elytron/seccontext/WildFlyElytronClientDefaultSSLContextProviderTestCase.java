/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2021, Red Hat Middleware LLC, and individual contributors
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
package org.wildfly.test.manual.elytron.seccontext;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.jboss.as.controller.client.helpers.Operations.createAddOperation;
import static org.jboss.as.controller.client.helpers.Operations.createAddress;
import static org.jboss.as.controller.client.helpers.Operations.createRemoveOperation;

import java.io.File;
import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.security.Security;

import javax.net.ssl.SSLContext;
import jakarta.ws.rs.client.ClientBuilder;
import jakarta.ws.rs.core.Response;

import org.codehaus.plexus.util.FileUtils;
import org.jboss.arquillian.container.test.api.ContainerController;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.junit.InSequence;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.as.arquillian.container.ManagementClient;
import org.jboss.as.controller.client.ModelControllerClient;
import org.jboss.as.test.integration.management.util.CLIWrapper;
import org.jboss.as.test.integration.security.common.CoreUtils;
import org.jboss.as.test.shared.ServerReload;
import org.jboss.as.test.shared.TestSuiteEnvironment;
import org.jboss.dmr.ModelNode;
import org.jboss.resteasy.client.jaxrs.ResteasyClient;
import org.jboss.resteasy.client.jaxrs.ResteasyClientBuilder;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.wildfly.security.auth.client.WildFlyElytronClientDefaultSSLContextProvider;

/**
 * Integration test for WildFlyElytronClientDefaultSSLContextProvider.
 * This test uses server that has 2-way SSL context configured with need-client-auth set to true.
 * WildFlyElytronClientDefaultSSLContextProvider is configured on client side to provide default SSL context that RESTEsy client utilizes to connect to the server.
 */
@RunWith(Arquillian.class)
public class WildFlyElytronClientDefaultSSLContextProviderTestCase {
    private static final String[] SERVER_KEY_STORE1 = {"subsystem", "elytron", "key-store", "twoWayKS"};
    private static final String[] SERVER_KEY_MANAGER1 = {"subsystem", "elytron", "key-manager", "twoWayKM"};

    private static final String[] SERVER_TRUST_STORE1 = {"subsystem", "elytron", "key-store", "twoWayTS"};
    private static final String[] SERVER_TRUST_MANAGER1 = {"subsystem", "elytron", "trust-manager", "twoWayTM"};
    private static final String[] SERVER_SSL_CONTEXT1 = {"subsystem", "elytron", "server-ssl-context", "twoWaySSC"};

    private static final String SERVER_KEYSTORE1_FILENAME = "server.keystore";
    private static final String SERVER_TRUSTSTORE1_FILENAME = "server.truststore";
    private static final String CONTAINER = "default-jbossas";
    private static final String SERVER_ADDRESS = TestSuiteEnvironment.getServerAddress();
    private static final String JKS = "JKS";

    private static final File KEYSTORES_DIR = new File("target/keystores");

    private static final String CONFIG_FILE = "./src/test/resources/wildfly-config-default-ssl-context.xml";

    @ArquillianResource
    private static ContainerController serverController;

    @BeforeClass
    public static void generateKeyStoresAndConfigureWildFlyElytronClientDefaultSSLContextProvider() throws Exception {
        if (!KEYSTORES_DIR.exists()) {
            KEYSTORES_DIR.mkdirs();
        }
        CoreUtils.createKeyMaterial(KEYSTORES_DIR, JKS);
        Security.insertProviderAt(new WildFlyElytronClientDefaultSSLContextProvider(CONFIG_FILE), 1);
    }

    @Test
    @RunAsClient
    @InSequence(1)
    public void startAndConfigureContainerToRequireClientAuth() {
        if (!serverController.isStarted(CONTAINER)) {
            serverController.start(CONTAINER);
        }
        try {
            ModelControllerClient modelControllerClient = TestSuiteEnvironment.getModelControllerClient();
            ManagementClient managementClient = new ManagementClient(modelControllerClient, SERVER_ADDRESS, getManagementPort(), "remote+http");
            configureSSLContextOnServer(managementClient, KEYSTORES_DIR, SERVER_KEYSTORE1_FILENAME, SERVER_KEY_STORE1, SERVER_KEY_MANAGER1, SERVER_TRUSTSTORE1_FILENAME, SERVER_TRUST_STORE1, SERVER_TRUST_MANAGER1, SERVER_SSL_CONTEXT1);
            reloadServer(modelControllerClient);
        } catch (Exception e) {
            Assert.fail();
        }
    }

    @Test
    @InSequence(2)
    @RunAsClient
    public void testConnectionWithRESTEasyClient() {
        Assert.assertNotNull(Security.getProvider(WildFlyElytronClientDefaultSSLContextProvider.class.getSimpleName()));
        try {
            ResteasyClientBuilder builder = (ResteasyClientBuilder) ClientBuilder.newBuilder();
            ResteasyClient resteasyClient = builder.sslContext(SSLContext.getDefault()).hostnameVerification(ResteasyClientBuilder.HostnameVerificationPolicy.ANY).build();
            Assert.assertEquals(WildFlyElytronClientDefaultSSLContextProvider.class.getSimpleName(), resteasyClient.getSslContext().getProvider().getName());
            Response response = resteasyClient.target("https://localhost:" + 8443).request().get();
            Assert.assertEquals(200, response.getStatus());
            response.close();
        } catch (NoSuchAlgorithmException e) {
            Assert.fail("WildFlyElytronClientDefaultSSLContextProvider did not provide default SSLContext successfully");
        }
    }

    @Test
    @RunAsClient
    @InSequence(3)
    public void restoreConfigAndStopContainer() throws IOException {
        try {
            ManagementClient managementClient = new ManagementClient(TestSuiteEnvironment.getModelControllerClient(), SERVER_ADDRESS, getManagementPort(), "remote+http");
            restoreConfiguration(managementClient);
        } catch (Exception e) {
            Assert.fail();
        } finally {
            serverController.stop(CONTAINER);
            FileUtils.deleteDirectory(KEYSTORES_DIR);
            Security.removeProvider(new WildFlyElytronClientDefaultSSLContextProvider().getName());
        }
    }

    private static void configureSSLContextOnServer(ManagementClient managementClient, File directory, String keystoreFilename, String[] keyStore, String[] keyManager, String truststoreFilename, String[] trustStore, String[] trustManager, String[] sslContext1) {
        try {
            ModelNode credential = new ModelNode();
            credential.get("clear-text").set("123456");
            ModelNode modelNode = createAddOperation(createAddress(keyStore));
            modelNode.get("type").set(JKS);
            modelNode.get("path").set(new File(directory, keystoreFilename).getAbsolutePath());
            modelNode.get("credential-reference").set(credential);
            managementClient.getControllerClient().execute(modelNode);

            modelNode = createAddOperation(createAddress(keyManager));
            modelNode.get("algorithm").set("SunX509");
            modelNode.get("key-store").set(keyStore[keyStore.length - 1]);
            modelNode.get("credential-reference").set(credential);
            managementClient.getControllerClient().execute(modelNode);

            modelNode = createAddOperation(createAddress(trustStore));
            modelNode.get("type").set(JKS);
            modelNode.get("path").set(new File(directory, truststoreFilename).getAbsolutePath());
            modelNode.get("credential-reference").set(credential);
            managementClient.getControllerClient().execute(modelNode);

            modelNode = createAddOperation(createAddress(trustManager));
            modelNode.get("algorithm").set("SunX509");
            modelNode.get("key-store").set(trustStore[trustStore.length - 1]);
            modelNode.get("credential-reference").set(credential);
            managementClient.getControllerClient().execute(modelNode);

            modelNode = createAddOperation(createAddress(sslContext1));
            modelNode.get("key-manager").set(keyManager[keyManager.length - 1]);
            modelNode.get("trust-manager").set(trustManager[trustManager.length - 1]);
            modelNode.get("protocols").set(new ModelNode().add("TLSv1.2"));
            modelNode.get("need-client-auth").set(true);
            Assert.assertTrue(managementClient.getControllerClient().execute(modelNode).asString().contains("\"outcome\" => \"success\""));

            // Remove the reference to the legacy security realm and update the https-listener to use the ssl-context we just created
            CLIWrapper cli = new CLIWrapper(true);
            cli.sendLine("batch");
            cli.sendLine("/subsystem=undertow/server=default-server/https-listener=https:write-attribute(name=ssl-context,value=twoWaySSC)");
            cli.sendLine("run-batch");
            cli.sendLine("reload");
        } catch (Exception e) {
            Assert.fail();
        }
    }

    private void restoreConfiguration(ManagementClient managementClient) throws IOException {
        managementClient.getControllerClient().execute(createRemoveOperation(createAddress(SERVER_SSL_CONTEXT1)));
        managementClient.getControllerClient().execute(createRemoveOperation(createAddress(SERVER_KEY_MANAGER1)));
        managementClient.getControllerClient().execute(createRemoveOperation(createAddress(SERVER_TRUST_MANAGER1)));
        managementClient.getControllerClient().execute(createRemoveOperation(createAddress(SERVER_KEY_STORE1)));
        managementClient.getControllerClient().execute(createRemoveOperation(createAddress(SERVER_TRUST_STORE1)));
    }

    private void reloadServer(ModelControllerClient managementClient) {
        ServerReload.executeReloadAndWaitForCompletion(managementClient, (int) SECONDS.toMillis(90), false, SERVER_ADDRESS, getManagementPort());
    }

    private int getManagementPort() {
        return TestSuiteEnvironment.getServerPort();
    }
}
