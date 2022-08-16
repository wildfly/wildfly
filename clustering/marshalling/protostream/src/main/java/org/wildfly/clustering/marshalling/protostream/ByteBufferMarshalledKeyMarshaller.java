/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2022, Red Hat, Inc., and individual contributors
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

package org.wildfly.clustering.marshalling.protostream;

import java.io.IOException;
import java.nio.ByteBuffer;

import org.infinispan.protostream.descriptors.WireType;
import org.wildfly.clustering.marshalling.spi.ByteBufferMarshalledKey;

/**
 * {@link ProtoStreamMarshaller} for a {@link ByteBufferMarshalledKey}.
 * @author Paul Ferraro
 */
public class ByteBufferMarshalledKeyMarshaller implements ProtoStreamMarshaller<ByteBufferMarshalledKey<Object>> {

    private static final int BUFFER_INDEX = 1;
    private static final int HASH_CODE_INDEX = 2;

    @Override
    public ByteBufferMarshalledKey<Object> readFrom(ProtoStreamReader reader) throws IOException {
        ByteBuffer buffer = null;
        int hashCode = 0;
        while (!reader.isAtEnd()) {
            int tag = reader.readTag();
            switch (WireType.getTagFieldNumber(tag)) {
                case BUFFER_INDEX:
                    buffer = reader.readByteBuffer();
                    break;
                case HASH_CODE_INDEX:
                    hashCode = reader.readSFixed32();
                    break;
                default:
                    reader.skipField(tag);
            }
        }
        return new ByteBufferMarshalledKey<>(buffer, hashCode);
    }

    @Override
    public void writeTo(ProtoStreamWriter writer, ByteBufferMarshalledKey<Object> key) throws IOException {
        ByteBuffer buffer = key.getBuffer();
        if (buffer != null) {
            writer.writeBytes(BUFFER_INDEX, buffer.mark());
            buffer.reset();
        }
        int hashCode = key.hashCode();
        if (hashCode != 0) {
            writer.writeSFixed32(HASH_CODE_INDEX, hashCode);
        }
    }

    @SuppressWarnings("unchecked")
    @Override
    public Class<? extends ByteBufferMarshalledKey<Object>> getJavaClass() {
        return (Class<ByteBufferMarshalledKey<Object>>) (Class<?>) ByteBufferMarshalledKey.class;
    }
}
