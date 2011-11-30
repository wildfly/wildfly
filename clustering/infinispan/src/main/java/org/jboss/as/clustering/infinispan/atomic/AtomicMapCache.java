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
