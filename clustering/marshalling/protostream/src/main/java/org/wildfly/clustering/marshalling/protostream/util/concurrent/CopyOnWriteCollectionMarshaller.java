/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.marshalling.protostream.util.concurrent;

import java.io.IOException;
import java.util.Collection;
import java.util.LinkedList;
import java.util.function.Supplier;

import org.wildfly.clustering.marshalling.protostream.ProtoStreamMarshaller;
import org.wildfly.clustering.marshalling.protostream.SimpleFunctionalMarshaller;
import org.wildfly.clustering.marshalling.protostream.util.CollectionMarshaller;
import org.wildfly.common.function.ExceptionFunction;

/**
 * Marshaller for copy-on-write implementations of {@link Collection}.
 * @author Paul Ferraro
 * @param <T> the collection type of this marshaller
 */
public class CopyOnWriteCollectionMarshaller<T extends Collection<Object>> extends SimpleFunctionalMarshaller<T, Collection<Object>> {
    private static final ProtoStreamMarshaller<Collection<Object>> MARSHALLER = new CollectionMarshaller<>(LinkedList::new);

    @SuppressWarnings("unchecked")
    public CopyOnWriteCollectionMarshaller(Supplier<T> factory) {
        super((Class<T>) factory.get().getClass(), MARSHALLER, new ExceptionFunction<Collection<Object>, T, IOException>() {
            @Override
            public T apply(Collection<Object> collection) {
                T result = factory.get();
                result.addAll(collection);
                return result;
            }
        });
    }
}
