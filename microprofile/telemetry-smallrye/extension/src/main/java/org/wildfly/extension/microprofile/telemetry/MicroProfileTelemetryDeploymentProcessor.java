/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.extension.microprofile.telemetry;

import static org.jboss.as.weld.Capabilities.WELD_CAPABILITY_NAME;
import static org.wildfly.extension.microprofile.telemetry.MicroProfileTelemetryExtensionLogger.MPTEL_LOGGER;
import static org.wildfly.extension.opentelemetry.api.WildFlyOpenTelemetryConfig.OTEL_SDK_DISABLED;
import static org.wildfly.extension.opentelemetry.api.WildFlyOpenTelemetryConfig.OTEL_TRACES_SAMPLER;

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
                if (config == null) {
                    MPTEL_LOGGER.debug("WildFlyOpenTelemetryConfig attachment is null, using empty server properties");
                }
                Map<String, String> serverProperties = config != null ? config.properties() : Map.of();
                Map<String, String> properties = resolveProperties(serverProperties, deploymentConfig);
                properties.putIfAbsent(WildFlyOpenTelemetryConfig.OTEL_SERVICE_NAME, getServiceName(deploymentUnit));
                Set<Signal> applicationExporters = EnumSet.noneOf(Signal.class);
                for (Signal signal : Signal.values()) {
                    if (usesApplicationProperty(deploymentConfig, signal.exporterProperty())) {
                        applicationExporters.add(signal);
                    }
                }
                if (!applicationExporters.isEmpty()) {
                    String endpoint = serverProperties.get(WildFlyOpenTelemetryConfig.OTEL_EXPORTER_OTLP_ENDPOINT);
                    if (endpoint != null) {
                        properties.put(WildFlyOpenTelemetryConfig.OTEL_EXPORTER_OTLP_ENDPOINT, endpoint);
                    }
                }
                deploymentUnit.putAttachment(OpenTelemetryDeploymentProcessor.CONFIG_ATTACHMENT_KEY,
                        new DeploymentTelemetryConfig(properties, applicationExporters,
                                usesApplicationProperty(deploymentConfig, OTEL_TRACES_SAMPLER)));
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
     * @param serverProperties the subsystem configuration properties
     * @param deploymentConfig the deployment MicroProfile Config
     * @return the effective OpenTelemetry properties
     */
    static Map<String, String> resolveProperties(Map<String, String> serverProperties, Config deploymentConfig) {
        Map<String, String> properties = new HashMap<>();
        boolean applicationExporter = false;
        for (Signal signal : Signal.values()) {
            applicationExporter |= usesApplicationProperty(deploymentConfig, signal.exporterProperty());
        }
        final boolean hasApplicationExporter = applicationExporter;
        serverProperties.forEach((propertyName, value) -> {
            if (!isExporterProperty(propertyName) && (!hasApplicationExporter || !isExporterConfiguration(propertyName))) {
                properties.put(propertyName, value);
            }
        });
        properties.put(OTEL_SDK_DISABLED, "true");
        for (String propertyName : deploymentConfig.getPropertyNames()) {
            if (propertyName.startsWith("otel.") || propertyName.startsWith("OTEL_")) {
                if (isExporterProperty(propertyName) && !usesApplicationProperty(deploymentConfig, propertyName)) {
                    continue;
                }
                ConfigValue value = deploymentConfig.getConfigValue(propertyName);
                if (value.getValue() != null) {
                    properties.put(propertyName, value.getValue());
                }
            }
        }
        return properties;
    }

    /**
     * Returns whether a property configures an exporter endpoint or credentials.
     *
     * @param propertyName the property name
     * @return true for exporter transport properties
     */
    private static boolean isExporterConfiguration(String propertyName) {
        return propertyName.startsWith("otel.exporter.") || propertyName.startsWith("OTEL_EXPORTER_");
    }

    /**
     * Returns whether a property configures an exporter selector (not endpoint/credentials).
     *
     * @param propertyName the property name
     * @return true for exporter selector properties only (logs/metrics/traces exporter)
     */
    private static boolean isExporterProperty(String propertyName) {
        // Only filter signal exporter selectors, not endpoint/credential properties
        return propertyName.equals(WildFlyOpenTelemetryConfig.OTEL_LOGS_EXPORTER)
                || propertyName.equals(WildFlyOpenTelemetryConfig.OTEL_METRICS_EXPORTER)
                || propertyName.equals(WildFlyOpenTelemetryConfig.OTEL_TRACES_EXPORTER)
                || propertyName.equals("OTEL_LOGS_EXPORTER")
                || propertyName.equals("OTEL_METRICS_EXPORTER")
                || propertyName.equals("OTEL_TRACES_EXPORTER");
    }

    /**
     * Determines whether a property's winning value comes from application-local configuration.
     *
     * @param config the deployment MicroProfile Config
     * @param propertyName the property whose source is classified
     * @return true unless the winning source is the environment or system properties
     */
    private static boolean usesApplicationProperty(Config config, String propertyName) {
        ConfigValue value = config.getConfigValue(propertyName);
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
