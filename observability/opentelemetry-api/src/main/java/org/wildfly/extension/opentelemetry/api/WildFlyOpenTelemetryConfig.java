/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.extension.opentelemetry.api;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.wildfly.extension.observability.shared.FilterModel;
import org.wildfly.service.descriptor.NullaryServiceDescriptor;

public final class WildFlyOpenTelemetryConfig {
    public static final String INSTRUMENTATION_VERSION =
            Optional.ofNullable(WildFlyOpenTelemetryConfig.class.getPackage().getImplementationVersion())
                    .orElse("SNAPSHOT");
    public static NullaryServiceDescriptor<WildFlyOpenTelemetryConfig> SERVICE_DESCRIPTOR =
            NullaryServiceDescriptor.of("org.wildfly.extension.opentelemetry.config",
                WildFlyOpenTelemetryConfig.class);

    public static final String OTEL_BSP_MAX_EXPORT_BATCH_SIZE = "otel.bsp.max.export.batch.size";
    public static final String OTEL_BSP_MAX_QUEUE_SIZE = "otel.bsp.max.queue.size";
    public static final String OTEL_BSP_SCHEDULE_DELAY = "otel.bsp.schedule.delay";
    public static final String OTEL_EXPORTER_OTLP_ENDPOINT = "otel.exporter.otlp.endpoint";
    public static final String OTEL_EXPORTER_OTLP_PROTOCOL = "otel.exporter.otlp.protocol";
    public static final String OTEL_EXPORTER_OTLP_TIMEOUT = "otel.exporter.otlp.timeout";
    public static final String OTEL_LOGS_EXPORTER = "otel.logs.exporter";
    public static final String OTEL_PROPAGATORS = "otel.propagators";
    public static final String OTEL_METRICS_EXPORTER = "otel.metrics.exporter";
    public static final String OTEL_SDK_DISABLED = "otel.sdk.disabled";
    public static final String OTEL_SERVICE_NAME = "otel.service.name";
    public static final String OTEL_TRACES_EXPORTER = "otel.traces.exporter";
    public static final String OTEL_TRACES_SAMPLER = "otel.traces.sampler";
    public static final String OTEL_TRACES_SAMPLER_ARG = "otel.traces.sampler.arg";

    private final Map<String, String> properties;
    private final boolean mpTelemetryInstalled;
    private final List<FilterModel> filters;
    private final boolean systemMetrics;

    public WildFlyOpenTelemetryConfig(Map<String, String> properties,
                               boolean mpTelemetryInstalled,
                               List<FilterModel> filters,
                               boolean systemMetrics) {
        this.properties = Collections.unmodifiableMap(properties);
        this.mpTelemetryInstalled = mpTelemetryInstalled;
        this.filters = filters;
        this.systemMetrics = systemMetrics;
    }

    public Map<String, String> getProperties() {
        return properties;
    }

    public List<FilterModel> getFilters() {
        return filters;
    }

    public boolean isMpTelemetryInstalled() {
        return mpTelemetryInstalled;
    }

    public boolean exposeSystemMetrics() {
        return systemMetrics;
    }

    public WildFlyOpenTelemetryConfig withProperties(Map<String, String> properties) {
        return new WildFlyOpenTelemetryConfig(properties, mpTelemetryInstalled, filters, systemMetrics);
    }

    public static class Builder {
        final Map<String, String> properties = new HashMap<>();
        private boolean mpTelemetryInstalled;
        private List<FilterModel> filters = List.of();
        private boolean systemMetrics = true;

        public Builder() {
            addValue(OTEL_EXPORTER_OTLP_PROTOCOL, "grpc");
            addValue(OTEL_PROPAGATORS, "tracecontext,baggage");
            addValue(OTEL_SDK_DISABLED, "false");

        }

        public Builder setServiceName(String serviceName) {
            addValue(OTEL_SERVICE_NAME, serviceName);
            return this;
        }

        public Builder setExporter(String exporter) {
            if (!exporter.equals("otlp")) {
                throw new IllegalArgumentException("An unexpected exporter type was found: " + exporter);
            }
            addValue(OTEL_TRACES_EXPORTER, exporter);
            addValue(OTEL_LOGS_EXPORTER, exporter);
            addValue(OTEL_METRICS_EXPORTER, exporter);

            return this;
        }

        public Builder setOtlpEndpoint(String endpoint) {
            addValue(OTEL_EXPORTER_OTLP_ENDPOINT, endpoint);
            return this;
        }

        public Builder setExportTimeout(long timeout) {
            addValue(OTEL_EXPORTER_OTLP_TIMEOUT, timeout);
            return this;
        }

        public Builder setExportInterval(Long interval) {
            addValue("otel.metric.export.interval", interval);
            return this;
        }

        public Builder setBatchDelay(long delay) {
            addValue(OTEL_BSP_SCHEDULE_DELAY, delay);
            return this;
        }

        public Builder setMaxQueueSize(long maxQueueSize) {
            addValue(OTEL_BSP_MAX_QUEUE_SIZE, maxQueueSize);
            return this;
        }

        public Builder setMaxExportBatchSize(long maxExportBatchSize) {
            addValue(OTEL_BSP_MAX_EXPORT_BATCH_SIZE, maxExportBatchSize);
            return this;
        }

        public Builder setSampler(String sampler) {
            if (sampler != null) {
                switch (sampler) {
                    case "on":
                        addValue(OTEL_TRACES_SAMPLER, "always_on");
                        break;
                    case "off":
                        addValue(OTEL_TRACES_SAMPLER, "always_off");
                        break;
                    case "ratio":
                        addValue(OTEL_TRACES_SAMPLER, "traceidratio");
                        break;
                }
            }
            return this;
        }

        public Builder setSamplerRatio(Double ratio) {
            addValue(OTEL_TRACES_SAMPLER_ARG, ratio);
            return this;
        }

        public Builder setInjectVertx(boolean injectVertx) {
            if (injectVertx) {
                addValue("otel.exporter.vertx.cdi.identifier", "vertx");
            }
            return this;
        }

        public Builder setMicroProfileTelemetryInstalled(boolean microProfileTelemetryInstalled) {
            this.mpTelemetryInstalled = microProfileTelemetryInstalled;
            return this;
        }

        public Builder setFilters(List<FilterModel> filters) {
            this.filters = filters;
            return this;
        }

        public Builder setSystemMetrics(boolean systemMetrics) {
            this.systemMetrics = systemMetrics;
            return this;
        }

        public WildFlyOpenTelemetryConfig build() {
            return new WildFlyOpenTelemetryConfig(properties, mpTelemetryInstalled, filters, systemMetrics);
        }

        /**
         * Only add the value to the config if it is non-null, and convert the type to String to
         * satisfy library requirements.
         */
        private void addValue(String key, Object value) {
            if (value != null) {
                properties.put(key, value.toString());
            }
        }
    }
}
