/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2021, Red Hat, Inc., and individual contributors
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

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import io.undertow.server.HttpServerExchange;
import io.undertow.server.session.Session;
import io.undertow.server.session.SessionConfig;
import io.undertow.server.session.SessionManager;

/**
 * A session implementation for sessions created after the response is committed.
 * This is a workaround for those applications that trigger creation of a new session after calling {@link javax.servlet.http.HttpServletResponse#sendRedirect(String)}.
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
}
