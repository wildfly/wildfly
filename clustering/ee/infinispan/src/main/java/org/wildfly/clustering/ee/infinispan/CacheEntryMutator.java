/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.ee.infinispan;

import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

import org.infinispan.Cache;
import org.infinispan.context.Flag;
import org.wildfly.clustering.ee.Mutator;

/**
 * Mutates a given cache entry.
 * @author Paul Ferraro
 */
public class CacheEntryMutator<K, V> implements Mutator {

    private final Cache<K, V> cache;
    private final K key;
    private final V value;
    private final AtomicBoolean mutated;

    public CacheEntryMutator(Cache<K, V> cache, Map.Entry<K, V> entry) {
        this(cache, entry.getKey(), entry.getValue());
    }

    public CacheEntryMutator(Cache<K, V> cache, K key, V value) {
        this.cache = cache;
        this.key = key;
        this.value = value;
        this.mutated = cache.getCacheConfiguration().transaction().transactionMode().isTransactional() ? new AtomicBoolean(false) : null;
    }

    @Override
    public void mutate() {
        // We only ever have to perform a replace once within a batch
        if ((this.mutated == null) || this.mutated.compareAndSet(false, true)) {
            // Use FAIL_SILENTLY to prevent mutation from failing locally due to remote exceptions
            this.cache.getAdvancedCache().withFlags(Flag.IGNORE_RETURN_VALUES, Flag.FAIL_SILENTLY).put(this.key, this.value);
        }
    }
}
