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

import java.util.Set;
import java.util.concurrent.TimeUnit;

import org.wildfly.clustering.web.session.Session;

import io.undertow.server.HttpServerExchange;
import io.undertow.server.session.SessionConfig;
import io.undertow.server.session.SessionListener.SessionDestroyedReason;

/**
 * Undertow facade for a {@link Session}.
 * @author Paul Ferraro
 */
public class SessionFacade implements io.undertow.server.session.Session {

    private final Session<Void> session;
    private final SessionConfig config;
    private final UndertowSessionManager manager;

    public SessionFacade(UndertowSessionManager manager, Session<Void> session, SessionConfig config) {
        this.manager = manager;
        this.session = session;
        this.config = config;
    }

    @Override
    public String getId() {
        return this.session.getId();
    }

    @Override
    public void requestDone(HttpServerExchange exchange) {
        this.session.close();
        this.manager.getSessionManager().getBatcher().endBatch(true);
    }

    @Override
    public long getCreationTime() {
        return this.session.getMetaData().getCreationTime().getTime();
    }

    @Override
    public long getLastAccessedTime() {
        return this.session.getMetaData().getLastAccessedTime().getTime();
    }

    @Override
    public void setMaxInactiveInterval(int interval) {
        this.session.getMetaData().setMaxInactiveInterval(interval, TimeUnit.SECONDS);
    }

    @Override
    public int getMaxInactiveInterval() {
        return (int) this.session.getMetaData().getMaxInactiveInterval(TimeUnit.SECONDS);
    }

    @Override
    public Object getAttribute(String name) {
        return this.session.getAttributes().getAttribute(name);
    }

    @Override
    public Set<String> getAttributeNames() {
        return this.session.getAttributes().getAttributeNames();
    }

    @Override
    public Object setAttribute(String name, Object value) {
        if (value == null) {
            return this.removeAttribute(name);
        }
        Object old = this.session.getAttributes().setAttribute(name, value);
        if (old == null) {
            this.manager.getSessionListeners().attributeAdded(this, name, value);
        } else if (old != value) {
            this.manager.getSessionListeners().attributeUpdated(this, name, value, old);
        }
        return old;
    }

    @Override
    public Object removeAttribute(String name) {
        Object old = this.session.getAttributes().removeAttribute(name);
        if (old != null) {
            this.manager.getSessionListeners().attributeRemoved(this, name, old);
        }
        return old;
    }

    @Override
    public void invalidate(HttpServerExchange exchange) {
        this.session.invalidate();
        this.manager.getSessionListeners().sessionDestroyed(this, exchange, SessionDestroyedReason.INVALIDATED);
        if (exchange != null) {
            this.config.clearSession(exchange, this.session.getId());
        }
        this.manager.getSessionManager().getBatcher().endBatch(true);
    }

    @Override
    public io.undertow.server.session.SessionManager getSessionManager() {
        return this.manager;
    }

    @Override
    public String changeSessionId(HttpServerExchange exchange, SessionConfig config) {
        Session<Void> session = this.manager.getSessionManager().createSession(this.manager.getSessionManager().createSessionId());
        for (String name: this.session.getAttributes().getAttributeNames()) {
            session.getAttributes().setAttribute(name, this.session.getAttributes().getAttribute(name));
        }
        session.getMetaData().setMaxInactiveInterval(this.session.getMetaData().getMaxInactiveInterval(TimeUnit.MILLISECONDS), TimeUnit.MILLISECONDS);
        session.getMetaData().setLastAccessedTime(this.session.getMetaData().getLastAccessedTime());
        this.session.invalidate();
        config.setSessionId(exchange, session.getId());
        return session.getId();
    }
}
