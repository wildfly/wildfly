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
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.OptionalInt;

import org.infinispan.protostream.ImmutableSerializationContext;
import org.infinispan.protostream.RawProtoStreamReader;
import org.infinispan.protostream.RawProtoStreamWriter;
import org.infinispan.protostream.impl.WireFormat;
import org.wildfly.clustering.marshalling.protostream.Any;
import org.wildfly.clustering.marshalling.protostream.FieldMarshaller;
import org.wildfly.clustering.marshalling.protostream.Predictable;
import org.wildfly.clustering.marshalling.protostream.ProtoStreamMarshaller;
import org.wildfly.security.manager.WildFlySecurityManager;

/**
 * @author Paul Ferraro
 */
public class EnumMapMarshaller<E extends Enum<E>> implements ProtoStreamMarshaller<EnumMap<E, Object>> {

    private static final int ENUM_SET_INDEX = 1;

    private final FieldMarshaller<EnumSet<E>, EnumSetBuilder<E>> marshaller = new EnumSetFieldMarshaller<>();
    private final int valueIndex = this.marshaller.getFields() + 1;

    @Override
    public EnumMap<E, Object> readFrom(ImmutableSerializationContext context, RawProtoStreamReader reader) throws IOException {
        EnumSetBuilder<E> builder = new EnumSetBuilder<>();
        List<Object> values = new LinkedList<>();
        boolean reading = true;
        while (reading) {
            int tag = reader.readTag();
            int index = WireFormat.getTagFieldNumber(tag);
            if ((index >= ENUM_SET_INDEX) && (index < ENUM_SET_INDEX + this.marshaller.getFields())) {
                builder = this.marshaller.readField(context, reader, index - ENUM_SET_INDEX, builder);
            } else if (index == this.valueIndex) {
                values.add(ProtoStreamMarshaller.read(context, reader.readByteBuffer(), Any.class).get());
            } else {
                reading = (tag != 0) && reader.skipField(tag);
            }
        }
        EnumSet<E> enumSet = builder.build();
        Iterator<E> enumValues = enumSet.iterator();
        EnumMap<E, Object> enumMap = new EnumMap<>(builder.getEnumClass());
        for (Object value : values) {
            enumMap.put(enumValues.next(), value);
        }
        return enumMap;
    }

    @Override
    public void writeTo(ImmutableSerializationContext context, RawProtoStreamWriter writer, EnumMap<E, Object> map) throws IOException {
        this.marshaller.writeFields(context, writer, ENUM_SET_INDEX, this.keys(map));

        for (Object value : map.values()) {
            writer.writeBytes(this.valueIndex, ProtoStreamMarshaller.write(context, new Any(value)));
        }
    }

    @Override
    public OptionalInt size(ImmutableSerializationContext context, EnumMap<E, Object> map) {
        int size = 0;

        OptionalInt setSize = this.marshaller.size(context, ENUM_SET_INDEX, this.keys(map));
        if (setSize.isPresent()) {
            size += setSize.getAsInt();
        } else {
            return setSize;
        }

        for (Object value : map.values()) {
            OptionalInt valueSize = Predictable.computeSize(context, this.valueIndex, new Any(value));
            if (valueSize.isPresent()) {
                size += valueSize.getAsInt();
            } else {
                return valueSize;
            }
        }
        return OptionalInt.of(size);
    }

    @SuppressWarnings("unchecked")
    @Override
    public Class<? extends EnumMap<E, Object>> getJavaClass() {
        return (Class<EnumMap<E, Object>>) (Class<?>) EnumMap.class;
    }

    private EnumSet<E> keys(EnumMap<E, Object> map) {
        EnumSet<E> set = EnumSet.noneOf(this.findEnumClass(map));
        set.addAll(map.keySet());
        return set;
    }

    private Class<E> findEnumClass(EnumMap<E, Object> map) {
        Iterator<E> values = map.keySet().iterator();
        if (values.hasNext()) {
            return values.next().getDeclaringClass();
        }
        // If EnumMap is empty, we need to resort to reflection to obtain the enum type
        return WildFlySecurityManager.doUnchecked(new PrivilegedAction<Class<E>>() {
            @SuppressWarnings("unchecked")
            @Override
            public Class<E> run() {
                try {
                    Field field = EnumMap.class.getDeclaredField("keyType");
                    field.setAccessible(true);
                    return (Class<E>) field.get(map);
                } catch (NoSuchFieldException | IllegalAccessException e) {
                    throw new IllegalStateException(e);
                }
            }
        });
    }
}
