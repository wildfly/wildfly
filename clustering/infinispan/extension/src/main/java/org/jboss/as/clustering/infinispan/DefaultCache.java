/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2015, Red Hat, Inc., and individual contributors
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

package org.jboss.as.clustering.infinispan;

import java.util.Arrays;
import java.util.EnumSet;
import java.util.Set;

import org.infinispan.AdvancedCache;
import org.infinispan.cache.impl.AbstractDelegatingAdvancedCache;
import org.infinispan.context.Flag;
import org.infinispan.manager.EmbeddedCacheManager;
import org.wildfly.clustering.ee.Batch;
import org.wildfly.clustering.ee.Batcher;

/**
 * {@link AdvancedCache} decorator associated with a {@link DefaultCacheContainer}.
 * Overrides {@link #startBatch()} and {@link #endBatch(boolean)} methods to use WildFly's {@link Batcher} mechanism, instead of Infinispan's {@link org.infinispan.batch.BatchContainer}.
 * @author Paul Ferraro
 */
public class DefaultCache<K, V> extends AbstractDelegatingAdvancedCache<K, V> {
    // Holds reference to active batch across start/end batch methods
    private static final ThreadLocal<Batch> CURRENT_BATCH = new ThreadLocal<>();

    private final EmbeddedCacheManager manager;
    private final Batcher<? extends Batch> batcher;
    private final Set<Flag> flags;

    DefaultCache(final EmbeddedCacheManager manager, final Batcher<? extends Batch> batcher, final AdvancedCache<K, V> cache, final Set<Flag> flags) {
        super(cache, new AdvancedCacheWrapper<K, V>() {
            @Override
            public AdvancedCache<K, V> wrap(AdvancedCache<K, V> cache) {
                return new DefaultCache<>(manager, batcher, cache, flags);
            }
        });
        this.manager = manager;
        this.batcher = batcher;
        this.flags = flags;
    }

    public DefaultCache(EmbeddedCacheManager manager, BatcherFactory batcherFactory, AdvancedCache<K, V> cache) {
        this(manager, batcherFactory.createBatcher(cache), cache, EnumSet.noneOf(Flag.class));
    }

    @Override
    public EmbeddedCacheManager getCacheManager() {
        return this.manager;
    }

    @Override
    public boolean startBatch() {
        // If cache was not configured with a Batcher then this is a no-op
        if (this.batcher == null) return false;
        // If a batch is already associated with the current thread then don't create a new one
        if (CURRENT_BATCH.get() != null) return false;
        // Associate a new catch with the current thread
        CURRENT_BATCH.set(this.batcher.createBatch());
        return true;
    }

    @Override
    public void endBatch(boolean successful) {
        Batch batch = CURRENT_BATCH.get();
        // If no batch is associated with the current thread then this is a no-op
        if (batch != null) {
            try {
                if (successful) {
                    batch.close();
                } else {
                    batch.discard();
                }
            } finally {
                // Disassociate the batch with the current thread no matter what
                CURRENT_BATCH.remove();
            }
        }
    }

    @Override
    public AdvancedCache<K, V> withFlags(Flag... flags) {
        Set<Flag> set = EnumSet.copyOf(this.flags);
        set.addAll(Arrays.asList(flags));
        return new DefaultCache<>(this.manager, this.batcher, this.cache.withFlags(flags), set);
    }

    @Override
    public boolean equals(Object object) {
        return (object == this) || (object == this.cache);
    }

    @Override
    public int hashCode() {
        return this.cache.hashCode();
    }
}
