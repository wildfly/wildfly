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
import java.util.OptionalInt;

import org.infinispan.protostream.ImmutableSerializationContext;
import org.infinispan.protostream.RawProtoStreamReader;
import org.infinispan.protostream.RawProtoStreamWriter;
import org.infinispan.protostream.impl.RawProtoStreamReaderImpl;
import org.infinispan.protostream.impl.RawProtoStreamWriterImpl;
import org.infinispan.protostream.impl.WireFormat;

import protostream.com.google.protobuf.CodedOutputStream;

/**
 * @author Paul Ferraro
 */
public enum PrimitiveMarshaller implements ScalarMarshallerProvider {
    VOID(new ScalarMarshaller<Void>() {
        @Override
        public Void readFrom(ImmutableSerializationContext context, RawProtoStreamReader reader) throws IOException {
            return null;
        }

        @Override
        public void writeTo(ImmutableSerializationContext context, RawProtoStreamWriter writer, Void value) throws IOException {
        }

        @Override
        public OptionalInt size(ImmutableSerializationContext context, Void value) {
            return OptionalInt.of(0);
        }

        @Override
        public Class<? extends Void> getJavaClass() {
            return Void.class;
        }

        @Override
        public int getWireType() {
            return 0;
        }
    }),
    BOOLEAN(new ScalarMarshaller<Boolean>() {
        @Override
        public Boolean readFrom(ImmutableSerializationContext context, RawProtoStreamReader reader) throws IOException {
            return Boolean.valueOf(reader.readBool());
        }

        @Override
        public void writeTo(ImmutableSerializationContext context, RawProtoStreamWriter writer, Boolean value) throws IOException {
            ((RawProtoStreamWriterImpl) writer).getDelegate().writeBoolNoTag(value.booleanValue());
        }

        @Override
        public OptionalInt size(ImmutableSerializationContext context, Boolean value) {
            return OptionalInt.of(CodedOutputStream.computeBoolSizeNoTag(value.booleanValue()));
        }

        @Override
        public Class<? extends Boolean> getJavaClass() {
            return Boolean.class;
        }

        @Override
        public int getWireType() {
            return WireFormat.WIRETYPE_VARINT;
        }
    }),
    BYTE(new ScalarMarshaller<Byte>() {
        @Override
        public Byte readFrom(ImmutableSerializationContext context, RawProtoStreamReader reader) throws IOException {
            return Byte.valueOf(((RawProtoStreamReaderImpl) reader).getDelegate().readRawByte());
        }

        @Override
        public void writeTo(ImmutableSerializationContext context, RawProtoStreamWriter writer, Byte value) throws IOException {
            ((RawProtoStreamWriterImpl) writer).getDelegate().writeRawByte(value.byteValue());
        }

        @Override
        public OptionalInt size(ImmutableSerializationContext context, Byte value) {
            return OptionalInt.of(1);
        }

        @Override
        public Class<? extends Byte> getJavaClass() {
            return Byte.class;
        }

        @Override
        public int getWireType() {
            return WireFormat.WIRETYPE_VARINT;
        }
    }),
    SHORT(new ScalarMarshaller<Short>() {
        @Override
        public Short readFrom(ImmutableSerializationContext context, RawProtoStreamReader reader) throws IOException {
            return Short.valueOf((short) reader.readSInt32());
        }

        @Override
        public void writeTo(ImmutableSerializationContext context, RawProtoStreamWriter writer, Short value) throws IOException {
            ((RawProtoStreamWriterImpl) writer).getDelegate().writeSInt32NoTag(value.shortValue());
        }

        @Override
        public OptionalInt size(ImmutableSerializationContext context, Short value) {
            return OptionalInt.of(CodedOutputStream.computeSInt32SizeNoTag(value.shortValue()));
        }

        @Override
        public Class<? extends Short> getJavaClass() {
            return Short.class;
        }

        @Override
        public int getWireType() {
            return WireFormat.WIRETYPE_VARINT;
        }
    }),
    INTEGER(new ScalarMarshaller<Integer>() {
        @Override
        public Integer readFrom(ImmutableSerializationContext context, RawProtoStreamReader reader) throws IOException {
            return Integer.valueOf(reader.readSInt32());
        }

        @Override
        public void writeTo(ImmutableSerializationContext context, RawProtoStreamWriter writer, Integer value) throws IOException {
            ((RawProtoStreamWriterImpl) writer).getDelegate().writeSInt32NoTag(value.intValue());
        }

        @Override
        public OptionalInt size(ImmutableSerializationContext context, Integer value) {
            return OptionalInt.of(CodedOutputStream.computeSInt32SizeNoTag(value.intValue()));
        }

        @Override
        public Class<? extends Integer> getJavaClass() {
            return Integer.class;
        }

        @Override
        public int getWireType() {
            return WireFormat.WIRETYPE_VARINT;
        }
    }),
    LONG(new ScalarMarshaller<Long>() {
        @Override
        public Long readFrom(ImmutableSerializationContext context, RawProtoStreamReader reader) throws IOException {
            return Long.valueOf(reader.readSInt64());
        }

        @Override
        public void writeTo(ImmutableSerializationContext context, RawProtoStreamWriter writer, Long value) throws IOException {
            ((RawProtoStreamWriterImpl) writer).getDelegate().writeSInt64NoTag(value.longValue());
        }

        @Override
        public OptionalInt size(ImmutableSerializationContext context, Long value) {
            return OptionalInt.of(CodedOutputStream.computeSInt64SizeNoTag(value.longValue()));
        }

        @Override
        public Class<? extends Long> getJavaClass() {
            return Long.class;
        }

        @Override
        public int getWireType() {
            return WireFormat.WIRETYPE_VARINT;
        }
    }),
    FLOAT(new ScalarMarshaller<Float>() {
        @Override
        public Float readFrom(ImmutableSerializationContext context, RawProtoStreamReader reader) throws IOException {
            return Float.valueOf(reader.readFloat());
        }

        @Override
        public void writeTo(ImmutableSerializationContext context, RawProtoStreamWriter writer, Float value) throws IOException {
            ((RawProtoStreamWriterImpl) writer).getDelegate().writeFloatNoTag(value.floatValue());
        }

        @Override
        public OptionalInt size(ImmutableSerializationContext context, Float value) {
            return OptionalInt.of(CodedOutputStream.computeFloatSizeNoTag(value.floatValue()));
        }

        @Override
        public Class<? extends Float> getJavaClass() {
            return Float.class;
        }

        @Override
        public int getWireType() {
            return WireFormat.WIRETYPE_FIXED32;
        }
    }),
    DOUBLE(new ScalarMarshaller<Double>() {
        @Override
        public Double readFrom(ImmutableSerializationContext context, RawProtoStreamReader reader) throws IOException {
            return Double.valueOf(reader.readDouble());
        }

        @Override
        public void writeTo(ImmutableSerializationContext context, RawProtoStreamWriter writer, Double value) throws IOException {
            ((RawProtoStreamWriterImpl) writer).getDelegate().writeDoubleNoTag(value.doubleValue());
        }

        @Override
        public OptionalInt size(ImmutableSerializationContext context, Double value) {
            return OptionalInt.of(CodedOutputStream.computeDoubleSizeNoTag(value.doubleValue()));
        }

        @Override
        public Class<? extends Double> getJavaClass() {
            return Double.class;
        }

        @Override
        public int getWireType() {
            return WireFormat.WIRETYPE_FIXED64;
        }
    }),
    CHARACTER(new ScalarMarshaller<Character>() {
        @Override
        public Character readFrom(ImmutableSerializationContext context, RawProtoStreamReader reader) throws IOException {
            return Character.valueOf((char) reader.readUInt32());
        }

        @Override
        public void writeTo(ImmutableSerializationContext context, RawProtoStreamWriter writer, Character value) throws IOException {
            writer.writeUInt32NoTag(value.charValue());
        }

        @Override
        public Class<? extends Character> getJavaClass() {
            return Character.class;
        }

        @Override
        public int getWireType() {
            return WireFormat.WIRETYPE_VARINT;
        }
    }),
    ;
    private final ScalarMarshaller<?> marshaller;

    PrimitiveMarshaller(ScalarMarshaller<?> marshaller) {
        this.marshaller = marshaller;
    }

    @Override
    public ScalarMarshaller<?> getMarshaller() {
        return this.marshaller;
    }
}
