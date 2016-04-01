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
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpSession;
import javax.servlet.http.HttpSessionActivationListener;
import javax.servlet.http.HttpSessionBindingEvent;
import javax.servlet.http.HttpSessionBindingListener;
import javax.servlet.http.HttpSessionEvent;

import org.infinispan.Cache;
import org.infinispan.commons.CacheException;
import org.infinispan.configuration.cache.Configuration;
import org.infinispan.context.Flag;
import org.infinispan.distribution.DistributionManager;
import org.infinispan.notifications.Listener;
import org.infinispan.notifications.cachelistener.annotation.CacheEntryActivated;
import org.infinispan.notifications.cachelistener.annotation.CacheEntryPassivated;
import org.infinispan.notifications.cachelistener.annotation.CacheEntryRemoved;
import org.infinispan.notifications.cachelistener.annotation.DataRehashed;
import org.infinispan.notifications.cachelistener.event.CacheEntryActivatedEvent;
import org.infinispan.notifications.cachelistener.event.CacheEntryPassivatedEvent;
import org.infinispan.notifications.cachelistener.event.CacheEntryRemovedEvent;
import org.infinispan.notifications.cachelistener.event.DataRehashedEvent;
import org.infinispan.remoting.transport.Address;
import org.wildfly.clustering.dispatcher.Command;
import org.wildfly.clustering.dispatcher.CommandDispatcher;
import org.wildfly.clustering.dispatcher.CommandDispatcherFactory;
import org.wildfly.clustering.ee.Batch;
import org.wildfly.clustering.ee.Batcher;
import org.wildfly.clustering.ee.Invoker;
import org.wildfly.clustering.ee.Recordable;
import org.wildfly.clustering.ee.infinispan.RetryingInvoker;
import org.wildfly.clustering.ee.infinispan.TransactionBatch;
import org.wildfly.clustering.group.Node;
import org.wildfly.clustering.group.NodeFactory;
import org.wildfly.clustering.infinispan.spi.distribution.ConsistentHashLocality;
import org.wildfly.clustering.infinispan.spi.distribution.Key;
import org.wildfly.clustering.infinispan.spi.distribution.Locality;
import org.wildfly.clustering.infinispan.spi.distribution.SimpleLocality;
import org.wildfly.clustering.service.concurrent.ServiceExecutor;
import org.wildfly.clustering.service.concurrent.StampedLockServiceExecutor;
import org.wildfly.clustering.web.IdentifierFactory;
import org.wildfly.clustering.web.infinispan.logging.InfinispanWebLogger;
import org.wildfly.clustering.web.session.ImmutableHttpSessionAdapter;
import org.wildfly.clustering.web.session.ImmutableSession;
import org.wildfly.clustering.web.session.ImmutableSessionAttributes;
import org.wildfly.clustering.web.session.ImmutableSessionMetaData;
import org.wildfly.clustering.web.session.Session;
import org.wildfly.clustering.web.session.SessionAttributes;
import org.wildfly.clustering.web.session.SessionExpirationListener;
import org.wildfly.clustering.web.session.SessionManager;
import org.wildfly.clustering.web.session.SessionMetaData;

/**
 * Generic session manager implementation - independent of cache mapping strategy.
 * @author Paul Ferraro
 */
@Listener(primaryOnly = true)
public class InfinispanSessionManager<MV, AV, L> implements SessionManager<L, TransactionBatch> {
    private final SessionExpirationListener expirationListener;
    private final Batcher<TransactionBatch> batcher;
    private final Cache<? extends Key<String>, ?> cache;
    private final SessionFactory<MV, AV, L> factory;
    private final IdentifierFactory<String> identifierFactory;
    private final CommandDispatcherFactory dispatcherFactory;
    private final NodeFactory<Address> nodeFactory;
    private final int maxActiveSessions;
    private volatile Duration defaultMaxInactiveInterval = Duration.ofMinutes(30L);
    private final boolean persistent;
    private final Invoker invoker = new RetryingInvoker(0, 10, 100);
    private final SessionCreationMetaDataKeyFilter filter = new SessionCreationMetaDataKeyFilter();
    private final Locality locality;
    private final Recordable<ImmutableSession> recorder;
    private final ServletContext context;

    private volatile CommandDispatcher<Scheduler> dispatcher;
    private volatile Scheduler scheduler;
    private volatile ServiceExecutor executor;

    public InfinispanSessionManager(SessionFactory<MV, AV, L> factory, InfinispanSessionManagerConfiguration configuration) {
        this.factory = factory;
        this.cache = configuration.getCache();
        this.locality = new ConsistentHashLocality(this.cache);
        this.expirationListener = configuration.getExpirationListener();
        this.identifierFactory = configuration.getIdentifierFactory();
        this.batcher = configuration.getBatcher();
        this.dispatcherFactory = configuration.getCommandDispatcherFactory();
        this.nodeFactory = configuration.getNodeFactory();
        this.maxActiveSessions = configuration.getMaxActiveSessions();
        this.recorder = configuration.getInactiveSessionRecorder();
        this.context = configuration.getServletContext();
        Configuration config = this.cache.getCacheConfiguration();
        // If cache is clustered or configured with a write-through cache store
        // then we need to trigger any HttpSessionActivationListeners per request
        // See SRV.7.7.2 Distributed Environments
        this.persistent = config.clustering().cacheMode().needsStateTransfer() || (config.persistence().usingStores() && !config.persistence().passivation());
    }

    @Override
    public void start() {
        this.executor = new StampedLockServiceExecutor();
        if (this.recorder != null) {
            this.recorder.reset();
        }
        this.identifierFactory.start();
        final List<Scheduler> schedulers = new ArrayList<>(2);
        schedulers.add(new SessionExpirationScheduler(this.batcher, new ExpiredSessionRemover<>(this.factory, this.expirationListener)));
        if (this.maxActiveSessions >= 0) {
            schedulers.add(new SessionEvictionScheduler(this.cache.getName() + ".eviction", this.factory, this.dispatcherFactory, this.maxActiveSessions));
        }
        this.scheduler = new Scheduler() {
            @Override
            public void schedule(String sessionId, ImmutableSessionMetaData metaData) {
                schedulers.forEach(scheduler -> scheduler.schedule(sessionId, metaData));
            }

            @Override
            public void cancel(String sessionId) {
                schedulers.forEach(scheduler -> scheduler.cancel(sessionId));
            }

            @Override
            public void cancel(Locality locality) {
                schedulers.forEach(scheduler -> scheduler.cancel(locality));
            }

            @Override
            public void close() {
                schedulers.forEach(scheduler -> scheduler.close());
            }
        };
        this.dispatcher = this.dispatcherFactory.createCommandDispatcher(this.cache.getName() + ".schedulers", this.scheduler);
        this.cache.addListener(this, this.filter);
        this.schedule(this.cache, new SimpleLocality(false), this.locality);
    }

    @Override
    public void stop() {
        this.executor.close(() -> {
            this.cache.removeListener(this);
            this.dispatcher.close();
            this.scheduler.close();
            this.identifierFactory.stop();
        });
    }

    boolean isPersistent() {
        return this.persistent;
    }

    private void cancel(String sessionId) {
        try {
            this.executeOnPrimaryOwner(sessionId, new CancelSchedulerCommand(sessionId));
        } catch (Exception e) {
            InfinispanWebLogger.ROOT_LOGGER.failedToCancelSession(e, sessionId);
        }
    }

    void schedule(String sessionId, ImmutableSessionMetaData metaData) {
        try {
            this.executeOnPrimaryOwner(sessionId, new ScheduleSchedulerCommand(sessionId, metaData));
        } catch (Exception e) {
            InfinispanWebLogger.ROOT_LOGGER.failedToScheduleSession(e, sessionId);
        }
    }

    private void executeOnPrimaryOwner(final String sessionId, final Command<Void, Scheduler> command) throws Exception {
        this.invoker.invoke(() -> {
            // This should only go remote following a failover
            Node node = this.locatePrimaryOwner(sessionId);
            return this.dispatcher.executeOnNode(command, node);
        }).get();
    }

    private Node locatePrimaryOwner(String sessionId) {
        DistributionManager dist = this.cache.getAdvancedCache().getDistributionManager();
        Address address = (dist != null) ? dist.getPrimaryLocation(new Key<>(sessionId)) : null;
        return (address != null) ? this.nodeFactory.createNode(address) : this.dispatcherFactory.getGroup().getLocalNode();
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
    public boolean containsSession(String id) {
        return this.cache.containsKey(new SessionCreationMetaDataKey(id));
    }

    @Override
    public Session<L> findSession(String id) {
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
        this.cancel(id);
        if (this.persistent) {
            this.triggerPostActivationEvents(session);
        }
        return new SchedulableSession(this.factory.createSession(id, value), session);
    }

    @Override
    public Session<L> createSession(String id) {
        Session<L> session = this.factory.createSession(id, this.factory.createValue(id, null));
        session.getMetaData().setMaxInactiveInterval(this.defaultMaxInactiveInterval);
        return new SchedulableSession(session, session);
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
        try (Stream<? extends Key<String>> keys = this.cache.getAdvancedCache().withFlags(flags).keySet().stream()) {
            return keys.filter(this.filter.and(key -> this.locality.isLocal(key))).map(key -> key.getValue()).collect(Collectors.toSet());
        }
    }

    @Override
    public int getMaxActiveSessions() {
        return this.maxActiveSessions;
    }

    @Override
    public long getActiveSessionCount() {
        return this.getActiveSessions().size();
    }

    @CacheEntryActivated
    public void activated(CacheEntryActivatedEvent<SessionCreationMetaDataKey, ?> event) {
        if (!event.isPre() && !this.persistent) {
            this.executor.execute(() -> {
                String id = event.getKey().getValue();
                InfinispanWebLogger.ROOT_LOGGER.tracef("Session %s was activated", id);
                Map.Entry<MV, AV> value = this.factory.findValue(id);
                if (value != null) {
                    ImmutableSession session = this.factory.createImmutableSession(id, value);
                    this.triggerPostActivationEvents(session);
                }
            });
        }
    }

    @CacheEntryPassivated
    public void passivated(CacheEntryPassivatedEvent<SessionCreationMetaDataKey, ?> event) {
        if (event.isPre() && !this.persistent) {
            this.executor.execute(() -> {
                String id = event.getKey().getValue();
                InfinispanWebLogger.ROOT_LOGGER.tracef("Session %s will be passivated", id);
                Map.Entry<MV, AV> value = this.factory.findValue(id);
                if (value != null) {
                    ImmutableSession session = this.factory.createImmutableSession(id, value);
                    this.triggerPrePassivationEvents(session);
                }
            });
        }
    }

    @CacheEntryRemoved
    public void removed(CacheEntryRemovedEvent<SessionCreationMetaDataKey, ?> event) {
        if (event.isPre()) {
            this.executor.execute(() -> {
                String id = event.getKey().getValue();
                InfinispanWebLogger.ROOT_LOGGER.tracef("Session %s will be removed", id);
                Map.Entry<MV, AV> value = this.factory.findValue(id);
                if (value != null) {
                    ImmutableSession session = this.factory.createImmutableSession(id, value);
                    ImmutableSessionAttributes attributes = session.getAttributes();

                    HttpSession httpSession = new ImmutableHttpSessionAdapter(session, this.context);

                    for (String name: attributes.getAttributeNames()) {
                        Object attribute = attributes.getAttribute(name);
                        if (attribute instanceof HttpSessionBindingListener) {
                            HttpSessionBindingListener listener = (HttpSessionBindingListener) attribute;
                            listener.valueUnbound(new HttpSessionBindingEvent(httpSession, name, attribute));
                        }
                    }

                    if (this.recorder != null) {
                        this.recorder.record(session);
                    }
                }
            });
        }
    }

    @DataRehashed
    public void dataRehashed(DataRehashedEvent<SessionCreationMetaDataKey, ?> event) {
        this.executor.execute(() -> {
            Cache<SessionCreationMetaDataKey, ?> cache = event.getCache();
            Address localAddress = cache.getCacheManager().getAddress();
            Locality newLocality = new ConsistentHashLocality(localAddress, event.getConsistentHashAtEnd());
            if (event.isPre()) {
                this.scheduler.cancel(newLocality);
            } else {
                Locality oldLocality = new ConsistentHashLocality(localAddress, event.getConsistentHashAtStart());
                this.schedule(cache, oldLocality, newLocality);
            }
        });
    }

    private void schedule(Cache<? extends Key<String>, ?> cache, Locality oldLocality, Locality newLocality) {
        SessionMetaDataFactory<MV, L> metaDataFactory = this.factory.getMetaDataFactory();
        // Iterate over sessions in memory
        try (Stream<? extends Key<String>> keys = cache.getAdvancedCache().withFlags(Flag.CACHE_MODE_LOCAL, Flag.SKIP_CACHE_LOAD).keySet().stream()) {
            // If we are the new primary owner of this session then schedule expiration of this session locally
            keys.filter(this.filter).filter(key -> !oldLocality.isLocal(key) && newLocality.isLocal(key)).map(key -> key.getValue()).forEach(id -> {
                try (Batch batch = this.batcher.createBatch()) {
                    try {
                        // We need to lookup the session to obtain its meta data
                        MV value = metaDataFactory.tryValue(id);
                        if (value != null) {
                            this.scheduler.schedule(id, metaDataFactory.createImmutableSessionMetaData(id, value));
                        }
                    } catch (CacheException e) {
                        batch.discard();
                    }
                }
            });
        }
    }

    void triggerPrePassivationEvents(ImmutableSession session) {
        List<HttpSessionActivationListener> listeners = findListeners(session);
        if (!listeners.isEmpty()) {
            HttpSessionEvent event = new HttpSessionEvent(new ImmutableHttpSessionAdapter(session, this.context));
            listeners.forEach(listener -> listener.sessionWillPassivate(event));
        }
    }

    void triggerPostActivationEvents(ImmutableSession session) {
        List<HttpSessionActivationListener> listeners = findListeners(session);
        if (!listeners.isEmpty()) {
            HttpSessionEvent event = new HttpSessionEvent(new ImmutableHttpSessionAdapter(session, this.context));
            listeners.forEach(listener -> listener.sessionDidActivate(event));
        }
    }

    private static List<HttpSessionActivationListener> findListeners(ImmutableSession session) {
        ImmutableSessionAttributes attributes = session.getAttributes();
        return attributes.getAttributeNames().stream().map(name -> attributes.getAttribute(name)).filter(attribute -> attribute instanceof HttpSessionActivationListener).map(attribute -> (HttpSessionActivationListener) attribute).collect(Collectors.toList());
    }

    // Session decorator that performs scheduling on close().
    private class SchedulableSession implements Session<L> {
        private final Session<L> session;
        private final ImmutableSession immutableSession;

        SchedulableSession(Session<L> session, ImmutableSession immutableSession) {
            this.session = session;
            this.immutableSession = immutableSession;
        }

        @Override
        public String getId() {
            return this.session.getId();
        }

        @Override
        public SessionMetaData getMetaData() {
            if (!this.isValid()) {
                throw InfinispanWebLogger.ROOT_LOGGER.invalidSession(this.getId());
            }
            return this.session.getMetaData();
        }

        @Override
        public boolean isValid() {
            return this.session.isValid();
        }

        @Override
        public void invalidate() {
            this.session.invalidate();
        }

        @Override
        public SessionAttributes getAttributes() {
            if (!this.isValid()) {
                throw InfinispanWebLogger.ROOT_LOGGER.invalidSession(this.getId());
            }
            return this.session.getAttributes();
        }

        @Override
        public void close() {
            boolean valid = this.session.isValid();
            if (valid && InfinispanSessionManager.this.isPersistent()) {
                InfinispanSessionManager.this.triggerPrePassivationEvents(this.immutableSession);
            }
            this.session.close();
            if (valid) {
                InfinispanSessionManager.this.schedule(this.immutableSession.getId(), this.immutableSession.getMetaData());
            }
        }

        @Override
        public L getLocalContext() {
            return this.session.getLocalContext();
        }
    }
}
