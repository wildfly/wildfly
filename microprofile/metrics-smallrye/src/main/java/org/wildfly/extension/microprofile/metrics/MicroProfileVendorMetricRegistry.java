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
package org.wildfly.extension.microprofile.metrics;

import static org.eclipse.microprofile.metrics.MetricRegistry.Type.BASE;
import static org.eclipse.microprofile.metrics.MetricRegistry.Type.VENDOR;
import static org.wildfly.extension.metrics.MetricMetadata.Type.COUNTER;
import static org.wildfly.extension.microprofile.metrics._private.MicroProfileMetricsLogger.LOGGER;


import java.util.Optional;
import java.util.OptionalDouble;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import io.smallrye.metrics.ExtendedMetadata;
import io.smallrye.metrics.MetricRegistries;
import org.eclipse.microprofile.metrics.Counter;
import org.eclipse.microprofile.metrics.Gauge;
import org.eclipse.microprofile.metrics.Metadata;
import org.eclipse.microprofile.metrics.Metric;
import org.eclipse.microprofile.metrics.MetricType;
import org.eclipse.microprofile.metrics.MetricUnits;
import org.eclipse.microprofile.metrics.Tag;
import org.jboss.as.controller.client.helpers.MeasurementUnit;
import org.wildfly.extension.metrics.MetricID;
import org.wildfly.extension.metrics.MetricMetadata;
import org.wildfly.extension.metrics.MetricRegistry;
import org.wildfly.extension.metrics.WildFlyMetricMetadata;

public class MicroProfileVendorMetricRegistry implements MetricRegistry {

    final org.eclipse.microprofile.metrics.MetricRegistry vendorRegistry = MetricRegistries.get(VENDOR);
    private final ReadWriteLock lock = new ReentrantReadWriteLock();

    @Override
    public void registerMetric(org.wildfly.extension.metrics.Metric metric, MetricMetadata metadata) {
        final Metric mpMetric;
        if (metadata.getType() == COUNTER) {
            mpMetric = new Counter() {
                @Override
                public void inc() {
                }

                @Override
                public void inc(long n) {
                }

                @Override
                public long getCount() {
                    OptionalDouble value = metric.getValue();

                    if (!value.isPresent()) {
                        // Smallrye will discard reporting this metric if we throw a
                        // RuntimeException, after logging at DEBUG. That's what we want.
                        throw LOGGER.metricUnavailable();
                    }
                    return Double.valueOf(value.getAsDouble()).longValue();
                }
            };
        } else {
            mpMetric = new Gauge<Number>() {
                @Override
                public Double getValue() {
                    OptionalDouble value = metric.getValue();

                    if (!value.isPresent()) {
                        // Smallrye will discard reporting this metric if we throw a
                        // RuntimeException, after logging at DEBUG. That's what we want.
                        throw LOGGER.metricUnavailable();
                    }
                    return value.getAsDouble();
                }
            };
        }

        lock.writeLock().lock();
        try {
            synchronized (vendorRegistry) { // TODO does the writeLock eliminate the need for this synchronized?
                final Metadata mpMetadata;
                Metadata existingMetadata = vendorRegistry.getMetadata().get(metadata.getMetricName());
                if (existingMetadata != null) {
                    mpMetadata = existingMetadata;
                } else {
                    mpMetadata = new ExtendedMetadata(metadata.getMetricName(), metadata.getMetricName(), metadata.getDescription(),
                            metadata.getType() == COUNTER ? MetricType.COUNTER : MetricType.GAUGE, metricUnit(metadata.getMeasurementUnit()),
                            null, false,
                            // for WildFly subsystem metrics, the microprofile scope is put in the OpenMetrics tags
                            // so that the name of the metric does not change ("vendor_" will not be prepended to it).
                            Optional.of(false));
                }
                Tag[] mpTags = toMicroProfileMetricsTags(metadata.getTags());
                vendorRegistry.register(mpMetadata, mpMetric, mpTags);
            }
        } finally {
            lock.writeLock().unlock();
        }
    }

    @Override
    public void unregister(MetricID metricID) {
        lock.writeLock().lock();
        try {
            vendorRegistry.remove(toMicroProfileMetricID(metricID));
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

    private org.eclipse.microprofile.metrics.MetricID toMicroProfileMetricID(MetricID metricID) {
        return new org.eclipse.microprofile.metrics.MetricID(metricID.getMetricName(), toMicroProfileMetricsTags(metricID.getTags()));
    }

    void removeAllMetrics() {
        lock.writeLock().lock();
        try {
            for (org.eclipse.microprofile.metrics.MetricRegistry registry : new org.eclipse.microprofile.metrics.MetricRegistry[]{
                    MetricRegistries.get(BASE),
                    MetricRegistries.get(VENDOR)}) {
                for (String name : registry.getNames()) {
                    registry.remove(name);
                }
            }
        } finally {
            lock.writeLock().unlock();
        }
    }

    private Tag[] toMicroProfileMetricsTags(WildFlyMetricMetadata.MetricTag[] tags) {
        if (tags == null || tags.length == 0) {
            return new Tag[0];
        }
        Tag[] mpTags = new Tag[tags.length];
        for (int i = 0; i < tags.length; i++) {
            mpTags[i] = new Tag(tags[i].getKey(), tags[i].getValue());
        }
        return mpTags;
    }

    private String metricUnit(MeasurementUnit unit) {
        if (unit == null) {
            return MetricUnits.NONE;
        }
        switch (unit) {

            case PERCENTAGE:
                return MetricUnits.PERCENT;
            case BYTES:
                return MetricUnits.BYTES;
            case KILOBYTES:
                return MetricUnits.KILOBYTES;
            case MEGABYTES:
                return MetricUnits.MEGABYTES;
            case GIGABYTES:
                return MetricUnits.GIGABYTES;
            case TERABYTES:
                return "terabytes";
            case PETABYTES:
                return "petabytes";
            case BITS:
                return MetricUnits.BITS;
            case KILOBITS:
                return MetricUnits.KILOBITS;
            case MEGABITS:
                return MetricUnits.MEBIBITS;
            case GIGABITS:
                return MetricUnits.GIGABITS;
            case TERABITS:
                return "terabits";
            case PETABITS:
                return "petabits";
            case EPOCH_MILLISECONDS:
                return MetricUnits.MILLISECONDS;
            case EPOCH_SECONDS:
                return MetricUnits.SECONDS;
            case JIFFYS:
                return "jiffys";
            case NANOSECONDS:
                return MetricUnits.NANOSECONDS;
            case MICROSECONDS:
                return MetricUnits.MICROSECONDS;
            case MILLISECONDS:
                return MetricUnits.MILLISECONDS;
            case SECONDS:
                return MetricUnits.SECONDS;
            case MINUTES:
                return MetricUnits.MINUTES;
            case HOURS:
                return MetricUnits.HOURS;
            case DAYS:
                return MetricUnits.DAYS;
            case PER_JIFFY:
                return "per-jiffy";
            case PER_NANOSECOND:
                return "per_nanoseconds";
            case PER_MICROSECOND:
                return "per_microseconds";
            case PER_MILLISECOND:
                return "per_milliseconds";
            case PER_SECOND:
                return MetricUnits.PER_SECOND;
            case PER_MINUTE:
                return "per_minutes";
            case PER_HOUR:
                return "per_hour";
            case PER_DAY:
                return "per_day";
            case CELSIUS:
                return "degree_celsius";
            case KELVIN:
                return "kelvin";
            case FAHRENHEIGHT:
                return "degree_fahrenheit";
            case NONE:
            default:
                return "none";
        }
    }
}
