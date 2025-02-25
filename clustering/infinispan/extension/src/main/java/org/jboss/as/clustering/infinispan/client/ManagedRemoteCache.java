/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.clustering.infinispan.client;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.Flow;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.UnaryOperator;

import javax.management.ObjectName;

import jakarta.transaction.TransactionManager;

import io.netty.channel.Channel;

import org.infinispan.api.async.AsyncCacheEntryProcessor;
import org.infinispan.api.common.CacheEntry;
import org.infinispan.api.common.CacheEntryVersion;
import org.infinispan.api.common.CacheOptions;
import org.infinispan.api.common.CacheWriteOptions;
import org.infinispan.api.common.events.cache.CacheEntryEvent;
import org.infinispan.api.common.events.cache.CacheEntryEventType;
import org.infinispan.api.common.events.cache.CacheListenerOptions;
import org.infinispan.api.common.process.CacheEntryProcessorResult;
import org.infinispan.api.common.process.CacheProcessorOptions;
import org.infinispan.api.configuration.CacheConfiguration;
import org.infinispan.client.hotrod.CacheTopologyInfo;
import org.infinispan.client.hotrod.DataFormat;
import org.infinispan.client.hotrod.Flag;
import org.infinispan.client.hotrod.MetadataValue;
import org.infinispan.client.hotrod.RemoteCache;
import org.infinispan.client.hotrod.RemoteCacheManager;
import org.infinispan.client.hotrod.ServerStatistics;
import org.infinispan.client.hotrod.StreamingRemoteCache;
import org.infinispan.client.hotrod.configuration.Configuration;
import org.infinispan.client.hotrod.event.impl.ClientListenerNotifier;
import org.infinispan.client.hotrod.impl.ClientStatistics;
import org.infinispan.client.hotrod.impl.InternalRemoteCache;
import org.infinispan.client.hotrod.impl.RemoteCacheSupport;
import org.infinispan.client.hotrod.impl.operations.CacheOperationsFactory;
import org.infinispan.client.hotrod.impl.operations.GetWithMetadataOperation.GetWithMetadataResult;
import org.infinispan.client.hotrod.impl.operations.PingResponse;
import org.infinispan.client.hotrod.impl.transport.netty.OperationDispatcher;
import org.infinispan.commons.api.query.ContinuousQuery;
import org.infinispan.commons.util.CloseableIterator;
import org.infinispan.commons.util.CloseableIteratorCollection;
import org.infinispan.commons.util.CloseableIteratorSet;
import org.infinispan.commons.util.IntSet;
import org.reactivestreams.Publisher;
import org.wildfly.clustering.infinispan.client.RemoteCacheContainer;
import org.wildfly.clustering.server.Registrar;
import org.wildfly.clustering.server.Registration;

/**
 * {@link RemoteCache} decorator that handles registration on {@link #start()} and deregistration on {@link #stop()}.
 * N.B. Implements {@link InternalRemoteCache} to support casting, as required by {@link org.infinispan.client.hotrod.Search#getQueryFactory(RemoteCache)}.
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
    public InternalRemoteCache<K, V> withFlags(Flag... flags) {
        return new ManagedRemoteCache<>(this.container, this.manager, this.cache.withFlags(flags), this.registrar, this.registration);
    }

    @Override
    public InternalRemoteCache<K, V> noFlags() {
        return new ManagedRemoteCache<>(this.container, this.manager, this.cache.noFlags(), this.registrar, this.registration);
    }

    @Override
    public <T, U> InternalRemoteCache<T, U> withDataFormat(DataFormat dataFormat) {
        return new ManagedRemoteCache<>(this.container, this.manager, this.cache.withDataFormat(dataFormat), this.registrar, this.registration);
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
    public CloseableIterator<Map.Entry<Object, MetadataValue<Object>>> retrieveEntriesWithMetadata(Set<Integer> segments, int batchSize) {
        return this.cache.retrieveEntriesWithMetadata(segments, batchSize);
    }

    @Override
    public <E> Publisher<Map.Entry<K, E>> publishEntries(String filterConverterFactory, Object[] filterConverterParams, Set<Integer> segments, int batchSize) {
        return this.cache.publishEntries(filterConverterFactory, filterConverterParams, segments, batchSize);
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
    public <T> org.infinispan.commons.api.query.Query<T> query(String query) {
        return this.cache.query(query);
    }

    @Override
    public ContinuousQuery<K, V> continuousQuery() {
        return this.cache.continuousQuery();
    }

    @Override
    public CloseableIterator<Map.Entry<Object, Object>> retrieveEntriesByQuery(org.infinispan.commons.api.query.Query<?> filterQuery, Set<Integer> segments, int batchSize) {
        return this.cache.retrieveEntriesByQuery(filterQuery, segments, batchSize);
    }

    @Override
    public <E> Publisher<Map.Entry<K, E>> publishEntriesByQuery(org.infinispan.commons.api.query.Query<?> filterQuery, Set<Integer> segments, int batchSize) {
        return this.cache.publishEntriesByQuery(filterQuery, segments, batchSize);
    }

    @Override
    public Set<Flag> flags() {
        return this.cache.flags();
    }

    @Override
    public byte[] getNameBytes() {
        return this.cache.getNameBytes();
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
    public CompletionStage<GetWithMetadataResult<V>> getWithMetadataAsync(K key, Channel channel) {
        return this.cache.getWithMetadataAsync(key, channel);
    }

    @Override
    public int flagInt() {
        return this.cache.flagInt();
    }

    @Override
    public boolean hasForceReturnFlag() {
        return this.cache.hasForceReturnFlag();
    }

    @Override
    public void resolveStorage() {
        this.cache.resolveStorage();
    }

    @Override
    public void init(Configuration configuration, OperationDispatcher dispatcher) {
        this.cache.init(configuration, dispatcher);
    }

    @Override
    public void init(Configuration configuration, OperationDispatcher dispatcher, ObjectName jmxParent) {
        this.cache.init(configuration, dispatcher, jmxParent);
    }

    @Override
    public OperationDispatcher getDispatcher() {
        return this.cache.getDispatcher();
    }

    @Override
    public byte[] keyToBytes(Object key) {
        return this.cache.keyToBytes(key);
    }

    @Override
    public CompletionStage<PingResponse> ping() {
        return this.cache.ping();
    }

    @Override
    public Channel addNearCacheListener(Object listener, int bloomBits) {
        return this.cache.addNearCacheListener(listener, bloomBits);
    }

    @Override
    public CompletionStage<Void> updateBloomFilter() {
        return this.cache.updateBloomFilter();
    }

    @Override
    public CacheOperationsFactory getOperationsFactory() {
        return this.cache.getOperationsFactory();
    }

    @Override
    public ClientListenerNotifier getListenerNotifier() {
        return this.cache.getListenerNotifier();
    }

    @Override
    public CompletionStage<CacheConfiguration> configuration() {
        return this.cache.configuration();
    }

    @Override
    public CompletionStage<V> get(K key, CacheOptions options) {
        return this.cache.get(key, options);
    }

    @Override
    public CompletionStage<CacheEntry<K, V>> getEntry(K key, CacheOptions options) {
        return this.cache.getEntry(key, options);
    }

    @Override
    public CompletionStage<CacheEntry<K, V>> putIfAbsent(K key, V value, CacheWriteOptions options) {
        return this.cache.putIfAbsent(key, value, options);
    }

    @Override
    public CompletionStage<Boolean> setIfAbsent(K key, V value, CacheWriteOptions options) {
        return this.cache.setIfAbsent(key, value, options);
    }

    @Override
    public CompletionStage<CacheEntry<K, V>> put(K key, V value, CacheWriteOptions options) {
        return this.cache.put(key, value, options);
    }

    @Override
    public CompletionStage<Void> set(K key, V value, CacheWriteOptions options) {
        return this.cache.set(key, value, options);
    }

    @Override
    public CompletionStage<Boolean> replace(K key, V value, CacheEntryVersion version, CacheWriteOptions options) {
        return this.cache.replace(key, value, version, options);
    }

    @Override
    public CompletionStage<CacheEntry<K, V>> getOrReplaceEntry(K key, V value, CacheEntryVersion version, CacheWriteOptions options) {
        return this.cache.getOrReplaceEntry(key, value, version, options);
    }

    @Override
    public CompletionStage<Boolean> remove(K key, CacheOptions options) {
        return this.cache.remove(key, options);
    }

    @Override
    public CompletionStage<Boolean> remove(K key, CacheEntryVersion version, CacheOptions options) {
        return this.cache.remove(key, version, options);
    }

    @Override
    public CompletionStage<CacheEntry<K, V>> getAndRemove(K key, CacheOptions options) {
        return this.cache.getAndRemove(key, options);
    }

    @Override
    public Flow.Publisher<K> keys(CacheOptions options) {
        return this.cache.keys(options);
    }

    @Override
    public Flow.Publisher<CacheEntry<K, V>> entries(CacheOptions options) {
        return this.cache.entries(options);
    }

    @Override
    public CompletionStage<Void> putAll(Map<K, V> entries, CacheWriteOptions options) {
        return this.cache.putAll(entries, options);
    }

    @Override
    public CompletionStage<Void> putAll(Flow.Publisher<CacheEntry<K, V>> entries, CacheWriteOptions options) {
        return this.cache.putAll(entries, options);
    }

    @Override
    public Flow.Publisher<CacheEntry<K, V>> getAll(Set<K> keys, CacheOptions options) {
        return this.cache.getAll(keys, options);
    }

    @Override
    public Flow.Publisher<CacheEntry<K, V>> getAll(CacheOptions options, K[] keys) {
        return this.cache.getAll(options, keys);
    }

    @Override
    public Flow.Publisher<K> removeAll(Set<K> keys, CacheWriteOptions options) {
        return this.cache.removeAll(keys, options);
    }

    @Override
    public Flow.Publisher<K> removeAll(Flow.Publisher<K> keys, CacheWriteOptions options) {
        return this.cache.removeAll(keys, options);
    }

    @Override
    public Flow.Publisher<CacheEntry<K, V>> getAndRemoveAll(Set<K> keys, CacheWriteOptions options) {
        return this.cache.getAndRemoveAll(keys, options);
    }

    @Override
    public Flow.Publisher<CacheEntry<K, V>> getAndRemoveAll(Flow.Publisher<K> keys, CacheWriteOptions options) {
        return this.cache.getAndRemoveAll(keys, options);
    }

    @Override
    public CompletionStage<Long> estimateSize(CacheOptions options) {
        return this.cache.estimateSize(options);
    }

    @Override
    public CompletionStage<Void> clear(CacheOptions options) {
        return this.cache.clear(options);
    }

    @Override
    public Flow.Publisher<CacheEntryEvent<K, V>> listen(CacheListenerOptions options, CacheEntryEventType[] types) {
        return this.cache.listen(options, types);
    }

    @Override
    public <T> Flow.Publisher<CacheEntryProcessorResult<K, T>> process(Set<K> keys, AsyncCacheEntryProcessor<K, V, T> task, CacheOptions options) {
        return this.cache.process(keys, task, options);
    }

    @Override
    public <T> Flow.Publisher<CacheEntryProcessorResult<K, T>> processAll(AsyncCacheEntryProcessor<K, V, T> processor, CacheProcessorOptions options) {
        return this.cache.processAll(processor, options);
    }
}
