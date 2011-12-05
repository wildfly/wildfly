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

import org.jboss.as.server.deployment.Attachments;
import org.jboss.as.server.deployment.DeploymentPhaseContext;
import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.as.server.deployment.DeploymentUnitProcessingException;
import org.jboss.as.server.deployment.DeploymentUnitProcessor;
import org.jboss.as.server.moduleservice.ServiceModuleLoader;
import org.jboss.modules.ModuleIdentifier;
import org.jboss.vfs.VirtualFile;

/**
 * Deployment processor that generates module identifiers for the deployment and attaches it.
 *
 * @author Stuart Douglas
 *
 */
public class ModuleIdentifierProcessor implements DeploymentUnitProcessor {

    @Override
    public void deploy(DeploymentPhaseContext phaseContext) throws DeploymentUnitProcessingException {
        final DeploymentUnit deploymentUnit = phaseContext.getDeploymentUnit();
        final ResourceRoot deploymentRoot = deploymentUnit.getAttachment(Attachments.DEPLOYMENT_ROOT);

        final DeploymentUnit parent = deploymentUnit.getParent();
        final DeploymentUnit topLevelDeployment = parent == null ? deploymentUnit : parent;
        final VirtualFile toplevelRoot = topLevelDeployment.getAttachment(Attachments.DEPLOYMENT_ROOT).getRoot();
        final ModuleIdentifier moduleIdentifier = createModuleIdentifier(deploymentUnit.getName(), deploymentRoot, topLevelDeployment, toplevelRoot, deploymentUnit.getParent() == null);


        deploymentUnit.putAttachment(Attachments.MODULE_IDENTIFIER, moduleIdentifier);
    }

    public static ModuleIdentifier createModuleIdentifier(final String deploymentUnitName, final ResourceRoot deploymentRoot, final DeploymentUnit topLevelDeployment, final VirtualFile toplevelRoot, final boolean topLevel) {
        // generate the module identifier for the deployment
        final ModuleIdentifier moduleIdentifier;
        if (topLevel) {
            moduleIdentifier = ModuleIdentifier.create(ServiceModuleLoader.MODULE_PREFIX + deploymentUnitName);
        } else {
            String relativePath = deploymentRoot.getRoot().getPathNameRelativeTo(toplevelRoot);
            moduleIdentifier = ModuleIdentifier.create(ServiceModuleLoader.MODULE_PREFIX + topLevelDeployment.getName() + '.' + relativePath.replace('/', '.'));
        }
        return moduleIdentifier;
    }

    @Override
    public void undeploy(DeploymentUnit deploymentUnit) {

    }

}
