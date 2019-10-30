package org.jboss.as.security.vault;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.net.URLDecoder;
import java.security.Permission;

public abstract class VaultTest {
    private static final String CODE_LOCATION = VaultTest.class.getProtectionDomain().getCodeSource().getLocation().getFile();
    static final String ENC_FILE_DIR_VALUE = CODE_LOCATION + "test_vault_dir";
    static final String KEYSTORE_URL_VALUE = getKeystorePath();
    static final String KEYSTORE_PASSWORD = "mypassword";
    static final String BLOCK_NAME = "myblock";
    static final String ATTRIBUTE_NAME = "the_attribute_I_want_to_store";
    static final String VALUE_TO_STORE = "the_value";
    static final String KEYSTORE_ALIAS_VALUE = "vault";
    static final String ITERATION_COUNT_VALUE = "12";
    static final String MASKED_MYPASSWORD_VALUE = "MASK-0UWB5tlhOmKYzJVl9KZaPN";
    static final String MASKED_MYPASSWORD_VALUE_INCORRECT = "MASK-UWB5tlhOmKYzJVl9KZaPN";
    static final String SALT_VALUE = "bdfbdf12";
    static final ByteArrayOutputStream SYSTEM_OUT = new ByteArrayOutputStream();

    static String getKeystorePath() {
        try {
            return new String(URLDecoder.decode(CODE_LOCATION, "UTF-8") + "org/jboss/as/security/vault.keystore");
        } catch (Exception e) {
            throw new Error("Unable to decode url", e);
        }
    }

    protected static class ExitException extends SecurityException {

        public final int status;

        public ExitException(int status) {
            super("There is no escape!");
            this.status = status;
        }
    }

    static class NoExitSecurityManager extends SecurityManager {

        @Override
        public void checkPermission(Permission perm) {
            // allow anything.
        }

        @Override
        public void checkPermission(Permission perm, Object context) {
            // allow anything.
        }

        @Override
        public void checkExit(int status) {
            super.checkExit(status);
            throw new VaultTest.ExitException(status);
        }
    }

    /**
     * Clean given directory.
     *
     * @param dir
     */
    static void cleanDirectory(String dir) {
        File directory = new File(dir);
        if (directory.exists()) {
            for (File f : directory.listFiles()) {
                f.delete();
            }
        }
    }
}
