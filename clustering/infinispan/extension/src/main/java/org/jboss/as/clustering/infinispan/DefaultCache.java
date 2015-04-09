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
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import org.infinispan.AdvancedCache;
import org.infinispan.cache.impl.AbstractDelegatingAdvancedCache;
import org.infinispan.context.Flag;
import org.infinispan.manager.EmbeddedCacheManager;
import org.infinispan.metadata.Metadata;
import org.infinispan.notifications.Listener;
import org.wildfly.clustering.ee.Batch;
import org.wildfly.clustering.ee.Batcher;

/**
 * @author Paul Ferraro
 */
public class DefaultCache<K, V> extends AbstractDelegatingAdvancedCache<K, V> {
    private static final ThreadLocal<Batch> CURRENT_BATCH = new ThreadLocal<>();
    private final EmbeddedCacheManager manager;
    private final Batcher<? extends Batch> batcher;
    private final boolean sync;
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
        this.sync = cache.getCacheConfiguration().clustering().cacheMode().isSynchronous();
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
        if (this.batcher == null) return false;
        Batch batch = CURRENT_BATCH.get();
        if (batch != null) return false;
        CURRENT_BATCH.set(this.batcher.createBatch());
        return true;
    }

    @Override
    public void endBatch(boolean successful) {
        Batch batch = CURRENT_BATCH.get();
        if (batch != null) {
            try {
                if (successful) {
                    batch.close();
                } else {
                    batch.discard();
                }
            } finally {
                CURRENT_BATCH.remove();
            }
        }
    }

    // Workaround for https://hibernate.atlassian.net/browse/HHH-9337
    @Override
    public void removeListener(Object listener) {
        if (listener.getClass().isAnnotationPresent(Listener.class)) {
            super.removeListener(listener);
        }
    }

    @Override
    public AdvancedCache<K, V> withFlags(Flag... flags) {
        Set<Flag> set = EnumSet.copyOf(this.flags);
        set.addAll(Arrays.asList(flags));
        return new DefaultCache<>(this.manager, this.batcher, this.cache.withFlags(flags), set);
    }

    @Override
    public void clear() {
        if (this.sync) {
            this.cache.clear();
        } else {
            this.cache.clearAsync();
        }
    }

    @Override
    public V get(Object key) {
        return super.get(key);
    }

    @Override
    public V put(K key, V value) {
        V result = null;
        if (this.sync || !this.flags.contains(Flag.IGNORE_RETURN_VALUES)) {
            result = this.cache.put(key, value);
        } else {
            this.cache.putAsync(key, value);
        }
        return result;
    }

    @Override
    public V put(K key, V value, Metadata metadata) {
        V result = null;
        if (this.sync) {
            result = this.cache.put(key, value, metadata);
        } else {
            this.cache.putAsync(key, value, metadata);
        }
        return result;
    }

    @Override
    public V put(K key, V value, long lifespan, TimeUnit unit) {
        V result = null;
        if (this.sync) {
            result = this.cache.put(key, value, lifespan, unit);
        } else {
            this.cache.putAsync(key, value, lifespan, unit);
        }
        return result;
    }

    @Override
    public V put(K key, V value, long lifespan, TimeUnit lifespanUnit, long maxIdleTime, TimeUnit maxIdleTimeUnit) {
        V result = null;
        if (this.sync) {
            result = this.cache.put(key, value, lifespan, lifespanUnit, maxIdleTime, maxIdleTimeUnit);
        } else {
            this.cache.putAsync(key, value, lifespan, lifespanUnit, maxIdleTime, maxIdleTimeUnit);
        }
        return result;
    }

    @Override
    public void putAll(Map<? extends K, ? extends V> map) {
        if (this.sync) {
            this.cache.putAll(map);
        } else {
            this.cache.putAllAsync(map);
        }
    }

    @Override
    public void putAll(Map<? extends K, ? extends V> map, long lifespan, TimeUnit unit) {
        if (this.sync) {
            this.cache.putAll(map, lifespan, unit);
        } else {
            this.cache.putAllAsync(map, lifespan, unit);
        }
    }

    @Override
    public void putAll(Map<? extends K, ? extends V> map, long lifespan, TimeUnit lifespanUnit, long maxIdleTime, TimeUnit maxIdleTimeUnit) {
        if (this.sync) {
            this.cache.putAll(map, lifespan, lifespanUnit, maxIdleTime, maxIdleTimeUnit);
        } else {
            this.cache.putAllAsync(map, lifespan, lifespanUnit, maxIdleTime, maxIdleTimeUnit);
        }
    }

    @Override
    public V remove(Object key) {
        V result = null;
        if (this.sync) {
            result = this.cache.remove(key);
        } else {
            this.cache.removeAsync(key);
        }
        return result;
    }

    @Override
    public boolean remove(Object key, Object value) {
        boolean result = false;
        if (this.sync) {
            result = this.cache.remove(key, value);
        } else {
            this.cache.removeAsync(key, value);
        }
        return result;
    }

    @Override
    public V replace(K key, V value) {
        V result = null;
        if (this.sync) {
            result = this.cache.replace(key, value);
        } else {
            this.cache.replaceAsync(key, value);
        }
        return result;
    }

    @Override
    public boolean replace(K key, V oldValue, V newValue) {
        boolean result = false;
        if (this.sync) {
            result = this.cache.replace(key, oldValue, newValue);
        } else {
            this.cache.replaceAsync(key, oldValue, newValue);
        }
        return result;
    }

    @Override
    public V replace(K key, V value, long lifespan, TimeUnit unit) {
        V result = null;
        if (this.sync) {
            result = this.cache.replace(key, value, lifespan, unit);
        } else {
            this.cache.replaceAsync(key, value, lifespan, unit);
        }
        return result;
    }

    @Override
    public boolean replace(K key, V oldValue, V newValue, long lifespan, TimeUnit unit) {
        boolean result = false;
        if (this.sync) {
            result = this.cache.replace(key, oldValue, newValue, lifespan, unit);
        } else {
            this.cache.replaceAsync(key, oldValue, newValue, lifespan, unit);
        }
        return result;
    }

    @Override
    public V replace(K key, V value, long lifespan, TimeUnit lifespanUnit, long maxIdleTime, TimeUnit maxIdleTimeUnit) {
        V result = null;
        if (this.sync) {
            result = this.cache.replace(key, value, lifespan, lifespanUnit, maxIdleTime, maxIdleTimeUnit);
        } else {
            this.cache.replaceAsync(key, value, lifespan, lifespanUnit, maxIdleTime, maxIdleTimeUnit);
        }
        return result;
    }

    @Override
    public boolean replace(K key, V oldValue, V newValue, long lifespan, TimeUnit lifespanUnit, long maxIdleTime, TimeUnit maxIdleTimeUnit) {
        boolean result = false;
        if (this.sync) {
            result = this.cache.replace(key, oldValue, newValue, lifespan, lifespanUnit, maxIdleTime, maxIdleTimeUnit);
        } else {
            this.cache.replaceAsync(key, oldValue, newValue, lifespan, lifespanUnit, maxIdleTime, maxIdleTimeUnit);
        }
        return result;
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
