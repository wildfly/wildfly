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

package org.jboss.as.server.deployment.module;

import org.jboss.as.server.deployment.AttachmentList;
import org.jboss.as.server.deployment.Attachments;
import org.jboss.as.server.deployment.DeploymentPhaseContext;
import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.as.server.deployment.DeploymentUnitProcessingException;
import org.jboss.as.server.deployment.DeploymentUnitProcessor;
import org.jboss.as.server.deployment.SubDeploymentMarker;
import org.jboss.as.server.moduleservice.ServiceModuleLoader;
import org.jboss.modules.ModuleIdentifier;

/**
 * This process creates additional module entries for ear deployments. Thnis only runs for ear deployments, and makes additional
 * modules out of any resource roots that are not sub deployments or part of /lib
 *
 * @author Stuart Douglas
 */
public final class AdditionalModuleProcessor implements DeploymentUnitProcessor {

    public void deploy(final DeploymentPhaseContext phaseContext) throws DeploymentUnitProcessingException {
        final DeploymentUnit deploymentUnit = phaseContext.getDeploymentUnit();
        if (deploymentUnit.getParent() != null) {
            return;
        }
        final ResourceRoot deploymentRoot = deploymentUnit.getAttachment(Attachments.DEPLOYMENT_ROOT);
        final AttachmentList<ResourceRoot> resourceRoots = deploymentUnit.getAttachment(Attachments.RESOURCE_ROOTS);
        if (resourceRoots != null) {
            for (ResourceRoot resourceRoot : resourceRoots) {
                if (ModuleRootMarker.isModuleRoot(resourceRoot)) {
                    continue;
                }
                if (SubDeploymentMarker.isSubDeployment(resourceRoot)) {
                    continue;
                }
                String pathName = resourceRoot.getRoot().getPathNameRelativeTo(deploymentRoot.getRoot());
                ModuleIdentifier identifier = ModuleIdentifier.create(ServiceModuleLoader.MODULE_PREFIX
                        + deploymentUnit.getName() + "." + pathName);
                AdditionalModuleSpecification module = new AdditionalModuleSpecification(identifier, resourceRoot);
                deploymentUnit.addToAttachmentList(Attachments.ADDITIONAL_MODULES, module);
            }
        }
    }

    public void undeploy(final DeploymentUnit context) {
    }
}
