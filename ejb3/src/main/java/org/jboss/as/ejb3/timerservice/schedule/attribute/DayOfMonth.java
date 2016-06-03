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
import org.jboss.as.ejb3.timerservice.schedule.util.CalendarUtil;
import org.jboss.as.ejb3.timerservice.schedule.value.RangeValue;
import org.jboss.as.ejb3.timerservice.schedule.value.ScheduleExpressionType;
import org.jboss.as.ejb3.timerservice.schedule.value.ScheduleValue;
import org.jboss.as.ejb3.timerservice.schedule.value.SingleValue;

import java.util.Calendar;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.regex.Pattern;

/**
 * Represents the value of a day in a month, constructed out of a {@link javax.ejb.ScheduleExpression#getDayOfMonth()}
 * <p/>
 * <p>
 * A {@link DayOfMonth} can hold an {@link Integer} or a {@link String} as its value.
 * value. The various ways in which a
 * {@link DayOfMonth} value can be represented are:
 * <ul>
 * <li>Wildcard. For example, dayOfMonth = "*"</li>
 * <li>Range. Examples:
 * <ul>
 * <li>dayOfMonth = "1-10"</li>
 * <li>dayOfMonth = "Sun-Tue"</li>
 * <li>dayOfMonth = "1st-5th"</li>
 * </ul>
 * </li>
 * <li>List. Examples:
 * <ul>
 * <li>dayOfMonth = "1, 12, 20"</li>
 * <li>dayOfMonth = "Mon, Fri, Sun"</li>
 * <li>dayOfMonth = "3rd, 1st, Last"</li>
 * </ul>
 * </li>
 * <li>Single value. Examples:
 * <ul>
 * <li>dayOfMonth = "Fri"</li>
 * <li>dayOfMonth = "Last"</li>
 * <li>dayOfMonth = "10"</li>
 * </ul>
 * </li>
 * </ul>
 * </p>
 *
 * @author Jaikiran Pai
 * @version $Revision: $
 */
public class DayOfMonth extends IntegerBasedExpression {

    /**
     * The maximum allowed value for the {@link DayOfMonth}
     */
    public static final Integer MAX_DAY_OF_MONTH = 31;

    /**
     * The minimum allowed value for the {@link DayOfMonth}
     */
    public static final Integer MIN_DAY_OF_MONTH = -7;

    /**
     * A {@link DayOfMonth} can be represented as a {@link String} too (for example "1st", "Sun" etc...).
     * Internally, we map all allowed {@link String} values to their {@link Integer} equivalents.
     * This map holds the {@link String} name to {@link Integer} value mapping.
     */
    private static final Map<String, Integer> DAY_OF_MONTH_ALIAS = new HashMap<String, Integer>();

    static {
        DAY_OF_MONTH_ALIAS.put("sun", Calendar.SUNDAY);
        DAY_OF_MONTH_ALIAS.put("mon", Calendar.MONDAY);
        DAY_OF_MONTH_ALIAS.put("tue", Calendar.TUESDAY);
        DAY_OF_MONTH_ALIAS.put("wed", Calendar.WEDNESDAY);
        DAY_OF_MONTH_ALIAS.put("thu", Calendar.THURSDAY);
        DAY_OF_MONTH_ALIAS.put("fri", Calendar.FRIDAY);
        DAY_OF_MONTH_ALIAS.put("sat", Calendar.SATURDAY);

    }

    private static final Set<String> ORDINALS = new HashSet<String>();

    private static final Map<String, Integer> ORDINAL_TO_WEEK_NUMBER_MAPPING = new HashMap<String, Integer>();

    static {
        ORDINALS.add("1st");
        ORDINALS.add("2nd");
        ORDINALS.add("3rd");
        ORDINALS.add("4th");
        ORDINALS.add("5th");
        ORDINALS.add("last");

        ORDINAL_TO_WEEK_NUMBER_MAPPING.put("1st", 1);
        ORDINAL_TO_WEEK_NUMBER_MAPPING.put("2nd", 2);
        ORDINAL_TO_WEEK_NUMBER_MAPPING.put("3rd", 3);
        ORDINAL_TO_WEEK_NUMBER_MAPPING.put("4th", 4);
        ORDINAL_TO_WEEK_NUMBER_MAPPING.put("5th", 5);

    }


    /**
     * Creates a {@link DayOfMonth} by parsing the passed {@link String} <code>value</code>
     * <p>
     * Valid values are of type {@link ScheduleExpressionType#WILDCARD}, {@link ScheduleExpressionType#RANGE},
     * {@link ScheduleExpressionType#LIST} or {@link ScheduleExpressionType#SINGLE_VALUE}
     * </p>
     *
     * @param value The value to be parsed
     * @throws IllegalArgumentException If the passed <code>value</code> is neither a {@link ScheduleExpressionType#WILDCARD},
     *                                  {@link ScheduleExpressionType#RANGE}, {@link ScheduleExpressionType#LIST},
     *                                  nor {@link ScheduleExpressionType#SINGLE_VALUE}.
     */
    public DayOfMonth(String value) {
        super(value);
    }

    /**
     * Returns the maximum allowed value for a {@link DayOfMonth}
     *
     * @see DayOfMonth#MAX_DAY_OF_MONTH
     */
    @Override
    protected Integer getMaxValue() {
        return MAX_DAY_OF_MONTH;
    }

    /**
     * Returns the minimum allowed value for a {@link DayOfMonth}
     *
     * @see DayOfMonth#MIN_DAY_OF_MONTH
     */
    @Override
    protected Integer getMinValue() {
        return MIN_DAY_OF_MONTH;
    }

    public Integer getNextMatch(Calendar currentCal) {
        if (this.scheduleExpressionType == ScheduleExpressionType.WILDCARD) {
            return currentCal.get(Calendar.DAY_OF_MONTH);
        }
        int currentDayOfMonth = currentCal.get(Calendar.DAY_OF_MONTH);
        SortedSet<Integer> eligibleDaysOfMonth = this.getEligibleDaysOfMonth(currentCal);
        if (eligibleDaysOfMonth.isEmpty()) {
            return null;
        }
        for (Integer hour : eligibleDaysOfMonth) {
            if (currentDayOfMonth == hour) {
                return currentDayOfMonth;
            }
            if (hour > currentDayOfMonth) {
                return hour;
            }
        }
        return eligibleDaysOfMonth.first();
    }

    @Override
    protected void assertValid(Integer value) throws IllegalArgumentException {
        if (value != null && value == 0) {
            throw EjbLogger.EJB3_TIMER_LOGGER.invalidValueDayOfMonth(value);
        }
        super.assertValid(value);
    }

    private boolean hasRelativeDayOfMonth() {
        if (this.relativeValues.isEmpty()) {
            return false;
        }
        return true;
    }

    private SortedSet<Integer> getEligibleDaysOfMonth(Calendar cal) {
        if (this.hasRelativeDayOfMonth() == false) {
            return this.absoluteValues;
        }
        SortedSet<Integer> eligibleDaysOfMonth = new TreeSet<Integer>(this.absoluteValues);
        for (ScheduleValue relativeValue : this.relativeValues) {
            if (relativeValue instanceof SingleValue) {
                SingleValue singleValue = (SingleValue) relativeValue;
                String value = singleValue.getValue();
                Integer absoluteDayOfMonth = this.getAbsoluteDayOfMonth(cal, value);
                eligibleDaysOfMonth.add(absoluteDayOfMonth);
            } else if (relativeValue instanceof RangeValue) {
                RangeValue range = (RangeValue) relativeValue;
                String start = range.getStart();
                String end = range.getEnd();

                Integer dayOfMonthStart = null;
                // either start will be relative or end will be relative or both are relative
                if (this.isRelativeValue(start)) {
                    dayOfMonthStart = this.getAbsoluteDayOfMonth(cal, start);
                } else {
                    dayOfMonthStart = this.parseInt(start);
                }

                Integer dayOfMonthEnd = null;
                if (this.isRelativeValue(end)) {
                    dayOfMonthEnd = this.getAbsoluteDayOfMonth(cal, end);
                } else {
                    dayOfMonthEnd = this.parseInt(end);
                }
                // validations
                this.assertValid(dayOfMonthStart);
                this.assertValid(dayOfMonthEnd);

                // start and end are both the same. So it's just a single value
                if (dayOfMonthStart.equals(dayOfMonthEnd)) {
                    eligibleDaysOfMonth.add(dayOfMonthEnd);
                    continue;

                }
                if (dayOfMonthStart > dayOfMonthEnd) {
                    // In range "x-y", if x is larger than y, the range is equivalent to
                    // "x-max, min-y", where max is the largest value of the corresponding attribute
                    // and min is the smallest.
                    for (int i = dayOfMonthStart; i <= this.getMaxValue(); i++) {
                        eligibleDaysOfMonth.add(i);
                    }
                    for (int i = this.getMinValue(); i <= dayOfMonthEnd; i++) {
                        eligibleDaysOfMonth.add(i);
                    }
                } else {
                    // just keep adding from range start to range end (both inclusive).
                    for (int i = dayOfMonthStart; i <= dayOfMonthEnd; i++) {
                        eligibleDaysOfMonth.add(i);
                    }
                }
            }

        }
        return eligibleDaysOfMonth;
    }

    private int getAbsoluteDayOfMonth(Calendar cal, String relativeDayOfMonth) {
        if (relativeDayOfMonth == null || relativeDayOfMonth.trim().isEmpty()) {
            throw EjbLogger.EJB3_TIMER_LOGGER.relativeDayOfMonthIsNull();
        }
        String trimmedRelativeDayOfMonth = relativeDayOfMonth.trim();
        if (trimmedRelativeDayOfMonth.equalsIgnoreCase("last")) {
            int lastDayOfCurrentMonth = CalendarUtil.getLastDateOfMonth(cal);
            return lastDayOfCurrentMonth;
        }
        if (this.isValidNegativeDayOfMonth(trimmedRelativeDayOfMonth)) {
            Integer negativeRelativeDayOfMonth = Integer.parseInt(trimmedRelativeDayOfMonth);
            int lastDayOfCurrentMonth = CalendarUtil.getLastDateOfMonth(cal);
            return lastDayOfCurrentMonth + negativeRelativeDayOfMonth;
        }
        if (this.isDayOfWeekBased(trimmedRelativeDayOfMonth)) {

            String[] parts = trimmedRelativeDayOfMonth.split("\\s+");
            String ordinal = parts[0];
            String day = parts[1];
            int dayOfWeek = DAY_OF_MONTH_ALIAS.get(day.toLowerCase(Locale.ENGLISH));

            Integer date = null;
            if (ordinal.equalsIgnoreCase("last")) {
                date = CalendarUtil.getDateOfLastDayOfWeekInMonth(cal, dayOfWeek);
            } else {
                int weekNumber = ORDINAL_TO_WEEK_NUMBER_MAPPING.get(ordinal.toLowerCase(Locale.ENGLISH));
                date = CalendarUtil.getNthDayOfMonth(cal, weekNumber, dayOfWeek);
            }

            // TODO: Rethink about this. The reason why we have this currently is to handle cases like:
            // 5th Wed which may not be valid for all months (i.e. all months do not have 5 weeks). In such
            // cases we set the date to last date of the month.
            // This needs to be thought about a bit more in detail, to understand it's impact on other scenarios.
            if (date == null) {
                date = CalendarUtil.getLastDateOfMonth(cal);
            }

            return date;
        }
        throw EjbLogger.EJB3_TIMER_LOGGER.invalidRelativeValue(relativeDayOfMonth);
    }

    private boolean isValidNegativeDayOfMonth(String dayOfMonth) {
        try {
            Integer val = Integer.parseInt(dayOfMonth.trim());
            if (val <= -1 && val >= -7) {
                return true;
            }
            return false;
        } catch (NumberFormatException nfe) {
            return false;
        }

    }

    private boolean isDayOfWeekBased(String relativeVal) {
        String trimmedVal = relativeVal.trim();
        // one or more spaces (which includes tabs and other forms of space)
        Pattern p = Pattern.compile("\\s+");
        String[] relativeParts = p.split(trimmedVal);
        if (relativeParts == null) {
            return false;
        }
        if (relativeParts.length != 2) {
            return false;
        }
        String ordinal = relativeParts[0];
        String dayOfWeek = relativeParts[1];
        if (ordinal == null || dayOfWeek == null) {
            return false;
        }
        String lowerCaseOrdinal = ordinal.toLowerCase(Locale.ENGLISH);
        if (ORDINALS.contains(lowerCaseOrdinal) == false) {
            return false;
        }
        String lowerCaseDayOfWeek = dayOfWeek.toLowerCase(Locale.ENGLISH);
        if (DAY_OF_MONTH_ALIAS.containsKey(lowerCaseDayOfWeek) == false) {
            return false;
        }
        return true;
    }

    @Override
    public boolean isRelativeValue(String value) {
        if (value == null) {
            throw EjbLogger.EJB3_TIMER_LOGGER.relativeValueIsNull();
        }
        if (value.equalsIgnoreCase("last")) {
            return true;
        }
        if (this.isValidNegativeDayOfMonth(value)) {
            return true;
        }
        if (this.isDayOfWeekBased(value)) {
            return true;
        }
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
            // day-of-month doesn't support increment
            case INCREMENT:
            default:
                return false;
        }
    }

    public Integer getFirstMatch(Calendar cal) {
        if (this.scheduleExpressionType == ScheduleExpressionType.WILDCARD) {
            return Calendar.SUNDAY;
        }
        SortedSet<Integer> eligibleDaysOfMonth = this.getEligibleDaysOfMonth(cal);
        if (eligibleDaysOfMonth.isEmpty()) {
            return null;
        }
        return eligibleDaysOfMonth.first();
    }

    @Override
    protected Integer parseInt(String alias) {
        try {
            return super.parseInt(alias);
        } catch (NumberFormatException nfe) {
            if (DAY_OF_MONTH_ALIAS != null) {
                String lowerCaseAlias = alias.toLowerCase(Locale.ENGLISH);
                return DAY_OF_MONTH_ALIAS.get(lowerCaseAlias);
            }
        }
        return null;
    }
}
