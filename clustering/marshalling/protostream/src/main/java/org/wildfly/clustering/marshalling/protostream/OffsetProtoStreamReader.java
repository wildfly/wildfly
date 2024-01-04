/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.marshalling.protostream;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;

import org.infinispan.protostream.ImmutableSerializationContext;
import org.infinispan.protostream.descriptors.WireType;

/**
 * @author Paul Ferraro
 */
public class OffsetProtoStreamReader implements ProtoStreamReader {

    private final ProtoStreamReader reader;
    private final int offset;

    OffsetProtoStreamReader(ProtoStreamReader reader, int offset) {
        this.reader = reader;
        this.offset = offset;
    }

    @Override
    public Context getContext() {
        return this.reader.getContext();
    }

    @Override
    public ImmutableSerializationContext getSerializationContext() {
        return this.reader.getSerializationContext();
    }

    @Override
    public <T> FieldSetReader<T> createFieldSetReader(FieldReadable<T> reader, int startIndex) {
        int endIndex = reader.nextIndex(startIndex);
        // Since this is a nested field set, the startIndex is relative
        int absoluteStartIndex = this.offset + startIndex;
        ProtoStreamReader offsetReader = new OffsetProtoStreamReader(this, absoluteStartIndex);
        return new FieldSetReader<>() {
            @Override
            public T readField(T current) throws IOException {
                int tag = offsetReader.getCurrentTag();
                // Determine index relative to this field set
                int relativeIndex = WireType.getTagFieldNumber(tag) - absoluteStartIndex;
                return reader.readFrom(offsetReader, relativeIndex, WireType.fromTag(tag), current);
            }

            @Override
            public boolean contains(int index) {
                return (index >= startIndex) && (index < endIndex);
            }
        };
    }

    @Override
    public int getCurrentTag() {
        return this.reader.getCurrentTag();
    }

    @Override
    public boolean isAtEnd() throws IOException {
        return this.reader.isAtEnd();
    }

    @Override
    public int readTag() throws IOException {
        return this.reader.readTag();
    }

    @Override
    public void checkLastTagWas(int tag) throws IOException {
        this.reader.checkLastTagWas(tag);
    }

    @Override
    public boolean skipField(int tag) throws IOException {
        return this.reader.skipField(tag);
    }

    @Override
    public boolean readBool() throws IOException {
        return this.reader.readBool();
    }

    @Override
    public int readEnum() throws IOException {
        return this.reader.readEnum();
    }

    @Override
    public String readString() throws IOException {
        return this.reader.readString();
    }

    @Override
    public byte[] readByteArray() throws IOException {
        return this.reader.readByteArray();
    }

    @Override
    public ByteBuffer readByteBuffer() throws IOException {
        return this.reader.readByteBuffer();
    }

    @Override
    public double readDouble() throws IOException {
        return this.reader.readDouble();
    }

    @Override
    public float readFloat() throws IOException {
        return this.reader.readFloat();
    }

    @Override
    public long readUInt64() throws IOException {
        return this.reader.readUInt64();
    }

    @Override
    public long readSInt64() throws IOException {
        return this.reader.readSFixed64();
    }

    @Override
    public long readSFixed64() throws IOException {
        return this.reader.readSFixed64();
    }

    @Override
    public int readUInt32() throws IOException {
        return this.reader.readUInt32();
    }

    @Override
    public int readSInt32() throws IOException {
        return this.reader.readSInt32();
    }

    @Override
    public int readSFixed32() throws IOException {
        return this.reader.readSFixed32();
    }

    @Override
    public int pushLimit(int limit) throws IOException {
        return this.reader.pushLimit(limit);
    }

    @Override
    public void popLimit(int oldLimit) {
        this.reader.popLimit(oldLimit);
    }

    @Override
    public byte[] fullBufferArray() throws IOException {
        return this.reader.fullBufferArray();
    }

    @Override
    public InputStream fullBufferInputStream() throws IOException {
        return this.reader.fullBufferInputStream();
    }

    @Override
    public boolean isInputStream() {
        return this.reader.isInputStream();
    }

    @Override
    public Object readAny() throws IOException {
        return this.reader.readAny();
    }

    @Override
    public <T> T readObject(Class<T> targetClass) throws IOException {
        return this.reader.readObject(targetClass);
    }

    @Deprecated
    @Override
    public int readInt32() throws IOException {
        return this.reader.readInt32();
    }

    @Deprecated
    @Override
    public int readFixed32() throws IOException {
        return this.reader.readFixed32();
    }

    @Deprecated
    @Override
    public long readInt64() throws IOException {
        return this.reader.readInt64();
    }

    @Deprecated
    @Override
    public long readFixed64() throws IOException {
        return this.reader.readFixed64();
    }
}
