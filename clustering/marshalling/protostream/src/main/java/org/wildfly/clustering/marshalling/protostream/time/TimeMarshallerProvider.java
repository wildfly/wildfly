/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.marshalling.protostream.time;

import java.time.DayOfWeek;
import java.time.Month;
import java.time.ZoneId;
import java.time.ZoneOffset;

import org.wildfly.clustering.marshalling.protostream.EnumMarshaller;
import org.wildfly.clustering.marshalling.protostream.FieldSetProtoStreamMarshaller;
import org.wildfly.clustering.marshalling.protostream.FunctionalScalarMarshaller;
import org.wildfly.clustering.marshalling.protostream.ProtoStreamMarshaller;
import org.wildfly.clustering.marshalling.protostream.ProtoStreamMarshallerProvider;
import org.wildfly.clustering.marshalling.protostream.Scalar;
import org.wildfly.common.function.Functions;

/**
 * Provider for java.time marshallers.
 * @author Paul Ferraro
 */
public enum TimeMarshallerProvider implements ProtoStreamMarshallerProvider {

    DAY_OF_WEEK(new EnumMarshaller<>(DayOfWeek.class)),
    DURATION(new FieldSetProtoStreamMarshaller<>(DurationMarshaller.INSTANCE)),
    INSTANT(new InstantMarshaller()),
    LOCAL_DATE(new FieldSetProtoStreamMarshaller<>(LocalDateMarshaller.INSTANCE)),
    LOCAL_DATE_TIME(new LocalDateTimeMarshaller()),
    LOCAL_TIME(new FieldSetProtoStreamMarshaller<>(LocalTimeMarshaller.INSTANCE)),
    MONTH(new EnumMarshaller<>(Month.class)),
    MONTH_DAY(new MonthDayMarshaller()),
    OFFSET_DATE_TIME(new OffsetDateTimeMarshaller()),
    OFFSET_TIME(new OffsetTimeMarshaller()),
    PERIOD(new PeriodMarshaller()),
    YEAR(new FieldSetProtoStreamMarshaller<>(YearMarshaller.INSTANCE)),
    YEAR_MONTH(new YearMonthMarshaller()),
    ZONE_ID(new FunctionalScalarMarshaller<>(ZoneId.class, Scalar.STRING.cast(String.class), Functions.constantSupplier(ZoneOffset.UTC), ZoneId::getId, ZoneId::of)),
    ZONE_OFFSET(new FieldSetProtoStreamMarshaller<>(ZoneOffsetMarshaller.INSTANCE)),
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
