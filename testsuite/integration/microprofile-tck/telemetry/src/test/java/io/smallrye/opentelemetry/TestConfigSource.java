package io.smallrye.opentelemetry;

import java.util.Map;

import io.smallrye.config.common.MapBackedConfigSource;

/**
 * Supplies fallback exporters so TCK deployments can create spans without an external collector.
 */
public class TestConfigSource extends MapBackedConfigSource {
    /**
     * Enables trace logging because OpenTelemetry 1.67 returns no-op tracers without a span processor.
     * Keeps defaults at the lowest priority so individual tests can override the exporters.
     */
    public TestConfigSource() {
        super("TestConfigSource",
                Map.of("otel.traces.exporter", "logging",
                        "otel.metrics.exporter", "none",
                        "otel.logs.exporter", "none"),
                Integer.MIN_VALUE);
    }
}
