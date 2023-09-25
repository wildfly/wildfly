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
    private static final int TIME_INDEX = DATE_INDEX + LocalDateMarshaller.INSTANCE.getFields();
    private static final int OFFSET_INDEX = TIME_INDEX + LocalTimeMarshaller.INSTANCE.getFields();
    private static final int ZONE_INDEX = OFFSET_INDEX + ZoneOffsetMarshaller.INSTANCE.getFields();

    @Override
    public ZonedDateTime readFrom(ProtoStreamReader reader) throws IOException {
        LocalDate date = LocalDateMarshaller.INSTANCE.getBuilder();
        LocalTime time = LocalTimeMarshaller.INSTANCE.getBuilder();
        ZoneId zone = ZoneOffsetMarshaller.INSTANCE.getBuilder();
        while (!reader.isAtEnd()) {
            int tag = reader.readTag();
            int index = WireType.getTagFieldNumber(tag);
            if (index >= DATE_INDEX && index < TIME_INDEX) {
                date = LocalDateMarshaller.INSTANCE.readField(reader, index - DATE_INDEX, date);
            } else if (index >= TIME_INDEX && index < OFFSET_INDEX) {
                time = LocalTimeMarshaller.INSTANCE.readField(reader, index - TIME_INDEX, time);
            } else if (index >= OFFSET_INDEX && index < ZONE_INDEX) {
                zone = ZoneOffsetMarshaller.INSTANCE.readField(reader, index - OFFSET_INDEX, (ZoneOffset) zone);
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
        LocalDateMarshaller.INSTANCE.writeFields(writer, DATE_INDEX, value.toLocalDate());
        LocalTimeMarshaller.INSTANCE.writeFields(writer, TIME_INDEX, value.toLocalTime());
        ZoneId zone = value.getZone();
        if (zone instanceof ZoneOffset) {
            ZoneOffsetMarshaller.INSTANCE.writeFields(writer, OFFSET_INDEX, (ZoneOffset) zone);
        } else {
            writer.writeString(ZONE_INDEX, zone.getId());
        }
    }

    @Override
    public Class<? extends ZonedDateTime> getJavaClass() {
        return ZonedDateTime.class;
    }
}
