/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.extension.micrometer;

import java.security.AccessController;
import java.security.PrivilegedAction;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

import io.micrometer.registry.otlp.AggregationTemporality;
import io.micrometer.registry.otlp.HistogramFlavor;
import io.micrometer.registry.otlp.OtlpConfig;
import org.wildfly.security.manager.WildFlySecurityManager;

public final class WildFlyMicrometerConfig implements OtlpConfig {
    /**
     * The OTLP endpoint to which to push metrics
     */
    private String endpoint;
    /**
     * How frequently, in seconds, to push metrics
     */
    private Long step;
    private List<String> exposedSubsystems;
    private Predicate<String> subsystemFilter;

    // Use Builder
    private WildFlyMicrometerConfig() {}

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

    @Override
    public AggregationTemporality aggregationTemporality() {
        if (WildFlySecurityManager.isChecking()) {
            return AccessController.doPrivileged((PrivilegedAction<AggregationTemporality>) OtlpConfig.super::aggregationTemporality);
        } else {
            return OtlpConfig.super.aggregationTemporality();
        }
    }

    @Override
    public HistogramFlavor histogramFlavor() {
        if (WildFlySecurityManager.isChecking()) {
            return AccessController.doPrivileged((PrivilegedAction<HistogramFlavor>) OtlpConfig.super::histogramFlavor);
        } else {
            return OtlpConfig.super.histogramFlavor();
        }
    }

    @Override
    public Map<String, String> headers() {
        if (WildFlySecurityManager.isChecking()) {
            return AccessController.doPrivileged((PrivilegedAction<Map<String, String>>) OtlpConfig.super::headers);
        } else {
            return OtlpConfig.super.headers();
        }
    }

    public Predicate<String> getSubsystemFilter() {
        return subsystemFilter;
    }

    public static class Builder {
        private final WildFlyMicrometerConfig config = new WildFlyMicrometerConfig();

        public Builder endpoint(String endpoint) {
            config.endpoint = endpoint;
            return this;
        }

        public Builder step(Long step) {
            config.step = step;
            return this;
        }
        public Builder exposedSubsystems(List<String> exposedSubsystems) {
            config.exposedSubsystems = exposedSubsystems;
            boolean exposeAnySubsystem = exposedSubsystems.remove("*");
            config.subsystemFilter = (subsystem) -> exposeAnySubsystem || exposedSubsystems.contains(subsystem);

            return this;
        }

        public WildFlyMicrometerConfig build() {
            return config;
        }
    }
}
