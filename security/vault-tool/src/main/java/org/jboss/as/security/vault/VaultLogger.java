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

import static org.jboss.logging.annotations.Message.NONE;

import org.jboss.as.server.services.security.VaultReaderException;
import org.jboss.logging.BasicLogger;
import org.jboss.logging.Logger;
import org.jboss.logging.annotations.Cause;
import org.jboss.logging.annotations.Message;
import org.jboss.logging.annotations.MessageLogger;
import org.jboss.security.vault.SecurityVaultException;

/**
 * Date: 05.11.2011
 *
 * @author <a href="mailto:jperkins@redhat.com">James R. Perkins</a>
 */
@MessageLogger(projectCode = "WFLYSEC", length = 4)
interface VaultLogger extends BasicLogger {

    /**
     * A logger with a category of the package name.
     */
    VaultLogger ROOT_LOGGER = Logger.getMessageLogger(VaultLogger.class, "org.jboss.as.security");

    /**
     * Creates a {@link RuntimeException}
     * @param e the underlying exception
     * @return the exception
     */
    @Message(id = 7, value = "Runtime Exception:")
    RuntimeException runtimeException(@Cause Throwable e);

    /**
     * Create a {@link SecurityException}
     * @param t underlying exception
     * @return {@link SecurityException}
     */
    @Message(id = 15, value = "Security Exception")
    SecurityException securityException(@Cause Throwable t);

    /**
     * Create a {@link org.jboss.as.server.services.security.VaultReaderException} to indicate there was an exception while reading from the vault
     * @param t underlying exception
     * @return {@link org.jboss.as.server.services.security.VaultReaderException}
     */
    @Message(id = 17, value = "Vault Reader Exception:")
    VaultReaderException vaultReaderException(@Cause Throwable t);

    /**
     * Create a {@link SecurityException} to indicate that the vault is not initialized
     * @return {@link SecurityException}
     */
    @Message(id = 26, value = "Vault is not initialized")
    SecurityException vaultNotInitializedException();

    /**
     * Create an Exception when KeyStore cannot be located with example how to create one.
     *
     * @param keystoreURL nonexistent keystore URL
     * @param keystoreURLExample  example keystore url
     * @return the exception
     */
    @Message(id = 37, value = "Keystore '%s' doesn't exist."
            + "\nkeystore could be created: "
            + "keytool -genseckey -alias Vault -storetype jceks -keyalg AES -keysize 128 -storepass secretsecret -keypass secretsecret -keystore %s")
    Exception keyStoreDoesnotExistWithExample(final String keystoreURL, final String keystoreURLExample);

    /**
     * Create an Exception when one cannot write to the KeyStore or it is not a file.
     *
     * @param keystoreURL URL of the keystore
     * @return the exception
     */
    @Message(id = 38, value = "Keystore [%s] is not writable or not a file.")
    Exception keyStoreNotWritable(final String keystoreURL);

    /**
     * Create an exception when KeyStore password is not specified.
     *
     * @return the exception
     */
    @Message(id = 39, value = "Keystore password has to be specified.")
    Exception keyStorePasswordNotSpecified();

    /**
     * Create an exception when encryption directory does not exist or is not a directory.
     *
     * @param directory directory name
     * @return the exception
     */
    @Message(id = 41, value = "Encryption directory is not a directory or doesn't exist. (%s)")
    Exception encryptionDirectoryDoesNotExist(final String directory);

    /**
     * Create an exception when encryption directory cannot be created.
     *
     * @param directory directory name
     * @return the exception
     */
    @Message(id = 42, value = "Cannot create encryption directory %s")
    Exception cannotCreateEncryptionDirectory(final String directory);

    /**
     * Create an exception when iteration count is out of range.
     *
     * @param iteration iteration count
     * @return the exception
     */
    @Message(id = 43, value = "Iteration count has to be within 1 - " + Integer.MAX_VALUE + ", but it is %s.")
    Exception iterationCountOutOfRange(final String iteration);

    /**
     * Create an exception when salt has different length than 8.
     *
     * @return the exception
     */
    @Message(id = 44, value = "Salt has to be exactly 8 characters long.")
    Exception saltWrongLength();

    /**
     * Unspecified exception encountered.
     *
     * @return the exception
     */
    @Message(id = 45, value = "Exception encountered:")
    Exception securityVaultException(@Cause SecurityVaultException cause);

    /**
     * Create an exception when Vault alias is not specified.
     *
     * @return the exception
     */
    @Message(id = 46, value = "Vault alias has to be specified.")
    Exception vaultAliasNotSpecified();

    /**
     * Display string at the end of successful attribute creation.
     *
     * @param VaultBlock  name of vault block
     * @param attributeName name of value attribute
     * @param configurationString configuration details
     * @return the localized text
     */
    @Message(id = 47, value =
            "Secured attribute value has been stored in Vault.\n" +
            "Please make note of the following:\n" +
            "********************************************\n" +
            "Vault Block:%s\n" + "Attribute Name:%s\n" +
            "Configuration should be done as follows:\n" +
            "%s\n" +
            "********************************************")
    String vaultAttributeCreateDisplay(String VaultBlock, String attributeName, String configurationString);

    /**
     * i18n version of string from Vault Tool utility
     *
     * @return the localized text
     */
    @Message(id = 48, value = "Vault Configuration commands in WildFly for CLI:")
    String vaultConfigurationTitle();

    /**
     * i18n version of string from Vault Tool utility
     *
     * @return the localized text
     */
    @Message(id = 49, value = "No console.")
    String noConsole();

    /**
     * i18n version of string from Vault Tool utility
     *
     * @return the localized text
     */
    @Message(id = 56, value = "Initializing Vault")
    String initializingVault();

    /**
     * i18n version of string from Vault Tool utility
     *
     * @return the localized text
     */
    @Message(id = 57, value = "Vault is initialized and ready for use")
    String vaultInitialized();

    /**
     * i18n version of string from Vault Tool utility
     *
     * @return the localized text
     */
    @Message(id = 58, value = "Handshake with Vault complete")
    String handshakeComplete();

    /**
     * i18n version of string from Vault Tool utility
     *
     * @return the localized text
     */
    @Message(id = 59, value = "Exception encountered:")
    String exceptionEncountered();

    /**
     * i18n version of string from Vault Tool utility
     *
     * @return the localized text
     */
    @Message(id = 68, value = "Problem while parsing command line parameters:")
    String problemParsingCommandLineParameters();

    /**
     * i18n version of string from Vault Tool utility
     *
     * @return the localized text
     */
    @Message(id = 80, value = "Secured attribute (password) already exists.")
    String cmdLineSecuredAttributeAlreadyExists();

    /**
     * i18n version of string from Vault Tool utility
     *
     * @return the localized text
     */
    @Message(id = 81, value = "Secured attribute (password) doesn't exist.")
    String cmdLineSecuredAttributeDoesNotExist();

    /**
     * i18n version of string from Vault Tool utility
     *
     * @return  the localized text
     */
    @Message(id = NONE, value = "Enter directory to store encrypted files:")
    String enterEncryptionDirectory();

    /**
     * i18n version of string from Vault Tool utility
     *
     * @return the localized text
     */
    @Message(id = NONE, value = "Enter Keystore URL:")
    String enterKeyStoreURL();

    /**
     * i18n version of string from Vault Tool utility
     *
     * @return the localized text
     */
    @Message(id = NONE, value = "Enter Keystore password:")
    String enterKeyStorePassword();

    /**
     * Keystore password confirmation
     *
     * @return the localized text
     */
    @Message(id = NONE, value = "Enter Keystore password again:")
    String enterKeyStorePasswordAgain();

    /**
     * i18n version of string from Vault Tool utility
     *
     * @return the localized text
     */
    @Message(id = NONE, value = "Enter 8 character salt:")
    String enterSalt();

    /**
     * i18n version of string from Vault Tool utility
     *
     * @return the localized text
     */
    @Message(id = NONE, value = "Enter iteration count as a number (e.g.: 44):")
    String enterIterationCount();

    /**
     * i18n version of string from Vault Tool utility
     *
     * @return the localized text
     */
    @Message(id = NONE, value = "Enter Keystore Alias:")
    String enterKeyStoreAlias();

    /**
     * i18n version of string from Vault Tool utility
     *
     * @return the localized text
     */
    @Message(id = NONE, value = "Enter your password:")
    String enterYourPassword();

    /**
     * Password confirmation
     *
     * @return the localized text
     */
    @Message(id = NONE, value = "Enter your password again:")
    String enterYourPasswordAgain();

    /**
     * i18n version of string from Vault Tool utility
     *
     * @return the localized text
     */
    @Message(id = NONE, value = "Values entered don't match")
    String passwordsDoNotMatch();

    /**
     * i18n version of string from Vault Tool utility
     *
     * @return the localized text
     */
    @Message(id = NONE, value = "Values match")
    String passwordsMatch();

    /**
     * i18n version of string from Vault Tool utility
     *
     * @return the localized text
     */
    @Message(id = NONE, value = "Please enter a Digit::  0: Store a secured attribute  1: Check whether a secured attribute exists  2: Remove secured attribute  3: Exit")
    String interactionCommandOptions();

    /**
     * i18n version of string from Vault Tool utility
     *
     * @return the localized text
     */
    @Message(id = NONE, value = "Task: Store a secured attribute")
    String taskStoreSecuredAttribute();

    /**
     * i18n version of string from Vault Tool utility
     *
     * @return the localized text
     */
    @Message(id = NONE, value = "Please enter secured attribute value (such as password)")
    String interactivePromptSecureAttributeValue();

    /**
     * i18n version of string from Vault Tool utility
     *
     * @return the localized text
     */
    @Message(id = NONE, value = "Enter Vault Block:")
    String interactivePromptVaultBlock();

    /**
     * i18n version of string from Vault Tool utility
     *
     * @return the localized text
     */
    @Message(id = NONE, value = "Enter Attribute Name:")
    String interactivePromptAttributeName();

    /**
     * i18n version of string from Vault Tool utility
     *
     * @return the localized text
     */
    @Message(id = NONE, value = "Problem occurred:")
    String problemOcurred();

    /**
     * i18n version of string from Vault Tool utility
     *
     * @return the localized text
     */
    @Message(id = NONE, value = "Please enter a Digit::   0: Start Interactive Session   1: Remove Interactive Session  2: Exit")
    String interactiveCommandString();

    /**
     * i18n version of string from Vault Tool utility
     *
     * @return the localized text
     */
    @Message(id = NONE, value = "Starting an interactive session")
    String startingInteractiveSession();

    /**
     * i18n version of string from Vault Tool utility
     *
     * @return the localized text
     */
    @Message(id = NONE, value = "Removing the current interactive session")
    String removingInteractiveSession();

    /**
     * i18n version of string from Vault Tool utility
     *
     * @return the localized text
     */
    @Message(id = NONE, value = "Keystore URL")
    String cmdLineKeyStoreURL();

    /**
     * i18n version of string from Vault Tool utility
     *
     * @return the localized text
     */
    @Message(id = NONE, value = "Keystore password")
    String cmdLineKeyStorePassword();

    /**
     * i18n version of string from Vault Tool utility
     *
     * @return the localized text
     */
    @Message(id = NONE, value = "Directory containing encrypted files")
    String cmdLineEncryptionDirectory();

    /**
     * i18n version of string from Vault Tool utility
     *
     * @return the localized text
     */
    @Message(id = NONE, value = "8 character salt")
    String cmdLineSalt();

    /**
     * i18n version of string from Vault Tool utility
     *
     * @return the localized text
     */
    @Message(id = NONE, value = "Iteration count")
    String cmdLineIterationCount();

    /**
     * i18n version of string from Vault Tool utility
     *
     * @return the localized text
     */
    @Message(id = NONE, value = "Vault keystore alias")
    String cmdLineVaultKeyStoreAlias();

    /**
     * i18n version of string from Vault Tool utility
     *
     * @return the localized text
     */
    @Message(id = NONE, value = "Vault block")
    String cmdLineVaultBlock();

    /**
     * i18n version of string from Vault Tool utility
     *
     * @return the localized text
     */
    @Message(id = NONE, value = "Attribute name")
    String cmdLineAttributeName();

    /**
     * i18n version of string from Vault Tool utility
     *
     * @return the localized text
     */
    @Message(id = NONE, value = "Automatically create keystore when it doesn't exist")
    String cmdLineAutomaticallyCreateKeystore();

    /**
     * i18n version of string from Vault Tool utility
     *
     * @return the localized text
     */
    @Message(id = NONE, value = "Secured attribute value (such as password) to store")
    String cmdLineSecuredAttribute();

    /**
     * i18n version of string from Vault Tool utility
     *
     * @return the localized text
     */
    @Message(id = NONE, value = "Check whether the secured attribute already exists in the Vault")
    String cmdLineCheckAttribute();

    /**
     * i18n version of string from Vault Tool utility
     *
     * @return the localized text
     */
    @Message(id = NONE, value = "Remove secured attribute from the Vault")
    String cmdLineRemoveSecuredAttribute();

    /**
     * i18n version of string from Vault Tool utility
     *
     * @return the localized text
     */
    @Message(id = NONE, value = "Help")
    String cmdLineHelp();

    /**
     * i18n version of string from Vault Tool utility
     *
     * @return the localized text
     */
    @Message(id = NONE, value = "Secured attribute %s has been successfuly removed from vault")
    String messageAttributeRemovedSuccessfuly(String displayFormattedAttribute);

    /**
     * i18n version of string from Vault Tool utility
     *
     * @return the localized text
     */
    @Message(id = NONE, value = "Secured attribute %s was not removed from vault, check whether it exist")
    String messageAttributeNotRemoved(String displayFormattedAttribute);

    /**
     * i18n version of string from Vault Tool utility
     *
     * @return the localized text
     */
    @Message(id = NONE, value = "Action not specified")
    String actionNotSpecified();

    /**
     * i18n version of string from Vault Tool utility
     *
     * @return the localized text
     */
    @Message(id = NONE, value = "Please enter secured attribute value again")
    String interactivePromptSecureAttributeValueAgain();

    /**
     * i18n version of string from Vault Tool utility
     *
     * @return the localized text
     */
    @Message(id = NONE, value = "Task: Verify whether a secured attribute exists")
    String taskVerifySecuredAttributeExists();

    /**
     * i18n version of string from Vault Tool utility
     *
     * @return the localized text
     */
    @Message(id = NONE, value = "No value has been stored for %s")
    String interactiveMessageNoValueStored(String displayFormattedAttribute);

    /**
     * i18n version of string from Vault Tool utility
     *
     * @return the localized text
     */
    @Message(id = NONE, value = "A value exists for %s")
    String interactiveMessageValueStored(String displayFormattedAttribute);

    /**
     * i18n version of string from Vault Tool utility
     *
     * @return the localized text
     */
    @Message(id = NONE, value = "Task: Remove secured attribute")
    String taskRemoveSecuredAttribute();

}
