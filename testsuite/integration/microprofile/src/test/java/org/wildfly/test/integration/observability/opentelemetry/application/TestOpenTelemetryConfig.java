/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.test.integration.observability.opentelemetry.application;

import java.util.HashMap;
import java.util.Map;

import io.smallrye.opentelemetry.api.OpenTelemetryConfig;
import jakarta.annotation.Priority;
import jakarta.enterprise.inject.Alternative;
import jakarta.inject.Singleton;

@Alternative
@Singleton
@Priority(Integer.MAX_VALUE)
public class TestOpenTelemetryConfig implements OpenTelemetryConfig {
    private final Map<String, String> properties = new HashMap<>();

    @Override
    public Map<String, String> properties() {
        properties.put("otel.metrics.exporter", "none");
        properties.put("otel.logs.exporter", "none");
        properties.put("otel.traces.exporter", "in-memory");
        return properties;
    }
}
