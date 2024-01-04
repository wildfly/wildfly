/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.marshalling.protostream.time;

import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;

import org.infinispan.protostream.descriptors.WireType;
import org.wildfly.clustering.marshalling.protostream.FieldSetReader;
import org.wildfly.clustering.marshalling.protostream.ProtoStreamMarshaller;
import org.wildfly.clustering.marshalling.protostream.ProtoStreamReader;
import org.wildfly.clustering.marshalling.protostream.ProtoStreamWriter;

/**
 * Marshaller for {@link ZonedDateTime} instances, using the following strategy:
 * <ol>
 * <li>Marshal duration since epoch</li>
 * <li>Marshal time zone</li>
 * </ol>
 * @author Paul Ferraro
 */
public class ZonedDateTimeMarshaller implements ProtoStreamMarshaller<ZonedDateTime> {

    private static final int DATE_INDEX = 1;
    private static final int TIME_INDEX = LocalDateMarshaller.INSTANCE.nextIndex(DATE_INDEX);
    private static final int OFFSET_INDEX = LocalTimeMarshaller.INSTANCE.nextIndex(TIME_INDEX);
    private static final int ZONE_INDEX = ZoneOffsetMarshaller.INSTANCE.nextIndex(OFFSET_INDEX);

    @Override
    public ZonedDateTime readFrom(ProtoStreamReader reader) throws IOException {
        FieldSetReader<LocalDate> dateReader = reader.createFieldSetReader(LocalDateMarshaller.INSTANCE, DATE_INDEX);
        FieldSetReader<LocalTime> timeReader = reader.createFieldSetReader(LocalTimeMarshaller.INSTANCE, TIME_INDEX);
        FieldSetReader<ZoneOffset> offsetReader = reader.createFieldSetReader(ZoneOffsetMarshaller.INSTANCE, OFFSET_INDEX);
        LocalDate date = LocalDateMarshaller.INSTANCE.createInitialValue();
        LocalTime time = LocalTimeMarshaller.INSTANCE.createInitialValue();
        ZoneId zone = ZoneOffsetMarshaller.INSTANCE.createInitialValue();
        while (!reader.isAtEnd()) {
            int tag = reader.readTag();
            int index = WireType.getTagFieldNumber(tag);
            if (dateReader.contains(index)) {
                date = dateReader.readField(date);
            } else if (timeReader.contains(index)) {
                time = timeReader.readField(time);
            } else if (offsetReader.contains(index)) {
                zone = offsetReader.readField((ZoneOffset) zone);
            } else if (index == ZONE_INDEX) {
                zone = ZoneId.of(reader.readString());
            } else {
                reader.skipField(tag);
            }
        }
        return ZonedDateTime.of(date, time, zone);
    }

    @Override
    public void writeTo(ProtoStreamWriter writer, ZonedDateTime value) throws IOException {
        writer.createFieldSetWriter(LocalDateMarshaller.INSTANCE, DATE_INDEX).writeFields(value.toLocalDate());
        writer.createFieldSetWriter(LocalTimeMarshaller.INSTANCE, TIME_INDEX).writeFields(value.toLocalTime());
        ZoneId zone = value.getZone();
        if (zone instanceof ZoneOffset) {
            writer.createFieldSetWriter(ZoneOffsetMarshaller.INSTANCE, OFFSET_INDEX).writeFields((ZoneOffset) zone);
        } else {
            writer.writeString(ZONE_INDEX, zone.getId());
        }
    }

    @Override
    public Class<? extends ZonedDateTime> getJavaClass() {
        return ZonedDateTime.class;
    }
}
