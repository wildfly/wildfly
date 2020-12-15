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

/**
 * @author Paul Ferraro
 */
public enum PrimitiveMarshaller implements ProtoStreamMarshallerProvider {
    VOID(Void.class) {
        @Override
        public Boolean readFrom(ImmutableSerializationContext context, RawProtoStreamReader reader) throws IOException {
            return null;
        }

        @Override
        public void writeTo(ImmutableSerializationContext context, RawProtoStreamWriter writer, Object value) throws IOException {
        }

        @Override
        public OptionalInt size(ImmutableSerializationContext context, Object value) {
            return OptionalInt.of(0);
        }
    },
    BOOLEAN(Boolean.class) {
        @Override
        public Boolean readFrom(ImmutableSerializationContext context, RawProtoStreamReader reader) throws IOException {
            return Boolean.valueOf(reader.readBool());
        }

        @Override
        public void writeTo(ImmutableSerializationContext context, RawProtoStreamWriter writer, Object value) throws IOException {
            ((RawProtoStreamWriterImpl) writer).getDelegate().writeBoolNoTag(((Boolean) value).booleanValue());
        }

        @Override
        public OptionalInt size(ImmutableSerializationContext context, Object value) {
            return OptionalInt.of(Byte.BYTES);
        }
    },
    BYTE(Byte.class) {
        @Override
        public Byte readFrom(ImmutableSerializationContext context, RawProtoStreamReader reader) throws IOException {
            return Byte.valueOf(((RawProtoStreamReaderImpl) reader).getDelegate().readRawByte());
        }

        @Override
        public void writeTo(ImmutableSerializationContext context, RawProtoStreamWriter writer, Object value) throws IOException {
            ((RawProtoStreamWriterImpl) writer).getDelegate().writeRawByte(((Byte) value).byteValue());
        }

        @Override
        public OptionalInt size(ImmutableSerializationContext context, Object value) {
            return OptionalInt.of(1);
        }
    },
    SHORT(Short.class) {
        @Override
        public Short readFrom(ImmutableSerializationContext context, RawProtoStreamReader reader) throws IOException {
            return Short.valueOf((short) reader.readSInt32());
        }

        @Override
        public void writeTo(ImmutableSerializationContext context, RawProtoStreamWriter writer, Object value) throws IOException {
            ((RawProtoStreamWriterImpl) writer).getDelegate().writeSInt32NoTag(((Short) value).shortValue());
        }

        @Override
        public OptionalInt size(ImmutableSerializationContext context, Object value) {
            return OptionalInt.of(Predictable.signedIntSize(((Short) value).shortValue()));
        }
    },
    INTEGER(Integer.class) {
        @Override
        public Integer readFrom(ImmutableSerializationContext context, RawProtoStreamReader reader) throws IOException {
            return Integer.valueOf(reader.readSInt32());
        }

        @Override
        public void writeTo(ImmutableSerializationContext context, RawProtoStreamWriter writer, Object value) throws IOException {
            ((RawProtoStreamWriterImpl) writer).getDelegate().writeSInt32NoTag(((Integer) value).intValue());
        }

        @Override
        public OptionalInt size(ImmutableSerializationContext context, Object value) {
            return OptionalInt.of(Predictable.signedIntSize(((Integer) value).intValue()));
        }
    },
    LONG(Long.class) {
        @Override
        public Long readFrom(ImmutableSerializationContext context, RawProtoStreamReader reader) throws IOException {
            return Long.valueOf(reader.readSInt64());
        }

        @Override
        public void writeTo(ImmutableSerializationContext context, RawProtoStreamWriter writer, Object value) throws IOException {
            ((RawProtoStreamWriterImpl) writer).getDelegate().writeSInt64NoTag(((Long) value).longValue());
        }

        @Override
        public OptionalInt size(ImmutableSerializationContext context, Object value) {
            return OptionalInt.of(Predictable.signedLongSize(((Long) value).longValue()));
        }
    },
    FLOAT(Float.class) {
        @Override
        public Float readFrom(ImmutableSerializationContext context, RawProtoStreamReader reader) throws IOException {
            return Float.valueOf(reader.readFloat());
        }

        @Override
        public void writeTo(ImmutableSerializationContext context, RawProtoStreamWriter writer, Object value) throws IOException {
            ((RawProtoStreamWriterImpl) writer).getDelegate().writeFloatNoTag(((Float) value).floatValue());
        }

        @Override
        public OptionalInt size(ImmutableSerializationContext context, Object value) {
            return OptionalInt.of(Float.BYTES);
        }
    },
    DOUBLE(Double.class) {
        @Override
        public Double readFrom(ImmutableSerializationContext context, RawProtoStreamReader reader) throws IOException {
            return Double.valueOf(reader.readDouble());
        }

        @Override
        public void writeTo(ImmutableSerializationContext context, RawProtoStreamWriter writer, Object value) throws IOException {
            ((RawProtoStreamWriterImpl) writer).getDelegate().writeDoubleNoTag(((Double) value).doubleValue());
        }

        @Override
        public OptionalInt size(ImmutableSerializationContext context, Object value) {
            return OptionalInt.of(Double.BYTES);
        }
    },
    CHARACTER(Character.class) {
        @Override
        public Character readFrom(ImmutableSerializationContext context, RawProtoStreamReader reader) throws IOException {
            return Character.valueOf((char) reader.readUInt32());
        }

        @Override
        public void writeTo(ImmutableSerializationContext context, RawProtoStreamWriter writer, Object value) throws IOException {
            Character character = (Character) value;
            writer.writeUInt32NoTag(character.charValue());
        }

        @Override
        public OptionalInt size(ImmutableSerializationContext context, Object value) {
            Character character = (Character) value;
            return OptionalInt.of(Predictable.unsignedIntSize(character.charValue()));
        }
    },
    ;
    private final Class<?> primitiveClass;

    PrimitiveMarshaller(Class<?> primitiveClass) {
        this.primitiveClass = primitiveClass;
    }

    @Override
    public Class<? extends Object> getJavaClass() {
        return this.primitiveClass;
    }

    @Override
    public ProtoStreamMarshaller<?> getMarshaller() {
        return this;
    }
}
