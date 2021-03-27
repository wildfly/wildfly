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
import java.util.function.Supplier;

import org.infinispan.protostream.BaseMarshaller;
import org.infinispan.protostream.ImmutableSerializationContext;

import protostream.com.google.protobuf.CodedOutputStream;

/**
 * {@link ProtoStreamWriter} implementation that does not write to any stream, but instead computes the number of bytes that would be written to a stream.
 * @author Paul Ferraro
 */
public class SizeComputingProtoStreamWriter implements ProtoStreamWriter, Supplier<OptionalInt> {

    private final ImmutableSerializationContext context;
    private int size = 0;
    private boolean present = true;

    public SizeComputingProtoStreamWriter(ImmutableSerializationContext context) {
        this.context = context;
    }

    @Override
    public OptionalInt get() {
        return this.present ? OptionalInt.of(this.size) : OptionalInt.empty();
    }

    @Override
    public ImmutableSerializationContext getSerializationContext() {
        return this.context;
    }

    @Override
    public void writeTag(int index, int wireType) {
        if (this.present) {
            this.size += CodedOutputStream.computeTagSize(index);
        }
    }

    @Override
    public void writeUInt32NoTag(int value) {
        if (this.present) {
            this.size += CodedOutputStream.computeUInt32SizeNoTag(value);
        }
    }

    @Override
    public void writeUInt64NoTag(long value) {
        if (this.present) {
            this.size += CodedOutputStream.computeUInt64SizeNoTag(value);
        }
    }

    @Override
    public void writeString(int index, String value) {
        if (this.present) {
            this.size += CodedOutputStream.computeStringSize(index, value);
        }
    }

    @Override
    public void writeInt32(int index, int value) {
        if (this.present) {
            this.size += CodedOutputStream.computeInt32Size(index, value);
        }
    }

    @Override
    public void writeFixed32(int index, int value) {
        if (this.present) {
            this.size += CodedOutputStream.computeFixed32Size(index, value);
        }
    }

    @Override
    public void writeUInt32(int index, int value) {
        if (this.present) {
            this.size += CodedOutputStream.computeUInt32Size(index, value);
        }
    }

    @Override
    public void writeSFixed32(int index, int value) {
        if (this.present) {
            this.size += CodedOutputStream.computeSFixed32Size(index, value);
        }
    }

    @Override
    public void writeSInt32(int index, int value) {
        if (this.present) {
            this.size += CodedOutputStream.computeSInt32Size(index, value);
        }
    }

    @Override
    public void writeInt64(int index, long value) {
        if (this.present) {
            this.size += CodedOutputStream.computeInt64Size(index, value);
        }
    }

    @Override
    public void writeUInt64(int index, long value) {
        if (this.present) {
            this.size += CodedOutputStream.computeUInt64Size(index, value);
        }
    }

    @Override
    public void writeFixed64(int index, long value) {
        if (this.present) {
            this.size += CodedOutputStream.computeFixed64Size(index, value);
        }
    }

    @Override
    public void writeSFixed64(int index, long value) {
        if (this.present) {
            this.size += CodedOutputStream.computeSFixed64Size(index, value);
        }
    }

    @Override
    public void writeSInt64(int index, long value) {
        if (this.present) {
            this.size += CodedOutputStream.computeSInt64Size(index, value);
        }
    }

    @Override
    public void writeEnum(int index, int value) {
        if (this.present) {
            this.size += CodedOutputStream.computeEnumSize(index, value);
        }
    }

    @Override
    public void writeBool(int index, boolean value) {
        if (this.present) {
            this.size += CodedOutputStream.computeBoolSize(index, value);
        }
    }

    @Override
    public void writeDouble(int index, double value) {
        if (this.present) {
            this.size += CodedOutputStream.computeDoubleSize(index, value);
        }
    }

    @Override
    public void writeFloat(int index, float value) {
        if (this.present) {
            this.size += CodedOutputStream.computeFloatSize(index, value);
        }
    }

    @Override
    public void writeBytes(int index, ByteBuffer value) {
        if (this.present) {
            this.size += CodedOutputStream.computeByteBufferSize(index, value);
        }
    }

    @Override
    public void writeBytes(int index, byte[] value) {
        if (this.present) {
            this.size += CodedOutputStream.computeByteArraySize(index, value);
        }
    }

    @Override
    public void writeBytes(int index, byte[] value, int offset, int length) {
        if (this.present) {
            this.writeBytes(index, ByteBuffer.wrap(value, offset, length));
        }
    }

    @Override
    public void writeRawBytes(byte[] value, int offset, int length) {
        if (this.present) {
            this.size += CodedOutputStream.computeByteBufferSizeNoTag(ByteBuffer.wrap(value, offset, length));
        }
    }

    @Override
    public void flush() {
        // Do nothing
    }

    @Override
    public void writeObject(int index, Object value) {
        if (this.present) {
            this.size += CodedOutputStream.computeTagSize(index);
            this.writeObjectNoTag(value);
        }
    }

    @Override
    public void writeObjectNoTag(Object value) {
        if (this.present) {
            BaseMarshaller<?> marshaller = this.context.getMarshaller(value.getClass());
            @SuppressWarnings("unchecked")
            OptionalInt size = (marshaller instanceof Marshallable) ? ((Marshallable<Object>) marshaller).size(this.context, value) : OptionalInt.empty();
            if (size.isPresent()) {
                this.size += CodedOutputStream.computeUInt32SizeNoTag(size.getAsInt()) + size.getAsInt();
            } else {
                this.present = false;
            }
        }
    }

    @Override
    public <E extends Enum<E>> void writeEnum(int index, E value) throws IOException {
        if (this.present) {
            EnumMarshaller<E> marshaller = (EnumMarshaller<E>) this.context.getMarshaller(value.getDeclaringClass());
            this.size += CodedOutputStream.computeEnumSize(index, marshaller.encode(value));
        }
    }

    @Override
    public void writeStringNoTag(String value) {
        if (this.present) {
            this.size += CodedOutputStream.computeStringSizeNoTag(value);
        }
    }

    @Override
    public void writeBoolNoTag(boolean value) throws IOException {
        if (this.present) {
            this.size += CodedOutputStream.computeBoolSizeNoTag(value);
        }
    }

    @Override
    public void writeSInt32NoTag(int value) throws IOException {
        if (this.present) {
            this.size += CodedOutputStream.computeSInt32SizeNoTag(value);
        }
    }

    @Override
    public void writeSInt64NoTag(long value) throws IOException {
        if (this.present) {
            this.size += CodedOutputStream.computeSInt64SizeNoTag(value);
        }
    }

    @Override
    public void writeFloatNoTag(float value) throws IOException {
        if (this.present) {
            this.size += CodedOutputStream.computeFloatSizeNoTag(value);
        }
    }

    @Override
    public void writeDoubleNoTag(double value) throws IOException {
        if (this.present) {
            this.size += CodedOutputStream.computeDoubleSizeNoTag(value);
        }
    }
}
