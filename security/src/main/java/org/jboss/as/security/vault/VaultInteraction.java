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
import java.util.Scanner;

import org.jboss.security.vault.SecurityVault;
import org.jboss.security.vault.SecurityVaultException;

/**
 * Interaction with initialized {@link SecurityVault} via the {@link VaultTool}
 *
 * @author Anil Saldhana
 */
public class VaultInteraction {

    private SecurityVault vault;
    private byte[] handshakeKey;

    public VaultInteraction(SecurityVault vault, byte[] handshakeKey) {
        this.vault = vault;
        this.handshakeKey = handshakeKey;
    }

    public void start() {
        Console console = System.console();

        if (console == null) {
            System.err.println("No console.");
            System.exit(1);
        }

        Scanner in = new Scanner(System.in);
        while (true) {
            String commandStr = "Please enter a Digit::   0: Store a password " + " 1: Check whether password exists "
                    + " 2: Exit";

            System.out.println(commandStr);
            int choice = in.nextInt();
            switch (choice) {
                case 0:
                    System.out.println("Task:  Store a password");
                    char[] attributeValue = VaultInteractiveSession.getSensitiveValue("Please enter attribute value");
                    String vaultBlock = null;

                    while (vaultBlock == null || vaultBlock.length() == 0) {
                        vaultBlock = console.readLine("Enter Vault Block:");
                    }

                    String attributeName = null;

                    while (attributeName == null || attributeName.length() == 0) {
                        attributeName = console.readLine("Enter Attribute Name:");
                    }
                    try {
                        vault.store(vaultBlock, attributeName, attributeValue, handshakeKey);

                        String keyAsString = new String(handshakeKey);
                        System.out.println("Attribute Value for (" + vaultBlock + ", " + attributeName + ") saved");

                        System.out.println("                ");
                        System.out.println("Please make note of the following:");
                        System.out.println("********************************************");
                        System.out.println("Vault Block:" + vaultBlock);
                        System.out.println("Attribute Name:" + attributeName);
                        System.out.println("Shared Key:" + keyAsString);
                        System.out.println("Configuration should be done as follows:");
                        System.out.println("VAULT::" + vaultBlock + "::" + attributeName + "::" + keyAsString);
                        System.out.println("********************************************");
                        System.out.println("                ");
                    } catch (SecurityVaultException e) {
                        System.out.println("Exception occurred:" + e.getLocalizedMessage());
                    }
                    break;
                case 1:
                    System.out.println("Task: Verify whether a password exists");
                    try {
                        vaultBlock = null;

                        while (vaultBlock == null || vaultBlock.length() == 0) {
                            vaultBlock = console.readLine("Enter Vault Block:");
                        }

                        attributeName = null;

                        while (attributeName == null || attributeName.length() == 0) {
                            attributeName = console.readLine("Enter Attribute Name:");
                        }
                        if (vault.exists(vaultBlock, attributeName) == false)
                            System.out.println("No value has been store for (" + vaultBlock + ", " + attributeName + ")");
                        else
                            System.out.println("A value exists for (" + vaultBlock + ", " + attributeName + ")");
                    } catch (SecurityVaultException e) {
                        System.out.println("Exception occurred:" + e.getLocalizedMessage());
                    }
                    break;
                default:
                    System.exit(0);
            }
        }
    }
}