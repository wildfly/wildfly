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
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.infinispan.Cache;
import org.infinispan.context.Flag;
import org.infinispan.notifications.Listener;
import org.infinispan.notifications.cachelistener.annotation.CacheEntryActivated;
import org.infinispan.notifications.cachelistener.annotation.CacheEntryPassivated;
import org.infinispan.notifications.cachelistener.annotation.CacheEntryRemoved;
import org.infinispan.notifications.cachelistener.event.CacheEntryActivatedEvent;
import org.infinispan.notifications.cachelistener.event.CacheEntryPassivatedEvent;
import org.infinispan.notifications.cachelistener.event.CacheEntryRemovedEvent;
import org.infinispan.notifications.cachelistener.filter.CacheEventFilter;
import org.wildfly.clustering.Registrar;
import org.wildfly.clustering.Registration;
import org.wildfly.clustering.ee.Batcher;
import org.wildfly.clustering.ee.Recordable;
import org.wildfly.clustering.ee.Scheduler;
import org.wildfly.clustering.ee.cache.CacheProperties;
import org.wildfly.clustering.ee.cache.tx.TransactionBatch;
import org.wildfly.clustering.infinispan.spi.PredicateKeyFilter;
import org.wildfly.clustering.infinispan.spi.distribution.CacheLocality;
import org.wildfly.clustering.infinispan.spi.distribution.Key;
import org.wildfly.clustering.infinispan.spi.distribution.Locality;
import org.wildfly.clustering.web.IdentifierFactory;
import org.wildfly.clustering.web.cache.session.ImmutableSessionActivationNotifier;
import org.wildfly.clustering.web.cache.session.ImmutableSessionBindingNotifier;
import org.wildfly.clustering.web.cache.session.SessionFactory;
import org.wildfly.clustering.web.cache.session.SimpleImmutableSession;
import org.wildfly.clustering.web.cache.session.ValidSession;
import org.wildfly.clustering.web.infinispan.logging.InfinispanWebLogger;
import org.wildfly.clustering.web.session.ImmutableSession;
import org.wildfly.clustering.web.session.ImmutableSessionMetaData;
import org.wildfly.clustering.web.session.Session;
import org.wildfly.clustering.web.session.SessionExpirationListener;
import org.wildfly.clustering.web.session.SessionManager;
import org.wildfly.clustering.web.session.SpecificationProvider;

/**
 * Generic session manager implementation - independent of cache mapping strategy.
 * @param <S> the HttpSession specification type
 * @param <SC> the ServletContext specification type
 * @param <AL> the HttpSessionAttributeListener specification type
 * @param <BL> the HttpSessionBindingListener specification type
 * @param <MV> the meta-data value type
 * @param <AV> the attributes value type
 * @param <LC> the local context type
 * @author Paul Ferraro
 */
@Listener
public class InfinispanSessionManager<S, SC, AL, BL, MV, AV, LC> implements SessionManager<LC, TransactionBatch> {

    private final Registrar<SessionExpirationListener> expirationRegistrar;
    private final SessionExpirationListener expirationListener;
    private final Batcher<TransactionBatch> batcher;
    private final Cache<Key<String>, ?> cache;
    private final CacheProperties properties;
    private final SessionFactory<SC, MV, AV, LC> factory;
    private final IdentifierFactory<String> identifierFactory;
    private final Scheduler<String, ImmutableSessionMetaData> expirationScheduler;
    private final Predicate<Object> filter = new SessionCreationMetaDataKeyFilter();
    private final Recordable<ImmutableSession> recorder;
    private final SC context;
    private final SpecificationProvider<S, SC, AL, BL> provider;

    private volatile Duration defaultMaxInactiveInterval = Duration.ofMinutes(30L);
    private volatile Registration expirationRegistration;

    public InfinispanSessionManager(SessionFactory<SC, MV, AV, LC> factory, InfinispanSessionManagerConfiguration<S, SC, AL, BL> configuration) {
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
        this.provider = configuration.getSpecificationProvider();
    }

    @Override
    public void start() {
        if (this.recorder != null) {
            this.recorder.reset();
        }
        this.identifierFactory.start();
        this.expirationRegistration = this.expirationRegistrar.register(this.expirationListener);
        CacheEventFilter<Object, Object> filter = new PredicateKeyFilter<>(this.filter);
        this.cache.addListener(this, filter, null);
        this.cache.addListener(this.factory.getMetaDataFactory(), filter, null);
        this.cache.addListener(this.factory.getAttributesFactory(), filter, null);
    }

    @Override
    public void stop() {
        this.expirationRegistration.close();
        this.cache.removeListener(this);
        this.cache.removeListener(this.factory.getMetaDataFactory());
        this.cache.removeListener(this.factory.getAttributesFactory());
        this.identifierFactory.stop();
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

        return new ValidSession<>(this.factory.createSession(id, value, this.context), this.expirationScheduler);
    }

    @Override
    public Session<LC> createSession(String id) {
        Map.Entry<MV, AV> entry = this.factory.createValue(id, null);
        if (entry == null) return null;
        Session<LC> session = this.factory.createSession(id, entry, this.context);
        session.getMetaData().setMaxInactiveInterval(this.defaultMaxInactiveInterval);
        return new ValidSession<>(session, this.expirationScheduler);
    }

    @Override
    public ImmutableSession viewSession(String id) {
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
            return keys.filter(this.filter.and(key -> locality.isLocal(key))).map(key -> key.getValue()).collect(Collectors.toSet());
        }
    }

    @Override
    public long getActiveSessionCount() {
        return this.getActiveSessions().size();
    }

    @CacheEntryActivated
    public void activated(CacheEntryActivatedEvent<SessionCreationMetaDataKey, ?> event) {
        if (!event.isPre() && !this.properties.isPersistent() && event.isOriginLocal()) {
            String id = event.getKey().getValue();
            InfinispanWebLogger.ROOT_LOGGER.tracef("Session %s was activated", id);
            Map.Entry<MV, AV> value = this.factory.tryValue(id);
            if (value != null) {
                ImmutableSession session = this.factory.createImmutableSession(id, value);
                new ImmutableSessionActivationNotifier<>(this.provider, session, this.context).postActivate();
            }
        }
    }

    @CacheEntryPassivated
    public void passivated(CacheEntryPassivatedEvent<SessionCreationMetaDataKey, ?> event) {
        if (event.isPre() && !this.properties.isPersistent() && event.isOriginLocal()) {
            String id = event.getKey().getValue();
            InfinispanWebLogger.ROOT_LOGGER.tracef("Session %s will be passivated", id);
            Map.Entry<MV, AV> value = this.factory.tryValue(id);
            if (value != null) {
                ImmutableSession session = this.factory.createImmutableSession(id, value);
                new ImmutableSessionActivationNotifier<>(this.provider, session, this.context).prePassivate();
            }
        }
    }

    @CacheEntryRemoved
    public void removed(CacheEntryRemovedEvent<SessionCreationMetaDataKey, ?> event) {
        if (event.isPre() && event.isOriginLocal()) {
            String id = event.getKey().getValue();
            InfinispanWebLogger.ROOT_LOGGER.tracef("Session %s will be removed", id);
            Map.Entry<MV, AV> value = this.factory.tryValue(id);
            if (value != null) {
                ImmutableSession session = this.factory.createImmutableSession(id, value);

                new ImmutableSessionBindingNotifier<>(this.provider, session, this.context).unbound();

                if (this.recorder != null) {
                    this.recorder.record(session);
                }
            }
        }
    }
}
