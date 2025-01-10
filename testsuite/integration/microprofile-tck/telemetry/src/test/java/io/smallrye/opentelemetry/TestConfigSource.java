package io.smallrye.opentelemetry;

import java.util.Map;

import io.smallrye.config.common.MapBackedConfigSource;

public class TestConfigSource extends MapBackedConfigSource {
    public TestConfigSource() {
        super("TestConfigSource",
                Map.of("otel.traces.exporter", "none",
                        "otel.metrics.exporter", "none",
                        "otel.logs.exporter", "none"),
                Integer.MIN_VALUE);
    }
}
