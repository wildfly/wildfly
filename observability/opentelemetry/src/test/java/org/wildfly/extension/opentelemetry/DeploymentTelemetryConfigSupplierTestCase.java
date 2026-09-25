/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.extension.opentelemetry;

import static org.junit.Assert.assertSame;

import org.junit.Test;

/** Verifies deployment configuration remains available when it is attached after OpenTelemetry setup. */
public class DeploymentTelemetryConfigSupplierTestCase {

    /** Verifies the supplier observes an attachment added after the supplier is created. */
    @Test
    public void testReadsConfigurationAttachedAfterCreation() {
        DeploymentTelemetryConfig initialConfig = new DeploymentTelemetryConfig(false);
        DeploymentTelemetryConfig laterConfig = new DeploymentTelemetryConfig(true);
        DeploymentTelemetryConfig[] attachedConfiguration = {initialConfig, laterConfig};
        int[] lookupCount = {0};

        DeploymentTelemetryConfigSupplier supplier = new DeploymentTelemetryConfigSupplier(
                () -> attachedConfiguration[lookupCount[0]++]);

        assertSame("The first lookup should observe the initial attachment", initialConfig, supplier.get());
        assertSame("The later lookup should observe the replacement attachment", laterConfig, supplier.get());
    }
}
