/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2010, Red Hat, Inc., and individual contributors
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

package org.jboss.as.server.deployment;

import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.registry.Resource;
import org.jboss.dmr.ModelNode;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceRegistry;

/**
 * The deployment unit.  This object retains data which is persistent for the life of the
 * deployment.
 */
public interface DeploymentUnit extends Attachable {

    /**
     * Get the service name of the root deployment unit service.
     *
     * @return the service name
     */
    ServiceName getServiceName();

    /**
     * Get the deployment unit of the parent (enclosing) deployment.
     *
     * @return the parent deployment unit, or {@code null} if this is a top-level deployment
     */
    DeploymentUnit getParent();

    /**
     * Get the simple name of the deployment unit.
     *
     * @return the simple name
     */
    String getName();

    /**
     * Get the service registry.
     *
     * @return the service registry
     */
    ServiceRegistry getServiceRegistry();

    /**
     * Get the extension deployment model root.
     *
     * @param subsystemName the subsystem name.
     * @return the model
     */
    ModelNode getDeploymentSubsystemModel(final String subsystemName);

    /**
     * Create a management sub-model for components from the deployment itself. Operations, metrics and descriptions
     * have to be registered as part of the subsystem registration {@link org.jboss.as.controller.ExtensionContext} and
     * {@linkplain org.jboss.as.controller.SubsystemRegistration.registerDeploymentModel}.
     *
     * @param subsystemName the subsystem name the model was registered
     * @param address the path address this sub-model should be created in
     * @return the model node
     */
    ModelNode createDeploymentSubModel(final String subsystemName, final PathElement address);

    /**
     *
     * This method is extension of {@link #createDeploymentSubModel(String, PathElement)}, the difference is that this method traverses recursively till last
     * element in {@link PathAddress}.
     *
     * @param subsystemName the subsystem name the model was registered
     * @param address the path address this sub-model should be created in
     * @return the model node
     */
    ModelNode createDeploymentSubModel(final String subsystemName, final PathAddress address);

    /**
     *
     * This method is extension of {@link #createDeploymentSubModel(String, PathAddress)}, the difference is that it accepts resource that should be registered
     * at specified path.
     *
     * @param subsystemName the subsystem name the model was registered
     * @param address the path address this sub-model should be created in
     * @param resource the resource that needs to be registered as submodule
     * @return the model node
     */
    ModelNode createDeploymentSubModel(final String subsystemName, final PathAddress address, final Resource resource);

}
