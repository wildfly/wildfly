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
 * This module is using message IDs in the range 12000-12099.
 * This file is using the subset 12040-12099 for non-logger messages.
 * See http://community.jboss.org/docs/DOC-16810 for the full list of
 * currently reserved JBAS message id blocks.
 *
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

    @Message(id = 12040, value = "Usage: ./domain.sh [args...]\nwhere args include:")
    String argUsage();

    /**
     * Instructions for the {@link CommandLineArgument#BACKUP} command line argument.
     *
     * @return the message.
     */
    @Message(id = 12041, value = "Keep a copy of the persistent domain configuration even if this host is not the Domain Controller")
    String argBackup();

    /**
     * Instructions for the {@link CommandLineArgument#CACHED_DC} command line argument.
     *
     * @return the message.
     */
    @Message(id = 12042, value = "If this host is not the Domain Controller and cannot contact the Domain Controller at boot, boot using a locally cached copy of the domain configuration (see --backup)")
    String argCachedDc();

    /**
     * Instructions for the {@link CommandLineArgument#DOMAIN_CONFIG} and {@link CommandLineArgument#SHORT_DOMAIN_CONFIG} command line arguments.
     *
     * @return the message.
     */
    @Message(id = 12043, value = "Name of the domain configuration file to use (default is \"domain.xml\")")
    String argDomainConfig();

    /**
     * Instructions for the {@link CommandLineArgument#SHORT_HELP} or {@link CommandLineArgument#HELP} command line argument.
     *
     * @return the message.
     */
    @Message(id = 12044, value = "Display this message and exit")
    String argHelp();

    /**
     * Instructions for the {@link CommandLineArgument#INTERPROCESS_HC_ADDRESS} command line argument.
     *
     * @return the message.
     */
    @Message(id = 12045, value = "Address on which the host controller should listen for communication from the process controller")
    String argInterProcessHcAddress();

    /**
     * Instructions for the {@link CommandLineArgument#INTERPROCESS_HC_PORT} command line argument.
     *
     * @return the message.
     */
    @Message(id = 12046, value = "Port on which the host controller should listen for communication from the process controller")
    String argInterProcessHcPort();

    /**
     * Instructions for the {@link CommandLineArgument#HOST_CONFIG} command line argument.
     *
     * @return the message.
     */
    @Message(id = 12047, value = "Name of the host configuration file to use (default is \"host.xml\")")
    String argHostConfig();

    /**
     * Instructions for the {@link CommandLineArgument#PC_ADDRESS} command line argument.
     *
     * @return the message.
     */
    @Message(id = 12048, value = "Address on which the process controller listens for communication from processes it controls")
    String argPcAddress();

    /**
     * Instructions for the {@link CommandLineArgument#PC_PORT} command line argument.
     *
     * @return the message.
     */
    @Message(id = 12049, value = "Port on which the process controller listens for communication from processes it controls")
    String argPcPort();

    /**
     * Instructions for the {@link CommandLineArgument#SHORT_PROPERTIES} or {@link CommandLineArgument#PROPERTIES} command line argument.
     *
     * @return the message.
     */
    @Message(id = 12050, value = "Load system properties from the given url")
    String argProperties();

    /**
     * Instructions for the {@link CommandLineArgument#SYSTEM_PROPERTY} command line argument.
     *
     * @return the message.
     */
    @Message(id = 12051, value = "Set a system property")
    String argSystem();

    /**
     * Instructions for the {@link CommandLineArgument#SHORT_VERSION}, {@link CommandLineArgument#LEGACY_SHORT_VERSION} or {@link CommandLineArgument#VERSION} command line argument.
     *
     * @return the message.
     */
    @Message(id = 12052, value = "Print version and exit")
    String argVersion();

    /**
     * Instructions for the {@link CommandLineArgument#PUBLIC_BIND_ADDRESS} or {@link CommandLineArgument#LEGACY_PUBLIC_BIND_ADDRESS} command line argument.
     *
     * @return the message.
     */
    @Message(id = 12053, value = "Set system property jboss.bind.address to the given value")
    String argPublicBindAddress();

    /**
     * Instructions for the {@link CommandLineArgument#INTERFACE_BIND_ADDRESS} command line argument.
     *
     * @return the message.
     */
    @Message(id = 12054, value = "Set system property jboss.bind.address.<interface> to the given value")
    String argInterfaceBindAddress();

    /**
     * Instructions for the {@link CommandLineArgument#DEFAULT_MULTICAST_ADDRESS} command line argument.
     *
     * @return the message.
     */
    @Message(id = 12055, value = "Set system property jboss.default.multicast.address to the given value")
    String argDefaultMulticastAddress();

    /**
     * Instructions for the {@link CommandLineArgument#INTERFACE_BIND_ADDRESS} command line argument.
     *
     * @param argument the name of the argument
     *
     * @return the message.
     */
    @Message(id = 12056, value = "No value was provided for argument %s")
    String noArgValue(String argument);

    /**
     * Creates an exception indicating the Java executable could not be found.
     *
     * @param binDir the directory the executable file should be located.
     *
     * @return an {@link IllegalStateException} for the error.
     */
    @Message(id = 12057, value = "Could not find java executable under %s.")
    IllegalStateException cannotFindJavaExe(String binDir);

    /**
     * Creates an exception indicating the authentication key must be 16 bytes long.
     *
     * @return an {@link IllegalArgumentException} for the error.
     */
    @Message(id = 12058, value = "Authentication key must be 16 bytes long")
    IllegalArgumentException invalidAuthKeyLen();

    /**
     * Creates an exception indicating the command must have at least one entry.
     *
     * @return an {@link IllegalArgumentException} for the error.
     */
    @Message(id = 12059, value = "cmd must have at least one entry")
    IllegalArgumentException invalidCommandLen();

    /**
     * Creates an exception indicating the Java home directory does not exist.
     *
     * @param dir the directory to Java home.
     *
     * @return an {@link IllegalStateException} for the error.
     */
    @Message(id = 12060, value = "Java home '%s' does not exist.")
    IllegalStateException invalidJavaHome(String dir);

    /**
     * Creates an exception indicating the Java home bin directory does not exist.
     *
     * @param binDir      the bin directory.
     * @param javaHomeDir the Java home directory.
     *
     * @return an {@link IllegalStateException} for the error.
     */
    @Message(id = 12061, value = "Java home's bin '%s' does not exist. The home directory was determined to be %s.")
    IllegalStateException invalidJavaHomeBin(String binDir, String javaHomeDir);

    /**
     * Creates an exception indicating the parameter has an invalid length.
     *
     * @param parameterName the parameter name.
     *
     * @return an {@link IllegalArgumentException} for the error.
     */
    @Message(id = 12062, value = "%s length is invalid")
    IllegalArgumentException invalidLength(String parameterName);

    /**
     * Creates an exception indicating the option, represented by the {@code option} parameter, is invalid.
     *
     * @param option the invalid option.
     *
     * @return an {@link IllegalArgumentException} for the error.
     */
    @Message(id = 12063, value = "Invalid option: %s")
    IllegalArgumentException invalidOption(String option);

    /**
     * Creates an exception indicating a command contains a {@code null} component.
     *
     * @return an {@link IllegalArgumentException} for the error.
     */
    @Message(id = 12064, value = "Command contains a null component")
    IllegalArgumentException nullCommandComponent();

    /**
     * Creates an exception indicating the variable is {@code null}.
     *
     * @param varName the variable name.
     *
     * @return an {@link IllegalArgumentException} for the error.
     */
    @Message(id = 12065, value = "%s is null")
    IllegalArgumentException nullVar(String varName);

    /**
     * Instructions for the {@link CommandLineArgument#ADMIN_ONLY} command line argument.
     *
     * @return the message.
     */
    @Message(id = 12066, value = "Set the host controller's running type to ADMIN_ONLY causing it to open administrative interfaces and accept management requests but not start servers or, if this host controller is the master for the domain, accept incoming connections from slave host controllers.")
    String argAdminOnly();

    /**
     * Instructions for the {@link CommandLineArgument#ADMIN_ONLY} command line argument.
     *
     * @return the message.
     */
    @Message(id = 12067, value = "Set system property jboss.domain.master.address to the given value. In a default slave Host Controller config, this is used to configure the address of the master Host Controller.")
    String argMasterAddress();

    /**
     * Instructions for the {@link CommandLineArgument#ADMIN_ONLY} command line argument.
     *
     * @return the message.
     */
    @Message(id = 12068, value = "Set system property jboss.domain.master.port to the given value. In a default slave Host Controller config, this is used to configure the port used for native management communication by the master Host Controller.")
    String argMasterPort();
}
