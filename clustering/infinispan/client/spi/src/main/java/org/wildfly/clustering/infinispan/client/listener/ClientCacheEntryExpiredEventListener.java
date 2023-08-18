/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2022, Red Hat, Inc., and individual contributors
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

package org.wildfly.clustering.infinispan.client.listener;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.concurrent.Executor;
import java.util.concurrent.RejectedExecutionException;
import java.util.function.BiConsumer;

import org.infinispan.client.hotrod.RemoteCache;
import org.infinispan.client.hotrod.annotation.ClientCacheEntryExpired;
import org.infinispan.client.hotrod.annotation.ClientListener;
import org.infinispan.client.hotrod.event.ClientCacheEntryCustomEvent;
import org.infinispan.commons.io.UnsignedNumeric;
import org.infinispan.commons.marshall.Marshaller;
import org.jboss.logging.Logger;

/**
 * Generic listener for cache entry expiration events.
 * @author Paul Ferraro
 */
@ClientListener(converterFactoryName = "___eager-key-value-version-converter", useRawData = true) // References org.infinispan.server.hotrod.KeyValueVersionConverterFactory
public class ClientCacheEntryExpiredEventListener<K, V> implements ListenerRegistrar {
    private static final Logger LOGGER = Logger.getLogger(ClientCacheEntryExpiredEventListener.class);

    private final RemoteCache<K, V> cache;
    private final BiConsumer<K, V> handler;

    public ClientCacheEntryExpiredEventListener(RemoteCache<K, V> cache, BiConsumer<K, V> handler) {
        this.cache = cache;
        this.handler = handler;
    }

    @ClientCacheEntryExpired
    public void expired(ClientCacheEntryCustomEvent<byte[]> event) {
        ByteBuffer buffer = ByteBuffer.wrap(event.getEventData());
        byte[] keyBytes = new byte[UnsignedNumeric.readUnsignedInt(buffer)];
        buffer.get(keyBytes);
        byte[] valueBytes = buffer.remaining() > 0 ? new byte[UnsignedNumeric.readUnsignedInt(buffer)] : null;
        if (valueBytes != null) {
            buffer.get(valueBytes);
        }
        @SuppressWarnings("deprecation")
        Executor executor = this.cache.getRemoteCacheManager().getAsyncExecutorService();
        Marshaller marshaller = this.cache.getRemoteCacheContainer().getMarshaller();
        try {
            executor.execute(() -> {
                try {
                    @SuppressWarnings("unchecked")
                    K key = (K) marshaller.objectFromByteBuffer(keyBytes);
                    @SuppressWarnings("unchecked")
                    V value = (valueBytes != null) ? (V) marshaller.objectFromByteBuffer(valueBytes) : null;
                    this.handler.accept(key, value);
                } catch (IOException | ClassNotFoundException e) {
                    LOGGER.warn(e.getLocalizedMessage(), e);
                }
            });
        } catch (RejectedExecutionException e) {
            LOGGER.debug(e.getLocalizedMessage(), e);
        }
    }

    @Override
    public ListenerRegistration register() {
        this.cache.addClientListener(this, null, new Object[] { true });
        return () -> this.cache.removeClientListener(this);
    }
}
