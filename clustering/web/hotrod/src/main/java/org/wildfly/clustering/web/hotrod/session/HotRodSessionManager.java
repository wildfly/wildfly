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
package org.wildfly.clustering.web.hotrod.session;

import java.time.Duration;
import java.util.Collections;
import java.util.Map;
import java.util.Set;

import javax.servlet.ServletContext;

import org.wildfly.clustering.Registrar;
import org.wildfly.clustering.Registration;
import org.wildfly.clustering.ee.Batcher;
import org.wildfly.clustering.ee.cache.tx.TransactionBatch;
import org.wildfly.clustering.web.IdentifierFactory;
import org.wildfly.clustering.web.cache.session.ImmutableSessionActivationNotifier;
import org.wildfly.clustering.web.cache.session.SessionFactory;
import org.wildfly.clustering.web.cache.session.SimpleImmutableSession;
import org.wildfly.clustering.web.hotrod.Logger;
import org.wildfly.clustering.web.session.ImmutableSession;
import org.wildfly.clustering.web.session.Session;
import org.wildfly.clustering.web.session.SessionAttributes;
import org.wildfly.clustering.web.session.SessionExpirationListener;
import org.wildfly.clustering.web.session.SessionManager;
import org.wildfly.clustering.web.session.SessionMetaData;

/**
 * Generic HotRod-based session manager implementation - independent of cache mapping strategy.
 * @author Paul Ferraro
 */
public class HotRodSessionManager<MV, AV, L> implements SessionManager<L, TransactionBatch> {
    private final Registrar<SessionExpirationListener> expirationRegistrar;
    private final SessionExpirationListener expirationListener;
    private final Scheduler expirationScheduler;
    private final SessionFactory<MV, AV, L> factory;
    private final IdentifierFactory<String> identifierFactory;
    private final ServletContext context;
    private final Batcher<TransactionBatch> batcher;
    private final Duration stopTimeout;

    private volatile Duration defaultMaxInactiveInterval = Duration.ofMinutes(30L);
    private volatile Registration expirationRegistration;

    public HotRodSessionManager(SessionFactory<MV, AV, L> factory, HotRodSessionManagerConfiguration configuration) {
        this.factory = factory;
        this.expirationRegistrar = configuration.getExpirationRegistrar();
        this.expirationListener = configuration.getExpirationListener();
        this.expirationScheduler = configuration.getExpirationScheduler();
        this.context = configuration.getServletContext();
        this.identifierFactory = configuration.getIdentifierFactory();
        this.batcher = configuration.getBatcher();
        this.stopTimeout = configuration.getStopTimeout();
    }

    @Override
    public void start() {
        this.expirationRegistration = this.expirationRegistrar.register(this.expirationListener);
    }

    @Override
    public void stop() {
        this.expirationRegistration.close();
    }

    @Override
    public Duration getStopTimeout() {
        return this.stopTimeout;
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
        Map.Entry<MV, AV> entry = this.factory.findValue(id);
        if (entry == null) {
            Logger.ROOT_LOGGER.tracef("Session %s not found", id);
            return null;
        }
        ImmutableSession session = this.factory.createImmutableSession(id, entry);
        if (session.getMetaData().isExpired()) {
            Logger.ROOT_LOGGER.tracef("Session %s was found, but has expired", id);
            this.expirationListener.sessionExpired(session);
            this.factory.remove(id);
            return null;
        }
        this.expirationScheduler.cancel(id);
        this.triggerPostActivationEvents(session);
        return new SchedulableSession(this.factory.createSession(id, entry), session);
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
        Map.Entry<MV, AV> entry = this.factory.findValue(id);
        return (entry != null) ? new SimpleImmutableSession(this.factory.createImmutableSession(id, entry)) : null;
    }

    @Override
    public Set<String> getActiveSessions() {
        return Collections.emptySet();
    }

    @Override
    public Set<String> getLocalSessions() {
        return Collections.emptySet();
    }

    @Override
    public long getActiveSessionCount() {
        return this.getActiveSessions().size();
    }

    void triggerPrePassivationEvents(ImmutableSession session) {
        new ImmutableSessionActivationNotifier(session, this.context).prePassivate();
    }

    void triggerPostActivationEvents(ImmutableSession session) {
        new ImmutableSessionActivationNotifier(session, this.context).postActivate();
    }

    void schedule(ImmutableSession session) {
        this.expirationScheduler.schedule(session.getId(), session.getMetaData());
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
                throw Logger.ROOT_LOGGER.invalidSession(this.getId());
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
                throw Logger.ROOT_LOGGER.invalidSession(this.getId());
            }
            this.session.invalidate();
        }

        @Override
        public SessionAttributes getAttributes() {
            if (!this.session.isValid()) {
                throw Logger.ROOT_LOGGER.invalidSession(this.getId());
            }
            return this.session.getAttributes();
        }

        @Override
        public void close() {
            boolean valid = this.session.isValid();
            if (valid) {
                HotRodSessionManager.this.triggerPrePassivationEvents(this.immutableSession);
            }
            this.session.close();
            if (valid) {
                HotRodSessionManager.this.schedule(this.immutableSession);
            }
        }

        @Override
        public L getLocalContext() {
            return this.session.getLocalContext();
        }
    }
}
