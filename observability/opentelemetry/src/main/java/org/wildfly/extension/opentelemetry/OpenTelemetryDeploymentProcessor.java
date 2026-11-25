/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.extension.opentelemetry;

import static org.jboss.as.weld.Capabilities.WELD_CAPABILITY_NAME;
import static org.wildfly.extension.opentelemetry.OpenTelemetryExtensionLogger.OTEL_LOGGER;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

import io.smallrye.opentelemetry.implementation.cdi.OpenTelemetryExtension;
import org.jboss.as.controller.capability.CapabilityServiceSupport;
import org.jboss.as.ee.structure.DeploymentType;
import org.jboss.as.ee.structure.DeploymentTypeMarker;
import org.jboss.as.server.deployment.Attachments;
import org.jboss.as.server.deployment.DeploymentPhaseContext;
import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.as.server.deployment.DeploymentUnitProcessingException;
import org.jboss.as.server.deployment.DeploymentUnitProcessor;
import org.jboss.as.weld.WeldCapability;
import org.wildfly.extension.opentelemetry.api.OpenTelemetryCdiExtension;
import org.wildfly.extension.opentelemetry.api.WildFlyOpenTelemetryConfig;

class OpenTelemetryDeploymentProcessor implements DeploymentUnitProcessor {
    private final Supplier<WildFlyOpenTelemetryConfig> openTelemetryConfig;

    public OpenTelemetryDeploymentProcessor(Supplier<WildFlyOpenTelemetryConfig> openTelemetryConfig) {
        this.openTelemetryConfig = openTelemetryConfig;
    }

    @Override
    public void deploy(DeploymentPhaseContext deploymentPhaseContext) throws DeploymentUnitProcessingException {
        OTEL_LOGGER.debug("OpenTelemetry Subsystem is processing deployment");

        final DeploymentUnit deploymentUnit = deploymentPhaseContext.getDeploymentUnit();
        if (DeploymentTypeMarker.isType(DeploymentType.EAR, deploymentUnit)) {
            return;
        }

        try {
            final WeldCapability weldCapability = deploymentUnit.getAttachment(Attachments.CAPABILITY_SERVICE_SUPPORT)
                    .getCapabilityRuntimeAPI(WELD_CAPABILITY_NAME, WeldCapability.class);
            if (!weldCapability.isPartOfWeldDeployment(deploymentUnit)) {
                // Jakarta RESTful Web Services require Jakarta Contexts and Dependency Injection, without which,
                // there's no integration needed
                OTEL_LOGGER.debug("The deployment does not have Jakarta Contexts and Dependency Injection enabled. Skipping OpenTelemetry integration.");
                return;
            }

            Map<String, String> config = new HashMap<>(openTelemetryConfig.get().properties());
            if (!config.containsKey(WildFlyOpenTelemetryConfig.OTEL_SERVICE_NAME)) {
                config.put(WildFlyOpenTelemetryConfig.OTEL_SERVICE_NAME, getServiceName(deploymentUnit));
            }

            weldCapability.registerExtensionInstance(
                    new OpenTelemetryCdiExtension(!openTelemetryConfig.get().isMpTelemetryInstalled(), config), deploymentUnit);
            weldCapability.registerExtensionInstance(new OpenTelemetryExtension(), deploymentUnit);
        } catch (CapabilityServiceSupport.NoSuchCapabilityException e) {
            // We should not be here since the subsystem depends on weld capability. Just in case ...
            throw OTEL_LOGGER.deploymentRequiresCapability(deploymentPhaseContext.getDeploymentUnit().getName(),
                    WELD_CAPABILITY_NAME);
        }
    }

    private String getServiceName(DeploymentUnit deploymentUnit) {
        String serviceName = deploymentUnit.getServiceName().getSimpleName();
        if (null != deploymentUnit.getParent()) {
            serviceName = deploymentUnit.getParent().getServiceName().getSimpleName() + "!" + serviceName;
        }
        return serviceName;
    }
}
