/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.extension.micrometer;

import java.util.List;
import java.util.function.Predicate;

public class WildFlyMicrometerConfig {
    private List<String> exposedSubsystems;
    private Predicate<String> subsystemFilter;

    // Use Builder
    public WildFlyMicrometerConfig() {}

    public Predicate<String> getSubsystemFilter() {
        return subsystemFilter;
    }

    public static class Builder {
        private final WildFlyMicrometerConfig config = new WildFlyMicrometerConfig();

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
