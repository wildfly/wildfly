package org.jboss.as.test.integration.web.headers.authentication;

import org.jboss.as.arquillian.container.ManagementClient;
import org.jboss.as.clustering.controller.Operations;
import org.jboss.as.test.integration.security.common.CoreUtils;
import org.jboss.as.test.shared.ServerReload;
import org.jboss.as.test.shared.SnapshotRestoreSetupTask;
import org.jboss.as.test.shared.TestSuiteEnvironment;
import org.jboss.dmr.ModelNode;
import org.wildfly.test.security.common.other.KeyStoreUtils;
import org.wildfly.test.security.common.other.KeyUtils;

import java.io.File;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.List;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ADD;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ALLOW_RESOURCE_SERVICE_RESTART;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OPERATION_HEADERS;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ROLLBACK_ON_RUNTIME_FAILURE;
import static org.jboss.as.test.integration.management.util.ModelUtil.createOpNode;

/**
 * Server setup task for test ResponseHeaderAuthenticationTestCase.
 * Configures key-store and application-security-domain.
 */
public class ResponseHeaderAuthenticationServerSetupTask extends SnapshotRestoreSetupTask {

    public static final String PASSWORD = "password1";
    private static final String ALIAS = "single-sign-on";
    private static final String KEYSTORE_FILE_NAME = "single-sign-on.jks";

    @Override
    public void doSetup(ManagementClient managementClient, String containerId) throws Exception {
        List<ModelNode> operations = new ArrayList<>();

        // /subsystem=elytron/http-authentication-factory=application-http-authentication:add(http-server-mechanism-factory=global, security-domain=ApplicationDomain,mechanism-configurations=[{mechanism-name=BASIC, mechanism-realm-configurations=[{realm-name=Application Realm}]},{mechanism-name=FORM}])
        ModelNode addHttpAuthenticationFactory = createOpNode("subsystem=elytron/http-authentication-factory=test-http-authentication", ADD);
        addHttpAuthenticationFactory.get("http-server-mechanism-factory").set("global");
        addHttpAuthenticationFactory.get("security-domain").set("ApplicationDomain");
        addHttpAuthenticationFactory.get("mechanism-configurations").get(0).get("mechanism-name").set("BASIC");
        addHttpAuthenticationFactory.get("mechanism-configurations").get(0).get("mechanism-realm-configurations").get(0).get("realm-name").set("Application Realm");
        addHttpAuthenticationFactory.get("mechanism-configurations").get(1).get("mechanism-name").set("FORM");
        operations.add(addHttpAuthenticationFactory);

        // /subsystem=elytron/key-store=single-sign-on:add(path=single-sign-on.jks, type=JKS, relative-to=jboss.server.config.dir, credential-reference={clear-text=password})
        ModelNode addKeyStore = createOpNode("subsystem=elytron/key-store=single-sign-on", ADD);
        addKeyStore.get("path").set(KEYSTORE_FILE_NAME);
        addKeyStore.get("type").set("JKS");
        addKeyStore.get("relative-to").set("jboss.server.config.dir");
        ModelNode credentialReference = new ModelNode();
        credentialReference.get("clear-text").set(ResponseHeaderAuthenticationTestCase.PASSWORD);
        addKeyStore.get("credential-reference").set(credentialReference);
        operations.add(addKeyStore);

        // /subsystem=undertow/application-security-domain=ApplicationDomain:add(http-authentication-factory=application-http-authentication)
        ModelNode addSecurityDomain = createOpNode("subsystem=undertow/application-security-domain=ApplicationDomain", ADD);
        addSecurityDomain.get("http-authentication-factory").set("test-http-authentication");
        operations.add(addSecurityDomain);

        // /subsystem=undertow/application-security-domain=ApplicationDomain/setting=single-sign-on:add(key-alias=single-sign-on, credential-reference={clear-text=password},key-store=single-sign-on)
        ModelNode addSingleSignOn = createOpNode("subsystem=undertow/application-security-domain=ApplicationDomain/setting=single-sign-on", ADD);
        addSingleSignOn.get("key-alias").set("single-sign-on");
        addSingleSignOn.get("credential-reference").set(credentialReference);
        addSingleSignOn.get("key-store").set("single-sign-on");
        operations.add(addSingleSignOn);

        ModelNode updateOp = Operations.createCompositeOperation(operations);
        updateOp.get(OPERATION_HEADERS, ROLLBACK_ON_RUNTIME_FAILURE).set(true);
        updateOp.get(OPERATION_HEADERS, ALLOW_RESOURCE_SERVICE_RESTART).set(true);
        CoreUtils.applyUpdate(updateOp, managementClient.getControllerClient());

        this.createKeyStore();

        ServerReload.executeReloadAndWaitForCompletion(managementClient);
    }

    /**
     * Creates key store with certificate and stores it on the path {$jboss.home}/standalone/configuration/single-sign-on.jks
     *
     * @throws Exception
     */
    private void createKeyStore() throws Exception {
        File keyStoreFile = new File(TestSuiteEnvironment.getSystemProperty("jboss.home") + File.separator + "standalone"
                + File.separator + "configuration" + File.separator + KEYSTORE_FILE_NAME);
        keyStoreFile.createNewFile();

        String serverName = "server";
        KeyPair server = KeyUtils.generateKeyPair();
        X509Certificate serverCert = KeyUtils.generateX509Certificate(serverName, server);

        KeyStore keyStore = loadKeyStore();

        // generate a key pair
        KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("RSA");
        keyPairGenerator.initialize(2048, new SecureRandom());
        KeyPair keyPair = keyPairGenerator.generateKeyPair();
        PrivateKey signingKey = keyPair.getPrivate();

        keyStore.setKeyEntry(ALIAS, signingKey, PASSWORD.toCharArray(), new X509Certificate[]{serverCert});

        KeyStoreUtils.saveKeystore(keyStore, PASSWORD, keyStoreFile);
    }

    private static KeyStore loadKeyStore() throws Exception {
        KeyStore ks = KeyStore.getInstance("JKS");
        ks.load(null, PASSWORD.toCharArray());
        return ks;
    }
}
