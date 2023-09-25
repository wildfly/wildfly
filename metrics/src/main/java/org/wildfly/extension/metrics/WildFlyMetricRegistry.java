/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.extension.metrics;

import static java.util.Objects.requireNonNull;

import java.io.Closeable;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;


public class WildFlyMetricRegistry implements Closeable, MetricRegistry {

    /* Key is the metric name */
    private Map<String, MetricMetadata> metadataMap = new HashMap();
    private Map<MetricID, Metric> metricMap = new TreeMap<>();
    private final ReadWriteLock lock = new ReentrantReadWriteLock();


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
}