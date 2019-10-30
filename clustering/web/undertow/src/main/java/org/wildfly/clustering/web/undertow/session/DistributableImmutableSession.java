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

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.wildfly.clustering.web.session.ImmutableSession;
import org.wildfly.clustering.web.session.ImmutableSessionAttributes;
import org.wildfly.clustering.web.session.ImmutableSessionMetaData;

import io.undertow.server.HttpServerExchange;
import io.undertow.server.session.Session;
import io.undertow.server.session.SessionConfig;
import io.undertow.server.session.SessionManager;

/**
 * Undertow adapter for an {@link ImmutableSession}.
 * @author Paul Ferraro
 */
public class DistributableImmutableSession implements Session {

    private final SessionManager manager;
    private final String id;
    private final Map<String, Object> attributes = new HashMap<>();
    private final long creationTime;
    private final long lastAccessedTime;
    private final int maxInactiveInterval;

    public DistributableImmutableSession(SessionManager manager, ImmutableSession session) {
        this.manager = manager;
        this.id = session.getId();
        ImmutableSessionAttributes attributes = session.getAttributes();
        for (String name: attributes.getAttributeNames()) {
            this.attributes.put(name, attributes.getAttribute(name));
        }
        ImmutableSessionMetaData metaData = session.getMetaData();
        this.creationTime = metaData.getCreationTime().toEpochMilli();
        this.lastAccessedTime = metaData.getLastAccessedTime().toEpochMilli();
        this.maxInactiveInterval = (int) metaData.getMaxInactiveInterval().getSeconds();
    }

    @Override
    public String getId() {
        return this.id;
    }

    @Override
    public SessionManager getSessionManager() {
        return this.manager;
    }

    @Override
    public void requestDone(HttpServerExchange serverExchange) {
        // Do nothing
    }

    @Override
    public long getCreationTime() {
        return this.creationTime;
    }

    @Override
    public long getLastAccessedTime() {
        return this.lastAccessedTime;
    }

    @Override
    public int getMaxInactiveInterval() {
        return this.maxInactiveInterval;
    }

    @Override
    public void setMaxInactiveInterval(int interval) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Object getAttribute(String name) {
        return this.attributes.get(name);
    }

    @Override
    public Set<String> getAttributeNames() {
        return Collections.unmodifiableSet(this.attributes.keySet());
    }

    @Override
    public Object setAttribute(String name, Object value) {
        return value;
    }

    @Override
    public Object removeAttribute(String name) {
        return null;
    }

    @Override
    public void invalidate(HttpServerExchange exchange) {
        // Do nothing
    }

    @Override
    public String changeSessionId(HttpServerExchange exchange, SessionConfig config) {
        return null;
    }
}
