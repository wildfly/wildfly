/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.infinispan.client.near;

import java.util.Iterator;
import java.util.Map;

import org.infinispan.client.hotrod.MetadataValue;
import org.infinispan.client.hotrod.near.NearCache;

import com.github.benmanes.caffeine.cache.Cache;

/**
 * Near cache implementation based on a Caffeine cache.
 * @author Paul Ferraro
 */
public class CaffeineNearCache<K, V> implements NearCache<K, V> {

    private final Map<K, MetadataValue<V>> map;

    public CaffeineNearCache(Cache<K, MetadataValue<V>> cache) {
        this.map = cache.asMap();
    }

    @Override
    public boolean putIfAbsent(K key, MetadataValue<V> value) {
        return this.map.putIfAbsent(key, value) == null;
    }

    @Override
    public boolean remove(K key) {
        return this.map.remove(key) != null;
    }

    @Override
    public boolean remove(K key, MetadataValue<V> value) {
        return this.map.remove(key, value);
    }

    @Override
    public boolean replace(K key, MetadataValue<V> prevValue, MetadataValue<V> newValue) {
        return this.map.replace(key, prevValue, newValue);
    }

    @Override
    public MetadataValue<V> get(K key) {
        return this.map.get(key);
    }

    @Override
    public void clear() {
        this.map.clear();
    }

    @Override
    public int size() {
        return this.map.size();
    }

    @Override
    public Iterator<Map.Entry<K, MetadataValue<V>>> iterator() {
        return this.map.entrySet().iterator();
    }
}
