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
import java.io.ObjectOutput;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.OptionalInt;

import org.infinispan.protostream.ImmutableSerializationContext;
import org.infinispan.protostream.ProtobufUtil;
import org.infinispan.protostream.RawProtoStreamWriter;
import org.infinispan.protostream.impl.RawProtoStreamWriterImpl;
import org.wildfly.clustering.marshalling.spi.ByteBufferOutputStream;

import protostream.com.google.protobuf.CodedOutputStream;

/**
 * {@link ObjectOutput} facade for a {@link RawProtoStreamWriter} allowing externalizers to write protobuf messages.
 * This implementation intentionally does not conform to the binary layout prescribed by {@link ObjectOutput}.
 * @author Paul Ferraro
 */
public class ProtoStreamObjectOutput implements ObjectOutput {

    private final ImmutableSerializationContext context;
    private final RawProtoStreamWriter writer;

    public ProtoStreamObjectOutput(ImmutableSerializationContext context, RawProtoStreamWriter writer) {
        this.context = context;
        this.writer = writer;
    }

    @Override
    public void writeBoolean(boolean value) throws IOException {
        RawProtoStreamWriterImpl writer = (RawProtoStreamWriterImpl) this.writer;
        writer.getDelegate().writeBoolNoTag(value);
    }

    @Override
    public void writeByte(int value) throws IOException {
        RawProtoStreamWriterImpl writer = (RawProtoStreamWriterImpl) this.writer;
        writer.getDelegate().writeRawByte((byte) value);
    }

    @Override
    public void writeShort(int value) throws IOException {
        // Write fixed length short, rather than varint
        RawProtoStreamWriterImpl writer = (RawProtoStreamWriterImpl) this.writer;
        writer.getDelegate().writeRawByte((byte) (0xff & (value >> 8)));
        writer.getDelegate().writeRawByte((byte) (0xff & value));
    }

    @Override
    public void writeChar(int value) throws IOException {
        // Use varint encoding instead of unsigned short, as these values are most likely <= Byte.MAX_VALUE
        this.writer.writeUInt32NoTag(value);
    }

    @Override
    public void writeInt(int value) throws IOException {
        RawProtoStreamWriterImpl writer = (RawProtoStreamWriterImpl) this.writer;
        writer.getDelegate().writeSInt32NoTag(value);
    }

    @Override
    public void writeLong(long value) throws IOException {
        RawProtoStreamWriterImpl writer = (RawProtoStreamWriterImpl) this.writer;
        writer.getDelegate().writeSInt64NoTag(value);
    }

    @Override
    public void writeFloat(float value) throws IOException {
        RawProtoStreamWriterImpl writer = (RawProtoStreamWriterImpl) this.writer;
        writer.getDelegate().writeFloatNoTag(value);
    }

    @Override
    public void writeDouble(double value) throws IOException {
        RawProtoStreamWriterImpl writer = (RawProtoStreamWriterImpl) this.writer;
        writer.getDelegate().writeDoubleNoTag(value);
    }

    @Override
    public void writeBytes(String value) throws IOException {
        for (int i = 0; i < value.length(); ++i) {
            this.writeByte(value.charAt(i));
        }
    }

    @Override
    public void writeChars(String value) throws IOException {
        for (int i = 0; i < value.length(); ++i) {
            this.writeShort(value.charAt(i));
        }
    }

    @Override
    public void writeUTF(String value) throws IOException {
        byte[] bytes = value.getBytes(StandardCharsets.UTF_8);
        this.writeChar(bytes.length); // unsigned varint
        this.write(bytes);
    }

    @Override
    public void writeObject(Object object) throws IOException {
        Predictable<Any> marshaller = (AnyMarshaller) this.context.getMarshaller(Any.class);
        Any any = new Any(object);
        OptionalInt size = marshaller.size(this.context, any);
        try (ByteBufferOutputStream output = new ByteBufferOutputStream(size.isPresent() ? OptionalInt.of(CodedOutputStream.computeUInt32SizeNoTag(size.getAsInt()) + size.getAsInt()) : OptionalInt.empty())) {
            ProtobufUtil.writeTo(this.context, output, any);
            ByteBuffer buffer = output.getBuffer();
            int offset = buffer.arrayOffset();
            int length = buffer.limit() - offset;
            this.writeChar(length); // unsigned varint
            this.write(buffer.array(), offset, length);
        }
    }

    @Override
    public void write(int value) throws IOException {
        this.writeByte(value);
    }

    @Override
    public void write(byte[] bytes) throws IOException {
        this.write(bytes, 0, bytes.length);
    }

    @Override
    public void write(byte[] bytes, int offset, int length) throws IOException {
        this.writer.writeRawBytes(bytes, offset, length);
    }

    @Override
    public void flush() throws IOException {
        this.writer.flush();
    }

    @Override
    public void close() throws IOException {
        this.flush();
    }
}
