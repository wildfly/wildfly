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

import org.infinispan.protostream.ImmutableSerializationContext;
import org.infinispan.protostream.RawProtoStreamReader;
import org.infinispan.protostream.RawProtoStreamWriter;
import org.infinispan.protostream.impl.WireFormat;
import org.wildfly.clustering.marshalling.protostream.AutoSizedProtoStreamMarshaller;

/**
 * Marshaller for {@link OffsetTime} instances, using the following strategy:
 * <ol>
 * <li>Marshal local time</li>
 * <li>Marshal zone offset</li>
 * </ol>
 * @author Paul Ferraro
 */
public enum OffsetTimeMarshaller implements AutoSizedProtoStreamMarshaller<OffsetTime> {
    INSTANCE;

    private static final LocalTime DEFAULT_TIME = LocalTime.MIDNIGHT;
    private static final ZoneOffset DEFAULT_OFFSET = ZoneOffset.UTC;

    private static final int TIME_INDEX = 1;
    private static final int OFFSET_INDEX = 6;

    @Override
    public OffsetTime readFrom(ImmutableSerializationContext context, RawProtoStreamReader reader) throws IOException {
        LocalTime time = DEFAULT_TIME;
        ZoneOffset offset = DEFAULT_OFFSET;
        boolean reading = true;
        while (reading) {
            int tag = reader.readTag();
            int index = WireFormat.getTagFieldNumber(tag);
            switch (index) {
                case 1:
                case 2:
                case 3:
                case 4:
                case 5:
                    time = LocalTimeMarshaller.INSTANCE.readField(context, reader, index - TIME_INDEX, time);
                    break;
                case 6:
                case 7:
                case 8:
                    offset = ZoneOffsetMarshaller.INSTANCE.readField(context, reader, index - OFFSET_INDEX, offset);
                    break;
                default:
                    reading = (tag != 0) && reader.skipField(tag);
            }
        }
        return OffsetTime.of(time, offset);
    }

    @Override
    public void writeTo(ImmutableSerializationContext context, RawProtoStreamWriter writer, OffsetTime value) throws IOException {
        LocalTime time = value.toLocalTime();
        if (!time.equals(DEFAULT_TIME)) {
            LocalTimeMarshaller.INSTANCE.writeFields(context, writer, TIME_INDEX, value.toLocalTime());
        }
        ZoneOffset offset = value.getOffset();
        if (!offset.equals(DEFAULT_OFFSET)) {
            ZoneOffsetMarshaller.INSTANCE.writeFields(context, writer, OFFSET_INDEX, value.getOffset());
        }
    }

    @Override
    public Class<? extends OffsetTime> getJavaClass() {
        return OffsetTime.class;
    }
}
