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
import org.jboss.modules.Module;
import org.wildfly.security.manager.WildFlySecurityManager;

/**
 * @author Paul Ferraro
 */
public enum AnyField implements ProtoStreamMarshaller<Object>, Field {
    BOOLEAN(Boolean.class) {
        @Override
        public Object readFrom(ImmutableSerializationContext context, RawProtoStreamReader reader) throws IOException {
            return PrimitiveMarshaller.BOOLEAN.readFrom(context, reader);
        }

        @Override
        public void writeTo(ImmutableSerializationContext context, RawProtoStreamWriter writer, Object value) throws IOException {
            PrimitiveMarshaller.BOOLEAN.writeTo(context, writer, value);
        }

        @Override
        public OptionalInt size(ImmutableSerializationContext context, Object value) {
            return PrimitiveMarshaller.BOOLEAN.size(context, value);
        }
    },
    BYTE(Byte.class) {
        @Override
        public Object readFrom(ImmutableSerializationContext context, RawProtoStreamReader reader) throws IOException {
            return PrimitiveMarshaller.BYTE.readFrom(context, reader);
        }

        @Override
        public void writeTo(ImmutableSerializationContext context, RawProtoStreamWriter writer, Object value) throws IOException {
            PrimitiveMarshaller.BYTE.writeTo(context, writer, value);
        }

        @Override
        public OptionalInt size(ImmutableSerializationContext context, Object value) {
            return PrimitiveMarshaller.BYTE.size(context, value);
        }
    },
    SHORT(Short.class) {
        @Override
        public Object readFrom(ImmutableSerializationContext context, RawProtoStreamReader reader) throws IOException {
            return PrimitiveMarshaller.SHORT.readFrom(context, reader);
        }

        @Override
        public void writeTo(ImmutableSerializationContext context, RawProtoStreamWriter writer, Object value) throws IOException {
            PrimitiveMarshaller.SHORT.writeTo(context, writer, value);
        }

        @Override
        public OptionalInt size(ImmutableSerializationContext context, Object value) {
            return PrimitiveMarshaller.SHORT.size(context, value);
        }
    },
    INTEGER(Integer.class) {
        @Override
        public Object readFrom(ImmutableSerializationContext context, RawProtoStreamReader reader) throws IOException {
            return PrimitiveMarshaller.INTEGER.readFrom(context, reader);
        }

        @Override
        public void writeTo(ImmutableSerializationContext context, RawProtoStreamWriter writer, Object value) throws IOException {
            PrimitiveMarshaller.INTEGER.writeTo(context, writer, value);
        }

        @Override
        public OptionalInt size(ImmutableSerializationContext context, Object value) {
            return PrimitiveMarshaller.INTEGER.size(context, value);
        }
    },
    LONG(Long.class) {
        @Override
        public Object readFrom(ImmutableSerializationContext context, RawProtoStreamReader reader) throws IOException {
            return PrimitiveMarshaller.LONG.readFrom(context, reader);
        }

        @Override
        public void writeTo(ImmutableSerializationContext context, RawProtoStreamWriter writer, Object value) throws IOException {
            PrimitiveMarshaller.LONG.writeTo(context, writer, value);
        }

        @Override
        public OptionalInt size(ImmutableSerializationContext context, Object value) {
            return PrimitiveMarshaller.LONG.size(context, value);
        }
    },
    FLOAT(Float.class) {
        @Override
        public Object readFrom(ImmutableSerializationContext context, RawProtoStreamReader reader) throws IOException {
            return PrimitiveMarshaller.FLOAT.readFrom(context, reader);
        }

        @Override
        public void writeTo(ImmutableSerializationContext context, RawProtoStreamWriter writer, Object value) throws IOException {
            PrimitiveMarshaller.FLOAT.writeTo(context, writer, value);
        }

        @Override
        public OptionalInt size(ImmutableSerializationContext context, Object value) {
            return PrimitiveMarshaller.FLOAT.size(context, value);
        }
    },
    DOUBLE(Double.class) {
        @Override
        public Object readFrom(ImmutableSerializationContext context, RawProtoStreamReader reader) throws IOException {
            return PrimitiveMarshaller.DOUBLE.readFrom(context, reader);
        }

        @Override
        public void writeTo(ImmutableSerializationContext context, RawProtoStreamWriter writer, Object value) throws IOException {
            PrimitiveMarshaller.DOUBLE.writeTo(context, writer, value);
        }

        @Override
        public OptionalInt size(ImmutableSerializationContext context, Object value) {
            return PrimitiveMarshaller.DOUBLE.size(context, value);
        }
    },
    CHARACTER(Character.class) {
        @Override
        public Object readFrom(ImmutableSerializationContext context, RawProtoStreamReader reader) throws IOException {
            return PrimitiveMarshaller.CHARACTER.readFrom(context, reader);
        }

        @Override
        public void writeTo(ImmutableSerializationContext context, RawProtoStreamWriter writer, Object value) throws IOException {
            PrimitiveMarshaller.CHARACTER.writeTo(context, writer, value);
        }

        @Override
        public OptionalInt size(ImmutableSerializationContext context, Object value) {
            return PrimitiveMarshaller.CHARACTER.size(context, value);
        }
    },
    STRING(String.class) {
        private final byte[] empty = new byte[0];

        @Override
        public String readFrom(ImmutableSerializationContext context, RawProtoStreamReader reader) throws IOException {
            byte[] bytes = (byte[]) BYTE_ARRAY.readFrom(context, reader);
            return (bytes.length > 0) ? new String(bytes, StandardCharsets.UTF_8) : null;
        }

        @Override
        public void writeTo(ImmutableSerializationContext context, RawProtoStreamWriter writer, Object value) throws IOException {
            String string = (String) value;
            BYTE_ARRAY.writeTo(context, writer, (string != null) ? string.getBytes(StandardCharsets.UTF_8) : this.empty);
        }

        @Override
        public OptionalInt size(ImmutableSerializationContext context, Object value) {
            String string = (String) value;
            return (string != null) ? OptionalInt.of(Predictable.stringSize(string)) : BYTE_ARRAY.size(context, this.empty);
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
            return OptionalInt.of(Predictable.unsignedIntSize(size) + bytes);
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

        @Override
        public OptionalInt size(ImmutableSerializationContext context, Object value) {
            byte[] bytes = (byte[]) value;
            return OptionalInt.of(Predictable.byteArraySize(bytes.length));
        }
    },
    SHORT_ARRAY(short[].class) {
        private final ProtoStreamMarshaller<Object> marshaller = new ArrayMarshaller(new ValueMarshaller<>(Short.TYPE), PrimitiveMarshaller.SHORT);

        @Override
        public Object readFrom(ImmutableSerializationContext context, RawProtoStreamReader reader) throws IOException {
            return this.marshaller.readFrom(context, reader);
        }

        @Override
        public void writeTo(ImmutableSerializationContext context, RawProtoStreamWriter writer, Object value) throws IOException {
            this.marshaller.writeTo(context, writer, value);
        }

        @Override
        public OptionalInt size(ImmutableSerializationContext context, Object value) {
            return this.marshaller.size(context, value);
        }
    },
    INTEGER_ARRAY(int[].class) {
        private final ProtoStreamMarshaller<Object> marshaller = new ArrayMarshaller(new ValueMarshaller<>(Integer.TYPE), PrimitiveMarshaller.INTEGER);

        @Override
        public Object readFrom(ImmutableSerializationContext context, RawProtoStreamReader reader) throws IOException {
            return this.marshaller.readFrom(context, reader);
        }

        @Override
        public void writeTo(ImmutableSerializationContext context, RawProtoStreamWriter writer, Object value) throws IOException {
            this.marshaller.writeTo(context, writer, value);
        }

        @Override
        public OptionalInt size(ImmutableSerializationContext context, Object value) {
            return this.marshaller.size(context, value);
        }
    },
    LONG_ARRAY(long[].class) {
        private final ProtoStreamMarshaller<Object> marshaller = new ArrayMarshaller(new ValueMarshaller<>(Long.TYPE), PrimitiveMarshaller.LONG);

        @Override
        public Object readFrom(ImmutableSerializationContext context, RawProtoStreamReader reader) throws IOException {
            return this.marshaller.readFrom(context, reader);
        }

        @Override
        public void writeTo(ImmutableSerializationContext context, RawProtoStreamWriter writer, Object value) throws IOException {
            this.marshaller.writeTo(context, writer, value);
        }

        @Override
        public OptionalInt size(ImmutableSerializationContext context, Object value) {
            return this.marshaller.size(context, value);
        }
    },
    FLOAT_ARRAY(float[].class) {
        private final ProtoStreamMarshaller<Object> marshaller = new ArrayMarshaller(new ValueMarshaller<>(Float.TYPE), PrimitiveMarshaller.FLOAT);

        @Override
        public Object readFrom(ImmutableSerializationContext context, RawProtoStreamReader reader) throws IOException {
            return this.marshaller.readFrom(context, reader);
        }

        @Override
        public void writeTo(ImmutableSerializationContext context, RawProtoStreamWriter writer, Object value) throws IOException {
            this.marshaller.writeTo(context, writer, value);
        }

        @Override
        public OptionalInt size(ImmutableSerializationContext context, Object value) {
            return this.marshaller.size(context, value);
        }
    },
    DOUBLE_ARRAY(double[].class) {
        private final ProtoStreamMarshaller<Object> marshaller = new ArrayMarshaller(new ValueMarshaller<>(Double.TYPE), PrimitiveMarshaller.DOUBLE);

        @Override
        public Object readFrom(ImmutableSerializationContext context, RawProtoStreamReader reader) throws IOException {
            return this.marshaller.readFrom(context, reader);
        }

        @Override
        public void writeTo(ImmutableSerializationContext context, RawProtoStreamWriter writer, Object value) throws IOException {
            this.marshaller.writeTo(context, writer, value);
        }

        @Override
        public OptionalInt size(ImmutableSerializationContext context, Object value) {
            return this.marshaller.size(context, value);
        }
    },
    CHAR_ARRAY(char[].class) {
        private final ProtoStreamMarshaller<Object> marshaller = new ArrayMarshaller(new ValueMarshaller<>(Character.TYPE), PrimitiveMarshaller.CHARACTER);

        @Override
        public Object readFrom(ImmutableSerializationContext context, RawProtoStreamReader reader) throws IOException {
            return this.marshaller.readFrom(context, reader);
        }

        @Override
        public void writeTo(ImmutableSerializationContext context, RawProtoStreamWriter writer, Object value) throws IOException {
            this.marshaller.writeTo(context, writer, value);
        }

        @Override
        public OptionalInt size(ImmutableSerializationContext context, Object value) {
            return this.marshaller.size(context, value);
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
            return OptionalInt.of(Predictable.unsignedIntSize(id));
        }
    },
    IDENTIFIED_OBJECT(Void.class) {
        private final ProtoStreamMarshaller<Object> marshaller = new TypedObjectMarshaller(ClassMarshaller.ID);

        @Override
        public Object readFrom(ImmutableSerializationContext context, RawProtoStreamReader reader) throws IOException {
            return this.marshaller.readFrom(context, reader);
        }

        @Override
        public void writeTo(ImmutableSerializationContext context, RawProtoStreamWriter writer, Object value) throws IOException {
            this.marshaller.writeTo(context, writer, value);
        }

        @Override
        public OptionalInt size(ImmutableSerializationContext context, Object value) {
            return this.marshaller.size(context, value);
        }
    },
    IDENTIFIED_ENUM(Void.class) {
        @SuppressWarnings({ "unchecked", "rawtypes" })
        private final ProtoStreamMarshaller<Object> marshaller = new TypedEnumMarshaller(ClassMarshaller.ID);

        @Override
        public Object readFrom(ImmutableSerializationContext context, RawProtoStreamReader reader) throws IOException {
            return this.marshaller.readFrom(context, reader);
        }

        @Override
        public void writeTo(ImmutableSerializationContext context, RawProtoStreamWriter writer, Object value) throws IOException {
            this.marshaller.writeTo(context, writer, value);
        }

        @Override
        public OptionalInt size(ImmutableSerializationContext context, Object value) {
            return this.marshaller.size(context, value);
        }
    },
    IDENTIFIED_ARRAY(Void.class) {
        private final ProtoStreamMarshaller<Object> marshaller = new ArrayMarshaller(ClassMarshaller.ID, ObjectMarshaller.INSTANCE);

        @Override
        public Object readFrom(ImmutableSerializationContext context, RawProtoStreamReader reader) throws IOException {
            return this.marshaller.readFrom(context, reader);
        }

        @Override
        public void writeTo(ImmutableSerializationContext context, RawProtoStreamWriter writer, Object value) throws IOException {
            this.marshaller.writeTo(context, writer, value);
        }

        @Override
        public OptionalInt size(ImmutableSerializationContext context, Object value) {
            return this.marshaller.size(context, value);
        }
    },
    OBJECT(Void.class) {
        private final ProtoStreamMarshaller<Object> marshaller = new TypedObjectMarshaller(ClassMarshaller.ANY);

        @Override
        public Object readFrom(ImmutableSerializationContext context, RawProtoStreamReader reader) throws IOException {
            return this.marshaller.readFrom(context, reader);
        }

        @Override
        public void writeTo(ImmutableSerializationContext context, RawProtoStreamWriter writer, Object value) throws IOException {
            this.marshaller.writeTo(context, writer, value);
        }

        @Override
        public OptionalInt size(ImmutableSerializationContext context, Object value) {
            return this.marshaller.size(context, value);
        }
    },
    ENUM(Void.class) {
        @SuppressWarnings({ "rawtypes", "unchecked" })
        private final ProtoStreamMarshaller<Object> marshaller = new TypedEnumMarshaller(ClassMarshaller.ANY);

        @Override
        public Object readFrom(ImmutableSerializationContext context, RawProtoStreamReader reader) throws IOException {
            return this.marshaller.readFrom(context, reader);
        }

        @Override
        public void writeTo(ImmutableSerializationContext context, RawProtoStreamWriter writer, Object value) throws IOException {
            this.marshaller.writeTo(context, writer, value);
        }

        @Override
        public OptionalInt size(ImmutableSerializationContext context, Object value) {
            return this.marshaller.size(context, value);
        }
    },
    FIELD_ARRAY(Void.class) {
        private final ProtoStreamMarshaller<Object> marshaller = new ArrayMarshaller(ClassMarshaller.FIELD, ObjectMarshaller.INSTANCE);

        @Override
        public Object readFrom(ImmutableSerializationContext context, RawProtoStreamReader reader) throws IOException {
            return this.marshaller.readFrom(context, reader);
        }

        @Override
        public void writeTo(ImmutableSerializationContext context, RawProtoStreamWriter writer, Object value) throws IOException {
            this.marshaller.writeTo(context, writer, value);
        }

        @Override
        public OptionalInt size(ImmutableSerializationContext context, Object value) {
            return this.marshaller.size(context, value);
        }
    },
    ARRAY(Void.class) {
        private final ProtoStreamMarshaller<Object> marshaller = new ArrayMarshaller(ClassMarshaller.ANY, ObjectMarshaller.INSTANCE);

        @Override
        public Object readFrom(ImmutableSerializationContext context, RawProtoStreamReader reader) throws IOException {
            return this.marshaller.readFrom(context, reader);
        }

        @Override
        public void writeTo(ImmutableSerializationContext context, RawProtoStreamWriter writer, Object value) throws IOException {
            this.marshaller.writeTo(context, writer, value);
        }

        @Override
        public OptionalInt size(ImmutableSerializationContext context, Object value) {
            return this.marshaller.size(context, value);
        }
    },
    MULTI_DIMENSIONAL_ARRAY(Void.class) {
        private final ProtoStreamMarshaller<Object> marshaller = new ArrayMarshaller(ClassMarshaller.ARRAY, ObjectMarshaller.INSTANCE);

        @Override
        public Object readFrom(ImmutableSerializationContext context, RawProtoStreamReader reader) throws IOException {
            return this.marshaller.readFrom(context, reader);
        }

        @Override
        public void writeTo(ImmutableSerializationContext context, RawProtoStreamWriter writer, Object value) throws IOException {
            this.marshaller.writeTo(context, writer, value);
        }

        @Override
        public OptionalInt size(ImmutableSerializationContext context, Object value) {
            return this.marshaller.size(context, value);
        }
    },
    IDENTIFIED_CLASS(Void.class) {
        @Override
        public Class<?> readFrom(ImmutableSerializationContext context, RawProtoStreamReader reader) throws IOException {
            return ClassMarshaller.ID.readFrom(context, reader);
        }

        @Override
        public void writeTo(ImmutableSerializationContext context, RawProtoStreamWriter writer, Object value) throws IOException {
            ClassMarshaller.ID.writeTo(context, writer, (Class<?>) value);
        }

        @Override
        public OptionalInt size(ImmutableSerializationContext context, Object value) {
            return ClassMarshaller.ID.size(context, (Class<?>) value);
        }
    },
    NAMED_CLASS(Void.class) {
        @Override
        public Class<?> readFrom(ImmutableSerializationContext context, RawProtoStreamReader reader) throws IOException {
            return ClassMarshaller.NAME.readFrom(context, reader);
        }

        @Override
        public void writeTo(ImmutableSerializationContext context, RawProtoStreamWriter writer, Object value) throws IOException {
            ClassMarshaller.NAME.writeTo(context, writer, (Class<?>) value);
        }

        @Override
        public OptionalInt size(ImmutableSerializationContext context, Object value) {
            return ClassMarshaller.NAME.size(context, (Class<?>) value);
        }
    },
    FIELD_CLASS(Void.class) {
        @Override
        public Object readFrom(ImmutableSerializationContext context, RawProtoStreamReader reader) throws IOException {
            return ClassMarshaller.FIELD.readFrom(context, reader);
        }

        @Override
        public void writeTo(ImmutableSerializationContext context, RawProtoStreamWriter writer, Object value) throws IOException {
            Class<?> targetClass = (Class<?>) value;
            ClassMarshaller.FIELD.writeTo(context, writer, targetClass);
        }

        @Override
        public OptionalInt size(ImmutableSerializationContext context, Object value) {
            Class<?> targetClass = (Class<?>) value;
            return ClassMarshaller.FIELD.size(context, targetClass);
        }
    },
    LOADED_CLASS(Void.class) {
        @Override
        public Object readFrom(ImmutableSerializationContext context, RawProtoStreamReader reader) throws IOException {
            return ClassMarshaller.LOADED.readFrom(context, reader);
        }

        @Override
        public void writeTo(ImmutableSerializationContext context, RawProtoStreamWriter writer, Object value) throws IOException {
            ClassMarshaller.LOADED.writeTo(context, writer, (Class<?>) value);
        }

        @Override
        public OptionalInt size(ImmutableSerializationContext context, Object value) {
            return ClassMarshaller.LOADED.size(context, (Class<?>) value);
        }
    },
    ARRAY_CLASS(Void.class) {
        @Override
        public Class<?> readFrom(ImmutableSerializationContext context, RawProtoStreamReader reader) throws IOException {
            return ClassMarshaller.ARRAY.readFrom(context, reader);
        }

        @Override
        public void writeTo(ImmutableSerializationContext context, RawProtoStreamWriter writer, Object value) throws IOException {
            ClassMarshaller.ARRAY.writeTo(context, writer, (Class<?>) value);
        }

        @Override
        public OptionalInt size(ImmutableSerializationContext context, Object value) {
            return ClassMarshaller.ARRAY.size(context, (Class<?>) value);
        }
    },
    OBJECT_CLASS(Void.class) {
        @Override
        public Class<?> readFrom(ImmutableSerializationContext context, RawProtoStreamReader reader) throws IOException {
            return ClassMarshaller.OBJECT.readFrom(context, reader);
        }

        @Override
        public void writeTo(ImmutableSerializationContext context, RawProtoStreamWriter writer, Object value) throws IOException {
            ClassMarshaller.OBJECT.writeTo(context, writer, (Class<?>) value);
        }

        @Override
        public OptionalInt size(ImmutableSerializationContext context, Object value) {
            return ClassMarshaller.OBJECT.size(context, (Class<?>) value);
        }
    },
    PROXY(Void.class) {
        @Override
        public Object readFrom(ImmutableSerializationContext context, RawProtoStreamReader reader) throws IOException {
            Module module = (Module) ObjectMarshaller.INSTANCE.readFrom(context, reader);
            ClassLoader loader = (module != null) ? module.getClassLoader() : WildFlySecurityManager.getCurrentContextClassLoaderPrivileged();
            Class<?>[] interfaceClasses = new Class<?>[reader.readUInt32()];
            for (int i = 0; i < interfaceClasses.length; ++i) {
                interfaceClasses[i] = ClassMarshaller.ANY.readFrom(context, reader);
            }
            InvocationHandler handler = (InvocationHandler) ObjectMarshaller.INSTANCE.readFrom(context, reader);
            return Proxy.newProxyInstance(loader, interfaceClasses, handler);
        }

        @Override
        public void writeTo(ImmutableSerializationContext context, RawProtoStreamWriter writer, Object value) throws IOException {
            ObjectMarshaller.INSTANCE.writeTo(context, writer, Module.forClass(value.getClass()));
            Class<?>[] interfaceClasses = value.getClass().getInterfaces();
            writer.writeUInt32NoTag(interfaceClasses.length);
            for (Class<?> interfaceClass : interfaceClasses) {
                ClassMarshaller.ANY.writeTo(context, writer, interfaceClass);
            }
            ObjectMarshaller.INSTANCE.writeTo(context, writer, Proxy.getInvocationHandler(value));
        }

        @Override
        public OptionalInt size(ImmutableSerializationContext context, Object value) {
            OptionalInt size = ObjectMarshaller.INSTANCE.size(context, Proxy.getInvocationHandler(value));
            if (size.isPresent()) {
                OptionalInt moduleSize = ObjectMarshaller.INSTANCE.size(context, Module.forClass(value.getClass()));
                if (moduleSize.isPresent()) {
                    Class<?>[] interfaceClasses = value.getClass().getInterfaces();
                    size = OptionalInt.of(size.getAsInt() + moduleSize.getAsInt() + Predictable.unsignedIntSize(interfaceClasses.length));
                    for (Class<?> interfaceClass : interfaceClasses) {
                        OptionalInt interfaceSize = ClassMarshaller.ANY.size(context, interfaceClass);
                        size = size.isPresent() && interfaceSize.isPresent() ? OptionalInt.of(size.getAsInt() + interfaceSize.getAsInt()) : OptionalInt.empty();
                    }
                }
            }
            return size;
        }
    },
    THROWABLE(Throwable.class) {
        @Override
        public Object readFrom(ImmutableSerializationContext context, RawProtoStreamReader reader) throws IOException {
            Class<?> targetClass = ClassMarshaller.ANY.readFrom(context, reader);

            return new ExceptionMarshaller<>(targetClass.asSubclass(Throwable.class)).readFrom(context, reader);
        }

        @Override
        public void writeTo(ImmutableSerializationContext context, RawProtoStreamWriter writer, Object value) throws IOException {
            Throwable exception = (Throwable) value;
            @SuppressWarnings("unchecked")
            Class<Throwable> exceptionClass = (Class<Throwable>) exception.getClass();
            ClassMarshaller.ANY.writeTo(context, writer, exceptionClass);

            new ExceptionMarshaller<>(exceptionClass).writeTo(context, writer, exception);
        }

        @Override
        public OptionalInt size(ImmutableSerializationContext context, Object value) {
            Throwable exception = (Throwable) value;
            @SuppressWarnings("unchecked")
            Class<Throwable> exceptionClass = (Class<Throwable>) exception.getClass();
            OptionalInt classSize = ClassMarshaller.ANY.size(context, exceptionClass);
            OptionalInt exceptionSize = new ExceptionMarshaller<>(exceptionClass).size(context, exception);
            return classSize.isPresent() && exceptionSize.isPresent() ? OptionalInt.of(classSize.getAsInt() + exceptionSize.getAsInt()) : OptionalInt.empty();
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