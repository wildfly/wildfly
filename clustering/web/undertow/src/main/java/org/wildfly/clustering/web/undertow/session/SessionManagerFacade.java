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
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.jboss.metadata.web.jboss.ReplicationConfig;
import org.wildfly.clustering.web.Batcher;
import org.wildfly.clustering.web.session.Session;
import org.wildfly.clustering.web.session.SessionManager;

/**
 * Undertow facade for a {@link SessionManager}.
 * @author Paul Ferraro
 */
public class SessionManagerFacade implements UndertowSessionManager {
    private final ReplicationConfig config;
    private final SessionListeners sessionListeners = new SessionListeners();
    private final SessionManager<Void> manager;

    public SessionManagerFacade(SessionManager<Void> manager, ReplicationConfig config) {
        this.manager = manager;
        this.config = config;
        if (this.config.getUseJK() == null) {
            this.config.setUseJK(true);
        }
    }

    @Override
    public SessionListeners getSessionListeners() {
        return this.sessionListeners;
    }

    @Override
    public SessionManager<Void> getSessionManager() {
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
            if (this.manager.findSession(id) != null) {
                throw UndertowMessages.MESSAGES.sessionAlreadyExists(id);
            }
        } else {
            id = this.manager.createSessionId();
        }

        this.manager.getBatcher().startBatch();
        Session<Void> session = this.manager.createSession(id);
        io.undertow.server.session.Session facade = this.getSession(session, exchange, config);
        this.sessionListeners.sessionCreated(facade, exchange);
        return facade;
    }

    @Override
    public io.undertow.server.session.Session getSession(HttpServerExchange exchange, SessionConfig config) {
        String id = this.findSessionId(exchange, config);
        if (id == null) return null;
        Batcher batcher = this.manager.getBatcher();
        boolean started = batcher.startBatch();
        Session<Void> session = this.manager.findSession(id);
        if (session == null) {
            if (started) {
                batcher.endBatch(false);
            }
            return null;
        }
        return this.getSession(session, exchange, config);
    }

    /**
     * Strips routing information from requested session identifier.
     */
    private String findSessionId(HttpServerExchange exchange, SessionConfig config) {
        String id = config.findSessionId(exchange);
        return this.config.getUseJK().booleanValue() ? this.parse(id).getKey() : id;
    }

    /**
     * Appends routing information to session identifier.
     */
    private io.undertow.server.session.Session getSession(Session<Void> session, HttpServerExchange exchange, SessionConfig config) {
        SessionFacade facade = new SessionFacade(this, session, config);
        if (this.config.getUseJK().booleanValue()) {
            String id = session.getId();
            config.setSessionId(exchange, this.format(id, this.locate(id)));
        }
        return facade;
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
    public int activeSessions() {
        return this.manager.size();
    }
}
