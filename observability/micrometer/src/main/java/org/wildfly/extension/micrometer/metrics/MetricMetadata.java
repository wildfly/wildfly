/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.extension.micrometer.metrics;

import java.util.Locale;

import org.jboss.as.controller.client.helpers.MeasurementUnit;

public interface MetricMetadata {
    String getMetricName();

    MetricTag[] getTags();

    String getDescription();

    MeasurementUnit getMeasurementUnit();

    Type getType();

    MetricID getMetricID();

    default String getBaseMetricUnit() {
        return baseMetricUnit(getMeasurementUnit());
    }

    static String baseMetricUnit(MeasurementUnit unit) {
        if (unit == null) {
            return "none";
        }
        switch (unit.getBaseUnits()) {
            case PERCENTAGE:
                return "percent";
            case BYTES:
            case KILOBYTES:
            case MEGABYTES:
            case GIGABYTES:
            case TERABYTES:
            case PETABYTES:
                return "bytes";
            case BITS:
            case KILOBITS:
            case MEGABITS:
            case GIGABITS:
            case TERABITS:
            case PETABITS:
                return "bits";
            case EPOCH_MILLISECONDS:
            case EPOCH_SECONDS:
            case NANOSECONDS:
            case MILLISECONDS:
            case MICROSECONDS:
            case SECONDS:
            case MINUTES:
            case HOURS:
            case DAYS:
                return "seconds";
            case JIFFYS:
                return "jiffys";
            case PER_JIFFY:
                return "per-jiffy";
            case PER_NANOSECOND:
            case PER_MICROSECOND:
            case PER_MILLISECOND:
            case PER_SECOND:
            case PER_MINUTE:
            case PER_HOUR:
            case PER_DAY:
                return "per_second";
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

    enum Type {
        COUNTER,
        GAUGE;

        @Override
        public String toString() {
            return this.name().toLowerCase(Locale.ENGLISH);
        }
    }

    class MetricTag {
        private String key;
        private String value;

        public MetricTag() {
        }

        public MetricTag(String key, String value) {
            this.key = key;
            this.value = value;
        }

        public String getKey() {
            return key;
        }

        public String getValue() {
            return value;
        }

        public void setKey(String key) {
            this.key = key;
        }

        public void setValue(String value) {
            this.value = value;
        }

        @Override
        public String toString() {
            return "MetricTag{" +
                    "key='" + key + '\'' +
                    ", value='" + value + '\'' +
                    '}';
        }
    }
}
