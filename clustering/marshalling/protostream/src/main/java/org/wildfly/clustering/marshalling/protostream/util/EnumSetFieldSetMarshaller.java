/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.marshalling.protostream.util;

import java.io.IOException;
import java.lang.reflect.Field;
import java.security.PrivilegedAction;
import java.util.BitSet;
import java.util.EnumSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import org.infinispan.protostream.descriptors.WireType;
import org.wildfly.clustering.marshalling.protostream.FieldSetMarshaller;
import org.wildfly.clustering.marshalling.protostream.ProtoStreamReader;
import org.wildfly.clustering.marshalling.protostream.ProtoStreamWriter;
import org.wildfly.security.manager.WildFlySecurityManager;

/**
 * Marshaller for the fields of an {@link EnumSet}.
 * @author Paul Ferraro
 * @param <E> the enum type for this marshaller
 */
public class EnumSetFieldSetMarshaller<E extends Enum<E>> implements FieldSetMarshaller.Supplied<EnumSet<E>, EnumSetBuilder<E>> {

    static final Field ENUM_SET_CLASS_FIELD = WildFlySecurityManager.doUnchecked(new PrivilegedAction<Field>() {
        @Override
        public Field run() {
            for (Field field : EnumSet.class.getDeclaredFields()) {
                if (field.getType() == Class.class) {
                    field.setAccessible(true);
                    return field;
                }
            }
            throw new IllegalStateException();
        }
    });

    private static final int CLASS_INDEX = 0;
    private static final int COMPLEMENT_CLASS_INDEX = 1;
    private static final int BITS_INDEX = 2;
    private static final int ELEMENT_INDEX = 3;
    private static final int FIELDS = 4;

    @Override
    public EnumSetBuilder<E> createInitialValue() {
        return new DefaultEnumSetBuilder<>();
    }

    @Override
    public int getFields() {
        return FIELDS;
    }

    @Override
    public EnumSetBuilder<E> readFrom(ProtoStreamReader reader, int index, WireType type, EnumSetBuilder<E> builder) throws IOException {
        switch (index) {
            case CLASS_INDEX:
                return builder.setComplement(false).setEnumClass(reader.readObject(Class.class));
            case COMPLEMENT_CLASS_INDEX:
                return builder.setComplement(true).setEnumClass(reader.readObject(Class.class));
            case BITS_INDEX:
                return builder.setBits(BitSet.valueOf(reader.readByteArray()));
            case ELEMENT_INDEX:
                return builder.add(reader.readUInt32());
            default:
                reader.skipField(type);
                throw new IllegalArgumentException(Integer.toString(index));
        }
    }

    @Override
    public void writeTo(ProtoStreamWriter writer, EnumSet<E> set) throws IOException {
        Class<?> enumClass = this.findEnumClass(set);
        Object[] values = enumClass.getEnumConstants();
        // Marshal the smaller of the set versus the set's complement
        boolean complement = set.size() * 2 > values.length;

        writer.writeObject(complement ? COMPLEMENT_CLASS_INDEX : CLASS_INDEX, enumClass);

        EnumSet<E> targetSet = complement ? EnumSet.complementOf(set) : set;

        // Write as BitSet or individual elements depending on size
        if (((values.length + Byte.SIZE - 1) / Byte.SIZE) < targetSet.size()) {
            BitSet bits = new BitSet(values.length);
            for (int i = 0; i < values.length; ++i) {
                bits.set(i, targetSet.contains(values[i]));
            }
            writer.writeBytes(BITS_INDEX, bits.toByteArray());
        } else {
            for (E value : targetSet) {
                writer.writeUInt32(ELEMENT_INDEX, value.ordinal());
            }
        }
    }

    private Class<?> findEnumClass(EnumSet<E> set) {
        EnumSet<E> nonEmptySet = set.isEmpty() ? EnumSet.complementOf(set) : set;
        Iterator<E> values = nonEmptySet.iterator();
        if (values.hasNext()) {
            return values.next().getDeclaringClass();
        }
        // Java allows enums with no values - thus one could technically create an empty EnumSet for such an enum
        // While this is unlikely, we need to resort to reflection to obtain the enum type
        return WildFlySecurityManager.doUnchecked(new PrivilegedAction<Class<?>>() {
            @Override
            public Class<?> run() {
                try {
                    return (Class<?>) ENUM_SET_CLASS_FIELD.get(set);
                } catch (IllegalAccessException e) {
                    throw new IllegalStateException(e);
                }
            }
        });
    }

    static class DefaultEnumSetBuilder<E extends Enum<E>> implements EnumSetBuilder<E> {

        private final List<Integer> elements = new LinkedList<>();
        private Class<E> enumClass = null;
        private boolean complement = false;
        private BitSet bits = null;

        @Override
        public EnumSetBuilder<E> setEnumClass(Class<E> enumClass) {
            this.enumClass = enumClass;
            return this;
        }

        @Override
        public EnumSetBuilder<E> setComplement(boolean complement) {
            this.complement = complement;
            return this;
        }

        @Override
        public EnumSetBuilder<E> setBits(BitSet bits) {
            this.bits = bits;
            return this;
        }

        @Override
        public EnumSetBuilder<E> add(int ordinal) {
            this.elements.add(ordinal);
            return this;
        }

        @Override
        public Class<E> getEnumClass() {
            return this.enumClass;
        }

        @Override
        public EnumSet<E> get() {
            EnumSet<E> set = EnumSet.noneOf(this.enumClass);
            E[] values = this.enumClass.getEnumConstants();
            if (this.bits != null) {
                for (int i = 0; i < values.length; ++i) {
                    if (this.bits.get(i)) {
                        set.add(values[i]);
                    }
                }
            } else {
                for (Integer element : this.elements) {
                    set.add(values[element]);
                }
            }
            return this.complement ? EnumSet.complementOf(set) : set;
        }
    }
}
