/*
 * JBoss, Home of Professional Open Source. Copyright 2019, Red Hat, Inc., and
 * individual contributors as indicated by the @author tags. See the
 * copyright.txt file in the distribution for a full listing of individual
 * contributors.
 *
 * This is free software; you can redistribute it and/or modify it under the
 * terms of the GNU Lesser General Public License as published by the Free
 * Software Foundation; either version 2.1 of the License, or (at your option)
 * any later version.
 *
 * This software is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public License for more
 * details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this software; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA, or see the FSF
 * site: http://www.fsf.org.
 */
package org.wildfly.extension.microprofile.openapi.deployment;

import java.util.LinkedHashMap;
import java.util.Map;

import org.jboss.as.server.deployment.Attachments;
import org.jboss.as.server.deployment.DeploymentPhaseContext;
import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.as.server.deployment.DeploymentUnitProcessor;
import org.jboss.as.server.deployment.module.ModuleDependency;
import org.jboss.as.server.deployment.module.ModuleSpecification;
import org.jboss.modules.Module;
import org.jboss.modules.ModuleLoader;

/**
 * Add dependencies required by the deployment unit to access the MicroProfile
 * OpenAPI
 *
 * @author Michael Edgar
 */
public class DependencyProcessor implements DeploymentUnitProcessor {

    private static final Map<String, Boolean> DEPLOYMENT_UNIT_DEPENDENCIES = new LinkedHashMap<>(3);

    static {
        DEPLOYMENT_UNIT_DEPENDENCIES.put("org.wildfly.extension.microprofile.openapi-smallrye", Boolean.TRUE);
        DEPLOYMENT_UNIT_DEPENDENCIES.put("org.eclipse.microprofile.openapi.api", Boolean.FALSE);
        DEPLOYMENT_UNIT_DEPENDENCIES.put("io.smallrye.openapi", Boolean.FALSE);
    }

    @Override
    public void deploy(DeploymentPhaseContext phaseContext) {
        final DeploymentUnit deploymentUnit = phaseContext.getDeploymentUnit();
        final ModuleSpecification specification = deploymentUnit.getAttachment(Attachments.MODULE_SPECIFICATION);
        final ModuleLoader loader = Module.getBootModuleLoader();

        DEPLOYMENT_UNIT_DEPENDENCIES.entrySet()
                                    .stream()
                                    .map(dependency -> createModuleDependency(loader, dependency))
                                    .forEach(specification::addSystemDependency);
    }

    @Override
    public void undeploy(DeploymentUnit context) {
        // No action
    }

    private ModuleDependency createModuleDependency(ModuleLoader loader, Map.Entry<String, Boolean> dependency) {
        // Declaring each argument for documentation purposes.
        String name = dependency.getKey();
        boolean optional = false;
        boolean export = dependency.getValue();
        boolean importServices = true;
        boolean userSpecified = false;

        return new ModuleDependency(loader, name, optional, export, importServices, userSpecified);
    }
}
