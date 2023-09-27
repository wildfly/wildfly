/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.marshalling.protostream.util;

import java.io.IOException;
import java.util.AbstractMap.SimpleEntry;
import java.util.Map;
import java.util.function.Function;

import org.infinispan.protostream.descriptors.WireType;
import org.wildfly.clustering.marshalling.protostream.ProtoStreamMarshaller;
import org.wildfly.clustering.marshalling.protostream.ProtoStreamReader;
import org.wildfly.clustering.marshalling.protostream.ProtoStreamWriter;

/**
 * Marshaller for a {@link java.util.Map.Entry}
 * @author Paul Ferraro
 * @param <T> the map entry type of this marshaller
 */
public class MapEntryMarshaller<T extends Map.Entry<Object, Object>> implements ProtoStreamMarshaller<T> {

    private static final int KEY_INDEX = 1;
    private static final int VALUE_INDEX = 2;

    private final Class<? extends T> targetClass;
    private final Function<SimpleEntry<Object, Object>, T> factory;

    @SuppressWarnings("unchecked")
    public MapEntryMarshaller(Function<SimpleEntry<Object, Object>, T> factory) {
        this.targetClass = (Class<T>) factory.apply(new SimpleEntry<>(null, null)).getClass();
        this.factory = factory;
    }

    @Override
    public T readFrom(ProtoStreamReader reader) throws IOException {
        SimpleEntry<Object, Object> entry = new SimpleEntry<>(null, null);
        while (!reader.isAtEnd()) {
            int tag = reader.readTag();
            int index = WireType.getTagFieldNumber(tag);
            switch (index) {
                case KEY_INDEX:
                    Object key = reader.readAny();
                    entry = new SimpleEntry<>(key, entry.getValue());
                    break;
                case VALUE_INDEX:
                    Object value = reader.readAny();
                    entry.setValue(value);
                    break;
                default:
                    reader.skipField(tag);
            }
        }
        return this.factory.apply(entry);
    }

    @Override
    public void writeTo(ProtoStreamWriter writer, T entry) throws IOException {
        Object key = entry.getKey();
        if (key != null) {
            writer.writeAny(KEY_INDEX, key);
        }
        Object value = entry.getValue();
        if (key != null) {
            writer.writeAny(VALUE_INDEX, value);
        }
    }

    @Override
    public Class<? extends T> getJavaClass() {
        return this.targetClass;
    }
}
