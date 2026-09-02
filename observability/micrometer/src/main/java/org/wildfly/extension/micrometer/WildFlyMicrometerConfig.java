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
    private final List<FilterModel> filters = new ArrayList<>();
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

        public Builder exposedSubsystems(List<String> exposedSubsystems) {
            config.exposedSubsystems = exposedSubsystems;
            boolean exposeAnySubsystem = exposedSubsystems.remove("*");
            config.subsystemFilter = (subsystem) -> exposeAnySubsystem || exposedSubsystems.contains(subsystem);

            return this;
        }

        public Builder exposeSystemMetrics(boolean systemMetrics){
            config.systemMetrics = systemMetrics;
            return this;
        }

        public Builder addFilters(List<FilterModel> filters) {
            config.filters.addAll(filters);
            return this;
        }

        public WildFlyMicrometerConfig build() {
            return config;
        }
    }
}
