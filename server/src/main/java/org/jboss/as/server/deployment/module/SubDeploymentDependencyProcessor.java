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

import java.util.ArrayList;
import java.util.List;

import org.jboss.as.server.deployment.Attachments;
import org.jboss.as.server.deployment.DeploymentPhaseContext;
import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.as.server.deployment.DeploymentUnitProcessingException;
import org.jboss.as.server.deployment.DeploymentUnitProcessor;
import org.jboss.modules.ModuleIdentifier;
import org.jboss.modules.ModuleLoader;
import org.jboss.modules.filter.PathFilters;

/**
 * Processor that set up a module dependency on the parent module
 *
 * @author Stuart Douglas
 */
public class SubDeploymentDependencyProcessor implements DeploymentUnitProcessor {

    @Override
    public void deploy(DeploymentPhaseContext phaseContext) throws DeploymentUnitProcessingException {
        final DeploymentUnit deploymentUnit = phaseContext.getDeploymentUnit();
        final DeploymentUnit parent = deploymentUnit.getParent() == null ? deploymentUnit : deploymentUnit.getParent();


        final ModuleSpecification parentModuleSpec = parent.getAttachment(Attachments.MODULE_SPECIFICATION);

        final ModuleSpecification moduleSpec = deploymentUnit.getAttachment(Attachments.MODULE_SPECIFICATION);
        final ModuleLoader moduleLoader = deploymentUnit.getAttachment(Attachments.SERVICE_MODULE_LOADER);
        final ModuleIdentifier moduleIdentifier = deploymentUnit.getAttachment(Attachments.MODULE_IDENTIFIER);

        if (deploymentUnit.getParent() != null) {
            final ModuleIdentifier parentModule = parent.getAttachment(Attachments.MODULE_IDENTIFIER);
            if (parentModule != null) {
                // access to ear classes
                ModuleDependency moduleDependency = new ModuleDependency(moduleLoader, parentModule, false, false, true, false);
                moduleDependency.addImportFilter(PathFilters.acceptAll(), true);
                moduleSpec.addLocalDependency(moduleDependency);
            }
        }

        //If the sub deployments aren't isolated, then we need to set up dependencies between the sub deployments
        if (!parentModuleSpec.isSubDeploymentModulesIsolated()) {
            final List<DeploymentUnit> subDeployments = parent.getAttachmentList(Attachments.SUB_DEPLOYMENTS);
            final List<ModuleDependency> accessibleModules = new ArrayList<ModuleDependency>();
            for (DeploymentUnit subDeployment : subDeployments) {
                final ModuleSpecification subModule = subDeployment.getAttachment(Attachments.MODULE_SPECIFICATION);
                if (!subModule.isPrivateModule()) {
                    ModuleIdentifier identifier = subDeployment.getAttachment(Attachments.MODULE_IDENTIFIER);
                    ModuleDependency dependency = new ModuleDependency(moduleLoader, identifier, false, false, true, false);
                    dependency.addImportFilter(PathFilters.acceptAll(), true);
                    accessibleModules.add(dependency);
                }
            }
            for (ModuleDependency identifier : accessibleModules) {
                if (!identifier.equals(moduleIdentifier)) {
                    moduleSpec.addLocalDependency(identifier);
                }
            }
        }

    }

    @Override
    public void undeploy(DeploymentUnit context) {
    }

}
