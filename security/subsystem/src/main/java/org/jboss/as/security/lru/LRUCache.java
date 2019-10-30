/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2014 Red Hat, Inc., and individual contributors
 * as indicated by the @author tags.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package org.jboss.as.security.lru;

import java.util.AbstractMap;
import java.util.AbstractSet;
import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicIntegerFieldUpdater;
import java.util.concurrent.atomic.AtomicReferenceFieldUpdater;

/**
 * A non-blocking cache where entries are indexed by a key.
 * <p/>
 * <p>To reduce contention, entry allocation and eviction execute in a sampling
 * fashion (entry hits modulo N). Eviction follows an LRU approach (oldest sampled
 * entries are removed first) when the cache is out of capacity.</p>
 * <p/>
 *
 * @author Jason T. Greene
 */
public class LRUCache<K, V> extends AbstractMap<K, V> implements ConcurrentMap<K, V> {
    private static final int SAMPLE_INTERVAL = 5;

    /**
     * Max active entries that are present in the cache.
     */
    private final int maxEntries;

    private final ConcurrentHashMap<K, CacheEntry<K, V>> cache;
    private final ConcurrentDirectDeque<CacheEntry<K, V>> accessQueue;
    private final RemoveCallback<K, V> removeCallback;

    public LRUCache(int maxEntries) {
        this(maxEntries, null);
    }

    public LRUCache(int maxEntries, RemoveCallback<K, V> removeCallback) {
        this.cache = new ConcurrentHashMap<>();
        this.accessQueue = ConcurrentDirectDeque.newInstance();
        this.maxEntries = maxEntries;
        this.removeCallback = removeCallback;
    }

    public V put(K key, V newValue) {
        return put(key, newValue, false);
    }

    public V put(K key, V newValue, boolean ifAbsent) {
        CacheEntry<K, V> entry = cache.get(key);
        V old = null;
        if (entry == null) {
            entry = new CacheEntry<>(key, newValue);
            CacheEntry<K, V> result = cache.putIfAbsent(key, entry);
            if (result != null) {
                return this.put(key, newValue);
            }

            bumpAccess(entry);
        } else {
            old = entry.getValue();
            if (ifAbsent) {
                return old;
            }
            entry.setValue(newValue);
            if (entry.hit() % SAMPLE_INTERVAL == 0) {
                bumpAccess(entry);
            }
        }

        if (cache.size() > maxEntries) {
            //remove the oldest
            CacheEntry<K, V> oldest = accessQueue.poll();
            if (oldest != entry) {
                this.remove(oldest.key());
            }
        }

        return old;
    }

    public V replace(K key, V newValue) {
        CacheEntry<K, V> cacheEntry = get0(key);
        if (cacheEntry == null) return null;

        bumpAccess(cacheEntry);
        V old = cacheEntry.getValue();
        cacheEntry.setValue(newValue);
        if (removeCallback != null) {
            removeCallback.afterRemove(key, old);
        }
        return old;
    }

    public boolean replace(K key, V oldValue, V newValue) {
        CacheEntry<K, V> cacheEntry = get0(key);
        if (cacheEntry == null || cacheEntry.getValue() != oldValue) {
            return false;
        }


        boolean ret = cacheEntry.setValue(oldValue, newValue);
        if (ret) {
            bumpAccess(cacheEntry);
        }

        if (removeCallback != null) {
            removeCallback.afterRemove(key, oldValue);
        }

        return ret;
    }

    public V get(Object key) {
        CacheEntry<K, V> cacheEntry = get0(key);
        if (cacheEntry == null) return null;

        return cacheEntry.getValue();
    }

    private CacheEntry<K, V> get0(Object key) {
        @SuppressWarnings("SuspiciousMethodCalls")
        CacheEntry<K, V> cacheEntry = cache.get(key);
        if (cacheEntry == null) {
            return null;
        }

        if (cacheEntry.hit() % SAMPLE_INTERVAL == 0) {
            bumpAccess(cacheEntry);
        }
        return cacheEntry;
    }

    private void bumpAccess(CacheEntry<K, V> cacheEntry) {
        Object prevToken = cacheEntry.claimToken();
        if (prevToken == Boolean.FALSE)
            return;

        if (prevToken != null) {
            accessQueue.removeToken(prevToken);
        }

        Object token = null;
        try {
            token = accessQueue.offerLastAndReturnToken(cacheEntry);
        } catch (Throwable t) {
            // In case of disaster (OOME), we need to release the claim, so leave it aas null
        }

        if (!cacheEntry.setToken(token) && token != null) { // Always set if null
            accessQueue.removeToken(token);
        }
    }

    public boolean remove(Object key, Object value) {
        CacheEntry<K, V> toRemove = cache.get(key);
        if (toRemove == null || toRemove.getValue() != value || !cache.remove(key, toRemove)) {
            return false;
        }
        Object old = toRemove.killToken();
        if (old != null) {
            accessQueue.removeToken(old);
        }
        return true;
    }

    public V remove(Object key) {
        CacheEntry<K, V> remove = cache.remove(key);
        if (remove == null) {
            return null;
        }
        Object old = remove.killToken();
        if (old != null) {
            accessQueue.removeToken(old);
        }
        if (removeCallback != null) {
            removeCallback.afterRemove(remove.key(), remove.getValue());
        }
        return remove.getValue();
    }

    public void clear() {
        if (removeCallback == null) {
            cache.clear();
            accessQueue.clear();
        } else {
            for (Iterator<Entry<K, V>> iter =  entrySet().iterator(); iter.hasNext();) {
                iter.next();
                iter.remove();
            }
        }
    }

    public int size() {
        return cache.size();
    }

    @Override
    public Set<Entry<K, V>> entrySet() {
        return new WrappedEntrySet(cache.entrySet());
    }

    @Override
    public V putIfAbsent(K key, V value) {
        return put(key, value, true);
    }

    public static final class CacheEntry<K, V> {
        private static final Object CLAIM_TOKEN = new Object();
        private static final Object TOKEN_AVAILABLE = new Object();
        private static final Object DEAD_TOKEN = new Object();

        private static final AtomicIntegerFieldUpdater<CacheEntry> hitsUpdater = AtomicIntegerFieldUpdater.newUpdater(CacheEntry.class, "hits");
        private static final AtomicReferenceFieldUpdater<CacheEntry, Object> tokenUpdater = AtomicReferenceFieldUpdater.newUpdater(CacheEntry.class, Object.class, "tokenState");
        private static final AtomicReferenceFieldUpdater<CacheEntry, Object> valueUpdater = AtomicReferenceFieldUpdater.newUpdater(CacheEntry.class, Object.class, "value");

        private final K key;
        private volatile V value;
        private volatile int hits = 1;

        @SuppressWarnings("UnusedDeclaration")
        private volatile Object tokenState = TOKEN_AVAILABLE;
        private volatile Object accessToken;

        private CacheEntry(K key, V value) {
            this.key = key;
            this.value = value;
        }

        public V setValue(final V value) {
            V old = this.value;
            this.value = value;
            return old;
        }

        public boolean setValue(final V oldValue, V newValue) {
            return valueUpdater.compareAndSet(this, oldValue, newValue);
        }

        public V getValue() {
            return value;
        }

        public int hit() {
            for (; ; ) {
                int i = hits;

                if (hitsUpdater.weakCompareAndSet(this, i, ++i)) {
                    return i;
                }

            }
        }

        public K key() {
            return key;
        }

        Object claimToken() {
            for (;;) {
                if (tokenState == DEAD_TOKEN) {
                    return Boolean.FALSE;
                }
                if (tokenUpdater.compareAndSet(this, TOKEN_AVAILABLE, CLAIM_TOKEN)) {
                    return accessToken;
                }
            }
        }

        Object killToken() {
            Object old = claimToken();
            tokenState = DEAD_TOKEN;
            return old;
        }

        boolean setToken(Object token) {
            this.accessToken = token;
            this.tokenState = TOKEN_AVAILABLE;
            return true;
        }

        public String toString() {
            return key.toString();
        }
    }

    private class WrappedEntrySet extends AbstractSet<Entry<K, V>> {
        private Set<Entry<K, CacheEntry<K, V>>> set;

        public WrappedEntrySet(Set<Entry<K, CacheEntry<K, V>>> set) {
            this.set = set;
        }

        public Iterator<Entry<K, V>> iterator() {
            return new WrappedIterator(set.iterator());
        }

        @Override
        public int size() {
            return set.size();
        }

        @Override
        public boolean contains(Object o) {
            if (!(o instanceof Entry))
                return false;
            Entry<?,?> e = (Entry<?,?>)o;
            V v = LRUCache.this.get(e.getKey());
            return v != null && v.equals(e.getValue());
        }

        @Override
        public boolean remove(Object o) {
            if (!(o instanceof Entry))
                return false;
            Entry<?,?> e = (Entry<?,?>)o;
            return LRUCache.this.remove(e.getKey()) != null;
        }

        public boolean isEmpty() {
            return LRUCache.this.isEmpty();
        }

        public void clear() {
            LRUCache.this.clear();
        }
    }

    private class WrappedIterator implements Iterator<Entry<K, V>> {
        private final Iterator<Entry<K, CacheEntry<K, V>>> iterator;
        private CacheEntry<K, V> last;

        @Override
        public boolean hasNext() {
            return iterator.hasNext();
        }

        @Override
        public Entry<K, V> next() {
            final Entry<K, CacheEntry<K, V>> next = iterator.next();
            last = next.getValue();

            return new Entry<K, V>() {
                @Override
                public K getKey() {
                    return next.getKey();
                }

                @Override
                public V getValue() {
                    return next.getValue().getValue();
                }

                @Override
                public V setValue(V value) {
                    return next.getValue().setValue(value);
                }
            };
        }

        @Override
        public void remove() {
            if (last == null) {
                throw new IllegalStateException("next() not called");
            }
            LRUCache.this.remove(last.key());
        }

        public WrappedIterator(Iterator<Entry<K, CacheEntry<K, V>>> iterator) {
            this.iterator = iterator;
        }
    }
}
