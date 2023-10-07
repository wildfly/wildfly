/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
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

import org.infinispan.protostream.descriptors.WireType;
import org.wildfly.clustering.marshalling.protostream.FieldSetMarshaller;
import org.wildfly.clustering.marshalling.protostream.FieldSetReader;
import org.wildfly.clustering.marshalling.protostream.ProtoStreamMarshaller;
import org.wildfly.clustering.marshalling.protostream.ProtoStreamReader;
import org.wildfly.clustering.marshalling.protostream.ProtoStreamWriter;
import org.wildfly.security.manager.WildFlySecurityManager;

/**
 * Marshaller for an {@link EnumMap}.
 * @author Paul Ferraro
 * @param <E> the enum key type of this marshaller
 */
public class EnumMapMarshaller<E extends Enum<E>> implements ProtoStreamMarshaller<EnumMap<E, Object>> {

    static final Field ENUM_MAP_KEY_CLASS_FIELD = WildFlySecurityManager.doUnchecked(new PrivilegedAction<Field>() {
        @Override
        public Field run() {
            for (Field field : EnumMap.class.getDeclaredFields()) {
                if (field.getType() == Class.class) {
                    field.setAccessible(true);
                    return field;
                }
            }
            throw new IllegalStateException();
        }
    });

    private static final int ENUM_SET_INDEX = 1;

    private final FieldSetMarshaller<EnumSet<E>, EnumSetBuilder<E>> marshaller = new EnumSetFieldSetMarshaller<>();
    private final int valueIndex = this.marshaller.nextIndex(ENUM_SET_INDEX);

    @Override
    public EnumMap<E, Object> readFrom(ProtoStreamReader reader) throws IOException {
        FieldSetReader<EnumSetBuilder<E>> enumReader = reader.createFieldSetReader(this.marshaller, ENUM_SET_INDEX);
        EnumSetBuilder<E> builder = this.marshaller.createInitialValue();
        List<Object> values = new LinkedList<>();
        while (!reader.isAtEnd()) {
            int tag = reader.readTag();
            int index = WireType.getTagFieldNumber(tag);
            if (enumReader.contains(index)) {
                builder = enumReader.readField(builder);
            } else if (index == this.valueIndex) {
                values.add(reader.readAny());
            } else {
                reader.skipField(tag);
            }
        }
        EnumSet<E> enumSet = builder.get();
        Iterator<E> enumValues = enumSet.iterator();
        EnumMap<E, Object> enumMap = new EnumMap<>(builder.getEnumClass());
        for (Object value : values) {
            enumMap.put(enumValues.next(), value);
        }
        return enumMap;
    }

    @Override
    public void writeTo(ProtoStreamWriter writer, EnumMap<E, Object> map) throws IOException {
        EnumSet<E> set = EnumSet.noneOf(this.findEnumClass(map));
        set.addAll(map.keySet());
        writer.createFieldSetWriter(this.marshaller, ENUM_SET_INDEX).writeFields(set);

        for (Object value : map.values()) {
            writer.writeAny(this.valueIndex, value);
        }
    }

    @SuppressWarnings("unchecked")
    @Override
    public Class<? extends EnumMap<E, Object>> getJavaClass() {
        return (Class<EnumMap<E, Object>>) (Class<?>) EnumMap.class;
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
                    return (Class<E>) ENUM_MAP_KEY_CLASS_FIELD.get(map);
                } catch (IllegalAccessException e) {
                    throw new IllegalStateException(e);
                }
            }
        });
    }
}
