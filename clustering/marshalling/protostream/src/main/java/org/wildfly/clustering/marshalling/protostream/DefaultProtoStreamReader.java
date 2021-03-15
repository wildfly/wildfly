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

import org.infinispan.protostream.EnumMarshaller;
import org.infinispan.protostream.ImmutableSerializationContext;
import org.infinispan.protostream.RawProtoStreamReader;
import org.infinispan.protostream.RawProtobufMarshaller;
import org.infinispan.protostream.impl.RawProtoStreamReaderImpl;

import protostream.com.google.protobuf.CodedInputStream;

/**
 * {@link ProtoStreamWriter} implementation that reads from a {@link CodedInputStream}.
 * @author Paul Ferraro
 */
public class DefaultProtoStreamReader implements ProtoStreamReader {

    private final ImmutableSerializationContext context;
    private final CodedInputStream input;

    public DefaultProtoStreamReader(ImmutableSerializationContext context, RawProtoStreamReader reader) {
        this(context, ((RawProtoStreamReaderImpl) reader).getDelegate());
    }

    public DefaultProtoStreamReader(ImmutableSerializationContext context, CodedInputStream input) {
        this.context = context;
        this.input = input;
    }

    @Override
    public ImmutableSerializationContext getSerializationContext() {
        return this.context;
    }

    @Override
    public <T> T readObject(Class<T> targetClass) throws IOException {
        int limit = this.input.readUInt32();
        int oldLimit = this.input.pushLimit(limit);
        try {
            RawProtobufMarshaller<T> marshaller = (RawProtobufMarshaller<T>) this.context.getMarshaller(targetClass);
            // Avoid redundant wrapping of the RawProtoStreamReader
            T result = (marshaller instanceof ProtoStreamMarshaller) ? ((ProtoStreamMarshaller<T>) marshaller).readFrom(this) : marshaller.readFrom(this.context, this);
            // Ensure marshaller reached limit
            this.input.checkLastTagWas(0);
            return result;
        } finally {
            this.input.popLimit(oldLimit);
        }
    }

    @Override
    public <E extends Enum<E>> E readEnum(Class<E> enumClass) throws IOException {
        EnumMarshaller<E> marshaller = (EnumMarshaller<E>) this.context.getMarshaller(enumClass);
        int code = this.input.readEnum();
        return marshaller.decode(code);
    }

    @Override
    public int readTag() throws IOException {
        return this.input.readTag();
    }

    @Override
    public void checkLastTagWas(int tag) throws IOException {
        this.input.checkLastTagWas(tag);
    }

    @Override
    public boolean skipField(int tag) throws IOException {
        return this.input.skipField(tag);
    }

    @Override
    public boolean readBool() throws IOException {
        return this.input.readBool();
    }

    @Override
    public int readEnum() throws IOException {
        return this.input.readEnum();
    }

    @Override
    public byte readRawByte() throws IOException {
        return this.input.readRawByte();
    }

    @Override
    public String readString() throws IOException {
        return this.input.readString();
    }

    @Override
    public byte[] readByteArray() throws IOException {
        return this.input.readByteArray();
    }

    @Override
    public ByteBuffer readByteBuffer() throws IOException {
        return this.input.readByteBuffer();
    }

    @Override
    public double readDouble() throws IOException {
        return this.input.readDouble();
    }

    @Override
    public float readFloat() throws IOException {
        return this.input.readFloat();
    }

    @Override
    public long readInt64() throws IOException {
        return this.input.readInt64();
    }

    @Override
    public long readUInt64() throws IOException {
        return this.input.readUInt64();
    }

    @Override
    public long readSInt64() throws IOException {
        return this.input.readSInt64();
    }

    @Override
    public long readFixed64() throws IOException {
        return this.input.readFixed64();
    }

    @Override
    public long readSFixed64() throws IOException {
        return this.input.readSFixed64();
    }

    @Override
    public long readRawVarint64() throws IOException {
        return this.input.readRawVarint64();
    }

    @Override
    public int readInt32() throws IOException {
        return this.input.readInt32();
    }

    @Override
    public int readUInt32() throws IOException {
        return this.input.readUInt32();
    }

    @Override
    public int readSInt32() throws IOException {
        return this.input.readSInt32();
    }

    @Override
    public int readFixed32() throws IOException {
        return this.input.readFixed32();
    }

    @Override
    public int readSFixed32() throws IOException {
        return this.input.readSFixed32();
    }

    @Override
    public int readRawVarint32() throws IOException {
        return this.input.readRawVarint32();
    }

    @Override
    public int pushLimit(int limit) throws IOException {
        return this.input.pushLimit(limit);
    }

    @Override
    public void popLimit(int oldLimit) {
        this.input.popLimit(oldLimit);
    }
}
