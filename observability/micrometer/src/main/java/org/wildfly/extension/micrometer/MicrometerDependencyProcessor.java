/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.extension.micrometer;


import org.jboss.as.server.deployment.Attachments;
import org.jboss.as.server.deployment.DeploymentPhaseContext;
import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.as.server.deployment.DeploymentUnitProcessor;
import org.jboss.as.server.deployment.module.ModuleDependency;
import org.jboss.as.server.deployment.module.ModuleSpecification;
import org.jboss.modules.Module;
import org.jboss.modules.ModuleLoader;

class MicrometerDependencyProcessor implements DeploymentUnitProcessor {
    static final String[] MODULES = {
    };

    static final String[] EXPORTED_MODULES = {
            "org.wildfly.micrometer.deployment",
            "io.opentelemetry.otlp",
            "io.micrometer"
    };

    @Override
    public void deploy(DeploymentPhaseContext phaseContext) {
        addDependencies(phaseContext.getDeploymentUnit());
    }

    @Override
    public void undeploy(DeploymentUnit deploymentUnit) {

    }

    private void addDependencies(DeploymentUnit deploymentUnit) {
        ModuleSpecification moduleSpecification = deploymentUnit.getAttachment(Attachments.MODULE_SPECIFICATION);
        ModuleLoader moduleLoader = Module.getBootModuleLoader();
        for (String module : MODULES) {
            moduleSpecification.addSystemDependency(new ModuleDependency(moduleLoader, module, false, false, true, false));
        }
        for (String module : EXPORTED_MODULES) {
            moduleSpecification.addSystemDependency(new ModuleDependency(moduleLoader, module, false, true, true, false));
        }
    }
}
