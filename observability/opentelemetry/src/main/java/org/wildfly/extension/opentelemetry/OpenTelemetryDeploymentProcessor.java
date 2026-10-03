/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.extension.opentelemetry;

import static org.jboss.as.weld.Capabilities.WELD_CAPABILITY_NAME;
import static org.wildfly.extension.opentelemetry.OpenTelemetryExtensionLogger.OTEL_LOGGER;
import static org.wildfly.extension.opentelemetry.api.WildFlyOpenTelemetryConfig.OTEL_SDK_DISABLED;

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
import io.opentelemetry.sdk.trace.samplers.Sampler;
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
import org.wildfly.extension.opentelemetry.exporters.AggregatingMetricExporter;
import org.wildfly.extension.opentelemetry.service.OpenTelemetryService;

/**
 * Creates deployment-scoped OpenTelemetry providers and releases them when the deployment stops.
 */
public class OpenTelemetryDeploymentProcessor implements DeploymentUnitProcessor {
    /** Deployment attachment containing effective OpenTelemetry properties and exporter ownership. */
    public static final AttachmentKey<DeploymentTelemetryConfig> CONFIG_ATTACHMENT_KEY =
            AttachmentKey.create(DeploymentTelemetryConfig.class);

    /** Service supplied by the deployment dependency before POST_MODULE processing. */
    static final AttachmentKey<OpenTelemetryService> SERVICE_ATTACHMENT_KEY =
            AttachmentKey.create(OpenTelemetryService.class);

    private static final AttachmentKey<DeploymentHandle> HANDLE_KEY =
            AttachmentKey.create(DeploymentHandle.class);

    /** Creates a processor that reads the ready OpenTelemetry service from each deployment. */
    public OpenTelemetryDeploymentProcessor() {
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
                // Jakarta RESTful Web Services require Jakarta Contexts and Dependency Injection,
                // without which, there's no integration needed
                OTEL_LOGGER.debug("The deployment does not have Jakarta Contexts and Dependency Injection enabled. Skipping OpenTelemetry integration.");
                return;
            }

            final OpenTelemetryService service = deploymentUnit.getAttachment(SERVICE_ATTACHMENT_KEY);
            final OpenTelemetry serverOtel = service.getOpenTelemetry();
            final String deploymentName = getDeploymentName(deploymentUnit);
            final ClassLoader deploymentClassLoader = deploymentUnit.getAttachment(Attachments.MODULE).getClassLoader();
            final DeploymentTelemetryConfig attachedConfig = deploymentUnit.getAttachment(CONFIG_ATTACHMENT_KEY);
            final DeploymentTelemetryConfig config = attachedConfig == null
                    ? new DeploymentTelemetryConfig(false)
                    : attachedConfig;

            // Clean up any existing registration (handles redeploy scenario)
            service.unregisterDeploymentMetricReader(deploymentName);

            final DeploymentHandle handle = new DeploymentHandle(deploymentName, deploymentClassLoader, service);
            deploymentUnit.putAttachment(HANDLE_KEY, handle);
            try {
                if (Boolean.parseBoolean(config.properties().get(OTEL_SDK_DISABLED))) {
                    OpenTelemetry disabled = OpenTelemetry.noop();
                    service.routingOpenTelemetry.register(deploymentClassLoader, disabled);
                    weldCapability.registerExtensionInstance(new OpenTelemetryCdiExtension(disabled), deploymentUnit);
                    return;
                }
                // Reactive Messaging can cache its tracer before CDI creates the deployment SDK.
                // Route it to the server SDK until the deployment registration below
                // replaces this temporary delegate.
                service.routingOpenTelemetry.register(deploymentClassLoader, serverOtel);
                final Supplier<OpenTelemetry> deploymentOpenTelemetry =
                        () -> createDeploymentOpenTelemetry(config, service, serverOtel,
                                                             getServiceName(deploymentUnit), handle);
                final OpenTelemetryCdiExtension extension = config.hasApplicationExporters()
                        || config.hasApplicationSamplerOverride()
                        ? new OpenTelemetryCdiExtension(deploymentOpenTelemetry)
                        : new OpenTelemetryCdiExtension(deploymentOpenTelemetry.get());
                weldCapability.registerExtensionInstance(extension, deploymentUnit);
            } catch (RuntimeException cause) {
                deploymentUnit.removeAttachment(HANDLE_KEY);
                handle.close();
                throw new DeploymentUnitProcessingException(
                        "Failed to install OpenTelemetry for " + deploymentUnit.getName(), cause);
            }
        } catch (CapabilityServiceSupport.NoSuchCapabilityException e) {
            throw OTEL_LOGGER.deploymentRequiresCapability(deploymentPhaseContext.getDeploymentUnit().getName(),
                    WELD_CAPABILITY_NAME);
        } catch (RuntimeException cause) {
            throw new DeploymentUnitProcessingException(
                    "Failed to configure application OpenTelemetry exporters for "
                            + deploymentUnit.getName(), cause);
        }
    }

    /** {@inheritDoc} */
    @Override
    public void undeploy(DeploymentUnit deploymentUnit) {
        DeploymentHandle handle = deploymentUnit.removeAttachment(HANDLE_KEY);

        if (handle != null) {
            handle.close();
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
        final DeploymentMetricReader reader = config.usesApplicationExporter(Signal.METRICS)
                ? null
                : new DeploymentMetricReader(AggregationTemporality.DELTA);
        handle.metricReaderHandle = reader == null
                ? null
                : new AggregatingMetricExporter.DeploymentMetricReaderHandle(reader, handle.deploymentName);
        handle.applicationOpenTelemetry = config.hasApplicationExporters() || config.hasApplicationSamplerOverride()
                ? createApplicationOpenTelemetry(config, handle.deploymentClassLoader, service,
                        handle.deploymentName, defaultServiceName,
                        config.hasApplicationExporters() ? reader : null)
                : null;

        final MeterProvider meterProvider =
                configureApplicationMetricsExporter(config, service, handle, deploymentResource, reader);

        // Keep server span processors while allowing the deployment to choose its own sampler.
        final TracerProvider tracerProvider =
                configureApplicationTracerProvider(config, service, serverOpenTelemetry, handle,
                                  deploymentResource);

        // Create a deployment-isolated logger provider so log records carry the same deployment resource as
        // metrics and traces while sharing the server's exporter.
        final LoggerProvider loggerProvider =
                configureApplicationLoggerProvider(config, service, serverOpenTelemetry, handle,
                                  deploymentResource);

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
        // Cached connector tracers resolve this registration when they start a span.
        service.routingOpenTelemetry.register(handle.deploymentClassLoader, deploymentOpenTelemetry);

        return deploymentOpenTelemetry;
    }

    private static LoggerProvider configureApplicationLoggerProvider(DeploymentTelemetryConfig config,
                                                    OpenTelemetryService service,
                                                    OpenTelemetry serverOpenTelemetry,
                                                    DeploymentHandle handle,
                                                    Resource deploymentResource) {
        handle.loggerProvider = config.usesApplicationExporter(Signal.LOGS)
                ? null
                : service.createDeploymentLoggerProvider(deploymentResource);
        final LoggerProvider loggerProvider = config.usesApplicationExporter(Signal.LOGS)
                ? handle.applicationOpenTelemetry.getSdkLoggerProvider()
                : handle.loggerProvider != null
                        ? handle.loggerProvider
                        : serverOpenTelemetry.getLogsBridge();
        return loggerProvider;
    }

    /**
     * Selects the application tracer provider or shares server export processors with the resolved deployment sampler.
     *
     * @param config the effective deployment configuration
     * @param service the server OpenTelemetry service
     * @param serverOpenTelemetry the server fallback when its tracer provider is disabled
     * @param handle the deployment lifecycle state
     * @param deploymentResource the deployment resource
     * @return the tracer provider exposed to the deployment
     */
    private static TracerProvider configureApplicationTracerProvider(DeploymentTelemetryConfig config,
                                                    OpenTelemetryService service,
                                                    OpenTelemetry serverOpenTelemetry,
                                                    DeploymentHandle handle,
                                                    Resource deploymentResource) {
        Sampler deploymentSampler = config.hasApplicationSamplerOverride()
                ? handle.applicationOpenTelemetry.getSdkTracerProvider().getSampler()
                : null;
        handle.tracerProvider = config.usesApplicationExporter(Signal.TRACES)
                ? null
                : service.createDeploymentTracerProvider(deploymentResource, deploymentSampler);
        final TracerProvider tracerProvider = config.usesApplicationExporter(Signal.TRACES)
                ? handle.applicationOpenTelemetry.getSdkTracerProvider()
                : handle.tracerProvider != null
                        ? handle.tracerProvider
                        : serverOpenTelemetry.getTracerProvider();
        return tracerProvider;
    }

    /**
     * Uses the auxiliary SDK for application exporters, otherwise creates a server-exported meter provider.
     *
     * @param config the effective deployment configuration
     * @param service the server OpenTelemetry service
     * @param handle the deployment lifecycle state
     * @param deploymentResource the deployment resource for a standalone meter provider
     * @param reader the reader for server-managed metrics, or {@code null} for application-owned metrics
     * @return the meter provider exposed to the deployment
     */
    private static MeterProvider configureApplicationMetricsExporter(DeploymentTelemetryConfig config,
                                                  OpenTelemetryService service,
                                                  DeploymentHandle handle,
                                                  Resource deploymentResource,
                                                  DeploymentMetricReader reader) {
        if (config.hasApplicationExporters()) {
            return handle.applicationOpenTelemetry.getSdkMeterProvider();
        }
        handle.meterProvider = service.createDeploymentMeterProvider(deploymentResource, reader);
        return handle.meterProvider;
    }

    /**
     * Builds the auxiliary SDK for application exporters or sampler resolution.
     *
     * @param config the effective deployment configuration
     * @param deploymentClassLoader the deployment class loader used for provider discovery
     * @param service the server OpenTelemetry service used to merge deployment resources
     * @param deploymentName the canonical deployment name
     * @param defaultServiceName the deployment-derived service name
     * @param reader the reader for server-managed metrics, or {@code null} for application-owned metrics or sampler-only configuration
     * @return the deployment-owned SDK
     */
    private OpenTelemetrySdk createApplicationOpenTelemetry(DeploymentTelemetryConfig config,
                                                             ClassLoader deploymentClassLoader,
                                                             OpenTelemetryService service,
                                                             String deploymentName,
                                                             String defaultServiceName,
                                                             DeploymentMetricReader reader) {
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
                .addMeterProviderCustomizer((builder, ignored) ->
                        reader == null ? builder : builder.registerMetricReader(reader))
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
        final OpenTelemetryService service;
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
         * @param service the server service that owns this deployment's registrations
         */
        DeploymentHandle(String deploymentName, ClassLoader deploymentClassLoader, OpenTelemetryService service) {
            this.deploymentName = deploymentName;
            this.deploymentClassLoader = deploymentClassLoader;
            this.service = service;
        }

        /**
         * Removes registrations and closes deployment-owned providers through the captured service.
         * The service supplier may already be empty when undeployment runs during a server reload.
         */
        void close() {
            service.routingOpenTelemetry.unregister(deploymentClassLoader);
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
