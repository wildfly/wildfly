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
import org.jboss.modules.filter.PathFilters;

/**
 * Deployment unit processor that will extract module dependencies from an and attach them.
 *
 * @author John E. Bailey
 * @author <a href="mailto:ropalka@redhat.com">Richard Opalka</a>
 * @author Stuart Douglas
 */
public final class ManifestDependencyProcessor implements DeploymentUnitProcessor {

    private static final String DEPENDENCIES_ATTR = "Dependencies";
    private static final String EXPORT_PARAM = "export";
    private static final String OPTIONAL_PARAM = "optional";
    private static final String SERVICES_PARAM = "services";
    private static final String ANNOTATIONS_PARAM = "annotations";
    private static final String META_INF = "meta-inf";

    /**
     * Process the deployment root for module dependency information.
     *
     * @param phaseContext the deployment unit context
     * @throws org.jboss.as.server.deployment.DeploymentUnitProcessingException
     */
    public void deploy(final DeploymentPhaseContext phaseContext) throws DeploymentUnitProcessingException {
        final DeploymentUnit deploymentUnit = phaseContext.getDeploymentUnit();
        final ServiceModuleLoader deploymentModuleLoader = deploymentUnit.getAttachment(Attachments.SERVICE_MODULE_LOADER);
        final List<ResourceRoot> allResourceRoots = DeploymentUtils.allResourceRoots(deploymentUnit);

        for (final ResourceRoot resourceRoot : allResourceRoots) {
            final Manifest manifest = resourceRoot.getAttachment(Attachments.MANIFEST);
            if (manifest == null)
                continue;

            final String dependencyString = manifest.getMainAttributes().getValue(DEPENDENCIES_ATTR);
            if (dependencyString == null)
                continue;

            final String[] dependencyDefs = dependencyString.split(",");
            for (final String dependencyDef : dependencyDefs) {
                final String[] dependencyParts = dependencyDef.trim().split(" ");
                if (dependencyParts.length == 0) {
                    throw new RuntimeException("Invalid dependency: " + dependencyString);
                }

                final ModuleIdentifier dependencyId = ModuleIdentifier.fromString(dependencyParts[0]);
                final boolean export = containsParam(dependencyParts, EXPORT_PARAM);
                final boolean optional = containsParam(dependencyParts, OPTIONAL_PARAM);
                final boolean services = containsParam(dependencyParts, SERVICES_PARAM);
                final boolean annotations = containsParam(dependencyParts, ANNOTATIONS_PARAM);
                final boolean metaInf = containsParam(dependencyParts, META_INF);
                final ModuleLoader dependencyLoader;
                if (dependencyId.getName().startsWith("deployment.")) {
                    dependencyLoader = deploymentModuleLoader;
                } else {
                    dependencyLoader = Module.getBootModuleLoader();
                }
                if(annotations) {
                    deploymentUnit.addToAttachmentList(Attachments.ADDITIONAL_ANNOTATION_INDEXES, dependencyId);
                }

                final ModuleDependency dependency = new ModuleDependency(dependencyLoader, dependencyId, optional, export, services);
                if(metaInf) {
                    dependency.addImportFilter(PathFilters.getMetaInfSubdirectoriesFilter(), true);
                }
                deploymentUnit.addToAttachmentList(Attachments.MANIFEST_DEPENDENCIES, dependency);
            }
        }

    }

    public void undeploy(final DeploymentUnit context) {
    }

    private boolean containsParam(final String[] parts, final String expected) {
        if (parts.length > 1) {
            for (int i = 1; i < parts.length; i++) {
                if (expected.equals(parts[i])) {
                    return true;
                }
            }
        }
        return false;
    }
}
