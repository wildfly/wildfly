/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.extension.opentelemetry;

import java.util.Map;
import java.util.Set;

import org.wildfly.extension.opentelemetry.api.WildFlyOpenTelemetryConfig;

/**
 * Carries effective deployment OpenTelemetry properties and identifies signals whose exporter selector
 * is owned by application-local MicroProfile Config.
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

    /**
     * Creates an immutable deployment configuration.
     *
     * @param properties effective OpenTelemetry properties
     * @param applicationExporters signals selected by application-local MP Config
     */
    public DeploymentTelemetryConfig(Map<String, String> properties, Set<Signal> applicationExporters) {
        this.properties = Map.copyOf(properties);
        this.applicationExporters = Set.copyOf(applicationExporters);
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
     * Tests whether any signal needs the auxiliary deployment SDK.
     *
     * @return true when at least one signal is application-owned
     */
    public boolean hasApplicationExporters() {
        return !applicationExporters.isEmpty();
    }
}
