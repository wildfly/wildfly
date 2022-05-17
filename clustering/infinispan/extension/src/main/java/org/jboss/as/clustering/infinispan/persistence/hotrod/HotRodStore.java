/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2018, Red Hat, Inc., and individual contributors
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

package org.jboss.as.clustering.infinispan.persistence.hotrod;

import java.io.IOException;
import java.util.EnumSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.PrimitiveIterator;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReferenceArray;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.core.Flowable;

import org.infinispan.Cache;
import org.infinispan.client.hotrod.DefaultTemplate;
import org.infinispan.client.hotrod.Flag;
import org.infinispan.client.hotrod.RemoteCache;
import org.infinispan.client.hotrod.configuration.NearCacheMode;
import org.infinispan.client.hotrod.configuration.RemoteCacheConfigurationBuilder;
import org.infinispan.client.hotrod.configuration.TransactionMode;
import org.infinispan.commons.configuration.ConfiguredBy;
import org.infinispan.commons.io.ByteBuffer;
import org.infinispan.commons.util.IntSet;
import org.infinispan.commons.util.IntSets;
import org.infinispan.commons.util.concurrent.CompletableFutures;
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
    private static final Set<Characteristic> CHARACTERISTICS = EnumSet.of(Characteristic.SHAREABLE, Characteristic.BULK_READ, Characteristic.EXPIRATION, Characteristic.SEGMENTABLE);

    private volatile RemoteCacheContainer container;
    private volatile AtomicReferenceArray<RemoteCache<ByteBuffer, ByteBuffer>> caches;
    private volatile BlockingManager blockingManager;
    private volatile PersistenceMarshaller marshaller;
    private volatile MarshallableEntryFactory<K,V> entryFactory;
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
        this.batchSize = configuration.maxBatchSize();
        this.marshaller = context.getPersistenceMarshaller();
        this.entryFactory = context.getMarshallableEntryFactory();

        String template = configuration.cacheConfiguration();
        String templateName = (template != null) ? template : DefaultTemplate.DIST_SYNC.getTemplateName();
        Consumer<RemoteCacheConfigurationBuilder> configurator = new Consumer<>() {
            @Override
            public void accept(RemoteCacheConfigurationBuilder builder) {
                builder.forceReturnValues(false).transactionMode(TransactionMode.NONE).nearCacheMode(NearCacheMode.DISABLED).templateName(templateName);
            }
        };

        this.segments = configuration.segmented() && (cache.getAdvancedCache().getDistributionManager() != null) ? cache.getCacheConfiguration().clustering().hash().numSegments() : 1;

        this.caches = new AtomicReferenceArray<>(this.segments);
        for (int i = 0; i < this.segments; ++i) {
            this.container.getConfiguration().addRemoteCache(this.segmentCacheName(i), configurator);
        }
        // When unshared, add/removeSegments(...) will be triggered as needed.
        return configuration.shared() ? this.addSegments(IntSets.immutableRangeSet(this.segments)) : CompletableFutures.completedNull();
    }

    @Override
    public CompletionStage<Void> stop() {
        CompletableFuture<Void> result = CompletableFutures.completedNull();
        for (int i = 0; i < this.caches.length(); ++i) {
            RemoteCache<ByteBuffer, ByteBuffer> cache = this.caches.get(i);
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

    private RemoteCache<ByteBuffer, ByteBuffer> segmentCache(int segment) {
        return this.caches.get(this.segmentIndex(segment));
    }

    private PrimitiveIterator.OfInt segmentIterator(IntSet segments) {
        return (this.segments > 1) ? segments.iterator() : IntStream.of(0).iterator();
    }

    @Override
    public Set<Characteristic> characteristics() {
        return CHARACTERISTICS;
    }

    @Override
    public CompletionStage<MarshallableEntry<K, V>> load(int segment, Object key) {
        RemoteCache<ByteBuffer, ByteBuffer> cache = this.segmentCache(segment);
        if (cache == null) return CompletableFutures.completedNull();
        try {
            return cache.getAsync(this.marshalKey(key)).thenApply(value -> (value != null) ? this.entryFactory.create(key, this.unmarshalValue(value)) : null);
        } catch (PersistenceException e) {
            return CompletableFutures.completedExceptionFuture(e);
        }
    }

    @Override
    public CompletionStage<Void> write(int segment, MarshallableEntry<? extends K, ? extends V> entry) {
        RemoteCache<ByteBuffer, ByteBuffer> cache = this.segmentCache(segment);
        if (cache == null) return CompletableFutures.completedNull();
        Metadata metadata = entry.getMetadata();
        try {
            return cache.putAsync(entry.getKeyBytes(), this.marshalValue(entry.getMarshalledValue()), metadata.lifespan(), TimeUnit.MILLISECONDS, metadata.maxIdle(), TimeUnit.MILLISECONDS).thenAccept(Functions.discardingConsumer());
        } catch (PersistenceException e) {
            return CompletableFutures.completedExceptionFuture(e);
        }
    }

    @Override
    public CompletionStage<Boolean> delete(int segment, Object key) {
        RemoteCache<ByteBuffer, ByteBuffer> cache = this.segmentCache(segment);
        if (cache == null) return CompletableFutures.completedNull();
        try {
            return cache.withFlags(Flag.FORCE_RETURN_VALUE).removeAsync(this.marshalKey(key)).thenApply(Objects::nonNull);
        } catch (PersistenceException e) {
            return CompletableFutures.completedExceptionFuture(e);
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
        return removeCompletable.mergeWith(writeCompletable).toCompletionStage(null);
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
                RemoteCache<ByteBuffer, ByteBuffer> cache = this.segmentCache(segment);
                if (cache != null) {
                    keys = Stream.concat(keys, cache.keySet().stream().map(this::unmarshalKey));
                }
            }
            Stream<K> filteredKeys = (filter != null) ? keys.filter(filter) : keys;
            return Flowable.fromPublisher(this.blockingManager.blockingPublisher(Flowable.defer(() -> Flowable.fromStream(filteredKeys).doFinally(filteredKeys::close))));
        } catch (PersistenceException e) {
            return Flowable.fromCompletionStage(CompletableFutures.completedExceptionFuture(e));
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
                RemoteCache<ByteBuffer, ByteBuffer> cache = this.segmentCache(segment);
                if (cache != null) {
                    entries = Stream.concat(entries, cache.entrySet().stream().map(this::unmarshalEntry));
                }
            }
            Stream<MarshallableEntry<K, V>> filteredEntries = (filter != null) ? entries.filter(entry -> filter.test(entry.getKey())) : entries;
            return Flowable.fromPublisher(this.blockingManager.blockingPublisher(Flowable.defer(() -> Flowable.fromStream(filteredEntries).doFinally(filteredEntries::close))));
        } catch (PersistenceException e) {
            return Flowable.fromCompletionStage(CompletableFutures.completedExceptionFuture(e));
        }
    }

    @Override
    public CompletionStage<Void> clear() {
        CompletableFuture<Void> result = CompletableFutures.completedNull();
        for (int i = 0; i < this.caches.length(); ++i) {
            RemoteCache<ByteBuffer, ByteBuffer> cache = this.caches.get(i);
            if (cache != null) {
                result = CompletableFuture.allOf(result, cache.clearAsync());
            }
        }
        return result;
    }

    @Override
    public CompletionStage<Boolean> containsKey(int segment, Object key) {
        RemoteCache<ByteBuffer, ByteBuffer> cache = this.segmentCache(segment);
        if (cache == null) return CompletableFutures.completedFalse();
        try {
            return cache.containsKeyAsync(this.marshalKey(key));
        } catch (PersistenceException e) {
            return CompletableFutures.completedExceptionFuture(e);
        }
    }

    @Override
    public CompletionStage<Boolean> isAvailable() {
        return this.container.isAvailable();
    }

    @Override
    public CompletionStage<Long> size(IntSet segments) {
        CompletableFuture<Long> result = CompletableFuture.completedFuture(0L);
        PrimitiveIterator.OfInt iterator = this.segmentIterator(segments);
        while (iterator.hasNext()) {
            int segment = iterator.nextInt();
            RemoteCache<ByteBuffer, ByteBuffer> cache = this.caches.get(segment);
            result = result.thenCombine(cache.sizeAsync(), Long::sum);
        }
        return result;
    }

    @Override
    public CompletionStage<Void> addSegments(IntSet segments) {
        CompletableFuture<Void> result = CompletableFutures.completedNull();
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

                this.caches.set(index, cache);
            }, "hotrod-store-add-segments").toCompletableFuture());
        }
        return result;
    }

    @Override
    public CompletionStage<Void> removeSegments(IntSet segments) {
        CompletableFuture<Void> result = CompletableFutures.completedNull();
        PrimitiveIterator.OfInt iterator = segments.iterator();
        while (iterator.hasNext()) {
            int segment = iterator.nextInt();
            RemoteCache<ByteBuffer, ByteBuffer> cache = this.caches.get(segment);
            if (cache != null) {
                this.caches.set(segment, null);
                result = CompletableFuture.allOf(result, cache.clearAsync().thenRun(cache::stop).toCompletableFuture());
            }
        }
        return result;
    }

    @Override
    public Publisher<MarshallableEntry<K, V>> purgeExpired() {
        return Flowable.empty();
    }

    private ByteBuffer marshalKey(Object key) {
        return this.entryFactory.create(key).getKeyBytes();
    }

    @SuppressWarnings("unchecked")
    private K unmarshalKey(ByteBuffer key) {
        try {
            return (K) this.marshaller.objectFromByteBuffer(key.getBuf(), key.getOffset(), key.getLength());
        } catch (IOException | ClassNotFoundException e) {
            throw new PersistenceException(e);
        }
    }

    private MarshallableEntry<K, V> unmarshalEntry(Map.Entry<ByteBuffer, ByteBuffer> entry) {
        return this.entryFactory.create(this.unmarshalKey(entry.getKey()), this.unmarshalValue(entry.getValue()));
    }

    private ByteBuffer marshalValue(MarshalledValue value) {
        try {
            return this.marshaller.objectToBuffer(value);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new PersistenceException(e);
        } catch (IOException e) {
            throw new PersistenceException(e);
        }
    }

    private MarshalledValue unmarshalValue(ByteBuffer buffer) {
        if (buffer == null) return null;
        try {
            return (MarshalledValue) this.marshaller.objectFromByteBuffer(buffer.getBuf(), buffer.getOffset(), buffer.getLength());
        } catch (IOException | ClassNotFoundException e) {
            throw new PersistenceException(e);
        }
    }
}
