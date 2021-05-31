/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2020, Red Hat, Inc., and individual contributors
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
import static org.jboss.as.test.integration.security.common.SSLTruststoreUtil.HTTPS_PORT;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.security.PrivilegedAction;
import java.security.Security;

import javax.ws.rs.ProcessingException;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.core.Response;

import org.codehaus.plexus.util.FileUtils;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.as.arquillian.api.ServerSetup;
import org.jboss.as.controller.client.ModelControllerClient;
import org.jboss.as.test.integration.management.util.CLIWrapper;
import org.jboss.as.test.integration.security.common.CoreUtils;
import org.jboss.as.test.integration.security.common.SecurityTestConstants;
import org.jboss.as.test.shared.TestSuiteEnvironment;
import org.jboss.resteasy.client.jaxrs.ResteasyClient;
import org.jboss.resteasy.client.jaxrs.ResteasyClientBuilder;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.wildfly.security.auth.client.AuthenticationContext;
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

/**
 * Smoke test for two way SSL connection with Undertow HTTPS listener backed by Elytron
 * server-ssl-context
 * with need-client-auth=true (client certificate is required) using SSLv2Hello.
 *
 * In case the client certificate is not trusted or present, the SSL handshake should fail.
 *
 * Additionally, smoke test for one way SSL connection with Undertow HTTPS listener
 * backed by Elytron server-ssl-context.
 *
 * @author Ondrej Kotek
 * @author Sonia Zaldana
 */
@RunWith(Arquillian.class)
@ServerSetup({ UndertowSSLv2HelloTestCase.ElytronSslContextInUndertowSetupTask.class, WelcomeContent.SetupTask.class})
@RunAsClient
@Ignore("WFLY-14835")
public class UndertowSSLv2HelloTestCase {

    private static final String NAME = UndertowTwoWaySslNeedClientAuthTestCase.class.getSimpleName();
    private static final File WORK_DIR = new File("target" + File.separatorChar +  NAME);
    private static final File SERVER_KEYSTORE_FILE = new File(WORK_DIR, SecurityTestConstants.SERVER_KEYSTORE);
    private static final File SERVER_TRUSTSTORE_FILE = new File(WORK_DIR, SecurityTestConstants.SERVER_TRUSTSTORE);
    private static final String PASSWORD = SecurityTestConstants.KEYSTORE_PASSWORD;
    private static final String SSLV2HELLO_CONTEXT = "SSLv2HelloContext";
    private static final String DEFAULT_CONTEXT = "DefaultContext";
    private static final String SSLV2HELLO_CONTEXT_ONE_WAY = "SSLv2HelloContextOneWay";
    private static final String HTTPS = "https";
    private static URL securedRootUrl;
    public static String disabledAlgorithms;

    // just to make server setup task work
    @Deployment(testable = false)
    public static WebArchive createDeployment() {
        return ShrinkWrap.create(WebArchive.class, NAME + ".war")
                .add(new StringAsset("index page"), "index.html");
    }

    @BeforeClass
    public static void setUp() {
        disabledAlgorithms = Security.getProperty("jdk.tls.disabledAlgorithms");
        if (disabledAlgorithms != null && (disabledAlgorithms.contains("TLSv1") || disabledAlgorithms.contains("TLSv1.1"))) {
            // reset the disabled algorithms to make sure that the protocols required in this test are available
            Security.setProperty("jdk.tls.disabledAlgorithms", "");
        }

        try {
            securedRootUrl = new URL("https", TestSuiteEnvironment.getServerAddress(), HTTPS_PORT, "");
        } catch (MalformedURLException ex) {
            throw new IllegalStateException("Unable to create HTTPS URL to server root", ex);
        }
    }

    @AfterClass
    public static void cleanUp() {
        if (disabledAlgorithms != null) {
            Security.setProperty("jdk.tls.disabledAlgorithms", disabledAlgorithms);
        }
    }

    /**
     * One way SSL - RESTEasy client sends SSLv2Hello message and server supports the protocol.
     * Handshake should succeed.
     */
    @Test
    public void testOneWayRestEasyElytronClientServerSupportsSSLv2Hello() throws Exception {

        configureSSLContext(SSLV2HELLO_CONTEXT_ONE_WAY);

        AuthenticationContext context = doPrivileged((PrivilegedAction<AuthenticationContext>) () -> {
            try {
                URL config = getClass().getResource("wildfly-config-one-way-sslv2hello.xml");
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

        restoreConfiguration();
    }

    /**
     * Two way SSL - RESTEasy client sends SSLv2Hello message and server supports the protocol.
     * Handshake should succeed.
     */
    @Test
    public void testTwoWayRestEasyElytronClientServerSupportsSSLv2Hello() throws Exception {

        configureSSLContext(SSLV2HELLO_CONTEXT);

        AuthenticationContext context = doPrivileged((PrivilegedAction<AuthenticationContext>) () -> {
            try {
                URL config = getClass().getResource("wildfly-config-sslv2hello.xml");
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

        restoreConfiguration();
    }

    /**
     * Two way SSL - Server supports SSLv2Hello, but client does not support SSLv2Hello.
     * Handshake should succeed as they still share protocol TLSv1 in common.
     */
    @Test
    public void testTwoWayRestEasyElytronClientNoSSLv2HelloSupport() throws Exception {
        configureSSLContext(SSLV2HELLO_CONTEXT);
        AuthenticationContext context = doPrivileged((PrivilegedAction<AuthenticationContext>) () -> {
            try {
                URL config = getClass().getResource("wildfly-config-no-sslv2hello.xml");
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
        restoreConfiguration();
    }

    /**
     * Two way SSL - Server does not support SSLv2Hello, but client sends SSLv2Hello message.
     * Handshake should fail.
     */
    @Test(expected = ProcessingException.class)
    public void testTwoWayRestEasyElytronServerNoSSLv2HelloSupport() throws Exception {
        configureSSLContext(DEFAULT_CONTEXT);
        AuthenticationContext context = doPrivileged((PrivilegedAction<AuthenticationContext>) () -> {
            try {
                URL config = getClass().getResource("wildfly-config-sslv2hello.xml");
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
        restoreConfiguration();
    }

    /**
     * Two Way SSL - Client and Server don't support SSLv2Hello as it has not been explicitly configured.
     * They each have their default configuration. Handshake should succeed.
     */
    @Test
    public void testTwoWayRestEasyElytronServerClientDefaultConfig() throws Exception{
        configureSSLContext(DEFAULT_CONTEXT);
        AuthenticationContext context = doPrivileged((PrivilegedAction<AuthenticationContext>) () -> {
            try {
                URL config = getClass().getResource("wildfly-config-no-sslv2hello.xml");
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
        restoreConfiguration();
    }

    private void configureSSLContext(String sslContextName) throws Exception {

        try(CLIWrapper cli = new CLIWrapper(true)) {
            cli.sendLine("batch");
            cli.sendLine(String.format("/subsystem=undertow/server=default-server/https-listener=%s:undefine-attribute" +
                    "(name=security-realm)", HTTPS));
            cli.sendLine(String.format("/subsystem=undertow/server=default-server/https-listener=%s:write-attribute" +
                    "(name=ssl-context,value=%s)", HTTPS, sslContextName));
            cli.sendLine("run-batch");
            cli.sendLine("reload");
        }
    }

    private void restoreConfiguration() throws Exception {
        try (CLIWrapper cli = new CLIWrapper(true)) {
            cli.sendLine("batch");
            cli.sendLine(String.format("/subsystem=undertow/server=default-server/https-listener=%s:undefine-attribute" +
                    "(name=ssl-context)", HTTPS));
            cli.sendLine(String.format("/subsystem=undertow/server=default-server/https-listener=%s:write-attribute" +
                    "(name=security-realm,value=ApplicationRealm)", HTTPS));
            cli.sendLine("run-batch");
            cli.sendLine("reload");
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
                    SimpleServerSslContext.builder().withName(SSLV2HELLO_CONTEXT)
                            .withKeyManagers(NAME)
                            .withTrustManagers(NAME)
                            .withNeedClientAuth(true)
                            .withProtocols("SSLv2Hello", "TLSv1")
                            .build(),
                    SimpleServerSslContext.builder().withName(DEFAULT_CONTEXT)
                            .withKeyManagers(NAME)
                            .withTrustManagers(NAME)
                            .withNeedClientAuth(true)
                            .build(),
                    SimpleServerSslContext.builder().withName(SSLV2HELLO_CONTEXT_ONE_WAY)
                            .withKeyManagers(NAME)
                            .withProtocols("SSLv2Hello", "TLSv1")
                            .build()
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
