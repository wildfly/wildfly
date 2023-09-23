/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.marshalling.protostream;

import java.io.IOException;
import java.nio.ByteBuffer;

import org.infinispan.protostream.ImmutableSerializationContext;
import org.infinispan.protostream.descriptors.WireType;

/**
 * @author Paul Ferraro
 */
public class OffsetProtoStreamWriter implements ProtoStreamWriter {

    private final ProtoStreamWriter writer;
    private final int offset;

    OffsetProtoStreamWriter(ProtoStreamWriter writer, int offset) {
        this.writer = writer;
        this.offset = offset;
    }

    @Override
    public <T> FieldSetWriter<T> createFieldSetWriter(Writable<T> writer, int startIndex) {
        ProtoStreamWriter offsetWriter = new OffsetProtoStreamWriter(this, this.offset + startIndex);
        return new FieldSetWriter<>() {
            @Override
            public void writeFields(T value) throws IOException {
                writer.writeTo(offsetWriter, value);
            }
        };
    }

    @Override
    public Context getContext() {
        return this.writer.getContext();
    }

    @Override
    public ImmutableSerializationContext getSerializationContext() {
        return this.writer.getSerializationContext();
    }

    @Override
    public void flush() throws IOException {
        this.writer.flush();
    }

    @Override
    public void writeTag(int number, WireType wireType) throws IOException {
        this.writer.writeTag(this.offset + number, wireType);
    }

    @Override
    public void writeVarint32(int value) throws IOException {
        this.writer.writeVarint32(value);
    }

    @Override
    public void writeVarint64(long value) throws IOException {
        this.writer.writeVarint64(value);
    }

    @Override
    public void writeRawBytes(byte[] value, int offset, int length) throws IOException {
        this.writer.writeRawBytes(value, offset, length);
    }

    @Override
    public void writeString(int number, String value) throws IOException {
        this.writer.writeString(this.offset + number, value);
    }

    @Override
    public void writeUInt32(int number, int value) throws IOException {
        this.writer.writeUInt32(this.offset + number, value);
    }

    @Override
    public void writeSInt32(int number, int value) throws IOException {
        this.writer.writeSInt32(this.offset + number, value);
    }

    @Override
    public void writeSFixed32(int number, int value) throws IOException {
        this.writer.writeSFixed32(this.offset + number, value);
    }

    @Override
    public void writeUInt64(int number, long value) throws IOException {
        this.writer.writeUInt64(this.offset + number, value);
    }

    @Override
    public void writeSInt64(int number, long value) throws IOException {
        this.writer.writeSInt64(this.offset + number, value);
    }

    @Override
    public void writeSFixed64(int number, long value) throws IOException {
        this.writer.writeSFixed64(this.offset + number, value);
    }

    @Override
    public void writeEnum(int number, int value) throws IOException {
        this.writer.writeEnum(this.offset + number, value);
    }

    @Override
    public void writeBool(int number, boolean value) throws IOException {
        this.writer.writeBool(this.offset + number, value);
    }

    @Override
    public void writeDouble(int number, double value) throws IOException {
        this.writer.writeDouble(this.offset + number, value);
    }

    @Override
    public void writeFloat(int number, float value) throws IOException {
        this.writer.writeFloat(this.offset + number, value);
    }

    @Override
    public void writeBytes(int number, ByteBuffer value) throws IOException {
        this.writer.writeBytes(this.offset + number, value);
    }

    @Override
    public void writeBytes(int number, byte[] value) throws IOException {
        this.writer.writeBytes(this.offset + number, value);
    }

    @Override
    public void writeBytes(int number, byte[] value, int offset, int length) throws IOException {
        this.writer.writeBytes(this.offset + number, value);
    }

    @Deprecated
    @Override
    public void writeRawByte(byte value) throws IOException {
        this.writer.writeRawByte(value);
    }

    @Deprecated
    @Override
    public void writeRawBytes(ByteBuffer value) throws IOException {
        this.writer.writeRawBytes(value);
    }

    @Override
    public void writeAnyNoTag(Object value) throws IOException {
        this.writer.writeAnyNoTag(value);
    }

    @Override
    public void writeObjectNoTag(Object value) throws IOException {
        this.writer.writeObjectNoTag(value);
    }

    @Deprecated
    @Override
    public void writeInt32(int index, int value) throws IOException {
        this.writer.writeInt32(this.offset + index, value);
    }

    @Deprecated
    @Override
    public void writeInt64(int index, long value) throws IOException {
        this.writer.writeInt64(this.offset + index, value);
    }

    @Deprecated
    @Override
    public void writeFixed32(int index, int value) throws IOException {
        this.writer.writeFixed32(this.offset + index, value);
    }

    @Deprecated
    @Override
    public void writeFixed64(int index, long value) throws IOException {
        this.writer.writeFixed64(this.offset + index, value);
    }
}
