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

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.jboss.as.server.deployment.AttachmentKey;
import org.jboss.as.server.deployment.Attachments;
import org.jboss.as.server.deployment.DeploymentPhaseContext;
import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.as.server.deployment.DeploymentUnitProcessingException;
import org.jboss.as.server.deployment.DeploymentUnitProcessor;
import org.jboss.as.server.deployment.SubDeploymentMarker;
import org.jboss.as.server.moduleservice.ModuleLoadService;
import org.jboss.as.server.moduleservice.ServiceModuleLoader;
import org.jboss.modules.DependencySpec;
import org.jboss.modules.ModuleIdentifier;
import org.jboss.modules.ModuleSpec;
import org.jboss.modules.ResourceLoaderSpec;
import org.jboss.modules.filter.MultiplePathFilterBuilder;
import org.jboss.modules.filter.PathFilter;
import org.jboss.modules.filter.PathFilters;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ValueService;
import org.jboss.msc.service.ServiceController.Mode;
import org.jboss.msc.value.ImmediateValue;

/**
 * Processor responsible for creating the module spec service for this deployment. Once the module spec service is created the
 * module can be loaded by {@link ServiceModuleLoader}.
 *
 * @author John Bailey
 * @author Stuart Douglas
 */
public class ModuleSpecProcessor implements DeploymentUnitProcessor {

    private static final AttachmentKey<Boolean> MARKER = AttachmentKey.create(Boolean.class);

    public void deploy(DeploymentPhaseContext phaseContext) throws DeploymentUnitProcessingException {
        final DeploymentUnit deploymentUnit = phaseContext.getDeploymentUnit();

        if (deploymentUnit.getAttachment(MARKER) != null) {
            return;
        }
        deploymentUnit.putAttachment(MARKER, true);

        // Don't create a ModuleSpec for OSGi deployments
        if (deploymentUnit.hasAttachment(Attachments.OSGI_MANIFEST)) {
            return;
        }

        final ResourceRoot mainRoot = deploymentUnit.getAttachment(Attachments.DEPLOYMENT_ROOT);
        final List<ResourceRoot> additionalRoots = deploymentUnit.getAttachment(Attachments.RESOURCE_ROOTS);
        if (mainRoot == null) {
            return;
        }
        List<ResourceRoot> resourceRoots = new ArrayList<ResourceRoot>();
        // Add internal resource roots
        if (ModuleRootMarker.isModuleRoot(mainRoot)) {
            resourceRoots.add(mainRoot);
        }
        if (additionalRoots != null)
            for (ResourceRoot additionalRoot : additionalRoots) {
                if (ModuleRootMarker.isModuleRoot(additionalRoot) && !SubDeploymentMarker.isSubDeployment(additionalRoot)) {
                    resourceRoots.add(additionalRoot);
                }
            }

        final ModuleSpecification moduleSpecification = deploymentUnit.getAttachment(Attachments.MODULE_SPECIFICATION);

        final ModuleIdentifier moduleIdentifier = deploymentUnit.getAttachment(Attachments.MODULE_IDENTIFIER);
        if (moduleIdentifier == null) {
            throw new DeploymentUnitProcessingException("No Module Identifier attached to deployment "
                    + deploymentUnit.getName());
        }

        // create the module servce and set it to attach to the deployment in the next phase
        ServiceName moduleServiceName = createModuleService(phaseContext, deploymentUnit, resourceRoots, moduleSpecification,
                moduleIdentifier);
        phaseContext.addDeploymentDependency(moduleServiceName, Attachments.MODULE);

        final List<AdditionalModuleSpecification> additionalModules = deploymentUnit.getAttachment(Attachments.ADDITIONAL_MODULES);
        if (additionalModules == null) {
            return;
        }
        for (AdditionalModuleSpecification module : additionalModules) {

            ServiceName additionalModuleServiceName = createModuleService(phaseContext, deploymentUnit, module
                    .getResourceRoots(), module, module.getModuleIdentifier());
            phaseContext.addToAttachmentList(Attachments.NEXT_PHASE_DEPS, additionalModuleServiceName);
        }
    }

    private ServiceName createModuleService(DeploymentPhaseContext phaseContext, final DeploymentUnit deploymentUnit,
            final List<ResourceRoot> resourceRoots, final ModuleSpecification moduleSpecification,
            final ModuleIdentifier moduleIdentifier) throws DeploymentUnitProcessingException {
        final ModuleSpec.Builder specBuilder = ModuleSpec.build(moduleIdentifier);
        final List<ModuleDependency> dependencies = moduleSpecification.getDependencies();

        for (ResourceRoot resourceRoot : resourceRoots) {
            addResourceRoot(specBuilder, resourceRoot);
        }
        final boolean childFirst;
        if (moduleSpecification.getChildFirst() == null) {
            childFirst = false;
        } else {
            childFirst = moduleSpecification.getChildFirst();
        }

        if (childFirst) {
            specBuilder.addDependency(DependencySpec.createLocalDependencySpec());
        }
        if (dependencies != null)
            for (ModuleDependency dependency : dependencies) {
                final List<FilterSpecification> importFilters = dependency.getImportFilters();
                final List<FilterSpecification> exportFilters = dependency.getExportFilters();
                final PathFilter importFilter;
                final PathFilter exportFilter;
                final MultiplePathFilterBuilder importBuilder = PathFilters.multiplePathFilterBuilder(true);
                for (FilterSpecification filter : importFilters) {
                    importBuilder.addFilter(filter.getPathFilter(), filter.isInclude());
                }
                if (dependency.isImportServices()) {
                    importBuilder.addFilter(PathFilters.getMetaInfServicesFilter(), true);
                }
                importBuilder.addFilter(PathFilters.getMetaInfSubdirectoriesFilter(), false);
                importBuilder.addFilter(PathFilters.getMetaInfFilter(), false);
                importFilter = importBuilder.create();
                if (exportFilters.isEmpty()) {
                    if (dependency.isExport()) {
                        exportFilter = PathFilters.acceptAll();
                    } else {
                        exportFilter = PathFilters.rejectAll();
                    }
                } else {
                    final MultiplePathFilterBuilder exportBuilder = PathFilters
                            .multiplePathFilterBuilder(dependency.isExport());
                    for (FilterSpecification filter : exportFilters) {
                        exportBuilder.addFilter(filter.getPathFilter(), filter.isInclude());
                    }
                    exportFilter = exportBuilder.create();
                }
                DependencySpec depSpec = DependencySpec.createModuleDependencySpec(importFilter, exportFilter, dependency
                        .getModuleLoader(), dependency.getIdentifier(), dependency.isOptional());
                specBuilder.addDependency(depSpec);

                final String depName = dependency.getIdentifier().getName();
                if (depName.startsWith(ServiceModuleLoader.MODULE_PREFIX)) {
                    phaseContext.addToAttachmentList(Attachments.NEXT_PHASE_DEPS, ServiceModuleLoader
                            .moduleSpecServiceName(dependency.getIdentifier()));
                }
            }
        if (!childFirst) {
            specBuilder.addDependency(DependencySpec.createLocalDependencySpec());
        }
        final ModuleSpec moduleSpec = specBuilder.create();
        final ServiceName moduleSpecServiceName = ServiceModuleLoader.moduleSpecServiceName(moduleIdentifier);
        final ValueService<ModuleSpec> moduleSpecService = new ValueService<ModuleSpec>(new ImmediateValue<ModuleSpec>(
                moduleSpec));
        phaseContext.getServiceTarget().addService(moduleSpecServiceName, moduleSpecService).addDependencies(
                deploymentUnit.getServiceName()).addDependencies(phaseContext.getPhaseServiceName()).setInitialMode(
                Mode.ON_DEMAND).install();
        return ModuleLoadService.install(phaseContext.getServiceTarget(), moduleIdentifier, dependencies);
    }

    private static void addResourceRoot(final ModuleSpec.Builder specBuilder, final ResourceRoot resource)
            throws DeploymentUnitProcessingException {
        try {
            specBuilder.addResourceRoot(ResourceLoaderSpec.createResourceLoaderSpec(new VFSResourceLoader(resource
                    .getRootName(), resource.getRoot())));
        } catch (IOException e) {
            throw new DeploymentUnitProcessingException("Failed to create VFSResourceLoader for root ["
                    + resource.getRootName() + "]", e);
        }
    }

    public void undeploy(DeploymentUnit context) {
        context.removeAttachment(MARKER);
    }

}
