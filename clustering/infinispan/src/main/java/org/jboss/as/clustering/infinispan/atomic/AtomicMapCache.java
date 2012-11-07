/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2012, Red Hat, Inc., and individual contributors
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

package org.jboss.as.clustering.infinispan.atomic;

import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.infinispan.AdvancedCache;
import org.infinispan.atomic.AtomicMapLookup;
import org.infinispan.util.concurrent.NotifyingFuture;
import org.jboss.as.clustering.infinispan.AbstractAdvancedCache;

public class AtomicMapCache<K, MK, MV> extends AbstractAdvancedCache<K, Map<MK, MV>> {
    public AtomicMapCache(AdvancedCache<K, Map<MK, MV>> cache) {
        super(cache);
    }

    @Override
    protected AdvancedCache<K, Map<MK, MV>> wrap(AdvancedCache<K, Map<MK, MV>> cache) {
        return new AtomicMapCache<K, MK, MV>(cache);
    }

    @SuppressWarnings("unchecked")
    @Override
    public Map<MK, MV> get(Object key) {
        return AtomicMapLookup.getAtomicMap(this.cache, (K) key, false);
    }

    @Override
    public Map<MK, MV> putIfAbsent(K key, Map<MK, MV> value) {
        return AtomicMapLookup.getAtomicMap(this.cache, key, true);
    }

    @Override
    public void putForExternalRead(K key, Map<MK, MV> value) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Map<MK, MV> put(K key, Map<MK, MV> value, long lifespan, TimeUnit unit) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Map<MK, MV> putIfAbsent(K key, Map<MK, MV> value, long lifespan, TimeUnit unit) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void putAll(Map<? extends K, ? extends Map<MK, MV>> map, long lifespan, TimeUnit unit) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Map<MK, MV> replace(K key, Map<MK, MV> value, long lifespan, TimeUnit unit) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean replace(K key, Map<MK, MV> oldValue, Map<MK, MV> value, long lifespan, TimeUnit unit) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Map<MK, MV> put(K key, Map<MK, MV> value, long lifespan, TimeUnit lifespanUnit, long maxIdleTime, TimeUnit maxIdleTimeUnit) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Map<MK, MV> putIfAbsent(K key, Map<MK, MV> value, long lifespan, TimeUnit lifespanUnit, long maxIdleTime, TimeUnit maxIdleTimeUnit) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void putAll(Map<? extends K, ? extends Map<MK, MV>> map, long lifespan, TimeUnit lifespanUnit, long maxIdleTime, TimeUnit maxIdleTimeUnit) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Map<MK, MV> replace(K key, Map<MK, MV> value, long lifespan, TimeUnit lifespanUnit, long maxIdleTime, TimeUnit maxIdleTimeUnit) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean replace(K key, Map<MK, MV> oldValue, Map<MK, MV> value, long lifespan, TimeUnit lifespanUnit, long maxIdleTime, TimeUnit maxIdleTimeUnit) {
        throw new UnsupportedOperationException();
    }

    @Override
    public NotifyingFuture<Map<MK, MV>> putAsync(K key, Map<MK, MV> value) {
        throw new UnsupportedOperationException();
    }

    @Override
    public NotifyingFuture<Map<MK, MV>> putAsync(K key, Map<MK, MV> value, long lifespan, TimeUnit unit) {
        throw new UnsupportedOperationException();
    }

    @Override
    public NotifyingFuture<Map<MK, MV>> putAsync(K key, Map<MK, MV> value, long lifespan, TimeUnit lifespanUnit, long maxIdle, TimeUnit maxIdleUnit) {
        throw new UnsupportedOperationException();
    }

    @Override
    public NotifyingFuture<Void> putAllAsync(Map<? extends K, ? extends Map<MK, MV>> data) {
        throw new UnsupportedOperationException();
    }

    @Override
    public NotifyingFuture<Void> putAllAsync(Map<? extends K, ? extends Map<MK, MV>> data, long lifespan, TimeUnit unit) {
        throw new UnsupportedOperationException();
    }

    @Override
    public NotifyingFuture<Void> putAllAsync(Map<? extends K, ? extends Map<MK, MV>> data, long lifespan, TimeUnit lifespanUnit, long maxIdle, TimeUnit maxIdleUnit) {
        throw new UnsupportedOperationException();
    }

    @Override
    public NotifyingFuture<Map<MK, MV>> putIfAbsentAsync(K key, Map<MK, MV> value) {
        throw new UnsupportedOperationException();
    }

    @Override
    public NotifyingFuture<Map<MK, MV>> putIfAbsentAsync(K key, Map<MK, MV> value, long lifespan, TimeUnit unit) {
        throw new UnsupportedOperationException();
    }

    @Override
    public NotifyingFuture<Map<MK, MV>> putIfAbsentAsync(K key, Map<MK, MV> value, long lifespan, TimeUnit lifespanUnit, long maxIdle, TimeUnit maxIdleUnit) {
        throw new UnsupportedOperationException();
    }

    @Override
    public NotifyingFuture<Map<MK, MV>> replaceAsync(K key, Map<MK, MV> value) {
        throw new UnsupportedOperationException();
    }

    @Override
    public NotifyingFuture<Map<MK, MV>> replaceAsync(K key, Map<MK, MV> value, long lifespan, TimeUnit unit) {
        throw new UnsupportedOperationException();
    }

    @Override
    public NotifyingFuture<Map<MK, MV>> replaceAsync(K key, Map<MK, MV> value, long lifespan, TimeUnit lifespanUnit, long maxIdle, TimeUnit maxIdleUnit) {
        throw new UnsupportedOperationException();
    }

    @Override
    public NotifyingFuture<Boolean> replaceAsync(K key, Map<MK, MV> oldValue, Map<MK, MV> newValue) {
        throw new UnsupportedOperationException();
    }

    @Override
    public NotifyingFuture<Boolean> replaceAsync(K key, Map<MK, MV> oldValue, Map<MK, MV> newValue, long lifespan, TimeUnit unit) {
        throw new UnsupportedOperationException();
    }

    @Override
    public NotifyingFuture<Boolean> replaceAsync(K key, Map<MK, MV> oldValue, Map<MK, MV> newValue, long lifespan, TimeUnit lifespanUnit, long maxIdle, TimeUnit maxIdleUnit) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean replace(K key, Map<MK, MV> oldValue, Map<MK, MV> newValue) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Map<MK, MV> replace(K key, Map<MK, MV> value) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Map<MK, MV> put(K key, Map<MK, MV> value) {
        throw new UnsupportedOperationException();
    }
}
