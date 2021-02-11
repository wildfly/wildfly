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

package org.wildfly.clustering.marshalling.protostream.util;

import java.io.IOException;
import java.lang.reflect.Field;
import java.security.PrivilegedAction;
import java.util.BitSet;
import java.util.EnumSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import org.wildfly.clustering.marshalling.protostream.FieldSetMarshaller;
import org.wildfly.clustering.marshalling.protostream.ProtoStreamReader;
import org.wildfly.clustering.marshalling.protostream.ProtoStreamWriter;
import org.wildfly.security.manager.WildFlySecurityManager;

/**
 * Marshaller for the fields of an {@link EnumSet}.
 * @author Paul Ferraro
 * @param <E> the enum type for this marshaller
 */
public class EnumSetFieldSetMarshaller<E extends Enum<E>> implements FieldSetMarshaller<EnumSet<E>, EnumSetBuilder<E>> {

    private static final int CLASS_INDEX = 0;
    private static final int COMPLEMENT_CLASS_INDEX = 1;
    private static final int BITS_INDEX = 2;
    private static final int ELEMENT_INDEX = 3;
    private static final int FIELDS = 4;

    @Override
    public EnumSetBuilder<E> getBuilder() {
        return new DefaultEnumSetBuilder<>();
    }

    @Override
    public int getFields() {
        return FIELDS;
    }

    @Override
    public EnumSetBuilder<E> readField(ProtoStreamReader reader, int index, EnumSetBuilder<E> builder) throws IOException {
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
                throw new IllegalArgumentException(Integer.toString(index));
        }
    }

    @Override
    public void writeFields(ProtoStreamWriter writer, int startIndex, EnumSet<E> set) throws IOException {
        Class<?> enumClass = this.findEnumClass(set);
        Object[] values = enumClass.getEnumConstants();
        // Marshal the smaller of the set versus the set's complement
        boolean complement = set.size() * 2 > values.length;

        writer.writeObject(startIndex  + (complement ? COMPLEMENT_CLASS_INDEX : CLASS_INDEX), enumClass);

        EnumSet<E> targetSet = complement ? EnumSet.complementOf(set) : set;

        // Write as BitSet or individual elements depending on size
        if (((values.length + Byte.SIZE - 1) / Byte.SIZE) < targetSet.size()) {
            BitSet bits = new BitSet(values.length);
            for (int i = 0; i < values.length; ++i) {
                bits.set(i, targetSet.contains(values[i]));
            }
            writer.writeBytes(startIndex + BITS_INDEX, bits.toByteArray());
        } else {
            for (E value : targetSet) {
                writer.writeUInt32(startIndex + ELEMENT_INDEX, value.ordinal());
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
                    Field field = EnumSet.class.getDeclaredField("elementType");
                    field.setAccessible(true);
                    return (Class<?>) field.get(set);
                } catch (NoSuchFieldException | IllegalAccessException e) {
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
        public EnumSet<E> build() {
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
                    set.add(values[element.intValue()]);
                }
            }
            return this.complement ? EnumSet.complementOf(set) : set;
        }
    }
}
