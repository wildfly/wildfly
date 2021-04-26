/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2020, Red Hat, Inc., and individual contributors
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

package org.wildfly.clustering.web.hotrod.session;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.time.Instant;
import java.util.Collection;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.infinispan.client.hotrod.Flag;
import org.infinispan.client.hotrod.RemoteCache;
import org.infinispan.client.hotrod.annotation.ClientCacheEntryExpired;
import org.infinispan.client.hotrod.annotation.ClientListener;
import org.infinispan.client.hotrod.event.ClientCacheEntryCustomEvent;
import org.infinispan.commons.io.UnsignedNumeric;
import org.infinispan.commons.marshall.Marshaller;
import org.jboss.as.clustering.context.DefaultExecutorService;
import org.jboss.as.clustering.context.DefaultThreadFactory;
import org.wildfly.clustering.Registrar;
import org.wildfly.clustering.Registration;
import org.wildfly.clustering.ee.Remover;
import org.wildfly.clustering.web.LocalContextFactory;
import org.wildfly.clustering.web.cache.session.CompositeSessionFactory;
import org.wildfly.clustering.web.cache.session.CompositeSessionMetaDataEntry;
import org.wildfly.clustering.web.cache.session.ImmutableSessionAttributesFactory;
import org.wildfly.clustering.web.cache.session.ImmutableSessionMetaDataFactory;
import org.wildfly.clustering.web.cache.session.SessionAccessMetaData;
import org.wildfly.clustering.web.cache.session.SessionAttributesFactory;
import org.wildfly.clustering.web.cache.session.SessionCreationMetaDataEntry;
import org.wildfly.clustering.web.cache.session.SessionMetaDataFactory;
import org.wildfly.clustering.web.cache.session.SimpleSessionCreationMetaData;
import org.wildfly.clustering.web.hotrod.logging.Logger;
import org.wildfly.clustering.web.session.ImmutableSession;
import org.wildfly.clustering.web.session.ImmutableSessionAttributes;
import org.wildfly.clustering.web.session.ImmutableSessionMetaData;
import org.wildfly.clustering.web.session.SessionExpirationListener;
import org.wildfly.security.manager.WildFlySecurityManager;

/**
 * @author Paul Ferraro
 */
@ClientListener(converterFactoryName = "___eager-key-value-version-converter", useRawData = true) // References org.infinispan.server.hotrod.KeyValueVersionConverterFactory
public class HotRodSessionFactory<C, V, L> extends CompositeSessionFactory<C, V, L> implements Registrar<SessionExpirationListener> {

    private final RemoteCache<SessionCreationMetaDataKey, SessionCreationMetaDataEntry<L>> creationMetaDataCache;
    private final RemoteCache<SessionAccessMetaDataKey, SessionAccessMetaData> accessMetaDataCache;
    private final ImmutableSessionMetaDataFactory<CompositeSessionMetaDataEntry<L>> metaDataFactory;
    private final ImmutableSessionAttributesFactory<V> attributesFactory;
    private final Remover<String> attributesRemover;
    private final Collection<SessionExpirationListener> listeners = new CopyOnWriteArraySet<>();
    private final ExecutorService executor = Executors.newCachedThreadPool(new DefaultThreadFactory(this.getClass()));
    private final boolean nearCacheEnabled;

    /**
     * Constructs a new session factory
     * @param config
     * @param metaDataFactory
     * @param attributesFactory
     * @param localContextFactory
     */
    public HotRodSessionFactory(HotRodSessionMetaDataFactoryConfiguration config, SessionMetaDataFactory<CompositeSessionMetaDataEntry<L>> metaDataFactory, SessionAttributesFactory<C, V> attributesFactory, LocalContextFactory<L> localContextFactory) {
        super(metaDataFactory, attributesFactory, localContextFactory);
        this.metaDataFactory = metaDataFactory;
        this.attributesFactory = attributesFactory;
        this.attributesRemover = attributesFactory;
        this.creationMetaDataCache = config.getCache();
        this.accessMetaDataCache= config.getCache();
        this.creationMetaDataCache.addClientListener(this, null, new Object[] { Boolean.TRUE });
        this.nearCacheEnabled = this.creationMetaDataCache.getRemoteCacheManager().getConfiguration().remoteCaches().get(this.creationMetaDataCache.getName()).nearCacheMode().enabled();
    }

    @Override
    public void close() {
        this.creationMetaDataCache.removeClientListener(this);
        WildFlySecurityManager.doUnchecked(this.executor, DefaultExecutorService.SHUTDOWN_ACTION);
        try {
            this.executor.awaitTermination(this.creationMetaDataCache.getRemoteCacheManager().getConfiguration().transaction().timeout(), TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    @ClientCacheEntryExpired
    public void expired(ClientCacheEntryCustomEvent<byte[]> event) {
        RemoteCache<SessionCreationMetaDataKey, SessionCreationMetaDataEntry<L>> creationMetaDataCache = this.creationMetaDataCache;
        RemoteCache<SessionAccessMetaDataKey, SessionAccessMetaData> accessMetaDataCache = this.accessMetaDataCache;
        ImmutableSessionMetaDataFactory<CompositeSessionMetaDataEntry<L>> metaDataFactory = this.metaDataFactory;
        ImmutableSessionAttributesFactory<V> attributesFactory = this.attributesFactory;
        Remover<String> attributesRemover = this.attributesRemover;
        Collection<SessionExpirationListener> listeners = this.listeners;
        boolean nearCacheEnabled = this.nearCacheEnabled;
        Runnable task = new Runnable() {
            @Override
            public void run() {
                ByteBuffer buffer = ByteBuffer.wrap(event.getEventData());
                byte[] key = new byte[UnsignedNumeric.readUnsignedInt(buffer)];
                buffer.get(key);
                byte[] value = buffer.remaining() > 0 ? new byte[UnsignedNumeric.readUnsignedInt(buffer)] : null;
                if (value != null) {
                    buffer.get(value);
                }
                Marshaller marshaller = creationMetaDataCache.getRemoteCacheManager().getConfiguration().marshaller();
                String id = null;
                try {
                    SessionCreationMetaDataKey creationKey = (SessionCreationMetaDataKey) marshaller.objectFromByteBuffer(key);
                    id = creationKey.getId();
                    @SuppressWarnings("unchecked")
                    SessionCreationMetaDataEntry<L> creationEntry = (value != null) ? (SessionCreationMetaDataEntry<L>) marshaller.objectFromByteBuffer(value) : new SessionCreationMetaDataEntry<>(new SimpleSessionCreationMetaData(Instant.EPOCH));
                    // Ensure entry is removed from near cache
                    if (nearCacheEnabled) {
                        creationMetaDataCache.withFlags(Flag.SKIP_LISTENER_NOTIFICATION).remove(creationKey);
                    }
                    SessionAccessMetaData accessMetaData = accessMetaDataCache.withFlags(Flag.FORCE_RETURN_VALUE).remove(new SessionAccessMetaDataKey(id));
                    if (accessMetaData != null) {
                        V attributesValue = attributesFactory.findValue(id);
                        if (attributesValue != null) {
                            ImmutableSessionMetaData metaData = metaDataFactory.createImmutableSessionMetaData(id, new CompositeSessionMetaDataEntry<>(creationEntry, accessMetaData));
                            ImmutableSessionAttributes attributes = attributesFactory.createImmutableSessionAttributes(id, attributesValue);
                            ImmutableSession session = HotRodSessionFactory.this.createImmutableSession(id, metaData, attributes);
                            Logger.ROOT_LOGGER.tracef("Session %s has expired.", id);
                            for (SessionExpirationListener listener : listeners) {
                                listener.sessionExpired(session);
                            }
                            attributesRemover.remove(id);
                        }
                    }
                } catch (IOException | ClassNotFoundException e) {
                    Logger.ROOT_LOGGER.failedToExpireSession(e, id);
                }
            }
        };
        this.executor.submit(task);
    }

    @Override
    public Registration register(SessionExpirationListener listener) {
        this.listeners.add(listener);
        return () -> this.listeners.remove(listener);
    }
}
