/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2017, Red Hat, Inc., and individual contributors
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
package org.jboss.as.test.integration.web.security.cert;

import java.io.File;
import java.io.IOException;
import java.security.InvalidKeyException;
import java.security.KeyPair;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.SignatureException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

import org.bouncycastle.operator.OperatorCreationException;
import org.codehaus.plexus.util.FileUtils;
import org.jboss.as.arquillian.api.ServerSetupTask;
import org.jboss.as.arquillian.container.ManagementClient;
import org.jboss.as.test.integration.security.common.SecurityTestConstants;
import org.jboss.as.test.integration.web.security.WebSecurityCommon;
import org.junit.Assert;
import org.wildfly.test.security.common.other.KeyStoreUtils;
import org.wildfly.test.security.common.other.KeyUtils;

/**
 * {@code ServerSetupTask} for the Web CERT tests.
 *
 * @author Jan Stourac
 */
public class WebCERTTestsSetup implements ServerSetupTask {
    static final int HTTPS_PORT = 8380;


    private WebCERTTestsSecurityDomainSetup legacySetup = null;
    private WebCERTTestsElytronSetup elytronSetup = null;

    private static final String NAME = WebCERTTestsSetup.class.getSimpleName();
    private static final File WORK_DIR = new File("target" + File.separatorChar + NAME);
    private static final File SERVER_KEYSTORE_FILE = new File(WORK_DIR, SecurityTestConstants.SERVER_KEYSTORE);
    private static final File SERVER_TRUSTSTORE_FILE = new File(WORK_DIR, SecurityTestConstants.SERVER_TRUSTSTORE);
    private static final File CLIENT_KEYSTORE_FILE = new File(WORK_DIR, SecurityTestConstants.CLIENT_KEYSTORE);
    private static final String PASSWORD = SecurityTestConstants.KEYSTORE_PASSWORD;

    @Override
    public void setup(ManagementClient managementClient, String containerId) throws Exception {
        keyMaterialSetup(WORK_DIR);
        if (WebSecurityCommon.isElytron()) {
            elytronSetup = new WebCERTTestsElytronSetup();
            try {
                elytronSetup.setup(managementClient, containerId);
            } catch (Exception e) {
                throw new RuntimeException("Setting up of Elytron based security domain failed.", e);
            }
        } else {
            legacySetup = new WebCERTTestsSecurityDomainSetup();
            legacySetup.setup(managementClient, containerId);
        }
    }

    @Override
    public void tearDown(ManagementClient managementClient, String containerId) throws Exception {
        if (WebSecurityCommon.isElytron()) {
            try {
                elytronSetup.tearDown(managementClient, containerId);
            } catch (Exception e) {
                throw new RuntimeException("Cleaning up for Elytron based security domain failed.", e);
            }
        } else {
            legacySetup.tearDown(managementClient, containerId);
        }
        FileUtils.deleteDirectory(WORK_DIR);
    }

    public static File getClientKeystoreFile() {
        return CLIENT_KEYSTORE_FILE;
    }

    public static File getServerKeystoreFile() {
        return SERVER_KEYSTORE_FILE;
    }

    public static File getServerTruststoreFile() {
        return SERVER_TRUSTSTORE_FILE;
    }

    public static String getPassword() {
        return PASSWORD;
    }

    protected static void keyMaterialSetup(File workDir) throws Exception {
        FileUtils.deleteDirectory(workDir);
        workDir.mkdirs();
        Assert.assertTrue(workDir.exists());
        Assert.assertTrue(workDir.isDirectory());
        generateCertificatesAndKeystores(PASSWORD, CLIENT_KEYSTORE_FILE, SERVER_KEYSTORE_FILE, SERVER_TRUSTSTORE_FILE);
    }

    private static void generateCertificatesAndKeystores(String keystorePassword, File clientKeystoreFile, File
            serverKeystoreFile, File serverTruststoreFile) throws
            NoSuchAlgorithmException, CertificateException, KeyStoreException, IOException,
            OperatorCreationException, SignatureException, InvalidKeyException {
        KeyPair testClient = KeyUtils.generateKeyPair();
        KeyPair testClient2 = KeyUtils.generateKeyPair();
        KeyPair server = KeyUtils.generateKeyPair();

        String testClientName = "test client";
        String testClient2Name = "test client 2";
        String serverName = "server";
        X509Certificate testClientCert = KeyUtils.generateX509Certificate(testClientName, testClient);
        X509Certificate testClientCert2 = KeyUtils.generateX509Certificate(testClient2Name, testClient2);
        X509Certificate serverCert = KeyUtils.generateX509Certificate(serverName, server);

        KeyStoreUtils.KeyEntry[] keys = new KeyStoreUtils.KeyEntry[]{
                new KeyStoreUtils.KeyEntry(testClientName, testClient, testClientCert),
                new KeyStoreUtils.KeyEntry(testClient2Name, testClient2, testClientCert2),
        };
        KeyStoreUtils.CertEntry[] certs = new KeyStoreUtils.CertEntry[]{
                new KeyStoreUtils.CertEntry(serverName, serverCert),
        };
        KeyStore clientKeystore = KeyStoreUtils.generateKeystore(keys, certs, keystorePassword);

        keys = new KeyStoreUtils.KeyEntry[]{
                new KeyStoreUtils.KeyEntry(serverName, server, serverCert),
        };
        KeyStore serverKeystore = KeyStoreUtils.generateKeystore(keys, null, keystorePassword);

        final String secRealmAliasTestClient = "CN=test client";
        final String secRealmAliasTestClient2 = "CN=test client 2";
        certs = new KeyStoreUtils.CertEntry[]{
                new KeyStoreUtils.CertEntry(testClientName, testClientCert),
                new KeyStoreUtils.CertEntry(testClient2Name, testClientCert2),
                new KeyStoreUtils.CertEntry(secRealmAliasTestClient, testClientCert),
                new KeyStoreUtils.CertEntry(secRealmAliasTestClient2, testClientCert2),
        };
        KeyStore serverTruststore = KeyStoreUtils.generateKeystore(null, certs, keystorePassword);

        KeyStoreUtils.saveKeystore(clientKeystore, keystorePassword, clientKeystoreFile);
        KeyStoreUtils.saveKeystore(serverKeystore, keystorePassword, serverKeystoreFile);
        KeyStoreUtils.saveKeystore(serverTruststore, keystorePassword, serverTruststoreFile);
    }
}
