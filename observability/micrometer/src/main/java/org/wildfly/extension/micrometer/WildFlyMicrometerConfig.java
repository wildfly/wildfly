/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.extension.micrometer;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

import org.wildfly.extension.observability.shared.FilterModel;

/** Holds Micrometer subsystem exposure configuration independently of caller-owned lists. */
public class WildFlyMicrometerConfig {
    private Predicate<String> subsystemFilter;
    private List<FilterModel> filters = List.of();
    private boolean systemMetrics;

    // Use Builder
    public WildFlyMicrometerConfig() {}

    public Predicate<String> getSubsystemFilter() {
        return subsystemFilter;
    }

    /** Returns the configured metric filters. */
    public List<FilterModel> getFilters() {
        return filters;
    }

    /** Returns whether system metrics are exposed. */
    public boolean exposeSystemMetrics() {
        return systemMetrics;
    }

    public static class Builder {
        private final WildFlyMicrometerConfig config = new WildFlyMicrometerConfig();
        private final List<FilterModel> filters = new ArrayList<>();

        /**
         * Captures subsystem exposure without modifying or retaining the caller's list.
         *
         * @param exposedSubsystems the exposed subsystem names, or {@code "*"} to expose all subsystems
         * @return this builder
         */
        public Builder exposedSubsystems(List<String> exposedSubsystems) {
            List<String> configuredSubsystems = new ArrayList<>(exposedSubsystems);
            boolean exposeAnySubsystem = configuredSubsystems.remove("*");
            List<String> subsystems = List.copyOf(configuredSubsystems);
            config.subsystemFilter = (subsystem) -> exposeAnySubsystem || subsystems.contains(subsystem);

            return this;
        }

        /** Configures whether system metrics are exposed. */
        public Builder exposeSystemMetrics(boolean systemMetrics) {
            config.systemMetrics = systemMetrics;
            return this;
        }

        /** Adds metric filters to the configuration being built. */
        public Builder addFilters(List<FilterModel> filters) {
            this.filters.addAll(filters);
            return this;
        }

        /** Builds the immutable configuration snapshot. */
        public WildFlyMicrometerConfig build() {
            config.filters = List.copyOf(filters);
            return config;
        }
    }
}
