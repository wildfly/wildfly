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

package org.wildfly.clustering.marshalling.protostream.time;

import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

import org.infinispan.protostream.ImmutableSerializationContext;
import org.infinispan.protostream.RawProtoStreamReader;
import org.infinispan.protostream.RawProtoStreamWriter;
import org.wildfly.clustering.marshalling.protostream.ProtoStreamMarshaller;

import protostream.com.google.protobuf.WireFormat;

/**
 * Marshaller for {@link LocalDateTime} instances, using the following strategy:
 * <ol>
 * <li>Marshal local date</li>
 * <li>Marshal local time</li>
 * </ol>
 * @author Paul Ferraro
 */
public enum LocalDateTimeMarshaller implements ProtoStreamMarshaller<LocalDateTime> {
    INSTANCE;

    private static final LocalDate DEFAULT_DATE = LocalDate.ofEpochDay(0);
    private static final LocalTime DEFAULT_TIME = LocalTime.MIDNIGHT;

    private static final int DATE_INDEX = 1;
    private static final int TIME_INDEX = DATE_INDEX + LocalDateFieldMarshaller.INSTANCE.getFields();

    @Override
    public LocalDateTime readFrom(ImmutableSerializationContext context, RawProtoStreamReader reader) throws IOException {
        LocalDate date = DEFAULT_DATE;
        LocalTime time = DEFAULT_TIME;
        boolean reading = true;
        while (reading) {
            int tag = reader.readTag();
            int index = WireFormat.getTagFieldNumber(tag);
            if (index >= DATE_INDEX && index < TIME_INDEX) {
                date = LocalDateFieldMarshaller.INSTANCE.readField(context, reader, index - DATE_INDEX, date);
            } else if (index >= TIME_INDEX && index < TIME_INDEX + LocalTimeFieldMarshaller.INSTANCE.getFields()) {
                time = LocalTimeFieldMarshaller.INSTANCE.readField(context, reader, index - TIME_INDEX, time);
            } else {
                reading = (tag != 0) && reader.skipField(tag);
            }
        }
        return LocalDateTime.of(date, time);
    }

    @Override
    public void writeTo(ImmutableSerializationContext context, RawProtoStreamWriter writer, LocalDateTime dateTime) throws IOException {
        LocalDate date = dateTime.toLocalDate();
        if (!date.equals(DEFAULT_DATE)) {
            LocalDateFieldMarshaller.INSTANCE.writeFields(context, writer, DATE_INDEX, date);
        }
        LocalTime time = dateTime.toLocalTime();
        if (!time.equals(DEFAULT_TIME)) {
            LocalTimeFieldMarshaller.INSTANCE.writeFields(context, writer, TIME_INDEX, time);
        }
    }

    @Override
    public Class<? extends LocalDateTime> getJavaClass() {
        return LocalDateTime.class;
    }
}
