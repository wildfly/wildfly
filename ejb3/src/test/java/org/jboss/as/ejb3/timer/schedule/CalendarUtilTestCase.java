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

import org.jboss.as.ejb3.timerservice.schedule.util.CalendarUtil;
import org.junit.Assert;
import org.junit.Test;

/**
 * Tests {@link CalendarUtil}
 *
 * @author Jaikiran Pai
 * @version $Revision: $
 */
public class CalendarUtilTestCase {

    /**
     * Tests that the {@link CalendarUtil#getLastDateOfMonth(java.util.Calendar)} returns the correct
     * date for various months.
     */
    @Test
    public void testLastDateOfMonth() {
        // check last date of march
        Calendar march = new GregorianCalendar();
        march.set(Calendar.MONTH, Calendar.MARCH);
        march.set(Calendar.DAY_OF_MONTH, 1);

        int lastDateOfMarch = CalendarUtil.getLastDateOfMonth(march);
        Assert.assertEquals("Unexpected last date for march", 31, lastDateOfMarch);

        // check for april
        Calendar april = new GregorianCalendar();
        april.set(Calendar.MONTH, Calendar.APRIL);
        april.set(Calendar.DAY_OF_MONTH, 1);

        int lastDateOfApril = CalendarUtil.getLastDateOfMonth(april);
        Assert.assertEquals("Unexpected last date for april", 30, lastDateOfApril);

        // check of february (non-leap year)
        Calendar nonLeapFebruary = new GregorianCalendar();
        nonLeapFebruary.set(Calendar.MONTH, Calendar.FEBRUARY);
        nonLeapFebruary.set(Calendar.YEAR, 2010);
        nonLeapFebruary.set(Calendar.DAY_OF_MONTH, 1);

        int lastDateOfNonLeapFebruary = CalendarUtil.getLastDateOfMonth(nonLeapFebruary);
        Assert.assertEquals("Unexpected last date for non-leap february", 28, lastDateOfNonLeapFebruary);

        // check for february (leap year)
        Calendar leapFebruary = new GregorianCalendar();
        leapFebruary.set(Calendar.MONTH, Calendar.FEBRUARY);
        leapFebruary.set(Calendar.YEAR, 2012);
        leapFebruary.set(Calendar.DAY_OF_MONTH, 1);

        int lastDateOfLeapFebruary = CalendarUtil.getLastDateOfMonth(leapFebruary);
        Assert.assertEquals("Unexpected last date for leap february", 29, lastDateOfLeapFebruary);

    }

    @Test
    public void test1stXXXDay() {
        final int FIRST_WEEK = 1;

        // 1st Sun of July 2010 (hardcoded, after checking with the system calendar)
        int expectedDateOfFirstSunOfJuly2010 = 4;
        Calendar july2010 = new GregorianCalendar();
        july2010.set(Calendar.DAY_OF_MONTH, 1);
        july2010.set(Calendar.MONTH, Calendar.JULY);
        july2010.set(Calendar.YEAR, 2010);

        int dateOfFirstSunOfJuly2010 = CalendarUtil.getNthDayOfMonth(july2010, FIRST_WEEK, Calendar.SUNDAY);
        Assert.assertEquals("Unexpected date for 1st sunday of July 2010", expectedDateOfFirstSunOfJuly2010,
                dateOfFirstSunOfJuly2010);

        // 1st Mon of June 2009
        int expectedDateOfFirstMonOfJune2009 = 1;
        Calendar june2009 = new GregorianCalendar();
        june2009.set(Calendar.DAY_OF_MONTH, 1);
        june2009.set(Calendar.MONTH, Calendar.JUNE);
        june2009.set(Calendar.YEAR, 2009);

        int dateOfFirstMonJune2009 = CalendarUtil.getNthDayOfMonth(june2009, FIRST_WEEK, Calendar.MONDAY);
        Assert.assertEquals("Unexpected date for 1st monday of June 2009", expectedDateOfFirstMonOfJune2009,
                dateOfFirstMonJune2009);

        // 1st Tue of Feb 2012
        int expectedDateOfFirstTueOfFeb2012 = 7;
        Calendar feb2012 = new GregorianCalendar();
        feb2012.set(Calendar.DAY_OF_MONTH, 1);
        feb2012.set(Calendar.MONTH, Calendar.FEBRUARY);
        feb2012.set(Calendar.YEAR, 2012);

        int dateOfFirstTueFeb2012 = CalendarUtil.getNthDayOfMonth(feb2012, FIRST_WEEK, Calendar.TUESDAY);
        Assert.assertEquals("Unexpected date for 1st tuesday of Feb 2012", expectedDateOfFirstTueOfFeb2012,
                dateOfFirstTueFeb2012);

        // 1st Wed of Jan 2006
        int expectedDateOfFirstMonOfJan2006 = 4;
        Calendar jan2006 = new GregorianCalendar();
        jan2006.set(Calendar.DAY_OF_MONTH, 1);
        jan2006.set(Calendar.MONTH, Calendar.JANUARY);
        jan2006.set(Calendar.YEAR, 2006);

        int dateOfFirstWedJan2006 = CalendarUtil.getNthDayOfMonth(jan2006, FIRST_WEEK, Calendar.WEDNESDAY);
        Assert.assertEquals("Unexpected date for 1st wednesday of Jan 2006", expectedDateOfFirstMonOfJan2006,
                dateOfFirstWedJan2006);

        // 1st Thu of June 1999
        int expectedDateOfFirstThuOfSep1999 = 2;
        Calendar sep1999 = new GregorianCalendar();
        sep1999.set(Calendar.DAY_OF_MONTH, 1);
        sep1999.set(Calendar.MONTH, Calendar.SEPTEMBER);
        sep1999.set(Calendar.YEAR, 1999);

        int dateOfFirstThuSep1999 = CalendarUtil.getNthDayOfMonth(sep1999, FIRST_WEEK, Calendar.THURSDAY);
        Assert.assertEquals("Unexpected date for 1st thursday of September 1999", expectedDateOfFirstThuOfSep1999,
                dateOfFirstThuSep1999);

        // 1st Fri of Dec 2058
        int expectedDateOfFirstFriOfDec2058 = 6;
        Calendar dec2058 = new GregorianCalendar();
        dec2058.set(Calendar.DAY_OF_MONTH, 1);
        dec2058.set(Calendar.MONTH, Calendar.DECEMBER);
        dec2058.set(Calendar.YEAR, 2058);

        int dateOfFirstFriDec2058 = CalendarUtil.getNthDayOfMonth(dec2058, FIRST_WEEK, Calendar.FRIDAY);
        Assert.assertEquals("Unexpected date for 1st friday of December 2058", expectedDateOfFirstFriOfDec2058,
                dateOfFirstFriDec2058);

        // 1st Sat of Aug 2000
        int expectedDateOfFirstSatOfAug2000 = 5;
        Calendar aug2000 = new GregorianCalendar();
        aug2000.set(Calendar.DAY_OF_MONTH, 1);
        aug2000.set(Calendar.MONTH, Calendar.AUGUST);
        aug2000.set(Calendar.YEAR, 2000);

        int dateOfFirstSatAug2000 = CalendarUtil.getNthDayOfMonth(aug2000, FIRST_WEEK, Calendar.SATURDAY);
        Assert.assertEquals("Unexpected date for 1st saturday of August 2000", expectedDateOfFirstSatOfAug2000,
                dateOfFirstSatAug2000);

    }

    @Test
    public void test2ndXXXDay() {
        final int SECOND_WEEK = 2;

        // 2nd Sun of May 2010 (hardcoded, after checking with the system calendar)
        int expectedDateOfSecondSunOfMay2010 = 9;
        Calendar may2010 = new GregorianCalendar();
        may2010.set(Calendar.MONTH, Calendar.MAY);
        may2010.set(Calendar.YEAR, 2010);

        int dateOfSecondSunOfMay2010 = CalendarUtil.getNthDayOfMonth(may2010, SECOND_WEEK, Calendar.SUNDAY);
        Assert.assertEquals("Unexpected date for 2nd sunday of May 2010", expectedDateOfSecondSunOfMay2010,
                dateOfSecondSunOfMay2010);

        // 2nd Mon of Feb 2111
        int expectedDateOfSecondMonOfFeb2111 = 9;
        Calendar feb2111 = new GregorianCalendar();
        feb2111.set(Calendar.MONTH, Calendar.FEBRUARY);
        feb2111.set(Calendar.YEAR, 2111);

        int dateOfSecondMonFeb2111 = CalendarUtil.getNthDayOfMonth(feb2111, SECOND_WEEK, Calendar.MONDAY);
        Assert.assertEquals("Unexpected date for 2nd monday of Feb 2111", expectedDateOfSecondMonOfFeb2111,
                dateOfSecondMonFeb2111);

        // 2nd Tue of Oct 2016
        int expectedDateOfSecondTueOct2016 = 11;
        Calendar oct2016 = new GregorianCalendar();
        oct2016.set(Calendar.MONTH, Calendar.OCTOBER);
        oct2016.set(Calendar.YEAR, 2016);

        int dateOfSecondTueOct2016 = CalendarUtil.getNthDayOfMonth(oct2016, SECOND_WEEK, Calendar.TUESDAY);
        Assert.assertEquals("Unexpected date for 2nd tuesday of Oct 2016", expectedDateOfSecondTueOct2016,
                dateOfSecondTueOct2016);

        // 2nd Wed of Apr 2010
        int expectedDateOfSecWedApr2010 = 14;
        Calendar apr2010 = new GregorianCalendar();
        apr2010.set(Calendar.DAY_OF_MONTH, 1);
        apr2010.set(Calendar.MONTH, Calendar.APRIL);
        apr2010.set(Calendar.YEAR, 2010);

        int dateOfSecondWedApril2010 = CalendarUtil.getNthDayOfMonth(apr2010, SECOND_WEEK, Calendar.WEDNESDAY);
        Assert.assertEquals("Unexpected date for 2nd wednesday of April 2010", expectedDateOfSecWedApr2010,
                dateOfSecondWedApril2010);

        // 2nd Thu of Mar 2067
        int expectedDateOfSecondThuMar2067 = 10;
        Calendar march2067 = new GregorianCalendar();
        march2067.set(Calendar.DAY_OF_MONTH, 1);
        march2067.set(Calendar.MONTH, Calendar.MARCH);
        march2067.set(Calendar.YEAR, 2067);

        int dateOfSecThuMarch2067 = CalendarUtil.getNthDayOfMonth(march2067, SECOND_WEEK, Calendar.THURSDAY);
        Assert.assertEquals("Unexpected date for 2nd thursday of March 2067", expectedDateOfSecondThuMar2067,
                dateOfSecThuMarch2067);

        // 2nd Fri of Nov 2020
        int expectedDateOfSecFriNov2020 = 13;
        Calendar nov2020 = new GregorianCalendar();
        nov2020.set(Calendar.DAY_OF_MONTH, 1);
        nov2020.set(Calendar.MONTH, Calendar.NOVEMBER);
        nov2020.set(Calendar.YEAR, 2020);

        int dateOfFirstFriDec2058 = CalendarUtil.getNthDayOfMonth(nov2020, SECOND_WEEK, Calendar.FRIDAY);
        Assert.assertEquals("Unexpected date for 2nd friday of November 2020", expectedDateOfSecFriNov2020,
                dateOfFirstFriDec2058);

        // 2nd Sat of Sep 2013
        int expectedDateOfSecSatOfSep2013 = 14;
        Calendar aug2000 = new GregorianCalendar();
        aug2000.set(Calendar.DAY_OF_MONTH, 1);
        aug2000.set(Calendar.MONTH, Calendar.SEPTEMBER);
        aug2000.set(Calendar.YEAR, 2013);

        int dateOfSecSatSep2013 = CalendarUtil.getNthDayOfMonth(aug2000, SECOND_WEEK, Calendar.SATURDAY);
        Assert.assertEquals("Unexpected date for 2nd saturday of September 2013", expectedDateOfSecSatOfSep2013,
                dateOfSecSatSep2013);

    }

    @Test
    public void test3rdXXXDay() {
        // 1st Sun of July 2010 (hardcoded, after checking with the system calendar)
        int expectedDateOfFirstSunOfJuly2010 = 4;
        Calendar july2010 = new GregorianCalendar();
        july2010.set(Calendar.MONTH, Calendar.JULY);
        july2010.set(Calendar.YEAR, 2010);

        int dateOfFirstSunOfJuly2010 = CalendarUtil.getNthDayOfMonth(july2010, 1, Calendar.SUNDAY);
        Assert.assertEquals("Unexpected date for 1st sunday of July 2010", expectedDateOfFirstSunOfJuly2010,
                dateOfFirstSunOfJuly2010);

    }

    @Test
    public void test4thXXXDay() {
        // 1st Sun of July 2010 (hardcoded, after checking with the system calendar)
        int expectedDateOfFirstSunOfJuly2010 = 4;
        Calendar july2010 = new GregorianCalendar();
        july2010.set(Calendar.MONTH, Calendar.JULY);
        july2010.set(Calendar.YEAR, 2010);

        int dateOfFirstSunOfJuly2010 = CalendarUtil.getNthDayOfMonth(july2010, 1, Calendar.SUNDAY);
        Assert.assertEquals("Unexpected date for 1st sunday of July 2010", expectedDateOfFirstSunOfJuly2010,
                dateOfFirstSunOfJuly2010);

    }

    @Test
    public void test5thXXXDay() {
        // 1st Sun of July 2010 (hardcoded, after checking with the system calendar)
        int expectedDateOfFirstSunOfJuly2010 = 4;
        Calendar july2010 = new GregorianCalendar();
        july2010.set(Calendar.MONTH, Calendar.JULY);
        july2010.set(Calendar.YEAR, 2010);

        int dateOfFirstSunOfJuly2010 = CalendarUtil.getNthDayOfMonth(july2010, 1, Calendar.SUNDAY);
        Assert.assertEquals("Unexpected date for 1st sunday of July 2010", expectedDateOfFirstSunOfJuly2010,
                dateOfFirstSunOfJuly2010);

    }

    @Test
    public void test1stSunday() {
        // 1st Sun of July 2010 (hardcoded, after checking with the system calendar)
        int expectedDateOfFirstSunOfJuly2010 = 4;
        Calendar july2010 = new GregorianCalendar();
        july2010.set(Calendar.MONTH, Calendar.JULY);
        july2010.set(Calendar.YEAR, 2010);

        int dateOfFirstSunOfJuly2010 = CalendarUtil.getNthDayOfMonth(july2010, 1, Calendar.SUNDAY);
        Assert.assertEquals("Unexpected date for 1st sunday of July 2010", expectedDateOfFirstSunOfJuly2010,
                dateOfFirstSunOfJuly2010);

    }

}
