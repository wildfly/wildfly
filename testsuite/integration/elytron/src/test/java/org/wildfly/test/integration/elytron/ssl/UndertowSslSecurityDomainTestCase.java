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

import static javax.servlet.http.HttpServletResponse.SC_FORBIDDEN;
import static javax.servlet.http.HttpServletResponse.SC_OK;
import static org.jboss.as.test.integration.security.common.SSLTruststoreUtil.HTTPS_PORT;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import org.apache.http.client.HttpClient;
import org.apache.http.impl.client.CloseableHttpClient;
import org.codehaus.plexus.util.FileUtils;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.as.arquillian.api.ServerSetup;
import org.jboss.as.controller.client.ModelControllerClient;
import org.jboss.as.test.categories.CommonCriteria;
import org.jboss.as.test.integration.security.common.CoreUtils;
import org.jboss.as.test.integration.security.common.SSLTruststoreUtil;
import org.jboss.as.test.integration.security.common.SecurityTestConstants;
import org.jboss.as.test.integration.security.common.Utils;
import org.jboss.as.test.integration.security.common.servlets.SimpleServlet;
import org.jboss.as.test.shared.TestSuiteEnvironment;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;
import org.wildfly.test.security.common.AbstractElytronSetupTask;
import org.wildfly.test.security.common.elytron.ClientCertUndertowDomainMapper;
import org.wildfly.test.security.common.elytron.ConcatenatingPrincipalDecoder;
import org.wildfly.test.security.common.elytron.ConfigurableElement;
import org.wildfly.test.security.common.elytron.ConstantPrincipalDecoder;
import org.wildfly.test.security.common.elytron.CredentialReference;
import org.wildfly.test.security.common.elytron.Path;
import org.wildfly.test.security.common.elytron.PropertyFileAuthzBasedDomain;
import org.wildfly.test.security.common.elytron.SimpleKeyManager;
import org.wildfly.test.security.common.elytron.SimpleKeyStore;
import org.wildfly.test.security.common.elytron.KeyStoreRealm;
import org.wildfly.test.security.common.elytron.SimpleServerSslContext;
import org.wildfly.test.security.common.elytron.SimpleTrustManager;
import org.wildfly.test.security.common.elytron.X500AttributePrincipalDecoder;
import org.wildfly.test.security.common.elytron.UndertowSslContext;
import org.wildfly.test.security.common.elytron.UserWithRoles;

/**
 * Smoke tests for certificate based authentication using Elytron server-ssl-context, security domain,
 * and key store realm.
 *
 * This test case is preparation and temporary replacement for
 * testsuite/integration/web/src/test/java/org/jboss/as/test/integration/web/security/cert/WebSecurityCERTTestCase.java
 * before making it work with Elytron.
 *
 * @author Ondrej Kotek
 */
@RunWith(Arquillian.class)
@ServerSetup({ UndertowSslSecurityDomainTestCase.ElytronSslContextInUndertowSetupTask.class })
@RunAsClient
@Category(CommonCriteria.class)
public class UndertowSslSecurityDomainTestCase {

    private static final String NAME = UndertowSslSecurityDomainTestCase.class.getSimpleName();
    private static final File WORK_DIR = new File("target" + File.separatorChar +  NAME);
    private static final File SERVER_KEYSTORE_FILE = new File(WORK_DIR, SecurityTestConstants.SERVER_KEYSTORE);
    private static final File SERVER_TRUSTSTORE_FILE = new File(WORK_DIR, SecurityTestConstants.SERVER_TRUSTSTORE);
    private static final File CLIENT_KEYSTORE_FILE = new File(WORK_DIR, SecurityTestConstants.CLIENT_KEYSTORE);
    private static final File CLIENT_TRUSTSTORE_FILE = new File(WORK_DIR, SecurityTestConstants.CLIENT_TRUSTSTORE);
    private static final File UNTRUSTED_STORE_FILE = new File(WORK_DIR, SecurityTestConstants.UNTRUSTED_KEYSTORE);
    private static final String PASSWORD = SecurityTestConstants.KEYSTORE_PASSWORD;

    private static URL securedUrl;
    private static URL securedUrlRole1;
    private static URL securedUrlRole2;

    /**
     * Creates WAR with a secured servlet and CLIENT-CERT authentication configured in web.xml deployment descriptor.
     */
    @Deployment(testable = false)
    public static WebArchive createDeployment() {
        return ShrinkWrap.create(WebArchive.class, NAME + ".war")
                .addClass(SimpleServlet.class)
                .addAsWebInfResource(UndertowSslSecurityDomainTestCase.class.getPackage(), NAME + "-web.xml", "web.xml")
                .addAsWebInfResource(UndertowSslSecurityDomainTestCase.class.getPackage(), NAME + "-jboss-web.xml", "jboss-web.xml");
    }

    @BeforeClass
    public static void setSecuredRootUrl() throws Exception {
        try {
            securedUrl = new URL("https", TestSuiteEnvironment.getServerAddress(), HTTPS_PORT, "/" + NAME + "/");
            securedUrlRole1 = new URL("https", TestSuiteEnvironment.getServerAddress(), HTTPS_PORT, "/" + NAME + "/role1");
            securedUrlRole2 = new URL("https", TestSuiteEnvironment.getServerAddress(), HTTPS_PORT, "/" + NAME + "/role2");
        } catch (MalformedURLException ex) {
            throw new IllegalStateException("Unable to create HTTPS URL to server root", ex);
        }
    }

    /**
     * Tests access to resource that does not require authentication.
     */
    @Test
    public void testUnprotectedAccess() {
        HttpClient client = SSLTruststoreUtil
                .getHttpClientWithSSL(CLIENT_KEYSTORE_FILE, PASSWORD, CLIENT_TRUSTSTORE_FILE, PASSWORD);
        assertUnprotectedAccess(client);
        closeClient(client);
    }

    /**
     * Tests access to resource that requires authentication and authorization.
     */
    @Test
    public void testProtectedAccess() {
        HttpClient client = SSLTruststoreUtil
                .getHttpClientWithSSL(CLIENT_KEYSTORE_FILE, PASSWORD, CLIENT_TRUSTSTORE_FILE, PASSWORD);
        assertProtectedAccess(client, SC_OK);
        closeClient(client);
    }

    /**
     * Tests access to resource that requires authentication and authorization. Principal has not required role.
     */
    @Test
    public void testForbidden() {
        HttpClient client = SSLTruststoreUtil
                .getHttpClientWithSSL(CLIENT_KEYSTORE_FILE, PASSWORD, CLIENT_TRUSTSTORE_FILE, PASSWORD);
        assertAccessForbidden(client);
        closeClient(client);
    }

    /**
     * Tests access to resource that requires authentication and authorization. Client has not trusted certificate.
     */
    @Test
    public void testUntrustedCertificate() {
        HttpClient client = SSLTruststoreUtil
                .getHttpClientWithSSL(UNTRUSTED_STORE_FILE, PASSWORD, CLIENT_TRUSTSTORE_FILE, PASSWORD);
        assertProtectedAccess(client, SC_FORBIDDEN);
        closeClient(client);
    }

    private void assertUnprotectedAccess(HttpClient client) {
        try {
            Utils.makeCallWithHttpClient(securedUrl, client, SC_OK);
        } catch (IOException | URISyntaxException ex) {
            throw new IllegalStateException("Unable to request " + securedUrl.toExternalForm(), ex);
        }
    }

    private void assertProtectedAccess(HttpClient client, int expectedStatusCode) {
        try {
            Utils.makeCallWithHttpClient(securedUrlRole1, client, expectedStatusCode);
        } catch (IOException | URISyntaxException ex) {
            throw new IllegalStateException("Unable to request " + securedUrlRole1.toExternalForm(), ex);
        }
    }

    private void assertAccessForbidden(HttpClient client) {
        try {
            Utils.makeCallWithHttpClient(securedUrlRole2, client, SC_FORBIDDEN);
        } catch (IOException | URISyntaxException ex) {
            throw new IllegalStateException("Unable to request " + securedUrlRole2.toExternalForm(), ex);
        }
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
                KeyStoreRealm.builder().withName(NAME)
                        .withKeyStore(NAME + SecurityTestConstants.SERVER_TRUSTSTORE)
                        .build(),
                ConstantPrincipalDecoder.builder().withName(NAME + "constant").withConstant("CN").build(),
                X500AttributePrincipalDecoder.builder().withName(NAME + "X500")
                        .withOid("2.5.4.3")
                        .withMaximumSegments(1)
                        .build(),
                ConcatenatingPrincipalDecoder.builder().withName(NAME)
                        .withJoiner("=")
                        .withDecoders(NAME + "constant", NAME + "X500")
                        .build(),
                PropertyFileAuthzBasedDomain.builder().withName(NAME)
                        .withAuthnRealm(NAME)
                        .withPrincipalDecoder(NAME)
                        .withUser(UserWithRoles.builder().withName("CN=client").withRoles("Role1").build())
                        .build(),
                ClientCertUndertowDomainMapper.builder().withName(NAME).withSecurityDomain(NAME).build(),
                SimpleServerSslContext.builder().withName(NAME)
                        .withKeyManagers(NAME)
                        .withTrustManagers(NAME)
                        .withSecurityDomain(NAME)
                        .withAuthenticationOptional(true)
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
