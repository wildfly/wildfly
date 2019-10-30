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
package org.jboss.as.ejb3.timerservice.schedule.util;

import java.util.Calendar;
import java.util.GregorianCalendar;

/**
 * CalendarUtil
 *
 * @author Jaikiran Pai
 * @version $Revision: $
 */
public class CalendarUtil {


    /**
     * Returns the last date of the month represented by the passed <code>cal</code>
     *
     * @param calendar The {@link java.util.Calendar} whose {@link java.util.Calendar#MONTH} field will be used
     *                 as the current month
     * @return
     */
    public static int getLastDateOfMonth(Calendar calendar) {
        Calendar tmpCal = new GregorianCalendar(calendar.getTimeZone());
        tmpCal.set(Calendar.YEAR, calendar.get(Calendar.YEAR));
        tmpCal.set(Calendar.MONTH, calendar.get(Calendar.MONTH));
        tmpCal.set(Calendar.DAY_OF_MONTH, 1);
        return tmpCal.getActualMaximum(Calendar.DAY_OF_MONTH);
    }

    public static Integer getNthDayOfMonth(Calendar cal, int n, int dayOfWeek) {
        int dateOfFirstXXXDay = getFirstDateInMonthForDayOfWeek(cal, dayOfWeek);

        final int FIRST_WEEK = 1;
        final int NUM_DAYS_IN_WEEK = 7;

        int weekDiff = n - FIRST_WEEK;

        int dateOfNthXXXDayInMonth = dateOfFirstXXXDay + (weekDiff * NUM_DAYS_IN_WEEK);
        int maxDateInCurrentMonth = CalendarUtil.getLastDateOfMonth(cal);
        if (dateOfNthXXXDayInMonth > maxDateInCurrentMonth) {
            return null;
        }

        return dateOfNthXXXDayInMonth;
    }

    public static int getDateOfLastDayOfWeekInMonth(Calendar calendar, int dayOfWeek) {
        int lastDateOfMonth = getLastDateOfMonth(calendar);
        Calendar tmpCal = new GregorianCalendar(calendar.getTimeZone());
        tmpCal.set(Calendar.YEAR, calendar.get(Calendar.YEAR));
        tmpCal.set(Calendar.MONTH, calendar.get(Calendar.MONTH));
        tmpCal.set(Calendar.DATE, lastDateOfMonth);

        int day = tmpCal.get(Calendar.DAY_OF_WEEK);
        if (day == dayOfWeek) {
            return tmpCal.get(Calendar.DATE);
        }
        while (day != dayOfWeek) {
            tmpCal.add(Calendar.DATE, -1);
            day = tmpCal.get(Calendar.DAY_OF_WEEK);
        }
        return tmpCal.get(Calendar.DATE);
    }

    private static int getFirstDateInMonthForDayOfWeek(Calendar cal, final int dayOfWeek) {
        Calendar tmpCal = new GregorianCalendar(cal.getTimeZone());
        tmpCal.set(Calendar.YEAR, cal.get(Calendar.YEAR));
        tmpCal.set(Calendar.MONTH, cal.get(Calendar.MONTH));
        tmpCal.set(Calendar.DATE, 1);

        int day = tmpCal.get(Calendar.DAY_OF_WEEK);
        if (day == dayOfWeek) {
            return tmpCal.get(Calendar.DATE);
        }
        while (day != dayOfWeek) {
            tmpCal.add(Calendar.DATE, 1);
            day = tmpCal.get(Calendar.DAY_OF_WEEK);
        }
        return tmpCal.get(Calendar.DATE);
    }

}
