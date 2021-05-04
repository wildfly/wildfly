/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2019, Red Hat, Inc., and individual contributors
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

package org.wildfly.clustering.infinispan.client.near;

import java.util.Iterator;
import java.util.Map;

import org.infinispan.client.hotrod.MetadataValue;
import org.infinispan.client.hotrod.near.NearCache;

import com.github.benmanes.caffeine.cache.Cache;

/**
 * Near cache implementation based on a Caffeine cache.
 * Workaround for ISPN-10248, {@link NearCache} is package protected.
 * To be refactored into org.wildfly.clustering.infinispan.client.near package once Infinispan increases visibility of {@link NearCache}.
 * @author Paul Ferraro
 */
public class CaffeineNearCache<K, V> implements NearCache<K, V> {

    private final Map<K, MetadataValue<V>> map;

    public CaffeineNearCache(Cache<K, MetadataValue<V>> cache) {
        this.map = cache.asMap();
    }

    @Override
    public void put(K key, MetadataValue<V> value) {
        this.map.put(key, value);
    }

    @Override
    public void putIfAbsent(K key, MetadataValue<V> value) {
        this.map.putIfAbsent(key, value);
    }

    @Override
    public boolean remove(K key) {
        return this.map.remove(key) != null;
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
