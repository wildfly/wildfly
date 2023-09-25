/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.extension.opentelemetry;

import static org.wildfly.extension.opentelemetry.OpenTelemetrySubsystemDefinition.API_MODULE;
import static org.wildfly.extension.opentelemetry.OpenTelemetrySubsystemDefinition.EXPORTED_MODULES;

import org.jboss.as.controller.capability.CapabilityServiceSupport;
import org.jboss.as.server.deployment.Attachments;
import org.jboss.as.server.deployment.DeploymentPhaseContext;
import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.as.server.deployment.DeploymentUnitProcessor;
import org.jboss.as.server.deployment.module.ModuleDependency;
import org.jboss.as.server.deployment.module.ModuleSpecification;
import org.jboss.as.weld.Capabilities;
import org.jboss.as.weld.WeldCapability;
import org.jboss.modules.Module;
import org.jboss.modules.ModuleLoader;

class OpenTelemetryDependencyProcessor implements DeploymentUnitProcessor {
    public OpenTelemetryDependencyProcessor() {

    }

    @Override
    public void deploy(DeploymentPhaseContext phaseContext) {
        addDependencies(phaseContext.getDeploymentUnit());
    }

    private void addDependencies(DeploymentUnit deploymentUnit) {
        final ModuleSpecification moduleSpecification = deploymentUnit.getAttachment(Attachments.MODULE_SPECIFICATION);
        final ModuleLoader moduleLoader = Module.getBootModuleLoader();

        try {
            CapabilityServiceSupport support = deploymentUnit.getAttachment(Attachments.CAPABILITY_SERVICE_SUPPORT);
            WeldCapability weldCapability = support.getCapabilityRuntimeAPI(Capabilities.WELD_CAPABILITY_NAME,
                    WeldCapability.class);
            if (weldCapability.isPartOfWeldDeployment(deploymentUnit)) {
                // Export the -api module only if CDI is available
                moduleSpecification.addSystemDependency(new ModuleDependency(moduleLoader, API_MODULE,
                        false, true, true, false));
            }

            // Export all other modules regardless of CDI availability
            for (String module : EXPORTED_MODULES) {
                moduleSpecification.addSystemDependency(new ModuleDependency(moduleLoader, module, false, true,
                        true, false));
            }
        } catch (CapabilityServiceSupport.NoSuchCapabilityException e) {
            throw new IllegalStateException();
        }
    }
}
