/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2013, Red Hat, Inc., and individual contributors
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
package org.jboss.as.clustering.infinispan.invoker;

import java.util.Collections;

import org.infinispan.Cache;
import org.infinispan.commons.CacheException;
import org.infinispan.context.Flag;
import org.jboss.as.clustering.infinispan.InfinispanLogger;

/**
 * Evicts a cache entry.
 * @author Paul Ferraro
 */
public interface Evictor<K> {
    /**
     * Evict the specified item from the cache.
     * @param id the item identifier
     */
    void evict(K id);

    /**
     * Reusable eviction operation.
     */
    class EvictOperation<K, V> implements CacheInvoker.Operation<K, V, Boolean> {
        private final K key;
        private final CacheInvoker.Operation<K, V, Boolean> operation;

        public EvictOperation(K key) {
            this.key = key;
            this.operation = new PreLockedEvictOperation<>(key);
        }

        @Override
        public Boolean invoke(Cache<K, V> cache) {
            boolean locked = cache.getAdvancedCache().withFlags(Flag.FAIL_SILENTLY).lock(Collections.singleton(this.key));
            return (locked) ? this.operation.invoke(cache) : locked;
        }
    }

    /**
     * Reusable eviction operation.
     */
    class PreLockedEvictOperation<K, V> implements CacheInvoker.Operation<K, V, Boolean> {
        private final K key;

        public PreLockedEvictOperation(K key) {
            this.key = key;
        }

        @Override
        public Boolean invoke(Cache<K, V> cache) {
            try {
                cache.getAdvancedCache().withFlags(Flag.SKIP_LOCKING).evict(this.key);
                return true;
            } catch (CacheException e) {
                InfinispanLogger.ROOT_LOGGER.debugf(e, "Failed to evict %s from %s.%s cache", this.key, cache.getCacheManager().getCacheManagerConfiguration().globalJmxStatistics().cacheManagerName(), cache.getName());
                return false;
            }
        }
    }
}
