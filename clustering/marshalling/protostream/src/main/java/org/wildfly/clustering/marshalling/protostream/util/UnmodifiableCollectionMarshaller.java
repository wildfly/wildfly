/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.marshalling.protostream.util;

import java.io.IOException;
import java.util.Collection;
import java.util.LinkedList;
import java.util.function.Function;

import org.wildfly.clustering.marshalling.protostream.ProtoStreamMarshaller;
import org.wildfly.clustering.marshalling.protostream.SimpleFunctionalMarshaller;
import org.wildfly.common.function.ExceptionFunction;

/**
 * Marshaller for unmodifiable collections created via {@link java.util.List#of()} or {@link java.util.Set#of()} methods.
 * @author Paul Ferraro
 */
public class UnmodifiableCollectionMarshaller<E, T extends Collection<Object>> extends SimpleFunctionalMarshaller<T, Collection<Object>> {

    private static final ProtoStreamMarshaller<Collection<Object>> MARSHALLER = new CollectionMarshaller<>(LinkedList::new);

    public UnmodifiableCollectionMarshaller(Class<T> targetClass, Function<Object[], T> factory) {
        super(targetClass, MARSHALLER, new ExceptionFunction<>() {
            @Override
            public T apply(Collection<Object> collection) throws IOException {
                return factory.apply(collection.toArray());
            }
        });
    }
}
