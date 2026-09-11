/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.extension.micrometer.metrics;

import static org.junit.Assert.assertEquals;

import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.client.helpers.MeasurementUnit;
import org.junit.Test;
import org.wildfly.extension.micrometer.registry.WildFlyCompositeRegistry;

public class MetricRegistrationTest {
    @Test
    public void unregistersMetricsForRemovedResourceAndChildren() {
        WildFlyCompositeRegistry registry = new WildFlyCompositeRegistry();
        MetricRegistration registration = new MetricRegistration(registry);
        PathAddress resource = PathAddress.pathAddress("subsystem", "test");

        registration.registerMetric(new WildFlyMetric(null, resource, "metric"), metadata(resource));
        registration.registerMetric(new WildFlyMetric(null, resource.append("child", "one"), "metric"),
                metadata(resource.append("child", "one")));
        assertEquals(2, registry.getMeters().size());

        registration.unregister(resource);

        assertEquals(0, registry.getMeters().size());
    }

    private static WildFlyMetricMetadata metadata(PathAddress address) {
        return new WildFlyMetricMetadata("metric", address, "description", MeasurementUnit.NONE, MetricMetadata.Type.GAUGE);
    }
}
