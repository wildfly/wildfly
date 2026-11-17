/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.extension.metrics;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import org.jboss.as.controller.PathAddress;
import org.junit.Test;

/** Verifies retry-queue coalescing, cancellation, ordering, and generations. */
public class WildFlyMetricsResourceMetricsRetryQueueTest {
    private static final long TIMEOUT_SECONDS = 5;
    private static final long SHORT_TIMEOUT_MILLIS = 200;

    /** Verifies duplicate addresses are processed once. */
    @Test
    public void coalescesDuplicateAddresses() throws Exception {
        ExecutorService executor = Executors.newSingleThreadExecutor();
        try {
            CountDownLatch processed = new CountDownLatch(1);
            AtomicInteger calls = new AtomicInteger();
            PathAddress address = PathAddress.pathAddress("subsystem", "test");
            ResourceMetricsRetryQueue queue = new ResourceMetricsRetryQueue(executor, (pending, attempt) -> {
                calls.incrementAndGet();
                processed.countDown();
                return true;
            }, (pending, attempts) -> { });

            queue.add(address);
            queue.add(address);

            assertTrue("Address was not processed", processed.await(TIMEOUT_SECONDS, TimeUnit.SECONDS));
            assertEquals("Duplicate address should be processed once", 1, calls.get());
            queue.stop();
        } finally {
            executor.shutdownNow();
        }
    }

    /**
     * Verifies that a failed address is retried behind other queued work.
     *
     * <p>The executor stores the drain task instead of running it immediately. After
     * both addresses are queued, the test runs that task directly. The processor
     * records each address, fails {@code first}'s initial attempt, and succeeds for
     * {@code second} and the retry. Since the queue appends failed work at its tail,
     * {@code second} must be processed before {@code first} is retried.
     *
     * <p>Running the stored task synchronously makes the queue order deterministic
     * without coordinating worker threads with latches.
     */
    @Test
    public void retriesAddressesRoundRobin() throws Exception {
        PathAddress first = PathAddress.pathAddress("subsystem", "first");
        PathAddress second = PathAddress.pathAddress("subsystem", "second");
        Queue<Runnable> submittedTasks = new ArrayDeque<>();
        Executor executor = submittedTasks::add;
        List<PathAddress> attempts = new ArrayList<>();
        ResourceMetricsRetryQueue queue = new ResourceMetricsRetryQueue(
                executor,
                (address, attempt) -> {
                    attempts.add(address.address());
                    return address.address().equals(second) || attempt > 1;
                },
                (address, attempt) -> {});

        queue.add(first);
        queue.add(second);

        assertEquals("One drain task should be waiting", 1, submittedTasks.size());
        submittedTasks.remove().run();
        assertEquals("A failed address should be retried after other queued work", List.of(first, second, first), attempts);
        queue.stop();
    }

    /** Verifies removing and re-adding an address creates a new generation. */
    @Test
    public void removeAndReaddUsesTheNewGeneration() throws Exception {
        ExecutorService executor = Executors.newSingleThreadExecutor();
        try {
            PathAddress address = PathAddress.pathAddress("subsystem", "test");
            CountDownLatch firstAttempt = new CountDownLatch(1);
            CountDownLatch release = new CountDownLatch(1);
            CountDownLatch secondAttempt = new CountDownLatch(1);
            AtomicInteger calls = new AtomicInteger();
            AtomicLong firstGeneration = new AtomicLong();
            AtomicLong secondGeneration = new AtomicLong();
            ResourceMetricsRetryQueue queue = new ResourceMetricsRetryQueue(executor, (pending, attempt) -> {
                if (calls.incrementAndGet() == 1) {
                    firstGeneration.set(pending.generation());
                    firstAttempt.countDown();
                    try {
                        release.await();
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                } else {
                    secondGeneration.set(pending.generation());
                    secondAttempt.countDown();
                }
                return true;
            }, (pending, attempts) -> { });

            queue.add(address);
            assertTrue("First generation did not start", firstAttempt.await(TIMEOUT_SECONDS, TimeUnit.SECONDS));
            queue.remove(address);
            queue.add(address);
            release.countDown();

            assertTrue("Second generation did not start", secondAttempt.await(TIMEOUT_SECONDS, TimeUnit.SECONDS));
            assertTrue("Re-added address should use a new generation", firstGeneration.get() != secondGeneration.get());
            queue.stop();
        } finally {
            executor.shutdownNow();
        }
    }

    /** Verifies removing a parent cancels all pending descendant work. */
    @Test
    public void removingParentCancelsPendingDescendants() throws Exception {
        ExecutorService executor = Executors.newSingleThreadExecutor();
        try {
            PathAddress parent = PathAddress.pathAddress("subsystem", "parent");
            PathAddress child = parent.append("child", "one");
            CountDownLatch parentStarted = new CountDownLatch(1);
            CountDownLatch releaseParent = new CountDownLatch(1);
            CountDownLatch childProcessed = new CountDownLatch(1);
            ResourceMetricsRetryQueue queue = new ResourceMetricsRetryQueue(executor, (pending, attempt) -> {
                if (pending.address().equals(parent)) {
                    parentStarted.countDown();
                    try {
                        releaseParent.await();
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                } else if (pending.address().equals(child)) {
                    childProcessed.countDown();
                }
                return true;
            }, (pending, attempts) -> { });

            queue.add(parent);
            assertTrue("Parent work did not start", parentStarted.await(TIMEOUT_SECONDS, TimeUnit.SECONDS));
            queue.add(child);
            queue.remove(parent);
            releaseParent.countDown();

            assertTrue("Removed descendant should not be processed", !childProcessed.await(SHORT_TIMEOUT_MILLIS, TimeUnit.MILLISECONDS));
            queue.stop();
        } finally {
            executor.shutdownNow();
        }
    }
}
