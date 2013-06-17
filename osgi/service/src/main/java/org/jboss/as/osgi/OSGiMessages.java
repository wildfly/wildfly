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

import java.io.File;

import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.as.server.deployment.DeploymentUnitProcessingException;
import org.jboss.logging.Messages;
import org.jboss.logging.annotations.Cause;
import org.jboss.logging.annotations.Message;
import org.jboss.logging.annotations.MessageBundle;
import org.jboss.modules.Module;
import org.jboss.msc.service.StartException;
import org.jboss.osgi.deployment.deployer.Deployment;
import org.jboss.vfs.VirtualFile;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleException;

/**
 * Logging Id ranges: 11950-11999
 *
 * https://community.jboss.org/wiki/LoggingIds
 *
 * @author <a href="mailto:jperkins@redhat.com">James R. Perkins</a>
 * @author Thomas.Diesler@jboss.com
 */
@MessageBundle(projectCode = "JBAS")
public interface OSGiMessages {

    OSGiMessages MESSAGES = Messages.getBundle(OSGiMessages.class);

    @Message(id = 11950, value = "%s is null")
    IllegalArgumentException illegalArgumentNull(String name);

    @Message(id = 11951, value = "Cannot create bundle deployment from: %s")
    DeploymentUnitProcessingException cannotCreateBundleDeployment(@Cause Throwable th, DeploymentUnit deploymentUnit);

    @Message(id = 11952, value = "Cannot deploy bundle revision: %s")
    BundleException cannotDeployBundleRevision(@Cause Throwable th, Deployment deployment);

    @Message(id = 11953, value = "Cannot find bundles directory: %s")
    IllegalStateException illegalStateCannotFindBundleDir(File dir);

    @Message(id = 11954, value = "Cannot parse OSGi metadata: %s")
    DeploymentUnitProcessingException cannotParseOSGiMetadata(@Cause Throwable th, VirtualFile file);

    @Message(id = 11955, value = "Failed to process initial capability: %s")
    StartException startFailedToProcessInitialCapability(@Cause Throwable th, String identifier);

    @Message(id = 11956, value = "Failed to create Framework services")
    StartException startFailedToCreateFrameworkServices(@Cause Throwable th);

    //@Message(id = 11957, value = "Failed to install deployment: %s")
    //StartException startFailedToInstallDeployment(@Cause Throwable th, Deployment deployment);

    @Message(id = 11958, value = "Failed to register module: %s")
    IllegalStateException illegalStateFailedToRegisterModule(@Cause Throwable th, Module module);

    @Message(id = 11959, value = "StartLevel service not available")
    String startLevelServiceNotAvailable();

    @Message(id = 11960, value = "Cannot obtain bundle resource for: %s")
    IllegalArgumentException illegalArgumentCannotObtainBundleResource(String name);

    @Message(id = 11961, value = "Cannot resolve capability: %s")
    StartException cannotResolveInitialCapability(@Cause Throwable th, String identifier);

    @Message(id = 11962, value = "Illegal repository base location: %s")
    IllegalStateException illegalStateArtifactBaseLocation(File dir);

    @Message(id = 11963, value = "Invalid servlet alias: %s")
    String invalidServletAlias(String alias);

    @Message(id = 11964, value = "Invalid resource name: %s")
    String invalidResourceName(String name);

    @Message(id = 11965, value = "HttpService mapping does not exist: %s")
    String aliasMappingDoesNotExist(String alias);

    @Message(id = 11966, value = "HttpService mapping '%s' not owned by bundle: %s")
    String aliasMappingNotOwnedByBundle(String alias, Bundle bundle);

    @Message(id = 11967, value = "HttpService mapping already exists: %s")
    String aliasMappingAlreadyExists(String alias);

    @Message(id = 11968, value = "Cannot start bundle: %s")
    StartException cannotStartBundle(@Cause Throwable cause, Bundle bundle);

    @Message(id = 11969, value = "Cannot activate deferred module phase for: %s")
    BundleException cannotActivateDeferredModulePhase(@Cause Throwable cause, Bundle bundle);

    @Message(id = 11970, value = "Cannot deactivate deferred module phase for: %s")
    BundleException cannotDeactivateDeferredModulePhase(@Cause Throwable cause, Bundle bundle);

    @Message(id = 11971, value = "Servlet %s already registered with HttpService")
    String servletAlreadyRegistered(String info);

    @Message(id = 11972, value = "No layers directory found at %s")
    IllegalStateException illegalStateNoLayersDirectoryFound(File dir);

    @Message(id = 11973, value = "Cannot find layer %s under directory %s")
    IllegalStateException illegalStateCannotFindLayer(String layer, File dir);

    @Message(id = 11974, value = "Starting web context failed")
    String startContextFailed();
}
