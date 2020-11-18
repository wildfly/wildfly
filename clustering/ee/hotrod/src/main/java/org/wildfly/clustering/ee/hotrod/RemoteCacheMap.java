/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2020, Red Hat, Inc., and individual contributors
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
