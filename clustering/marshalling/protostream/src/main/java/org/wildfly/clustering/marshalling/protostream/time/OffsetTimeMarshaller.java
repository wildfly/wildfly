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
