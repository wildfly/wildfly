/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.weld.deployment.processor;

import static org.jboss.as.weld.Capabilities.WELD_CAPABILITY_NAME;

import org.jboss.as.controller.capability.CapabilityServiceSupport;
import org.jboss.as.server.deployment.Attachments;
import org.jboss.as.server.deployment.DeploymentPhaseContext;
import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.as.server.deployment.DeploymentUnitProcessingException;
import org.jboss.as.server.deployment.DeploymentUnitProcessor;
import org.jboss.as.server.deployment.module.ModuleDependency;
import org.jboss.as.server.deployment.module.ModuleSpecification;
import org.jboss.as.weld.WeldCapability;
import org.jboss.modules.Module;
import org.jboss.modules.ModuleLoader;

/**
 *
 * @author Martin Kouba
 */
public class WeldBeanValidationDependencyProcessor implements DeploymentUnitProcessor {

    private static final String CDI_BEAN_VALIDATION_ID = "org.hibernate.validator.cdi";

    @Override
    public void deploy(DeploymentPhaseContext phaseContext) throws DeploymentUnitProcessingException {

        final DeploymentUnit deploymentUnit = phaseContext.getDeploymentUnit();
        final ModuleSpecification moduleSpecification = deploymentUnit.getAttachment(Attachments.MODULE_SPECIFICATION);
        final ModuleLoader moduleLoader = Module.getBootModuleLoader();
        final CapabilityServiceSupport support = deploymentUnit.getAttachment(Attachments.CAPABILITY_SERVICE_SUPPORT);

        final WeldCapability weldCapability;
        try {
            weldCapability = support.getCapabilityRuntimeAPI(WELD_CAPABILITY_NAME, WeldCapability.class);
        } catch (CapabilityServiceSupport.NoSuchCapabilityException ignored) {
            return;
        }

        if (!weldCapability.isPartOfWeldDeployment(deploymentUnit)) {
            return; // Skip if there are no beans.xml files in the deployment
        }

        ModuleDependency cdiBeanValidationDep = ModuleDependency.Builder.of(moduleLoader, CDI_BEAN_VALIDATION_ID).setImportServices(true).build();
        moduleSpecification.addSystemDependency(cdiBeanValidationDep);
    }
}
