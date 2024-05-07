/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.extension.micrometer;

import static org.wildfly.extension.micrometer._private.MicrometerExtensionLogger.MICROMETER_LOGGER;

import java.util.List;

import org.jboss.as.controller.capability.CapabilityServiceSupport;
import org.jboss.as.server.deployment.Attachments;
import org.jboss.as.server.deployment.DeploymentPhaseContext;
import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.as.server.deployment.DeploymentUnitProcessingException;
import org.jboss.as.server.deployment.DeploymentUnitProcessor;
import org.jboss.as.weld.WeldCapability;
import org.wildfly.extension.micrometer.api.MicrometerCdiExtension;
import org.wildfly.extension.micrometer.registry.WildFlyCompositeRegistry;

class MicrometerDeploymentProcessor implements DeploymentUnitProcessor {
    public static final String WELD_CAPABILITY_NAME = "org.wildfly.weld";
    private final boolean exposeAnySubsystem;
    private final List<String> exposedSubsystems;
    private final WildFlyCompositeRegistry wildFlyRegistry;

    MicrometerDeploymentProcessor(boolean exposeAnySubsystem,
                                  List<String> exposedSubsystems,
                                  WildFlyCompositeRegistry wildFlyRegistry) {
        this.exposeAnySubsystem = exposeAnySubsystem;
        this.exposedSubsystems = exposedSubsystems;
        this.wildFlyRegistry = wildFlyRegistry;
    }

    @Override
    public void deploy(DeploymentPhaseContext deploymentPhaseContext) throws DeploymentUnitProcessingException {
        final DeploymentUnit deploymentUnit = deploymentPhaseContext.getDeploymentUnit();

//        MicrometerDeploymentService.install(deploymentPhaseContext.getServiceTarget(),
//                deploymentPhaseContext,
//                deploymentUnit.getAttachment(DeploymentModelUtils.DEPLOYMENT_RESOURCE),
//                deploymentUnit.getAttachment(DeploymentModelUtils.MUTABLE_REGISTRATION_ATTACHMENT),
//                wildFlyRegistry,
//                exposeAnySubsystem,
//                exposedSubsystems);

        registerCdiExtension(deploymentPhaseContext);
    }

    @Override
    public void undeploy(DeploymentUnit context) {
    }

    private void registerCdiExtension(DeploymentPhaseContext deploymentPhaseContext) throws DeploymentUnitProcessingException {
        DeploymentUnit deploymentUnit = deploymentPhaseContext.getDeploymentUnit();
        try {
            CapabilityServiceSupport support = deploymentUnit.getAttachment(Attachments.CAPABILITY_SERVICE_SUPPORT);

            final WeldCapability weldCapability = support.getCapabilityRuntimeAPI(WELD_CAPABILITY_NAME, WeldCapability.class);
            if (!weldCapability.isPartOfWeldDeployment(deploymentUnit)) {
                MICROMETER_LOGGER.noCdiDeployment();
            } else {
                weldCapability.registerExtensionInstance(new MicrometerCdiExtension(wildFlyRegistry), deploymentUnit);
            }
        } catch (CapabilityServiceSupport.NoSuchCapabilityException e) {
            //We should not be here since the subsystem depends on weld capability. Just in case ...
            MICROMETER_LOGGER.deploymentRequiresCapability(deploymentUnit.getName(), WELD_CAPABILITY_NAME);
        }
    }
}
