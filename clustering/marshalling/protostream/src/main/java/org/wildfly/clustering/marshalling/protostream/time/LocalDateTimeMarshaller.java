/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.marshalling.protostream.time;

import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

import org.infinispan.protostream.descriptors.WireType;
import org.wildfly.clustering.marshalling.protostream.FieldSetReader;
import org.wildfly.clustering.marshalling.protostream.ProtoStreamMarshaller;
import org.wildfly.clustering.marshalling.protostream.ProtoStreamReader;
import org.wildfly.clustering.marshalling.protostream.ProtoStreamWriter;

/**
 * Marshaller for {@link LocalDateTime} instances, using the following strategy:
 * <ol>
 * <li>Marshal local date</li>
 * <li>Marshal local time</li>
 * </ol>
 * @author Paul Ferraro
 */
public class LocalDateTimeMarshaller implements ProtoStreamMarshaller<LocalDateTime> {

    private static final int DATE_INDEX = 1;
    private static final int TIME_INDEX = LocalDateMarshaller.INSTANCE.nextIndex(DATE_INDEX);

    @Override
    public LocalDateTime readFrom(ProtoStreamReader reader) throws IOException {
        FieldSetReader<LocalDate> dateReader = reader.createFieldSetReader(LocalDateMarshaller.INSTANCE, DATE_INDEX);
        FieldSetReader<LocalTime> timeReader = reader.createFieldSetReader(LocalTimeMarshaller.INSTANCE, TIME_INDEX);
        LocalDate date = LocalDateMarshaller.INSTANCE.createInitialValue();
        LocalTime time = LocalTimeMarshaller.INSTANCE.createInitialValue();
        while (!reader.isAtEnd()) {
            int tag = reader.readTag();
            int index = WireType.getTagFieldNumber(tag);
            if (dateReader.contains(index)) {
                date = dateReader.readField(date);
            } else if (timeReader.contains(index)) {
                time = timeReader.readField(time);
            } else {
                reader.skipField(tag);
            }
        }
        return LocalDateTime.of(date, time);
    }

    @Override
    public void writeTo(ProtoStreamWriter writer, LocalDateTime value) throws IOException {
        writer.createFieldSetWriter(LocalDateMarshaller.INSTANCE, DATE_INDEX).writeFields(value.toLocalDate());
        writer.createFieldSetWriter(LocalTimeMarshaller.INSTANCE, TIME_INDEX).writeFields(value.toLocalTime());
    }

    @Override
    public Class<? extends LocalDateTime> getJavaClass() {
        return LocalDateTime.class;
    }
}
