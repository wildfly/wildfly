/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.marshalling.protostream.util.concurrent;

import java.io.IOException;
import java.util.Comparator;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentMap;
import java.util.function.Function;

import org.wildfly.clustering.marshalling.protostream.ProtoStreamMarshaller;
import org.wildfly.clustering.marshalling.protostream.SimpleFunctionalMarshaller;
import org.wildfly.clustering.marshalling.protostream.util.SortedMapMarshaller;
import org.wildfly.common.function.ExceptionFunction;

/**
 * Marshaller for a concurrent {@link SortedMap} that does not allow null values.
 * @author Paul Ferraro
 * @param <T> the map type of this marshaller
 */
public class ConcurrentSortedMapMarshaller<T extends ConcurrentMap<Object, Object> & SortedMap<Object, Object>> extends SimpleFunctionalMarshaller<T, SortedMap<Object, Object>> {
    private static final ProtoStreamMarshaller<SortedMap<Object, Object>> MARSHALLER = new SortedMapMarshaller<>(TreeMap::new);

    @SuppressWarnings("unchecked")
    public ConcurrentSortedMapMarshaller(Function<Comparator<? super Object>, T> factory) {
        super((Class<T>) factory.apply((Comparator<Object>) (Comparator<?>) Comparator.naturalOrder()).getClass(), MARSHALLER, new ExceptionFunction<SortedMap<Object, Object>, T, IOException>() {
            @Override
            public T apply(SortedMap<Object, Object> map) {
                T result = factory.apply(map.comparator());
                result.putAll(map);
                return result;
            }
        });
    }
}
