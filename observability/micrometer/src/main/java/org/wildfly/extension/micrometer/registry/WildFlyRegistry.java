/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.extension.micrometer.registry;

import java.util.Arrays;
import java.util.Locale;
import java.util.OptionalDouble;
import java.util.stream.Collectors;

import io.micrometer.core.instrument.FunctionCounter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.Meter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tag;
import io.micrometer.core.instrument.Tags;
import org.jboss.as.controller.client.helpers.MeasurementUnit;
import org.wildfly.extension.micrometer.metrics.MetricMetadata;
import org.wildfly.extension.micrometer.metrics.WildFlyMetric;

public interface WildFlyRegistry extends AutoCloseable {
    Meter remove(Meter.Id mappedId);
    void close();

    default Meter.Id addMeter(WildFlyMetric metric, MetricMetadata metadata) {
        return switch (metadata.getType()) {
            case GAUGE -> addGauge(metric, metadata);
            case COUNTER -> addCounter(metric, metadata);
        };
    }

    private Meter.Id addCounter(WildFlyMetric metric, MetricMetadata metadata) {
        return FunctionCounter.builder(metadata.getMetricName(), metric,
                        value -> getMetricValue(metric, metadata.getMeasurementUnit()))
                .tags(getTags(metadata))
                .baseUnit(getBaseUnit(metadata))
                .description(metadata.getDescription())
                .register((MeterRegistry) this)
                .getId();
    }

    private Meter.Id addGauge(WildFlyMetric metric, MetricMetadata metadata) {
        return Gauge.builder(metadata.getMetricName(), metric,
                        value -> getMetricValue(metric, metadata.getMeasurementUnit()))
                .tags(getTags(metadata))
                .baseUnit(getBaseUnit(metadata))
                .description(metadata.getDescription())
                .register((MeterRegistry) this)
                .getId();
    }

    private Tags getTags(MetricMetadata metadata) {
        return Tags.of(Arrays.stream(metadata.getTags())
                .map(t -> Tag.of(t.getKey(), t.getValue()))
                .collect(Collectors.toList()));
    }

    private String getBaseUnit(MetricMetadata metadata) {
        String measurementUnit = metadata.getBaseMetricUnit();
        return "none".equalsIgnoreCase(measurementUnit) ? null : measurementUnit.toLowerCase(Locale.ENGLISH);
    }

    private double getMetricValue(WildFlyMetric metric, MeasurementUnit unit) {
        OptionalDouble metricValue = metric.getValue();
        return metricValue.isPresent() ?
                scaleToBaseUnit(metricValue.getAsDouble(), unit) :
                0.0;
    }

    private double scaleToBaseUnit(double value, MeasurementUnit unit) {
        return value * MeasurementUnit.calculateOffset(unit, unit.getBaseUnits());
    }
}
