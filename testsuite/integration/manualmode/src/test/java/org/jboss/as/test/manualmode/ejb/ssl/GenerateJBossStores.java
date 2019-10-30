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
package org.jboss.as.test.manualmode.ejb.ssl;

import java.io.File;
import java.io.FileOutputStream;
import java.security.KeyStore;
import java.security.cert.X509Certificate;

import javax.security.auth.x500.X500Principal;

import org.wildfly.security.x500.cert.SelfSignedX509CertificateAndSigningKey;

/**
 * Generates the keystores and truststore needed for the manualmode tests
 * @author <a href="mailto:jucook@redhat.com">Justin Cook</a>
 */
public class GenerateJBossStores {
    private static final String SERVER_KEYSTORE_ALIAS = "jbossalias";
    private static final String SERVER_KEYSTORE_PASSWORD = "JBossPassword";
    private static final String SERVER_KEYSTORE_FILENAME = "jbossServer.keystore";

    private static final String CLIENT_KEYSTORE_FILENAME = "jbossClient.keystore";
    private static final String CLIENT_TRUSTSTORE_FILENAME = "jbossClient.truststore";
    private static final String CLIENT_KEYSTORE_ALIAS = "clientalias";
    private static final String CLIENT_KEYSTORE_PASSWORD = "clientPassword";

    private static final String CLIENT_ALIAS_DN = "CN=localhost, OU=Client Unit, O=JBoss, L=Pune, ST=Maharashtra, C=IN";
    private static final String JBOSS_ALIAS_DN = "CN=localhost, OU=JBoss Unit, O=JBoss, L=Pune, ST=Maharashtra, C=IN";

    private static final String WORKING_DIRECTORY_LOCATION = GenerateJBossStores.class.getProtectionDomain().getCodeSource().getLocation().getPath() + "ejb3/ssl";

    private static final File SERVER_KEY_STORE_FILE = new File(WORKING_DIRECTORY_LOCATION, SERVER_KEYSTORE_FILENAME);
    private static final File CLIENT_KEY_STORE_FILE = new File(WORKING_DIRECTORY_LOCATION, CLIENT_KEYSTORE_FILENAME);
    private static final File TRUST_STORE_FILE = new File(WORKING_DIRECTORY_LOCATION, CLIENT_TRUSTSTORE_FILENAME);

    private static final String SHA_1_RSA = "SHA1withRSA";

    private static KeyStore loadKeyStore() throws Exception {
        KeyStore ks = KeyStore.getInstance("JKS");
        ks.load(null, null);
        return ks;
    }

    private static SelfSignedX509CertificateAndSigningKey createSelfSigned(String DN) {
        return SelfSignedX509CertificateAndSigningKey.builder()
                .setDn(new X500Principal(DN))
                .setKeyAlgorithmName("RSA")
                .setSignatureAlgorithmName(SHA_1_RSA)
                .setKeySize(1024)
                .build();
    }

    private static KeyStore createKeyStore(SelfSignedX509CertificateAndSigningKey selfSignedX509CertificateAndSigningKey, String alias, char[] password) throws Exception {
        KeyStore keyStore = loadKeyStore();

        X509Certificate certificate = selfSignedX509CertificateAndSigningKey.getSelfSignedCertificate();
        keyStore.setKeyEntry(alias, selfSignedX509CertificateAndSigningKey.getSigningKey(), password, new X509Certificate[]{certificate});

        return keyStore;
    }

    private static void addCertEntry(SelfSignedX509CertificateAndSigningKey selfSignedX509CertificateAndSigningKey, KeyStore trustStore, String alias) throws Exception {
        X509Certificate certificate = selfSignedX509CertificateAndSigningKey.getSelfSignedCertificate();
        trustStore.setCertificateEntry(alias, certificate);
    }

    private static void createTemporaryKeyStoreFile(KeyStore keyStore, File outputFile, char[] password) throws Exception {
        try (FileOutputStream fos = new FileOutputStream(outputFile)){
            keyStore.store(fos, password);
        }
    }

    private static void setUpKeyStores() throws Exception {
        File workingDir = new File(WORKING_DIRECTORY_LOCATION);
        if (workingDir.exists() == false) {
            workingDir.mkdirs();
        }

        SelfSignedX509CertificateAndSigningKey clientAliasSelfSignedX509CertificateAndSigningKey = createSelfSigned(CLIENT_ALIAS_DN);
        SelfSignedX509CertificateAndSigningKey jbossAliasSelfSignedX509CertificateAndSigningKey = createSelfSigned(JBOSS_ALIAS_DN);

        KeyStore serverKeyStore = createKeyStore(jbossAliasSelfSignedX509CertificateAndSigningKey, SERVER_KEYSTORE_ALIAS, SERVER_KEYSTORE_PASSWORD.toCharArray());
        addCertEntry(clientAliasSelfSignedX509CertificateAndSigningKey, serverKeyStore, CLIENT_KEYSTORE_ALIAS);
        KeyStore clientKeyStore = createKeyStore(clientAliasSelfSignedX509CertificateAndSigningKey, CLIENT_KEYSTORE_ALIAS, CLIENT_KEYSTORE_PASSWORD.toCharArray());
        KeyStore trustStore = createKeyStore(clientAliasSelfSignedX509CertificateAndSigningKey, CLIENT_KEYSTORE_ALIAS, CLIENT_KEYSTORE_PASSWORD.toCharArray());
        addCertEntry(jbossAliasSelfSignedX509CertificateAndSigningKey, trustStore, SERVER_KEYSTORE_ALIAS);

        createTemporaryKeyStoreFile(serverKeyStore, SERVER_KEY_STORE_FILE, SERVER_KEYSTORE_PASSWORD.toCharArray());
        createTemporaryKeyStoreFile(clientKeyStore, CLIENT_KEY_STORE_FILE, CLIENT_KEYSTORE_PASSWORD.toCharArray());
        createTemporaryKeyStoreFile(trustStore, TRUST_STORE_FILE, CLIENT_KEYSTORE_PASSWORD.toCharArray());
    }

    public static void main(String[] args) throws Exception {
        setUpKeyStores();
    }
}
