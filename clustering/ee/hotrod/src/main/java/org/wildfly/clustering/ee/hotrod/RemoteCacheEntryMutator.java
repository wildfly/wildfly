/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.ee.hotrod;

import java.time.Duration;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

import org.infinispan.client.hotrod.Flag;
import org.infinispan.client.hotrod.RemoteCache;
import org.wildfly.clustering.ee.Mutator;
import org.wildfly.common.function.Functions;

/**
 * Mutates a given cache entry.
 * @author Paul Ferraro
 */
public class RemoteCacheEntryMutator<K, V> implements Mutator {

    private final RemoteCache<K, V> cache;
    private final Flag[] flags;
    private final K id;
    private final V value;
    private final Supplier<Duration> maxIdle;

    public RemoteCacheEntryMutator(RemoteCache<K, V> cache, Flag[] flags, K id, V value) {
        this(cache, flags, id, value, Functions.constantSupplier(Duration.ZERO));
    }

    public RemoteCacheEntryMutator(RemoteCache<K, V> cache, Flag[] flags, K id, V value, Supplier<Duration> maxIdle) {
        this.cache = cache;
        this.flags = flags;
        this.id = id;
        this.value = value;
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
        this.cache.withFlags(this.flags).put(this.id, this.value, 0, TimeUnit.SECONDS, seconds, TimeUnit.SECONDS);
    }
}
