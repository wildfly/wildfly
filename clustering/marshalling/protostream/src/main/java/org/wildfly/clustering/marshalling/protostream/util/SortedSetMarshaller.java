/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.marshalling.protostream.util;

import java.io.IOException;
import java.util.Comparator;
import java.util.SortedSet;
import java.util.function.Function;

import org.infinispan.protostream.descriptors.WireType;
import org.wildfly.clustering.marshalling.protostream.FieldSetReader;
import org.wildfly.clustering.marshalling.protostream.ProtoStreamReader;
import org.wildfly.clustering.marshalling.protostream.ProtoStreamWriter;

/**
 * Marshaller for a {@link SortedSet}.
 * @author Paul Ferraro
 * @param <T> the set type of this marshaller
 */
public class SortedSetMarshaller<T extends SortedSet<Object>> extends AbstractCollectionMarshaller<T> {

    private static final int COMPARATOR_INDEX = 2;

    private final Function<Comparator<? super Object>, T> factory;

    @SuppressWarnings("unchecked")
    public SortedSetMarshaller(Function<Comparator<? super Object>, T> factory) {
        super((Class<T>) factory.apply((Comparator<Object>) ComparatorMarshaller.INSTANCE.createInitialValue()).getClass());
        this.factory = factory;
    }

    @SuppressWarnings("unchecked")
    @Override
    public T readFrom(ProtoStreamReader reader) throws IOException {
        FieldSetReader<Comparator<?>> comparatorReader = reader.createFieldSetReader(ComparatorMarshaller.INSTANCE, COMPARATOR_INDEX);
        Comparator<Object> comparator = (Comparator<Object>) ComparatorMarshaller.INSTANCE.createInitialValue();
        T set = this.factory.apply(comparator);
        while (!reader.isAtEnd()) {

            int tag = reader.readTag();
            int index = WireType.getTagFieldNumber(tag);
            if (index == ELEMENT_INDEX) {
                set.add(reader.readAny());
            } else if (comparatorReader.contains(index)) {
                T existing = set;
                comparator = (Comparator<Object>) comparatorReader.readField(comparator);
                set = this.factory.apply(comparator);
                set.addAll(existing);
            } else {
                reader.skipField(tag);
            }
        }
        return set;
    }

    @Override
    public void writeTo(ProtoStreamWriter writer, T set) throws IOException {
        super.writeTo(writer, set);
        Comparator<?> comparator = set.comparator();
        if (comparator != ComparatorMarshaller.INSTANCE.createInitialValue()) {
            writer.createFieldSetWriter(ComparatorMarshaller.INSTANCE, COMPARATOR_INDEX).writeFields(comparator);
        }
    }
}
