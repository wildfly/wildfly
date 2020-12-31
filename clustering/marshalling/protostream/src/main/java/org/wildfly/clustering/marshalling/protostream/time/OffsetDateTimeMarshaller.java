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

import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;

import org.infinispan.protostream.ImmutableSerializationContext;
import org.infinispan.protostream.RawProtoStreamReader;
import org.infinispan.protostream.RawProtoStreamWriter;
import org.infinispan.protostream.impl.WireFormat;
import org.wildfly.clustering.marshalling.protostream.ProtoStreamMarshaller;

/**
 * Marshaller for {@link OffsetDateTime} instances, using the following strategy:
 * <ol>
 * <li>Marshal local date</li>
 * <li>Marshal local time</li>
 * <li>Marshal zone offset</li>
 * </ol>
 * @author Paul Ferraro
 */
public enum OffsetDateTimeMarshaller implements ProtoStreamMarshaller<OffsetDateTime> {
    INSTANCE;

    private static final LocalDate DEFAULT_DATE = LocalDate.ofEpochDay(0);
    private static final LocalTime DEFAULT_TIME = LocalTime.MIDNIGHT;
    private static final ZoneOffset DEFAULT_OFFSET = ZoneOffset.UTC;

    private static final int DATE_INDEX = 1;
    private static final int TIME_INDEX = DATE_INDEX + LocalDateFieldMarshaller.INSTANCE.getFields();
    private static final int OFFSET_INDEX = TIME_INDEX + LocalTimeFieldMarshaller.INSTANCE.getFields();

    @Override
    public OffsetDateTime readFrom(ImmutableSerializationContext context, RawProtoStreamReader reader) throws IOException {
        LocalDate date = DEFAULT_DATE;
        LocalTime time = DEFAULT_TIME;
        ZoneOffset offset = DEFAULT_OFFSET;
        boolean reading = true;
        while (reading) {
            int tag = reader.readTag();
            int index = WireFormat.getTagFieldNumber(tag);
            if (index >= DATE_INDEX && index < TIME_INDEX) {
                date = LocalDateFieldMarshaller.INSTANCE.readField(context, reader, index - DATE_INDEX, date);
            } else if (index >= TIME_INDEX && index < OFFSET_INDEX) {
                time = LocalTimeFieldMarshaller.INSTANCE.readField(context, reader, index - TIME_INDEX, time);
            } else if (index >= OFFSET_INDEX && index < OFFSET_INDEX + ZoneOffsetFieldMarshaller.INSTANCE.getFields()) {
                offset = ZoneOffsetFieldMarshaller.INSTANCE.readField(context, reader, index - OFFSET_INDEX, offset);
            } else {
                reading = (tag != 0) && reader.skipField(tag);
            }
        }
        return OffsetDateTime.of(date, time, offset);
    }

    @Override
    public void writeTo(ImmutableSerializationContext context, RawProtoStreamWriter writer, OffsetDateTime value) throws IOException {
        LocalDate date = value.toLocalDate();
        if (!date.equals(DEFAULT_DATE)) {
            LocalDateFieldMarshaller.INSTANCE.writeFields(context, writer, DATE_INDEX, date);
        }
        LocalTime time = value.toLocalTime();
        if (!time.equals(DEFAULT_TIME)) {
            LocalTimeFieldMarshaller.INSTANCE.writeFields(context, writer, TIME_INDEX, time);
        }
        ZoneOffset offset = value.getOffset();
        if (!offset.equals(DEFAULT_OFFSET)) {
            ZoneOffsetFieldMarshaller.INSTANCE.writeFields(context, writer, OFFSET_INDEX, offset);
        }
    }

    @Override
    public Class<? extends OffsetDateTime> getJavaClass() {
        return OffsetDateTime.class;
    }
}
