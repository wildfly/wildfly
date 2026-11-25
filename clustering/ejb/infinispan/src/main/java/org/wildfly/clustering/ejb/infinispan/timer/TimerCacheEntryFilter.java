/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.ejb.infinispan.timer;

import java.util.Map;
import java.util.function.Predicate;

import org.infinispan.util.function.SerializablePredicate;
import org.wildfly.clustering.ejb.cache.timer.TimerMetaDataKey;

/**
 * Cache entry filters for use with cache streams.
 * @author Paul Ferraro
 */
public enum TimerCacheEntryFilter implements SerializablePredicate<Map.Entry<Object, Object>> {
    META_DATA_ENTRY(TimerMetaDataKey.class);

    private final Class<?> keyClass;

    TimerCacheEntryFilter(Class<?> keyClass) {
        this.keyClass = keyClass;
    }

    @Override
    public boolean test(Map.Entry<Object, Object> entry) {
        return this.keyClass.isInstance(entry.getKey());
    }

    @SuppressWarnings("unchecked")
    <K, V> Predicate<Map.Entry<? super K, ? super V>> cast() {
        return (Predicate<Map.Entry<? super K, ? super V>>) (Predicate<?>) this;
    }
}
