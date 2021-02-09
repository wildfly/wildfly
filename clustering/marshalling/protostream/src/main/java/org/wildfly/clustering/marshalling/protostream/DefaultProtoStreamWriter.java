/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2021, Red Hat, Inc., and individual contributors
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
import org.infinispan.protostream.RawProtoStreamWriter;
import org.infinispan.protostream.impl.RawProtoStreamWriterImpl;
import org.infinispan.protostream.impl.WireFormat;
import org.wildfly.clustering.marshalling.spi.ByteBufferOutputStream;

import protostream.com.google.protobuf.CodedOutputStream;

/**
 * {@link ProtoStreamWriter} implementation that writes to a {@link CodedOutputStream}.
 * @author Paul Ferraro
 */
public class DefaultProtoStreamWriter implements ProtoStreamWriter {

    private final ImmutableSerializationContext context;
    private final CodedOutputStream output;

    public DefaultProtoStreamWriter(ImmutableSerializationContext context, RawProtoStreamWriter writer) {
        this(context, ((RawProtoStreamWriterImpl) writer).getDelegate());
    }

    public DefaultProtoStreamWriter(ImmutableSerializationContext context, CodedOutputStream output) {
        this.context = context;
        this.output = output;
    }

    @Override
    public ImmutableSerializationContext getSerializationContext() {
        return this.context;
    }

    @Override
    public void writeObject(int index, Object value) throws IOException {
        this.output.writeTag(index, WireFormat.WIRETYPE_LENGTH_DELIMITED);
        this.writeObjectNoTag(value);
    }

    @Override
    public void writeObjectNoTag(Object value) throws IOException {
        BaseMarshaller<?> marshaller = this.context.getMarshaller(value.getClass());
        @SuppressWarnings("unchecked")
        OptionalInt size = (marshaller instanceof Marshallable) ? ((Marshallable<Object>) marshaller).size(this.context, value) : OptionalInt.empty();
        try (ByteBufferOutputStream output = new ByteBufferOutputStream(size)) {
            ProtobufUtil.writeTo(this.context, output, value);
            ByteBuffer buffer = output.getBuffer();
            int offset = buffer.arrayOffset();
            int length = buffer.limit() - offset;
            this.output.writeUInt32NoTag(length);
            this.output.writeRawBytes(buffer.array(), offset, length);
        }
    }

    @Override
    public <E extends Enum<E>> void writeEnum(int index, E value) throws IOException {
        EnumMarshaller<E> marshaller = (EnumMarshaller<E>) this.context.getMarshaller(value.getDeclaringClass());
        this.output.writeEnum(index, marshaller.encode(value));
    }

    @Override
    public void writeStringNoTag(String value) throws IOException {
        this.output.writeStringNoTag(value);
    }

    @Override
    public void writeBoolNoTag(boolean value) throws IOException {
        this.output.writeBoolNoTag(value);
    }

    @Override
    public void writeSInt32NoTag(int value) throws IOException {
        this.output.writeSInt32NoTag(value);
    }

    @Override
    public void writeSInt64NoTag(long value) throws IOException {
        this.output.writeSInt64NoTag(value);
    }

    @Override
    public void writeFloatNoTag(float value) throws IOException {
        this.output.writeFloatNoTag(value);
    }

    @Override
    public void writeDoubleNoTag(double value) throws IOException {
        this.output.writeDoubleNoTag(value);
    }

    @Override
    public void flush() throws IOException {
        this.output.flush();
    }

    @Override
    public void writeTag(int index, int wireType) throws IOException {
        this.output.writeTag(index, wireType);
    }

    @Override
    public void writeUInt32NoTag(int value) throws IOException {
        this.output.writeUInt32NoTag(value);
    }

    @Override
    public void writeUInt64NoTag(long value) throws IOException {
        this.output.writeUInt64NoTag(value);
    }

    @Override
    public void writeString(int index, String value) throws IOException {
        this.output.writeString(index, value);
    }

    @Override
    public void writeInt32(int index, int value) throws IOException {
        this.output.writeInt32(index, value);
    }

    @Override
    public void writeFixed32(int index, int value) throws IOException {
        this.output.writeFixed32(index, value);
    }

    @Override
    public void writeUInt32(int index, int value) throws IOException {
        this.output.writeUInt32(index, value);
    }

    @Override
    public void writeSFixed32(int index, int value) throws IOException {
        this.output.writeSFixed32(index, value);
    }

    @Override
    public void writeSInt32(int index, int value) throws IOException {
        this.output.writeSInt32(index, value);
    }

    @Override
    public void writeInt64(int index, long value) throws IOException {
        this.output.writeInt64(index, value);
    }

    @Override
    public void writeUInt64(int index, long value) throws IOException {
        this.output.writeUInt64(index, value);
    }

    @Override
    public void writeFixed64(int index, long value) throws IOException {
        this.output.writeFixed64(index, value);
    }

    @Override
    public void writeSFixed64(int index, long value) throws IOException {
        this.output.writeSFixed64(index, value);
    }

    @Override
    public void writeSInt64(int index, long value) throws IOException {
        this.output.writeSInt64(index, value);
    }

    @Override
    public void writeEnum(int index, int value) throws IOException {
        this.output.writeEnum(index, value);
    }

    @Override
    public void writeBool(int index, boolean value) throws IOException {
        this.output.writeBool(index, value);
    }

    @Override
    public void writeDouble(int index, double value) throws IOException {
        this.output.writeDouble(index, value);
    }

    @Override
    public void writeFloat(int index, float value) throws IOException {
        this.output.writeFloat(index, value);
    }

    @Override
    public void writeBytes(int index, ByteBuffer value) throws IOException {
        this.output.writeByteBuffer(index, value);
    }

    @Override
    public void writeBytes(int index, byte[] value) throws IOException {
        this.output.writeByteArray(index, value);
    }

    @Override
    public void writeBytes(int index, byte[] value, int offset, int length) throws IOException {
        this.output.writeByteArray(index, value, offset, length);
    }

    @Override
    public void writeRawBytes(byte[] value, int offset, int length) throws IOException {
        this.output.writeRawBytes(value, offset, length);
    }
}
