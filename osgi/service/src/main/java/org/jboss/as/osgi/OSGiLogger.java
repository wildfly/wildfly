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

import static org.jboss.logging.Logger.Level.ERROR;
import static org.jboss.logging.Logger.Level.INFO;
import static org.jboss.logging.Logger.Level.WARN;

import java.util.Collection;

import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.logging.BasicLogger;
import org.jboss.logging.Logger;
import org.jboss.logging.annotations.Cause;
import org.jboss.logging.annotations.LogMessage;
import org.jboss.logging.annotations.Message;
import org.jboss.logging.annotations.MessageLogger;
import org.jboss.modules.ModuleIdentifier;
import org.jboss.osgi.deployment.deployer.Deployment;
import org.osgi.framework.Bundle;
import org.osgi.resource.Requirement;

/**
 * Logging Id ranges: 11900-11949
 *
 * https://community.jboss.org/wiki/LoggingIds
 *
 * @author <a href="mailto:jperkins@redhat.com">James R. Perkins</a>
 * @author Thomas.Diesler@jboss.com
 */
@MessageLogger(projectCode = "JBAS")
public interface OSGiLogger extends BasicLogger {

    OSGiLogger LOGGER = Logger.getMessageLogger(OSGiLogger.class, OSGiLogger.class.getPackage().getName());

    @LogMessage(level = ERROR)
    @Message(id = 11900, value = "Cannot start bundle: %s")
    void errorCannotStartBundle(@Cause Throwable cause, Bundle bundle);

    //@LogMessage(level = ERROR)
    //@Message(id = 11901, value = "Problem adding module: %s")
    //void errorAddingModule(@Cause Throwable cause, String moduleId);

    //@LogMessage(level = ERROR)
    //@Message(id = 11902, value = "Failed to uninstall deployment: %s")
    //void errorFailedToUninstallDeployment(@Cause Throwable cause, Deployment deployment);

    //@LogMessage(level = ERROR)
    //@Message(id = 11903, value = "Cannot add module as it was not found: %s")
    //void errorModuleNotFound(String moduleId);

    @LogMessage(level = WARN)
    @Message(id = 11904, value = "Cannot find composite annotation index in: %s")
    void warnCannotFindAnnotationIndex(DeploymentUnit deploymentUnit);

    @LogMessage(level = WARN)
    @Message(id = 11905, value = "Cannot undeploy bundle: %s")
    void warnCannotUndeployBundle(@Cause Throwable cause, Bundle bundle);

    @LogMessage(level = INFO)
    @Message(id = 11906, value = "Activating OSGi Subsystem")
    void infoActivatingSubsystem();

    @LogMessage(level = INFO)
    @Message(id = 11907, value = "Register module: %s")
    void infoRegisterModule(ModuleIdentifier moduleId);

    @LogMessage(level = INFO)
    @Message(id = 11908, value = "Unregister module: %s")
    void infoUnregisterModule(ModuleIdentifier moduleId);

    @LogMessage(level = ERROR)
    @Message(id = 11909, value = "Management operation '%s' failed")
    void errorInOperationHandler(@Cause Throwable cause, String opname);

    @LogMessage(level = WARN)
    @Message(id = 11910, value = "Cannot resolve requirements: %s")
    void warnCannotResolve(Collection<Requirement> requirements);

    @LogMessage(level = INFO)
    @Message(id = 11911, value = "Bound system context: %s")
    void infoBoundSystemContext(String jndiName);

    @LogMessage(level = INFO)
    @Message(id = 11912, value = "Unbound system context: %s")
    void infoUnboundSystemContext(String jndiName);

    @LogMessage(level = INFO)
    @Message(id = 11913, value = "Register HttpService alias: %s")
    void infoRegisterHttpServiceAlias(String alias);

    @LogMessage(level = INFO)
    @Message(id = 11914, value = "Unregister HttpService alias: %s")
    void infoUnregisterHttpServiceAlias(String alias);

    @LogMessage(level = WARN)
    @Message(id = 11915, value = "Cannot deploy from management operation, bypassing deployment unit processors: %s")
    void warnCannotDeployBundleFromManagementOperation(Deployment dep);
}
