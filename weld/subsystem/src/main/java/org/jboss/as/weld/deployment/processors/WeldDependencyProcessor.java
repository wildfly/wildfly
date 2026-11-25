/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.weld.deployment.processors;

import org.jboss.as.server.deployment.Attachments;
import org.jboss.as.server.deployment.DeploymentPhaseContext;
import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.as.server.deployment.DeploymentUnitProcessingException;
import org.jboss.as.server.deployment.DeploymentUnitProcessor;
import org.jboss.as.server.deployment.module.ModuleDependency;
import org.jboss.as.server.deployment.module.ModuleSpecification;
import org.jboss.as.weld._private.WeldDeploymentMarker;
import org.jboss.modules.Module;
import org.jboss.modules.ModuleLoader;
import org.jboss.modules.filter.PathFilters;

/**
 * Deployment processor which adds a module dependencies for modules needed for weld deployments.
 *
 * @author Stuart Douglas
 */
public class WeldDependencyProcessor implements DeploymentUnitProcessor {

    private static final String JAVAX_PERSISTENCE_API_ID = "jakarta.persistence.api";
    private static final String JBOSS_AS_WELD_ID = "org.jboss.as.weld";
    private static final String JBOSS_AS_WELD_EJB_ID = "org.jboss.as.weld.ejb";
    private static final String WELD_CORE_ID = "org.jboss.weld.core";
    private static final String WELD_API_ID = "org.jboss.weld.api";
    private static final String WELD_SPI_ID = "org.jboss.weld.spi";
    private static final String JAVAX_ENTERPRISE_API = "jakarta.enterprise.api";
    private static final String JAVAX_INJECT_API = "jakarta.inject.api";

    /**
     * Add dependencies for modules required for weld deployments, if managed weld configurations are attached to the deployment
     *
     */
    @Override
    public void deploy(DeploymentPhaseContext phaseContext) throws DeploymentUnitProcessingException {
        final DeploymentUnit deploymentUnit = phaseContext.getDeploymentUnit();
        final ModuleSpecification moduleSpecification = deploymentUnit.getAttachment(Attachments.MODULE_SPECIFICATION);

        final ModuleLoader moduleLoader = Module.getBootModuleLoader();
        addDependency(moduleSpecification, moduleLoader, JAVAX_ENTERPRISE_API);
        addDependency(moduleSpecification, moduleLoader, JAVAX_INJECT_API);

        if (!WeldDeploymentMarker.isPartOfWeldDeployment(deploymentUnit)) {
            return; // Skip if there are no beans.xml files in the deployment
        }
        addDependency(moduleSpecification, moduleLoader, JAVAX_PERSISTENCE_API_ID);
        addDependency(moduleSpecification, moduleLoader, WELD_CORE_ID);
        addDependency(moduleSpecification, moduleLoader, WELD_API_ID);
        addDependency(moduleSpecification, moduleLoader, WELD_SPI_ID);

        ModuleDependency weldSubsystemDependency = ModuleDependency.Builder.of(moduleLoader, JBOSS_AS_WELD_ID).build();
        weldSubsystemDependency.addImportFilter(PathFilters.getMetaInfFilter(), true);
        weldSubsystemDependency.addImportFilter(PathFilters.is("org/jboss/as/weld/injection"), true);
        weldSubsystemDependency.addImportFilter(PathFilters.acceptAll(), false);
        weldSubsystemDependency.addExportFilter(PathFilters.getMetaInfFilter(), true);
        moduleSpecification.addSystemDependency(weldSubsystemDependency);

        // Due to serialization of Jakarta Enterprise Beans
        ModuleDependency weldEjbDependency = ModuleDependency.Builder.of(moduleLoader, JBOSS_AS_WELD_EJB_ID).setOptional(true).build();
        weldEjbDependency.addImportFilter(PathFilters.is("org/jboss/as/weld/ejb"), true);
        weldEjbDependency.addImportFilter(PathFilters.acceptAll(), false);
        moduleSpecification.addSystemDependency(weldEjbDependency);
    }

    private void addDependency(ModuleSpecification moduleSpecification, ModuleLoader moduleLoader,
                               String moduleIdentifier) {
        addDependency(moduleSpecification, moduleLoader, moduleIdentifier, false);
    }

    private void addDependency(ModuleSpecification moduleSpecification, ModuleLoader moduleLoader,
            String moduleIdentifier, boolean optional) {
        moduleSpecification.addSystemDependency(ModuleDependency.Builder.of(moduleLoader, moduleIdentifier).setOptional(optional).setImportServices(true).build());
    }
}
