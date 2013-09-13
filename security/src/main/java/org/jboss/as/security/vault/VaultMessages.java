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

package org.jboss.as.security.vault;

import org.jboss.logging.Messages;
import org.jboss.logging.annotations.Cause;
import org.jboss.logging.annotations.Message;
import org.jboss.logging.annotations.MessageBundle;
import org.jboss.security.vault.SecurityVaultException;

/**
 * This module is using message IDs in the range 2100-21099.
 * <p/>
 * This file is using the subset 21000-21099 for non-logger messages. They are used for command line utility texts therefore
 * projectCode is set to "" to be suppressed from the messages.
 * <p/>
 * See <a href="http://community.jboss.org/docs/DOC-16810">http://community.jboss.org/docs/DOC-16810</a> for the full list of
 * currently reserved JBAS message id blocks.
 * <p/>
 * Date: 12.09.2013
 *
 * @author Peter Skopek (pskopek at redhat dot com)
 *
 */
@MessageBundle(projectCode = "")
public interface VaultMessages {

    /**
     * The messages
     */
    VaultMessages MESSAGES = Messages.getBundle(VaultMessages.class);

    /**
     * Create an Exception when KeyStore cannot be located with example how to create one.
     *
     * @param keystoreURL
     * @param keystoreURLExample
     * @return
     */
    @Message(id = 21000, value = "Keystore '%s' doesn't exist."
            + "\nkeystore could be created: "
            + "keytool -genseckey -alias Vault -storetype jceks -keyalg AES -keysize 128 -storepass secretsecret -keypass secretsecret -keystore %s")
    Exception keyStoreDoesnotExistWithExample(final String keystoreURL, final String keystoreURLExample);

    /**
     * Create an Exception when one cannot write to the KeyStore or it is not a file.
     *
     * @param keystoreURL
     * @return
     */
    @Message(id = 21001, value = "Keystore [%s] is not writable or not a file.")
    Exception keyStoreNotWritable(final String keystoreURL);

    /**
     * Create an exception when KeyStore password is not specified.
     *
     * @return
     */
    @Message(id = 21002, value = "Keystore password has to be specified.")
    Exception keyStorePasswordNotSpecified();

    /**
     * Create an exception when encryption directory is not specified.
     *
     * @return
     */
    @Message(id = 21003, value = "Encryption directory has to be specified.")
    Exception encryptionDirectoryHasToBeSpecified();

    /**
     * Create an exception when encryption directory does not exist or is not a directory.
     *
     * @param directory
     * @return
     */
    @Message(id = 21004, value = "Encryption directory is not a directory or doesn't exist. (%s)")
    Exception encryptionDirectoryDoesNotExist(final String directory);

    /**
     * Create an exception when encryption directory cannot be created.
     *
     * @param directory
     * @return
     */
    @Message(id = 21005, value = "Cannot create encryption directory %s")
    Exception cannotCreateEncryptionDirectory(final String directory);

    /**
     * Create an exception when iteration count is out of range.
     *
     * @param iteration
     * @return
     */
    @Message(id = 21006, value = "Iteration count has to be within 1 - " + Integer.MAX_VALUE + ", but it is %s.")
    Exception iterationCountOutOfRange(final String iteration);

    /**
     * Create an exception when salt has different length than 8.
     *
     * @return
     */
    @Message(id = 21007, value = "Salt has to be exactly 8 characters long.")
    Exception saltWrongLength();

    /**
     * Unspecified exception encountered.
     *
     * @param e
     * @return
     */
    @Message(id = 21008, value = "Exception encountered:")
    Exception securityVaultException(@Cause SecurityVaultException e);

    /**
     * Create an exception when Vault alias is not specified.
     *
     * @return
     */
    @Message(id = 21009, value = "Vault alias has to be specified.")
    Exception vaultAliasNotSpecified();

    /**
     * Display string at the end of successful attribute creation.
     *
     * @param VaultBlock
     * @param attributeName
     * @param configurationString
     * @return
     */
    @Message(id = 21010, value =
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
     * @return
     */
    @Message(id = 21011, value = "Vault Configuration in WildFly configuration file:")
    String vaultConfigurationTitle();

    /**
     * i18n version of string from Vault Tool utility
     *
     * @return
     */
    @Message(id = 21012, value = "No console.")
    String noConsole();

    /**
     * i18n version of string from Vault Tool utility
     *
     * @return
     */
    @Message(id = 21013, value = "Enter directory to store encrypted files:")
    String enterEncryptionDirectory();

    /**
     * i18n version of string from Vault Tool utility
     *
     * @return
     */
    @Message(id = 21014, value = "Enter Keystore URL:")
    String enterKeyStoreURL();

    /**
     * i18n version of string from Vault Tool utility
     *
     * @return
     */
    @Message(id = 21015, value = "Enter Keystore password")
    String enterKeyStorePassword();

    /**
     * i18n version of string from Vault Tool utility
     *
     * @return
     */
    @Message(id = 21016, value = "Enter 8 character salt:")
    String enterSalt();

    /**
     * i18n version of string from Vault Tool utility
     *
     * @return
     */
    @Message(id = 21017, value = "Enter iteration count as a number (e.g.: 44):")
    String enterIterationCount();

    /**
     * i18n version of string from Vault Tool utility
     *
     * @return
     */
    @Message(id = 21018, value = "Enter Keystore Alias:")
    String enterKeyStoreAlias();

    /**
     * i18n version of string from Vault Tool utility
     *
     * @return
     */
    @Message(id = 21019, value = "Initializing Vault")
    String initializingVault();

    /**
     * i18n version of string from Vault Tool utility
     *
     * @return
     */
    @Message(id = 21020, value = "Vault is initialized and ready for use")
    String vaultInitialized();

    /**
     * i18n version of string from Vault Tool utility
     *
     * @return
     */
    @Message(id = 21021, value = "Handshake with Vault complete")
    String handshakeComplete();

    /**
     * i18n version of string from Vault Tool utility
     *
     * @return
     */
    @Message(id = 21022, value = "Exception encountered:")
    String exceptionEncountered();

    /**
     * i18n version of string from Vault Tool utility
     *
     * @return
     */
    @Message(id = 21023, value = "Enter your password")
    String enterYourPassword();

    /**
     * i18n version of string from Vault Tool utility
     *
     * @return
     */
    @Message(id = 21024, value = " again: ")
    String passwordAgain();

    /**
     * i18n version of string from Vault Tool utility
     *
     * @return
     */
    @Message(id = 21025, value = "Values entered don't match")
    String passwordsDoNotMatch();

    /**
     * i18n version of string from Vault Tool utility
     *
     * @return
     */
    @Message(id = 21026, value = "Values match")
    String passwordsMatch();

    /**
     * i18n version of string from Vault Tool utility
     *
     * @return
     */
    @Message(id = 21027, value = "Problem occurred:")
    String problemOcurred();

    /**
     * i18n version of string from Vault Tool utility
     *
     * @return
     */
    @Message(id = 21028, value = "Please enter a Digit::   0: Start Interactive Session   1: Remove Interactive Session  2: Exit")
    String interactiveCommandString();

    /**
     * i18n version of string from Vault Tool utility
     *
     * @return
     */
    @Message(id = 21029, value = "Starting an interactive session")
    String startingInteractiveSession();

    /**
     * i18n version of string from Vault Tool utility
     *
     * @return
     */
    @Message(id = 21030, value = "Removing the current interactive session")
    String removingInteractiveSession();

    /**
     * i18n version of string from Vault Tool utility
     *
     * @return
     */
    @Message(id = 21031, value = "Problem while parsing command line parameters:")
    String problemParsingCommandLineParameters();

    /**
     * i18n version of string from Vault Tool utility
     *
     * @return
     */
    @Message(id = 21032, value = "Keystore URL")
    String cmdLineKeyStoreURL();

    /**
     * i18n version of string from Vault Tool utility
     *
     * @return
     */
    @Message(id = 21033, value = "Keystore password")
    String cmdLineKeyStorePassword();

    /**
     * i18n version of string from Vault Tool utility
     *
     * @return
     */
    @Message(id = 21034, value = "Directory containing encrypted files")
    String cmdLineEncryptionDirectory();

    /**
     * i18n version of string from Vault Tool utility
     *
     * @return
     */
    @Message(id = 21035, value = "8 character salt")
    String cmdLineSalt();

    /**
     * i18n version of string from Vault Tool utility
     *
     * @return
     */
    @Message(id = 21036, value = "Iteration count")
    String cmdLineIterationCount();

    /**
     * i18n version of string from Vault Tool utility
     *
     * @return
     */
    @Message(id = 21037, value = "Vault keystore alias")
    String cmdLineVaultKeyStoreAlias();

    /**
     * i18n version of string from Vault Tool utility
     *
     * @return
     */
    @Message(id = 21038, value = "Vault block")
    String cmdLineVaultBlock();

    /**
     * i18n version of string from Vault Tool utility
     *
     * @return
     */
    @Message(id = 21039, value = "Attribute name")
    String cmdLineAttributeName();

    /**
     * i18n version of string from Vault Tool utility
     *
     * @return
     */
    @Message(id = 21040, value = "Secured attribute value (such as password) to store")
    String cmdLineSecuredAttribute();

    /**
     * i18n version of string from Vault Tool utility
     *
     * @return
     */
    @Message(id = 21041, value = "Check whether the secured attribute already exists in the Vault")
    String cmdLineCheckAttribute();

    /**
     * i18n version of string from Vault Tool utility
     *
     * @return
     */
    @Message(id = 21042, value = "Help")
    String cmdLineHelp();

    /**
     * i18n version of string from Vault Tool utility
     *
     * @return
     */
    @Message(id = 21043, value = "Secured attribute (password) already exists.")
    String cmdLineSecuredAttributeAlreadyExists();

    /**
     * i18n version of string from Vault Tool utility
     *
     * @return
     */
    @Message(id = 21044, value = "Secured attribute (password) doesn't exist.")
    String cmdLineSecuredAttributeDoesNotExist();

}
