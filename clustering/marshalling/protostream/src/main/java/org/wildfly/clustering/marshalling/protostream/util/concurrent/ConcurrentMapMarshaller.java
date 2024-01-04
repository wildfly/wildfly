/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.marshalling.protostream.util.concurrent;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentMap;
import java.util.function.Supplier;

import org.wildfly.clustering.marshalling.protostream.ProtoStreamMarshaller;
import org.wildfly.clustering.marshalling.protostream.SimpleFunctionalMarshaller;
import org.wildfly.clustering.marshalling.protostream.util.MapMarshaller;
import org.wildfly.common.function.ExceptionFunction;

/**
 * Marshaller for a {@link ConcurrentMap} that does not allow null values.
 * @author Paul Ferraro
 * @param <T> the map type of this marshaller
 */
public class ConcurrentMapMarshaller<T extends ConcurrentMap<Object, Object>> extends SimpleFunctionalMarshaller<T, Map<Object, Object>> {
    private static final ProtoStreamMarshaller<Map<Object, Object>> MARSHALLER = new MapMarshaller<>(HashMap::new);

    @SuppressWarnings("unchecked")
    public ConcurrentMapMarshaller(Supplier<T> factory) {
        super((Class<T>) factory.get().getClass(), MARSHALLER, new ExceptionFunction<Map<Object, Object>, T, IOException>() {
            @Override
            public T apply(Map<Object, Object> map) {
                T result = factory.get();
                result.putAll(map);
                return result;
            }
        });
    }
}
