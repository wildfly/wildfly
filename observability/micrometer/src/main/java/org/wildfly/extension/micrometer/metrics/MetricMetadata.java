/*
 * JBoss, Home of Professional Open Source.
 *
 * Copyright 2022 Red Hat, Inc., and individual contributors
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

package org.wildfly.extension.micrometer.metrics;

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
            return this.name().toLowerCase();
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
