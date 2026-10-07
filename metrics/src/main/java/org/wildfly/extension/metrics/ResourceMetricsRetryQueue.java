/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.extension.metrics;

import java.util.Queue;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Executor;
import java.util.concurrent.Future;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;

import org.jboss.as.controller.PathAddress;

/**
 * Runs resource metric collection outside the management notification thread.
 *
 * The queue coalesces duplicate notifications and lets callers cancel work for
 * resources that are removed before collection completes.
 */
final class ResourceMetricsRetryQueue {
    private static final long RETRY_TIMEOUT_NANOS = TimeUnit.MINUTES.toNanos(30);
    private static final long RETRY_DELAY_MILLIS = 100;

    private final Executor executor;
    private final BiFunction<PendingResource, Integer, Boolean> processor;
    private final BiConsumer<PathAddress, Integer> timeoutProcessor;
    private final Queue<PendingResource> queue = new ConcurrentLinkedQueue<>();
    private final Map<PathAddress, Long> pending = new ConcurrentHashMap<>();

    private Future<?> task;
    private boolean stopped;
    private long nextGeneration;

    ResourceMetricsRetryQueue(Executor executor, BiFunction<PendingResource, Integer, Boolean> processor,
                              BiConsumer<PathAddress, Integer> timeoutProcessor) {
        this.executor = executor;
        this.processor = processor;
        this.timeoutProcessor = timeoutProcessor;
    }

    /**
     * Queues an address for asynchronous collection unless it is already queued.
     *
     * @param address the management resource address to collect
     */
    void add(PathAddress address) {
        synchronized (this) {
            if (stopped || pending.containsKey(address)) {
                return;
            }
            long generation = ++nextGeneration;
            pending.put(address, generation);
            queue.add(new PendingResource(address, System.nanoTime() + RETRY_TIMEOUT_NANOS, 0, generation));
            if (task == null) {
                FutureTask<Void> newTask = new FutureTask<>(this::drain, null);
                task = newTask;
                executor.execute(newTask);
            }
        }
    }

    /**
     * Cancels collection for an address that is no longer present.
     *
     * @param address the removed management resource address
     */
    synchronized void remove(PathAddress address) {
        pending.keySet().removeIf(candidate -> isDescendant(address, candidate));
        queue.removeIf(pendingResource -> isDescendant(address, pendingResource.address()));
    }

    /**
     * Determines whether a resource address is the removed resource or one of its descendants.
     *
     * @param parent the removed resource address
     * @param candidate the pending resource address
     * @return {@code true} when the candidate belongs to the removed resource subtree
     */
    private static boolean isDescendant(PathAddress parent, PathAddress candidate) {
        return candidate.size() >= parent.size() && parent.equals(candidate.subAddress(0, parent.size()));
    }

    /**
     * Determines whether collection is still active for an address.
     *
     * @param address the resource address
     * @return {@code true} if the address has not been removed or processed
     */
    boolean isPending(PathAddress address) {
        return pending.containsKey(address);
    }

    /**
     * Determines whether a specific enqueue generation is still active.
     *
     * @param resource the queued resource generation
     * @return {@code true} if this generation has not been removed or replaced
     */
    boolean isPending(PendingResource resource) {
        return Long.valueOf(resource.generation()).equals(pending.get(resource.address()));
    }

    /**
     * Cancels the worker and discards all pending collection work.
     */
    void stop() {
        Future<?> currentTask;
        synchronized (this) {
            stopped = true;
            currentTask = task;
            task = null;
        }
        pending.clear();
        queue.clear();
        if (currentTask != null) {
            currentTask.cancel(true);
        }
    }

    /**
     * Processes queued addresses and starts a replacement worker when new work
     * arrived while the current worker was draining.
     */
    private void drain() {
        try {
            while (!queue.isEmpty()) {
                int batchSize = queue.size();
                boolean retry = false;
                for (int i = 0; i < batchSize; i++) {
                    PendingResource pendingResource = queue.poll();
                    if (pendingResource == null) {
                        break;
                    }
                    PathAddress address = pendingResource.address();
                    if (!isPending(pendingResource)) {
                        continue;
                    }
                    int attempt = pendingResource.attempt() + 1;
                    if (System.nanoTime() >= pendingResource.deadline()) {
                        timeoutProcessor.accept(address, attempt);
                        pending.remove(address, pendingResource.generation());
                    } else if (processor.apply(pendingResource, attempt)) {
                        pending.remove(address, pendingResource.generation());
                    } else {
                        queue.add(new PendingResource(address, pendingResource.deadline(), attempt,
                                pendingResource.generation()));
                        retry = true;
                    }
                }
                if (retry) {
                    try {
                        Thread.sleep(RETRY_DELAY_MILLIS);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        return;
                    }
                }
            }
        } finally {
            synchronized (this) {
                task = null;
                if (!stopped && !queue.isEmpty()) {
                    FutureTask<Void> newTask = new FutureTask<>(this::drain, null);
                    task = newTask;
                    executor.execute(newTask);
                }
            }
        }
    }

    /**
     * Tracks an address, its absolute retry deadline, and the number of attempts
     * already made.
     */
    record PendingResource(PathAddress address, long deadline, int attempt, long generation) {
    }
}
