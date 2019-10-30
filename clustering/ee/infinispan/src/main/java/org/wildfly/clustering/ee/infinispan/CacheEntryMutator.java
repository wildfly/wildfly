/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2014, Red Hat, Inc., and individual contributors
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
    private final K id;
    private final V value;
    private final AtomicBoolean mutated;

    public CacheEntryMutator(Cache<K, V> cache, Map.Entry<K, V> entry) {
        this(cache, entry.getKey(), entry.getValue());
    }

    public CacheEntryMutator(Cache<K, V> cache, K id, V value) {
        this.cache = cache;
        this.id = id;
        this.value = value;
        this.mutated = cache.getCacheConfiguration().transaction().transactionMode().isTransactional() ? new AtomicBoolean(false) : null;
    }

    @Override
    public void mutate() {
        // We only ever have to perform a replace once within a batch
        if ((this.mutated == null) || this.mutated.compareAndSet(false, true)) {
            // Use FAIL_SILENTLY to prevent mutation from failing locally due to remote exceptions
            this.cache.getAdvancedCache().withFlags(Flag.IGNORE_RETURN_VALUES, Flag.FAIL_SILENTLY).put(this.id, this.value);
        }
    }
}
