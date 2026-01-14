/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.web.undertow.session;

import java.util.Map;
import java.util.Set;

import org.wildfly.clustering.server.service.DecoratedService;
import org.wildfly.clustering.server.service.Service;
import org.wildfly.clustering.session.SessionManager;

import io.undertow.server.HttpServerExchange;
import io.undertow.server.session.Session;
import io.undertow.server.session.SessionConfig;
import io.undertow.server.session.SessionListener;
import io.undertow.server.session.SessionListeners;
import io.undertow.server.session.SessionManagerStatistics;

/**
 * Decorator of an Undertow session manager.
 * @author Paul Ferraro
 */
public class DecoratedSessionManager extends DecoratedService implements UndertowSessionManager {

    private final UndertowSessionManager manager;

    public DecoratedSessionManager(UndertowSessionManager manager) {
        this(manager, manager);
    }

    protected DecoratedSessionManager(UndertowSessionManager manager, Service service) {
        super(service);
        this.manager = manager;
    }

    @Override
    public String getDeploymentName() {
        return this.manager.getDeploymentName();
    }

    @Override
    public Session createSession(HttpServerExchange serverExchange, SessionConfig sessionCookieConfig) {
        return this.manager.createSession(serverExchange, sessionCookieConfig);
    }

    @Override
    public Session getSession(HttpServerExchange serverExchange, SessionConfig sessionCookieConfig) {
        return this.manager.getSession(serverExchange, sessionCookieConfig);
    }

    @Override
    public Session getSession(String sessionId) {
        return this.manager.getSession(sessionId);
    }

    @Override
    public void registerSessionListener(SessionListener listener) {
        this.manager.registerSessionListener(listener);
    }

    @Override
    public void removeSessionListener(SessionListener listener) {
        this.manager.removeSessionListener(listener);
    }

    @Override
    public void setDefaultSessionTimeout(int timeout) {
        this.manager.setDefaultSessionTimeout(timeout);
    }

    @Override
    public Set<String> getTransientSessions() {
        return this.manager.getTransientSessions();
    }

    @Override
    public Set<String> getActiveSessions() {
        return this.manager.getActiveSessions();
    }

    @Override
    public Set<String> getAllSessions() {
        return this.manager.getAllSessions();
    }

    @Override
    public SessionManagerStatistics getStatistics() {
        return this.manager.getStatistics();
    }

    @Override
    public SessionListeners getSessionListeners() {
        return this.manager.getSessionListeners();
    }

    @Override
    public SessionManager<Map<String, Object>> getSessionManager() {
        return this.manager.getSessionManager();
    }
}
