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
import java.io.InvalidClassException;
import java.lang.reflect.Array;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;
import java.util.OptionalInt;

import org.infinispan.protostream.BaseMarshaller;
import org.infinispan.protostream.ImmutableSerializationContext;
import org.infinispan.protostream.RawProtoStreamReader;
import org.infinispan.protostream.RawProtoStreamWriter;
import org.jboss.modules.Module;
import org.wildfly.security.manager.WildFlySecurityManager;

/**
 * Various strategies for marshalling a Class.
 * @author Paul Ferraro
 */
public enum ClassMarshaller implements ProtoStreamMarshaller<Class<?>> {
    ANY() {
        @Override
        public Class<?> readFrom(ImmutableSerializationContext context, RawProtoStreamReader reader) throws IOException {
            return (Class<?>) ObjectMarshaller.INSTANCE.readFrom(context, reader);
        }

        @Override
        public void writeTo(ImmutableSerializationContext context, RawProtoStreamWriter writer, Class<?> targetClass) throws IOException {
            ObjectMarshaller.INSTANCE.writeTo(context, writer, targetClass);
        }

        @Override
        public OptionalInt size(ImmutableSerializationContext context, Class<?> value) {
            return ObjectMarshaller.INSTANCE.size(context, value);
        }
    },
    ARRAY() {
        @Override
        public Class<?> readFrom(ImmutableSerializationContext context, RawProtoStreamReader reader) throws IOException {
            int dimensions = reader.readUInt32();
            Class<?> targetClass = ClassMarshaller.ANY.readFrom(context, reader);
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
            ClassMarshaller.ANY.writeTo(context, writer, componentClass);
        }

        @Override
        public OptionalInt size(ImmutableSerializationContext context, Class<?> targetClass) {
            int dimensions = 0;
            Class<?> componentClass = targetClass;
            while (componentClass.isArray() && !componentClass.getComponentType().isPrimitive()) {
                componentClass = componentClass.getComponentType();
                dimensions += 1;
            }
            return OptionalInt.of(Predictable.unsignedIntSize(dimensions) + ClassMarshaller.ANY.size(context, componentClass).getAsInt());
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
            return OptionalInt.of(Predictable.unsignedIntSize(AnyField.fromJavaType(value).getIndex()));
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
            return OptionalInt.of(Predictable.unsignedIntSize(typeId));
        }
    },
    LOADED() {
        @Override
        public Class<?> readFrom(ImmutableSerializationContext context, RawProtoStreamReader reader) throws IOException {
            String className = (String) AnyField.STRING.readFrom(context, reader);
            Module module = (Module) ObjectMarshaller.INSTANCE.readFrom(context, reader);
            PrivilegedExceptionAction<Class<?>> action = new PrivilegedExceptionAction<Class<?>>() {
                @Override
                public Class<?> run() throws ClassNotFoundException {
                    return (module != null) ? module.getClassLoader().loadClass(className) : Class.forName(className);
                }
            };
            try {
                return WildFlySecurityManager.doUnchecked(action);
            } catch (PrivilegedActionException e) {
                InvalidClassException exception = new InvalidClassException(className, e.getException().getMessage());
                exception.initCause(e.getException());
                throw exception;
            }
        }

        @Override
        public void writeTo(ImmutableSerializationContext context, RawProtoStreamWriter writer, Class<?> targetClass) throws IOException {
            AnyField.STRING.writeTo(context, writer, targetClass.getName());
            ObjectMarshaller.INSTANCE.writeTo(context, writer, Module.forClass(targetClass));
        }

        @Override
        public OptionalInt size(ImmutableSerializationContext context, Class<?> targetClass) {
            return OptionalInt.of(AnyField.STRING.size(context, targetClass.getName()).getAsInt() + ObjectMarshaller.INSTANCE.size(context, Module.forClass(targetClass)).getAsInt());
        }
    },
    NAME() {
        @Override
        public Class<?> readFrom(ImmutableSerializationContext context, RawProtoStreamReader reader) throws IOException {
            String typeName = (String) AnyField.STRING.readFrom(context, reader);
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

    @Override
    public Class<? extends Class<?>> getJavaClass() {
        return null;
    }
}
