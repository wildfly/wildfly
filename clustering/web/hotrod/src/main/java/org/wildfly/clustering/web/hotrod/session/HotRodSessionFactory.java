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

import java.time.Duration;
import java.time.Instant;
import java.util.Collection;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

import org.infinispan.client.hotrod.Flag;
import org.infinispan.client.hotrod.RemoteCache;
import org.infinispan.client.hotrod.annotation.ClientCacheEntryExpired;
import org.infinispan.client.hotrod.annotation.ClientListener;
import org.infinispan.client.hotrod.event.ClientCacheEntryExpiredEvent;
import org.wildfly.clustering.Registrar;
import org.wildfly.clustering.Registration;
import org.wildfly.clustering.context.DefaultExecutorService;
import org.wildfly.clustering.context.DefaultThreadFactory;
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
import org.wildfly.clustering.web.cache.session.SimpleSessionAccessMetaData;
import org.wildfly.clustering.web.hotrod.logging.Logger;
import org.wildfly.clustering.web.session.ImmutableSession;
import org.wildfly.clustering.web.session.ImmutableSessionAttributes;
import org.wildfly.clustering.web.session.ImmutableSessionMetaData;
import org.wildfly.security.manager.WildFlySecurityManager;

/**
 * Factory for creating a {@link org.wildfly.clustering.web.session.Session} backed by a set of {@link RemoteCache} entries.
 * @author Paul Ferraro
 * @param <MC> the marshalling context type
 * @param <AV> the session attribute entry type
 * @param <LC> the local context type
 */
@ClientListener
public class HotRodSessionFactory<MC, AV, LC> extends CompositeSessionFactory<MC, AV, LC> implements Registrar<Consumer<ImmutableSession>> {

    private final RemoteCache<SessionCreationMetaDataKey, SessionCreationMetaDataEntry<LC>> creationMetaDataCache;
    private final RemoteCache<SessionAccessMetaDataKey, SessionAccessMetaData> accessMetaDataCache;
    private final ImmutableSessionMetaDataFactory<CompositeSessionMetaDataEntry<LC>> metaDataFactory;
    private final ImmutableSessionAttributesFactory<AV> attributesFactory;
    private final Remover<String> attributesRemover;
    private final Collection<Consumer<ImmutableSession>> listeners = new CopyOnWriteArraySet<>();
    private final ExecutorService executor;
    private final boolean nearCacheEnabled;

    /**
     * Constructs a new session factory
     * @param config
     * @param metaDataFactory
     * @param attributesFactory
     * @param localContextFactory
     */
    public HotRodSessionFactory(HotRodSessionFactoryConfiguration config, SessionMetaDataFactory<CompositeSessionMetaDataEntry<LC>> metaDataFactory, SessionAttributesFactory<MC, AV> attributesFactory, LocalContextFactory<LC> localContextFactory) {
        super(metaDataFactory, attributesFactory, localContextFactory);
        this.metaDataFactory = metaDataFactory;
        this.attributesFactory = attributesFactory;
        this.attributesRemover = attributesFactory;
        this.creationMetaDataCache = config.getCache();
        this.accessMetaDataCache= config.getCache();
        this.executor = Executors.newFixedThreadPool(config.getExpirationThreadPoolSize(), new DefaultThreadFactory(this.getClass()));
        this.creationMetaDataCache.addClientListener(this);
        this.nearCacheEnabled = this.creationMetaDataCache.getRemoteCacheContainer().getConfiguration().remoteCaches().get(this.creationMetaDataCache.getName()).nearCacheMode().enabled();
    }

    @Override
    public void close() {
        this.creationMetaDataCache.removeClientListener(this);
        WildFlySecurityManager.doUnchecked(this.executor, DefaultExecutorService.SHUTDOWN_ACTION);
        try {
            this.executor.awaitTermination(this.creationMetaDataCache.getRemoteCacheContainer().getConfiguration().transactionTimeout(), TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    @ClientCacheEntryExpired
    public void expired(ClientCacheEntryExpiredEvent<SessionAccessMetaDataKey> event) {
        RemoteCache<SessionCreationMetaDataKey, SessionCreationMetaDataEntry<LC>> creationMetaDataCache = this.creationMetaDataCache;
        RemoteCache<SessionAccessMetaDataKey, SessionAccessMetaData> accessMetaDataCache = this.accessMetaDataCache;
        ImmutableSessionMetaDataFactory<CompositeSessionMetaDataEntry<LC>> metaDataFactory = this.metaDataFactory;
        ImmutableSessionAttributesFactory<AV> attributesFactory = this.attributesFactory;
        Remover<String> attributesRemover = this.attributesRemover;
        Collection<Consumer<ImmutableSession>> listeners = this.listeners;
        boolean nearCacheEnabled = this.nearCacheEnabled;
        String id = event.getKey().getId();
        Runnable task = new Runnable() {
            @Override
            public void run() {
                SessionCreationMetaDataEntry<LC> creationMetaDataEntry = creationMetaDataCache.withFlags(Flag.FORCE_RETURN_VALUE).remove(new SessionCreationMetaDataKey(id));
                if (creationMetaDataEntry != null) {
                    // Ensure access metadata entry is removed from near cache
                    if (nearCacheEnabled) {
                        accessMetaDataCache.withFlags(Flag.SKIP_LISTENER_NOTIFICATION).remove(new SessionAccessMetaDataKey(id));
                    }
                    AV attributesValue = attributesFactory.findValue(id);
                    if (attributesValue != null) {
                        // Fabricate a reasonable SessionAccessMetaData
                        SessionAccessMetaData accessMetaData = new SimpleSessionAccessMetaData();
                        Duration lastAccess = Duration.ofSeconds(1);
                        Duration sinceCreation = Duration.between(creationMetaDataEntry.getMetaData().getCreationTime(), Instant.now()).minus(lastAccess);
                        accessMetaData.setLastAccessDuration(sinceCreation, lastAccess);

                        // Notify session expiration listeners
                        ImmutableSessionMetaData metaData = metaDataFactory.createImmutableSessionMetaData(id, new CompositeSessionMetaDataEntry<>(creationMetaDataEntry, accessMetaData));
                        ImmutableSessionAttributes attributes = attributesFactory.createImmutableSessionAttributes(id, attributesValue);
                        ImmutableSession session = HotRodSessionFactory.this.createImmutableSession(id, metaData, attributes);
                        Logger.ROOT_LOGGER.tracef("Session %s has expired.", id);
                        for (Consumer<ImmutableSession> listener : listeners) {
                            listener.accept(session);
                        }
                        attributesRemover.remove(id);
                    }
                }
            }
        };
        this.executor.submit(task);
    }

    @Override
    public Registration register(Consumer<ImmutableSession> listener) {
        this.listeners.add(listener);
        return () -> this.listeners.remove(listener);
    }
}
