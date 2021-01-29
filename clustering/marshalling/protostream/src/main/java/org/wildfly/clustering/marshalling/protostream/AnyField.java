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
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Proxy;
import java.nio.charset.StandardCharsets;
import java.util.BitSet;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.OptionalInt;

import org.infinispan.protostream.ImmutableSerializationContext;
import org.infinispan.protostream.RawProtoStreamReader;
import org.infinispan.protostream.RawProtoStreamWriter;
import org.infinispan.protostream.impl.RawProtoStreamReaderImpl;
import org.wildfly.security.manager.WildFlySecurityManager;

import protostream.com.google.protobuf.CodedOutputStream;

/**
 * A set of fields used by {@link AnyMarshaller}.
 * @author Paul Ferraro
 */
public enum AnyField implements ProtoStreamMarshallerProvider, Field<Object> {
    BOOLEAN(Boolean.class) {
        @Override
        public ProtoStreamMarshaller<?> getMarshaller() {
            return PrimitiveMarshaller.BOOLEAN;
        }
    },
    BYTE(Byte.class) {
        @Override
        public ProtoStreamMarshaller<?> getMarshaller() {
            return PrimitiveMarshaller.BYTE;
        }
    },
    SHORT(Short.class) {
        @Override
        public ProtoStreamMarshaller<?> getMarshaller() {
            return PrimitiveMarshaller.SHORT;
        }
    },
    INTEGER(Integer.class) {
        @Override
        public ProtoStreamMarshaller<?> getMarshaller() {
            return PrimitiveMarshaller.INTEGER;
        }
    },
    LONG(Long.class) {
        @Override
        public ProtoStreamMarshaller<?> getMarshaller() {
            return PrimitiveMarshaller.LONG;
        }
    },
    FLOAT(Float.class) {
        @Override
        public ProtoStreamMarshaller<?> getMarshaller() {
            return PrimitiveMarshaller.FLOAT;
        }
    },
    DOUBLE(Double.class) {
        @Override
        public ProtoStreamMarshaller<?> getMarshaller() {
            return PrimitiveMarshaller.DOUBLE;
        }
    },
    CHARACTER(Character.class) {
        @Override
        public ProtoStreamMarshaller<?> getMarshaller() {
            return PrimitiveMarshaller.CHARACTER;
        }
    },
    STRING(String.class) {
        private final byte[] empty = new byte[0];

        @Override
        public String readFrom(ImmutableSerializationContext context, RawProtoStreamReader reader) throws IOException {
            byte[] bytes = BYTE_ARRAY.cast(byte[].class).readFrom(context, reader);
            return (bytes.length > 0) ? new String(bytes, StandardCharsets.UTF_8) : null;
        }

        @Override
        public void writeTo(ImmutableSerializationContext context, RawProtoStreamWriter writer, Object value) throws IOException {
            String string = (String) value;
            BYTE_ARRAY.cast(byte[].class).writeTo(context, writer, (string != null) ? string.getBytes(StandardCharsets.UTF_8) : this.empty);
        }
    },
    BOOLEAN_ARRAY(boolean[].class) {
        @Override
        public boolean[] readFrom(ImmutableSerializationContext context, RawProtoStreamReader reader) throws IOException {
            int size = reader.readUInt32();
            // Calculate number of bytes in BitSet
            int bytes = (size / Byte.SIZE) + ((size % Byte.SIZE) > 0 ? 1 : 0);
            BitSet set = BitSet.valueOf(((RawProtoStreamReaderImpl) reader).getDelegate().readRawBytes(bytes));
            boolean[] values = new boolean[size];
            for (int i = 0; i < size; ++i) {
                values[i] = set.get(i);
            }
            return values;
        }

        @Override
        public void writeTo(ImmutableSerializationContext context, RawProtoStreamWriter writer, Object value) throws IOException {
            boolean[] values = (boolean[]) value;
            int size = values.length;
            // N.B. Write the length of the array, which corresponds to the number of used bits.
            writer.writeUInt32NoTag(size);
            // Pack boolean values into BitSet
            BitSet set = new BitSet(size);
            for (int i = 0; i < size; ++i) {
                set.set(i, values[i]);
            }
            byte[] bytes = set.toByteArray();
            writer.writeRawBytes(bytes, 0, bytes.length);
        }

        @Override
        public OptionalInt size(ImmutableSerializationContext context, Object value) {
            boolean[] values = (boolean[]) value;
            int size = values.length;
            // Calculate number of bytes in BitSet
            int bytes = (size / Byte.SIZE) + ((size % Byte.SIZE) > 0 ? 1 : 0);
            return OptionalInt.of(CodedOutputStream.computeUInt32SizeNoTag(size) + bytes);
        }
    },
    BYTE_ARRAY(byte[].class) {
        @Override
        public byte[] readFrom(ImmutableSerializationContext context, RawProtoStreamReader reader) throws IOException {
            return reader.readByteArray();
        }

        @Override
        public void writeTo(ImmutableSerializationContext context, RawProtoStreamWriter writer, Object value) throws IOException {
            byte[] bytes = (byte[]) value;
            writer.writeUInt32NoTag(bytes.length);
            writer.writeRawBytes(bytes, 0, bytes.length);
        }
    },
    SHORT_ARRAY(short[].class) {
        private final ProtoStreamMarshaller<Object> marshaller = new ArrayMarshaller(new ValueMarshaller<>(Short.TYPE), PrimitiveMarshaller.SHORT);

        @Override
        public ProtoStreamMarshaller<?> getMarshaller() {
            return this.marshaller;
        }
    },
    INTEGER_ARRAY(int[].class) {
        private final ProtoStreamMarshaller<Object> marshaller = new ArrayMarshaller(new ValueMarshaller<>(Integer.TYPE), PrimitiveMarshaller.INTEGER);

        @Override
        public ProtoStreamMarshaller<?> getMarshaller() {
            return this.marshaller;
        }
    },
    LONG_ARRAY(long[].class) {
        private final ProtoStreamMarshaller<Object> marshaller = new ArrayMarshaller(new ValueMarshaller<>(Long.TYPE), PrimitiveMarshaller.LONG);

        @Override
        public ProtoStreamMarshaller<?> getMarshaller() {
            return this.marshaller;
        }
    },
    FLOAT_ARRAY(float[].class) {
        private final ProtoStreamMarshaller<Object> marshaller = new ArrayMarshaller(new ValueMarshaller<>(Float.TYPE), PrimitiveMarshaller.FLOAT);

        @Override
        public ProtoStreamMarshaller<?> getMarshaller() {
            return this.marshaller;
        }
    },
    DOUBLE_ARRAY(double[].class) {
        private final ProtoStreamMarshaller<Object> marshaller = new ArrayMarshaller(new ValueMarshaller<>(Double.TYPE), PrimitiveMarshaller.DOUBLE);

        @Override
        public ProtoStreamMarshaller<?> getMarshaller() {
            return this.marshaller;
        }
    },
    CHAR_ARRAY(char[].class) {
        private final ProtoStreamMarshaller<Object> marshaller = new ArrayMarshaller(new ValueMarshaller<>(Character.TYPE), PrimitiveMarshaller.CHARACTER);

        @Override
        public ProtoStreamMarshaller<?> getMarshaller() {
            return this.marshaller;
        }
    },
    REFERENCE(Void.class) {
        @Override
        public Object readFrom(ImmutableSerializationContext context, RawProtoStreamReader reader) throws IOException {
            return reader.readUInt32();
        }

        @Override
        public void writeTo(ImmutableSerializationContext context, RawProtoStreamWriter writer, Object value) throws IOException {
            Integer id = (Integer) value;
            writer.writeUInt32NoTag(id.intValue());
        }

        @Override
        public OptionalInt size(ImmutableSerializationContext context, Object value) {
            Integer id = (Integer) value;
            return OptionalInt.of(CodedOutputStream.computeUInt32SizeNoTag(id));
        }
    },
    IDENTIFIED_OBJECT(Void.class) {
        private final ProtoStreamMarshaller<Object> marshaller = new TypedObjectMarshaller(ClassField.ID);

        @Override
        public ProtoStreamMarshaller<?> getMarshaller() {
            return this.marshaller;
        }
    },
    IDENTIFIED_ENUM(Void.class) {
        @SuppressWarnings({ "unchecked", "rawtypes" })
        private final ProtoStreamMarshaller<Object> marshaller = new TypedEnumMarshaller(ClassField.ID);

        @Override
        public ProtoStreamMarshaller<?> getMarshaller() {
            return this.marshaller;
        }
    },
    IDENTIFIED_ARRAY(Void.class) {
        private final ProtoStreamMarshaller<Object> marshaller = new ArrayMarshaller(ClassField.ID, ObjectMarshaller.INSTANCE);

        @Override
        public ProtoStreamMarshaller<?> getMarshaller() {
            return this.marshaller;
        }
    },
    OBJECT(Void.class) {
        private final ProtoStreamMarshaller<Object> marshaller = new TypedObjectMarshaller(ClassField.ANY);

        @Override
        public ProtoStreamMarshaller<?> getMarshaller() {
            return this.marshaller;
        }
    },
    ENUM(Void.class) {
        @SuppressWarnings({ "rawtypes", "unchecked" })
        private final ProtoStreamMarshaller<Object> marshaller = new TypedEnumMarshaller(ClassField.ANY);

        @Override
        public ProtoStreamMarshaller<?> getMarshaller() {
            return this.marshaller;
        }
    },
    FIELD_ARRAY(Void.class) {
        private final ProtoStreamMarshaller<Object> marshaller = new ArrayMarshaller(ClassField.FIELD, ObjectMarshaller.INSTANCE);

        @Override
        public ProtoStreamMarshaller<?> getMarshaller() {
            return this.marshaller;
        }
    },
    ARRAY(Void.class) {
        private final ProtoStreamMarshaller<Object> marshaller = new ArrayMarshaller(ClassField.ANY, ObjectMarshaller.INSTANCE);

        @Override
        public ProtoStreamMarshaller<?> getMarshaller() {
            return this.marshaller;
        }
    },
    MULTI_DIMENSIONAL_ARRAY(Void.class) {
        private final ProtoStreamMarshaller<Object> marshaller = new ArrayMarshaller(ClassField.ARRAY, ObjectMarshaller.INSTANCE);

        @Override
        public ProtoStreamMarshaller<?> getMarshaller() {
            return this.marshaller;
        }
    },
    PROXY(Void.class) {
        @Override
        public Object readFrom(ImmutableSerializationContext context, RawProtoStreamReader reader) throws IOException {
            InvocationHandler handler = (InvocationHandler) ObjectMarshaller.INSTANCE.readFrom(context, reader);
            Class<?>[] interfaceClasses = new Class<?>[reader.readUInt32()];
            for (int i = 0; i < interfaceClasses.length; ++i) {
                interfaceClasses[i] = ClassField.ANY.readFrom(context, reader);
            }
            return Proxy.newProxyInstance(WildFlySecurityManager.getClassLoaderPrivileged(handler.getClass()), interfaceClasses, handler);
        }

        @Override
        public void writeTo(ImmutableSerializationContext context, RawProtoStreamWriter writer, Object value) throws IOException {
            ObjectMarshaller.INSTANCE.writeTo(context, writer, Proxy.getInvocationHandler(value));
            Class<?>[] interfaceClasses = value.getClass().getInterfaces();
            writer.writeUInt32NoTag(interfaceClasses.length);
            for (Class<?> interfaceClass : interfaceClasses) {
                ClassField.ANY.writeTo(context, writer, interfaceClass);
            }
        }

        @Override
        public OptionalInt size(ImmutableSerializationContext context, Object value) {
            OptionalInt size = ObjectMarshaller.INSTANCE.size(context, Proxy.getInvocationHandler(value));
            if (size.isPresent()) {
                Class<?>[] interfaceClasses = value.getClass().getInterfaces();
                size = OptionalInt.of(size.getAsInt() + CodedOutputStream.computeUInt32SizeNoTag(interfaceClasses.length));
                for (Class<?> interfaceClass : interfaceClasses) {
                    OptionalInt interfaceSize = ClassField.ANY.size(context, interfaceClass);
                    size = size.isPresent() && interfaceSize.isPresent() ? OptionalInt.of(size.getAsInt() + interfaceSize.getAsInt()) : OptionalInt.empty();
                }
            }
            return size;
        }
    },
    ;
    private final Class<?> targetClass;

    AnyField(Class<?> targetClass) {
        this.targetClass = targetClass;
    }

    @Override
    public Class<? extends Object> getJavaClass() {
        return this.targetClass;
    }

    @Override
    public int getIndex() {
        return this.ordinal() + 1;
    }

    @Override
    public ProtoStreamMarshaller<?> getMarshaller() {
        return this;
    }

    private static final AnyField[] VALUES = AnyField.values();

    static AnyField fromIndex(int index) {
        return (index > 0) ? VALUES[index - 1] : null;
    }

    private static final Map<Class<?>, AnyField> FIELDS = new IdentityHashMap<>();
    static {
        for (AnyField field : VALUES) {
            if (field.targetClass != Void.class) {
                FIELDS.put(field.targetClass, field);
            }
        }
    }

    static AnyField fromJavaType(Class<?> targetClass) {
        return FIELDS.get(targetClass);
    }
}