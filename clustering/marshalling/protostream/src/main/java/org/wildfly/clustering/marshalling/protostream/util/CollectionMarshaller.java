/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.marshalling.protostream.util;

import java.io.IOException;
import java.util.Collection;
import java.util.function.Supplier;

import org.infinispan.protostream.descriptors.WireType;
import org.wildfly.clustering.marshalling.protostream.ProtoStreamReader;

/**
 * Marshaller for a basic collection.
 * @author Paul Ferraro
 * @param <T> the collection type of this marshaller
 */
public class CollectionMarshaller<T extends Collection<Object>> extends AbstractCollectionMarshaller<T> {

    private final Supplier<T> factory;

    @SuppressWarnings("unchecked")
   public CollectionMarshaller(Supplier<T> factory) {
        super((Class<T>) factory.get().getClass());
        this.factory = factory;
    }

    @Override
    public T readFrom(ProtoStreamReader reader) throws IOException {
        T collection = this.factory.get();
        while (!reader.isAtEnd()) {
            int tag = reader.readTag();
            int index = WireType.getTagFieldNumber(tag);
            switch (index) {
                case ELEMENT_INDEX:
                    collection.add(reader.readAny());
                    break;
                default:
                    reader.skipField(tag);
            }
        }
        return collection;
    }
}
