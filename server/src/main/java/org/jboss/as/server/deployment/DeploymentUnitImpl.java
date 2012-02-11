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

import org.jboss.as.controller.PathElement;
import org.jboss.dmr.ModelNode;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceRegistry;

/**
 * Default implementation for DeploymentUnitContext.
 *
 * @author John E. Bailey
 */
class DeploymentUnitImpl extends SimpleAttachable implements DeploymentUnit {
    private final DeploymentUnit parent;
    private final String name;
    private final ServiceRegistry serviceRegistry;

    /**
     * Construct a new instance.
     *
     * @param parent the parent (enclosing) deployment unit, if any
     * @param name the deployment unit name
     * @param serviceRegistry the service registry
     */
    DeploymentUnitImpl(final DeploymentUnit parent, final String name, final ServiceRegistry serviceRegistry) {
        this.parent = parent;
        this.name = name;
        this.serviceRegistry = serviceRegistry;
    }

    public ServiceName getServiceName() {
        if (parent != null) {
            return Services.deploymentUnitName(parent.getName(), name);
        } else {
            return Services.deploymentUnitName(name);
        }
    }

    /** {@inheritDoc} */
    public DeploymentUnit getParent() {
        return parent;
    }

    /** {@inheritDoc} */
    public String getName() {
        return name;
    }

    /** {@inheritDoc} */
    public ServiceRegistry getServiceRegistry() {
        return serviceRegistry;
    }

    /** {@inheritDoc} */
    public String toString() {
        if (parent != null) {
            return String.format("subdeployment \"%s\" of %s", name, parent);
        } else {
            return String.format("deployment \"%s\"", name);
        }
    }

    @Override
    public ModelNode getDeploymentSubsystemModel(final String subsystemName) {
        return DeploymentModelUtils.getSubsystemRoot(subsystemName, this);
    }

    @Override
    public ModelNode createDeploymentSubModel(final String subsystemName, final PathElement address) {
        return DeploymentModelUtils.createDeploymentSubModel(subsystemName, address, this);
    }

}
