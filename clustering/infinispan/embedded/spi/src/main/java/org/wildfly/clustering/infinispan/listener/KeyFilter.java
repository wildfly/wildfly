/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.infinispan.listener;

import java.util.function.Predicate;

import org.infinispan.metadata.Metadata;
import org.infinispan.notifications.cachelistener.filter.CacheEventFilter;
import org.infinispan.notifications.cachelistener.filter.EventType;

/**
 * A {@link CacheEventFilter} for filtering events based on the cache key.
 * @author Paul Ferraro
 */
public class KeyFilter<K> implements CacheEventFilter<K, Object> {

    private final Predicate<? super K> predicate;

    public KeyFilter(Class<? super K> keyClass) {
        this(keyClass::isInstance);
    }

    public KeyFilter(Predicate<? super K> predicate) {
        this.predicate = predicate;
    }

    @Override
    public boolean accept(K key, Object oldValue, Metadata oldMetadata, Object newValue, Metadata newMetadata, EventType eventType) {
        return this.predicate.test(key);
    }
}
