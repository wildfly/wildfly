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
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.BitSet;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.OptionalInt;

import org.infinispan.protostream.ImmutableSerializationContext;
import org.infinispan.protostream.RawProtoStreamReader;
import org.infinispan.protostream.RawProtoStreamWriter;
import org.infinispan.protostream.impl.RawProtoStreamReaderImpl;
import org.infinispan.protostream.impl.WireFormat;
import org.jboss.modules.Module;
import org.wildfly.security.manager.WildFlySecurityManager;

import protostream.com.google.protobuf.CodedOutputStream;

/**
 * A set of fields used by {@link AnyMarshaller}.
 * @author Paul Ferraro
 */
public enum AnyField implements ScalarMarshallerProvider, Field<Object> {
    BOOLEAN(PrimitiveMarshaller.BOOLEAN),
    BYTE(PrimitiveMarshaller.BYTE),
    SHORT(PrimitiveMarshaller.SHORT),
    INTEGER(PrimitiveMarshaller.INTEGER),
    LONG(PrimitiveMarshaller.LONG),
    FLOAT(PrimitiveMarshaller.FLOAT),
    DOUBLE(PrimitiveMarshaller.DOUBLE),
    CHARACTER(PrimitiveMarshaller.CHARACTER),
    STRING(new ScalarMarshaller<String>() {
        private final byte[] empty = new byte[0];

        @Override
        public String readFrom(ImmutableSerializationContext context, RawProtoStreamReader reader) throws IOException {
            byte[] bytes = BYTE_ARRAY.cast(byte[].class).readFrom(context, reader);
            return (bytes.length > 0) ? new String(bytes, StandardCharsets.UTF_8) : null;
        }

        @Override
        public void writeTo(ImmutableSerializationContext context, RawProtoStreamWriter writer, String value) throws IOException {
            BYTE_ARRAY.cast(byte[].class).writeTo(context, writer, (value != null) ? value.getBytes(StandardCharsets.UTF_8) : this.empty);
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
    BOOLEAN_ARRAY(new ScalarMarshaller<boolean[]>() {
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
        public void writeTo(ImmutableSerializationContext context, RawProtoStreamWriter writer, boolean[] values) throws IOException {
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
        public OptionalInt size(ImmutableSerializationContext context, boolean[] values) {
            int size = values.length;
            // Calculate number of bytes in BitSet
            int bytes = (size / Byte.SIZE) + ((size % Byte.SIZE) > 0 ? 1 : 0);
            return OptionalInt.of(CodedOutputStream.computeUInt32SizeNoTag(size) + bytes);
        }

        @Override
        public Class<? extends boolean[]> getJavaClass() {
            return boolean[].class;
        }

        @Override
        public int getWireType() {
            return WireFormat.WIRETYPE_LENGTH_DELIMITED;
        }
    }),
    BYTE_ARRAY(new ScalarMarshaller<byte[]>() {
        @Override
        public byte[] readFrom(ImmutableSerializationContext context, RawProtoStreamReader reader) throws IOException {
            return reader.readByteArray();
        }

        @Override
        public void writeTo(ImmutableSerializationContext context, RawProtoStreamWriter writer, byte[] bytes) throws IOException {
            ByteBufferMarshaller.INSTANCE.writeTo(context, writer, ByteBuffer.wrap(bytes));
        }

        @Override
        public Class<? extends byte[]> getJavaClass() {
            return byte[].class;
        }

        @Override
        public int getWireType() {
            return ByteBufferMarshaller.INSTANCE.getWireType();
        }
    }),
    SHORT_ARRAY(new ArrayMarshaller(short[].class, new ScalarValueMarshaller<>(Short.TYPE), PrimitiveMarshaller.SHORT)),
    INTEGER_ARRAY(new ArrayMarshaller(int[].class, new ScalarValueMarshaller<>(Integer.TYPE), PrimitiveMarshaller.INTEGER)),
    LONG_ARRAY(new ArrayMarshaller(long[].class, new ScalarValueMarshaller<>(Long.TYPE), PrimitiveMarshaller.LONG)),
    FLOAT_ARRAY(new ArrayMarshaller(float[].class, new ScalarValueMarshaller<>(Float.TYPE), PrimitiveMarshaller.FLOAT)),
    DOUBLE_ARRAY(new ArrayMarshaller(double[].class, new ScalarValueMarshaller<>(Double.TYPE), PrimitiveMarshaller.DOUBLE)),
    CHAR_ARRAY(new ArrayMarshaller(char[].class, new ScalarValueMarshaller<>(Character.TYPE), PrimitiveMarshaller.CHARACTER)),
    REFERENCE(new ScalarMarshaller<Object>() {
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

        @Override
        public Class<? extends Object> getJavaClass() {
            return Object.class;
        }

        @Override
        public int getWireType() {
            return WireFormat.WIRETYPE_VARINT;
        }
    }),
    IDENTIFIED_OBJECT(new TypedObjectMarshaller(ClassField.ID)),
    IDENTIFIED_ENUM(new TypedEnumMarshaller<>(ClassField.ID)),
    IDENTIFIED_ARRAY(new ArrayMarshaller(ClassField.ID, ObjectMarshaller.INSTANCE)),
    OBJECT(new TypedObjectMarshaller(ClassField.ANY)),
    ENUM(new TypedEnumMarshaller<>(ClassField.ANY)),
    FIELD_ARRAY(new ArrayMarshaller(ClassField.FIELD, ObjectMarshaller.INSTANCE)),
    ARRAY(new ArrayMarshaller(ClassField.ANY, ObjectMarshaller.INSTANCE)),
    MULTI_DIMENSIONAL_ARRAY(new ArrayMarshaller(ClassField.ARRAY, ObjectMarshaller.INSTANCE)),
    PROXY(new ScalarMarshaller<Object>() {
        @Override
        public Object readFrom(ImmutableSerializationContext context, RawProtoStreamReader reader) throws IOException {
            Module module = (Module) ObjectMarshaller.INSTANCE.readFrom(context, reader);
            ClassLoader loader = (module != null) ? module.getClassLoader() : WildFlySecurityManager.getCurrentContextClassLoaderPrivileged();
            Class<?>[] interfaceClasses = new Class<?>[reader.readUInt32()];
            for (int i = 0; i < interfaceClasses.length; ++i) {
                interfaceClasses[i] = ClassField.ANY.readFrom(context, reader);
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
                ClassField.ANY.writeTo(context, writer, interfaceClass);
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
                    size = OptionalInt.of(size.getAsInt() + moduleSize.getAsInt() + CodedOutputStream.computeUInt32SizeNoTag(interfaceClasses.length));
                    for (Class<?> interfaceClass : interfaceClasses) {
                        OptionalInt interfaceSize = ClassField.ANY.size(context, interfaceClass);
                        size = size.isPresent() && interfaceSize.isPresent() ? OptionalInt.of(size.getAsInt() + interfaceSize.getAsInt()) : OptionalInt.empty();
                    }
                }
            }
            return size;
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
    THROWABLE(new ScalarMarshaller<Throwable>() {
        @Override
        public Throwable readFrom(ImmutableSerializationContext context, RawProtoStreamReader reader) throws IOException {
            Class<?> targetClass = ClassField.ANY.readFrom(context, reader);

            return new ExceptionMarshaller<>(targetClass.asSubclass(Throwable.class)).readFrom(context, reader);
        }

        @Override
        public void writeTo(ImmutableSerializationContext context, RawProtoStreamWriter writer, Throwable exception) throws IOException {
            @SuppressWarnings("unchecked")
            Class<Throwable> exceptionClass = (Class<Throwable>) exception.getClass();
            ClassField.ANY.writeTo(context, writer, exceptionClass);

            new ExceptionMarshaller<>(exceptionClass).writeTo(context, writer, exception);
        }

        @Override
        public OptionalInt size(ImmutableSerializationContext context, Throwable exception) {
            @SuppressWarnings("unchecked")
            Class<Throwable> exceptionClass = (Class<Throwable>) exception.getClass();
            OptionalInt classSize = ClassField.ANY.size(context, exceptionClass);
            OptionalInt exceptionSize = new ExceptionMarshaller<>(exceptionClass).size(context, exception);
            return classSize.isPresent() && exceptionSize.isPresent() ? OptionalInt.of(classSize.getAsInt() + exceptionSize.getAsInt()) : OptionalInt.empty();
        }

        @Override
        public Class<? extends Throwable> getJavaClass() {
            return Throwable.class;
        }

        @Override
        public int getWireType() {
            return WireFormat.WIRETYPE_LENGTH_DELIMITED;
        }
    }),
    ;
    private final ScalarMarshaller<?> marshaller;

    AnyField(ScalarMarshaller<?> marshaller) {
        this.marshaller = marshaller;
    }

    @Override
    public int getIndex() {
        return this.ordinal() + 1;
    }

    @Override
    public ScalarMarshaller<?> getMarshaller() {
        return this.marshaller;
    }

    private static final AnyField[] VALUES = AnyField.values();

    static AnyField fromIndex(int index) {
        return (index > 0) ? VALUES[index - 1] : null;
    }

    private static final Map<Class<?>, AnyField> FIELDS = new IdentityHashMap<>();
    static {
        for (AnyField field : VALUES) {
            Class<?> fieldClass = field.getJavaClass();
            if ((fieldClass != Object.class) && (fieldClass != Enum.class)) {
                FIELDS.put(fieldClass, field);
            }
        }
    }

    static AnyField fromJavaType(Class<?> targetClass) {
        return FIELDS.get(targetClass);
    }
}