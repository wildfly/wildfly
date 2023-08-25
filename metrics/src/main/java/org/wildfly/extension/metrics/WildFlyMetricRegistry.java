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
package org.wildfly.extension.metrics;

import static java.util.Objects.requireNonNull;

import java.io.Closeable;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;


public class WildFlyMetricRegistry implements Closeable, MetricRegistry {

    /* Key is the metric name */
    private Map<String, MetricMetadata> metadataMap = new HashMap();
    private Map<MetricID, Metric> metricMap = new TreeMap<>();
    private final ReadWriteLock lock = new ReentrantReadWriteLock();
    private final Map<String, WildFlyMetricRegistry> deployments = new ConcurrentHashMap<>();


    @Override
    public void close() {

        lock.writeLock().lock();
        try {
            metricMap.clear();
            metadataMap.clear();
        } finally {
            lock.writeLock().unlock();
        }
    }

    Map<MetricID, Metric> getMetrics() {
        return metricMap;
    }

    Map<String, MetricMetadata> getMetricMetadata() {
        return metadataMap;
    }

    @Override
    public synchronized void registerMetric(Metric metric, MetricMetadata metadata) {
        requireNonNull(metadata);
        requireNonNull(metric);

        lock.writeLock().lock();
        try {
            MetricID metricID = metadata.getMetricID();
            if (!metadataMap.containsKey(metadata.getMetricName())) {
                metadataMap.put(metadata.getMetricName(), metadata);
            }
            metricMap.put(metricID, metric);
        } finally {
            lock.writeLock().unlock();
        }
    }

    @Override
    public void unregister(MetricID metricID) {
        lock.writeLock().lock();
        try {
            metricMap.remove(metricID);
        } finally {
            lock.writeLock().unlock();
        }
    }

    @Override
    public void readLock() {
        lock.readLock().lock();
    }

    @Override
    public void unlock() {
        lock.readLock().unlock();
    }

    public MetricRegistry addDeploymentRegistry(String path) {
        WildFlyMetricRegistry deploymentRegistry = new WildFlyMetricRegistry();
        deployments.put(path, deploymentRegistry);

        return deploymentRegistry;
    }

    public void removeDeploymentRegistry(String path) {
        deployments.remove(path);
    }

    public Map<String, WildFlyMetricRegistry> getDeploymentRegistries() {
        return deployments;
    }
}
