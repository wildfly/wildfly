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

import org.wildfly.security.x500.cert.BasicConstraintsExtension;
import org.wildfly.security.x500.cert.SelfSignedX509CertificateAndSigningKey;

/**
 * Generates the jks and properties files needed for some ws tests
 * @author <a href="mailto:jucook@redhat.com">Justin Cook</a>
 */
public class GenerateJKSAndPropertiesFiles {
    private static final char[] ACTAS_PASSWORD = "aapass".toCharArray();
    private static final char[] SERVICE_PASSWORD = "sspass".toCharArray();
    private static final char[] STS_PASSWORD = "stsspass".toCharArray();
    private static final char[] CLIENT_STORE_PASSWORD = "cspass".toCharArray();

    private static final char[] ACTAS_KEY_PASSWORD = "aspass".toCharArray();
    private static final char[] SERVICE_KEY_PASSWORD = "skpass".toCharArray();
    private static final char[] STS_KEY_PASSWORD = "stskpass".toCharArray();
    private static final char[] CLIENT_KEY_PASSWORD = "ckpass".toCharArray();

    private static final String ACTAS_ALIAS = "myactaskey";
    private static final String ACTAS_ALIAS_TRUST = "actasclient";
    private static final String SERVICE_ALIAS = "myservicekey";
    private static final String STS_ALIAS = "mystskey";
    private static final String LOCALHOST_ALIAS = "mytomcatkey";
    private static final String CLIENT_ALIAS = "myclientkey";

    private static final String WEB_INF_WORKING_DIRECTORY_LOCATION = GenerateJKSAndPropertiesFiles.class.getProtectionDomain().getCodeSource().getLocation().getPath() + "org/jboss/as/test/integration/ws/wsse/trust/WEB-INF";
    private static final String META_INF_WORKING_DIRECTORY_LOCATION = GenerateJKSAndPropertiesFiles.class.getProtectionDomain().getCodeSource().getLocation().getPath() + "org/jboss/as/test/integration/ws/wsse/trust/META-INF";

    private static final String ACTAS_JKS_NAME = "actasstore.jks";
    private static final String SERVICE_JKS_NAME = "servicestore.jks";
    private static final String STS_JKS_NAME = "stsstore.jks";
    private static final String CLIENT_STORE_JKS_NAME = "clientstore.jks";

    private static final File ACTAS_JKS_FILE = new File(WEB_INF_WORKING_DIRECTORY_LOCATION, ACTAS_JKS_NAME);
    private static final File SERVICE_JKS_FILE = new File(WEB_INF_WORKING_DIRECTORY_LOCATION, SERVICE_JKS_NAME);
    private static final File STS_JKS_FILE = new File(WEB_INF_WORKING_DIRECTORY_LOCATION, STS_JKS_NAME);
    private static final File CLIENT_STORE_JKS_FILE = new File(META_INF_WORKING_DIRECTORY_LOCATION, CLIENT_STORE_JKS_NAME);

    private static final String ACTAS_DN = "CN=www.actas.com, OU=IT Department, O=Sample ActAs Web Service -- NOT FOR PRODUCTION, L=Dayton, ST=Ohio, C=US";
    private static final String SERVICE_DN = "EMAILADDRESS=service@service.com, CN=www.service.com, OU=IT Department, O=Sample Web Service Provider -- NOT FOR PRODUCTION, L=Buffalo, ST=New York, C=US";
    private static final String STS_DN = "EMAILADDRESS=sts@sts.com, CN=www.sts.com, OU=IT Department, O=Sample STS -- NOT FOR PRODUCTION, L=Baltimore, ST=Maryland, C=US";
    private static final String LOCALHOST_DN = "CN=localhost";
    private static final String CLIENT_DN = "EMAILADDRESS=client@client.com, CN=www.client.com, OU=IT Department, O=Sample Client -- NOT FOR PRODUCTION, L=Niagara Falls, ST=New York, C=US";

    private static final String SHA_1_RSA = "SHA1withRSA";
    private static final String SHA_256_RSA = "SHA256withRSA";

    private static KeyStore loadKeyStore() throws Exception {
        KeyStore ks = KeyStore.getInstance("JKS");
        ks.load(null, null);
        return ks;
    }

    private static SelfSignedX509CertificateAndSigningKey createBasicSelfSigned(String DN, String signatureAlgorithmName, int keySize) {
        return SelfSignedX509CertificateAndSigningKey.builder()
                .setDn(new X500Principal(DN))
                .setKeyAlgorithmName("RSA")
                .setSignatureAlgorithmName(signatureAlgorithmName)
                .setKeySize(keySize)
                .build();
    }

    private static SelfSignedX509CertificateAndSigningKey createExtensionSelfSigned(String DN) {
        return SelfSignedX509CertificateAndSigningKey.builder()
                .setDn(new X500Principal(DN))
                .setKeyAlgorithmName("RSA")
                .setSignatureAlgorithmName(SHA_1_RSA)
                .setKeySize(1024)
                .addExtension(new BasicConstraintsExtension(false, true, 2147483647))
                .build();
    }

    private static KeyStore createKeyStore(SelfSignedX509CertificateAndSigningKey selfSignedX509CertificateAndSigningKey, String alias, char[] password) throws Exception {
        KeyStore keyStore = loadKeyStore();

        X509Certificate certificate = selfSignedX509CertificateAndSigningKey.getSelfSignedCertificate();
        keyStore.setKeyEntry(alias, selfSignedX509CertificateAndSigningKey.getSigningKey(), password, new X509Certificate[]{certificate});

        return keyStore;
    }

    private static void createTemporaryKeyStoreFile(KeyStore keyStore, File outputFile, char[] password) throws Exception {
        try (FileOutputStream fos = new FileOutputStream(outputFile)){
            keyStore.store(fos, password);
        }
    }

    private static void setUpKeyStores() throws Exception {
        File workingDir = new File(WEB_INF_WORKING_DIRECTORY_LOCATION);
        if (workingDir.exists() == false) {
            workingDir.mkdirs();
        }

        SelfSignedX509CertificateAndSigningKey actasSelfSignedX509CertificateAndSigningKey = createBasicSelfSigned(ACTAS_DN, SHA_256_RSA, 2048);
        SelfSignedX509CertificateAndSigningKey serviceSelfSignedX509CertificateAndSigningKey = createExtensionSelfSigned(SERVICE_DN);
        SelfSignedX509CertificateAndSigningKey stsSelfSignedX509CertificateAndSigningKey = createExtensionSelfSigned(STS_DN);
        SelfSignedX509CertificateAndSigningKey localhostSelfSignedX509CertificateAndSigningKey = createBasicSelfSigned(LOCALHOST_DN, SHA_1_RSA, 1024);
        SelfSignedX509CertificateAndSigningKey clientSelfSignedX509CertificateAndSigningKey = createExtensionSelfSigned(CLIENT_DN);

        KeyStore actasKeyStore = createKeyStore(actasSelfSignedX509CertificateAndSigningKey, ACTAS_ALIAS, ACTAS_KEY_PASSWORD);
        actasKeyStore.setCertificateEntry(STS_ALIAS, stsSelfSignedX509CertificateAndSigningKey.getSelfSignedCertificate());

        KeyStore serviceKeyStore = createKeyStore(serviceSelfSignedX509CertificateAndSigningKey, SERVICE_ALIAS, SERVICE_KEY_PASSWORD);
        serviceKeyStore.setCertificateEntry(STS_ALIAS, stsSelfSignedX509CertificateAndSigningKey.getSelfSignedCertificate());
        serviceKeyStore.setCertificateEntry(LOCALHOST_ALIAS, localhostSelfSignedX509CertificateAndSigningKey.getSelfSignedCertificate());

        KeyStore stsKeyStore = createKeyStore(stsSelfSignedX509CertificateAndSigningKey, STS_ALIAS, STS_KEY_PASSWORD);
        stsKeyStore.setCertificateEntry(SERVICE_ALIAS, serviceSelfSignedX509CertificateAndSigningKey.getSelfSignedCertificate());
        stsKeyStore.setCertificateEntry(CLIENT_ALIAS, clientSelfSignedX509CertificateAndSigningKey.getSelfSignedCertificate());
        stsKeyStore.setCertificateEntry(ACTAS_ALIAS_TRUST, actasSelfSignedX509CertificateAndSigningKey.getSelfSignedCertificate());

        KeyStore clientStoreKeyStore = createKeyStore(clientSelfSignedX509CertificateAndSigningKey, CLIENT_ALIAS, CLIENT_KEY_PASSWORD);
        clientStoreKeyStore.setCertificateEntry(ACTAS_ALIAS, actasSelfSignedX509CertificateAndSigningKey.getSelfSignedCertificate());
        clientStoreKeyStore.setCertificateEntry(STS_ALIAS, stsSelfSignedX509CertificateAndSigningKey.getSelfSignedCertificate());
        clientStoreKeyStore.setCertificateEntry(SERVICE_ALIAS, serviceSelfSignedX509CertificateAndSigningKey.getSelfSignedCertificate());
        clientStoreKeyStore.setCertificateEntry(LOCALHOST_ALIAS, localhostSelfSignedX509CertificateAndSigningKey.getSelfSignedCertificate());

        createTemporaryKeyStoreFile(actasKeyStore, ACTAS_JKS_FILE, ACTAS_PASSWORD);
        createTemporaryKeyStoreFile(serviceKeyStore, SERVICE_JKS_FILE, SERVICE_PASSWORD);
        createTemporaryKeyStoreFile(stsKeyStore, STS_JKS_FILE, STS_PASSWORD);
        createTemporaryKeyStoreFile(clientStoreKeyStore, CLIENT_STORE_JKS_FILE, CLIENT_STORE_PASSWORD);
    }

    public static void main(String[] args) throws Exception {
        setUpKeyStores();
    }
}
