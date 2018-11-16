package org.wildfly.extension.microprofile.metrics;

import org.jboss.as.controller.client.helpers.MeasurementUnit;

public class UnitConverter {

    static double scaleToBase(double initialValue, MeasurementUnit unit) {
        if (unit == null) {
            return initialValue;
        }

        final double ratio;
        switch (unit) {
            case BITS:
                ratio = 1 / 8;
                break;
            case KILOBITS:
                ratio = 1_000 / 8;
                break;
            case MEGABITS:
                ratio = 1_000_000 / 8;
                break;
            case GIGABITS:
                ratio = 1_000_000_000 / 8;
                break;
            case TERABITS:
                ratio = 1_000_000_000_000L / 8;
                break;
            case PETABITS:
                ratio = 1_000_000_000_000_000L / 8;
                break;
            case KILOBYTES:
                ratio = 1_000;
                break;
            case MEGABYTES:
                ratio = 1_000_000;
                break;
            case GIGABYTES:
                ratio = 1_000_000_000;
                break;
            case TERABYTES:
                ratio = 1_000_000_000_000L;
                break;
            case PETABYTES:
                ratio = 1_000_000_000_000_000L;
                break;
            case NANOSECONDS:
                ratio = 0.000_000_001;
                break;
            case MICROSECONDS:
                ratio = 0.000_001;
                break;
            case MILLISECONDS:
            case EPOCH_MILLISECONDS:
                ratio = 0.001;
                break;
            case MINUTES:
                ratio = 60;
                break;
            case HOURS:
                ratio = 3600;
                break;
            case DAYS:
                ratio = 24 * 3600;
                break;
            default:
                ratio = 1.0;
        }

        return initialValue * ratio;
    }

    static String unitSuffix(MeasurementUnit unit) {
        if (unit == null) {
            return "";
        }
        switch (unit) {
            case BYTES:
            case KILOBYTES:
            case MEGABYTES:
            case GIGABYTES:
            case TERABYTES:
            case PETABYTES:
            case BITS:
            case KILOBITS:
            case MEGABITS:
            case GIGABITS:
            case TERABITS:
            case PETABITS:
                return "_bytes";
            case EPOCH_MILLISECONDS:
            case EPOCH_SECONDS:
            case NANOSECONDS:
            case MICROSECONDS:
            case MILLISECONDS:
            case SECONDS:
            case MINUTES:
            case HOURS:
            case DAYS:
                return "_seconds";
            default:
                return "";
        }
    }
}
