/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.extension.micrometer.otlp;

import java.time.Duration;
import java.util.Map;

import io.micrometer.registry.otlp.OtlpConfig;

public final class WildFlyMicrometerConfig implements OtlpConfig {
    /**
     * The OTLP endpoint to which to push metrics
     */
    private final String endpoint;
    /**
     * How frequently, in seconds, to push metrics
     */
    private final long step;

    public WildFlyMicrometerConfig(String endpoint, long step) {
        this.endpoint = endpoint;
        this.step = step;
    }

    @Override
    public String get(String key) {
        return null; // Accept defaults not explicitly overridden below
    }

    @Override
    public Map<String, String> resourceAttributes() {
        Map<String, String> attributes = OtlpConfig.super.resourceAttributes();
        if (!attributes.containsKey("service.name")) {
            attributes.put("service.name", "wildfly");
        }
        return attributes;
    }

    @Override
    public String url() {
        return endpoint;
    }

    @Override
    public Duration step() {
        return Duration.ofSeconds(step);
    }
}
