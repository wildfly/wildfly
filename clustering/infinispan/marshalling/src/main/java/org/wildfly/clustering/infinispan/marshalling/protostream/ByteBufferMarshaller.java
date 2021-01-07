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

package org.wildfly.clustering.infinispan.marshalling.protostream;

import java.io.IOException;
import java.io.StreamCorruptedException;

import org.infinispan.commons.io.ByteBufferImpl;
import org.infinispan.protostream.ImmutableSerializationContext;
import org.infinispan.protostream.RawProtoStreamReader;
import org.infinispan.protostream.RawProtoStreamWriter;
import org.wildfly.clustering.marshalling.protostream.AutoSizedProtoStreamMarshaller;

/**
 * Marshaller for an Infinispan ByteBuffer.
 * @author Paul Ferraro
 */
public enum ByteBufferMarshaller implements AutoSizedProtoStreamMarshaller<ByteBufferImpl> {
    INSTANCE;

    @Override
    public ByteBufferImpl readFrom(ImmutableSerializationContext context, RawProtoStreamReader reader) throws IOException {
        byte[] bytes = reader.readByteArray();
        if (reader.readTag() != 0) {
            throw new StreamCorruptedException();
        }
        return ByteBufferImpl.create(bytes);
    }

    @Override
    public void writeTo(ImmutableSerializationContext context, RawProtoStreamWriter writer, ByteBufferImpl buffer) throws IOException {
        writer.writeUInt32NoTag(buffer.getLength());
        writer.writeRawBytes(buffer.getBuf(), buffer.getOffset(), buffer.getLength());
    }

    @Override
    public Class<? extends ByteBufferImpl> getJavaClass() {
        return ByteBufferImpl.class;
    }
}
