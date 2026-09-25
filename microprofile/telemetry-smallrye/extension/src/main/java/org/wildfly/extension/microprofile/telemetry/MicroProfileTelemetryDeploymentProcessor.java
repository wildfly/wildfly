/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.extension.microprofile.telemetry;

import static org.jboss.as.weld.Capabilities.WELD_CAPABILITY_NAME;
import static org.wildfly.extension.microprofile.telemetry.MicroProfileTelemetryExtensionLogger.MPTEL_LOGGER;

import java.util.HashMap;
import java.util.Map;

import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.config.spi.ConfigProviderResolver;
import org.jboss.as.controller.capability.CapabilityServiceSupport;
import org.jboss.as.ee.structure.DeploymentType;
import org.jboss.as.ee.structure.DeploymentTypeMarker;
import org.jboss.as.server.deployment.AttachmentKey;
import org.jboss.as.server.deployment.Attachments;
import org.jboss.as.server.deployment.DeploymentPhaseContext;
import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.as.server.deployment.DeploymentUnitProcessingException;
import org.jboss.as.server.deployment.DeploymentUnitProcessor;
import org.jboss.as.weld.WeldCapability;
import org.jboss.modules.Module;
import org.wildfly.extension.microprofile.telemetry.api.MicroProfileTelemetryCdiExtension;
import org.wildfly.extension.opentelemetry.OpenTelemetryDeploymentProcessor;
import org.wildfly.extension.opentelemetry.api.WildFlyOpenTelemetryConfig;

public class MicroProfileTelemetryDeploymentProcessor implements DeploymentUnitProcessor {
    private static final String OTEL_RESOURCE_ATTRIBUTES = "otel.resource.attributes";

    static final AttachmentKey<WildFlyOpenTelemetryConfig> CONFIG_ATTACHMENT_KEY = AttachmentKey.create(WildFlyOpenTelemetryConfig.class);

    @Override
    public void deploy(DeploymentPhaseContext deploymentPhaseContext) throws DeploymentUnitProcessingException {
        final DeploymentUnit deploymentUnit = deploymentPhaseContext.getDeploymentUnit();
        if (DeploymentTypeMarker.isType(DeploymentType.EAR, deploymentUnit)) {
            return;
        }

        try {
            final CapabilityServiceSupport support = deploymentUnit.getAttachment(Attachments.CAPABILITY_SERVICE_SUPPORT);
            final WeldCapability weldCapability = support.getCapabilityRuntimeAPI(WELD_CAPABILITY_NAME, WeldCapability.class);
            if (weldCapability == null || !weldCapability.isPartOfWeldDeployment(deploymentUnit)) {
                MPTEL_LOGGER.debug("The deployment does not have Jakarta Contexts and Dependency Injection enabled. " +
                        "Skipping MicroProfile Telemetry integration.");
            } else {
                Module module = deploymentUnit.getAttachment(Attachments.MODULE);
                Config deploymentConfig = ConfigProviderResolver.instance().getConfig(module.getClassLoader());
                Map<String, String> deploymentProperties = new HashMap<>();
                deploymentConfig.getOptionalValue(WildFlyOpenTelemetryConfig.OTEL_SERVICE_NAME, String.class)
                        .ifPresent(value -> deploymentProperties.put(WildFlyOpenTelemetryConfig.OTEL_SERVICE_NAME, value));
                deploymentConfig.getOptionalValue(OTEL_RESOURCE_ATTRIBUTES, String.class)
                        .ifPresent(value -> deploymentProperties.put(OTEL_RESOURCE_ATTRIBUTES, value));
                deploymentUnit.putAttachment(OpenTelemetryDeploymentProcessor.CONFIG_PROPERTIES_ATTACHMENT_KEY,
                        Map.copyOf(deploymentProperties));

                WildFlyOpenTelemetryConfig config = deploymentUnit.getAttachment(CONFIG_ATTACHMENT_KEY);
                Map<String, String> properties = new HashMap<>(config.properties());
                if (!properties.containsKey("otel.service.name")) {
                    properties.put("otel.service.name", getServiceName(deploymentUnit));
                }

                weldCapability.registerExtensionInstance(new MicroProfileTelemetryCdiExtension(properties),
                        deploymentUnit);
            }
        } catch (CapabilityServiceSupport.NoSuchCapabilityException e) {
            throw MPTEL_LOGGER.deploymentRequiresCapability(deploymentPhaseContext.getDeploymentUnit().getName(),
                    WELD_CAPABILITY_NAME);
        }
    }

    @Override
    public void undeploy(DeploymentUnit context) {
    }

    private String getServiceName(DeploymentUnit deploymentUnit) {
        String serviceName = deploymentUnit.getServiceName().getSimpleName();
        if (null != deploymentUnit.getParent()) {
            serviceName = deploymentUnit.getParent().getServiceName().getSimpleName() + "!" + serviceName;
        }
        return serviceName;
    }
}
