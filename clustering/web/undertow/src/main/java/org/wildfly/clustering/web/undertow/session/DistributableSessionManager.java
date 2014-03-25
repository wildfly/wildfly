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

import java.util.Collections;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import org.wildfly.clustering.web.Batch;
import org.wildfly.clustering.web.session.ImmutableSession;
import org.wildfly.clustering.web.session.Session;
import org.wildfly.clustering.web.session.SessionManager;

/**
 * Adapts a distributable {@link SessionManager} to an Undertow {@link io.undertow.server.session.SessionManager}.
 * @author Paul Ferraro
 */
public class DistributableSessionManager implements UndertowSessionManager {

    private static final int MAX_SESSION_ID_GENERATION_ATTEMPTS = 10;

    private final String deploymentName;
    private final SessionListeners sessionListeners = new SessionListeners();
    private final SessionManager<LocalSessionContext> manager;

    public DistributableSessionManager(String deploymentName, SessionManager<LocalSessionContext> manager) {
        this.deploymentName = deploymentName;
        this.manager = manager;
    }

    @Override
    public SessionListeners getSessionListeners() {
        return this.sessionListeners;
    }

    @Override
    public SessionManager<LocalSessionContext> getSessionManager() {
        return this.manager;
    }

    @Override
    public void start() {
        this.manager.start();
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

        String id = config.findSessionId(exchange);

        if (id == null) {
            int attempts = 0;
            do {
                if (++attempts > MAX_SESSION_ID_GENERATION_ATTEMPTS) {
                    throw UndertowMessages.MESSAGES.couldNotGenerateUniqueSessionId();
                }
                id = this.manager.createIdentifier();
            } while (this.manager.containsSession(id));

            config.setSessionId(exchange, id);
        }

        Batch batch = this.manager.getBatcher().startBatch();
        try {
            Session<LocalSessionContext> session = this.manager.createSession(id);
            io.undertow.server.session.Session adapter = new DistributableSession(this, session, config, batch);
            this.sessionListeners.sessionCreated(adapter, exchange);
            return adapter;
        } catch (RuntimeException | Error e) {
            batch.discard();
            throw e;
        }
    }

    @Override
    public io.undertow.server.session.Session getSession(HttpServerExchange exchange, SessionConfig config) {
        String id = config.findSessionId(exchange);
        if (id == null) return null;

        Batch batch = this.manager.getBatcher().startBatch();
        try {
            Session<LocalSessionContext> session = this.manager.findSession(id);
            if (session == null) {
                batch.close();
                return null;
            }
            return new DistributableSession(this, session, config, batch);
        } catch (RuntimeException | Error e) {
            batch.discard();
            throw e;
        }
    }

    @Override
    public void registerSessionListener(SessionListener listener) {
        this.sessionListeners.addSessionListener(listener);
    }

    @Override
    public void removeSessionListener(SessionListener listener) {
        this.sessionListeners.removeSessionListener(listener);
    }

    @Override
    public void setDefaultSessionTimeout(int timeout) {
        this.manager.setDefaultMaxInactiveInterval(timeout, TimeUnit.SECONDS);
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
        Batch batch = this.manager.getBatcher().startBatch();
        try {
            ImmutableSession session = this.manager.viewSession(sessionId);
            return (session != null) ? new DistributableImmutableSession(this, session) : null;
        } finally {
            batch.discard();
        }
    }

    @Override
    public String getDeploymentName() {
        return this.deploymentName;
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
