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
