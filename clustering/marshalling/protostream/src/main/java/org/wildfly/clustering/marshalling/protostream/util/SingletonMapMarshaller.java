/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.marshalling.protostream.util;

import java.util.Map;
import java.io.IOException;
import java.util.AbstractMap.SimpleEntry;
import java.util.function.BiFunction;
import java.util.function.Function;

import org.wildfly.clustering.marshalling.protostream.FunctionalMarshaller;
import org.wildfly.common.function.ExceptionFunction;

/**
 * Marshaller for a singleton map.
 * @author Paul Ferraro
 */
public class SingletonMapMarshaller extends FunctionalMarshaller<Map<Object, Object>, SimpleEntry<Object, Object>> {
    private static final ExceptionFunction<Map<Object, Object>, SimpleEntry<Object, Object>, IOException> FUNCTION = new ExceptionFunction<>() {
        @Override
        public SimpleEntry<Object, Object> apply(Map<Object, Object> map) {
            return new SimpleEntry<>(map.entrySet().iterator().next());
        }
    };

    @SuppressWarnings("unchecked")
    public SingletonMapMarshaller(BiFunction<Object, Object, Map<Object, Object>> factory) {
        super((Class<Map<Object, Object>>) factory.apply(null, null).getClass(), new MapEntryMarshaller<>(Function.identity()), FUNCTION, new ExceptionFunction<SimpleEntry<Object, Object>, Map<Object, Object>, IOException>() {
            @Override
            public Map<Object, Object> apply(SimpleEntry<Object, Object> entry) {
                return factory.apply(entry.getKey(), entry.getValue());
            }
        });
    }
}
