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
import java.util.Collections;
import java.util.List;

import org.jboss.as.server.ServerLogger;
import org.jboss.as.server.ServerMessages;
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
import org.jboss.msc.service.ServiceController.Mode;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ValueService;
import org.jboss.msc.value.ImmediateValue;

/**
 * Processor responsible for creating the module spec service for this deployment. Once the module spec service is created the
 * module can be loaded by {@link ServiceModuleLoader}.
 *
 * @author John Bailey
 * @author Stuart Douglas
 * @author Marius Bogoevici
 * @author Thomas.Diesler@jboss.com
 */
public class ModuleSpecProcessor implements DeploymentUnitProcessor {

    private static final ServerLogger logger = ServerLogger.DEPLOYMENT_LOGGER;

    @Override
    public void deploy(final DeploymentPhaseContext phaseContext) throws DeploymentUnitProcessingException {

        final DeploymentUnit deploymentUnit = phaseContext.getDeploymentUnit();
        if (deploymentUnit.hasAttachment(Attachments.MODULE))
            return;

        // No {@link ModuleSpec} creation for OSGi deployments
        if (deploymentUnit.hasAttachment(Attachments.OSGI_MANIFEST))
            return;

        deployModuleSpec(phaseContext);
    }

    @Override
    public void undeploy(final DeploymentUnit deploymentUnit) {
    }

    private void deployModuleSpec(final DeploymentPhaseContext phaseContext) throws DeploymentUnitProcessingException {

        final DeploymentUnit deploymentUnit = phaseContext.getDeploymentUnit();
        final DeploymentUnit topLevelDeployment = deploymentUnit.getParent() == null ? deploymentUnit : deploymentUnit.getParent();

        final ResourceRoot mainRoot = deploymentUnit.getAttachment(Attachments.DEPLOYMENT_ROOT);
        if (mainRoot == null)
            return;

        // Add internal resource roots
        final ModuleSpecification moduleSpec = deploymentUnit.getAttachment(Attachments.MODULE_SPECIFICATION);
        final List<ResourceRoot> resourceRoots = new ArrayList<ResourceRoot>();
        if (ModuleRootMarker.isModuleRoot(mainRoot)) {
            resourceRoots.add(mainRoot);
        }
        final List<ResourceRoot> additionalRoots = deploymentUnit.getAttachmentList(Attachments.RESOURCE_ROOTS);
        for (final ResourceRoot additionalRoot : additionalRoots) {
            if (ModuleRootMarker.isModuleRoot(additionalRoot) && !SubDeploymentMarker.isSubDeployment(additionalRoot)) {
                resourceRoots.add(additionalRoot);
            }
        }

        final ModuleIdentifier moduleIdentifier = deploymentUnit.getAttachment(Attachments.MODULE_IDENTIFIER);
        if (moduleIdentifier == null) {
            throw ServerMessages.MESSAGES.noModuleIdentifier(deploymentUnit.getName());
        }

        final List<AdditionalModuleSpecification> additionalModules = topLevelDeployment.getAttachmentList(Attachments.ADDITIONAL_MODULES);

        // create the module service and set it to attach to the deployment in the next phase
        final ServiceName moduleServiceName = createModuleService(phaseContext, deploymentUnit, resourceRoots, moduleSpec, moduleIdentifier, additionalModules);
        phaseContext.addDeploymentDependency(moduleServiceName, Attachments.MODULE);

        for (final DeploymentUnit subDeployment : deploymentUnit.getAttachmentList(Attachments.SUB_DEPLOYMENTS)) {
            ModuleIdentifier moduleId = subDeployment.getAttachment(Attachments.MODULE_IDENTIFIER);
            if (moduleId != null) {
                phaseContext.addToAttachmentList(Attachments.NEXT_PHASE_DEPS, ServiceModuleLoader.moduleSpecServiceName(moduleId));
            }
        }

        if (deploymentUnit.getParent() != null) {
            //they have already been added by the parent
            return;
        }
        for (final AdditionalModuleSpecification module : additionalModules) {
            addSystemDependencies(moduleSpec, module);
            List<ResourceRoot> roots = module.getResourceRoots();
            ServiceName serviceName = createModuleService(phaseContext, deploymentUnit, roots, module, module.getModuleIdentifier(), additionalModules);
            phaseContext.addToAttachmentList(Attachments.NEXT_PHASE_DEPS, serviceName);
        }
    }

    /**
     * Gives any additional modules the same system dependencies as the primary module.
     * <p/>
     * This makes sure they can access all API classes etc.
     *
     * @param moduleSpecification The primary module spec
     * @param module              The additional module
     */
    private void addSystemDependencies(final ModuleSpecification moduleSpecification, final AdditionalModuleSpecification module) {
        module.addSystemDependencies(moduleSpecification.getSystemDependencies());
    }


    private ServiceName createModuleService(final DeploymentPhaseContext phaseContext, final DeploymentUnit deploymentUnit,
                                            final List<ResourceRoot> resourceRoots, final ModuleSpecification moduleSpecification,
                                            final ModuleIdentifier moduleIdentifier, List<AdditionalModuleSpecification> additionalModuleSpecifications) throws DeploymentUnitProcessingException {
        logger.debug("Creating module: " + moduleIdentifier);
        final ModuleSpec.Builder specBuilder = ModuleSpec.build(moduleIdentifier);
        for (final DependencySpec dep : moduleSpecification.getModuleSystemDependencies()) {
            specBuilder.addDependency(dep);
        }
        final List<ModuleDependency> dependencies = moduleSpecification.getSystemDependencies();
        final List<ModuleDependency> localDependencies = moduleSpecification.getLocalDependencies();
        final List<ModuleDependency> userDependencies = moduleSpecification.getUserDependencies();

        installAliases(moduleSpecification, moduleIdentifier, deploymentUnit, phaseContext);

        // add additional resource loaders first
        for (final ResourceLoaderSpec resourceLoaderSpec : moduleSpecification.getResourceLoaders()) {
            logger.debug("Adding resource loader " + resourceLoaderSpec + " to module " + moduleIdentifier);
            specBuilder.addResourceRoot(resourceLoaderSpec);
        }

        for (final ResourceRoot resourceRoot : resourceRoots) {
            logger.debug("Adding resource " + resourceRoot.getRoot() + " to module " + moduleIdentifier);
            addResourceRoot(specBuilder, resourceRoot);
        }

        createDependencies(specBuilder, dependencies, false);
        createDependencies(specBuilder, userDependencies, false);

        if (moduleSpecification.isLocalLast()) {
            createDependencies(specBuilder, localDependencies, moduleSpecification.isLocalDependenciesTransitive());
            specBuilder.addDependency(DependencySpec.createLocalDependencySpec());
        } else {
            specBuilder.addDependency(DependencySpec.createLocalDependencySpec());
            createDependencies(specBuilder, localDependencies, moduleSpecification.isLocalDependenciesTransitive());
        }

        final DelegatingClassFileTransformer delegatingClassFileTransformer = new DelegatingClassFileTransformer();
        specBuilder.setClassFileTransformer(delegatingClassFileTransformer);
        deploymentUnit.putAttachment(DelegatingClassFileTransformer.ATTACHMENT_KEY, delegatingClassFileTransformer);
        final ModuleSpec moduleSpec = specBuilder.create();
        final ServiceName moduleSpecServiceName = ServiceModuleLoader.moduleSpecServiceName(moduleIdentifier);
        final ValueService<ModuleSpec> moduleSpecService = new ValueService<ModuleSpec>(new ImmediateValue<ModuleSpec>(
                moduleSpec));
        phaseContext.getServiceTarget().addService(moduleSpecServiceName, moduleSpecService).addDependencies(
                deploymentUnit.getServiceName()).addDependencies(phaseContext.getPhaseServiceName()).setInitialMode(
                Mode.ON_DEMAND).install();

        final List<ModuleDependency> allDependencies = new ArrayList<ModuleDependency>();
        allDependencies.addAll(dependencies);
        allDependencies.addAll(localDependencies);
        allDependencies.addAll(userDependencies);


        //this is not that nice, but there is a situation where additional modules cause problems
        //due to re-exporting of dependencies, as required by the Class-Path transitivity requirement
        //we hack around this by simply making every module load service dependent on all the additional
        //modules
        //this will mostly resolve the issue, except in cases where another deployment is depending on a
        //module that suffers from this problem (i.e. if you have a cross-deployment dependency, and the
        //dependency you are targeting exports another module, it is possible for a race condition to occur
        //where a module is loaded before its dependency is ready. This should be a corner case and can
        //be worked around using whole deployment inter-deployment dependencies, rather than just a module
        //dependency
        //for more details see AS7-5971
        for (AdditionalModuleSpecification module : additionalModuleSpecifications) {
            allDependencies.add(new ModuleDependency(null, module.getModuleIdentifier(), false, false, false, false));
        }

        return ModuleLoadService.install(phaseContext.getServiceTarget(), moduleIdentifier, allDependencies);
    }

    private void installAliases(final ModuleSpecification moduleSpecification, final ModuleIdentifier moduleIdentifier, final DeploymentUnit deploymentUnit, final DeploymentPhaseContext phaseContext) {
        for (final ModuleIdentifier alias : moduleSpecification.getAliases()) {
            final ServiceName moduleSpecServiceName = ServiceModuleLoader.moduleSpecServiceName(alias);
            final ModuleSpec spec = ModuleSpec.buildAlias(alias, moduleIdentifier).create();
            final ValueService<ModuleSpec> moduleSpecService = new ValueService<ModuleSpec>(new ImmediateValue<ModuleSpec>(spec));
            phaseContext.getServiceTarget().addService(moduleSpecServiceName, moduleSpecService).addDependencies(
                    deploymentUnit.getServiceName()).addDependencies(phaseContext.getPhaseServiceName()).setInitialMode(
                    Mode.ON_DEMAND).install();
            ModuleLoadService.installService(phaseContext.getServiceTarget(), alias, Collections.singletonList(moduleIdentifier));
        }
    }

    private void createDependencies(final ModuleSpec.Builder specBuilder, final List<ModuleDependency> apiDependencies, final boolean requireTransitive) {
        if (apiDependencies != null) {
            for (final ModuleDependency dependency : apiDependencies) {
                final boolean export = requireTransitive ? true : dependency.isExport();
                final List<FilterSpecification> importFilters = dependency.getImportFilters();
                final List<FilterSpecification> exportFilters = dependency.getExportFilters();
                final PathFilter importFilter;
                final PathFilter exportFilter;
                final MultiplePathFilterBuilder importBuilder = PathFilters.multiplePathFilterBuilder(true);
                for (final FilterSpecification filter : importFilters) {
                    importBuilder.addFilter(filter.getPathFilter(), filter.isInclude());
                }
                if (dependency.isImportServices()) {
                    importBuilder.addFilter(PathFilters.getMetaInfServicesFilter(), true);
                }
                importBuilder.addFilter(PathFilters.getMetaInfSubdirectoriesFilter(), false);
                importBuilder.addFilter(PathFilters.getMetaInfFilter(), false);
                importFilter = importBuilder.create();
                if (exportFilters.isEmpty()) {
                    if (export) {
                        exportFilter = PathFilters.acceptAll();
                    } else {
                        exportFilter = PathFilters.rejectAll();
                    }
                } else {
                    final MultiplePathFilterBuilder exportBuilder = PathFilters
                            .multiplePathFilterBuilder(export);
                    for (final FilterSpecification filter : exportFilters) {
                        exportBuilder.addFilter(filter.getPathFilter(), filter.isInclude());
                    }
                    exportFilter = exportBuilder.create();
                }
                final DependencySpec depSpec = DependencySpec.createModuleDependencySpec(importFilter, exportFilter, dependency
                        .getModuleLoader(), dependency.getIdentifier(), dependency.isOptional());
                specBuilder.addDependency(depSpec);
                logger.debug("Adding dependency " + dependency + " to module " + specBuilder.getIdentifier());
            }
        }
    }

    private void addResourceRoot(final ModuleSpec.Builder specBuilder, final ResourceRoot resource)
            throws DeploymentUnitProcessingException {
        try {
            if (resource.getExportFilters().isEmpty()) {
                specBuilder.addResourceRoot(ResourceLoaderSpec.createResourceLoaderSpec(new VFSResourceLoader(resource
                        .getRootName(), resource.getRoot(), resource.isUsePhysicalCodeSource())));
            } else {
                final MultiplePathFilterBuilder filterBuilder = PathFilters.multiplePathFilterBuilder(true);
                for (final FilterSpecification filter : resource.getExportFilters()) {
                    filterBuilder.addFilter(filter.getPathFilter(), filter.isInclude());
                }
                specBuilder.addResourceRoot(ResourceLoaderSpec.createResourceLoaderSpec(new VFSResourceLoader(resource
                        .getRootName(), resource.getRoot(), resource.isUsePhysicalCodeSource()), filterBuilder.create()));
            }
        } catch (IOException e) {
            throw ServerMessages.MESSAGES.failedToCreateVFSResourceLoader(resource.getRootName(), e);
        }
    }

}
