/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.marshalling.protostream.util;

import java.io.IOException;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;

import org.infinispan.protostream.descriptors.WireType;
import org.wildfly.clustering.marshalling.protostream.ProtoStreamReader;
import org.wildfly.clustering.marshalling.protostream.ProtoStreamWriter;
import org.wildfly.clustering.marshalling.spi.util.LinkedHashMapExternalizer;

/**
 * Marshaller for a {@link LinkedHashMap}.
 * @author Paul Ferraro
 */
public class LinkedHashMapMarshaller extends AbstractMapMarshaller<LinkedHashMap<Object, Object>> {

    private static final int ACCESS_ORDER_INDEX = VALUE_INDEX + 1;

    @SuppressWarnings("unchecked")
    public LinkedHashMapMarshaller() {
        super((Class<LinkedHashMap<Object, Object>>) (Class<?>) LinkedHashMap.class);
    }

    @Override
    public LinkedHashMap<Object, Object> readFrom(ProtoStreamReader reader) throws IOException {
        LinkedHashMap<Object, Object> map = new LinkedHashMap<>(16, 0.75f, false);
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
                case ACCESS_ORDER_INDEX:
                    map = new LinkedHashMap<>(16, 0.75f, reader.readBool());
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

    @Override
    public void writeTo(ProtoStreamWriter writer, LinkedHashMap<Object, Object> map) throws IOException {
        synchronized (map) { // Avoid ConcurrentModificationException
            super.writeTo(writer, map);
            boolean accessOrder = LinkedHashMapExternalizer.ACCESS_ORDER.apply(map);
            if (accessOrder) {
                writer.writeBool(ACCESS_ORDER_INDEX, accessOrder);
            }
        }
    }
}
