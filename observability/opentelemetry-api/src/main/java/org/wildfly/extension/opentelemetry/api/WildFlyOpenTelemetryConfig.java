/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.extension.opentelemetry.api;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import io.smallrye.opentelemetry.api.OpenTelemetryConfig;
import org.wildfly.service.descriptor.NullaryServiceDescriptor;

public final class WildFlyOpenTelemetryConfig implements OpenTelemetryConfig {
    public static NullaryServiceDescriptor<WildFlyOpenTelemetryConfig> SERVICE_DESCRIPTOR =
            NullaryServiceDescriptor.of("org.wildfly.extension.opentelemetry.config",
                WildFlyOpenTelemetryConfig.class);

    public static final String OTEL_BSP_MAX_EXPORT_BATCH_SIZE = "otel.bsp.max.export.batch.size";
    public static final String OTEL_BSP_MAX_QUEUE_SIZE = "otel.bsp.max.queue.size";
    public static final String OTEL_BSP_SCHEDULE_DELAY = "otel.bsp.schedule.delay";
    public static final String OTEL_EXPORTER_OTLP_ENDPOINT = "otel.exporter.otlp.endpoint";
    public static final String OTEL_EXPORTER_OTLP_PROTOCOL = "otel.exporter.otlp.protocol";
    public static final String OTEL_EXPORTER_OTLP_TIMEOUT = "otel.exporter.otlp.timeout";
    public static final String OTEL_EXPORTER_OTLP_TRACES_PROTOCOL = "otel.exporter.otlp.traces.protocol";
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

    public WildFlyOpenTelemetryConfig(Map<String, String> properties, boolean mpTelemetryInstalled) {
        this.properties = Collections.unmodifiableMap(properties);
        this.mpTelemetryInstalled = mpTelemetryInstalled;
    }

    public WildFlyOpenTelemetryConfig(String serviceName, String exporter, String endpoint,
                                      Long batchDelay, Long maxQueueSize, Long maxExportBatchSize,
                                      Long exportTimeout, String spanProcessorType, String sampler, Double ratio,
                                      boolean mpTelemetryInstalled) {
        Map<String, String> config = new HashMap<>();
        // Default to on
        addValue(config, OTEL_SDK_DISABLED, "false");

        addValue(config, OTEL_SERVICE_NAME, serviceName);
        addValue(config, OTEL_TRACES_EXPORTER, exporter);
        addValue(config, OTEL_LOGS_EXPORTER, exporter);
        addValue(config, OTEL_METRICS_EXPORTER, exporter);
        addValue(config, OTEL_PROPAGATORS, "tracecontext,baggage");


        if (exporter.equals("otlp")) {
            addValue(config, OTEL_EXPORTER_OTLP_ENDPOINT, endpoint);
            addValue(config, OTEL_EXPORTER_OTLP_PROTOCOL, "grpc");
            addValue(config, OTEL_EXPORTER_OTLP_TIMEOUT, exportTimeout);
            addValue(config, "otel.metric.export.interval", batchDelay);
        } else {
            throw new IllegalArgumentException("An unexpected exporter type was found: " + exporter);
        }

        addValue(config, OTEL_BSP_SCHEDULE_DELAY, batchDelay);
        addValue(config, OTEL_BSP_MAX_QUEUE_SIZE, maxQueueSize);
        addValue(config, OTEL_BSP_MAX_EXPORT_BATCH_SIZE, maxExportBatchSize);

        if (sampler != null) {
            switch (sampler) {
                case "on":
                    addValue(config, OTEL_TRACES_SAMPLER, "always_on");
                    break;
                case "off":
                    addValue(config, OTEL_TRACES_SAMPLER, "always_off");
                    break;
                case "ratio":
                    addValue(config, OTEL_TRACES_SAMPLER, "traceidratio");
                    addValue(config, OTEL_TRACES_SAMPLER_ARG, ratio);
                    break;
            }
        }


        properties = Collections.unmodifiableMap(config);
        this.mpTelemetryInstalled = mpTelemetryInstalled;
    }

    @Override
    public Map<String, String> properties() {
        return properties;
    }

    public boolean isMpTelemetryInstalled() {
        return mpTelemetryInstalled;
    }

    /**
     *  Only add the value to the config if it is non-null, and convert the type to String to
     *  satisfy library requirements.
     */
    private void addValue(Map<String, String> config, String key, Object value) {
        if (value != null) {
            config.put(key, value.toString());
        }
    }
}
