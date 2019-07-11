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
import org.wildfly.clustering.web.cache.session.Scheduler;
import org.wildfly.clustering.web.cache.session.SessionFactory;
import org.wildfly.clustering.web.cache.session.SimpleImmutableSession;
import org.wildfly.clustering.web.cache.session.ValidSession;
import org.wildfly.clustering.web.hotrod.Logger;
import org.wildfly.clustering.web.session.ImmutableSession;
import org.wildfly.clustering.web.session.Session;
import org.wildfly.clustering.web.session.SessionExpirationListener;
import org.wildfly.clustering.web.session.SessionManager;

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
        return new ValidSession<>(this.factory.createSession(id, entry, this.context), this.expirationScheduler);
    }

    @Override
    public Session<L> createSession(String id) {
        Map.Entry<MV, AV> entry = this.factory.createValue(id, null);
        if (entry == null) return null;
        Session<L> session = this.factory.createSession(id, entry, this.context);
        session.getMetaData().setMaxInactiveInterval(this.defaultMaxInactiveInterval);
        return new ValidSession<>(session, this.expirationScheduler);
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
}
