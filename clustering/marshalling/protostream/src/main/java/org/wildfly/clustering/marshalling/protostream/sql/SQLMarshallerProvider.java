/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.marshalling.protostream.sql;

import java.io.IOException;
import java.sql.Date;
import java.sql.Time;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

import org.wildfly.clustering.marshalling.protostream.FunctionalMarshaller;
import org.wildfly.clustering.marshalling.protostream.ProtoStreamMarshaller;
import org.wildfly.clustering.marshalling.protostream.ProtoStreamMarshallerProvider;
import org.wildfly.common.function.ExceptionFunction;

/**
 * Marshallers for java.sql.* date/time classes.
 * @author Paul Ferraro
 */
public enum SQLMarshallerProvider implements ProtoStreamMarshallerProvider {

    DATE(Date.class, LocalDate.class, Date::toLocalDate, Date::valueOf),
    TIME(Time.class, LocalTime.class, Time::toLocalTime, Time::valueOf),
    TIMESTAMP(Timestamp.class, LocalDateTime.class, Timestamp::toLocalDateTime, Timestamp::valueOf),
    ;
    private final ProtoStreamMarshaller<?> marshaller;

    <T, V> SQLMarshallerProvider(Class<T> targetClass, Class<V> sourceClass, ExceptionFunction<T, V, IOException> function, ExceptionFunction<V, T, IOException> factory) {
        this.marshaller = new FunctionalMarshaller<>(targetClass, sourceClass, function, factory);
    }

    @Override
    public ProtoStreamMarshaller<?> getMarshaller() {
        return this.marshaller;
    }
}
