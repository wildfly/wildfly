package org.jboss.as.security.vault;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.io.File;

public class VaultSessionTestCase extends VaultTest {

    @Before
    public void setUp() throws Exception {
        cleanDirectory(ENC_FILE_DIR_VALUE);
        System.setSecurityManager(new VaultTest.NoExitSecurityManager());
    }

    @After
    public void tearDown() throws Exception {
        System.setSecurityManager(null); // or save and restore original
        cleanDirectory(ENC_FILE_DIR_VALUE);
        File keyStoreFile = new File(KEYSTORE_URL_VALUE);
        if (keyStoreFile.exists()) {
            keyStoreFile.delete();
        }
    }

    @Test
    public void testVaultConfiguration() {
        final String expectedCommand = "/core-service=vault:add(vault-options=[(\"KEYSTORE_URL\" => \"" + KEYSTORE_URL_VALUE + "\")," +
                "(\"KEYSTORE_PASSWORD\" => \"" + MASKED_MYPASSWORD_VALUE + "\"),(\"KEYSTORE_ALIAS\" => \"" + KEYSTORE_ALIAS_VALUE + "\")," +
                "(\"SALT\" => \"" + SALT_VALUE + "\"),(\"ITERATION_COUNT\" => \"" + ITERATION_COUNT_VALUE + "\")," +
                "(\"ENC_FILE_DIR\" => \"" + ENC_FILE_DIR_VALUE + "/\")])";

        VaultSession vaultSession = null;
        try {
            vaultSession = new VaultSession(KEYSTORE_URL_VALUE, KEYSTORE_PASSWORD, ENC_FILE_DIR_VALUE, SALT_VALUE, Integer.valueOf(ITERATION_COUNT_VALUE), true);
            vaultSession.startVaultSession(KEYSTORE_ALIAS_VALUE);
        } catch (Exception e) {
            Assert.fail("Failed while initializing vault session with exception " + e.getMessage());
        }

        Assert.assertEquals(expectedCommand, vaultSession.vaultConfiguration());
    }
}
