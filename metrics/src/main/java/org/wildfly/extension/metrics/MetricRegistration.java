/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.extension.metrics;

import java.util.ArrayList;
import java.util.List;

public class MetricRegistration {

    private final List<Runnable> registrationTasks = new ArrayList<>();
    private final List<MetricID> unregistrationTasks = new ArrayList<>();
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
            for (MetricID id : unregistrationTasks) {
                registry.unregister(id);
            }
            unregistrationTasks.clear();
        }
        for (Runnable cleanupTask : cleanUpTasks) {
            cleanupTask.run();
        }
        cleanUpTasks.clear();
    }

    public void registerMetric(WildFlyMetric metric, WildFlyMetricMetadata metadata) {
        registry.registerMetric(metric, metadata);
    }

    public synchronized void addRegistrationTask(Runnable task) {
        registrationTasks.add(task);
    }

    public void addUnregistrationTask(MetricID metricID) {
        unregistrationTasks.add(metricID);
    }

    void addCleanUpTask(Runnable task) {
        cleanUpTasks.add(task);
    }
}
