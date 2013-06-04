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
package org.wildfly.clustering.web.infinispan;

import java.util.Collections;

import org.infinispan.Cache;
import org.infinispan.context.Flag;
import org.jboss.as.clustering.infinispan.invoker.CacheInvoker;

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
    class LockingEvictOperation<K, V> implements CacheInvoker.Operation<K, V, Boolean> {
        private final K key;

        public LockingEvictOperation(K key) {
            this.key = key;
        }

        @Override
        public Boolean invoke(Cache<K, V> cache) {
            boolean locked = cache.getAdvancedCache().lock(Collections.singleton(key));
            if (locked) {
                cache.getAdvancedCache().withFlags(Flag.SKIP_LOCKING).evict(this.key);
            }
            return locked;
        }
    }

    /**
     * Reusable eviction operation.
     */
    class EvictOperation<K, V> implements CacheInvoker.Operation<K, V, Void> {
        private final K key;

        public EvictOperation(K key) {
            this.key = key;
        }

        @Override
        public Void invoke(Cache<K, V> cache) {
            cache.getAdvancedCache().withFlags(Flag.SKIP_LOCKING).evict(this.key);
            return null;
        }
    }
}
