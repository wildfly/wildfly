/*
 * JBoss, Home of Professional Open Source.
 *
 * Copyright 2022 Red Hat, Inc., and individual contributors
 * as indicated by the @author tags.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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
