/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.infinispan.marshalling.protostream;

import java.io.IOException;

import org.infinispan.commons.io.ByteBufferImpl;
import org.infinispan.protostream.descriptors.WireType;
import org.wildfly.clustering.marshalling.protostream.ProtoStreamMarshaller;
import org.wildfly.clustering.marshalling.protostream.ProtoStreamReader;
import org.wildfly.clustering.marshalling.protostream.ProtoStreamWriter;

/**
 * Marshaller for an Infinispan ByteBuffer.
 * @author Paul Ferraro
 */
public enum ByteBufferMarshaller implements ProtoStreamMarshaller<ByteBufferImpl> {
    INSTANCE;

    private static final int BUFFER_INDEX = 1;

    @Override
    public ByteBufferImpl readFrom(ProtoStreamReader reader) throws IOException {
        ByteBufferImpl buffer = ByteBufferImpl.EMPTY_INSTANCE;
        while (!reader.isAtEnd()) {
            int tag = reader.readTag();
            switch (WireType.getTagFieldNumber(tag)) {
                case BUFFER_INDEX:
                    buffer = ByteBufferImpl.create(reader.readByteArray());
                    break;
                default:
                    reader.skipField(tag);
            }
        }
        return buffer;
    }

    @Override
    public void writeTo(ProtoStreamWriter writer, ByteBufferImpl buffer) throws IOException {
        int length = buffer.getLength();
        if (length > 0) {
            writer.writeBytes(BUFFER_INDEX, buffer.getBuf(), buffer.getOffset(), length);
        }
    }

    @Override
    public Class<? extends ByteBufferImpl> getJavaClass() {
        return ByteBufferImpl.class;
    }
}
