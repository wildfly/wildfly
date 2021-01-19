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
import java.nio.ByteBuffer;
import java.util.OptionalInt;

import org.infinispan.protostream.BaseMarshaller;
import org.infinispan.protostream.ImmutableSerializationContext;
import org.infinispan.protostream.ProtobufUtil;
import org.infinispan.protostream.RawProtoStreamReader;
import org.infinispan.protostream.RawProtoStreamWriter;
import org.wildfly.clustering.marshalling.spi.ByteBufferOutputStream;

import protostream.com.google.protobuf.CodedOutputStream;

/**
 * @author Paul Ferraro
 */
public class TypedObjectMarshaller implements ProtoStreamMarshaller<Object> {

    private final ProtoStreamMarshaller<Class<?>> typeMarshaller;

    public TypedObjectMarshaller(ProtoStreamMarshaller<Class<?>> typeMarshaller) {
        this.typeMarshaller = typeMarshaller;
    }

    @Override
    public Object readFrom(ImmutableSerializationContext context, RawProtoStreamReader reader) throws IOException {
        ByteBuffer buffer = reader.readByteBuffer();
        Class<?> targetClass = this.typeMarshaller.readFrom(context, reader);
        return ProtobufUtil.fromByteBuffer(context, buffer, targetClass);
    }

    @Override
    public void writeTo(ImmutableSerializationContext context, RawProtoStreamWriter writer, Object value) throws IOException {
        try (ByteBufferOutputStream output = new ByteBufferOutputStream(objectSize(context, value))) {
            ProtobufUtil.writeTo(context, output, value);
            ByteBuffer buffer = output.getBuffer();
            int offset = buffer.arrayOffset();
            int size = buffer.limit() - offset;
            writer.writeUInt32NoTag(size);
            writer.writeRawBytes(buffer.array(), offset, size);
        }

        this.typeMarshaller.writeTo(context, writer, value.getClass());
    }

    @SuppressWarnings("unchecked")
    private static OptionalInt objectSize(ImmutableSerializationContext context, Object value) {
        BaseMarshaller<?> marshaller = context.getMarshaller(value.getClass());
        OptionalInt size = (marshaller instanceof Predictable) ? ((Predictable<Object>) marshaller).size(context, value) : OptionalInt.empty();
        return size.isPresent() ? OptionalInt.of(CodedOutputStream.computeUInt32SizeNoTag(size.getAsInt()) + size.getAsInt()) : size;
    }

    @Override
    public OptionalInt size(ImmutableSerializationContext context, Object value) {
        OptionalInt objectSize = objectSize(context, value);
        OptionalInt typeSize = this.typeMarshaller.size(context, value.getClass());
        return objectSize.isPresent() && typeSize.isPresent() ? OptionalInt.of(objectSize.getAsInt() + typeSize.getAsInt()) : OptionalInt.empty();
    }

    @Override
    public Class<? extends Object> getJavaClass() {
        return null;
    }
}
