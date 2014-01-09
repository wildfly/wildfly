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

import java.util.AbstractMap;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import org.wildfly.clustering.web.Batch;
import org.wildfly.clustering.web.session.ImmutableSession;
import org.wildfly.clustering.web.session.Session;
import org.wildfly.clustering.web.session.SessionManager;

/**
 * Undertow adapter for a {@link SessionManager}.
 * @author Paul Ferraro
 */
public class SessionManagerAdapter implements UndertowSessionManager {


    private final String deploymentName;
    private final SessionListeners sessionListeners = new SessionListeners();
    private final SessionManager<LocalSessionContext> manager;

    public SessionManagerAdapter(String deploymentName, SessionManager<LocalSessionContext> manager) {
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
    public Map.Entry<String, String> parse(String id) {
        int index = (id != null) ? id.indexOf('.') : -1;
        return (index < 0) ? new AbstractMap.SimpleImmutableEntry<String, String>(id, null) : new AbstractMap.SimpleImmutableEntry<>(id.substring(0, index), id.substring(index + 1));
    }

    @Override
    public String format(String sessionId, String routeId) {
        return (routeId != null) ? String.format("%s.%s", sessionId, routeId) : sessionId;
    }

    @Override
    public String locate(String sessionId) {
        return this.manager.locate(sessionId);
    }

    @Override
    public String getDeploymentName() {
        return deploymentName;
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
        String id = this.findSessionId(exchange, config);

        if (id != null) {
            if (this.manager.containsSession(id)) {
                throw UndertowMessages.MESSAGES.sessionAlreadyExists(id);
            }
        } else {
            id = this.manager.createSessionId();
        }

        Batch batch = this.manager.getBatcher().startBatch();
        try {
            Session<LocalSessionContext> session = this.manager.createSession(id);
            io.undertow.server.session.Session adapter = this.getSession(session, exchange, config, batch);
            this.sessionListeners.sessionCreated(adapter, exchange);
            return adapter;
        } catch (RuntimeException | Error e) {
            batch.discard();
            throw e;
        }
    }

    @Override
    public io.undertow.server.session.Session getSession(HttpServerExchange exchange, SessionConfig config) {
        String id = this.findSessionId(exchange, config);
        if (id == null) return null;

        Batch batch = this.manager.getBatcher().startBatch();
        try {
            Session<LocalSessionContext> session = this.manager.findSession(id);
            if (session == null) {
                batch.close();
                return null;
            }
            return this.getSession(session, exchange, config, batch);
        } catch (RuntimeException | Error e) {
            batch.discard();
            throw e;
        }
    }

    /**
     * Strips routing information from requested session identifier.
     */
    private String findSessionId(HttpServerExchange exchange, SessionConfig config) {
        String id = config.findSessionId(exchange);
        return this.parse(id).getKey();
    }

    /**
     * Appends routing information to session identifier.
     */
    private io.undertow.server.session.Session getSession(Session<LocalSessionContext> session, HttpServerExchange exchange, SessionConfig config, Batch batch) {
        SessionAdapter adapter = new SessionAdapter(this, session, config, batch);
        if (config != null) {
            String id = session.getId();
            String route = this.locate(id);
            config.setSessionId(exchange, (route != null) ? this.format(id, route) : id);
        }
        return adapter;
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
            return (session != null) ? new ImmutableSessionAdapter(this, session) : null;
        } finally {
            batch.discard();
        }
    }
}
