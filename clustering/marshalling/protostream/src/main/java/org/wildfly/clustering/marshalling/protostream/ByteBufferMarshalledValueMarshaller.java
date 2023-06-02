/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2023, Red Hat, Inc., and individual contributors
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
import java.util.OptionalInt;

import org.infinispan.protostream.descriptors.WireType;
import org.wildfly.clustering.marshalling.spi.ByteBufferMarshalledValue;

/**
 * {@link ProtoStreamMarshaller} for a {@link ByteBufferMarshalledValue}.
 * @author Paul Ferraro
 */
public class ByteBufferMarshalledValueMarshaller implements ProtoStreamMarshaller<ByteBufferMarshalledValue<Object>> {

    private static final int BUFFER_INDEX = 1;

    @Override
    public ByteBufferMarshalledValue<Object> readFrom(ProtoStreamReader reader) throws IOException {
        ByteBuffer buffer = null;
        while (!reader.isAtEnd()) {
            int tag = reader.readTag();
            switch (WireType.getTagFieldNumber(tag)) {
                case BUFFER_INDEX:
                    buffer = reader.readByteBuffer();
                    break;
                default:
                    reader.skipField(tag);
            }
        }
        return new ByteBufferMarshalledValue<>(buffer);
    }

    @Override
    public void writeTo(ProtoStreamWriter writer, ByteBufferMarshalledValue<Object> key) throws IOException {
        ByteBuffer buffer = key.getBuffer();
        if (buffer != null) {
            writer.writeBytes(BUFFER_INDEX, buffer);
        }
    }

    @Override
    public OptionalInt size(ProtoStreamSizeOperation operation, ByteBufferMarshalledValue<Object> value) {
        if (value.isEmpty()) return OptionalInt.of(0);
        OptionalInt size = value.size();
        return size.isPresent() ? OptionalInt.of(operation.tagSize(BUFFER_INDEX, WireType.LENGTH_DELIMITED) + operation.varIntSize(size.getAsInt()) + size.getAsInt()) : OptionalInt.empty();
    }

    @SuppressWarnings("unchecked")
    @Override
    public Class<? extends ByteBufferMarshalledValue<Object>> getJavaClass() {
        return (Class<ByteBufferMarshalledValue<Object>>) (Class<?>) ByteBufferMarshalledValue.class;
    }
}
