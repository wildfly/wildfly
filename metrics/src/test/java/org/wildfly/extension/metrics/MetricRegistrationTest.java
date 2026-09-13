/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.extension.metrics;

import static org.junit.Assert.assertEquals;

import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.client.helpers.MeasurementUnit;
import org.junit.Test;

public class MetricRegistrationTest {
    @Test
    public void unregistersMetricsForRemovedResourceAndChildren() {
        WildFlyMetricRegistry registry = new WildFlyMetricRegistry();
        MetricRegistration registration = new MetricRegistration(registry);
        PathAddress resource = PathAddress.pathAddress("subsystem", "test");

        registration.registerMetric(new WildFlyMetric(null, resource, "metric"), metadata(resource));
        registration.registerMetric(new WildFlyMetric(null, resource.append("child", "one"), "metric"),
                metadata(resource.append("child", "one")));
        registration.registerMetric(new WildFlyMetric(null, PathAddress.pathAddress("subsystem", "other"), "metric"),
                metadata(PathAddress.pathAddress("subsystem", "other")));

        registration.unregister(resource);

        assertEquals(1, registry.getMetrics().size());
    }

    private static WildFlyMetricMetadata metadata(PathAddress address) {
        return new WildFlyMetricMetadata("metric", address, null, "description", MeasurementUnit.NONE,
                MetricMetadata.Type.GAUGE);
    }
}
