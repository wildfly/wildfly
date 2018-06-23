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

import org.jboss.as.ejb3.timerservice.schedule.attribute.DayOfMonth;
import org.jboss.as.ejb3.timerservice.schedule.attribute.DayOfWeek;
import org.jboss.as.ejb3.timerservice.schedule.attribute.Hour;
import org.jboss.as.ejb3.timerservice.schedule.attribute.Minute;
import org.jboss.as.ejb3.timerservice.schedule.attribute.Month;
import org.jboss.as.ejb3.timerservice.schedule.attribute.Second;
import org.jboss.as.ejb3.timerservice.schedule.attribute.Year;

import static org.jboss.as.ejb3.logging.EjbLogger.EJB3_TIMER_LOGGER;

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
            throw EJB3_TIMER_LOGGER.invalidScheduleExpression(this.getClass().getName());
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
        String timezoneId = schedule.getTimezone();
        if (timezoneId != null && !(timezoneId = timezoneId.trim()).isEmpty()) {
            // If the timezone ID wasn't valid, then Timezone.getTimeZone returns
            // GMT, which may not always be desirable.
            // So we first check to see if the timezone id specified is available in
            // timezone ids in the system. If it's available then we log a WARN message
            // and fallback on the server's timezone.
            String[] availableTimeZoneIDs = TimeZone.getAvailableIDs();
            if (availableTimeZoneIDs != null && Arrays.asList(availableTimeZoneIDs).contains(timezoneId)) {
                this.timezone = TimeZone.getTimeZone(timezoneId);
            } else {
                // use server's timezone
                this.timezone = TimeZone.getDefault();
                EJB3_TIMER_LOGGER.unknownTimezoneId(timezoneId, this.timezone.getID());
            }
        } else {
            this.timezone = TimeZone.getDefault();
        }

        // Now that we have parsed the values from the ScheduleExpression,
        // determine and set the first timeout (relative to the current time)
        // of this CalendarBasedTimeout
        setFirstTimeout();
        }

    public Calendar getNextTimeout() {
        return getNextTimeout(new GregorianCalendar(this.timezone), true);
    }

    /**
     * @return
     */
    public Calendar getFirstTimeout() {
        return this.firstTimeout;
    }

    private void setFirstTimeout() {
        Calendar currentCal = new GregorianCalendar(this.timezone);
        Date start = this.scheduleExpression.getStart();
        if (start != null) {
            currentCal.setTime(start);
        } else {
            resetTimeToFirstValues(currentCal);
        }
        this.firstTimeout = getNextTimeout(currentCal, false);
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
        return getNextTimeout(currentCal, true);
    }

    private Calendar getNextTimeout(Calendar currentCal, boolean increment) {
        if (this.noMoreTimeouts(currentCal)) {
            return null;
        }
        Calendar nextCal = (Calendar) currentCal.clone();
        nextCal.setTimeZone(this.timezone);
        Date start = this.scheduleExpression.getStart();
        if (start != null && currentCal.getTime().before(start)) {
            //this may result in a millisecond component, however that is ok
            //otherwise WFLY-6561 will rear its only head
            //also as the start time may include milliseconds this is technically correct
            nextCal.setTime(start);
        } else {
            if (increment) {
                // increment the current second by 1
                nextCal.add(Calendar.SECOND, 1);
            }
            nextCal.add(Calendar.MILLISECOND, -nextCal.get(Calendar.MILLISECOND));
        }
        nextCal.setFirstDayOfWeek(Calendar.SUNDAY);

        nextCal = this.computeNextTime(nextCal);
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

    private Calendar computeNextTime(Calendar nextCal) {
        int currentSecond = nextCal.get(Calendar.SECOND);
        int currentMinute = nextCal.get(Calendar.MINUTE);
        int currentHour = nextCal.get(Calendar.HOUR_OF_DAY);
        final int currentTimeInSeconds = currentHour*3600 + currentMinute*60 + currentSecond;

        // compute next second
        Integer nextSecond = this.second.getNextMatch(currentSecond);
        if (nextSecond == null) {
            return null;
        }
        // compute next minute
        if (nextSecond < currentSecond) {
            currentMinute++;
        }
        Integer nextMinute = this.minute.getNextMatch(currentMinute < 60 ? currentMinute : 0);
        if (nextMinute == null) {
            return null;
        }
        // reset second if minute was changed  (Fix WFLY-5955)
        if( nextMinute != currentMinute) {
            nextSecond = this.second.getNextMatch(0);
        }
        // compute next hour
        if (nextMinute < currentMinute) {
            currentHour++;
        }
        Integer nextHour = this.hour.getNextMatch(currentHour < 24 ? currentHour : 0);
        if (nextHour == null) {
            return null;
        }
        if(nextHour != currentHour) {
            // reset second/minute if hour changed  (Fix WFLY-5955)
            nextSecond = this.second.getNextMatch(0);
            nextMinute = this.minute.getNextMatch(0);
        }

        final int nextTimeInSeconds = nextHour*3600 + nextMinute*60 + nextSecond;
        if (nextTimeInSeconds == currentTimeInSeconds) {
            // no change in time
            return nextCal;
        }

        // Set the time before adding the a day. If we do it after,
        // we could be using an invalid DST value in setTime method
        setTime(nextCal, nextHour, nextMinute, nextSecond);

        // time change
        if (nextTimeInSeconds < currentTimeInSeconds) {
            // advance to next day
            nextCal.add(Calendar.DATE, 1);
        }

        return nextCal;
    }

    private Calendar computeNextDayOfWeek(Calendar nextCal) {
        Integer nextDayOfWeek = this.dayOfWeek.getNextMatch(nextCal);

        if (nextDayOfWeek == null) {
            return null;
        }
        int currentDayOfWeek = nextCal.get(Calendar.DAY_OF_WEEK);
        // if the current day-of-week is a match, then nothing else to
        // do. Just return back the calendar
        if (currentDayOfWeek == nextDayOfWeek) {
            return nextCal;
        }
        int currentMonth = nextCal.get(Calendar.MONTH);

        // At this point, a suitable "next" day-of-week has been identified.
        // There can be 2 cases
        // 1) The "next" day-of-week is greater than the current day-of-week : This
        // implies that the next day-of-week is within the "current" week.
        // 2) The "next" day-of-week is lesser than the current day-of-week : This implies
        // that the next day-of-week is in the next week (i.e. current week needs to
        // be advanced to next week).
        if (nextDayOfWeek < currentDayOfWeek) {
            // advance one week
            nextCal.add(Calendar.WEEK_OF_MONTH, 1);
        }
        // set the chosen day of week
        nextCal.set(Calendar.DAY_OF_WEEK, nextDayOfWeek);
        // since we are moving to a different day-of-week (as compared to the current day-of-week),
        // we should reset the second, minute and hour appropriately, to their first possible
        // values
        resetTimeToFirstValues(nextCal);

        if (nextCal.get(Calendar.MONTH) != currentMonth) {
            nextCal = computeNextMonth(nextCal);
        }
        return nextCal;
    }

    private Calendar computeNextMonth(Calendar nextCal) {
        Integer nextMonth = this.month.getNextMatch(nextCal);

        if (nextMonth == null) {
            return null;
        }
        int currentMonth = nextCal.get(Calendar.MONTH);
        // if the current month is a match, then nothing else to
        // do. Just return back the calendar
        if (currentMonth == nextMonth) {
            return nextCal;
        }

        // At this point, a suitable "next" month has been identified.
        // There can be 2 cases
        // 1) The "next" month is greater than the current month : This
        // implies that the next month is within the "current" year.
        // 2) The "next" month is lesser than the current month : This implies
        // that the next month is in the next year (i.e. current year needs to
        // be advanced to next year).
        if (nextMonth < currentMonth) {
            // advance to next year
            nextCal.add(Calendar.YEAR, 1);
        }
        // set the chosen month
        nextCal.set(Calendar.MONTH, nextMonth);
        // since we are moving to a different month (as compared to the current month),
        // we should reset the second, minute, hour, day-of-week and dayofmonth appropriately, to their first possible
        // values
        nextCal.set(Calendar.DAY_OF_WEEK, this.dayOfWeek.getFirst());
        nextCal.set(Calendar.DAY_OF_MONTH, 1);
        resetTimeToFirstValues(nextCal);

        return nextCal;
    }

    private Calendar computeNextDate(Calendar nextCal) {
        if (this.isDayOfMonthWildcard()) {
            return this.computeNextDayOfWeek(nextCal);
        }

        if (this.isDayOfWeekWildcard()) {
            return this.computeNextDayOfMonth(nextCal);
        }

        // both day-of-month and day-of-week are *non-wildcards*
        Calendar nextDayOfMonthCal = this.computeNextDayOfMonth((Calendar) nextCal.clone());
        Calendar nextDayOfWeekCal = this.computeNextDayOfWeek((Calendar) nextCal.clone());

        if (nextDayOfMonthCal == null) {
            return nextDayOfWeekCal;
        }
        if (nextDayOfWeekCal == null) {
            return nextDayOfMonthCal;
        }

        return nextDayOfWeekCal.getTime().before(nextDayOfMonthCal.getTime()) ? nextDayOfWeekCal : nextDayOfMonthCal;
    }

    private Calendar computeNextDayOfMonth(Calendar nextCal) {
        Integer nextDayOfMonth = this.dayOfMonth.getNextMatch(nextCal);

        if (nextDayOfMonth == null) {
            return null;
        }
        int currentDayOfMonth = nextCal.get(Calendar.DAY_OF_MONTH);
        // if the current day-of-month is a match, then nothing else to
        // do. Just return back the calendar
        if (currentDayOfMonth == nextDayOfMonth) {
            return nextCal;
        }

        if (nextDayOfMonth > currentDayOfMonth) {
            if (this.monthHasDate(nextCal, nextDayOfMonth)) {
                // set the chosen day-of-month
                nextCal.set(Calendar.DAY_OF_MONTH, nextDayOfMonth);
                // since we are moving to a different day-of-month (as compared to the current day-of-month),
                // we should reset the second, minute and hour appropriately, to their first possible
                // values
                resetTimeToFirstValues(nextCal);

            } else {
                nextCal = this.advanceTillMonthHasDate(nextCal, nextDayOfMonth);
            }
        } else {
            // since the next day is before the current day we need to shift to the next month
            nextCal.add(Calendar.MONTH, 1);
            // also we need to reset the time
            resetTimeToFirstValues(nextCal);
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

    private Calendar computeNextYear(Calendar nextCal) {
        Integer nextYear = this.year.getNextMatch(nextCal);

        if (nextYear == null || nextYear > Year.MAX_YEAR) {
            return null;
        }
        int currentYear = nextCal.get(Calendar.YEAR);
        // if the current year is a match, then nothing else to
        // do. Just return back the calendar
        if (currentYear == nextYear) {
            return nextCal;
        }
        // If the next year is lesser than the current year, then
        // we have no more timeouts for the calendar expression
        if (nextYear < currentYear) {
            return null;
        }

        // at this point we have chosen a year which is greater than the current
        // year.
        // set the chosen year
        nextCal.set(Calendar.YEAR, nextYear);
        // since we are moving to a different year (as compared to the current year),
        // we should reset all other calendar attribute expressions appropriately, to their first possible
        // values
        nextCal.set(Calendar.MONTH, this.month.getFirstMatch());
        nextCal.set(Calendar.DAY_OF_MONTH, 1);
        resetTimeToFirstValues(nextCal);

        // recompute date
        nextCal = this.computeNextDate(nextCal);

        return nextCal;
    }

    private Calendar advanceTillMonthHasDate(Calendar cal, Integer date) {
        resetTimeToFirstValues(cal);

        // make sure the month can handle the date
        while (monthHasDate(cal, date) == false) {
            if (cal.get(Calendar.YEAR) > Year.MAX_YEAR) {
                return null;
            }
            // this month can't handle the date, so advance month to next month
            // and get the next suitable matching month
            cal.add(Calendar.MONTH, 1);
            cal = this.computeNextMonth(cal);
            if (cal == null) {
                return null;
            }
            date = this.dayOfMonth.getFirstMatch(cal);
            if (date == null) {
                return null;
            }
        }
        cal.set(Calendar.DAY_OF_MONTH, date);
        return cal;
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
            throw EJB3_TIMER_LOGGER.invalidScheduleExpressionSecond(schedule);
        }
        if (schedule.getMinute() == null) {
            throw EJB3_TIMER_LOGGER.invalidScheduleExpressionMinute(schedule);
        }
        if (schedule.getHour() == null) {
            throw EJB3_TIMER_LOGGER.invalidScheduleExpressionHour(schedule);
        }
        if (schedule.getDayOfMonth() == null) {
            throw EJB3_TIMER_LOGGER.invalidScheduleExpressionDayOfMonth(schedule);
        }
        if (schedule.getDayOfWeek() == null) {
            throw EJB3_TIMER_LOGGER.invalidScheduleExpressionDayOfWeek(schedule);
        }
        if (schedule.getMonth() == null) {
            throw EJB3_TIMER_LOGGER.invalidScheduleExpressionMonth(schedule);
        }
        if (schedule.getYear() == null) {
            throw EJB3_TIMER_LOGGER.invalidScheduleExpressionYear(schedule);
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

    /**
     *
     * @param calendar
     */
    private void resetTimeToFirstValues(Calendar calendar) {
        final int currentHour = calendar.get(Calendar.HOUR_OF_DAY);
        final int currentMinute = calendar.get(Calendar.MINUTE);
        final int currentSecond = calendar.get(Calendar.SECOND);
        final int firstHour = this.hour.getFirst();
        final int firstMinute = this.minute.getFirst();
        final int firstSecond = this.second.getFirst();
        if (currentHour != firstHour || currentMinute != firstMinute || currentSecond != firstSecond) {
            setTime(calendar, firstHour, firstMinute, firstSecond);
        }
    }

    private void setTime(Calendar calendar, int hour, int minute, int second) {
        int dst = calendar.get(Calendar.DST_OFFSET);
        calendar.clear(Calendar.HOUR_OF_DAY);
        calendar.set(Calendar.HOUR_OF_DAY, hour);
        calendar.clear(Calendar.MINUTE);
        calendar.set(Calendar.MINUTE, minute);
        calendar.clear(Calendar.SECOND);
        calendar.set(Calendar.SECOND, second);
        // restore summertime offset WFLY-9537
        // this is to avoid to have the standard time (winter) set by GregorianCalendar
        // after clear and set the time explicit
        // see comment for computeTime() -> http://grepcode.com/file/repository.grepcode.com/java/root/jdk/openjdk/8-b132/java/util/GregorianCalendar.java#2776
        calendar.set(Calendar.DST_OFFSET, dst);
    }

}
