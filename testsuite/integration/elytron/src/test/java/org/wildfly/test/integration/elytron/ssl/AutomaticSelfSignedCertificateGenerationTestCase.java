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

import static javax.servlet.http.HttpServletResponse.SC_OK;
import static org.jboss.as.test.integration.security.common.SSLTruststoreUtil.HTTPS_PORT;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.security.KeyStore;
import java.security.cert.X509Certificate;

import javax.net.ssl.SSLHandshakeException;

import org.apache.http.client.HttpClient;
import org.apache.http.impl.client.CloseableHttpClient;
import org.codehaus.plexus.util.FileUtils;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.as.arquillian.api.ServerSetup;
import org.jboss.as.controller.client.ModelControllerClient;
import org.jboss.as.test.categories.CommonCriteria;
import org.jboss.as.test.integration.management.util.CLIWrapper;
import org.jboss.as.test.integration.security.common.SSLTruststoreUtil;
import org.jboss.as.test.integration.security.common.SecurityTestConstants;
import org.jboss.as.test.integration.security.common.Utils;
import org.jboss.as.test.integration.security.common.servlets.SimpleServlet;
import org.jboss.as.test.shared.TestSuiteEnvironment;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;
import org.wildfly.test.security.common.AbstractElytronSetupTask;
import org.wildfly.test.security.common.elytron.ConfigurableElement;
import org.wildfly.test.security.common.elytron.CredentialReference;
import org.wildfly.test.security.common.elytron.Path;
import org.wildfly.test.security.common.elytron.SimpleKeyManager;
import org.wildfly.test.security.common.elytron.SimpleKeyStore;
import org.wildfly.test.security.common.elytron.SimpleServerSslContext;
import org.wildfly.test.security.common.elytron.UndertowSslContext;

/**
 * Tests for the automatic creation of a lazily generated self-signed certificate when Elytron
 * is in use.
 *
 * @author <a href="mailto:fjuma@redhat.com">Farah Juma</a>
 */
@RunWith(Arquillian.class)
@ServerSetup({ AutomaticSelfSignedCertificateGenerationTestCase.ElytronAndUndertowSetupTask.class })
@RunAsClient
@Category(CommonCriteria.class)
public class AutomaticSelfSignedCertificateGenerationTestCase {

    private static final String NAME = AutomaticSelfSignedCertificateGenerationTestCase.class.getSimpleName();
    private static final File WORK_DIR = new File("target" + File.separatorChar +  NAME);
    private static final File SERVER_KEYSTORE_FILE = new File(WORK_DIR, SecurityTestConstants.SERVER_KEYSTORE);
    private static final File CLIENT_TRUSTSTORE_FILE = new File(WORK_DIR, SecurityTestConstants.CLIENT_TRUSTSTORE);
    private static final String PASSWORD = SecurityTestConstants.KEYSTORE_PASSWORD;
    private static final String GENERATE_SELF_SIGNED_CERTIFICATE_HOST="customHostName";
    private static final String SERVER_ALIAS = "server";

    @Deployment(testable = false)
    public static WebArchive createDeployment() {
        return ShrinkWrap.create(WebArchive.class, NAME + ".war").addClasses(SimpleServlet.class);
    }

    @Test
    public void testSelfSignedCertificateGenerated() throws Exception {
        final URL servletUrl = new URL("https", TestSuiteEnvironment.getServerAddress(), HTTPS_PORT,
                "/" + NAME + "/" + SimpleServlet.SERVLET_PATH.substring(1));
        HttpClient client = SSLTruststoreUtil.getHttpClientWithSSL(null, null);
        try {
            assertFalse(SERVER_KEYSTORE_FILE.exists()); // keystore doesn't exist initially
            try {
                // attempt to access the https interface
                Utils.makeCallWithHttpClient(servletUrl, client, SC_OK);
            } catch (SSLHandshakeException expected) {
            }
            // keystore should now exist and should contain a self-signed certificate
            assertTrue(SERVER_KEYSTORE_FILE.exists());
            X509Certificate serverCert;
            try (FileInputStream is = new FileInputStream(SERVER_KEYSTORE_FILE.getAbsolutePath())) {
                KeyStore keyStore = KeyStore.getInstance("PKCS12");
                keyStore.load(is, PASSWORD.toCharArray());
                assertEquals(1, keyStore.size());
                serverCert = (X509Certificate) keyStore.getCertificate(SERVER_ALIAS);
                assertEquals(serverCert.getSubjectX500Principal(), serverCert.getIssuerX500Principal());
                assertEquals("CN=" + GENERATE_SELF_SIGNED_CERTIFICATE_HOST, serverCert.getSubjectX500Principal().getName());
            }

            // add the server's newly generated certificate to the client's truststore
            try (FileOutputStream fos = new FileOutputStream(CLIENT_TRUSTSTORE_FILE)) {
                KeyStore trustStore = KeyStore.getInstance("PKCS12");
                trustStore.load(null, null);
                trustStore.setCertificateEntry("server", serverCert);
                trustStore.store(fos, PASSWORD.toCharArray());
            }

            try {
                // attempt to access the https interface again
                client = SSLTruststoreUtil.getHttpClientWithSSL(CLIENT_TRUSTSTORE_FILE, PASSWORD);
                Utils.makeCallWithHttpClient(servletUrl, client, SC_OK);
            } catch (IOException | URISyntaxException ex) {
                throw new IllegalStateException("Unable to request server root over HTTPS", ex);
            } finally {
                if (CLIENT_TRUSTSTORE_FILE.exists()) {
                    CLIENT_TRUSTSTORE_FILE.delete();
                }
            }

            try (FileInputStream is = new FileInputStream(SERVER_KEYSTORE_FILE.getAbsolutePath())) {
                KeyStore keyStore = KeyStore.getInstance("PKCS12");
                keyStore.load(is, PASSWORD.toCharArray());
                assertEquals(1, keyStore.size());
                assertEquals(serverCert, keyStore.getCertificate(SERVER_ALIAS)); // server's certificate should be unchanged
            }
        } finally {
            closeClient(client);
        }
    }

    @Test
    public void testSelfSignedCertificateNotGeneratedIfGenerateAttributeUndefined() throws Exception {
        // undefine generate-self-signed-certificate-host
        try (CLIWrapper cli = new CLIWrapper(true)) {
            cli.sendLine(String.format("/subsystem=elytron/key-manager=%s:undefine-attribute(name=generate-self-signed-certificate-host)",
                    NAME));
            cli.sendLine(String.format("reload"));
        }
        try {
            final URL servletUrl = new URL("https", TestSuiteEnvironment.getServerAddress(), HTTPS_PORT,
                    "/" + NAME + "/" + SimpleServlet.SERVLET_PATH.substring(1));
            HttpClient client = SSLTruststoreUtil.getHttpClientWithSSL(null, null);
            assertFalse(SERVER_KEYSTORE_FILE.exists()); // keystore doesn't exist
            try {
                // attempt to access the https interface
                Utils.makeCallWithHttpClient(servletUrl, client, SC_OK);
                fail("Expected SSLHandshakeException not thrown");
            } catch (SSLHandshakeException expected) {
            }
            assertFalse(SERVER_KEYSTORE_FILE.exists()); // keystore wasn't created
        } finally {
            // clean up
            try (CLIWrapper cli = new CLIWrapper(true)) {
                cli.sendLine(String.format("/subsystem=elytron/key-manager=%s:write-attribute(name=generate-self-signed-certificate-host,value=%s)",
                        NAME, GENERATE_SELF_SIGNED_CERTIFICATE_HOST));
                cli.sendLine(String.format("reload"));
            }
        }
    }

    static class ElytronAndUndertowSetupTask extends AbstractElytronSetupTask {

        @Override
        protected void setup(final ModelControllerClient modelControllerClient) throws Exception {
            if (WORK_DIR.exists()) {
                FileUtils.deleteDirectory(WORK_DIR);
            }
            WORK_DIR.mkdirs();
            super.setup(modelControllerClient);
        }

        @Override
        protected ConfigurableElement[] getConfigurableElements() {
            return new ConfigurableElement[] {
                    SimpleKeyStore.builder().withName(NAME + SecurityTestConstants.SERVER_KEYSTORE)
                            .withPath(Path.builder().withPath(SERVER_KEYSTORE_FILE.getAbsolutePath()).build())
                            .withCredentialReference(CredentialReference.builder().withClearText(PASSWORD).build())
                            .withType("PKCS12")
                            .build(),
                    SimpleKeyManager.builder().withName(NAME)
                            .withKeyStore(NAME + SecurityTestConstants.SERVER_KEYSTORE)
                            .withCredentialReference(CredentialReference.builder().withClearText(PASSWORD).build())
                            .withGenerateSelfSignedCertificateHost(GENERATE_SELF_SIGNED_CERTIFICATE_HOST)
                            .build(),
                    SimpleServerSslContext.builder().withName(NAME)
                            .withKeyManagers(NAME)
                            .build(),
                    UndertowSslContext.builder().withName(NAME).build()
            };
        }

        @Override
        protected void tearDown(ModelControllerClient modelControllerClient) throws Exception {
            super.tearDown(modelControllerClient);
            FileUtils.deleteDirectory(WORK_DIR);
        }
    }

    private void closeClient(HttpClient client) {
        try {
            ((CloseableHttpClient) client).close();
        } catch (IOException ex) {
            throw new IllegalStateException("Unable to close HTTP client", ex);
        }
    }
}
