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

import static org.jboss.logging.Logger.Level.ERROR;
import static org.jboss.logging.Logger.Level.INFO;
import static org.jboss.logging.Logger.Level.WARN;

import java.net.URISyntaxException;
import java.util.jar.Attributes;

import org.jboss.as.controller.PathAddress;
import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.as.server.deployment.DeploymentUnitProcessor;
import org.jboss.as.server.deployment.Phase;
import org.jboss.as.server.deployment.module.ExtensionListEntry;
import org.jboss.as.server.deployment.module.ResourceRoot;
import org.jboss.logging.BasicLogger;
import org.jboss.logging.Cause;
import org.jboss.logging.LogMessage;
import org.jboss.logging.Logger;
import org.jboss.logging.Message;
import org.jboss.logging.MessageLogger;
import org.jboss.modules.ModuleIdentifier;

/**
 * This module is using message IDs in the range 15700-15999.
 * This file is using the subset 15850-15875 for server logger messages.
 * See http://community.jboss.org/docs/DOC-16810 for the full list of
 * currently reserved JBAS message id blocks.
 *
 * @author <a href="mailto:jperkins@redhat.com">James R. Perkins</a>
 * @author Mike M. Clark
 */
@MessageLogger(projectCode = "JBAS")
public interface ServerLogger extends BasicLogger {

    /**
     * A logger with a category of the package name.
     */
    ServerLogger ROOT_LOGGER = Logger.getMessageLogger(ServerLogger.class, ServerLogger.class.getPackage().getName());

    /**
     * A logger with the category {@code org.jboss.as}.
     */
    ServerLogger AS_ROOT_LOGGER = Logger.getMessageLogger(ServerLogger.class, "org.jboss.as");

    /**
     * A logger with the category {@code org.jboss.as.config}.
     */
    ServerLogger CONFIG_LOGGER = Logger.getMessageLogger(ServerLogger.class, "org.jboss.as.config");

    /**
     * A logger with the category {@code org.jboss.as.server.deployment}.
     */
    ServerLogger DEPLOYMENT_LOGGER = Logger.getMessageLogger(ServerLogger.class, "org.jboss.as.server.deployment");

    /**
     * A logger with the category {@code org.jboss.as.deployment.module}.
     */
    ServerLogger DEPLOYMENT_MODULE_LOGGER = Logger.getMessageLogger(ServerLogger.class, "org.jboss.as.deployment.module");

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
     * Logs an informational message indicating the server is starting.
     *
     * @param prettyVersion  the server version.
     */
    @LogMessage(level = INFO)
    @Message("%s starting")
    void serverStarting(String prettyVersion);

    /**
     * Logs an informational message indicating the server is stopped.
     *
     * @param prettyVersion  the server version.
     * @param time     the time it took to stop.
     */
    @LogMessage(level = INFO)
    @Message("%s stopped in %dms")
    void serverStopped(String prettyVersion, int time);

    /**
     * Log message for when a jboss-deployment-structure.xml file is ignored
     */
    @LogMessage(level = WARN)
    @Message(id = 15850, value = "%s in subdeployment ignored. jboss-deployment-structure.xml is only parsed for top level deployments.")
    void jbossDeploymentStructureIgnored(String file);

    /**
     * Message for when a pre-computed annotation index cannot be loaded
     */
    @LogMessage(level = ERROR)
    @Message(id = 15851, value = "Could not read provided index: %s")
    void cannotLoadAnnotationIndex(String index);

    @LogMessage(level = WARN)
    @Message(id = 15852, value = "Could not index class %s at %s")
    void cannotIndexClass(String className, String resourceRoot, @Cause Throwable throwable);

    @LogMessage(level = WARN)
    @Message(id = 15853, value = "More than two unique criteria addresses were seen: %s")
    void moreThanTwoUniqueCriteria(String addresses);

    @LogMessage(level = WARN)
    @Message(id = 15854, value = "Checking two unique criteria addresses were seen: %s")
    void checkingTwoUniqueCriteria(String addresses);

    @LogMessage(level = WARN)
    @Message(id = 15855, value = "Two unique criteria addresses were seen: %s")
    void twoUniqueCriteriaAddresses(String addresses);

    @LogMessage(level = INFO)
    @Message(id = 15856, value = "Undeploy of deployment \"%s\" was rolled back with failure message %s")
    void undeploymentRolledBack(String deployment, String message);

    @LogMessage(level = INFO)
    @Message(id = 15857, value = "Undeploy of deployment \"%s\" was rolled back with no failure message")
    void undeploymentRolledBackWithNoMessage(String deployment);

    @LogMessage(level = INFO)
    @Message(id = 18558, value = "Undeployed \"%s\"")
    void deploymentUndeployed(String deploymentName);

    @LogMessage(level = INFO)
    @Message(id = 18559, value = "Deployed \"%s\"")
    void deploymentDeployed(String deploymentUnitName);

    @LogMessage(level = INFO)
    @Message(id = 15860, value = "Redeploy of deployment \"%s\" was rolled back with failure message %s")
    void redeployRolledBack(String deployment, String message);

    @LogMessage(level = INFO)
    @Message(id = 15861, value = "Redeploy of deployment \"%s\" was rolled back with no failure message")
    void redeployRolledBackWithNoMessage(String deployment);

    @LogMessage(level = INFO)
    @Message(id = 18562, value = "Redeployed \"%s\"")
    void deploymentRedeployed(String deploymentName);

    @LogMessage(level = INFO)
    @Message(id = 15863, value = "Replacement of deployment \"%s\" by deployment \"%s\" was rolled back with failure message %s")
    void replaceRolledBack(String replaced, String deployment, String message);

    @LogMessage(level = INFO)
    @Message(id = 15864, value = "Replacement of deployment \"%s\" by deployment \"%s\" was rolled back with no failure message")
    void replaceRolledBackWithNoMessage(String replaced, String deployment);

    @LogMessage(level = INFO)
    @Message(id = 18565, value = "Replaced deployment \"%s\" with deployment \"%s\"")
    void deploymentReplaced(String replaced, String deployment);

    @LogMessage(level = WARN)
    @Message(id = 18566, value = "Annotations import option %s specified in jboss-deployment-structure.xml for additional module %s has been ignored. Additional modules cannot import annotations.")
    void annotationImportIgnored(ModuleIdentifier annotationModuleId, ModuleIdentifier additionalModuleId);

    @LogMessage(level = WARN)
    @Message(id = 18567, value = "Deployment \"%s\" is using a private module (\"%s\") which may be changed or removed in future versions without notice.")
    void privateApiUsed(String deployment, ModuleIdentifier dependency);

    @LogMessage(level = WARN)
    @Message(id = 18568, value = "Deployment \"%s\" is using an unsupported module (\"%s\") which may be changed or removed in future versions without notice.")
    void unsupportedApiUsed(String deployment, ModuleIdentifier dependency);

    @LogMessage(level = WARN)
    @Message(id = 18569, value = "Exception occurred removing deployment content %s")
    void failedToRemoveDeploymentContent(@Cause Throwable cause, String hash);

    @LogMessage(level = INFO)
    @Message(id = 15870, value = "Deploy of deployment \"%s\" was rolled back with failure message %s")
    void deploymentRolledBack(String deployment, String message);

    @LogMessage(level = INFO)
    @Message(id = 15871, value = "Deploy of deployment \"%s\" was rolled back with no failure message")
    void deploymentRolledBackWithNoMessage(String deployment);

    @LogMessage(level = WARN)
    @Message(id = 15872, value = "Failed to parse property (%s), value (%s) as an integer")
    void failedToParseCommandLineInteger(String property, String value);

    @LogMessage(level = ERROR)
    @Message(id = 15873, value = "Cannot add module '%s' as URLStreamHandlerFactory provider")
    void cannotAddURLStreamHandlerFactory(@Cause Exception cause, String moduleID);

    @LogMessage(level = INFO)
    @Message(id = 15874, value = "%s started in %dms - Started %d of %d services (%d services are passive or on-demand)")
    void startedClean(String prettyVersionString, long time, int startedServices, int allServices, int passiveOnDemandServices);

    @LogMessage(level = ERROR)
    @Message(id = 15875, value = "%s started (with errors) in %dms - Started %d of %d services (%d services failed or missing dependencies, %d services are passive or on-demand)")
    void startedWitErrors(String prettyVersionString, long time, int startedServices, int allServices, int problemServices, int passiveOnDemandServices);

    @LogMessage(level = INFO)
    @Message(id = 15876, value = "Starting deployment of \"%s\"")
    void startingDeployment(String deployment);

    @LogMessage(level = INFO)
    @Message(id = 15877, value = "Stopped deployment %s in %dms")
    void stoppedDeployment(String deployment, int elapsedTime);

    @LogMessage(level = INFO)
    @Message(id = 15878, value = "Deployment '%s' started successfully")
    void deploymentStarted(String deployment);

    @LogMessage(level = ERROR)
    @Message(id = 15879, value = "Deployment '%s' has failed services\n    Failed services: %s")
    void deploymentHasFailedServices(String deployment, String failures);

    @LogMessage(level = ERROR)
    @Message(id = 15880, value = "Deployment '%s' has services missing dependencies\n    Missing dependencies: %s")
    void deploymentHasMissingDependencies(String deployment, String missing);

    @LogMessage(level = ERROR)
    @Message(id = 15881, value = "Deployment '%s' has failed services and services missing dependencies\n    Failed services: %s\n    Missing dependencies: %s")
    void deploymentHasMissingAndFailedServices(String deployment, String failures, String missing);

    @LogMessage(level = ERROR)
    @Message(id = 15882, value = "%s caught exception attempting to revert operation %s at address %s")
    void caughtExceptionRevertingOperation(@Cause Exception cause, String handler, String operation, PathAddress address);

    @LogMessage(level = WARN)
    @Message(id = 15883, value = "No security realm defined for native management service; all access will be unrestricted.")
    void nativeManagementInterfaceIsUnsecured();

    @LogMessage(level = WARN)
    @Message(id = 15884, value = "No security realm defined for http management service; all access will be unrestricted.")
    void httpManagementInterfaceIsUnsecured();

    @LogMessage(level = INFO)
    @Message(id = 15885, value = "Creating http management service using network interface (%s) and port (%d)")
    void creatingHttpManagementServiceOnPort(String interfaceName, int port);

    @LogMessage(level = INFO)
    @Message(id = 15886, value = "Creating http management service using network interface (%s) and secure port (%d)")
    void creatingHttpManagementServiceOnSecurePort(String interfaceName, int securePort);

    @LogMessage(level = INFO)
    @Message(id = 15887, value = "Creating http management service using network interface (%s), port (%d) and secure port (%d)")
    void creatingHttpManagementServiceOnPortAndSecurePort(String interfaceName, int port, int securePort);

    @LogMessage(level = INFO)
    @Message(id = 15888, value = "Creating http management service using socket-binding (%s)")
    void creatingHttpManagementServiceOnSocket(String socketName);

    @LogMessage(level = INFO)
    @Message(id = 15889, value = "Creating http management service using secure-socket-binding (%s)")
    void creatingHttpManagementServiceOnSecureSocket(String secureSocketName);

    @LogMessage(level = INFO)
    @Message(id = 15890, value = "Creating http management service using socket-binding (%s) and secure-socket-binding (%s)")
    void creatingHttpManagementServiceOnSocketAndSecureSocket(String socketName, String secureSocketName);

    @LogMessage(level = WARN)
    @Message(id = 15891, value = "Caught exception closing input stream for uploaded deployment content")
    void caughtExceptionClosingContentInputStream(@Cause Exception cause);

    @LogMessage(level = ERROR)
    @Message(id = 15892, value = "Deployment unit processor %s unexpectedly threw an exception during undeploy phase %s of %s")
    void caughtExceptionUndeploying(@Cause Throwable cause, DeploymentUnitProcessor dup, Phase phase, DeploymentUnit unit);

    @LogMessage(level = WARN)
    @Message(id = 15893, value = "Encountered invalid class name '%s' for service type '%s'")
    void invalidServiceClassName(String className, String type);

    @LogMessage(level = ERROR)
    @Message(id = 15894, value = "Module %s will not have it's annotations processed as no %s file was found in the deployment. Please generate this file using the Jandex ant task.")
    void noCompositeIndex(ModuleIdentifier identifier, String indexLocation);

    @LogMessage(level = WARN)
    @Message(id = 15895, value = "Extension %s is missing the required manifest attribute %s-%s (skipping extension)")
    void extensionMissingManifestAttribute(String item, String again, Attributes.Name suffix);

    @LogMessage(level = WARN)
    @Message(id = 15896, value = "Extension %s URI syntax is invalid: %s")
    void invalidExtensionURI(String item, URISyntaxException e);

    @LogMessage(level = WARN)
    @Message(id = 15897, value = "Could not find Extension-List entry %s referenced from %s")
    void cannotFindExtensionListEntry(ExtensionListEntry entry, ResourceRoot resourceRoot);
}
