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
package org.jboss.as.test.integration.security.loginmodules;

import java.io.File;
import java.io.FileOutputStream;
import java.security.KeyStore;
import java.security.cert.X509Certificate;

import javax.security.auth.x500.X500Principal;

import org.wildfly.security.x500.cert.SelfSignedX509CertificateAndSigningKey;

/**
 * Generates the ldaps.jks keystore needed for some of the LDAP tests
 * @author <a href="mailto:jucook@redhat.com">Justin Cook</a>
 */
public class GenerateLDAPSJKSStore {
    private static final char[] GENERATED_KEYSTORE_PASSWORD = "secret".toCharArray();

    private static final String ALIAS = "localhost";

    private static final String WORKING_DIRECTORY_LOCATION = GenerateLDAPSJKSStore.class.getProtectionDomain().getCodeSource().getLocation().getPath() + "org/jboss/as/test/integration/security/loginmodules";
    private static final File KEY_STORE_FILE = new File(WORKING_DIRECTORY_LOCATION, "ldaps.jks");

    private static final String DN = "CN=localhost, OU=JBoss, O=Red Hat, C=US";
    private static final String SHA_1_RSA = "SHA1withRSA";

    private static KeyStore loadKeyStore() throws Exception {
        KeyStore ks = KeyStore.getInstance("JKS");
        ks.load(null, null);
        return ks;
    }

    private static SelfSignedX509CertificateAndSigningKey createSelfSigned() {
        return SelfSignedX509CertificateAndSigningKey.builder()
                .setDn(new X500Principal(DN))
                .setKeyAlgorithmName("RSA")
                .setSignatureAlgorithmName(SHA_1_RSA)
                .setKeySize(1024)
                .build();
    }

    private static KeyStore createKeyStore(SelfSignedX509CertificateAndSigningKey selfSignedX509CertificateAndSigningKey) throws Exception {
        KeyStore keyStore = loadKeyStore();

        X509Certificate certificate = selfSignedX509CertificateAndSigningKey.getSelfSignedCertificate();
        keyStore.setKeyEntry(ALIAS, selfSignedX509CertificateAndSigningKey.getSigningKey(), GENERATED_KEYSTORE_PASSWORD, new X509Certificate[]{certificate});

        return keyStore;
    }

    private static void createTemporaryKeyStoreFile(KeyStore keyStore) throws Exception {
        try (FileOutputStream fos = new FileOutputStream(KEY_STORE_FILE)){
            keyStore.store(fos, GENERATED_KEYSTORE_PASSWORD);
        }
    }

    private static void setUpKeyStores() throws Exception {
        File workingDir = new File(WORKING_DIRECTORY_LOCATION);
        if (workingDir.exists() == false) {
            workingDir.mkdirs();
        }

        SelfSignedX509CertificateAndSigningKey selfSignedX509CertificateAndSigningKey = createSelfSigned();
        KeyStore keyStore = createKeyStore(selfSignedX509CertificateAndSigningKey);
        createTemporaryKeyStoreFile(keyStore);
    }

    public static void main(String[] args) throws Exception {
        setUpKeyStores();
    }
}
