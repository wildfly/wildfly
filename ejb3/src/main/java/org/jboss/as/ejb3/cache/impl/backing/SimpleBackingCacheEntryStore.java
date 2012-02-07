/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2006, Red Hat Middleware LLC, and individual contributors
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

package org.jboss.as.ejb3.cache.impl.backing;

import java.io.Serializable;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentSkipListSet;

import org.jboss.as.ejb3.EjbMessages;
import org.jboss.as.ejb3.cache.Cacheable;
import org.jboss.as.ejb3.cache.spi.BackingCacheEntry;
import org.jboss.as.ejb3.cache.spi.BackingCacheEntryStore;
import org.jboss.as.ejb3.cache.spi.BackingCacheEntryStoreConfig;
import org.jboss.as.ejb3.cache.spi.GroupCompatibilityChecker;
import org.jboss.as.ejb3.cache.spi.PersistentObjectStore;
import org.jboss.as.ejb3.cache.spi.impl.AbstractBackingCacheEntryStore;
import org.jboss.as.ejb3.cache.spi.impl.CacheableTimestamp;
import org.jboss.as.ejb3.component.stateful.StatefulTimeoutInfo;
import org.jboss.as.server.ServerEnvironment;
import org.jboss.ejb.client.Affinity;
import org.jboss.ejb.client.NodeAffinity;

/**
 * A {@link BackingCacheEntryStore} that stores in a simple <code>Map</code> and delegates to a provided
 * {@link PersistentObjectStore} for persistence.
 *
 * @author Brian Stansberry
 * @author Paul Ferraro
 */
public class SimpleBackingCacheEntryStore<K extends Serializable, V extends Cacheable<K>, E extends BackingCacheEntry<K, V>> extends AbstractBackingCacheEntryStore<K, V, E> {
    private final PersistentObjectStore<K, E> store;
    private final Map<K, EntryHolder> cache = new ConcurrentHashMap<K, EntryHolder>();


    /**
     * SORTED SETS COMPARE FOR EQUALITY USING Comparable
     * not equals()/hashCode()
     * <p/>
     * This means that when removing from this set you MUST use the timestamp that is stored in {@link #cache}, otherwise
     * nothing will be removed.
     */
    private final SortedSet<CacheableTimestamp<K>> entries = new ConcurrentSkipListSet<CacheableTimestamp<K>>();
    private final ServerEnvironment environment;

    /**
     * Create a new SimpleIntegratedObjectStore.
     */
    public SimpleBackingCacheEntryStore(PersistentObjectStore<K, E> store, ServerEnvironment environment, StatefulTimeoutInfo timeout, BackingCacheEntryStoreConfig config) {
        super(timeout, config);
        this.store = store;
        this.environment = environment;
    }

    @Override
    public Affinity getStrictAffinity() {
        return new NodeAffinity(this.environment.getNodeName());
    }

    @Override
    public Affinity getWeakAffinity(K key) {
        return Affinity.NONE;
    }

    @Override
    public boolean hasAffinity(K key) {
        return true;
    }

    @Override
    public boolean isClustered() {
        return false;
    }

    @Override
    public E get(K key, boolean lock) {
        EntryHolder holder = cache.get(key);
        if (holder == null) {
            E value = store.load(key);
            if (value != null) {
                CacheableTimestamp<K> timestamp = new CacheableTimestamp<K>(value);
                cache.put(key, new EntryHolder(value, timestamp));
                this.entries.add(timestamp);
            }
            return value;
        }
        return holder.value;
    }

    @Override
    public Set<K> insert(E entry) {
        K key = entry.getId();
        if (cache.containsKey(key)) {
            throw EjbMessages.MESSAGES.duplicateCacheEntry(key);
        }
        CacheableTimestamp<K> timestamp = new CacheableTimestamp<K>(entry);
        cache.put(key, new EntryHolder(entry, timestamp));
        this.entries.add(timestamp);
        final Set<K> toPassivate = new HashSet<K>();
        int maxSize = this.getConfig().getMaxSize();
        int thisSize = cache.size();
        if (thisSize > maxSize) {
            int remaining = thisSize - maxSize;
            final Iterator<CacheableTimestamp<K>> iterator = this.entries.iterator();
            while (remaining > 0 && iterator.hasNext()) {
                // Passivate the oldest
                final EntryHolder holder = this.cache.get(iterator.next().getId());
                if (holder != null && !holder.value.getId().equals(timestamp.getId()) && !holder.value.isInUse()) {
                    remaining--;
                    toPassivate.add(holder.value.getId());
                }
            }
        }
        return toPassivate;
    }

    @Override
    public void update(E entry, boolean modified) {
        K key = entry.getId();
        if (!cache.containsKey(key)) {
            throw EjbMessages.MESSAGES.missingCacheEntry(key);
        }
        this.update(entry);
        // Otherwise we do nothing; we already have a ref to the entry
    }

    @Override
    public void passivate(E entry) {
        synchronized (entry) {
            K key = entry.getId();
            store.store(entry);
            EntryHolder holder = cache.remove(key);
            if (holder != null) {
                this.remove(holder.timestamp);
            }
        }
    }

    @Override
    public E remove(K id) {
        E entry = get(id, false);
        if (entry != null) {
            EntryHolder holder = cache.remove(id);
            if (holder != null) {
                this.remove(holder.timestamp);
            }
        }
        return entry;
    }

    private void remove(CacheableTimestamp<K> timestamp) {
        this.entries.remove(timestamp);
    }

    private void update(E entry) {
        CacheableTimestamp<K> timestamp = new CacheableTimestamp<K>(entry);
        final EntryHolder holder = cache.get(entry.getId());
        if (holder != null) {
            this.entries.remove(holder.timestamp);
        }
        cache.put(entry.getId(), new EntryHolder(entry, timestamp));
        this.entries.add(timestamp);
    }

    @Override
    public void start() {
        store.start();
    }

    @Override
    public void stop() {
        store.stop();
    }

    @Override
    public boolean isCompatibleWith(GroupCompatibilityChecker other) {
        if (other instanceof BackingCacheEntryStore) {
            return ((BackingCacheEntryStore<?, ?, ?>) other).isClustered() == false;
        }
        return false;
    }

    private final class EntryHolder {
        private final CacheableTimestamp<K> timestamp;
        private final E value;

        private EntryHolder(final E value, final CacheableTimestamp<K> timestamp) {
            this.value = value;
            this.timestamp = timestamp;
        }
    }
}
