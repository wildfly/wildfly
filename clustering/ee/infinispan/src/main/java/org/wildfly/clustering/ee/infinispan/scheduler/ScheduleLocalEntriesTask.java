/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2020, Red Hat, Inc., and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
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
import org.wildfly.clustering.infinispan.spi.distribution.Locality;

/**
 * A task which schedules newly owned entries.
 * @author Paul Ferraro
 * @param <I> identifier type
 * @param <K> cache key type
 * @param <V> cache value type
 */
public class ScheduleLocalEntriesTask<I, M, K extends Key<I>, V extends M> implements BiConsumer<Locality, Locality> {
    private final Cache<K, V> cache;
    private final Predicate<Map.Entry<? super K, ? super V>> filter;
    private final Scheduler<I, M> scheduler;

    public ScheduleLocalEntriesTask(Cache<K, V> cache, Predicate<Map.Entry<? super K, ? super V>> filter, Scheduler<I, M> scheduler) {
        this.cache = cache;
        this.filter = filter;
        this.scheduler = scheduler;
    }

    @Override
    public void accept(Locality oldLocality, Locality newLocality) {
        // Iterate over local entries, including any cache stores to include entries that may be passivated/invalidated
        try (Stream<Map.Entry<K, V>> stream = this.cache.getAdvancedCache().withFlags(Flag.CACHE_MODE_LOCAL).entrySet().stream().filter(this.filter)) {
            Iterator<Map.Entry<K, V>> entries = stream.iterator();
            while (entries.hasNext()) {
                if (Thread.currentThread().isInterrupted()) break;
                Map.Entry<K, V> entry = entries.next();
                K key = entry.getKey();
                // If we are the new primary owner of this bean then schedule expiration of this bean locally
                if (!oldLocality.isLocal(key) && newLocality.isLocal(key)) {
                    this.scheduler.schedule(key.getId(), entry.getValue());
                }
            }
        }
    }
}
