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

package org.wildfly.clustering.marshalling.protostream.time;

import java.time.DayOfWeek;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Month;
import java.time.ZoneId;
import java.time.ZoneOffset;

import org.wildfly.clustering.marshalling.protostream.AnyField;
import org.wildfly.clustering.marshalling.protostream.EnumMarshaller;
import org.wildfly.clustering.marshalling.protostream.FunctionalMarshaller;
import org.wildfly.clustering.marshalling.protostream.PrimitiveMarshaller;
import org.wildfly.clustering.marshalling.protostream.ProtoStreamMarshallerProvider;
import org.wildfly.clustering.marshalling.protostream.ProtoStreamMarshaller;

/**
 * Provider for java.time marshallers.
 * @author Paul Ferraro
 */
public enum TimeMarshallerProvider implements ProtoStreamMarshallerProvider {

    DAY_OF_WEEK(new EnumMarshaller<>(DayOfWeek.class)),
    DURATION(DurationMarshaller.INSTANCE),
    INSTANT(new FunctionalMarshaller<>(Instant.class, DurationMarshaller.INSTANCE, instant -> Duration.ofSeconds(instant.getEpochSecond(), instant.getNano()), duration -> Instant.ofEpochSecond(duration.getSeconds(), duration.getNano()))),
    LOCAL_DATE(new FunctionalMarshaller<>(LocalDate.class, PrimitiveMarshaller.LONG.cast(Long.class), LocalDate::toEpochDay, LocalDate::ofEpochDay)),
    LOCAL_DATE_TIME(new FunctionalMarshaller<>(LocalDateTime.class, DurationMarshaller.INSTANCE, dateTime -> Duration.ofSeconds(dateTime.toEpochSecond(ZoneOffset.UTC), dateTime.getNano()), duration -> LocalDateTime.ofEpochSecond(duration.getSeconds(), duration.getNano(), ZoneOffset.UTC))),
    LOCAL_TIME(LocalTimeMarshaller.INSTANCE),
    MONTH(new EnumMarshaller<>(Month.class)),
    MONTH_DAY(MonthDayMarshaller.INSTANCE),
    OFFSET_DATE_TIME(OffsetDateTimeMarshaller.INSTANCE),
    OFFSET_TIME(OffsetTimeMarshaller.INSTANCE),
    PERIOD(PeriodMarshaller.INSTANCE),
    YEAR(YearMarshaller.INSTANCE),
    YEAR_MONTH(YearMonthMarshaller.INSTANCE),
    ZONE_ID(new FunctionalMarshaller<>(ZoneId.class, AnyField.STRING.cast(String.class), ZoneId::getId, ZoneId::of)),
    ZONE_OFFSET(ZoneOffsetMarshaller.INSTANCE),
    ZONED_DATE_TIME(ZonedDateTimeMarshaller.INSTANCE),
    ;
    private final ProtoStreamMarshaller<?> marshaller;

    TimeMarshallerProvider(ProtoStreamMarshaller<?> marshaller) {
        this.marshaller = marshaller;
    }

    @Override
    public ProtoStreamMarshaller<?> getMarshaller() {
        return this.marshaller;
    }
}
