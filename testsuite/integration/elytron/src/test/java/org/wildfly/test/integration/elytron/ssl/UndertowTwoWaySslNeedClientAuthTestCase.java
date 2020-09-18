/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2017, Red Hat, Inc., and individual contributors
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

package org.wildfly.test.integration.elytron.ssl;

import static java.security.AccessController.doPrivileged;
import static javax.servlet.http.HttpServletResponse.SC_OK;
import static org.jboss.as.test.integration.security.common.SSLTruststoreUtil.HTTPS_PORT;
import static org.junit.Assert.fail;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.SocketException;
import java.net.URISyntaxException;
import java.net.URL;
import java.security.AccessController;
import java.security.GeneralSecurityException;
import java.security.PrivilegedAction;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLException;
import javax.net.ssl.SSLHandshakeException;
import javax.ws.rs.ProcessingException;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.core.Response;

import org.apache.http.client.HttpClient;
import org.apache.http.impl.client.CloseableHttpClient;
import org.codehaus.plexus.util.FileUtils;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.as.arquillian.api.ServerSetup;
import org.jboss.as.controller.client.ModelControllerClient;
import org.jboss.as.test.integration.security.common.CoreUtils;
import org.jboss.as.test.integration.security.common.SSLTruststoreUtil;
import org.jboss.as.test.integration.security.common.SecurityTestConstants;
import org.jboss.as.test.integration.security.common.Utils;
import org.jboss.as.test.shared.TestSuiteEnvironment;
import org.jboss.resteasy.client.jaxrs.BasicAuthentication;
import org.jboss.resteasy.client.jaxrs.ResteasyClient;
import org.jboss.resteasy.client.jaxrs.ResteasyClientBuilder;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.wildfly.security.auth.client.AuthenticationContext;
import org.wildfly.security.auth.client.AuthenticationContextConfigurationClient;
import org.wildfly.security.auth.client.ElytronXmlParser;
import org.wildfly.security.auth.client.InvalidAuthenticationConfigurationException;
import org.wildfly.test.integration.elytron.util.WelcomeContent;
import org.wildfly.test.security.common.AbstractElytronSetupTask;
import org.wildfly.test.security.common.elytron.ConfigurableElement;
import org.wildfly.test.security.common.elytron.CredentialReference;
import org.wildfly.test.security.common.elytron.Path;
import org.wildfly.test.security.common.elytron.SimpleKeyManager;
import org.wildfly.test.security.common.elytron.SimpleKeyStore;
import org.wildfly.test.security.common.elytron.SimpleServerSslContext;
import org.wildfly.test.security.common.elytron.SimpleTrustManager;
import org.wildfly.test.security.common.elytron.UndertowSslContext;

/**
 * Smoke test for two way SSL connection with Undertow HTTPS listener backed by Elytron server-ssl-context
 * with need-client-auth=true (client certificate is required).
 *
 * In case the client certificate is not trusted or present, the SSL handshake should fail.
 *
 * @author Ondrej Kotek
 */
@RunWith(Arquillian.class)
@ServerSetup({ UndertowTwoWaySslNeedClientAuthTestCase.ElytronSslContextInUndertowSetupTask.class, WelcomeContent.SetupTask.class })
@RunAsClient
public class UndertowTwoWaySslNeedClientAuthTestCase {

    private static final String NAME = UndertowTwoWaySslNeedClientAuthTestCase.class.getSimpleName();
    private static final File WORK_DIR = new File("target" + File.separatorChar +  NAME);
    private static final File SERVER_KEYSTORE_FILE = new File(WORK_DIR, SecurityTestConstants.SERVER_KEYSTORE);
    private static final File SERVER_TRUSTSTORE_FILE = new File(WORK_DIR, SecurityTestConstants.SERVER_TRUSTSTORE);
    private static final File CLIENT_KEYSTORE_FILE = new File(WORK_DIR, SecurityTestConstants.CLIENT_KEYSTORE);
    private static final File CLIENT_TRUSTSTORE_FILE = new File(WORK_DIR, SecurityTestConstants.CLIENT_TRUSTSTORE);
    private static final File UNTRUSTED_STORE_FILE = new File(WORK_DIR, SecurityTestConstants.UNTRUSTED_KEYSTORE);
    private static final String PASSWORD = SecurityTestConstants.KEYSTORE_PASSWORD;

    private static URL securedRootUrl;

    // just to make server setup task work
    @Deployment(testable = false)
    public static WebArchive createDeployment() {
        return ShrinkWrap.create(WebArchive.class, NAME + ".war")
                .add(new StringAsset("index page"), "index.html");
    }

    @BeforeClass
    public static void setSecuredRootUrl() throws Exception {
        try {
            securedRootUrl = new URL("https", TestSuiteEnvironment.getServerAddress(), HTTPS_PORT, "");
        } catch (MalformedURLException ex) {
            throw new IllegalStateException("Unable to create HTTPS URL to server root", ex);
        }
    }

    @Test
    public void testSendingTrustedClientCertificate() {
        HttpClient client = SSLTruststoreUtil
                .getHttpClientWithSSL(CLIENT_KEYSTORE_FILE, PASSWORD, CLIENT_TRUSTSTORE_FILE, PASSWORD);
        assertConnectionToServer(client, SC_OK);
        closeClient(client);
    }

    @Test
    public void testSendingNonTrustedClientCertificateFails() {
        HttpClient client = SSLTruststoreUtil
                .getHttpClientWithSSL(UNTRUSTED_STORE_FILE, PASSWORD, CLIENT_TRUSTSTORE_FILE, PASSWORD);
        assertSslHandshakeFails(client);
        closeClient(client);
    }

    @Test
    public void testSendingNoClientCertificateFails() {
        HttpClient client = SSLTruststoreUtil.getHttpClientWithSSL(CLIENT_TRUSTSTORE_FILE, PASSWORD);
        assertSslHandshakeFails(client);
        closeClient(client);
    }

    /**
     * RESTEasy client loads truststore from Elytron client configuration. This truststore contains correct server certificate.
     */
    @Test
    public void testResteasyElytronClientTrustedServer() {
        AuthenticationContext context = doPrivileged((PrivilegedAction<AuthenticationContext>) () -> {
            try {
                URL config = getClass().getResource("wildfly-config-correct-truststore.xml");
                return ElytronXmlParser.parseAuthenticationClientConfiguration(config.toURI()).create();
            } catch (Throwable t) {
                throw new InvalidAuthenticationConfigurationException(t);
            }
        });
        context.run(() -> {
            ResteasyClientBuilder resteasyClientBuilder = (ResteasyClientBuilder) ClientBuilder.newBuilder().hostnameVerifier((s, sslSession) -> true);
            ResteasyClient client = resteasyClientBuilder.build();
            Response response = client.target(String.valueOf(securedRootUrl)).request().get();
            Assert.assertEquals(200, response.getStatus());
        });
    }

    /**
     * RESTEasy client loads SSL Context from Elytron client config.
     * This SSL Context does not have truststore configured, so exception is expected.
     */
    @Test(expected = ProcessingException.class)
    public void testResteasyElytronClientMissingTruststore() {
        AuthenticationContext context = doPrivileged((PrivilegedAction<AuthenticationContext>) () -> {
            try {
                URL config = getClass().getResource("wildfly-config-correct-truststore-missing.xml");
                return ElytronXmlParser.parseAuthenticationClientConfiguration(config.toURI()).create();
            } catch (Throwable t) {
                throw new InvalidAuthenticationConfigurationException(t);
            }
        });
        context.run(() -> {
            ResteasyClientBuilder resteasyClientBuilder = (ResteasyClientBuilder) ClientBuilder.newBuilder();
            ResteasyClient client = resteasyClientBuilder.build();
            Response response = client.target(String.valueOf(securedRootUrl)).request().get();
            Assert.assertEquals("Hello World!", response.readEntity(String.class));
            Assert.assertEquals(200, response.getStatus());
        });
    }

    /**
     * Elytron client has configured truststore that does not contain server's certificate.
     * Test will pass because Elytron config is ignored since different ssl context is specified on RESTEasy client builder specifically.
     */
    @Test
    public void testClientConfigProviderSSLContextIgnoredIfDifferentIsSet() throws URISyntaxException, GeneralSecurityException {
        AuthenticationContextConfigurationClient AUTH_CONTEXT_CLIENT =
                AccessController.doPrivileged((PrivilegedAction<AuthenticationContextConfigurationClient>) AuthenticationContextConfigurationClient::new);

        AuthenticationContext context = doPrivileged((PrivilegedAction<AuthenticationContext>) () -> {
            try {
                URL config = getClass().getResource("wildfly-config-correct-truststore-missing.xml");
                return ElytronXmlParser.parseAuthenticationClientConfiguration(config.toURI()).create();
            } catch (Throwable t) {
                throw new InvalidAuthenticationConfigurationException(t);
            }
        });
        AuthenticationContext contextWithTruststore = doPrivileged((PrivilegedAction<AuthenticationContext>) () -> {
            try {
                URL config = getClass().getResource("wildfly-config-correct-truststore.xml");
                return ElytronXmlParser.parseAuthenticationClientConfiguration(config.toURI()).create();
            } catch (Throwable t) {
                throw new InvalidAuthenticationConfigurationException(t);
            }
        });
        SSLContext sslContext = AUTH_CONTEXT_CLIENT.getSSLContext(securedRootUrl.toURI(), contextWithTruststore);
        context.run(() -> {
            ResteasyClientBuilder resteasyClientBuilder = (ResteasyClientBuilder) ClientBuilder.newBuilder();
            resteasyClientBuilder.sslContext(sslContext).hostnameVerifier((s, sslSession) -> true);
            ResteasyClient client = resteasyClientBuilder.build();
            Response response = client.target(String.valueOf(securedRootUrl)).request().get();
            Assert.assertEquals(200, response.getStatus());
        });
    }

    /**
     * Test situation when credentials are set on RESTEeasy client, but truststore is part of SSLContext configured for Elytron client.
     * Test that Elytron SSLContext will be used successfully.
     */
    @Test
    public void testClientConfigProviderSSLContextIsSuccessfulWhenBasicSetOnRESTEasy() {
        AuthenticationContext context = doPrivileged((PrivilegedAction<AuthenticationContext>) () -> {
            try {
                URL config = getClass().getResource("wildfly-config-correct-truststore.xml");
                return ElytronXmlParser.parseAuthenticationClientConfiguration(config.toURI()).create();
            } catch (Throwable t) {
                throw new InvalidAuthenticationConfigurationException(t);
            }
        });
        context.run(() -> {
            ResteasyClientBuilder resteasyClientBuilder = (ResteasyClientBuilder) ClientBuilder.newBuilder();
            resteasyClientBuilder.hostnameVerifier((s, sslSession) -> true);
            ResteasyClient client = resteasyClientBuilder.build();
            client.register(new BasicAuthentication("randomName", "randomPass"));
            Response response = client.target(String.valueOf(securedRootUrl)).request().get();
            Assert.assertEquals(200, response.getStatus());
        });
    }

    /**
     * Test that RESTEasy client does choose SSLContext from Elytron client based on destination of the request.
     * In this case the truststore is set for different endpoint/server and so SSL handshake will fail.
     */
    @Test(expected = ProcessingException.class)
    public void testClientConfigProviderSSLContextForDifferentHostWillNotWork() {
        AuthenticationContext context = doPrivileged((PrivilegedAction<AuthenticationContext>) () -> {
            try {
                URL config = getClass().getResource("wildfly-config-correct-truststore-different-host.xml");
                return ElytronXmlParser.parseAuthenticationClientConfiguration(config.toURI()).create();
            } catch (Throwable t) {
                throw new InvalidAuthenticationConfigurationException(t);
            }
        });
        context.run(() -> {
            ResteasyClientBuilder resteasyClientBuilder = (ResteasyClientBuilder) ClientBuilder.newBuilder().hostnameVerifier((s, sslSession) -> true);
            ResteasyClient client = resteasyClientBuilder.build();
            Response response = client.target(String.valueOf(securedRootUrl)).request().get();
            Assert.assertEquals(200, response.getStatus());
        });
    }

    /**
     * Test that RESTEasy client does choose SSLContext from Elytron client based on destination of the request.
     * In this case the truststore is set for correct endpoint/server and so SSL handshake will succeed.
     */
    @Test
    public void testClientConfigProviderSSLContextForCorrectHostWillWork() {
        AuthenticationContext context = doPrivileged((PrivilegedAction<AuthenticationContext>) () -> {
            try {
                URL config = getClass().getResource("wildfly-config-correct-truststore-correct-host.xml");
                return ElytronXmlParser.parseAuthenticationClientConfiguration(config.toURI()).create();
            } catch (Throwable t) {
                throw new InvalidAuthenticationConfigurationException(t);
            }
        });
        context.run(() -> {
            ResteasyClientBuilder resteasyClientBuilder = (ResteasyClientBuilder) ClientBuilder.newBuilder().hostnameVerifier((s, sslSession) -> true);
            ResteasyClient client = resteasyClientBuilder.build();
            Response response = client.target(String.valueOf(securedRootUrl)).request().get();
            Assert.assertEquals(200, response.getStatus());
        });
    }

    private void assertConnectionToServer(HttpClient client, int expectedStatusCode) {
        try {
            Utils.makeCallWithHttpClient(securedRootUrl, client, expectedStatusCode);
        } catch (IOException | URISyntaxException ex) {
            throw new IllegalStateException("Unable to request server root over HTTPS", ex);
        }
    }

    private void assertSslHandshakeFails(HttpClient client) {
        try {
            Utils.makeCallWithHttpClient(securedRootUrl, client, SC_OK);
        } catch (SSLHandshakeException | SocketException e) {
            // expected
            return;
        } catch (SSLException e) {
            if (e.getCause() instanceof SocketException) return; // expected
            throw new IllegalStateException("Unexpected SSLException", e);
        } catch (IOException | URISyntaxException ex) {
            throw new IllegalStateException("Unable to request server root over HTTPS", ex);
        }
        fail("SSL handshake should fail");
    }

    private void closeClient(HttpClient client) {
        try {
            ((CloseableHttpClient) client).close();
        } catch (IOException ex) {
            throw new IllegalStateException("Unable to close HTTP client", ex);
        }
    }

    /**
     * Creates Elytron server-ssl-context and key/trust stores.
     */
    static class ElytronSslContextInUndertowSetupTask extends AbstractElytronSetupTask {

        @Override
        protected void setup(final ModelControllerClient modelControllerClient) throws Exception {
            keyMaterialSetup(WORK_DIR);
            super.setup(modelControllerClient);
        }

        @Override
        protected ConfigurableElement[] getConfigurableElements() {
            return new ConfigurableElement[] {
                SimpleKeyStore.builder().withName(NAME + SecurityTestConstants.SERVER_KEYSTORE)
                        .withPath(Path.builder().withPath(SERVER_KEYSTORE_FILE.getPath()).build())
                        .withCredentialReference(CredentialReference.builder().withClearText(PASSWORD).build())
                        .build(),
                SimpleKeyStore.builder().withName(NAME + SecurityTestConstants.SERVER_TRUSTSTORE)
                        .withPath(Path.builder().withPath(SERVER_TRUSTSTORE_FILE.getPath()).build())
                        .withCredentialReference(CredentialReference.builder().withClearText(PASSWORD).build())
                        .build(),
                SimpleKeyManager.builder().withName(NAME)
                        .withKeyStore(NAME + SecurityTestConstants.SERVER_KEYSTORE)
                        .withCredentialReference(CredentialReference.builder().withClearText(PASSWORD).build())
                        .build(),
                SimpleTrustManager.builder().withName(NAME)
                        .withKeyStore(NAME + SecurityTestConstants.SERVER_TRUSTSTORE)
                        .build(),
                SimpleServerSslContext.builder().withName(NAME)
                        .withKeyManagers(NAME)
                        .withTrustManagers(NAME)
                        .withNeedClientAuth(true)
                        .build(),
                UndertowSslContext.builder().withName(NAME).build()
            };
        }

        @Override
        protected void tearDown(ModelControllerClient modelControllerClient) throws Exception {
            super.tearDown(modelControllerClient);
            FileUtils.deleteDirectory(WORK_DIR);
        }

        protected static void keyMaterialSetup(File workDir) throws Exception {
            FileUtils.deleteDirectory(workDir);
            workDir.mkdirs();
            Assert.assertTrue(workDir.exists());
            Assert.assertTrue(workDir.isDirectory());
            CoreUtils.createKeyMaterial(workDir);
        }
    }
}
