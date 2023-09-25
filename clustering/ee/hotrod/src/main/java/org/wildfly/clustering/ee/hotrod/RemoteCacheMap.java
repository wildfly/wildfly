/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.ee.hotrod;

import java.util.Map;

import org.infinispan.client.hotrod.Flag;
import org.infinispan.client.hotrod.RemoteCache;
import org.infinispan.commons.util.CloseableIteratorCollection;
import org.infinispan.commons.util.CloseableIteratorSet;

/**
 * Map view of a remote cache that forces return values where necessary.
 * @author Paul Ferraro
 */
public class RemoteCacheMap<K, V> implements Map<K, V> {
    private final RemoteCache<K, V> cache;

    public RemoteCacheMap(RemoteCache<K, V> cache) {
        this.cache = cache;
    }

    @Override
    public int size() {
        return this.cache.size();
    }

    @Override
    public boolean isEmpty() {
        return this.cache.isEmpty();
    }

    @Override
    public boolean containsKey(Object key) {
        return this.cache.containsKey(key);
    }

    @Override
    public boolean containsValue(Object value) {
        return this.cache.containsValue(value);
    }

    @Override
    public V get(Object key) {
        return this.cache.get(key);
    }

    @Override
    public V put(K key, V value) {
        return this.cache.withFlags(Flag.FORCE_RETURN_VALUE).put(key, value);
    }

    @Override
    public V remove(Object key) {
        return this.cache.withFlags(Flag.FORCE_RETURN_VALUE).remove(key);
    }

    @Override
    public void putAll(Map<? extends K, ? extends V> map) {
        this.cache.putAll(map);
    }

    @Override
    public void clear() {
        this.cache.clear();
    }

    @Override
    public CloseableIteratorSet<K> keySet() {
        return this.cache.keySet();
    }

    @Override
    public CloseableIteratorCollection<V> values() {
        return this.cache.values();
    }

    @Override
    public CloseableIteratorSet<Entry<K, V>> entrySet() {
        return this.cache.entrySet();
    }
}
