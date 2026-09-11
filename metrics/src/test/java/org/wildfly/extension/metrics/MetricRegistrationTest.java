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

    @Test
    public void removesMetadataAfterTheLastMetricWithThatName() {
        WildFlyMetricRegistry registry = new WildFlyMetricRegistry();
        MetricRegistration registration = new MetricRegistration(registry);
        PathAddress first = PathAddress.pathAddress("subsystem", "first");
        PathAddress second = PathAddress.pathAddress("subsystem", "second");

        registration.registerMetric(new WildFlyMetric(null, first, "metric"), metadata(first));
        registration.registerMetric(new WildFlyMetric(null, second, "metric"), metadata(second));

        registration.unregister(first);
        assertEquals(1, registry.getMetricMetadata().size());

        registration.unregister(second);
        assertEquals(0, registry.getMetricMetadata().size());
    }

    private static WildFlyMetricMetadata metadata(PathAddress address) {
        return new WildFlyMetricMetadata("metric", address, null, "description", MeasurementUnit.NONE,
                MetricMetadata.Type.GAUGE);
    }
}
