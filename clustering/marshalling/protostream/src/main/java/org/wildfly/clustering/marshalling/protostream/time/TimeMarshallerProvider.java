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
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.Month;
import java.time.Year;
import java.time.ZoneId;
import java.time.ZoneOffset;

import org.wildfly.clustering.marshalling.protostream.AnyField;
import org.wildfly.clustering.marshalling.protostream.EnumMarshaller;
import org.wildfly.clustering.marshalling.protostream.ProtoStreamMarshaller;
import org.wildfly.clustering.marshalling.protostream.ProtoStreamMarshallerProvider;
import org.wildfly.clustering.marshalling.protostream.SimpleFieldSetMarshaller;
import org.wildfly.clustering.marshalling.protostream.SingleFieldMarshaller;
import org.wildfly.common.function.Functions;

/**
 * Provider for java.time marshallers.
 * @author Paul Ferraro
 */
public enum TimeMarshallerProvider implements ProtoStreamMarshallerProvider {

    DAY_OF_WEEK(new EnumMarshaller<>(DayOfWeek.class)),
    DURATION(DurationMarshaller.INSTANCE),
    INSTANT(InstantMarshaller.INSTANCE),
    LOCAL_DATE(new SimpleFieldSetMarshaller<>(LocalDateFieldMarshaller.INSTANCE, Functions.constantSupplier(LocalDate.ofEpochDay(0)))),
    LOCAL_DATE_TIME(LocalDateTimeMarshaller.INSTANCE),
    LOCAL_TIME(new SimpleFieldSetMarshaller<>(LocalTimeFieldMarshaller.INSTANCE, Functions.constantSupplier(LocalTime.MIDNIGHT))),
    MONTH(new EnumMarshaller<>(Month.class)),
    MONTH_DAY(MonthDayMarshaller.INSTANCE),
    OFFSET_DATE_TIME(OffsetDateTimeMarshaller.INSTANCE),
    OFFSET_TIME(OffsetTimeMarshaller.INSTANCE),
    PERIOD(PeriodMarshaller.INSTANCE),
    YEAR(new SimpleFieldSetMarshaller<>(YearFieldMarshaller.INSTANCE, Functions.constantSupplier(Year.of(YearFieldMarshaller.EPOCH)))),
    YEAR_MONTH(YearMonthMarshaller.INSTANCE),
    ZONE_ID(new SingleFieldMarshaller<>(ZoneId.class, AnyField.STRING.cast(String.class), Functions.constantSupplier(ZoneOffset.UTC), ZoneId::getId, ZoneId::of)),
    ZONE_OFFSET(new SimpleFieldSetMarshaller<>(ZoneOffsetFieldMarshaller.INSTANCE, Functions.constantSupplier(ZoneOffset.UTC))),
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
