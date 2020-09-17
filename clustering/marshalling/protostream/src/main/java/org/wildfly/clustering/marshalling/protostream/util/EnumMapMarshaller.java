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
import java.util.EnumMap;
import java.util.Iterator;
import java.util.OptionalInt;

import org.infinispan.protostream.ImmutableSerializationContext;
import org.infinispan.protostream.RawProtoStreamReader;
import org.infinispan.protostream.RawProtoStreamWriter;
import org.wildfly.clustering.marshalling.protostream.AnyField;
import org.wildfly.clustering.marshalling.protostream.ClassMarshaller;
import org.wildfly.clustering.marshalling.protostream.ObjectMarshaller;
import org.wildfly.clustering.marshalling.protostream.Predictable;
import org.wildfly.clustering.marshalling.protostream.ProtoStreamMarshaller;
import org.wildfly.security.manager.WildFlySecurityManager;

/**
 * @author Paul Ferraro
 */
public class EnumMapMarshaller<E extends Enum<E>> implements ProtoStreamMarshaller<EnumMap<E, Object>> {

    @SuppressWarnings("unchecked")
    @Override
    public EnumMap<E, Object> readFrom(ImmutableSerializationContext context, RawProtoStreamReader reader) throws IOException {
        Class<E> enumClass = (Class<E>) ClassMarshaller.ANY.readFrom(context, reader);
        EnumMap<E, Object> map = new EnumMap<>(enumClass);
        BitSet keys = BitSet.valueOf((byte[]) AnyField.BYTE_ARRAY.readFrom(context, reader));
        E[] enumValues = enumClass.getEnumConstants();
        for (int i = 0; i < enumValues.length; ++i) {
            if (keys.get(i)) {
                map.put(enumValues[i], ObjectMarshaller.INSTANCE.readFrom(context, reader));
            }
        }
        return map;
    }

    @Override
    public void writeTo(ImmutableSerializationContext context, RawProtoStreamWriter writer, EnumMap<E, Object> map) throws IOException {
        Class<?> enumClass = this.findEnumClass(map);
        ClassMarshaller.ANY.writeTo(context, writer, enumClass);
        Object[] enumValues = enumClass.getEnumConstants();
        // Represent EnumMap keys as a BitSet
        BitSet keys = new BitSet(enumValues.length);
        for (int i = 0; i < enumValues.length; ++i) {
            keys.set(i, map.containsKey(enumValues[i]));
        }
        AnyField.BYTE_ARRAY.writeTo(context, writer, keys.toByteArray());
        for (Object value : map.values()) {
            ObjectMarshaller.INSTANCE.writeTo(context, writer, value);
        }
    }

    @Override
    public OptionalInt size(ImmutableSerializationContext context, EnumMap<E, Object> map) {
        Class<?> enumClass = this.findEnumClass(map);
        OptionalInt size = ClassMarshaller.ANY.size(context, enumClass);
        if (size.isPresent()) {
            Object[] enumValues = enumClass.getEnumConstants();
            // Determine number of bytes in BitSet
            int bytes = enumValues.length / Byte.SIZE;
            if (enumValues.length % Byte.SIZE > 0) {
                bytes += 1;
            }
            size = OptionalInt.of(size.getAsInt() + bytes + Predictable.unsignedIntSize(bytes));
            for (Object value : map.values()) {
                OptionalInt valueSize = ObjectMarshaller.INSTANCE.size(context, value);
                size = size.isPresent() && valueSize.isPresent() ? OptionalInt.of(size.getAsInt() + valueSize.getAsInt()) : OptionalInt.empty();
            }
        }
        return size;
    }

    @SuppressWarnings("unchecked")
    @Override
    public Class<? extends EnumMap<E, Object>> getJavaClass() {
        return (Class<EnumMap<E, Object>>) (Class<?>) EnumMap.class;
    }

    private Class<?> findEnumClass(EnumMap<E, Object> map) {
        Iterator<E> values = map.keySet().iterator();
        if (values.hasNext()) {
            return values.next().getDeclaringClass();
        }
        // If EnumMap is empty, we need to resort to reflection to obtain the enum type
        return WildFlySecurityManager.doUnchecked(new PrivilegedAction<Class<?>>() {
            @Override
            public Class<?> run() {
                try {
                    Field field = EnumMap.class.getDeclaredField("keyType");
                    field.setAccessible(true);
                    return (Class<?>) field.get(map);
                } catch (NoSuchFieldException | IllegalAccessException e) {
                    throw new IllegalStateException(e);
                }
            }
        });
    }
}
