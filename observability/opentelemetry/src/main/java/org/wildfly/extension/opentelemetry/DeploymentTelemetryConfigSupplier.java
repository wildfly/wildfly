/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.extension.opentelemetry;

import java.util.function.Supplier;

/** Supplies the current neutral deployment configuration without coupling OpenTelemetry to its producer. */
final class DeploymentTelemetryConfigSupplier implements Supplier<DeploymentTelemetryConfig> {
    private final Supplier<DeploymentTelemetryConfig> attachedConfiguration;

    /**
     * Creates a supplier backed by a neutral configuration lookup.
     *
     * @param attachedConfiguration reads the current attached configuration
     */
    DeploymentTelemetryConfigSupplier(Supplier<DeploymentTelemetryConfig> attachedConfiguration) {
        this.attachedConfiguration = attachedConfiguration;
    }

    /**
     * Returns the latest attached configuration, or server-only defaults when none is attached.
     *
     * @return the current deployment configuration
     */
    @Override
    public DeploymentTelemetryConfig get() {
        DeploymentTelemetryConfig config = attachedConfiguration.get();
        return config == null ? new DeploymentTelemetryConfig(false) : config;
    }
}
