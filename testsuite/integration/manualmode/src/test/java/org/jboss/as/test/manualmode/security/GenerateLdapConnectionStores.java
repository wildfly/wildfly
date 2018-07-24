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
package org.jboss.as.test.manualmode.security;

import java.io.File;
import java.io.FileOutputStream;
import java.security.KeyStore;
import java.security.cert.X509Certificate;

import javax.security.auth.x500.X500Principal;

import org.wildfly.security.x500.cert.SelfSignedX509CertificateAndSigningKey;

/**
 * Generates the keystores and truststore needed for the outbound LDAP connection tests
 * @author <a href="mailto:jucook@redhat.com">Justin Cook</a>
 */
class GenerateLdapConnectionStores {
    private static final char[] KEYSTORE_PASSWORD = "123456".toCharArray();

    private static final String LDAP_DN = "CN=LDAP";
    private static final String JBAS_DN = "CN=JBAS";

    private static final String LDAP_ALIAS = "server";
    private static final String JBAS_ALIAS = "client";

    private static final String LDAP_KEYSTORE_FILENAME = "ldaps.keystore";
    private static final String JBAS_KEYSTORE_FILENAME = "jbas.keystore";
    private static final String TRUSTSTORE_FILENAME = "jbas.truststore";

    private static final String WORKING_DIRECTORY_LOCATION = GenerateLdapConnectionStores.class.getProtectionDomain().getCodeSource().getLocation().getPath() + "org/jboss/as/test/manualmode/security";

    private static final File LDAP_KEY_STORE_FILE = new File(WORKING_DIRECTORY_LOCATION, LDAP_KEYSTORE_FILENAME);
    private static final File JBAS_KEY_STORE_FILE = new File(WORKING_DIRECTORY_LOCATION, JBAS_KEYSTORE_FILENAME);
    private static final File TRUST_STORE_FILE = new File(WORKING_DIRECTORY_LOCATION, TRUSTSTORE_FILENAME);

    private static final String SHA_256_RSA = "SHA256withRSA";

    private static KeyStore loadKeyStore() throws Exception {
        KeyStore ks = KeyStore.getInstance("JKS");
        ks.load(null, null);
        return ks;
    }

    private static SelfSignedX509CertificateAndSigningKey createSelfSigned(String DN) {
        return SelfSignedX509CertificateAndSigningKey.builder()
                .setDn(new X500Principal(DN))
                .setKeyAlgorithmName("RSA")
                .setSignatureAlgorithmName(SHA_256_RSA)
                .build();
    }

    private static KeyStore createKeyStore(SelfSignedX509CertificateAndSigningKey selfSignedX509CertificateAndSigningKey, String alias) throws Exception {
        KeyStore keyStore = loadKeyStore();

        X509Certificate certificate = selfSignedX509CertificateAndSigningKey.getSelfSignedCertificate();
        keyStore.setKeyEntry(alias, selfSignedX509CertificateAndSigningKey.getSigningKey(), KEYSTORE_PASSWORD, new X509Certificate[]{certificate});

        return keyStore;
    }

    private static KeyStore createTrustStore(SelfSignedX509CertificateAndSigningKey selfSignedX509CertificateAndSigningKey, String alias) throws Exception {
        KeyStore trustStore = loadKeyStore();

        X509Certificate certificate = selfSignedX509CertificateAndSigningKey.getSelfSignedCertificate();
        trustStore.setCertificateEntry(alias, certificate);

        return trustStore;
    }

    private static void createTemporaryKeyStoreFile(KeyStore keyStore, File outputFile) throws Exception {
        try (FileOutputStream fos = new FileOutputStream(outputFile)){
            keyStore.store(fos, KEYSTORE_PASSWORD);
        }
    }

    public static void setUpKeyStores() throws Exception {
        File workingDir = new File(WORKING_DIRECTORY_LOCATION);
        if (workingDir.exists() == false) {
            workingDir.mkdirs();
        }

        SelfSignedX509CertificateAndSigningKey ldapSelfSignedX509CertificateAndSigningKey = createSelfSigned(LDAP_DN);
        SelfSignedX509CertificateAndSigningKey jbasSelfSignedX509CertificateAndSigningKey = createSelfSigned(JBAS_DN);

        KeyStore ldapKeyStore = createKeyStore(ldapSelfSignedX509CertificateAndSigningKey, LDAP_ALIAS);
        KeyStore jbasKeyStore = createKeyStore(jbasSelfSignedX509CertificateAndSigningKey, JBAS_ALIAS);
        KeyStore trustStore = createTrustStore(ldapSelfSignedX509CertificateAndSigningKey, LDAP_ALIAS);

        createTemporaryKeyStoreFile(ldapKeyStore, LDAP_KEY_STORE_FILE);
        createTemporaryKeyStoreFile(jbasKeyStore, JBAS_KEY_STORE_FILE);
        createTemporaryKeyStoreFile(trustStore, TRUST_STORE_FILE);
    }

    public static void removeKeyStores() {
        File[] testFiles = {
                LDAP_KEY_STORE_FILE,
                JBAS_KEY_STORE_FILE,
                TRUST_STORE_FILE
        };
        for (File file : testFiles) {
            if (file.exists()) {
                file.delete();
            }
        }
    }
}
