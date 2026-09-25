/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.extension.opentelemetry;

import static org.jboss.as.weld.Capabilities.WELD_CAPABILITY_NAME;
import static org.wildfly.extension.opentelemetry.OpenTelemetryExtensionLogger.OTEL_LOGGER;
import static org.wildfly.extension.opentelemetry.api.WildFlyOpenTelemetryConfig.OTEL_SERVICE_NAME;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.logs.LoggerProvider;
import io.opentelemetry.api.trace.TracerProvider;
import io.opentelemetry.sdk.autoconfigure.ResourceConfiguration;
import io.opentelemetry.sdk.autoconfigure.spi.internal.DefaultConfigProperties;
import io.opentelemetry.sdk.logs.SdkLoggerProvider;
import io.opentelemetry.sdk.metrics.SdkMeterProvider;
import io.opentelemetry.sdk.metrics.data.AggregationTemporality;
import io.opentelemetry.sdk.resources.Resource;
import io.opentelemetry.sdk.trace.SdkTracerProvider;
import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.config.spi.ConfigProviderResolver;
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
import org.jboss.modules.Module;
import org.wildfly.extension.opentelemetry.api.DeploymentOpenTelemetry;
import org.wildfly.extension.opentelemetry.api.OpenTelemetryCdiExtension;

/**
 * Creates deployment-scoped OpenTelemetry providers and releases them when the deployment stops.
 */
class OpenTelemetryDeploymentProcessor implements DeploymentUnitProcessor {
    private static final String MICROPROFILE_CONFIG_CAPABILITY = "org.wildfly.microprofile.config";
    private static final String OTEL_RESOURCE_ATTRIBUTES = "otel.resource.attributes";
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

            OpenTelemetryService service = openTelemetryService.get();
            OpenTelemetry serverOtel = service.getOpenTelemetry();
            String deploymentName = getDeploymentName(deploymentUnit);
            ClassLoader deploymentClassLoader = deploymentUnit.getAttachment(Attachments.MODULE).getClassLoader();

            // Clean up any existing registration (handles redeploy scenario)
            service.unregisterDeploymentMetricReader(deploymentName);

            // Create deployment-isolated meter provider with DELTA temporality
            DeploymentMetricReader reader = new DeploymentMetricReader(AggregationTemporality.DELTA);

            // Build deployment resource (merges server resource with deployment-specific attributes)
            Resource deploymentResource = service.createDeploymentResource(deploymentName,
                    getServiceName(deploymentUnit), getDeploymentConfigResource(deploymentUnit));

            SdkMeterProvider deploymentMeter = service.createDeploymentMeterProvider(deploymentResource, reader);

            // Register with server for aggregation
            AggregatingMetricExporter.DeploymentMetricReaderHandle handle =
                    new AggregatingMetricExporter.DeploymentMetricReaderHandle(reader, deploymentName);
            service.registerDeploymentMetricReader(deploymentName, handle);

            // Reuse the server's configured sampler and span processor so deployments retain all tracing settings.
            SdkTracerProvider deploymentTracerProvider = service.createDeploymentTracerProvider(deploymentResource);
            TracerProvider tracerProvider = deploymentTracerProvider != null
                    ? deploymentTracerProvider
                    : serverOtel.getTracerProvider();

            // Create a deployment-isolated logger provider so log records carry the same deployment resource as
            // metrics and traces while sharing the server's exporter.
            SdkLoggerProvider deploymentLoggerProvider = service.createDeploymentLoggerProvider(deploymentResource);
            LoggerProvider loggerProvider = deploymentLoggerProvider != null
                    ? deploymentLoggerProvider
                    : serverOtel.getLogsBridge();

            // Create wrapped OpenTelemetry for this deployment
            DeploymentOpenTelemetry deploymentOtel = new DeploymentOpenTelemetry(
                    tracerProvider,
                    loggerProvider,
                    deploymentMeter,
                    serverOtel.getPropagators()
            );

            if (deploymentLoggerProvider != null) {
                service.registerDeploymentLogHandler(deploymentClassLoader, deploymentOtel);
            }

            // Store for cleanup on undeploy
            deploymentUnit.putAttachment(HANDLE_KEY, new DeploymentHandle(deploymentName, deploymentClassLoader,
                    handle, deploymentMeter, deploymentTracerProvider, deploymentLoggerProvider));

            // Register CDI extension with wrapped instance
            weldCapability.registerExtensionInstance(
                    new OpenTelemetryCdiExtension(deploymentOtel), deploymentUnit);
        } catch (CapabilityServiceSupport.NoSuchCapabilityException e) {
            throw OTEL_LOGGER.deploymentRequiresCapability(deploymentPhaseContext.getDeploymentUnit().getName(),
                    WELD_CAPABILITY_NAME);
        }
    }

    /** {@inheritDoc} */
    @Override
    public void undeploy(DeploymentUnit deploymentUnit) {
        DeploymentHandle handle = deploymentUnit.getAttachment(HANDLE_KEY);

        if (handle != null) {
            // Unregister from server tracking
            OpenTelemetryService service = openTelemetryService.get();
            service.unregisterDeploymentMetricReader(handle.deploymentName, handle.metricReaderHandle);
            service.unregisterDeploymentLogHandler(handle.deploymentClassLoader);

            // Shutdown the meter provider
            handle.meterProvider.close();

            // Shutdown the deployment tracer provider. Its wrapper flushes but does not close the shared processor.
            if (handle.tracerProvider != null) {
                handle.tracerProvider.close();
            }

            if (handle.loggerProvider != null) {
                handle.loggerProvider.close();
            }
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
     * Reads deployment-scoped OpenTelemetry resource configuration when MicroProfile Config is available.
     *
     * @param deploymentUnit the deployment whose configuration is read
     * @return the configured deployment resource, or an empty resource when configuration is unavailable
     */
    private Resource getDeploymentConfigResource(DeploymentUnit deploymentUnit) {
        CapabilityServiceSupport capabilitySupport =
                deploymentUnit.getAttachment(Attachments.CAPABILITY_SERVICE_SUPPORT);
        if (!capabilitySupport.hasCapability(MICROPROFILE_CONFIG_CAPABILITY)) {
            return Resource.empty();
        }

        Module module = deploymentUnit.getAttachment(Attachments.MODULE);
        Config config = ConfigProviderResolver.instance().getConfig(module.getClassLoader());
        return createDeploymentConfigResource(
                config.getOptionalValue(OTEL_SERVICE_NAME, String.class).orElse(null),
                config.getOptionalValue(OTEL_RESOURCE_ATTRIBUTES, String.class).orElse(null));
    }

    /**
     * Uses the OpenTelemetry SDK parser so deployment resource attributes follow standard escaping rules.
     *
     * @param serviceName the optional deployment service name
     * @param resourceAttributes the optional encoded resource attributes
     * @return the resource represented by the deployment configuration
     */
    static Resource createDeploymentConfigResource(String serviceName, String resourceAttributes) {
        Map<String, String> properties = new HashMap<>();
        if (serviceName != null) {
            properties.put(OTEL_SERVICE_NAME, serviceName);
        }
        if (resourceAttributes != null) {
            properties.put(OTEL_RESOURCE_ATTRIBUTES, resourceAttributes);
        }
        return ResourceConfiguration.createEnvironmentResource(DefaultConfigProperties.createFromMap(properties));
    }

    /** Holds the deployment-owned providers and registrations required during undeployment. */
    private static class DeploymentHandle {
        final String deploymentName;
        final ClassLoader deploymentClassLoader;
        final AggregatingMetricExporter.DeploymentMetricReaderHandle metricReaderHandle;
        final SdkMeterProvider meterProvider;
        final SdkTracerProvider tracerProvider;
        final SdkLoggerProvider loggerProvider;

        /**
         * Captures the deployment-owned telemetry state that must be released together.
         *
         * @param deploymentName the canonical deployment name
         * @param deploymentClassLoader the deployment class loader used for log routing
         * @param metricReaderHandle the exact metric-reader registration owned by this deployment
         * @param meterProvider the deployment meter provider
         * @param tracerProvider the deployment tracer provider, or {@code null} when tracing is disabled
         * @param loggerProvider the deployment logger provider, or {@code null} when logging is disabled
         */
        DeploymentHandle(String deploymentName, ClassLoader deploymentClassLoader,
                         AggregatingMetricExporter.DeploymentMetricReaderHandle metricReaderHandle,
                         SdkMeterProvider meterProvider, SdkTracerProvider tracerProvider,
                         SdkLoggerProvider loggerProvider) {
            this.deploymentName = deploymentName;
            this.deploymentClassLoader = deploymentClassLoader;
            this.metricReaderHandle = metricReaderHandle;
            this.meterProvider = meterProvider;
            this.tracerProvider = tracerProvider;
            this.loggerProvider = loggerProvider;
        }
    }
}
