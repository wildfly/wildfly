/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.extension.metrics;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.jboss.as.controller.PathAddress;

public class MetricRegistration {

    private final List<Runnable> registrationTasks = new ArrayList<>();
    private final List<RegisteredMetric> metrics = new ArrayList<>();
    private final MetricRegistry registry;
    private final List<Runnable> cleanUpTasks = new ArrayList<>();

    public MetricRegistration(MetricRegistry registry) {
        this.registry = registry;
    }

    public void register() { // synchronized to avoid registering same thing twice. Shouldn't really be possible; just being cautious
        synchronized (registry) {
            for (Runnable task : registrationTasks) {
                task.run();
            }
            // This object will last until undeploy or server stop,
            // so clean up and save memory
            registrationTasks.clear();
        }
    }

    public void unregister() {
        synchronized (registry) {
            for (RegisteredMetric metric : metrics) {
                registry.unregister(metric.id());
            }
            metrics.clear();
        }
        for (Runnable cleanupTask : cleanUpTasks) {
            cleanupTask.run();
        }
        cleanUpTasks.clear();
    }

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

    public void registerMetric(WildFlyMetric metric, WildFlyMetricMetadata metadata) {
        registry.registerMetric(metric, metadata);
        metrics.add(new RegisteredMetric(metadata.getMetricID(), metadata.getAddress()));
    }

    public synchronized void addRegistrationTask(Runnable task) {
        registrationTasks.add(task);
    }

    void addCleanUpTask(Runnable task) {
        cleanUpTasks.add(task);
    }

    private static boolean isDescendant(PathAddress parent, PathAddress candidate) {
        return candidate.size() >= parent.size() && parent.equals(candidate.subAddress(0, parent.size()));
    }

    private record RegisteredMetric(MetricID id, PathAddress address) { }
}
