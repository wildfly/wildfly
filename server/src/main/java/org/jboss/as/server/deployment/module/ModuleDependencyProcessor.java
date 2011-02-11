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

package org.jboss.as.server.deployment.module;

import java.util.List;
import java.util.jar.Manifest;

import org.jboss.as.server.deployment.Attachments;
import org.jboss.as.server.deployment.DeploymentPhaseContext;
import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.as.server.deployment.DeploymentUnitProcessingException;
import org.jboss.as.server.deployment.DeploymentUnitProcessor;
import org.jboss.as.server.deployment.DeploymentUtils;
import org.jboss.as.server.moduleservice.ServiceModuleLoader;
import org.jboss.modules.Module;
import org.jboss.modules.ModuleIdentifier;
import org.jboss.modules.ModuleLoader;

/**
 * Deployment unit processor that will extract module dependencies from an archive.
 *
 * @author John E. Bailey
 */
public class ModuleDependencyProcessor implements DeploymentUnitProcessor {

    /**
     * Process the deployment root for module dependency information.
     *
     * @param phaseContext the deployment unit context
     * @throws DeploymentUnitProcessingException
     */
    public void deploy(DeploymentPhaseContext phaseContext) throws DeploymentUnitProcessingException {
        final DeploymentUnit deploymentUnit = phaseContext.getDeploymentUnit();

        List<ResourceRoot> allResourceRoots = DeploymentUtils.allResourceRoots(deploymentUnit);
        for (ResourceRoot resourceRoot : allResourceRoots) {
            final Manifest manifest = resourceRoot.getAttachment(Attachments.MANIFEST);
            if (manifest == null)
                continue;

            final ServiceModuleLoader deploymentModuleLoader = deploymentUnit.getAttachment(Attachments.SERVICE_MODULE_LOADER);

            final String dependencyString = manifest.getMainAttributes().getValue("Dependencies");
            if (dependencyString == null)
                return;
            final String[] dependencyDefs = dependencyString.split(",");
            for (String dependencyDef : dependencyDefs) {
                final String[] dependencyParts = dependencyDef.split(" ");
                final int dependencyPartsLength = dependencyParts.length;
                if (dependencyPartsLength == 0)
                    throw new RuntimeException("Invalid dependency: " + dependencyString);

                final ModuleIdentifier dependencyId = ModuleIdentifier.fromString(dependencyParts[0]);
                boolean export = parseOptionalExportParams(dependencyParts, "export");
                boolean optional = parseOptionalExportParams(dependencyParts, "optional");
                final ModuleLoader dependencyLoader;
                if (dependencyId.getName().startsWith("deployment.")) {
                    dependencyLoader = deploymentModuleLoader;
                } else {
                    dependencyLoader = Module.getSystemModuleLoader();
                }
                ModuleDependency dependency = new ModuleDependency(dependencyLoader, dependencyId, optional, export, false);
                phaseContext.getDeploymentUnit().addToAttachmentList(Attachments.MODULE_DEPENDENCIES, dependency);
            }
        }
    }

    public void undeploy(final DeploymentUnit context) {
    }

    private boolean parseOptionalExportParams(final String[] parts, final String expected) {
        if(parts.length > 1) {
            final String part = parts[1];
            if(expected.equals(part))
                return true;
        }
        if(parts.length > 2) {
            final String part = parts[2];
            if(expected.equals(part))
                return true;
        }
        return false;
    }
}
