/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.marshalling.spi.util;

import java.util.Collection;
import java.util.function.Function;

import org.wildfly.clustering.marshalling.spi.ObjectExternalizer;

/**
 * Externalizer for singleton collections.
 * @author Paul Ferraro
 */
public class SingletonCollectionExternalizer<T extends Collection<Object>> extends ObjectExternalizer<T> {
    private static final Function<Collection<Object>, Object> ACCESSOR = new Function<>() {
        @Override
        public Object apply(Collection<Object> collection) {
            return collection.iterator().next();
        }
    };

    @SuppressWarnings("unchecked")
    public SingletonCollectionExternalizer(Function<Object, T> factory) {
        super((Class<T>) factory.apply(null).getClass(), factory, accessor());
    }

    @SuppressWarnings("unchecked")
    public static <T extends Collection<Object>> Function<T, Object> accessor() {
        return (Function<T, Object>) ACCESSOR;
    }
}
