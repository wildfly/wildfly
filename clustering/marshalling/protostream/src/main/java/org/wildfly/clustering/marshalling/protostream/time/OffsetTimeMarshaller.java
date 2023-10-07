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
import org.wildfly.clustering.marshalling.protostream.FieldSetReader;
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
    private static final int OFFSET_INDEX = LocalTimeMarshaller.INSTANCE.nextIndex(TIME_INDEX);

    @Override
    public OffsetTime readFrom(ProtoStreamReader reader) throws IOException {
        FieldSetReader<LocalTime> timeReader = reader.createFieldSetReader(LocalTimeMarshaller.INSTANCE, TIME_INDEX);
        FieldSetReader<ZoneOffset> offsetReader = reader.createFieldSetReader(ZoneOffsetMarshaller.INSTANCE, OFFSET_INDEX);
        LocalTime time = LocalTimeMarshaller.INSTANCE.createInitialValue();
        ZoneOffset offset = ZoneOffsetMarshaller.INSTANCE.createInitialValue();
        while (!reader.isAtEnd()) {
            int tag = reader.readTag();
            int index = WireType.getTagFieldNumber(tag);
            if (timeReader.contains(index)) {
                time = timeReader.readField(time);
            } else if (offsetReader.contains(index)) {
                offset = offsetReader.readField(offset);
            } else {
                reader.skipField(tag);
            }
        }
        return OffsetTime.of(time, offset);
    }

    @Override
    public void writeTo(ProtoStreamWriter writer, OffsetTime value) throws IOException {
        writer.createFieldSetWriter(LocalTimeMarshaller.INSTANCE, TIME_INDEX).writeFields(value.toLocalTime());
        writer.createFieldSetWriter(ZoneOffsetMarshaller.INSTANCE, OFFSET_INDEX).writeFields(value.getOffset());
    }

    @Override
    public Class<? extends OffsetTime> getJavaClass() {
        return OffsetTime.class;
    }
}
