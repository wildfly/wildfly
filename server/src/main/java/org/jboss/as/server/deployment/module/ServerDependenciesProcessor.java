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

import org.jboss.as.server.logging.ServerLogger;
import org.jboss.as.server.deployment.Attachments;
import org.jboss.as.server.deployment.DeploymentPhaseContext;
import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.as.server.deployment.DeploymentUnitProcessingException;
import org.jboss.as.server.deployment.DeploymentUnitProcessor;
import org.jboss.modules.Module;
import org.jboss.modules.ModuleIdentifier;
import org.jboss.modules.ModuleLoadException;
import org.jboss.modules.ModuleLoader;

/**
 * DUP thats adds dependencies that are available to all deployments
 *
 * @author Stuart Douglas
 * @author Thomas.Diesler@jboss.com
 */
public class ServerDependenciesProcessor implements DeploymentUnitProcessor {

    private static ModuleIdentifier[] DEFAULT_MODULES = new ModuleIdentifier[] {
        ModuleIdentifier.create("javax.api"),
        ModuleIdentifier.create("org.jboss.vfs"),
    };

    private static ModuleIdentifier[] DEFAULT_MODULES_WITH_SERVICE_IMPORTS = new ModuleIdentifier[] {
            // The Sun JDK is added as a dependency with service import = true since it's required for JSR-223 Javascript engine to be available.
            // @see https://issues.jboss.org/browse/AS7-1116 and https://issues.jboss.org/browse/WFLY-1373
            ModuleIdentifier.create("sun.jdk"),
            ModuleIdentifier.create("ibm.jdk"),
    };

    @Override
    public void deploy(final DeploymentPhaseContext phaseContext) throws DeploymentUnitProcessingException {
        final DeploymentUnit deploymentUnit = phaseContext.getDeploymentUnit();
        final ModuleSpecification moduleSpecification = deploymentUnit.getAttachment(Attachments.MODULE_SPECIFICATION);
        final ModuleLoader moduleLoader = Module.getBootModuleLoader();
        // add module dependency (these do not require services to be imported)
        for (ModuleIdentifier moduleId : DEFAULT_MODULES) {
            try {
                moduleLoader.loadModule(moduleId);
                moduleSpecification.addSystemDependency(new ModuleDependency(moduleLoader, moduleId, false, false, false, false));
            } catch (ModuleLoadException ex) {
                ServerLogger.ROOT_LOGGER.debugf("Module not found: %s", moduleId);
            }
        }
        // add module dependency with importServices = true
        for (ModuleIdentifier moduleId : DEFAULT_MODULES_WITH_SERVICE_IMPORTS) {
            try {
                moduleLoader.loadModule(moduleId);
                moduleSpecification.addSystemDependency(new ModuleDependency(moduleLoader, moduleId, false, false, true, false));
            } catch (ModuleLoadException ex) {
                ServerLogger.ROOT_LOGGER.debugf("Module not found: %s", moduleId);
            }
        }

    }

    @Override
    public void undeploy(final DeploymentUnit context) {
    }
}
