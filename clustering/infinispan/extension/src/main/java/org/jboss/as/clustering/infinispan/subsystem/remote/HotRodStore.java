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
import java.util.EnumSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.CompletionStage;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.stream.Stream;

import org.infinispan.client.hotrod.DefaultTemplate;
import org.infinispan.client.hotrod.Flag;
import org.infinispan.client.hotrod.RemoteCache;
import org.infinispan.client.hotrod.RemoteCacheManager;
import org.infinispan.client.hotrod.configuration.NearCacheMode;
import org.infinispan.client.hotrod.configuration.RemoteCacheConfigurationBuilder;
import org.infinispan.client.hotrod.configuration.TransactionMode;
import org.infinispan.client.hotrod.exceptions.HotRodClientException;
import org.infinispan.client.hotrod.impl.operations.OperationsFactory;
import org.infinispan.client.hotrod.impl.operations.PingResponse;
import org.infinispan.commons.configuration.ConfiguredBy;
import org.infinispan.commons.io.ByteBuffer;
import org.infinispan.commons.util.IntSet;
import org.infinispan.marshall.persistence.PersistenceMarshaller;
import org.infinispan.persistence.spi.InitializationContext;
import org.infinispan.persistence.spi.MarshallableEntry;
import org.infinispan.persistence.spi.MarshallableEntryFactory;
import org.infinispan.persistence.spi.MarshalledValue;
import org.infinispan.persistence.spi.NonBlockingStore;
import org.infinispan.persistence.spi.PersistenceException;
import org.infinispan.util.concurrent.BlockingManager;
import org.infinispan.util.concurrent.CompletableFutures;
import org.jboss.as.clustering.infinispan.InfinispanLogger;
import org.reactivestreams.Publisher;
import org.wildfly.clustering.infinispan.client.RemoteCacheContainer;
import org.wildfly.common.function.Functions;

import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.core.Flowable;

/**
 * Implementation of Infinispan {@link AdvancedLoadWriteStore} configured with a started container-managed {@link RemoteCacheContainer} instance.
 * Remote caches are auto-created on the remote server if supported by the protocol.
 *
 * @author Radoslav Husar
 * @author Paul Ferraro
 */
@ConfiguredBy(HotRodStoreConfiguration.class)
public class HotRodStore<K, V> implements NonBlockingStore<K, V> {
    private static final Set<Characteristic> CHARACTERISTICS = EnumSet.of(Characteristic.SHAREABLE, Characteristic.BULK_READ);

    private RemoteCache<ByteBuffer, ByteBuffer> cache;
    private BlockingManager blockingManager;
    private OperationsFactory operationsFactory;
    private PersistenceMarshaller marshaller;
    private MarshallableEntryFactory<K,V> entryFactory;
    private int batchSize;

    @Override
    public CompletionStage<Void> start(InitializationContext context) {
        this.blockingManager = context.getBlockingManager();
        HotRodStoreConfiguration configuration = context.getConfiguration();

        RemoteCacheContainer container = configuration.remoteCacheContainer();
        String cacheConfiguration = configuration.cacheConfiguration();
        String cacheName = context.getCache().getName();
        this.batchSize = configuration.maxBatchSize();

        this.marshaller = context.getPersistenceMarshaller();
        this.entryFactory = context.getMarshallableEntryFactory();

        String templateName = (cacheConfiguration != null) ? cacheConfiguration : DefaultTemplate.DIST_SYNC.getTemplateName();
        Consumer<RemoteCacheConfigurationBuilder> configurator = new Consumer<RemoteCacheConfigurationBuilder>() {
            @Override
            public void accept(RemoteCacheConfigurationBuilder builder) {
                builder.forceReturnValues(false).transactionMode(TransactionMode.NONE).nearCacheMode(NearCacheMode.DISABLED).templateName(templateName);
            }
        };
        container.getConfiguration().addRemoteCache(cacheName, configurator);

        Runnable task = new Runnable() {
            @Override
            public void run() {
                try {
                    HotRodStore.this.setRemoteCache(container, cacheName);
                } catch (HotRodClientException ex) {
                    throw new PersistenceException(ex);
                }
            }
        };
        return this.blockingManager.runBlocking(task, "hotrod-store-start");
    }

    synchronized void setRemoteCache(RemoteCacheContainer container, String cacheName) {
        this.cache = container.getCache(cacheName);

        if (this.cache == null) {
            // Administration support was introduced in protocol version 2.7
            throw InfinispanLogger.ROOT_LOGGER.remoteCacheMustBeDefined(container.getConfiguration().version().toString(), cacheName);
        }

        RemoteCacheManager manager = this.cache.getRemoteCacheManager();
        this.operationsFactory = new OperationsFactory(manager.getChannelFactory(), manager.getCodec(), null, manager.getConfiguration());

        this.cache.start();
    }

    @Override
    public CompletionStage<Void> stop() {
        return (this.cache != null) ? this.blockingManager.runBlocking(this.cache::stop, "hotrod-store-stop") : CompletableFutures.completedNull();
    }

    @Override
    public Set<Characteristic> characteristics() {
        return CHARACTERISTICS;
    }

    @Override
    public CompletionStage<MarshallableEntry<K, V>> load(int segment, Object key) {
        return this.cache.getAsync(this.marshalKey(key)).thenApply(value -> (value != null) ? this.entryFactory.create(key, this.unmarshalValue(value)) : null);
    }

    @Override
    public CompletionStage<Void> write(int segment, MarshallableEntry<? extends K, ? extends V> entry) {
        return this.cache.putAsync(entry.getKeyBytes(), this.marshalValue(entry.getMarshalledValue())).thenAccept(Functions.discardingConsumer());
    }

    @Override
    public CompletionStage<Boolean> delete(int segment, Object key) {
        return this.cache.withFlags(Flag.FORCE_RETURN_VALUE).removeAsync(this.marshalKey(key)).thenApply(Objects::nonNull);
    }

    @Override
    public CompletionStage<Void> batch(int publisherCount, Publisher<SegmentedPublisher<Object>> removePublisher, Publisher<SegmentedPublisher<MarshallableEntry<K, V>>> writePublisher) {
        Completable removeCompletable = Flowable.fromPublisher(removePublisher)
                .flatMap(sp -> Flowable.fromPublisher(sp), publisherCount)
                .flatMapCompletable(key -> Completable.fromCompletionStage(this.cache.removeAsync(this.marshalKey(key))), false, this.batchSize);
        Completable writeCompletable = Flowable.fromPublisher(writePublisher)
                .flatMap(sp -> Flowable.fromPublisher(sp), publisherCount)
                .flatMapCompletable(entry -> Completable.fromCompletionStage(this.cache.putAsync(entry.getKeyBytes(), this.marshalValue(entry.getMarshalledValue()))), false, this.batchSize);
        return removeCompletable.mergeWith(writeCompletable).toCompletionStage(null);
    }

    @Override
    public Flowable<K> publishKeys(IntSet segments, Predicate<? super K> filter) {
        Stream<K> keys = this.cache.keySet().stream().map(this::unmarshalKey);
        Stream<K> filteredKeys = (filter != null) ? keys.filter(filter) : keys;
        return Flowable.fromPublisher(this.blockingManager.blockingPublisher(Flowable.defer(() -> Flowable.fromStream(filteredKeys).doFinally(filteredKeys::close))));
    }

    @Override
    public Publisher<MarshallableEntry<K, V>> publishEntries(IntSet segments, Predicate<? super K> filter, boolean includeValues) {
        return includeValues ? this.publishEntries(segments, filter) : this.publishKeys(segments, filter).map(this.entryFactory::create);
    }

    private Flowable<MarshallableEntry<K, V>> publishEntries(IntSet segments, Predicate<? super K> filter) {
        Stream<MarshallableEntry<K, V>> entries = this.cache.entrySet().stream().map(this::unmarshalEntry);
        Stream<MarshallableEntry<K, V>> filteredEntries = (filter != null) ? entries.filter(entry -> filter.test(entry.getKey())) : entries;
        return Flowable.fromPublisher(this.blockingManager.blockingPublisher(Flowable.defer(() -> Flowable.fromStream(filteredEntries).doFinally(filteredEntries::close))));
    }

    @Override
    public CompletionStage<Void> clear() {
        return this.cache.clearAsync();
    }

    @Override
    public CompletionStage<Boolean> containsKey(int segment, Object key) {
        return this.cache.containsKeyAsync(this.marshalKey(key));
    }

    @Override
    public CompletionStage<Boolean> isAvailable() {
        return this.operationsFactory.newFaultTolerantPingOperation().execute().thenApply(PingResponse::isSuccess);
    }

    @Override
    public CompletionStage<Long> size(IntSet segments) {
        return this.cache.sizeAsync();
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
