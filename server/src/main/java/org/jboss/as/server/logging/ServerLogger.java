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

package org.jboss.as.server.logging;

import static org.jboss.logging.Logger.Level.ERROR;
import static org.jboss.logging.Logger.Level.INFO;
import static org.jboss.logging.Logger.Level.WARN;

import java.io.File;
import java.io.IOException;
import java.io.InvalidObjectException;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.UnknownHostException;
import java.util.List;
import java.util.Set;
import java.util.jar.Attributes;

import javax.xml.namespace.QName;
import javax.xml.stream.Location;
import javax.xml.stream.XMLStreamException;

import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.process.CommandLineConstants;
import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.as.server.deployment.DeploymentUnitProcessingException;
import org.jboss.as.server.deployment.DeploymentUnitProcessor;
import org.jboss.as.server.deployment.MountType;
import org.jboss.as.server.deployment.Phase;
import org.jboss.as.server.deployment.module.ExtensionListEntry;
import org.jboss.as.server.deployment.module.ResourceRoot;
import org.jboss.as.server.services.security.VaultReaderException;
import org.jboss.invocation.proxy.MethodIdentifier;
import org.jboss.logging.BasicLogger;
import org.jboss.logging.Logger;
import org.jboss.logging.annotations.Cause;
import org.jboss.logging.annotations.LogMessage;
import org.jboss.logging.annotations.Message;
import org.jboss.logging.annotations.MessageLogger;
import org.jboss.logging.annotations.Param;
import org.jboss.modules.Module;
import org.jboss.modules.ModuleIdentifier;
import org.jboss.modules.ModuleLoadException;
import org.jboss.msc.service.ServiceController.Mode;
import org.jboss.msc.service.ServiceNotFoundException;
import org.jboss.msc.service.StartException;
import org.jboss.vfs.VirtualFile;

/**
 * @author <a href="mailto:jperkins@redhat.com">James R. Perkins</a>
 * @author Mike M. Clark
 */
@MessageLogger(projectCode = "WFLYSRV", length = 4)
public interface ServerLogger extends BasicLogger {

    /**
     * A logger with a category of the package name.
     */
    ServerLogger ROOT_LOGGER = Logger.getMessageLogger(ServerLogger.class, "org.jboss.as.server");

    /**
     * A logger with the category {@code org.jboss.as}.
     */
    ServerLogger AS_ROOT_LOGGER = Logger.getMessageLogger(ServerLogger.class, "org.jboss.as");

    /**
     * A logger with the category {@code org.jboss.as.config}.
     */
    ServerLogger CONFIG_LOGGER = Logger.getMessageLogger(ServerLogger.class, "org.jboss.as.config");

    /**
     * A logger with the category {@code org.jboss.as.warn.fdlimit}.
     */
    ServerLogger FD_LIMIT_LOGGER = Logger.getMessageLogger(ServerLogger.class, "org.jboss.as.warn.fd-limit");

    /**
     * A logger with the category {@code org.jboss.as.server.deployment}.
     */
    ServerLogger DEPLOYMENT_LOGGER = Logger.getMessageLogger(ServerLogger.class, "org.jboss.as.server.deployment");

    /**
     * Logger for private APIs.
     */
    ServerLogger PRIVATE_DEP_LOGGER = Logger.getMessageLogger(ServerLogger.class, "org.jboss.as.dependency.private");
    /**
     * Logger for unsupported APIs.
     */
    ServerLogger UNSUPPORTED_DEP_LOGGER = Logger.getMessageLogger(ServerLogger.class, "org.jboss.as.dependency.unsupported");

    /**
     * A logger with the category {@code org.jboss.as.server.moduleservice}.
     */
    ServerLogger MODULE_SERVICE_LOGGER = Logger.getMessageLogger(ServerLogger.class, "org.jboss.as.server.moduleservice");

    /**
     * A logger with the category {@code org.jboss.as.server.net}.
     */
    ServerLogger NETWORK_LOGGER = Logger.getMessageLogger(ServerLogger.class, "org.jboss.as.server.net");

    /**
     * Log message for when a jboss-deployment-structure.xml file is ignored
     * @param file name of the ignored file
     */
    @LogMessage(level = WARN)
    @Message(id = 1, value = "%s in subdeployment ignored. jboss-deployment-structure.xml is only parsed for top level deployments.")
    void jbossDeploymentStructureIgnored(String file);

    /**
     * Message for when a pre-computed annotation index cannot be loaded
     * @param index name of the annotation index
     */
    @LogMessage(level = ERROR)
    @Message(id = 2, value = "Could not read provided index: %s")
    void cannotLoadAnnotationIndex(String index);

    @LogMessage(level = WARN)
    @Message(id = 3, value = "Could not index class %s at %s")
    void cannotIndexClass(String className, String resourceRoot, @Cause Throwable throwable);

    // @LogMessage(level = WARN)
    // @Message(id = 4, value = "More than two unique criteria addresses were seen: %s")
    // void moreThanTwoUniqueCriteria(String addresses);

    // @LogMessage(level = WARN)
    // @Message(id = 5, value = "Checking two unique criteria addresses were seen: %s")
    // void checkingTwoUniqueCriteria(String addresses);

    // @LogMessage(level = WARN)
    // @Message(id = 6, value = "Two unique criteria addresses were seen: %s")
    // void twoUniqueCriteriaAddresses(String addresses);

    @LogMessage(level = ERROR)
    @Message(id = 7, value = "Undeploy of deployment \"%s\" was rolled back with the following failure message: %s")
    void undeploymentRolledBack(String deployment, String message);

    @LogMessage(level = ERROR)
    @Message(id = 8, value = "Undeploy of deployment \"%s\" was rolled back with no failure message")
    void undeploymentRolledBackWithNoMessage(String deployment);

    @LogMessage(level = INFO)
    @Message(id = 9, value = "Undeployed \"%s\" (runtime-name: \"%s\")")
    void deploymentUndeployed(String managementName, String deploymentUnitName);

    @LogMessage(level = INFO)
    @Message(id = 10, value = "Deployed \"%s\" (runtime-name : \"%s\")")
    void deploymentDeployed(String managementName, String deploymentUnitName);

    @LogMessage(level = ERROR)
    @Message(id = 11, value = "Redeploy of deployment \"%s\" was rolled back with the following failure message: %s")
    void redeployRolledBack(String deployment, String message);

    @LogMessage(level = ERROR)
    @Message(id = 12, value = "Redeploy of deployment \"%s\" was rolled back with no failure message")
    void redeployRolledBackWithNoMessage(String deployment);

    @LogMessage(level = INFO)
    @Message(id = 13, value = "Redeployed \"%s\"")
    void deploymentRedeployed(String deploymentName);

    @LogMessage(level = ERROR)
    @Message(id = 14, value = "Replacement of deployment \"%s\" by deployment \"%s\" was rolled back with the following failure message: %s")
    void replaceRolledBack(String replaced, String deployment, String message);

    @LogMessage(level = ERROR)
    @Message(id = 15, value = "Replacement of deployment \"%s\" by deployment \"%s\" was rolled back with no failure message")
    void replaceRolledBackWithNoMessage(String replaced, String deployment);

    @LogMessage(level = INFO)
    @Message(id = 16, value = "Replaced deployment \"%s\" with deployment \"%s\"")
    void deploymentReplaced(String replaced, String deployment);

    @LogMessage(level = WARN)
    @Message(id = 17, value = "Annotations import option %s specified in jboss-deployment-structure.xml for additional module %s has been ignored. Additional modules cannot import annotations.")
    void annotationImportIgnored(ModuleIdentifier annotationModuleId, ModuleIdentifier additionalModuleId);

    @LogMessage(level = WARN)
    @Message(id = 18, value = "Deployment \"%s\" is using a private module (\"%s\") which may be changed or removed in future versions without notice.")
    void privateApiUsed(String deployment, ModuleIdentifier dependency);

    @LogMessage(level = WARN)
    @Message(id = 19, value = "Deployment \"%s\" is using an unsupported module (\"%s\") which may be changed or removed in future versions without notice.")
    void unsupportedApiUsed(String deployment, ModuleIdentifier dependency);

    @LogMessage(level = WARN)
    @Message(id = 20, value = "Exception occurred removing deployment content %s")
    void failedToRemoveDeploymentContent(@Cause Throwable cause, String hash);

    @LogMessage(level = ERROR)
    @Message(id = 21, value = "Deploy of deployment \"%s\" was rolled back with the following failure message: %s")
    void deploymentRolledBack(String deployment, String message);

    @LogMessage(level = ERROR)
    @Message(id = 22, value = "Deploy of deployment \"%s\" was rolled back with no failure message")
    void deploymentRolledBackWithNoMessage(String deployment);

    @LogMessage(level = WARN)
    @Message(id = 23, value = "Failed to parse property (%s), value (%s) as an integer")
    void failedToParseCommandLineInteger(String property, String value);

    @LogMessage(level = ERROR)
    @Message(id = 24, value = "Cannot add module '%s' as URLStreamHandlerFactory provider")
    void cannotAddURLStreamHandlerFactory(@Cause Exception cause, String moduleID);

    @LogMessage(level = INFO)
    @Message(id = 25, value = "%s started in %dms - Started %d of %d services (%d services are lazy, passive or on-demand)")
    void startedClean(String prettyVersionString, long time, int startedServices, int allServices, int passiveOnDemandServices);

    @LogMessage(level = ERROR)
    @Message(id = 26, value = "%s started (with errors) in %dms - Started %d of %d services (%d services failed or missing dependencies, %d services are lazy, passive or on-demand)")
    void startedWitErrors(String prettyVersionString, long time, int startedServices, int allServices, int problemServices, int passiveOnDemandServices);

    @LogMessage(level = INFO)
    @Message(id = 27, value = "Starting deployment of \"%s\" (runtime-name: \"%s\")")
    void startingDeployment(String managementName, String deploymentUnitName);

    @LogMessage(level = INFO)
    @Message(id = 28, value = "Stopped deployment %s (runtime-name: %s) in %dms")
    void stoppedDeployment(String managementName, String deploymentUnitName, int elapsedTime);

    // @LogMessage(level = INFO)
    // @Message(id = 29, value = "Deployment '%s' started successfully")
    // void deploymentStarted(String deployment);

    // @LogMessage(level = ERROR)
    // @Message(id = 30, value = "Deployment '%s' has failed services%n    Failed services: %s")
    // void deploymentHasFailedServices(String deployment, String failures);

    // @LogMessage(level = ERROR)
    // @Message(id = 31, value = "Deployment '%s' has services missing dependencies%n    Missing dependencies: %s")
    // void deploymentHasMissingDependencies(String deployment, String missing);

    // @LogMessage(level = ERROR)
    // @Message(id = 32, value = "Deployment '%s' has failed services and services missing dependencies%n    Failed services: %s%n    Missing dependencies: %s")
    // void deploymentHasMissingAndFailedServices(String deployment, String failures, String missing);

    @LogMessage(level = ERROR)
    @Message(id = 33, value = "%s caught exception attempting to revert operation %s at address %s")
    void caughtExceptionRevertingOperation(@Cause Exception cause, String handler, String operation, PathAddress address);

    @LogMessage(level = WARN)
    @Message(id = 34, value = "No security realm defined for native management service; all access will be unrestricted.")
    void nativeManagementInterfaceIsUnsecured();

    @LogMessage(level = WARN)
    @Message(id = 35, value = "No security realm defined for http management service; all access will be unrestricted.")
    void httpManagementInterfaceIsUnsecured();

    @LogMessage(level = INFO)
    @Message(id = 36, value = "Creating http management service using network interface (%s) and port (%d)")
    void creatingHttpManagementServiceOnPort(String interfaceName, int port);

    @LogMessage(level = INFO)
    @Message(id = 37, value = "Creating http management service using network interface (%s) and secure port (%d)")
    void creatingHttpManagementServiceOnSecurePort(String interfaceName, int securePort);

    @LogMessage(level = INFO)
    @Message(id = 38, value = "Creating http management service using network interface (%s), port (%d) and secure port (%d)")
    void creatingHttpManagementServiceOnPortAndSecurePort(String interfaceName, int port, int securePort);

    @LogMessage(level = INFO)
    @Message(id = 39, value = "Creating http management service using socket-binding (%s)")
    void creatingHttpManagementServiceOnSocket(String socketName);

    @LogMessage(level = INFO)
    @Message(id = 40, value = "Creating http management service using secure-socket-binding (%s)")
    void creatingHttpManagementServiceOnSecureSocket(String secureSocketName);

    @LogMessage(level = INFO)
    @Message(id = 41, value = "Creating http management service using socket-binding (%s) and secure-socket-binding (%s)")
    void creatingHttpManagementServiceOnSocketAndSecureSocket(String socketName, String secureSocketName);

    @LogMessage(level = WARN)
    @Message(id = 42, value = "Caught exception closing input stream for uploaded deployment content")
    void caughtExceptionClosingContentInputStream(@Cause Exception cause);

    @LogMessage(level = ERROR)
    @Message(id = 43, value = "Deployment unit processor %s unexpectedly threw an exception during undeploy phase %s of %s")
    void caughtExceptionUndeploying(@Cause Throwable cause, DeploymentUnitProcessor dup, Phase phase, DeploymentUnit unit);

    @LogMessage(level = ERROR)
    @Message(id = 44, value = "Module %s will not have it's annotations processed as no %s file was found in the deployment. Please generate this file using the Jandex ant task.")
    void noCompositeIndex(ModuleIdentifier identifier, String indexLocation);

    @LogMessage(level = WARN)
    @Message(id = 45, value = "Extension %s is missing the required manifest attribute %s-%s (skipping extension)")
    void extensionMissingManifestAttribute(String item, String again, Attributes.Name suffix);

    @LogMessage(level = WARN)
    @Message(id = 46, value = "Extension %s URI syntax is invalid: %s")
    void invalidExtensionURI(String item, URISyntaxException e);

    @LogMessage(level = WARN)
    @Message(id = 47, value = "Could not find Extension-List entry %s referenced from %s")
    void cannotFindExtensionListEntry(ExtensionListEntry entry, ResourceRoot resourceRoot);

    @LogMessage(level = WARN)
    @Message(id = 48, value = "A server name configuration was provided both via system property %s ('%s') and via the xml configuration ('%s'). The xml configuration value will be used.")
    void duplicateServerNameConfiguration(String systemProperty, String rawServerProp, String processName);

    /**
     * Logs an informational message indicating the server is starting.
     *
     * @param prettyVersion  the server version.
     */
    @LogMessage(level = INFO)
    @Message(id = 49, value = "%s starting")
    void serverStarting(String prettyVersion);

    /**
     * Logs an informational message indicating the server is stopped.
     *
     * @param prettyVersion  the server version.
     * @param time     the time it took to stop.
     */
    @LogMessage(level = INFO)
    @Message(id = 50, value = "%s stopped in %dms")
    void serverStopped(String prettyVersion, int time);

    @LogMessage(level = INFO)
    @Message(id = 51, value= "Admin console listening on http://%s:%d")
    void logHttpConsole(String httpAddr, int httpPort);

    @LogMessage(level = INFO)
    @Message(id = 52, value= "Admin console listening on https://%s:%d")
    void logHttpsConsole(String httpsAddr, int httpsPort);

    @LogMessage(level = INFO)
    @Message(id = 53, value= "Admin console listening on http://%s:%d and https://%s:%d")
    void logHttpAndHttpsConsole(String httpAddr, int httpPort, String httpsAddr, int httpsPort);

    @LogMessage(level = INFO)
    @Message(id = 54, value= "Admin console is not enabled")
    void logNoConsole();

    @LogMessage(level = Logger.Level.ERROR)
    @Message(id = 55, value = "Caught exception during boot")
    void caughtExceptionDuringBoot(@Cause Exception e);


    @LogMessage(level = Logger.Level.FATAL)
    @Message(id = 56, value = "Server boot has failed in an unrecoverable manner; exiting. See previous messages for details.")
    void unsuccessfulBoot();

    /**
     * Logs an error message indicating the content for a configured deployment was unavailable at boot but boot
     * was allowed to proceed because the HC is in admin-only mode.
     *
     * @param contentHash    the content hash that could not be found.
     * @param deploymentName the deployment name.
     */
    @LogMessage(level = Logger.Level.ERROR)
    @Message(id = 57, value = "No deployment content with hash %s is available in the deployment content repository for deployment %s. Because this Host Controller is booting in ADMIN-ONLY mode, boot will be allowed to proceed to provide administrators an opportunity to correct this problem. If this Host Controller were not in ADMIN-ONLY mode this would be a fatal boot failure.")
    void reportAdminOnlyMissingDeploymentContent(String contentHash, String deploymentName);

    @LogMessage(level = Logger.Level.WARN)
    @Message(id = 58, value = "Additional resource root %s added via jboss-deployment-structure.xml does not exist")
    void additionalResourceRootDoesNotExist(String resourceRoot);

    @LogMessage(level = Logger.Level.WARN)
    @Message(id = 59, value = "Class Path entry %s in %s  does not point to a valid jar for a Class-Path reference.")
    void classPathEntryNotValid(String classPathEntry, String resourceRoot);

    @LogMessage(level = INFO)
    @Message(id = 60, value= "Http management interface listening on http://%s:%d/management")
    void logHttpManagement(String httpAddr, int httpPort);

    @LogMessage(level = INFO)
    @Message(id = 61, value= "Http management interface listening on https://%s:%d/management")
    void logHttpsManagement(String httpsAddr, int httpsPort);

    @LogMessage(level = INFO)
    @Message(id = 62, value= "Http management interface listening on http://%s:%d/management and https://%s:%d/management")
    void logHttpAndHttpsManagement(String httpAddr, int httpPort, String httpsAddr, int httpsPort);

    @LogMessage(level = INFO)
    @Message(id = 63, value= "Http management interface is not enabled")
    void logNoHttpManagement();

    @LogMessage(level = WARN)
    @Message(id = 64, value = "urn:jboss:deployment-structure namespace found in jboss.xml for a sub deployment %s. This is only valid in a top level deployment.")
    void jbossDeploymentStructureNamespaceIgnored(String deploymentUnitName);

    @LogMessage(level = Logger.Level.WARN)
    @Message(id = 65, value = "Failed to unmount deployment overlay")
    void failedToUnmountContentOverride(@Cause Throwable cause);

    // @LogMessage(level = WARN)
    // @Message(id = 66, value= "Cannot install reflection index for unresolved bundle: %s")
    // void warnCannotInstallReflectionIndexForUnresolvedBundle(String bundle);

    @LogMessage(level = WARN)
    @Message(id = 67, value= "jboss-deployment-dependencies cannot be used in a sub deployment, it must be specified at ear level: %s")
    void deploymentDependenciesAreATopLevelElement(String name);

    @LogMessage(level = Logger.Level.ERROR)
    @Message(id = 68, value = "No deployment overlay content with hash %s is available in the deployment content repository for deployment %s at location %s. Because this Host Controller is booting in ADMIN-ONLY mode, boot will be allowed to proceed to provide administrators an opportunity to correct this problem. If this Host Controller were not in ADMIN-ONLY mode this would be a fatal boot failure.")
    void reportAdminOnlyMissingDeploymentOverlayContent(String contentHash, String deploymentName, String contentName);

    @LogMessage(level = Logger.Level.INFO)
    @Message(id = 69, value = "Defer %s for %s making it %s")
    void infoDeferDeploymentPhase(Phase phase, String deploymentName, Mode mode);

    @LogMessage(level = Logger.Level.INFO)
    @Message(id = 70, value = "Deployment restart detected for deployment %s, performing full redeploy instead.")
    void deploymentRestartDetected(String deployment);

    @LogMessage(level = WARN)
    @Message(id = 71, value = "The operating system has limited the number of open files to %d for this process; a value of at least 4096 is recommended")
    void fdTooLow(long fdCount);

    /**
     * Instructions for the usage of standalone.sh.
     *
     * @return the message.
     */
    // @Message(id = Message.NONE, value = "Usage: ./standalone.sh [args...]%nwhere args include:")
    // String argUsage();

    /**
     * Instructions for the {@link org.jboss.as.process.CommandLineConstants#SERVER_CONFIG} command line argument.
     *
     * @return the message.
     */
    @Message(id = Message.NONE, value = "Name of the server configuration file to use (default is \"standalone.xml\") (Same as -c)")
    String argServerConfig();

    /**
     * Instructions for the {@link org.jboss.as.process.CommandLineConstants#SHORT_SERVER_CONFIG} command line argument.
     *
     * @return the message.
     */
    @Message(id = Message.NONE, value = "Name of the server configuration file to use (default is \"standalone.xml\") (Same as --server-config)")
    String argShortServerConfig();

    /**
     * Instructions for the {@link org.jboss.as.process.CommandLineConstants#READ_ONLY_SERVER_CONFIG} command line arguments.
     *
     * @return the message.
     */
    @Message(id = Message.NONE, value = "Name of the server configuration file to use. This differs from '" + CommandLineConstants.SERVER_CONFIG +
            "' and '" + CommandLineConstants.SHORT_SERVER_CONFIG + "' in that the original file is never overwritten.")
    String argReadOnlyServerConfig();


    /**
     * Instructions for the {@link CommandLineConstants#SHORT_HELP} or {@link CommandLineConstants#HELP} command line argument.
     *
     * @return the message.
     */
    @Message(id = Message.NONE, value = "Display this message and exit")
    String argHelp();

    /**
     * Instructions for the {@link CommandLineConstants#SHORT_PROPERTIES} or {@link CommandLineConstants#PROPERTIES} command line argument.
     *
     * @return the message.
     */
    @Message(id = Message.NONE, value = "Load system properties from the given url")
    String argProperties();

    /**
     * Instructions for the {@link CommandLineConstants#SECURITY_PROP} command line argument.
     *
     * @return the message.
     */
    @Message(id = Message.NONE, value = "Set a security property")
    String argSecurityProperty();

    /**
     * Instructions for the {@link CommandLineConstants#SYS_PROP} command line argument.
     *
     * @return the message.
     */
    @Message(id = Message.NONE, value = "Set a system property")
    String argSystem();

    /**
     * Instructions for the {@link CommandLineConstants#SHORT_VERSION}, {@link
     * CommandLineConstants#OLD_SHORT_VERSION or {@link CommandLineConstants#VERSION}
     * command line argument.
     *
     * @return the message.
     */
    @Message(id = Message.NONE, value = "Print version and exit")
    String argVersion();

    /**
     * Instructions for the {@link CommandLineConstants#PUBLIC_BIND_ADDRESS} command line argument.
     *
     * @return the message.
     */
    @Message(id = Message.NONE, value = "Set system property jboss.bind.address to the given value")
    String argPublicBindAddress();

    /**
     * Instructions for the {code -b<interface></interface>} command line
     * argument.
     *
     * @return the message.
     */
    @Message(id = Message.NONE, value = "Set system property jboss.bind.address.<interface> to the given value")
    String argInterfaceBindAddress();

    /**
     * Instructions for the {@link CommandLineConstants#DEFAULT_MULTICAST_ADDRESS} command line
     * argument.
     *
     * @return the message.
     */
    @Message(id = Message.NONE, value = "Set system property jboss.default.multicast.address to the given value")
    String argDefaultMulticastAddress();

    /**
     * Instructions for the {@link CommandLineConstants#ADMIN_ONLY} command line argument.
     *
     * @return the message.
     */
    @Message(id = Message.NONE, value = "Set the server's running type to ADMIN_ONLY causing it to open administrative interfaces and accept management requests but not start other runtime services or accept end user requests.")
    String argAdminOnly();

    /**
     * Instructions for the {@link CommandLineConstants#DEBUG} command line argument.
     *
     * @return the message.
     */
    @Message(id = Message.NONE, value = "Activate debug mode with an optional argument to specify the port. Only works if the launch script supports it.")
    String argDebugPort();

    /**
     * Creates an error message indicating a value was expected for the given command line option.
     *
     * @param option the name of the command line option
     *
     * @return a message that can by output to stderr.
     */
    @Message(id = 72, value = "Value expected for option %s")
    String valueExpectedForCommandLineOption(String option);

    /**
     * Creates an error message indicating an invalid command line option was presented.
     *
     * @param option the name of the command line option
     *
     * @return a message that can by output to stderr.
     */
    @Message(id = 73, value = "Invalid option '%s'")
    String invalidCommandLineOption(String option);

    /**
     * Creates an error message indicating a malformed URL was provided as a value for a command line option.
     *
     * @param urlSpec the provided url
     * @param option the name of the command line option
     *
     * @return a message that can by output to stderr.
     */
    @Message(id = 74, value = "Malformed URL '%s' provided for option '%s'")
    String malformedCommandLineURL(String urlSpec, String option);

    /**
     * Creates an error message indicating {@link java.util.Properties#load(java.io.InputStream) properties could not be loaded}
     * from a given url.
     *
     * @param url the provided url
     *
     * @return a message that can by output to stderr.
     */
    @Message(id = 75, value = "Unable to load properties from URL '%s'")
    String unableToLoadProperties(URL url);

    /**
     * Creates an error message indicating creating a security vault failed.
     *
     * @param cause the problem
     * @param msg the problem (for use in the message)
     *
     * @return a RuntimeException wrapper
     */
    @Message(id = 76, value = "Error initializing vault --  %s")
    RuntimeException cannotCreateVault(@Param VaultReaderException cause, VaultReaderException msg);

    /**
     * Creates an error message indicating that connecting to the HC failed.
     *
     * @param e the problem
     * @return a StartException
     */
    // @Message(id = 77, value = "Failed to connect to the host-controller")
    // StartException failedToConnectToHC(@Param Exception e);

    /**
     * Creates an error message indicating that the operation connecting to the
     * HC got cancelled before it could complete.
     *
     * @return a StartException
     */
    // @Message(id = 78, value = "Connection request to the host-controller was cancelled")
    // StartException cancelledHCConnect();

    @Message(id = 79, value = "hostControllerName must be null if the server is not in a managed domain")
    IllegalArgumentException hostControllerNameNonNullInStandalone();

    @Message(id = 80, value = "hostControllerName may not be null if the server is in a managed domain")
    IllegalArgumentException hostControllerNameNullInDomain();

    @Message(id = 81, value = "An IP address cannot be resolved using the given interface selection criteria. Failure was -- %s")
    OperationFailedException cannotResolveInterface(Exception msg, @Param Exception cause);

    @Message(id = 82, value = "failed to resolve interface %s")
    StartException failedToResolveInterface(String name);

    @Message(id = 83, value = "Failed to start the http-interface service")
    StartException failedToStartHttpManagementService(@Param Exception e);

    @Message(id = 84, value = "No deployment content with hash %s is available in the deployment content repository.")
    OperationFailedException noSuchDeploymentContent(String hash);

    @Message(id = 85, value = "No deployment with name %s found")
    OperationFailedException noSuchDeployment(String deploymentName);

    @Message(id = 86, value = "Cannot use %s with the same value for parameters %s and %s. " +
            "Use %s to redeploy the same content or %s to replace content with a new version with the same name.")
    OperationFailedException cannotReplaceDeployment(String replaceOperation, String name, String toReplace, String redeployOperation, String fullReplace);

    @Message(id = 87, value = "Deployment %s is already started")
    OperationFailedException deploymentAlreadyStarted(String deploymentName);

    @Message(id = 88, value = "Missing configuration value for: %s")
    IllegalStateException missingHomeDirConfiguration(String propertyName);

    // @Message(id = 89, value = "%n        %s is missing: %s")
    // String missingDependencies(ServiceName dependentService, String missingDependencies);

    @Message(id = 90, value = "%s is required")
    OperationFailedException attributeIsRequired(String attribute);

    @Message(id = 91, value = "%s is not allowed when %s are present")
    OperationFailedException attributeNotAllowedWhenAlternativeIsPresent(String attribute, List<String> alternatives);

    @Message(id = 92, value = "%s is invalid")
    OperationFailedException attributeIsInvalid(String attribute);

    @Message(id = 93, value = "Caught IOException reading uploaded deployment content")
    OperationFailedException caughtIOExceptionUploadingContent(@Cause IOException cause);

    @Message(id = 94, value = "Null stream at index [%d]")
    OperationFailedException nullStreamAttachment(int index);

    @Message(id = 95, value = "'%s' is not a valid URL")
    OperationFailedException invalidDeploymentURL(String urlSpec, @Cause MalformedURLException e);

    @Message(id = 96, value = "Error obtaining input stream from URL '%s'")
    OperationFailedException problemOpeningStreamFromDeploymentURL(String urlSpec, @Cause IOException e);

    @Message(id = 97, value = "ServiceModuleLoader already started")
    IllegalStateException serviceModuleLoaderAlreadyStarted();

    @Message(id = 98, value = "ServiceModuleLoader already stopped")
    IllegalStateException serviceModuleLoaderAlreadyStopped();

    @Message(id = 99, value = "'%s' cannot be loaded from a ServiceModuleLoader as its name does not start with '%s'")
    IllegalArgumentException missingModulePrefix(ModuleIdentifier identifier, String prefix);

    @Message(id = 100, value = "Failed to read '%s'")
    DeploymentUnitProcessingException failedToReadVirtualFile(VirtualFile file, @Cause IOException cause);

    @Message(id = 101, value = "Deployment root is required")
    IllegalArgumentException deploymentRootRequired();

    @Message(id = 102, value = "Sub-deployments require a parent deployment unit")
    IllegalArgumentException subdeploymentsRequireParent();

    @Message(id = 103, value = "No Module Identifier attached to deployment '%s'")
    DeploymentUnitProcessingException noModuleIdentifier(String deploymentUnitName);

    @Message(id = 104, value = "Failed to create VFSResourceLoader for root [%s]")
    DeploymentUnitProcessingException failedToCreateVFSResourceLoader(String resourceRoot, @Cause IOException cause);

    @Message(id = 105, value = "Failed to get file from remote repository")
    RuntimeException failedToGetFileFromRemoteRepository(@Cause Throwable cause);

    @Message(id = 106, value = "Unable to create local directory: %s")
    IOException cannotCreateLocalDirectory(File path);

    @Message(id = 107, value = "Did not read the entire file. Missing: %d")
    IOException didNotReadEntireFile(long missing);

    @Message(id = 108, value = "No value was provided for argument %s%n")
    String noArgValue(String argument);

    @Message(id = 109, value = "Could not find the file repository connection to the host controller.")
    IllegalStateException couldNotFindHcFileRepositoryConnection();

    // @Message(id = 110, value = "Only 'hash' is allowed for deployment addition for a domain mode server: %s")
    // IllegalStateException onlyHashAllowedForDeploymentAddInDomainServer(ModelNode contentItemNode);

    // @Message(id = 111, value = "Only 'hash' is allowed for deployment full replacement for a domain mode server: %s")
    // IllegalStateException onlyHashAllowedForDeploymentFullReplaceInDomainServer(ModelNode contentItemNode);

    @Message(id = 112, value = "Unknown mount type %s")
    IllegalArgumentException unknownMountType(MountType mountType);

    /**
     * Creates an exception indicating a failure to create a temp file provider.
     *
     * @return a {@link StartException} for the error.
     */
    @Message(id = 113, value = "Failed to create temp file provider")
    StartException failedCreatingTempProvider();

    @Message(id = 114, value = "%s cannot be defined when either %s or %s is also defined")
    OperationFailedException illegalCombinationOfHttpManagementInterfaceConfigurations(String interfaceAttr,
                                                                                       String socketBindingAttr,
                                                                                       String secureSocketBindingAttr);

    @Message(id = 115, value = "System property %s cannot be set via the xml configuration file or from a management client; " +
            "it's value must be known at initial process start so it can only set from the commmand line")
    OperationFailedException systemPropertyNotManageable(String propertyName);


    @Message(id = 116, value = "System property %s cannot be set after the server name has been set via the xml " +
            "configuration file or from a management client")
    OperationFailedException systemPropertyCannotOverrideServerName(String propertyName);

    @Message(id = 117, value = "Unable to initialise a basic SSLContext '%s'")
    IOException unableToInitialiseSSLContext(String message);

    @Message(id = 118, value = "Determined modules directory does not exist: %s")
    IllegalStateException modulesDirectoryDoesNotExist(File f);

    @Message(id = 119, value = "Home directory does not exist: %s")
    IllegalStateException homeDirectoryDoesNotExist(File f);

    /////////////////////////////////////
    // New range of ids

    @Message(id = 120, value = "Bundles directory does not exist: %s")
    IllegalStateException bundlesDirectoryDoesNotExist(File f);

    @Message(id = 121, value = "Configuration directory does not exist: %s")
    IllegalStateException configDirectoryDoesNotExist(File f);

    @Message(id = 122, value = "Server base directory does not exist: %s")
    IllegalStateException serverBaseDirectoryDoesNotExist(File f);

    @Message(id = 123, value = "Server data directory is not a directory: %s")
    IllegalStateException serverDataDirectoryIsNotDirectory(File file);

    @Message(id = 124, value = "Could not create server data directory: %s")
    IllegalStateException couldNotCreateServerDataDirectory(File file);

    @Message(id = 125, value = "Server content directory is not a directory: %s")
    IllegalStateException serverContentDirectoryIsNotDirectory(File file);

    @Message(id = 126, value = "Could not create server content directory: %s")
    IllegalStateException couldNotCreateServerContentDirectory(File file);

    @Message(id = 127, value = "Log directory is not a directory: %s")
    IllegalStateException logDirectoryIsNotADirectory(File f);

    @Message(id = 128, value = "Could not create log directory: %s")
    IllegalStateException couldNotCreateLogDirectory(File f);

    @Message(id = 129, value = "Server temp directory does not exist: %s")
    IllegalStateException serverTempDirectoryIsNotADirectory(File file);

    @Message(id = 130, value = "Could not create server temp directory: %s")
    IllegalStateException couldNotCreateServerTempDirectory(File file);

    @Message(id = 131, value = "Controller temp directory does not exist: %s")
    IllegalStateException controllerTempDirectoryIsNotADirectory(File file);

    @Message(id = 132, value = "Could not create server temp directory: %s")
    IllegalStateException couldNotCreateControllerTempDirectory(File file);

    @Message(id = 133, value = "Domain base dir does not exist: %s")
    IllegalStateException domainBaseDirDoesNotExist(File file);

    @Message(id = 134, value = "Domain config dir does not exist: %s")
    IllegalStateException domainConfigDirDoesNotExist(File file);

    @Message(id = 135, value = "Server base directory is not a directory: %s")
    IllegalStateException serverBaseDirectoryIsNotADirectory(File file);

    @Message(id = 136, value = "Could not create server base directory: %s")
    IllegalStateException couldNotCreateServerBaseDirectory(File file);

    @Message(id = 137, value = "No deployment content with hash %s is available in the deployment content repository for deployment '%s'. This is a fatal boot error. To correct the problem, either restart with the --admin-only switch set and use the CLI to install the missing content or remove it from the configuration, or remove the deployment from the xml configuration file and restart.")
    OperationFailedException noSuchDeploymentContentAtBoot(String contentHash, String deploymentName);

    /** Label for DEBUG log listing of the server's system properties */
    @Message(id = Message.NONE, value = "Configured system properties:")
    String configuredSystemPropertiesLabel();

    /** Label for DEBUG log listing of the server's VM arguments */
    @Message(id = Message.NONE, value = "VM Arguments: %s")
    String vmArgumentsLabel(String arguments);

    /** Label for DEBUG log listing of the server's system environme
     * nt properties */
    @Message(id = Message.NONE, value = "Configured system environment:")
    String configuredSystemEnvironmentLabel();

    @Message(id = 138, value = "VFS is not available from the configured module loader")
    IllegalStateException vfsNotAvailable();

    @Message(id = 139, value = "Server controller service was removed")
    ServiceNotFoundException serverControllerServiceRemoved();

    @Message(id = 140, value = "Root service was removed")
    IllegalStateException rootServiceRemoved();

    @Message(id = 141, value = "Cannot start server")
    IllegalStateException cannotStartServer(@Cause Exception e);

    @Message(id = 142, value = "Naming context has not been set")
    IllegalStateException namingContextHasNotBeenSet();

    @Message(id = 143, value = "No directory called '%s' exists under '%s'")
    IllegalArgumentException embeddedServerDirectoryNotFound(String relativePath, String homePath);

    @Message(id = 144, value = "-D%s=%s does not exist")
    IllegalArgumentException propertySpecifiedFileDoesNotExist(String property, String path);

    @Message(id = 145, value = "-D%s=%s is not a directory")
    IllegalArgumentException propertySpecifiedFileIsNotADirectory(String property, String path);

    @Message(id = 146, value = "Error copying '%s' to '%s'")
    RuntimeException errorCopyingFile(String src, String dest, @Cause IOException e);

    @Message(id = 147, value = "%s is null")
    InvalidObjectException invalidObject(String field);

    @Message(id = 148, value = "portOffset is out of range")
    InvalidObjectException invalidPortOffset();

    @Message(id = 149, value = "Invalid '%s' value: %d, the maximum index is %d")
    OperationFailedException invalidStreamIndex(String name, int value, int maxIndex);

    @Message(id = 150, value = "Cannot create input stream from URL '%s'")
    OperationFailedException invalidStreamURL(@Cause Exception cause, String url);

    @Message(id = 151, value = "No bytes available at param %s")
    OperationFailedException invalidStreamBytes(String param);

    @Message(id = 152, value = "Only 1 piece of content is current supported (AS7-431)")
    OperationFailedException multipleContentItemsNotSupported();

    @Message(id = 153, value = "Failed to process phase %s of %s")
    StartException deploymentPhaseFailed(Phase phase, DeploymentUnit deploymentUnit, @Cause Throwable cause);

    @Message(id = 154, value = "Null initial deployment unit")
    IllegalArgumentException nullInitialDeploymentUnit();

    @Message(id = 155, value = "Attachment key is null")
    IllegalArgumentException nullAttachmentKey();

    @Message(id = 156, value = "Failed to index deployment root for annotations")
    DeploymentUnitProcessingException deploymentIndexingFailed(@Cause Throwable cause);

    @Message(id = 157, value = "No Seam Integration jar present: %s")
    DeploymentUnitProcessingException noSeamIntegrationJarPresent(Module module);

    @Message(id = 158, value = "Failed to instantiate a %s")
    DeploymentUnitProcessingException failedToInstantiateClassFileTransformer(String clazz, @Cause Exception cause);

    @Message(id = 159, value = "No deployment repository available.")
    DeploymentUnitProcessingException noDeploymentRepositoryAvailable();

    @Message(id = 160, value = "Failed to mount deployment content")
    DeploymentUnitProcessingException deploymentMountFailed(@Cause IOException cause);

    @Message(id = 161, value = "Failed to get manifest for deployment %s")
    DeploymentUnitProcessingException failedToGetManifest(VirtualFile file, @Cause IOException cause);

    // @Message(id = 162, value = "Invalid dependency: %s")
    // RuntimeException invalidDependency(String dependency);

    @Message(id = 163, value = "Cannot merge resource root for a different file. This: %s mergee: %s")
    IllegalArgumentException cannotMergeResourceRoot(VirtualFile file, VirtualFile mergee);

    @Message(id = 164, value = "Failed to create temp file provider")
    RuntimeException failedToCreateTempFileProvider(@Cause IOException cause);

    @Message(id = 165, value = "Resource is too large to be a valid class file")
    IOException resourceTooLarge();

    @Message(id = 166, value = "Sub deployment %s in jboss-deployment-structure.xml was not found. Available sub deployments: %s")
    DeploymentUnitProcessingException subdeploymentNotFound(String path, StringBuilder subdeployments);

    @Message(id = 167, value = "No jboss-deployment-structure.xml file found at %s")
    DeploymentUnitProcessingException deploymentStructureFileNotFound(File file);

    @Message(id = 168, value = "Error loading jboss-deployment-structure.xml from %s")
    DeploymentUnitProcessingException errorLoadingDeploymentStructureFile(String path, @Cause XMLStreamException cause);

    @Message(id = 169, value = "Sub deployment '%s' is listed twice in jboss-deployment-structure.xml")
    XMLStreamException duplicateSubdeploymentListing(String name);

    @Message(id = 170, value = "Additional module name '%s' is not valid. Names must start with 'deployment.'")
    XMLStreamException invalidModuleName(String name);

    @Message(id = 171, value = "External resource roots not supported, resource roots may not start with a '/' : %s")
    XMLStreamException externalResourceRootsNotSupported(String path);

    @Message(id = 172, value = "Unexpected end of document")
    XMLStreamException unexpectedEndOfDocument(@Param Location location);

    @Message(id = 173, value = "Missing one or more required attributes:%s")
    XMLStreamException missingRequiredAttributes(String missing, @Param Location location);

    @Message(id = 174, value = "Unexpected content of type '%s', name is '%s', text is: '%s'")
    XMLStreamException unexpectedContent(String kind, QName name, String text, @Param Location location);

    @Message(id = 175, value = "No method found with id: %s on class (or its super class) %s")
    DeploymentUnitProcessingException noMethodFound(MethodIdentifier method, Class<?> clazz);

    @Message(id = 176, value = "Method cannot be null")
    IllegalArgumentException nullMethod();

    @Message(id = 177, value = "Error getting reflective information for %s with ClassLoader %s")
    RuntimeException errorGettingReflectiveInformation(Class<?> clazz, ClassLoader cl, @Param Throwable cause);

    @Message(id = 178, value = "External Module Service already started")
    IllegalStateException externalModuleServiceAlreadyStarted();

    @Message(id = 179, value = "Failed to load module: %s")
    StartException failedToLoadModule(ModuleIdentifier module, @Cause ModuleLoadException cause);

    //@Message(id = 180, value = "Timeout waiting for module service: %s")
    //ModuleLoadException timeoutWaitingForModuleService(ModuleIdentifier module);

    @Message(id = 181, value = "%s cannot be defined when %s is also defined")
    OperationFailedException conflictingConfigs(String choice, String alternative);

    @Message(id = 182, value = "This operation is for internal use only")
    OperationFailedException internalUseOnly();

    @Message(id = 183, value = "Was not able to get root resource")
    RuntimeException cannotGetRootResource();

    // @Message(id = 184, value = "Failed to resolve expression: %s")
    // IllegalStateException failedToResolveExpression(String expression);

    // @Message(id = 185, value = "Unexpected char seen: %s")
    // IllegalStateException unexpectedChar(String c);

    // @Message(id = 186, value = "Incomplete expression: %s")
    // IllegalStateException incompleteExpression(String expression);

    @Message(id = 187, value = "Failed to get multicast address for %s")
    OperationFailedException failedToResolveMulticastAddress(@Cause UnknownHostException cause, String address);

    @Message(id = 188, value = "Failed to get multicast address for %s")
    RuntimeException failedToResolveMulticastAddressForRollback(@Cause UnknownHostException cause, String address);

    // @Message(id = 189, value = "Validation for %s is not implemented")
    // UnsupportedOperationException attributeValidationUnimplemented(String attribute);

    @Message(id = 190, value = "Cannot add more than one socket binding group. Add of '%s' attempted, but '%s' already exists")
    OperationFailedException cannotAddMoreThanOneSocketBindingGroupForServer(PathAddress wanted, PathAddress existing);

    @Message(id = 191, value = "Can't use both --server-config and --initial-server-config")
    IllegalArgumentException cannotHaveBothInitialServerConfigAndServerConfig();

    @Message(id = 192, value = "Duplicate namespace %s in jboss-all.xml")
    XMLStreamException duplicateJBossXmlNamespace(QName namespace, @Param Location location);

    @Message(id = 193, value = "Two different versions of the same namespaces are present in jboss-all.xml, %s and %s are both present")
    DeploymentUnitProcessingException equivalentNamespacesInJBossXml(QName key, QName s);

    @Message(id = 194, value = "Error loading jboss-all.xml from %s")
    DeploymentUnitProcessingException errorLoadingJBossXmlFile(String path, @Cause XMLStreamException e);

    @Message(id = 195, value = "Cannot obtain required module for: %s")
    IllegalStateException nullModuleAttachment(DeploymentUnit depUnit);

    @Message(id = 196, value = "Failed to get content for deployment overlay %s at %s")
    DeploymentUnitProcessingException deploymentOverlayFailed(@Cause Exception cause, String contentOverlay, String file);

    // @Message(id = 197, value = "Wildcard character * is only allowed at the beginning or end of the deployment name %s")
    // OperationFailedException wildcardOnlyAllowedAtStartOrEnd(String str);


    @Message(id = 198, value = "No deployment overlay content with hash %s is available in the deployment content repository for deployment overlay '%s' at location %s. This is a fatal boot error. To correct the problem, either restart with the --admin-only switch set and use the CLI to install the missing content or remove it from the configuration, or remove the deployment overlay from the xml configuration file and restart.")
    OperationFailedException noSuchDeploymentOverlayContentAtBoot(String contentHash, String deploymentOverlayName, String contentFile);

    @Message(id = 199, value = "No deployment overlay content with hash %s is available in the deployment content repository.")
    OperationFailedException noSuchDeploymentOverlayContent(String hash);

    @Message(id = 200, value = "Failed to read file %s")
    OperationFailedException failedToLoadFile(VirtualFile file, @Cause IOException e);

    @Message(id = 201, value = "Cannot have more than one of %s")
    OperationFailedException cannotHaveMoreThanOneManagedContentItem(Set<String> managedAttributes);

    @Message(id = 202, value = "Unknown content item key: %s")
    OperationFailedException unknownContentItemKey(String key);

    @Message(id = 203, value = "Cannot use %s when %s are used")
    OperationFailedException cannotMixUnmanagedAndManagedContentItems(Set<String> usedManaged, Set<String> usedUnmanaged);

    @Message(id = 204, value = "Null '%s'")
    OperationFailedException nullParameter(String name);

    @Message(id = 205, value = "There is already a deployment called %s with the same runtime name %s")
    OperationFailedException runtimeNameMustBeUnique(String existingDeployment, String runtimename);

    @Message(id = 206, value = "Multiple deployment unit processors registered with priority %s and class %s")
    IllegalStateException duplicateDeploymentUnitProcessor(int priority, Class aClass);

    @LogMessage(level = INFO)
    @Message(id = 207, value = "Starting subdeployment (runtime-name: \"%s\")")
    void startingSubDeployment(String deploymentUnitName);

    @LogMessage(level = INFO)
    @Message(id = 208, value = "Stopped subdeployment (runtime-name: %s) in %dms")
    void stoppedSubDeployment(String deploymentUnitName, int elapsedTime);

    @Message(id = 209, value = "When specifying a 'module' you also need to specify the 'code'")
    OperationFailedException vaultModuleWithNoCode();
}
