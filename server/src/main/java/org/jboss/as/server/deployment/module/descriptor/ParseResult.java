/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2012, Red Hat, Inc., and individual contributors
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

package org.jboss.as.server.deployment.module.descriptor;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.modules.ModuleLoader;

/**
* @author Stuart Douglas
*/
class ParseResult {

    private final ModuleLoader moduleLoader;
    private final DeploymentUnit deploymentUnit;

    private Boolean earSubDeploymentsIsolated = null;
    private ModuleStructureSpec rootDeploymentSpecification;
    private final Map<String, ModuleStructureSpec> subDeploymentSpecifications = new HashMap<String, ModuleStructureSpec>();
    private final List<ModuleStructureSpec> additionalModules = new ArrayList<ModuleStructureSpec>();

    public ParseResult(final ModuleLoader moduleLoader, final DeploymentUnit deploymentUnit) {
        this.moduleLoader = moduleLoader;
        this.deploymentUnit = deploymentUnit;
    }

    public void setEarSubDeploymentsIsolated(final Boolean earSubDeploymentsIsolated) {
        this.earSubDeploymentsIsolated = earSubDeploymentsIsolated;
    }

    public void setRootDeploymentSpecification(final ModuleStructureSpec rootDeploymentSpecification) {
        this.rootDeploymentSpecification = rootDeploymentSpecification;
    }

    public ModuleLoader getModuleLoader() {
        return moduleLoader;
    }

    public DeploymentUnit getDeploymentUnit() {
        return deploymentUnit;
    }

    public Boolean getEarSubDeploymentsIsolated() {
        return earSubDeploymentsIsolated;
    }

    public ModuleStructureSpec getRootDeploymentSpecification() {
        return rootDeploymentSpecification;
    }

    public Map<String, ModuleStructureSpec> getSubDeploymentSpecifications() {
        return subDeploymentSpecifications;
    }

    public List<ModuleStructureSpec> getAdditionalModules() {
        return additionalModules;
    }
}
