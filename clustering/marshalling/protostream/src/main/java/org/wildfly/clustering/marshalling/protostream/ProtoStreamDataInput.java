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

import java.io.DataInput;
import java.io.IOException;

import org.infinispan.protostream.ImmutableSerializationContext;
import org.infinispan.protostream.RawProtoStreamReader;

/**
 * {@link DataInput} facade for a {@link RawProtoStreamReader} allowing serializers to read protobuf messages.
 * This implementation intentionally does not conform to the binary layout prescribed by {@link DataInput}.
 * @author Paul Ferraro
 */
public class ProtoStreamDataInput implements DataInput {

    private final ProtoStreamReader reader;

    public ProtoStreamDataInput(ImmutableSerializationContext context, RawProtoStreamReader reader) {
        this.reader = new DefaultProtoStreamReader(context, reader);
    }

    public ProtoStreamDataInput(ProtoStreamReader reader) {
        this.reader = reader;
    }

    @Override
    public void readFully(byte[] bytes) throws IOException {
        this.readFully(bytes, 0, bytes.length);
    }

    @Override
    public void readFully(byte[] buffer, int offset, int length) throws IOException {
        for (int i = 0; i < length; ++i) {
            buffer[i + offset] = this.reader.readRawByte();
        }
    }

    @Override
    public int skipBytes(int bytes) throws IOException {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean readBoolean() throws IOException {
        return this.reader.readBool();
    }

    @Override
    public byte readByte() throws IOException {
        return (byte) this.reader.readUInt32();
    }

    @Override
    public int readUnsignedByte() throws IOException {
        return Byte.toUnsignedInt((byte) this.reader.readUInt32());
    }

    @Override
    public short readShort() throws IOException {
        return (short) this.reader.readUInt32();
    }

    @Override
    public int readUnsignedShort() throws IOException {
        return Short.toUnsignedInt((short) this.reader.readUInt32());
    }

    @Override
    public char readChar() throws IOException {
        return (char) this.reader.readUInt32();
    }

    @Override
    public int readInt() throws IOException {
        return this.reader.readSInt32();
    }

    @Override
    public long readLong() throws IOException {
        return this.reader.readSInt64();
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
    public String readLine() throws IOException {
        return this.readUTF();
    }

    @Override
    public String readUTF() throws IOException {
        return this.reader.readString();
    }
}
