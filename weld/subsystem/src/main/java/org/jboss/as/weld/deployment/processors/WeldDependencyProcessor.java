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

package org.jboss.as.weld.deployment.processors;

import org.jboss.as.ee.weld.WeldDeploymentMarker;
import org.jboss.as.server.deployment.Attachments;
import org.jboss.as.server.deployment.DeploymentPhaseContext;
import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.as.server.deployment.DeploymentUnitProcessingException;
import org.jboss.as.server.deployment.DeploymentUnitProcessor;
import org.jboss.as.server.deployment.module.ModuleDependency;
import org.jboss.as.server.deployment.module.ModuleSpecification;
import org.jboss.modules.Module;
import org.jboss.modules.ModuleIdentifier;
import org.jboss.modules.ModuleLoader;
import org.jboss.modules.filter.PathFilters;

/**
 * Deployment processor which adds a module dependencies for modules needed for weld deployments.
 *
 * @author Stuart Douglas
 */
public class WeldDependencyProcessor implements DeploymentUnitProcessor {

    private static final ModuleIdentifier JAVAX_PERSISTENCE_API_ID = ModuleIdentifier.create("javax.persistence.api");
    private static final ModuleIdentifier JBOSS_AS_WELD_ID = ModuleIdentifier.create("org.jboss.as.weld");
    private static final ModuleIdentifier JBOSS_AS_WELD_EJB_ID = ModuleIdentifier.create("org.jboss.as.weld.ejb");
    private static final ModuleIdentifier WELD_CORE_ID = ModuleIdentifier.create("org.jboss.weld.core");
    private static final ModuleIdentifier WELD_PROBE_ID = ModuleIdentifier.create("org.jboss.weld.probe");
    private static final ModuleIdentifier WELD_API_ID = ModuleIdentifier.create("org.jboss.weld.api");
    private static final ModuleIdentifier WELD_SPI_ID = ModuleIdentifier.create("org.jboss.weld.spi");
    private static final ModuleIdentifier JAVAX_ENTERPRISE_API = ModuleIdentifier.create("javax.enterprise.api");
    private static final ModuleIdentifier JAVAX_INJECT_API = ModuleIdentifier.create("javax.inject.api");

    /**
     * Add dependencies for modules required for weld deployments, if managed weld configurations are attached to the deployment
     *
     */
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
        addDependency(moduleSpecification, moduleLoader, WELD_PROBE_ID, true);
        addDependency(moduleSpecification, moduleLoader, WELD_API_ID);
        addDependency(moduleSpecification, moduleLoader, WELD_SPI_ID);

        ModuleDependency weldSubsystemDependency = new ModuleDependency(moduleLoader, JBOSS_AS_WELD_ID, false, false, false, false);
        weldSubsystemDependency.addImportFilter(PathFilters.getMetaInfFilter(), true);
        weldSubsystemDependency.addImportFilter(PathFilters.is("org/jboss/as/weld/injection"), true);
        weldSubsystemDependency.addImportFilter(PathFilters.acceptAll(), false);
        weldSubsystemDependency.addExportFilter(PathFilters.getMetaInfFilter(), true);
        moduleSpecification.addSystemDependency(weldSubsystemDependency);

        // Due to serialization of EJBs
        ModuleDependency weldEjbDependency = new ModuleDependency(moduleLoader, JBOSS_AS_WELD_EJB_ID, true, false, false, false);
        weldEjbDependency.addImportFilter(PathFilters.is("org/jboss/as/weld/ejb"), true);
        weldEjbDependency.addImportFilter(PathFilters.acceptAll(), false);
        moduleSpecification.addSystemDependency(weldEjbDependency);
    }

    private void addDependency(ModuleSpecification moduleSpecification, ModuleLoader moduleLoader,
                               ModuleIdentifier moduleIdentifier) {
        addDependency(moduleSpecification, moduleLoader, moduleIdentifier, false);
    }

    private void addDependency(ModuleSpecification moduleSpecification, ModuleLoader moduleLoader,
            ModuleIdentifier moduleIdentifier, boolean optional) {
        moduleSpecification.addSystemDependency(new ModuleDependency(moduleLoader, moduleIdentifier, optional, false, true, false));
    }

    @Override
    public void undeploy(DeploymentUnit context) {

    }
}
