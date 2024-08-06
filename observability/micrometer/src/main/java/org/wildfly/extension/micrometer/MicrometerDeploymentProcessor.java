/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.extension.micrometer;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.DEPLOYMENT;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SUBDEPLOYMENT;
import static org.wildfly.extension.micrometer.MicrometerExtensionLogger.MICROMETER_LOGGER;
import static org.wildfly.extension.micrometer.MicrometerSubsystemRegistrar.MICROMETER_COLLECTOR_RUNTIME_CAPABILITY;

import java.util.function.Supplier;

import io.micrometer.core.instrument.MeterRegistry;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.capability.CapabilityServiceSupport;
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
import org.wildfly.extension.micrometer.metrics.MicrometerCollector;
import org.wildfly.subsystem.service.ServiceDependency;
import org.wildfly.subsystem.service.ServiceInstaller;

class MicrometerDeploymentProcessor implements DeploymentUnitProcessor {
    static final String WELD_CAPABILITY_NAME = "org.wildfly.weld";

    private final MicrometerSubsystemRegistrar.MicrometerDeploymentConfiguration config;

    MicrometerDeploymentProcessor(MicrometerSubsystemRegistrar.MicrometerDeploymentConfiguration config) {
        this.config = config;
    }

    @Override
    public void deploy(DeploymentPhaseContext deploymentPhaseContext) throws DeploymentUnitProcessingException {
        final DeploymentUnit deploymentUnit = deploymentPhaseContext.getDeploymentUnit();

        ServiceDependency<MicrometerCollector> collector = ServiceDependency.on(MICROMETER_COLLECTOR_RUNTIME_CAPABILITY.getCapabilityServiceName());
        Supplier<MetricRegistration> factory = () -> collector.get().collectResourceMetrics(
                deploymentUnit.getAttachment(DeploymentModelUtils.DEPLOYMENT_RESOURCE),
                deploymentUnit.getAttachment(DeploymentModelUtils.MUTABLE_REGISTRATION_ATTACHMENT),
                createDeploymentAddressPrefix(deploymentUnit)::append,
                this.config.getSubsystemFilter());
        ServiceInstaller.builder(factory)
                .requires(ServiceDependency.on(DeploymentCompleteServiceProcessor.serviceName(deploymentUnit.getServiceName())))
                .requires(collector)
                .asActive()
                .onStop(MetricRegistration::unregister)
                .build()
                .install(deploymentPhaseContext);

        registerCdiExtension(deploymentPhaseContext);
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

    private void registerCdiExtension(DeploymentPhaseContext deploymentPhaseContext) throws DeploymentUnitProcessingException {
        DeploymentUnit deploymentUnit = deploymentPhaseContext.getDeploymentUnit();
        try {
            CapabilityServiceSupport support = deploymentUnit.getAttachment(Attachments.CAPABILITY_SERVICE_SUPPORT);

            final WeldCapability weldCapability = support.getCapabilityRuntimeAPI(WELD_CAPABILITY_NAME, WeldCapability.class);
            if (!weldCapability.isPartOfWeldDeployment(deploymentUnit)) {
                MICROMETER_LOGGER.noCdiDeployment();
            } else {
                weldCapability.registerExtensionInstance(new MicrometerCdiExtension((MeterRegistry) config.getRegistry()), deploymentUnit);
            }
        } catch (CapabilityServiceSupport.NoSuchCapabilityException e) {
            //We should not be here since the subsystem depends on weld capability. Just in case ...
            MICROMETER_LOGGER.deploymentRequiresCapability(deploymentUnit.getName(), WELD_CAPABILITY_NAME);
        }
    }
}
