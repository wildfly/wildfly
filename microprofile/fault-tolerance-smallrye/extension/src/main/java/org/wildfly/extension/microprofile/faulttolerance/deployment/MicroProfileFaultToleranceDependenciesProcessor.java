/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.extension.microprofile.faulttolerance.deployment;

import static org.jboss.as.weld.Capabilities.WELD_CAPABILITY_NAME;

import java.util.Optional;

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

/**
 * Conditionally adds "org.eclipse.microprofile.fault-tolerance.api" dependency if this deployment is part of Weld deployment.
 *
 * @author Radoslav Husar
 */
public class MicroProfileFaultToleranceDependenciesProcessor implements DeploymentUnitProcessor {

    @Override
    public void deploy(DeploymentPhaseContext phaseContext) {
        DeploymentUnit deploymentUnit = phaseContext.getDeploymentUnit();

        CapabilityServiceSupport support = deploymentUnit.getAttachment(Attachments.CAPABILITY_SERVICE_SUPPORT);
        Optional<WeldCapability> weldCapability = support.getOptionalCapabilityRuntimeAPI(WELD_CAPABILITY_NAME, WeldCapability.class);
        if (weldCapability.isPresent() && weldCapability.get().isPartOfWeldDeployment(deploymentUnit) && MicroProfileFaultToleranceMarker.hasMicroProfileFaultToleranceAnnotations(deploymentUnit)) {
            MicroProfileFaultToleranceMarker.mark(deploymentUnit);

            ModuleSpecification moduleSpecification = deploymentUnit.getAttachment(Attachments.MODULE_SPECIFICATION);
            ModuleLoader moduleLoader = Module.getBootModuleLoader();
            moduleSpecification.addSystemDependency(ModuleDependency.Builder.of(moduleLoader, "org.eclipse.microprofile.fault-tolerance.api").build());
            moduleSpecification.addSystemDependency(ModuleDependency.Builder.of(moduleLoader, "org.wildfly.microprofile.fault-tolerance-smallrye.deployment").setImportServices(true).build());
        }
    }

    @Override
    public void undeploy(DeploymentUnit deploymentUnit) {
        MicroProfileFaultToleranceMarker.clearMark(deploymentUnit);
    }
}
