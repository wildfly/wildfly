/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.web.undertow.session;

import java.util.Set;

import io.undertow.server.HttpServerExchange;
import io.undertow.server.session.Session;
import io.undertow.server.session.SessionConfig;
import io.undertow.server.session.SessionManager;

/**
 * An Undertow session that delegates all methods to a session reference.
 * For use outside of request scope.
 * @author Paul Ferraro
 */
public class ReferencedSession implements Session {
    private final SessionReference reference;

    public ReferencedSession(SessionReference reference) {
        this.reference = reference;
    }

    @Override
    public SessionManager getSessionManager() {
        return this.reference.getManager();
    }

    @Override
    public String getId() {
        return this.reference.getId();
    }

    @Override
    public long getCreationTime() {
        return this.reference.getReader().read(Session::getCreationTime);
    }

    @Override
    public long getLastAccessedTime() {
        return this.reference.getReader().read(Session::getLastAccessedTime);
    }

    @Override
    public void setMaxInactiveInterval(int interval) {
        this.reference.getReader().consume(session -> session.setMaxInactiveInterval(interval));
    }

    @Override
    public int getMaxInactiveInterval() {
        return this.reference.getReader().read(Session::getMaxInactiveInterval);
    }

    @Override
    public Object getAttribute(String name) {
        return this.reference.getReader().read(session -> session.getAttribute(name));
    }

    @Override
    public Set<String> getAttributeNames() {
        return this.reference.getReader().read(Session::getAttributeNames);
    }

    @Override
    public Object setAttribute(String name, Object value) {
        return this.reference.getReader().read(session -> session.setAttribute(name, value));
    }

    @Override
    public Object removeAttribute(String name) {
        return this.reference.getReader().read(session -> session.removeAttribute(name));
    }

    @Override
    public void invalidate(HttpServerExchange exchange) {
        this.reference.getReader().consume(session -> session.invalidate(exchange));
    }

    @Override
    public void requestDone(HttpServerExchange serverExchange) {
        // Not relevant to a detached session
    }

    @Override
    public String changeSessionId(HttpServerExchange exchange, SessionConfig config) {
        // A detached session cannot change its ID
        return this.getId();
    }
}
