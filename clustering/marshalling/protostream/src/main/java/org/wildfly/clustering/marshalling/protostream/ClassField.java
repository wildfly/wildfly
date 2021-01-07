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
import java.lang.reflect.Array;
import java.util.OptionalInt;

import org.infinispan.protostream.BaseMarshaller;
import org.infinispan.protostream.ImmutableSerializationContext;
import org.infinispan.protostream.RawProtoStreamReader;
import org.infinispan.protostream.RawProtoStreamWriter;

import protostream.com.google.protobuf.CodedOutputStream;

/**
 * Various strategies for marshalling a Class.
 * @author Paul Ferraro
 */
public enum ClassField implements Field<Class<?>> {
    ANY() {
        @Override
        public Class<?> readFrom(ImmutableSerializationContext context, RawProtoStreamReader reader) throws IOException {
            return this.getMarshaller(context).readFrom(context, reader);
        }

        @Override
        public void writeTo(ImmutableSerializationContext context, RawProtoStreamWriter writer, Class<?> targetClass) throws IOException {
            this.getMarshaller(context).writeTo(context, writer, targetClass);
        }

        @Override
        public OptionalInt size(ImmutableSerializationContext context, Class<?> value) {
            return this.getMarshaller(context).size(context, value);
        }

        @SuppressWarnings("unchecked")
        private ProtoStreamMarshaller<Class<?>> getMarshaller(ImmutableSerializationContext context) {
            return (ProtoStreamMarshaller<Class<?>>) (ProtoStreamMarshaller<?>) context.getMarshaller(Class.class);
        }
    },
    ARRAY() {
        @Override
        public Class<?> readFrom(ImmutableSerializationContext context, RawProtoStreamReader reader) throws IOException {
            int dimensions = reader.readUInt32();
            Class<?> targetClass = ClassField.ANY.readFrom(context, reader);
            for (int i = 0; i < dimensions; ++i) {
                targetClass = Array.newInstance(targetClass, 0).getClass();
            }
            return targetClass;
        }

        @Override
        public void writeTo(ImmutableSerializationContext context, RawProtoStreamWriter writer, Class<?> targetClass) throws IOException {
            int dimensions = 0;
            Class<?> componentClass = targetClass;
            while (componentClass.isArray() && !componentClass.getComponentType().isPrimitive()) {
                componentClass = componentClass.getComponentType();
                dimensions += 1;
            }
            writer.writeUInt32NoTag(dimensions);
            ClassField.ANY.writeTo(context, writer, componentClass);
        }

        @Override
        public OptionalInt size(ImmutableSerializationContext context, Class<?> targetClass) {
            int dimensions = 0;
            Class<?> componentClass = targetClass;
            while (componentClass.isArray() && !componentClass.getComponentType().isPrimitive()) {
                componentClass = componentClass.getComponentType();
                dimensions += 1;
            }
            return OptionalInt.of(CodedOutputStream.computeUInt32SizeNoTag(dimensions) + ClassField.ANY.size(context, componentClass).getAsInt());
        }
    },
    FIELD() {
        @Override
        public Class<?> readFrom(ImmutableSerializationContext context, RawProtoStreamReader reader) throws IOException {
            return AnyField.fromIndex(reader.readUInt32()).getJavaClass();
        }

        @Override
        public void writeTo(ImmutableSerializationContext context, RawProtoStreamWriter writer, Class<?> value) throws IOException {
            writer.writeUInt32NoTag(AnyField.fromJavaType(value).getIndex());
        }

        @Override
        public OptionalInt size(ImmutableSerializationContext context, Class<?> value) {
            return OptionalInt.of(CodedOutputStream.computeUInt32SizeNoTag(AnyField.fromJavaType(value).getIndex()));
        }
    },
    ID() {
        @Override
        public Class<?> readFrom(ImmutableSerializationContext context, RawProtoStreamReader reader) throws IOException {
            int typeId = reader.readUInt32();
            String typeName = context.getDescriptorByTypeId(typeId).getFullName();
            BaseMarshaller<?> marshaller = context.getMarshaller(typeName);
            return marshaller.getJavaClass();
        }

        @Override
        public void writeTo(ImmutableSerializationContext context, RawProtoStreamWriter writer, Class<?> value) throws IOException {
            BaseMarshaller<?> marshaller = context.getMarshaller(value);
            String typeName = marshaller.getTypeName();
            int typeId = context.getDescriptorByName(typeName).getTypeId();
            writer.writeUInt32NoTag(typeId);
        }

        @Override
        public OptionalInt size(ImmutableSerializationContext context, Class<?> value) {
            BaseMarshaller<?> marshaller = context.getMarshaller(value);
            String typeName = marshaller.getTypeName();
            int typeId = context.getDescriptorByName(typeName).getTypeId();
            return OptionalInt.of(CodedOutputStream.computeUInt32SizeNoTag(typeId));
        }
    },
    NAME() {
        @Override
        public Class<?> readFrom(ImmutableSerializationContext context, RawProtoStreamReader reader) throws IOException {
            String typeName = AnyField.STRING.cast(String.class).readFrom(context, reader);
            BaseMarshaller<?> marshaller = context.getMarshaller(typeName);
            return marshaller.getJavaClass();
        }

        @Override
        public void writeTo(ImmutableSerializationContext context, RawProtoStreamWriter writer, Class<?> value) throws IOException {
            BaseMarshaller<?> marshaller = context.getMarshaller(value);
            String typeName = marshaller.getTypeName();
            AnyField.STRING.writeTo(context, writer, typeName);
        }

        @Override
        public OptionalInt size(ImmutableSerializationContext context, Class<?> value) {
            BaseMarshaller<?> marshaller = context.getMarshaller(value);
            String typeName = marshaller.getTypeName();
            return AnyField.STRING.size(context, typeName);
        }
    },
    OBJECT() {
        @Override
        public Class<?> readFrom(ImmutableSerializationContext context, RawProtoStreamReader reader) throws IOException {
            return Object.class;
        }

        @Override
        public void writeTo(ImmutableSerializationContext context, RawProtoStreamWriter writer, Class<?> value) throws IOException {
        }

        @Override
        public OptionalInt size(ImmutableSerializationContext context, Class<?> value) {
            return OptionalInt.of(0);
        }
    },
    ;
    @SuppressWarnings("unchecked")
    private static final Class<? extends Class<?>> TARGET_CLASS = (Class<? extends Class<?>>) (Class<?>) Class.class;

    @Override
    public Class<? extends Class<?>> getJavaClass() {
        return TARGET_CLASS;
    }

    @Override
    public int getIndex() {
        return this.ordinal();
    }
}