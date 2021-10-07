/*
 * JBoss, Home of Professional Open Source.
 *
 * Copyright 2021 Red Hat, Inc., and individual contributors
 * as indicated by the @author tags.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.wildfly.extension.opentelemetry.deployment;

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
import org.wildfly.extension.opentelemetry.api.OpenTelemetryCdiExtension;

public class OpenTelemetryDependencyProcessor implements DeploymentUnitProcessor {
    @Override
    public void deploy(DeploymentPhaseContext phaseContext) {
        addDependencies(phaseContext.getDeploymentUnit());
    }

    @Override
    public void undeploy(DeploymentUnit deploymentUnit) {

    }

    private void addDependencies(DeploymentUnit deploymentUnit) {
        ModuleSpecification moduleSpecification = deploymentUnit.getAttachment(Attachments.MODULE_SPECIFICATION);

        try {
            CapabilityServiceSupport support = deploymentUnit.getAttachment(Attachments.CAPABILITY_SERVICE_SUPPORT);
            WeldCapability weldCapability = support.getCapabilityRuntimeAPI(Capabilities.WELD_CAPABILITY_NAME,
                    WeldCapability.class);
            weldCapability.registerExtensionInstance(new OpenTelemetryCdiExtension(), deploymentUnit);
        } catch (CapabilityServiceSupport.NoSuchCapabilityException e) {
            throw new IllegalStateException();
        }

        ModuleLoader moduleLoader = Module.getBootModuleLoader();
        for (String module : EXPORTED_MODULES) {
            ModuleDependency modDep = new ModuleDependency(moduleLoader, module, false, true,
                    true, false);
            moduleSpecification.addSystemDependency(modDep);
        }
    }
}
