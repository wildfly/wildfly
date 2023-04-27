/*
 * JBoss, Home of Professional Open Source.
 *
 * Copyright 2023 Red Hat, Inc., and individual contributors
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
package org.wildfly.extension.micrometer.registry;

import java.util.Arrays;
import java.util.OptionalDouble;
import java.util.stream.Collectors;

import org.jboss.as.controller.client.helpers.MeasurementUnit;
import org.wildfly.extension.micrometer.MicrometerExtensionLogger;
import org.wildfly.extension.micrometer.metrics.MetricMetadata;
import org.wildfly.extension.micrometer.metrics.WildFlyMetric;
import io.micrometer.core.instrument.FunctionCounter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.Meter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tag;
import io.micrometer.core.instrument.Tags;

public interface WildFlyRegistry {
    Meter remove(Meter.Id mappedId);

    default Meter.Id addMeter(WildFlyMetric metric, MetricMetadata metadata) {
        switch (metadata.getType()) {
            case GAUGE:
                return addGauge(metric, metadata);
            case COUNTER:
                return addCounter(metric, metadata);
            default:
                throw MicrometerExtensionLogger.MICROMETER_LOGGER.unsupportedMetricType(metadata.getType().name());
        }
    }

    default void close() {

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
        return "none".equalsIgnoreCase(measurementUnit) ? null : measurementUnit.toLowerCase();
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
