/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2017, Red Hat, Inc., and individual contributors
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
