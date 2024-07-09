/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.extension.micrometer.metrics;

import java.util.ArrayList;
import java.util.List;

import io.micrometer.core.instrument.Meter;
import org.wildfly.extension.micrometer.registry.WildFlyRegistry;

public class MetricRegistration {

    private final List<Runnable> registrationTasks = new ArrayList<>();
    private final List<Meter.Id> unregistrationTasks = new ArrayList<>();
    private final WildFlyRegistry registry;

    public MetricRegistration(WildFlyRegistry registry) {
        this.registry = registry;
    }

    public void register() {
        // synchronized to avoid registering same thing twice. Shouldn't really be possible; just being cautious
        synchronized (registry) {
            registrationTasks.forEach(Runnable::run);
            registrationTasks.clear();
        }
    }

    public void unregister() {
        synchronized (registry) {
            unregistrationTasks.forEach(registry::remove);
            unregistrationTasks.clear();
        }
    }

    public void registerMetric(WildFlyMetric metric, WildFlyMetricMetadata metadata) {
        unregistrationTasks.add(registry.addMeter(metric, metadata));
    }

    public synchronized void addRegistrationTask(Runnable task) {
        registrationTasks.add(task);
    }
}
