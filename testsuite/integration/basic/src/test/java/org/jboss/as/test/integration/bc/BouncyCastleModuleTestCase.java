/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.integration.bc;

import static org.jboss.as.test.shared.PermissionUtils.createPermissionsXmlAsset;

import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.security.Security;
import java.security.SecurityPermission;
import java.util.Arrays;
import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;

import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.logging.Logger;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Verify that BouncyCastle securtiy provider can be loaded and used through JCE api.
 * Basically, this can fail when security provider class isn't in properly signed jar with signature accepted by used JDK.
 * See https://docs.oracle.com/javase/8/docs/technotes/guides/security/crypto/HowToImplAProvider.html#Step1a for details.
 */
@RunWith(Arquillian.class)
public class BouncyCastleModuleTestCase {

    private static final String BC_DEPLOYMENT = "bc-test";
    private static final Logger logger = Logger.getLogger(BouncyCastleModuleTestCase.class);

    @Deployment(name = BC_DEPLOYMENT, testable = true) public static WebArchive createDeployment() {
        WebArchive archive = ShrinkWrap.create(WebArchive.class, BC_DEPLOYMENT + ".war");
        archive.addPackage(BouncyCastleModuleTestCase.class.getPackage());
        archive.addAsWebInfResource(EmptyAsset.INSTANCE, "beans.xml"); //needed to load CDI for arquillian
        archive.addAsManifestResource(createPermissionsXmlAsset(
                new SecurityPermission("insertProvider")
        ), "permissions.xml");
        archive.setManifest(new StringAsset(""
                + "Manifest-Version: 1.0\n"
                + "Dependencies: org.bouncycastle.bcprov, org.bouncycastle.bcpkix, org.bouncycastle.bcmail\n"));
        return archive;
    }

    @Test
    public void testBouncyCastleProviderIsUsableThroughJceApi() throws Exception {

        BouncyCastleProvider bcProvider = null;
        try {
            bcProvider = new BouncyCastleProvider();
            useBouncyCastleProviderThroughJceApi(bcProvider);
        } catch (Exception e) {
            if (e instanceof SecurityException && e.getMessage().contains("JCE cannot authenticate the provider")) {
                String bcLocation = (bcProvider == null)
                        ? ""
                        : "(" + bcProvider.getClass().getResource("/") + ")";
                throw new Exception("Packaging with BouncyCastleProvider" + bcLocation
                        + " is probably not properly signed for JCE usage, see server log for details.", e);
            } else {
                throw e;
            }
        }
    }

    private static void useBouncyCastleProviderThroughJceApi(BouncyCastleProvider bcProvider) throws Exception {

        Security.addProvider(bcProvider);

        KeyGenerator keygenerator = KeyGenerator.getInstance("DES");
        Key myDesKey = keygenerator.generateKey();

        Cipher desCipher;

        // Create the cipher with explicit BC provider
        desCipher = Cipher.getInstance("DES/ECB/PKCS5Padding", bcProvider.getName());

        // Initialize the cipher for encryption
        desCipher.init(Cipher.ENCRYPT_MODE, myDesKey);

        // Sensitive information
        byte[] text = "Nobody can see me".getBytes(StandardCharsets.UTF_8);
        logger.debug("Text [Byte Format]: " + Arrays.toString(text));
        logger.debug("Text: " + new String(text));

        // Encrypt the text
        byte[] textEncrypted = desCipher.doFinal(text);
        logger.debug("Text Encryted [Byte Format]: " + Arrays.toString(textEncrypted));

        // Initialize the same cipher for decryption
        desCipher.init(Cipher.DECRYPT_MODE, myDesKey);

        // Decrypt the text
        byte[] textDecrypted = desCipher.doFinal(textEncrypted);
        logger.debug("Text Decryted: " + new String(textDecrypted,StandardCharsets.UTF_8));
    }
}
