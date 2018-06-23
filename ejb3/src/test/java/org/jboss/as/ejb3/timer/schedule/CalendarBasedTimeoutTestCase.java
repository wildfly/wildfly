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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.TimeZone;

import javax.ejb.ScheduleExpression;

import org.jboss.as.ejb3.timerservice.schedule.CalendarBasedTimeout;
import org.junit.Assert;
import org.junit.Test;

/**
 * CalendarBasedTimeoutTestCase
 *
 * @author Jaikiran Pai
 * @author Eduardo Martins
 * @author "<a href=\"mailto:wfink@redhat.com\">Wolf-Dieter Fink</a>"
 */
public class CalendarBasedTimeoutTestCase {

    /**
     * Logger
     */
//    private static Logger logger = Logger.getLogger(CalendarBasedTimeoutTestCase.class);

    /**
     * The timezone which is in use
     */
    private TimeZone timezone;
    private String timeZoneDisplayName;

    /**
     * This method returns a collection of all available timezones in the system.
     * The tests in this {@link CalendarBasedTimeoutTestCase} will then be run
     * against each of these timezones
     */
    private static List<TimeZone> getTimezones() {
        String[] candidates = TimeZone.getAvailableIDs();
        List<TimeZone> timeZones = new ArrayList<TimeZone>(candidates.length);
        for (String timezoneID : candidates) {
            TimeZone timeZone = TimeZone.getTimeZone(timezoneID);
            boolean different = true;
            for (int i = timeZones.size() - 1; i >= 0; i--)  {
                TimeZone testee = timeZones.get(i);
                if (testee.hasSameRules(timeZone)) {
                    different = false;
                    break;
                }
            }
            if (different) {
                timeZones.add(timeZone);
            }
        }
        return timeZones;
    }

    /**
     * Asserts timeouts based on next day of week.
     * Uses expression dayOfWeek=saturday hour=3 minute=21 second=50.
     * Expected next timeout is SAT 2014-03-29 3:21:50
     */
    @Test
    public void testNextDayOfWeek() {
        // start date is SAT 2014-03-22 4:00:00, has to advance to SAT of next week
        testNextDayOfWeek(new GregorianCalendar(2014,2,22,4,0,0).getTime());
        // start date is TUE 2014-03-25 2:00:00, has to advance to SAT of same week
        testNextDayOfWeek(new GregorianCalendar(2014,2,25,2,0,0).getTime());
    }

    private void testNextDayOfWeek(Date start) {
        ScheduleExpression expression = new ScheduleExpression();
        expression.dayOfWeek("6");
        expression.hour("3");
        expression.minute("21");
        expression.second("50");
        expression.start(start);
        CalendarBasedTimeout calendarTimeout = new CalendarBasedTimeout(expression);
        Calendar firstTimeout = calendarTimeout.getFirstTimeout();
        Assert.assertNotNull(firstTimeout);
        Assert.assertEquals(50, firstTimeout.get(Calendar.SECOND));
        Assert.assertEquals(21, firstTimeout.get(Calendar.MINUTE));
        Assert.assertEquals(3, firstTimeout.get(Calendar.HOUR_OF_DAY));
        Assert.assertEquals(7, firstTimeout.get(Calendar.DAY_OF_WEEK));
        Assert.assertEquals(29, firstTimeout.get(Calendar.DAY_OF_MONTH));
    }

    @Test
    public void testCalendarBasedTimeout() {
        for (TimeZone tz : getTimezones()) {
            this.timezone = tz;
            this.timeZoneDisplayName = this.timezone.getDisplayName();

            testEverySecondTimeout();
            testEveryMinuteEveryHourEveryDay();
            testEveryMorningFiveFifteen();
            testEveryWeekdayEightFifteen();
            testEveryMonWedFriTwelveThirtyNoon();
            testEvery31stOfTheMonth();
            testRun29thOfFeb();
            testSomeSpecificTime();
            testEvery10Seconds();
        }
    }

    //@Test
    public void testEverySecondTimeout() {
        ScheduleExpression everySecondExpression = this.getTimezoneSpecificScheduleExpression();
        everySecondExpression.second("*");
        everySecondExpression.minute("*");
        everySecondExpression.hour("*");

        CalendarBasedTimeout calendarTimeout = new CalendarBasedTimeout(everySecondExpression);

        Calendar firstTimeout = calendarTimeout.getFirstTimeout();
        Calendar nextTimeout = calendarTimeout.getNextTimeout(firstTimeout);

        Assert.assertNotNull(timeZoneDisplayName, nextTimeout);
        Assert.assertNotNull(timeZoneDisplayName, nextTimeout.after(firstTimeout));
//        logger.debug("Previous timeout was: " + firstTimeout.getTime() + " Next timeout is " + nextTimeout.getTime());
        long diff = nextTimeout.getTimeInMillis() - firstTimeout.getTimeInMillis();
        Assert.assertEquals(timeZoneDisplayName, 1000, diff);
    }

    //@Test
    public void testEveryMinuteEveryHourEveryDay() {
        ScheduleExpression everyMinEveryHourEveryDay = this.getTimezoneSpecificScheduleExpression();
        everyMinEveryHourEveryDay.minute("*");
        everyMinEveryHourEveryDay.hour("*");

        CalendarBasedTimeout calendarTimeout = new CalendarBasedTimeout(everyMinEveryHourEveryDay);

        Calendar firstTimeout = calendarTimeout.getFirstTimeout();
        Calendar previousTimeout = firstTimeout;
        for (int i = 1; i <= 65; i++) {
            Calendar nextTimeout = calendarTimeout.getNextTimeout(previousTimeout);

            Assert.assertNotNull(timeZoneDisplayName, nextTimeout);
            Assert.assertNotNull(timeZoneDisplayName, nextTimeout.after(previousTimeout));
//            logger.debug("First timeout was: " + firstTimeout.getTime() + " Previous timeout was: "
//                    + previousTimeout.getTime() + " Next timeout is " + nextTimeout.getTime());
            long diff = nextTimeout.getTimeInMillis() - previousTimeout.getTimeInMillis();
            long diffWithFirstTimeout = nextTimeout.getTimeInMillis() - firstTimeout.getTimeInMillis();
            Assert.assertEquals(timeZoneDisplayName, 60 * 1000, diff);
            Assert.assertEquals(timeZoneDisplayName, 60 * 1000 * i, diffWithFirstTimeout);

            previousTimeout = nextTimeout;
        }
    }

    //@Test
    public void testEveryMorningFiveFifteen() {
        ScheduleExpression everyMorningFiveFifteen = this.getTimezoneSpecificScheduleExpression();
        everyMorningFiveFifteen.minute(15);
        everyMorningFiveFifteen.hour(5);

        CalendarBasedTimeout calendarTimeout = new CalendarBasedTimeout(everyMorningFiveFifteen);

        Calendar firstTimeout = calendarTimeout.getFirstTimeout();

        Assert.assertNotNull(timeZoneDisplayName, firstTimeout);
        int minute = firstTimeout.get(Calendar.MINUTE);
        int second = firstTimeout.get(Calendar.SECOND);
        int hour = firstTimeout.get(Calendar.HOUR_OF_DAY);
        int amOrPm = firstTimeout.get(Calendar.AM_PM);
        Assert.assertEquals(timeZoneDisplayName, 0, second);
        Assert.assertEquals(timeZoneDisplayName, 15, minute);
        Assert.assertEquals(timeZoneDisplayName, 5, hour);
        Assert.assertEquals(timeZoneDisplayName, Calendar.AM, amOrPm);

        Calendar previousTimeout = firstTimeout;
        for (int i = 1; i <= 370; i++) {
            Calendar nextTimeout = calendarTimeout.getNextTimeout(previousTimeout);

            Assert.assertNotNull(timeZoneDisplayName, nextTimeout);
            Assert.assertNotNull(timeZoneDisplayName, nextTimeout.after(previousTimeout));
//            logger.debug("First timeout was: " + firstTimeout.getTime() + " Previous timeout was: "
//                    + previousTimeout.getTime() + " Next timeout is " + nextTimeout.getTime());

            Assert.assertEquals(timeZoneDisplayName, 0, nextTimeout.get(Calendar.SECOND));
            Assert.assertEquals(timeZoneDisplayName, 15, nextTimeout.get(Calendar.MINUTE));

            Assert.assertEquals(timeZoneDisplayName, 5, nextTimeout.get(Calendar.HOUR_OF_DAY));
            Assert.assertEquals(timeZoneDisplayName, Calendar.AM, nextTimeout.get(Calendar.AM_PM));

            previousTimeout = nextTimeout;

        }
    }

    //@Test
    public void testEveryWeekdayEightFifteen() {
        ScheduleExpression everyWeekDayThreeFifteen = this.getTimezoneSpecificScheduleExpression();
        everyWeekDayThreeFifteen.minute(15);
        everyWeekDayThreeFifteen.hour(8);
        everyWeekDayThreeFifteen.dayOfWeek("Mon-Fri");

        CalendarBasedTimeout calendarTimeout = new CalendarBasedTimeout(everyWeekDayThreeFifteen);

        Calendar firstTimeout = calendarTimeout.getFirstTimeout();
        Assert.assertNotNull(timeZoneDisplayName, firstTimeout);
        int minute = firstTimeout.get(Calendar.MINUTE);
        int second = firstTimeout.get(Calendar.SECOND);
        int hour = firstTimeout.get(Calendar.HOUR_OF_DAY);
        int amOrPm = firstTimeout.get(Calendar.AM_PM);
        Assert.assertEquals(timeZoneDisplayName, 0, second);
        Assert.assertEquals(timeZoneDisplayName, 15, minute);
        Assert.assertEquals(timeZoneDisplayName, 8, hour);
        Assert.assertEquals(timeZoneDisplayName, Calendar.AM, amOrPm);
        Assert.assertTrue(timeZoneDisplayName, this.isWeekDay(firstTimeout));

        Calendar previousTimeout = firstTimeout;
        for (int i = 1; i <= 180; i++) {
            Calendar nextTimeout = calendarTimeout.getNextTimeout(previousTimeout);

            Assert.assertNotNull(timeZoneDisplayName, nextTimeout);
            Assert.assertNotNull(timeZoneDisplayName, nextTimeout.after(previousTimeout));

//            logger.debug("First timeout was: " + firstTimeoutDate + " Previous timeout was: " + previousTimeout.getTime()
//                    + " Next timeout is " + nextTimeoutDate);

            int nextMinute = nextTimeout.get(Calendar.MINUTE);
            int nextSecond = nextTimeout.get(Calendar.SECOND);
            int nextHour = nextTimeout.get(Calendar.HOUR_OF_DAY);
            int nextAmOrPm = nextTimeout.get(Calendar.AM_PM);
            Assert.assertEquals(timeZoneDisplayName, 0, nextSecond);
            Assert.assertEquals(timeZoneDisplayName, 15, nextMinute);
            Assert.assertEquals(timeZoneDisplayName, 8, nextHour);
            Assert.assertEquals(timeZoneDisplayName, Calendar.AM, nextAmOrPm);
            Assert.assertTrue(timeZoneDisplayName, this.isWeekDay(nextTimeout));

            previousTimeout = nextTimeout;
        }
    }

    //@Test
    public void testEveryMonWedFriTwelveThirtyNoon() {
        ScheduleExpression everyMonWedFriTwelveThirtyNoon = this.getTimezoneSpecificScheduleExpression();
        everyMonWedFriTwelveThirtyNoon.hour(12);
        everyMonWedFriTwelveThirtyNoon.second("30");
        everyMonWedFriTwelveThirtyNoon.dayOfWeek("Mon,Wed,Fri");

        CalendarBasedTimeout calendarTimeout = new CalendarBasedTimeout(everyMonWedFriTwelveThirtyNoon);

        Calendar firstTimeout = calendarTimeout.getFirstTimeout();
        Assert.assertNotNull(timeZoneDisplayName, firstTimeout);
        int minute = firstTimeout.get(Calendar.MINUTE);
        int second = firstTimeout.get(Calendar.SECOND);
        int hour = firstTimeout.get(Calendar.HOUR_OF_DAY);
        int amOrPm = firstTimeout.get(Calendar.AM_PM);
        int dayOfWeek = firstTimeout.get(Calendar.DAY_OF_WEEK);
        Assert.assertEquals(timeZoneDisplayName, 30, second);
        Assert.assertEquals(timeZoneDisplayName, 0, minute);
        Assert.assertEquals(timeZoneDisplayName, 12, hour);
        Assert.assertEquals(timeZoneDisplayName, Calendar.PM, amOrPm);
        List<Integer> validDays = new ArrayList<Integer>();
        validDays.add(Calendar.MONDAY);
        validDays.add(Calendar.WEDNESDAY);
        validDays.add(Calendar.FRIDAY);
        Assert.assertTrue(timeZoneDisplayName, validDays.contains(dayOfWeek));

        Calendar previousTimeout = firstTimeout;
        for (int i = 1; i <= 180; i++) {
            Calendar nextTimeout = calendarTimeout.getNextTimeout(previousTimeout);

            Assert.assertNotNull(timeZoneDisplayName, nextTimeout);
            Assert.assertNotNull(timeZoneDisplayName, nextTimeout.after(previousTimeout));

//            logger.debug("First timeout was: " + firstTimeoutDate + " Previous timeout was: " + previousTimeout.getTime()
//                    + " Next timeout is " + nextTimeoutDate);

            int nextMinute = nextTimeout.get(Calendar.MINUTE);
            int nextSecond = nextTimeout.get(Calendar.SECOND);
            int nextHour = nextTimeout.get(Calendar.HOUR_OF_DAY);
            int nextAmOrPm = nextTimeout.get(Calendar.AM_PM);
            int nextDayOfWeek = nextTimeout.get(Calendar.DAY_OF_WEEK);
            Assert.assertEquals(timeZoneDisplayName, 30, nextSecond);
            Assert.assertEquals(timeZoneDisplayName, 0, nextMinute);
            Assert.assertEquals(timeZoneDisplayName, 12, nextHour);
            Assert.assertEquals(timeZoneDisplayName, Calendar.PM, nextAmOrPm);
            Assert.assertTrue(timeZoneDisplayName, validDays.contains(nextDayOfWeek));
            previousTimeout = nextTimeout;
        }
    }

    //@Test
    public void testEvery31stOfTheMonth() {
        ScheduleExpression every31st9_30_15_AM = this.getTimezoneSpecificScheduleExpression();
        every31st9_30_15_AM.dayOfMonth(31);
        every31st9_30_15_AM.hour(9);
        every31st9_30_15_AM.minute("30");
        every31st9_30_15_AM.second(15);

        CalendarBasedTimeout calendarTimeout = new CalendarBasedTimeout(every31st9_30_15_AM);

        Calendar firstTimeout = calendarTimeout.getFirstTimeout();
        Assert.assertNotNull(timeZoneDisplayName, firstTimeout);
        int minute = firstTimeout.get(Calendar.MINUTE);
        int second = firstTimeout.get(Calendar.SECOND);
        int hour = firstTimeout.get(Calendar.HOUR_OF_DAY);
        int amOrPm = firstTimeout.get(Calendar.AM_PM);
        int dayOfMonth = firstTimeout.get(Calendar.DAY_OF_MONTH);
        Assert.assertEquals(timeZoneDisplayName, 15, second);
        Assert.assertEquals(timeZoneDisplayName, 30, minute);
        Assert.assertEquals(timeZoneDisplayName, 9, hour);
        Assert.assertEquals(timeZoneDisplayName, Calendar.AM, amOrPm);
        Assert.assertEquals(timeZoneDisplayName, 31, dayOfMonth);

        Calendar previousTimeout = firstTimeout;
        for (int i = 1; i <= 18; i++) {
            Calendar nextTimeout = calendarTimeout.getNextTimeout(previousTimeout);

            Assert.assertNotNull(timeZoneDisplayName, nextTimeout);
            Assert.assertNotNull(timeZoneDisplayName, nextTimeout.after(previousTimeout));

//            logger.debug("First timeout was: " + firstTimeoutDate + " Previous timeout was: " + previousTimeout.getTime()
//                    + " Next timeout is " + nextTimeoutDate);

            int nextMinute = nextTimeout.get(Calendar.MINUTE);
            int nextSecond = nextTimeout.get(Calendar.SECOND);
            int nextHour = nextTimeout.get(Calendar.HOUR_OF_DAY);
            int nextAmOrPm = nextTimeout.get(Calendar.AM_PM);
            int nextDayOfMonth = nextTimeout.get(Calendar.DAY_OF_MONTH);
            Assert.assertEquals(timeZoneDisplayName, 15, nextSecond);
            Assert.assertEquals(timeZoneDisplayName, 30, nextMinute);
            Assert.assertEquals(timeZoneDisplayName, 9, nextHour);
            Assert.assertEquals(timeZoneDisplayName, Calendar.AM, nextAmOrPm);
            Assert.assertEquals(timeZoneDisplayName, 31, nextDayOfMonth);

            previousTimeout = nextTimeout;
        }
    }

    //@Test
    public void testRun29thOfFeb() {
        ScheduleExpression everyLeapYearOn29thFeb = this.getTimezoneSpecificScheduleExpression();
        everyLeapYearOn29thFeb.dayOfMonth(29);
        everyLeapYearOn29thFeb.month("fEb");

        CalendarBasedTimeout calendarTimeout = new CalendarBasedTimeout(everyLeapYearOn29thFeb);

        Calendar firstTimeout = calendarTimeout.getFirstTimeout();
        Assert.assertNotNull(timeZoneDisplayName, firstTimeout);
        int minute = firstTimeout.get(Calendar.MINUTE);
        int second = firstTimeout.get(Calendar.SECOND);
        int hour = firstTimeout.get(Calendar.HOUR_OF_DAY);
        int amOrPm = firstTimeout.get(Calendar.AM_PM);
        int dayOfMonth = firstTimeout.get(Calendar.DAY_OF_MONTH);
        int month = firstTimeout.get(Calendar.MONTH);

        Assert.assertEquals(timeZoneDisplayName, 0, second);
        Assert.assertEquals(timeZoneDisplayName, 0, minute);
        Assert.assertEquals(timeZoneDisplayName, 0, hour);
        Assert.assertEquals(timeZoneDisplayName, Calendar.AM, amOrPm);
        Assert.assertEquals(timeZoneDisplayName, 29, dayOfMonth);
        Assert.assertEquals(timeZoneDisplayName, Calendar.FEBRUARY, month);
        Assert.assertTrue(timeZoneDisplayName, this.isLeapYear(firstTimeout));

        Calendar previousTimeout = firstTimeout;
        for (int i = 1; i <= 2; i++) {
            Calendar nextTimeout = calendarTimeout.getNextTimeout(previousTimeout);

            Assert.assertNotNull(timeZoneDisplayName, nextTimeout);
            Assert.assertNotNull(timeZoneDisplayName, nextTimeout.after(previousTimeout));

//            logger.debug("First timeout was: " + firstTimeoutDate + " Previous timeout was: " + previousTimeout.getTime()
//                    + " Next timeout is " + nextTimeoutDate);

            int nextMinute = nextTimeout.get(Calendar.MINUTE);
            int nextSecond = nextTimeout.get(Calendar.SECOND);
            int nextHour = nextTimeout.get(Calendar.HOUR_OF_DAY);
            int nextAmOrPm = nextTimeout.get(Calendar.AM_PM);
            int nextDayOfMonth = nextTimeout.get(Calendar.DAY_OF_MONTH);
            int nextMonth = nextTimeout.get(Calendar.MONTH);

            Assert.assertEquals(timeZoneDisplayName, 0, nextSecond);
            Assert.assertEquals(timeZoneDisplayName, 0, nextMinute);
            Assert.assertEquals(timeZoneDisplayName, 0, nextHour);
            Assert.assertEquals(timeZoneDisplayName, Calendar.AM, nextAmOrPm);
            Assert.assertEquals(timeZoneDisplayName, 29, nextDayOfMonth);
            Assert.assertEquals(timeZoneDisplayName, Calendar.FEBRUARY, nextMonth);
            Assert.assertTrue(timeZoneDisplayName, this.isLeapYear(nextTimeout));

            previousTimeout = nextTimeout;
        }
    }

    //@Test
    public void testSomeSpecificTime() {
        ScheduleExpression every0_15_30_Sec_At_9_30_PM = this.getTimezoneSpecificScheduleExpression();
        every0_15_30_Sec_At_9_30_PM.dayOfMonth(31);
        every0_15_30_Sec_At_9_30_PM.month("Nov-Feb");
        every0_15_30_Sec_At_9_30_PM.second("0,15,30");
        every0_15_30_Sec_At_9_30_PM.minute(30);
        every0_15_30_Sec_At_9_30_PM.hour("21");

        CalendarBasedTimeout calendarTimeout = new CalendarBasedTimeout(every0_15_30_Sec_At_9_30_PM);
        Calendar firstTimeout = calendarTimeout.getFirstTimeout();
        Assert.assertNotNull(timeZoneDisplayName, firstTimeout);
//        logger.debug("First timeout is " + firstTimeoutDate);

        int minute = firstTimeout.get(Calendar.MINUTE);
        int second = firstTimeout.get(Calendar.SECOND);
        int hour = firstTimeout.get(Calendar.HOUR_OF_DAY);
        int amOrPm = firstTimeout.get(Calendar.AM_PM);
        int dayOfMonth = firstTimeout.get(Calendar.DAY_OF_MONTH);
        int month = firstTimeout.get(Calendar.MONTH);

        Assert.assertEquals(timeZoneDisplayName, 0, second);
        Assert.assertEquals(timeZoneDisplayName, 30, minute);
        Assert.assertEquals(timeZoneDisplayName, 21, hour);
        Assert.assertEquals(timeZoneDisplayName, Calendar.PM, amOrPm);
        Assert.assertEquals(timeZoneDisplayName, 31, dayOfMonth);
        List<Integer> validMonths = new ArrayList<Integer>();
        validMonths.add(Calendar.NOVEMBER);
        validMonths.add(Calendar.DECEMBER);
        validMonths.add(Calendar.JANUARY);
        validMonths.add(Calendar.FEBRUARY);
        Assert.assertTrue(timeZoneDisplayName, validMonths.contains(month));

        Calendar nextTimeout = calendarTimeout.getNextTimeout(firstTimeout);
        long diff = nextTimeout.getTimeInMillis() - firstTimeout.getTimeInMillis();
        Assert.assertEquals(timeZoneDisplayName, 15 * 1000, diff);

        Calendar date = new GregorianCalendar(2014,3,18);
        Calendar nextTimeoutFromNow = calendarTimeout.getNextTimeout(date);
//        logger.debug("Next timeout from now is " + nextTimeoutFromNow.getTime());
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

        Assert.assertTrue(timeZoneDisplayName, validSeconds.contains(nextSecond));
        Assert.assertEquals(timeZoneDisplayName, 30, nextMinute);
        Assert.assertEquals(timeZoneDisplayName, 21, nextHour);
        Assert.assertEquals(timeZoneDisplayName, Calendar.PM, nextAmOrPM);
        Assert.assertEquals(timeZoneDisplayName, 31, nextDayOfMonth);
        Assert.assertTrue(timeZoneDisplayName, validMonths.contains(nextMonth));

    }

    //@Test
    public void testEvery10Seconds() {
        ScheduleExpression every10Secs = this.getTimezoneSpecificScheduleExpression();
        every10Secs.second("*/10");
        every10Secs.minute("*");
        every10Secs.hour("*");

        CalendarBasedTimeout calendarTimeout = new CalendarBasedTimeout(every10Secs);
        Calendar firstTimeout = calendarTimeout.getFirstTimeout();

        int firstTimeoutSecond = firstTimeout.get(Calendar.SECOND);
        Assert.assertTrue(timeZoneDisplayName, firstTimeoutSecond % 10 == 0);

        Calendar previousTimeout = firstTimeout;
        for (int i = 0; i < 5; i++) {
            Calendar nextTimeout = calendarTimeout.getNextTimeout(previousTimeout);
            int nextTimeoutSecond = nextTimeout.get(Calendar.SECOND);
            Assert.assertTrue(timeZoneDisplayName, nextTimeoutSecond % 10 == 0);

            previousTimeout = nextTimeout;

        }

    }


    /**
     * WFLY-1468
     * Create a Timeout with a schedule start date in the past (day before) to ensure the time is set correctly.
     * The schedule is on the first day of month to ensure that the calculated time must be moved to the next month.
     *
     * The test is run for each day of a whole year.
     */
    @Test
    public void testWFLY1468() {
        ScheduleExpression schedule = new ScheduleExpression();
        int year = 2013;
        int month = Calendar.JUNE;
        int dayOfMonth = 3;
        int hourOfDay = 2;
        int minutes = 0;
        Calendar start = new GregorianCalendar(year, month, dayOfMonth, hourOfDay, minutes);
        schedule.hour("0-12")
                .month("*")
                .dayOfMonth("3")
                .minute("0/5")
                .second("0")
                .start(start.getTime());
        CalendarBasedTimeout calendarTimeout = new CalendarBasedTimeout(schedule);
        Calendar firstTimeout = calendarTimeout.getFirstTimeout();
        // assert first timeout result
        if(firstTimeout.get(Calendar.DAY_OF_MONTH) != 3 ||
                firstTimeout.get(Calendar.HOUR_OF_DAY) != 2 ||
                firstTimeout.get(Calendar.MINUTE) != 0 ||
                firstTimeout.get(Calendar.SECOND) != 0) {
            Assert.fail(firstTimeout.toString());
        }
    }

    /**
     * Testcase #1 for WFLY-3947
     */
    @Test
    public void testWFLY3947_1() {
        TimeZone timeZone = TimeZone.getTimeZone("Europe/Lisbon");
        int year = 2013;
        int month = Calendar.MARCH;
        int dayOfMonth = 31;
        int hourOfDay = 3;
        int minute = 30;
        int second = 0;

        Calendar start = new GregorianCalendar(timeZone);
        start.clear();
        start.set(year, month, dayOfMonth, hourOfDay, minute, second);

        ScheduleExpression expression = new ScheduleExpression().timezone(timeZone.getID()).dayOfMonth("*").hour("1").minute("30").second("0").start(start.getTime());

        CalendarBasedTimeout calendarTimeout = new CalendarBasedTimeout(expression);
        Calendar firstTimeout = calendarTimeout.getFirstTimeout();
        Assert.assertNotNull(firstTimeout);
        Assert.assertEquals(year, firstTimeout.get(Calendar.YEAR));
        Assert.assertEquals(Calendar.APRIL, firstTimeout.get(Calendar.MONTH));
        Assert.assertEquals(1, firstTimeout.get(Calendar.DAY_OF_MONTH));
        Assert.assertEquals(1, firstTimeout.get(Calendar.HOUR_OF_DAY));
        Assert.assertEquals(30, firstTimeout.get(Calendar.MINUTE));
        Assert.assertEquals(second, firstTimeout.get(Calendar.SECOND));
    }

    /**
     * Testcase #2 for WFLY-3947
     */
    @Test
    public void testWFLY3947_2() {
        TimeZone timeZone = TimeZone.getTimeZone("Australia/Lord_Howe");
        int year = 2013;
        int month = Calendar.OCTOBER;
        int dayOfMonth = 6;
        int hourOfDay = 2;
        int minute = 41;
        int second = 0;

        Calendar start = new GregorianCalendar(timeZone);
        start.clear();
        start.set(year, month, dayOfMonth, hourOfDay, minute, second);

        ScheduleExpression expression = new ScheduleExpression().timezone(timeZone.getID()).dayOfMonth("*").hour("2, 3").minute("20, 40").second("0").start(start.getTime());

        CalendarBasedTimeout calendarTimeout = new CalendarBasedTimeout(expression);
        Calendar firstTimeout = calendarTimeout.getFirstTimeout();
        Assert.assertNotNull(firstTimeout);
        Assert.assertEquals(year, firstTimeout.get(Calendar.YEAR));
        Assert.assertEquals(month, firstTimeout.get(Calendar.MONTH));
        Assert.assertEquals(dayOfMonth, firstTimeout.get(Calendar.DAY_OF_MONTH));
        Assert.assertEquals(3, firstTimeout.get(Calendar.HOUR_OF_DAY));
        Assert.assertEquals(20, firstTimeout.get(Calendar.MINUTE));
        Assert.assertEquals(second, firstTimeout.get(Calendar.SECOND));
    }

    /**
     * If we have an overflow for minutes, the seconds must be reseted.
     * Test for WFLY-5995
     */
    @Test
    public void testWFLY5995_MinuteOverflow() {
        int year = 2016;
        int month = Calendar.JANUARY;
        int dayOfMonth = 14;
        int hourOfDay = 9;
        int minute = 46;
        int second = 42;

        Calendar start = new GregorianCalendar();
        start.clear();
        start.set(year, month, dayOfMonth, hourOfDay, minute, second);

        ScheduleExpression expression = new ScheduleExpression().dayOfMonth("*").hour("*").minute("0-45").second("0/10").start(start.getTime());

        CalendarBasedTimeout calendarTimeout = new CalendarBasedTimeout(expression);
        Calendar firstTimeout = calendarTimeout.getFirstTimeout();
        Assert.assertNotNull(firstTimeout);
        Assert.assertEquals(year, firstTimeout.get(Calendar.YEAR));
        Assert.assertEquals(month, firstTimeout.get(Calendar.MONTH));
        Assert.assertEquals(dayOfMonth, firstTimeout.get(Calendar.DAY_OF_MONTH));
        Assert.assertEquals(10, firstTimeout.get(Calendar.HOUR_OF_DAY));
        Assert.assertEquals(0, firstTimeout.get(Calendar.MINUTE));
        Assert.assertEquals(0, firstTimeout.get(Calendar.SECOND));
    }

    /**
     * If we have an overflow for hours, the minutes and seconds must be reseted.
     * Test for WFLY-5995
     */
    @Test
    public void testWFLY5995_HourOverflow() {
        int year = 2016;
        int month = Calendar.JANUARY;
        int dayOfMonth = 14;
        int hourOfDay = 9;
        int minute = 45;
        int second = 35;

        Calendar start = new GregorianCalendar();
        start.clear();
        start.set(year, month, dayOfMonth, hourOfDay, minute, second);

        ScheduleExpression expression = new ScheduleExpression().dayOfMonth("*").hour("20-22").minute("0/5").second("20,40").start(start.getTime());

        CalendarBasedTimeout calendarTimeout = new CalendarBasedTimeout(expression);
        Calendar firstTimeout = calendarTimeout.getFirstTimeout();
        Assert.assertNotNull(firstTimeout);
        Assert.assertEquals(year, firstTimeout.get(Calendar.YEAR));
        Assert.assertEquals(month, firstTimeout.get(Calendar.MONTH));
        Assert.assertEquals(dayOfMonth, firstTimeout.get(Calendar.DAY_OF_MONTH));
        Assert.assertEquals(20, firstTimeout.get(Calendar.HOUR_OF_DAY));
        Assert.assertEquals(0, firstTimeout.get(Calendar.MINUTE));
        Assert.assertEquals(20, firstTimeout.get(Calendar.SECOND));
    }

    /**
     * Check if the hour/minute/second is reseted correct if the day must be updated
     */
    @Test
    public void testDayOverflow() {
        int year = 2016;
        int month = Calendar.JANUARY;
        int dayOfMonth = 14;
        int hourOfDay = 9;
        int minute = 56;
        int second = 0;

        Calendar start = new GregorianCalendar();
        start.clear();
        start.set(year, month, dayOfMonth, hourOfDay, minute, second);

        ScheduleExpression expression = new ScheduleExpression().dayOfMonth("2-13").hour("3-9").minute("0/5").second("0").start(start.getTime());

        CalendarBasedTimeout calendarTimeout = new CalendarBasedTimeout(expression);
        Calendar firstTimeout = calendarTimeout.getFirstTimeout();
        Assert.assertNotNull(firstTimeout);
        Assert.assertEquals(year, firstTimeout.get(Calendar.YEAR));
        Assert.assertEquals(1, firstTimeout.get(Calendar.MONTH));
        Assert.assertEquals(2, firstTimeout.get(Calendar.DAY_OF_MONTH));
        Assert.assertEquals(3, firstTimeout.get(Calendar.HOUR_OF_DAY));
        Assert.assertEquals(0, firstTimeout.get(Calendar.MINUTE));
        Assert.assertEquals(0, firstTimeout.get(Calendar.SECOND));
    }

    /**
     * Change CET winter time to CEST summer time.
     * The timer should be fired every 15 minutes (absolutely).
     * The calendar time will jump from 2:00CET to 3:00CEST
     * The test should be run similar in any OS/JVM default timezone
     * This is a test to ensure WFLY-9537 will not break this.
     */
    @Test
    public void testChangeCET2CEST() {
        Calendar start = new GregorianCalendar(TimeZone.getTimeZone("Europe/Berlin"));
        start.clear();
        // set half an hour before the CET->CEST DST switch 2017
        start.set(2017, Calendar.MARCH, 26, 1, 30, 0);

        ScheduleExpression schedule = new ScheduleExpression();
        schedule.hour("*")
                .minute("0/15")
                .second("0")
                .timezone("Europe/Berlin")  // don't fail the check below if running in a not default TZ
                .start(start.getTime());
        CalendarBasedTimeout calendarTimeout = new CalendarBasedTimeout(schedule);
        Calendar firstTimeout = calendarTimeout.getFirstTimeout();
        // assert first timeout result
        Assert.assertNotNull(firstTimeout);
        if(firstTimeout.get(Calendar.YEAR) != 2017 ||
                firstTimeout.get(Calendar.MONTH) != Calendar.MARCH ||
                firstTimeout.get(Calendar.DAY_OF_MONTH) != 26 ||
                firstTimeout.get(Calendar.HOUR_OF_DAY) != 1 ||
                firstTimeout.get(Calendar.MINUTE) != 30 ||
                firstTimeout.get(Calendar.SECOND) != 0 ||
                firstTimeout.get(Calendar.DST_OFFSET) != 0) {
            Assert.fail("Start time unexpected : " + firstTimeout.toString());
        }
        Calendar current = firstTimeout;
        for(int i = 0 ; i<3 ; i++) {
            Calendar next = calendarTimeout.getNextTimeout(current);
            if(current.getTimeInMillis() != (next.getTimeInMillis() - 900000)) {
                Assert.fail("Schedule is more than 15 minutes from " + current.getTime() + " to " + next.getTime());
            }
            current = next;
        }
        if(current.get(Calendar.YEAR) != 2017 ||
                current.get(Calendar.MONTH) != Calendar.MARCH ||
                current.get(Calendar.DAY_OF_MONTH) != 26 ||
                current.get(Calendar.HOUR_OF_DAY) != 3 ||
                current.get(Calendar.MINUTE) != 15 ||
                current.get(Calendar.DST_OFFSET) != 3600000) {
            Assert.fail("End time unexpected : " + current.toString());
        }
    }

    /**
     * Change CEST summer time to CEST winter time.
     * The timer should be fired every 15 minutes (absolutely).
     * The calendar time will jump from 3:00CEST back to 2:00CET
     * but the timer must run within 2:00-3:00 CEST and 2:00-3:00CET!
     * The test should be run similar in any OS/JVM default timezone
     * This is a test for WFLY-9537
     */
    @Test
    public void testChangeCEST2CET() {
        Calendar start = new GregorianCalendar(TimeZone.getTimeZone("Europe/Berlin"));
        start.clear();
        // set half an hour before the CEST->CET DST switch 2017
        start.set(2017, Calendar.OCTOBER, 29, 1, 30, 0);

        ScheduleExpression schedule = new ScheduleExpression();
        schedule.hour("*")
                .minute("5/15")
                .second("0")
                .timezone("Europe/Berlin")  // don't fail the check below if running in a not default TZ
                .start(start.getTime());
        CalendarBasedTimeout calendarTimeout = new CalendarBasedTimeout(schedule);
        Calendar firstTimeout = calendarTimeout.getFirstTimeout();
        // assert first timeout result
        Assert.assertNotNull(firstTimeout);
        if(firstTimeout.get(Calendar.YEAR) != 2017 ||
                firstTimeout.get(Calendar.MONTH) != Calendar.OCTOBER ||
                firstTimeout.get(Calendar.DAY_OF_MONTH) != 29 ||
                firstTimeout.get(Calendar.HOUR_OF_DAY) != 1 ||
                firstTimeout.get(Calendar.MINUTE) != 35 ||
                firstTimeout.get(Calendar.SECOND) != 0 ||
                firstTimeout.get(Calendar.DST_OFFSET) != 3600000) {
            Assert.fail("Start time unexpected : " + firstTimeout.toString());
        }
        Calendar current = firstTimeout;
        for(int i = 0 ; i<7 ; i++) {
            Calendar next = calendarTimeout.getNextTimeout(current);
            if(current.getTimeInMillis() != (next.getTimeInMillis() - 900000)) {
                Assert.fail("Schedule is more than 15 minutes from " + current.getTime() + " to " + next.getTime());
            }
            current = next;
        }
        if(current.get(Calendar.YEAR) != 2017 ||
                current.get(Calendar.MONTH) != Calendar.OCTOBER ||
                current.get(Calendar.DAY_OF_MONTH) != 29 ||
                current.get(Calendar.HOUR_OF_DAY) != 2 ||
                current.get(Calendar.MINUTE) != 20 ||
                current.get(Calendar.DST_OFFSET) != 0) {
            Assert.fail("End time unexpected : " + current.toString());
        }
    }

    /**
     * Change PST winter time to PST summer time.
     * The timer should be fired every 15 minutes (absolutely).
     * This is a test to ensure WFLY-9537 will not break this.
     */
    @Test
    public void testChangeUS2Summer() {
        Calendar start = new GregorianCalendar(TimeZone.getTimeZone("America/Los_Angeles"));
        start.clear();
        // set half an hour before Los Angeles summer time switch
        start.set(2017, Calendar.MARCH, 12, 1, 30, 0);

        ScheduleExpression schedule = new ScheduleExpression();
        schedule.hour("*")
                .minute("0/15")
                .second("0")
                .timezone("America/Los_Angeles")  // don't fail the check below if running in a not default TZ
                .start(start.getTime());
        CalendarBasedTimeout calendarTimeout = new CalendarBasedTimeout(schedule);
        Calendar firstTimeout = calendarTimeout.getFirstTimeout();
        // assert first timeout result
        Assert.assertNotNull(firstTimeout);
        if(firstTimeout.get(Calendar.YEAR) != 2017 ||
                firstTimeout.get(Calendar.MONTH) != Calendar.MARCH ||
                firstTimeout.get(Calendar.DAY_OF_MONTH) != 12 ||
                firstTimeout.get(Calendar.HOUR_OF_DAY) != 1 ||
                firstTimeout.get(Calendar.MINUTE) != 30 ||
                firstTimeout.get(Calendar.SECOND) != 0 ||
                firstTimeout.get(Calendar.DST_OFFSET) != 0) {
            Assert.fail("Start time unexpected : " + firstTimeout.toString());
        }
        Calendar current = firstTimeout;
        for(int i = 0 ; i<3 ; i++) {
            Calendar next = calendarTimeout.getNextTimeout(current);
            if(current.getTimeInMillis() != (next.getTimeInMillis() - 900000)) {
                Assert.fail("Schedule is more than 15 minutes from " + current.getTime() + " to " + next.getTime());
            }
            current = next;
        }
        if(current.get(Calendar.YEAR) != 2017 ||
                current.get(Calendar.MONTH) != Calendar.MARCH ||
                current.get(Calendar.DAY_OF_MONTH) != 12 ||
                current.get(Calendar.HOUR_OF_DAY) != 3 ||
                current.get(Calendar.MINUTE) != 15 ||
                current.get(Calendar.DST_OFFSET) != 3600000) {
            Assert.fail("End time unexpected : " + current.toString());
        }
    }

    /**
     * Change PST summer time to PST winter time.
     * The timer should be fired every 15 minutes (absolutely).
     * This is a test for WFLY-9537
     */
    @Test
    public void testChangeUS2Winter() {
        Calendar start = new GregorianCalendar(TimeZone.getTimeZone("America/Los_Angeles"));
        start.clear();
        // set half an hour before Los Angeles time switch to winter time
        start.set(2017, Calendar.NOVEMBER, 5, 0, 30, 0);

        ScheduleExpression schedule = new ScheduleExpression();
        schedule.hour("*")
                .minute("0/15")
                .second("0")
                .timezone("America/Los_Angeles")  // don't fail the check below if running in a not default TZ
                .start(start.getTime());
        CalendarBasedTimeout calendarTimeout = new CalendarBasedTimeout(schedule);
        Calendar firstTimeout = calendarTimeout.getFirstTimeout();
        // assert first timeout result
        Assert.assertNotNull(firstTimeout);
        if(firstTimeout.get(Calendar.YEAR) != 2017 ||
                firstTimeout.get(Calendar.MONTH) != Calendar.NOVEMBER ||
                firstTimeout.get(Calendar.DAY_OF_MONTH) != 5 ||
                firstTimeout.get(Calendar.HOUR_OF_DAY) != 0 ||
                firstTimeout.get(Calendar.MINUTE) != 30 ||
                firstTimeout.get(Calendar.SECOND) != 0 ||
                firstTimeout.get(Calendar.DST_OFFSET) != 3600000) {
            Assert.fail("Start time unexpected : " + firstTimeout.toString());
        }
        Calendar current = firstTimeout;
        for(int i = 0 ; i<7 ; i++) {
            Calendar next = calendarTimeout.getNextTimeout(current);
            if(current.getTimeInMillis() != (next.getTimeInMillis() - 900000)) {
                Assert.fail("Schedule is more than 15 minutes from " + current.getTime() + " to " + next.getTime());
            }
            current = next;
        }
        if(current.get(Calendar.YEAR) != 2017 ||
                current.get(Calendar.MONTH) != Calendar.NOVEMBER ||
                current.get(Calendar.DAY_OF_MONTH) != 5 ||
                current.get(Calendar.HOUR_OF_DAY) != 1 ||
                current.get(Calendar.MINUTE) != 15 ||
                current.get(Calendar.DST_OFFSET) != 0) {
            Assert.fail("End time unexpected : " + current.toString());
        }
    }

    /**
     * This test asserts that the timer increments in seconds, minutes and hours
     * are the same for a complete year using a DST timezone and a non-DST timezone.
     *
     * This test covers WFLY-10106 issue.
     */
    @Test
    public void testTimeoutIncrements(){
        TimeZone dstTimezone = TimeZone.getTimeZone("Atlantic/Canary");
        TimeZone nonDstTimezone = TimeZone.getTimeZone("Africa/Abidjan");

        Assert.assertTrue(dstTimezone.useDaylightTime());
        Assert.assertTrue(!nonDstTimezone.useDaylightTime());

        for (TimeZone tz : Arrays.asList(dstTimezone, nonDstTimezone)) {
            this.timezone = tz;

            testSecondIncrement();
            testMinutesIncrement();
            testHoursIncrement();
        }
    }

    public void testSecondIncrement() {
        Calendar start = new GregorianCalendar(timezone);
        start.clear();
        start.set(2018, Calendar.JANUARY, 1, 10, 00, 0);

        ScheduleExpression schedule = new ScheduleExpression();
        schedule.hour("*")
                .minute("*")
                .second("*/30")
                .timezone(timezone.getID())
                .start(start.getTime());
        CalendarBasedTimeout calendarTimeout = new CalendarBasedTimeout(schedule);
        Calendar firstTimeout = calendarTimeout.getFirstTimeout();

        Assert.assertNotNull(firstTimeout);

        if(firstTimeout.get(Calendar.YEAR) != 2018 ||
                firstTimeout.get(Calendar.MONTH) != Calendar.JANUARY ||
                firstTimeout.get(Calendar.DAY_OF_MONTH) != 1 ||
                firstTimeout.get(Calendar.HOUR_OF_DAY) != 10 ||
                firstTimeout.get(Calendar.MINUTE) != 0 ||
                firstTimeout.get(Calendar.SECOND) != 0 ||
                firstTimeout.get(Calendar.DST_OFFSET) != start.get(Calendar.DST_OFFSET) ) {
            Assert.fail("Start time unexpected : " + firstTimeout.toString());
        }

        Calendar current = firstTimeout;
        long numInvocations = 366*24*60*60/30;
        long millisecondsDiff = 30*1000;
        for(int i = 0 ; i<numInvocations; i++) {
            Calendar next = calendarTimeout.getNextTimeout(current);
            if(current.getTimeInMillis() != (next.getTimeInMillis() - millisecondsDiff)) {
                Assert.fail("Schedule is more than 30 seconds from " + current.getTime() + " to " + next.getTime());
            }
            current = next;
        }

        if(current.get(Calendar.YEAR) != 2019 ||
                current.get(Calendar.MONTH) != Calendar.JANUARY ||
                current.get(Calendar.DAY_OF_MONTH) != 2 ||
                current.get(Calendar.HOUR_OF_DAY) != 10 ||
                current.get(Calendar.MINUTE) != 0 ||
                current.get(Calendar.SECOND) != 0 ||
                current.get(Calendar.DST_OFFSET) != start.get(Calendar.DST_OFFSET) ) {
            Assert.fail("End time unexpected : " + current.toString());
        }
    }

    public void testMinutesIncrement() {
        Calendar start = new GregorianCalendar(timezone);
        start.clear();
        start.set(2017, Calendar.JANUARY, 1, 0, 0, 0);

        ScheduleExpression schedule = new ScheduleExpression();
        schedule.hour("*")
                .minute("*/15")
                .second("0")
                .timezone(timezone.getID())
                .start(start.getTime());
        CalendarBasedTimeout calendarTimeout = new CalendarBasedTimeout(schedule);
        Calendar firstTimeout = calendarTimeout.getFirstTimeout();

        Assert.assertNotNull(firstTimeout);

        if(firstTimeout.get(Calendar.YEAR) != 2017 ||
                firstTimeout.get(Calendar.MONTH) != Calendar.JANUARY ||
                firstTimeout.get(Calendar.DAY_OF_MONTH) != 1 ||
                firstTimeout.get(Calendar.HOUR_OF_DAY) != 0 ||
                firstTimeout.get(Calendar.MINUTE) != 0 ||
                firstTimeout.get(Calendar.SECOND) != 0 ||
                firstTimeout.get(Calendar.DST_OFFSET) != start.get(Calendar.DST_OFFSET) ) {
            Assert.fail("Start time unexpected : " + firstTimeout.toString());
        }

        Calendar current = firstTimeout;
        long numInvocations = 366*24*60/15;
        long millisecondsDiff = 15*60*1000;
        for(int i = 0 ; i<numInvocations; i++) {
            Calendar next = calendarTimeout.getNextTimeout(current);
            if(current.getTimeInMillis() != (next.getTimeInMillis() - millisecondsDiff)) {
                Assert.fail("Schedule is more than 15 minutes from " + current.getTime() + " to " + next.getTime());
            }
            current = next;
        }

        if(current.get(Calendar.YEAR) != 2018 ||
                current.get(Calendar.MONTH) != Calendar.JANUARY ||
                current.get(Calendar.DAY_OF_MONTH) != 2 ||
                current.get(Calendar.HOUR_OF_DAY) != 0 ||
                current.get(Calendar.MINUTE) != 0 ||
                current.get(Calendar.SECOND) != 0 ||
                current.get(Calendar.DST_OFFSET) != start.get(Calendar.DST_OFFSET) ) {
            Assert.fail("End time unexpected : " + current.toString());
        }
    }

    public void testHoursIncrement() {
        Calendar start = new GregorianCalendar(timezone);
        start.clear();
        start.set(2017, Calendar.JANUARY, 1, 0, 0, 0);

        ScheduleExpression schedule = new ScheduleExpression();
        schedule.hour("*")
                .minute("30")
                .second("0")
                .timezone(timezone.getID())
                .start(start.getTime());

        CalendarBasedTimeout calendarTimeout = new CalendarBasedTimeout(schedule);
        Calendar firstTimeout = calendarTimeout.getFirstTimeout();

        Assert.assertNotNull(firstTimeout);

        if(firstTimeout.get(Calendar.YEAR) != 2017 ||
                firstTimeout.get(Calendar.MONTH) != Calendar.JANUARY ||
                firstTimeout.get(Calendar.DAY_OF_MONTH) != 1 ||
                firstTimeout.get(Calendar.HOUR_OF_DAY) != 0 ||
                firstTimeout.get(Calendar.MINUTE) != 30 ||
                firstTimeout.get(Calendar.SECOND) != 0 ||
                firstTimeout.get(Calendar.DST_OFFSET) != start.get(Calendar.DST_OFFSET) ) {
            Assert.fail("Start time unexpected : " + firstTimeout.toString());
        }

        Calendar current = firstTimeout;
        long numInvocations = 366*24;
        long millisecondsDiff = 60*60*1000;
        for(int i = 0 ; i<numInvocations; i++) {
            Calendar next = calendarTimeout.getNextTimeout(current);
            if(current.getTimeInMillis() != (next.getTimeInMillis() - millisecondsDiff)) {
                Assert.fail("Schedule is more than 1 hours from " + current.getTime() + " to " + next.getTime() + " for timezone " + timezone.getID());
            }
            current = next;
        }

        if(current.get(Calendar.YEAR) != 2018 ||
                current.get(Calendar.MONTH) != Calendar.JANUARY ||
                current.get(Calendar.DAY_OF_MONTH) != 2 ||
                current.get(Calendar.HOUR_OF_DAY) != 0 ||
                current.get(Calendar.MINUTE) != 30 ||
                current.get(Calendar.SECOND) != 0 ||
                current.get(Calendar.DST_OFFSET) != start.get(Calendar.DST_OFFSET) ) {
            Assert.fail("End time unexpected : " + current.toString());
        }
    }

    /**
     * This test asserts that a timer scheduled to run during the ambiguous hour when the
     * Daylight Savings period ends is not executed twice.
     *
     * It configures a timer to be fired on October 29, 2017 at 01:30:00 in Europe/Lisbon TZ.
     * There are two 01:30:00 that day: 01:30:00 WEST and 01:30:00 WET. The timer has to be
     * fired just once.
     */
    @Test
    public void testTimerAtAmbiguousHourWESTtoWET() {
        // WEST -> WET
        // Sunday, 29 October 2017, 02:00:00 -> 01:00:00
        Calendar start = new GregorianCalendar(TimeZone.getTimeZone("Europe/Lisbon"));
        start.clear();
        start.set(2017, Calendar.OCTOBER, 29, 0, 0, 0);

        ScheduleExpression schedule = new ScheduleExpression();
        schedule.hour("1")
                .minute("30")
                .second("0")
                .timezone("Europe/Lisbon")
                .start(start.getTime());
        CalendarBasedTimeout calendarTimeout = new CalendarBasedTimeout(schedule);

        Calendar timeout = calendarTimeout.getFirstTimeout();

        Assert.assertNotNull(timeout);

        //Assert timeout is 29 October at 01:30 WEST
        if (timeout.get(Calendar.YEAR) != 2017 ||
                timeout.get(Calendar.MONTH) != Calendar.OCTOBER ||
                timeout.get(Calendar.DAY_OF_MONTH) != 29 ||
                timeout.get(Calendar.HOUR_OF_DAY) != 1 ||
                timeout.get(Calendar.MINUTE) != 30 ||
                timeout.get(Calendar.SECOND) != 0 ||
                timeout.get(Calendar.DST_OFFSET) != 3600000) {
            Assert.fail("Time unexpected : " + timeout.toString());
        }

        //Asserts elapsed time from start was 1h 30min:
        Assert.assertTrue("Schedule is more than 1h 30min hours from " + start.getTime() + " to " + timeout.getTime(), timeout.getTimeInMillis()-start.getTimeInMillis() == 1*60*60*1000 + 30*60*1000);

        timeout = calendarTimeout.getNextTimeout(timeout);

        //Assert timeout is 30 October at 01:30 WET
        if (timeout.get(Calendar.YEAR) != 2017 ||
                timeout.get(Calendar.MONTH) != Calendar.OCTOBER ||
                timeout.get(Calendar.DAY_OF_MONTH) != 30 ||
                timeout.get(Calendar.HOUR_OF_DAY) != 1 ||
                timeout.get(Calendar.MINUTE) != 30 ||
                timeout.get(Calendar.SECOND) != 0 ||
                timeout.get(Calendar.DST_OFFSET) != 0) {
            Assert.fail("Time unexpected : " + timeout.toString());
        }
    }

    /**
     * This test asserts that a timer scheduled to run during the removed hour when the
     * Daylight Savings period starts is executed.
     *
     * It configures a timer to be fired on March 26, 2017 at 03:30:00 in Europe/Helsinki TZ.
     * This hour does not exist in that timezone, this test asserts the timer is fired once
     * during this ambiguous hour.
     */
    @Test
    public void testTimerAtAmbiguousHourEETtoEEST() {
        // EET --> EEST
        // Sunday, 26 March 2017, 03:00:00 --> 04:00:00
        Calendar start = new GregorianCalendar(TimeZone.getTimeZone("Europe/Helsinki"));
        start.clear();
        start.set(2017, Calendar.MARCH, 26, 0, 0, 0);

        ScheduleExpression schedule = new ScheduleExpression();
        schedule.hour("3")
                .minute("30")
                .second("0")
                .timezone("Europe/Helsinki")
                .start(start.getTime());
        CalendarBasedTimeout calendarTimeout = new CalendarBasedTimeout(schedule);

        Calendar timeout = calendarTimeout.getFirstTimeout();

        Assert.assertNotNull(timeout);

        //Assert timeout is 26 March at 03:30 EET
        if (timeout.get(Calendar.YEAR) != 2017 ||
                timeout.get(Calendar.MONTH) != Calendar.MARCH ||
                timeout.get(Calendar.DAY_OF_MONTH) != 26 ||
                timeout.get(Calendar.HOUR_OF_DAY) != 3 ||
                timeout.get(Calendar.MINUTE) != 30 ||
                timeout.get(Calendar.SECOND) != 0 ||
                timeout.get(Calendar.DST_OFFSET) != 0) {
            Assert.fail("Time unexpected : " + timeout.toString());
        }

        //Asserts elapsed time from start was 3h 30min:
        Assert.assertTrue("Schedule is more than 3h 30min hours from " + start.getTime() + " to " + timeout.getTime(), timeout.getTimeInMillis()-start.getTimeInMillis() == 3*60*60*1000 + 30*60*1000);

        timeout = calendarTimeout.getNextTimeout(timeout);

        //Assert timeout is 27 March at 03:30 EEST
        if (timeout.get(Calendar.YEAR) != 2017 ||
                timeout.get(Calendar.MONTH) != Calendar.MARCH ||
                timeout.get(Calendar.DAY_OF_MONTH) != 27 ||
                timeout.get(Calendar.HOUR_OF_DAY) != 3 ||
                timeout.get(Calendar.MINUTE) != 30 ||
                timeout.get(Calendar.SECOND) != 0 ||
                timeout.get(Calendar.DST_OFFSET) != 3600000) {
            Assert.fail("Time unexpected : " + timeout.toString());
        }
    }

    private ScheduleExpression getTimezoneSpecificScheduleExpression() {
        ScheduleExpression scheduleExpression = new ScheduleExpression().timezone(this.timezone.getID());
        GregorianCalendar start = new GregorianCalendar(this.timezone);
        start.clear();
        start.set(2014,0,1,1,0,0);
        return scheduleExpression.start(start.getTime());
    }

    private boolean isLeapYear(Calendar cal) {
        int year = cal.get(Calendar.YEAR);
        return year % 4 == 0;
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
