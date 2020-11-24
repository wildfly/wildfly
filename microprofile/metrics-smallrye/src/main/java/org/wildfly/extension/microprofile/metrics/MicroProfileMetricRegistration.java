package org.wildfly.extension.microprofile.metrics;

import static org.wildfly.extension.metrics.api.MetricMetadata.Type.COUNTER;

import java.util.Map;
import java.util.Optional;
import java.util.OptionalDouble;

import io.smallrye.metrics.ExtendedMetadata;
import io.smallrye.metrics.MetricRegistries;
import org.eclipse.microprofile.metrics.Counter;
import org.eclipse.microprofile.metrics.Gauge;
import org.eclipse.microprofile.metrics.Metadata;
import org.eclipse.microprofile.metrics.Metric;
import org.eclipse.microprofile.metrics.MetricID;
import org.eclipse.microprofile.metrics.MetricRegistry;
import org.eclipse.microprofile.metrics.MetricType;
import org.eclipse.microprofile.metrics.MetricUnits;
import org.eclipse.microprofile.metrics.Tag;
import org.jboss.as.controller.client.helpers.MeasurementUnit;
import org.wildfly.extension.metrics.api.MetricRegistration;
import org.wildfly.extension.metrics.api.WildFlyMetric;
import org.wildfly.extension.metrics.api.WildFlyMetricMetadata;

public class MicroProfileMetricRegistration extends MetricRegistration {

    @Override
    public void registerMetric(WildFlyMetricMetadata metricMetadata, WildFlyMetric wildFlyMetric) {
        final Metric metric;
        if (metricMetadata.getType() == COUNTER) {
            metric = new Counter() {
                @Override
                public void inc() {
                }

                @Override
                public void inc(long n) {
                }

                @Override
                public long getCount() {
                    OptionalDouble value = wildFlyMetric.getValue();
                    return  Double.valueOf(value.orElse(0)).longValue();
                }
            };
        } else {
            metric = new Gauge<Number>() {
                @Override
                public Double getValue() {
                    OptionalDouble value = wildFlyMetric.getValue();
                    return  value.orElse(0);
                }
            };
        }
        final Metadata metadata;
        MetricRegistry vendorRegistry = MetricRegistries.get(MetricRegistry.Type.VENDOR);
        synchronized (vendorRegistry) {
            Metadata existingMetadata = vendorRegistry.getMetadata().get(metricMetadata.getMetricName());
            if (existingMetadata != null) {
                metadata = existingMetadata;
            } else {
                metadata = new ExtendedMetadata(metricMetadata.getMetricName(), metricMetadata.getMetricName(), metricMetadata.getDescription(),
                        metricMetadata.getType() == COUNTER ? MetricType.COUNTER : MetricType.GAUGE, metricUnit(metricMetadata.getMeasurementUnit()),
                        null, false,
                        // for WildFly subsystem metrics, the microprofile scope is put in the OpenMetrics tags
                        // so that the name of the metric does not change ("vendor_" will not be prepended to it).
                        Optional.of(false));
            }
            MetricID mpMetricID = toMicroProfileMetricID(metricMetadata.getMetricID());
            vendorRegistry.register(metadata, metric, mpMetricID.getTagsAsArray());
        }
    }

    @Override
    public void unregister() {
        MetricRegistry registry = MetricRegistries.get(MetricRegistry.Type.VENDOR);
        for (org.wildfly.extension.metrics.api.MetricID metricID : unregistrationTasks) {
            registry.remove(toMicroProfileMetricID(metricID));
        }
    }

    private static MetricID toMicroProfileMetricID(org.wildfly.extension.metrics.api.MetricID wildflyMetricID) {
        Tag[] tags;
        if (wildflyMetricID.getTags().isEmpty()) {
            tags = new Tag[0];
        } else {
            tags = new Tag[wildflyMetricID.getTags().size()];
            int i = 0;
            for (Map.Entry<String, String> wildFlyMetricTag : wildflyMetricID.getTags().entrySet()) {
                tags[i] = new Tag(wildFlyMetricTag.getKey(), wildFlyMetricTag.getValue());
                i++;
            }
        }
        return new MetricID(wildflyMetricID.getName(), tags);
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
