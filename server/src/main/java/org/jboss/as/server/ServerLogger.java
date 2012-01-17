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

import org.jboss.logging.BasicLogger;
import org.jboss.logging.Cause;
import org.jboss.logging.LogMessage;
import org.jboss.logging.Logger;
import org.jboss.logging.Message;
import org.jboss.logging.MessageLogger;
import org.jboss.modules.ModuleIdentifier;

import static org.jboss.logging.Logger.Level.ERROR;
import static org.jboss.logging.Logger.Level.INFO;
import static org.jboss.logging.Logger.Level.WARN;

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
     * A logger with the category {@code org.jboss.as.controller}.
     */
    ServerLogger CONTROLLER_LOGGER = Logger.getMessageLogger(ServerLogger.class, "org.jboss.as.controller");

    /**
     * A logger with the category {@code org.jboss.as.deployment}.
     */
    ServerLogger DEPLOYMENT_LOGGER = Logger.getMessageLogger(ServerLogger.class, "org.jboss.as.deployment");

    /**
     * Logger for private APIs.
     */
    ServerLogger PRIVATE_DEP_LOGGER = Logger.getMessageLogger(ServerLogger.class, "org.jboss.as.dependency.private");
    /**
     * Logger for unsupported APIs.
     */
    ServerLogger UNSUPPORTED_DEP_LOGGER = Logger.getMessageLogger(ServerLogger.class, "org.jboss.as.dependency.unsupported");

    /**
     * A logger with the category {@code org.jboss.as.deployment.module}.
     */
    ServerLogger DEPLOYMENT_MODULE_LOGGER = Logger.getMessageLogger(ServerLogger.class, "org.jboss.as.deployment.module");
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

    @Message(id = 18569, value = "Exception occurred removing deployment content %s")
    void failedToRemoveDeploymentContent(@Cause Throwable cause, String hash);

    @LogMessage(level = INFO)
    @Message(id = 15870, value = "Deploy of deployment \"%s\" was rolled back with failure message %s")
    void deploymentRolledBack(String deployment, String message);

    @LogMessage(level = INFO)
    @Message(id = 15871, value = "Deploy of deployment \"%s\" was rolled back with no failure message")
    void deploymentRolledBackWithNoMessage(String deployment);

}
