/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.extension.microprofile.health.deployment;

import static org.jboss.as.weld.Capabilities.WELD_CAPABILITY_NAME;

import org.jboss.as.controller.capability.CapabilityServiceSupport;
import org.jboss.as.server.deployment.Attachments;
import org.jboss.as.server.deployment.DeploymentPhaseContext;
import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.as.server.deployment.DeploymentUnitProcessor;
import org.jboss.as.server.deployment.module.ModuleDependency;
import org.jboss.as.server.deployment.module.ModuleSpecification;
import org.jboss.as.weld.WeldCapability;
import org.jboss.modules.Module;
import org.jboss.modules.ModuleLoader;
import org.wildfly.extension.microprofile.health.MicroProfileHealthReporter;

/**
 * Add dependencies required by deployment unit to access the Config API (programmatically or using CDI).
 */
public class DependencyProcessor implements DeploymentUnitProcessor {

    @Override
    public void deploy(DeploymentPhaseContext phaseContext) {
        DeploymentUnit unit = phaseContext.getDeploymentUnit();
        ModuleSpecification moduleSpecification = unit.getAttachment(Attachments.MODULE_SPECIFICATION);
        ModuleLoader moduleLoader = Module.getBootModuleLoader();

        moduleSpecification.addSystemDependency(ModuleDependency.Builder.of(moduleLoader, "org.eclipse.microprofile.health.api").build());

        CapabilityServiceSupport support = unit.getAttachment(Attachments.CAPABILITY_SERVICE_SUPPORT);
        try {
            WeldCapability weldCapability = support.getCapabilityRuntimeAPI(WELD_CAPABILITY_NAME, WeldCapability.class);

            if (weldCapability.isPartOfWeldDeployment(unit)) {
                // Ensure MicroProfileHealthReporter is available to DeploymentProcessor
                phaseContext.addDeploymentDependency(support.getCapabilityServiceName(MicroProfileHealthReporter.SERVICE_DESCRIPTOR), MicroProfileHealthReporter.ATTACHMENT_KEY);
            }
        } catch (CapabilityServiceSupport.NoSuchCapabilityException e) {
            throw new IllegalStateException(e);
        }
    }
}
