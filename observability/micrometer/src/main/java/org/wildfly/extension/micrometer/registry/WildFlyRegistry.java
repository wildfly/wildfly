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
        // Seed the id before register() publishes the meter, so a scrape racing registration can still deny it;
        // then replace it with the authoritative registered id (see addGauge for the same rationale).
        Meter.Id id = newMeterId(metadata, Meter.Type.COUNTER);
        metric.setMeterId(id);
        Meter.Id registeredId = FunctionCounter.builder(metadata.getMetricName(), metric,
                        value -> getMetricValue(metric, metadata.getMeasurementUnit()))
                .tags(getTags(metadata))
                .baseUnit(getBaseUnit(metadata))
                .description(metadata.getDescription())
                .register((MeterRegistry) this)
                .getId();
        metric.setMeterId(registeredId);
        return registeredId;
    }

    private Meter.Id addGauge(WildFlyMetric metric, MetricMetadata metadata) {
        // Seed the id before register() publishes the meter, so a scrape racing registration can still deny it;
        // then replace it with the authoritative registered id.
        Meter.Id id = newMeterId(metadata, Meter.Type.GAUGE);
        metric.setMeterId(id);
        Meter.Id registeredId = Gauge.builder(metadata.getMetricName(), metric,
                        value -> getMetricValue(metric, metadata.getMeasurementUnit()))
                .tags(getTags(metadata))
                .baseUnit(getBaseUnit(metadata))
                .description(metadata.getDescription())
                .register((MeterRegistry) this)
                .getId();
        metric.setMeterId(registeredId);
        return registeredId;
    }

    /**
     * Creates a meter identity before registration publishes it, so a concurrent scrape can hide denied series.
     *
     * @param metadata metadata describing the meter
     * @param type the Micrometer meter type
     * @return the identity used by the meter builder
     */
    private Meter.Id newMeterId(MetricMetadata metadata, Meter.Type type) {
        return new Meter.Id(metadata.getMetricName(), getTags(metadata), getBaseUnit(metadata), metadata.getDescription(), type);
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
