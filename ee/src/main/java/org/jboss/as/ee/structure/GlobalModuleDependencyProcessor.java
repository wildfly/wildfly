/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.ee.structure;

import java.util.List;

import org.jboss.as.server.deployment.Attachments;
import org.jboss.as.server.deployment.DeploymentPhaseContext;
import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.as.server.deployment.DeploymentUnitProcessingException;
import org.jboss.as.server.deployment.DeploymentUnitProcessor;
import org.jboss.as.server.deployment.module.ModuleDependency;
import org.jboss.as.server.deployment.module.ModuleSpecification;
import org.jboss.modules.Module;
import org.jboss.modules.filter.PathFilters;

import static org.jboss.as.ee.subsystem.GlobalModulesDefinition.GlobalModule;

/**
 * Dependency processor that adds modules defined in the global-modules section of
 * the configuration to all deployments.
 *
 * @author Stuart Douglas
 */
public class GlobalModuleDependencyProcessor implements DeploymentUnitProcessor {

    private volatile List<GlobalModule> globalModules;

    public GlobalModuleDependencyProcessor() {
    }

    @Override
    public void deploy(final DeploymentPhaseContext phaseContext) throws DeploymentUnitProcessingException {
        final DeploymentUnit deploymentUnit = phaseContext.getDeploymentUnit();
        final ModuleSpecification moduleSpecification = deploymentUnit.getAttachment(Attachments.MODULE_SPECIFICATION);
        final List<GlobalModule> globalMods = this.globalModules;

            for (final GlobalModule module : globalMods) {
                final ModuleDependency dependency = ModuleDependency.Builder.of(Module.getBootModuleLoader(), module.getModuleName()).setImportServices(module.isServices()).build();

                if (module.isMetaInf()) {
                    dependency.addImportFilter(PathFilters.getMetaInfSubdirectoriesFilter(), true);
                    dependency.addImportFilter(PathFilters.getMetaInfFilter(), true);
                }

                if(module.isAnnotations()) {
                    deploymentUnit.addToAttachmentList(Attachments.ADDITIONAL_INDEX_MODULES, module.getModuleName());
                }

                moduleSpecification.addSystemDependency(dependency);
            }
    }

    /**
     * Set the global modules configuration for the container.
     * @param globalModules a fully resolved (i.e. with expressions resolved and default values set) global modules configuration
     */
    public void setGlobalModules(final List<GlobalModule> globalModules) {
        this.globalModules = globalModules;
    }
}
