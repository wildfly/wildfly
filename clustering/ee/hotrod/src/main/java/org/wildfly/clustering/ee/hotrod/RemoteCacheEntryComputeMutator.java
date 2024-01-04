/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.ee.hotrod;

import java.time.Duration;
import java.util.concurrent.TimeUnit;
import java.util.function.BiFunction;
import java.util.function.Supplier;

import org.infinispan.client.hotrod.Flag;
import org.infinispan.client.hotrod.RemoteCache;
import org.wildfly.clustering.ee.Mutator;
import org.wildfly.common.function.Functions;

/**
 * Mutator for a cache entry using a compute function.
 * @author Paul Ferraro
 * @param <K> the cache key type
 * @param <V> the cache value type
 */
public class RemoteCacheEntryComputeMutator<K, V> implements Mutator {

    private final RemoteCache<K, V> cache;
    private final Flag[] flags;
    private final K key;
    private final BiFunction<Object, V, V> function;
    private final Supplier<Duration> maxIdle;

    public RemoteCacheEntryComputeMutator(RemoteCache<K, V> cache, Flag[] flags, K key, BiFunction<Object, V, V> function) {
        this(cache, flags, key, function, Functions.constantSupplier(Duration.ZERO));
    }

    public RemoteCacheEntryComputeMutator(RemoteCache<K, V> cache, Flag[] flags, K key, BiFunction<Object, V, V> function, Supplier<Duration> maxIdle) {
        this.cache = cache;
        this.flags = flags;
        this.key = key;
        this.function = function;
        this.maxIdle = maxIdle;
    }

    @Override
    public void mutate() {
        Duration maxIdleDuration = this.maxIdle.get();
        long seconds = maxIdleDuration.getSeconds();
        int nanos = maxIdleDuration.getNano();
        if (nanos > 0) {
            seconds += 1;
        }
        this.cache.withFlags(this.flags).compute(this.key, this.function, 0, TimeUnit.SECONDS, seconds, TimeUnit.SECONDS);
    }
}
