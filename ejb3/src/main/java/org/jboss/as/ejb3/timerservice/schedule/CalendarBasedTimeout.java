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

import static org.jboss.as.ejb3.logging.EjbLogger.ROOT_LOGGER;

/**
 * CalendarBasedTimeout is responsible for computing timeouts using a {@link javax.ejb.ScheduleExpression},
 * which is modeled after UNIX tool named cron.
 *
 * Timeout computation handles DST changes depending on the schedule expression.
 *
 * When schedule expressions are used without specific hours, i.e. when schedule expression's hour is set with the wildcard value, then the traditional cron behaviour is used:
 *  - timeouts falling in the DST rollback repeated time periods, are not skipped
 *  - timeouts falling in the DST forward skipped time periods are skipped
 *
 * When schedule expressions are used with specific hours, then the modern cron behaviour is used, which in most situations results in same behavior as not having DST changes:
 *  - timeouts falling in the DST forward skipped time periods are not skipped, these are replaced with timeouts by adding the DST forward period to the original timeout time
 *  - timeouts falling in the DST rollback repeated time periods (STD) are skipped.
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
     * indicates if the schedule expression allows any hour timeouts
     */
    private final boolean anyHourScheduleExpression;

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

        this.anyHourScheduleExpression = scheduleExpression.getHour().equals("*");

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

        // ensure there are timeouts after currentCal
        if (this.noMoreTimeouts(currentCal)) {
            return null;
        }

        // clone current cal and ensure correct timezone
        Calendar nextCalOnThisTimezone = (Calendar) currentCal.clone();
        nextCalOnThisTimezone.setTimeZone(this.timezone);
        // ensure 1st day of week is sunday
        nextCalOnThisTimezone.setFirstDayOfWeek(Calendar.SUNDAY);
        // roll millisecond to 0, it's not used by schedule expressions
        setCalendarMillisecond(nextCalOnThisTimezone, 0);

        // the cal that will hold next timeout, for now pointing to the clone
        Calendar nextCal = nextCalOnThisTimezone;

        // if the time zone does DST changes let's use instead (for computing next timeout) a calendar in the timezone
        // clone without dst changes
        Calendar nextCalOnTimezoneWithoutDST = null;
        if (timezone.useDaylightTime()) {
            // create calendar on timezone without dst
            nextCalOnTimezoneWithoutDST = (Calendar) nextCal.clone();
            nextCalOnTimezoneWithoutDST.setTimeZone(timezoneWithoutDstSavings);
            // copy time to calendar without dst
            nextCalOnTimezoneWithoutDST.setTimeInMillis(nextCal.getTimeInMillis());
            if (anyHourScheduleExpression || !inDSTForwardPeriod(nextCal)) {
                // when copying time from a dst timezone the dst offset needs to be added to achieve same time,
                // but if DST changes are being *handled* similar to modern cron, and nextCalOnThisTimezone is in DST
                // forward period, then may exist skipped timeouts yet to handle, thus only in such case don't add dst savings,
                // to start computing next timeout from skipped time any repeated timeout is handled later
                nextCalOnTimezoneWithoutDST.add(Calendar.MILLISECOND, nextCal.get(Calendar.DST_OFFSET));
            }
            nextCal = nextCalOnTimezoneWithoutDST;
        }

        if (!includes) {
            // if current call is not a candidate for nextCal then advance time a second
            nextCal.add(Calendar.SECOND, 1);
        }
        // and compute next timeout
        nextCal = computeNextTimeout(nextCal);

        // post processing of nextCal
        if (nextCal != null) {
            if (nextCalOnTimezoneWithoutDST != null) {
                // timeout was computed using a timezone without DST changes
                // copy time among calendars and point nextCal to the one with correct timezone
                nextCalOnThisTimezone.setTimeInMillis(nextCal.getTimeInMillis());
                nextCal = nextCalOnThisTimezone;
                // compare time between both calendars
                if (compareCalendarDateAndTime(nextCalOnTimezoneWithoutDST, nextCalOnThisTimezone) == 0) {
                    // no change in time, which means that both calendars are on STD at the computed time
                    if (anyHourScheduleExpression) {
                        // ignoring DST changes means there will be, due to DST rollbacks, repeated timeouts for ambiguous times (DST and STD)
                        // since computation of next timeout is on a timezone without dst changes, when copying an ambiguous time to the correct timezone will always result in the STD version, so when that happens we select the DST version first
                        // (unless that DST version is not after current timeout, then assume we are now in the STD rollback period and use STD)
                        // to detect when to start the timeouts on STD we must look at the current cal, if it's an ambiguous time on STD and nextCal is after the DST rollback period, then we must revert to first repeated timeout
                        int currentCalDstOffset = currentCal.get(Calendar.DST_OFFSET);
                        if (currentCalDstOffset != 0 && !inDSTRollbackRepeatedPeriod(nextCal)) {
                            // currentCal on DST and nextCal not on DST rollback period
                            long currentCalTimePlusDstOffset = currentCal.getTimeInMillis()+currentCalDstOffset;
                            if(inDSTRollbackRepeatedPeriod(currentCalTimePlusDstOffset, currentCal.getTimeZone())) {
                                // but adding the dst offset to currentCal puts it in DST rollback period, go back in time minute by minute, till time when DST rollback period started is found
                                long dstRollbackPeriodStartTime = currentCalTimePlusDstOffset;
                                while (true) {
                                    if (currentCal.getTimeZone().inDaylightTime(new Date(dstRollbackPeriodStartTime-60000))) {
                                        break;
                                    }
                                    dstRollbackPeriodStartTime -= 60000;
                                }
                                // load it to calendar without DST
                                nextCalOnTimezoneWithoutDST.setTimeInMillis(dstRollbackPeriodStartTime);
                                // reset seconds field
                                resetCalendarSecondToFirst(nextCalOnTimezoneWithoutDST);
                                // compute next timeout since that minute (inclusive)
                                nextCalOnTimezoneWithoutDST = computeNextTimeout(nextCalOnTimezoneWithoutDST);
                                // load it to nextCal and we are done, we got the first repeated timeout
                                nextCal.setTimeInMillis(nextCalOnTimezoneWithoutDST.getTimeInMillis());
                            }
                        } else {
                            // nextCal is on DST rollback period, set nextCal DST offset to the one in currentCal
                            nextCal.set(Calendar.DST_OFFSET, currentCal.get(Calendar.DST_OFFSET));
                        }
                    } else {
                        // handling DST changes, there are no repeated timeouts, which means we only compute the earliest versions, i.e. the ones on DST
                        if (inDSTRollbackRepeatedPeriod(nextCal)) {
                            // in DST rollback period, revert to the DST version of the time
                            nextCal.set(Calendar.DST_OFFSET, timezone.getDSTSavings());
                            // now compare with current cal
                            int compareResult = compareCalendarDateAndTime(nextCal, currentCal);
                            if ((compareResult == 0 && !includes) || compareResult < 0) {
                                // nextCal is before currentCal, or it's equal but computation does not allows that, move on to next timeout
                                nextCal = getNextTimeout(nextCal, false);
                            }
                        }
                    }
                } else {
                    // times do not match, i.e. time went forward due to DST change
                    // remove the DST offset from nextCal to obtain same time
                    Calendar nextCalMinusDstSavings = (Calendar) nextCal.clone();
                    nextCalMinusDstSavings.add(Calendar.MILLISECOND, -timezone.getDSTSavings());
                    // compare it with nextCalOnTimezoneWithoutDST
                    if (compareCalendarDateAndTime(nextCalMinusDstSavings, nextCalOnTimezoneWithoutDST) == 0) {
                        // now times match as expected, and nextCalMinusDstSavings is our next timeout
                        nextCal = nextCalMinusDstSavings;
                        // still if handling DST changes and in DST forward period, it's possible the computed timeout overlapped with the
                        // replacement for a skipped timeout and was already used (i.e. it's not after currentCal)
                        // in such case advance to next timeout
                        if (!anyHourScheduleExpression && inDSTForwardPeriod(nextCal) && compareCalendarDateAndTime(nextCal, currentCal) < 1) {
                            nextCal = computeNextTimeoutNotOnSkippedInterval(currentCal, nextCalOnTimezoneWithoutDST);
                        }
                    } else {
                        // times continue to not match, which means there is no such time, nextCalOnTimezoneWithoutDST in the dst forward skipped period
                        Calendar nextNextCal = computeNextTimeoutNotOnSkippedInterval(currentCal, nextCalOnTimezoneWithoutDST);
                        if (!anyHourScheduleExpression) {
                            // when handling DST changes let's accept nextCal (not nextCalMinusDstSavings) as replacement for the skipped timeout, if the first 'not skipped' timeout is after
                            if (nextNextCal != null && compareCalendarDateAndTime(nextNextCal, nextCal) < 1) {
                                // nextNextCal is not after nextCal, use it instead
                                nextCal = nextNextCal;
                            }
                        } else {
                            // if not handling DST changes then timeouts in skipped periods are skipped, let's move to nextNextCal
                            nextCal = nextNextCal;
                        }
                    }
                }
            }
        }

        // final check to ensure next cal did not event over end date
        if (nextCal != null && this.noMoreTimeouts(nextCal)) {
            return null;
        }

        return nextCal;
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
     * @param calendar
     * @return true if the calendar is in the period after dst forward; false otherwise
     */
    private boolean inDSTForwardPeriod(Calendar calendar) {
        final long time = calendar.getTimeInMillis();
        final TimeZone timeZone = calendar.getTimeZone();
        return timeZone.inDaylightTime(new Date(time)) && !timeZone.inDaylightTime(new Date(time - timeZone.getDSTSavings()));
    }

    /**
     *
     * @param calendar
     * @return true if the calendar is in the repeated period after a dst rollback; false otherwise
     */
    private boolean inDSTRollbackRepeatedPeriod(Calendar calendar) {
        final long time = calendar.getTimeInMillis();
        final TimeZone timeZone = calendar.getTimeZone();
        return inDSTRollbackRepeatedPeriod(time, timeZone);
    }

    /**
     *
     * @param time
     * @param timeZone
     * @return true if the specified time is in the repeated period after a dst rollback; false otherwise
     */
    private boolean inDSTRollbackRepeatedPeriod(long time, TimeZone timeZone) {
        return !timeZone.inDaylightTime(new Date(time)) && timeZone.inDaylightTime(new Date(time - timeZone.getDSTSavings()));
    }

    /**
     * @param currentCal the original timeout, in the timezone with dst changes
     * @param nextCalOnTimezoneWithoutDST the next cal candidate, in a timezone without dst changes
     * @return next timeout in currentCal's timezone, after nextCalOnTimezoneWithoutDST, which is not in currentCal's
     *  timezone dst forward skipped period, and is after currentCal's; or null if there is no such timeout
     */
    private Calendar computeNextTimeoutNotOnSkippedInterval(Calendar currentCal, Calendar nextCalOnTimezoneWithoutDST) {
        Calendar nextNextCalOnTimezoneWithoutDST = (Calendar) nextCalOnTimezoneWithoutDST.clone();
        Calendar nextNextCal = (Calendar) currentCal.clone();
        do {
            nextNextCalOnTimezoneWithoutDST.add(Calendar.SECOND, 1);
            nextNextCalOnTimezoneWithoutDST = computeNextTimeout(nextNextCalOnTimezoneWithoutDST);
            if (nextNextCalOnTimezoneWithoutDST == null) {
                return null;
            }
            nextNextCal.setTimeInMillis(nextNextCalOnTimezoneWithoutDST.getTimeInMillis());
            nextNextCal.add(Calendar.MILLISECOND, -timezone.getDSTSavings());
            // continue to next timeout unless it's not a skipped one or it's not after current cal
        } while (compareCalendarDateAndTime(nextNextCalOnTimezoneWithoutDST, nextNextCal) != 0 || compareCalendarDateAndTime(currentCal, nextNextCal) > -1);
        return nextNextCal;
    }

    /**
     * Compares two {@code Calendar} fields, except timezone and dst offsets.
     *
     * @param  x the first {@code int} to compare
     * @param  y the second {@code int} to compare
     * @return the value {@code 0} if {@code x == y};
     *         a value less than {@code 0} if {@code x < y}; and
     *         a value greater than {@code 0} if {@code x > y}
     */
    private static int compareCalendarDateAndTime(Calendar x, Calendar y) {
        int compareResult = Integer.compare(x.get(Calendar.YEAR), y.get(Calendar.YEAR));
        if (compareResult != 0) {
            return compareResult;
        }
        compareResult = Integer.compare(x.get(Calendar.MONTH), y.get(Calendar.MONTH));
        if (compareResult != 0) {
            return compareResult;
        }
        compareResult = Integer.compare(x.get(Calendar.DAY_OF_MONTH), y.get(Calendar.DAY_OF_MONTH));
        if (compareResult != 0) {
            return compareResult;
        }
        compareResult = Integer.compare(x.get(Calendar.HOUR_OF_DAY), y.get(Calendar.HOUR_OF_DAY));
        if (compareResult != 0) {
            return compareResult;
        }
        compareResult = Integer.compare(x.get(Calendar.MINUTE), y.get(Calendar.MINUTE));
        if (compareResult != 0) {
            return compareResult;
        }
        return Integer.compare(x.get(Calendar.SECOND), y.get(Calendar.SECOND));
    }

}
