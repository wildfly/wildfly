/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.web.undertow.session;

import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

import io.undertow.server.HttpServerExchange;
import io.undertow.server.session.Session;
import io.undertow.server.session.SessionConfig;
import io.undertow.server.session.SessionManager;

/**
 * Session decorator that auto-detaches on {@link #requestDone(HttpServerExchange)}.
 * @author Paul Ferraro
 */
public class DetachableSession implements Session {

    private final AtomicReference<Session> reference;

    DetachableSession(Session session) {
        this.reference = new AtomicReference<>(session);
    }

    @Override
    public String getId() {
        return this.reference.get().getId();
    }

    @Override
    public SessionManager getSessionManager() {
        return this.reference.get().getSessionManager();
    }

    @Override
    public void requestDone(HttpServerExchange exchange) {
        // Subsequent session access will use detached session
        this.reference.getAndUpdate(this::detach).requestDone(exchange);
    }

    private Session detach(Session session) {
        return new ReferencedSession(new DistributableSessionReference(session.getSessionManager(), session.getId()));
    }

    @Override
    public long getCreationTime() {
        return this.reference.get().getCreationTime();
    }

    @Override
    public long getLastAccessedTime() {
        return this.reference.get().getLastAccessedTime();
    }

    @Override
    public int getMaxInactiveInterval() {
        return this.reference.get().getMaxInactiveInterval();
    }

    @Override
    public void setMaxInactiveInterval(int interval) {
        this.reference.get().setMaxInactiveInterval(interval);
    }

    @Override
    public Set<String> getAttributeNames() {
        return this.reference.get().getAttributeNames();
    }

    @Override
    public Object getAttribute(String name) {
        return this.reference.get().getAttribute(name);
    }

    @Override
    public Object setAttribute(String name, Object value) {
        return this.reference.get().setAttribute(name, value);
    }

    @Override
    public Object removeAttribute(String name) {
        return this.reference.get().removeAttribute(name);
    }

    @Override
    public void invalidate(HttpServerExchange exchange) {
        this.reference.get().invalidate(exchange);
    }

    @Override
    public String changeSessionId(HttpServerExchange exchange, SessionConfig config) {
        return this.reference.get().changeSessionId(exchange, config);
    }
}
