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

package org.jboss.as.security.vault;

import java.io.File;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.PBEParameterSpec;

import org.jboss.as.security.logging.SecurityLogger;
import org.jboss.security.plugins.PBEUtils;
import org.jboss.security.vault.SecurityVault;
import org.jboss.security.vault.SecurityVaultException;
import org.jboss.security.vault.SecurityVaultFactory;
import org.picketbox.plugins.vault.PicketBoxSecurityVault;

/**
 * Non-interactive session for {@link VaultTool}
 *
 * @author Peter Skopek
 *
 */
public final class VaultSession {

    public static final String VAULT_ENC_ALGORITHM = "PBEwithMD5andDES";

    static final Charset CHARSET = StandardCharsets.UTF_8;

    private String keystoreURL;
    private String keystorePassword;
    private String keystoreMaskedPassword;
    private String encryptionDirectory;
    private String salt;
    private int iterationCount;

    private SecurityVault vault;
    private String vaultAlias;

    /**
     * Constructor to create VaultSession.
     *
     * @param keystoreURL
     * @param keystorePassword
     * @param encryptionDirectory
     * @param salt
     * @param iterationCount
     * @throws Exception
     */
    public VaultSession(String keystoreURL, String keystorePassword, String encryptionDirectory, String salt, int iterationCount)
            throws Exception {
        this.keystoreURL = keystoreURL;
        this.keystorePassword = keystorePassword;
        this.encryptionDirectory = encryptionDirectory;
        this.salt = salt;
        this.iterationCount = iterationCount;
        validate();
    }

    /**
     * Validate fields sent to this class's constructor.
     */
    private void validate() throws Exception {
        validateKeystoreURL();
        validateEncryptionDirectory();
        validateSalt();
        validateIterationCount();
        validateKeystorePassword();
    }

    protected void validateKeystoreURL() throws Exception {

        File f = new File(keystoreURL);
        if (!f.exists()) {
            throw SecurityLogger.ROOT_LOGGER.keyStoreDoesnotExistWithExample(keystoreURL, keystoreURL);
        } else if (!f.canWrite() || !f.isFile()) {
            throw SecurityLogger.ROOT_LOGGER.keyStoreNotWritable(keystoreURL);
        }
    }

    protected void validateKeystorePassword() throws Exception {
        if (keystorePassword == null) {
            throw SecurityLogger.ROOT_LOGGER.keyStorePasswordNotSpecified();
        }
    }

    protected void validateEncryptionDirectory() throws Exception {
        if (encryptionDirectory == null) {
            throw new Exception("Encryption directory has to be specified.");
        }
        if (!encryptionDirectory.endsWith("/") || encryptionDirectory.endsWith("\\")) {
            encryptionDirectory = encryptionDirectory + (System.getProperty("file.separator", "/"));
        }
        File d = new File(encryptionDirectory);
        if (!d.exists()) {
            if (!d.mkdirs()) {
                throw SecurityLogger.ROOT_LOGGER.cannotCreateEncryptionDirectory(d.getAbsolutePath());
            }
        }
        if (!d.isDirectory()) {
            throw SecurityLogger.ROOT_LOGGER.encryptionDirectoryDoesNotExist(encryptionDirectory);
        }
    }

    protected void validateIterationCount() throws Exception {
        if (iterationCount < 1 && iterationCount > Integer.MAX_VALUE) {
            throw SecurityLogger.ROOT_LOGGER.iterationCountOutOfRange(String.valueOf(iterationCount));
        }
    }

    protected void validateSalt() throws Exception {
        if (salt == null || salt.length() != 8) {
            throw SecurityLogger.ROOT_LOGGER.saltWrongLength();
        }
    }

    /**
     * Method to compute masked password based on class attributes.
     *
     * @return masked password prefixed with {link @PicketBoxSecurityVault.PASS_MASK_PREFIX}.
     * @throws Exception
     */
    private String computeMaskedPassword() throws Exception {

        // Create the PBE secret key
        SecretKeyFactory factory = SecretKeyFactory.getInstance(VAULT_ENC_ALGORITHM);

        char[] password = "somearbitrarycrazystringthatdoesnotmatter".toCharArray();
        PBEParameterSpec cipherSpec = new PBEParameterSpec(salt.getBytes(), iterationCount);
        PBEKeySpec keySpec = new PBEKeySpec(password);
        SecretKey cipherKey = factory.generateSecret(keySpec);

        String maskedPass = PBEUtils.encode64(keystorePassword.getBytes(), VAULT_ENC_ALGORITHM, cipherKey, cipherSpec);

        return PicketBoxSecurityVault.PASS_MASK_PREFIX + maskedPass;
    }

    /**
     * Initialize the underlying vault.
     *
     * @throws Exception
     */
    private void initSecurityVault() throws Exception {
        try {
            this.vault = SecurityVaultFactory.get();
            this.vault.init(getVaultOptionsMap());
            handshake();
        } catch (SecurityVaultException e) {
            throw SecurityLogger.ROOT_LOGGER.securityVaultException(e);
        }
    }

    /**
     * Start the vault with given alias.
     *
     * @param vaultAlias
     * @throws Exception
     */
    public void startVaultSession(String vaultAlias) throws Exception {
        if (vaultAlias == null) {
            throw SecurityLogger.ROOT_LOGGER.vaultAliasNotSpecified();
        }
        this.keystoreMaskedPassword = (org.jboss.security.Util.isPasswordCommand(keystorePassword))
                ? keystorePassword
                : computeMaskedPassword();
        this.vaultAlias = vaultAlias;
        initSecurityVault();
    }

    private Map<String, Object> getVaultOptionsMap() {
        Map<String, Object> options = new HashMap<String, Object>();
        options.put(PicketBoxSecurityVault.KEYSTORE_URL, keystoreURL);
        options.put(PicketBoxSecurityVault.KEYSTORE_PASSWORD, keystoreMaskedPassword);
        options.put(PicketBoxSecurityVault.KEYSTORE_ALIAS, vaultAlias);
        options.put(PicketBoxSecurityVault.SALT, salt);
        options.put(PicketBoxSecurityVault.ITERATION_COUNT, Integer.toString(iterationCount));
        options.put(PicketBoxSecurityVault.ENC_FILE_DIR, encryptionDirectory);
        return options;
    }

    private void handshake() throws SecurityVaultException {
        Map<String, Object> handshakeOptions = new HashMap<String, Object>();
        handshakeOptions.put(PicketBoxSecurityVault.PUBLIC_CERT, vaultAlias);
        vault.handshake(handshakeOptions);
    }

    /**
     * Add secured attribute to specified vault block. This method can be called only after successful
     * startVaultSession() call.
     *
     * @param vaultBlock
     * @param attributeName
     * @param attributeValue
     * @return secured attribute configuration
     */
    public String addSecuredAttribute(String vaultBlock, String attributeName, char[] attributeValue) throws Exception {
        vault.store(vaultBlock, attributeName, attributeValue, null);
        return securedAttributeConfigurationString(vaultBlock, attributeName);
    }

    /**
     * Add secured attribute to specified vault block. This method can be called only after successful
     * startVaultSession() call.
     * After successful storage the secured attribute information will be displayed at standard output.
     * For silent method @see addSecuredAttribute
     *
     * @param vaultBlock
     * @param attributeName
     * @param attributeValue
     * @throws Exception
     */
    public void addSecuredAttributeWithDisplay(String vaultBlock, String attributeName, char[] attributeValue) throws Exception {
        vault.store(vaultBlock, attributeName, attributeValue, null);
        attributeCreatedDisplay(vaultBlock, attributeName);
    }

    /**
     * Check whether secured attribute is already set for given vault block and attribute name. This method can be called only after
     * successful startVaultSession() call.
     *
     * @param vaultBlock
     * @param attributeName
     * @return true is password already exists for given vault block and attribute name.
     * @throws Exception
     */
    public boolean checkSecuredAttribute(String vaultBlock, String attributeName) throws Exception {
        return vault.exists(vaultBlock, attributeName);
    }

    /**
     * Display info about stored secured attribute.
     *
     * @param vaultBlock
     * @param attributeName
     */
    private void attributeCreatedDisplay(String vaultBlock, String attributeName) {
        System.out.println(SecurityLogger.ROOT_LOGGER.vaultAttributeCreateDisplay(vaultBlock, attributeName, securedAttributeConfigurationString(vaultBlock, attributeName)));
    }

    /**
     * Returns configuration string for secured attribute.
     *
     * @param vaultBlock
     * @param attributeName
     * @return
     */
    private String securedAttributeConfigurationString(String vaultBlock, String attributeName) {
        return "VAULT::" + vaultBlock + "::" + attributeName + "::1";
    }

    /**
     * Display info about vault itself in form of AS7 configuration file.
     */
    public void vaultConfigurationDisplay() {
        System.out.println(SecurityLogger.ROOT_LOGGER.vaultConfigurationTitle());
        System.out.println("********************************************");
        System.out.println("...");
        System.out.println("</extensions>");
        System.out.print(vaultConfiguration());
        System.out.println("<management> ...");
        System.out.println("********************************************");
    }

    /**
     * Returns vault configuration string in user readable form.
     * @return
     */
    public String vaultConfiguration() {
        StringBuilder sb = new StringBuilder();
        sb.append("<vault>").append("\n");
        sb.append("  <vault-option name=\"KEYSTORE_URL\" value=\"" + keystoreURL + "\"/>").append("\n");
        sb.append("  <vault-option name=\"KEYSTORE_PASSWORD\" value=\"" + keystoreMaskedPassword + "\"/>").append("\n");
        sb.append("  <vault-option name=\"KEYSTORE_ALIAS\" value=\"" + vaultAlias + "\"/>").append("\n");
        sb.append("  <vault-option name=\"SALT\" value=\"" + salt + "\"/>").append("\n");
        sb.append("  <vault-option name=\"ITERATION_COUNT\" value=\"" + iterationCount + "\"/>").append("\n");
        sb.append("  <vault-option name=\"ENC_FILE_DIR\" value=\"" + encryptionDirectory + "\"/>").append("\n");
        sb.append("</vault>");
        return sb.toString();
    }

    /**
     * Method to get keystore masked password to use further in configuration.
     * Has to be used after {@link startVaultSession} method.
     *
     * @return the keystoreMaskedPassword
     */
    public String getKeystoreMaskedPassword() {
        return keystoreMaskedPassword;
    }
}
