/*
 * Copyright 2020 Red Hat, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.wildfly.test.integration.elytron.certs.realm;

import java.io.File;
import java.io.OutputStream;
import java.math.BigInteger;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.KeyStore;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.List;
import javax.security.auth.x500.X500Principal;
import org.apache.http.HttpStatus;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.message.BasicHeader;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.as.arquillian.api.ServerSetup;
import org.jboss.as.arquillian.api.ServerSetupTask;
import org.jboss.as.arquillian.container.ManagementClient;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.client.ModelControllerClient;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.controller.operations.common.Util;
import org.jboss.as.test.integration.security.common.CoreUtils;
import org.jboss.as.test.integration.security.common.Utils;
import org.jboss.as.test.integration.security.common.servlets.PrincipalPrintingServlet;
import org.jboss.as.test.shared.CliUtils;
import org.jboss.as.test.shared.ServerReload;
import org.jboss.dmr.ModelNode;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.wildfly.security.x500.GeneralName;
import org.wildfly.security.x500.cert.BasicConstraintsExtension;
import org.wildfly.security.x500.cert.SelfSignedX509CertificateAndSigningKey;
import org.wildfly.security.x500.cert.SubjectAlternativeNamesExtension;
import org.wildfly.security.x500.cert.X509CertificateBuilder;
import org.wildfly.test.integration.elytron.certs.CommonBase;
import org.wildfly.test.integration.elytron.util.WelcomeContent;
import org.wildfly.test.security.common.AbstractElytronSetupTask;
import org.wildfly.test.security.common.elytron.ConfigurableElement;
import org.wildfly.test.security.common.elytron.ConstantRoleMapper;
import org.wildfly.test.security.common.elytron.CredentialReference;
import org.wildfly.test.security.common.elytron.KeyStoreRealm;
import org.wildfly.test.security.common.elytron.MechanismConfiguration;
import org.wildfly.test.security.common.elytron.Path;
import org.wildfly.test.security.common.elytron.SimpleHttpAuthenticationFactory;
import org.wildfly.test.security.common.elytron.SimpleKeyManager;
import org.wildfly.test.security.common.elytron.SimpleKeyStore;
import org.wildfly.test.security.common.elytron.SimpleSecurityDomain;
import org.wildfly.test.security.common.elytron.SimpleServerSslContext;
import org.wildfly.test.security.common.elytron.SimpleTrustManager;
import org.wildfly.test.security.common.elytron.UndertowSslContext;
import org.wildfly.test.security.common.elytron.X500AttributePrincipalDecoder;
import org.wildfly.test.undertow.common.UndertowApplicationSecurityDomain;

/**
 * <p>Test for the key-store certificate realm. It checks direct connection
 * and using proxy headers.</p>
 *
 * @author rmartinc
 */
@RunWith(Arquillian.class)
@RunAsClient
@ServerSetup({KeystoreRealmTestCase.ServerSetup.class, KeystoreRealmTestCase.ForwardingSetup.class, WelcomeContent.SetupTask.class})
public class KeystoreRealmTestCase extends CommonBase {

    protected static KeyStore trustStore;
    protected static KeyStore keyStore;
    protected static KeyStore usersStore;
    protected static KeyStore user1Store;

    protected static final String PASSWORD = "Elytron";
    private static final char[] PASSWORD_CHAR = PASSWORD.toCharArray();
    private static final String CA_JKS_LOCATION = "." + File.separator + "target" + File.separator + "test-classes" +
            File.separator + "ca" + File.separator + "jks";
    private static final File WORKING_DIR_CA = new File(CA_JKS_LOCATION);
    private static final File TRUST_FILE = new File(WORKING_DIR_CA, "ca.truststore");
    private static final File KEYSTORE_FILE = new File(WORKING_DIR_CA, "server.keystore");
    private static final File USERS_KEYSTORE_FILE = new File(WORKING_DIR_CA, "users.keystore");
    private static final File USER1_KEYSTORE_FILE = new File(WORKING_DIR_CA, "user1.keystore");
    private static final String CETIFICATE_DOMAIN = "certificateDomain";

    @ArquillianResource
    protected URL webAppURL;

    @Deployment
    protected static WebArchive createDeployment() {
        final Package currentPackage = KeystoreRealmTestCase.class.getPackage();
        return ShrinkWrap.create(WebArchive.class, KeystoreRealmTestCase.class.getSimpleName() + ".war")
                .addClasses(PrincipalPrintingServlet.class)
                .addAsWebInfResource(currentPackage, "KeystoreRealmTestCase-web.xml", "web.xml")
                .addAsWebInfResource(Utils.getJBossWebXmlAsset(CETIFICATE_DOMAIN), "jboss-web.xml");
    }

    protected URL getPrincipalServletURL() throws MalformedURLException, URISyntaxException{
        return new URIBuilder()
                .setScheme("https")
                .setHost("localhost")
                .setPort(8443)
                .setPath(webAppURL.getPath() + PrincipalPrintingServlet.SERVLET_PATH.substring(1))
                .build().toURL();
    }

    @Test
    public void testRootConnectionNoCertificate() throws Exception {
        // trustStore has no keys => ssl error
        testCommon(trustStore, trustStore, PASSWORD, false);
    }

    @Test
    public void testUserRootConnection() throws Exception {
        // user certificate should work at TLS level
        testCommon(user1Store, trustStore, PASSWORD, true);
    }

    @Test
    public void testServerRootConnection() throws Exception {
        // server certificate should work at TLS level
        testCommon(keyStore, trustStore, PASSWORD, true);
    }

    @Test
    public void testUserPrincipal() throws Exception {
        // user certificate should connect to the servlet app
        performHttpGet(user1Store, trustStore, PASSWORD, getPrincipalServletURL(), HttpStatus.SC_OK, "user1");
    }

    @Test
    public void testServerPrincipal() throws Exception {
        // the server certificate is not in the usersStore
        performHttpGet(keyStore, trustStore, PASSWORD, getPrincipalServletURL(), HttpStatus.SC_FORBIDDEN, "localhost");
    }

    @Test
    public void testUserPrincipalWithHeaders() throws Exception {
        // Using server certificate for TLS connect using proxy for user1
        Base64.Encoder encoder = Base64.getMimeEncoder();
        String cert = "-----BEGIN CERTIFICATE----- " + encoder.encodeToString(user1Store.getCertificate("user1").getEncoded()) + " -----END CERTIFICATE-----";
        cert = cert.replace("\r\n", " ");
        performHttpGet(keyStore, trustStore, PASSWORD, getPrincipalServletURL(), HttpStatus.SC_OK, "user1",
                new BasicHeader("SSL_SESSION_ID", "1633d36df6f28e1325912b46f7d214f97370c39a6b3fc24ee374a76b3f9b0fba"),
                new BasicHeader("SSL_CLIENT_CERT", cert),
                new BasicHeader("SSL_CIPHER", "ECDHE-RSA-AES128-GCM-SHA256"));
    }

    /**
     * Setup class to establish https and the certificate real.
     */
    static class ServerSetup extends AbstractElytronSetupTask {

        private static KeyStore createKeyStore() throws Exception {
            KeyStore ks = KeyStore.getInstance("JKS");
            ks.load(null, null);
            return ks;
        }

        private static void createTemporaryKeyStoreFile(KeyStore keyStore, File outputFile, char[] password) throws Exception {
            try (OutputStream fos = Files.newOutputStream(outputFile.toPath())) {
                keyStore.store(fos, password);
            }
        }

        @Override
        protected void setup(ModelControllerClient modelControllerClient) throws Exception {
            Assert.assertTrue(WORKING_DIR_CA.mkdirs());
            keyStore = createKeyStore();
            trustStore = createKeyStore();
            usersStore = createKeyStore();
            user1Store = createKeyStore();

            // generate the CA key and certificate
            KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("RSA");
            X500Principal issuerDN = new X500Principal("C=UK, O=elytron.com, CN=Elytron CA");
            SelfSignedX509CertificateAndSigningKey issuerSelfSignedX509CertificateAndSigningKey = SelfSignedX509CertificateAndSigningKey.builder()
                    .setDn(issuerDN)
                    .setKeyAlgorithmName("RSA")
                    .setSignatureAlgorithmName("SHA1withRSA")
                    .addExtension(false, "BasicConstraints", "CA:true,pathlen:2147483647")
                    .build();
            X509Certificate issuerCertificate = issuerSelfSignedX509CertificateAndSigningKey.getSelfSignedCertificate();

            keyStore.setCertificateEntry("ca", issuerCertificate);
            trustStore.setCertificateEntry("ca", issuerCertificate);

            // generate the server key and keystore
            KeyPair serverKeys = keyPairGenerator.generateKeyPair();
            X509Certificate serverCertificate = new X509CertificateBuilder().setIssuerDn(issuerDN)
                    .setSubjectDn(new X500Principal("C=UK, O=elytron.com, CN=localhost"))
                    .setSignatureAlgorithmName("SHA1withRSA")
                    .setSigningKey(issuerSelfSignedX509CertificateAndSigningKey.getSigningKey())
                    .setPublicKey(serverKeys.getPublic())
                    .setSerialNumber(new BigInteger("3"))
                    .addExtension(new BasicConstraintsExtension(false, false, -1))
                    .addExtension(new SubjectAlternativeNamesExtension(false,
                            Arrays.asList(new GeneralName.DNSName("localhost"), new GeneralName.IPAddress("127.0.0.1"))))
                    .build();
            keyStore.setKeyEntry("localhost", serverKeys.getPrivate(), PASSWORD_CHAR,
                    new X509Certificate[]{serverCertificate, issuerCertificate});

            // generate the users store with user1 and the server itself
            KeyPair user1Keys = keyPairGenerator.generateKeyPair();
            X509Certificate user1Certificate = new X509CertificateBuilder().setIssuerDn(issuerDN)
                    .setSubjectDn(new X500Principal("C=UK, O=elytron.com, CN=user1"))
                    .setSignatureAlgorithmName("SHA1withRSA")
                    .setSigningKey(issuerSelfSignedX509CertificateAndSigningKey.getSigningKey())
                    .setPublicKey(user1Keys.getPublic())
                    .setSerialNumber(new BigInteger("3"))
                    .addExtension(new BasicConstraintsExtension(false, false, -1))
                    .build();
            user1Store.setKeyEntry("user1", user1Keys.getPrivate(), PASSWORD_CHAR,
                    new X509Certificate[]{user1Certificate, issuerCertificate});
            usersStore.setCertificateEntry("user1", user1Certificate);

            createTemporaryKeyStoreFile(keyStore, KEYSTORE_FILE, PASSWORD_CHAR);
            createTemporaryKeyStoreFile(trustStore, TRUST_FILE, PASSWORD_CHAR);
            createTemporaryKeyStoreFile(usersStore, USERS_KEYSTORE_FILE, PASSWORD_CHAR);
            createTemporaryKeyStoreFile(user1Store, USER1_KEYSTORE_FILE, PASSWORD_CHAR);

            super.setup(modelControllerClient);
        }

        @Override
        protected void tearDown(ModelControllerClient modelControllerClient) throws Exception {
            super.tearDown(modelControllerClient);
            Assert.assertTrue(TRUST_FILE.delete());
            Assert.assertTrue(KEYSTORE_FILE.delete());
            Assert.assertTrue(USERS_KEYSTORE_FILE.delete());
            Assert.assertTrue(USER1_KEYSTORE_FILE.delete());
            Assert.assertTrue(WORKING_DIR_CA.delete());
        }

        @Override
        protected ConfigurableElement[] getConfigurableElements() {
            List<ConfigurableElement> elements = new ArrayList<>();

            // credential reference for all the stores
            CredentialReference serverKeyStoreCredRef = CredentialReference.builder().withClearText(PASSWORD).build();
            // create the keystore for the server
            Path serverKeyStorePath = Path.builder().withPath(CliUtils.asAbsolutePath(KEYSTORE_FILE)).build();
            SimpleKeyStore serverKeyStore = SimpleKeyStore.builder().withName("serverKeyStore").withCredentialReference(
                    serverKeyStoreCredRef).withType("JKS").withPath(serverKeyStorePath).build();
            elements.add(serverKeyStore);
            SimpleKeyManager serverKeyManager = SimpleKeyManager.builder().withName("serverKeyManager").withKeyStore(
                    serverKeyStore.getName()).withCredentialReference(serverKeyStoreCredRef).build();
            elements.add(serverKeyManager);
            // Prepare server trust-store (with CRL configuration) and related key-manager for server ssl context
            Path serverTrustStorePath = Path.builder().withPath(CliUtils.asAbsolutePath(TRUST_FILE)).build();
            SimpleKeyStore serverTrustStore = SimpleKeyStore.builder().withName("serverTrustStore").withCredentialReference(
                    serverKeyStoreCredRef).withType("JKS").withPath(serverTrustStorePath).build();
            elements.add(serverTrustStore);
            // create the trust-manager
            SimpleTrustManager serverTrustManager = SimpleTrustManager.builder().withName("serverTrustManager").withKeyStore(
                    serverTrustStore.getName()).withAlgorithm("PKIX").build();
            elements.add(serverTrustManager);
            // Create final server ssl context with prepared key and trust managers.
            SimpleServerSslContext serverSslContext = SimpleServerSslContext.builder().withName("serverSslContext").withKeyManagers(
                    serverKeyManager.getName()).withNeedClientAuth(true).withTrustManagers(
                    serverTrustManager.getName()).build();
            elements.add(serverSslContext);
            // Configure created server ssl context for undertow default HTTPS listener.
            UndertowSslContext undertowSslContext = UndertowSslContext.builder().withHttpsListener("https").withName(
                    serverSslContext.getName()).build();
            elements.add(undertowSslContext);

            // create the keystore realm for users
            Path usersStorePath = Path.builder().withPath(CliUtils.asAbsolutePath(USERS_KEYSTORE_FILE)).build();
            SimpleKeyStore usersKeyStore = SimpleKeyStore.builder().withName("usersKeyStore").withCredentialReference(
                    serverKeyStoreCredRef).withType("JKS").withPath(usersStorePath).build();
            elements.add(usersKeyStore);
            KeyStoreRealm usersKeyStoreRealm = KeyStoreRealm.builder().withName("usersKeyStoreRealm").withKeyStore("usersKeyStore").build();
            elements.add(usersKeyStoreRealm);
            ConstantRoleMapper usersRoleMapper = ConstantRoleMapper.builder().withName("usersRoleMapper").withRoles("Users").build();
            elements.add(usersRoleMapper);
            X500AttributePrincipalDecoder cnUsersDecoder = X500AttributePrincipalDecoder.builder().withName("cnUsersDecoder")
                    .withOid("2.5.4.3").withMaximumSegments(1).build();
            elements.add(cnUsersDecoder);
            SimpleSecurityDomain certificateDomain = SimpleSecurityDomain.builder().withName(CETIFICATE_DOMAIN)
                    .withDefaultRealm("usersKeyStoreRealm")
                    .withPrincipalDecoder("cnUsersDecoder")
                    .withRoleMapper("usersRoleMapper")
                    .withPermissionMapper("default-permission-mapper")
                    .withRealms(SimpleSecurityDomain.SecurityDomainRealm.builder()
                            .withRealm("usersKeyStoreRealm")
                            .build())
                    .build();
            elements.add(certificateDomain);
            SimpleHttpAuthenticationFactory certificateHttpAuthFact = SimpleHttpAuthenticationFactory.builder()
                    .withName("certificateHttpAuthFact")
                    .withHttpServerMechanismFactory("global")
                    .withSecurityDomain("certificateDomain")
                    .addMechanismConfiguration(MechanismConfiguration.builder()
                            .withMechanismName("CLIENT_CERT").build())
                    .build();
            elements.add(certificateHttpAuthFact);
            UndertowApplicationSecurityDomain undertowDomain = UndertowApplicationSecurityDomain.builder()
                    .withName(CETIFICATE_DOMAIN)
                    .httpAuthenticationFactory("certificateHttpAuthFact")
                    .build();
            elements.add(undertowDomain);

            return elements.toArray(new ConfigurableElement[0]);
        }
    }

    /**
     * Helper class to enable the forwarding options to true.
     */
    static class ForwardingSetup implements ServerSetupTask {

        private PathAddress getHttpsPath() {
            return PathAddress.pathAddress().append("subsystem", "undertow")
                    .append("server", "default-server")
                    .append("https-listener", "https");
        }

        @Override
        public void setup(ManagementClient mc, String string) throws Exception {
            ModelNode op = Util.createOperation(ModelDescriptionConstants.WRITE_ATTRIBUTE_OPERATION, getHttpsPath());
            op.get(ModelDescriptionConstants.NAME).set("certificate-forwarding");
            op.get(ModelDescriptionConstants.VALUE).set("true");
            CoreUtils.applyUpdate(op, mc.getControllerClient());
            op = Util.createOperation(ModelDescriptionConstants.WRITE_ATTRIBUTE_OPERATION, getHttpsPath());
            op.get(ModelDescriptionConstants.NAME).set("proxy-address-forwarding");
            op.get(ModelDescriptionConstants.VALUE).set("true");
            CoreUtils.applyUpdate(op, mc.getControllerClient());
            ServerReload.reloadIfRequired(mc);
        }

        @Override
        public void tearDown(ManagementClient mc, String string) throws Exception {
            ModelNode op = Util.createOperation(ModelDescriptionConstants.UNDEFINE_ATTRIBUTE_OPERATION, getHttpsPath());
            op.get(ModelDescriptionConstants.NAME).set("certificate-forwarding");
            CoreUtils.applyUpdate(op, mc.getControllerClient());
            op = Util.createOperation(ModelDescriptionConstants.UNDEFINE_ATTRIBUTE_OPERATION, getHttpsPath());
            op.get(ModelDescriptionConstants.NAME).set("proxy-address-forwarding");
            CoreUtils.applyUpdate(op, mc.getControllerClient());
            ServerReload.reloadIfRequired(mc);
        }

    }
}
