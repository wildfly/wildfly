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

package org.jboss.as.service;

import java.util.List;

import org.jboss.as.server.deployment.Attachments;
import org.jboss.as.server.deployment.DeploymentPhaseContext;
import org.jboss.as.server.deployment.DeploymentType;
import org.jboss.as.server.deployment.DeploymentTypeMarker;
import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.as.server.deployment.DeploymentUnitProcessingException;
import org.jboss.as.server.deployment.DeploymentUnitProcessor;
import org.jboss.as.server.deployment.ResourceRootType;
import org.jboss.as.server.deployment.ResourceRootTypeMarker;
import org.jboss.as.server.deployment.module.ResourceRoot;
import org.jboss.vfs.VirtualFile;

/**
 * Deployment processor used to determine if a possible sub-deployment contains a service descriptor.
 *
 * @author John Bailey
 */
public class SarSubDeploymentProcessor implements DeploymentUnitProcessor {
    public void deploy(final DeploymentPhaseContext phaseContext) throws DeploymentUnitProcessingException {
       final DeploymentUnit deploymentUnit = phaseContext.getDeploymentUnit();
        if (!DeploymentTypeMarker.isType(DeploymentType.EAR, deploymentUnit)) {
            return;
        }

        final List<ResourceRoot> resourceRoots = deploymentUnit.getAttachment(Attachments.RESOURCE_ROOTS);
        for(ResourceRoot resourceRoot : resourceRoots) {
            final VirtualFile rootFile = resourceRoot.getRoot();
            if (!ResourceRootTypeMarker.isSubDeployment(resourceRoot)) {
                final VirtualFile sarDescriptor = rootFile.getChild(ServiceDeploymentParsingProcessor.SERVICE_DESCRIPTOR_PATH);
                if(sarDescriptor.exists()) {
                    ResourceRootTypeMarker.setType(ResourceRootType.SAR, resourceRoot);
                }
            }
        }
    }

    public void undeploy(DeploymentUnit context) {
    }
}
