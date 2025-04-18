/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.extension.undertow.deployment;

import java.util.HashMap;
import java.util.Map;

import io.undertow.server.handlers.MetricsHandler;
import io.undertow.servlet.api.MetricsCollector;

/**
 * @author Tomaz Cerar (c) 2014 Red Hat Inc.
 */
public class UndertowMetricsCollector implements MetricsCollector {
    private final Map<String, MetricsHandler> metrics = new HashMap<>();

    @Override
    public void registerMetric(String name, MetricsHandler handler) {
        metrics.put(name, handler);
    }

    public MetricsHandler.MetricResult getMetrics(String name) {
        if (metrics.containsKey(name)) {
            return metrics.get(name).getMetrics();
        }
        return null;
    }
}
