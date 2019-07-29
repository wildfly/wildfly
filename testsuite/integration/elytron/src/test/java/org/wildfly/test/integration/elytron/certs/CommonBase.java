/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2019, Red Hat, Inc., and individual contributors
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

package org.wildfly.test.integration.elytron.certs;

import java.io.Closeable;
import java.io.IOException;
import java.net.InetAddress;
import java.net.URI;
import java.net.URISyntaxException;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.SSLSocket;

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
import org.junit.Assert;

/**
 * Common methods that are used for both CRL and OCSP tests.
 *
 * @author Jan Stourac <jstourac@redhat.com>
 */
public class CommonBase {
    private Logger logger = Logger.getLogger(CommonBase.class);

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

        final SSLContext clientContext;
        try {
            clientContext = SSLContexts.custom().loadTrustMaterial(clientTruststore,
                    new TrustSelfSignedStrategy()).loadKeyMaterial(clientKeystore, password.toCharArray()).build();
        } catch (NoSuchAlgorithmException | KeyStoreException | KeyManagementException | UnrecoverableKeyException e) {
            throw new RuntimeException("Failed to read keystore", e);
        }

        performConnectionTest(clientContext, expectValid);
    }

    private void performConnectionTest(SSLContext clientContext, boolean expectValid) throws Exception {
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

    private void performHttpGet(SSLContext clientContext) throws IOException, URISyntaxException {
        HttpEntity httpEntity = null;
        CloseableHttpResponse response = null;
        int statusCode = 0;
        String responseString = "";
        try (final CloseableHttpClient httpClient = HttpClients.custom().setSSLContext(clientContext).build()) {
            URI uri = new URIBuilder().setScheme("https").setHost("localhost").setPort(8443).setPath("/").build();
            HttpGet httpget = new HttpGet(uri);
            response = httpClient.execute(httpget);

            httpEntity = response.getEntity();
            Assert.assertNotNull("HTTP entity is null, which is not expected!", httpEntity);
            statusCode = response.getStatusLine().getStatusCode();
            responseString = EntityUtils.toString(httpEntity);
        } finally {
            Assert.assertEquals(HttpStatus.SC_OK, statusCode);
            Assert.assertTrue(responseString.contains("Welcome to "));

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
}
