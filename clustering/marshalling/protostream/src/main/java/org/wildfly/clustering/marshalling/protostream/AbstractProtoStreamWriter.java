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

import org.infinispan.protostream.ProtobufTagMarshaller.WriteContext;
import org.infinispan.protostream.TagWriter;
import org.infinispan.protostream.descriptors.WireType;

/**
 * Delegates most {@link ProtoStreamWriter} operations to a {@link TagWriter}.
 * @author Paul Ferraro
 */
public abstract class AbstractProtoStreamWriter extends DefaultProtoStreamOperation implements ProtoStreamWriter, WriteContext {

    private final TagWriter writer;

    protected AbstractProtoStreamWriter(WriteContext context) {
        super(context);
        this.writer = context.getWriter();
    }

    @Override
    public TagWriter getWriter() {
        return this.writer;
    }

    @Override
    public void writeTag(int number, int wireType) throws IOException {
        this.writer.writeTag(number, wireType);
    }

    @Override
    public void writeTag(int number, WireType wireType) throws IOException {
        this.writer.writeTag(number, wireType);
    }

    @Override
    public void writeVarint32(int value) throws IOException {
        this.writer.writeVarint32(value);
    }

    @Override
    public void writeVarint64(long value) throws IOException {
        this.writer.writeVarint64(value);
    }

    @Deprecated
    @Override
    public void writeRawByte(byte value) throws IOException {
        this.writer.writeRawByte(value);
    }

    @Override
    public void writeRawBytes(byte[] value, int offset, int length) throws IOException {
        this.writer.writeRawBytes(value, offset, length);
    }

    @Deprecated
    @Override
    public void writeRawBytes(ByteBuffer value) throws IOException {
        this.writer.writeRawBytes(value);
    }

    @Override
    public void writeBool(int index, boolean value) throws IOException {
        this.writer.writeBool(index, value);
    }

    @Override
    public void writeEnum(int index, int value) throws IOException {
        this.writer.writeEnum(index, value);
    }

    @Override
    public void writeInt32(int index, int value) throws IOException {
        this.writer.writeInt32(index, value);
    }

    @Override
    public void writeFixed32(int index, int value) throws IOException {
        this.writer.writeFixed32(index, value);
    }

    @Override
    public void writeUInt32(int index, int value) throws IOException {
        this.writer.writeUInt32(index, value);
    }

    @Override
    public void writeSInt32(int index, int value) throws IOException {
        this.writer.writeSInt32(index, value);
    }

    @Override
    public void writeSFixed32(int index, int value) throws IOException {
        this.writer.writeSFixed32(index, value);
    }

    @Override
    public void writeInt64(int index, long value) throws IOException {
        this.writer.writeInt64(index, value);
    }

    @Override
    public void writeFixed64(int index, long value) throws IOException {
        this.writer.writeFixed64(index, value);
    }

    @Override
    public void writeUInt64(int index, long value) throws IOException {
        this.writer.writeUInt64(index, value);
    }

    @Override
    public void writeSInt64(int index, long value) throws IOException {
        this.writer.writeSInt64(index, value);
    }

    @Override
    public void writeSFixed64(int index, long value) throws IOException {
        this.writer.writeSFixed64(index, value);
    }

    @Override
    public void writeFloat(int index, float value) throws IOException {
        this.writer.writeFloat(index, value);
    }

    @Override
    public void writeDouble(int index, double value) throws IOException {
        this.writer.writeDouble(index, value);
    }

    @Override
    public void writeBytes(int index, byte[] value) throws IOException {
        this.writer.writeBytes(index, value);
    }

    @Override
    public void writeBytes(int index, byte[] value, int offset, int length) throws IOException {
        this.writer.writeBytes(index, value, offset, length);
    }

    @Override
    public void writeBytes(int index, ByteBuffer value) throws IOException {
        this.writer.writeBytes(index, value);
    }

    @Override
    public void writeString(int index, String value) throws IOException {
        this.writer.writeString(index, value);
    }

    @Override
    public void flush() throws IOException {
        this.writer.flush();
    }
}
