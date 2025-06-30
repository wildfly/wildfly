/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2025, Red Hat, Inc., and individual contributors
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

import java.time.Duration;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Supplier;

import jakarta.servlet.http.HttpServletRequest;

import io.undertow.security.api.AuthenticatedSessionManager.AuthenticatedSession;
import io.undertow.server.HttpServerExchange;
import io.undertow.servlet.handlers.security.CachedAuthenticatedSessionHandler;

import org.wildfly.clustering.session.Session;
import org.wildfly.clustering.session.SessionMetaData;

/**
 * An abstract {@link io.undertow.server.session.Session} that delegates to a {@link Session}.
 * @author Paul Ferraro
 */
public abstract class AbstractSession implements io.undertow.server.session.Session, Supplier<Session<Map<String, Object>>> {
    // These mechanisms can auto-reauthenticate and thus use local context (instead of replicating)
    private static final Set<String> AUTO_REAUTHENTICATING_MECHANISMS = Set.of(HttpServletRequest.BASIC_AUTH, HttpServletRequest.DIGEST_AUTH, HttpServletRequest.CLIENT_CERT_AUTH);
    static final String WEB_SOCKET_CHANNELS_ATTRIBUTE = "io.undertow.websocket.current-connections";
    private static final Set<String> LOCAL_CONTEXT_ATTRIBUTES = Set.of(WEB_SOCKET_CHANNELS_ATTRIBUTE);

    private final UndertowSessionManager manager;
    private final Supplier<Session<Map<String, Object>>> reference;

    public AbstractSession(UndertowSessionManager manager, Supplier<Session<Map<String, Object>>> reference) {
        this.manager = manager;
        this.reference = reference;
    }

    @Override
    public Session<Map<String, Object>> get() {
        return this.reference.get();
    }

    @Override
    public UndertowSessionManager getSessionManager() {
        return this.manager;
    }

    @Override
    public String getId() {
        return this.reference.get().getId();
    }

    @Override
    public long getCreationTime() {
        return this.reference.get().getMetaData().getCreationTime().toEpochMilli();
    }

    @Override
    public long getLastAccessedTime() {
        SessionMetaData metaData = this.reference.get().getMetaData();
        return Optional.ofNullable(metaData.getLastAccessStartTime()).orElseGet(metaData::getCreationTime).toEpochMilli();
    }

    @Override
    public int getMaxInactiveInterval() {
        return (int) this.reference.get().getMetaData().getTimeout().getSeconds();
    }

    @Override
    public void setMaxInactiveInterval(int interval) {
        this.reference.get().getMetaData().setTimeout(Duration.ofSeconds(interval));
    }

    @Override
    public Set<String> getAttributeNames() {
        return this.reference.get().getAttributes().keySet();
    }

    @Override
    public Object getAttribute(String name) {
        Session<Map<String, Object>> session = this.reference.get();
        if (CachedAuthenticatedSessionHandler.ATTRIBUTE_NAME.equals(name)) {
            AuthenticatedSession auth = (AuthenticatedSession) session.getAttributes().get(name);
            return (auth != null) ? auth : session.getContext().get(name);
        }
        if (LOCAL_CONTEXT_ATTRIBUTES.contains(name)) {
            return session.getContext().get(name);
        }
        return session.getAttributes().get(name);
    }

    @Override
    public Object setAttribute(String name, Object value) {
        if (value == null) {
            return this.removeAttribute(name);
        }
        Session<Map<String, Object>> session = this.reference.get();
        if (CachedAuthenticatedSessionHandler.ATTRIBUTE_NAME.equals(name)) {
            AuthenticatedSession auth = (AuthenticatedSession) value;
            return AUTO_REAUTHENTICATING_MECHANISMS.contains(auth.getMechanism()) ? session.getContext().put(name, auth) : session.getAttributes().put(name, auth);
        }
        if (LOCAL_CONTEXT_ATTRIBUTES.contains(name)) {
            return session.getContext().put(name, value);
        }
        Object old = session.getAttributes().put(name, value);
        if (old == null) {
            this.manager.getSessionListeners().attributeAdded(this, name, value);
        } else if (old != value) {
            this.manager.getSessionListeners().attributeUpdated(this, name, value, old);
        }
        return old;
    }

    @Override
    public Object removeAttribute(String name) {
        Session<Map<String, Object>> session = this.reference.get();
        if (CachedAuthenticatedSessionHandler.ATTRIBUTE_NAME.equals(name)) {
            AuthenticatedSession auth = (AuthenticatedSession) session.getAttributes().remove(name);
            return (auth != null) ? auth : session.getContext().remove(name);
        }
        if (LOCAL_CONTEXT_ATTRIBUTES.contains(name)) {
            return session.getContext().remove(name);
        }
        Object old = session.getAttributes().remove(name);
        if (old != null) {
            this.manager.getSessionListeners().attributeRemoved(this, name, old);
        }
        return old;
    }

    @Override
    public void invalidate(HttpServerExchange exchange) {
        this.reference.get().invalidate();
    }
}
