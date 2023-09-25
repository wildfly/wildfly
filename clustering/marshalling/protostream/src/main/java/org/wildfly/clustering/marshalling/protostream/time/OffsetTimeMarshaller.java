/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.marshalling.protostream.time;

import java.io.IOException;
import java.time.LocalTime;
import java.time.OffsetTime;
import java.time.ZoneOffset;

import org.infinispan.protostream.descriptors.WireType;
import org.wildfly.clustering.marshalling.protostream.ProtoStreamMarshaller;
import org.wildfly.clustering.marshalling.protostream.ProtoStreamReader;
import org.wildfly.clustering.marshalling.protostream.ProtoStreamWriter;

/**
 * Marshaller for {@link OffsetTime} instances, using the following strategy:
 * <ol>
 * <li>Marshal local time</li>
 * <li>Marshal zone offset</li>
 * </ol>
 * @author Paul Ferraro
 */
public class OffsetTimeMarshaller implements ProtoStreamMarshaller<OffsetTime> {

    private static final int TIME_INDEX = 1;
    private static final int OFFSET_INDEX = TIME_INDEX + LocalTimeMarshaller.INSTANCE.getFields();

    @Override
    public OffsetTime readFrom(ProtoStreamReader reader) throws IOException {
        LocalTime time = LocalTimeMarshaller.INSTANCE.getBuilder();
        ZoneOffset offset = ZoneOffsetMarshaller.INSTANCE.getBuilder();
        while (!reader.isAtEnd()) {
            int tag = reader.readTag();
            int index = WireType.getTagFieldNumber(tag);
            if (index >= TIME_INDEX && index < OFFSET_INDEX) {
                time = LocalTimeMarshaller.INSTANCE.readField(reader, index - TIME_INDEX, time);
            } else if (index >= OFFSET_INDEX && index < OFFSET_INDEX + ZoneOffsetMarshaller.INSTANCE.getFields()) {
                offset = ZoneOffsetMarshaller.INSTANCE.readField(reader, index - OFFSET_INDEX, offset);
            } else {
                reader.skipField(tag);
            }
        }
        return OffsetTime.of(time, offset);
    }

    @Override
    public void writeTo(ProtoStreamWriter writer, OffsetTime value) throws IOException {
        LocalTimeMarshaller.INSTANCE.writeFields(writer, TIME_INDEX, value.toLocalTime());
        ZoneOffsetMarshaller.INSTANCE.writeFields(writer, OFFSET_INDEX, value.getOffset());
    }

    @Override
    public Class<? extends OffsetTime> getJavaClass() {
        return OffsetTime.class;
    }
}
