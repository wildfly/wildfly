/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2009, Red Hat Middleware LLC, and individual contributors
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
package org.jboss.as.ejb3.timerservice.schedule.value;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.jboss.as.ejb3.logging.EjbLogger;

/**
 * Represents a value for a {@link javax.ejb.ScheduleExpression} which is expressed as a range type. An
 * {@link RangeValue} comprises of a start and an end value for the range, separated by a "-"
 * <p/>
 * <p>
 * Each side of the range must be an individual attribute value. Members of a range <b>cannot</b> themselves
 * be lists, wild-cards, ranges, or increments. In range ”x-y”, if x is larger than y, the range is equivalent
 * to “x-max, min-y”, where max is the largest value of the corresponding attribute and min is the
 * smallest. The range “x-x”, where both range values are the same, evaluates to the single value x.
 * </p>
 *
 * @author Jaikiran Pai
 * @version $Revision: $
 * @see ScheduleExpressionType#RANGE
 */
public class RangeValue implements ScheduleValue {
    /**
     * The separator which is used for parsing a {@link String} which
     * represents a {@link RangeValue}
     */
    public static final String RANGE_SEPARATOR = "-";

    private static final Pattern RANGE_PATTERN;

    static {
        final String POSITIVE_OR_NEGATIVE_INTEGER = "\\s*-?\\s*\\d+\\s*";
        final String WORD = "\\s*([1-5][a-zA-Z]{2})?\\s*[a-zA-Z]+\\s*[a-zA-Z]*\\s*";
        final String OR = "|";
        final String OPEN_GROUP = "(";
        final String CLOSE_GROUP = ")";

        String rangeRegex = OPEN_GROUP + POSITIVE_OR_NEGATIVE_INTEGER + OR + WORD + CLOSE_GROUP + RANGE_SEPARATOR
                + OPEN_GROUP + POSITIVE_OR_NEGATIVE_INTEGER + OR + WORD + CLOSE_GROUP;

        RANGE_PATTERN = Pattern.compile(rangeRegex);

    }

    /**
     * The start value of the range
     */
    private String rangeStart;

    /**
     * The end value of the range
     */
    private String rangeEnd;

    /**
     * Creates a {@link RangeValue} by parsing the passed <code>value</code>.
     * <p>
     * Upon successfully parsing the passed <code>value</code>, this constructor
     * sets the start and the end value of this {@link RangeValue}
     * </p>
     *
     * @param range The value to be parsed
     * @throws IllegalArgumentException If the passed <code>value</code> cannot be
     *                                  represented as an {@link RangeValue}
     */
    public RangeValue(String range) {
        String[] values = getRangeValues(range);
        if (values == null || values.length != 2) {
            throw EjbLogger.EJB3_TIMER_LOGGER.invalidRange(range);
        }

        this.rangeStart = values[0].trim();
        this.rangeEnd = values[1].trim();
    }

    /**
     * Returns the start value of this {@link RangeValue}
     *
     * @return
     */
    public String getStart() {
        return this.rangeStart;
    }

    /**
     * Returns the end value of this {@link RangeValue}
     *
     * @return
     */
    public String getEnd() {
        return this.rangeEnd;
    }

    public static boolean accepts(String value) {
        if (value == null) {
            return false;
        }
        Matcher matcher = RANGE_PATTERN.matcher(value);
        return matcher.matches();
    }

    private String[] getRangeValues(String val) {
        if (val == null) {
            return null;
        }
        Matcher matcher = RANGE_PATTERN.matcher(val);
        if (!matcher.matches()) {
            return null;
        }
        String[] rangeVals = new String[2];
        rangeVals[0] = matcher.group(1);
        rangeVals[1] = matcher.group(3);

        return rangeVals;
    }

    public String asString() {
        return this.rangeStart + RANGE_SEPARATOR + this.rangeStart;
    }
}
