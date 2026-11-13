/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.extension.micrometer;

import java.util.List;
import java.util.function.Predicate;

/** Holds Micrometer subsystem exposure configuration independently of caller-owned lists. */
public class WildFlyMicrometerConfig {
    private Predicate<String> subsystemFilter;

    // Use Builder
    public WildFlyMicrometerConfig() {}

    public Predicate<String> getSubsystemFilter() {
        return subsystemFilter;
    }

    public static class Builder {
        private final WildFlyMicrometerConfig config = new WildFlyMicrometerConfig();

        /**
         * Captures subsystem exposure without modifying or retaining the caller's list.
         *
         * @param exposedSubsystems the exposed subsystem names, or {@code "*"} to expose all subsystems
         * @return this builder
         */
        public Builder exposedSubsystems(List<String> exposedSubsystems) {
            List<String> subsystems = List.copyOf(exposedSubsystems);
            boolean exposeAnySubsystem = subsystems.contains("*");
            config.subsystemFilter = (subsystem) -> exposeAnySubsystem || subsystems.contains(subsystem);

            return this;
        }

        public WildFlyMicrometerConfig build() {
            return config;
        }
    }
}
