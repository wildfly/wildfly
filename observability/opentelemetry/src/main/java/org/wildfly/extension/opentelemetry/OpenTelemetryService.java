/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.extension.opentelemetry;

import static org.wildfly.extension.opentelemetry.OpenTelemetryExtensionLogger.OTEL_LOGGER;
import static org.wildfly.extension.opentelemetry.api.WildFlyOpenTelemetryConfig.OTEL_SDK_DISABLED;
import static org.wildfly.extension.opentelemetry.api.WildFlyOpenTelemetryConfig.OTEL_SERVICE_NAME;

import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.LogManager;

import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.context.Context;
import io.opentelemetry.sdk.OpenTelemetrySdk;
import io.opentelemetry.sdk.autoconfigure.AutoConfiguredOpenTelemetrySdk;
import io.opentelemetry.sdk.autoconfigure.AutoConfiguredOpenTelemetrySdkBuilder;
import io.opentelemetry.sdk.common.CompletableResultCode;
import io.opentelemetry.sdk.logs.SdkLoggerProvider;
import io.opentelemetry.sdk.logs.data.LogRecordData;
import io.opentelemetry.sdk.logs.export.BatchLogRecordProcessor;
import io.opentelemetry.sdk.logs.export.LogRecordExporter;
import io.opentelemetry.sdk.metrics.SdkMeterProvider;
import io.opentelemetry.sdk.resources.Resource;
import io.opentelemetry.sdk.trace.ReadWriteSpan;
import io.opentelemetry.sdk.trace.ReadableSpan;
import io.opentelemetry.sdk.trace.SdkTracerProvider;
import io.opentelemetry.sdk.trace.SdkTracerProviderBuilder;
import io.opentelemetry.sdk.trace.SpanProcessor;
import io.opentelemetry.sdk.trace.samplers.Sampler;
import io.opentelemetry.semconv.ServiceAttributes;
import io.opentelemetry.instrumentation.runtimemetrics.java8.RuntimeMetrics;
import org.wildfly.extension.opentelemetry.api.WildFlyOpenTelemetryConfig;
import org.wildfly.security.manager.WildFlySecurityManager;

/**
 * Server-level OpenTelemetry service that builds and manages the shared OpenTelemetry instance.
 * Follows the pattern of MicrometerService for consistency with WildFly's observability architecture.
 */
public class OpenTelemetryService {
    /**
     * Resource attribute key identifying the deployment. Mirrors the {@code deployment.name} OpenTelemetry
     * semantic convention, defined locally because that convention is only available at test scope.
     */
    private static final AttributeKey<String> DEPLOYMENT_NAME = AttributeKey.stringKey("deployment.name");

    private final OpenTelemetry openTelemetry;
    private final String configuredServiceName;
    private final ConcurrentHashMap<String, AggregatingMetricExporter.DeploymentMetricReaderHandle> deploymentMetricReaders;
    private final List<AutoCloseable> closeables = new ArrayList<>();
    private final List<SpanProcessor> spanProcessors = new ArrayList<>();
    private volatile Resource serverResource = Resource.getDefault();
    private RoutingOpenTelemetryLogHandler logHandler;

    private volatile Sampler sampler;
    private volatile AggregatingMetricExporter metricExporter;
    private volatile LogRecordExporter logRecordExporter;

    /**
     * Creates and configures the server OpenTelemetry runtime.
     *
     * @param config the resolved subsystem configuration
     */
    private OpenTelemetryService(WildFlyOpenTelemetryConfig config) {
        this.deploymentMetricReaders = new ConcurrentHashMap<>();
        this.configuredServiceName = config.properties().get(OTEL_SERVICE_NAME);
        this.openTelemetry = buildOpenTelemetry(config);
    }

    /**
     * Runs the given action under a privileged context only when a security manager is checking,
     * avoiding an unnecessary {@link AccessController} frame otherwise.
     *
     * @param action the action to run; use a {@code PrivilegedAction<Void>} returning {@code null} for void work
     * @param <T> the action's result type
     * @return the action's result
     */
    private static <T> T doPrivilegedIfChecking(PrivilegedAction<T> action) {
        return WildFlySecurityManager.isChecking() ? AccessController.doPrivileged(action) : action.run();
    }

    /**
     * Builds the configured SDK and captures the components deployments must share.
     *
     * @param config the resolved subsystem configuration
     * @return the configured SDK, or the no-op implementation when disabled
     */
    private OpenTelemetry buildOpenTelemetry(WildFlyOpenTelemetryConfig config) {
        Map<String, String> properties = config.properties();

        if (Boolean.parseBoolean(properties.get(OTEL_SDK_DISABLED))) {
            return OpenTelemetry.noop();
        }

        AutoConfiguredOpenTelemetrySdkBuilder builder = AutoConfiguredOpenTelemetrySdk.builder()
            .setServiceClassLoader(AutoConfiguredOpenTelemetrySdk.class.getClassLoader())
            .addPropertiesSupplier(config::properties)
            .disableShutdownHook()
            .addResourceCustomizer((resource, configProps) -> {
              this.serverResource = resource;
              return resource;
            })
            .addMetricExporterCustomizer((exporter, configProps) -> {
              OTEL_LOGGER.debugf("Wrapping metric exporter with AggregatingMetricExporter");
              this.metricExporter = new AggregatingMetricExporter(exporter, () -> deploymentMetricReaders);
              return metricExporter;
            })
            .addSamplerCustomizer((sampler, configProps) -> {
              this.sampler = sampler;
              return sampler;
            })
            .addSpanProcessorCustomizer((spanProcessor, configProps) -> {
              spanProcessors.add(spanProcessor);
              return spanProcessor;
            })
            .addLogRecordExporterCustomizer((exporter, configProps) -> {
              // Capture the exporter so deployments can export logs under their own service.name.
              this.logRecordExporter = exporter;
              return exporter;
            });

        // Requires FilePermission/RuntimePermission
        OpenTelemetrySdk otel = doPrivilegedIfChecking(() -> builder.build().getOpenTelemetrySdk());

        // Register JVM runtime metrics (e.g. jvm.class.count) on the server SDK so publication begins as soon as the
        // subsystem starts, mirroring SmallRye's OpenTelemetryProducer. Tracked so shutdown() unregisters the observers.
        // Requires access to JVM management beans and to register a GC notification listener
        closeables.add(doPrivilegedIfChecking(() -> RuntimeMetrics.create(otel)));
        return otel;
    }

    /**
     * Adds a class-loader-aware telemetry handler to the root logger.
     *
     * @param openTelemetry the server telemetry instance
     */
    private void installLogHandler(OpenTelemetry openTelemetry) {
        logHandler = new RoutingOpenTelemetryLogHandler(openTelemetry);
        LogManager.getLogManager().getLogger("").addHandler(logHandler);
    }

    /** Removes and closes the telemetry handler when it has been installed. */
    private void uninstallLogHandler() {
        if (logHandler != null) {
            LogManager.getLogManager().getLogger("").removeHandler(logHandler);
            logHandler.close();
            logHandler = null;
        }
    }

    /**
     * Returns the server OpenTelemetry instance.
     *
     * @return the configured server instance
     */
    public OpenTelemetry getOpenTelemetry() {
        return openTelemetry;
    }

    /**
     * Creates a deployment tracer provider with the server's configured sampler and span processors.
     *
     * @param resource the deployment resource
     * @return the provider, or {@code null} when the server SDK is disabled
     */
    SdkTracerProvider createDeploymentTracerProvider(Resource resource) {
        return sampler == null ? null : createDeploymentTracerProvider(resource, sampler, spanProcessors);
    }

    /**
     * Builds a deployment tracer provider without transferring ownership of server span processors.
     *
     * @param resource the deployment resource
     * @param sampler the configured server sampler
     * @param spanProcessors the configured server span processors
     * @return the deployment tracer provider
     */
    static SdkTracerProvider createDeploymentTracerProvider(Resource resource, Sampler sampler,
                                                             List<SpanProcessor> spanProcessors) {
        SdkTracerProviderBuilder builder = SdkTracerProvider.builder()
                .setResource(resource)
                .setSampler(sampler);
        for (SpanProcessor spanProcessor : spanProcessors) {
            builder.addSpanProcessor(new NonClosingSpanProcessor(spanProcessor));
        }
        return builder.build();
    }

    /**
     * Creates a deployment meter provider that feeds the given reader for aggregation into server exports.
     *
     * @param resource the deployment resource
     * @param reader the deployment metric reader collected during periodic exports
     * @return the deployment meter provider
     */
    SdkMeterProvider createDeploymentMeterProvider(Resource resource, DeploymentMetricReader reader) {
        return SdkMeterProvider.builder()
                .setResource(resource)
                .registerMetricReader(reader)
                .build();
    }

    /**
     * Creates a deployment logger provider that emits under the deployment resource while sharing the server exporter.
     *
     * @param resource the deployment resource
     * @return the deployment logger provider, or {@code null} when logging is disabled
     */
    SdkLoggerProvider createDeploymentLoggerProvider(Resource resource) {
        if (logRecordExporter == null) {
            return null;
        }
        return SdkLoggerProvider.builder()
                .setResource(resource)
                .addLogRecordProcessor(BatchLogRecordProcessor.builder(
                        new NonClosingLogRecordExporter(logRecordExporter)).build())
                .build();
    }

    /**
     * Registers deployment-specific log routing and lazily installs the root handler.
     *
     * @param deploymentClassLoader the deployment class loader used to identify log records
     * @param deploymentOpenTelemetry the deployment telemetry instance
     */
    public synchronized void registerDeploymentLogHandler(ClassLoader deploymentClassLoader,
                                                          OpenTelemetry deploymentOpenTelemetry) {
        // The logging subsystem configures the root logger after this service starts. Install lazily so that
        // configuration does not discard the handler before the first deployment can use it.
        if (logHandler == null) {
            // Requires LoggingPermission
            doPrivilegedIfChecking(() -> {
                installLogHandler(openTelemetry);
                return null;
            });
        }
        logHandler.register(deploymentClassLoader, deploymentOpenTelemetry);
    }

    /**
     * Removes log routing for a stopped deployment.
     *
     * @param deploymentClassLoader the deployment class loader to remove
     */
    public synchronized void unregisterDeploymentLogHandler(ClassLoader deploymentClassLoader) {
        if (logHandler != null) {
            logHandler.unregister(deploymentClassLoader);
        }
    }

    /**
     * Merges server and deployment resources while enforcing the deployment's identity attributes.
     *
     * @param deploymentName the canonical deployment name
     * @param defaultServiceName the service name used when no configured name exists
     * @param deploymentResource the deployment-scoped configured resource
     * @return the merged deployment resource
     */
    Resource createDeploymentResource(String deploymentName, String defaultServiceName,
                                      Resource deploymentResource) {
        return createDeploymentResource(serverResource, configuredServiceName, deploymentName, defaultServiceName,
                deploymentResource);
    }

    /**
     * Merges resource layers using deployment configuration before server and deployment name fallbacks.
     *
     * @param serverResource the autoconfigured server resource
     * @param configuredServiceName the subsystem service name
     * @param deploymentName the canonical deployment name
     * @param defaultServiceName the service name derived from the deployment
     * @param deploymentResource the deployment-scoped configured resource
     * @return the merged deployment resource
     */
    static Resource createDeploymentResource(Resource serverResource, String configuredServiceName,
                                             String deploymentName, String defaultServiceName,
                                             Resource deploymentResource) {
        String serviceName = deploymentResource.getAttribute(ServiceAttributes.SERVICE_NAME);
        serviceName = serviceName != null ? serviceName : configuredServiceName;
        if (serviceName == null) {
            serviceName = defaultServiceName;
        }

        Attributes attributes = Attributes.builder()
                .put(DEPLOYMENT_NAME, deploymentName)
                .put(ServiceAttributes.SERVICE_NAME, serviceName)
                .build();
        return serverResource.merge(deploymentResource).merge(Resource.create(attributes));
    }

    /**
     * Registers a deployment metric reader for inclusion in periodic exports.
     *
     * @param deploymentName the canonical deployment name
     * @param handle the deployment reader registration
     */
    public void registerDeploymentMetricReader(String deploymentName,
                                               AggregatingMetricExporter.DeploymentMetricReaderHandle handle) {
        OTEL_LOGGER.debugf("Registering deployment metric reader for: %s", deploymentName);
        deploymentMetricReaders.put(deploymentName, handle);
    }

    /**
     * Exports pending points and unregisters the named deployment metric reader.
     *
     * @param deploymentName the deployment to unregister
     */
    public void unregisterDeploymentMetricReader(String deploymentName) {
        OTEL_LOGGER.debugf("Unregistering deployment metric reader for: %s", deploymentName);
        unregisterDeploymentMetricReader(deploymentName, null, deploymentMetricReaders, metricExporter);
    }

    /**
     * Exports and unregisters a deployment reader only when the current registration is the expected handle.
     *
     * @param deploymentName the deployment to unregister
     * @param expectedHandle the exact registration owned by the stopping deployment
     */
    public void unregisterDeploymentMetricReader(String deploymentName,
            AggregatingMetricExporter.DeploymentMetricReaderHandle expectedHandle) {
        OTEL_LOGGER.debugf("Unregistering deployment metric reader for: %s", deploymentName);
        unregisterDeploymentMetricReader(deploymentName, expectedHandle, deploymentMetricReaders, metricExporter);
    }

    /**
     * Exports and removes a deployment reader if it still owns the registry entry.
     *
     * @param deploymentName the deployment to unregister
     * @param expectedHandle the required current handle, or {@code null} to accept any current handle
     * @param deploymentMetricReaders the active deployment readers
     * @param metricExporter the shared exporter wrapper, which may be {@code null} when metrics are disabled
     */
    static void unregisterDeploymentMetricReader(String deploymentName,
            AggregatingMetricExporter.DeploymentMetricReaderHandle expectedHandle,
            Map<String, AggregatingMetricExporter.DeploymentMetricReaderHandle> deploymentMetricReaders,
            AggregatingMetricExporter metricExporter) {
        AggregatingMetricExporter.DeploymentMetricReaderHandle handle =
                deploymentMetricReaders.get(deploymentName);
        if (handle == null || (expectedHandle != null && handle != expectedHandle)) {
            return;
        }
        if (metricExporter != null) {
            metricExporter.exportDeployment(handle);
        }
        deploymentMetricReaders.remove(deploymentName, handle);
    }

    /**
     * Returns a read-only view of registered deployment metric readers.
     *
     * @return the active deployment reader registrations
     */
    public Map<String, AggregatingMetricExporter.DeploymentMetricReaderHandle> getDeploymentMetricReaders() {
        return Collections.unmodifiableMap(deploymentMetricReaders);
    }

    /**
     * Exports pending deployment metrics, removes runtime integrations, and shuts down the SDK.
     * Called when the subsystem is stopped.
     */
    public synchronized void shutdown() {
        OTEL_LOGGER.debugf("Shutting down OpenTelemetry service");

        // Export pending DELTA points before readers and the shared exporter are discarded.
        for (String deploymentName : List.copyOf(deploymentMetricReaders.keySet())) {
            unregisterDeploymentMetricReader(deploymentName);
        }

        // Unregister runtime metric observers before the SDK shuts down
        for (AutoCloseable closeable : closeables) {
            try {
                closeable.close();
            } catch (Exception e) {
                OTEL_LOGGER.debugf(e, "Failed to close %s during OpenTelemetry service shutdown", closeable);
            }
        }
        closeables.clear();

        // Requires LoggingPermission
        doPrivilegedIfChecking(() -> {
            uninstallLogHandler();
            return null;
        });

        // Shutdown the SDK if possible
        if (openTelemetry instanceof OpenTelemetrySdk sdk) {
            sdk.close();
        }
    }

    /** Builds an {@link OpenTelemetryService} after its configuration has been supplied. */
    public static class Builder {
        private WildFlyOpenTelemetryConfig config;

        /**
         * Sets the resolved subsystem configuration.
         *
         * @param config the subsystem configuration
         * @return this builder
         */
        public Builder config(WildFlyOpenTelemetryConfig config) {
            this.config = config;
            return this;
        }

        /**
         * Creates the configured service.
         *
         * @return the configured service
         * @throws IllegalStateException when no configuration was supplied
         */
        public OpenTelemetryService build() {
            if (config == null) {
                throw new IllegalStateException("config is required");
            }
            return new OpenTelemetryService(config);
        }
    }

    /**
     * Delegates deployment spans to a server-owned processor without allowing deployment shutdown to close it.
     */
    private static final class NonClosingSpanProcessor implements SpanProcessor {
        private final SpanProcessor delegate;

        /**
         * Creates a non-owning wrapper around a server span processor.
         *
         * @param delegate the server-owned processor
         */
        private NonClosingSpanProcessor(SpanProcessor delegate) {
            this.delegate = delegate;
        }

        /** {@inheritDoc} */
        @Override
        public void onStart(Context parentContext, ReadWriteSpan span) {
            delegate.onStart(parentContext, span);
        }

        /** {@inheritDoc} */
        @Override
        public boolean isStartRequired() {
            return delegate.isStartRequired();
        }

        /** {@inheritDoc} */
        @Override
        public void onEnd(ReadableSpan span) {
            delegate.onEnd(span);
        }

        /** {@inheritDoc} */
        @Override
        public boolean isEndRequired() {
            return delegate.isEndRequired();
        }

        /** {@inheritDoc} */
        @Override
        public CompletableResultCode forceFlush() {
            return delegate.forceFlush();
        }

        /**
         * Flushes the shared processor without shutting it down because the server owns its lifecycle.
         *
         * @return the flush result
         */
        @Override
        public CompletableResultCode shutdown() {
            return delegate.forceFlush();
        }
    }

    /** Prevents a deployment logger provider from closing the server-owned exporter. */
    private static final class NonClosingLogRecordExporter implements LogRecordExporter {
        private final LogRecordExporter delegate;

        /**
         * Creates a non-owning wrapper around the server log exporter.
         *
         * @param delegate the server-owned exporter
         */
        private NonClosingLogRecordExporter(LogRecordExporter delegate) {
            this.delegate = delegate;
        }

        /** {@inheritDoc} */
        @Override
        public CompletableResultCode export(Collection<LogRecordData> logs) {
            return delegate.export(logs);
        }

        /** {@inheritDoc} */
        @Override
        public CompletableResultCode flush() {
            return delegate.flush();
        }

        /** Leaves the server-owned exporter running when a deployment stops. */
        @Override
        public CompletableResultCode shutdown() {
            return CompletableResultCode.ofSuccess();
        }
    }
}
