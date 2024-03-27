package org.wildfly.extension.micrometer.registry;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.OptionalDouble;
import java.util.stream.Collectors;

import io.micrometer.core.instrument.FunctionCounter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.Meter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tag;
import io.micrometer.core.instrument.Tags;
import io.micrometer.core.instrument.composite.CompositeMeterRegistry;
import org.jboss.as.controller.client.helpers.MeasurementUnit;
import org.wildfly.extension.micrometer.MicrometerExtensionLogger;
import org.wildfly.extension.micrometer.metrics.MetricMetadata;
import org.wildfly.extension.micrometer.metrics.WildFlyMetric;

public class WildFlyCompositeRegistry extends CompositeMeterRegistry implements WildFlyRegistry {
    private List<WildFlyRegistry> registries = new ArrayList<>();

    public WildFlyCompositeRegistry() {
        super();
    }

//    @Override
//    public Meter.Id addMeter(WildFlyMetric metric, MetricMetadata metadata) {
//        switch (metadata.getType()) {
//            case GAUGE:
//                return addGauge(metric, metadata);
//            case COUNTER:
//                return addCounter(metric, metadata);
//            default:
//                throw MicrometerExtensionLogger.MICROMETER_LOGGER.unsupportedMetricType(metadata.getType().name());
//        }
//    }

    private Meter.Id addCounter(WildFlyMetric metric, MetricMetadata metadata) {
        FunctionCounter.Builder<WildFlyMetric> builder = FunctionCounter.builder(metadata.getMetricName(), metric,
                        value -> getMetricValue(metric, metadata.getMeasurementUnit()))
                .tags(getTags(metadata))
                .baseUnit(getBaseUnit(metadata))
                .description(metadata.getDescription());
        Optional<FunctionCounter> first = this.getRegistries().stream().map(builder::register).findFirst();
        if (first.isPresent()) {
            return first.get().getId();
        } else {
            throw MicrometerExtensionLogger.MICROMETER_LOGGER.errorRegisteringMetric(metadata.getMetricName());
        }
    }

    private Meter.Id addGauge(WildFlyMetric metric, MetricMetadata metadata) {
        Gauge.Builder<WildFlyMetric> builder = Gauge.builder(metadata.getMetricName(), metric,
                        value -> getMetricValue(metric, metadata.getMeasurementUnit()))
                .tags(getTags(metadata))
                .baseUnit(getBaseUnit(metadata))
                .description(metadata.getDescription());
        Optional<Gauge> first = this.getRegistries().stream().map(builder::register).findFirst();
        if (first.isPresent()) {
            return first.get().getId();
        } else {
            throw MicrometerExtensionLogger.MICROMETER_LOGGER.errorRegisteringMetric(metadata.getMetricName());
        }
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

    public void addRegistry(WildFlyRegistry registry) {
        this.add((MeterRegistry) registry);
        this.registries.add(registry);
    }
}
