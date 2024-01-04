/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.web.hotrod.session;

import java.time.Duration;
import java.time.Instant;
import java.util.Collection;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.function.Supplier;

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
import org.wildfly.clustering.web.cache.session.CompositeSessionFactory;
import org.wildfly.clustering.web.cache.session.attributes.ImmutableSessionAttributesFactory;
import org.wildfly.clustering.web.cache.session.attributes.SessionAttributesFactory;
import org.wildfly.clustering.web.cache.session.metadata.ImmutableSessionMetaDataFactory;
import org.wildfly.clustering.web.cache.session.metadata.SessionMetaDataFactory;
import org.wildfly.clustering.web.cache.session.metadata.fine.SessionAccessMetaDataEntry;
import org.wildfly.clustering.web.cache.session.metadata.fine.SessionCreationMetaDataEntry;
import org.wildfly.clustering.web.cache.session.metadata.fine.SessionMetaDataEntry;
import org.wildfly.clustering.web.cache.session.metadata.fine.DefaultSessionMetaDataEntry;
import org.wildfly.clustering.web.cache.session.metadata.fine.DefaultSessionAccessMetaDataEntry;
import org.wildfly.clustering.web.hotrod.logging.Logger;
import org.wildfly.clustering.web.hotrod.session.metadata.SessionAccessMetaDataKey;
import org.wildfly.clustering.web.hotrod.session.metadata.SessionCreationMetaDataKey;
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
public class HotRodSessionFactory<MC, AV, LC> extends CompositeSessionFactory<MC, SessionMetaDataEntry<LC>, AV, LC> implements Registrar<Consumer<ImmutableSession>> {
    private static final ThreadFactory THREAD_FACTORY = new DefaultThreadFactory(HotRodSessionFactory.class);

    private final RemoteCache<SessionCreationMetaDataKey, SessionCreationMetaDataEntry<LC>> creationMetaDataCache;
    private final Flag[] forceReturnFlags;
    private final ImmutableSessionMetaDataFactory<SessionMetaDataEntry<LC>> metaDataFactory;
    private final ImmutableSessionAttributesFactory<AV> attributesFactory;
    private final Remover<String> attributesRemover;
    private final Collection<Consumer<ImmutableSession>> listeners = new CopyOnWriteArraySet<>();
    private final ExecutorService executor;

    /**
     * Constructs a new session factory
     * @param config
     * @param metaDataFactory
     * @param attributesFactory
     * @param localContextFactory
     */
    public HotRodSessionFactory(HotRodSessionFactoryConfiguration config, SessionMetaDataFactory<SessionMetaDataEntry<LC>> metaDataFactory, SessionAttributesFactory<MC, AV> attributesFactory, Supplier<LC> localContextFactory) {
        super(metaDataFactory, attributesFactory, localContextFactory);
        this.metaDataFactory = metaDataFactory;
        this.attributesFactory = attributesFactory;
        this.attributesRemover = attributesFactory;
        this.creationMetaDataCache = config.getCache();
        this.forceReturnFlags = config.getForceReturnFlags();
        this.executor = Executors.newFixedThreadPool(config.getExpirationThreadPoolSize(), THREAD_FACTORY);
        this.creationMetaDataCache.addClientListener(this);
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
        Flag[] forceReturnFlags = this.forceReturnFlags;
        ImmutableSessionMetaDataFactory<SessionMetaDataEntry<LC>> metaDataFactory = this.metaDataFactory;
        ImmutableSessionAttributesFactory<AV> attributesFactory = this.attributesFactory;
        Remover<String> attributesRemover = this.attributesRemover;
        Collection<Consumer<ImmutableSession>> listeners = this.listeners;
        String id = event.getKey().getId();
        Runnable task = new Runnable() {
            @Override
            public void run() {
                SessionCreationMetaDataEntry<LC> creationMetaDataEntry = creationMetaDataCache.withFlags(forceReturnFlags).remove(new SessionCreationMetaDataKey(id));
                if (creationMetaDataEntry != null) {
                    AV attributesValue = attributesFactory.findValue(id);
                    if (attributesValue != null) {
                        // Fabricate a reasonable SessionAccessMetaData
                        SessionAccessMetaDataEntry accessMetaData = new DefaultSessionAccessMetaDataEntry();
                        Duration lastAccess = Duration.ofSeconds(1);
                        Duration sinceCreation = Duration.between(creationMetaDataEntry.getCreationTime(), Instant.now()).minus(lastAccess);
                        accessMetaData.setLastAccessDuration(sinceCreation, lastAccess);

                        // Notify session expiration listeners
                        ImmutableSessionMetaData metaData = metaDataFactory.createImmutableSessionMetaData(id, new DefaultSessionMetaDataEntry<>(creationMetaDataEntry, accessMetaData));
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
