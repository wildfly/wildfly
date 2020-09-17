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
            System.err.println(VaultLogger.ROOT_LOGGER.noConsole());
            System.exit(1);
        }

        while (encDir == null || encDir.length() == 0) {
            encDir = console
                    .readLine(VaultLogger.ROOT_LOGGER.enterEncryptionDirectory() + " ");
        }

        while (keystoreURL == null || keystoreURL.length() == 0) {
            keystoreURL = console.readLine(VaultLogger.ROOT_LOGGER.enterKeyStoreURL() + " ");
        }

        char[] keystorePasswd = getSensitiveValue(VaultLogger.ROOT_LOGGER.enterKeyStorePassword(), VaultLogger.ROOT_LOGGER.enterKeyStorePasswordAgain());

        try {
            while (salt == null || salt.length() != 8) {
                salt = console.readLine(VaultLogger.ROOT_LOGGER.enterSalt() + " ");
            }

            String ic = console.readLine(VaultLogger.ROOT_LOGGER.enterIterationCount() + " ");
            iterationCount = Integer.parseInt(ic);
            vaultNISession = new VaultSession(keystoreURL, new String(keystorePasswd), encDir, salt, iterationCount, true);

            while (keystoreAlias == null || keystoreAlias.length() == 0) {
                keystoreAlias = console.readLine(VaultLogger.ROOT_LOGGER.enterKeyStoreAlias() + " ");
            }

            System.out.println(VaultLogger.ROOT_LOGGER.initializingVault());
            vaultNISession.startVaultSession(keystoreAlias);
            vaultNISession.vaultConfigurationDisplay();

            System.out.println(VaultLogger.ROOT_LOGGER.vaultInitialized());
            System.out.println(VaultLogger.ROOT_LOGGER.handshakeComplete());

            VaultInteraction vaultInteraction = new VaultInteraction(vaultNISession);
            vaultInteraction.start();
        } catch (Exception e) {
            System.out.println(VaultLogger.ROOT_LOGGER.exceptionEncountered());
            e.printStackTrace(System.err);
        }
    }

    public static char[] getSensitiveValue(String passwordPrompt, String confirmationPrompt) {
        while (true) {
            if (passwordPrompt == null)
                passwordPrompt = VaultLogger.ROOT_LOGGER.enterYourPassword();
            if (confirmationPrompt == null) {
                confirmationPrompt = VaultLogger.ROOT_LOGGER.enterYourPasswordAgain();
            }

            Console console = System.console();

            char[] passwd = console.readPassword(passwordPrompt + " ");
            char[] passwd1 = console.readPassword(confirmationPrompt + " ");
            boolean noMatch = !Arrays.equals(passwd, passwd1);
            if (noMatch)
                System.out.println(VaultLogger.ROOT_LOGGER.passwordsDoNotMatch());
            else {
                System.out.println(VaultLogger.ROOT_LOGGER.passwordsMatch());
                return passwd;
            }
        }
    }

}