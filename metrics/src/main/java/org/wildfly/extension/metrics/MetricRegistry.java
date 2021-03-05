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
