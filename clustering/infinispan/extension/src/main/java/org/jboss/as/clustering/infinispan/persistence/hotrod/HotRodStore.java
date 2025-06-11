/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.clustering.infinispan.persistence.hotrod;

import java.util.EnumSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.PrimitiveIterator;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReferenceArray;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.core.Flowable;
import io.reactivex.rxjava3.schedulers.Schedulers;

import org.infinispan.Cache;
import org.infinispan.client.hotrod.DataFormat;
import org.infinispan.client.hotrod.Flag;
import org.infinispan.client.hotrod.RemoteCache;
import org.infinispan.client.hotrod.configuration.NearCacheMode;
import org.infinispan.client.hotrod.configuration.RemoteCacheConfigurationBuilder;
import org.infinispan.client.hotrod.configuration.TransactionMode;
import org.infinispan.client.hotrod.impl.InternalRemoteCache;
import org.infinispan.commons.configuration.ConfiguredBy;
import org.infinispan.commons.io.ByteBuffer;
import org.infinispan.commons.util.IntSet;
import org.infinispan.commons.util.IntSets;
import org.infinispan.marshall.persistence.PersistenceMarshaller;
import org.infinispan.metadata.Metadata;
import org.infinispan.persistence.spi.InitializationContext;
import org.infinispan.persistence.spi.MarshallableEntry;
import org.infinispan.persistence.spi.MarshallableEntryFactory;
import org.infinispan.persistence.spi.MarshalledValue;
import org.infinispan.persistence.spi.NonBlockingStore;
import org.infinispan.persistence.spi.PersistenceException;
import org.infinispan.util.concurrent.BlockingManager;
import org.jboss.as.clustering.infinispan.logging.InfinispanLogger;
import org.reactivestreams.Publisher;
import org.wildfly.clustering.infinispan.client.RemoteCacheContainer;
import org.wildfly.common.function.Functions;

/**
 * Variation of {@link org.infinispan.persistence.remote.RemoteStore} configured with a started container-managed {@link RemoteCacheContainer} instance.
 * Remote caches are auto-created on the remote server if supported by the protocol.
 * Supports segmentation by using a separate remote cache per segment.
 *
 * @author Radoslav Husar
 * @author Paul Ferraro
 */
@ConfiguredBy(HotRodStoreConfiguration.class)
public class HotRodStore<K, V> implements NonBlockingStore<K, V> {
    private static final String DEFAULT_CONFIGURATION = """
{
    "distributed-cache": {
        "mode" : "SYNC",
        "transaction" : {
            "mode" : "NON_XA",
            "locking" : "PESSIMISTIC"
        }
    }
}""";

    private volatile RemoteCacheContainer container;
    private volatile AtomicReferenceArray<RemoteCache<Object, MarshalledValue>> caches;
    private volatile BlockingManager blockingManager;
    private volatile Executor executor;
    private volatile PersistenceMarshaller marshaller;
    private volatile MarshallableEntryFactory<K, V> entryFactory;
    private volatile int batchSize;
    private volatile String cacheName;
    private volatile int segments;

    @Override
    public CompletionStage<Void> start(InitializationContext context) {
        Cache<?, ?> cache = context.getCache();
        HotRodStoreConfiguration configuration = context.getConfiguration();
        if (configuration.preload()) {
            throw new IllegalStateException();
        }

        this.container = configuration.remoteCacheContainer();
        this.cacheName = cache.getName();
        this.blockingManager = context.getBlockingManager();
        this.executor = context.getNonBlockingExecutor();
        this.batchSize = configuration.maxBatchSize();
        this.marshaller = context.getPersistenceMarshaller();
        this.entryFactory = context.getMarshallableEntryFactory();

        String template = configuration.cacheConfiguration();
        Consumer<RemoteCacheConfigurationBuilder> configurator = new Consumer<>() {
            @Override
            public void accept(RemoteCacheConfigurationBuilder builder) {
                builder.forceReturnValues(false).transactionMode(TransactionMode.NONE).nearCacheMode(NearCacheMode.DISABLED);
                if (template != null) {
                    builder.templateName(template);
                } else {
                    builder.configuration(DEFAULT_CONFIGURATION);
                }
            }
        };

        this.segments = configuration.segmented() && (cache.getAdvancedCache().getDistributionManager() != null) ? cache.getCacheConfiguration().clustering().hash().numSegments() : 1;

        this.caches = new AtomicReferenceArray<>(this.segments);
        for (int i = 0; i < this.segments; ++i) {
            this.container.getConfiguration().addRemoteCache(this.segmentCacheName(i), configurator);
        }
        // When unshared, add/removeSegments(...) will be triggered as needed.
        return configuration.shared() ? this.addSegments(IntSets.immutableRangeSet(this.segments)) : CompletableFuture.completedStage(null);
    }

    @Override
    public CompletionStage<Void> stop() {
        CompletableFuture<Void> result = CompletableFuture.completedFuture(null);
        for (int i = 0; i < this.caches.length(); ++i) {
            RemoteCache<Object, MarshalledValue> cache = this.caches.get(i);
            if (cache != null) {
                result = CompletableFuture.allOf(result, this.blockingManager.runBlocking(() -> {
                    cache.stop();
                    cache.getRemoteCacheContainer().getConfiguration().removeRemoteCache(cache.getName());
                }, "hotrod-store-stop").toCompletableFuture());
            }
        }
        return result;
    }

    private String segmentCacheName(int segment) {
        return (this.segments > 1) ? this.cacheName + '.' + segment : this.cacheName;
    }

    private int segmentIndex(int segment) {
        return (this.segments > 1) ? segment : 0;
    }

    private RemoteCache<Object, MarshalledValue> segmentCache(int segment) {
        return this.caches.get(this.segmentIndex(segment));
    }

    private PrimitiveIterator.OfInt segmentIterator(IntSet segments) {
        return (this.segments > 1) ? segments.iterator() : IntStream.of(0).iterator();
    }

    @Override
    public Set<Characteristic> characteristics() {
        // N.B.  we must return a new, mutable instance, since this value may be modified by PersistenceManagerImpl
        return EnumSet.of(Characteristic.SHAREABLE, Characteristic.BULK_READ, Characteristic.EXPIRATION, Characteristic.SEGMENTABLE);
    }

    @Override
    public CompletionStage<MarshallableEntry<K, V>> load(int segment, Object key) {
        RemoteCache<Object, MarshalledValue> cache = this.segmentCache(segment);
        if (cache == null) return CompletableFuture.completedStage(null);
        try {
            return cache.getAsync(key)
                    .thenApplyAsync(value -> (value != null) ? this.entryFactory.create(key, value) : null, this.executor);
        } catch (PersistenceException e) {
            return CompletableFuture.failedStage(e);
        }
    }

    @Override
    public CompletionStage<Void> write(int segment, MarshallableEntry<? extends K, ? extends V> entry) {
        RemoteCache<Object, MarshalledValue> cache = this.segmentCache(segment);
        if (cache == null) return CompletableFuture.completedStage(null);
        Metadata metadata = entry.getMetadata();
        long lifespan = (metadata != null) ? metadata.lifespan() : -1L;
        long maxIdle = (metadata != null) ? metadata.maxIdle() : -1L;
        try {
            return cache.withFlags(Flag.SKIP_LISTENER_NOTIFICATION).putAsync(entry.getKey(), entry.getMarshalledValue(), lifespan, TimeUnit.MILLISECONDS, maxIdle, TimeUnit.MILLISECONDS)
                    .thenAcceptAsync(Functions.discardingConsumer(), this.executor);
        } catch (PersistenceException e) {
            return CompletableFuture.failedStage(e);
        }
    }

    @Override
    public CompletionStage<Boolean> delete(int segment, Object key) {
        RemoteCache<Object, MarshalledValue> cache = this.segmentCache(segment);
        if (cache == null) return CompletableFuture.completedStage(null);
        try {
            return cache.withFlags(Flag.FORCE_RETURN_VALUE, Flag.SKIP_LISTENER_NOTIFICATION).removeAsync(key)
                    .thenApplyAsync(Objects::nonNull, this.executor);
        } catch (PersistenceException e) {
            return CompletableFuture.failedStage(e);
        }
    }

    @Override
    public CompletionStage<Void> batch(int publisherCount, Publisher<SegmentedPublisher<Object>> removePublisher, Publisher<SegmentedPublisher<MarshallableEntry<K, V>>> writePublisher) {
        Completable removeCompletable = Flowable.fromPublisher(removePublisher)
                .flatMap(sp -> Flowable.fromPublisher(sp).map(key -> Map.entry(key, sp.getSegment())), publisherCount)
                .flatMapCompletable(this::remove, false, this.batchSize);
        Completable writeCompletable = Flowable.fromPublisher(writePublisher)
                .flatMap(sp -> Flowable.fromPublisher(sp).map(entry -> Map.entry(entry, sp.getSegment())), publisherCount)
                .flatMapCompletable(this::write, false, this.batchSize);
        return removeCompletable.mergeWith(writeCompletable)
                .observeOn(Schedulers.from(this.executor))
                .toCompletionStage(null);
    }

    private Completable write(Map.Entry<MarshallableEntry<K, V>, Integer> entry) {
        return Completable.fromCompletionStage(this.write(entry.getValue(), entry.getKey()));
    }

    private Completable remove(Map.Entry<Object, Integer> entry) {
        return Completable.fromCompletionStage(this.delete(entry.getValue(), entry.getKey()));
    }

    @Override
    public Flowable<K> publishKeys(IntSet segments, Predicate<? super K> filter) {
        Stream<K> keys = Stream.empty();
        PrimitiveIterator.OfInt iterator = this.segmentIterator(segments);
        try {
            while (iterator.hasNext()) {
                int segment = iterator.nextInt();
                RemoteCache<Object, MarshalledValue> cache = this.segmentCache(segment);
                if (cache != null) {
                    keys = Stream.concat(keys, cache.keySet().stream().map(key -> (K) key));
                }
            }
            Stream<K> filteredKeys = (filter != null) ? keys.filter(filter) : keys;
            return Flowable.fromStream(filteredKeys).observeOn(Schedulers.from(this.executor)).doFinally(filteredKeys::close);
        } catch (PersistenceException e) {
            return Flowable.fromCompletionStage(CompletableFuture.failedStage(e));
        }
    }

    @Override
    public Publisher<MarshallableEntry<K, V>> publishEntries(IntSet segments, Predicate<? super K> filter, boolean includeValues) {
        return includeValues ? this.publishEntries(segments, filter) : this.publishKeys(segments, filter).map(this.entryFactory::create);
    }

    private Flowable<MarshallableEntry<K, V>> publishEntries(IntSet segments, Predicate<? super K> filter) {
        Stream<MarshallableEntry<K, V>> entries = Stream.empty();
        PrimitiveIterator.OfInt iterator = this.segmentIterator(segments);
        try {
            while (iterator.hasNext()) {
                int segment = iterator.nextInt();
                RemoteCache<Object, MarshalledValue> cache = this.segmentCache(segment);
                if (cache != null) {
                    entries = Stream.concat(entries, cache.entrySet().stream().map(entry -> this.entryFactory.create(entry.getKey(), entry.getValue())));
                }
            }
            Stream<MarshallableEntry<K, V>> filteredEntries = (filter != null) ? entries.filter(entry -> filter.test(entry.getKey())) : entries;
            return Flowable.fromStream(filteredEntries).observeOn(Schedulers.from(this.executor)).doFinally(filteredEntries::close);
        } catch (PersistenceException e) {
            return Flowable.fromCompletionStage(CompletableFuture.failedStage(e));
        }
    }

    @Override
    public CompletionStage<Void> clear() {
        CompletableFuture<Void> result = CompletableFuture.completedFuture(null);
        for (int i = 0; i < this.caches.length(); ++i) {
            RemoteCache<Object, MarshalledValue> cache = this.caches.get(i);
            if (cache != null) {
                result = CompletableFuture.allOf(result, cache.withFlags(Flag.SKIP_LISTENER_NOTIFICATION).clearAsync().thenApplyAsync(Function.identity(), this.executor));
            }
        }
        return result;
    }

    @Override
    public CompletionStage<Boolean> containsKey(int segment, Object key) {
        RemoteCache<Object, MarshalledValue> cache = this.segmentCache(segment);
        if (cache == null) return CompletableFuture.completedStage(false);
        try {
            return cache.containsKeyAsync(key).thenApplyAsync(Function.identity(), this.executor);
        } catch (PersistenceException e) {
            return CompletableFuture.failedStage(e);
        }
    }

    @Override
    public CompletionStage<Boolean> isAvailable() {
        InternalRemoteCache<?, ?> internalCache = (InternalRemoteCache<?, ?>) this.segmentCache(0);
        return internalCache.ping().handleAsync((v, t) -> t == null && v.isSuccess(), this.executor);
    }

    @Override
    public CompletionStage<Long> size(IntSet segments) {
        CompletableFuture<Long> result = CompletableFuture.completedFuture(0L);
        PrimitiveIterator.OfInt iterator = this.segmentIterator(segments);
        while (iterator.hasNext()) {
            int segment = iterator.nextInt();
            RemoteCache<Object, MarshalledValue> cache = this.caches.get(segment);
            result = result.thenCombineAsync(cache.sizeAsync(), Long::sum, this.executor);
        }
        return result;
    }

    @Override
    public CompletionStage<Void> addSegments(IntSet segments) {
        CompletableFuture<Void> result = CompletableFuture.completedFuture(null);
        PrimitiveIterator.OfInt iterator = segments.iterator();
        while (iterator.hasNext()) {
            int segment = iterator.nextInt();
            String cacheName = this.segmentCacheName(segment);
            int index = this.segmentIndex(segment);
            result = CompletableFuture.allOf(result, this.blockingManager.runBlocking(() -> {
                RemoteCache<ByteBuffer, ByteBuffer> cache = this.container.getCache(cacheName);

                if (cache == null) {
                    // Administration support was introduced in protocol version 2.7
                    throw InfinispanLogger.ROOT_LOGGER.remoteCacheMustBeDefined(this.container.getConfiguration().version().toString(), cacheName);
                }

                cache.start();

                this.caches.set(index, cache.withDataFormat(DataFormat.builder().keyMarshaller(this.marshaller).valueMarshaller(this.marshaller).build()));
            }, "hotrod-store-add-segments").toCompletableFuture());
        }
        return result;
    }

    @Override
    public CompletionStage<Void> removeSegments(IntSet segments) {
        CompletableFuture<Void> result = CompletableFuture.completedFuture(null);
        PrimitiveIterator.OfInt iterator = segments.iterator();
        while (iterator.hasNext()) {
            int segment = iterator.nextInt();
            RemoteCache<Object, MarshalledValue> cache = this.caches.get(segment);
            if (cache != null) {
                this.caches.set(segment, null);
                result = CompletableFuture.allOf(result, this.blockingManager.thenRunBlocking(cache.clearAsync().thenAcceptAsync(Functions.discardingConsumer(), this.executor), cache::stop, "hotrod-store-remove-segments").toCompletableFuture());
            }
        }
        return result;
    }

    @Override
    public Publisher<MarshallableEntry<K, V>> purgeExpired() {
        return Flowable.empty();
    }
}
