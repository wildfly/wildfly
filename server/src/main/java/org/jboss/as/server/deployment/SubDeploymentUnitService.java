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

import org.jboss.as.server.deployment.module.ModuleSpecification;
import org.jboss.as.server.deployment.module.ResourceRoot;
import org.jboss.msc.service.DelegatingServiceRegistry;
import org.jboss.msc.service.ServiceContainer;

/**
 * Service responsible for installing the correct services to install a {@link DeploymentUnit}.
 *
 * @author John Bailey
 */
public class SubDeploymentUnitService extends AbstractDeploymentUnitService {
    private final ResourceRoot deploymentRoot;
    private final DeploymentUnit parent;

    public SubDeploymentUnitService(ResourceRoot deploymentRoot, DeploymentUnit parent) {
        if (deploymentRoot == null) throw new IllegalArgumentException("Deployment root is required");
        this.deploymentRoot = deploymentRoot;
        if (parent == null) throw new IllegalArgumentException("Sub-deployments require a parent deployment unit");
        this.parent = parent;
    }

    protected DeploymentUnit createAndInitializeDeploymentUnit(ServiceContainer container) {
        final DeploymentUnit deploymentUnit = new DeploymentUnitImpl(parent, deploymentRoot.getRootName(), new DelegatingServiceRegistry(container));
        deploymentUnit.putAttachment(Attachments.DEPLOYMENT_ROOT, deploymentRoot);
        deploymentUnit.putAttachment(Attachments.MODULE_SPECIFICATION, new ModuleSpecification());
        return deploymentUnit;
    }
}
