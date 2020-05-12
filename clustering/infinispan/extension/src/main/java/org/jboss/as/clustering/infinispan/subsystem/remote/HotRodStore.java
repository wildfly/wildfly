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
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.Executor;
import java.util.function.Predicate;

import org.infinispan.client.hotrod.Flag;
import org.infinispan.client.hotrod.ProtocolVersion;
import org.infinispan.client.hotrod.RemoteCache;
import org.infinispan.client.hotrod.RemoteCacheManager;
import org.infinispan.client.hotrod.exceptions.HotRodClientException;
import org.infinispan.client.hotrod.impl.Util;
import org.infinispan.client.hotrod.impl.operations.OperationsFactory;
import org.infinispan.client.hotrod.impl.operations.PingOperation;
import org.infinispan.commons.io.ByteBuffer;
import org.infinispan.commons.marshall.WrappedByteArray;
import org.infinispan.commons.persistence.Store;
import org.infinispan.commons.util.CloseableIterator;
import org.infinispan.commons.util.IntSet;
import org.infinispan.commons.util.IteratorMapper;
import org.infinispan.marshall.core.MarshalledEntry;
import org.infinispan.metadata.InternalMetadata;
import org.infinispan.persistence.PersistenceUtil;
import org.infinispan.persistence.spi.AdvancedLoadWriteStore;
import org.infinispan.persistence.spi.InitializationContext;
import org.infinispan.persistence.spi.PersistenceException;
import org.infinispan.persistence.spi.SegmentedAdvancedLoadWriteStore;
import org.jboss.as.clustering.infinispan.InfinispanLogger;
import org.reactivestreams.Publisher;
import org.wildfly.clustering.infinispan.client.RemoteCacheContainer;
import org.wildfly.clustering.infinispan.client.near.EmptyNearCacheService;

import io.reactivex.Flowable;
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
public class HotRodStore<K, V> implements SegmentedAdvancedLoadWriteStore<K, V>, Function<CloseableIterator<byte[]>, Publisher<K>>, Consumer<K> {

    private InitializationContext ctx;

    private RemoteCache<byte[], byte[]> remoteCache;

    @Override
    public void init(InitializationContext ctx) {
        this.ctx = ctx;

        HotRodStoreConfiguration configuration = ctx.getConfiguration();
        RemoteCacheContainer remoteCacheContainer = configuration.attributes().attribute(HotRodStoreConfiguration.REMOTE_CACHE_CONTAINER).get();
        String cacheConfiguration = configuration.attributes().attribute(HotRodStoreConfiguration.CACHE_CONFIGURATION).get();
        String cacheName = ctx.getCache().getName();

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
    public MarshalledEntry<K, V> load(Object key) throws PersistenceException {
        byte[] bytes = this.remoteCache.get(this.marshall(key));
        if (bytes == null) {
            return null;
        }
        Map.Entry<ByteBuffer, ByteBuffer> entry = this.unmarshallValue(bytes);
        return this.ctx.getMarshalledEntryFactory().newMarshalledEntry(key, entry.getKey(), entry.getValue());
    }

    private MarshalledEntry<K, V> asMarshalledEntry(Object key) {
        return this.ctx.getMarshalledEntryFactory().newMarshalledEntry(key, (Object) null, (InternalMetadata) null);
    }

    @Override
    public void write(MarshalledEntry<? extends K, ? extends V> entry) {
        this.remoteCache.put(this.marshall(entry.getKey()), this.marshall(entry));
    }

    @Override
    public void writeBatch(Iterable<MarshalledEntry<? extends K, ? extends V>> marshalledEntries) {
        Map<byte[], byte[]> batch = new HashMap<>();
        for (MarshalledEntry<? extends K, ? extends V> entry : marshalledEntries) {
            batch.put(this.marshall(entry.getKey()), this.marshall(entry));
        }

        if (!batch.isEmpty()) {
            this.remoteCache.putAll(batch);
        }
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
    public Publisher<MarshalledEntry<K, V>> publishEntries(Predicate<? super K> filter, boolean fetchValue, boolean fetchMetadata) {
        return this.publishEntries(null, filter, fetchValue, fetchMetadata);
    }

    @Override
    public Publisher<MarshalledEntry<K, V>> publishEntries(IntSet segments, Predicate<? super K> filter, boolean fetchValue, boolean fetchMetadata) {
        Flowable<K> keys = this.publishKeys(filter);
        return (fetchValue || fetchMetadata) ? keys.map(this::load) : keys.map(this::asMarshalledEntry);
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

    private byte[] marshall(Object key) {
        try {
            return (key instanceof WrappedByteArray) ? ((WrappedByteArray) key).getBytes() : this.ctx.getMarshaller().objectToByteBuffer(key);
        } catch (IOException | InterruptedException e) {
            throw new PersistenceException(e);
        }
    }

    private byte[] marshall(MarshalledEntry<? extends K, ? extends V> entry) {
        return this.marshall(new AbstractMap.SimpleImmutableEntry<>(entry.getValueBytes(), entry.getMetadataBytes()));
    }

    @SuppressWarnings("unchecked")
    private K unmarshallKey(byte[] bytes) {
        return (K) this.unmarshall(bytes);
    }

    @SuppressWarnings("unchecked")
    private Map.Entry<ByteBuffer, ByteBuffer> unmarshallValue(byte[] bytes) {
        return (Map.Entry<ByteBuffer, ByteBuffer>) this.unmarshall(bytes);
    }

    private Object unmarshall(byte[] bytes) {
        try {
            return this.ctx.getMarshaller().objectFromByteBuffer(bytes);
        } catch (IOException | ClassNotFoundException e) {
            throw new PersistenceException(e);
        }
    }

    @Override
    public boolean isAvailable() {
        RemoteCacheManager manager = this.remoteCache.getRemoteCacheManager();
        OperationsFactory operationsFactory = new OperationsFactory(manager.getChannelFactory(), manager.getCodec(), null, manager.getConfiguration());
        PingOperation.PingResponse response = Util.await(operationsFactory.newFaultTolerantPingOperation().execute());
        return response.isSuccess();
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
