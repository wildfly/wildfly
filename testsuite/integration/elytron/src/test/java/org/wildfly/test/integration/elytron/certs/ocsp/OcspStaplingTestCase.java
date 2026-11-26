/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.test.integration.elytron.certs.ocsp;

import static org.wildfly.security.x500.X500.OID_AD_OCSP;
import static org.wildfly.security.x500.X500.OID_KP_OCSP_SIGNING;

import java.io.File;
import java.io.FileOutputStream;
import java.math.BigInteger;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.URI;
import java.net.URL;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.Security;
import java.security.cert.CertPathValidatorException;
import java.security.cert.CertificateRevokedException;
import java.security.cert.X509Certificate;
import java.util.Collections;
import java.util.LinkedList;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLException;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import javax.security.auth.x500.X500Principal;

import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.as.arquillian.api.ServerSetup;
import org.jboss.as.controller.client.ModelControllerClient;
import org.jboss.as.controller.client.helpers.Operations;
import org.jboss.as.test.integration.management.util.CLIWrapper;
import org.jboss.as.test.shared.CliUtils;
import org.jboss.as.test.shared.ServerReload;
import org.jboss.dmr.ModelNode;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.wildfly.security.ssl.X509RevocationTrustManager;
import org.wildfly.security.x500.GeneralName;
import org.wildfly.security.x500.cert.AccessDescription;
import org.wildfly.security.x500.cert.AuthorityInformationAccessExtension;
import org.wildfly.security.x500.cert.BasicConstraintsExtension;
import org.wildfly.security.x500.cert.ExtendedKeyUsageExtension;
import org.wildfly.security.x500.cert.SelfSignedX509CertificateAndSigningKey;
import org.wildfly.security.x500.cert.X509CertificateBuilder;
import org.wildfly.test.integration.elytron.certs.CommonBase;
import org.wildfly.test.security.common.AbstractElytronSetupTask;
import org.wildfly.test.security.common.elytron.ConfigurableElement;
import org.wildfly.test.security.common.elytron.CredentialReference;
import org.wildfly.test.security.common.elytron.Path;
import org.wildfly.test.security.common.elytron.SimpleKeyManager;
import org.wildfly.test.security.common.elytron.SimpleKeyStore;
import org.wildfly.test.security.common.elytron.SimpleServerSslContext;
import org.wildfly.test.security.common.elytron.UndertowSslContext;

/**
 * Tests OCSP stapling end to end: the server (JDK server-side stapling enabled through the
 * {@code jdk.tls.server.enableStatusRequestExtension} system property) staples the OCSP response for its certificate,
 * and a client using {@link X509RevocationTrustManager} with {@code setAcceptOCSPStapling(true)} consumes it.
 * <p>
 * The client's OCSP responder override points at a closed port, so the client has no live OCSP path. A successful
 * handshake with a good certificate (soft-fail disabled) can therefore only be explained by the stapled response, and
 * a rejected handshake with a revoked certificate (soft-fail enabled) can only be explained by the stapled
 * {@code REVOKED} status. The responder that the <em>server</em> queries is a {@link SimpleOcspServer} reachable through
 * the AIA extension of the server certificates.
 * <p>
 * The test creates its own CA, responder certificate and server certificates, so it does not depend on
 * {@link OcspTestBase} static state or on test order: every test selects the server key store it needs.
 */
@RunWith(Arquillian.class)
@RunAsClient
@ServerSetup(OcspStaplingTestCase.OcspStaplingServerSetup.class)
public class OcspStaplingTestCase extends CommonBase {

    private static final String STAPLING_PROPERTY = "jdk.tls.server.enableStatusRequestExtension";
    private static final String PASSWORD = "Elytron";
    private static final char[] PASSWORD_CHAR = PASSWORD.toCharArray();
    private static final int HTTPS_PORT = 8443;

    private static final String GOOD_KEY_STORE = "ocspStaplingGoodKeyStore";
    private static final String REVOKED_KEY_STORE = "ocspStaplingRevokedKeyStore";
    private static final String KEY_MANAGER = "ocspStaplingKeyManager";
    private static final String SSL_CONTEXT = "ocspStaplingSslContext";

    private static final File WORKING_DIR = new File("." + File.separator + "target" + File.separator + "ocsp-stapling");
    private static final File GOOD_FILE = new File(WORKING_DIR, "ocsp-stapling-good.keystore");
    private static final File REVOKED_FILE = new File(WORKING_DIR, "ocsp-stapling-revoked.keystore");

    private static SimpleOcspServer ocspServer;
    private static KeyStore caTrustStore;
    private static X509Certificate responderCertificate;

    /**
     * A good certificate is accepted: the server staples a GOOD response, the client cannot reach any responder and
     * soft-fail is disabled, so the handshake only succeeds if the staple is used.
     */
    @Test
    public void testStapledGoodAccepted() throws Exception {
        selectServerKeyStore(GOOD_KEY_STORE);
        Assert.assertNull("Handshake with a good certificate must succeed using the stapled response",
                handshake(false));
    }

    /**
     * A revoked certificate is rejected: soft-fail is enabled and the client has no live OCSP path, so the handshake
     * is only rejected if the stapled REVOKED response is used.
     */
    @Test
    public void testStapledRevokedRejected() throws Exception {
        selectServerKeyStore(REVOKED_KEY_STORE);
        SSLException failure = handshake(true);
        Assert.assertNotNull("Handshake with a revoked certificate must be rejected using the stapled response", failure);
        // With soft-fail enabled and no reachable responder, only a stapled REVOKED status can fail the handshake
        // for revocation, so the cause chain must say so.
        Assert.assertTrue("Handshake must fail because the certificate is revoked, but was: " + failure,
                isRevocationFailure(failure));
    }

    private static boolean isRevocationFailure(Throwable failure) {
        for (Throwable t = failure; t != null; t = t.getCause()) {
            if (t instanceof CertificateRevokedException) {
                return true;
            }
            if (t instanceof CertPathValidatorException
                    && ((CertPathValidatorException) t).getReason() == CertPathValidatorException.BasicReason.REVOKED) {
                return true;
            }
        }
        return false;
    }

    /**
     * Points the server key manager at the given key store and reloads so the change is applied.
     */
    private void selectServerKeyStore(String keyStoreName) throws Exception {
        try (CLIWrapper cli = new CLIWrapper(true)) {
            try {
                cli.sendLine(String.format(
                        "/subsystem=elytron/key-manager=%s:write-attribute(name=key-store, value=%s)",
                        KEY_MANAGER, keyStoreName));
            } finally {
                ServerReload.executeReloadAndWaitForCompletion(managementClient);
            }
        }
    }

    /**
     * Connects to the server with a stapling-accepting client whose OCSP responder is a dead port.
     *
     * @return the exception that failed the handshake, or {@code null} when the handshake succeeded
     */
    private SSLException handshake(boolean softFail) throws Exception {
        int deadOcspPort;
        try (ServerSocket reserved = new ServerSocket(0)) {
            deadOcspPort = reserved.getLocalPort();
        }

        TrustManagerFactory tmf = TrustManagerFactory.getInstance("PKIX");
        TrustManager trustManager = X509RevocationTrustManager.builder()
                .setTrustManagerFactory(tmf)
                .setTrustStore(caTrustStore)
                .setOcspResponderCert(responderCertificate)
                .setResponderURI(new URI("http://localhost:" + deadOcspPort + "/ocsp"))
                .setSoftFail(softFail)
                .setAcceptOCSPStapling(true)
                .build();
        SSLContext clientContext = SSLContext.getInstance("TLS");
        clientContext.init(null, new TrustManager[] {trustManager}, null);

        try (SSLSocket socket = (SSLSocket) clientContext.getSocketFactory()
                .createSocket(InetAddress.getLoopbackAddress(), HTTPS_PORT)) {
            socket.setSoTimeout(30000);
            socket.startHandshake();
            return null;
        } catch (SSLException e) {
            return e;
        }
    }

    /**
     * This deployment is required just so the server setup task is executed.
     */
    @Deployment
    protected static WebArchive createDeployment() {
        return ShrinkWrap.create(WebArchive.class, "dummy.war");
    }

    @ArquillianResource
    protected URL url;

    /**
     * Creates the CA and certificates, starts the responder, configures the server SSL context and enables stapling.
     */
    static class OcspStaplingServerSetup extends AbstractElytronSetupTask {

        @Override
        protected void setup(ModelControllerClient modelControllerClient) throws Exception {
            prepareCertificatesAndResponder();

            super.setup(modelControllerClient);

            ModelNode address = new ModelNode().add("system-property", STAPLING_PROPERTY);
            ModelNode add = Operations.createAddOperation(address);
            add.get("value").set("true");
            ModelNode result = modelControllerClient.execute(add);
            if (!Operations.isSuccessfulOutcome(result)) {
                throw new IllegalStateException("Unable to add system property: " + Operations.getFailureDescription(result));
            }
            // The property is only read by the JDK when the server SSL context is created, so apply it with a reload.
            ServerReload.executeReloadAndWaitForCompletion(modelControllerClient);
        }

        @Override
        protected void tearDown(ModelControllerClient modelControllerClient) throws Exception {
            try {
                super.tearDown(modelControllerClient);
            } finally {
                try {
                    ModelNode remove = Operations.createRemoveOperation(
                            new ModelNode().add("system-property", STAPLING_PROPERTY));
                    ModelNode result = modelControllerClient.execute(remove);
                    if (!Operations.isSuccessfulOutcome(result)) {
                        throw new IllegalStateException(
                                "Unable to remove system property: " + Operations.getFailureDescription(result));
                    }
                    ServerReload.executeReloadAndWaitForCompletion(modelControllerClient);
                } finally {
                    if (ocspServer != null) {
                        ocspServer.stop();
                        ocspServer = null;
                    }
                    GOOD_FILE.delete();
                    REVOKED_FILE.delete();
                    WORKING_DIR.delete();
                }
            }
        }

        @Override
        protected ConfigurableElement[] getConfigurableElements() {
            LinkedList<ConfigurableElement> elements = new LinkedList<>();
            CredentialReference credRef = CredentialReference.builder().withClearText(PASSWORD).build();

            SimpleKeyStore goodKeyStore = SimpleKeyStore.builder().withName(GOOD_KEY_STORE)
                    .withCredentialReference(credRef).withType("JKS")
                    .withPath(Path.builder().withPath(CliUtils.asAbsolutePath(GOOD_FILE)).build()).build();
            elements.add(goodKeyStore);

            SimpleKeyStore revokedKeyStore = SimpleKeyStore.builder().withName(REVOKED_KEY_STORE)
                    .withCredentialReference(credRef).withType("JKS")
                    .withPath(Path.builder().withPath(CliUtils.asAbsolutePath(REVOKED_FILE)).build()).build();
            elements.add(revokedKeyStore);

            // Starts on the good key store; each test selects the one it needs.
            SimpleKeyManager keyManager = SimpleKeyManager.builder().withName(KEY_MANAGER)
                    .withKeyStore(goodKeyStore.getName()).withCredentialReference(credRef).build();
            elements.add(keyManager);

            SimpleServerSslContext sslContext = SimpleServerSslContext.builder().withName(SSL_CONTEXT)
                    .withKeyManagers(keyManager.getName()).build();
            elements.add(sslContext);

            elements.add(UndertowSslContext.builder().withHttpsListener("https").withName(sslContext.getName()).build());

            return elements.toArray(new ConfigurableElement[] {});
        }

        private static void prepareCertificatesAndResponder() throws Exception {
            WORKING_DIR.mkdirs();
            // SimpleOcspServer signs responses with the BouncyCastle provider.
            Security.addProvider(new BouncyCastleProvider());

            int ocspPort;
            try (ServerSocket reserved = new ServerSocket(0)) {
                ocspPort = reserved.getLocalPort();
            }
            String responderUrl = "http://localhost:" + ocspPort + "/ocsp";

            KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("RSA");
            X500Principal caDn = new X500Principal("CN=Elytron OCSP Stapling CA, O=Elytron, C=UK");

            SelfSignedX509CertificateAndSigningKey ca = SelfSignedX509CertificateAndSigningKey.builder()
                    .setDn(caDn)
                    .setKeyAlgorithmName("RSA")
                    .setSignatureAlgorithmName("SHA256withRSA")
                    .addExtension(false, "BasicConstraints", "CA:true,pathlen:2147483647")
                    .build();
            X509Certificate caCertificate = ca.getSelfSignedCertificate();

            caTrustStore = KeyStore.getInstance("JKS");
            caTrustStore.load(null, null);
            caTrustStore.setCertificateEntry("ca", caCertificate);

            // OCSP responder certificate
            KeyPair responderKeys = keyPairGenerator.generateKeyPair();
            PrivateKey responderKey = responderKeys.getPrivate();
            responderCertificate = new X509CertificateBuilder()
                    .setIssuerDn(caDn)
                    .setSubjectDn(new X500Principal("OU=Elytron, O=Elytron, C=UK, CN=OcspStaplingResponder"))
                    .setSignatureAlgorithmName("SHA256withRSA")
                    .setSigningKey(ca.getSigningKey())
                    .setPublicKey(responderKeys.getPublic())
                    .setSerialNumber(new BigInteger("201"))
                    .addExtension(new BasicConstraintsExtension(false, false, -1))
                    .addExtension(new ExtendedKeyUsageExtension(false, Collections.singletonList(OID_KP_OCSP_SIGNING)))
                    .build();

            X509Certificate goodCertificate = createServerKeyStore(keyPairGenerator, ca, caDn, caCertificate,
                    responderUrl, new BigInteger("202"), GOOD_FILE);
            X509Certificate revokedCertificate = createServerKeyStore(keyPairGenerator, ca, caDn, caCertificate,
                    responderUrl, new BigInteger("203"), REVOKED_FILE);

            ocspServer = new SimpleOcspServer(ocspPort, responderCertificate, responderKey);
            ocspServer.addCertificate(caCertificate, goodCertificate, SimpleOcspServer.RevocationStatus.GOOD);
            ocspServer.addCertificate(caCertificate, revokedCertificate, SimpleOcspServer.RevocationStatus.REVOKED);
            ocspServer.start();
        }

        private static X509Certificate createServerKeyStore(KeyPairGenerator keyPairGenerator,
                SelfSignedX509CertificateAndSigningKey ca, X500Principal caDn, X509Certificate caCertificate,
                String responderUrl, BigInteger serial, File file) throws Exception {
            KeyPair keys = keyPairGenerator.generateKeyPair();
            X509Certificate certificate = new X509CertificateBuilder()
                    .setIssuerDn(caDn)
                    .setSubjectDn(new X500Principal("OU=Elytron, O=Elytron, C=UK, CN=localhost"))
                    .setSignatureAlgorithmName("SHA256withRSA")
                    .setSigningKey(ca.getSigningKey())
                    .setPublicKey(keys.getPublic())
                    .setSerialNumber(serial)
                    .addExtension(new BasicConstraintsExtension(false, false, -1))
                    .addExtension(new AuthorityInformationAccessExtension(Collections.singletonList(
                            new AccessDescription(OID_AD_OCSP, new GeneralName.URIName(responderUrl)))))
                    .build();
            KeyStore keyStore = KeyStore.getInstance("JKS");
            keyStore.load(null, null);
            keyStore.setKeyEntry("localhost", keys.getPrivate(), PASSWORD_CHAR,
                    new X509Certificate[] {certificate, caCertificate});
            try (FileOutputStream fos = new FileOutputStream(file)) {
                keyStore.store(fos, PASSWORD_CHAR);
            }
            return certificate;
        }
    }
}
