/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.extension.metrics;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.jboss.as.controller.PathAddress;

/** Tracks registered metrics and coordinates cleanup shared by overlapping lifecycle calls. */
public class MetricRegistration {

    private final List<Runnable> registrationTasks = new ArrayList<>();
    private final List<RegisteredMetric> metrics = new ArrayList<>();
    private final MetricRegistry registry;
    private final List<Runnable> cleanUpTasks = new ArrayList<>();
    /** Guarded by the registry monitor; full unregistration permanently closes metric writes. */
    private boolean unregistered;

    public MetricRegistration(MetricRegistry registry) {
        this.registry = registry;
    }

    /** Executes queued registration work only while this registration remains active. */
    public void register() {
        synchronized (registry) {
            if (unregistered) {
                return;
            }
            for (Runnable task : registrationTasks) {
                task.run();
            }
            // This object will last until undeploy or server stop,
            // so clean up and save memory
            registrationTasks.clear();
        }
    }

    /**
     * Permanently stops registration, removes metrics, and claims cleanup so concurrent calls cannot run it twice.
     * Cleanup callbacks run outside the registry lock so other lifecycle calls can proceed.
     */
    public void unregister() {
        List<Runnable> claimedCleanupTasks;
        synchronized (registry) {
            // Permanently reject new registrations and discard work queued before shutdown.
            unregistered = true;
            registrationTasks.clear();
            for (RegisteredMetric metric : metrics) {
                registry.unregister(metric.id());
            }
            metrics.clear();
            // Copy the callbacks into a private list so this call can run them after releasing the lock.
            claimedCleanupTasks = new ArrayList<>(cleanUpTasks);
            // Claim the callbacks while still holding the lock so concurrent unregister calls cannot run them again.
            cleanUpTasks.clear();
        }
        // Run the claimed callbacks without blocking other lifecycle calls on the registry lock.
        for (Runnable cleanupTask : claimedCleanupTasks) {
            cleanupTask.run();
        }
    }

    /**
     * Removes metrics for a resource subtree while allowing later resource generations to register metrics.
     *
     * @param address the address of the removed resource
     */
    public void unregister(PathAddress address) {
        synchronized (registry) {
            for (Iterator<RegisteredMetric> iterator = metrics.iterator(); iterator.hasNext();) {
                RegisteredMetric metric = iterator.next();
                if (isDescendant(address, metric.address())) {
                    registry.unregister(metric.id());
                    iterator.remove();
                }
            }
        }
    }

    /**
     * Registers a discovered metric unless full unregistration has already stopped metric writes.
     *
     * @param metric the metric whose value will be read
     * @param metadata the metric's identity and description
     */
    public void registerMetric(WildFlyMetric metric, WildFlyMetricMetadata metadata) {
        synchronized (registry) {
            if (unregistered) {
                return;
            }
            registry.registerMetric(metric, metadata);
            metrics.add(new RegisteredMetric(metadata.getMetricID(), metadata.getAddress()));
        }
    }

    /**
     * Queues metric registration work while active without retaining work submitted after shutdown.
     *
     * @param task the metric registration callback
     */
    public void addRegistrationTask(Runnable task) {
        synchronized (registry) {
            if (!unregistered) {
                registrationTasks.add(task);
            }
        }
    }

    /**
     * Queues cleanup while active, or runs it immediately after shutdown so late listeners are not retained.
     * Cleanup runs outside the registry lock.
     *
     * @param task the cleanup callback to run when the registration is unregistered
     */
    void addCleanUpTask(Runnable task) {
        synchronized (registry) {
            if (!unregistered) {
                cleanUpTasks.add(task);
                return;
            }
        }
        task.run();
    }

    private static boolean isDescendant(PathAddress parent, PathAddress candidate) {
        return candidate.size() >= parent.size() && parent.equals(candidate.subAddress(0, parent.size()));
    }

    private record RegisteredMetric(MetricID id, PathAddress address) { }
}
