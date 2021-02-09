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

import org.infinispan.protostream.impl.WireFormat;

/**
 * Enumeration of common scalar marshaller implementations.
 * @author Paul Ferraro
 */
public enum Scalar implements ScalarMarshallerProvider {

    ANY(new ScalarMarshaller<Object>() {
        @Override
        public Object readFrom(ProtoStreamReader reader) throws IOException {
            return reader.readObject(Any.class).get();
        }

        @Override
        public void writeTo(ProtoStreamWriter writer, Object value) throws IOException {
            writer.writeObjectNoTag(new Any(value));
        }

        @Override
        public Class<? extends Object> getJavaClass() {
            return Object.class;
        }

        @Override
        public int getWireType() {
            return WireFormat.WIRETYPE_LENGTH_DELIMITED;
        }
    }),
    BOOLEAN(new ScalarMarshaller<Boolean>() {
        @Override
        public Boolean readFrom(ProtoStreamReader reader) throws IOException {
            return Boolean.valueOf(reader.readBool());
        }

        @Override
        public void writeTo(ProtoStreamWriter writer, Boolean value) throws IOException {
            writer.writeBoolNoTag(value.booleanValue());
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
        public Byte readFrom(ProtoStreamReader reader) throws IOException {
            return Byte.valueOf((byte) reader.readSInt32());
        }

        @Override
        public void writeTo(ProtoStreamWriter writer, Byte value) throws IOException {
            writer.writeSInt32NoTag(value.byteValue());
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
        public Short readFrom(ProtoStreamReader reader) throws IOException {
            return Short.valueOf((short) reader.readSInt32());
        }

        @Override
        public void writeTo(ProtoStreamWriter writer, Short value) throws IOException {
            writer.writeSInt32NoTag(value.shortValue());
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
        public Integer readFrom(ProtoStreamReader reader) throws IOException {
            return Integer.valueOf(reader.readSInt32());
        }

        @Override
        public void writeTo(ProtoStreamWriter writer, Integer value) throws IOException {
            writer.writeSInt32NoTag(value.intValue());
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
        public Long readFrom(ProtoStreamReader reader) throws IOException {
            return Long.valueOf(reader.readSInt64());
        }

        @Override
        public void writeTo(ProtoStreamWriter writer, Long value) throws IOException {
            writer.writeSInt64NoTag(value.longValue());
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
        public Float readFrom(ProtoStreamReader reader) throws IOException {
            return Float.valueOf(reader.readFloat());
        }

        @Override
        public void writeTo(ProtoStreamWriter writer, Float value) throws IOException {
            writer.writeFloatNoTag(value.floatValue());
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
        public Double readFrom(ProtoStreamReader reader) throws IOException {
            return Double.valueOf(reader.readDouble());
        }

        @Override
        public void writeTo(ProtoStreamWriter writer, Double value) throws IOException {
            writer.writeDoubleNoTag(value.doubleValue());
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
        public Character readFrom(ProtoStreamReader reader) throws IOException {
            return Character.valueOf((char) reader.readUInt32());
        }

        @Override
        public void writeTo(ProtoStreamWriter writer, Character value) throws IOException {
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
    BYTE_BUFFER(new ScalarMarshaller<ByteBuffer>() {
        @Override
        public ByteBuffer readFrom(ProtoStreamReader reader) throws IOException {
            return reader.readByteBuffer();
        }

        @Override
        public void writeTo(ProtoStreamWriter writer, ByteBuffer buffer) throws IOException {
            int offset = buffer.arrayOffset();
            int size = buffer.limit() - offset;
            writer.writeUInt32NoTag(size);
            writer.writeRawBytes(buffer.array(), offset, size);
        }

        @Override
        public Class<? extends ByteBuffer> getJavaClass() {
            return ByteBuffer.class;
        }

        @Override
        public int getWireType() {
            return WireFormat.WIRETYPE_LENGTH_DELIMITED;
        }
    }),
    BYTE_ARRAY(new ScalarMarshaller<byte[]>() {
        @Override
        public byte[] readFrom(ProtoStreamReader reader) throws IOException {
            return reader.readByteArray();
        }

        @Override
        public void writeTo(ProtoStreamWriter writer, byte[] value) throws IOException {
            writer.writeUInt32NoTag(value.length);
            writer.writeRawBytes(value, 0, value.length);
        }

        @Override
        public Class<? extends byte[]> getJavaClass() {
            return byte[].class;
        }

        @Override
        public int getWireType() {
            return WireFormat.WIRETYPE_LENGTH_DELIMITED;
        }
    }),
    STRING(new ScalarMarshaller<String>() {
        @Override
        public String readFrom(ProtoStreamReader reader) throws IOException {
            return reader.readString();
        }

        @Override
        public void writeTo(ProtoStreamWriter writer, String value) throws IOException {
            writer.writeStringNoTag(value);
        }

        @Override
        public Class<? extends String> getJavaClass() {
            return String.class;
        }

        @Override
        public int getWireType() {
            return WireFormat.WIRETYPE_LENGTH_DELIMITED;
        }
    }),
    ;
    private final ScalarMarshaller<?> marshaller;

    Scalar(ScalarMarshaller<?> marshaller) {
        this.marshaller = marshaller;
    }

    @Override
    public ScalarMarshaller<?> getMarshaller() {
        return this.marshaller;
    }
}
