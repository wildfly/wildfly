/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.marshalling.protostream.util;

import java.io.IOException;
import java.util.Collection;

import org.wildfly.clustering.marshalling.protostream.ProtoStreamMarshaller;
import org.wildfly.clustering.marshalling.protostream.ProtoStreamWriter;

/**
 * Abstract collection marshaller that writes the elements of the collection.
 * @author Paul Ferraro
 * @param <T> the collection type of this marshaller
 */
public abstract class AbstractCollectionMarshaller<T extends Collection<Object>> implements ProtoStreamMarshaller<T> {

    protected static final int ELEMENT_INDEX = 1;

    private final Class<? extends T> collectionClass;

    public AbstractCollectionMarshaller(Class<? extends T> collectionClass) {
        this.collectionClass = collectionClass;
    }

    @Override
    public void writeTo(ProtoStreamWriter writer, T collection) throws IOException {
        synchronized (collection) { // Avoid ConcurrentModificationException
            for (Object element : collection) {
                writer.writeAny(ELEMENT_INDEX, element);
            }
        }
    }

    @Override
    public Class<? extends T> getJavaClass() {
        return this.collectionClass;
    }
}
