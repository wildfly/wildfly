/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2021, Red Hat, Inc., and individual contributors
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

package org.jboss.as.ejb3.timerservice.distributable;

import java.time.Instant;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.TimeZone;
import java.util.function.UnaryOperator;

import jakarta.ejb.ScheduleExpression;

import org.jboss.as.ejb3.timerservice.schedule.CalendarBasedTimeout;
import org.jboss.as.ejb3.timerservice.schedule.attribute.DayOfMonth;
import org.jboss.as.ejb3.timerservice.schedule.attribute.DayOfWeek;
import org.jboss.as.ejb3.timerservice.schedule.attribute.Hour;
import org.jboss.as.ejb3.timerservice.schedule.attribute.Minute;
import org.jboss.as.ejb3.timerservice.schedule.attribute.Month;
import org.jboss.as.ejb3.timerservice.schedule.attribute.Second;
import org.jboss.as.ejb3.timerservice.schedule.attribute.Year;
import org.kohsuke.MetaInfServices;
import org.wildfly.clustering.ejb.timer.ImmutableScheduleExpression;
import org.wildfly.clustering.ejb.timer.ScheduleTimerOperationProvider;

/**
 * Provides a mechanism for calculating the next timeout for a given {@link ScheduleExpression}.
 * @author Paul Ferraro
 */
@MetaInfServices(ScheduleTimerOperationProvider.class)
public class DefaultScheduleTimerOperationProvider implements ScheduleTimerOperationProvider {

    @Override
    public UnaryOperator<Instant> createOperator(ImmutableScheduleExpression expression) {
        return new DefaultScheduleTimerOperator(expression);
    }

    private static class DefaultScheduleTimerOperator implements UnaryOperator<Instant> {
        private final CalendarBasedTimeout timeout;
        private final Calendar first;

        DefaultScheduleTimerOperator(ImmutableScheduleExpression expression) {
            Instant start = expression.getStart();
            Instant end = expression.getEnd();
            this.timeout = new CalendarBasedTimeout(new Second(expression.getSecond()),
                        new Minute(expression.getMinute()),
                        new Hour(expression.getHour()),
                        new DayOfMonth(expression.getDayOfMonth()),
                        new Month(expression.getMonth()),
                        new DayOfWeek(expression.getDayOfWeek()),
                        new Year(expression.getYear()),
                        TimeZone.getTimeZone(expression.getZone()),
                        (start != null) ? Date.from(start) : null,
                        (end != null) ? Date.from(end) : null);
            this.first = (start != null) ? this.timeout.getFirstTimeout() : this.timeout.getNextTimeout();
        }

        @Override
        public Instant apply(Instant lastTimeout) {
            Calendar next = (lastTimeout != null) ? this.timeout.getNextTimeout(createCalendar(lastTimeout)) : this.first;
            return (next != null) ? next.toInstant() : null;
        }

        private static Calendar createCalendar(Instant instant) {
            Calendar calendar = new GregorianCalendar();
            calendar.setTimeInMillis(instant.toEpochMilli());
            return calendar;
        }
    }
}
