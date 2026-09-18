/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.extension.micrometer;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

import org.wildfly.extension.observability.shared.FilterModel;

public class WildFlyMicrometerConfig {
    private List<String> exposedSubsystems;
    private Predicate<String> subsystemFilter;
    private List<FilterModel> filters = List.of();
    private boolean systemMetrics;

    // Use Builder
    public WildFlyMicrometerConfig() {}

    public Predicate<String> getSubsystemFilter() {
        return subsystemFilter;
    }

    public List<FilterModel> getFilters() {
        return filters;
    }

    public boolean exposeSystemMetrics() {
        return systemMetrics;
    }

    public static class Builder {
        private final WildFlyMicrometerConfig config = new WildFlyMicrometerConfig();
        private final List<FilterModel> filters = new ArrayList<>();

        public Builder exposedSubsystems(List<String> exposedSubsystems) {
            List<String> configuredSubsystems = new ArrayList<>(exposedSubsystems);
            boolean exposeAnySubsystem = configuredSubsystems.remove("*");
            config.exposedSubsystems = List.copyOf(configuredSubsystems);
            config.subsystemFilter = (subsystem) -> exposeAnySubsystem || config.exposedSubsystems.contains(subsystem);

            return this;
        }

        public Builder exposeSystemMetrics(boolean systemMetrics){
            config.systemMetrics = systemMetrics;
            return this;
        }

        public Builder addFilters(List<FilterModel> filters) {
            this.filters.addAll(filters);
            return this;
        }

        public WildFlyMicrometerConfig build() {
            config.filters = List.copyOf(filters);
            return config;
        }
    }
}
