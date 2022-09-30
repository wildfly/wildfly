package org.wildfly.extension.micrometer.metrics;

import java.util.Arrays;
import java.util.List;
import java.util.OptionalDouble;
import java.util.stream.Collectors;

import io.micrometer.core.instrument.Meter;
import io.micrometer.core.instrument.Tag;
import io.micrometer.core.instrument.Tags;
import io.micrometer.prometheus.PrometheusConfig;
import io.micrometer.prometheus.PrometheusMeterRegistry;
import org.jboss.as.controller.client.helpers.MeasurementUnit;
import org.wildfly.extension.micrometer.MicrometerExtensionLogger;

public class WildFlyRegistry extends PrometheusMeterRegistry {
    public WildFlyRegistry() {
        super(PrometheusConfig.DEFAULT);
        this.throwExceptionOnRegistrationFailure();
    }

    public Meter addMeter(WildFlyMetric metric, MetricMetadata metadata) {
        switch (metadata.getType()) {
            case GAUGE:
                return addGauge(metric, metadata);
            case COUNTER:
                return addCounter(metric, metadata);
            default:
                throw MicrometerExtensionLogger.MICROMETER_LOGGER.unsupportedMetricType(metadata.getType().name());
        }
    }

    private Meter addCounter(WildFlyMetric metric, MetricMetadata metadata) {
        MeasurementUnit measurementUnit = metadata.getMeasurementUnit();
        return newFunctionCounter(generateMetricId(metadata, Meter.Type.COUNTER), metric,
                value -> getMetricValue(metric, measurementUnit));
    }

    private Meter addGauge(WildFlyMetric metric, MetricMetadata metadata) {
        MeasurementUnit measurementUnit = metadata.getMeasurementUnit();
        return newGauge(generateMetricId(metadata, Meter.Type.GAUGE),
                metric, value -> getMetricValue(metric, measurementUnit));
    }

    private Meter.Id generateMetricId(MetricMetadata metadata, Meter.Type gauge) {
        return new Meter.Id(metadata.getMetricName(),
                Tags.of(getTags(metadata)),
                getBaseUnit(metadata),
                metadata.getDescription(),
                gauge);
    }

    private double getMetricValue(WildFlyMetric metric, MeasurementUnit unit) {
        OptionalDouble metricValue = metric.getValue();
        return metricValue.isPresent() ?
                scaleToBaseUnit(metricValue.getAsDouble(), unit) :
                0.0;
    }

    private List<Tag> getTags(MetricMetadata metadata) {
        return Arrays.stream(metadata.getTags())
                .map(t -> Tag.of(t.getKey(), t.getValue()))
                .collect(Collectors.toList());
    }

    private String getBaseUnit(MetricMetadata metadata) {
        String measurementUnit = metadata.getBaseMetricUnit();
        return "none".equalsIgnoreCase(measurementUnit) ? null : measurementUnit.toLowerCase();
    }

    private double scaleToBaseUnit(double value, MeasurementUnit unit) {
        return value * MeasurementUnit.calculateOffset(unit, unit.getBaseUnits());
    }
}
