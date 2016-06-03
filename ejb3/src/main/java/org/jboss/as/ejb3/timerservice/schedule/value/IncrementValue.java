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
 * Represents a value for a {@link ScheduleExpression} which is expressed as an increment type. An
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
     * The "x" value in the x&#47;y expression
     */
    private String start;

    /**
     * The "y" value in the x&#47;y expression
     */
    private String interval;

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
            throw EjbLogger.EJB3_TIMER_LOGGER.invalidIncrementValue(value);
        }

        this.start = tokenizer.nextToken().trim();
        this.interval = tokenizer.nextToken().trim();
    }

    /**
     * Returns the start of this {@link IncrementValue}
     *
     * @return
     */
    public String getStart() {
        return this.start;
    }

    /**
     * Returns the interval of this {@link IncrementValue}
     *
     * @return
     */
    public String getInterval() {
        return this.interval;
    }
}
