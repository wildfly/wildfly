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

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.TimeUnit;

import javax.servlet.http.HttpSession;
import javax.servlet.http.HttpSessionActivationListener;
import javax.servlet.http.HttpSessionBindingEvent;
import javax.servlet.http.HttpSessionBindingListener;
import javax.servlet.http.HttpSessionEvent;
import javax.servlet.http.HttpSessionListener;

import org.infinispan.Cache;
import org.infinispan.affinity.KeyAffinityService;
import org.infinispan.affinity.KeyGenerator;
import org.infinispan.distribution.DistributionManager;
import org.infinispan.notifications.Listener;
import org.infinispan.notifications.cachelistener.annotation.CacheEntryActivated;
import org.infinispan.notifications.cachelistener.annotation.CacheEntryPassivated;
import org.infinispan.notifications.cachelistener.annotation.CacheEntryRemoved;
import org.infinispan.notifications.cachelistener.event.CacheEntryActivatedEvent;
import org.infinispan.notifications.cachelistener.event.CacheEntryPassivatedEvent;
import org.infinispan.notifications.cachelistener.event.CacheEntryRemovedEvent;
import org.infinispan.remoting.transport.Address;
import org.infinispan.util.concurrent.ConcurrentHashSet;
import org.jboss.as.clustering.infinispan.affinity.KeyAffinityServiceFactory;
import org.jboss.as.clustering.registry.Registry;
import org.jboss.metadata.web.jboss.JBossWebMetaData;
import org.wildfly.clustering.web.Batcher;
import org.wildfly.clustering.web.infinispan.InfinispanWebLogger;
import org.wildfly.clustering.web.infinispan.Scheduler;
import org.wildfly.clustering.web.session.ImmutableHttpSessionAdapter;
import org.wildfly.clustering.web.session.ImmutableSession;
import org.wildfly.clustering.web.session.ImmutableSessionAttributes;
import org.wildfly.clustering.web.session.Session;
import org.wildfly.clustering.web.session.SessionAttributes;
import org.wildfly.clustering.web.session.SessionContext;
import org.wildfly.clustering.web.session.SessionIdentifierFactory;
import org.wildfly.clustering.web.session.SessionManager;
import org.wildfly.clustering.web.session.SessionMetaData;

/**
 * Generic session manager implementation - independent of cache mapping strategy.
 * @author Paul Ferraro
 */
@Listener
public class InfinispanSessionManager<V, L> implements SessionManager<L>, KeyGenerator<String>, Batcher {
    private final SessionContext context;
    private final Cache<String, V> cache;
    private final SessionFactory<V, L> factory;
    private final SessionIdentifierFactory idFactory;
    private final KeyAffinityService<String> affinity;
    private final Registry<String, Void> registry;
    private final List<Scheduler<Session<L>>> schedulers = new CopyOnWriteArrayList<>();
    private volatile Time defaultMaxInactiveInterval = new Time(30, TimeUnit.MINUTES);
    private final Set<String> activeSessions = new ConcurrentHashSet<>();
    private final int maxActiveSessions;

    public InfinispanSessionManager(SessionContext context, SessionIdentifierFactory idFactory, Cache<String, V> cache, SessionFactory<V, L> factory, KeyAffinityServiceFactory affinityFactory, Registry<String, Void> registry, JBossWebMetaData metaData) {
        this.context = context;
        this.factory = factory;
        this.idFactory = idFactory;
        this.cache = cache;
        this.affinity = affinityFactory.createService(this.cache, this);
        this.registry = registry;
        this.maxActiveSessions = metaData.getMaxActiveSessions().intValue();
    }

    @Override
    public void start() {
        this.cache.addListener(this);
        this.affinity.start();
        this.schedulers.add(new SessionExpirationScheduler<L>(this, new ExpiredSessionRemover<>(this.factory)));
        if (this.maxActiveSessions > 0) {
            this.schedulers.add(new SessionEvictionScheduler<L>(this, this.factory, this.maxActiveSessions));
        }
    }

    @Override
    public void stop() {
        for (Scheduler<?> scheduler: this.schedulers) {
            scheduler.close();
        }
        this.schedulers.clear();
        this.affinity.stop();
        this.cache.removeListener(this);
    }

    @Override
    public boolean startBatch() {
        return this.cache.startBatch();
    }

    @Override
    public void endBatch(final boolean successful) {
        this.cache.endBatch(successful);
    }

    @Override
    public String locate(String sessionId) {
        Address location = this.locatePrimaryOwner(sessionId);
        if ((location != null) && !location.equals(this.cache.getCacheManager().getAddress())) {
            // Lookup jvm route for address
            Map.Entry<String, Void> entry = this.registry.getRemoteEntry(location);
            if (entry != null) {
                return entry.getKey();
            }
        }
        Map.Entry<String, Void> entry = this.registry.getLocalEntry();
        if (entry == null) {
            // Accommodate mod_cluster's lazy jvm route auto-generation
            entry = this.registry.refreshLocalEntry();
        }
        return (entry != null) ? entry.getKey() : null;
    }

    private Address locatePrimaryOwner(String sessionId) {
        DistributionManager dist = this.cache.getAdvancedCache().getDistributionManager();
        return (dist != null) ? dist.getPrimaryLocation(sessionId) : this.cache.getCacheManager().getAddress();
    }

    @Override
    public Batcher getBatcher() {
        return this;
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
    public String getKey() {
        return this.idFactory.createSessionId();
    }

    @Override
    public String createSessionId() {
        return this.affinity.getKeyForAddress(this.cache.getCacheManager().getAddress());
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
        for (Scheduler<Session<L>> scheduler: this.schedulers) {
            scheduler.cancel(session);
        }
        this.activeSessions.add(id);
        return new SchedulableSession<>(session, this.schedulers);
    }

    @Override
    public Session<L> createSession(String id) {
        Session<L> session = this.factory.createSession(id, this.factory.createValue(id));
        final Time time = this.defaultMaxInactiveInterval;
        session.getMetaData().setMaxInactiveInterval(time.getValue(), time.getUnit());
        this.activeSessions.add(id);
        return new SchedulableSession<>(session, this.schedulers);
    }

    @Override
    public int size() {
        return this.activeSessions.size();
    }

    @CacheEntryActivated
    public void activated(CacheEntryActivatedEvent<String, ?> event) {
        // Cache may contain non-string keys, so ignore any others
        if (!event.isPre() && event.isOriginLocal() && (event.getKey() instanceof String)) {
            String id = event.getKey();
            InfinispanWebLogger.ROOT_LOGGER.tracef("Session %s was activated", id);
            ImmutableSession session = this.factory.createImmutableSession(id, this.factory.findValue(id));
            ImmutableSessionAttributes attributes = session.getAttributes();

            HttpSessionEvent sessionEvent = new HttpSessionEvent(new ImmutableHttpSessionAdapter(session));
            for (String attribute: attributes.getAttributeNames()) {
                Object value = attributes.getAttribute(attribute);
                if (value instanceof HttpSessionActivationListener) {
                    HttpSessionActivationListener listener = (HttpSessionActivationListener) value;
                    listener.sessionDidActivate(sessionEvent);
                }
            }
        }
    }

    @CacheEntryPassivated
    public void passivated(CacheEntryPassivatedEvent<String, ?> event) {
        // Cache may contain non-string keys, so ignore any others
        if (event.isPre() && (event.getKey() instanceof String)) {
            String id = event.getKey();
            this.activeSessions.remove(id);
            if (event.isOriginLocal()) {
                InfinispanWebLogger.ROOT_LOGGER.tracef("Session %s will be passivated", id);
                ImmutableSession session = this.factory.createSession(id, this.factory.findValue(id));
                ImmutableSessionAttributes attributes = session.getAttributes();

                HttpSessionEvent sessionEvent = new HttpSessionEvent(new ImmutableHttpSessionAdapter(session));
                for (String attribute: attributes.getAttributeNames()) {
                    Object value = attributes.getAttribute(attribute);
                    if (value instanceof HttpSessionActivationListener) {
                        HttpSessionActivationListener listener = (HttpSessionActivationListener) value;
                        listener.sessionWillPassivate(sessionEvent);
                    }
                }
            }
        }
    }

    @CacheEntryRemoved
    public void removed(CacheEntryRemovedEvent<String, ?> event) {
        // Cache may contain non-string keys, so ignore any others
        if (event.isPre() && event.isOriginLocal() && (event.getKey() instanceof String)) {
            String id = event.getKey();
            this.activeSessions.remove(id);
            if (event.isOriginLocal()) {
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
    }

    // Session decorator that performs scheduling on close().
    private static class SchedulableSession<L> implements Session<L> {
        private final Session<L> session;
        private final List<Scheduler<Session<L>>> schedulers;

        SchedulableSession(Session<L> session, List<Scheduler<Session<L>>> schedulers) {
            this.session = session;
            this.schedulers = schedulers;
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
            this.session.close();
            for (Scheduler<Session<L>> scheduler: this.schedulers) {
                scheduler.schedule(this.session);
            }
        }

        @Override
        public L getLocalContext() {
            return this.session.getLocalContext();
        }
    }
}
