/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.ee.hotrod;

import java.util.function.BiFunction;

import org.infinispan.client.hotrod.RemoteCache;
import org.wildfly.clustering.ee.Mutator;

/**
 * Mutator for a cache entry using a compute function.
 * @author Paul Ferraro
 * @param <K> the cache key type
 * @param <V> the cache value type
 */
public class RemoteCacheEntryComputeMutator<K, V> implements Mutator {

    private final RemoteCache<K, V> cache;
    private final K key;
    private final BiFunction<Object, V, V> function;

    public RemoteCacheEntryComputeMutator(RemoteCache<K, V> cache, K key, BiFunction<Object, V, V> function) {
        this.cache = cache;
        this.key = key;
        this.function = function;
    }

    @Override
    public void mutate() {
        this.cache.compute(this.key, this.function);
    }
}
