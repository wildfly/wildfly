/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.extension.micrometer.metrics;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import io.micrometer.core.instrument.Meter;
import org.jboss.as.controller.PathAddress;
import org.wildfly.extension.micrometer.registry.WildFlyRegistry;

public class MetricRegistration {

    private final List<Runnable> registrationTasks = new ArrayList<>();
    private final List<RegisteredMetric> metrics = new ArrayList<>();
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
        unregister(address -> true);
    }

    public void unregister(PathAddress address) {
        unregister(metric -> isDescendant(address, metric.metric().getAddress()));
    }

    private void unregister(java.util.function.Predicate<RegisteredMetric> predicate) {
        synchronized (registry) {
            for (Iterator<RegisteredMetric> iterator = metrics.iterator(); iterator.hasNext();) {
                RegisteredMetric metric = iterator.next();
                if (predicate.test(metric)) {
                    registry.remove(metric.id());
                    iterator.remove();
                }
            }
        }
    }

    public void registerMetric(WildFlyMetric metric, WildFlyMetricMetadata metadata) {
        metrics.add(new RegisteredMetric(metric, registry.addMeter(metric, metadata)));
    }

    public synchronized void addRegistrationTask(Runnable task) {
        registrationTasks.add(task);
    }

    private static boolean isDescendant(PathAddress parent, PathAddress candidate) {
        if (candidate.size() < parent.size()) {
            return false;
        }
        return parent.equals(candidate.subAddress(0, parent.size()));
    }

    private record RegisteredMetric(WildFlyMetric metric, Meter.Id id) { }
}
