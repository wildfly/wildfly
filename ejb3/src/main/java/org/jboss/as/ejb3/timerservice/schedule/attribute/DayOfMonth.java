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

import java.util.Calendar;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.regex.Pattern;

import org.jboss.as.ejb3.logging.EjbLogger;
import org.jboss.as.ejb3.timerservice.schedule.util.CalendarUtil;
import org.jboss.as.ejb3.timerservice.schedule.value.RangeValue;
import org.jboss.as.ejb3.timerservice.schedule.value.ScheduleExpressionType;
import org.jboss.as.ejb3.timerservice.schedule.value.ScheduleValue;
import org.jboss.as.ejb3.timerservice.schedule.value.SingleValue;

/**
 * Represents the value of a day in a month, constructed out of a {@link jakarta.ejb.ScheduleExpression#getDayOfMonth()}
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
     * Regex pattern for multiple space characters
     */
    public static final Pattern REGEX_SPACES = Pattern.compile("\\s+");
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
    private static final Map<String, Integer> ORDINAL_TO_WEEK_NUMBER_MAPPING = new HashMap<>(8);

    static {
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
            throw EjbLogger.EJB3_TIMER_LOGGER.invalidScheduleValue(DayOfMonth.class.getSimpleName(), String.valueOf(value));
        }
        super.assertValid(value);
    }

    private boolean hasRelativeDayOfMonth() {
        return !this.relativeValues.isEmpty();
    }

    private SortedSet<Integer> getEligibleDaysOfMonth(Calendar cal) {
        if (!this.hasRelativeDayOfMonth()) {
            return this.absoluteValues;
        }
        SortedSet<Integer> eligibleDaysOfMonth = new TreeSet<>(this.absoluteValues);
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

                Integer dayOfMonthStart;
                // either start will be relative or end will be relative or both are relative
                if (this.isRelativeValue(start)) {
                    dayOfMonthStart = this.getAbsoluteDayOfMonth(cal, start);
                } else {
                    dayOfMonthStart = this.parseInt(start);
                }

                Integer dayOfMonthEnd;
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

    /**
     * Gets the absolute day of month.
     * @param cal the calendar
     * @param trimmedRelativeDayOfMonth a non-null, trimmed, relative day of month
     * @return the absolute day of month
     */
    private int getAbsoluteDayOfMonth(Calendar cal, String trimmedRelativeDayOfMonth) {
        if (trimmedRelativeDayOfMonth.isEmpty()) {
            throw EjbLogger.EJB3_TIMER_LOGGER.invalidScheduleValue(DayOfMonth.class.getSimpleName(), trimmedRelativeDayOfMonth);
        }
        trimmedRelativeDayOfMonth = trimmedRelativeDayOfMonth.toLowerCase(Locale.ROOT);
        if (trimmedRelativeDayOfMonth.equals("last")) {
            return CalendarUtil.getLastDateOfMonth(cal);
        }
        if (this.isValidNegativeDayOfMonth(trimmedRelativeDayOfMonth)) {
            int negativeRelativeDayOfMonth = Integer.parseInt(trimmedRelativeDayOfMonth);
            int lastDayOfCurrentMonth = CalendarUtil.getLastDateOfMonth(cal);
            return lastDayOfCurrentMonth + negativeRelativeDayOfMonth;
        }
        String[] parts = splitDayOfWeekBased(trimmedRelativeDayOfMonth);
        if (parts != null) {
            String ordinal = parts[0];
            String day = parts[1];

            // DAY_OF_WEEK_ALIAS value is 0-based, while Calendar.SUNDAY is 1,
            // MONDAY is 2, so need to add 1 match Calendar's value.
            int dayOfWeek = DayOfWeek.DAY_OF_WEEK_ALIAS.get(day) + 1;

            Integer date;
            if (ordinal.equals("last")) {
                date = CalendarUtil.getDateOfLastDayOfWeekInMonth(cal, dayOfWeek);
            } else {
                int weekNumber = ORDINAL_TO_WEEK_NUMBER_MAPPING.get(ordinal);
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
        throw EjbLogger.EJB3_TIMER_LOGGER.invalidScheduleValue(DayOfMonth.class.getSimpleName(), trimmedRelativeDayOfMonth);
    }

    private boolean isValidNegativeDayOfMonth(String dayOfMonth) {
        try {
            int val = Integer.parseInt(dayOfMonth.trim());
            if (val <= -1 && val >= -7) {
                return true;
            }
            return false;
        } catch (NumberFormatException nfe) {
            return false;
        }

    }

    /**
     * Checks if a relative value is weekOfDay-based, and splits the passed
     * {@code trimmedLowerCaseRelativeVal} to 2 parts.
     * @param trimmedLowerCaseRelativeVal must be non-null, trimmed and lower case value
     * @return 2 parts, or null if {@code trimmedLowerCaseRelativeVal} is not dayOfWeek-based
     */
    private String[] splitDayOfWeekBased(String trimmedLowerCaseRelativeVal) {
        String[] relativeParts = REGEX_SPACES.split(trimmedLowerCaseRelativeVal);
        if (relativeParts == null) {
            return null;
        }
        if (relativeParts.length != 2) {
            return null;
        }
        String lowerCaseOrdinal = relativeParts[0];
        String lowerCaseDayOfWeek = relativeParts[1];
        if (lowerCaseOrdinal == null || lowerCaseDayOfWeek == null) {
            return null;
        }
        if (!ORDINAL_TO_WEEK_NUMBER_MAPPING.containsKey(lowerCaseOrdinal) && !lowerCaseOrdinal.equals("last")) {
            return null;
        }
        if (!DayOfWeek.DAY_OF_WEEK_ALIAS.containsKey(lowerCaseDayOfWeek)) {
            return null;
        }
        return relativeParts;
    }

    @Override
    public boolean isRelativeValue(String value) {
        String lowerCaseValue = value.toLowerCase(Locale.ROOT);
        if (lowerCaseValue.equals("last")) {
            return true;
        }
        if (this.isValidNegativeDayOfMonth(lowerCaseValue)) {
            return true;
        }
        if (this.splitDayOfWeekBased(lowerCaseValue) != null) {
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
            String lowerCaseAlias = alias.toLowerCase(Locale.ENGLISH);
            final Integer dayOfWeekInteger = DayOfWeek.DAY_OF_WEEK_ALIAS.get(lowerCaseAlias);

            // DAY_OF_WEEK_ALIAS value is 0-based, while Calendar.SUNDAY is 1,
            // MONDAY is 2, so need to add 1 match Calendar's value.
            return dayOfWeekInteger == null ? null : dayOfWeekInteger + 1;
        }
    }
}
