/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.web.undertow.session;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import io.undertow.server.HttpServerExchange;
import io.undertow.server.session.Session;
import io.undertow.server.session.SessionConfig;
import io.undertow.server.session.SessionManager;

/**
 * A session implementation for sessions created after the response is committed.
 * This is a workaround for those applications that trigger creation of a new session after calling {@link jakarta.servlet.http.HttpServletResponse#sendRedirect(String)}.
 * @author Paul Ferraro
 */
public class OrphanSession implements Session {

    private final SessionManager manager;
    private final String id;
    private final long creationTime = System.currentTimeMillis();
    private final Map<String, Object> attributes = new ConcurrentHashMap<>();

    private volatile int maxInactiveInterval = 0;

    public OrphanSession(SessionManager manager, String id) {
        this.manager = manager;
        this.id = id;
    }

    @Override
    public String getId() {
        return this.id;
    }

    @Override
    public void requestDone(HttpServerExchange serverExchange) {
        // This will never be invoked
    }

    @Override
    public long getCreationTime() {
        return this.creationTime;
    }

    @Override
    public long getLastAccessedTime() {
        return this.creationTime;
    }

    @Override
    public void setMaxInactiveInterval(int interval) {
        this.maxInactiveInterval = interval;
    }

    @Override
    public int getMaxInactiveInterval() {
        return this.maxInactiveInterval;
    }

    @Override
    public Object getAttribute(String name) {
        return this.attributes.get(name);
    }

    @Override
    public Set<String> getAttributeNames() {
        return this.attributes.keySet();
    }

    @Override
    public Object setAttribute(String name, Object value) {
        return this.attributes.put(name, value);
    }

    @Override
    public Object removeAttribute(String name) {
        return this.attributes.remove(name);
    }

    @Override
    public void invalidate(HttpServerExchange exchange) {
        // Do nothing
        // This session is not referenced by the session manager and will be garbage collected at the end of the current request
    }

    @Override
    public SessionManager getSessionManager() {
        return this.manager;
    }

    @Override
    public String changeSessionId(HttpServerExchange exchange, SessionConfig config) {
        // Don't actually change the session ID - since this session is not referenced by any client
        // Thus session fixation is a non-issue
        return this.id;
    }

    /*
     * New method in io.undertow.server.session.Session that can add the @Override annotation when Undertow is upgraded
     */
    public boolean isInvalid() {
        return true;
    }
}
