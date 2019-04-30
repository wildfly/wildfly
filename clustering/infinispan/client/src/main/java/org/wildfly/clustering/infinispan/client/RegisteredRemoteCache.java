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

package org.wildfly.clustering.infinispan.client;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.UnaryOperator;

import org.infinispan.client.hotrod.CacheTopologyInfo;
import org.infinispan.client.hotrod.DataFormat;
import org.infinispan.client.hotrod.Flag;
import org.infinispan.client.hotrod.MetadataValue;
import org.infinispan.client.hotrod.RemoteCache;
import org.infinispan.client.hotrod.RemoteCacheManager;
import org.infinispan.client.hotrod.ServerStatistics;
import org.infinispan.client.hotrod.StreamingRemoteCache;
import org.infinispan.client.hotrod.VersionedValue;
import org.infinispan.client.hotrod.impl.RemoteCacheSupport;
import org.infinispan.client.hotrod.jmx.RemoteCacheClientStatisticsMXBean;
import org.infinispan.commons.util.CloseableIterator;
import org.infinispan.commons.util.CloseableIteratorCollection;
import org.infinispan.commons.util.CloseableIteratorSet;
import org.infinispan.commons.util.IntSet;
import org.infinispan.query.dsl.Query;
import org.wildfly.clustering.Registrar;
import org.wildfly.clustering.Registration;

/**
 * {@link RemoteCache} decorator that handles registration on {@link #start()} and deregistration on {@link #stop()}.
 * @author Paul Ferraro
 */
public class RegisteredRemoteCache<K, V> extends RemoteCacheSupport<K, V> implements UnaryOperator<Registration> {

    private final Registrar<String> registrar;
    private final AtomicReference<Registration> registration;
    private final RemoteCacheManager manager;
    private final RemoteCache<K, V> cache;

    public RegisteredRemoteCache(RemoteCacheManager manager, RemoteCache<K, V> cache, Registrar<String> registrar) {
        this(manager, cache, registrar, new AtomicReference<>());
    }

    private RegisteredRemoteCache(RemoteCacheManager manager, RemoteCache<K, V> cache, Registrar<String> registrar, AtomicReference<Registration> registration) {
        this.manager = manager;
        this.cache = cache;
        this.registrar = registrar;
        this.registration = registration;
    }

    @Override
    public boolean isTransactional() {
        return this.cache.isTransactional();
    }

    @Override
    public boolean removeWithVersion(K key, long version) {
        return this.cache.removeWithVersion(key, version);
    }

    @Override
    public void start() {
        if (this.registration.getAndUpdate(this) == null) {
            this.cache.start();
        }
    }

    @Override
    public Registration apply(Registration registration) {
        return (registration == null) ? this.registrar.register(this.getName()) : registration;
    }

    @Override
    public void stop() {
        try (Registration registration = this.registration.getAndSet(null)) {
            if (registration != null) {
                this.cache.stop();
            }
        }
    }

    @Override
    public V remove(Object key) {
        return this.cache.remove(key);
    }

    @Override
    public boolean remove(Object key, Object value) {
        return this.cache.remove(key, value);
    }

    @Override
    public boolean replace(K key, V oldValue, V value, long lifespan, TimeUnit lifespanUnit, long maxIdleTime, TimeUnit maxIdleTimeUnit) {
        return this.cache.replace(key, oldValue, value, lifespan, lifespanUnit, maxIdleTime, maxIdleTimeUnit);
    }

    @Override
    public CompletableFuture<Boolean> removeWithVersionAsync(K key, long version) {
        return this.cache.removeWithVersionAsync(key, version);
    }

    @Override
    public boolean replaceWithVersion(K key, V newValue, long version, int lifespanSeconds, int maxIdleTimeSeconds) {
        return this.cache.replaceWithVersion(key, newValue, version, lifespanSeconds, maxIdleTimeSeconds);
    }

    @Override
    public boolean replaceWithVersion(K key, V newValue, long version, long lifespan, TimeUnit lifespanTimeUnit, long maxIdle, TimeUnit maxIdleTimeUnit) {
        return this.cache.replaceWithVersion(key, newValue, version, lifespan, lifespanTimeUnit, maxIdle, maxIdleTimeUnit);
    }

    @Override
    public CompletableFuture<Boolean> replaceWithVersionAsync(K key, V newValue, long version, int lifespanSeconds, int maxIdleSeconds) {
        return this.cache.replaceWithVersionAsync(key, newValue, version, lifespanSeconds, maxIdleSeconds);
    }

    @Override
    public CloseableIterator<Entry<Object, Object>> retrieveEntries(String filterConverterFactory, Set<Integer> segments, int batchSize) {
        return this.cache.retrieveEntries(filterConverterFactory, segments, batchSize);
    }

    @Override
    public CloseableIterator<Entry<Object, Object>> retrieveEntries(String filterConverterFactory, Object[] filterConverterParams, Set<Integer> segments, int batchSize) {
        return this.cache.retrieveEntries(filterConverterFactory, filterConverterParams, segments, batchSize);
    }

    @Override
    public CloseableIterator<Entry<Object, Object>> retrieveEntries(String filterConverterFactory, int batchSize) {
        return this.cache.retrieveEntries(filterConverterFactory, batchSize);
    }

    @Override
    public CloseableIterator<Entry<Object, Object>> retrieveEntriesByQuery(Query filterQuery, Set<Integer> segments, int batchSize) {
        return this.cache.retrieveEntriesByQuery(filterQuery, segments, batchSize);
    }

    @Override
    public CloseableIterator<Entry<Object, MetadataValue<Object>>> retrieveEntriesWithMetadata(Set<Integer> segments, int batchSize) {
        return this.cache.retrieveEntriesWithMetadata(segments, batchSize);
    }

    @Deprecated
    @Override
    public VersionedValue<V> getVersioned(K key) {
        return this.cache.getVersioned(key);
    }

    @Override
    public MetadataValue<V> getWithMetadata(K key) {
        return this.cache.getWithMetadata(key);
    }

    @Override
    public CompletableFuture<MetadataValue<V>> getWithMetadataAsync(K key) {
        return this.cache.getWithMetadataAsync(key);
    }

    @Override
    public CloseableIteratorSet<K> keySet() {
        return this.cache.keySet();
    }

    @Override
    public CloseableIteratorSet<K> keySet(IntSet segments) {
        return this.cache.keySet(segments);
    }

    @Override
    public CloseableIteratorCollection<V> values() {
        return this.cache.values();
    }

    @Override
    public CloseableIteratorCollection<V> values(IntSet segments) {
        return this.cache.values(segments);
    }

    @Override
    public CloseableIteratorSet<Entry<K, V>> entrySet() {
        return this.cache.entrySet();
    }

    @Override
    public CloseableIteratorSet<Entry<K, V>> entrySet(IntSet segments) {
        return this.cache.entrySet(segments);
    }

    @Override
    public void putAll(Map<? extends K, ? extends V> map, long lifespan, TimeUnit lifespanUnit, long maxIdleTime, TimeUnit maxIdleTimeUnit) {
        this.cache.putAll(map, lifespan, lifespanUnit, maxIdleTime, maxIdleTimeUnit);
    }

    @Override
    public CompletableFuture<Void> putAllAsync(Map<? extends K, ? extends V> data, long lifespan, TimeUnit lifespanUnit, long maxIdle, TimeUnit maxIdleUnit) {
        return this.putAllAsync(data, lifespan, lifespanUnit, maxIdle, maxIdleUnit);
    }

    @Override
    public RemoteCacheClientStatisticsMXBean clientStatistics() {
        return this.cache.clientStatistics();
    }

    @Override
    public ServerStatistics serverStatistics() {
        return this.cache.serverStatistics();
    }

    @Override
    public RemoteCache<K, V> withFlags(Flag... flags) {
        return new RegisteredRemoteCache<>(this.manager, this.cache.withFlags(flags), this.registrar, this.registration);
    }

    @Override
    public RemoteCacheManager getRemoteCacheManager() {
        return this.manager;
    }

    @Override
    public Map<K, V> getAll(Set<? extends K> keys) {
        return this.cache.getAll(keys);
    }

    @Override
    public String getProtocolVersion() {
        return this.cache.getProtocolVersion();
    }

    @Override
    public void addClientListener(Object listener) {
        this.cache.addClientListener(listener);
    }

    @Override
    public void addClientListener(Object listener, Object[] filterFactoryParams, Object[] converterFactoryParams) {
        this.cache.addClientListener(listener, filterFactoryParams, converterFactoryParams);
    }

    @Override
    public void removeClientListener(Object listener) {
        this.cache.removeClientListener(listener);
    }

    @Deprecated
    @Override
    public Set<Object> getListeners() {
        return this.cache.getListeners();
    }

    @Override
    public <T> T execute(String scriptName, Map<String, ?> params) {
        return this.cache.execute(scriptName, params);
    }

    @Override
    public CacheTopologyInfo getCacheTopologyInfo() {
        return this.cache.getCacheTopologyInfo();
    }

    @Override
    public StreamingRemoteCache<K> streaming() {
        return this.cache.streaming();
    }

    @Override
    public <T, U> RemoteCache<T, U> withDataFormat(DataFormat dataFormat) {
        return new RegisteredRemoteCache<>(this.manager, this.cache.withDataFormat(dataFormat), this.registrar, this.registration);
    }

    @Override
    public DataFormat getDataFormat() {
        return this.cache.getDataFormat();
    }

    @Override
    public String getName() {
        return this.cache.getName();
    }

    @Override
    public String getVersion() {
        return this.cache.getVersion();
    }

    @Override
    public V put(K arg0, V arg1, long arg2, TimeUnit arg3, long arg4, TimeUnit arg5) {
        return this.cache.put(arg0, arg1, arg2, arg3, arg4, arg5);
    }

    @Override
    public V putIfAbsent(K arg0, V arg1, long arg2, TimeUnit arg3, long arg4, TimeUnit arg5) {
        return this.cache.putIfAbsent(arg0, arg1, arg2, arg3, arg4, arg5);
    }

    @Override
    public V replace(K arg0, V arg1, long arg2, TimeUnit arg3, long arg4, TimeUnit arg5) {
        return this.cache.replace(arg0, arg1, arg2, arg3, arg4, arg5);
    }

    @Override
    public CompletableFuture<Void> clearAsync() {
        return this.cache.clearAsync();
    }

    @Override
    public CompletableFuture<V> getAsync(K arg0) {
        return this.cache.getAsync(arg0);
    }

    @Override
    public CompletableFuture<V> putAsync(K arg0, V arg1, long arg2, TimeUnit arg3, long arg4, TimeUnit arg5) {
        return this.cache.putAsync(arg0, arg1, arg2, arg3, arg4, arg5);
    }

    @Override
    public CompletableFuture<V> putIfAbsentAsync(K arg0, V arg1, long arg2, TimeUnit arg3, long arg4, TimeUnit arg5) {
        return this.cache.putIfAbsentAsync(arg0, arg1, arg2, arg3, arg4, arg5);
    }

    @Override
    public CompletableFuture<V> removeAsync(Object arg0) {
        return this.cache.removeAsync(arg0);
    }

    @Override
    public CompletableFuture<V> replaceAsync(K arg0, V arg1, long arg2, TimeUnit arg3, long arg4, TimeUnit arg5) {
        return this.replaceAsync(arg0, arg1, arg2, arg3, arg4, arg5);
    }

    @Override
    public int size() {
        return this.cache.size();
    }

    @Override
    public CompletableFuture<Long> sizeAsync() {
        return this.cache.sizeAsync();
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
    public void clear() {
        this.cache.clear();
    }

    @Override
    protected void set(K key, V value) {
        this.put(key, value, this.defaultLifespan, TimeUnit.MILLISECONDS, this.defaultMaxIdleTime, TimeUnit.MILLISECONDS);
    }
}
