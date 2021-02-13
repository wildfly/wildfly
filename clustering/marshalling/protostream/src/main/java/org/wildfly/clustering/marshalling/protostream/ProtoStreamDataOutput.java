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

import java.io.DataOutput;
import java.io.IOException;

import org.infinispan.protostream.ImmutableSerializationContext;
import org.infinispan.protostream.RawProtoStreamWriter;

/**
 * {@link DataOutput} facade for a {@link RawProtoStreamWriter} allowing serializers to write protobuf messages.
 * This implementation intentionally does not conform to the binary layout prescribed by {@link DataOutput}.
 * @author Paul Ferraro
 */
public class ProtoStreamDataOutput implements DataOutput {

    private final ProtoStreamWriter writer;

    public ProtoStreamDataOutput(ImmutableSerializationContext context, RawProtoStreamWriter writer) {
        this.writer = new DefaultProtoStreamWriter(context, writer);
    }

    public ProtoStreamDataOutput(ProtoStreamWriter writer) {
        this.writer = writer;
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
    public void writeBoolean(boolean value) throws IOException {
        this.writer.writeBoolNoTag(value);
    }

    @Override
    public void writeByte(int value) throws IOException {
        this.writer.writeUInt32NoTag((byte) value);
    }

    @Override
    public void writeShort(int value) throws IOException {
        this.writer.writeUInt32NoTag((short) value);
    }

    @Override
    public void writeChar(int value) throws IOException {
        this.writer.writeUInt32NoTag(value);
    }

    @Override
    public void writeInt(int value) throws IOException {
        this.writer.writeSInt32NoTag(value);
    }

    @Override
    public void writeLong(long value) throws IOException {
        this.writer.writeSInt64NoTag(value);
    }

    @Override
    public void writeFloat(float value) throws IOException {
        this.writer.writeFloatNoTag(value);
    }

    @Override
    public void writeDouble(double value) throws IOException {
        this.writer.writeDoubleNoTag(value);
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
            this.writeChar(value.charAt(i));
        }
    }

    @Override
    public void writeUTF(String value) throws IOException {
        this.writer.writeStringNoTag(value);
    }
}
