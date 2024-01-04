/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.marshalling.protostream;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.OptionalInt;

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
            writer.writeBytes(BUFFER_INDEX, buffer);
        }
        int hashCode = key.hashCode();
        if (hashCode != 0) {
            writer.writeSFixed32(HASH_CODE_INDEX, hashCode);
        }
    }

    @Override
    public OptionalInt size(ProtoStreamSizeOperation operation, ByteBufferMarshalledKey<Object> key) {
        if (key.isEmpty()) return OptionalInt.of(0);
        int hashCodeSize = WireType.FIXED_32_SIZE + operation.tagSize(HASH_CODE_INDEX, WireType.FIXED32);
        OptionalInt size = key.size();
        return size.isPresent() ? OptionalInt.of(operation.tagSize(BUFFER_INDEX, WireType.LENGTH_DELIMITED) + operation.varIntSize(size.getAsInt()) + size.getAsInt() + hashCodeSize) : OptionalInt.empty();
    }

    @SuppressWarnings("unchecked")
    @Override
    public Class<? extends ByteBufferMarshalledKey<Object>> getJavaClass() {
        return (Class<ByteBufferMarshalledKey<Object>>) (Class<?>) ByteBufferMarshalledKey.class;
    }
}
