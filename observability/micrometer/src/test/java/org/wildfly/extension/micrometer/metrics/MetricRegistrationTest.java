/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.extension.micrometer.metrics;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

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

    @Test
    public void addingRegistrationTaskWaitsForRegistration() throws Exception {
        WildFlyCompositeRegistry registry = new WildFlyCompositeRegistry();
        MetricRegistration registration = new MetricRegistration(registry);
        CountDownLatch taskStarted = new CountDownLatch(1);
        CountDownLatch releaseTask = new CountDownLatch(1);
        CountDownLatch taskAdded = new CountDownLatch(1);

        registration.addRegistrationTask(() -> {
            taskStarted.countDown();
            try {
                releaseTask.await();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });

        Thread registerThread = new Thread(registration::register);
        registerThread.start();
        assertTrue(taskStarted.await(5, TimeUnit.SECONDS));

        Thread addThread = new Thread(() -> {
            registration.addRegistrationTask(() -> { });
            taskAdded.countDown();
        });
        addThread.start();

        try {
            assertFalse(taskAdded.await(100, TimeUnit.MILLISECONDS));
        } finally {
            releaseTask.countDown();
        }

        assertTrue(taskAdded.await(5, TimeUnit.SECONDS));
        registerThread.join(5_000);
        addThread.join(5_000);
    }

    private static WildFlyMetricMetadata metadata(PathAddress address) {
        return new WildFlyMetricMetadata("metric", address, "description", MeasurementUnit.NONE, MetricMetadata.Type.GAUGE);
    }
}
