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

package org.wildfly.clustering.marshalling.protostream.sql;

import java.sql.Date;
import java.sql.Time;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.function.Function;

import org.wildfly.clustering.marshalling.protostream.FunctionalMarshaller;
import org.wildfly.clustering.marshalling.protostream.ProtoStreamMarshaller;
import org.wildfly.clustering.marshalling.protostream.ProtoStreamMarshallerProvider;
import org.wildfly.clustering.marshalling.protostream.time.TimeMarshallerProvider;

/**
 * Marshallers for java.sql.* date/time classes.
 * @author Paul Ferraro
 */
public enum SQLMarshallerProvider implements ProtoStreamMarshallerProvider {

    DATE(Date.class, TimeMarshallerProvider.LOCAL_DATE.cast(LocalDate.class), Date::toLocalDate, Date::valueOf),
    TIME(Time.class, TimeMarshallerProvider.LOCAL_TIME.cast(LocalTime.class), Time::toLocalTime, Time::valueOf),
    TIMESTAMP(Timestamp.class, TimeMarshallerProvider.LOCAL_DATE_TIME.cast(LocalDateTime.class), Timestamp::toLocalDateTime, Timestamp::valueOf),
    ;
    private final ProtoStreamMarshaller<?> marshaller;

    <T, V> SQLMarshallerProvider(Class<T> targetClass, ProtoStreamMarshaller<V> marshaller, Function<T, V> function, Function<V, T> factory) {
        this.marshaller = new FunctionalMarshaller<>(targetClass, marshaller, function, factory);
    }

    @Override
    public ProtoStreamMarshaller<?> getMarshaller() {
        return this.marshaller;
    }
}
