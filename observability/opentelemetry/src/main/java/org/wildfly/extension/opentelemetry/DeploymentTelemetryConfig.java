/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.extension.opentelemetry;

import java.util.Collections;
import java.util.Map;
import java.util.Set;

import org.wildfly.extension.opentelemetry.api.WildFlyOpenTelemetryConfig;

/**
 * Carries effective deployment OpenTelemetry properties and identifies application-local exporter
 * and sampler selections.
 */
public final class DeploymentTelemetryConfig {
    /** OpenTelemetry signals that can select a deployment-owned exporter pipeline. */
    public enum Signal {
        /** Log records. */
        LOGS(WildFlyOpenTelemetryConfig.OTEL_LOGS_EXPORTER),
        /** Metrics. */
        METRICS(WildFlyOpenTelemetryConfig.OTEL_METRICS_EXPORTER),
        /** Traces. */
        TRACES(WildFlyOpenTelemetryConfig.OTEL_TRACES_EXPORTER);

        private final String exporterProperty;

        /**
         * Associates a signal with its standard exporter selector.
         *
         * @param exporterProperty the canonical OpenTelemetry exporter property
         */
        Signal(String exporterProperty) {
            this.exporterProperty = exporterProperty;
        }

        /**
         * Returns the canonical exporter selector for this signal.
         *
         * @return the exporter property name
         */
        public String exporterProperty() {
            return exporterProperty;
        }
    }

    private final Map<String, String> properties;
    private final Set<Signal> applicationExporters;
    private final boolean applicationSamplerOverride;

    /**
     * Creates an immutable deployment configuration.
     *
     * @param properties effective OpenTelemetry properties
     * @param applicationExporters signals selected by application-local MP Config
     * @param applicationSamplerOverride whether the deployment selects its own trace sampler
     */
    public DeploymentTelemetryConfig(boolean applicationSamplerOverride) {
        this(Collections.emptyMap(), Collections.emptySet(), applicationSamplerOverride);
    }

    public DeploymentTelemetryConfig(Map<String, String> properties, Set<Signal> applicationExporters,
                                     boolean applicationSamplerOverride) {
        this.properties = Map.copyOf(properties);
        this.applicationExporters = Set.copyOf(applicationExporters);
        this.applicationSamplerOverride = applicationSamplerOverride;
    }

    /**
     * Returns the effective OpenTelemetry properties.
     *
     * @return immutable effective properties
     */
    public Map<String, String> properties() {
        return properties;
    }

    /**
     * Tests whether an application-local selector owns the signal pipeline.
     *
     * @param signal the signal to test
     * @return true when the deployment owns the signal pipeline
     */
    public boolean usesApplicationExporter(Signal signal) {
        return applicationExporters.contains(signal);
    }

    /**
     * Tests whether any exporter selector is application-owned.
     *
     * @return true when at least one signal is application-owned
     */
    public boolean hasApplicationExporters() {
        return !applicationExporters.isEmpty();
    }

    /**
     * Tests whether the deployment selected its own trace sampler.
     *
     * @return true when the sampler came from application-local configuration
     */
    public boolean hasApplicationSamplerOverride() {
        return applicationSamplerOverride;
    }
}
