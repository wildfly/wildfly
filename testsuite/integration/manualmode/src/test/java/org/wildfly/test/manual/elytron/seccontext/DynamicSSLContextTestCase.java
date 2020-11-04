/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.test.manual.elytron.seccontext;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.jboss.as.controller.client.helpers.Operations.createAddOperation;
import static org.jboss.as.controller.client.helpers.Operations.createAddress;
import static org.jboss.as.controller.client.helpers.Operations.createRemoveOperation;
import static org.jboss.as.controller.client.helpers.Operations.createUndefineAttributeOperation;
import static org.jboss.as.controller.client.helpers.Operations.createWriteAttributeOperation;
import static org.jboss.as.test.shared.PermissionUtils.createPermissionsXmlAsset;

import java.io.File;
import java.io.IOException;
import java.net.SocketPermission;
import java.util.ArrayList;
import java.util.List;
import java.util.PropertyPermission;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;

import org.codehaus.plexus.util.FileUtils;
import org.jboss.arquillian.container.test.api.ContainerController;
import org.jboss.arquillian.container.test.api.Deployer;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.OperateOnDeployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.container.test.api.TargetsContainer;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.junit.InSequence;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.as.arquillian.container.ManagementClient;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.client.ModelControllerClient;
import org.jboss.as.test.integration.security.common.CoreUtils;
import org.jboss.as.test.shared.ServerReload;
import org.jboss.as.test.shared.TestSuiteEnvironment;
import org.jboss.dmr.ModelNode;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Smoke test for client deployed to server that has dynamic ssl context configured as default in the Elytron subsystem
 *
 * @author dvilkola@redhat.com
 */
@RunWith(Arquillian.class)
public class DynamicSSLContextTestCase {
    private static final String[] SERVER_KEY_STORE1 = {"subsystem", "elytron", "key-store", "twoWayKS1"};
    private static final String[] SERVER_KEY_STORE2 = {"subsystem", "elytron", "key-store", "twoWayKS2"};
    private static final String[] SERVER_KEY_MANAGER1 = {"subsystem", "elytron", "key-manager", "twoWayKM1"};
    private static final String[] SERVER_KEY_MANAGER2 = {"subsystem", "elytron", "key-manager", "twoWayKM2"};

    private static final String[] SERVER_TRUST_STORE1 = {"subsystem", "elytron", "key-store", "twoWayTS1"};
    private static final String[] SERVER_TRUST_STORE2 = {"subsystem", "elytron", "key-store", "twoWayTS2"};
    private static final String[] SERVER_TRUST_MANAGER1 = {"subsystem", "elytron", "trust-manager", "twoWayTM1"};
    private static final String[] SERVER_TRUST_MANAGER2 = {"subsystem", "elytron", "trust-manager", "twoWayTM2"};
    private static final String[] SERVER_SSL_CONTEXT1 = {"subsystem", "elytron", "server-ssl-context", "twoWaySSC1"};
    private static final String[] SERVER_SSL_CONTEXT2 = {"subsystem", "elytron", "server-ssl-context", "twoWaySSC2"};

    private static final String[] CLIENT_KEY_STORE1 = {"subsystem", "elytron", "key-store", "clientKS1"};
    private static final String[] CLIENT_KEY_STORE2 = {"subsystem", "elytron", "key-store", "clientKS2"};
    private static final String[] CLIENT_KEY_MANAGER1 = {"subsystem", "elytron", "key-manager", "clientKM1"};
    private static final String[] CLIENT_KEY_MANAGER2 = {"subsystem", "elytron", "key-manager", "clientKM2"};
    private static final String[] CLIENT_SSL_CONTEXT1 = {"subsystem", "elytron", "client-ssl-context", "client1-ssl-context"};
    private static final String[] CLIENT_SSL_CONTEXT2 = {"subsystem", "elytron", "client-ssl-context", "client2-ssl-context"};

    private static final String[] CLIENT_TRUST_STORE1 = {"subsystem", "elytron", "key-store", "clientTS1"};
    private static final String[] CLIENT_TRUST_STORE2 = {"subsystem", "elytron", "key-store", "clientTS2"};
    private static final String[] CLIENT_TRUST_MANAGER1 = {"subsystem", "elytron", "trust-manager", "clientTM1"};
    private static final String[] CLIENT_TRUST_MANAGER2 = {"subsystem", "elytron", "trust-manager", "clientTM2"};

    private static final String[] AUTHENTICATION_CONTEXT = {"subsystem", "elytron", "authentication-context", "ac"};
    private static final String[] DYNAMIC_SSL_CONTEXT = {"subsystem", "elytron", "dynamic-client-ssl-context", "dynamicClientSSLContext"};
    private static final String[] ELYTRON_SUBSYSTEM = {"subsystem", "elytron"};

    private static final String[] HTTP_LISTENER_1 = {"subsystem", "undertow", "server", "default-server", "https-listener", "first-listener"};
    private static final String[] HTTP_LISTENER_2 = {"subsystem", "undertow", "server=default-server", "https-listener", "second-listener"};

    private static final String[] SOCKET_BINDING_1 = {"socket-binding-group", "standard-sockets", "socket-binding", "first-socket-binding"};
    private static final String[] SOCKET_BINDING_2 = {"socket-binding-group", "standard-sockets", "socket-binding", "second-socket-binding"};

    private static final String CLIENT_KEYSTORE1_FILENAME = "client.keystore";
    private static final String CLIENT_TRUSTSTORE1_FILENAME = "client.truststore";
    private static final String SERVER_KEYSTORE1_FILENAME = "server.keystore";
    private static final String SERVER_TRUSTSTORE1_FILENAME = "server.truststore";
    private static final String CONTAINER = "default-jbossas";
    private static final String DEPLOYMENT = "deployment";
    private static final String SERVER_ADDRESS = TestSuiteEnvironment.getServerAddress();
    private static final String JKS = "JKS";

    private static final int PORT1 = 10443;
    private static final int PORT2 = 11443;

    private static final File KEYSTORES_DIR = new File("target/keystores");
    private static final File KEYSTORES2_DIR = new File("target/keystores2");

    @ArquillianResource
    private static ContainerController serverController;

    @ArquillianResource
    private static Deployer deployer;

    @BeforeClass
    public static void generateKeystores() throws Exception {
        if (!KEYSTORES_DIR.exists()) {
            KEYSTORES_DIR.mkdirs();
        }
        if (!KEYSTORES2_DIR.exists()) {
            KEYSTORES2_DIR.mkdirs();
        }
        CoreUtils.createKeyMaterial(KEYSTORES_DIR, JKS);
        CoreUtils.createKeyMaterial(KEYSTORES2_DIR, JKS);
    }

    @AfterClass
    public static void deleteKeystores() throws Exception {
        if (serverController.isStarted(CONTAINER)) {
            serverController.stop(CONTAINER);
        }
        if (KEYSTORES_DIR.exists()) {
            FileUtils.deleteDirectory(KEYSTORES_DIR);
        }
        if (KEYSTORES2_DIR.exists()) {
            FileUtils.deleteDirectory(KEYSTORES2_DIR);
        }
    }

    @Deployment(name = DEPLOYMENT, managed = false)
    @TargetsContainer(CONTAINER)
    public static Archive<?> deploy() {
        return ShrinkWrap.create(WebArchive.class, "archive" + ".war")
                .addAsManifestResource(new StringAsset("Dependencies: org.jboss.as.controller-client,org.jboss.dmr\n"), "MANIFEST.MF")
                .addPackage(TestSuiteEnvironment.class.getPackage())
                .addAsManifestResource(createPermissionsXmlAsset(
                        new PropertyPermission("management.address", "read"),
                        new PropertyPermission("node0", "read"),
                        new SocketPermission(SERVER_ADDRESS, "connect,resolve")), "permissions.xml");
    }

    @Test
    @RunAsClient
    @InSequence(1)
    public void startAndConfigureContainerToUseDynamicSSLContextAsDefault() {
        if (!serverController.isStarted(CONTAINER)) {
            serverController.start(CONTAINER);
        }
        try {
            ModelControllerClient modelControllerClient = TestSuiteEnvironment.getModelControllerClient();
            ManagementClient managementClient = new ManagementClient(modelControllerClient, SERVER_ADDRESS, getManagementPort(), "remote+http");

            addHTTPSListener(managementClient, KEYSTORES_DIR, SERVER_KEYSTORE1_FILENAME, SERVER_KEY_STORE1, SERVER_KEY_MANAGER1, SERVER_TRUSTSTORE1_FILENAME, SERVER_TRUST_STORE1, SERVER_TRUST_MANAGER1, SERVER_SSL_CONTEXT1, HTTP_LISTENER_1, SOCKET_BINDING_1, PORT1);
            addHTTPSListener(managementClient, KEYSTORES2_DIR, SERVER_KEYSTORE1_FILENAME, SERVER_KEY_STORE2, SERVER_KEY_MANAGER2, SERVER_TRUSTSTORE1_FILENAME, SERVER_TRUST_STORE2, SERVER_TRUST_MANAGER2, SERVER_SSL_CONTEXT2, HTTP_LISTENER_2, SOCKET_BINDING_2, PORT2);

            addSSLContext(managementClient, KEYSTORES_DIR, CLIENT_KEYSTORE1_FILENAME, CLIENT_KEY_STORE1, CLIENT_KEY_MANAGER1, CLIENT_TRUSTSTORE1_FILENAME, CLIENT_TRUST_STORE1, CLIENT_TRUST_MANAGER1, CLIENT_SSL_CONTEXT1);
            addSSLContext(managementClient, KEYSTORES2_DIR, CLIENT_KEYSTORE1_FILENAME, CLIENT_KEY_STORE2, CLIENT_KEY_MANAGER2, CLIENT_TRUSTSTORE1_FILENAME, CLIENT_TRUST_STORE2, CLIENT_TRUST_MANAGER2, CLIENT_SSL_CONTEXT2);

            addAuthenticationContext(managementClient);
            configureDefaultSSLContext(managementClient);

            reloadServer(modelControllerClient);
            deployer.deploy(DEPLOYMENT);
        } catch (Exception e) {
            Assert.fail();
        }
    }

    @Test
    @InSequence(2)
    @OperateOnDeployment(DEPLOYMENT)
    public void clientRunningInsideContainerUsesDynamicSSLContextTest() throws Exception {
        // default ssl context is configured to be dynamic in the server
        SSLSocketFactory sf = SSLContext.getDefault().getSocketFactory();
        // both handshakes will succeed even though these ports require different truststore and keystore
        SSLSocket socket = (SSLSocket) sf.createSocket(SERVER_ADDRESS, PORT1);
        socket.startHandshake();
        socket = (SSLSocket) sf.createSocket(SERVER_ADDRESS, PORT2);
        socket.startHandshake();
    }

    @Test
    @RunAsClient
    @InSequence(3)
    public void restoreConfigAndStopContainer() throws IOException {
        try {
            ManagementClient managementClient = new ManagementClient(TestSuiteEnvironment.getModelControllerClient(), SERVER_ADDRESS, getManagementPort(), "remote+http");
            restoreConfiguration(managementClient);
            deployer.undeploy(DEPLOYMENT);
        } catch (Exception e) {
            Assert.fail();
        }
    }

    private static void addHTTPSListener(ManagementClient managementClient, File directory, String serverKeystoreFilename, String[] keyStore, String[] keyManager, String serverTruststoreFilename, String[] trustStore, String[] trustManager, String[] sslContext, String[] httpsListener, String[] socketBinding, int port) throws IOException {
        addSSLContext(managementClient, directory, serverKeystoreFilename, keyStore, keyManager, serverTruststoreFilename, trustStore, trustManager, sslContext);
        ModelNode modelNode;

        modelNode = createAddOperation(createAddress(socketBinding));
        modelNode.get("port").set(port);
        managementClient.getControllerClient().execute(modelNode);

        ModelNode mn = createAddOperation(PathAddress.parseCLIStyleAddress("/subsystem=undertow/server=default-server/https-listener=" + httpsListener[httpsListener.length - 1]).toModelNode());
        mn.get("socket-binding").set(socketBinding[socketBinding.length - 1]);
        mn.get("ssl-context").set(sslContext[sslContext.length - 1]);
        mn.get("enable-http2").set(true);

        managementClient.getControllerClient().execute(mn);
    }

    private static void addSSLContext(ManagementClient managementClient, File directory, String keystoreFilename, String[] keyStore, String[] keyManager, String truststoreFilename, String[] trustStore, String[] trustManager, String[] sslContext1) {
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
            if (trustManager[trustManager.length - 1].contains("twoWayTM")) {
                modelNode.get("need-client-auth").set(true);
            }
            Assert.assertTrue(managementClient.getControllerClient().execute(modelNode).asString().contains("\"outcome\" => \"success\""));
        } catch (IOException e) {
            Assert.fail();
        }
    }

    private void restoreConfiguration(ManagementClient managementClient) throws IOException {
        managementClient.getControllerClient().execute(createUndefineAttributeOperation(createAddress(ELYTRON_SUBSYSTEM), "default-ssl-context"));
        managementClient.getControllerClient().execute(createRemoveOperation(createAddress(DYNAMIC_SSL_CONTEXT)));
        managementClient.getControllerClient().execute(createRemoveOperation(createAddress(AUTHENTICATION_CONTEXT)));
        managementClient.getControllerClient().execute(createRemoveOperation(
                PathAddress.parseCLIStyleAddress("/subsystem=undertow/server=default-server/https-listener=" + HTTP_LISTENER_1[HTTP_LISTENER_1.length - 1]).toModelNode()));
        managementClient.getControllerClient().execute(createRemoveOperation(
                PathAddress.parseCLIStyleAddress("/subsystem=undertow/server=default-server/https-listener=" + HTTP_LISTENER_2[HTTP_LISTENER_2.length - 1]).toModelNode()));

        managementClient.getControllerClient().execute(createRemoveOperation(createAddress(SOCKET_BINDING_1)));
        managementClient.getControllerClient().execute(createRemoveOperation(createAddress(SOCKET_BINDING_2)));

        managementClient.getControllerClient().execute(createRemoveOperation(createAddress(SERVER_SSL_CONTEXT1)));
        managementClient.getControllerClient().execute(createRemoveOperation(createAddress(SERVER_KEY_MANAGER1)));
        managementClient.getControllerClient().execute(createRemoveOperation(createAddress(SERVER_TRUST_MANAGER1)));
        managementClient.getControllerClient().execute(createRemoveOperation(createAddress(SERVER_KEY_STORE1)));
        managementClient.getControllerClient().execute(createRemoveOperation(createAddress(SERVER_TRUST_STORE1)));

        managementClient.getControllerClient().execute(createRemoveOperation(createAddress(SERVER_SSL_CONTEXT2)));
        managementClient.getControllerClient().execute(createRemoveOperation(createAddress(SERVER_KEY_MANAGER2)));
        managementClient.getControllerClient().execute(createRemoveOperation(createAddress(SERVER_TRUST_MANAGER2)));
        managementClient.getControllerClient().execute(createRemoveOperation(createAddress(SERVER_KEY_STORE2)));
        managementClient.getControllerClient().execute(createRemoveOperation(createAddress(SERVER_TRUST_STORE2)));

        managementClient.getControllerClient().execute(createRemoveOperation(createAddress(CLIENT_SSL_CONTEXT1)));
        managementClient.getControllerClient().execute(createRemoveOperation(createAddress(CLIENT_KEY_MANAGER1)));
        managementClient.getControllerClient().execute(createRemoveOperation(createAddress(CLIENT_TRUST_MANAGER1)));
        managementClient.getControllerClient().execute(createRemoveOperation(createAddress(CLIENT_KEY_STORE1)));
        managementClient.getControllerClient().execute(createRemoveOperation(createAddress(CLIENT_TRUST_STORE1)));

        managementClient.getControllerClient().execute(createRemoveOperation(createAddress(CLIENT_SSL_CONTEXT2)));
        managementClient.getControllerClient().execute(createRemoveOperation(createAddress(CLIENT_KEY_MANAGER2)));
        managementClient.getControllerClient().execute(createRemoveOperation(createAddress(CLIENT_TRUST_MANAGER2)));
        managementClient.getControllerClient().execute(createRemoveOperation(createAddress(CLIENT_KEY_STORE2)));
        managementClient.getControllerClient().execute(createRemoveOperation(createAddress(CLIENT_TRUST_STORE2)));
    }

    private void configureDefaultSSLContext(ManagementClient managementClient) throws IOException {
        ModelNode modelNode;
        modelNode = createWriteAttributeOperation(createAddress(ELYTRON_SUBSYSTEM),
                "default-ssl-context", DYNAMIC_SSL_CONTEXT[DYNAMIC_SSL_CONTEXT.length - 1]);
        Assert.assertTrue(managementClient.getControllerClient().execute(modelNode).asString().contains("\"outcome\" => \"success\""));
    }

    private void addAuthenticationContext(ManagementClient managementClient) throws IOException {
        ModelNode modelNode = createAddOperation(createAddress(AUTHENTICATION_CONTEXT));
        List<ModelNode> matchRules = new ArrayList<>();
        ModelNode matchRule = new ModelNode();
        matchRule.get("match-port").set(PORT1);
        matchRule.get("ssl-context").set(CLIENT_SSL_CONTEXT1[CLIENT_SSL_CONTEXT1.length - 1]);
        matchRules.add(matchRule);

        matchRule = new ModelNode();
        matchRule.get("match-port").set(PORT2);
        matchRule.get("ssl-context").set(CLIENT_SSL_CONTEXT2[CLIENT_SSL_CONTEXT2.length - 1]);
        matchRules.add(matchRule);

        // SSL context that should be used when no other rule matches
        matchRule = new ModelNode();
        matchRule.get("ssl-context").set(CLIENT_SSL_CONTEXT2[CLIENT_SSL_CONTEXT2.length - 1]);
        matchRules.add(matchRule);

        modelNode.get("match-rules").set(matchRules);
        Assert.assertTrue(managementClient.getControllerClient().execute(modelNode).asString().contains("\"outcome\" => \"success\""));

        modelNode = createAddOperation(createAddress(DYNAMIC_SSL_CONTEXT));
        modelNode.get("authentication-context").set("ac");
        managementClient.getControllerClient().execute(modelNode);
    }

    private void reloadServer(ModelControllerClient managementClient) {
        ServerReload.executeReloadAndWaitForCompletion(managementClient, (int) SECONDS.toMillis(90), false, SERVER_ADDRESS, getManagementPort());
    }

    private int getManagementPort() {
        return 9990;
    }
}
