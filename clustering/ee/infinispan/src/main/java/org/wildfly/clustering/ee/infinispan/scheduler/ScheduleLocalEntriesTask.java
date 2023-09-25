/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.ee.infinispan.scheduler;

import java.util.Iterator;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Predicate;
import java.util.stream.Stream;

import org.infinispan.Cache;
import org.infinispan.context.Flag;
import org.wildfly.clustering.ee.Key;
import org.wildfly.clustering.infinispan.distribution.Locality;

/**
 * A task which schedules newly owned entries.
 * @author Paul Ferraro
 * @param <I> identifier type
 * @param <M> the expiration metadata type
 * @param <K> cache key type
 */
public class ScheduleLocalEntriesTask<I, M, K extends Key<I>> implements BiConsumer<Locality, Locality> {
    private final Cache<K, Object> cache;
    private final Predicate<Map.Entry<? super K, ? super Object>> filter;
    private final CacheEntryScheduler<I, M> scheduler;

    public ScheduleLocalEntriesTask(Cache<K, Object> cache, Predicate<Map.Entry<? super K, ? super Object>> filter, CacheEntryScheduler<I, M> scheduler) {
        this.cache = cache;
        this.filter = filter;
        this.scheduler = scheduler;
    }

    @Override
    public void accept(Locality oldLocality, Locality newLocality) {
        // Iterate over local entries, including any cache stores to include entries that may be passivated/invalidated
        try (Stream<Map.Entry<K, Object>> stream = this.cache.getAdvancedCache().withFlags(Flag.CACHE_MODE_LOCAL).entrySet().stream().filter(this.filter)) {
            Iterator<Map.Entry<K, Object>> entries = stream.iterator();
            while (entries.hasNext()) {
                if (Thread.currentThread().isInterrupted()) break;
                K key = entries.next().getKey();
                // If we are the new primary owner of this bean then schedule expiration of this bean locally
                if (!oldLocality.isLocal(key) && newLocality.isLocal(key)) {
                    this.scheduler.schedule(key.getId());
                }
            }
        }
    }
}
