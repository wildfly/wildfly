/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.extension.microprofile.lra.participant.deployment;

import io.narayana.lra.client.internal.proxy.nonjaxrs.LRACDIExtension;
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
import org.jboss.modules.filter.PathFilters;

import static org.jboss.as.weld.Capabilities.WELD_CAPABILITY_NAME;

public class LRAParticipantDeploymentDependencyProcessor implements DeploymentUnitProcessor {

    @Override
    public void deploy(DeploymentPhaseContext phaseContext) {
        DeploymentUnit deploymentUnit = phaseContext.getDeploymentUnit();

        if (LRAAnnotationsUtil.isNotLRADeployment(deploymentUnit)) {
            return;
        }

        addModuleDependencies(deploymentUnit);
    }

    @Override
    public void undeploy(DeploymentUnit context) {
    }

    private void addModuleDependencies(DeploymentUnit deploymentUnit) {
        final ModuleSpecification moduleSpecification = deploymentUnit.getAttachment(Attachments.MODULE_SPECIFICATION);
        final ModuleLoader moduleLoader = Module.getBootModuleLoader();

        moduleSpecification.addSystemDependency(new ModuleDependency(moduleLoader, "org.eclipse.microprofile.lra.api", false, false, false, false));
        ModuleDependency lraParticipantDependency = new ModuleDependency(moduleLoader, "org.jboss.narayana.lra.lra-participant", false, false, true, false);
        lraParticipantDependency.addImportFilter(PathFilters.getMetaInfFilter(), true);
        moduleSpecification.addSystemDependency(lraParticipantDependency);
        moduleSpecification.addSystemDependency(new ModuleDependency(moduleLoader, "io.smallrye.jandex", false, false, true, false));
        moduleSpecification.addSystemDependency(new ModuleDependency(moduleLoader, "org.jboss.as.weld.common", false, false, true, false));
        moduleSpecification.addSystemDependency(new ModuleDependency(moduleLoader, "org.jboss.resteasy.resteasy-cdi", false, false, true, false));

        final CapabilityServiceSupport support = deploymentUnit.getAttachment(Attachments.CAPABILITY_SERVICE_SUPPORT);
        if (support.hasCapability(WELD_CAPABILITY_NAME)) {
            support.getOptionalCapabilityRuntimeAPI(WELD_CAPABILITY_NAME, WeldCapability.class).get()
                .registerExtensionInstance(new LRACDIExtension(), deploymentUnit);
        }
    }
}