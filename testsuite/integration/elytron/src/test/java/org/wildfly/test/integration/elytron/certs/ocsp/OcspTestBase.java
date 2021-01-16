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

package org.wildfly.test.integration.elytron.certs.ocsp;

import static org.wildfly.security.x500.X500.OID_AD_OCSP;
import static org.wildfly.security.x500.X500.OID_KP_OCSP_SIGNING;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.math.BigInteger;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.KeyStore;
import java.security.Principal;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.Security;
import java.security.cert.X509Certificate;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.LinkedList;

import javax.security.auth.x500.X500Principal;

import org.jboss.as.controller.client.ModelControllerClient;
import org.jboss.as.test.shared.CliUtils;

import org.wildfly.security.x500.GeneralName;
import org.wildfly.security.x500.cert.AccessDescription;
import org.wildfly.security.x500.cert.AuthorityInformationAccessExtension;
import org.wildfly.security.x500.cert.BasicConstraintsExtension;
import org.wildfly.security.x500.cert.ExtendedKeyUsageExtension;
import org.wildfly.security.x500.cert.SelfSignedX509CertificateAndSigningKey;
import org.wildfly.security.x500.cert.X509CertificateBuilder;
import org.wildfly.test.integration.elytron.certs.CommonBase;
import org.wildfly.test.security.common.AbstractElytronSetupTask;
import org.wildfly.test.security.common.elytron.CertificateRevocationList;
import org.wildfly.test.security.common.elytron.ConfigurableElement;
import org.wildfly.test.security.common.elytron.CredentialReference;
import org.wildfly.test.security.common.elytron.Ocsp;
import org.wildfly.test.security.common.elytron.Path;
import org.wildfly.test.security.common.elytron.SimpleKeyManager;
import org.wildfly.test.security.common.elytron.SimpleKeyStore;
import org.wildfly.test.security.common.elytron.SimpleServerSslContext;
import org.wildfly.test.security.common.elytron.SimpleTrustManager;
import org.wildfly.test.security.common.elytron.UndertowSslContext;

import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.cert.X509CRLHolder;
import org.bouncycastle.cert.X509v2CRLBuilder;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.openssl.MiscPEMGenerator;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;
import org.bouncycastle.util.io.pem.PemWriter;
import org.junit.Assert;

/**
 * Auxiliary methods and variables for OCSP tests.
 *
 * @author Jan Stourac <jstourac@redhat.com>
 */
public class OcspTestBase extends CommonBase {

    protected static KeyStore trustStore;
    protected static KeyStore ocspCheckedGoodKeyStore;
    protected static KeyStore ocspCheckedGoodNoUrlKeyStore;
    protected static KeyStore ocspCheckedRevokedKeyStore;
    protected static KeyStore ocspCheckedUnknownKeyStore;
    protected static KeyStore ocspCheckedTooLongChainKeyStore;

    private static final int OCSP_PORT = 4854;
    protected static TestingOcspServer ocspServer = null;
    protected static final String PASSWORD = "Elytron";
    protected static final char[] PASSWORD_CHAR = PASSWORD.toCharArray();
    protected static final String CA_JKS_LOCATION = "." + File.separator + "target" + File.separator + "test-classes" +
            File.separator + "ca" + File.separator + "jks";
    protected static final File WORKING_DIR_CA = new File(CA_JKS_LOCATION);
    protected static final File LADYBIRD_FILE = new File(WORKING_DIR_CA,"ladybird.keystore");
    protected static final File OCSP_RESPONDER_FILE = new File(WORKING_DIR_CA,"ocsp-responder.keystore");
    protected static final File OCSP_CHECKED_GOOD_FILE = new File(WORKING_DIR_CA,"ocsp-checked-good.keystore");
    protected static final File OCSP_CHECKED_GOOD_NO_URL_FILE = new File(WORKING_DIR_CA,"ocsp-checked-good-no-url.keystore");
    protected static final File OCSP_CHECKED_REVOKED_FILE = new File(WORKING_DIR_CA, "ocsp-checked-revoked.keystore");
    protected static final File OCSP_CHECKED_UNKNOWN_FILE = new File(WORKING_DIR_CA, "ocsp-checked-unknown.keystore");
    protected static final File OCSP_CHECKED_TOO_LONG_CHAIN_FILE =
            new File(WORKING_DIR_CA, "ocsp-checked-too-long-chain.keystore");

    private static final String CA_CRL_LOCATION = "." + File.separator + "target" + File.separator + "test-classes" +
            File.separator + "ca" + File.separator + "crl";
    private static final File WORKING_DIR_CACRL = new File(CA_CRL_LOCATION);
    protected static final File CA_BLANK_PEM_CRL = new File(WORKING_DIR_CACRL, "blank.pem");
    protected static final File TRUST_FILE = new File(WORKING_DIR_CA,"ca.truststore");

    protected static final String OCSP_RESPONDER_URL = "http://localhost:" + OCSP_PORT + "/ocsp";

    private static KeyStore createKeyStore() throws Exception {
        KeyStore ks = KeyStore.getInstance("JKS");
        ks.load(null,null);
        return ks;
    }

    private static void createTemporaryKeyStoreFile(KeyStore keyStore, File outputFile, char[] password) throws Exception {
        if (!outputFile.exists()) {
            outputFile.createNewFile();
        }
        try (FileOutputStream fos = new FileOutputStream(outputFile)){
            keyStore.store(fos, password);
        }
    }

    private static org.bouncycastle.asn1.x500.X500Name convertSunStyleToBCStyle(Principal dn) {
        String dnName = dn.getName();
        String[] dnComponents = dnName.split(", ");
        StringBuilder dnBuffer = new StringBuilder(dnName.length());

        dnBuffer.append(dnComponents[dnComponents.length-1]);
        for(int i = dnComponents.length-2; i >= 0; i--){
            dnBuffer.append(',');
            dnBuffer.append(dnComponents[i]);
        }

        return new X500Name(dnBuffer.toString());
    }

    private static X509Certificate issuerCertificate;
    private static X509Certificate intermediateIssuerCertificate;
    private static X509Certificate ocspCheckedGoodCertificate;
    private static X509Certificate ocspCheckedGoodNoUrlCertificate;
    private static X509Certificate ocspCheckedRevokedCertificate;
    private static X509Certificate ocspCheckedTooLongChainCertificate;

    public static void beforeTest() throws Exception {
        Assert.assertTrue(WORKING_DIR_CA.mkdirs());
        Assert.assertTrue(WORKING_DIR_CACRL.mkdirs());

        KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("RSA");
        Security.addProvider(new BouncyCastleProvider());

        X500Principal issuerDN = new X500Principal("CN=Elytron CA, ST=Elytron, C=UK, EMAILADDRESS=elytron@wildfly.org, O=Root Certificate Authority");
        X500Principal intermediateIssuerDN = new X500Principal("CN=Elytron ICA, ST=Elytron, C=UK, O=Intermediate Certificate Authority");

        KeyStore ladybirdKeyStore = createKeyStore();
        trustStore = createKeyStore();

        // Generates the issuer certificate and adds it to the keystores
        SelfSignedX509CertificateAndSigningKey issuerSelfSignedX509CertificateAndSigningKey = SelfSignedX509CertificateAndSigningKey.builder()
                .setDn(issuerDN)
                .setKeyAlgorithmName("RSA")
                .setSignatureAlgorithmName("SHA256withRSA")
                .addExtension(false, "BasicConstraints", "CA:true,pathlen:2147483647")
                .build();
        issuerCertificate = issuerSelfSignedX509CertificateAndSigningKey.getSelfSignedCertificate();
        ladybirdKeyStore.setCertificateEntry("ca", issuerCertificate);
        trustStore.setCertificateEntry("mykey",issuerCertificate);

        // Generates certificate and keystore for Ladybird
        KeyPair ladybirdKeys = keyPairGenerator.generateKeyPair();
        PrivateKey ladybirdSigningKey = ladybirdKeys.getPrivate();
        PublicKey ladybirdPublicKey = ladybirdKeys.getPublic();

        X509Certificate ladybirdCertificate = new X509CertificateBuilder()
                .setIssuerDn(issuerDN)
                .setSubjectDn(new X500Principal("OU=Elytron, O=Elytron, C=UK, ST=Elytron, CN=localhost"))
                .setSignatureAlgorithmName("SHA256withRSA")
                .setSigningKey(issuerSelfSignedX509CertificateAndSigningKey.getSigningKey())
                .setPublicKey(ladybirdPublicKey)
                .setSerialNumber(new BigInteger("3"))
                .addExtension(new BasicConstraintsExtension(false, false, -1))
                .build();
        ladybirdKeyStore.setKeyEntry("ladybird", ladybirdSigningKey, PASSWORD_CHAR,
                new X509Certificate[]{ladybirdCertificate, issuerCertificate});

        // Generates certificate and keystore for OCSP responder
        KeyPair ocspResponderKeys = keyPairGenerator.generateKeyPair();
        PrivateKey ocspResponderSigningKey = ocspResponderKeys.getPrivate();
        PublicKey ocspResponderPublicKey = ocspResponderKeys.getPublic();

        X509Certificate ocspResponderCertificate = new X509CertificateBuilder()
                .setIssuerDn(issuerDN)
                .setSubjectDn(new X500Principal("OU=Elytron, O=Elytron, C=UK, ST=Elytron, CN=OcspResponder"))
                .setSignatureAlgorithmName("SHA256withRSA")
                .setSigningKey(issuerSelfSignedX509CertificateAndSigningKey.getSigningKey())
                .setPublicKey(ocspResponderPublicKey)
                .setSerialNumber(new BigInteger("15"))
                .addExtension(new BasicConstraintsExtension(false, false, -1))
                .addExtension(new ExtendedKeyUsageExtension(false, Collections.singletonList(OID_KP_OCSP_SIGNING)))
                .build();
        KeyStore ocspResponderKeyStore = createKeyStore();
        ocspResponderKeyStore.setCertificateEntry("ca", issuerCertificate);
        ocspResponderKeyStore.setKeyEntry("ocspResponder", ocspResponderSigningKey, PASSWORD_CHAR,
                new X509Certificate[]{ocspResponderCertificate, issuerCertificate});
        createTemporaryKeyStoreFile(ocspResponderKeyStore, OCSP_RESPONDER_FILE, PASSWORD_CHAR);

        // Generates GOOD certificate referencing the OCSP responder
        KeyPair ocspCheckedGoodKeys = keyPairGenerator.generateKeyPair();
        PrivateKey ocspCheckedGoodSigningKey = ocspCheckedGoodKeys.getPrivate();
        PublicKey ocspCheckedGoodPublicKey = ocspCheckedGoodKeys.getPublic();

        ocspCheckedGoodCertificate = new X509CertificateBuilder()
                .setIssuerDn(issuerDN)
                .setSubjectDn(new X500Principal("OU=Elytron, O=Elytron, C=UK, ST=Elytron, CN=localhost"))
                .setSignatureAlgorithmName("SHA256withRSA")
                .setSigningKey(issuerSelfSignedX509CertificateAndSigningKey.getSigningKey())
                .setPublicKey(ocspCheckedGoodPublicKey)
                .setSerialNumber(new BigInteger("16"))
                .addExtension(new BasicConstraintsExtension(false, false, -1))
                .addExtension(new AuthorityInformationAccessExtension(Collections.singletonList(
                        new AccessDescription(OID_AD_OCSP, new GeneralName.URIName(OCSP_RESPONDER_URL))
                ))).build();
        ocspCheckedGoodKeyStore = createKeyStore();
        ocspCheckedGoodKeyStore.setCertificateEntry("ca", issuerCertificate);
        ocspCheckedGoodKeyStore.setKeyEntry("localhost", ocspCheckedGoodSigningKey, PASSWORD_CHAR,
                new X509Certificate[]{ocspCheckedGoodCertificate, issuerCertificate});
        createTemporaryKeyStoreFile(ocspCheckedGoodKeyStore, OCSP_CHECKED_GOOD_FILE, PASSWORD_CHAR);

        // Generates GOOD certificate but not referencing OCSP responder
        KeyPair ocspCheckedGoodNoUrlKeys = keyPairGenerator.generateKeyPair();
        PrivateKey ocspCheckedGoodNoUrlSigningKey = ocspCheckedGoodNoUrlKeys.getPrivate();
        PublicKey ocspCheckedGoodNoUrlPublicKey = ocspCheckedGoodNoUrlKeys.getPublic();

        ocspCheckedGoodNoUrlCertificate = new X509CertificateBuilder()
                .setIssuerDn(issuerDN)
                .setSubjectDn(new X500Principal("OU=Elytron, O=Elytron, C=UK, ST=Elytron, CN=localhost"))
                .setSignatureAlgorithmName("SHA256withRSA")
                .setSigningKey(issuerSelfSignedX509CertificateAndSigningKey.getSigningKey())
                .setPublicKey(ocspCheckedGoodNoUrlPublicKey)
                .setSerialNumber(new BigInteger("53"))
                .addExtension(new BasicConstraintsExtension(false, false, -1))
                .build();
        ocspCheckedGoodNoUrlKeyStore = createKeyStore();
        ocspCheckedGoodNoUrlKeyStore.setCertificateEntry("ca", issuerCertificate);
        ocspCheckedGoodNoUrlKeyStore.setKeyEntry("localhost", ocspCheckedGoodNoUrlSigningKey, PASSWORD_CHAR,
                new X509Certificate[]{ocspCheckedGoodNoUrlCertificate, issuerCertificate});
        createTemporaryKeyStoreFile(ocspCheckedGoodNoUrlKeyStore, OCSP_CHECKED_GOOD_NO_URL_FILE, PASSWORD_CHAR);

        // Generates REVOKED certificate referencing the OCSP responder
        KeyPair ocspCheckedRevokedKeys = keyPairGenerator.generateKeyPair();
        PrivateKey ocspCheckedRevokedSigningKey = ocspCheckedRevokedKeys.getPrivate();
        PublicKey ocspCheckedRevokedPublicKey = ocspCheckedRevokedKeys.getPublic();

        ocspCheckedRevokedCertificate = new X509CertificateBuilder()
                .setIssuerDn(issuerDN)
                .setSubjectDn(new X500Principal("OU=Elytron, O=Elytron, C=UK, ST=Elytron, CN=localhost"))
                .setSignatureAlgorithmName("SHA256withRSA")
                .setSigningKey(issuerSelfSignedX509CertificateAndSigningKey.getSigningKey())
                .setPublicKey(ocspCheckedRevokedPublicKey)
                .setSerialNumber(new BigInteger("17"))
                .addExtension(new BasicConstraintsExtension(false, false, -1))
                .addExtension(new AuthorityInformationAccessExtension(Collections.singletonList(
                        new AccessDescription(OID_AD_OCSP, new GeneralName.URIName(OCSP_RESPONDER_URL))
                )))
                .build();
        ocspCheckedRevokedKeyStore = createKeyStore();
        ocspCheckedRevokedKeyStore.setCertificateEntry("ca", issuerCertificate);
        ocspCheckedRevokedKeyStore.setKeyEntry("localhost", ocspCheckedRevokedSigningKey, PASSWORD_CHAR,
                new X509Certificate[]{ocspCheckedRevokedCertificate, issuerCertificate});
        createTemporaryKeyStoreFile(ocspCheckedRevokedKeyStore, OCSP_CHECKED_REVOKED_FILE, PASSWORD_CHAR);

        // Generates UNKNOWN certificate referencing the OCSP responder
        KeyPair ocspCheckedUnknownKeys = keyPairGenerator.generateKeyPair();
        PrivateKey ocspCheckedUnknownSigningKey = ocspCheckedUnknownKeys.getPrivate();
        PublicKey ocspCheckedUnknownPublicKey = ocspCheckedUnknownKeys.getPublic();

        X509Certificate ocspCheckedUnknownCertificate = new X509CertificateBuilder()
                .setIssuerDn(issuerDN)
                .setSubjectDn(new X500Principal("OU=Elytron, O=Elytron, C=UK, ST=Elytron, CN=ocspCheckedUnknown"))
                .setSignatureAlgorithmName("SHA256withRSA")
                .setSigningKey(issuerSelfSignedX509CertificateAndSigningKey.getSigningKey())
                .setPublicKey(ocspCheckedUnknownPublicKey)
                .setSerialNumber(new BigInteger("18"))
                .addExtension(new BasicConstraintsExtension(false, false, -1))
                .addExtension(new AuthorityInformationAccessExtension(Collections.singletonList(
                        new AccessDescription(OID_AD_OCSP, new GeneralName.URIName(OCSP_RESPONDER_URL))
                )))
                .build();
        ocspCheckedUnknownKeyStore = createKeyStore();
        ocspCheckedUnknownKeyStore.setCertificateEntry("ca", issuerCertificate);
        ocspCheckedUnknownKeyStore.setKeyEntry("localhost", ocspCheckedUnknownSigningKey, PASSWORD_CHAR,
                new X509Certificate[]{ocspCheckedUnknownCertificate, issuerCertificate});
        createTemporaryKeyStoreFile(ocspCheckedUnknownKeyStore, OCSP_CHECKED_UNKNOWN_FILE, PASSWORD_CHAR);

        // Generates the intermediate issuer certificate
        KeyPair intermediateIssuerKeys = keyPairGenerator.generateKeyPair();
        PrivateKey intermediateIssuerSigningKey = intermediateIssuerKeys.getPrivate();
        PublicKey intermediateIssuerPublicKey = intermediateIssuerKeys.getPublic();

        intermediateIssuerCertificate = new X509CertificateBuilder()
                .setIssuerDn(issuerDN)
                .setSubjectDn(intermediateIssuerDN)
                .setSignatureAlgorithmName("SHA256withRSA")
                .setSigningKey(issuerSelfSignedX509CertificateAndSigningKey.getSigningKey())
                .setPublicKey(intermediateIssuerPublicKey)
                .setSerialNumber(new BigInteger("6"))
                .addExtension(new BasicConstraintsExtension(false, true, -1))
                .addExtension(new AuthorityInformationAccessExtension(Collections.singletonList(
                        new AccessDescription(OID_AD_OCSP, new GeneralName.URIName(OCSP_RESPONDER_URL))
                )))
                .build();

        // Generates GOOD certificate with more intermediate certificates referencing the OCSP responder
        KeyPair ocspCheckedTooLongChainKeys = keyPairGenerator.generateKeyPair();
        PrivateKey ocspCheckedTooLongChainSigningKey = ocspCheckedTooLongChainKeys.getPrivate();
        PublicKey ocspCheckedTooLongChainPublicKey = ocspCheckedTooLongChainKeys.getPublic();

        ocspCheckedTooLongChainCertificate = new X509CertificateBuilder()
                .setIssuerDn(intermediateIssuerDN)
                .setSubjectDn(new X500Principal("OU=Elytron, O=Elytron, C=UK, ST=Elytron, CN=localhost"))
                .setSignatureAlgorithmName("SHA256withRSA")
                .setSigningKey(intermediateIssuerSigningKey)
                .setPublicKey(ocspCheckedTooLongChainPublicKey)
                .setSerialNumber(new BigInteger("20"))
                .addExtension(new BasicConstraintsExtension(false, false, -1))
                .addExtension(new AuthorityInformationAccessExtension(Collections.singletonList(
                        new AccessDescription(OID_AD_OCSP, new GeneralName.URIName(OCSP_RESPONDER_URL))
                ))).build();
        ocspCheckedTooLongChainKeyStore = createKeyStore();
        ocspCheckedTooLongChainKeyStore.setCertificateEntry("ca", issuerCertificate);
        ocspCheckedTooLongChainKeyStore.setCertificateEntry("ca2", intermediateIssuerCertificate);
        ocspCheckedTooLongChainKeyStore.setKeyEntry("localhost", ocspCheckedTooLongChainSigningKey, PASSWORD_CHAR,
                new X509Certificate[]{ocspCheckedTooLongChainCertificate, intermediateIssuerCertificate, issuerCertificate});
        createTemporaryKeyStoreFile(ocspCheckedTooLongChainKeyStore, OCSP_CHECKED_TOO_LONG_CHAIN_FILE, PASSWORD_CHAR);

        prepareCrlFiles(intermediateIssuerCertificate, issuerSelfSignedX509CertificateAndSigningKey);

        // Create the temporary files
        createTemporaryKeyStoreFile(ladybirdKeyStore, LADYBIRD_FILE, PASSWORD_CHAR);
        createTemporaryKeyStoreFile(trustStore, TRUST_FILE, PASSWORD_CHAR); // trust file for server config
    }

    public static void startOcspServer() throws Exception {
        ocspServer = new TestingOcspServer(OCSP_PORT);
        ocspServer.createIssuer(1, issuerCertificate);
        ocspServer.createIssuer(2, intermediateIssuerCertificate);
        ocspServer.createCertificate(3, 1, intermediateIssuerCertificate);
        ocspServer.createCertificate(1, 1, ocspCheckedGoodCertificate);
        ocspServer.createCertificate(2, 1, ocspCheckedRevokedCertificate);
        ocspServer.revokeCertificate(2, 4);
        ocspServer.createCertificate(3, 2, ocspCheckedTooLongChainCertificate);
        ocspServer.createCertificate(4, 1, ocspCheckedGoodNoUrlCertificate);

        ocspServer.start();
    }

    private static void prepareCrlFiles(X509Certificate intermediateIssuerCertificate,
            SelfSignedX509CertificateAndSigningKey issuerSelfSignedX509CertificateAndSigningKey) throws Exception {
        // Used for all CRLs
        Calendar calendar = Calendar.getInstance();
        Date currentDate = calendar.getTime();
        calendar.add(Calendar.YEAR, 1);
        Date nextYear = calendar.getTime();
        calendar.add(Calendar.YEAR, -1);
        calendar.add(Calendar.SECOND, -30);

        // Creates the CRL for ca/crl/blank.pem
        X509v2CRLBuilder caBlankCrlBuilder = new X509v2CRLBuilder(
                convertSunStyleToBCStyle(intermediateIssuerCertificate.getIssuerDN()),
                currentDate
        );
        X509CRLHolder caBlankCrlHolder = caBlankCrlBuilder.setNextUpdate(nextYear).build(
                new JcaContentSignerBuilder("SHA256withRSA")
                        .setProvider("BC")
                        .build(issuerSelfSignedX509CertificateAndSigningKey.getSigningKey())
        );

        PemWriter caBlankCrlOutput = new PemWriter(new OutputStreamWriter(new FileOutputStream(CA_BLANK_PEM_CRL)));
        caBlankCrlOutput.writeObject(new MiscPEMGenerator(caBlankCrlHolder));
        caBlankCrlOutput.close();
    }

    public static void stopOcspServer() throws Exception {
        if (ocspServer != null) {
            ocspServer.stop();
        }
    }

    public static void afterTest() {
        Assert.assertTrue(LADYBIRD_FILE.delete());
        Assert.assertTrue(OCSP_RESPONDER_FILE.delete());
        Assert.assertTrue(OCSP_CHECKED_GOOD_FILE.delete());
        Assert.assertTrue(OCSP_CHECKED_GOOD_NO_URL_FILE.delete());
        Assert.assertTrue(OCSP_CHECKED_REVOKED_FILE.delete());
        Assert.assertTrue(OCSP_CHECKED_UNKNOWN_FILE.delete());
        Assert.assertTrue(OCSP_CHECKED_TOO_LONG_CHAIN_FILE.delete());
        Assert.assertTrue(TRUST_FILE.delete());
        Assert.assertTrue(WORKING_DIR_CA.delete());
        Assert.assertTrue(CA_BLANK_PEM_CRL.delete());
        Assert.assertTrue(WORKING_DIR_CACRL.delete());
    }

    /**
     * Server setup task that configures OCSP and related dependencies but OCSP responder is not started.
     */
    static class OcspResponderDownServerSetup extends AbstractElytronSetupTask {
        @Override
        protected void setup(ModelControllerClient modelControllerClient) throws Exception {
            OcspTestBase.beforeTest();

            super.setup(modelControllerClient);
        }

        @Override
        protected void tearDown(ModelControllerClient modelControllerClient) throws Exception {
            super.tearDown(modelControllerClient);

            OcspTestBase.afterTest();
        }

        @Override
        protected ConfigurableElement[] getConfigurableElements() {
            return getConfigurableElementsCommon();
        }
    }

    /**
     * Server setup task that configures OCSP and related dependencies with also fully functional OCSP responder.
     */
    static class OcspServerSetup extends AbstractElytronSetupTask {

        @Override
        protected void setup(ModelControllerClient modelControllerClient) throws Exception {
            OcspTestBase.beforeTest();

            startOcspServer();

            super.setup(modelControllerClient);
        }

        @Override
        protected void tearDown(ModelControllerClient modelControllerClient) throws Exception {
            super.tearDown(modelControllerClient);

            stopOcspServer();

            OcspTestBase.afterTest();
        }

        @Override
        protected ConfigurableElement[] getConfigurableElements() {
            return getConfigurableElementsCommon();
        }
    }

    /**
     * Common part of server configuration that is used for OCSP testing.
     *
     * @return list of configurable elements that shall be setup on server
     */
    private static ConfigurableElement[] getConfigurableElementsCommon() {
        LinkedList<ConfigurableElement> elements = new LinkedList<>();

        CredentialReference serverKeyStoreCredRef = CredentialReference.builder().withClearText(PASSWORD).build();

        CredentialReference ocspResponderKeyStoreCredRef = CredentialReference.builder().withClearText(PASSWORD).build();

        // Prepare server key-store and key-manager for server ssl context
        Path serverKeyStorePath = Path.builder().withPath(CliUtils.asAbsolutePath(LADYBIRD_FILE)).build();

        SimpleKeyStore serverKeyStore = SimpleKeyStore.builder().withName("serverKeyStore").withCredentialReference(
                serverKeyStoreCredRef).withType("JKS").withPath(serverKeyStorePath).build();
        elements.add(serverKeyStore);

        SimpleKeyManager serverKeyManager = SimpleKeyManager.builder().withName("serverKeyManager").withKeyStore(
                serverKeyStore.getName()).withCredentialReference(serverKeyStoreCredRef).build();
        elements.add(serverKeyManager);

        // Prepare server trust-store (with OCSP configuration) and related key-manager for server ssl context
        Path serverTrustStorePath = Path.builder().withPath(CliUtils.asAbsolutePath(TRUST_FILE)).build();
        SimpleKeyStore serverTrustStore = SimpleKeyStore.builder().withName("serverTrustStore").withCredentialReference(
                serverKeyStoreCredRef).withType("JKS").withPath(serverTrustStorePath).build();
        elements.add(serverTrustStore);

        // Add OCSP responder key store
        Path ocspResponderKeyStorePath = Path.builder().withPath(OCSP_RESPONDER_FILE.getAbsolutePath()).build();
        SimpleKeyStore ocspResponderKeyStore = SimpleKeyStore.builder().withName("ocspResponderKeyStore").withCredentialReference(
                ocspResponderKeyStoreCredRef).withType("JKS").withPath(ocspResponderKeyStorePath).build();
        elements.add(ocspResponderKeyStore);

        Ocsp ocsp = Ocsp.builder()
                .withPreferCrls(false)
                .withResponderKeyStore("ocspResponderKeyStore")
                .withResponderCertificate("ocspResponder").build();

        CertificateRevocationList crl =
                CertificateRevocationList.builder().withPath(CliUtils.asAbsolutePath(CA_BLANK_PEM_CRL)).build();
        SimpleTrustManager serverTrustManager = SimpleTrustManager.builder()
                .withName("serverTrustManager")
                .withKeyStore(serverTrustStore.getName())
                .withOcsp(ocsp)
                .withSoftFail(false)
                .withCrl(crl)
                .withAlgorithm("PKIX").build();
        elements.add(serverTrustManager);

        // Create final server ssl context with prepared key and trust managers.
        SimpleServerSslContext serverSslContext =
                SimpleServerSslContext.builder().withName("serverSslContext").withKeyManagers(
                        serverKeyManager.getName()).withNeedClientAuth(true).withTrustManagers(
                        serverTrustManager.getName()).build();
        elements.add(serverSslContext);

        // Configure created server ssl context for undertow default HTTPS listener.
        UndertowSslContext undertowSslContext =
                UndertowSslContext.builder().withHttpsListener("https").withName(serverSslContext.getName()).build();
        elements.add(undertowSslContext);

        return elements.toArray(new ConfigurableElement[]{});
    }
}
