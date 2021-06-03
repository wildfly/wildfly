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

package org.wildfly.test.integration.elytron.certs.crl;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.math.BigInteger;
import java.net.URL;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.KeyStore;
import java.security.Principal;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.Security;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;

import javax.security.auth.x500.X500Principal;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.as.arquillian.api.ServerSetup;
import org.jboss.as.controller.client.ModelControllerClient;
import org.jboss.as.test.integration.management.util.CLIWrapper;
import org.jboss.as.test.shared.CliUtils;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;

import org.wildfly.security.x500.cert.BasicConstraintsExtension;
import org.wildfly.security.x500.cert.SelfSignedX509CertificateAndSigningKey;
import org.wildfly.security.x500.cert.X509CertificateBuilder;
import org.wildfly.test.integration.elytron.certs.CommonBase;
import org.wildfly.test.integration.elytron.util.WelcomeContent;
import org.wildfly.test.security.common.AbstractElytronSetupTask;
import org.wildfly.test.security.common.elytron.CertificateRevocationList;
import org.wildfly.test.security.common.elytron.ConfigurableElement;
import org.wildfly.test.security.common.elytron.CredentialReference;
import org.wildfly.test.security.common.elytron.Path;
import org.wildfly.test.security.common.elytron.SimpleKeyManager;
import org.wildfly.test.security.common.elytron.SimpleKeyStore;
import org.wildfly.test.security.common.elytron.SimpleServerSslContext;
import org.wildfly.test.security.common.elytron.SimpleTrustManager;

import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.asn1.x509.CRLReason;
import org.bouncycastle.cert.X509CRLHolder;
import org.bouncycastle.cert.X509v2CRLBuilder;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.openssl.MiscPEMGenerator;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;
import org.bouncycastle.util.io.pem.PemWriter;
import org.junit.Assert;
import org.junit.runner.RunWith;

/**
 * Auxiliary methods and variables for @{@link CertificateRevocationListTestCase}.
 *
 * @author Jan Stourac <jstourac@redhat.com>
 */
@RunWith(Arquillian.class)
@RunAsClient
@ServerSetup({CertificateRevocationListTestBase.CrlServerSetup.class, WelcomeContent.SetupTask.class })
public class CertificateRevocationListTestBase extends CommonBase {

    protected static KeyStore trustStore;
    protected static KeyStore goodCertKeyStore;
    protected static KeyStore revokedCertKeyStore;
    protected static KeyStore otherRevokedCertKeyStore;

    protected static final String PASSWORD = "Elytron";
    protected static final String TWO_WAY_SSL_CONTEXT_NAME = "serverSslContext";
    protected static final String TWO_WAY_MULTIPLE_CRL_SSL_CONTEXT = "otherServerSslContext";
    protected static final String TWO_WAY_SINGLE_CRL_SSL_CONTEXT = "singleCrlSslContext";

    private static final char[] PASSWORD_CHAR = PASSWORD.toCharArray();
    private static final String HTTPS = "https";
    private static final String CA_JKS_LOCATION = "." + File.separator + "target" + File.separator + "test-classes" +
            File.separator + "ca" + File.separator + "jks";
    private static final File WORKING_DIR_CA = new File(CA_JKS_LOCATION);
    private static final File LADYBIRD_FILE = new File(WORKING_DIR_CA, "ladybird.keystore");
    private static final File CHECKED_GOOD_FILE = new File(WORKING_DIR_CA, "checked-good.keystore");
    private static final File CHECKED_REVOKED_FILE = new File(WORKING_DIR_CA, "checked-revoked.keystore");
    private static final File OTHER_REVOKED_FILE = new File(WORKING_DIR_CA, "other-revoked.keystore");
    private static final File TRUST_FILE = new File(WORKING_DIR_CA, "ca.truststore");
    private static final String CA_CRL_LOCATION = "." + File.separator + "target" + File.separator + "test-classes" +
            File.separator + "ca" + File.separator + "crl";
    private static final File WORKING_DIR_CACRL = new File(CA_CRL_LOCATION);
    private static final File CA_BLANK_PEM_CRL = new File(WORKING_DIR_CACRL, "blank.pem");
    private static final File CA_OTHER_PEM_CRL = new File(WORKING_DIR_CACRL, "other.pem");

    private static KeyStore createKeyStore() throws Exception {
        KeyStore ks = KeyStore.getInstance("JKS");
        ks.load(null, null);
        return ks;
    }

    private static void createTemporaryKeyStoreFile(KeyStore keyStore, File outputFile, char[] password)
            throws Exception {
        if (!outputFile.exists()) {
            outputFile.createNewFile();
        }
        try (FileOutputStream fos = new FileOutputStream(outputFile)) {
            keyStore.store(fos, password);
        }
    }

    private static X500Name convertSunStyleToBCStyle(Principal dn) {
        String dnName = dn.getName();
        String[] dnComponents = dnName.split(", ");
        StringBuilder dnBuffer = new StringBuilder(dnName.length());

        dnBuffer.append(dnComponents[dnComponents.length - 1]);
        for (int i = dnComponents.length - 2; i >= 0; i--) {
            dnBuffer.append(',');
            dnBuffer.append(dnComponents[i]);
        }

        return new X500Name(dnBuffer.toString());
    }

    public static void beforeTest() throws Exception {
        Assert.assertTrue(WORKING_DIR_CA.mkdirs());
        Assert.assertTrue(WORKING_DIR_CACRL.mkdirs());

        KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("RSA");
        Security.addProvider(new BouncyCastleProvider());

        X500Principal issuerDN = new X500Principal(
                "CN=Elytron CA, ST=Elytron, C=UK, EMAILADDRESS=elytron@wildfly.org, O=Root Certificate Authority");

        KeyStore ladybirdKeyStore = createKeyStore();
        trustStore = createKeyStore();

        // Generates the issuer certificate and adds it to the keystores
        SelfSignedX509CertificateAndSigningKey issuerSelfSignedX509CertificateAndSigningKey =
                SelfSignedX509CertificateAndSigningKey.builder()
                        .setDn(issuerDN)
                        .setKeyAlgorithmName("RSA")
                        .setSignatureAlgorithmName("SHA256withRSA")
                        .addExtension(false, "BasicConstraints","CA:true,pathlen:2147483647")
                        .build();
        X509Certificate issuerCertificate = issuerSelfSignedX509CertificateAndSigningKey.getSelfSignedCertificate();
        ladybirdKeyStore.setCertificateEntry("ca", issuerCertificate);
        trustStore.setCertificateEntry("mykey", issuerCertificate);

        // Generates certificate and keystore for Ladybird
        KeyPair ladybirdKeys = keyPairGenerator.generateKeyPair();
        PrivateKey ladybirdSigningKey = ladybirdKeys.getPrivate();
        PublicKey ladybirdPublicKey = ladybirdKeys.getPublic();

        X509Certificate ladybirdCertificate = new X509CertificateBuilder().setIssuerDn(issuerDN)
                .setSubjectDn(new X500Principal("OU=Elytron, O=Elytron, C=UK, ST=Elytron, CN=localhost"))
                .setSignatureAlgorithmName("SHA256withRSA")
                .setSigningKey(issuerSelfSignedX509CertificateAndSigningKey.getSigningKey())
                .setPublicKey(ladybirdPublicKey)
                .setSerialNumber(new BigInteger("3"))
                .addExtension(new BasicConstraintsExtension(false, false, -1))
                .build();
        ladybirdKeyStore.setKeyEntry("ladybird", ladybirdSigningKey, PASSWORD_CHAR,
                new X509Certificate[]{ladybirdCertificate, issuerCertificate});

        // Generates GOOD certificate - it is not part of CRL
        KeyPair checkedGoodKeys = keyPairGenerator.generateKeyPair();
        PrivateKey checkedGoodSigningKey = checkedGoodKeys.getPrivate();
        PublicKey checkedGoodPublicKey = checkedGoodKeys.getPublic();

        X509Certificate checkedGoodCertificate = new X509CertificateBuilder().setIssuerDn(issuerDN)
                .setSubjectDn(new X500Principal("OU=Elytron, O=Elytron, C=UK, ST=Elytron, CN=localhost"))
                .setSignatureAlgorithmName("SHA256withRSA")
                .setSigningKey(issuerSelfSignedX509CertificateAndSigningKey.getSigningKey())
                .setPublicKey(checkedGoodPublicKey)
                .setSerialNumber(new BigInteger("16"))
                .addExtension(new BasicConstraintsExtension(false, false, -1))
                .build();
        goodCertKeyStore = createKeyStore();
        goodCertKeyStore.setCertificateEntry("ca", issuerCertificate);
        goodCertKeyStore.setKeyEntry("localhost", checkedGoodSigningKey, PASSWORD_CHAR,
                new X509Certificate[]{checkedGoodCertificate, issuerCertificate});
        createTemporaryKeyStoreFile(goodCertKeyStore, CHECKED_GOOD_FILE, PASSWORD_CHAR);

        // Generates REVOKED certificate - this one will be part of CRL
        KeyPair checkedRevokedKeys = keyPairGenerator.generateKeyPair();
        PrivateKey checkedRevokedSigningKey = checkedRevokedKeys.getPrivate();
        PublicKey checkedRevokedPublicKey = checkedRevokedKeys.getPublic();

        X509Certificate checkedRevokedCertificate = new X509CertificateBuilder().setIssuerDn(issuerDN)
                .setSubjectDn(new X500Principal("OU=Elytron, O=Elytron, C=UK, ST=Elytron, CN=localhost"))
                .setSignatureAlgorithmName("SHA256withRSA")
                .setSigningKey(issuerSelfSignedX509CertificateAndSigningKey.getSigningKey())
                .setPublicKey(checkedRevokedPublicKey)
                .setSerialNumber(new BigInteger("17"))
                .addExtension(new BasicConstraintsExtension(false, false, -1))
                .build();
        revokedCertKeyStore = createKeyStore();
        revokedCertKeyStore.setCertificateEntry("ca", issuerCertificate);
        revokedCertKeyStore.setKeyEntry("localhost", checkedRevokedSigningKey, PASSWORD_CHAR,
                new X509Certificate[]{checkedRevokedCertificate, issuerCertificate});
        createTemporaryKeyStoreFile(revokedCertKeyStore, CHECKED_REVOKED_FILE, PASSWORD_CHAR);

        // Creates the CRL for ca/crl/blank.pem
        prepareCrlFiles(issuerCertificate, issuerSelfSignedX509CertificateAndSigningKey, checkedRevokedCertificate, CA_BLANK_PEM_CRL);

        // Generates another REVOKED certificate - this one will be part of another CRL
        KeyPair otherRevokedKeys = keyPairGenerator.generateKeyPair();
        PrivateKey otherRevokedSigningKey = otherRevokedKeys.getPrivate();
        PublicKey otherRevokedPublicKey = otherRevokedKeys.getPublic();

        X509Certificate otherRevokedCertificate = new X509CertificateBuilder().setIssuerDn(issuerDN)
                .setSubjectDn(new X500Principal("OU=Elytron, O=Elytron, C=UK, ST=Elytron, CN=localhost"))
                .setSignatureAlgorithmName("SHA256withRSA")
                .setSigningKey(issuerSelfSignedX509CertificateAndSigningKey.getSigningKey())
                .setPublicKey(otherRevokedPublicKey)
                .setSerialNumber(new BigInteger("17"))
                .addExtension(new BasicConstraintsExtension(false, false, -1))
                .build();
        otherRevokedCertKeyStore = createKeyStore();
        otherRevokedCertKeyStore.setCertificateEntry("ca", issuerCertificate);
        otherRevokedCertKeyStore.setKeyEntry("localhost", otherRevokedSigningKey, PASSWORD_CHAR,
                new X509Certificate[]{otherRevokedCertificate, issuerCertificate});
        createTemporaryKeyStoreFile(otherRevokedCertKeyStore, OTHER_REVOKED_FILE, PASSWORD_CHAR);

        // Creates the CRL for ca/crl/other.pem
        prepareCrlFiles(issuerCertificate, issuerSelfSignedX509CertificateAndSigningKey, otherRevokedCertificate, CA_OTHER_PEM_CRL);

        // Create the temporary files
        createTemporaryKeyStoreFile(ladybirdKeyStore, LADYBIRD_FILE, PASSWORD_CHAR); // keystore for server config
        createTemporaryKeyStoreFile(trustStore, TRUST_FILE, PASSWORD_CHAR); // trust file for server config
    }

    private static void prepareCrlFiles(X509Certificate intermediateIssuerCertificate,
            SelfSignedX509CertificateAndSigningKey issuerSelfSignedX509CertificateAndSigningKey,
            X509Certificate revoked, File crlFile) throws Exception {
        // Used for all CRLs
        Calendar calendar = Calendar.getInstance();
        Date currentDate = calendar.getTime();
        calendar.add(Calendar.YEAR, 1);
        Date nextYear = calendar.getTime();
        calendar.add(Calendar.YEAR, -1);
        calendar.add(Calendar.SECOND, -30);
        Date revokeDate = calendar.getTime();

        X509v2CRLBuilder caBlankCrlBuilder =
                new X509v2CRLBuilder(convertSunStyleToBCStyle(intermediateIssuerCertificate.getIssuerDN()),
                        currentDate);
        caBlankCrlBuilder.addCRLEntry(revoked.getSerialNumber(), currentDate, CRLReason.unspecified);

        X509CRLHolder caBlankCrlHolder = caBlankCrlBuilder.setNextUpdate(nextYear).build(
                new JcaContentSignerBuilder("SHA256withRSA").setProvider("BC").build(
                        issuerSelfSignedX509CertificateAndSigningKey.getSigningKey()));

        PemWriter caBlankCrlOutput = new PemWriter(new OutputStreamWriter(new FileOutputStream(crlFile)));
        caBlankCrlOutput.writeObject(new MiscPEMGenerator(caBlankCrlHolder));
        caBlankCrlOutput.close();
    }

    public static void afterTest() {
        Assert.assertTrue(LADYBIRD_FILE.delete());
        Assert.assertTrue(CHECKED_GOOD_FILE.delete());
        Assert.assertTrue(CHECKED_REVOKED_FILE.delete());
        Assert.assertTrue(OTHER_REVOKED_FILE.delete());
        Assert.assertTrue(TRUST_FILE.delete());
        Assert.assertTrue(WORKING_DIR_CA.delete());
        Assert.assertTrue(CA_BLANK_PEM_CRL.delete());
        Assert.assertTrue(CA_OTHER_PEM_CRL.delete());
        Assert.assertTrue(WORKING_DIR_CACRL.delete());
    }

    protected static void configureSSLContext(String sslContextName) throws Exception {
        try(CLIWrapper cli = new CLIWrapper(true)) {
            cli.sendLine("batch");
            cli.sendLine(String.format("/subsystem=undertow/server=default-server/https-listener=%s:undefine-attribute" +
                    "(name=security-realm)", HTTPS));
            cli.sendLine(String.format("/subsystem=undertow/server=default-server/https-listener=%s:write-attribute" +
                    "(name=ssl-context,value=%s)", HTTPS, sslContextName));
            cli.sendLine("run-batch");
            cli.sendLine("reload");
        }
    }

    protected void restoreConfiguration() throws Exception {
        try (CLIWrapper cli = new CLIWrapper(true)) {
            cli.sendLine("batch");
            cli.sendLine(String.format("/subsystem=undertow/server=default-server/https-listener=%s:undefine-attribute" +
                    "(name=ssl-context)", HTTPS));
            cli.sendLine(String.format("/subsystem=undertow/server=default-server/https-listener=%s:write-attribute" +
                    "(name=security-realm,value=ApplicationRealm)", HTTPS));
            cli.sendLine("run-batch");
            cli.sendLine("reload");
        }
    }

    // This is a dummy deployment just to CrlServerSetup task is kicked off. Not sure about better way ATM.
    @Deployment
    protected static WebArchive createDeployment() {
        return createDeployment("dummy");
    }

    @ArquillianResource
    protected URL url;

    protected static WebArchive createDeployment(final String name) {
        return ShrinkWrap.create(WebArchive.class, name + ".war");
    }

    static class CrlServerSetup extends AbstractElytronSetupTask {

        @Override
        protected void setup(ModelControllerClient modelControllerClient) throws Exception {
            CertificateRevocationListTestBase.beforeTest();

            super.setup(modelControllerClient);
        }

        @Override
        protected void tearDown(ModelControllerClient modelControllerClient) throws Exception {
            super.tearDown(modelControllerClient);

            CertificateRevocationListTestBase.afterTest();
        }

        @Override
        protected ConfigurableElement[] getConfigurableElements() {
            LinkedList<ConfigurableElement> elements = new LinkedList<>();

            CredentialReference serverKeyStoreCredRef = CredentialReference.builder().withClearText(PASSWORD).build();

            // Prepare server key-store and key-manager for server ssl context
            Path serverKeyStorePath = Path.builder().withPath(CliUtils.asAbsolutePath(LADYBIRD_FILE)).build();

            SimpleKeyStore serverKeyStore = SimpleKeyStore.builder().withName("serverKeyStore").withCredentialReference(
                    serverKeyStoreCredRef).withType("JKS").withPath(serverKeyStorePath).build();
            elements.add(serverKeyStore);

            SimpleKeyManager serverKeyManager = SimpleKeyManager.builder().withName("serverKeyManager").withKeyStore(
                    serverKeyStore.getName()).withCredentialReference(serverKeyStoreCredRef).build();
            elements.add(serverKeyManager);

            // Prepare server trust-store (with CRL configuration) and related key-manager for server ssl context
            Path serverTrustStorePath = Path.builder().withPath(CliUtils.asAbsolutePath(TRUST_FILE)).build();
            SimpleKeyStore serverTrustStore =
                    SimpleKeyStore.builder().withName("serverTrustStore").withCredentialReference(
                            serverKeyStoreCredRef).withType("JKS").withPath(serverTrustStorePath).build();
            elements.add(serverTrustStore);

            CertificateRevocationList crl =
                    CertificateRevocationList.builder().withPath(CliUtils.asAbsolutePath(CA_BLANK_PEM_CRL)).build();
            SimpleTrustManager serverTrustManager =
                    SimpleTrustManager.builder().withName("serverTrustManager").withKeyStore(
                            serverTrustStore.getName()).withSoftFail(false).withCrl(crl).withAlgorithm("PKIX").build();
            elements.add(serverTrustManager);

            // Prepare alternative server trust manager (with multiple CRLs configuration) to test CRLs support
            CertificateRevocationList crl2 =
                    CertificateRevocationList.builder().withPath(CliUtils.asAbsolutePath(CA_OTHER_PEM_CRL)).build();

            List<CertificateRevocationList> crls = new ArrayList<>();
            crls.add(crl);
            crls.add(crl2);

            SimpleTrustManager multipleCrlServerTrustManager =
                    SimpleTrustManager.builder().withName("multipleCrlServerTrustManager").withKeyStore(
                            serverTrustStore.getName()).withSoftFail(false).withCrls(crls).withAlgorithm("PKIX").build();
            elements.add(multipleCrlServerTrustManager);


            // Prepare trust manager that configures a single CRL using the certificate-revocation-lists attribute
            crls.remove(crl2);
            SimpleTrustManager singleCrlServerTrustManager =
                    SimpleTrustManager.builder().withName("singleCrlServerTrustManager").withKeyStore(
                            serverTrustStore.getName()).withSoftFail(false).withCrls(crls).withAlgorithm("PKIX").build();

            elements.add(singleCrlServerTrustManager);

            // Create two way server ssl context with prepared key and trust managers.
            SimpleServerSslContext twoWayServerSslContext =
                    SimpleServerSslContext.builder().withName(TWO_WAY_SSL_CONTEXT_NAME).withKeyManagers(
                            serverKeyManager.getName()).withNeedClientAuth(true).withTrustManagers(
                            serverTrustManager.getName()).build();
            elements.add(twoWayServerSslContext);

            // Create another two way server ssl context to use the trust manager that supports multiple CRLs
            SimpleServerSslContext otherTwoWaySslContext =
                    SimpleServerSslContext.builder().withName(TWO_WAY_MULTIPLE_CRL_SSL_CONTEXT).withKeyManagers(
                            serverKeyManager.getName()).withNeedClientAuth(true).withTrustManagers(
                            multipleCrlServerTrustManager.getName()).build();
            elements.add(otherTwoWaySslContext);

            // Creates another two way server ssl context to use the trust manager that has configured a single CRL
            // using the certificate-revocation-lists attribute
            SimpleServerSslContext singleCrlTwoWaySslContext =
                    SimpleServerSslContext.builder().withName(TWO_WAY_SINGLE_CRL_SSL_CONTEXT).withKeyManagers(
                            serverKeyManager.getName()).withNeedClientAuth(true).withTrustManagers(
                            singleCrlServerTrustManager.getName()).build();
            elements.add(singleCrlTwoWaySslContext);

            return elements.toArray(new ConfigurableElement[]{});
        }
    }
}
