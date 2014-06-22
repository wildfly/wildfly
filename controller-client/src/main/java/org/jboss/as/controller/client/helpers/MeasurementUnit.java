/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2011, Red Hat, Inc., and individual contributors
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

package org.jboss.as.controller.client.helpers;

import org.jboss.as.controller.client.logging.ControllerClientLogger;

/**
 * Metric data values can be in one of the following known units of measurement. These enum values should correspond to
 * the "units" attribute enumerated type values as defined in the plugin descriptor's &ltmetric> element.
 *
 * @author Joseph Marques
 * @author Brian Stansberry (c) 2011 Red Hat Inc.
 */
public enum MeasurementUnit {

    // Simple Metric Types - Absolute and Relative
    NONE("", Family.ABSOLUTE, Scale.ONE), //
    PERCENTAGE("%", Family.RELATIVE, Scale.HECTO), //

    // Absolute Sizes in Bytes (utilization)
    BYTES("B", Family.BYTES, Scale.ONE), //
    KILOBYTES("KB", Family.BYTES, Scale.KILO), //
    MEGABYTES("MB", Family.BYTES, Scale.MEGA), //
    GIGABYTES("GB", Family.BYTES, Scale.GIGA), //
    TERABYTES("TB", Family.BYTES, Scale.TERA), //
    PETABYTES("PB", Family.BYTES, Scale.PETA), //

    // Absolute Sizes in Bits (throughput)
    BITS("b", Family.BITS, Scale.ONE), //
    KILOBITS("Kb", Family.BITS, Scale.KILO), //
    MEGABITS("Mb", Family.BITS, Scale.MEGA), //
    GIGABITS("Gb", Family.BITS, Scale.GIGA), //
    TERABITS("Tb", Family.BITS, Scale.TERA), //
    PETABITS("Pb", Family.BITS, Scale.PETA), //

    // Absolute Time - no display, only hints to the UI how to display
    EPOCH_MILLISECONDS("", Family.DURATION, Scale.MILLI), //
    EPOCH_SECONDS("", Family.DURATION, Scale.SEC), //

    // Relative Time
    JIFFYS("j", Family.TIME, Scale.JIFFY), //
    NANOSECONDS("ns", Family.TIME, Scale.NANO), //
    MICROSECONDS("us", Family.TIME, Scale.MICRO), //
    MILLISECONDS("ms", Family.TIME, Scale.MILLI), //
    SECONDS("s", Family.TIME, Scale.SEC), //
    MINUTES("m", Family.TIME, Scale.MIN), //
    HOURS("h", Family.TIME, Scale.HOUR), //
    DAYS("d", Family.TIME, Scale.DAY), //

    // Rate
    PER_JIFFY("/j", Family.RATE, Scale.PER_JIFFY), //
    PER_NANOSECOND("/ns", Family.RATE, Scale.PER_NANO), //
    PER_MICROSECOND("/us", Family.RATE, Scale.PER_MICRO), //
    PER_MILLISECOND("/ms", Family.RATE, Scale.PER_MILLI), //
    PER_SECOND("/s", Family.RATE, Scale.PER_SEC), //
    PER_MINUTE("/m", Family.RATE, Scale.PER_MIN), //
    PER_HOUR("/h", Family.RATE, Scale.PER_HOUR), //
    PER_DAY(" /d", Family.RATE, Scale.PER_DAY), //

    // Temperature
    CELSIUS("C", Family.TEMPERATURE, Scale.ONE), //
    KELVIN("K", Family.TEMPERATURE, Scale.ONE), //
    FAHRENHEIGHT("F", Family.TEMPERATURE, Scale.ONE);

    private String displayUnits;
    private Family family;
    private Scale scale;

    private MeasurementUnit(String displayUnits, Family family, Scale scale) {
        if (displayUnits.length() > 5) {
            throw ControllerClientLogger.ROOT_LOGGER.maxDisplayUnitLength();
        }

        this.displayUnits = displayUnits;
        this.family = family;
        this.scale = scale;
    }

    public static MeasurementUnit getUsingDisplayUnits(String displayUnits, MeasurementUnit.Family family) {
        for (MeasurementUnit units : values()) {
            if ((units.getFamily() == family) && units.toString().equalsIgnoreCase(displayUnits)) {
                return units;
            }
        }

        return null;
    }

    public MeasurementUnit getBaseUnits() {
        if (family == Family.BYTES) {
            return BYTES;
        } else if (family == Family.BITS) {
            return BITS;
        } else if (family == Family.TIME) {
            return SECONDS;
        } else if (family == Family.RATE) {
            return PER_SECOND;
        } else if (family == Family.TEMPERATURE) {
            return CELSIUS;
        } else if ((family == Family.ABSOLUTE) || (family == Family.DURATION) || (family == Family.RELATIVE)) {
            /*
             * Members of these families are their own base units
             */
            return this;
        }

        return null;
    }

    public boolean isComparableTo(MeasurementUnit other) {
        return family == other.family;
    }

    public static Double calculateOffset(MeasurementUnit first, MeasurementUnit second)
        throws MeasurementConversionException {
        if (first.isComparableTo(second) == false) {
            throw new MeasurementConversionException(ControllerClientLogger.ROOT_LOGGER.cannotConvert(first.name(), second.name()));
        }

        return Scale.calculateOffset(first.scale, second.scale);
    }

    public Family getFamily() {
        return family;
    }

    /**
     * A Java bean style getter to allow us to access the enum name from JSPs (e.g. ${measureUnits.name}).
     *
     * @return the enum name
     */
    public String getName() {
        return name();
    }

    @Override
    public String toString() {
        return this.displayUnits;
    }

    public enum Family {
        ABSOLUTE, BITS, BYTES, DURATION, RELATIVE, RATE, TIME, TEMPERATURE;
    }

    private enum Scale {
        // Binary based scaling factors
        CENTI(Type.SIZE), //
        ONE(Type.SIZE, CENTI, 100), //
        HECTO(Type.SIZE, ONE, 100), //
        KILO(Type.SIZE, ONE, 1024), //
        MEGA(Type.SIZE, KILO, 1024), //
        GIGA(Type.SIZE, MEGA, 1024), //
        TERA(Type.SIZE, GIGA, 1024), //
        PETA(Type.SIZE, TERA, 1024), //

        // Time based scaling factors
        JIFFY(Type.TIME), //
        NANO(Type.TIME, JIFFY, 1000), //
        MICRO(Type.TIME, NANO, 1000), //
        MILLI(Type.TIME, MICRO, 1000), //
        SEC(Type.TIME, MILLI, 1000), //
        MIN(Type.TIME, SEC, 60), //
        HOUR(Type.TIME, MIN, 60), //
        DAY(Type.TIME, HOUR, 24), //
        WEEK(Type.TIME, DAY, 7), //
        YEAR(Type.TIME, WEEK, 52),

        // Rate based scaling factors
        PER_JIFFY(Type.RATE), //
        PER_NANO(Type.RATE, PER_JIFFY, .001d), //
        PER_MICRO(Type.RATE, PER_NANO, .001d), //
        PER_MILLI(Type.RATE, PER_MICRO, .001d), //
        PER_SEC(Type.RATE, PER_MILLI, .001d), //
        PER_MIN(Type.RATE, PER_SEC, 1d/60d), //
        PER_HOUR(Type.RATE, PER_MIN, 1d/60d), //
        PER_DAY(Type.RATE, PER_HOUR, 1d/24d), //
        PER_WEEK(Type.RATE, PER_DAY, 1d/7d), //
        PER_YEAR(Type.RATE, PER_WEEK, 1d/52d);

        enum Type {
            NONE, SIZE, TIME, RATE;
        }

        private Type type;
        private Scale comparisonScale;
        private double offset;

        private Scale(Type type) {
            this.type = type;
        }

        private Scale(Type type, Scale comparisonScale, double offset) {
            this(type);

            this.comparisonScale = comparisonScale;
            this.offset = offset;
        }

        // don't expose this, force callers through MeasurementUnit.applyScale()
        /**
         * @param  value
         * @param  scale
         *
         * @return value scaled by the supplied Scale offset. 0.0 if scale is null.
         */
        static Double scaleUp(Double value, Scale scale) {
            if (null == scale) {
                return 0.0;
            }

            return value * scale.offset;
        }

        // don't expose this, force callers through MeasurementUnit.applyScale()
        /**
         * @param  value
         * @param  scale
         *
         * @return value scaled by the supplied Scale offset. 0.0 if scale is null.
         */
        static Double scaleDown(Double value, Scale scale) {
            if (null == scale) {
                return 0.0;
            }

            return value / scale.offset;
        }

        // don't expose this, force callers through MeasurementUnit.getScaleOffset()
        static Double calculateOffset(Scale first, Scale second) {
            if (first.type != second.type) {
                /*
                 * special return value meaning invalid comparison, though it was designed that this method would only
                 * be calling by the outer enumeration, which wouldn't do the necessary checks in advance. so this code
                 * path will rarely, if ever, be followed.
                 */
                return null;
            }

            if ((first.comparisonScale == null) && (second.comparisonScale == null)) {
                return 1.0;
            }

            Scale higher;
            Scale lower;
            if (first.comparisonScale == null) {
                higher = second;
                lower = first;
            } else if (second.comparisonScale == null) {
                higher = first;
                lower = second;
            } else {
                if (first.comparisonScale.ordinal() > second.comparisonScale.ordinal()) {
                    higher = first;
                    lower = second;
                } else if (first.comparisonScale.ordinal() < second.comparisonScale.ordinal()) {
                    higher = second;
                    lower = first;
                } else {
                    return 1.0;
                }
            }

            double results = 1.0;

            Scale movingScale = higher;
            while ((movingScale != lower) && (movingScale.comparisonScale != null)) {
                results *= movingScale.offset;
                movingScale = movingScale.comparisonScale;
            }

            if (first == lower) {
                return 1 / results; // return inverse
            }

            return results;
        }
    }

    public static class MeasurementConversionException extends RuntimeException {
        public MeasurementConversionException(String message) {
            super(message);
        }
    }
}
