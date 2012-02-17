/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2011, Red Hat, Inc., and individual contributors
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

import java.io.Console;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.PBEParameterSpec;

import org.jboss.security.plugins.PBEUtils;
import org.jboss.security.vault.SecurityVault;
import org.jboss.security.vault.SecurityVaultException;
import org.jboss.security.vault.SecurityVaultFactory;
import org.picketbox.plugins.vault.PicketBoxSecurityVault;

/**
 * An interactive session for {@link VaultTool}
 *
 * @author Anil Saldhana
 */
public class VaultInteractiveSession {

    private String maskedPassword, salt, keystoreURL, encDir, keystoreAlias;
    private int iterationCount = 0;

    private byte[] handshakeKey;

    private SecurityVault vault = null;

    public VaultInteractiveSession() {
    }

    public void start() {
        Console console = System.console();

        if (console == null) {
            System.err.println("No console.");
            System.exit(1);
        }

        while (encDir == null || encDir.length() == 0 || !(encDir.endsWith("/") || encDir.endsWith("\\"))) {
            encDir = console
                    .readLine("Enter directory to store encrypted files (end with either / or \\ based on Unix or Windows:");
        }

        while (keystoreURL == null || keystoreURL.length() == 0) {
            keystoreURL = console.readLine("Enter Keystore URL:");
        }

        char[] keystorePasswd = getSensitiveValue("Enter Keystore password");

        try {
            maskedPassword = getMaskedPassword(console, new String(keystorePasswd));
            System.out.println("                ");
            System.out.println("Please make note of the following:");
            System.out.println("********************************************");
            System.out.println("Masked Password:" + maskedPassword);
            System.out.println("salt:" + salt);
            System.out.println("Iteration Count:" + iterationCount);
            System.out.println("********************************************");
            System.out.println("                ");
        } catch (Exception e) {
            System.out.println("Exception encountered:" + e.getLocalizedMessage());
        }

        while (keystoreAlias == null || keystoreAlias.length() == 0) {
            keystoreAlias = console.readLine("Enter Keystore Alias:");
        }

        try {
            vault = SecurityVaultFactory.get();
            System.out.println("Obtained Vault");
            Map<String, Object> options = new HashMap<String, Object>();
            options.putAll(getMap());
            System.out.println("Initializing Vault");
            vault.init(options);
            System.out.println("Vault is initialized and ready for use");
            handshake();
            System.out.println("Handshake with Vault complete");
            VaultInteraction vaultInteraction = new VaultInteraction(vault, handshakeKey);
            vaultInteraction.start();
        } catch (SecurityVaultException e) {
            System.out.println("Exception encountered:" + e.getLocalizedMessage());
        }
    }

    public static char[] getSensitiveValue(String passwordPrompt) {
        while (true) {
            if (passwordPrompt == null)
                passwordPrompt = "Enter your password";

            Console console = System.console();

            char[] passwd = console.readPassword(passwordPrompt + ": ");
            char[] passwd1 = console.readPassword(passwordPrompt + " again: ");
            boolean noMatch = !Arrays.equals(passwd, passwd1);
            if (noMatch)
                System.out.println("Values entered don't match");
            else {
                System.out.println("Values match");
                return passwd;
            }
        }
    }

    private String getMaskedPassword(Console console, String passwd) throws Exception {
        while (salt == null || salt.length() != 8) {
            salt = console.readLine("Enter 8 character salt:");
        }

        String ic = console.readLine("Enter iteration count as a number (Eg: 44):");
        iterationCount = Integer.parseInt(ic);

        String algo = "PBEwithMD5andDES";

        // Create the PBE secret key
        SecretKeyFactory factory = SecretKeyFactory.getInstance("PBEwithMD5andDES");

        char[] password = "somearbitrarycrazystringthatdoesnotmatter".toCharArray();
        PBEParameterSpec cipherSpec = new PBEParameterSpec(salt.getBytes(), iterationCount);
        PBEKeySpec keySpec = new PBEKeySpec(password);
        SecretKey cipherKey = factory.generateSecret(keySpec);

        String maskedPass = PBEUtils.encode64(passwd.getBytes(), algo, cipherKey, cipherSpec);

        return new String(PicketBoxSecurityVault.PASS_MASK_PREFIX) + maskedPass;
    }

    private Map<String, Object> getMap() {
        Map<String, Object> options = new HashMap<String, Object>();
        options.put(PicketBoxSecurityVault.KEYSTORE_URL, keystoreURL);
        options.put(PicketBoxSecurityVault.KEYSTORE_PASSWORD, maskedPassword);
        options.put(PicketBoxSecurityVault.KEYSTORE_ALIAS, keystoreAlias);
        options.put(PicketBoxSecurityVault.SALT, salt);
        options.put(PicketBoxSecurityVault.ITERATION_COUNT, "" + iterationCount);

        options.put(PicketBoxSecurityVault.ENC_FILE_DIR, encDir);

        return options;
    }

    private void handshake() throws SecurityVaultException {
        Map<String, Object> handshakeOptions = new HashMap<String, Object>();
        handshakeOptions.put(PicketBoxSecurityVault.PUBLIC_CERT, keystoreAlias);
        handshakeKey = vault.handshake(handshakeOptions);
    }
}