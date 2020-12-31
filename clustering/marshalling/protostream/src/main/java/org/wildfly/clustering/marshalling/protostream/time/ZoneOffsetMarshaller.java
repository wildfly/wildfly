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
import java.time.ZoneOffset;

import org.infinispan.protostream.ImmutableSerializationContext;
import org.infinispan.protostream.RawProtoStreamReader;
import org.infinispan.protostream.RawProtoStreamWriter;
import org.infinispan.protostream.impl.WireFormat;
import org.wildfly.clustering.marshalling.protostream.EmbeddableMarshaller;

/**
 * Marshalling for {@link ZoneOffset} instances using the following strategy:
 * <ol>
 * <li>Marshal {@link ZoneOffset#UTC} as zero bytes</li>
 * <li>If offset is of form &plusmn;HH, marshal as signed integer of hours</li>
 * <li>If offset is of form &plusmn;HH:MM, marshal as signed integer of total minutes<li>
 * <li>If offset is of form &plusmn;HH:MM:SS, marshal as signed integer of total seconds<li>
 * </ol>
 * @author Paul Ferraro
 */
public enum ZoneOffsetMarshaller implements EmbeddableMarshaller<ZoneOffset> {
    INSTANCE;

    private static final int HOURS_INDEX = 0;
    private static final int MINUTES_INDEX = 1;
    private static final int SECONDS_INDEX = 2;

    @Override
    public ZoneOffset readField(ImmutableSerializationContext context, RawProtoStreamReader reader, int index, ZoneOffset offset) throws IOException {
        switch (index) {
            case HOURS_INDEX:
                return ZoneOffset.ofHours(reader.readSInt32());
            case MINUTES_INDEX:
                return ZoneOffset.ofTotalSeconds(reader.readSInt32() * 60);
            case SECONDS_INDEX:
                return ZoneOffset.ofTotalSeconds(reader.readSInt32());
            default:
                return offset;
        }
    }

    @Override
    public void writeFields(ImmutableSerializationContext context, RawProtoStreamWriter writer, int startIndex, ZoneOffset offset) throws IOException {
        int seconds = offset.getTotalSeconds();
        if (seconds != 0) {
            if (seconds % 60 == 0) {
                int minutes = seconds / 60;
                if (minutes % 60 == 0) {
                    int hours = minutes / 60;
                    // Typical offsets
                    writer.writeSInt32(startIndex + HOURS_INDEX, hours);
                } else {
                    // Uncommon fractional hour offsets
                    writer.writeSInt32(startIndex + MINUTES_INDEX, minutes);
                }
            } else {
                // Synthetic offsets
                writer.writeSInt32(startIndex + SECONDS_INDEX, seconds);
            }
        }
    }

    @Override
    public ZoneOffset readFrom(ImmutableSerializationContext context, RawProtoStreamReader reader) throws IOException {
        ZoneOffset offset = ZoneOffset.UTC;
        boolean reading = true;
        while (reading) {
            int tag = reader.readTag();
            int index = WireFormat.getTagFieldNumber(tag) - 1;
            switch (index) {
                case HOURS_INDEX:
                case MINUTES_INDEX:
                case SECONDS_INDEX:
                    offset = this.readField(context, reader, index, offset);
                    break;
                default:
                    reading = (tag != 0) && reader.skipField(tag);
            }
        }
        return offset;
    }

    @Override
    public Class<? extends ZoneOffset> getJavaClass() {
        return ZoneOffset.class;
    }
}
