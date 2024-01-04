/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.marshalling.protostream.util;

import java.io.IOException;
import java.util.Comparator;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.SortedMap;
import java.util.function.Function;

import org.infinispan.protostream.descriptors.WireType;
import org.wildfly.clustering.marshalling.protostream.FieldSetReader;
import org.wildfly.clustering.marshalling.protostream.ProtoStreamReader;
import org.wildfly.clustering.marshalling.protostream.ProtoStreamWriter;

/**
 * Marshaller for a {@link SortedMap}.
 * @author Paul Ferraro
 * @param <T> the map type of this marshaller
 */
public class SortedMapMarshaller<T extends SortedMap<Object, Object>> extends AbstractMapMarshaller<T> {

    private static final int COMPARATOR_INDEX = VALUE_INDEX + 1;

    private final Function<Comparator<? super Object>, T> factory;

    @SuppressWarnings("unchecked")
    public SortedMapMarshaller(Function<Comparator<? super Object>, T> factory) {
        super((Class<T>) factory.apply((Comparator<Object>) ComparatorMarshaller.INSTANCE.createInitialValue()).getClass());
        this.factory = factory;
    }

    @SuppressWarnings("unchecked")
    @Override
    public T readFrom(ProtoStreamReader reader) throws IOException {
        FieldSetReader<Comparator<?>> comparatorReader = reader.createFieldSetReader(ComparatorMarshaller.INSTANCE, COMPARATOR_INDEX);
        Comparator<Object> comparator = (Comparator<Object>) ComparatorMarshaller.INSTANCE.createInitialValue();
        T map = this.factory.apply(comparator);
        List<Object> keys = new LinkedList<>();
        List<Object> values = new LinkedList<>();
        while (!reader.isAtEnd()) {
            int tag = reader.readTag();
            int index = WireType.getTagFieldNumber(tag);
            if (index == KEY_INDEX) {
                keys.add(reader.readAny());
            } else if (index == VALUE_INDEX) {
                values.add(reader.readAny());
            } else if (comparatorReader.contains(index)) {
                comparator = (Comparator<Object>) comparatorReader.readField(comparator);
                map = this.factory.apply(comparator);
            } else {
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
    public void writeTo(ProtoStreamWriter writer, T map) throws IOException {
        super.writeTo(writer, map);
        Comparator<?> comparator = map.comparator();
        if (comparator != ComparatorMarshaller.INSTANCE.createInitialValue()) {
            writer.createFieldSetWriter(ComparatorMarshaller.INSTANCE, COMPARATOR_INDEX).writeFields(comparator);
        }
    }
}
