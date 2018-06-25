/*
 * JBoss, Home of Professional Open Source
 *
 * Copyright 2018 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jboss.as.test.integration.ws.wsse;

import java.io.File;
import java.io.FileOutputStream;
import java.math.BigInteger;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.List;

import javax.security.auth.x500.X500Principal;

import org.wildfly.security.x500.GeneralName;
import org.wildfly.security.x500.cert.BasicConstraintsExtension;
import org.wildfly.security.x500.cert.IssuerAlternativeNamesExtension;
import org.wildfly.security.x500.cert.SelfSignedX509CertificateAndSigningKey;
import org.wildfly.security.x500.cert.SubjectAlternativeNamesExtension;
import org.wildfly.security.x500.cert.X509CertificateBuilder;

/**
 * Generates the jks and properties files needed for SignEncrypt tests
 * @author <a href="mailto:jucook@redhat.com">Justin Cook</a>
 */
public class GenerateSignEncryptFiles {
    private static final char[] GENERATED_KEYSTORE_PASSWORD = "password".toCharArray();

    private static final String ALICE_ALIAS = "alice";
    private static final String BOB_ALIAS = "bob";
    private static final String JOHN_ALIAS = "john";
    private static final String MAX_ALIAS = "max";

    private static final String WORKING_DIRECTORY_LOCATION = GenerateWSKeyStores.class.getProtectionDomain().getCodeSource().getLocation().getPath() + "org/jboss/as/test/integration/ws/wsse";

    private static final File ALICE_JKS_FILE = new File(WORKING_DIRECTORY_LOCATION, "alice.jks");
    private static final File BOB_JKS_FILE = new File(WORKING_DIRECTORY_LOCATION, "bob.jks");
    private static final File JOHN_JKS_FILE = new File(WORKING_DIRECTORY_LOCATION, "john.jks");
    private static final File MAX_JKS_FILE = new File(WORKING_DIRECTORY_LOCATION, "max.jks");

    private static final String ALICE_DN = "CN=alice, OU=eng, O=apache.org";
    private static final String BOB_DN = "CN=bob, OU=eng, O=apache.org";
    private static final String JOHN_DN = "CN=John, OU=Test, O=Test, L=Test, ST=Test, C=IT";
    private static final String MAX_DN = "CN=Max, OU=Test, O=Test, L=Test, ST=Test, C=CZ";
    private static final String CXFCA_DN = "CN=cxfca, OU=eng, O=apache.org";

    private static final String ALICE_SERIAL_NUMBER = "49546001";
    private static final String BOB_SERIAL_NUMBER = "49546002";

    private static final String SHA_1_RSA = "SHA1withRSA";
    private static final String SHA_256_RSA = "SHA256withRSA";

    private static KeyStore loadKeyStore() throws Exception {
        KeyStore ks = KeyStore.getInstance("JKS");
        ks.load(null, null);
        return ks;
    }

    private static SelfSignedX509CertificateAndSigningKey createBasicSelfSigned(String DN, String signatureAlgorithmName) {
        return SelfSignedX509CertificateAndSigningKey.builder()
                .setDn(new X500Principal(DN))
                .setKeyAlgorithmName("RSA")
                .setSignatureAlgorithmName(signatureAlgorithmName)
                .setKeySize(1024)
                .build();
    }

    private static SelfSignedX509CertificateAndSigningKey createExtensionSelfSigned() {
        return SelfSignedX509CertificateAndSigningKey.builder()
                .setDn(new X500Principal(CXFCA_DN))
                .setKeyAlgorithmName("RSA")
                .setSignatureAlgorithmName(SHA_1_RSA)
                .setKeySize(1024)
                .addExtension(new BasicConstraintsExtension(true, true, 2147483647))
                .addExtension(false, "IssuerAlternativeName","DNS:NOT_FOR_PRODUCTION_USE")
                .build();
    }

    private static X509Certificate createCertificate(SelfSignedX509CertificateAndSigningKey issuerSelfSignedX509CertificateAndSigningKey, PublicKey publicKey, String subjectDN, String serialNumber) throws Exception {
        List<GeneralName> issuerAltName = new ArrayList<>();
        issuerAltName.add(new GeneralName.DNSName("NOT_FOR_PRODUCTION_USE"));
        List<GeneralName> subjectAltName = new ArrayList<>();
        subjectAltName.add(new GeneralName.DNSName("localhost"));

        return new X509CertificateBuilder()
                .setIssuerDn(issuerSelfSignedX509CertificateAndSigningKey.getSelfSignedCertificate().getIssuerX500Principal())
                .setSubjectDn(new X500Principal(subjectDN))
                .setSignatureAlgorithmName(SHA_1_RSA)
                .setSigningKey(issuerSelfSignedX509CertificateAndSigningKey.getSigningKey())
                .setPublicKey(publicKey)
                .addExtension(new IssuerAlternativeNamesExtension(false, issuerAltName))
                .addExtension(new SubjectAlternativeNamesExtension(false, subjectAltName))
                .setSerialNumber(new BigInteger(serialNumber))
                .build();
    }

    private static KeyStore createSelfSignedKeyStore(SelfSignedX509CertificateAndSigningKey selfSignedX509CertificateAndSigningKey, String alias) throws Exception {
        KeyStore keyStore = loadKeyStore();

        X509Certificate certificate = selfSignedX509CertificateAndSigningKey.getSelfSignedCertificate();
        keyStore.setKeyEntry(alias, selfSignedX509CertificateAndSigningKey.getSigningKey(), GENERATED_KEYSTORE_PASSWORD, new X509Certificate[]{certificate});

        return keyStore;
    }

    private static KeyStore createChainedKeyStore(SelfSignedX509CertificateAndSigningKey selfSignedX509CertificateAndSigningKey, X509Certificate subjectCertificate, PrivateKey signingKey, String alias) throws Exception {
        KeyStore keyStore = loadKeyStore();

        X509Certificate issuerCertificate = selfSignedX509CertificateAndSigningKey.getSelfSignedCertificate();
        keyStore.setKeyEntry(alias, signingKey, GENERATED_KEYSTORE_PASSWORD, new X509Certificate[]{subjectCertificate, issuerCertificate});

        return keyStore;
    }

    private static void createTemporaryKeyStoreFile(KeyStore keyStore, File outputFile) throws Exception {
        try (FileOutputStream fos = new FileOutputStream(outputFile)){
            keyStore.store(fos, GENERATED_KEYSTORE_PASSWORD);
        }
    }

    private static void setUpKeyStores() throws Exception {
        File workingDir = new File(WORKING_DIRECTORY_LOCATION);
        if (workingDir.exists() == false) {
            workingDir.mkdirs();
        }

        KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("RSA");
        KeyPair aliceKeys = keyPairGenerator.generateKeyPair();
        PrivateKey aliceSigningKey = aliceKeys.getPrivate();
        PublicKey alicePublicKey = aliceKeys.getPublic();
        KeyPair bobKeys = keyPairGenerator.generateKeyPair();
        PrivateKey bobSigningKey = bobKeys.getPrivate();
        PublicKey bobPublicKey = bobKeys.getPublic();

        SelfSignedX509CertificateAndSigningKey cxfcaSelfSignedX509CertificateAndSigningKey = createExtensionSelfSigned();
        SelfSignedX509CertificateAndSigningKey johnSelfSignedX509CertificateAndSigningKey = createBasicSelfSigned(JOHN_DN, SHA_256_RSA);
        SelfSignedX509CertificateAndSigningKey maxSelfSignedX509CertificateAndSigningKey = createBasicSelfSigned(MAX_DN, SHA_1_RSA);

        X509Certificate aliceCertificate = createCertificate(cxfcaSelfSignedX509CertificateAndSigningKey, alicePublicKey, ALICE_DN, ALICE_SERIAL_NUMBER);
        X509Certificate bobCertificate = createCertificate(cxfcaSelfSignedX509CertificateAndSigningKey, bobPublicKey, BOB_DN, BOB_SERIAL_NUMBER);

        KeyStore aliceKeyStore = createChainedKeyStore(cxfcaSelfSignedX509CertificateAndSigningKey, aliceCertificate, aliceSigningKey, ALICE_ALIAS);
        aliceKeyStore.setCertificateEntry(BOB_ALIAS, bobCertificate);

        KeyStore bobKeyStore = createChainedKeyStore(cxfcaSelfSignedX509CertificateAndSigningKey, bobCertificate, bobSigningKey, BOB_ALIAS);
        bobKeyStore.setCertificateEntry(ALICE_ALIAS, aliceCertificate);
        bobKeyStore.setCertificateEntry(JOHN_ALIAS, johnSelfSignedX509CertificateAndSigningKey.getSelfSignedCertificate());

        KeyStore johnKeyStore = createSelfSignedKeyStore(johnSelfSignedX509CertificateAndSigningKey, JOHN_ALIAS);
        johnKeyStore.setCertificateEntry(BOB_ALIAS, bobCertificate);

        KeyStore maxKeyStore = createSelfSignedKeyStore(maxSelfSignedX509CertificateAndSigningKey, MAX_ALIAS);
        maxKeyStore.setCertificateEntry(BOB_ALIAS, bobCertificate);

        createTemporaryKeyStoreFile(aliceKeyStore, ALICE_JKS_FILE);
        createTemporaryKeyStoreFile(bobKeyStore, BOB_JKS_FILE);
        createTemporaryKeyStoreFile(johnKeyStore, JOHN_JKS_FILE);
        createTemporaryKeyStoreFile(maxKeyStore, MAX_JKS_FILE);
    }

    public static void main(String[] args) throws Exception {
        setUpKeyStores();
    }
}
