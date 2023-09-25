/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.ee.infinispan.scheduler;

import java.util.Iterator;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.stream.Stream;

import org.infinispan.Cache;
import org.infinispan.context.Flag;
import org.wildfly.clustering.ee.Key;
import org.wildfly.clustering.infinispan.distribution.Locality;

/**
 * A task which schedules newly owned keys.
 * @author Paul Ferraro
 * @param <I> identifier type
 * @param <K> cache key type
 */
public class ScheduleLocalKeysTask<I, K extends Key<I>> implements BiConsumer<Locality, Locality> {
    private final Cache<K, ?> cache;
    private final Predicate<? super K> filter;
    private final Consumer<I> scheduleTask;

    public ScheduleLocalKeysTask(Cache<K, ?> cache, Predicate<? super K> filter, CacheEntryScheduler<I, ?> scheduler) {
        this(cache, filter, scheduler::schedule);
    }

    public ScheduleLocalKeysTask(Cache<K, ?> cache, Predicate<? super K> filter, Consumer<I> scheduleTask) {
        this.cache = cache;
        this.filter = filter;
        this.scheduleTask = scheduleTask;
    }

    @Override
    public void accept(Locality oldLocality, Locality newLocality) {
        // Iterate over local keys, including any cache stores to include entries that may be passivated/invalidated
        try (Stream<K> stream = this.cache.getAdvancedCache().withFlags(Flag.CACHE_MODE_LOCAL).keySet().stream().filter(this.filter)) {
            Iterator<K> keys = stream.iterator();
            while (keys.hasNext()) {
                if (Thread.currentThread().isInterrupted()) break;
                K key = keys.next();
                // If we are the new primary owner of this entry then schedule it locally
                if (!oldLocality.isLocal(key) && newLocality.isLocal(key)) {
                    this.scheduleTask.accept(key.getId());
                }
            }
        }
    }
}