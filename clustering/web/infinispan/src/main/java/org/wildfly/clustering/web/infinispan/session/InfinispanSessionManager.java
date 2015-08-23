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

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.servlet.http.HttpSession;
import javax.servlet.http.HttpSessionActivationListener;
import javax.servlet.http.HttpSessionBindingEvent;
import javax.servlet.http.HttpSessionBindingListener;
import javax.servlet.http.HttpSessionEvent;
import javax.servlet.http.HttpSessionListener;

import org.infinispan.Cache;
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
import org.infinispan.stream.CacheCollectors;
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
import org.wildfly.clustering.infinispan.spi.distribution.Locality;
import org.wildfly.clustering.infinispan.spi.distribution.SimpleLocality;
import org.wildfly.clustering.web.IdentifierFactory;
import org.wildfly.clustering.web.infinispan.logging.InfinispanWebLogger;
import org.wildfly.clustering.web.session.ImmutableHttpSessionAdapter;
import org.wildfly.clustering.web.session.ImmutableSession;
import org.wildfly.clustering.web.session.ImmutableSessionAttributes;
import org.wildfly.clustering.web.session.Session;
import org.wildfly.clustering.web.session.SessionAttributes;
import org.wildfly.clustering.web.session.SessionContext;
import org.wildfly.clustering.web.session.SessionManager;
import org.wildfly.clustering.web.session.SessionMetaData;

/**
 * Generic session manager implementation - independent of cache mapping strategy.
 * @author Paul Ferraro
 */
@Listener(primaryOnly = true)
public class InfinispanSessionManager<V, L> implements SessionManager<L, TransactionBatch> {
    private final SessionContext context;
    private final Batcher<TransactionBatch> batcher;
    private final Cache<String, ?> cache;
    private final SessionFactory<V, L> factory;
    private final IdentifierFactory<String> identifierFactory;
    private final CommandDispatcherFactory dispatcherFactory;
    private final NodeFactory<Address> nodeFactory;
    private final int maxActiveSessions;
    private volatile Time defaultMaxInactiveInterval = new Time(30, TimeUnit.MINUTES);
    private final boolean persistent;
    private final Invoker invoker = new RetryingInvoker(0, 10, 100);
    private final SessionIdentifierFilter filter = new SessionIdentifierFilter();
    private final Recordable<ImmutableSession> recorder;

    volatile CommandDispatcher<Scheduler> dispatcher;
    private volatile Scheduler scheduler;

    public InfinispanSessionManager(SessionFactory<V, L> factory, InfinispanSessionManagerConfiguration configuration) {
        this.factory = factory;
        this.cache = configuration.getCache();
        this.context = configuration.getSessionContext();
        this.identifierFactory = configuration.getIdentifierFactory();
        this.batcher = configuration.getBatcher();
        this.dispatcherFactory = configuration.getCommandDispatcherFactory();
        this.nodeFactory = configuration.getNodeFactory();
        this.maxActiveSessions = configuration.getMaxActiveSessions();
        this.recorder = configuration.getInactiveSessionRecorder();
        Configuration config = this.cache.getCacheConfiguration();
        // If cache is clustered or configured with a write-through cache store
        // then we need to trigger any HttpSessionActivationListeners per request
        // See SRV.7.7.2 Distributed Environments
        this.persistent = config.clustering().cacheMode().isClustered() || (config.persistence().usingStores() && !config.persistence().passivation());
    }

    @Override
    public void start() {
        if (this.recorder != null) {
            this.recorder.reset();
        }
        this.identifierFactory.start();
        final List<Scheduler> schedulers = new ArrayList<>(2);
        schedulers.add(new SessionExpirationScheduler(this.batcher, new ExpiredSessionRemover<>(this.factory)));
        if (this.maxActiveSessions >= 0) {
            schedulers.add(new SessionEvictionScheduler(this.cache.getName() + ".eviction", this.factory, this.dispatcherFactory, this.maxActiveSessions));
        }
        this.scheduler = new Scheduler() {
            @Override
            public void schedule(ImmutableSession session) {
                for (Scheduler scheduler: schedulers) {
                    scheduler.schedule(session);
                }
            }

            @Override
            public void cancel(String sessionId) {
                for (Scheduler scheduler: schedulers) {
                    scheduler.cancel(sessionId);
                }
            }

            @Override
            public void cancel(Locality locality) {
                for (Scheduler scheduler: schedulers) {
                    scheduler.cancel(locality);
                }
            }

            @Override
            public void close() {
                for (Scheduler scheduler: schedulers) {
                    scheduler.close();
                }
            }
        };
        this.dispatcher = this.dispatcherFactory.createCommandDispatcher(this.cache.getName() + ".schedulers", this.scheduler);
        this.cache.addListener(this, this.filter);
        this.schedule(this.cache, new SimpleLocality(false), new ConsistentHashLocality(this.cache));
    }

    @Override
    public void stop() {
        this.cache.removeListener(this);
        this.dispatcher.close();
        this.scheduler.close();
        this.identifierFactory.stop();
    }

    boolean isPersistent() {
        return this.persistent;
    }

    private void cancel(ImmutableSession session) {
        try {
            this.executeOnPrimaryOwner(session, new CancelSchedulerCommand(session.getId()));
        } catch (Exception e) {
            InfinispanWebLogger.ROOT_LOGGER.failedToCancelSession(e, session.getId());
        }
    }

    void schedule(ImmutableSession session) {
        try {
            this.executeOnPrimaryOwner(session, new ScheduleSchedulerCommand(session));
        } catch (Exception e) {
            InfinispanWebLogger.ROOT_LOGGER.failedToScheduleSession(e, session.getId());
        }
    }

    private void executeOnPrimaryOwner(final ImmutableSession session, final Command<Void, Scheduler> command) throws Exception {
        Callable<Void> task = new Callable<Void>() {
            @Override
            public Void call() throws Exception {
                // This should only go remote following a failover
                Node node = InfinispanSessionManager.this.locatePrimaryOwner(session);
                return InfinispanSessionManager.this.dispatcher.executeOnNode(command, node).get();
            }
        };
        this.invoker.invoke(task);
    }

    Node locatePrimaryOwner(ImmutableSession session) {
        DistributionManager dist = this.cache.getAdvancedCache().getDistributionManager();
        Address address = (dist != null) ? dist.getPrimaryLocation(session.getId()) : null;
        return (address != null) ? this.nodeFactory.createNode(address) : this.dispatcherFactory.getGroup().getLocalNode();
    }

    @Override
    public Batcher<TransactionBatch> getBatcher() {
        return this.batcher;
    }

    @Override
    public long getDefaultMaxInactiveInterval(TimeUnit unit) {
        return this.defaultMaxInactiveInterval.convert(unit);
    }

    @Override
    public void setDefaultMaxInactiveInterval(long value, TimeUnit unit) {
        this.defaultMaxInactiveInterval = new Time(value, unit);
    }

    @Override
    public String createIdentifier() {
        return this.identifierFactory.createIdentifier();
    }

    @Override
    public boolean containsSession(String id) {
        return this.cache.containsKey(id);
    }

    @Override
    public Session<L> findSession(String id) {
        V value = this.factory.findValue(id);
        if (value == null) {
            InfinispanWebLogger.ROOT_LOGGER.tracef("Session %s not found", id);
            return null;
        }
        Session<L> session = this.factory.createSession(id, value);
        if (session.getMetaData().isExpired()) {
            InfinispanWebLogger.ROOT_LOGGER.tracef("Session %s was found, but has expired", id);
            session.invalidate();
            return null;
        }
        ImmutableSession immutableSession = this.persistent ? this.factory.createImmutableSession(id, value) : session;
        this.cancel(immutableSession);
        if (this.persistent) {
            triggerPostActivationEvents(immutableSession);
        }
        return new SchedulableSession(session, immutableSession);
    }

    @Override
    public Session<L> createSession(String id) {
        Session<L> session = this.factory.createSession(id, this.factory.createValue(id, null));
        final Time time = this.defaultMaxInactiveInterval;
        session.getMetaData().setMaxInactiveInterval(time.getValue(), time.getUnit());
        return new SchedulableSession(session, session);
    }

    @Override
    public ImmutableSession viewSession(String id) {
        V value = this.factory.findValue(id);
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
        try (Stream<String> keys = this.cache.getAdvancedCache().withFlags(flags).keySet().stream()) {
            return keys.filter(this.filter).collect(CacheCollectors.serializableCollector(() -> Collectors.toSet()));
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
    public void activated(CacheEntryActivatedEvent<String, ?> event) {
        if (!event.isPre() && !this.persistent) {
            String id = event.getKey();
            InfinispanWebLogger.ROOT_LOGGER.tracef("Session %s was activated", id);
            V value = this.factory.findValue(id);
            if (value != null) {
                ImmutableSession session = this.factory.createImmutableSession(id, value);
                triggerPostActivationEvents(session);
            }
        }
    }

    @CacheEntryPassivated
    public void passivated(CacheEntryPassivatedEvent<String, ?> event) {
        if (event.isPre() && !this.persistent) {
            String id = event.getKey();
            InfinispanWebLogger.ROOT_LOGGER.tracef("Session %s will be passivated", id);
            V value = this.factory.findValue(id);
            if (value != null) {
                ImmutableSession session = this.factory.createImmutableSession(id, value);
                triggerPrePassivationEvents(session);
            }
        }
    }

    @CacheEntryRemoved
    public void removed(CacheEntryRemovedEvent<String, ?> event) {
        if (event.isPre()) {
            String id = event.getKey();
            InfinispanWebLogger.ROOT_LOGGER.tracef("Session %s will be removed", id);
            V value = this.factory.findValue(id);
            if (value != null) {
                ImmutableSession session = this.factory.createImmutableSession(id, value);
                ImmutableSessionAttributes attributes = session.getAttributes();

                HttpSession httpSession = new ImmutableHttpSessionAdapter(session);
                HttpSessionEvent sessionEvent = new HttpSessionEvent(httpSession);
                for (HttpSessionListener listener: this.context.getSessionListeners()) {
                    listener.sessionDestroyed(sessionEvent);
                }

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
        }
    }

    @DataRehashed
    public void dataRehashed(DataRehashedEvent<String, ?> event) {
        Cache<String, ?> cache = event.getCache();
        Address localAddress = cache.getCacheManager().getAddress();
        Locality oldLocality = new ConsistentHashLocality(localAddress, event.getConsistentHashAtStart());
        Locality newLocality = new ConsistentHashLocality(localAddress, event.getConsistentHashAtEnd());
        if (event.isPre()) {
            this.scheduler.cancel(newLocality);
        } else {
            this.schedule(cache, oldLocality, newLocality);
        }
    }

    private void schedule(Cache<String, ?> cache, Locality oldLocality, Locality newLocality) {
        Consumer<String> scheduler = key -> {
            Batch batch = this.batcher.createBatch();
            try {
                // We need to lookup the session to obtain its meta data
                V value = this.factory.findValue(key);
                if (value != null) {
                    this.scheduler.schedule(this.factory.createImmutableSession(key, value));
                }
            } finally {
                batch.discard();
            }
        };
        // Iterate over sessions in memory
        try (Stream<String> keys = cache.getAdvancedCache().withFlags(Flag.CACHE_MODE_LOCAL, Flag.SKIP_CACHE_LOAD).keySet().stream()) {
            // If we are the new primary owner of this session then schedule expiration of this session locally
            keys.filter(this.filter).filter(key -> !oldLocality.isLocal(key) && newLocality.isLocal(key)).forEach(scheduler);
        }
    }

    static void triggerPrePassivationEvents(ImmutableSession session) {
        List<HttpSessionActivationListener> listeners = findListeners(session);
        if (!listeners.isEmpty()) {
            HttpSessionEvent event = new HttpSessionEvent(new ImmutableHttpSessionAdapter(session));
            for (HttpSessionActivationListener listener: listeners) {
                listener.sessionWillPassivate(event);
            }
        }
    }

    static void triggerPostActivationEvents(ImmutableSession session) {
        List<HttpSessionActivationListener> listeners = findListeners(session);
        if (!listeners.isEmpty()) {
            HttpSessionEvent event = new HttpSessionEvent(new ImmutableHttpSessionAdapter(session));
            for (HttpSessionActivationListener listener: listeners) {
                listener.sessionDidActivate(event);
            }
        }
    }

    private static List<HttpSessionActivationListener> findListeners(ImmutableSession session) {
        ImmutableSessionAttributes attributes = session.getAttributes();
        Set<String> names = attributes.getAttributeNames();
        List<HttpSessionActivationListener> listeners = new ArrayList<>(names.size());
        for (String name: names) {
            Object attribute = attributes.getAttribute(name);
            if (attribute instanceof HttpSessionActivationListener) {
                listeners.add((HttpSessionActivationListener) attribute);
            }
        }
        return listeners;
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
        public SessionContext getContext() {
            return this.session.getContext();
        }

        @Override
        public void close() {
            if (isPersistent()) {
                triggerPrePassivationEvents(this.immutableSession);
            }
            this.session.close();
            InfinispanSessionManager.this.schedule(this.immutableSession);
        }

        @Override
        public L getLocalContext() {
            return this.session.getLocalContext();
        }
    }
}
