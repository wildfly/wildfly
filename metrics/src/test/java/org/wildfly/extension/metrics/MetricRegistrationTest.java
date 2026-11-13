/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.extension.metrics;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.client.helpers.MeasurementUnit;
import org.junit.Test;

/** Verifies metric removal, terminal shutdown, and ownership of concurrent cleanup work. */
public class MetricRegistrationTest {
    /** Verifies resource removal includes children while allowing metrics for a later resource generation. */
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

        assertEquals("Only the unrelated metric should remain", 1, registry.getMetrics().size());
        registration.registerMetric(new WildFlyMetric(null, resource, "metric"), metadata(resource));
        assertEquals("Resource removal must allow later metric registration", 2, registry.getMetrics().size());
    }

    /** Verifies metadata is removed after the final metric with a name is removed. */
    @Test
    public void removesMetadataAfterTheLastMetricWithThatName() {
        WildFlyMetricRegistry registry = new WildFlyMetricRegistry();
        MetricRegistration registration = new MetricRegistration(registry);
        PathAddress first = PathAddress.pathAddress("subsystem", "first");
        PathAddress second = PathAddress.pathAddress("subsystem", "second");

        registration.registerMetric(new WildFlyMetric(null, first, "metric"), metadata(first));
        registration.registerMetric(new WildFlyMetric(null, second, "metric"), metadata(second));

        registration.unregister(first);
        assertEquals("Metadata should remain while one metric exists", 1, registry.getMetricMetadata().size());

        registration.unregister(second);
        assertEquals("Metadata should be removed after the final metric", 0, registry.getMetricMetadata().size());
    }

    /** Verifies overlapping unregister calls run each cleanup once without holding up the other caller. */
    @Test
    public void concurrentUnregisterRunsCleanupOnce() throws Exception {
        MetricRegistration registration = new MetricRegistration(new WildFlyMetricRegistry());
        CountDownLatch cleanupStarted = new CountDownLatch(1);
        CountDownLatch releaseCleanup = new CountDownLatch(1);
        AtomicInteger firstCleanupCalls = new AtomicInteger();
        AtomicInteger secondCleanupCalls = new AtomicInteger();
        registration.addCleanUpTask(() -> {
            if (firstCleanupCalls.incrementAndGet() == 1) {
                cleanupStarted.countDown();
                try {
                    assertTrue("Cleanup was not released", releaseCleanup.await(5, TimeUnit.SECONDS));
                } catch (InterruptedException exception) {
                    Thread.currentThread().interrupt();
                    throw new AssertionError(exception);
                }
            }
        });
        registration.addCleanUpTask(secondCleanupCalls::incrementAndGet);

        Runnable unregister = registration::unregister;
        ExecutorService executor = Executors.newFixedThreadPool(2);
        try {
            Future<?> firstUnregister = executor.submit(unregister);
            assertTrue("First unregister did not start cleanup", cleanupStarted.await(5, TimeUnit.SECONDS));

            Future<?> secondUnregister = executor.submit(unregister);
            secondUnregister.get(5, TimeUnit.SECONDS);
            assertEquals("The second unregister must not run cleanup owned by the first", 1, firstCleanupCalls.get());

            releaseCleanup.countDown();
            firstUnregister.get(5, TimeUnit.SECONDS);
            assertEquals("The remaining cleanup must run exactly once", 1, secondCleanupCalls.get());
        } finally {
            releaseCleanup.countDown();
            executor.shutdownNow();
        }
    }

    /** Verifies a delayed RUNNING callback cannot register metrics queued before shutdown. */
    @Test
    public void ignoresQueuedRegistrationAfterUnregister() {
        WildFlyMetricRegistry registry = new WildFlyMetricRegistry();
        MetricRegistration registration = new MetricRegistration(registry);
        PathAddress resource = PathAddress.pathAddress("subsystem", "test");
        registration.addRegistrationTask(() -> registration.registerMetric(
                new WildFlyMetric(null, resource, "metric"), metadata(resource)));

        registration.unregister();
        registration.register();

        assertEquals("Delayed registration must not restore metrics after shutdown", 0, registry.getMetrics().size());
        assertEquals("Delayed registration must not restore metadata after shutdown", 0, registry.getMetricMetadata().size());
    }

    /** Verifies registration work submitted after shutdown is never executed. */
    @Test
    public void ignoresRegistrationTasksAddedAfterUnregister() {
        MetricRegistration registration = new MetricRegistration(new WildFlyMetricRegistry());
        AtomicInteger calls = new AtomicInteger();

        registration.unregister();
        registration.addRegistrationTask(calls::incrementAndGet);
        registration.register();

        assertEquals("Shutdown registrations must not accept more work", 0, calls.get());
    }

    /** Verifies direct metric writes cannot restore a fully unregistered registration. */
    @Test
    public void ignoresMetricWritesAfterUnregister() {
        WildFlyMetricRegistry registry = new WildFlyMetricRegistry();
        MetricRegistration registration = new MetricRegistration(registry);
        PathAddress resource = PathAddress.pathAddress("subsystem", "test");

        registration.unregister();
        registration.registerMetric(new WildFlyMetric(null, resource, "metric"), metadata(resource));

        assertEquals("Late writes must not restore metrics", 0, registry.getMetrics().size());
        assertEquals("Late writes must not restore metadata", 0, registry.getMetricMetadata().size());
    }

    /** Verifies cleanup arriving after shutdown runs immediately outside the registry lock. */
    @Test
    public void immediatelyRunsCleanupAddedAfterUnregister() {
        WildFlyMetricRegistry registry = new WildFlyMetricRegistry();
        MetricRegistration registration = new MetricRegistration(registry);
        AtomicInteger calls = new AtomicInteger();

        registration.unregister();
        registration.addCleanUpTask(() -> {
            assertFalse("Late cleanup must not hold the registry lock", Thread.holdsLock(registry));
            calls.incrementAndGet();
        });

        assertEquals("Late cleanup must run without another unregister call", 1, calls.get());
        registration.unregister();
        assertEquals("Late cleanup must run only once", 1, calls.get());
    }

    /** Creates metadata for a test metric at the supplied address. */
    private static WildFlyMetricMetadata metadata(PathAddress address) {
        return new WildFlyMetricMetadata("metric", address, null, "description", MeasurementUnit.NONE,
                MetricMetadata.Type.GAUGE);
    }
}
