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
package org.jboss.as.test.integration.ws.authentication.policy;

import java.io.File;
import java.io.FileOutputStream;
import java.security.KeyStore;
import java.security.cert.X509Certificate;

import javax.security.auth.x500.X500Principal;

import org.wildfly.security.x500.cert.SelfSignedX509CertificateAndSigningKey;

/**
 * Generates the sts_keystore.jks file needed for AuthenticationPolicyContextTestCase
 * @author <a href="mailto:jucook@redhat.com">Justin Cook</a>
 */
public class GenerateSTSKeystore {
    private static final char[] GENERATED_KEYSTORE_PASSWORD = "testpass".toCharArray();
    private static final char[] GENERATED_KEY_PASSWORD = "keypass".toCharArray();

    private static final String SERVICE_1_ALIAS = "service1";
    private static final String SERVICE_2_ALIAS = "service2";
    private static final String STS_ALIAS = "sts";

    private static final String WORKING_DIRECTORY_LOCATION = GenerateSTSKeystore.class.getProtectionDomain().getCodeSource().getLocation().getPath() + "org/jboss/as/test/integration/ws/authentication/policy/resources/WEB-INF";

    private static final File KEY_STORE_FILE = new File(WORKING_DIRECTORY_LOCATION, "sts_keystore.jks");

    private static final String SERVICE_1_DN = "CN=Service Provider 1, OU=Sample Unit, O=Technobug, L=Miami, ST=FL, C=US";
    private static final String SERVICE_2_DN = "CN=Service Provider 2, OU=Sample Unit, O=Technobug, L=Miami, ST=FL, C=US";
    private static final String STS_DN = "CN=JBoss STS, OU=JBoss Division, O=Red Hat Inc, L=Raleigh, ST=NC, C=US";

    private static final String MD_5_RSA = "MD5withRSA";

    private static KeyStore loadKeyStore() throws Exception {
        KeyStore ks = KeyStore.getInstance("JKS");
        ks.load(null, null);
        return ks;
    }

    private static SelfSignedX509CertificateAndSigningKey createSelfSigned(String DN) {
        return SelfSignedX509CertificateAndSigningKey.builder()
                .setDn(new X500Principal(DN))
                .setKeyAlgorithmName("RSA")
                .setSignatureAlgorithmName(MD_5_RSA)
                .setKeySize(1024)
                .build();
    }

    private static KeyStore createKeyStore(SelfSignedX509CertificateAndSigningKey selfSignedX509CertificateAndSigningKey) throws Exception {
        KeyStore keyStore = loadKeyStore();

        X509Certificate certificate = selfSignedX509CertificateAndSigningKey.getSelfSignedCertificate();
        keyStore.setKeyEntry(STS_ALIAS, selfSignedX509CertificateAndSigningKey.getSigningKey(), GENERATED_KEY_PASSWORD, new X509Certificate[]{certificate});

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

        SelfSignedX509CertificateAndSigningKey service1SelfSignedX509CertificateAndSigningKey = createSelfSigned(SERVICE_1_DN);
        SelfSignedX509CertificateAndSigningKey service2SelfSignedX509CertificateAndSigningKey = createSelfSigned(SERVICE_2_DN);
        SelfSignedX509CertificateAndSigningKey stsSelfSignedX509CertificateAndSigningKey = createSelfSigned(STS_DN);

        KeyStore keyStore = createKeyStore(stsSelfSignedX509CertificateAndSigningKey);
        keyStore.setCertificateEntry(SERVICE_1_ALIAS, service1SelfSignedX509CertificateAndSigningKey.getSelfSignedCertificate());
        keyStore.setCertificateEntry(SERVICE_2_ALIAS, service2SelfSignedX509CertificateAndSigningKey.getSelfSignedCertificate());

        createTemporaryKeyStoreFile(keyStore);
    }

    public static void main(String[] args) throws Exception {
        setUpKeyStores();
    }
}
