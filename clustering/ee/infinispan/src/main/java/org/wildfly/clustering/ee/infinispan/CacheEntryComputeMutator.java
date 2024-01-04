/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.ee.infinispan;

import java.util.function.BiFunction;

import org.infinispan.Cache;
import org.infinispan.context.Flag;
import org.wildfly.clustering.ee.Mutator;

/**
 * Mutator for a cache entry using a compute function.
 * @author Paul Ferraro
 * @param <K> the cache key type
 * @param <V> the cache value type
 */
public class CacheEntryComputeMutator<K, V> implements Mutator {

    private final Cache<K, V> cache;
    private final K key;
    private final BiFunction<Object, V, V> function;

    public CacheEntryComputeMutator(Cache<K, V> cache, K key, BiFunction<Object, V, V> function) {
        this.cache = cache;
        this.key = key;
        this.function = function;
    }

    @Override
    public void mutate() {
        // Use FAIL_SILENTLY to prevent mutation from failing locally due to remote exceptions
        this.cache.getAdvancedCache().withFlags(Flag.IGNORE_RETURN_VALUES, Flag.FAIL_SILENTLY).compute(this.key, this.function);
    }
}
