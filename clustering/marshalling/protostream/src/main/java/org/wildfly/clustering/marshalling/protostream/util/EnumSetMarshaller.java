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

package org.wildfly.clustering.marshalling.protostream.util;

import java.io.IOException;
import java.lang.reflect.Field;
import java.security.PrivilegedAction;
import java.util.BitSet;
import java.util.EnumSet;
import java.util.Iterator;
import java.util.OptionalInt;

import org.infinispan.protostream.ImmutableSerializationContext;
import org.infinispan.protostream.RawProtoStreamReader;
import org.infinispan.protostream.RawProtoStreamWriter;
import org.wildfly.clustering.marshalling.protostream.ObjectMarshaller;
import org.wildfly.clustering.marshalling.protostream.ProtoStreamMarshaller;
import org.wildfly.security.manager.WildFlySecurityManager;

import protostream.com.google.protobuf.CodedOutputStream;

/**
 * ProtoStream optimized marshaller for an EnumSet
 * @author Paul Ferraro
 */
public class EnumSetMarshaller<E extends Enum<E>> implements ProtoStreamMarshaller<EnumSet<E>> {

    @SuppressWarnings("unchecked")
    @Override
    public EnumSet<E> readFrom(ImmutableSerializationContext context, RawProtoStreamReader reader) throws IOException {
        Class<E> enumClass = (Class<E>) ObjectMarshaller.INSTANCE.readFrom(context, reader);
        EnumSet<E> set = EnumSet.noneOf(enumClass);
        BitSet values = BitSet.valueOf(reader.readByteArray());
        E[] enumValues = enumClass.getEnumConstants();
        for (int i = 0; i < enumValues.length; ++i) {
            if (values.get(i)) {
                set.add(enumValues[i]);
            }
        }
        return set;
    }

    @Override
    public void writeTo(ImmutableSerializationContext context, RawProtoStreamWriter writer, EnumSet<E> set) throws IOException {
        Class<?> enumClass = this.findEnumClass(set);
        ObjectMarshaller.INSTANCE.writeTo(context, writer, enumClass);
        Object[] enumValues = enumClass.getEnumConstants();
        // Represent EnumSet as a BitSet
        BitSet values = new BitSet(enumValues.length);
        for (int i = 0; i < enumValues.length; ++i) {
            values.set(i, set.contains(enumValues[i]));
        }
        byte[] bytes = values.toByteArray();
        writer.writeUInt32NoTag(bytes.length);
        writer.writeRawBytes(bytes, 0, bytes.length);
    }

    @Override
    public OptionalInt size(ImmutableSerializationContext context, EnumSet<E> set) {
        Class<?> enumClass = this.findEnumClass(set);
        OptionalInt size = ObjectMarshaller.INSTANCE.size(context, enumClass);
        Object[] enumValues = enumClass.getEnumConstants();
        // Determine number of bytes in BitSet
        int bytes = enumValues.length / Byte.SIZE;
        if (enumValues.length % Byte.SIZE > 0) {
            bytes += 1;
        }
        return size.isPresent() ? OptionalInt.of(size.getAsInt() + bytes + CodedOutputStream.computeUInt32SizeNoTag(bytes)) : OptionalInt.empty();
    }

    @SuppressWarnings("unchecked")
    @Override
    public Class<? extends EnumSet<E>> getJavaClass() {
        return (Class<EnumSet<E>>) (Class<?>) EnumSet.class;
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
