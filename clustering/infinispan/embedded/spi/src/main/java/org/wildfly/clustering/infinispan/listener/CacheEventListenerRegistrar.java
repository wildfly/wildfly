/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.infinispan.listener;

import org.infinispan.Cache;
import org.infinispan.notifications.cachelistener.filter.CacheEventFilter;

/**
 * A registering cache event listener.
 * @author Paul Ferraro
 */
public class CacheEventListenerRegistrar<K, V> extends EventListenerRegistrar implements CacheListenerRegistrar<K, V> {

    private final Cache<K, V> cache;
    private final Object listener;

    public CacheEventListenerRegistrar(Cache<K, V> cache) {
        super(cache);
        this.cache = cache;
        this.listener = this;
    }

    public CacheEventListenerRegistrar(Cache<K, V> cache, Object listener) {
        super(cache, listener);
        this.cache = cache;
        this.listener = listener;
    }

    @Override
    public ListenerRegistration register(CacheEventFilter<? super K, ? super V> filter) {
        this.cache.addListener(this.listener, filter, null);
        return () -> this.cache.removeListener(this.listener);
    }
}
