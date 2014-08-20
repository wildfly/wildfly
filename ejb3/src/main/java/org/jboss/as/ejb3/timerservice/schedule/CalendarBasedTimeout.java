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
import java.util.HashMap;
import java.util.Map;
import java.util.SimpleTimeZone;
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
import org.jboss.as.ejb3.timerservice.schedule.util.CalendarUtil;

import static org.jboss.as.ejb3.logging.EjbLogger.ROOT_LOGGER;

/**
 * CalendarBasedTimeout is responsible for computing timeouts using a {@link javax.ejb.ScheduleExpression},
 * which is modeled after UNIX tool named cron.
 *
 * Note that only the traditional cron behaviour is provided:
 *  - timeouts falling in the DST rollback repeated time periods, are not skipped
 *  - timeouts falling in the DST forward skipped time periods are skipped
 *
 * @author Eduardo Martins
 * @author Jaikiran Pai
 * @author "<a href=\"mailto:wfink@redhat.com\">Wolf-Dieter Fink</a>"
 * @version $Revision: $
 */
public class CalendarBasedTimeout {

    /**
     * a map caching timezone clones, but without DST savings
     */
    private static final Map<String, TimeZone> TIMEZONES_WITHOUT_DST = createTimezonesWithoutDstMap();

    private static Map<String, TimeZone> createTimezonesWithoutDstMap() {
        Map<String, TimeZone> map = new HashMap<>();
        String[] availableTimeZoneIDs = TimeZone.getAvailableIDs();
        if (availableTimeZoneIDs != null) {
            for (String timeZoneID : availableTimeZoneIDs) {
                TimeZone timeZone = TimeZone.getTimeZone(timeZoneID);
                if (timeZone.useDaylightTime()) {
                    map.put(timeZoneID, new SimpleTimeZone(timeZone.getRawOffset(), timeZoneID+" without DST Savings"));
                }
            }
        }
        return map;
    }

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
    private final TimeZone timezone;

    /**
     * The timezone's clone without DST savings
     */
    private final TimeZone timezoneWithoutDstSavings;

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
            // timezone ids in the system. If it's not available then we log a WARN message
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
        if (this.timezone.useDaylightTime()) {
            TimeZone timezoneWithoutDstSavings = TIMEZONES_WITHOUT_DST.get(this.timezone.getID());
            if (timezoneWithoutDstSavings != null) {
                this.timezoneWithoutDstSavings = timezoneWithoutDstSavings;
            } else {
                this.timezoneWithoutDstSavings = new SimpleTimeZone(timezone.getRawOffset(), timezone.getID()+" without DST Savings");
            }
        } else {
            this.timezoneWithoutDstSavings = this.timezone;
        }

        // Now that we have parsed the values from the ScheduleExpression,
        // determine and set the first timeout (relative to the current time)
        // of this CalendarBasedTimeout
        Calendar calendar = new GregorianCalendar(this.timezone);
        Date start = this.scheduleExpression.getStart();
        if (start != null) {
            calendar.setTime(start);
        } else {
            // use valid time's first values
            resetCalendarHourToFirst(calendar);
            resetCalendarMinuteToFirst(calendar);
            resetCalendarSecondToFirst(calendar);
        }
        this.firstTimeout = getNextTimeout(calendar, true);
    }

    /**
     * @return
     */
    public Calendar getFirstTimeout() {
        return firstTimeout != null ? (Calendar) this.firstTimeout.clone() : null;
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

    public Calendar getNextTimeout() {
        return getNextTimeout(new GregorianCalendar(this.timezone));
    }

    public Calendar getNextTimeout(Calendar calendar) {
        Date start = this.scheduleExpression.getStart();
        if (start != null && calendar.getTime().before(start)) {
            return getFirstTimeout();
        } else {
            return getNextTimeout(calendar, false);
        }
    }

    /**
     * Computes next timeout after the specified one.
     *
     * @param currentCal the current cal
     * @param includes if the current cal should be considered a candidate for next timeout
     * @return
     */
    private Calendar getNextTimeout(Calendar currentCal, boolean includes) {

        // ensures there are timeouts after currentCal
        if (this.noMoreTimeouts(currentCal)) {
            return null;
        }

        // 1st next cal candidate is the current cal clone
        Calendar nextCal = (Calendar) currentCal.clone();
        // ensure correct time zone
        nextCal.setTimeZone(this.timezone);
        // ensure 1st day of week is sunday
        nextCal.setFirstDayOfWeek(Calendar.SUNDAY);
        // roll millisecond to 0, it's not used by schedule expressions
        setCalendarMillisecond(nextCal, 0);

        if (timezone.useDaylightTime()) {
            nextCal = getNextTimeoutOnTimeZoneWithDST(nextCal, includes);
        } else {
            // calendar on timezone without dst, just compute next timeout
            if (!includes) {
                // but first, if current call is not a candidate for nextCal then advance time a second
                nextCal.add(Calendar.SECOND, 1);
            }
            nextCal = computeNextTimeout(nextCal);
        }

        // a final check to ensure next cal did not went over end date
        if (nextCal != null && this.noMoreTimeouts(nextCal)) {
            nextCal = null;
        }

        return nextCal;
    }


    /**
     * Computes next timeout after the specified one.
     *
     * @param currentCal the current cal
     * @param includes if the current cal should be considered a candidate for next timeout
     * @return
     */
    private Calendar getNextTimeoutOnTimeZoneWithDST(Calendar currentCal, boolean includes) {

        // create current cal clone on the timezone with dst
        Calendar nextCalOnThisTimezone = (Calendar) currentCal.clone();

        // create current cal clone on timezone without dst
        Calendar nextCalOnTimezoneWithoutDST = (Calendar) currentCal.clone();
        nextCalOnTimezoneWithoutDST.setTimeZone(timezoneWithoutDstSavings);
        nextCalOnTimezoneWithoutDST.setTimeInMillis(currentCal.getTimeInMillis() + currentCal.get(Calendar.DST_OFFSET));

        // compute next timeout
        if (!includes) {
            // but first, if current call is not a candidate then advance time a second
            nextCalOnTimezoneWithoutDST.add(Calendar.SECOND, 1);
        }
        nextCalOnTimezoneWithoutDST = computeNextTimeout(nextCalOnTimezoneWithoutDST);
        if (nextCalOnTimezoneWithoutDST == null) {
            return null;
        }

        // copy computed timeout to calendar in timezone with dst
        nextCalOnThisTimezone.setTimeInMillis(nextCalOnTimezoneWithoutDST.getTimeInMillis());
        // compare time between both calendars
        if (CalendarUtil.compareCalendarDateAndTime(nextCalOnTimezoneWithoutDST, nextCalOnThisTimezone) == 0) {
            // no change in time, which means that both calendars are on STD at the computed time
            if (CalendarUtil.inDSTRollbackRepeatedPeriod(nextCalOnThisTimezone)) {
                // but computed timeout is in the DST rollback repeated period, resolve time ambiguity by comparing current cal with the DST version of computed time
                long currentCalTime = currentCal.getTimeInMillis();
                long nextCalOnThisTimezoneDSTTime = nextCalOnThisTimezone.getTimeInMillis() - timezone.getDSTSavings();
                if (currentCalTime < nextCalOnThisTimezoneDSTTime || (currentCalTime == nextCalOnThisTimezoneDSTTime && includes)) {
                    // current cal is before, or same time and current cal is a valid result, opt for DST
                    nextCalOnThisTimezone.set(Calendar.DST_OFFSET, timezone.getDSTSavings());
                }
            }
        } else {
            // times do not match, which means the timezone is on DST for the computed time, remove the DST offset to obtain same time
            nextCalOnThisTimezone.add(Calendar.MILLISECOND, -timezone.getDSTSavings());
            // confirm time matches
            if (CalendarUtil.compareCalendarDateAndTime(nextCalOnThisTimezone, nextCalOnTimezoneWithoutDST) != 0) {
                // time still does not match, which means there is no such time, the computed timeout is in the DST forward skipped period, compute first one after skipped period
                CalendarUtil.advanceCalendarTillDST(nextCalOnThisTimezone);
                nextCalOnThisTimezone = getNextTimeout(nextCalOnThisTimezone, true);
            }
        }

        if (nextCalSkippedDSTRollbackRepeatedPeriod(currentCal, nextCalOnThisTimezone)) {
            // oops, next cal went over a DST rollback repeated period which has timeouts
            // advance current call till 1st minute on STD
            CalendarUtil.advanceCalendarTillSTD(currentCal);
            // recompute next timeout since that minute (inclusive)
            nextCalOnTimezoneWithoutDST.setTimeInMillis(currentCal.getTimeInMillis());
            nextCalOnTimezoneWithoutDST = computeNextTimeout(nextCalOnTimezoneWithoutDST);
            nextCalOnThisTimezone.setTimeInMillis(nextCalOnTimezoneWithoutDST.getTimeInMillis());
        }

        return nextCalOnThisTimezone;
    }

    /**
     * Computes next timeout, starting with the provided calendar.
     *
     * Computation is done by computing each field separately, starting with the time's second, till the date's year.
     * Note that this computation is only valid when used with timezones without DST changes. Computation by setting
     * time fields using timezones with DST changes are incorrect, a very inefficient time advance/ticking by minute is
     * needed, since at any point in time there may occur a variable (30min, 1h, ...) DST rollback or forward.
     *
     * Also note that specified calendar is considered a candidate for the computation result.
     *
     * @param nextCal
     * @return
     */
    private Calendar computeNextTimeout(Calendar nextCal) {
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

        return nextCal;
    }

    private Calendar computeNextSecond(Calendar currentCal) {
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

        // At this point, a suitable "next" second has been identified.
        // There can be 2 cases
        // 1) The "next" second is greater than the current second : This
        // implies that the next second is within the "current" minute.
        // 2) The "next" second is lesser than the current second : This implies
        // that the next second is in the next minute (i.e. current minute needs to
        // be advanced to next minute).

        // set the chosen second
        setCalendarSecond(currentCal, nextSecond);
        // case#2
        if (nextSecond < currentSecond) {
            // advance the minute to next minute
            currentCal.add(Calendar.MINUTE, 1);
        }

        return currentCal;
    }

    private Calendar computeNextMinute(Calendar currentCal) {
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

        // At this point, a suitable "next" minute has been identified.
        // There can be 2 cases
        // 1) The "next" minute is greater than the current minute : This
        // implies that the next minute is within the "current" hour.
        // 2) The "next" minute is lesser than the current minute : This implies
        // that the next minute is in the next hour (i.e. current hour needs to
        // be advanced to next hour).

        // set the chosen minute
        setCalendarMinute(currentCal, nextMinute);
        // since we are moving to a different minute (as compared to the current minute),
        // we should reset the second, to its first possible value
        resetCalendarSecondToFirst(currentCal);
        // case#2
        if (nextMinute < currentMinute) {
            // advance the hour to next hour
            currentCal.add(Calendar.HOUR_OF_DAY, 1);
        }
        return currentCal;
    }

    private Calendar computeNextHour(Calendar currentCal) {
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

        // At this point, a suitable "next" hour has been identified.
        // There can be 2 cases
        // 1) The "next" hour is greater than the current hour : This
        // implies that the next hour is within the "current" day.
        // 2) The "next" hour is lesser than the current hour : This implies
        // that the next hour is in the next day (i.e. current day needs to
        // be advanced to next day).

        // set the chosen day of hour
        setCalendarHour(currentCal, nextHour);
        // since we are moving to a different hour (as compared to the current hour),
        // we should reset the second and minute appropriately, to their first possible
        // values
        resetCalendarSecondToFirst(currentCal);
        resetCalendarMinuteToFirst(currentCal);
        // case#2
        if (nextHour < currentHour) {
            // advance to next day
            currentCal.add(Calendar.DATE, 1);
        }

        return currentCal;
    }

    private Calendar computeNextDayOfWeek(Calendar currentCal) {
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
        int currentMonth = currentCal.get(Calendar.MONTH);

        // At this point, a suitable "next" day-of-week has been identified.
        // There can be 2 cases
        // 1) The "next" day-of-week is greater than the current day-of-week : This
        // implies that the next day-of-week is within the "current" week.
        // 2) The "next" day-of-week is lesser than the current day-of-week : This implies
        // that the next day-of-week is in the next week (i.e. current week needs to
        // be advanced to next week).

        // set the chosen day of week
        currentCal.set(Calendar.DAY_OF_WEEK, nextDayOfWeek);
        // since we are moving to a different day-of-week (as compared to the current day-of-week),
        // we should reset the second, minute and hour appropriately, to their first possible
        // values
        resetCalendarHourToFirst(currentCal);
        resetCalendarMinuteToFirst(currentCal);
        resetCalendarSecondToFirst(currentCal);
        // case#2
        if (nextDayOfWeek < currentDayOfWeek) {
            // advance one week
            currentCal.add(Calendar.WEEK_OF_MONTH, 1);
        }
        if (currentCal.get(Calendar.MONTH) != currentMonth) {
            currentCal = computeNextMonth(currentCal);
        }
        return currentCal;
    }

    private Calendar computeNextMonth(Calendar currentCal) {
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

        // At this point, a suitable "next" month has been identified.
        // There can be 2 cases
        // 1) The "next" month is greater than the current month : This
        // implies that the next month is within the "current" year.
        // 2) The "next" month is lesser than the current month : This implies
        // that the next month is in the next year (i.e. current year needs to
        // be advanced to next year).

        // case#2
        if (nextMonth < currentMonth) {
            // advance to next year
            currentCal.add(Calendar.YEAR, 1);
        }
        // set the chosen month
        currentCal.set(Calendar.MONTH, nextMonth);
        // since we are moving to a different month (as compared to the current month),
        // we should reset the second, minute, hour, day-of-week and dayofmonth appropriately, to their first possible
        // values
        resetCalendarSecondToFirst(currentCal);
        resetCalendarMinuteToFirst(currentCal);
        resetCalendarHourToFirst(currentCal);
        // note, day of month/week must be computed elsewhere
        currentCal.set(Calendar.DAY_OF_MONTH, 1);

        return currentCal;
    }

    private Calendar computeNextDate(Calendar currentCal) {
        if (this.isDayOfMonthWildcard()) {
            return this.computeNextDayOfWeek(currentCal);
        }

        if (this.isDayOfWeekWildcard()) {
            return this.computeNextDayOfMonth(currentCal);
        }

        // both day-of-month and day-of-week are *non-wildcards*
        Calendar nextDayOfMonthCal = this.computeNextDayOfMonth((Calendar) currentCal.clone());
        Calendar nextDayOfWeekCal = this.computeNextDayOfWeek((Calendar) currentCal.clone());

        if (nextDayOfMonthCal == null) {
            return nextDayOfWeekCal;
        }
        if (nextDayOfWeekCal == null) {
            return nextDayOfMonthCal;
        }

        return nextDayOfWeekCal.getTime().before(nextDayOfMonthCal.getTime()) ? nextDayOfWeekCal : nextDayOfMonthCal;

    }

    private Calendar computeNextDayOfMonth(Calendar currentCal) {
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

        if (nextDayOfMonth > currentDayOfMonth) {
            if (this.monthHasDate(currentCal, nextDayOfMonth)) {
                // set the chosen day-of-month
                currentCal.set(Calendar.DAY_OF_MONTH, nextDayOfMonth);
                // since we are moving to a different day-of-month (as compared to the current day-of-month),
                // we should reset the second, minute and hour appropriately, to their first possible
                // values
                resetCalendarSecondToFirst(currentCal);
                resetCalendarMinuteToFirst(currentCal);
                resetCalendarHourToFirst(currentCal);
            } else {
                currentCal = this.advanceTillMonthHasDate(currentCal, nextDayOfMonth);
            }
        } else {
            // since the next day is before the current day we need to shift to the next month
            currentCal.add(Calendar.MONTH, 1);
            // also we need to reset the time
            resetCalendarSecondToFirst(currentCal);
            resetCalendarMinuteToFirst(currentCal);
            resetCalendarHourToFirst(currentCal);
            currentCal = this.computeNextMonth(currentCal);
            if (currentCal == null) {
                return null;
            }
            nextDayOfMonth = this.dayOfMonth.getFirstMatch(currentCal);
            if (nextDayOfMonth == null) {
                return null;
            }
            // make sure the month can handle the date
            currentCal = this.advanceTillMonthHasDate(currentCal, nextDayOfMonth);
        }
        return currentCal;
    }


    private Calendar computeNextYear(Calendar currentCal) {
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

        // at this point we have chosen a year which is greater than the current
        // year.
        // set the chosen year
        currentCal.set(Calendar.YEAR, nextYear);
        // since we are moving to a different year (as compared to the current year),
        // we should reset all other calendar attribute expressions appropriately, to their first possible
        // values
        resetCalendarSecondToFirst(currentCal);
        resetCalendarMinuteToFirst(currentCal);
        resetCalendarHourToFirst(currentCal);
        currentCal.set(Calendar.MONTH, this.month.getFirstMatch());
        currentCal.set(Calendar.DAY_OF_MONTH, 1);
        currentCal = this.computeNextDate(currentCal);

        return currentCal;
    }

    private Calendar advanceTillMonthHasDate(Calendar cal, Integer date) {
        resetCalendarSecondToFirst(cal);
        resetCalendarMinuteToFirst(cal);
        resetCalendarHourToFirst(cal);

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

    /**
     * Resets the calendar's hour field to the first valid value.
     * @param calendar
     */
    private void resetCalendarHourToFirst(Calendar calendar) {
        setCalendarHour(calendar, this.hour.getFirst());
    }

    /**
     * Resets the calendar's minute field to the first valid value.
     * @param calendar
     */
    private void resetCalendarMinuteToFirst(Calendar calendar) {
        setCalendarMinute(calendar, this.minute.getFirst());
    }

    /**
     * Resets the calendar's second field to the first valid value.
     * @param calendar
     */
    private void resetCalendarSecondToFirst(Calendar calendar) {
        setCalendarSecond(calendar, this.second.getFirst());
    }

    /**
     * Sets the calendar's hour field to the specified value.
     * @param calendar
     * @param value
     */
    private static void setCalendarHour(Calendar calendar, int value) {
        // setting fields with 'add' works around ambiguous times (std vs dst)
        calendar.add(Calendar.HOUR_OF_DAY, value - calendar.get(Calendar.HOUR_OF_DAY));
    }

    /**
     * Sets the calendar's minute field to the specified value.
     * @param calendar
     * @param value
     */
    private static void setCalendarMinute(Calendar calendar, int value) {
        // setting fields with 'add' works around ambiguous times (std vs dst)
        calendar.add(Calendar.MINUTE, value - calendar.get(Calendar.MINUTE));
    }

    /**
     * Sets the calendar's second field to the specified value.
     * @param calendar
     * @param value
     */
    private static void setCalendarSecond(Calendar calendar, int value) {
        // setting fields with 'add' works around ambiguous times (std vs dst)
        calendar.add(Calendar.SECOND, value - calendar.get(Calendar.SECOND));
    }

    /**
     * Sets the calendar's millisecond field to the specified value.
     * @param calendar
     * @param value
     */
    private static void setCalendarMillisecond(Calendar calendar, int value) {
        // setting fields with 'add' works around ambiguous times (std vs dst)
        calendar.add(Calendar.MILLISECOND, value - calendar.get(Calendar.MILLISECOND));
    }

    /**
     *
     * @param currentCal
     * @param nextCal
     * @return true if current cal is a DST version of a DST rollback repeated time, and next cal is after the related DST rollback
     */
    private static boolean nextCalSkippedDSTRollbackRepeatedPeriod(Calendar currentCal, Calendar nextCal) {
        int currentCalDstOffset = currentCal.get(Calendar.DST_OFFSET);
        if (currentCalDstOffset != 0) {
            // currentCal on DST
            long currentCalTimePlusDstOffset = currentCal.getTimeInMillis() + currentCalDstOffset;
            if(CalendarUtil.inDSTRollbackRepeatedPeriod(currentCalTimePlusDstOffset, currentCal.getTimeZone())) {
                // adding the DST offset to currentCal puts it in DST rollback repeated period
                if (nextCal.get(Calendar.DST_OFFSET) == 0 || (nextCal.getTimeInMillis() > currentCalTimePlusDstOffset)) {
                    // next cal is on STD or in a different DST period
                    return true;
                }
            }
        }
        return false;
    }

}
