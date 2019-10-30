/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2012, Red Hat, Inc., and individual contributors
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
package org.wildfly.test.integration.security.picketlink.federation.util;

import java.io.File;
import java.io.FileOutputStream;
import java.security.KeyStore;
import java.security.cert.X509Certificate;

import javax.security.auth.x500.X500Principal;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.wildfly.security.x500.cert.SelfSignedX509CertificateAndSigningKey;

/**
 * @author Pedro Igor
 */
public class FederationArchiveUtil {
    private static final char[] GENERATED_KEYSTORE_PASSWORD = "store123".toCharArray();
    private static final char[] GENERATED_KEY_PASSWORD = "test123".toCharArray();

    private static final String ALIAS = "servercert";

    private static final String WORKING_DIRECTORY_LOCATION = FederationArchiveUtil.class.getProtectionDomain().getCodeSource().getLocation().getPath() + "org/wildfly/test/integration/security/picketlink/federation/util";
    private static final File KEY_STORE_FILE = new File(WORKING_DIRECTORY_LOCATION, "jbid_test_keystore.jks");

    private static final String DN = "CN=jbid test, OU=JBoss, O=JBoss, C=US";
    private static final String MD_5_RSA = "MD5withRSA";

    private static KeyStore loadKeyStore() throws Exception {
        KeyStore ks = KeyStore.getInstance("JKS");
        ks.load(null, null);
        return ks;
    }

    private static SelfSignedX509CertificateAndSigningKey createSelfSigned() {
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
        keyStore.setKeyEntry(ALIAS, selfSignedX509CertificateAndSigningKey.getSigningKey(), GENERATED_KEY_PASSWORD, new X509Certificate[]{certificate});

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

    public static WebArchive identityProvider(String deploymentName) {
        return identityProvider(deploymentName, null, null);
    }

    public static WebArchive identityProvider(String deploymentName, String indexContent, String hostedIndexContent) {
        WebArchive war = ShrinkWrap.create(WebArchive.class, deploymentName);

        war.addAsManifestResource(new StringAsset("Dependencies: org.picketlink meta-inf,org.jboss.dmr meta-inf,org.jboss.as.controller\n"), "MANIFEST.MF");
        war.addAsWebInfResource(FederationArchiveUtil.class.getPackage(), "web.xml", "web.xml");
        war.addAsWebResource(FederationArchiveUtil.class.getPackage(), "login.jsp", "login.jsp");
        war.addAsWebResource(FederationArchiveUtil.class.getPackage(), "login-error.jsp", "login-error.jsp");
        war.add(new StringAsset(indexContent != null ? indexContent : "Welcome to IdP"), "index.jsp");
        war.add(new StringAsset(hostedIndexContent != null ? hostedIndexContent : "Welcome to IdP hosted"), "hosted/index.jsp");
        war.addAsResource(new StringAsset("tomcat=tomcat"), "users.properties");
        war.addAsResource(new StringAsset("tomcat=gooduser"), "roles.properties");

        return war;
    }

    public static WebArchive identityProviderWithKeyStore(String deploymentName) throws Exception {
        setUpKeyStores();

        WebArchive war = identityProvider(deploymentName);

        war.addAsResource(FederationArchiveUtil.class.getPackage(), "jbid_test_keystore.jks", "jbid_test_keystore.jks");

        return war;
    }

    public static WebArchive serviceProvider(String deploymentName) {
        final WebArchive war = ShrinkWrap.create(WebArchive.class, deploymentName);

        war.addAsManifestResource(new StringAsset("Dependencies: org.picketlink meta-inf,org.jboss.dmr meta-inf,org.jboss.as.controller\n"), "MANIFEST.MF");
        war.addAsWebInfResource(FederationArchiveUtil.class.getPackage(), "web.xml", "web.xml");
        war.add(new StringAsset("Welcome to " + deploymentName), "index.jsp");
        war.add(new StringAsset("Logout in progress"), "logout.jsp");

        return war;
    }

    public static WebArchive serviceProviderWithKeyStore(String deploymentName) throws Exception {
        setUpKeyStores();

        WebArchive war = serviceProvider(deploymentName);

        war.addAsResource(FederationArchiveUtil.class.getPackage(), "jbid_test_keystore.jks", "jbid_test_keystore.jks");

        return war;
    }

}
