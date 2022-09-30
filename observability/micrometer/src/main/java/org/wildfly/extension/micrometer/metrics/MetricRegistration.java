/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2020, Red Hat, Inc., and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.wildfly.extension.micrometer.metrics;

import java.util.ArrayList;
import java.util.List;

import io.micrometer.core.instrument.Meter;

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
            for (Runnable task : registrationTasks) {
                task.run();
            }
            registrationTasks.clear();
        }
    }

    public void unregister() {
        synchronized (registry) {
            for (Meter.Id id : unregistrationTasks) {
                registry.remove(id);
            }
            unregistrationTasks.clear();
        }
    }

    public void registerMetric(WildFlyMetric metric, WildFlyMetricMetadata metadata) {
        // TODO
        unregistrationTasks.add(registry.addMeter(metric, metadata).getId());
    }

    public synchronized void addRegistrationTask(Runnable task) {
        registrationTasks.add(task);
    }
}
