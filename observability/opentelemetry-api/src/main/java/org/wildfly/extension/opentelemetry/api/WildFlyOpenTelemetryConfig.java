/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.extension.opentelemetry.api;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import io.smallrye.opentelemetry.api.OpenTelemetryConfig;

public final class WildFlyOpenTelemetryConfig implements OpenTelemetryConfig {

    private final Map<String, String> properties;

    public WildFlyOpenTelemetryConfig(Map<String, String> properties) {
        this.properties = Collections.unmodifiableMap(properties);
    }

    public WildFlyOpenTelemetryConfig(String serviceName, String exporter, String endpoint,
                                      Long batchDelay, Long maxQueueSize, Long maxExportBatchSize,
                                      Long exportTimeout, String spanProcessorType, String sampler, Double ratio) {
        Map<String, String> config = new HashMap<>();
        // Default to on
        addValue(config, OTEL_SDK_DISABLED, "false");
        addValue(config, OTEL_EXPERIMENTAL_SDK_ENABLED, "true");

        addValue(config, OTEL_SERVICE_NAME, serviceName);
        // WildFly defaults to "otlp", which will cause problems due to naming collisions with otel-java. Since we only
        // support one exporter, we're overriding that default value here so that we select the appropriate exporters
        // from SmallRye OpenTelemetry. In a future release, we'll update the management model to reflect this value,
        // as well as, perhaps, support user-provided exporters via the ConfigurableSpanExporterProvider SPI.
        addValue(config, OTEL_TRACES_EXPORTER, "vertxgrpc");
        // We don't support otel logging or metrics, so we have to set these to "none" to prevent the autoconfiguration
        // API from trying to configure them.
        addValue(config, OTEL_LOGS_EXPORTER, "none");
        addValue(config, OTEL_METRICS_EXPORTER, "none");

        switch (exporter) {
            case "jaeger":
                addValue(config, OTEL_EXPORTER_JAEGER_ENDPOINT, endpoint);
                addValue(config, OTEL_EXPORTER_JAEGER_TIMEOUT, exportTimeout);
                break;
            case "otlp":
                addValue(config, OTEL_EXPORTER_OTLP_ENDPOINT, endpoint);
                addValue(config, OTEL_EXPORTER_OTLP_TIMEOUT, exportTimeout);
                break;
        }

        addValue(config, OTEL_BSP_SCHEDULE_DELAY, batchDelay);
        addValue(config, OTEL_BSP_MAX_QUEUE_SIZE, maxQueueSize);
        addValue(config, OTEL_BSP_MAX_EXPORT_BATCH_SIZE, maxExportBatchSize);

        addValue(config, OTEL_SPAN_PROCESSOR_TYPE, spanProcessorType);

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
    }

    @Override
    public Map<String, String> properties() {
        return properties;
    }

    /* Only add the value to the config if it is non-null, and convert the type to String to
       satisfy library requirements.
     */
    private void addValue(Map<String, String> config, String key, Object value) {
        if (value != null) {
            config.put(key, value.toString());
        }
    }
}
