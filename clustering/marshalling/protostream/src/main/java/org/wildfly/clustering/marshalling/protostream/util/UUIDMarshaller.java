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
import java.io.StreamCorruptedException;
import java.util.OptionalInt;
import java.util.UUID;

import org.infinispan.protostream.ImmutableSerializationContext;
import org.infinispan.protostream.RawProtoStreamReader;
import org.infinispan.protostream.RawProtoStreamWriter;
import org.infinispan.protostream.impl.RawProtoStreamWriterImpl;
import org.wildfly.clustering.marshalling.protostream.ProtoStreamMarshaller;

/**
 * Marshaller for a {@link UUID} using fixed size longs.
 * @author Paul Ferraro
 */
public enum UUIDMarshaller implements ProtoStreamMarshaller<UUID> {
    INSTANCE;

    @Override
    public Class<? extends UUID> getJavaClass() {
        return UUID.class;
    }

    @Override
    public UUID readFrom(ImmutableSerializationContext context, RawProtoStreamReader reader) throws IOException {
        long mostSignificantBits = reader.readFixed64();
        long leastSignificantBits = reader.readFixed64();
        if (reader.readTag() != 0) {
            throw new StreamCorruptedException();
        }
        return new UUID(mostSignificantBits, leastSignificantBits);
    }

    @Override
    public void writeTo(ImmutableSerializationContext context, RawProtoStreamWriter writer, UUID uuid) throws IOException {
        ((RawProtoStreamWriterImpl) writer).getDelegate().writeFixed64NoTag(uuid.getMostSignificantBits());
        ((RawProtoStreamWriterImpl) writer).getDelegate().writeFixed64NoTag(uuid.getLeastSignificantBits());
    }

    @Override
    public OptionalInt size(ImmutableSerializationContext context, UUID value) {
        return OptionalInt.of(Long.BYTES + Long.BYTES);
    }
}
