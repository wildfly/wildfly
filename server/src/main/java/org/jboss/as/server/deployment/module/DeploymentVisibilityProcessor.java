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

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.jboss.as.server.deployment.Attachments;
import org.jboss.as.server.deployment.DeploymentPhaseContext;
import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.as.server.deployment.DeploymentUnitProcessingException;
import org.jboss.as.server.deployment.DeploymentUnitProcessor;
import org.jboss.modules.ModuleIdentifier;

/**
 * Processor that aggregates all module descriptions visible to the deployment in an EEApplicationClasses structure.
 *
 * @author Stuart Douglas
 */
public class DeploymentVisibilityProcessor implements DeploymentUnitProcessor {

    @Override
    public void deploy(final DeploymentPhaseContext phaseContext) throws DeploymentUnitProcessingException {
        final DeploymentUnit deploymentUnit = phaseContext.getDeploymentUnit();
        final ModuleSpecification moduleSpec = deploymentUnit.getAttachment(org.jboss.as.server.deployment.Attachments.MODULE_SPECIFICATION);
        final Map<ModuleIdentifier, DeploymentUnit> deployments = new HashMap<ModuleIdentifier, DeploymentUnit>();
        //local classes are always first
        deploymentUnit.addToAttachmentList(Attachments.ACCESSIBLE_SUB_DEPLOYMENTS, deploymentUnit);
        buildModuleMap(deploymentUnit, deployments);

        for (final ModuleDependency dependency : moduleSpec.getAllDependencies()) {
            final DeploymentUnit sub = deployments.get(dependency.getIdentifier());
            if (sub != null) {
                deploymentUnit.addToAttachmentList(Attachments.ACCESSIBLE_SUB_DEPLOYMENTS, sub);
            }
        }
    }

    private void buildModuleMap(final DeploymentUnit deploymentUnit, final Map<ModuleIdentifier, DeploymentUnit> modules) {
        if (deploymentUnit.getParent() == null) {
            final List<DeploymentUnit> subDeployments = deploymentUnit.getAttachmentList(org.jboss.as.server.deployment.Attachments.SUB_DEPLOYMENTS);
            for (final DeploymentUnit sub : subDeployments) {
                final ModuleIdentifier identifier = sub.getAttachment(org.jboss.as.server.deployment.Attachments.MODULE_IDENTIFIER);
                if (identifier != null) {
                    modules.put(identifier, sub);
                }
            }
        } else {
            final DeploymentUnit parent = deploymentUnit.getParent();
            final List<DeploymentUnit> subDeployments = parent.getAttachmentList(org.jboss.as.server.deployment.Attachments.SUB_DEPLOYMENTS);
            //add the parent description
            final ModuleIdentifier parentIdentifier = parent.getAttachment(org.jboss.as.server.deployment.Attachments.MODULE_IDENTIFIER);
            if (parentIdentifier != null) {
                modules.put(parentIdentifier, parent);
            }

            for (final DeploymentUnit sub : subDeployments) {
                if (sub != deploymentUnit) {
                    final ModuleIdentifier identifier = sub.getAttachment(org.jboss.as.server.deployment.Attachments.MODULE_IDENTIFIER);
                    if (identifier != null) {
                        modules.put(identifier, sub);
                    }
                }
            }
        }
    }

    @Override
    public void undeploy(final DeploymentUnit context) {

    }
}
