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

import org.jboss.as.server.deployment.Attachments;
import org.jboss.as.server.deployment.DeploymentPhaseContext;
import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.as.server.deployment.DeploymentUnitProcessingException;
import org.jboss.as.server.deployment.DeploymentUnitProcessor;
import org.jboss.as.server.deployment.module.ModuleDependency;
import org.jboss.as.weld.WeldDeploymentMarker;
import org.jboss.modules.Module;
import org.jboss.modules.ModuleIdentifier;
import org.jboss.modules.ModuleLoader;

/**
 * Deployment processor which adds a module dependencies for modules needed for weld deployments.
 *
 * @author Stuart Douglas
 */
public class WeldDependencyProcessor implements DeploymentUnitProcessor {

    private static ModuleIdentifier JAVAX_PERSISTENCE_API_ID = ModuleIdentifier.create("javax.persistence.api");
    private static ModuleIdentifier JAVAEE_API_ID = ModuleIdentifier.create("javaee.api");
    private static final ModuleIdentifier JAVASSIST_ID = ModuleIdentifier.create("org.javassist");
    private static ModuleIdentifier JBOSS_INTERCEPTOR_ID = ModuleIdentifier.create("org.jboss.interceptor");
    private static ModuleIdentifier JBOSS_AS_WELD_ID = ModuleIdentifier.create("org.jboss.as.weld");
    private static ModuleIdentifier JBOSS_LOGGING_ID = ModuleIdentifier.create("org.jboss.logging");
    private static ModuleIdentifier WELD_CORE_ID = ModuleIdentifier.create("org.jboss.weld.core");
    private static ModuleIdentifier WELD_API_ID = ModuleIdentifier.create("org.jboss.weld.api");
    private static ModuleIdentifier WELD_SPI_ID = ModuleIdentifier.create("org.jboss.weld.spi");

    /**
     * Add dependencies for modules required for weld deployments, if managed weld configurations are attached to the deployment
     *
     */
    public void deploy(DeploymentPhaseContext phaseContext) throws DeploymentUnitProcessingException {
        final DeploymentUnit deploymentUnit = phaseContext.getDeploymentUnit();
        if (!WeldDeploymentMarker.isWeldDeployment(deploymentUnit)) {
            return; // Skip if there are no beans.xml files in the deployment
        }
        final ModuleLoader moduleLoader = Module.getSystemModuleLoader();
        addDepdenency(deploymentUnit, moduleLoader, JAVAX_PERSISTENCE_API_ID);
        addDepdenency(deploymentUnit, moduleLoader, JAVAEE_API_ID);
        addDepdenency(deploymentUnit, moduleLoader, JBOSS_AS_WELD_ID);
        addDepdenency(deploymentUnit, moduleLoader, JBOSS_INTERCEPTOR_ID);
        addDepdenency(deploymentUnit, moduleLoader, JBOSS_LOGGING_ID);
        addDepdenency(deploymentUnit, moduleLoader, JAVASSIST_ID);
        addDepdenency(deploymentUnit, moduleLoader, WELD_CORE_ID);
        addDepdenency(deploymentUnit, moduleLoader, WELD_API_ID);
        addDepdenency(deploymentUnit, moduleLoader, WELD_SPI_ID);
    }

    private void addDepdenency(DeploymentUnit deploymentUnit, ModuleLoader moduleLoader, ModuleIdentifier moduleIdentifier) {
        deploymentUnit.addToAttachmentList(Attachments.MODULE_DEPENDENCIES, new ModuleDependency(moduleLoader,
                moduleIdentifier, false, false, false));
    }

    @Override
    public void undeploy(DeploymentUnit context) {

    }
}
