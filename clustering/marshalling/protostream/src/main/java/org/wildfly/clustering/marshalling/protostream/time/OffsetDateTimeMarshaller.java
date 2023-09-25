/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.marshalling.protostream.time;

import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;

import org.infinispan.protostream.descriptors.WireType;
import org.wildfly.clustering.marshalling.protostream.FieldSetReader;
import org.wildfly.clustering.marshalling.protostream.ProtoStreamMarshaller;
import org.wildfly.clustering.marshalling.protostream.ProtoStreamReader;
import org.wildfly.clustering.marshalling.protostream.ProtoStreamWriter;

/**
 * Marshaller for {@link OffsetDateTime} instances, using the following strategy:
 * <ol>
 * <li>Marshal local date</li>
 * <li>Marshal local time</li>
 * <li>Marshal zone offset</li>
 * </ol>
 * @author Paul Ferraro
 */
public class OffsetDateTimeMarshaller implements ProtoStreamMarshaller<OffsetDateTime> {

    private static final int DATE_INDEX = 1;
    private static final int TIME_INDEX = LocalDateMarshaller.INSTANCE.nextIndex(DATE_INDEX);
    private static final int OFFSET_INDEX = LocalTimeMarshaller.INSTANCE.nextIndex(TIME_INDEX);

    @Override
    public OffsetDateTime readFrom(ProtoStreamReader reader) throws IOException {
        FieldSetReader<LocalDate> dateReader = reader.createFieldSetReader(LocalDateMarshaller.INSTANCE, DATE_INDEX);
        FieldSetReader<LocalTime> timeReader = reader.createFieldSetReader(LocalTimeMarshaller.INSTANCE, TIME_INDEX);
        FieldSetReader<ZoneOffset> offsetReader = reader.createFieldSetReader(ZoneOffsetMarshaller.INSTANCE, OFFSET_INDEX);
        LocalDate date = LocalDateMarshaller.INSTANCE.createInitialValue();
        LocalTime time = LocalTimeMarshaller.INSTANCE.createInitialValue();
        ZoneOffset offset = ZoneOffsetMarshaller.INSTANCE.createInitialValue();
        while (!reader.isAtEnd()) {
            int tag = reader.readTag();
            int index = WireType.getTagFieldNumber(tag);
            if (dateReader.contains(index)) {
                date = dateReader.readField(date);
            } else if (timeReader.contains(index)) {
                time = timeReader.readField(time);
            } else if (offsetReader.contains(index)) {
                offset = offsetReader.readField(offset);
            } else {
                reader.skipField(tag);
            }
        }
        return OffsetDateTime.of(date, time, offset);
    }

    @Override
    public void writeTo(ProtoStreamWriter writer, OffsetDateTime value) throws IOException {
        writer.createFieldSetWriter(LocalDateMarshaller.INSTANCE, DATE_INDEX).writeFields(value.toLocalDate());
        writer.createFieldSetWriter(LocalTimeMarshaller.INSTANCE, TIME_INDEX).writeFields(value.toLocalTime());
        writer.createFieldSetWriter(ZoneOffsetMarshaller.INSTANCE, OFFSET_INDEX).writeFields(value.getOffset());
    }

    @Override
    public Class<? extends OffsetDateTime> getJavaClass() {
        return OffsetDateTime.class;
    }
}
