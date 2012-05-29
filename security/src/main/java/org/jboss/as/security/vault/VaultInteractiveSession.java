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

/**
 * An interactive session for {@link VaultTool}
 *
 * @author Anil Saldhana
 */
public class VaultInteractiveSession {

    private String salt, keystoreURL, encDir, keystoreAlias;
    private int iterationCount = 0;

    // vault non-interactive session
    private VaultSession vaultNISession = null;

    public VaultInteractiveSession() {
    }

    public void start() {
        Console console = System.console();

        if (console == null) {
            System.err.println("No console.");
            System.exit(1);
        }

        while (encDir == null || encDir.length() == 0) {
            encDir = console
                    .readLine("Enter directory to store encrypted files:");
        }

        while (keystoreURL == null || keystoreURL.length() == 0) {
            keystoreURL = console.readLine("Enter Keystore URL:");
        }

        char[] keystorePasswd = getSensitiveValue("Enter Keystore password");

        try {
            while (salt == null || salt.length() != 8) {
                salt = console.readLine("Enter 8 character salt:");
            }

            String ic = console.readLine("Enter iteration count as a number (Eg: 44):");
            iterationCount = Integer.parseInt(ic);
            vaultNISession = new VaultSession(keystoreURL, new String(keystorePasswd), encDir, salt, iterationCount);

            while (keystoreAlias == null || keystoreAlias.length() == 0) {
                keystoreAlias = console.readLine("Enter Keystore Alias:");
            }

            System.out.println("Initializing Vault");
            vaultNISession.startVaultSession(keystoreAlias);
            vaultNISession.vaultConfigurationDisplay();

            System.out.println("Vault is initialized and ready for use");
            System.out.println("Handshake with Vault complete");

            VaultInteraction vaultInteraction = new VaultInteraction(vaultNISession);
            vaultInteraction.start();
        } catch (Exception e) {
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

}