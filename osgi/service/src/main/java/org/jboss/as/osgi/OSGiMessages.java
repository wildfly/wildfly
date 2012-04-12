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
import org.jboss.logging.Message;
import org.jboss.logging.MessageBundle;
import org.jboss.logging.Messages;
import org.jboss.modules.Module;
import org.jboss.osgi.deployment.deployer.Deployment;
import org.jboss.vfs.VirtualFile;

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

    @Message(id = 11950, value = "Cannot create bundle deployment from: %s")
    String cannotCreateBundleDeployment(DeploymentUnit deploymentUnit);

    @Message(id = 11951, value = "Cannot deploy bundle: %s")
    String cannotDeployBundle(Deployment deployment);

    @Message(id = 11952, value = "Cannot find bundles directory: %s")
    IllegalArgumentException cannotFindBundleDir(File dir);

    @Message(id = 11953, value = "Cannot parse: %s")
    String cannotParse(VirtualFile file);

    @Message(id = 11954, value = "Failed to create auto install list")
    String failedToCreateAutoInstallList();

    @Message(id = 11955, value = "Failed to create Framework services")
    String failedToCreateFrameworkServices();

    @Message(id = 11956, value = "Failed to install deployment: %s")
    String failedToInstallDeployment(Deployment deployment);

    @Message(id = 11957, value = "Failed to register module: %s")
    String failedToRegisterModule(Module module);

    @Message(id = 11958, value = "%s is null")
    IllegalArgumentException nullVar(String varName);

    @Message(id = 11959, value = "OSGi subsystem not active")
    String osgiSubsystemNotActive();

    @Message(id = 11960, value = "Property %s already exists")
    String propertyAlreadyExists(String name);

    @Message(id = 11961, value = "Service not available")
    String serviceNotAvailable();
}
