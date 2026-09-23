/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.extension.opentelemetry;

import static org.wildfly.extension.opentelemetry.OpenTelemetryExtensionLogger.OTEL_LOGGER;

import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.sdk.autoconfigure.AutoConfiguredOpenTelemetrySdk;
import io.opentelemetry.sdk.autoconfigure.AutoConfiguredOpenTelemetrySdkBuilder;
import org.wildfly.extension.opentelemetry.api.WildFlyOpenTelemetryConfig;

/**
 * Server-level OpenTelemetry service that builds and manages the shared OpenTelemetry instance.
 * Follows the pattern of MicrometerService for consistency with WildFly's observability architecture.
 */
public class OpenTelemetryService {
    private final OpenTelemetry openTelemetry;
    private final ConcurrentHashMap<String, AggregatingMetricExporter.DeploymentMetricReaderHandle> deploymentMetricReaders;

    private OpenTelemetryService(WildFlyOpenTelemetryConfig config) {
        this.deploymentMetricReaders = new ConcurrentHashMap<>();
        this.openTelemetry = buildOpenTelemetry(config);
    }

    private OpenTelemetry buildOpenTelemetry(WildFlyOpenTelemetryConfig config) {
        // Use OpenTelemetry autoconfigure to build from properties
        // This handles all the OTLP exporter configuration, samplers, etc.
        AutoConfiguredOpenTelemetrySdkBuilder builder = AutoConfiguredOpenTelemetrySdk.builder();

        // Add our config properties
        builder.addPropertiesSupplier(() -> config.properties());

        // Customize to use aggregating metric exporter
        builder.addMetricExporterCustomizer((exporter, configProps) -> {
            OTEL_LOGGER.debugf("Wrapping metric exporter with AggregatingMetricExporter");
            return new AggregatingMetricExporter(exporter, () -> deploymentMetricReaders);
        });

        AutoConfiguredOpenTelemetrySdk autoConfigured = builder.build();
        return autoConfigured.getOpenTelemetrySdk();
    }

    public OpenTelemetry getOpenTelemetry() {
        return openTelemetry;
    }

    public void registerDeploymentMetricReader(String deploymentName,
                                               AggregatingMetricExporter.DeploymentMetricReaderHandle handle) {
        OTEL_LOGGER.debugf("Registering deployment metric reader for: %s", deploymentName);
        deploymentMetricReaders.put(deploymentName, handle);
    }

    public void unregisterDeploymentMetricReader(String deploymentName) {
        OTEL_LOGGER.debugf("Unregistering deployment metric reader for: %s", deploymentName);
        deploymentMetricReaders.remove(deploymentName);
    }

    public Map<String, AggregatingMetricExporter.DeploymentMetricReaderHandle> getDeploymentMetricReaders() {
        return Collections.unmodifiableMap(deploymentMetricReaders);
    }

    public static class Builder {
        private WildFlyOpenTelemetryConfig config;

        public Builder config(WildFlyOpenTelemetryConfig config) {
            this.config = config;
            return this;
        }

        public OpenTelemetryService build() {
            if (config == null) {
                throw new IllegalStateException("config is required");
            }
            return new OpenTelemetryService(config);
        }
    }
}
