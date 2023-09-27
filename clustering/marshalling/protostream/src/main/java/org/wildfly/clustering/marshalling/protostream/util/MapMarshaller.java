/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.marshalling.protostream.util;

import java.io.IOException;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

import org.infinispan.protostream.descriptors.WireType;
import org.wildfly.clustering.marshalling.protostream.ProtoStreamReader;

/**
 * Marshaller for a {@link Map}.
 * @author Paul Ferraro
 * @param <T> the map type of this marshaller
 */
public class MapMarshaller<T extends Map<Object, Object>> extends AbstractMapMarshaller<T> {

    private final Supplier<T> factory;

    @SuppressWarnings("unchecked")
    public MapMarshaller(Supplier<T> factory) {
        super((Class<T>) factory.get().getClass());
        this.factory = factory;
    }

    @Override
    public T readFrom(ProtoStreamReader reader) throws IOException {
        T map = this.factory.get();
        List<Object> keys = new LinkedList<>();
        List<Object> values = new LinkedList<>();
        while (!reader.isAtEnd()) {
            int tag = reader.readTag();
            int index = WireType.getTagFieldNumber(tag);
            switch (index) {
                case KEY_INDEX:
                    keys.add(reader.readAny());
                    break;
                case VALUE_INDEX:
                    values.add(reader.readAny());
                    break;
                default:
                    reader.skipField(tag);
            }
        }
        Iterator<Object> keyIterator = keys.iterator();
        Iterator<Object> valueIterator = values.iterator();
        while (keyIterator.hasNext() || valueIterator.hasNext()) {
            map.put(keyIterator.next(), valueIterator.next());
        }
        return map;
    }
}
