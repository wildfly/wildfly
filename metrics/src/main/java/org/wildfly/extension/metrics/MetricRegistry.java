/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.extension.metrics;

/**
 * Provides a registry of metrics.
 */
public interface MetricRegistry {

    /**
     * Registers the given metric. Calls will block if another thread
     * {@link #readLock() holds the registry read lock}.
     *
     * @param metric the metric. Cannot be {@code null}
     * @param metadata metadata for the metric. Cannot be {@code null}
     */
    void registerMetric(Metric metric, MetricMetadata metadata);

    /**
     * Unregisters the given metric, if it is registered. Calls will block if another thread
     * {@link #readLock() holds the registry read lock}.
     *
     * @param metricID the id for the metric. Cannot be {@code null}
     */
    void unregister(MetricID metricID);

    /**
     * Acquires a non-exclusive read lock that will cause calls from other threads
     * to {@link #registerMetric(Metric, MetricMetadata)} or {@link #unregister(MetricID)}
     * to block. Must be followed by a call to {@link #unlock()}.
     */
    void readLock();

    /**
     * Releases the non-exclusive lock obtained by a call to {@link #readLock()}.
     */
    void unlock();
}
