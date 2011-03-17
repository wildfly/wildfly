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
package org.jboss.as.arquillian.service;

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

public class ArquillianDependencyProcessor implements DeploymentUnitProcessor {

    private static ModuleIdentifier ARQUILLIAN_JUNIT_ID = ModuleIdentifier.create("org.jboss.arquillian.junit");
    private static ModuleIdentifier SHRINKWRAP_ID = ModuleIdentifier.create("org.jboss.shrinkwrap.api");
    private static ModuleIdentifier JUNIT_ID = ModuleIdentifier.create("org.junit");
    @Override
    public void deploy(DeploymentPhaseContext phaseContext) throws DeploymentUnitProcessingException {
        final DeploymentUnit deploymentUnit = phaseContext.getDeploymentUnit();
        final ModuleLoader moduleLoader = Module.getBootModuleLoader();
        final ModuleSpecification moduleSpecification = deploymentUnit.getAttachment(Attachments.MODULE_SPECIFICATION);
        if (deploymentUnit.getAttachment(ArquillianConfig.KEY) != null) {
            addDepdenency(moduleSpecification, moduleLoader, ARQUILLIAN_JUNIT_ID);
            addDepdenency(moduleSpecification, moduleLoader, SHRINKWRAP_ID);
            addDepdenency(moduleSpecification, moduleLoader, JUNIT_ID);
        }
    }

    private void addDepdenency(ModuleSpecification moduleSpecification, ModuleLoader moduleLoader,
            ModuleIdentifier moduleIdentifier) {
        moduleSpecification.addDependency(new ModuleDependency(moduleLoader, moduleIdentifier, false, false, false));
    }

    @Override
    public void undeploy(DeploymentUnit context) {

    }

}
