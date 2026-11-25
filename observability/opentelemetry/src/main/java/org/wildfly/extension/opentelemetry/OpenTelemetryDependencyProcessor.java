/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.extension.opentelemetry;

import java.util.List;

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
    private static final String API_MODULE = "org.wildfly.extension.opentelemetry-api";
    private static final List<String> EXPORTED_MODULES = List.of(
        "io.opentelemetry.api",
        "io.opentelemetry.context",
        "io.opentelemetry.exporter",
        "io.opentelemetry.instrumentation.annotations",
        "io.opentelemetry.instrumentation.api",
        "io.opentelemetry.otlp",
        "io.opentelemetry.sdk",
        "io.opentelemetry.semconv",
        "io.smallrye.opentelemetry"
    );

    @Override
    public void deploy(DeploymentPhaseContext phaseContext) {
        addDependencies(phaseContext.getDeploymentUnit());
    }

    private void addDependencies(DeploymentUnit deploymentUnit) {
        try {
            final ModuleSpecification moduleSpecification = deploymentUnit.getAttachment(Attachments.MODULE_SPECIFICATION);
            final ModuleLoader moduleLoader = Module.getBootModuleLoader();

            WeldCapability weldCapability = deploymentUnit.getAttachment(Attachments.CAPABILITY_SERVICE_SUPPORT)
                .getCapabilityRuntimeAPI(Capabilities.WELD_CAPABILITY_NAME, WeldCapability.class);
            if (weldCapability.isPartOfWeldDeployment(deploymentUnit)) {
                // Export the -api module only if CDI is available
                moduleSpecification.addSystemDependency(ModuleDependency.Builder.of(moduleLoader, API_MODULE).setExport(true).setImportServices(true).build());
            }

            // Export all other modules regardless of CDI availability
            for (String module : EXPORTED_MODULES) {
                moduleSpecification.addSystemDependency(ModuleDependency.Builder.of(moduleLoader, module).setExport(true).setImportServices(true).build());
            }
        } catch (CapabilityServiceSupport.NoSuchCapabilityException e) {
            throw new IllegalStateException();
        }
    }
}
