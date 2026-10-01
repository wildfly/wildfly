/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.faces.mojarra.util;

import java.io.IOException;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Field;
import java.util.AbstractMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.infinispan.protostream.descriptors.WireType;
import org.wildfly.clustering.function.Function;
import org.wildfly.clustering.marshalling.protostream.ProtoStreamReader;
import org.wildfly.clustering.marshalling.protostream.ProtoStreamWriter;
import org.wildfly.clustering.marshalling.protostream.util.AbstractMapMarshaller;

import com.sun.faces.util.LRUMap;

/**
 * @author Paul Ferraro
 */
public class LRUMapMarshaller extends AbstractMapMarshaller<Object, Object, LRUMap<Object, Object>> {

    private static final int MAX_CAPACITY_INDEX = ENTRY_INDEX + 1;
    private static final int DEFAULT_MAX_CAPACITY = 15;
    private static final Function<LRUMap<Object, Object>, Integer> MAX_CAPACITY_HANDLE = Function.invoke(findHandle(LRUMap.class, Integer.TYPE));

    private static MethodHandle findHandle(Class<?> sourceClass, Class<?> fieldClass) {
        for (Field field : sourceClass.getDeclaredFields()) {
            if (field.getType() == fieldClass) {
                try {
                    return MethodHandles.privateLookupIn(sourceClass, MethodHandles.lookup()).findGetter(sourceClass, field.getName(), fieldClass);
                } catch (NoSuchFieldException | IllegalAccessException e) {
                    throw new IllegalStateException(e);
                }
            }
        }
        throw new IllegalArgumentException(fieldClass.getName());
    }

    @SuppressWarnings("unchecked")
    public LRUMapMarshaller() {
        super((Class<LRUMap<Object, Object>>) (Class<?>) LRUMap.class);
    }

    @Override
    public LRUMap<Object, Object> readFrom(ProtoStreamReader reader) throws IOException {
        int maxCapacity = DEFAULT_MAX_CAPACITY;
        List<Map.Entry<Object, Object>> entries = new LinkedList<>();
        while (!reader.isAtEnd()) {
            int tag = reader.readTag();
            switch (WireType.getTagFieldNumber(tag)) {
                case ENTRY_INDEX:
                    entries.add(reader.readObject(AbstractMap.SimpleEntry.class));
                    break;
                case MAX_CAPACITY_INDEX:
                    maxCapacity = reader.readUInt32();
                    break;
                default:
                    reader.skipField(tag);
            }
        }
        LRUMap<Object, Object> map = new LRUMap<>(maxCapacity);
        for (Map.Entry<Object, Object> entry : entries) {
            map.put(entry.getKey(), entry.getValue());
        }
        return map;
    }

    @Override
    public void writeTo(ProtoStreamWriter writer, LRUMap<Object, Object> map) throws IOException {
        super.writeTo(writer, map);
        int maxCapacity = MAX_CAPACITY_HANDLE.apply(map);
        if (maxCapacity != DEFAULT_MAX_CAPACITY) {
            writer.writeUInt32(MAX_CAPACITY_INDEX, maxCapacity);
        }
    }
}
