/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.extension.micrometer;

import static org.wildfly.extension.micrometer.MicrometerDeploymentProcessor.CONFIG_ATTACHMENT_KEY;
import static org.wildfly.extension.micrometer.MicrometerSubsystemRegistrar.EXPORTED_MODULES;
import static org.wildfly.extension.micrometer.MicrometerSubsystemRegistrar.SERVICE_DESCRIPTOR;

import org.jboss.as.controller.ServiceNameFactory;
import org.jboss.as.server.deployment.Attachments;
import org.jboss.as.server.deployment.DeploymentPhaseContext;
import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.as.server.deployment.DeploymentUnitProcessor;
import org.jboss.as.server.deployment.module.ModuleDependency;
import org.jboss.as.server.deployment.module.ModuleSpecification;
import org.jboss.modules.Module;
import org.jboss.modules.ModuleLoader;

class MicrometerDependencyProcessor implements DeploymentUnitProcessor {
    @Override
    public void deploy(DeploymentPhaseContext phaseContext) {
        addDependencies(phaseContext.getDeploymentUnit());

        // Ensure the MicrometerService is available to the Phase.POST_MODULE MicrometerDeploymentProcessor
        // TODO WFCORE-6941 the kernel should support an API such that an OSH can record this requirement without
        // needing to involve a DUP like this one that is separate from the one that consumes the dependency.
        phaseContext.addDeploymentDependency(ServiceNameFactory.resolveServiceName(SERVICE_DESCRIPTOR), CONFIG_ATTACHMENT_KEY);
    }

    private void addDependencies(DeploymentUnit deploymentUnit) {
        ModuleSpecification moduleSpecification = deploymentUnit.getAttachment(Attachments.MODULE_SPECIFICATION);
        ModuleLoader moduleLoader = Module.getBootModuleLoader();
        for (String module : EXPORTED_MODULES) {
            moduleSpecification.addSystemDependency(ModuleDependency.Builder.of(moduleLoader, module).setExport(true).setImportServices(true).build());
        }
    }
}
