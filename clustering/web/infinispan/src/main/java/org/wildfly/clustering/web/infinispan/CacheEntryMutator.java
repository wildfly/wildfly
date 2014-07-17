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

package org.wildfly.clustering.web.infinispan;

import java.util.EnumSet;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

import org.infinispan.Cache;
import org.infinispan.context.Flag;
import org.jboss.as.clustering.infinispan.invoker.CacheInvoker;
import org.jboss.as.clustering.infinispan.invoker.Mutator;

/**
 * Mutates a given cache entry.
 * @author Paul Ferraro
 */
public class CacheEntryMutator<K, V> implements Mutator {

    private final Cache<K, V> cache;
    private final CacheInvoker invoker;
    private final K id;
    private final V value;
    private final Set<Flag> flags;
    private final AtomicBoolean mutated;

    public CacheEntryMutator(Cache<K, V> cache, CacheInvoker invoker, K id, V value, Flag... flags) {
        this.cache = cache;
        this.invoker = invoker;
        this.id = id;
        this.value = value;
        this.flags = EnumSet.of(Flag.IGNORE_RETURN_VALUES, flags);
        this.mutated = cache.getCacheConfiguration().invocationBatching().enabled() ? new AtomicBoolean(false) : null;
    }

    @Override
    public void mutate() {
        // We only ever have to perform a replace once within a batch
        if ((this.mutated == null) || this.mutated.compareAndSet(false, true)) {
            this.invoker.invoke(this.cache, new MutateOperation<>(this.id, this.value), this.flags.toArray(new Flag[this.flags.size()]));
        }
    }
}
