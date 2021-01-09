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
import java.util.function.IntSupplier;

import org.infinispan.protostream.RawProtoStreamWriter;

import protostream.com.google.protobuf.CodedOutputStream;

/**
 * {@link RawProtoStreamWriter} implementation that computes the size of fields that would otherwise be written.
 * @author Paul Ferraro
 */
public class SizeComputingWriter implements RawProtoStreamWriter, IntSupplier {

    private int size;

    public SizeComputingWriter() {
        this(0);
    }

    public SizeComputingWriter(int size) {
        this.size = size;
    }

    @Override
    public int getAsInt() {
        return this.size;
    }

    @Override
    public void flush() throws IOException {
        // Nothing to flush
    }

    @Override
    public void writeTag(int number, int wireType) throws IOException {
        this.size += CodedOutputStream.computeTagSize(number);
    }

    @Override
    public void writeUInt32NoTag(int value) throws IOException {
        this.size += CodedOutputStream.computeUInt32SizeNoTag(value);
    }

    @Override
    public void writeUInt64NoTag(long value) throws IOException {
        this.size += CodedOutputStream.computeUInt64SizeNoTag(value);
    }

    @Override
    public void writeString(int number, String value) throws IOException {
        this.size += CodedOutputStream.computeStringSize(number, value);
    }

    @Override
    public void writeInt32(int number, int value) throws IOException {
        this.size += CodedOutputStream.computeInt32Size(number, value);
    }

    @Override
    public void writeFixed32(int number, int value) throws IOException {
        this.size += CodedOutputStream.computeFixed32Size(number, value);
    }

    @Override
    public void writeUInt32(int number, int value) throws IOException {
        this.size += CodedOutputStream.computeUInt32Size(number, value);
    }

    @Override
    public void writeSFixed32(int number, int value) throws IOException {
        this.size += CodedOutputStream.computeSFixed32Size(number, value);
    }

    @Override
    public void writeSInt32(int number, int value) throws IOException {
        this.size += CodedOutputStream.computeSInt32Size(number, value);
    }

    @Override
    public void writeInt64(int number, long value) throws IOException {
        this.size += CodedOutputStream.computeInt64Size(number, value);
    }

    @Override
    public void writeUInt64(int number, long value) throws IOException {
        this.size += CodedOutputStream.computeUInt64Size(number, value);
    }

    @Override
    public void writeFixed64(int number, long value) throws IOException {
        this.size += CodedOutputStream.computeFixed64Size(number, value);
    }

    @Override
    public void writeSFixed64(int number, long value) throws IOException {
        this.size += CodedOutputStream.computeSFixed64Size(number, value);
    }

    @Override
    public void writeSInt64(int number, long value) throws IOException {
        this.size += CodedOutputStream.computeSInt64Size(number, value);
    }

    @Override
    public void writeEnum(int number, int value) throws IOException {
        this.size += CodedOutputStream.computeEnumSize(number, value);
    }

    @Override
    public void writeBool(int number, boolean value) throws IOException {
        this.size += CodedOutputStream.computeBoolSize(number, value);
    }

    @Override
    public void writeDouble(int number, double value) throws IOException {
        this.size += CodedOutputStream.computeDoubleSize(number, value);
    }

    @Override
    public void writeFloat(int number, float value) throws IOException {
        this.size += CodedOutputStream.computeFloatSize(number, value);
    }

    @Override
    public void writeBytes(int number, ByteBuffer value) throws IOException {
        this.size += CodedOutputStream.computeByteBufferSize(number, value);
    }

    @Override
    public void writeBytes(int number, byte[] value) throws IOException {
        this.size += CodedOutputStream.computeByteArraySize(number, value);
    }

    @Override
    public void writeBytes(int number, byte[] value, int offset, int length) throws IOException {
        this.size += CodedOutputStream.computeByteBufferSize(number, ByteBuffer.wrap(value, offset, length));
    }

    @Override
    public void writeRawBytes(byte[] value, int offset, int length) throws IOException {
        this.size += CodedOutputStream.computeByteBufferSizeNoTag(ByteBuffer.wrap(value, offset, length));
    }
}
