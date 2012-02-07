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

package org.jboss.as.server;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;

import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.server.deployment.DeploymentUnitProcessingException;
import org.jboss.as.server.deployment.MountType;
import org.jboss.as.server.services.security.VaultReaderException;
import org.jboss.dmr.ModelNode;
import org.jboss.logging.Cause;
import org.jboss.logging.Message;
import org.jboss.logging.MessageBundle;
import org.jboss.logging.Messages;
import org.jboss.logging.Param;
import org.jboss.modules.ModuleIdentifier;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.StartException;
import org.jboss.vfs.VirtualFile;

/**
 * This module is using message IDs in the range 15700-15999.
 * This file is using the subset 15800-15849 for server non-logger messages.
 * See http://community.jboss.org/docs/DOC-16810 for the full list of
 * currently reserved JBAS message id blocks.
 *
 * @author <a href="mailto:jperkins@redhat.com">James R. Perkins</a>
 * @author Mike M. Clark
 */
@MessageBundle(projectCode = "JBAS")
public interface ServerMessages {

    // Messages for standalone.sh help text

    /**
     * The messages
     */
    ServerMessages MESSAGES = Messages.getBundle(ServerMessages.class);
    /**
     * Instructions for the usage of standalone.sh.
     *
     * @return the message.
     */
    @Message(id = Message.NONE, value = "Usage: ./standalone.sh [args...]%nwhere args include:")
    String argUsage();

    /**
     * Instructions for the {@link org.jboss.as.process.CommandLineArgument#DOMAIN_CONFIG} and {@link
     * org.jboss.as.process.CommandLineArgument#SHORT_DOMAIN_CONFIG} command line arguments.
     *
     * @return the message.
     */
    @Message(id = Message.NONE, value = "Name of the server configuration file to use (default is \"standalone.xml\")")
    String argServerConfig();

    /**
     * Instructions for the {@link org.jboss.as.process.CommandLineArgument#SHORT_HELP} or {@link
     * org.jboss.as.process.CommandLineArgument#HELP} command line argument.
     *
     * @return the message.
     */
    @Message(id = Message.NONE, value = "Display this message and exit")
    String argHelp();

    /**
     * Instructions for the {@link org.jboss.as.process.CommandLineArgument#SHORT_PROPERTIES} or {@link
     * org.jboss.as.process.CommandLineArgument#PROPERTIES} command line argument.
     *
     * @return the message.
     */
    @Message(id = Message.NONE, value = "Load system properties from the given url")
    String argProperties();

    /**
     * Instructions for the {@link org.jboss.as.server.CommandLineArgument#SECURITY_PROP} command line argument.
     *
     * @return the message.
     */
    @Message(id = Message.NONE, value = "Set a security property")
    String argSecurityProperty();

    /**
     * Instructions for the {@link org.jboss.as.process.CommandLineArgument#SYSTEM_PROPERTY} command line argument.
     *
     * @return the message.
     */
    @Message(id = Message.NONE, value = "Set a system property")
    String argSystem();

    /**
     * Instructions for the {@link org.jboss.as.process.CommandLineArgument#SHORT_VERSION}, {@link
     * org.jboss.as.process.CommandLineArgument#LEGACY_SHORT_VERSION} or {@link org.jboss.as.process.CommandLineArgument#VERSION}
     * command line argument.
     *
     * @return the message.
     */
    @Message(id = Message.NONE, value = "Print version and exit")
    String argVersion();

    /**
     * Instructions for the {@link org.jboss.as.process.CommandLineArgument#PUBLIC_BIND_ADDRESS} or {@link
     * org.jboss.as.process.CommandLineArgument#LEGACY_PUBLIC_BIND_ADDRESS} command line argument.
     *
     * @return the message.
     */
    @Message(id = Message.NONE, value = "Set system property jboss.bind.address to the given value")
    String argPublicBindAddress();

    /**
     * Instructions for the {@link org.jboss.as.process.CommandLineArgument#INTERFACE_BIND_ADDRESS} command line
     * argument.
     *
     * @return the message.
     */
    @Message(id = Message.NONE, value = "Set system property jboss.bind.address.<interface> to the given value")
    String argInterfaceBindAddress();

    /**
     * Instructions for the {@link org.jboss.as.process.CommandLineArgument#DEFAULT_MULTICAST_ADDRESS} command line
     * argument.
     *
     * @return the message.
     */
    @Message(id = Message.NONE, value = "Set system property jboss.default.multicast.address to the given value")
    String argDefaultMulticastAddress();

    /**
     * Instructions for the {@link org.jboss.as.process.CommandLineArgument#ADMIN_ONLY} command line argument.
     *
     * @return the message.
     */
    @Message(id = Message.NONE, value = "Set the server's running type to ADMIN_ONLY causing it to open administrative interfaces and accept management requests but not start other runtime services or accept end user requests.")
    String argAdminOnly();

    /**
     * Creates an error message indicating a value was expected for the given command line option.
     *
     * @param option the name of the command line option
     *
     * @return a message that can by output to stderr.
     */
    @Message(id = 15800, value = "Value expected for option %s")
    String valueExpectedForCommandLineOption(String option);

    /**
     * Creates an error message indicating an invalid command line option was presented.
     *
     * @param option the name of the command line option
     *
     * @return a message that can by output to stderr.
     */
    @Message(id = 15801, value = "Invalid option '%s'")
    String invalidCommandLineOption(String option);

    /**
     * Creates an error message indicating a malformed URL was provided as a value for a command line option.
     *
     * @param urlSpec the provided url
     * @param option the name of the command line option
     *
     * @return a message that can by output to stderr.
     */
    @Message(id = 15802, value = "Malformed URL '%s' provided for option '%s'")
    String malformedCommandLineURL(String urlSpec, String option);

    /**
     * Creates an error message indicating {@link java.util.Properties#load(InputStream) properties could not be loaded}
     * from a given url.
     *
     * @param url the provided url
     *
     * @return a message that can by output to stderr.
     */
    @Message(id = 15803, value = "Unable to load properties from URL '%s'")
    String unableToLoadProperties(URL url);

    /**
     * Creates an error message indicating creating a security vault failed.
     *
     * @param cause the problem
     * @param msg the problem (for use in the message)
     *
     * @return a RuntimeException wrapper
     */
    @Message(id = 15804, value = "Error initializing vault --  %s")
    RuntimeException cannotCreateVault(@Param VaultReaderException cause, VaultReaderException msg);

    /**
     * Creates an error message indicating that connecting to the HC failed.
     *
     * @param e the problem
     * @return a StartException
     */
    @Message(id = 15805, value = "Failed to connect to the host-controller")
    StartException failedToConnectToHC(@Param Exception e);

    /**
     * Creates an error message indicating that the operation connecting to the
     * HC got cancelled before it could complete.
     *
     * @return a StartException
     */
    @Message(id = 15806, value = "Connection request to the host-controller was cancelled")
    StartException cancelledHCConnect();

    @Message(id = 15807, value = "hostControllerName must be null if the server is not in a managed domain")
    IllegalArgumentException hostControllerNameNonNullInStandalone();

    @Message(id = 15808, value = "hostControllerName may not be null if the server is in a managed domain")
    IllegalArgumentException hostControllerNameNullInDomain();

    @Message(id = 15809, value = "An IP address cannot be resolved using the given interface selection criteria. Failure was -- %s")
    OperationFailedException cannotResolveInterface(Exception msg, @Param Exception cause);

    @Message(id = 15810, value = "failed to resolve interface %s")
    StartException failedToResolveInterface(String name);

    @Message(id = 15811, value = "Failed to start the http-interface service")
    StartException failedToStartHttpManagementService(@Param Exception e);

    @Message(id = 15812, value = "No deployment content with hash %s is available in the deployment content repository.")
    OperationFailedException noSuchDeploymentContent(String hash);

    @Message(id = 15813, value = "No deployment with name %s found")
    OperationFailedException noSuchDeployment(String deploymentName);

    @Message(id = 15814, value = "Cannot use %s with the same value for parameters %s and %s. " +
                        "Use %s to redeploy the same content or %s to replace content with a new version with the same name.")
    OperationFailedException cannotReplaceDeployment(String replaceOperation, String name, String toReplace, String redeployOperation, String fullReplace);

    @Message(id = 15815, value = "Deployment %s is already started")
    OperationFailedException deploymentAlreadyStarted(String deploymentName);

    @Message(id = 15816, value = "Missing configuration value for: %s")
    IllegalStateException missingHomeDirConfiguration(String propertyName);

    @Message(id = 15817, value = "\n        %s is missing: %s")
    String missingDependencies(ServiceName dependentService, String missingDependencies);

    @Message(id = 15818, value = "%s is required")
    OperationFailedException attributeIsRequired(String attribute);

    @Message(id = 15819, value = "%s is not allowed when %s are present")
    OperationFailedException attributeNotAllowedWhenAlternativeIsPresent(String attribute, List<String> alternatives);

    @Message(id = 15820, value = "%s is invalid")
    OperationFailedException attributeIsInvalid(String attribute);

    @Message(id = 15821, value = "Caught IOException reading uploaded deployment content")
    OperationFailedException caughtIOExceptionUploadingContent(@Cause IOException cause);

    @Message(id = 15822, value = "Null stream at index [%d]")
    OperationFailedException nullStreamAttachment(int index);

    @Message(id = 15823, value = "'%s' is not a valid URL")
    OperationFailedException invalidDeploymentURL(String urlSpec, @Cause MalformedURLException e);

    @Message(id = 15824, value = "Error obtaining input stream from URL '%s'")
    OperationFailedException problemOpeningStreamFromDeploymentURL(String urlSpec, @Cause IOException e);

    @Message(id = 15826, value = "ServiceModuleLoader already started")
    IllegalStateException serviceModuleLoaderAlreadyStarted();

    @Message(id = 15827, value = "ServiceModuleLoader already stopped")
    IllegalStateException serviceModuleLoaderAlreadyStopped();

    @Message(id = 15828, value = "'%s' cannot be loaded from a ServiceModuleLoader as its name does not start with '%s'")
    IllegalArgumentException missingModulePrefix(ModuleIdentifier identifier, String prefix);

    @Message(id = 15829, value = "Failed to read '%s'")
    DeploymentUnitProcessingException failedToReadVirtualFile(VirtualFile file, @Cause IOException cause);

    @Message(id = 15830, value = "Deployment root is required")
    IllegalArgumentException deploymentRootRequired();

    @Message(id = 15831, value = "Sub-deployments require a parent deployment unit")
    IllegalArgumentException subdeploymentsRequireParent();

    @Message(id = 15832, value = "No Module Identifier attached to deployment '%s'")
    DeploymentUnitProcessingException noModuleIdentifier(String deploymentUnitName);

    @Message(id = 15834, value = "Failed to create VFSResourceLoader for root [%s]")
    DeploymentUnitProcessingException failedToCreateVFSResourceLoader(String resourceRoot, @Cause IOException cause);

    @Message(id = 15835, value = "Failed to get file from remote repository")
    RuntimeException failedToGetFileFromRemoteRepository(@Cause Throwable cause);

    @Message(id = 15836, value = "Unable to create local directory: %s")
    IOException cannotCreateLocalDirectory(File path);

    @Message(id = 15837, value = "Did not read the entire file. Missing: %d")
    IOException didNotReadEntireFile(long missing);

   @Message(id = 15838, value = "No value was provided for argument %s%n")
    String noArgValue(String argument);

    @Message(id = 15839, value = "Could not find the file repository connection to the host controller.")
    IllegalStateException couldNotFindHcFileRepositoryConnection();

    @Message(id = 15840, value = "Only 'hash' is allowed for deployment addition for a domain mode server: %s")
    IllegalStateException onlyHashAllowedForDeploymentAddInDomainServer(ModelNode contentItemNode);

    @Message(id = 15841, value = "Only 'hash' is allowed for deployment full replacement for a domain mode server: %s")
    IllegalStateException onlyHashAllowedForDeploymentFullReplaceInDomainServer(ModelNode contentItemNode);

    @Message(id = 15842, value = "Unknown mount type %s")
    IllegalArgumentException unknownMountType(MountType mountType);

    /**
     * Creates an exception indicating a failure to create a temp file provider.
     *
     * @return a {@link StartException} for the error.
     */
    @Message(id = 15843, value = "Failed to create temp file provider")
    StartException failedCreatingTempProvider();

    @Message(id = 15844, value = "%s cannot be defined when either %s or %s is also defined")
    OperationFailedException illegalCombinationOfHttpManagementInterfaceConfigurations(String interfaceAttr,
                                                                             String socketBindingAttr,
                                                                             String secureSocketBindingAttr);

    @Message(id = 15845, value = "System property %s cannot be set via the xml configuration file or from a management client; " +
            "it's value must be known at initial process start so it can only set from the commmand line")
    OperationFailedException systemPropertyNotManageable(String propertyName);


    @Message(id = 15846, value = "System property %s cannot be set after the server name has been set via the xml " +
            "configuration file or from a management client")
    OperationFailedException systemPropertyCannotOverrideServerName(String propertyName);

    @Message(id = 15847, value = "Unable to initialise a basic SSLContext '%s'")
    IOException unableToInitialiseSSLContext(String message);

    /*
     * WARNING!!! id 15849 is the last id in the available block for this class. Once the block
     * is used up, check what ids ServerLogger is using, allocate another block for use by this
     * class from the overall id pool used by this module and update the class javadoc above
     * and this message accordingly.
     */
}
