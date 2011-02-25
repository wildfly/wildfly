/*
 * JBoss, Home of Professional Open Source
 * Copyright 2010, Red Hat Inc., and individual contributors as indicated
 * by the @authors tag. See the copyright.txt in the distribution for a
 * full listing of individual contributors.
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
import org.jboss.as.server.deployment.PrivateSubDeploymentMarker;
import org.jboss.modules.ModuleIdentifier;
import org.jboss.modules.ModuleLoader;

/**
 * Processor that set up module dependencies between sub deployments
 *
 * @author Stuart Douglas
 *
 */
public class SubDeploymentDependencyProcessor implements DeploymentUnitProcessor {

    @Override
    public void deploy(DeploymentPhaseContext phaseContext) throws DeploymentUnitProcessingException {
        final DeploymentUnit deploymentUnit = phaseContext.getDeploymentUnit();
        final DeploymentUnit parent = deploymentUnit.getParent();
        //only run for sub deployments
        if (parent == null) {
            return;
        }
        final ModuleSpecification moduleSpec=deploymentUnit.getAttachment(Attachments.MODULE_SPECIFICATION);
        final AttachmentList<DeploymentUnit> subDeployments = parent.getAttachment(Attachments.SUB_DEPLOYMENTS);
        final ModuleLoader moduleLoader = deploymentUnit.getAttachment(Attachments.SERVICE_MODULE_LOADER);
        final ModuleIdentifier parentModule = parent.getAttachment(Attachments.MODULE_IDENTIFIER);
        if (parentModule != null) {
            // access to ear classes
            moduleSpec.addDependency(new ModuleDependency(moduleLoader, parentModule, false, false, true));
        }

        for(DeploymentUnit subDeployment : subDeployments){
            if(subDeployment.getServiceName().equals(deploymentUnit.getServiceName())) {
                continue;
            }
            if(PrivateSubDeploymentMarker.isPrivate(subDeployment)) {
                continue;
            }
            ModuleDependency moduleDependency = new ModuleDependency(moduleLoader, subDeployment.getAttachment(Attachments.MODULE_IDENTIFIER), false, false, true);
            moduleSpec.addDependency(moduleDependency);
        }
    }

    @Override
    public void undeploy(DeploymentUnit context) {
    }

}
