/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
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
