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

package org.wildfly.clustering.marshalling.protostream.util;

import java.io.IOException;
import java.util.UUID;

import org.infinispan.protostream.ImmutableSerializationContext;
import org.infinispan.protostream.RawProtoStreamReader;
import org.infinispan.protostream.RawProtoStreamWriter;
import org.infinispan.protostream.impl.WireFormat;
import org.wildfly.clustering.marshalling.protostream.ProtoStreamMarshaller;

/**
 * Marshaller for a {@link UUID} using fixed size longs.
 * @author Paul Ferraro
 */
public enum UUIDMarshaller implements ProtoStreamMarshaller<UUID> {
    INSTANCE;

    private static final int MOST_SIGNIFICANT_BITS_INDEX = 1;
    private static final int LEAST_SIGNIFICANT_BITS_INDEX = 2;

    @Override
    public Class<? extends UUID> getJavaClass() {
        return UUID.class;
    }

    @Override
    public UUID readFrom(ImmutableSerializationContext context, RawProtoStreamReader reader) throws IOException {
        long mostSignificantBits = 0;
        long leastSignificantBits = 0;
        boolean reading = true;
        while (reading) {
            int tag = reader.readTag();
            int index = WireFormat.getTagFieldNumber(tag);
            switch (index) {
                case MOST_SIGNIFICANT_BITS_INDEX:
                    mostSignificantBits = reader.readSFixed64();
                    break;
                case LEAST_SIGNIFICANT_BITS_INDEX:
                    leastSignificantBits = reader.readSFixed64();
                    break;
                default:
                    reading = (tag != 0) && reader.skipField(tag);
            }
        }
        return new UUID(mostSignificantBits, leastSignificantBits);
    }

    @Override
    public void writeTo(ImmutableSerializationContext context, RawProtoStreamWriter writer, UUID uuid) throws IOException {
        long mostSignificantBits = uuid.getMostSignificantBits();
        if (mostSignificantBits != 0) {
            writer.writeSFixed64(MOST_SIGNIFICANT_BITS_INDEX, mostSignificantBits);
        }
        long leastSignificantBits = uuid.getLeastSignificantBits();
        if (leastSignificantBits != 0) {
            writer.writeSFixed64(LEAST_SIGNIFICANT_BITS_INDEX, leastSignificantBits);
        }
    }
}
