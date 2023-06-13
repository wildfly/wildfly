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

import java.util.StringTokenizer;

import org.jboss.as.ejb3.logging.EjbLogger;

/**
 * Represents a value for a {@code ScheduleExpression} which is expressed as an increment type. An
 * {@link IncrementValue} comprises of a start value and an interval, separated by a "/"
 * <p/>
 * <p>
 * An {@link IncrementValue} is specified in the form of x&#47;y to mean "Every N { seconds | minutes | hours }
 * within the { minute | hour | day }" (respectively).  For expression x/y, the attribute is constrained to
 * every yth value within the set of allowable values beginning at time x. The x value is inclusive.
 * The wildcard character (*) can be used in the x position, and is equivalent to 0.
 * </p>
 *
 * @author Jaikiran Pai
 * @version $Revision: $
 * @see ScheduleExpressionType#INCREMENT
 */
public class IncrementValue implements ScheduleValue {

    /**
     * The separator which is used for parsing a {@link String} which
     * represents a {@link IncrementValue}
     */
    public static final String INCREMENT_SEPARATOR = "/";

    /**
     * The "x" value as {@code int} in the x&#47;y expression
     */
    private final int start;

    /**
     * The "y" value as {@code int} in the x&#47;y expression
     */
    private final int interval;

    /**
     * Creates a {@link IncrementValue} by parsing the passed <code>value</code>.
     * <p>
     * Upon successfully parsing the passed <code>value</code>, this constructor
     * sets the start value and the interval value of this {@link IncrementValue}
     * </p>
     *
     * @param value The value to be parsed
     * @throws IllegalArgumentException If the passed <code>value</code> cannot be
     *                                  represented as an {@link IncrementValue}
     */
    public IncrementValue(String value) {
        StringTokenizer tokenizer = new StringTokenizer(value, INCREMENT_SEPARATOR);
        int numberOfTokens = tokenizer.countTokens();
        if (numberOfTokens != 2) {
            throw EjbLogger.EJB3_TIMER_LOGGER.invalidScheduleValue(ScheduleExpressionType.INCREMENT.name(), value);
        }

        String startVal = tokenizer.nextToken().trim();
        String intervalVal = tokenizer.nextToken().trim();

        try {
            this.start = "*".equals(startVal) ? 0 : Integer.parseInt(startVal);
            this.interval = Integer.parseInt(intervalVal);
        } catch (NumberFormatException e) {
            throw EjbLogger.EJB3_TIMER_LOGGER.invalidScheduleValue(ScheduleExpressionType.INCREMENT.name(), value);
        }

        // start will be validated by the target timer attribute classes
        // (Hour, Minute, and Second) accordingly.
        // check for invalid interval values here.
        // Note that 0 interval is valid, making this increment value behave as if it's a single value.
        if (this.interval < 0) {
            throw EjbLogger.EJB3_TIMER_LOGGER.invalidScheduleValue(ScheduleExpressionType.INCREMENT.name(), value);
        }
    }

    /**
     * Returns the start of this {@link IncrementValue}
     *
     * @return int start value
     */
    public int getStart() {
        return this.start;
    }

    /**
     * Returns the interval of this {@link IncrementValue}
     *
     * @return int interval value
     */
    public int getInterval() {
        return this.interval;
    }
}
