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

import org.jboss.as.ejb3.logging.EjbLogger;
import org.jboss.as.ejb3.timerservice.schedule.value.ScheduleExpressionType;

/**
 * Represents the value of a second constructed out of a {@link javax.ejb.ScheduleExpression#getSecond()}
 * <p/>
 * <p>
 * A {@link Second} can hold only {@link Integer} as its value. The only exception to this being the wildcard (*)
 * value. The various ways in which a
 * {@link Second} value can be represented are:
 * <ul>
 * <li>Wildcard. For example, second = "*"</li>
 * <li>Range. For example, second = "0-34"</li>
 * <li>List. For example, second = "15, 20, 59"</li>
 * <li>Single value. For example, second = "12"</li>
 * <li>Increment. For example, second = "* &#47; 5"</li>
 * </ul>
 * </p>
 *
 * @author Jaikiran Pai
 * @version $Revision: $
 */
public class Second extends IntegerBasedExpression {

    /**
     * The maximum allowed value for a second
     */
    public static final Integer MAX_SECOND = 59;

    /**
     * Minimum allowed value for a second
     */
    public static final Integer MIN_SECOND = 0;

    /**
     * Creates a {@link Second} by parsing the passed {@link String} <code>value</code>
     * <p>
     * Valid values are of type {@link org.jboss.as.ejb3.timerservice.schedule.value.ScheduleExpressionType#WILDCARD}, {@link org.jboss.as.ejb3.timerservice.schedule.value.ScheduleExpressionType#RANGE},
     * {@link org.jboss.as.ejb3.timerservice.schedule.value.ScheduleExpressionType#LIST} {@link org.jboss.as.ejb3.timerservice.schedule.value.ScheduleExpressionType#INCREMENT} or
     * {@link org.jboss.as.ejb3.timerservice.schedule.value.ScheduleExpressionType#SINGLE_VALUE}
     * </p>
     *
     * @param value The value to be parsed
     * @throws IllegalArgumentException If the passed <code>value</code> is neither a {@link org.jboss.as.ejb3.timerservice.schedule.value.ScheduleExpressionType#WILDCARD},
     *                                  {@link org.jboss.as.ejb3.timerservice.schedule.value.ScheduleExpressionType#RANGE}, {@link org.jboss.as.ejb3.timerservice.schedule.value.ScheduleExpressionType#LIST},
     *                                  {@link org.jboss.as.ejb3.timerservice.schedule.value.ScheduleExpressionType#INCREMENT} nor {@link org.jboss.as.ejb3.timerservice.schedule.value.ScheduleExpressionType#SINGLE_VALUE}.
     */
    public Second(String value) {
        super(value);
    }

    public Integer getNextMatch(int currentSecond) {
        if (this.scheduleExpressionType == ScheduleExpressionType.WILDCARD) {
            return currentSecond;
        }
        if (this.absoluteValues.isEmpty()) {
            return null;
        }
        for (Integer second : this.absoluteValues) {
            if (currentSecond == second) {
                return currentSecond;
            }
            if (second > currentSecond) {
                return second;
            }
        }
        return this.absoluteValues.first();
    }

    public int getFirst() {
        if (this.scheduleExpressionType == ScheduleExpressionType.WILDCARD) {
            return 0;
        }
        if (this.absoluteValues.isEmpty()) {
            throw EjbLogger.EJB3_TIMER_LOGGER.invalidExpressionSeconds(this.origValue);
        }
        return this.absoluteValues.first();
    }

    /**
     * Returns the maximum allowed value for a {@link Second}
     *
     * @see Second#MAX_SECOND
     */
    @Override
    protected Integer getMaxValue() {
        return MAX_SECOND;
    }

    /**
     * Returns the minimum allowed value for a {@link Second}
     *
     * @see Second#MIN_SECOND
     */
    @Override
    protected Integer getMinValue() {
        return MIN_SECOND;
    }

    @Override
    public boolean isRelativeValue(String value) {
        // seconds do not support relative values, so always return false
        return false;
    }

    @Override
    protected boolean accepts(ScheduleExpressionType scheduleExprType) {
        switch (scheduleExprType) {
            case RANGE:
            case LIST:
            case SINGLE_VALUE:
            case WILDCARD:
            case INCREMENT:
                return true;
            default:
                return false;
        }
    }

}
