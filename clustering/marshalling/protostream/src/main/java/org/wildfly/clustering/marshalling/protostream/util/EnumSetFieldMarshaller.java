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
import java.util.OptionalInt;
import java.util.Set;

import org.infinispan.protostream.ImmutableSerializationContext;
import org.infinispan.protostream.RawProtoStreamReader;
import org.infinispan.protostream.RawProtoStreamWriter;
import org.wildfly.clustering.marshalling.protostream.FieldMarshaller;
import org.wildfly.clustering.marshalling.protostream.Predictable;
import org.wildfly.clustering.marshalling.protostream.ProtoStreamMarshaller;
import org.wildfly.security.manager.WildFlySecurityManager;

import protostream.com.google.protobuf.CodedOutputStream;

/**
 * @author Paul Ferraro
 */
public class EnumSetFieldMarshaller<E extends Enum<E>> implements FieldMarshaller<EnumSet<E>, EnumSetBuilder<E>> {

    private static final int CLASS_INDEX = 0;
    private static final int COMPLEMENT_CLASS_INDEX = 1;
    private static final int BITS_INDEX = 2;
    private static final int FIELDS = 3;

    @Override
    public int getFields() {
        return FIELDS;
    }

    @Override
    public EnumSetBuilder<E> readField(ImmutableSerializationContext context, RawProtoStreamReader reader, int index, EnumSetBuilder<E> builder) throws IOException {
        switch (index) {
            case CLASS_INDEX:
                return builder.setComplement(false).setEnumClass(ProtoStreamMarshaller.read(context, reader.readByteBuffer(), Class.class));
            case COMPLEMENT_CLASS_INDEX:
                return builder.setComplement(true).setEnumClass(ProtoStreamMarshaller.read(context, reader.readByteBuffer(), Class.class));
            case BITS_INDEX:
                return builder.setBits(BitSet.valueOf(reader.readByteArray()));
            default:
                throw new IllegalArgumentException(Integer.toString(index));
        }
    }

    @Override
    public void writeFields(ImmutableSerializationContext context, RawProtoStreamWriter writer, int startIndex, EnumSet<E> value) throws IOException {
        Class<?> enumClass = this.findEnumClass(value);
        Object[] enumValues = enumClass.getEnumConstants();
        boolean complement = value.size() * 2 > enumValues.length;

        // If complement of set is smaller, marshal that instead
        writer.writeBytes(startIndex + (complement ? COMPLEMENT_CLASS_INDEX : CLASS_INDEX), ProtoStreamMarshaller.write(context, enumClass));

        Set<E> set = complement ? EnumSet.complementOf(value) : value;
        if (!set.isEmpty()) {
            // Represent EnumSet as a BitSet
            BitSet values = new BitSet(enumValues.length);
            for (int i = 0; i < enumValues.length; ++i) {
                values.set(i, set.contains(enumValues[i]));
            }
            writer.writeBytes(startIndex + BITS_INDEX, values.toByteArray());
        }
    }

    @Override
    public OptionalInt size(ImmutableSerializationContext context, int startIndex, EnumSet<E> value) {
        int size = 0;
        Class<?> enumClass = this.findEnumClass(value);
        Object[] enumValues = enumClass.getEnumConstants();
        boolean complement = value.size() * 2 > enumValues.length;

        OptionalInt classSize = Predictable.computeSize(context, startIndex + (complement ? COMPLEMENT_CLASS_INDEX : CLASS_INDEX), enumClass);
        if (classSize.isPresent()) {
            size += classSize.getAsInt();
        } else {
            return classSize;
        }

        Set<E> set = complement ? EnumSet.complementOf(value) : value;
        if (!set.isEmpty()) {
            int bytes = (enumValues.length / Byte.SIZE) + ((enumValues.length % Byte.SIZE) > 0 ? 1 : 0);
            size += CodedOutputStream.computeUInt32Size(startIndex + BITS_INDEX, bytes) + bytes;
        }
        return OptionalInt.of(size);
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
}
