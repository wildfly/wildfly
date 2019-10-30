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

import org.jboss.as.security.logging.SecurityLogger;

/**
 * Interaction with initialized {@link org.jboss.security.vault.SecurityVault} via the {@link VaultTool}
 *
 * @author Anil Saldhana
 */
public class VaultInteraction {

    private VaultSession vaultNISession;

    public VaultInteraction(VaultSession vaultSession) {
        this.vaultNISession = vaultSession;
    }

    public void start() {
        Console console = System.console();

        if (console == null) {
            System.err.println(SecurityLogger.ROOT_LOGGER.noConsole());
            System.exit(1);
        }

        Scanner in = new Scanner(System.in);
        while (true) {
            String commandStr = SecurityLogger.ROOT_LOGGER.interactionCommandOptions();

            System.out.println(commandStr);
            int choice = in.nextInt();
            switch (choice) {
                case 0:
                    System.out.println(SecurityLogger.ROOT_LOGGER.taskStoreSecuredAttribute());
                    char[] attributeValue = VaultInteractiveSession.getSensitiveValue(SecurityLogger.ROOT_LOGGER.interactivePromptSecureAttributeValue(), SecurityLogger.ROOT_LOGGER.interactivePromptSecureAttributeValueAgain());
                    String vaultBlock = null;

                    while (vaultBlock == null || vaultBlock.length() == 0) {
                        vaultBlock = console.readLine(SecurityLogger.ROOT_LOGGER.interactivePromptVaultBlock());
                    }

                    String attributeName = null;

                    while (attributeName == null || attributeName.length() == 0) {
                        attributeName = console.readLine(SecurityLogger.ROOT_LOGGER.interactivePromptAttributeName());
                    }
                    try {
                        vaultNISession.addSecuredAttributeWithDisplay(vaultBlock, attributeName, attributeValue);
                    } catch (Exception e) {
                        System.out.println(SecurityLogger.ROOT_LOGGER.problemOcurred() + "\n" + e.getLocalizedMessage());
                    }
                    break;
                case 1:
                    System.out.println(SecurityLogger.ROOT_LOGGER.taskVerifySecuredAttributeExists());
                    try {
                        vaultBlock = null;

                        while (vaultBlock == null || vaultBlock.length() == 0) {
                            vaultBlock = console.readLine(SecurityLogger.ROOT_LOGGER.interactivePromptVaultBlock());
                        }

                        attributeName = null;

                        while (attributeName == null || attributeName.length() == 0) {
                            attributeName = console.readLine(SecurityLogger.ROOT_LOGGER.interactivePromptAttributeName());
                        }
                        if (!vaultNISession.checkSecuredAttribute(vaultBlock, attributeName)) {
                            System.out.println(SecurityLogger.ROOT_LOGGER.interactiveMessageNoValueStored(VaultSession
                                    .blockAttributeDisplayFormat(vaultBlock, attributeName)));
                        } else {
                            System.out.println(SecurityLogger.ROOT_LOGGER.interactiveMessageValueStored(VaultSession
                                    .blockAttributeDisplayFormat(vaultBlock, attributeName)));
                        }
                    } catch (Exception e) {
                        System.out.println(SecurityLogger.ROOT_LOGGER.problemOcurred() + "\n" + e.getLocalizedMessage());
                    }
                    break;
                case 2:
                    System.out.println(SecurityLogger.ROOT_LOGGER.taskRemoveSecuredAttribute());
                    try {
                        vaultBlock = null;

                        while (vaultBlock == null || vaultBlock.length() == 0) {
                            vaultBlock = console.readLine(SecurityLogger.ROOT_LOGGER.interactivePromptVaultBlock());
                        }

                        attributeName = null;

                        while (attributeName == null || attributeName.length() == 0) {
                            attributeName = console.readLine(SecurityLogger.ROOT_LOGGER.interactivePromptAttributeName());
                        }
                        if (!vaultNISession.removeSecuredAttribute(vaultBlock, attributeName)) {
                            System.out.println(SecurityLogger.ROOT_LOGGER.messageAttributeNotRemoved(VaultSession
                                    .blockAttributeDisplayFormat(vaultBlock, attributeName)));
                        } else {
                            System.out.println(SecurityLogger.ROOT_LOGGER
                                    .messageAttributeRemovedSuccessfuly(VaultSession.blockAttributeDisplayFormat(
                                            vaultBlock, attributeName)));
                        }
                    } catch (Exception e) {
                        System.out.println(SecurityLogger.ROOT_LOGGER.problemOcurred() + "\n" + e.getLocalizedMessage());
                    }
                    break;
                default:
                    in.close();
                    System.exit(0);
            }
        }
    }
}