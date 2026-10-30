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

/** Verifies Micrometer metric removal and registration-task synchronization. */
public class MetricRegistrationTest {
    /** Verifies removing a resource also removes meters for its children. */
    @Test
    public void unregistersMetricsForRemovedResourceAndChildren() {
        WildFlyCompositeRegistry registry = new WildFlyCompositeRegistry();
        MetricRegistration registration = new MetricRegistration(registry);
        PathAddress resource = PathAddress.pathAddress("subsystem", "test");

        registration.registerMetric(new WildFlyMetric(null, resource, "metric"), metadata(resource));
        registration.registerMetric(new WildFlyMetric(null, resource.append("child", "one"), "metric"),
                metadata(resource.append("child", "one")));
        assertEquals("Both resource meters should be registered", 2, registry.getMeters().size());

        registration.unregister(resource);

        assertEquals("Removing the resource should remove its meters", 0, registry.getMeters().size());
    }

    /** Verifies adding a registration task waits for an active registration pass. */
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
        assertTrue("Registration task did not start", taskStarted.await(5, TimeUnit.SECONDS));

        Thread addThread = new Thread(() -> {
            registration.addRegistrationTask(() -> { });
            taskAdded.countDown();
        });
        addThread.start();

        try {
            assertFalse("Adding a task should wait for registration", taskAdded.await(100, TimeUnit.MILLISECONDS));
        } finally {
            releaseTask.countDown();
        }

        assertTrue("Queued task was not added after registration", taskAdded.await(5, TimeUnit.SECONDS));
        registerThread.join(5_000);
        addThread.join(5_000);
    }

    /** Creates metadata for a test meter at the supplied address. */
    private static WildFlyMetricMetadata metadata(PathAddress address) {
        return new WildFlyMetricMetadata("metric", address, "description", MeasurementUnit.NONE, MetricMetadata.Type.GAUGE);
    }
}
