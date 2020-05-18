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

package org.wildfly.clustering.marshalling.protostream;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.OptionalInt;

import org.infinispan.protostream.ImmutableSerializationContext;
import org.infinispan.protostream.ProtobufUtil;
import org.infinispan.protostream.RawProtoStreamReader;
import org.infinispan.protostream.RawProtoStreamWriter;
import org.wildfly.clustering.marshalling.spi.ByteBufferInputStream;
import org.wildfly.clustering.marshalling.spi.ByteBufferOutputStream;

/**
 * @author Paul Ferraro
 */
public enum ObjectMarshaller implements ProtoStreamMarshaller<Object> {
    INSTANCE;

    @Override
    public Class<? extends Object> getJavaClass() {
        return Object.class;
    }

    @Override
    public Object readFrom(ImmutableSerializationContext context, RawProtoStreamReader reader) throws IOException {
        try (InputStream input = new ByteBufferInputStream(reader.readByteBuffer())) {
            return ProtobufUtil.readFrom(context, input, Any.class).get();
        }
    }

    @Override
    public void writeTo(ImmutableSerializationContext context, RawProtoStreamWriter writer, Object value) throws IOException {
        try (ByteBufferOutputStream output = new ByteBufferOutputStream(this.size(context, value))) {
            ProtobufUtil.writeTo(context, output, new Any(value));
            ByteBuffer buffer = output.getBuffer();
            int offset = buffer.arrayOffset();
            int size = buffer.limit() - offset;
            writer.writeUInt32NoTag(size);
            writer.writeRawBytes(buffer.array(), offset, size);
        }
    }

    @Override
    public OptionalInt size(ImmutableSerializationContext context, Object value) {
        Predictable<Any> marshaller = (AnyMarshaller) context.getMarshaller(Any.class);
        OptionalInt size = marshaller.size(context, new Any(value));
        return size.isPresent() ? OptionalInt.of(Predictable.byteArraySize(size.getAsInt())) : OptionalInt.empty();
    }
}
