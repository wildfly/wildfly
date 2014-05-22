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
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

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
import org.infinispan.distribution.ch.ConsistentHash;
import org.infinispan.notifications.KeyFilter;
import org.infinispan.notifications.Listener;
import org.infinispan.notifications.cachelistener.annotation.CacheEntryActivated;
import org.infinispan.notifications.cachelistener.annotation.CacheEntryPassivated;
import org.infinispan.notifications.cachelistener.annotation.CacheEntryRemoved;
import org.infinispan.notifications.cachelistener.annotation.TopologyChanged;
import org.infinispan.notifications.cachelistener.event.CacheEntryActivatedEvent;
import org.infinispan.notifications.cachelistener.event.CacheEntryPassivatedEvent;
import org.infinispan.notifications.cachelistener.event.CacheEntryRemovedEvent;
import org.infinispan.notifications.cachelistener.event.TopologyChangedEvent;
import org.infinispan.remoting.transport.Address;
import org.jboss.as.clustering.concurrent.Scheduler;
import org.wildfly.clustering.dispatcher.CommandDispatcher;
import org.wildfly.clustering.dispatcher.CommandDispatcherFactory;
import org.wildfly.clustering.group.Node;
import org.wildfly.clustering.group.NodeFactory;
import org.wildfly.clustering.web.Batcher;
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
@Listener
public class InfinispanSessionManager<V, L> implements SessionManager<L>, KeyFilter {
    private final SessionContext context;
    private final Batcher batcher;
    private final Cache<String, ?> cache;
    private final SessionFactory<V, L> factory;
    private final IdentifierFactory<String> identifierFactory;
    private final CommandDispatcherFactory dispatcherFactory;
    private final NodeFactory<Address> nodeFactory;
    private final int maxActiveSessions;
    private volatile Time defaultMaxInactiveInterval = new Time(30, TimeUnit.MINUTES);
    private final boolean persistent;
    private volatile SchedulerContext schedulerContext;
    private volatile CommandDispatcher<SchedulerContext> dispatcher;

    public InfinispanSessionManager(SessionFactory<V, L> factory, InfinispanSessionManagerConfiguration configuration) {
        this.factory = factory;
        this.cache = configuration.getCache();
        this.context = configuration.getSessionContext();
        this.identifierFactory = configuration.getIdentifierFactory();
        this.batcher = configuration.getBatcher();
        this.dispatcherFactory = configuration.getCommandDispatcherFactory();
        this.nodeFactory = configuration.getNodeFactory();
        this.maxActiveSessions = configuration.getMaxActiveSessions();
        Configuration config = this.cache.getCacheConfiguration();
        // If cache is clustered or configured with a write-through cache store
        // then we need to trigger any HttpSessionActivationListeners per request
        // See SRV.7.7.2 Distributed Environments
        this.persistent = config.clustering().cacheMode().isClustered() || (config.persistence().usingStores() && !config.persistence().passivation());
    }

    @Override
    public void start() {
        this.identifierFactory.start();
        final List<Scheduler<ImmutableSession>> schedulers = new ArrayList<>(2);
        schedulers.add(new SessionExpirationScheduler(this.batcher, new ExpiredSessionRemover<>(this.factory)));
        if (this.maxActiveSessions >= 0) {
            schedulers.add(new SessionEvictionScheduler(this.cache.getName() + ".eviction", this.batcher, this.factory, this.dispatcherFactory, this.maxActiveSessions));
        }
        this.schedulerContext = new SchedulerContext() {
            @Override
            public void schedule(ImmutableSession session) {
                for (Scheduler<ImmutableSession> scheduler: schedulers) {
                    scheduler.schedule(session);
                }
            }

            @Override
            public void cancel(ImmutableSession session) {
                for (Scheduler<ImmutableSession> scheduler: schedulers) {
                    scheduler.cancel(session);
                }
            }

            @Override
            public void close() {
                for (Scheduler<?> scheduler: schedulers) {
                    scheduler.close();
                }
            }
        };
        this.dispatcher = this.dispatcherFactory.createCommandDispatcher(this.cache.getName() + ".schedulers", this.schedulerContext);
        this.cache.addListener(this, this);
    }

    @Override
    public void stop() {
        this.cache.removeListener(this);
        this.dispatcher.close();
        this.schedulerContext.close();
        this.identifierFactory.stop();
    }

    boolean isPersistent() {
        return this.persistent;
    }

    private void cancel(ImmutableSession session) {
        // This should only go remote following a failover
        this.dispatcher.executeOnNode(new CancelSchedulerCommand(session), this.locatePrimaryOwner(session));
    }

    void schedule(ImmutableSession session) {
        // This should only go remote following a failover
        this.dispatcher.executeOnNode(new ScheduleSchedulerCommand(session), this.locatePrimaryOwner(session));
    }

    private Node locatePrimaryOwner(ImmutableSession session) {
        DistributionManager dist = this.cache.getAdvancedCache().getDistributionManager();
        Address address = (dist != null) ? dist.getPrimaryLocation(session.getId()) : null;
        return (address != null) ? this.nodeFactory.createNode(address) : this.dispatcherFactory.getGroup().getLocalNode();
    }

    @Override
    public boolean accept(Object key) {
        return key instanceof String;
    }

    @Override
    public Batcher getBatcher() {
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
        this.cancel(session);
        if (this.persistent) {
            triggerPostActivationEvents(session);
        }
        return new SchedulableSession(session);
    }

    @Override
    public Session<L> createSession(String id) {
        Session<L> session = this.factory.createSession(id, this.factory.createValue(id));
        final Time time = this.defaultMaxInactiveInterval;
        session.getMetaData().setMaxInactiveInterval(time.getValue(), time.getUnit());
        return new SchedulableSession(session);
    }

    @Override
    public ImmutableSession viewSession(String id) {
        V value = this.factory.findValue(id);
        return (value != null) ? new SimpleImmutableSession(this.factory.createImmutableSession(id, value)) : null;
    }

    @Override
    public Set<String> getActiveSessions() {
        // Omit remote sessions (i.e. when using DIST mode) as well as passivated sessions
        return this.getSessions(Flag.CACHE_MODE_LOCAL, Flag.SKIP_CACHE_LOAD, Flag.SKIP_LOCKING);
    }

    @Override
    public Set<String> getLocalSessions() {
        // Omit remote sessions (i.e. when using DIST mode)
        return this.getSessions(Flag.CACHE_MODE_LOCAL, Flag.SKIP_LOCKING);
    }

    private Set<String> getSessions(Flag... flags) {
        Set<String> result = new HashSet<>();
        for (Object key: this.cache.getAdvancedCache().withFlags(flags).keySet()) {
            if (key instanceof String) {
                result.add((String) key);
            }
        }
        return result;
    }

    @CacheEntryActivated
    public void activated(CacheEntryActivatedEvent<String, ?> event) {
        if (!event.isPre() && !this.persistent) {
            String id = event.getKey();
            InfinispanWebLogger.ROOT_LOGGER.tracef("Session %s was activated", id);
            ImmutableSession session = this.factory.createImmutableSession(id, this.factory.findValue(id));
            triggerPostActivationEvents(session);
        }
    }

    @CacheEntryPassivated
    public void passivated(CacheEntryPassivatedEvent<String, ?> event) {
        if (event.isPre() && !this.persistent) {
            String id = event.getKey();
            InfinispanWebLogger.ROOT_LOGGER.tracef("Session %s will be passivated", id);
            ImmutableSession session = this.factory.createSession(id, this.factory.findValue(id));
            triggerPrePassivationEvents(session);
        }
    }

    @CacheEntryRemoved
    public void removed(CacheEntryRemovedEvent<String, ?> event) {
        if (event.isPre() && event.isOriginLocal()) {
            String id = event.getKey();
            InfinispanWebLogger.ROOT_LOGGER.tracef("Session %s will be removed", id);
            ImmutableSession session = this.factory.createImmutableSession(id, this.factory.findValue(id));
            ImmutableSessionAttributes attributes = session.getAttributes();

            HttpSession httpSession = new ImmutableHttpSessionAdapter(session);
            HttpSessionEvent sessionEvent = new HttpSessionEvent(httpSession);
            for (HttpSessionListener listener: this.context.getSessionListeners()) {
                listener.sessionDestroyed(sessionEvent);
            }

            for (String attribute: attributes.getAttributeNames()) {
                Object value = attributes.getAttribute(attribute);
                if (value instanceof HttpSessionBindingListener) {
                    HttpSessionBindingListener listener = (HttpSessionBindingListener) value;
                    listener.valueUnbound(new HttpSessionBindingEvent(httpSession, attribute, value));
                }
            }
        }
    }

    @TopologyChanged
    public void topologyChanged(TopologyChangedEvent<String, ?> event) {
        if (event.isPre()) return;

        Cache<String, ?> cache = event.getCache();
        Address localAddress = cache.getCacheManager().getAddress();
        ConsistentHash oldHash = event.getConsistentHashAtStart();
        ConsistentHash newHash = event.getConsistentHashAtEnd();
        Set<Address> oldAddresses = new HashSet<>(oldHash.getMembers());
        // Find members that left this cache view
        oldAddresses.removeAll(newHash.getMembers());
        if (!oldAddresses.isEmpty()) {
            // Iterate over sessions in memory
            for (Object key: cache.getAdvancedCache().withFlags(Flag.CACHE_MODE_LOCAL, Flag.SKIP_CACHE_LOAD, Flag.SKIP_LOCKING).keySet()) {
                // Cache may contain non-string keys, so ignore any others
                if (this.accept(key)) {
                    String sessionId = (String) key;
                    Address oldOwner = oldHash.locatePrimaryOwner(sessionId);
                    // If the old owner of this session has left the cache view...
                    if (oldAddresses.contains(oldOwner)) {
                        Address newOwner = newHash.locatePrimaryOwner(sessionId);
                        // And if we are the new primary owner of this session...
                        if (localAddress.equals(newOwner)) {
                            // Then schedule expiration of this session locally
                            boolean started = cache.startBatch();
                            try {
                                V value = this.factory.findValue(sessionId);
                                if (value != null) {
                                    InfinispanWebLogger.ROOT_LOGGER.debugf("Scheduling expiration of session %s on behalf of previous owner: %s", sessionId, oldOwner);
                                    ImmutableSession session = this.factory.createImmutableSession(sessionId, value);
                                    this.schedulerContext.cancel(session);
                                    this.schedulerContext.schedule(session);
                                }
                            } finally {
                                if (started) {
                                    cache.endBatch(false);
                                }
                            }
                        } else {
                            InfinispanWebLogger.ROOT_LOGGER.tracef("Expiration of session %s will be scheduled by node %s on behalf of previous owner: %s", sessionId, newOwner, oldOwner);
                        }
                    }
                }
            }
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

        SchedulableSession(Session<L> session) {
            this.session = session;
        }

        @Override
        public String getId() {
            return this.session.getId();
        }

        @Override
        public SessionMetaData getMetaData() {
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
            return this.session.getAttributes();
        }

        @Override
        public SessionContext getContext() {
            return this.session.getContext();
        }

        @Override
        public void close() {
            if (isPersistent()) {
                triggerPrePassivationEvents(this.session);
            }
            this.session.close();
            InfinispanSessionManager.this.schedule(this.session);
        }

        @Override
        public L getLocalContext() {
            return this.session.getLocalContext();
        }
    }
}
