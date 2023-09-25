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
    private static final int TIME_INDEX = DATE_INDEX + LocalDateMarshaller.INSTANCE.getFields();
    private static final int OFFSET_INDEX = TIME_INDEX + LocalTimeMarshaller.INSTANCE.getFields();

    @Override
    public OffsetDateTime readFrom(ProtoStreamReader reader) throws IOException {
        LocalDate date = LocalDateMarshaller.INSTANCE.getBuilder();
        LocalTime time = LocalTimeMarshaller.INSTANCE.getBuilder();
        ZoneOffset offset = ZoneOffsetMarshaller.INSTANCE.getBuilder();
        while (!reader.isAtEnd()) {
            int tag = reader.readTag();
            int index = WireType.getTagFieldNumber(tag);
            if (index >= DATE_INDEX && index < TIME_INDEX) {
                date = LocalDateMarshaller.INSTANCE.readField(reader, index - DATE_INDEX, date);
            } else if (index >= TIME_INDEX && index < OFFSET_INDEX) {
                time = LocalTimeMarshaller.INSTANCE.readField(reader, index - TIME_INDEX, time);
            } else if (index >= OFFSET_INDEX && index < OFFSET_INDEX + ZoneOffsetMarshaller.INSTANCE.getFields()) {
                offset = ZoneOffsetMarshaller.INSTANCE.readField(reader, index - OFFSET_INDEX, offset);
            } else {
                reader.skipField(tag);
            }
        }
        return OffsetDateTime.of(date, time, offset);
    }

    @Override
    public void writeTo(ProtoStreamWriter writer, OffsetDateTime value) throws IOException {
        LocalDateMarshaller.INSTANCE.writeFields(writer, DATE_INDEX, value.toLocalDate());
        LocalTimeMarshaller.INSTANCE.writeFields(writer, TIME_INDEX, value.toLocalTime());
        ZoneOffsetMarshaller.INSTANCE.writeFields(writer, OFFSET_INDEX, value.getOffset());
    }

    @Override
    public Class<? extends OffsetDateTime> getJavaClass() {
        return OffsetDateTime.class;
    }
}
