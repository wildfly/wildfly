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
import org.jboss.as.ejb3.timerservice.schedule.ScheduleExpressionTypeUtil;
import org.jboss.as.ejb3.timerservice.schedule.value.IncrementValue;
import org.jboss.as.ejb3.timerservice.schedule.value.ListValue;
import org.jboss.as.ejb3.timerservice.schedule.value.RangeValue;
import org.jboss.as.ejb3.timerservice.schedule.value.ScheduleExpressionType;
import org.jboss.as.ejb3.timerservice.schedule.value.ScheduleValue;
import org.jboss.as.ejb3.timerservice.schedule.value.SingleValue;

import java.util.HashSet;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

/**
 * Represents a {@link Integer} type value in a {@link javax.ejb.ScheduleExpression}.
 * <p/>
 * <p>
 * Examples for {@link IntegerBasedExpression} are the value of seconds, years, months etc...
 * which allow {@link Integer}.
 * </p>
 *
 * @author Jaikiran Pai
 * @version $Revision: $
 */
public abstract class IntegerBasedExpression {

    protected abstract Integer getMaxValue();

    protected abstract Integer getMinValue();

    protected abstract boolean accepts(ScheduleExpressionType scheduleExprType);

    protected final SortedSet<Integer> absoluteValues = new TreeSet<Integer>();

    protected final Set<ScheduleValue> relativeValues = new HashSet<ScheduleValue>();

    protected final ScheduleExpressionType scheduleExpressionType;

    protected final String origValue;

    public IntegerBasedExpression(String value) {
        this.origValue = value;
        // check the type of value
        this.scheduleExpressionType = ScheduleExpressionTypeUtil.getType(value);
        if (this.accepts(scheduleExpressionType) == false) {
            throw EjbLogger.EJB3_TIMER_LOGGER.invalidScheduleExpressionType(value, this.getClass().getName(), this.scheduleExpressionType.toString());
        }
        switch (this.scheduleExpressionType) {
            case RANGE:
                RangeValue range = new RangeValue(value);
                // process the range value
                this.processRangeValue(range);
                break;

            case LIST:
                ListValue list = new ListValue(value);
                // process the list value
                this.processListValue(list);
                break;

            case INCREMENT:
                IncrementValue incrValue = new IncrementValue(value);
                // process the increment value
                this.processIncrement(incrValue);
                break;

            case SINGLE_VALUE:
                SingleValue singleValue = new SingleValue(value);
                // process the single value
                this.processSingleValue(singleValue);
                break;

            case WILDCARD:
                // a wildcard is equivalent to "all possible" values, so
                // do nothing
                break;

            default:
                throw EjbLogger.EJB3_TIMER_LOGGER.invalidValueForSecondInScheduleExpression(value);
        }
    }

    protected void processListValue(ListValue list) {
        for (String listItem : list.getValues()) {
            this.processListItem(listItem);
        }
    }

    protected void processListItem(String listItem) {
        // check what type of a value the list item is.
        // Each item in the list must be an individual attribute value or a range.
        // List items can not themselves be lists, wild-cards, or increments.
        ScheduleExpressionType listItemType = ScheduleExpressionTypeUtil.getType(listItem);
        switch (listItemType) {
            case SINGLE_VALUE:
                SingleValue singleVal = new SingleValue(listItem);
                this.processSingleValue(singleVal);
                return;
            case RANGE:
                RangeValue range = new RangeValue(listItem);
                this.processRangeValue(range);
                return;
            default:
                throw EjbLogger.EJB3_TIMER_LOGGER.invalidListValue(listItem);
        }
    }

    protected void processRangeValue(RangeValue range) {
        String start = range.getStart();
        String end = range.getEnd();
        if (this.isRelativeValue(start) || this.isRelativeValue(end)) {
            this.relativeValues.add(range);
            return;
        }
        Integer rangeStart = this.parseInt(start);
        Integer rangeEnd = this.parseInt(end);

        // validations
        this.assertValid(rangeStart);
        this.assertValid(rangeEnd);

        // start and end are both the same. So it's just a single value
        if (rangeStart.equals(rangeEnd)) {
            this.absoluteValues.add(rangeStart);
            return;

        }
        if (rangeStart > rangeEnd) {
            // In range "x-y", if x is larger than y, the range is equivalent to
            // "x-max, min-y", where max is the largest value of the corresponding attribute
            // and min is the smallest.
            for (int i = rangeStart; i <= this.getMaxValue(); i++) {
                this.absoluteValues.add(i);
            }
            for (int i = this.getMinValue(); i <= rangeEnd; i++) {
                this.absoluteValues.add(i);
            }
        } else {
            // just keep adding from range start to range end (both inclusive).
            for (int i = rangeStart; i <= rangeEnd; i++) {
                this.absoluteValues.add(i);
            }
        }
    }

    protected void processIncrement(IncrementValue incr) {
        String startValue = incr.getStart();
        Integer start = startValue.equals("*") ? 0 : this.parseInt(startValue);
        // make sure it's a valid value
        this.assertValid(start);
        Integer interval = this.parseInt(incr.getInterval());
        this.absoluteValues.add(start);
        int next = start + interval;
        int maxValue = this.getMaxValue();
        while (next <= maxValue) {
            this.absoluteValues.add(next);
            next = next + interval;
        }
    }

    protected void processSingleValue(SingleValue singleValue) {
        String value = singleValue.getValue();
        if (this.isRelativeValue(value)) {
            this.relativeValues.add(singleValue);
        } else {
            Integer val = this.parseInt(value);
            this.assertValid(val);
            this.absoluteValues.add(val);
        }
    }

    protected Integer parseInt(String alias) {
        if (alias == null) {
            return null;
        }
        return Integer.parseInt(alias.trim());
    }

    protected void assertValid(Integer value) throws IllegalArgumentException {
        if (value == null) {
            throw EjbLogger.EJB3_TIMER_LOGGER.couldNotParseScheduleExpression(this.origValue);
        }
        int max = this.getMaxValue();
        int min = this.getMinValue();
        if (value > max || value < min) {
            throw EjbLogger.EJB3_TIMER_LOGGER.invalidValuesRange(value, min, max);
        }
    }

    public abstract boolean isRelativeValue(String value);

}
