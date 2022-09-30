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

package org.jboss.as.clustering.infinispan.client;

import java.net.SocketAddress;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.UnaryOperator;

import javax.management.ObjectName;

import jakarta.transaction.TransactionManager;

import org.infinispan.client.hotrod.CacheTopologyInfo;
import org.infinispan.client.hotrod.DataFormat;
import org.infinispan.client.hotrod.Flag;
import org.infinispan.client.hotrod.MetadataValue;
import org.infinispan.client.hotrod.RemoteCache;
import org.infinispan.client.hotrod.RemoteCacheManager;
import org.infinispan.client.hotrod.ServerStatistics;
import org.infinispan.client.hotrod.StreamingRemoteCache;
import org.infinispan.client.hotrod.configuration.Configuration;
import org.infinispan.client.hotrod.impl.ClientStatistics;
import org.infinispan.client.hotrod.impl.InternalRemoteCache;
import org.infinispan.client.hotrod.impl.RemoteCacheSupport;
import org.infinispan.client.hotrod.impl.operations.OperationsFactory;
import org.infinispan.client.hotrod.impl.operations.PingResponse;
import org.infinispan.client.hotrod.impl.operations.RetryAwareCompletionStage;
import org.infinispan.commons.util.CloseableIterator;
import org.infinispan.commons.util.CloseableIteratorCollection;
import org.infinispan.commons.util.CloseableIteratorSet;
import org.infinispan.commons.util.IntSet;
import org.infinispan.query.dsl.Query;
import org.reactivestreams.Publisher;
import org.wildfly.clustering.Registrar;
import org.wildfly.clustering.Registration;
import org.wildfly.clustering.infinispan.client.RemoteCacheContainer;

/**
 * {@link RemoteCache} decorator that handles registration on {@link #start()} and deregistration on {@link #stop()}.
 * @author Paul Ferraro
 */
public class ManagedRemoteCache<K, V> extends RemoteCacheSupport<K, V> implements InternalRemoteCache<K, V>, UnaryOperator<Registration> {

    private final Registrar<String> registrar;
    private final AtomicReference<Registration> registration;
    private final RemoteCacheContainer container;
    private final RemoteCacheManager manager;
    private final InternalRemoteCache<K, V> cache;

    public ManagedRemoteCache(RemoteCacheContainer container, RemoteCacheManager manager, RemoteCache<K, V> cache, Registrar<String> registrar) {
        this(container, manager, (InternalRemoteCache<K, V>) cache, registrar, new AtomicReference<>());
    }

    private ManagedRemoteCache(RemoteCacheContainer container, RemoteCacheManager manager, InternalRemoteCache<K, V> cache, Registrar<String> registrar, AtomicReference<Registration> registration) {
        this.container = container;
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
    public TransactionManager getTransactionManager() {
        return this.cache.getTransactionManager();
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
    public RemoteCacheContainer getRemoteCacheContainer() {
        return this.container;
    }

    @Deprecated
    @Override
    public RemoteCacheManager getRemoteCacheManager() {
        return this.manager;
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
    public ClientStatistics clientStatistics() {
        return this.cache.clientStatistics();
    }

    @Override
    public CloseableIteratorSet<Entry<K, V>> entrySet(IntSet segments) {
        return this.cache.entrySet(segments);
    }

    @Override
    public <T> T execute(String taskName, Map<String, ?> params) {
        return this.cache.execute(taskName, params);
    }

    @Override
    public CacheTopologyInfo getCacheTopologyInfo() {
        return this.cache.getCacheTopologyInfo();
    }

    @Override
    public DataFormat getDataFormat() {
        return this.cache.getDataFormat();
    }

    @Deprecated
    @Override
    public Set<Object> getListeners() {
        return this.cache.getListeners();
    }

    @Override
    public String getProtocolVersion() {
        return this.cache.getProtocolVersion();
    }

    @Override
    public CloseableIteratorSet<K> keySet(IntSet segments) {
        return this.cache.keySet(segments);
    }

    @Override
    public void removeClientListener(Object listener) {
        this.cache.removeClientListener(listener);
    }

    @Override
    public CompletableFuture<Boolean> replaceWithVersionAsync(K key, V newValue, long version, long lifespanSeconds, TimeUnit lifespanTimeUnit, long maxIdle, TimeUnit maxIdleTimeUnit) {
        return this.cache.replaceWithVersionAsync(key, newValue, version, lifespanSeconds, lifespanTimeUnit, maxIdle, maxIdleTimeUnit);
    }

    @Override
    public CloseableIterator<Map.Entry<Object, Object>> retrieveEntries(String filterConverterFactory, Object[] filterConverterParams, Set<Integer> segments, int batchSize) {
        return this.cache.retrieveEntries(filterConverterFactory, filterConverterParams, segments, batchSize);
    }

    @Override
    public CloseableIterator<Map.Entry<Object, Object>> retrieveEntriesByQuery(Query<?> filterQuery, Set<Integer> segments, int batchSize) {
        return this.cache.retrieveEntriesByQuery(filterQuery, segments, batchSize);
    }

    @Override
    public CloseableIterator<Map.Entry<Object, MetadataValue<Object>>> retrieveEntriesWithMetadata(Set<Integer> segments, int batchSize) {
        return this.cache.retrieveEntriesWithMetadata(segments, batchSize);
    }

    @Override
    public <E> Publisher<Map.Entry<K, E>> publishEntries(String filterConverterFactory, Object[] filterConverterParams, Set<Integer> segments, int batchSize) {
        return this.cache.publishEntries(filterConverterFactory, filterConverterParams, segments, batchSize);
    }

    @Override
    public <E> Publisher<Map.Entry<K, E>> publishEntriesByQuery(Query<?> filterQuery, Set<Integer> segments, int batchSize) {
        return this.cache.publishEntriesByQuery(filterQuery, segments, batchSize);
    }

    @Override
    public Publisher<Map.Entry<K, MetadataValue<V>>> publishEntriesWithMetadata(Set<Integer> segments, int batchSize) {
        return this.cache.publishEntriesWithMetadata(segments, batchSize);
    }

    @Override
    public ServerStatistics serverStatistics() {
        return this.cache.serverStatistics();
    }

    @Override
    public StreamingRemoteCache<K> streaming() {
        return this.cache.streaming();
    }

    @Override
    public CloseableIteratorCollection<V> values(IntSet segments) {
        return this.cache.values(segments);
    }

    @Override
    public <T, U> InternalRemoteCache<T, U> withDataFormat(DataFormat dataFormat) {
        return new ManagedRemoteCache<>(this.container, this.manager, this.cache.withDataFormat(dataFormat), this.registrar, this.registration);
    }

    @Override
    public InternalRemoteCache<K, V> withFlags(Flag... flags) {
        return new ManagedRemoteCache<>(this.container, this.manager, this.cache.withFlags(flags), this.registrar, this.registration);
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
    public CompletableFuture<Void> clearAsync() {
        return this.cache.clearAsync();
    }

    @Override
    public boolean isEmpty() {
        return this.cache.isEmpty();
    }

    @Override
    public boolean containsValue(Object value) {
        return this.cache.containsValue(value);
    }

    @Override
    public CompletableFuture<V> computeAsync(K key, BiFunction<? super K, ? super V, ? extends V> remappingFunction, long lifespan, TimeUnit lifespanUnit, long maxIdle, TimeUnit maxIdleUnit) {
        return this.cache.computeAsync(key, remappingFunction, lifespan, lifespanUnit, maxIdle, maxIdleUnit);
    }

    @Override
    public CompletableFuture<V> computeIfAbsentAsync(K key, Function<? super K, ? extends V> mappingFunction, long lifespan, TimeUnit lifespanUnit, long maxIdle, TimeUnit maxIdleUnit) {
        return this.cache.computeIfAbsentAsync(key, mappingFunction, lifespan, lifespanUnit, maxIdle, maxIdleUnit);
    }

    @Override
    public CompletableFuture<V> computeIfPresentAsync(K key, BiFunction<? super K, ? super V, ? extends V> remappingFunction, long lifespan, TimeUnit lifespanUnit, long maxIdle, TimeUnit maxIdleUnit) {
        return this.cache.computeIfPresentAsync(key, remappingFunction, lifespan, lifespanUnit, maxIdle, maxIdleUnit);
    }

    @Override
    public CompletableFuture<Boolean> containsKeyAsync(K key) {
        return this.cache.containsKeyAsync(key);
    }

    @Override
    public CompletableFuture<Map<K, V>> getAllAsync(Set<?> keys) {
        return this.cache.getAllAsync(keys);
    }

    @Override
    public CompletableFuture<V> getAsync(K key) {
        return this.cache.getAsync(key);
    }

    @Override
    public CompletableFuture<MetadataValue<V>> getWithMetadataAsync(K key) {
        return this.cache.getWithMetadataAsync(key);
    }

    @Override
    public CompletableFuture<V> mergeAsync(K key, V value, BiFunction<? super V, ? super V, ? extends V> remappingFunction, long lifespan, TimeUnit lifespanUnit, long maxIdleTime, TimeUnit maxIdleTimeUnit) {
        return this.cache.mergeAsync(key, value, remappingFunction, lifespan, lifespanUnit, maxIdleTime, maxIdleTimeUnit);
    }

    @Override
    public CompletableFuture<Void> putAllAsync(Map<? extends K, ? extends V> data, long lifespan, TimeUnit lifespanUnit, long maxIdle, TimeUnit maxIdleUnit) {
        return this.cache.putAllAsync(data, lifespan, lifespanUnit, maxIdle, maxIdleUnit);
    }

    @Override
    public CompletableFuture<V> putAsync(K key, V value, long lifespan, TimeUnit lifespanUnit, long maxIdle, TimeUnit maxIdleUnit) {
        return this.cache.putAsync(key, value, lifespan, lifespanUnit, maxIdle, maxIdleUnit);
    }

    @Override
    public CompletableFuture<V> putIfAbsentAsync(K key, V value, long lifespan, TimeUnit lifespanUnit, long maxIdle, TimeUnit maxIdleUnit) {
        return this.cache.putIfAbsentAsync(key, value, lifespan, lifespanUnit, maxIdle, maxIdleUnit);
    }

    @Override
    public CompletableFuture<V> removeAsync(Object key) {
        return this.cache.removeAsync(key);
    }

    @Override
    public CompletableFuture<Boolean> removeAsync(Object key, Object value) {
        return this.cache.removeAsync(key, value);
    }

    @Override
    public CompletableFuture<Boolean> removeWithVersionAsync(K key, long version) {
        return this.cache.removeWithVersionAsync(key, version);
    }

    @Override
    public void replaceAll(BiFunction<? super K, ? super V, ? extends V> function) {
        this.cache.replaceAll(function);
    }

    @Override
    public CompletableFuture<V> replaceAsync(K key, V value, long lifespan, TimeUnit lifespanUnit, long maxIdle, TimeUnit maxIdleUnit) {
        return this.cache.replaceAsync(key, value, lifespan, lifespanUnit, maxIdle, maxIdleUnit);
    }

    @Override
    public CompletableFuture<Boolean> replaceAsync(K key, V oldValue, V newValue, long lifespan, TimeUnit lifespanUnit, long maxIdle, TimeUnit maxIdleUnit) {
        return this.cache.replaceAsync(key, oldValue, newValue, lifespan, lifespanUnit, maxIdle, maxIdleUnit);
    }

    @Override
    public CompletableFuture<Long> sizeAsync() {
        return this.cache.sizeAsync();
    }

    @Override
    public CompletionStage<ServerStatistics> serverStatisticsAsync() {
        return this.cache.serverStatisticsAsync();
    }

    @Override
    public CloseableIterator<K> keyIterator(IntSet segments) {
        return this.cache.keyIterator(segments);
    }

    @Override
    public CloseableIterator<Entry<K, V>> entryIterator(IntSet segments) {
        return this.cache.entryIterator(segments);
    }

    @Override
    public RetryAwareCompletionStage<MetadataValue<V>> getWithMetadataAsync(K key, SocketAddress preferredAddress) {
        return this.cache.getWithMetadataAsync(key, preferredAddress);
    }

    @Override
    public boolean hasForceReturnFlag() {
        return this.cache.hasForceReturnFlag();
    }

    @Override
    public void resolveStorage(boolean objectStorage) {
        this.cache.resolveStorage(objectStorage);
    }

    @Override
    public void init(OperationsFactory operationsFactory, Configuration configuration, ObjectName jmxParent) {
        this.cache.init(operationsFactory, configuration, jmxParent);
    }

    @Override
    public void init(OperationsFactory operationsFactory, Configuration configuration) {
        this.cache.init(operationsFactory, configuration);
    }

    @Override
    public OperationsFactory getOperationsFactory() {
        return this.cache.getOperationsFactory();
    }

    @Override
    public boolean isObjectStorage() {
        return this.cache.isObjectStorage();
    }

    @Override
    public K keyAsObjectIfNeeded(Object key) {
        return this.cache.keyAsObjectIfNeeded(key);
    }

    @Override
    public byte[] keyToBytes(Object object) {
        return this.cache.keyToBytes(object);
    }

    @Override
    public CompletionStage<PingResponse> ping() {
        return this.cache.ping();
    }

    @Override
    public SocketAddress addNearCacheListener(Object listener, int bloomBits) {
        return this.cache.addNearCacheListener(listener, bloomBits);
    }

    @Override
    public CompletionStage<Void> updateBloomFilter() {
        return this.cache.updateBloomFilter();
    }
}
