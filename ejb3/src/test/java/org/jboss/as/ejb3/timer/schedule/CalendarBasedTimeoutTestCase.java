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
package org.jboss.as.ejb3.timer.schedule;

import javax.ejb.ScheduleExpression;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.TimeZone;

import org.jboss.as.ejb3.timerservice.schedule.CalendarBasedTimeout;
import org.jboss.logging.Logger;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

/**
 * CalendarBasedTimeoutTestCase
 *
 * Ignored because it is slow
 * 
 * @author Jaikiran Pai
 * @version $Revision: $
 */
@Ignore
@RunWith(Parameterized.class)
public class CalendarBasedTimeoutTestCase {

    /**
     * Logger
     */
    private static Logger logger = Logger.getLogger(CalendarBasedTimeoutTestCase.class);

    /**
     * The timezone which will be used during the tests
     */
    private TimeZone timezone;

    /**
     * Construct a testcase for the timezone
     */
    public CalendarBasedTimeoutTestCase(TimeZone timezone) {
        this.timezone = timezone;
    }

    /**
     * This method returns a collection of all available timezones in the system.
     * The tests in this {@link CalendarBasedTimeoutTestCase} will then be run
     * against each of these timezones
     */
    @Parameters
    public static Collection<Object[]> getTimezones() {
        String[] allTimezoneIDs = TimeZone.getAvailableIDs();
        Object[][] timezones = new Object[allTimezoneIDs.length][];
        int i = 0;
        for (String timezoneID : allTimezoneIDs) {
            timezones[i++] = new Object[]
                    {TimeZone.getTimeZone(timezoneID)};
        }

        return Arrays.asList(timezones);
    }

    @Test
    public void testEverySecondTimeout() {
        ScheduleExpression everySecondExpression = this.getTimezoneSpecificScheduleExpression();
        everySecondExpression.second("*");
        everySecondExpression.minute("*");
        everySecondExpression.hour("*");

        CalendarBasedTimeout calendarTimeout = new CalendarBasedTimeout(everySecondExpression);

        Calendar firstTimeout = calendarTimeout.getFirstTimeout();
        Calendar nextTimeout = calendarTimeout.getNextTimeout(firstTimeout);

        Assert.assertNotNull("Next timeout is null", nextTimeout);
        Assert.assertNotNull("Next timeout is *before* the current time", nextTimeout.after(firstTimeout));
        logger.debug("Previous timeout was: " + firstTimeout.getTime() + " Next timeout is " + nextTimeout.getTime());
        long diff = nextTimeout.getTimeInMillis() - firstTimeout.getTimeInMillis();
        Assert.assertEquals("Unexpected timeout value: " + nextTimeout, 1 * 1000, diff);
    }

    @Test
    public void testEveryMinuteEveryHourEveryDay() {
        ScheduleExpression everyMinEveryHourEveryDay = this.getTimezoneSpecificScheduleExpression();
        everyMinEveryHourEveryDay.minute("*");
        everyMinEveryHourEveryDay.hour("*");

        CalendarBasedTimeout calendarTimeout = new CalendarBasedTimeout(everyMinEveryHourEveryDay);

        Calendar firstTimeout = calendarTimeout.getFirstTimeout();
        Calendar previousTimeout = firstTimeout;
        for (int i = 1; i <= 65; i++) {
            Calendar nextTimeout = calendarTimeout.getNextTimeout(previousTimeout);

            Assert.assertNotNull("Next timeout is null", nextTimeout);
            Assert.assertNotNull("Next timeout is *before* the current time", nextTimeout.after(previousTimeout));
            logger.debug("First timeout was: " + firstTimeout.getTime() + " Previous timeout was: "
                    + previousTimeout.getTime() + " Next timeout is " + nextTimeout.getTime());
            long diff = nextTimeout.getTimeInMillis() - previousTimeout.getTimeInMillis();
            long diffWithFirstTimeout = nextTimeout.getTimeInMillis() - firstTimeout.getTimeInMillis();
            Assert.assertEquals("Unexpected timeout value: " + nextTimeout, 60 * 1000, diff);
            Assert.assertEquals("Unexpected timeout value when compared to first timeout: " + nextTimeout.getTime(),
                    60 * 1000 * i, diffWithFirstTimeout);

            previousTimeout = nextTimeout;
        }
    }

    @Test
    public void testEveryMorningFiveFifteen() {
        ScheduleExpression everyMorningFiveFifteen = this.getTimezoneSpecificScheduleExpression();
        everyMorningFiveFifteen.minute(15);
        everyMorningFiveFifteen.hour(5);

        CalendarBasedTimeout calendarTimeout = new CalendarBasedTimeout(everyMorningFiveFifteen);

        Calendar firstTimeout = calendarTimeout.getFirstTimeout();

        Assert.assertNotNull("first timeout is null", firstTimeout);
        Date firstTimeoutDate = firstTimeout.getTime();
        int minute = firstTimeout.get(Calendar.MINUTE);
        int second = firstTimeout.get(Calendar.SECOND);
        int hour = firstTimeout.get(Calendar.HOUR_OF_DAY);
        int amOrPm = firstTimeout.get(Calendar.AM_PM);
        Assert.assertEquals("Unexpected second in first timeout " + firstTimeoutDate, 0, second);
        Assert.assertEquals("Unexpected minute in first timeout " + firstTimeoutDate, 15, minute);
        Assert.assertEquals("Unexpected hour in first timeout " + firstTimeoutDate, 5, hour);
        Assert.assertEquals("Unexpected AM/PM in first timeout ", Calendar.AM, amOrPm);

        Calendar previousTimeout = firstTimeout;
        for (int i = 1; i <= 370; i++) {
            Calendar nextTimeout = calendarTimeout.getNextTimeout(previousTimeout);

            Assert.assertNotNull("Next timeout is null", nextTimeout);
            Assert.assertNotNull("Next timeout is *before* the current time", nextTimeout.after(previousTimeout));
            logger.debug("First timeout was: " + firstTimeout.getTime() + " Previous timeout was: "
                    + previousTimeout.getTime() + " Next timeout is " + nextTimeout.getTime());

            Assert.assertEquals("Unexpected second in next timeout " + nextTimeout, 0, nextTimeout.get(Calendar.SECOND));
            Assert.assertEquals("Unexpected minute in next timeout " + nextTimeout, 15, nextTimeout.get(Calendar.MINUTE));

            Assert
                    .assertEquals("Unexpected hour in next timeout " + nextTimeout, 5, nextTimeout.get(Calendar.HOUR_OF_DAY));
            Assert.assertEquals("Unexpected AM/PM in next timeout ", Calendar.AM, nextTimeout.get(Calendar.AM_PM));

            previousTimeout = nextTimeout;

        }
    }

    @Test
    public void testEveryWeekdayEightFifteen() {
        ScheduleExpression everyWeekDayThreeFifteen = this.getTimezoneSpecificScheduleExpression();
        everyWeekDayThreeFifteen.minute(15);
        everyWeekDayThreeFifteen.hour(8);
        everyWeekDayThreeFifteen.dayOfWeek("Mon-Fri");

        CalendarBasedTimeout calendarTimeout = new CalendarBasedTimeout(everyWeekDayThreeFifteen);

        Calendar firstTimeout = calendarTimeout.getFirstTimeout();
        Assert.assertNotNull("first timeout is null", firstTimeout);
        Date firstTimeoutDate = firstTimeout.getTime();
        int minute = firstTimeout.get(Calendar.MINUTE);
        int second = firstTimeout.get(Calendar.SECOND);
        int hour = firstTimeout.get(Calendar.HOUR_OF_DAY);
        int amOrPm = firstTimeout.get(Calendar.AM_PM);
        int dayOfWeek = firstTimeout.get(Calendar.DAY_OF_WEEK);
        Assert.assertEquals("Unexpected second in first timeout " + firstTimeoutDate, 0, second);
        Assert.assertEquals("Unexpected minute in first timeout " + firstTimeoutDate, 15, minute);
        Assert.assertEquals("Unexpected hour in first timeout " + firstTimeoutDate, 8, hour);
        Assert.assertEquals("Unexpected AM/PM in first timeout ", Calendar.AM, amOrPm);
        Assert.assertTrue("Unexpected day of week: " + dayOfWeek + " in first timeout", this.isWeekDay(firstTimeout));

        Calendar previousTimeout = firstTimeout;
        for (int i = 1; i <= 180; i++) {
            Calendar nextTimeout = calendarTimeout.getNextTimeout(previousTimeout);

            Assert.assertNotNull("Next timeout is null", nextTimeout);
            Assert.assertNotNull("Next timeout is *before* the current time", nextTimeout.after(previousTimeout));

            Date nextTimeoutDate = nextTimeout.getTime();
            logger.debug("First timeout was: " + firstTimeoutDate + " Previous timeout was: " + previousTimeout.getTime()
                    + " Next timeout is " + nextTimeoutDate);

            int nextMinute = nextTimeout.get(Calendar.MINUTE);
            int nextSecond = nextTimeout.get(Calendar.SECOND);
            int nextHour = nextTimeout.get(Calendar.HOUR_OF_DAY);
            int nextAmOrPm = nextTimeout.get(Calendar.AM_PM);
            int nextDayOfWeek = nextTimeout.get(Calendar.DAY_OF_WEEK);
            Assert.assertEquals("Unexpected second in next timeout " + nextTimeoutDate, 0, nextSecond);
            Assert.assertEquals("Unexpected minute in next timeout " + nextTimeoutDate, 15, nextMinute);
            Assert.assertEquals("Unexpected hour in next timeout " + nextTimeoutDate, 8, nextHour);
            Assert.assertEquals("Unexpected AM/PM in next timeout ", Calendar.AM, nextAmOrPm);
            Assert
                    .assertTrue("Unexpected day of week: " + nextDayOfWeek + " in next timeout", this.isWeekDay(nextTimeout));

            previousTimeout = nextTimeout;
        }
    }

    @Test
    public void testEveryMonWedFriTwelveThirtyNoon() {
        ScheduleExpression everyMonWedFriTwelveThirtyNoon = this.getTimezoneSpecificScheduleExpression();
        everyMonWedFriTwelveThirtyNoon.hour(12);
        everyMonWedFriTwelveThirtyNoon.second("30");
        everyMonWedFriTwelveThirtyNoon.dayOfWeek("Mon,Wed,Fri");

        CalendarBasedTimeout calendarTimeout = new CalendarBasedTimeout(everyMonWedFriTwelveThirtyNoon);

        Calendar firstTimeout = calendarTimeout.getFirstTimeout();
        Assert.assertNotNull("first timeout is null", firstTimeout);
        Date firstTimeoutDate = firstTimeout.getTime();
        int minute = firstTimeout.get(Calendar.MINUTE);
        int second = firstTimeout.get(Calendar.SECOND);
        int hour = firstTimeout.get(Calendar.HOUR_OF_DAY);
        int amOrPm = firstTimeout.get(Calendar.AM_PM);
        int dayOfWeek = firstTimeout.get(Calendar.DAY_OF_WEEK);
        Assert.assertEquals("Unexpected second in first timeout " + firstTimeoutDate, 30, second);
        Assert.assertEquals("Unexpected minute in first timeout " + firstTimeoutDate, 0, minute);
        Assert.assertEquals("Unexpected hour in first timeout " + firstTimeoutDate, 12, hour);
        Assert.assertEquals("Unexpected AM/PM in first timeout ", Calendar.PM, amOrPm);
        List<Integer> validDays = new ArrayList<Integer>();
        validDays.add(Calendar.MONDAY);
        validDays.add(Calendar.WEDNESDAY);
        validDays.add(Calendar.FRIDAY);
        Assert.assertTrue("Unexpected day of week: " + dayOfWeek + " in first timeout", validDays.contains(dayOfWeek));

        Calendar previousTimeout = firstTimeout;
        for (int i = 1; i <= 180; i++) {
            Calendar nextTimeout = calendarTimeout.getNextTimeout(previousTimeout);

            Assert.assertNotNull("Next timeout is null", nextTimeout);
            Assert.assertNotNull("Next timeout is *before* the current time", nextTimeout.after(previousTimeout));

            Date nextTimeoutDate = nextTimeout.getTime();
            logger.debug("First timeout was: " + firstTimeoutDate + " Previous timeout was: " + previousTimeout.getTime()
                    + " Next timeout is " + nextTimeoutDate);

            int nextMinute = nextTimeout.get(Calendar.MINUTE);
            int nextSecond = nextTimeout.get(Calendar.SECOND);
            int nextHour = nextTimeout.get(Calendar.HOUR_OF_DAY);
            int nextAmOrPm = nextTimeout.get(Calendar.AM_PM);
            int nextDayOfWeek = nextTimeout.get(Calendar.DAY_OF_WEEK);
            Assert.assertEquals("Unexpected second in next timeout " + nextTimeoutDate, 30, nextSecond);
            Assert.assertEquals("Unexpected minute in next timeout " + nextTimeoutDate, 0, nextMinute);
            Assert.assertEquals("Unexpected hour in next timeout " + nextTimeoutDate, 12, nextHour);
            Assert.assertEquals("Unexpected AM/PM in next timeout ", Calendar.PM, nextAmOrPm);
            Assert.assertTrue("Unexpected day of week: " + nextDayOfWeek + " in next timeout", validDays
                    .contains(nextDayOfWeek));
            previousTimeout = nextTimeout;
        }
    }

    @Test
    public void testEvery31stOfTheMonth() {
        ScheduleExpression every31st9_30_15_AM = this.getTimezoneSpecificScheduleExpression();
        every31st9_30_15_AM.dayOfMonth(31);
        every31st9_30_15_AM.hour(9);
        every31st9_30_15_AM.minute("30");
        every31st9_30_15_AM.second(15);

        CalendarBasedTimeout calendarTimeout = new CalendarBasedTimeout(every31st9_30_15_AM);

        Calendar firstTimeout = calendarTimeout.getFirstTimeout();
        Assert.assertNotNull("first timeout is null", firstTimeout);
        Date firstTimeoutDate = firstTimeout.getTime();
        int minute = firstTimeout.get(Calendar.MINUTE);
        int second = firstTimeout.get(Calendar.SECOND);
        int hour = firstTimeout.get(Calendar.HOUR_OF_DAY);
        int amOrPm = firstTimeout.get(Calendar.AM_PM);
        int dayOfMonth = firstTimeout.get(Calendar.DAY_OF_MONTH);
        Assert.assertEquals("Unexpected second in first timeout " + firstTimeoutDate, 15, second);
        Assert.assertEquals("Unexpected minute in first timeout " + firstTimeoutDate, 30, minute);
        Assert.assertEquals("Unexpected hour in first timeout " + firstTimeoutDate, 9, hour);
        Assert.assertEquals("Unexpected AM/PM in first timeout ", Calendar.AM, amOrPm);
        Assert.assertEquals("Unexpected day of month in first timeout ", 31, dayOfMonth);

        Calendar previousTimeout = firstTimeout;
        for (int i = 1; i <= 180; i++) {
            Calendar nextTimeout = calendarTimeout.getNextTimeout(previousTimeout);

            Assert.assertNotNull("Next timeout is null", nextTimeout);
            Assert.assertNotNull("Next timeout is *before* the current time", nextTimeout.after(previousTimeout));

            Date nextTimeoutDate = nextTimeout.getTime();
            logger.debug("First timeout was: " + firstTimeoutDate + " Previous timeout was: " + previousTimeout.getTime()
                    + " Next timeout is " + nextTimeoutDate);

            int nextMinute = nextTimeout.get(Calendar.MINUTE);
            int nextSecond = nextTimeout.get(Calendar.SECOND);
            int nextHour = nextTimeout.get(Calendar.HOUR_OF_DAY);
            int nextAmOrPm = nextTimeout.get(Calendar.AM_PM);
            int nextDayOfMonth = nextTimeout.get(Calendar.DAY_OF_MONTH);
            Assert.assertEquals("Unexpected second in next timeout " + nextTimeoutDate, 15, nextSecond);
            Assert.assertEquals("Unexpected minute in next timeout " + nextTimeoutDate, 30, nextMinute);
            Assert.assertEquals("Unexpected hour in next timeout " + nextTimeoutDate, 9, nextHour);
            Assert.assertEquals("Unexpected AM/PM in next timeout ", Calendar.AM, nextAmOrPm);
            Assert.assertEquals("Unexpected day of month in next timeout ", 31, nextDayOfMonth);

            previousTimeout = nextTimeout;
        }
    }

    @Test
    public void testRun29thOfFeb() {
        ScheduleExpression everyLeapYearOn29thFeb = this.getTimezoneSpecificScheduleExpression();
        everyLeapYearOn29thFeb.dayOfMonth(29);
        everyLeapYearOn29thFeb.month("fEb");

        CalendarBasedTimeout calendarTimeout = new CalendarBasedTimeout(everyLeapYearOn29thFeb);

        Calendar firstTimeout = calendarTimeout.getFirstTimeout();
        Assert.assertNotNull("first timeout is null", firstTimeout);
        Date firstTimeoutDate = firstTimeout.getTime();
        int minute = firstTimeout.get(Calendar.MINUTE);
        int second = firstTimeout.get(Calendar.SECOND);
        int hour = firstTimeout.get(Calendar.HOUR_OF_DAY);
        int amOrPm = firstTimeout.get(Calendar.AM_PM);
        int dayOfMonth = firstTimeout.get(Calendar.DAY_OF_MONTH);
        int year = firstTimeout.get(Calendar.YEAR);
        int month = firstTimeout.get(Calendar.MONTH);

        Assert.assertEquals("Unexpected second in first timeout " + firstTimeoutDate, 0, second);
        Assert.assertEquals("Unexpected minute in first timeout " + firstTimeoutDate, 0, minute);
        Assert.assertEquals("Unexpected hour in first timeout " + firstTimeout, 0, hour);
        Assert.assertEquals("Unexpected AM/PM in first timeout ", Calendar.AM, amOrPm);
        Assert.assertEquals("Unexpected day of month in first timeout ", 29, dayOfMonth);
        Assert.assertEquals("Unexpected month in first timeout ", Calendar.FEBRUARY, month);
        Assert.assertTrue("Year: " + year + " is not a leap year", this.isLeapYear(firstTimeout));

        Calendar previousTimeout = firstTimeout;
        for (int i = 1; i <= 2; i++) {
            Calendar nextTimeout = calendarTimeout.getNextTimeout(previousTimeout);

            Assert.assertNotNull("Next timeout is null", nextTimeout);
            Assert.assertNotNull("Next timeout is *before* the current time", nextTimeout.after(previousTimeout));

            Date nextTimeoutDate = nextTimeout.getTime();
            logger.debug("First timeout was: " + firstTimeoutDate + " Previous timeout was: " + previousTimeout.getTime()
                    + " Next timeout is " + nextTimeoutDate);

            int nextMinute = nextTimeout.get(Calendar.MINUTE);
            int nextSecond = nextTimeout.get(Calendar.SECOND);
            int nextHour = nextTimeout.get(Calendar.HOUR_OF_DAY);
            int nextAmOrPm = nextTimeout.get(Calendar.AM_PM);
            int nextDayOfMonth = nextTimeout.get(Calendar.DAY_OF_MONTH);
            int nextYear = nextTimeout.get(Calendar.YEAR);
            int nextMonth = nextTimeout.get(Calendar.MONTH);

            Assert.assertEquals("Unexpected second in next timeout " + nextTimeoutDate, 0, nextSecond);
            Assert.assertEquals("Unexpected minute in next timeout " + nextTimeoutDate, 0, nextMinute);
            Assert.assertEquals("Unexpected hour in next timeout " + nextTimeout, 0, nextHour);
            Assert.assertEquals("Unexpected AM/PM in next timeout ", Calendar.AM, nextAmOrPm);
            Assert.assertEquals("Unexpected day of month in next timeout ", 29, nextDayOfMonth);
            Assert.assertEquals("Unexpected month in next timeout ", Calendar.FEBRUARY, nextMonth);
            Assert.assertTrue("Year: " + nextYear + " is not a leap year", this.isLeapYear(nextTimeout));

            previousTimeout = nextTimeout;
        }
    }

    @Test
    public void testSomeSpecificTime() {
        ScheduleExpression every0_15_30_Sec_At_9_30_PM = this.getTimezoneSpecificScheduleExpression();
        every0_15_30_Sec_At_9_30_PM.dayOfMonth(31);
        every0_15_30_Sec_At_9_30_PM.month("Nov-Feb");
        every0_15_30_Sec_At_9_30_PM.second("0,15,30");
        every0_15_30_Sec_At_9_30_PM.minute(30);
        every0_15_30_Sec_At_9_30_PM.hour("21");

        CalendarBasedTimeout calendarTimeout = new CalendarBasedTimeout(every0_15_30_Sec_At_9_30_PM);
        Calendar firstTimeout = calendarTimeout.getFirstTimeout();
        Assert.assertNotNull("first timeout is null", firstTimeout);
        Date firstTimeoutDate = firstTimeout.getTime();
        logger.debug("First timeout is " + firstTimeoutDate);

        int minute = firstTimeout.get(Calendar.MINUTE);
        int second = firstTimeout.get(Calendar.SECOND);
        int hour = firstTimeout.get(Calendar.HOUR_OF_DAY);
        int amOrPm = firstTimeout.get(Calendar.AM_PM);
        int dayOfMonth = firstTimeout.get(Calendar.DAY_OF_MONTH);
        int month = firstTimeout.get(Calendar.MONTH);

        Assert.assertEquals("Unexpected second in first timeout " + firstTimeoutDate, 0, second);
        Assert.assertEquals("Unexpected minute in first timeout " + firstTimeoutDate, 30, minute);
        Assert.assertEquals("Unexpected hour in first timeout " + firstTimeoutDate, 21, hour);
        Assert.assertEquals("Unexpected AM/PM in first timeout ", Calendar.PM, amOrPm);
        Assert.assertEquals("Unexpected day of month in first timeout ", 31, dayOfMonth);
        List<Integer> validMonths = new ArrayList<Integer>();
        validMonths.add(Calendar.NOVEMBER);
        validMonths.add(Calendar.DECEMBER);
        validMonths.add(Calendar.JANUARY);
        validMonths.add(Calendar.FEBRUARY);
        Assert.assertTrue("Unexpected month: " + month, validMonths.contains(month));

        Calendar nextTimeout = calendarTimeout.getNextTimeout(firstTimeout);
        long diff = nextTimeout.getTimeInMillis() - firstTimeout.getTimeInMillis();
        Assert.assertEquals("Unexpected next timeout " + nextTimeout.getTime(), 15 * 1000, diff);

        Calendar currentSystemDate = new GregorianCalendar();
        Calendar nextTimeoutFromNow = calendarTimeout.getNextTimeout(currentSystemDate);
        logger.debug("Next timeout from now is " + nextTimeoutFromNow.getTime());
        int nextMinute = nextTimeoutFromNow.get(Calendar.MINUTE);
        int nextSecond = nextTimeoutFromNow.get(Calendar.SECOND);
        int nextHour = nextTimeoutFromNow.get(Calendar.HOUR_OF_DAY);
        int nextAmOrPM = nextTimeoutFromNow.get(Calendar.AM_PM);
        int nextDayOfMonth = nextTimeoutFromNow.get(Calendar.DAY_OF_MONTH);
        int nextMonth = nextTimeoutFromNow.get(Calendar.MONTH);

        List<Integer> validSeconds = new ArrayList<Integer>();
        validSeconds.add(0);
        validSeconds.add(15);
        validSeconds.add(30);

        Assert.assertTrue("Unexpected second in next timeout ", validSeconds.contains(nextSecond));
        Assert.assertEquals("Unexpected minute in next timeout ", 30, nextMinute);
        Assert.assertEquals("Unexpected hour in first timeout ", 21, nextHour);
        Assert.assertEquals("Unexpected AM/PM in next timeout ", Calendar.PM, nextAmOrPM);
        Assert.assertEquals("Unexpected day of month in next timeout ", 31, nextDayOfMonth);
        Assert.assertTrue("Unexpected month: " + nextMonth, validMonths.contains(nextMonth));

    }

    @Test
    public void testEvery10Seconds() {
        ScheduleExpression every10Secs = this.getTimezoneSpecificScheduleExpression();
        every10Secs.second("*/10");
        every10Secs.minute("*");
        every10Secs.hour("*");

        CalendarBasedTimeout calendarTimeout = new CalendarBasedTimeout(every10Secs);
        Calendar firstTimeout = calendarTimeout.getFirstTimeout();

        int firstTimeoutSecond = firstTimeout.get(Calendar.SECOND);
        Assert.assertTrue("Unexpected second " + firstTimeoutSecond + " in first timeout " + firstTimeout,
                firstTimeoutSecond % 10 == 0);

        Calendar previousTimeout = firstTimeout;
        for (int i = 0; i < 5; i++) {
            Calendar nextTimeout = calendarTimeout.getNextTimeout(previousTimeout);
            int nextTimeoutSecond = nextTimeout.get(Calendar.SECOND);
            Assert.assertTrue("Unexpected second " + nextTimeoutSecond + " in next timeout " + nextTimeout,
                    nextTimeoutSecond % 10 == 0);

            previousTimeout = nextTimeout;

        }

    }

    private ScheduleExpression getTimezoneSpecificScheduleExpression() {
        return new ScheduleExpression().timezone(this.timezone.getID());
    }

    private boolean isLeapYear(Calendar cal) {
        int year = cal.get(Calendar.YEAR);
        if (year % 4 == 0) {
            return true;
        }
        return false;
    }

    private boolean isWeekDay(Calendar cal) {
        int dayOfWeek = cal.get(Calendar.DAY_OF_WEEK);

        switch (dayOfWeek) {
            case Calendar.SATURDAY:
            case Calendar.SUNDAY:
                return false;
            default:
                return true;
        }
    }

}
