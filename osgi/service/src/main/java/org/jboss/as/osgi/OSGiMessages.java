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
import org.jboss.logging.Cause;
import org.jboss.logging.Message;
import org.jboss.logging.MessageBundle;
import org.jboss.logging.Messages;
import org.jboss.modules.Module;
import org.jboss.msc.service.StartException;
import org.jboss.osgi.deployment.deployer.Deployment;
import org.jboss.vfs.VirtualFile;
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

    @Message(id = 11960, value = "Cannot create bundle deployment from: %s")
    DeploymentUnitProcessingException cannotCreateBundleDeployment(@Cause Throwable th, DeploymentUnit deploymentUnit);

    @Message(id = 11961, value = "Cannot deploy bundle: %s")
    BundleException cannotDeployBundle(@Cause Throwable th, Deployment deployment);

    @Message(id = 11962, value = "Cannot find bundles directory: %s")
    IllegalStateException illegalStateCannotFindBundleDir(File dir);

    @Message(id = 11963, value = "Cannot parse OSGi metadata: %s")
    DeploymentUnitProcessingException cannotParseOSGiMetadata(@Cause Throwable th, VirtualFile file);

    @Message(id = 11964, value = "Failed to process initial capabilities")
    StartException startFailedToProcessInitialCapabilites(@Cause Throwable th);

    @Message(id = 11965, value = "Failed to create Framework services")
    StartException startFailedToCreateFrameworkServices(@Cause Throwable th);

    @Message(id = 11966, value = "Failed to install deployment: %s")
    StartException startFailedToInstallDeployment(@Cause Throwable th, Deployment deployment);

    @Message(id = 11967, value = "Failed to register module: %s")
    StartException startFailedToRegisterModule(@Cause Throwable th, Module module);

    @Message(id = 11968, value = "%s is null")
    IllegalArgumentException illegalArgumentNull(String name);

    //@Message(id = 11969, value = "OSGi subsystem not active")
    //String osgiSubsystemNotActive();

    //@Message(id = 11970, value = "Property %s already exists")
    //String propertyAlreadyExists(String name);

    @Message(id = 11971, value = "StartLevel service not available")
    String startLevelSrviceNotAvailable();

    @Message(id = 11972, value = "Cannot obtain bundle resource for: %s")
    IllegalArgumentException illegalArgumentCannotObtainBundleResource(String name);

    @Message(id = 11973, value = "Cannot resolve capability: %s")
    StartException startFailedCannotResolveInitialCapability(@Cause Throwable th, String identifier);
}
