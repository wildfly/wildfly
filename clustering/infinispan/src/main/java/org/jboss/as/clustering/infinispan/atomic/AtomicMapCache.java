package org.jboss.as.clustering.infinispan.atomic;

import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.infinispan.AbstractDelegatingAdvancedCache;
import org.infinispan.Cache;
import org.infinispan.atomic.AtomicMapLookup;
import org.infinispan.stats.Stats;
import org.infinispan.util.concurrent.NotifyingFuture;

public class AtomicMapCache<K, MK, MV> extends AbstractDelegatingAdvancedCache<K, Map<MK, MV>> {
    public AtomicMapCache(Cache<K, Map<MK, MV>> cache) {
        super(cache.getAdvancedCache());
    }

    @SuppressWarnings("unchecked")
    @Override
    public Map<MK, MV> get(Object key) {
        return AtomicMapLookup.getAtomicMap(this.getAdvancedCache(), (K) key, false);
    }

    @Override
    public Map<MK, MV> putIfAbsent(K key, Map<MK, MV> value) {
        return AtomicMapLookup.getAtomicMap(this.getAdvancedCache(), key, true);
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

    @Override
    public Stats getStats() {
        return this.getAdvancedCache().getStats();
    }
}
