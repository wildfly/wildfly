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
import java.util.concurrent.CompletionStage;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpSession;
import javax.servlet.http.HttpSessionActivationListener;
import javax.servlet.http.HttpSessionBindingEvent;
import javax.servlet.http.HttpSessionBindingListener;
import javax.servlet.http.HttpSessionEvent;

import org.infinispan.Cache;
import org.infinispan.context.Flag;
import org.infinispan.distribution.DistributionManager;
import org.infinispan.notifications.Listener;
import org.infinispan.notifications.cachelistener.annotation.CacheEntryActivated;
import org.infinispan.notifications.cachelistener.annotation.CacheEntryPassivated;
import org.infinispan.notifications.cachelistener.annotation.CacheEntryRemoved;
import org.infinispan.notifications.cachelistener.event.CacheEntryActivatedEvent;
import org.infinispan.notifications.cachelistener.event.CacheEntryPassivatedEvent;
import org.infinispan.notifications.cachelistener.event.CacheEntryRemovedEvent;
import org.infinispan.notifications.cachelistener.filter.CacheEventFilter;
import org.infinispan.remoting.transport.Address;
import org.wildfly.clustering.Registrar;
import org.wildfly.clustering.Registration;
import org.wildfly.clustering.dispatcher.Command;
import org.wildfly.clustering.dispatcher.CommandDispatcher;
import org.wildfly.clustering.dispatcher.CommandDispatcherException;
import org.wildfly.clustering.ee.Batcher;
import org.wildfly.clustering.ee.Invoker;
import org.wildfly.clustering.ee.Recordable;
import org.wildfly.clustering.ee.infinispan.CacheProperties;
import org.wildfly.clustering.ee.infinispan.TransactionBatch;
import org.wildfly.clustering.ee.retry.RetryingInvoker;
import org.wildfly.clustering.group.Group;
import org.wildfly.clustering.group.Node;
import org.wildfly.clustering.infinispan.spi.PredicateKeyFilter;
import org.wildfly.clustering.infinispan.spi.distribution.CacheLocality;
import org.wildfly.clustering.infinispan.spi.distribution.Key;
import org.wildfly.clustering.infinispan.spi.distribution.Locality;
import org.wildfly.clustering.spi.NodeFactory;
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
import org.wildfly.common.function.ExceptionSupplier;

/**
 * Generic session manager implementation - independent of cache mapping strategy.
 * @author Paul Ferraro
 */
@Listener(primaryOnly = true)
public class InfinispanSessionManager<MV, AV, L> implements SessionManager<L, TransactionBatch> {

    private static final Invoker INVOKER = new RetryingInvoker(Duration.ZERO, Duration.ofMillis(10), Duration.ofMillis(100));

    private final Registrar<SessionExpirationListener> expirationRegistrar;
    private final SessionExpirationListener expirationListener;
    private final Batcher<TransactionBatch> batcher;
    private final Cache<Key<String>, ?> cache;
    private final CacheProperties properties;
    private final SessionFactory<MV, AV, L> factory;
    private final IdentifierFactory<String> identifierFactory;
    private final CommandDispatcher<Scheduler> dispatcher;
    private final Group group;
    private final NodeFactory<Address> memberFactory;
    private final Predicate<Object> filter = new SessionCreationMetaDataKeyFilter();
    private final Recordable<ImmutableSession> recorder;
    private final ServletContext context;

    private volatile Duration defaultMaxInactiveInterval = Duration.ofMinutes(30L);
    private volatile Registration expirationRegistration;

    public InfinispanSessionManager(SessionFactory<MV, AV, L> factory, InfinispanSessionManagerConfiguration configuration) {
        this.factory = factory;
        this.cache = configuration.getCache();
        this.properties = configuration.getProperties();
        this.expirationRegistrar = configuration.getExpirationRegistar();
        this.expirationListener = configuration.getExpirationListener();
        this.identifierFactory = configuration.getIdentifierFactory();
        this.batcher = configuration.getBatcher();
        this.dispatcher = configuration.getCommandDispatcher();
        this.group = configuration.getGroup();
        this.memberFactory = configuration.getMemberFactory();
        this.recorder = configuration.getInactiveSessionRecorder();
        this.context = configuration.getServletContext();
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

    boolean isPersistent() {
        return this.properties.isPersistent();
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

    private void executeOnPrimaryOwner(final String sessionId, final Command<Void, Scheduler> command) throws CommandDispatcherException {
        CommandDispatcher<Scheduler> dispatcher = this.dispatcher;
        ExceptionSupplier<CompletionStage<Void>, CommandDispatcherException> action = new ExceptionSupplier<CompletionStage<Void>, CommandDispatcherException>() {
            @Override
            public CompletionStage<Void> get() throws CommandDispatcherException {
                // This should only go remote following a failover
                Node node = InfinispanSessionManager.this.locatePrimaryOwner(sessionId);
                return dispatcher.executeOnMember(command, node);
            }
        };
        INVOKER.invoke(action).toCompletableFuture().join();
    }

    Node locatePrimaryOwner(String sessionId) {
        DistributionManager dist = this.cache.getAdvancedCache().getDistributionManager();
        Address address = (dist != null) ? dist.getCacheTopology().getDistribution(new Key<>(sessionId)).primary() : null;
        Node node = (address != null) ? this.memberFactory.createNode(address) : null;
        return (node != null) ? node : this.group.getLocalMember();
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
        if (this.properties.isPersistent()) {
            // If cache is clustered or configured with a write-through cache store
            // then we need to trigger any HttpSessionActivationListeners per request
            // See SRV.7.7.2 Distributed Environments
            this.triggerPostActivationEvents(session);
        }
        return new SchedulableSession(this.factory.createSession(id, value), session);
    }

    @Override
    public Session<L> createSession(String id) {
        Map.Entry<MV, AV> entry = this.factory.createValue(id, null);
        if (entry == null) return null;
        Session<L> session = this.factory.createSession(id, entry);
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
        if (!event.isPre() && !this.properties.isPersistent()) {
            String id = event.getKey().getValue();
            InfinispanWebLogger.ROOT_LOGGER.tracef("Session %s was activated", id);
            Map.Entry<MV, AV> value = this.factory.findValue(id);
            if (value != null) {
                ImmutableSession session = this.factory.createImmutableSession(id, value);
                this.triggerPostActivationEvents(session);
            }
        }
    }

    @CacheEntryPassivated
    public void passivated(CacheEntryPassivatedEvent<SessionCreationMetaDataKey, ?> event) {
        if (event.isPre() && !this.properties.isPersistent()) {
            String id = event.getKey().getValue();
            InfinispanWebLogger.ROOT_LOGGER.tracef("Session %s will be passivated", id);
            Map.Entry<MV, AV> value = this.factory.findValue(id);
            if (value != null) {
                ImmutableSession session = this.factory.createImmutableSession(id, value);
                this.triggerPrePassivationEvents(session);
            }
        }
    }

    @CacheEntryRemoved
    public void removed(CacheEntryRemovedEvent<SessionCreationMetaDataKey, ?> event) {
        if (event.isPre()) {
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
        }
    }

    void triggerPrePassivationEvents(ImmutableSession session) {
        List<HttpSessionActivationListener> listeners = findListeners(session);
        if (!listeners.isEmpty()) {
            HttpSessionEvent event = new HttpSessionEvent(new ImmutableHttpSessionAdapter(session, this.context));
            for (HttpSessionActivationListener listener : listeners) {
                listener.sessionWillPassivate(event);
            }
        }
    }

    void triggerPostActivationEvents(ImmutableSession session) {
        List<HttpSessionActivationListener> listeners = findListeners(session);
        if (!listeners.isEmpty()) {
            HttpSessionEvent event = new HttpSessionEvent(new ImmutableHttpSessionAdapter(session, this.context));
            for (HttpSessionActivationListener listener : listeners) {
                listener.sessionDidActivate(event);
            }
        }
    }

    private static List<HttpSessionActivationListener> findListeners(ImmutableSession session) {
        ImmutableSessionAttributes attributes = session.getAttributes();
        List<HttpSessionActivationListener> listeners = new ArrayList<>(attributes.getAttributeNames().size());
        for (String name : attributes.getAttributeNames()) {
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
            if (!this.session.isValid()) {
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
            if (!this.session.isValid()) {
                throw InfinispanWebLogger.ROOT_LOGGER.invalidSession(this.getId());
            }
            this.session.invalidate();
        }

        @Override
        public SessionAttributes getAttributes() {
            if (!this.session.isValid()) {
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
