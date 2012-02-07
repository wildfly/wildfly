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

import static org.jboss.logging.Logger.Level.ERROR;
import static org.jboss.logging.Logger.Level.INFO;
import static org.jboss.logging.Logger.Level.WARN;

/**
 * Logging Id ranges: 11900-11959
 *
 * https://community.jboss.org/wiki/LoggingIds
 *
 * ERROR: 11900-11919
 * WARN : 11920-11939
 * INFO : 11940-11959
 *
 * @author <a href="mailto:jperkins@redhat.com">James R. Perkins</a>
 * @author Thomas.Diesler@jboss.com
 */
@MessageLogger(projectCode = "JBAS")
public interface OSGiLogger extends BasicLogger {

    OSGiLogger ROOT_LOGGER = Logger.getMessageLogger(OSGiLogger.class, OSGiLogger.class.getPackage().getName());

    @LogMessage(level = ERROR)
    @Message(id = 11900, value = "Cannot start bundle: %s")
    void cannotStart(@Cause Throwable cause, Bundle bundle);

    @LogMessage(level = ERROR)
    @Message(id = 11901, value = "Problem adding module: %s")
    void errorAddingModule(@Cause Throwable cause, String moduleId);

    @LogMessage(level = ERROR)
    @Message(id = 11902, value = "Failed to uninstall deployment: %s")
    void failedToUninstallDeployment(@Cause Throwable cause, Deployment deployment);

    @LogMessage(level = ERROR)
    @Message(id = 11903, value = "Failed to uninstall module: %s")
    void failedToUninstallModule(@Cause Throwable cause, Module module);

    @LogMessage(level = ERROR)
    @Message(id = 11904, value = "Cannot add module as it was not found: %s")
    void moduleNotFound(String moduleId);

    @LogMessage(level = WARN)
    @Message(id = 11920, value = "Cannot find composite annotation index in: %s")
    void cannotFindAnnotationIndex(DeploymentUnit deploymentUnit);

    @LogMessage(level = WARN)
    @Message(id = 11921, value = "Cannot undeploy bundle: %s")
    void cannotUndeployBundle(@Cause Throwable cause, Deployment deployment);

    @LogMessage(level = WARN)
    @Message(id = 11922, value = "Cannot resolve capability: %s")
    void cannotResolveCapability(String identifier);

    @LogMessage(level = INFO)
    @Message(id = 11940, value = "Activating OSGi Subsystem")
    void activatingSubsystem();

    @LogMessage(level = INFO)
    @Message(id = 11941, value = "Register module: %s")
    void registerModule(Module module);

    @LogMessage(level = INFO)
    @Message(id = 11942, value = "Stopping OSGi Framework")
    void stoppingOsgiFramework();

    @LogMessage(level = INFO)
    @Message(id = 11943, value = "Unregister module: %s")
    void unregisterModule(Module module);
}
