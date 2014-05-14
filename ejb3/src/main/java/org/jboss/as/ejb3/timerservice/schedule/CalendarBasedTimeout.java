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
package org.jboss.as.ejb3.timerservice.schedule;

import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.TimeZone;

import javax.ejb.ScheduleExpression;

import org.jboss.as.ejb3.logging.EjbLogger;
import org.jboss.as.ejb3.timerservice.schedule.attribute.DayOfMonth;
import org.jboss.as.ejb3.timerservice.schedule.attribute.DayOfWeek;
import org.jboss.as.ejb3.timerservice.schedule.attribute.Hour;
import org.jboss.as.ejb3.timerservice.schedule.attribute.Minute;
import org.jboss.as.ejb3.timerservice.schedule.attribute.Month;
import org.jboss.as.ejb3.timerservice.schedule.attribute.Second;
import org.jboss.as.ejb3.timerservice.schedule.attribute.Year;

import static org.jboss.as.ejb3.logging.EjbLogger.ROOT_LOGGER;

/**
 * CalendarBasedTimeout
 *
 * @author Jaikiran Pai
 * @author "<a href=\"mailto:wfink@redhat.com\">Wolf-Dieter Fink</a>"
 * @author Eduardo Martins
 * @version $Revision: $
 */
public class CalendarBasedTimeout {


    /**
     * The {@link javax.ejb.ScheduleExpression} from which this {@link CalendarBasedTimeout}
     * was created
     */
    private ScheduleExpression scheduleExpression;

    /**
     * The {@link Second} created out of the {@link javax.ejb.ScheduleExpression#getSecond()} value
     */
    private Second second;

    /**
     * The {@link org.jboss.as.ejb3.timerservice.schedule.attribute.Minute} created out of the {@link javax.ejb.ScheduleExpression#getMinute()} value
     */
    private Minute minute;

    /**
     * The {@link org.jboss.as.ejb3.timerservice.schedule.attribute.Hour} created out of the {@link javax.ejb.ScheduleExpression#getHour()} value
     */
    private Hour hour;

    /**
     * The {@link DayOfWeek} created out of the {@link javax.ejb.ScheduleExpression#getDayOfWeek()} value
     */
    private DayOfWeek dayOfWeek;

    /**
     * The {@link org.jboss.as.ejb3.timerservice.schedule.attribute.DayOfMonth} created out of the {@link javax.ejb.ScheduleExpression#getDayOfMonth()} value
     */
    private DayOfMonth dayOfMonth;

    /**
     * The {@link Month} created out of the {@link javax.ejb.ScheduleExpression#getMonth()} value
     */
    private Month month;

    /**
     * The {@link org.jboss.as.ejb3.timerservice.schedule.attribute.Year} created out of the {@link javax.ejb.ScheduleExpression#getYear()} value
     */
    private Year year;

    /**
     * The first timeout relative to the time when this {@link CalendarBasedTimeout} was created
     * from a {@link javax.ejb.ScheduleExpression}
     */
    private Calendar firstTimeout;

    /**
     * The timezone being used for this {@link CalendarBasedTimeout}
     */
    private TimeZone timezone;

    /**
     * Creates a {@link CalendarBasedTimeout} from the passed <code>schedule</code>.
     * <p>
     * This constructor parses the passed {@link javax.ejb.ScheduleExpression} and sets up
     * its internal representation of the same.
     * </p>
     *
     * @param schedule The schedule
     */
    public CalendarBasedTimeout(ScheduleExpression schedule) {
        if (schedule == null) {
            throw EjbLogger.ROOT_LOGGER.invalidScheduleExpression(this.getClass().getName());
        }
        // make sure that the schedule doesn't have null values for its various attributes
        this.nullCheckScheduleAttributes(schedule);

        // store the original expression from which this
        // CalendarBasedTimeout was created. Since the ScheduleExpression
        // is mutable, we will have to store a clone copy of the schedule,
        // so that any subsequent changes after the CalendarBasedTimeout construction,
        // do not affect this internal schedule expression.
        this.scheduleExpression = this.clone(schedule);

        // Start parsing the values in the ScheduleExpression
        this.second = new Second(schedule.getSecond());
        this.minute = new Minute(schedule.getMinute());
        this.hour = new Hour(schedule.getHour());
        this.dayOfWeek = new DayOfWeek(schedule.getDayOfWeek());
        this.dayOfMonth = new DayOfMonth(schedule.getDayOfMonth());
        this.month = new Month(schedule.getMonth());
        this.year = new Year(schedule.getYear());
        if (schedule.getTimezone() != null && schedule.getTimezone().trim().isEmpty() == false) {
            // If the timezone ID wasn't valid, then Timezone.getTimeZone returns
            // GMT, which may not always be desirable.
            // So we first check to see if the timezone id specified is available in
            // timezone ids in the system. If it's available then we log a WARN message
            // and fallback on the server's timezone.
            String timezoneId = schedule.getTimezone();
            String[] availableTimeZoneIDs = TimeZone.getAvailableIDs();
            if (availableTimeZoneIDs != null && Arrays.asList(availableTimeZoneIDs).contains(timezoneId)) {
                this.timezone = TimeZone.getTimeZone(timezoneId);
            } else {
                ROOT_LOGGER.unknownTimezoneId(timezoneId, TimeZone.getDefault().getID());
                // use server's timezone
                this.timezone = TimeZone.getDefault();
            }
        } else {
            this.timezone = TimeZone.getDefault();
        }

        // Now that we have parsed the values from the ScheduleExpression,
        // determine and set the first timeout (relative to the current time)
        // of this CalendarBasedTimeout
        this.setFirstTimeout();
    }

    public Calendar getNextTimeout() {
        Calendar now = new GregorianCalendar(this.timezone);
        now.setTime(new Date());

        return this.getNextTimeout(now);
    }

    /**
     * @return
     */
    public Calendar getFirstTimeout() {
        return this.firstTimeout;
    }


    private void setFirstTimeout() {
        this.firstTimeout = new GregorianCalendar(this.timezone);
        Date start = this.scheduleExpression.getStart();
        if (start != null) {
            this.firstTimeout.setTime(start);
        } else {
            this.firstTimeout.set(Calendar.SECOND, this.second.getFirst());
            this.firstTimeout.set(Calendar.MINUTE, this.minute.getFirst());
            this.firstTimeout.set(Calendar.HOUR_OF_DAY, this.hour.getFirst());
            this.firstTimeout.set(Calendar.MILLISECOND, 0);
        }
        this.firstTimeout.setFirstDayOfWeek(Calendar.SUNDAY);

        this.firstTimeout = this.computeNextSecond(this.firstTimeout);
        if (this.firstTimeout == null) {
            return;
        }

        this.firstTimeout = this.computeNextMinute(this.firstTimeout);
        if (this.firstTimeout == null) {
            return;
        }

        this.firstTimeout = this.computeNextHour(this.firstTimeout);
        if (this.firstTimeout == null) {
            return;
        }

        this.firstTimeout = this.computeNextMonth(this.firstTimeout);
        if (this.firstTimeout == null) {
            return;
        }

        this.firstTimeout = this.computeNextDate(this.firstTimeout);
        if (this.firstTimeout == null) {
            return;
        }

        this.firstTimeout = this.computeNextYear(this.firstTimeout);

        // one final check
        if (this.firstTimeout != null && this.noMoreTimeouts(this.firstTimeout)) {
            this.firstTimeout = null;
        }

    }

    /**
     * Returns the original {@link javax.ejb.ScheduleExpression} from which this {@link CalendarBasedTimeout}
     * was created.
     *
     * @return
     */
    public ScheduleExpression getScheduleExpression() {
        return this.scheduleExpression;
    }

    public Calendar getNextTimeout(Calendar currentCal) {
        if (this.noMoreTimeouts(currentCal)) {
            return null;
        }
        Calendar nextCal = this.copy(currentCal);

        nextCal.setTimeZone(this.timezone);
        Date start = this.scheduleExpression.getStart();
        if (start != null && currentCal.getTime().before(start)) {
            nextCal.setTime(start);
        } else {
            // increment the current second by 1
            nextCal.add(Calendar.SECOND, 1);
            nextCal.set(Calendar.MILLISECOND, 0);
        }
        nextCal.setFirstDayOfWeek(Calendar.SUNDAY);

        nextCal = this.computeNextSecond(nextCal);
        if (nextCal == null) {
            return null;
        }

        nextCal = this.computeNextMinute(nextCal);
        if (nextCal == null) {
            return null;
        }

        nextCal = this.computeNextHour(nextCal);
        if (nextCal == null) {
            return null;
        }

        nextCal = this.computeNextMonth(nextCal);
        if (nextCal == null) {
            return null;
        }

        nextCal = this.computeNextDate(nextCal);
        if (nextCal == null) {
            return null;
        }

        nextCal = this.computeNextYear(nextCal);
        if (nextCal == null) {
            return null;
        }

        // one final check
        if (this.noMoreTimeouts(nextCal)) {
            return null;
        }
        return nextCal;
    }

    private Calendar computeNextSecond(Calendar currentCal) {
        if (this.noMoreTimeouts(currentCal)) {
            return null;
        }

        Integer nextSecond = this.second.getNextMatch(currentCal);

        if (nextSecond == null) {
            return null;
        }
        int currentSecond = currentCal.get(Calendar.SECOND);
        // if the current second is a match, then nothing else to
        // do. Just return back the calendar
        if (currentSecond == nextSecond) {
            return currentCal;
        }

        Calendar nextCal = this.truncate(currentCal, Calendar.MINUTE);
        // At this point, a suitable "next" second has been identified.
        // There can be 2 cases
        // 1) The "next" second is greater than the current second : This
        // implies that the next second is within the "current" minute.
        // 2) The "next" second is lesser than the current second : This implies
        // that the next second is in the next minute (i.e. current minute needs to
        // be advanced to next minute).

        // handle case#1
        if (nextSecond > currentSecond) {
            nextCal.set(Calendar.SECOND, nextSecond);
            return nextCal;
        }

        // case#2
        if (nextSecond < currentSecond) {
            nextCal.set(Calendar.SECOND, nextSecond);
            // advance the minute to next minute
            nextCal.add(Calendar.MINUTE, 1);
            return nextCal;
        }

        return null;
    }

    private Calendar computeNextMinute(Calendar currentCal) {
        if (this.noMoreTimeouts(currentCal)) {
            return null;
        }

        Integer nextMinute = this.minute.getNextMatch(currentCal);

        if (nextMinute == null) {
            return null;
        }
        int currentMinute = currentCal.get(Calendar.MINUTE);
        // if the current minute is a match, then nothing else to
        // do. Just return back the calendar
        if (currentMinute == nextMinute) {
            return currentCal;
        }

        Calendar nextCal = this.truncate(currentCal, Calendar.HOUR_OF_DAY);
        // At this point, a suitable "next" minute has been identified.
        // There can be 2 cases
        // 1) The "next" minute is greater than the current minute : This
        // implies that the next minute is within the "current" hour.
        // 2) The "next" minute is lesser than the current minute : This implies
        // that the next minute is in the next hour (i.e. current hour needs to
        // be advanced to next hour).

        // handle case#1
        if (nextMinute > currentMinute) {
            // set the chosen minute
            nextCal.set(Calendar.MINUTE, nextMinute);
            // since we are moving to a different minute (as compared to the current minute),
            // we should reset the second, to its first possible value
            nextCal.set(Calendar.SECOND, this.second.getFirst());

            return nextCal;
        }

        // case#2
        if (nextMinute < currentMinute) {
            // since we are advancing the hour, we should
            // restart from the first eligible second
            nextCal.set(Calendar.SECOND, this.second.getFirst());
            // set the chosen minute
            nextCal.set(Calendar.MINUTE, nextMinute);
            // advance the hour to next hour
            nextCal.add(Calendar.HOUR_OF_DAY, 1);

            return nextCal;
        }

        return null;
    }

    private Calendar computeNextHour(Calendar currentCal) {
        if (this.noMoreTimeouts(currentCal)) {
            return null;
        }

        Integer nextHour = this.hour.getNextMatch(currentCal);

        if (nextHour == null) {
            return null;
        }
        int currentHour = currentCal.get(Calendar.HOUR_OF_DAY);
        // if the current hour is a match, then nothing else to
        // do. Just return back the calendar
        if (currentHour == nextHour) {
            return currentCal;
        }

        Calendar nextCal = this.truncate(currentCal, Calendar.DATE);
        // At this point, a suitable "next" hour has been identified.
        // There can be 2 cases
        // 1) The "next" hour is greater than the current hour : This
        // implies that the next hour is within the "current" day.
        // 2) The "next" hour is lesser than the current hour : This implies
        // that the next hour is in the next day (i.e. current day needs to
        // be advanced to next day).

        // handle case#1
        if (nextHour > currentHour) {
            // set the chosen day of hour
            nextCal.set(Calendar.HOUR_OF_DAY, nextHour);
            // since we are moving to a different hour (as compared to the current hour),
            // we should reset the second and minute appropriately, to their first possible
            // values
            nextCal.set(Calendar.SECOND, this.second.getFirst());
            nextCal.set(Calendar.MINUTE, this.minute.getFirst());

            return nextCal;
        }

        // case#2
        if (nextHour < currentHour) {
            // set the chosen hour
            nextCal.set(Calendar.HOUR_OF_DAY, nextHour);

            // since we are moving to a different hour (as compared to the current hour),
            // we should reset the second and minute appropriately, to their first possible
            // values
            nextCal.set(Calendar.SECOND, this.second.getFirst());
            nextCal.set(Calendar.MINUTE, this.minute.getFirst());

            // advance to next day
            nextCal.add(Calendar.DATE, 1);

            return nextCal;
        }

        return null;
    }

    private Calendar computeNextDayOfWeek(Calendar currentCal) {

        if (this.noMoreTimeouts(currentCal)) {
            return null;
        }

        Integer nextDayOfWeek = this.dayOfWeek.getNextMatch(currentCal);

        if (nextDayOfWeek == null) {
            return null;
        }
        int currentDayOfWeek = currentCal.get(Calendar.DAY_OF_WEEK);
        // if the current day-of-week is a match, then nothing else to
        // do. Just return back the calendar
        if (currentDayOfWeek == nextDayOfWeek) {
            return currentCal;
        }

        Calendar nextCal = this.truncate(currentCal, Calendar.WEEK_OF_MONTH);
        // At this point, a suitable "next" day-of-week has been identified.
        // There can be 2 cases
        // 1) The "next" day-of-week is greater than the current day-of-week : This
        // implies that the next day-of-week is within the "current" week.
        // 2) The "next" day-of-week is lesser than the current day-of-week : This implies
        // that the next day-of-week is in the next week (i.e. current week needs to
        // be advanced to next week).
        nextCal.set(Calendar.DAY_OF_WEEK, nextDayOfWeek);
        if (nextDayOfWeek < currentDayOfWeek) {
            nextCal.add(Calendar.WEEK_OF_MONTH, 1);
        }

        // since we are moving to a different day-of-week (as compared to the current day-of-week),
        // we should reset the second, minute and hour appropriately, to their first possible
        // values
        nextCal.set(Calendar.SECOND, this.second.getFirst());
        nextCal.set(Calendar.MINUTE, this.minute.getFirst());
        nextCal.set(Calendar.HOUR_OF_DAY, this.hour.getFirst());

        if (nextCal.get(Calendar.MONTH) != currentCal.get(Calendar.MONTH)) {
            nextCal = computeNextMonth(nextCal);
        }
        return nextCal;
    }

    private Calendar computeNextMonth(Calendar currentCal) {
        if (this.noMoreTimeouts(currentCal)) {
            return null;
        }

        Integer nextMonth = this.month.getNextMatch(currentCal);

        if (nextMonth == null) {
            return null;
        }
        int currentMonth = currentCal.get(Calendar.MONTH);
        // if the current month is a match, then nothing else to
        // do. Just return back the calendar
        if (currentMonth == nextMonth) {
            return currentCal;
        }

        Calendar nextCal = this.truncate(currentCal, Calendar.YEAR);
        // At this point, a suitable "next" month has been identified.
        // There can be 2 cases
        // 1) The "next" month is greater than the current month : This
        // implies that the next month is within the "current" year.
        // 2) The "next" month is lesser than the current month : This implies
        // that the next month is in the next year (i.e. current year needs to
        // be advanced to next year).

        // handle case#1
        if (nextMonth > currentMonth) {
            // since we are moving to a different month (as compared to the current month),
            // we should reset the second, minute, hour, day-of-week and dayofmonth appropriately, to their first possible
            // values
            nextCal.set(Calendar.SECOND, this.second.getFirst());
            nextCal.set(Calendar.MINUTE, this.minute.getFirst());
            nextCal.set(Calendar.HOUR_OF_DAY, this.hour.getFirst());
            nextCal.set(Calendar.DAY_OF_WEEK, this.dayOfWeek.getFirst());
            nextCal.set(Calendar.DAY_OF_MONTH, 1);

            // set the chosen month
            nextCal.set(Calendar.MONTH, nextMonth);
            return nextCal;
        }

        // case#2
        if (nextMonth < currentMonth) {
            // set the chosen month
            nextCal.set(Calendar.MONTH, nextMonth);
            // since we are moving to a different month (as compared to the current month),
            // we should reset the second, minute, hour, day-of-week and dayofmonth appropriately, to their first possible
            // values
            nextCal.set(Calendar.SECOND, this.second.getFirst());
            nextCal.set(Calendar.MINUTE, this.minute.getFirst());
            nextCal.set(Calendar.HOUR_OF_DAY, this.hour.getFirst());
            nextCal.set(Calendar.DAY_OF_WEEK, this.dayOfWeek.getFirst());
            nextCal.set(Calendar.DAY_OF_MONTH, 1);

            // advance to next year
            nextCal.add(Calendar.YEAR, 1);

            return nextCal;
        }

        return null;
    }

    private Calendar computeNextDate(Calendar currentCal) {
        if (this.noMoreTimeouts(currentCal)) {
            return null;
        }

        if (this.isDayOfMonthWildcard()) {
            return this.computeNextDayOfWeek(currentCal);
        }

        if (this.isDayOfWeekWildcard()) {
            return this.computeNextDayOfMonth(currentCal);
        }

        // both day-of-month and day-of-week are *non-wildcards*
        Calendar nextDayOfMonthCal = this.computeNextDayOfMonth(currentCal);
        Calendar nextDayOfWeekCal = this.computeNextDayOfWeek(currentCal);

        if (nextDayOfMonthCal == null) {
            return nextDayOfWeekCal;
        }
        if (nextDayOfWeekCal == null) {
            return nextDayOfMonthCal;
        }

        return nextDayOfWeekCal.getTime().before(nextDayOfMonthCal.getTime()) ? nextDayOfWeekCal : nextDayOfMonthCal;

    }

    private Calendar computeNextDayOfMonth(Calendar currentCal) {
        if (this.noMoreTimeouts(currentCal)) {
            return null;
        }

        Integer nextDayOfMonth = this.dayOfMonth.getNextMatch(currentCal);

        if (nextDayOfMonth == null) {
            return null;
        }
        int currentDayOfMonth = currentCal.get(Calendar.DAY_OF_MONTH);
        // if the current day-of-month is a match, then nothing else to
        // do. Just return back the calendar
        if (currentDayOfMonth == nextDayOfMonth) {
            return currentCal;
        }

        Calendar nextCal = this.truncate(currentCal, Calendar.MONTH);

        if (nextDayOfMonth > currentDayOfMonth) {
            if (this.monthHasDate(nextCal, nextDayOfMonth)) {
                // set the chosen day-of-month
                nextCal.set(Calendar.DAY_OF_MONTH, nextDayOfMonth);
                // since we are moving to a different day-of-month (as compared to the current day-of-month),
                // we should reset the second, minute and hour appropriately, to their first possible
                // values
                nextCal.set(Calendar.SECOND, this.second.getFirst());
                nextCal.set(Calendar.MINUTE, this.minute.getFirst());
                nextCal.set(Calendar.HOUR_OF_DAY, this.hour.getFirst());
            } else {
                nextCal = this.advanceTillMonthHasDate(nextCal, nextDayOfMonth);
            }
        } else if (nextDayOfMonth < currentDayOfMonth) {
            // since the next day is before the current day we need to shift to the next month
            nextCal.add(Calendar.MONTH, 1);
            // also we need to reset the time
            nextCal.set(Calendar.SECOND, this.second.getFirst());
            nextCal.set(Calendar.MINUTE, this.minute.getFirst());
            nextCal.set(Calendar.HOUR_OF_DAY, this.hour.getFirst());
            nextCal = this.computeNextMonth(nextCal);
            if (nextCal == null) {
                return null;
            }
            nextDayOfMonth = this.dayOfMonth.getFirstMatch(nextCal);
            if (nextDayOfMonth == null) {
                return null;
            }
            // make sure the month can handle the date
            nextCal = this.advanceTillMonthHasDate(nextCal, nextDayOfMonth);
        }

        return nextCal;
    }


    private Calendar computeNextYear(Calendar currentCal) {
        if (this.noMoreTimeouts(currentCal)) {
            return null;
        }

        Integer nextYear = this.year.getNextMatch(currentCal);

        if (nextYear == null || nextYear > Year.MAX_YEAR) {
            return null;
        }
        int currentYear = currentCal.get(Calendar.YEAR);
        // if the current year is a match, then nothing else to
        // do. Just return back the calendar
        if (currentYear == nextYear) {
            return currentCal;
        }
        // If the next year is lesser than the current year, then
        // we have no more timeouts for the calendar expression
        if (nextYear < currentYear) {
            return null;
        }

        Calendar nextCal = this.truncate(currentCal, Calendar.ERA);
        // at this point we have chosen a year which is greater than the current
        // year.
        // set the chosen year
        nextCal.set(Calendar.YEAR, nextYear);
        // since we are moving to a different year (as compared to the current year),
        // we should reset all other calendar attribute expressions appropriately, to their first possible
        // values
        nextCal.set(Calendar.SECOND, this.second.getFirst());
        nextCal.set(Calendar.MINUTE, this.minute.getFirst());
        nextCal.set(Calendar.HOUR_OF_DAY, this.hour.getFirst());
        nextCal.set(Calendar.MONTH, this.month.getFirstMatch());
        nextCal.set(Calendar.DAY_OF_MONTH, 1);

        nextCal = this.computeNextDate(nextCal);
        if (nextCal == null) {
            return null;
        }

        return nextCal;
    }

    private Calendar advanceTillMonthHasDate(Calendar cal, Integer date) {
        Calendar copy = this.truncate(cal, Calendar.MONTH);
        copy.set(Calendar.SECOND, this.second.getFirst());
        copy.set(Calendar.MINUTE, this.minute.getFirst());
        copy.set(Calendar.HOUR_OF_DAY, this.hour.getFirst());

        // make sure the month can handle the date
        while (monthHasDate(copy, date) == false) {
            if (copy.get(Calendar.YEAR) > Year.MAX_YEAR) {
                return null;
            }
            // this month can't handle the date, so advance month to next month
            // and get the next suitable matching month
            copy.add(Calendar.MONTH, 1);
            copy = this.computeNextMonth(copy);
            if (copy == null) {
                return null;
            }
            date = this.dayOfMonth.getFirstMatch(copy);
            if (date == null) {
                return null;
            }
        }
        copy.set(Calendar.DAY_OF_MONTH, date);
        return copy;
    }

    private Calendar copy(Calendar cal) {
        Calendar copy = new GregorianCalendar(cal.getTimeZone());
        copy.setTimeInMillis(cal.getTimeInMillis());

        return copy;
    }

    private Calendar truncate(Calendar cal, int field) {
        Calendar copy = new GregorianCalendar(cal.getTimeZone());
        copy.clear();
        for(int i=0; i <= field; i++) {
            if(cal.isSet(i)) {
                copy.set(i, cal.get(i));
            }
        }
        // set that week starts on SUNDAY (as is done in #setFirstTimeout())
        // to be able to compare dates' day of week correctly
        copy.setFirstDayOfWeek(Calendar.SUNDAY);
        return copy;
    }

    private boolean monthHasDate(Calendar cal, int date) {
        return date <= cal.getActualMaximum(Calendar.DAY_OF_MONTH);
    }

    private boolean isAfterEnd(Calendar cal) {
        Date end = this.scheduleExpression.getEnd();
        if (end == null) {
            return false;
        }
        // check that the next timeout isn't past the end date
        return cal.getTime().after(end);
    }

    private boolean noMoreTimeouts(Calendar cal) {
        if (cal.get(Calendar.YEAR) > Year.MAX_YEAR || isAfterEnd(cal)) {
            return true;
        }
        return false;
    }

    private boolean isDayOfWeekWildcard() {
        return this.scheduleExpression.getDayOfWeek().equals("*");
    }

    private boolean isDayOfMonthWildcard() {
        return this.scheduleExpression.getDayOfMonth().equals("*");
    }

    private void nullCheckScheduleAttributes(ScheduleExpression schedule) {
        if (schedule.getSecond() == null) {
            throw EjbLogger.ROOT_LOGGER.invalidScheduleExpressionSecond(schedule);
        }
        if (schedule.getMinute() == null) {
            throw EjbLogger.ROOT_LOGGER.invalidScheduleExpressionMinute(schedule);
        }
        if (schedule.getHour() == null) {
            throw EjbLogger.ROOT_LOGGER.invalidScheduleExpressionHour(schedule);
        }
        if (schedule.getDayOfMonth() == null) {
            throw EjbLogger.ROOT_LOGGER.invalidScheduleExpressionDayOfMonth(schedule);
        }
        if (schedule.getDayOfWeek() == null) {
            throw EjbLogger.ROOT_LOGGER.invalidScheduleExpressionDayOfWeek(schedule);
        }
        if (schedule.getMonth() == null) {
            throw EjbLogger.ROOT_LOGGER.invalidScheduleExpressionMonth(schedule);
        }
        if (schedule.getYear() == null) {
            throw EjbLogger.ROOT_LOGGER.invalidScheduleExpressionYear(schedule);
        }
    }

    private ScheduleExpression clone(ScheduleExpression schedule) {
        // clone the schedule
        ScheduleExpression clonedSchedule = new ScheduleExpression();
        clonedSchedule.second(schedule.getSecond());
        clonedSchedule.minute(schedule.getMinute());
        clonedSchedule.hour(schedule.getHour());
        clonedSchedule.dayOfWeek(schedule.getDayOfWeek());
        clonedSchedule.dayOfMonth(schedule.getDayOfMonth());
        clonedSchedule.month(schedule.getMonth());
        clonedSchedule.year(schedule.getYear());
        clonedSchedule.timezone(schedule.getTimezone());
        clonedSchedule.start(schedule.getStart());
        clonedSchedule.end(schedule.getEnd());

        return clonedSchedule;
    }

}
