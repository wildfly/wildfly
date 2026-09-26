/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.extension.opentelemetry;

import static org.jboss.as.weld.Capabilities.WELD_CAPABILITY_NAME;
import static org.wildfly.extension.opentelemetry.OpenTelemetryExtensionLogger.OTEL_LOGGER;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.logs.LoggerProvider;
import io.opentelemetry.api.metrics.MeterProvider;
import io.opentelemetry.api.trace.TracerProvider;
import io.opentelemetry.sdk.OpenTelemetrySdk;
import io.opentelemetry.sdk.autoconfigure.AutoConfiguredOpenTelemetrySdk;
import io.opentelemetry.sdk.autoconfigure.ResourceConfiguration;
import io.opentelemetry.sdk.autoconfigure.spi.internal.DefaultConfigProperties;
import io.opentelemetry.sdk.logs.SdkLoggerProvider;
import io.opentelemetry.sdk.metrics.SdkMeterProvider;
import io.opentelemetry.sdk.metrics.data.AggregationTemporality;
import io.opentelemetry.sdk.resources.Resource;
import io.opentelemetry.sdk.trace.SdkTracerProvider;
import org.jboss.as.controller.capability.CapabilityServiceSupport;
import org.jboss.as.ee.structure.DeploymentType;
import org.jboss.as.ee.structure.DeploymentTypeMarker;
import org.jboss.as.server.deployment.Attachments;
import org.jboss.as.server.deployment.AttachmentKey;
import org.jboss.as.server.deployment.DeploymentPhaseContext;
import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.as.server.deployment.DeploymentUnitProcessingException;
import org.jboss.as.server.deployment.DeploymentUnitProcessor;
import org.jboss.as.weld.WeldCapability;
import org.wildfly.extension.opentelemetry.DeploymentTelemetryConfig.Signal;
import org.wildfly.extension.opentelemetry.api.DeploymentOpenTelemetry;
import org.wildfly.extension.opentelemetry.api.OpenTelemetryCdiExtension;

/**
 * Creates deployment-scoped OpenTelemetry providers and releases them when the deployment stops.
 */
public class OpenTelemetryDeploymentProcessor implements DeploymentUnitProcessor {
    /** Deployment attachment containing effective OpenTelemetry properties and exporter ownership. */
    public static final AttachmentKey<DeploymentTelemetryConfig> CONFIG_ATTACHMENT_KEY =
            AttachmentKey.create(DeploymentTelemetryConfig.class);

    private static final AttachmentKey<DeploymentHandle> HANDLE_KEY =
            AttachmentKey.create(DeploymentHandle.class);

    private final Supplier<OpenTelemetryService> openTelemetryService;

    /**
     * Creates a deployment processor backed by the server OpenTelemetry service.
     *
     * @param openTelemetryService supplies the active server service
     */
    public OpenTelemetryDeploymentProcessor(Supplier<OpenTelemetryService> openTelemetryService) {
        this.openTelemetryService = openTelemetryService;
    }

    /** {@inheritDoc} */
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
                OTEL_LOGGER.debug("The deployment does not have Jakarta Contexts and Dependency Injection enabled. Skipping OpenTelemetry integration.");
                return;
            }

            final OpenTelemetryService service = openTelemetryService.get();
            final OpenTelemetry serverOtel = service.getOpenTelemetry();
            final String deploymentName = getDeploymentName(deploymentUnit);
            final String defaultServiceName = getServiceName(deploymentUnit);
            final ClassLoader deploymentClassLoader = deploymentUnit.getAttachment(Attachments.MODULE).getClassLoader();
            final DeploymentTelemetryConfig attachedConfig = deploymentUnit.getAttachment(CONFIG_ATTACHMENT_KEY);
            final DeploymentTelemetryConfig config = attachedConfig == null
                    ? new DeploymentTelemetryConfig(Collections.emptyMap(), Collections.emptySet())
                    : attachedConfig;

            // Clean up any existing registration (handles redeploy scenario)
            service.unregisterDeploymentMetricReader(deploymentName);

            final DeploymentHandle handle = new DeploymentHandle(deploymentName, deploymentClassLoader);
            deploymentUnit.putAttachment(HANDLE_KEY, handle);
            try {
                final Supplier<OpenTelemetry> deploymentOpenTelemetry = () -> createDeploymentOpenTelemetry(
                        config, service, serverOtel, defaultServiceName, handle);
                final OpenTelemetryCdiExtension extension = config.hasApplicationExporters()
                        ? new OpenTelemetryCdiExtension(deploymentOpenTelemetry)
                        : new OpenTelemetryCdiExtension(deploymentOpenTelemetry.get());
                weldCapability.registerExtensionInstance(extension, deploymentUnit);
            } catch (RuntimeException cause) {
                deploymentUnit.removeAttachment(HANDLE_KEY);
                handle.close(service);
                throw new DeploymentUnitProcessingException(
                        "Failed to install OpenTelemetry for " + deploymentUnit.getName(), cause);
            }
        } catch (CapabilityServiceSupport.NoSuchCapabilityException e) {
            throw OTEL_LOGGER.deploymentRequiresCapability(deploymentPhaseContext.getDeploymentUnit().getName(),
                    WELD_CAPABILITY_NAME);
        } catch (RuntimeException cause) {
            throw new DeploymentUnitProcessingException(
                    "Failed to configure application OpenTelemetry exporters for "
                            + deploymentUnit.getName(),
                    cause);
        }
    }

    /** {@inheritDoc} */
    @Override
    public void undeploy(DeploymentUnit deploymentUnit) {
        DeploymentHandle handle = deploymentUnit.removeAttachment(HANDLE_KEY);

        if (handle != null) {
            handle.close(openTelemetryService.get());
        }
    }

    /**
     * Returns the canonical deployment name used as the metric-reader registry key.
     *
     * @param deploymentUnit the deployment
     * @return the canonical deployment name
     */
    private String getDeploymentName(DeploymentUnit deploymentUnit) {
        return deploymentUnit.getServiceName().getCanonicalName();
    }

    /**
     * Returns the default service name exposed in telemetry resources.
     *
     * @param deploymentUnit the deployment
     * @return the deployment service name, including its parent archive when present
     */
    private String getServiceName(DeploymentUnit deploymentUnit) {
        String serviceName = deploymentUnit.getServiceName().getSimpleName();
        if (null != deploymentUnit.getParent()) {
            serviceName = deploymentUnit.getParent().getServiceName().getSimpleName() + "!" + serviceName;
        }
        return serviceName;
    }

    /**
     * Creates and registers the signal providers exposed to one deployment.
     *
     * @param config the effective deployment configuration
     * @param service the server OpenTelemetry service
     * @param serverOpenTelemetry the server OpenTelemetry instance
     * @param defaultServiceName the deployment-derived service name
     * @param handle the deployment lifecycle state populated by this method
     * @return the deployment-specific OpenTelemetry view
     */
    private OpenTelemetry createDeploymentOpenTelemetry(DeploymentTelemetryConfig config,
                                                         OpenTelemetryService service,
                                                         OpenTelemetry serverOpenTelemetry,
                                                         String defaultServiceName,
                                                         DeploymentHandle handle) {
        final Resource deploymentResource = service.createDeploymentResource(handle.deploymentName,
                defaultServiceName, createDeploymentConfigResource(config.properties()));
        handle.applicationOpenTelemetry = config.hasApplicationExporters()
                ? createApplicationOpenTelemetry(config, handle.deploymentClassLoader, service,
                        handle.deploymentName, defaultServiceName)
                : null;

        final DeploymentMetricReader reader = config.usesApplicationExporter(Signal.METRICS)
                ? null
                : new DeploymentMetricReader(AggregationTemporality.DELTA);
        handle.meterProvider = reader == null
                ? null
                : service.createDeploymentMeterProvider(deploymentResource, reader);
        handle.metricReaderHandle = reader == null
                ? null
                : new AggregatingMetricExporter.DeploymentMetricReaderHandle(reader, handle.deploymentName);
        final MeterProvider meterProvider = config.usesApplicationExporter(Signal.METRICS)
                ? handle.applicationOpenTelemetry.getSdkMeterProvider()
                : handle.meterProvider;

        // Reuse the server's configured sampler and span processor so deployments retain all tracing settings.
        handle.tracerProvider = config.usesApplicationExporter(Signal.TRACES)
                ? null
                : service.createDeploymentTracerProvider(deploymentResource);
        final TracerProvider tracerProvider = config.usesApplicationExporter(Signal.TRACES)
                ? handle.applicationOpenTelemetry.getSdkTracerProvider()
                : handle.tracerProvider != null
                        ? handle.tracerProvider
                        : serverOpenTelemetry.getTracerProvider();

        // Create a deployment-isolated logger provider so log records carry the same deployment resource as
        // metrics and traces while sharing the server's exporter.
        handle.loggerProvider = config.usesApplicationExporter(Signal.LOGS)
                ? null
                : service.createDeploymentLoggerProvider(deploymentResource);
        final LoggerProvider loggerProvider = config.usesApplicationExporter(Signal.LOGS)
                ? handle.applicationOpenTelemetry.getSdkLoggerProvider()
                : handle.loggerProvider != null
                        ? handle.loggerProvider
                        : serverOpenTelemetry.getLogsBridge();

        final DeploymentOpenTelemetry deploymentOpenTelemetry = new DeploymentOpenTelemetry(
                tracerProvider,
                loggerProvider,
                meterProvider,
                config.usesApplicationExporter(Signal.TRACES)
                        ? handle.applicationOpenTelemetry.getPropagators()
                        : serverOpenTelemetry.getPropagators()
        );

        if (handle.metricReaderHandle != null) {
            service.registerDeploymentMetricReader(handle.deploymentName, handle.metricReaderHandle);
        }
        if (config.usesApplicationExporter(Signal.LOGS) || handle.loggerProvider != null) {
            service.registerDeploymentLogHandler(handle.deploymentClassLoader, deploymentOpenTelemetry);
        }
        return deploymentOpenTelemetry;
    }

    /**
     * Builds the auxiliary SDK that owns application-selected signal pipelines.
     *
     * @param config the effective deployment configuration
     * @param deploymentClassLoader the deployment class loader used for provider discovery
     * @param service the server OpenTelemetry service used to merge deployment resources
     * @param deploymentName the canonical deployment name
     * @param defaultServiceName the deployment-derived service name
     * @return the deployment-owned SDK
     */
    private OpenTelemetrySdk createApplicationOpenTelemetry(DeploymentTelemetryConfig config,
                                                             ClassLoader deploymentClassLoader,
                                                             OpenTelemetryService service,
                                                             String deploymentName,
                                                             String defaultServiceName) {
        Map<String, String> properties = new HashMap<>(config.properties());
        for (Signal signal : Signal.values()) {
            if (!config.usesApplicationExporter(signal)) {
                properties.put(signal.exporterProperty(), "none");
            }
        }

        return AutoConfiguredOpenTelemetrySdk.builder()
                .setServiceClassLoader(deploymentClassLoader)
                .addPropertiesCustomizer(ignored -> properties)
                .addResourceCustomizer((resource, ignored) ->
                        service.createDeploymentResource(deploymentName, defaultServiceName, resource))
                .disableShutdownHook()
                .build()
                .getOpenTelemetrySdk();
    }

    /**
     * Uses the OpenTelemetry SDK parser so deployment resource attributes follow standard escaping rules.
     *
     * @param properties the deployment resource configuration properties
     * @return the resource represented by the deployment configuration
     */
    static Resource createDeploymentConfigResource(Map<String, String> properties) {
        return ResourceConfiguration.createEnvironmentResource(DefaultConfigProperties.createFromMap(properties));
    }

    /** Holds the deployment-owned providers and registrations required during undeployment. */
    private static final class DeploymentHandle {
        final String deploymentName;
        final ClassLoader deploymentClassLoader;
        AggregatingMetricExporter.DeploymentMetricReaderHandle metricReaderHandle;
        SdkMeterProvider meterProvider;
        SdkTracerProvider tracerProvider;
        SdkLoggerProvider loggerProvider;
        OpenTelemetrySdk applicationOpenTelemetry;

        /**
         * Starts tracking deployment-owned telemetry state before resource construction begins.
         *
         * @param deploymentName the canonical deployment name
         * @param deploymentClassLoader the deployment class loader used for log routing
         */
        DeploymentHandle(String deploymentName, ClassLoader deploymentClassLoader) {
            this.deploymentName = deploymentName;
            this.deploymentClassLoader = deploymentClassLoader;
        }

        /**
         * Removes server registrations and closes every provider owned by this deployment.
         *
         * @param service the server OpenTelemetry service holding deployment registrations
         */
        void close(OpenTelemetryService service) {
            service.unregisterDeploymentLogHandler(deploymentClassLoader);
            if (metricReaderHandle != null) {
                service.unregisterDeploymentMetricReader(deploymentName, metricReaderHandle);
            }
            if (meterProvider != null) {
                meterProvider.close();
            }
            if (tracerProvider != null) {
                tracerProvider.close();
            }
            if (loggerProvider != null) {
                loggerProvider.close();
            }
            if (applicationOpenTelemetry != null) {
                applicationOpenTelemetry.close();
            }
        }
    }
}
