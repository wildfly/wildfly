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

import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.TimeZone;

import javax.ejb.ScheduleExpression;

import org.jboss.as.ejb3.timerservice.schedule.CalendarBasedTimeout;
import org.jboss.logging.Logger;
import org.junit.Assert;
import org.junit.Test;

/**
 * CalendarBasedTimeoutTestCase
 *
 * @author Brad Maxwell
 * @version $Revision: $
 */
public class CalendarBasedTimeoutWithDifferentTimeZoneTestCase {

    /**
     * Logger
     */
    private static Logger logger = Logger.getLogger(CalendarBasedTimeoutWithDifferentTimeZoneTestCase.class);

    /**
     * Construct a testcase for the timezone
     */
    public CalendarBasedTimeoutWithDifferentTimeZoneTestCase() {
    }

    @Test
    public void testScheduledTimezoneDifferentThanCurrentSystem() {

        // This tests replicates an automatic timer below being deployed in a system whose default timezone is America/Chicago
        // @Schedule(persistent = false, timezone = "America/New_York", dayOfMonth = "*", dayOfWeek = "*", month = "*", hour =
        // "2", minute = "*", second = "0", year = "*")

        // The schedule for the timer is to fire every minute where the hour is 2 in the America/New_York timezone
        ScheduleExpression sch = new ScheduleExpression();
        sch.timezone("America/New_York");
        sch.dayOfMonth("*");
        sch.dayOfWeek("*");
        sch.month("*");
        sch.hour("2");
        sch.minute("*");
        sch.second("0");
        sch.year("*");

        CalendarBasedTimeout calendarTimeout = new CalendarBasedTimeout(sch);

        Calendar firstTimeout = calendarTimeout.getFirstTimeout();
        Assert.assertNotNull("first timeout is null", firstTimeout);
        logger.info("First timeout is " + firstTimeout.getTime());

        // currentCal sets up a dummy time in the future, the timezone is America/Chicago in which this imaginary system is
        // running
        TimeZone currentTimezone = TimeZone.getTimeZone("America/Chicago");
        Calendar currentCal = new GregorianCalendar(currentTimezone);
        currentCal.set(Calendar.YEAR, 2014);
        currentCal.set(Calendar.MONTH, 1);
        currentCal.set(Calendar.DATE, 8);
        currentCal.set(Calendar.HOUR_OF_DAY, 1);
        currentCal.set(Calendar.MINUTE, 1);
        currentCal.set(Calendar.SECOND, 1);
        currentCal.set(Calendar.MILLISECOND, 0);

        // https://issues.jboss.org/browse/WFLY-2840 - @Schedule EJB Timer not using timezone when calcualting next timeout
        // Next test WFLY-2840, by calling getNextTimeout with the dummy time above, the expected result is for the timer to
        // fire at 1:02:00
        // If the bug is not fixed it will return 2:00:00

        Calendar nextTimeout = calendarTimeout.getNextTimeout(currentCal);
        logger.info("Next timeout is " + nextTimeout.getTime());

        Calendar expectedCal = new GregorianCalendar(currentTimezone);
        expectedCal.set(Calendar.YEAR, 2014);
        expectedCal.set(Calendar.MONTH, 1);
        expectedCal.set(Calendar.DATE, 8);
        expectedCal.set(Calendar.HOUR_OF_DAY, 1);
        expectedCal.set(Calendar.MINUTE, 2);
        expectedCal.set(Calendar.SECOND, 0);
        expectedCal.set(Calendar.MILLISECOND, 0);

        Assert.assertEquals("[WFLY-2840] Next timeout should be: " + expectedCal.getTime(), expectedCal.getTime(),
                nextTimeout.getTime());
    }

}