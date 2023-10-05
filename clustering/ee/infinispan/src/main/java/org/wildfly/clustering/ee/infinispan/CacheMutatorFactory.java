/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.ee.infinispan;

import org.infinispan.Cache;
import org.wildfly.clustering.ee.Mutator;
import org.wildfly.clustering.ee.MutatorFactory;
import org.wildfly.clustering.ee.cache.CacheProperties;

/**
 * Factory for creating {@link Mutator} objects for an Infinispan cache.
 * @author Paul Ferraro
 */
public class CacheMutatorFactory<K, V> implements MutatorFactory<K, V> {

    private final Cache<K, V> cache;
    private final CacheProperties properties;

    public CacheMutatorFactory(Cache<K, V> cache) {
        this(cache, new InfinispanCacheProperties(cache.getCacheConfiguration()));
    }

    public CacheMutatorFactory(Cache<K, V> cache, CacheProperties properties) {
        this.cache = cache;
        this.properties = properties;
    }

    @Override
    public Mutator createMutator(K key, V value) {
        return this.properties.isPersistent() ? new CacheEntryMutator<>(this.cache, key, value) : Mutator.PASSIVE;
    }
}
