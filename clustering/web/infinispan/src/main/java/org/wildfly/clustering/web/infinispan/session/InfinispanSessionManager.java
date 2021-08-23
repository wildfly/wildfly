/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2013, Red Hat, Inc., and individual contributors
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
package org.wildfly.clustering.web.infinispan.session;

import java.time.Duration;
import java.util.AbstractMap.SimpleImmutableEntry;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletionStage;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.infinispan.Cache;
import org.infinispan.configuration.cache.PersistenceConfiguration;
import org.infinispan.configuration.cache.StoreConfiguration;
import org.infinispan.context.Flag;
import org.infinispan.notifications.Listener;
import org.infinispan.notifications.cachelistener.annotation.CacheEntryRemoved;
import org.infinispan.notifications.cachelistener.event.CacheEntryRemovedEvent;
import org.infinispan.util.concurrent.CompletableFutures;
import org.wildfly.clustering.Registrar;
import org.wildfly.clustering.Registration;
import org.wildfly.clustering.ee.Batcher;
import org.wildfly.clustering.ee.Key;
import org.wildfly.clustering.ee.Recordable;
import org.wildfly.clustering.ee.Scheduler;
import org.wildfly.clustering.ee.cache.CacheProperties;
import org.wildfly.clustering.ee.cache.tx.TransactionBatch;
import org.wildfly.clustering.infinispan.spi.PredicateKeyFilter;
import org.wildfly.clustering.infinispan.spi.distribution.CacheLocality;
import org.wildfly.clustering.infinispan.spi.distribution.Locality;
import org.wildfly.clustering.web.IdentifierFactory;
import org.wildfly.clustering.web.cache.session.SessionFactory;
import org.wildfly.clustering.web.cache.session.SessionMetaDataFactory;
import org.wildfly.clustering.web.cache.session.SimpleImmutableSession;
import org.wildfly.clustering.web.cache.session.ValidSession;
import org.wildfly.clustering.web.infinispan.logging.InfinispanWebLogger;
import org.wildfly.clustering.web.session.ImmutableSession;
import org.wildfly.clustering.web.session.ImmutableSessionMetaData;
import org.wildfly.clustering.web.session.Session;
import org.wildfly.clustering.web.session.SessionExpirationListener;
import org.wildfly.clustering.web.session.SessionExpirationMetaData;
import org.wildfly.clustering.web.session.SessionManager;

/**
 * Generic session manager implementation - independent of cache mapping strategy.
 * @param <SC> the ServletContext specification type
 * @param <MV> the meta-data value type
 * @param <AV> the attributes value type
 * @param <LC> the local context type
 * @author Paul Ferraro
 */
@Listener(primaryOnly = true)
public class InfinispanSessionManager<SC, MV, AV, LC> implements SessionManager<LC, TransactionBatch> {

    private final Registrar<SessionExpirationListener> expirationRegistrar;
    private final SessionExpirationListener expirationListener;
    private final Batcher<TransactionBatch> batcher;
    private final Cache<Key<String>, ?> cache;
    private final CacheProperties properties;
    private final SessionFactory<SC, MV, AV, LC> factory;
    private final IdentifierFactory<String> identifierFactory;
    private final Scheduler<String, SessionExpirationMetaData> expirationScheduler;
    private final Recordable<ImmutableSessionMetaData> recorder;
    private final SC context;
    private final Runnable startTask;
    private final Consumer<ImmutableSession> closeTask;
    private final Registrar<Map.Entry<SC, SessionManager<LC, TransactionBatch>>> contextRegistrar;

    private volatile Duration defaultMaxInactiveInterval = Duration.ofMinutes(30L);
    private volatile Registration expirationRegistration;
    private volatile Registration contextRegistration;

    public InfinispanSessionManager(SessionFactory<SC, MV, AV, LC> factory, InfinispanSessionManagerConfiguration<SC, LC> configuration) {
        this.factory = factory;
        this.cache = configuration.getCache();
        this.properties = configuration.getProperties();
        this.expirationRegistrar = configuration.getExpirationRegistar();
        this.expirationListener = configuration.getExpirationListener();
        this.identifierFactory = configuration.getIdentifierFactory();
        this.batcher = configuration.getBatcher();
        this.expirationScheduler = configuration.getExpirationScheduler();
        this.recorder = configuration.getInactiveSessionRecorder();
        this.context = configuration.getServletContext();
        this.contextRegistrar = configuration.getContextRegistrar();
        this.startTask = configuration.getStartTask();
        this.closeTask = new Consumer<ImmutableSession>() {
            @Override
            public void accept(ImmutableSession session) {
                if (session.isValid()) {
                    configuration.getExpirationScheduler().schedule(session.getId(), new SimpleSessionExpirationMetaData(session.getMetaData()));
                }
            }
        };
    }

    @Override
    public void start() {
        this.contextRegistration = this.contextRegistrar.register(new SimpleImmutableEntry<>(this.context, this));
        if (this.recorder != null) {
            this.recorder.reset();
            this.cache.addListener(this, new PredicateKeyFilter<>(SessionCreationMetaDataKeyFilter.INSTANCE), null);
        }
        this.identifierFactory.start();
        this.expirationRegistration = this.expirationRegistrar.register(this.expirationListener);
        this.startTask.run();
    }

    @Override
    public void stop() {
        if (!this.properties.isPersistent()) {
            PersistenceConfiguration persistence = this.cache.getCacheConfiguration().persistence();
            // Don't passivate sessions on stop if we will purge the store on startup
            if (persistence.passivation() && !persistence.stores().stream().allMatch(StoreConfiguration::purgeOnStartup)) {
                try (Stream<Key<String>> stream = this.cache.getAdvancedCache().withFlags(Flag.CACHE_MODE_LOCAL, Flag.SKIP_CACHE_LOAD, Flag.SKIP_LOCKING).keySet().stream()) {
                    stream.filter(SessionCreationMetaDataKeyFilter.INSTANCE).forEach(this.cache::evict);
                }
            }
        }
        this.expirationRegistration.close();
        if (this.recorder != null) {
            this.cache.removeListener(this);
        }
        this.identifierFactory.stop();
        this.contextRegistration.close();
    }

    @Override
    public Duration getStopTimeout() {
        return Duration.ofMillis(this.cache.getCacheConfiguration().transaction().cacheStopTimeout());
    }

    @Override
    public Batcher<TransactionBatch> getBatcher() {
        return this.batcher;
    }

    @Override
    public Duration getDefaultMaxInactiveInterval() {
        return this.defaultMaxInactiveInterval;
    }

    @Override
    public void setDefaultMaxInactiveInterval(Duration duration) {
        this.defaultMaxInactiveInterval = duration;
    }

    @Override
    public String createIdentifier() {
        return this.identifierFactory.createIdentifier();
    }

    @Override
    public Session<LC> findSession(String id) {
        Map.Entry<MV, AV> value = this.factory.findValue(id);
        if (value == null) {
            InfinispanWebLogger.ROOT_LOGGER.tracef("Session %s not found", id);
            return null;
        }
        ImmutableSession session = this.factory.createImmutableSession(id, value);
        if (session.getMetaData().isExpired()) {
            InfinispanWebLogger.ROOT_LOGGER.tracef("Session %s was found, but has expired", id);
            this.expirationListener.sessionExpired(session);
            this.factory.remove(id);
            return null;
        }
        this.expirationScheduler.cancel(id);

        return new ValidSession<>(this.factory.createSession(id, value, this.context), this.closeTask);
    }

    @Override
    public Session<LC> createSession(String id) {
        Map.Entry<MV, AV> entry = this.factory.createValue(id, null);
        if (entry == null) return null;
        Session<LC> session = this.factory.createSession(id, entry, this.context);
        session.getMetaData().setMaxInactiveInterval(this.defaultMaxInactiveInterval);
        return new ValidSession<>(session, this.closeTask);
    }

    @Override
    public ImmutableSession readSession(String id) {
        Map.Entry<MV, AV> value = this.factory.findValue(id);
        return (value != null) ? new SimpleImmutableSession(this.factory.createImmutableSession(id, value)) : null;
    }

    @Override
    public Set<String> getActiveSessions() {
        // Omit remote sessions (i.e. when using DIST mode) as well as passivated sessions
        return this.getSessions(Flag.CACHE_MODE_LOCAL, Flag.SKIP_CACHE_LOAD);
    }

    @Override
    public Set<String> getLocalSessions() {
        // Omit remote sessions (i.e. when using DIST mode)
        return this.getSessions(Flag.CACHE_MODE_LOCAL);
    }

    private Set<String> getSessions(Flag... flags) {
        Locality locality = new CacheLocality(this.cache);
        try (Stream<Key<String>> keys = this.cache.getAdvancedCache().withFlags(flags).keySet().stream()) {
            return keys.filter(SessionCreationMetaDataKeyFilter.INSTANCE.and(key -> locality.isLocal(key))).map(key -> key.getId()).collect(Collectors.toSet());
        }
    }

    @Override
    public long getActiveSessionCount() {
        return this.getActiveSessions().size();
    }

    @CacheEntryRemoved
    public CompletionStage<Void> removed(CacheEntryRemovedEvent<SessionCreationMetaDataKey, ?> event) {
        if (event.isPre()) {
            String id = event.getKey().getId();
            InfinispanWebLogger.ROOT_LOGGER.tracef("Session %s will be removed", id);
            SessionMetaDataFactory<MV> factory = this.factory.getMetaDataFactory();
            MV value = factory.tryValue(id);
            if (value != null) {
                ImmutableSessionMetaData metaData = factory.createImmutableSessionMetaData(id, value);
                this.recorder.record(metaData);
            }
        }
        return CompletableFutures.completedNull();
    }
}
