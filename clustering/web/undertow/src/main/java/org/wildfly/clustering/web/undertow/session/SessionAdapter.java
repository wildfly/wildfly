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

import java.util.AbstractMap.SimpleImmutableEntry;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.wildfly.clustering.web.session.Session;
import org.wildfly.clustering.web.session.SessionManager;

import io.undertow.server.HttpServerExchange;
import io.undertow.server.session.SessionConfig;
import io.undertow.server.session.SessionListener.SessionDestroyedReason;

/**
 * Undertow adapter for a {@link Session}.
 * @author Paul Ferraro
 */
public class SessionAdapter extends AbstractSessionAdapter<Session<Void>> {

    private final UndertowSessionManager manager;
    private volatile Map.Entry<Session<Void>, SessionConfig> entry;

    public SessionAdapter(UndertowSessionManager manager, Session<Void> session, SessionConfig config) {
        this.manager = manager;
        this.entry = new SimpleImmutableEntry<>(session, config);
    }

    @Override
    public io.undertow.server.session.SessionManager getSessionManager() {
        return this.manager;
    }

    @Override
    protected Session<Void> getSession() {
        return this.entry.getKey();
    }

    @Override
    public void requestDone(HttpServerExchange exchange) {
        this.getSession().close();
        this.manager.getSessionManager().getBatcher().endBatch(true);
    }

    @Override
    public void setMaxInactiveInterval(int interval) {
        this.getSession().getMetaData().setMaxInactiveInterval(interval, TimeUnit.SECONDS);
    }

    @Override
    public Object setAttribute(String name, Object value) {
        if (value == null) {
            return this.removeAttribute(name);
        }
        Object old = this.getSession().getAttributes().setAttribute(name, value);
        if (old == null) {
            this.manager.getSessionListeners().attributeAdded(this, name, value);
        } else if (old != value) {
            this.manager.getSessionListeners().attributeUpdated(this, name, value, old);
        }
        return old;
    }

    @Override
    public Object removeAttribute(String name) {
        Object old = this.getSession().getAttributes().removeAttribute(name);
        if (old != null) {
            this.manager.getSessionListeners().attributeRemoved(this, name, old);
        }
        return old;
    }

    @Override
    public void invalidate(HttpServerExchange exchange) {
        Map.Entry<Session<Void>, SessionConfig> entry = this.entry;
        Session<Void> session = entry.getKey();
        this.manager.getSessionListeners().sessionDestroyed(this, exchange, SessionDestroyedReason.INVALIDATED);
        session.invalidate();
        if (exchange != null) {
            entry.getValue().clearSession(exchange, session.getId());
        }
        this.manager.getSessionManager().getBatcher().endBatch(true);
    }

    @Override
    public String changeSessionId(HttpServerExchange exchange, SessionConfig config) {
        Session<Void> oldSession = this.getSession();
        SessionManager<Void> manager = this.manager.getSessionManager();
        String id = manager.createSessionId();
        Session<Void> newSession = manager.createSession(id);
        for (String name: oldSession.getAttributes().getAttributeNames()) {
            newSession.getAttributes().setAttribute(name, oldSession.getAttributes().getAttribute(name));
        }
        newSession.getMetaData().setMaxInactiveInterval(oldSession.getMetaData().getMaxInactiveInterval(TimeUnit.MILLISECONDS), TimeUnit.MILLISECONDS);
        newSession.getMetaData().setLastAccessedTime(oldSession.getMetaData().getLastAccessedTime());
        config.setSessionId(exchange, this.manager.format(id, manager.locate(id)));
        this.entry = new SimpleImmutableEntry<>(newSession, config);
        oldSession.invalidate();
        return id;
    }
}
