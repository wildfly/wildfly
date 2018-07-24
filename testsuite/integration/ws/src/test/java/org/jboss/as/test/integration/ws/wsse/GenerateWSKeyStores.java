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
import java.security.KeyStore;
import java.security.cert.X509Certificate;

import javax.security.auth.x500.X500Principal;

import org.wildfly.security.x500.cert.SelfSignedX509CertificateAndSigningKey;

/**
 * Generates the keystores and truststore needed for the ws tests
 * @author <a href="mailto:jucook@redhat.com">Justin Cook</a>
 */
public class GenerateWSKeyStores {
    private static final char[] GENERATED_KEYSTORE_PASSWORD = "changeit".toCharArray();

    private static final String CLIENT_KEYSTORE_ALIAS = "client";
    private static final String CLIENT_TRUSTSTORE_ALIAS = "myclientkey";
    private static final String TEST_KEYSTORE_ALIAS = "tomcat";
    private static final String TEST_TRUSTSTORE_ALIAS = "mykey";

    private static final String WORKING_DIRECTORY_LOCATION = GenerateWSKeyStores.class.getProtectionDomain().getCodeSource().getLocation().getPath() + "org/jboss/as/test/integration/ws/wsse/trust";

    private static final File CLIENT_KEY_STORE_FILE = new File(WORKING_DIRECTORY_LOCATION, "client.keystore");
    private static final File TEST_KEY_STORE_FILE = new File(WORKING_DIRECTORY_LOCATION, "test.keystore");
    private static final File TRUST_STORE_FILE = new File(WORKING_DIRECTORY_LOCATION, "test.truststore");

    private static KeyStore loadKeyStore() throws Exception {
        KeyStore ks = KeyStore.getInstance("JKS");
        ks.load(null, null);
        return ks;
    }

    private static SelfSignedX509CertificateAndSigningKey createSelfSigned() {
        X500Principal DN = new X500Principal("CN=Alessio, OU=JBoss, O=Red Hat, L=Milan, ST=MI, C=IT");

        return SelfSignedX509CertificateAndSigningKey.builder()
                .setDn(DN)
                .setKeyAlgorithmName("RSA")
                .setSignatureAlgorithmName("SHA256withRSA")
                .build();
    }

    private static KeyStore createKeyStore(SelfSignedX509CertificateAndSigningKey selfSignedX509CertificateAndSigningKey, String alias) throws Exception {
        KeyStore keyStore = loadKeyStore();

        X509Certificate certificate = selfSignedX509CertificateAndSigningKey.getSelfSignedCertificate();
        keyStore.setKeyEntry(alias, selfSignedX509CertificateAndSigningKey.getSigningKey(), GENERATED_KEYSTORE_PASSWORD, new X509Certificate[]{certificate});

        return keyStore;
    }

    private static void addCertEntry(SelfSignedX509CertificateAndSigningKey selfSignedX509CertificateAndSigningKey, KeyStore trustStore, String alias) throws Exception {
        X509Certificate certificate = selfSignedX509CertificateAndSigningKey.getSelfSignedCertificate();
        trustStore.setCertificateEntry(alias, certificate);
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

        KeyStore trustStore = loadKeyStore();

        SelfSignedX509CertificateAndSigningKey alessio1SelfSignedX509CertificateAndSigningKey = createSelfSigned();
        SelfSignedX509CertificateAndSigningKey alessio2SelfSignedX509CertificateAndSigningKey = createSelfSigned();

        KeyStore clientKeyStore = createKeyStore(alessio1SelfSignedX509CertificateAndSigningKey, CLIENT_KEYSTORE_ALIAS);
        KeyStore testKeyStore = createKeyStore(alessio2SelfSignedX509CertificateAndSigningKey, TEST_KEYSTORE_ALIAS);
        addCertEntry(alessio1SelfSignedX509CertificateAndSigningKey, trustStore, CLIENT_TRUSTSTORE_ALIAS);
        addCertEntry(alessio2SelfSignedX509CertificateAndSigningKey, trustStore, TEST_TRUSTSTORE_ALIAS);

        createTemporaryKeyStoreFile(clientKeyStore, CLIENT_KEY_STORE_FILE);
        createTemporaryKeyStoreFile(testKeyStore, TEST_KEY_STORE_FILE);
        createTemporaryKeyStoreFile(trustStore, TRUST_STORE_FILE);
    }

    public static void main(String[] args) throws Exception {
        setUpKeyStores();
    }
}
