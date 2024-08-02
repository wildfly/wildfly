/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.extension.microprofile.telemetry;

import static org.wildfly.extension.microprofile.telemetry.MicroProfileTelemetrySubsystemDefinition.EXPORTED_MODULES;

import org.jboss.as.server.deployment.Attachments;
import org.jboss.as.server.deployment.DeploymentPhaseContext;
import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.as.server.deployment.DeploymentUnitProcessor;
import org.jboss.as.server.deployment.module.ModuleDependency;
import org.jboss.as.server.deployment.module.ModuleSpecification;
import org.jboss.modules.Module;
import org.jboss.modules.ModuleLoader;
import org.wildfly.extension.opentelemetry.api.WildFlyOpenTelemetryConfig;
import org.wildfly.service.ServiceDependency;

class MicroProfileTelemetryDependencyProcessor implements DeploymentUnitProcessor {

    private final ServiceDependency<WildFlyOpenTelemetryConfig> configServiceDependency;

    MicroProfileTelemetryDependencyProcessor(ServiceDependency<WildFlyOpenTelemetryConfig> configServiceDependency) {
        this.configServiceDependency = configServiceDependency;
    }

    @Override
    public void deploy(DeploymentPhaseContext phaseContext) {
        addDependencies(phaseContext.getDeploymentUnit());

        // Ensure the OpenTelemetryConfig is available to the Phase.POST_MODULE MicroProfileTelemetryDeploymentProcessor
        // TODO WFCORE-6491 the kernel should support an API such that an OSH can record this requirement without
        // needing to involve a DUP like this one that is separate from the one that consumes the dependency.
        phaseContext.requires(configServiceDependency);
    }

    private void addDependencies(DeploymentUnit deploymentUnit) {
        final ModuleSpecification moduleSpecification = deploymentUnit.getAttachment(Attachments.MODULE_SPECIFICATION);
        final ModuleLoader moduleLoader = Module.getBootModuleLoader();

        for (String module : EXPORTED_MODULES) {
            moduleSpecification.addSystemDependency(new ModuleDependency(moduleLoader, module, false, true,
                    true, false));
        }
    }
}
