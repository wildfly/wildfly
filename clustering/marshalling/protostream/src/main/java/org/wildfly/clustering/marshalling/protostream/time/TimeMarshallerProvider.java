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
import java.time.Month;
import java.time.ZoneId;
import java.time.ZoneOffset;

import org.wildfly.clustering.marshalling.protostream.EnumMarshaller;
import org.wildfly.clustering.marshalling.protostream.FunctionalScalarMarshaller;
import org.wildfly.clustering.marshalling.protostream.ProtoStreamMarshaller;
import org.wildfly.clustering.marshalling.protostream.ProtoStreamMarshallerProvider;
import org.wildfly.clustering.marshalling.protostream.Scalar;
import org.wildfly.clustering.marshalling.protostream.SimpleFieldSetMarshaller;
import org.wildfly.common.function.Functions;

/**
 * Provider for java.time marshallers.
 * @author Paul Ferraro
 */
public enum TimeMarshallerProvider implements ProtoStreamMarshallerProvider {

    DAY_OF_WEEK(new EnumMarshaller<>(DayOfWeek.class)),
    DURATION(new SimpleFieldSetMarshaller<>(DurationMarshaller.INSTANCE)),
    INSTANT(new InstantMarshaller()),
    LOCAL_DATE(new SimpleFieldSetMarshaller<>(LocalDateMarshaller.INSTANCE)),
    LOCAL_DATE_TIME(new LocalDateTimeMarshaller()),
    LOCAL_TIME(new SimpleFieldSetMarshaller<>(LocalTimeMarshaller.INSTANCE)),
    MONTH(new EnumMarshaller<>(Month.class)),
    MONTH_DAY(new MonthDayMarshaller()),
    OFFSET_DATE_TIME(new OffsetDateTimeMarshaller()),
    OFFSET_TIME(new OffsetTimeMarshaller()),
    PERIOD(new PeriodMarshaller()),
    YEAR(new SimpleFieldSetMarshaller<>(YearMarshaller.INSTANCE)),
    YEAR_MONTH(new YearMonthMarshaller()),
    ZONE_ID(new FunctionalScalarMarshaller<>(ZoneId.class, Scalar.STRING.cast(String.class), Functions.constantSupplier(ZoneOffset.UTC), ZoneId::getId, ZoneId::of)),
    ZONE_OFFSET(new SimpleFieldSetMarshaller<>(ZoneOffsetMarshaller.INSTANCE)),
    ZONED_DATE_TIME(new ZonedDateTimeMarshaller()),
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
