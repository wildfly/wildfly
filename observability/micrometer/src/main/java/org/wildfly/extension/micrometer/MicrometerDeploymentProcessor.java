/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.extension.micrometer;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.DEPLOYMENT;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SUBDEPLOYMENT;
import static org.wildfly.extension.micrometer.MicrometerExtensionLogger.MICROMETER_LOGGER;
import static org.wildfly.extension.micrometer.MicrometerSubsystemRegistrar.MICROMETER_SERVICE_SERVICE_NAME;

import java.util.function.Supplier;

import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.capability.CapabilityServiceSupport;
import org.jboss.as.server.deployment.AttachmentKey;
import org.jboss.as.server.deployment.Attachments;
import org.jboss.as.server.deployment.DeploymentCompleteServiceProcessor;
import org.jboss.as.server.deployment.DeploymentModelUtils;
import org.jboss.as.server.deployment.DeploymentPhaseContext;
import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.as.server.deployment.DeploymentUnitProcessingException;
import org.jboss.as.server.deployment.DeploymentUnitProcessor;
import org.jboss.as.weld.WeldCapability;
import org.wildfly.extension.micrometer.api.MicrometerCdiExtension;
import org.wildfly.extension.micrometer.metrics.MetricRegistration;
import org.wildfly.subsystem.service.ServiceDependency;
import org.wildfly.subsystem.service.ServiceInstaller;

class MicrometerDeploymentProcessor implements DeploymentUnitProcessor {
    static final String WELD_CAPABILITY_NAME = "org.wildfly.weld";
    static final AttachmentKey<MicrometerService> CONFIG_ATTACHMENT_KEY = AttachmentKey.create(MicrometerService.class);

    MicrometerDeploymentProcessor() {
    }

    @Override
    public void deploy(DeploymentPhaseContext phaseContext) throws DeploymentUnitProcessingException {
        final DeploymentUnit deploymentUnit = phaseContext.getDeploymentUnit();

        ServiceDependency<MicrometerService> serviceDependency = ServiceDependency.on(MICROMETER_SERVICE_SERVICE_NAME);
        Supplier<MetricRegistration> factory = () -> serviceDependency.get().collectResourceMetrics(
                deploymentUnit.getAttachment(DeploymentModelUtils.DEPLOYMENT_RESOURCE),
                deploymentUnit.getAttachment(DeploymentModelUtils.MUTABLE_REGISTRATION_ATTACHMENT),
                createDeploymentAddressPrefix(deploymentUnit)::append);
        ServiceInstaller.builder(factory)
                .requires(ServiceDependency.on(DeploymentCompleteServiceProcessor.serviceName(deploymentUnit.getServiceName())))
                .requires(serviceDependency)
                .asActive()
                .onStop(MetricRegistration::unregister)
                .build()
                .install(phaseContext);

        registerCdiExtension(phaseContext, deploymentUnit.getAttachment(CONFIG_ATTACHMENT_KEY));
    }

    @Override
    public void undeploy(DeploymentUnit context) {
    }

    private PathAddress createDeploymentAddressPrefix(DeploymentUnit deploymentUnit) {
        if (deploymentUnit.getParent() == null) {
            return PathAddress.pathAddress(DEPLOYMENT, deploymentUnit.getAttachment(Attachments.MANAGEMENT_NAME));
        } else {
            return createDeploymentAddressPrefix(deploymentUnit.getParent()).append(SUBDEPLOYMENT, deploymentUnit.getName());
        }
    }

    private void registerCdiExtension(DeploymentPhaseContext deploymentPhaseContext, MicrometerService micrometerService) throws DeploymentUnitProcessingException {
        DeploymentUnit deploymentUnit = deploymentPhaseContext.getDeploymentUnit();
        try {
            CapabilityServiceSupport support = deploymentUnit.getAttachment(Attachments.CAPABILITY_SERVICE_SUPPORT);

            final WeldCapability weldCapability = support.getCapabilityRuntimeAPI(WELD_CAPABILITY_NAME, WeldCapability.class);
            if (!weldCapability.isPartOfWeldDeployment(deploymentUnit)) {
                MICROMETER_LOGGER.noCdiDeployment();
            } else {
                weldCapability.registerExtensionInstance(new MicrometerCdiExtension(micrometerService.getMicrometerRegistry()),
                    deploymentUnit);
            }
        } catch (CapabilityServiceSupport.NoSuchCapabilityException e) {
            //We should not be here since the subsystem depends on weld capability. Just in case ...
            MICROMETER_LOGGER.deploymentRequiresCapability(deploymentUnit.getName(), WELD_CAPABILITY_NAME);
        }
    }
}
