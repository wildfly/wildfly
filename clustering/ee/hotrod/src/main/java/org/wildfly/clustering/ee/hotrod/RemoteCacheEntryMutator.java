/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.ee.hotrod;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

import org.infinispan.client.hotrod.RemoteCache;
import org.wildfly.clustering.ee.Mutator;

/**
 * Mutates a given cache entry.
 * @author Paul Ferraro
 */
public class RemoteCacheEntryMutator<K, V> implements Mutator {

    private final RemoteCache<K, V> cache;
    private final K id;
    private final V value;
    private final Function<V, Duration> maxIdle;

    public RemoteCacheEntryMutator(RemoteCache<K, V> cache, Map.Entry<K, V> entry) {
        this(cache, entry, null);
    }

    public RemoteCacheEntryMutator(RemoteCache<K, V> cache, K id, V value) {
        this(cache, id, value, null);
    }

    public RemoteCacheEntryMutator(RemoteCache<K, V> cache, Map.Entry<K, V> entry, Function<V, Duration> maxIdle) {
        this(cache, entry.getKey(), entry.getValue(), maxIdle);
    }

    public RemoteCacheEntryMutator(RemoteCache<K, V> cache, K id, V value, Function<V, Duration> maxIdle) {
        this.cache = cache;
        this.id = id;
        this.value = value;
        this.maxIdle = maxIdle;
    }

    @Override
    public void mutate() {
        Duration maxIdleDuration = (this.maxIdle != null) ? this.maxIdle.apply(this.value) : Duration.ZERO;
        long seconds = maxIdleDuration.getSeconds();
        int nanos = maxIdleDuration.getNano();
        if (nanos > 0) {
            seconds += 1;
        }
        this.cache.put(this.id, this.value, 0, TimeUnit.SECONDS, seconds, TimeUnit.SECONDS);
    }
}
