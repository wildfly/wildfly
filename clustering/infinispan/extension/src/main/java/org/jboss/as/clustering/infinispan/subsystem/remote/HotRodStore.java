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

package org.jboss.as.clustering.infinispan.subsystem.remote;

import java.io.IOException;
import java.util.AbstractMap;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.Executor;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import org.infinispan.client.hotrod.Flag;
import org.infinispan.client.hotrod.MetadataValue;
import org.infinispan.client.hotrod.ProtocolVersion;
import org.infinispan.client.hotrod.RemoteCache;
import org.infinispan.client.hotrod.RemoteCacheManager;
import org.infinispan.client.hotrod.exceptions.HotRodClientException;
import org.infinispan.client.hotrod.impl.operations.OperationsFactory;
import org.infinispan.client.hotrod.impl.Util;
import org.infinispan.commons.configuration.ConfiguredBy;
import org.infinispan.commons.io.ByteBuffer;
import org.infinispan.commons.persistence.Store;
import org.infinispan.commons.util.CloseableIterator;
import org.infinispan.commons.util.IntSet;
import org.infinispan.commons.util.IteratorMapper;
import org.infinispan.persistence.PersistenceUtil;
import org.infinispan.persistence.spi.AdvancedLoadWriteStore;
import org.infinispan.persistence.spi.InitializationContext;
import org.infinispan.persistence.spi.MarshallableEntry;
import org.infinispan.persistence.spi.MarshallableEntryFactory;
import org.infinispan.persistence.spi.PersistenceException;
import org.infinispan.persistence.spi.SegmentedAdvancedLoadWriteStore;
import org.jboss.as.clustering.infinispan.InfinispanLogger;
import org.reactivestreams.Publisher;
import org.wildfly.clustering.infinispan.client.RemoteCacheContainer;
import org.wildfly.clustering.infinispan.client.near.EmptyNearCacheService;

import io.reactivex.Flowable;
import io.reactivex.functions.Action;
import io.reactivex.functions.Consumer;
import io.reactivex.functions.Function;
import io.reactivex.internal.functions.Functions;

/**
 * Simple implementation of Infinispan {@link AdvancedLoadWriteStore} configured with a started container-managed {@link RemoteCacheContainer}
 * instance. Does not perform wrapping entries in Infinispan internal objects, this stores "raw" values.
 *
 * @author Radoslav Husar
 */
@Store(shared = true)
@ConfiguredBy(HotRodStoreConfiguration.class)
public class HotRodStore<K, V> implements SegmentedAdvancedLoadWriteStore<K, V>, Function<CloseableIterator<byte[]>, Publisher<K>>, Consumer<K> {

    private InitializationContext ctx;
    private MarshallableEntryFactory<K,V> entryFactory;
    private OperationsFactory operationsFactory;

    private RemoteCache<byte[], byte[]> remoteCache;
    private int maxBatchSize;

    @Override
    public void init(InitializationContext ctx) {
        this.ctx = ctx;

        HotRodStoreConfiguration configuration = ctx.getConfiguration();
        RemoteCacheContainer remoteCacheContainer = configuration.attributes().attribute(HotRodStoreConfiguration.REMOTE_CACHE_CONTAINER).get();
        String cacheConfiguration = configuration.attributes().attribute(HotRodStoreConfiguration.CACHE_CONFIGURATION).get();
        String cacheName = ctx.getCache().getName();
        this.maxBatchSize = configuration.maxBatchSize();
        this.entryFactory = ctx.getMarshallableEntryFactory();

        try {
            ProtocolVersion protocolVersion = remoteCacheContainer.getConfiguration().version();

            // Administration support was introduced in protocol version 2.7
            if (protocolVersion.compareTo(ProtocolVersion.PROTOCOL_VERSION_27) < 0) {
                // Auto-disable near cache
                try (RemoteCacheContainer.NearCacheRegistration registration = remoteCacheContainer.registerNearCacheFactory(cacheName, EmptyNearCacheService::new)) {
                    this.remoteCache = remoteCacheContainer.getCache(cacheName, false);
                }
                if (this.remoteCache == null) {
                    throw InfinispanLogger.ROOT_LOGGER.remoteCacheMustBeDefined(protocolVersion.toString(), cacheName);
                }
            } else {
                InfinispanLogger.ROOT_LOGGER.remoteCacheCreated(cacheName, cacheConfiguration);
                // Auto-disable near cache
                try (RemoteCacheContainer.NearCacheRegistration registration = remoteCacheContainer.registerNearCacheFactory(cacheName, EmptyNearCacheService::new)) {
                    this.remoteCache = remoteCacheContainer.administration().getOrCreateCache(cacheName, cacheConfiguration);
                }
            }

            RemoteCacheManager manager = this.remoteCache.getRemoteCacheManager();
            this.operationsFactory = new OperationsFactory(manager.getChannelFactory(), manager.getCodec(), null, manager.getConfiguration());
        } catch (HotRodClientException ex) {
            throw new PersistenceException(ex);
        }
    }

    @Override
    public void start() {
        this.remoteCache.start();
    }

    @Override
    public void stop() {
        this.remoteCache.stop();
    }

    @Override
    public MarshallableEntry<K, V> loadEntry(Object key) throws PersistenceException {
        MetadataValue<byte[]> value = this.remoteCache.getWithMetadata(this.marshall(key));
        if (value == null) {
            return null;
        }
        Map.Entry<ByteBuffer, ByteBuffer> entry = this.unmarshallEntry(value.getValue());
        return this.entryFactory.create(key, entry.getKey(), entry.getValue(), value.getCreated(), value.getLastUsed());
    }

    private MarshallableEntry<K, V> asMarshalledEntry(Object key) {
        return this.entryFactory.create(key, null);
    }

    @Override
    public void write(MarshallableEntry<? extends K, ? extends V> entry) {
        this.remoteCache.put(this.marshall(entry.getKey()), this.marshall(entry));
    }

    @Override
    public CompletionStage<Void> bulkUpdate(Publisher<MarshallableEntry<? extends K, ? extends V>> marshalledEntries) {
        CompletableFuture<Void> future = new CompletableFuture<>();
        Action completeAction = new Action() {
            @Override
            public void run() {
                future.complete(null);
            }
        };
        Flowable.fromPublisher(marshalledEntries)
                .buffer(this.maxBatchSize)
                .doOnNext(entries -> this.remoteCache.putAll(entries.stream().collect(Collectors.toMap(this::marshallKey, this::marshallEntry))))
                .doOnError(PersistenceException::new)
                .subscribe(Functions.emptyConsumer(), future::completeExceptionally, completeAction);
        return future;
    }

    @Override
    public boolean contains(Object key) {
        return this.remoteCache.containsKey(this.marshall(key));
    }

    @Override
    public boolean delete(Object key) {
        return this.remoteCache.withFlags(Flag.FORCE_RETURN_VALUE).remove(this.marshall(key)) != null;
    }

    @Override
    public Flowable<K> publishKeys(Predicate<? super K> filter) {
        return this.publishKeys(null, filter);
    }

    @Override
    public Flowable<K> publishKeys(IntSet segments, Predicate<? super K> filter) {
        Flowable<K> keys = Flowable.using(Functions.justCallable(this.remoteCache.keySet(segments).iterator()), this, CloseableIterator::close);
        return (filter != null) ? keys.filter(filter::test) : keys;
    }

    @Override
    public Publisher<K> apply(CloseableIterator<byte[]> iterator) {
        return Flowable.fromIterable(new SimpleIterable<>(new IteratorMapper<>(iterator, this::unmarshallKey)));
    }

    @Override
    public Publisher<MarshallableEntry<K, V>> entryPublisher(IntSet segments, Predicate<? super K> filter, boolean fetchValue, boolean fetchMetadata) {
        Flowable<K> keys = this.publishKeys(filter);
        return (fetchValue || fetchMetadata) ? keys.map(this::loadEntry) : keys.map(this::asMarshalledEntry);
    }

    @Override
    public int size() {
        return this.remoteCache.size();
    }

    @Override
    public void clear() {
        this.remoteCache.clear();
    }

    @Override
    public void purge(Executor threadPool, PurgeListener<? super K> listener) {
        // Ignore
    }

    private byte[] marshallKey(MarshallableEntry<? extends K, ? extends V> entry) {
        return this.marshall(entry.getKey());
    }

    private byte[] marshallEntry(MarshallableEntry<? extends K, ? extends V> entry) {
        return this.marshall(new AbstractMap.SimpleImmutableEntry<>(entry.getValueBytes(), entry.getMetadataBytes()));
    }

    private byte[] marshall(Object object) {
        try {
            return this.ctx.getPersistenceMarshaller().objectToByteBuffer(object);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new PersistenceException(e);
        } catch (IOException e) {
            throw new PersistenceException(e);
        }
    }

    @SuppressWarnings("unchecked")
    private K unmarshallKey(byte[] bytes) {
        return (K) this.unmarshall(bytes);
    }

    @SuppressWarnings("unchecked")
    private Map.Entry<ByteBuffer, ByteBuffer> unmarshallEntry(byte[] bytes) {
        return (Map.Entry<ByteBuffer, ByteBuffer>) this.unmarshall(bytes);
    }

    private Object unmarshall(byte[] bytes) {
        try {
            return this.ctx.getPersistenceMarshaller().objectFromByteBuffer(bytes);
        } catch (IOException | ClassNotFoundException e) {
            throw new PersistenceException(e);
        }
    }

    @Override
    public boolean isAvailable() {
        return Util.await(this.operationsFactory.newFaultTolerantPingOperation().execute()).isSuccess();
    }

    @Override
    public int size(IntSet segments) {
        return PersistenceUtil.count(this, segments);
    }

    @Override
    public void clear(IntSet segments) {
        this.publishKeys(segments, null).blockingForEach(this);
    }

    @Override
    public void accept(K key) {
        this.remoteCache.remove(this.marshall(key));
    }

    private static class SimpleIterable<T> implements Iterable<T> {
        private final Iterator<T> iterator;

        SimpleIterable(Iterator<T> iterator) {
            this.iterator = iterator;
        }

        @Override
        public Iterator<T> iterator() {
            return this.iterator;
        }
    }
}
