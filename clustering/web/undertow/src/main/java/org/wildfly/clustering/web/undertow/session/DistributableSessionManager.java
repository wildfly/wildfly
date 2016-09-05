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
package org.wildfly.clustering.web.undertow.session;

import io.undertow.UndertowMessages;
import io.undertow.server.HttpServerExchange;
import io.undertow.server.session.SessionConfig;
import io.undertow.server.session.SessionListener;
import io.undertow.server.session.SessionListeners;
import io.undertow.server.session.SessionManagerStatistics;

import java.time.Duration;
import java.util.Base64;
import java.util.Collections;
import java.util.Set;

import org.wildfly.clustering.ee.Batch;
import org.wildfly.clustering.ee.Batcher;
import org.wildfly.clustering.web.session.ImmutableSession;
import org.wildfly.clustering.web.session.Session;
import org.wildfly.clustering.web.session.SessionManager;
import org.wildfly.clustering.web.undertow.logging.UndertowClusteringLogger;

/**
 * Adapts a distributable {@link SessionManager} to an Undertow {@link io.undertow.server.session.SessionManager}.
 * @author Paul Ferraro
 */
public class DistributableSessionManager implements UndertowSessionManager {

    private final String deploymentName;
    private final SessionListeners listeners;
    private final SessionManager<LocalSessionContext, Batch> manager;
    private final RecordableSessionManagerStatistics statistics;

    public DistributableSessionManager(String deploymentName, SessionManager<LocalSessionContext, Batch> manager, SessionListeners listeners, RecordableSessionManagerStatistics statistics) {
        this.deploymentName = deploymentName;
        this.manager = manager;
        this.listeners = listeners;
        this.statistics = statistics;
    }

    @Override
    public SessionListeners getSessionListeners() {
        return this.listeners;
    }

    @Override
    public SessionManager<LocalSessionContext, Batch> getSessionManager() {
        return this.manager;
    }

    @Override
    public void start() {
        this.manager.start();
        if (this.statistics != null) {
            this.statistics.reset();
        }
    }

    @Override
    public void stop() {
        this.manager.stop();
    }

    @Override
    public io.undertow.server.session.Session createSession(HttpServerExchange exchange, SessionConfig config) {
        if (config == null) {
            throw UndertowMessages.MESSAGES.couldNotFindSessionCookieConfig();
        }

        Batcher<Batch> batcher = this.manager.getBatcher();
        // Batch will be closed by Session.close();
        @SuppressWarnings("resource")
        Batch batch = batcher.createBatch();
        try {
            String requestedSessionId = config.findSessionId(exchange);
            String id = (requestedSessionId == null) ? this.manager.createIdentifier() : requestedSessionId;
            Session<LocalSessionContext> session = this.manager.createSession(id);
            if (session == null) {
                throw UndertowClusteringLogger.ROOT_LOGGER.sessionAlreadyExists(id);
            }
            if (requestedSessionId == null) {
                config.setSessionId(exchange, id);
            }

            io.undertow.server.session.Session adapter = new DistributableSession(this, session, config, batcher.suspendBatch());
            this.listeners.sessionCreated(adapter, exchange);
            if (this.statistics != null) {
                this.statistics.record(adapter);
            }
            return adapter;
        } catch (RuntimeException | Error e) {
            batch.discard();
            batch.close();
            throw e;
        }
    }

    @Override
    public io.undertow.server.session.Session getSession(HttpServerExchange exchange, SessionConfig config) {
        if (config == null) {
            throw UndertowMessages.MESSAGES.couldNotFindSessionCookieConfig();
        }

        Batcher<Batch> batcher = this.manager.getBatcher();
        @SuppressWarnings("resource")
        Batch batch = batcher.createBatch();
        try {
            String id = config.findSessionId(exchange);
            if (id == null) {
                batch.close();
                return null;
            }

            // If requested id contains invalid characters, then session cannot exist and would otherwise cause session lookup to fail
            try {
                Base64.getUrlDecoder().decode(id);
            } catch (IllegalArgumentException e) {
                batch.close();
                return null;
            }

            Session<LocalSessionContext> session = this.manager.findSession(id);
            if (session == null) {
                batch.close();
                return null;
            }
            return new DistributableSession(this, session, config, batcher.suspendBatch());
        } catch (RuntimeException | Error e) {
            batch.discard();
            batch.close();
            throw e;
        }
    }

    @Override
    public void registerSessionListener(SessionListener listener) {
        this.listeners.addSessionListener(listener);
    }

    @Override
    public void removeSessionListener(SessionListener listener) {
        this.listeners.removeSessionListener(listener);
    }

    @Override
    public void setDefaultSessionTimeout(int timeout) {
        this.manager.setDefaultMaxInactiveInterval(Duration.ofSeconds(timeout));
    }

    @Override
    public Set<String> getTransientSessions() {
        // We are a distributed session manager, so none of our sessions are transient
        return Collections.emptySet();
    }

    @Override
    public Set<String> getActiveSessions() {
        return this.manager.getActiveSessions();
    }

    @Override
    public Set<String> getAllSessions() {
        return this.manager.getLocalSessions();
    }

    @Override
    public io.undertow.server.session.Session getSession(String sessionId) {
        try (Batch batch = this.manager.getBatcher().createBatch()) {
            try {
                ImmutableSession session = this.manager.viewSession(sessionId);
                return (session != null) ? new DistributableImmutableSession(this, session) : null;
            } catch (RuntimeException | Error e) {
                batch.discard();
                throw e;
            }
        }
    }

    @Override
    public String getDeploymentName() {
        return this.deploymentName;
    }

    @Override
    public SessionManagerStatistics getStatistics() {
        return this.statistics;
    }

    @Override
    public boolean equals(Object object) {
        if (!(object instanceof DistributableSessionManager)) return false;
        DistributableSessionManager manager = (DistributableSessionManager) object;
        return this.deploymentName.equals(manager.getDeploymentName());
    }

    @Override
    public int hashCode() {
        return this.deploymentName.hashCode();
    }

    @Override
    public String toString() {
        return this.deploymentName;
    }
}
