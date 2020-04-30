/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2020, Red Hat, Inc., and individual contributors
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

package org.wildfly.clustering.marshalling.spi.time;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.Month;
import java.time.OffsetDateTime;
import java.time.OffsetTime;
import java.time.Year;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;

import org.wildfly.clustering.marshalling.Externalizer;
import org.wildfly.clustering.marshalling.spi.BinaryExternalizer;
import org.wildfly.clustering.marshalling.spi.EnumExternalizer;
import org.wildfly.clustering.marshalling.spi.ExternalizerProvider;
import org.wildfly.clustering.marshalling.spi.IntExternalizer;
import org.wildfly.clustering.marshalling.spi.LongExternalizer;
import org.wildfly.clustering.marshalling.spi.StringExternalizer;

/**
 * Externalizers for the java.time package
 * @author Paul Ferraro
 */
public enum TimeExternalizerProvider implements ExternalizerProvider {

    DAY_OF_WEEK(new EnumExternalizer<>(DayOfWeek.class)),
    DURATION(new DurationExternalizer()),
    INSTANT(new InstantExternalizer()),
    LOCAL_DATE(new LongExternalizer<>(LocalDate.class, LocalDate::ofEpochDay, LocalDate::toEpochDay)),
    LOCAL_TIME(new LongExternalizer<>(LocalTime.class, LocalTime::ofNanoOfDay, LocalTime::toNanoOfDay)),
    LOCAL_DATE_TIME(new BinaryExternalizer<>(LocalDateTime.class, LOCAL_DATE.cast(LocalDate.class), LOCAL_TIME.cast(LocalTime.class), LocalDateTime::toLocalDate, LocalDateTime::toLocalTime, LocalDateTime::of)),
    MONTH(new EnumExternalizer<>(Month.class)),
    MONTH_DAY(new MonthDayExternalizer()),
    PERIOD(new PeriodExternalizer()),
    YEAR(new IntExternalizer<>(Year.class, Year::of, Year::getValue)),
    YEAR_MONTH(new YearMonthExternalizer()),
    ZONE_ID(new StringExternalizer<>(ZoneId.class, ZoneId::of, ZoneId::getId)),
    ZONE_OFFSET(new StringExternalizer<>(ZoneOffset.class, ZoneOffset::of, ZoneOffset::getId)),
    // w/offset
    OFFSET_DATE_TIME(new BinaryExternalizer<>(OffsetDateTime.class, LOCAL_DATE_TIME.cast(LocalDateTime.class), ZONE_OFFSET.cast(ZoneOffset.class), OffsetDateTime::toLocalDateTime, OffsetDateTime::getOffset, OffsetDateTime::of)),
    OFFSET_TIME(new BinaryExternalizer<>(OffsetTime.class, LOCAL_TIME.cast(LocalTime.class), ZONE_OFFSET.cast(ZoneOffset.class), OffsetTime::toLocalTime, OffsetTime::getOffset, OffsetTime::of)),
    ZONED_DATE_TIME(new BinaryExternalizer<>(ZonedDateTime.class, LOCAL_DATE_TIME.cast(LocalDateTime.class), ZONE_ID.cast(ZoneId.class), ZonedDateTime::toLocalDateTime, ZonedDateTime::getZone, ZonedDateTime::of)),
    ;
    private final Externalizer<?> externalizer;

    TimeExternalizerProvider(Externalizer<?> externalizer) {
        this.externalizer = externalizer;
    }

    @Override
    public Externalizer<?> getExternalizer() {
        return this.externalizer;
    }
}
