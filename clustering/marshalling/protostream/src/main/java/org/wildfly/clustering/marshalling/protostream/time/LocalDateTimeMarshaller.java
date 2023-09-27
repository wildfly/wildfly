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
    private static final int TIME_INDEX = DATE_INDEX + LocalDateMarshaller.INSTANCE.getFields();

    @Override
    public LocalDateTime readFrom(ProtoStreamReader reader) throws IOException {
        LocalDate date = LocalDateMarshaller.INSTANCE.getBuilder();
        LocalTime time = LocalTimeMarshaller.INSTANCE.getBuilder();
        while (!reader.isAtEnd()) {
            int tag = reader.readTag();
            int index = WireType.getTagFieldNumber(tag);
            if (index >= DATE_INDEX && index < TIME_INDEX) {
                date = LocalDateMarshaller.INSTANCE.readField(reader, index - DATE_INDEX, date);
            } else if (index >= TIME_INDEX && index < TIME_INDEX + LocalTimeMarshaller.INSTANCE.getFields()) {
                time = LocalTimeMarshaller.INSTANCE.readField(reader, index - TIME_INDEX, time);
            } else {
                reader.skipField(tag);
            }
        }
        return LocalDateTime.of(date, time);
    }

    @Override
    public void writeTo(ProtoStreamWriter writer, LocalDateTime value) throws IOException {
        LocalDateMarshaller.INSTANCE.writeFields(writer, DATE_INDEX, value.toLocalDate());
        LocalTimeMarshaller.INSTANCE.writeFields(writer, TIME_INDEX, value.toLocalTime());
    }

    @Override
    public Class<? extends LocalDateTime> getJavaClass() {
        return LocalDateTime.class;
    }
}
