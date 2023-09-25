/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.infinispan.listener;

import java.util.function.Predicate;

import org.infinispan.notifications.cachelistener.filter.CacheEventFilter;

/**
 * A registering cache listener.
 * @author Paul Ferraro
 */
public interface CacheListenerRegistrar<K, V> extends ListenerRegistrar {

    /**
     * Registers this listener events for cache entries whose key is an instance of the specified class.
     * @param keyClass a key class
     * @return a listener registration
     */
    default ListenerRegistration register(Class<? super K> keyClass) {
        return this.register(new KeyFilter<>(keyClass));
    }

    /**
     * Registers this listener events for cache entries whose key matches the specified predicate.
     * @param keyClass a key class
     * @return a listener registration
     */
    default ListenerRegistration register(Predicate<? super K> keyPredicate) {
        return this.register(new KeyFilter<>(keyPredicate));
    }

    /**
     * Registers this listener events for cache entries that match the specified filter.
     * @param filter a cache event filter
     * @return a listener registration
     */
    ListenerRegistration register(CacheEventFilter<? super K, ? super V> filter);
}
