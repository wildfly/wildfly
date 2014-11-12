/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2013, Red Hat, Inc., and individual contributors
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

package org.jboss.as.test.integration.security.common;

import java.io.File;
import java.io.FileOutputStream;
import java.security.KeyStore;
import java.util.Random;

import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;

import org.jboss.as.security.vault.VaultSession;
import org.jboss.logging.Logger;
import org.picketbox.plugins.vault.PicketBoxSecurityVault;
import org.picketbox.util.KeyStoreUtil;

/**
 * VaultHandler is a handler for PicketBox Security Vault associated files. It can be used one-to-one with vault. It can create
 * required keystore and after action delete all vault data files. It should be used for testing purpose only.
 *
 * @author Peter Skopek (pskopek at redhat dot com)
 *
 */
public class VaultHandler {

    private static Logger LOGGER = Logger.getLogger(VaultHandler.class);

    public static final String ENC_DAT_FILE = "ENC.dat";
    public static final String SHARED_DAT_FILE = "Shared.dat";
    public static final String VAULT_DAT_FILE = "VAULT.dat";
    public static final String DEFAULT_KEYSTORE_FILE = "vault.keystore";

    private String encodedVaultFileDirectory;
    private String keyStoreType;
    private String keyStore;
    private String keyStorePassword;
    private int keySize = 128;
    private String alias = "defaultalias";
    private String salt;
    private int iterationCount;

    private VaultSession vaultSession;

    private static String FILE_SEPARATOR = System.getProperty("file.separator");
    private static String TMP_DIR = System.getProperty("java.io.tmpdir");
    private static String DEFAULT_PASSWORD = "super_secret";

    /**
     * Create vault with all required files. It is the most complete constructor.
     * If keyStore doesn't exist it will be created with specified keyStoreType and
     * encryption directory will be created if not existent.
     *
     * @param keyStore
     * @param keyStorePassword
     * @param keyStoreType - JCEKS, JKS or null
     * @param encodedVaultFileDirectory
     * @param keySize
     * @param alias
     * @param salt
     * @param iterationCount
     */
    public VaultHandler(String keyStore, String keyStorePassword, String keyStoreType, String encodedVaultFileDirectory,
            int keySize, String alias, String salt, int iterationCount) {

        if (alias != null) {
            this.alias = alias;
        }

        if (keySize != 0) {
            this.keySize = keySize;
        }

        if (keyStoreType == null) {
            this.keyStoreType = "JCEKS";
        } else {
            if (!keyStoreType.equals("JCEKS") && !keyStoreType.equals("JKS")) {
                throw new IllegalArgumentException("Wrong keyStoreType. Supported are only (JCEKS or JKS). Preferred is JCEKS.");
            }
            this.keyStoreType = keyStoreType;
        }

        if (keyStorePassword == null) {
            this.keyStorePassword = DEFAULT_PASSWORD;
        } else if (keyStorePassword.startsWith(PicketBoxSecurityVault.PASS_MASK_PREFIX)) {
            throw new IllegalArgumentException("keyStorePassword cannot be a masked password, use plain text password, please");
        } else {
            this.keyStorePassword = keyStorePassword;
        }

        try {
            File keyStoreFile = new File(keyStore);
            if (!keyStoreFile.exists()) {
                if (!this.keyStoreType.equals("JCEKS")) {
                    throw new RuntimeException("keyStoreType has to be JCEKS when creating new key store");
                }
                File keyStoreParent = keyStoreFile.getAbsoluteFile().getParentFile();
                if (keyStoreParent != null) {
                    if (!keyStoreParent.exists()) {
                        assert keyStoreParent.mkdirs();
                    } else {
                        assert keyStoreParent.isDirectory();
                    }
                }
                KeyStore ks = KeyStoreUtil.createKeyStore(this.keyStoreType, this.keyStorePassword.toCharArray());
                KeyGenerator keyGenerator = KeyGenerator.getInstance("AES");
                keyGenerator.init(this.keySize);
                SecretKey secretKey = keyGenerator.generateKey();
                KeyStore.SecretKeyEntry skEntry = new KeyStore.SecretKeyEntry(secretKey);
                KeyStore.PasswordProtection p = new KeyStore.PasswordProtection(this.keyStorePassword.toCharArray());
                ks.setEntry(this.alias, skEntry, p);
                ks.store(new FileOutputStream(keyStoreFile), this.keyStorePassword.toCharArray());
            }
            this.keyStore = keyStoreFile.getAbsolutePath();
        } catch (Exception e) {
            throw new RuntimeException("Problem creating keyStore: ", e);
        }

        File vaultDirectory = new File(encodedVaultFileDirectory).getAbsoluteFile();

        if (!vaultDirectory.exists()) {
            assert vaultDirectory.mkdirs();
        } else if (!vaultDirectory.isDirectory()) {
            throw new RuntimeException("Vault encryption directory has to be directory, but "
                    + vaultDirectory.getAbsolutePath() + " is not.");
        }

        this.encodedVaultFileDirectory = vaultDirectory.getAbsolutePath();

        if (salt == null) {
            String tmp = Long.toHexString(System.currentTimeMillis())
                    + Long.toHexString(System.currentTimeMillis())
                    + Long.toHexString(System.currentTimeMillis())
                    + Long.toHexString(System.currentTimeMillis());
            this.salt = tmp.substring(0, 8);
        } else {
            this.salt = salt;
        }

        if (iterationCount <= 0) {
            this.iterationCount = new Random().nextInt(90) + 1;
        } else {
            this.iterationCount = iterationCount;
        }

        if (LOGGER.isDebugEnabled()) {
            logCreatedVault();
        }

        try {
            /* removed as temporary workaround until PB 4.9.0.Beta2 is pulled to WF-CORE
            this.vaultSession = new VaultSession(this.keyStore, this.keyStorePassword, this.encodedVaultFileDirectory,
                    this.salt, this.iterationCount, true);
            */
            this.vaultSession = new VaultSession(this.keyStore, this.keyStorePassword, this.encodedVaultFileDirectory,
                    this.salt, this.iterationCount);
            this.vaultSession.startVaultSession(this.alias);
        } catch (Exception e) {
            throw new RuntimeException("Problem creating VaultSession: ", e);
        }
        LOGGER.debug("VaultSession started");
    }

    /**
     * Constructor with all default values, but keyStore and encodedVaultFileDirectory.
     *
     * @param keyStore
     * @param encodedVaultFileDirectory
     */
    public VaultHandler(String keyStore, String encodedVaultFileDirectory) {
        this(keyStore, null, null, encodedVaultFileDirectory, 0, null, null, 0);
    }

    /**
     * Constructor with all default values, but encodedVaultFileDirectory.
     * @param encodedVaultFileDirectory
     */
    public VaultHandler(String encodedVaultFileDirectory) {
        this(getDefaultKeystoreFile(encodedVaultFileDirectory), encodedVaultFileDirectory);
    }

    /**
     * Constructor with all default values.
     */
    public VaultHandler() {
        this(TMP_DIR);
    }

    public String getMaskedKeyStorePassword() {
        if (vaultSession != null) {
            return vaultSession.getKeystoreMaskedPassword();
        } else {
            throw new RuntimeException("getMaskedKeyStorePassword: Vault inside this handler is not initialized or created");
        }

    }

    public String addSecuredAttribute(String vaultBlock, String attributeName, char[] attributeValue) {
        if (vaultSession != null) {
            try {
                return vaultSession.addSecuredAttribute(vaultBlock, attributeName, attributeValue);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        } else {
            throw new RuntimeException("addSecuredAttribute: Vault inside this handler is not initialized or created");
        }
    }

    public boolean exists(String vaultBlock, String attributeName) {
        if (vaultSession != null) {
            try {
                return vaultSession.checkSecuredAttribute(vaultBlock, attributeName);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        } else {
            throw new RuntimeException("exists: Vault inside this handler is not initialized or created");
        }
    }

    /**
     * Return VaultSession for further vault manipulation when needed.
     * @return
     */
    public VaultSession getVaultSession() {
        return vaultSession;
    }

    /**
     * Delete all associated vault files and keystore. After this action VaultHandler is not usable anymore.
     */
    public void cleanUp() {

        cleanFilesystem(encodedVaultFileDirectory, false, keyStore);

        vaultSession = null;
    }

    public String getEncodedVaultFileDirectory() {
        return encodedVaultFileDirectory;
    }

    public String getKeyStoreType() {
        return keyStoreType;
    }

    public String getKeyStore() {
        return keyStore;
    }

    public int getKeySize() {
        return keySize;
    }

    public String getAlias() {
        return alias;
    }

    public String getSalt() {
        return salt;
    }

    public String getKeyStorePassword() {
        return keyStorePassword;
    }

    public int getIterationCount() {
        return iterationCount;
    }

    public String getIterationCountAsString() {
        return Integer.toString(iterationCount);
    }

    private void logCreatedVault() {
        LOGGER.debug("keystoreURL="+keyStore);
        LOGGER.debug("KEYSTORE_PASSWORD="+keyStorePassword);
        LOGGER.debug("ENC_FILE_DIR="+encodedVaultFileDirectory);
        LOGGER.debug("KEYSTORE_ALIAS="+alias);
        LOGGER.debug("SALT="+salt);
        LOGGER.debug("ITERATION_COUNT="+iterationCount);
    }

    private static String getDefaultKeystoreFile(String encodedVaultFileDirectory) {
        return encodedVaultFileDirectory + FILE_SEPARATOR + DEFAULT_KEYSTORE_FILE;
    }

    /**
     * Removes vault files from the given directory, with the assumption that the key store is located inside
     * the directory in a file named {@link #DEFAULT_KEYSTORE_FILE}.
     *
     * @param encodedVaultFileDirectory the path of the directory
     * @param removeEncodedVaultFileDirectory {@code true} if the directory itself should be removed
     */
    public static void cleanFilesystem(String encodedVaultFileDirectory, boolean removeEncodedVaultFileDirectory) {
        cleanFilesystem(encodedVaultFileDirectory, removeEncodedVaultFileDirectory, getDefaultKeystoreFile(encodedVaultFileDirectory));
    }

    /**
     * Removes vault files from the given directory, and also removes the given key store file.
     *
     * @param encodedVaultFileDirectory  the path of the directory
     * @param removeEncodedVaultFileDirectory {@code true} if the directory itself should be removed
     * @param keyStore the path of the key store
     */
    public static void cleanFilesystem(String encodedVaultFileDirectory, boolean removeEncodedVaultFileDirectory, String keyStore) {

        cleanFilesystem(new File(encodedVaultFileDirectory), removeEncodedVaultFileDirectory, new File(keyStore));
    }

    private static void cleanFilesystem(File encodedVaultFileDirectory, boolean removeEncodedVaultFileDirectory,
                                        File keyStore) {

        deleteIfExists(keyStore);

        File f = new File(keyStore.getParent(), keyStore.getName() + ".original");
        deleteIfExists(f);

        if (removeEncodedVaultFileDirectory) {
            deleteDirectory(encodedVaultFileDirectory);
        } else {
            cleanEncodedVaultFileDirectory(encodedVaultFileDirectory);
        }

    }

    private static void deleteDirectory(File f) {
        File[] children = f.isDirectory() ? f.listFiles() : null;
        if (children != null) {
            for (File child : children) {
                deleteDirectory(child);
            }
        }
        deleteIfExists(f);
    }

    private static void cleanEncodedVaultFileDirectory(File encodedVaultFileDirectory) {

        File f = new File(encodedVaultFileDirectory, VAULT_DAT_FILE);
        deleteIfExists(f);

        f = new File(encodedVaultFileDirectory, ENC_DAT_FILE);
        deleteIfExists(f);

        f = new File(encodedVaultFileDirectory, ENC_DAT_FILE + ".original");
        deleteIfExists(f);

        f = new File(encodedVaultFileDirectory, SHARED_DAT_FILE);
        deleteIfExists(f);

        // there might be a KEYSTORE_README file in the directory as a placeholder
        f = new File(encodedVaultFileDirectory, "KEYSTORE_README");
        deleteIfExists(f);

    }

    private static void deleteIfExists(File f) {
        assert !f.exists() || f.delete();
    }

}
