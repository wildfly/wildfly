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

package org.jboss.as.osgi;

import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.logging.BasicLogger;
import org.jboss.logging.Cause;
import org.jboss.logging.LogMessage;
import org.jboss.logging.Logger;
import org.jboss.logging.Message;
import org.jboss.logging.MessageLogger;
import org.jboss.modules.Module;
import org.jboss.osgi.deployment.deployer.Deployment;
import org.osgi.framework.Bundle;

import java.io.File;

import static org.jboss.logging.Logger.Level.ERROR;
import static org.jboss.logging.Logger.Level.INFO;
import static org.jboss.logging.Logger.Level.WARN;

/**
 * Date: 27.06.2011
 *
 * @author <a href="mailto:jperkins@redhat.com">James R. Perkins</a>
 */
@MessageLogger(projectCode = "JBAS")
public interface OSGiLogger extends BasicLogger {
    /**
     * The root logger with a category of the package.
     */
    OSGiLogger ROOT_LOGGER = Logger.getMessageLogger(OSGiLogger.class, OSGiLogger.class.getPackage().getName());

    /**
     * Logs an informational message indicating the OSGi subsystem is being activated.
     */
    @LogMessage(level = INFO)
    @Message(id = 11910, value = "Activating OSGi Subsystem")
    void activatingSubsystem();

    /**
     * Logs a warning message indicating the composite annotation index could not be found in the deployment unit.
     *
     * @param deploymentUnit the deployment unit.
     */
    @LogMessage(level = WARN)
    @Message(id = 11911, value = "Cannot find composite annotation index in: %s")
    void cannotFindAnnotationIndex(DeploymentUnit deploymentUnit);

    /**
     * Logs an error message indicating the bundle cannot start.
     *
     * @param cause  the cause of the error.
     * @param bundle the bundle that failed to start.
     */
    @LogMessage(level = ERROR)
    @Message(id = 11912, value = "Cannot start bundle: %s")
    void cannotStart(@Cause Throwable cause, Bundle bundle);

    /**
     * Logs a warning message indicating the bundle could not be undeployed.
     *
     * @param cause      the cause of the error.
     * @param deployment the deployment.
     */
    @LogMessage(level = WARN)
    @Message(id = 11913, value = "Cannot undeploy bundle: %s")
    void cannotUndeployBundle(@Cause Throwable cause, Deployment deployment);

    /**
     * Logs an error message indicating there was a problem adding the module represented by the {@code moduleId}
     * parameter.
     *
     * @param cause    the cause of the error.
     * @param moduleId the module id.
     */
    @LogMessage(level = ERROR)
    @Message(id = 11915, value = "Problem adding module: %s")
    void errorAddingModule(@Cause Throwable cause, String moduleId);

    /**
     * Logs an error message indicating the deployment failed to uninstall.
     *
     * @param cause      the cause of the error.
     * @param deployment the deployment that failed.
     */
    @LogMessage(level = ERROR)
    @Message(id = 11916, value = "Failed to uninstall deployment: %s")
    void failedToUninstallDeployment(@Cause Throwable cause, Deployment deployment);

    /**
     * Logs an error message indicating the deployment failed to uninstall.
     *
     * @param cause  the cause of the error.
     * @param module the module that failed to unregister.
     */
    @LogMessage(level = ERROR)
    @Message(id = 11917, value = "Failed to uninstall module: %s")
    void failedToUninstallModule(@Cause Throwable cause, Module module);

    /**
     * Logs a warning message indicating the OSGi bundle in the modules hierarchy was found.
     *
     * @param modulesFile the modules file.
     */
    @LogMessage(level = WARN)
    @Message(id = 11918, value = "Found OSGi bundle in modules hierarchy: %s")
    void foundOsgiBundle(File modulesFile);

    /**
     * Logs a warning message indicating the module could not be added as it was not found.
     *
     * @param moduleId the module id.
     */
    @LogMessage(level = ERROR)
    @Message(id = 11919, value = "Cannot add module as it was not found: %s")
    void moduleNotFound(String moduleId);

    /**
     * Logs an informational message indicating the module is attempting to be registered.
     *
     * @param module the module that is registering.
     */
    @LogMessage(level = INFO)
    @Message(id = 11920, value = "Register module: %s")
    void registerModule(Module module);

    /**
     * Logs an informational message indicating the OSGi framework is stopping.
     */
    @LogMessage(level = INFO)
    @Message(id = 11921, value = "Stopping OSGi Framework")
    void stoppingOsgiFramework();

    /**
     * Logs an informational message indicating the module is attempting to be unregistered.
     *
     * @param module the module that attempting to be unregistered.
     */
    @LogMessage(level = INFO)
    @Message(id = 11922, value = "Unregister module: %s")
    void unregisterModule(Module module);
}
