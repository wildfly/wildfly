/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.marshalling.protostream.util;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

import org.wildfly.clustering.marshalling.protostream.ProtoStreamMarshaller;
import org.wildfly.clustering.marshalling.protostream.SimpleFunctionalMarshaller;
import org.wildfly.common.function.ExceptionFunction;

/**
 * Marshaller for unmodifiable maps created via {@link java.util.Map#of()} or {@link java.util.Map#ofEntries()} methods.
 * @author Paul Ferraro
 */
public class UnmodifiableMapMarshaller<T extends Map<Object, Object>> extends SimpleFunctionalMarshaller<T, Map<Object, Object>> {
    private static final ProtoStreamMarshaller<Map<Object, Object>> MARSHALLER = new MapMarshaller<>(HashMap::new);

    public UnmodifiableMapMarshaller(Class<T> targetClass, Function<Map.Entry<? extends Object, ? extends Object>[], T> factory) {
        super(targetClass, MARSHALLER, new ExceptionFunction<Map<Object, Object>, T, IOException>() {
            @Override
            public T apply(Map<Object, Object> map) {
                @SuppressWarnings("unchecked")
                Map.Entry<Object, Object>[] entries = new Map.Entry[0];
                return factory.apply(map.entrySet().toArray(entries));
            }
        });
    }
}
