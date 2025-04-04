/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.extension.opentelemetry.api;

import java.util.HashMap;
import java.util.Map;
import javax.net.ssl.SSLContext;

import io.smallrye.opentelemetry.api.OpenTelemetryConfig;
import org.wildfly.service.descriptor.NullaryServiceDescriptor;

public final class WildFlyOpenTelemetryConfig implements OpenTelemetryConfig {
    private static final String EXPORTER_OTLP = "otlp";
    private static final String EXPORTER_NONE = "none";
    public static NullaryServiceDescriptor<WildFlyOpenTelemetryConfig> SERVICE_DESCRIPTOR =
            NullaryServiceDescriptor.of("org.wildfly.extension.opentelemetry.config", WildFlyOpenTelemetryConfig.class);

    // General
    public static final String OTEL_SDK_DISABLED = "otel.sdk.disabled";
    public static final String OTEL_SERVICE_NAME = "otel.service.name";

    // Exporters
    public static final String OTEL_EXPORTER_OTLP_COMPRESSION = "otel.exporter.otlp.compression";
    public static final String OTEL_EXPORTER_OTLP_ENDPOINT = "otel.exporter.otlp.endpoint";
    public static final String OTEL_EXPORTER_OTLP_PROTOCOL = "otel.exporter.otlp.protocol";
    public static final String OTEL_EXPORTER_OTLP_TIMEOUT = "otel.exporter.otlp.timeout";

    // Traces
    public static final String OTEL_TRACES_EXPORTER = "otel.traces.exporter";
    public static final String OTEL_TRACES_MAX_EXPORT_BATCH_SIZE = "otel.bsp.max.export.batch.size";
    public static final String OTEL_TRACES_MAX_QUEUE_SIZE = "otel.bsp.max.queue.size";
    public static final String OTEL_TRACES_PROPAGATORS = "otel.propagators";
    public static final String OTEL_TRACES_SAMPLER = "otel.traces.sampler";
    public static final String OTEL_TRACES_SAMPLER_ARG = "otel.traces.sampler.arg";
    public static final String OTEL_TRACES_SCHEDULE_DELAY = "otel.bsp.schedule.delay";

    // Metrics
    public static final String OTEL_METRICS_DEFAULT_HISTOGRAM_AGGREGATION = "otel.exporter.otlp.metrics.default.histogram.aggregation";
    public static final String OTEL_METRICS_EXEMPLAR_FILTER = "otel.metrics.exemplar.filter";
    public static final String OTEL_METRICS_EXPORTER = "otel.metrics.exporter";
    public static final String OTEL_METRICS_TEMPORALITY_PREFERENCE = "otel.exporter.otlp.metrics.temporality.preference";
    public static final String OTEL_METRIC_EXPORT_INTERVAL = "otel.metric.export.interval";

    // Logging
    public static final String OTEL_LOGS_MAX_EXPORT_BATCH_SIZE = "otel.blrp.max.export.batch.size";
    public static final String OTEL_LOGS_MAX_QUEUE_SIZE = "otel.blrp.max.queue.size";
    public static final String OTEL_LOGS_SCHEDULE_DELAY = "otel.blrp.schedule.delay";
    public static final String OTEL_LOGS_EXPORTER = "otel.logs.exporter";

    private final Map<String, String> properties;
    private final boolean mpTelemetryInstalled;
    private final SSLContext sslContext;

    WildFlyOpenTelemetryConfig(Map<String, String> properties,
                               boolean mpTelemetryInstalled,
                               SSLContext sslContext) {
        this.properties = properties;
        this.mpTelemetryInstalled = mpTelemetryInstalled;
        this.sslContext = sslContext;
    }

    @Override
    public Map<String, String> properties() {
        return properties;
    }

    public boolean isMpTelemetryInstalled() {
        return mpTelemetryInstalled;
    }

    public SSLContext getSslContext() {
        return sslContext;
    }

    public static class Builder {
        final Map<String, String> properties = new HashMap<>();
        private boolean mpTelemetryInstalled;
        private SSLContext sslContext;

        public Builder() {
            addValue(OTEL_EXPORTER_OTLP_PROTOCOL, "grpc");
            addValue(OTEL_TRACES_PROPAGATORS, "tracecontext,baggage");
            addValue(OTEL_SDK_DISABLED, "false");

            addValue(OTEL_TRACES_EXPORTER, EXPORTER_OTLP);
            addValue(OTEL_LOGS_EXPORTER, EXPORTER_OTLP);
            addValue(OTEL_METRICS_EXPORTER, EXPORTER_OTLP);
        }

        public WildFlyOpenTelemetryConfig build() {
            return new WildFlyOpenTelemetryConfig(properties, mpTelemetryInstalled, sslContext);
        }

        public Builder setServiceName(String serviceName) {
            addValue(OTEL_SERVICE_NAME, serviceName);
            return this;
        }

        public Builder setExporter(String exporter) {
            if (!exporter.equals(EXPORTER_OTLP)) {
                throw new IllegalArgumentException("An unexpected exporter type was found: " + exporter);
            }

            // Remove this?

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

        public Builder setCompression(String compression) {
            addValue(OTEL_EXPORTER_OTLP_COMPRESSION, compression);
            return this;
        }

        public Builder setMetricsExportInterval(Long interval) {
            addValue(OTEL_METRIC_EXPORT_INTERVAL, interval);
            return this;
        }

        public Builder setTracesEnabled(boolean enabled) {
            addValue(OTEL_TRACES_EXPORTER, enabled ? EXPORTER_OTLP : EXPORTER_NONE);

            return this;
        }

        public Builder setTracesExportInterval(long delay) {
            addValue(OTEL_TRACES_SCHEDULE_DELAY, delay);
            return this;
        }

        public Builder setLogsEnabled(boolean enabled) {
            addValue(OTEL_LOGS_EXPORTER, enabled ? EXPORTER_OTLP : EXPORTER_NONE);

            return this;
        }

        public Builder setLogsExportInterval(long delay) {
            addValue(OTEL_LOGS_SCHEDULE_DELAY, delay);
            return this;
        }

        public Builder setMaxQueueSize(long maxQueueSize) {
            addValue(OTEL_TRACES_MAX_QUEUE_SIZE, maxQueueSize);
            addValue(OTEL_LOGS_MAX_QUEUE_SIZE, maxQueueSize);
            return this;
        }

        public Builder setMaxExportBatchSize(long maxExportBatchSize) {
            addValue(OTEL_TRACES_MAX_EXPORT_BATCH_SIZE, maxExportBatchSize);
            addValue(OTEL_LOGS_MAX_EXPORT_BATCH_SIZE, maxExportBatchSize);
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

        public Builder setMetricsEnabled(boolean enabled) {
            addValue(OTEL_METRICS_EXPORTER, enabled ? EXPORTER_OTLP : EXPORTER_NONE);

            return this;
        }

        public Builder setMetricsExemplarFilter(String filter) {
            addValue(OTEL_METRICS_EXEMPLAR_FILTER, filter);
            return this;
        }

        public Builder setMetricsTemporality(String preference) {
            addValue(OTEL_METRICS_TEMPORALITY_PREFERENCE, preference);
            return this;
        }

        public Builder setMetricsHistogramAggregation(String aggregation) {
            addValue(OTEL_METRICS_DEFAULT_HISTOGRAM_AGGREGATION, aggregation);
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

        public Builder setSslContext(SSLContext sslContext) {
            this.sslContext = sslContext;
            return this;
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
