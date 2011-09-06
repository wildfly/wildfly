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

package org.jboss.as.process;

import org.jboss.logging.Message;
import org.jboss.logging.MessageBundle;
import org.jboss.logging.Messages;

/**
 * Date: 29.06.2011
 *
 * @author <a href="mailto:jperkins@redhat.com">James R. Perkins</a>
 */
@MessageBundle(projectCode = "JBAS")
interface ProcessMessages {
    /**
     * The default messages.
     */
    ProcessMessages MESSAGES = Messages.getBundle(ProcessMessages.class);

    @Message(value = "Usage: ./domain.sh [args...]%nwhere args include:")
    String argUsage();

    /**
     * Instructions for the {@link CliArgument#BACKUP} command line argument.
     *
     * @return the message.
     */
    @Message(value = "Keep a copy of the persistent domain configuration even if this host is not the Domain Controller")
    String argBackup();

    /**
     * Instructions for the {@link CliArgument#CACHED_DC} command line argument.
     *
     * @return the message.
     */
    @Message(value = "If this host is not the Domain Controller and cannot contact the Domain Controller at boot, boot using a locally cached copy of the domain configuration (see -backup)")
    String argCachedDc();

    /**
     * Instructions for the {@link CliArgument#DOMAIN_CONFIG} command line argument.
     *
     * @return the message.
     */
    @Message(value = "Name of the domain configuration file to use (default is \"domain.xml\")")
    String argDomainConfig();

    /**
     * Instructions for the {@link CliArgument#SHORT_HELP} or {@link CliArgument#HELP} command line argument.
     *
     * @return the message.
     */
    @Message(value = "Display this message and exit")
    String argHelp();

    /**
     * Instructions for the {@link CliArgument#INTERPROCESS_HC_ADDRESS} command line argument.
     *
     * @return the message.
     */
    @Message(value = "Address this host controller's socket should listen onr")
    String argInterProcessHcAddress();

    /**
     * Instructions for the {@link CliArgument#INTERPROCESS_HC_PORT} command line argument.
     *
     * @return the message.
     */
    @Message(value = "Port of this host controller's socket  should listen on")
    String argInterProcessHcPort();

    /**
     * Instructions for the {@link CliArgument#INTERPROCESS_NAME} command line argument.
     *
     * @return the message.
     */
    @Message(value = "Name of this process, used to register the socket with the server in the process controller")
    String argInterProcessName();

    /**
     * Instructions for the {@link CliArgument#HOST_CONFIG} command line argument.
     *
     * @return the message.
     */
    @Message(value = "Name of the host configuration file to use (default is \"host.xml\")")
    String argHostConfig();

    /**
     * Instructions for the {@link CliArgument#PC_ADDRESS} command line argument.
     *
     * @return the message.
     */
    @Message(value = "Address of process controller socket")
    String argPcAddress();

    /**
     * Instructions for the {@link CliArgument#PC_PORT} command line argument.
     *
     * @return the message.
     */
    @Message(value = "Port of process controller socket")
    String argPcPort();

    /**
     * Instructions for the {@link CliArgument#SHORT_PROPERTIES} or {@link CliArgument#PROPERTIES} command line argument.
     *
     * @return the message.
     */
    @Message(value = "Load system properties from the given url")
    String argProperties();

    /**
     * Instructions for the {@link CliArgument#SYSTEM_PROPERTY} command line argument.
     *
     * @return the message.
     */
    @Message(value = "Set a system property")
    String argSystem();

    /**
     * Instructions for the {@link CliArgument#SHORT_VERSION} or {@link CliArgument#VERSION} command line argument.
     *
     * @return the message.
     */
    @Message(value = "Print version and exit")
    String argVersion();

    /**
     * Creates an exception indicating the Java executable could not be found.
     *
     * @param binDir the directory the executable file should be located.
     *
     * @return an {@link IllegalStateException} for the error.
     */
    @Message(value = "Could not find java executable under %s.")
    IllegalStateException cannotFindJavaExe(String binDir);

    /**
     * Creates an exception indicating the authentication key must be 16 bytes long.
     *
     * @return an {@link IllegalArgumentException} for the error.
     */
    @Message(value = "Authentication key must be 16 bytes long")
    IllegalArgumentException invalidAuthKeyLen();

    /**
     * Creates an exception indicating the command must have at least one entry.
     *
     * @return an {@link IllegalArgumentException} for the error.
     */
    @Message(value = "cmd must have at least one entry")
    IllegalArgumentException invalidCommandLen();

    /**
     * Creates an exception indicating the Java home directory does not exist.
     *
     * @param dir the directory to Java home.
     *
     * @return an {@link IllegalStateException} for the error.
     */
    @Message(value = "Java home '%s' does not exist.")
    IllegalStateException invalidJavaHome(String dir);

    /**
     * Creates an exception indicating the Java home bin directory does not exist.
     *
     * @param binDir      the bin directory.
     * @param javaHomeDir the Java home directory.
     *
     * @return an {@link IllegalStateException} for the error.
     */
    @Message(value = "Java home's bin '%s' does not exist. The home directory was determined to be %s.")
    IllegalStateException invalidJavaHomeBin(String binDir, String javaHomeDir);

    /**
     * Creates an exception indicating the parameter has an invalid length.
     *
     * @param parameterName the parameter name.
     *
     * @return an {@link IllegalArgumentException} for the error.
     */
    @Message(value = "%s length is invalid")
    IllegalArgumentException invalidLength(String parameterName);

    /**
     * Creates an exception indicating the option, represented by the {@code option} parameter, is invalid.
     *
     * @param option the invalid option.
     *
     * @return an {@link IllegalArgumentException} for the error.
     */
    @Message(value = "Invalid option: %s")
    IllegalArgumentException invalidOption(String option);

    /**
     * Creates an exception indicating a command contains a {@code null} component.
     *
     * @return an {@link IllegalArgumentException} for the error.
     */
    @Message(value = "Command contains a null component")
    IllegalArgumentException nullCommandComponent();

    /**
     * Creates an exception indicating the variable is {@code null}.
     *
     * @param varName the variable name.
     *
     * @return an {@link IllegalArgumentException} for the error.
     */
    @Message(value = "%s is null")
    IllegalArgumentException nullVar(String varName);
}
