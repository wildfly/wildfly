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
package org.jboss.as.ejb3.timerservice.schedule.attribute;

import org.jboss.as.ejb3.timerservice.schedule.value.ScheduleExpressionType;

import java.util.Calendar;

/**
 * Represents in the year value part constructed out of a {@link javax.ejb.ScheduleExpression#getYear()}
 * <p/>
 * <p>
 * A {@link Year} can hold only {@link Integer} as its value. The only exception to this being the wildcard (*)
 * value. The various ways in which a
 * {@link Year} value can be represented are:
 * <ul>
 * <li>Wildcard. For example, year = "*"</li>
 * <li>Range. For example, year = "2009-2011"</li>
 * <li>List. For example, year = "2008, 2010, 2011"</li>
 * <li>Single value. For example, year = "2009"</li>
 * </ul>
 * </p>
 *
 * @author Jaikiran Pai
 * @version $Revision: $
 */
public class Year extends IntegerBasedExpression {

    // The EJB3. timer service spec says that the year
    // value can be any 4 digit value.
    // Hence the max value 9999
    public static final Integer MAX_YEAR = 9999;

    // TODO: think about this min value. The EJB3.1 timerservice spec
    // says, that the year value can be any 4 digit value.
    // That's the reason we have set it to 1000 here.
    public static final Integer MIN_YEAR = 1000;


    /**
     * Creates a {@link Year} by parsing the passed {@link String} <code>value</code>
     * <p>
     * Valid values are of type {@link org.jboss.as.ejb3.timerservice.schedule.value.ScheduleExpressionType#WILDCARD}, {@link org.jboss.as.ejb3.timerservice.schedule.value.ScheduleExpressionType#RANGE},
     * {@link org.jboss.as.ejb3.timerservice.schedule.value.ScheduleExpressionType#LIST} or {@link org.jboss.as.ejb3.timerservice.schedule.value.ScheduleExpressionType#SINGLE_VALUE}
     * </p>
     *
     * @param value The value to be parsed
     * @throws IllegalArgumentException If the passed <code>value</code> is neither a {@link org.jboss.as.ejb3.timerservice.schedule.value.ScheduleExpressionType#WILDCARD},
     *                                  {@link org.jboss.as.ejb3.timerservice.schedule.value.ScheduleExpressionType#RANGE}, {@link org.jboss.as.ejb3.timerservice.schedule.value.ScheduleExpressionType#LIST}
     *                                  nor {@link org.jboss.as.ejb3.timerservice.schedule.value.ScheduleExpressionType#SINGLE_VALUE}
     */
    public Year(String value) {
        super(value);
    }

    /**
     * Returns the maximum possible value for a {@link Year}
     *
     * @see Year#MAX_YEAR
     */
    @Override
    protected Integer getMaxValue() {
        return MAX_YEAR;
    }

    /**
     * Returns the minimum possible value for a {@link Year}
     *
     * @see Year#MIN_YEAR
     */
    @Override
    protected Integer getMinValue() {
        return MIN_YEAR;
    }

    @Override
    public boolean isRelativeValue(String value) {
        // year doesn't support relative values, so always return false
        return false;
    }

    @Override
    protected boolean accepts(ScheduleExpressionType scheduleExprType) {
        switch (scheduleExprType) {
            case RANGE:
            case LIST:
            case SINGLE_VALUE:
            case WILDCARD:
                return true;
            // year doesn't support increment
            case INCREMENT:
            default:
                return false;
        }
    }

    public Integer getNextMatch(Calendar currentCal) {
        if (this.scheduleExpressionType == ScheduleExpressionType.WILDCARD) {
            return currentCal.get(Calendar.YEAR);
        }
        if (this.absoluteValues.isEmpty()) {
            return null;
        }
        int currentYear = currentCal.get(Calendar.YEAR);
        for (Integer year : this.absoluteValues) {
            if (currentYear == year) {
                return currentYear;
            }
            if (year > currentYear) {
                return year;
            }
        }
        return this.absoluteValues.first();
    }
}
