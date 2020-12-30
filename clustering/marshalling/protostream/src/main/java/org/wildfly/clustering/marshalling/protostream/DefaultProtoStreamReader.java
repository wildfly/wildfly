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

import org.infinispan.protostream.ProtobufTagMarshaller;
import org.infinispan.protostream.ProtobufTagMarshaller.ReadContext;
import org.infinispan.protostream.TagReader;

/**
 * {@link ProtoStreamWriter} implementation that reads from a {@link TagReader}.
 * @author Paul Ferraro
 */
public class DefaultProtoStreamReader extends DefaultProtoStreamOperation implements ProtoStreamReader, ReadContext {

    private final TagReader reader;

    public DefaultProtoStreamReader(ReadContext context) {
        super(context);
        this.reader = context.getReader();
    }

    @Override
    public TagReader getReader() {
        return this.reader;
    }

    @Override
    public <T> T readObject(Class<T> targetClass) throws IOException {
        int limit = this.reader.readUInt32();
        int oldLimit = this.reader.pushLimit(limit);
        try {
            ProtobufTagMarshaller<T> marshaller = (ProtobufTagMarshaller<T>) this.getSerializationContext().getMarshaller(targetClass);
            // Avoid redundant DefaultProtoStreamReader instance, if possible
            T result = (marshaller instanceof ProtoStreamMarshaller) ? ((ProtoStreamMarshaller<T>) marshaller).readFrom(this) : marshaller.read(this);
            // Ensure marshaller reached limit
            this.reader.checkLastTagWas(0);
            return result;
        } finally {
            this.reader.popLimit(oldLimit);
        }
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
    public int readInt32() throws IOException {
        return this.reader.readInt32();
    }

    @Override
    public int readFixed32() throws IOException {
        return this.reader.readFixed32();
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
    public long readInt64() throws IOException {
        return this.reader.readInt64();
    }

    @Override
    public long readFixed64() throws IOException {
        return this.reader.readFixed64();
    }

    @Override
    public long readUInt64() throws IOException {
        return this.reader.readUInt64();
    }

    @Override
    public long readSInt64() throws IOException {
        return this.reader.readSInt64();
    }

    @Override
    public long readSFixed64() throws IOException {
        return this.reader.readSFixed64();
    }

    @Override
    public float readFloat() throws IOException {
        return this.reader.readFloat();
    }

    @Override
    public double readDouble() throws IOException {
        return this.reader.readDouble();
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
    public String readString() throws IOException {
        return this.reader.readString();
    }
}
