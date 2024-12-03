/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.test.integration.elytron.certs;

import java.io.Closeable;
import java.io.IOException;
import java.net.InetAddress;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.net.URL;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import javax.net.ssl.KeyManager;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509ExtendedKeyManager;

import org.apache.http.Header;

import org.hamcrest.MatcherAssert;
import org.jboss.as.test.integration.management.util.CLIWrapper;
import org.jboss.logging.Logger;

import org.apache.http.HttpEntity;
import org.apache.http.HttpStatus;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.conn.ssl.TrustSelfSignedStrategy;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.ssl.SSLContexts;
import org.apache.http.util.EntityUtils;
import org.hamcrest.CoreMatchers;
import org.junit.Assert;
import org.wildfly.security.auth.realm.KeyStoreBackedSecurityRealm;
import org.wildfly.security.auth.server.SecurityDomain;
import org.wildfly.security.auth.server.SecurityRealm;
import org.wildfly.security.permission.PermissionVerifier;
import org.wildfly.security.ssl.SSLContextBuilder;
import org.wildfly.security.ssl.X509RevocationTrustManager;
import org.wildfly.test.integration.elytron.certs.ocsp.OcspStaplingTestCase;
import org.wildfly.test.integration.elytron.certs.ocsp.OcspTestBase;


/**
 * Common methods that are used for both CRL and OCSP tests.
 *
 * @author Jan Stourac <jstourac@redhat.com>
 */
public class CommonBase {
    private Logger logger = Logger.getLogger(CommonBase.class);

    protected void setServerKeyStore(String value) throws Exception {
        try (CLIWrapper cli = new CLIWrapper(true)) {
            try {
                cli.sendLine(String.format(
                        "/subsystem=elytron/key-manager=serverKeyManager:write-attribute(name=key-store, value=\"%s\")",
                        value));
            } finally {
                cli.sendLine(String.format("reload"));
            }
        }
    }

    protected void setSoftFail(boolean value) throws Exception {
        try (CLIWrapper cli = new CLIWrapper(true)) {
            try {
                cli.sendLine(String.format(
                        "/subsystem=elytron/trust-manager=serverTrustManager:write-attribute(name=soft-fail, value=\"%s\")",
                        value));
            } finally {
                cli.sendLine(String.format("reload"));
            }
        }
    }

    protected void setCrl(String crlFile) throws Exception {
        try (CLIWrapper cli = new CLIWrapper(true)) {
            try {
                if (crlFile != null) {
                    cli.sendLine(String.format(
                            "/subsystem=elytron/trust-manager=serverTrustManager:write-attribute(name=certificate-revocation-list, value={path=%s})",
                            crlFile));
                } else {
                    cli.sendLine(
                            "/subsystem=elytron/trust-manager=serverTrustManager:undefine-attribute(name=certificate-revocation-list)");
                }
            } finally {
                cli.sendLine(String.format("reload"));
            }
        }
    }

    protected void setPreferCrls(Boolean preferCrls) throws Exception {
        try (CLIWrapper cli = new CLIWrapper(true)) {
            try {
                if (preferCrls != null) {
                    cli.sendLine(String.format(
                            "/subsystem=elytron/trust-manager=serverTrustManager:write-attribute(name=ocsp.prefer-crls, value=%s)",
                            preferCrls));
                } else {
                    cli.sendLine(
                            "/subsystem=elytron/trust-manager=serverTrustManager:undefine-attribute(name=ocsp.prefer-crls)");
                }
            } finally {
                cli.sendLine(String.format("reload"));
            }
        }
    }

    protected void setOcspUrl(String ocspResponderUrl) throws Exception {
        try (CLIWrapper cli = new CLIWrapper(true)) {
            try {
                if (ocspResponderUrl != null) {
                    cli.sendLine(String.format(
                            "/subsystem=elytron/trust-manager=serverTrustManager:write-attribute(name=ocsp.responder, value=%s)",
                            ocspResponderUrl));
                } else {
                    cli.sendLine(
                            "/subsystem=elytron/trust-manager=serverTrustManager:undefine-attribute(name=ocsp.responder)");
                }
            } finally {
                cli.sendLine(String.format("reload"));
            }
        }
    }

    protected void setMaxCertChain(Integer maximumCertPath) throws Exception {
        try (CLIWrapper cli = new CLIWrapper(true)) {
            try {
                if (maximumCertPath != null) {
                    cli.sendLine(String.format(
                            "/subsystem=elytron/trust-manager=serverTrustManager:write-attribute(name=maximum-cert-path, value=%s)",
                            maximumCertPath));
                } else {
                    cli.sendLine(
                            "/subsystem=elytron/trust-manager=serverTrustManager:undefine-attribute(name=maximum-cert-path)");
                }
            } finally {
                cli.sendLine(String.format("reload"));
            }
        }
    }

    protected void testCommon(KeyStore clientKeystore, KeyStore clientTruststore, String password,
            boolean expectValid) throws Exception {
        Assert.assertNotNull("Keystore for client is null!", clientKeystore);
        Assert.assertNotNull("Truststore for client is null!", clientTruststore);

        final SSLContext clientContext = createSSLContext(clientKeystore, clientTruststore, password);

        performConnectionTest(clientContext, expectValid);
    }

    protected static X509ExtendedKeyManager getKeyManager(KeyStore clientKeystore, final String password) throws Exception {
        KeyManagerFactory keyManagerFactory = KeyManagerFactory.getInstance("SunX509");
        keyManagerFactory.init(clientKeystore, password.toCharArray());

        for (KeyManager current : keyManagerFactory.getKeyManagers()) {
            if (current instanceof X509ExtendedKeyManager) {
                return (X509ExtendedKeyManager) current;
            }
        }

        throw new IllegalStateException("Unable to obtain X509ExtendedKeyManager.");
    }

    protected static SSLContext createSSLContextForOcspStapling(KeyStore clientKeystore, KeyStore clientTruststore, String password, boolean softFail) throws Exception {
        X509RevocationTrustManager.Builder revocationBuilder = X509RevocationTrustManager.builder();
        revocationBuilder.setTrustManagerFactory(TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm()));
        revocationBuilder.setTrustStore(clientTruststore);
        revocationBuilder.setCheckRevocation(true);
        revocationBuilder.setSoftFail(softFail);

        SSLContext clientContext = new SSLContextBuilder().setClientMode(true).setAcceptOCSPStapling(true)
                .setKeyManager(getKeyManager(clientKeystore, password))
                .setTrustManager(revocationBuilder.build())
                .setSecurityDomain(getKeyStoreBackedSecurityDomain("/jks/beetles.keystore"))
                .build().create();
        return clientContext;
    }
    private SSLContext createSSLContext(KeyStore clientKeystore, KeyStore clientTruststore, String password) throws Exception {
        try {
            return SSLContexts.custom().loadTrustMaterial(clientTruststore,
                    new TrustSelfSignedStrategy()).loadKeyMaterial(clientKeystore, password.toCharArray()).build();
        } catch (NoSuchAlgorithmException | KeyStoreException | KeyManagementException | UnrecoverableKeyException e) {
            throw new RuntimeException("Failed to read keystore", e);
        }
    }

    protected void performConnectionTest(SSLContext clientContext, boolean expectValid) throws Exception {
        // perform request from client to server with appropriate client ssl context (certificate)
        ExecutorService executorService = Executors.newSingleThreadExecutor();
        Future<SSLSocket> socketFuture = executorService.submit(() -> {
            try {
                logger.info("About to connect client");
                SSLSocket sslSocket =
                        (SSLSocket) clientContext.getSocketFactory().createSocket(InetAddress.getLoopbackAddress(),
                                8443);
                sslSocket.getSession();

                return sslSocket;
            } catch (Exception e) {
                throw new RuntimeException(e);
            } finally {
                logger.info("Client connected");
            }
        });

        SSLSocket clientSocket = socketFuture.get();
        SSLSession clientSession = clientSocket.getSession();

        try {
            if (expectValid) {
                Assert.assertTrue("Client SSL Session should be Valid", clientSession.isValid());
            } else {
                Assert.assertFalse("Client SSL Session should be Invalid", clientSession.isValid());
            }
        } finally {
            safeClose(clientSocket);
        }

        if (expectValid) {
            // Now check that complete HTTPS GET request can be performed successfully.
            performHttpGet(clientContext);
        }
    }

    protected void performConnectionOcspStaplingTest(SSLContext clientContext, boolean expectValid) throws Throwable {
        SSLSocket clientSocket = (SSLSocket) clientContext.getSocketFactory().createSocket(InetAddress.getLoopbackAddress(),
                8443);
        ExecutorService clientExecutorService = Executors.newSingleThreadExecutor();
        Future<byte[]> clientFuture = clientExecutorService.submit(() -> {
            try {
                byte[] received = new byte[2];
                clientSocket.getOutputStream().write(new byte[]{0x12, 0x34});
                clientSocket.getInputStream().read(received);

                Assert.assertNotNull(clientSocket.getSession().getPeerPrincipal().getName());
                Assert.assertNotEquals("TLSv1.3", clientSocket.getSession().getProtocol()); // since TLS 1.3 is not enabled by default
                return received;
            } catch (Exception e) {
                throw new RuntimeException("Client exception", e);
            }
        });
        try {
            Assert.assertNotNull(clientFuture.get());
            if (!expectValid) {
                Assert.fail("SSL connection is expected to fail but did not fail.");
            }
        } catch (Exception e) {
            if (expectValid) {
                throw e;
            }
        } finally {
            safeClose(clientSocket);
        }
    }

    protected void performHttpGet(SSLContext clientContext) throws Exception {
        URL url = new URIBuilder().setScheme("https").setHost("localhost").setPort(8443).setPath("/").build().toURL();
        performHttpGet(clientContext, url, HttpStatus.SC_OK, "Welcome to ");
    }

    protected void performHttpGet(KeyStore clientKeystore, KeyStore clientTruststore, String password,
            URL url, int expectedStatus, String containedText, Header... headers) throws Exception {
        performHttpGet(createSSLContext(clientKeystore, clientTruststore, password), url, expectedStatus, containedText, headers);
    }

    protected void performHttpGet(SSLContext clientContext, URL url, int expectedStatus, String containedText, Header... headers) throws IOException, URISyntaxException {
        HttpEntity httpEntity = null;
        CloseableHttpResponse response = null;
        int statusCode = 0;
        String responseString = "";
        try (final CloseableHttpClient httpClient = HttpClients.custom().setSSLContext(clientContext).build()) {
            HttpGet httpget = new HttpGet(url.toURI());
            httpget.setHeaders(headers);
            response = httpClient.execute(httpget);

            httpEntity = response.getEntity();
            Assert.assertNotNull("HTTP entity is null, which is not expected!", httpEntity);
            statusCode = response.getStatusLine().getStatusCode();
            responseString = EntityUtils.toString(httpEntity);
            Assert.assertEquals(expectedStatus, statusCode);
            if (expectedStatus == HttpStatus.SC_OK) {
                MatcherAssert.assertThat(responseString, CoreMatchers.containsString(containedText));
            }
        } finally {
            if (httpEntity != null) {
                EntityUtils.consumeQuietly(httpEntity);
            }
        }
    }

    private void safeClose(Closeable closeable) {
        try {
            closeable.close();
        } catch (Exception ignored) {
        }
    }

    protected static SecurityDomain getKeyStoreBackedSecurityDomain(String keyStorePath) throws Exception {
        SecurityRealm securityRealm = new KeyStoreBackedSecurityRealm(createKeyStore(keyStorePath));

        SecurityDomain.Builder builder = SecurityDomain.builder()
                .addRealm("KeystoreRealm", securityRealm)
                .build()
                .setDefaultRealmName("KeystoreRealm")
                .setPreRealmRewriter((String s) -> s.toLowerCase(Locale.ENGLISH))
                .setPermissionMapper((permissionMappable, roles) -> PermissionVerifier.ALL);
//        builder.setPrincipalDecoder(new X500AttributePrincipalDecoder("2.5.4.3", 1));
        return builder.build();
    }

    private static KeyStore createKeyStore(final String path) throws Exception {
        KeyStore keyStore = KeyStore.getInstance("jks");
        try (InputStream caTrustStoreFile = OcspStaplingTestCase.class.getResourceAsStream(path)) {
            keyStore.load(caTrustStoreFile, OcspTestBase.PASSWORD_CHAR);
        }

        return keyStore;
    }
}
