/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.persistence.jipijapa.hibernate7.service;

import com.github.benmanes.caffeine.cache.Cache;
import org.hibernate.internal.util.cache.InternalCache;

import java.util.function.Function;

/**
 * A specialized implementation of {@link InternalCache} that uses a Caffeine-based cache for
 * improved performance and caching capabilities. Designed for internal use within WildFly to
 * optimize Hibernate's internal caching mechanism.
 *
 * @author Sanne Grinovero <sanne@hibernate.org>
 * @param <K> the type of keys maintained by this cache
 * @param <V> the type of mapped values
 */
final class WildFlyCustomInternalCache<K, V> implements InternalCache<K, V> {

    private final Cache<K, V> cache;

    public WildFlyCustomInternalCache(Cache<K, V> caffeineCache) {
        this.cache = caffeineCache;
    }

    @Override
    public int heldElementsEstimate() {
        return Math.toIntExact(cache.estimatedSize());
    }

    @Override
    public V get(K key) {
        return cache.getIfPresent(key);
    }

    @Override
    public void put(K key, V value) {
        cache.put(key, value);
    }

    @Override
    public void clear() {
        cache.invalidateAll();
        cache.cleanUp();
    }

    @Override
    public V computeIfAbsent(K key, Function<? super K, ? extends V> mappingFunction) {
        return cache.get(key, mappingFunction);
    }

}
