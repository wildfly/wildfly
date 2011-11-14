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
 * Date: 27.06.2011
 *
 * @author <a href="mailto:jperkins@redhat.com">James R. Perkins</a>
 */
@MessageBundle(projectCode = "JBAS")
public interface OSGiMessages {
    /**
     * The messages.
     */
    OSGiMessages MESSAGES = Messages.getBundle(OSGiMessages.class);

    /**
     * A message indicating the deployment bundle could not be created from the deployment unit.
     *
     * @param deploymentUnit the deployment unit.
     *
     * @return the message.
     */
    @Message(id = 11940, value = "Cannot create bundle deployment from: %s")
    String cannotCreateBundleDeployment(DeploymentUnit deploymentUnit);

    /**
     * A message indicating the bundle cannot be deployed.
     *
     * @param deployment the deployment.
     *
     * @return the message.
     */
    @Message(id = 11941, value = "Cannot deploy bundle: %s")
    String cannotDeployBundle(Deployment deployment);

    /**
     * Creates an exception indicating the bundle directory could not be found.
     *
     * @param dir the bundle directory.
     *
     * @return an {@link IllegalArgumentException} for the error.
     */
    @Message(id = 11942, value = "Cannot find bundles directory: %s")
    IllegalArgumentException cannotFindBundleDir(File dir);

    /**
     * A message indicating the file could not be parsed.
     *
     * @param file the file that could not be parsed.
     *
     * @return the message.
     */
    @Message(id = 11943, value = "Cannot parse: %s")
    String cannotParse(VirtualFile file);

    /**
     * A message indicating a failure to create the auto install list.
     *
     * @return the message.
     */
    @Message(id = 11944, value = "Failed to create auto install list")
    String failedToCreateAutoInstallList();

    /**
     * A message indicating a failure to create the framework services.
     *
     * @return the message.
     */
    @Message(id = 11945, value = "Failed to create Framework services")
    String failedToCreateFrameworkServices();

    /**
     * A message indicating the deployment failed to install.
     *
     * @param deployment the deployment that failed.
     *
     * @return the message.
     */
    @Message(id = 11946, value = "Failed to install deployment: %s")
    String failedToInstallDeployment(Deployment deployment);

    /**
     * A message indicating the module failed to register.
     *
     * @param module the module that failed to register.
     *
     * @return the message.
     */
    @Message(id = 11947, value = "Failed to register module: %s")
    String failedToRegisterModule(Module module);

    /**
     * Creates an exception indicating the variable is {@code null}.
     *
     * @param varName the variable name.
     *
     * @return an {@link IllegalArgumentException} for the error.
     */
    @Message(id = 11948, value = "%s is null")
    IllegalArgumentException nullVar(String varName);

    /**
     * A message indicating that the OSGi subsysem is not active
     *
     * @return the message.
     */
    @Message(id = 11949, value = "OSGi subsystem not active")
    String osgiSubsystemNotActive();

    /**
     * A message indicating the property already exists.
     *
     * @param name the property name.
     *
     * @return the message.
     */
    @Message(id = 11950, value = "Property %s already exists")
    String propertyAlreadyExists(String name);

    /**
     * A message indicating that a service is not available.
     *
     * @return the message.
     */
    @Message(id = 11951, value = "Service not available")
    String serviceNotAvailable();
}
