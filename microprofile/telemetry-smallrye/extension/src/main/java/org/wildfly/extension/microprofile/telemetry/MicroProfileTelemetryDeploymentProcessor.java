/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.extension.microprofile.telemetry;

import static org.jboss.as.weld.Capabilities.WELD_CAPABILITY_NAME;
import static org.wildfly.extension.microprofile.telemetry.MicroProfileTelemetryExtensionLogger.MPTEL_LOGGER;

import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import io.smallrye.config.EnvConfigSource;
import io.smallrye.config.SysPropConfigSource;
import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.config.ConfigValue;
import org.eclipse.microprofile.config.spi.ConfigSource;
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
import org.wildfly.extension.opentelemetry.DeploymentTelemetryConfig;
import org.wildfly.extension.opentelemetry.DeploymentTelemetryConfig.Signal;
import org.wildfly.extension.opentelemetry.OpenTelemetryDeploymentProcessor;
import org.wildfly.extension.opentelemetry.api.WildFlyOpenTelemetryConfig;

/** Resolves deployment-visible MicroProfile Telemetry configuration for the OpenTelemetry deployment processor. */
public class MicroProfileTelemetryDeploymentProcessor implements DeploymentUnitProcessor {
    static final AttachmentKey<WildFlyOpenTelemetryConfig> CONFIG_ATTACHMENT_KEY = AttachmentKey.create(WildFlyOpenTelemetryConfig.class);

    /** {@inheritDoc} */
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
                WildFlyOpenTelemetryConfig config = deploymentUnit.getAttachment(CONFIG_ATTACHMENT_KEY);
                Map<String, String> properties = resolveProperties(config, deploymentConfig);
                properties.putIfAbsent(WildFlyOpenTelemetryConfig.OTEL_SERVICE_NAME, getServiceName(deploymentUnit));
                Set<Signal> applicationExporters = EnumSet.noneOf(Signal.class);
                for (Signal signal : Signal.values()) {
                    if (usesApplicationExporter(deploymentConfig, signal)) {
                        applicationExporters.add(signal);
                    }
                }
                deploymentUnit.putAttachment(OpenTelemetryDeploymentProcessor.CONFIG_ATTACHMENT_KEY,
                        new DeploymentTelemetryConfig(properties, applicationExporters));
            }
        } catch (CapabilityServiceSupport.NoSuchCapabilityException e) {
            throw MPTEL_LOGGER.deploymentRequiresCapability(deploymentPhaseContext.getDeploymentUnit().getName(),
                    WELD_CAPABILITY_NAME);
        }
    }

    /** {@inheritDoc} */
    @Override
    public void undeploy(DeploymentUnit context) {
    }

    /**
     * Merges subsystem properties with resolved deployment-visible OpenTelemetry properties.
     *
     * @param serverConfig the subsystem configuration
     * @param deploymentConfig the deployment MicroProfile Config
     * @return the effective OpenTelemetry properties
     */
    private Map<String, String> resolveProperties(WildFlyOpenTelemetryConfig serverConfig, Config deploymentConfig) {
        Map<String, String> properties = new HashMap<>(serverConfig.properties());
        properties.put("otel.sdk.disabled", "true");
        for (String propertyName : deploymentConfig.getPropertyNames()) {
            if (propertyName.startsWith("otel.") || propertyName.startsWith("OTEL_")) {
                ConfigValue value = deploymentConfig.getConfigValue(propertyName);
                if (value.getValue() != null) {
                    properties.put(propertyName, value.getValue());
                }
            }
        }
        for (Signal signal : Signal.values()) {
            ConfigValue value = deploymentConfig.getConfigValue(signal.exporterProperty());
            if (value.getValue() != null) {
                properties.put(signal.exporterProperty(), value.getValue());
            }
        }
        return properties;
    }

    /**
     * Determines whether the winning exporter selector comes from application-visible configuration.
     *
     * @param config the deployment MicroProfile Config
     * @param signal the signal whose selector is classified
     * @return true unless the winning source is the environment or system properties
     */
    private boolean usesApplicationExporter(Config config, Signal signal) {
        ConfigValue value = config.getConfigValue(signal.exporterProperty());
        if (value.getValue() == null) {
            return false;
        }

        for (ConfigSource source : config.getConfigSources()) {
            if (source.getName().equals(value.getSourceName()) && source.getOrdinal() == value.getSourceOrdinal()) {
                return !(source instanceof EnvConfigSource) && !(source instanceof SysPropConfigSource);
            }
        }

        return true;
    }

    /**
     * Returns the deployment-derived service name used when no explicit name is configured.
     *
     * @param deploymentUnit the deployment
     * @return the deployment service name
     */
    private String getServiceName(DeploymentUnit deploymentUnit) {
        String serviceName = deploymentUnit.getServiceName().getSimpleName();
        if (null != deploymentUnit.getParent()) {
            serviceName = deploymentUnit.getParent().getServiceName().getSimpleName() + "!" + serviceName;
        }
        return serviceName;
    }
}
