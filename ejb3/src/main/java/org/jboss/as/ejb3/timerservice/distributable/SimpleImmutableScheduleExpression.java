/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2022, Red Hat, Inc., and individual contributors
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

import static org.jboss.as.ejb3.logging.EjbLogger.EJB3_TIMER_LOGGER;

import java.time.DateTimeException;
import java.time.Instant;
import java.time.ZoneId;
import java.util.Date;

import jakarta.ejb.ScheduleExpression;

import org.wildfly.clustering.ejb.timer.ImmutableScheduleExpression;

/**
 * An immutable copy of a {@link ScheduleExpression}.
 * @author Paul Ferraro
 */
public class SimpleImmutableScheduleExpression implements ImmutableScheduleExpression {
    static final ZoneId DEFAULT_ZONE_ID = ZoneId.systemDefault();

    private final String second;
    private final String minute;
    private final String hour;
    private final String dayOfMonth;
    private final String month;
    private final String dayOfWeek;
    private final String year;

    private final ZoneId zone;
    private final Instant start;
    private final Instant end;

    public SimpleImmutableScheduleExpression(ScheduleExpression expression) {
        this.second = expression.getSecond();
        this.minute = expression.getMinute();
        this.hour = expression.getHour();
        this.dayOfMonth = expression.getDayOfMonth();
        this.month = expression.getMonth();
        this.dayOfWeek = expression.getDayOfWeek();
        this.year = expression.getYear();
        this.zone = createZoneId(expression.getTimezone());
        Date start = expression.getStart();
        Date end = expression.getEnd();
        this.start = (start != null) ? start.toInstant() : null;
        this.end = (end != null) ? end.toInstant() : null;
    }

    private static ZoneId createZoneId(String id) {
        if (id == null) return DEFAULT_ZONE_ID;
        try {
            return ZoneId.of(id.trim());
        } catch (DateTimeException e) {
            EJB3_TIMER_LOGGER.unknownTimezoneId(id, DEFAULT_ZONE_ID.getId());
            return DEFAULT_ZONE_ID;
        }
    }

    @Override
    public String getSecond() {
        return this.second;
    }

    @Override
    public String getMinute() {
        return this.minute;
    }

    @Override
    public String getHour() {
        return this.hour;
    }

    @Override
    public String getDayOfMonth() {
        return this.dayOfMonth;
    }

    @Override
    public String getMonth() {
        return this.month;
    }

    @Override
    public String getDayOfWeek() {
        return this.dayOfWeek;
    }

    @Override
    public String getYear() {
        return this.year;
    }

    @Override
    public ZoneId getZone() {
        return this.zone;
    }

    @Override
    public Instant getStart() {
        return this.start;
    }

    @Override
    public Instant getEnd() {
        return this.end;
    }
}
