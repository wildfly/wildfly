/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.extension.micrometer.metrics;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.List;

import org.jboss.as.controller.PathAddress;
import org.junit.Test;

/** Verifies subsystem exposure filtering for resource metric discovery. */
public class MicrometerCollectorExposureTest {

    /** Verifies only configured subsystems and the root resource pass the exposure filter. */
    @Test
    public void exposesOnlyConfiguredSubsystems() {
        MicrometerCollector collector = new MicrometerCollector(null, null, null,
                List.of("undertow")::contains);

        assertTrue(collector.isExposingMetrics(PathAddress.EMPTY_ADDRESS));
        assertTrue(collector.isExposingMetrics(PathAddress.pathAddress("subsystem", "undertow")));
        assertFalse(collector.isExposingMetrics(PathAddress.pathAddress("subsystem", "messaging-activemq")));
        assertFalse(collector.isExposingMetrics(PathAddress.pathAddress("deployment", "app.war")));
    }
}
