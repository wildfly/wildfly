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

import io.undertow.security.api.AuthenticatedSessionManager.AuthenticatedSession;
import io.undertow.security.idm.Account;
import io.undertow.server.HttpServerExchange;
import io.undertow.server.session.SessionConfig;
import io.undertow.server.session.SessionListener.SessionDestroyedReason;
import io.undertow.servlet.handlers.security.CachedAuthenticatedSessionHandler;

import java.io.NotSerializableException;
import java.io.Serializable;
import java.util.AbstractMap.SimpleImmutableEntry;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import javax.servlet.http.HttpServletRequest;

import org.wildfly.clustering.ee.Batch;
import org.wildfly.clustering.ee.BatchContext;
import org.wildfly.clustering.web.session.Session;
import org.wildfly.clustering.web.session.SessionManager;

/**
 * Adapts a distributable {@link Session} to an Undertow {@link io.undertow.server.session.Session}.
 * @author Paul Ferraro
 */
public class DistributableSession implements io.undertow.server.session.Session {
    // Undertow stores the authenticated session in the HttpSession using a special attribute with the following name
    private static final String AUTHENTICATED_SESSION_ATTRIBUTE_NAME = CachedAuthenticatedSessionHandler.class.getName() + ".AuthenticatedSession";

    private final UndertowSessionManager manager;
    private final Batch batch;

    private volatile Map.Entry<Session<LocalSessionContext>, SessionConfig> entry;

    public DistributableSession(UndertowSessionManager manager, Session<LocalSessionContext> session, SessionConfig config, Batch batch) {
        this.manager = manager;
        this.entry = new SimpleImmutableEntry<>(session, config);
        this.batch = batch;
    }

    @Override
    public io.undertow.server.session.SessionManager getSessionManager() {
        return this.manager;
    }

    @Override
    public void requestDone(HttpServerExchange exchange) {
        // Batch may no longer be active if session was invalidated
        if (this.batch.isActive()) {
            try (BatchContext context = this.manager.getSessionManager().getBatcher().resumeBatch(this.batch)) {
                this.entry.getKey().close();
                this.batch.close();
            }
        }
    }

    @Override
    public String getId() {
        return this.entry.getKey().getId();
    }

    @Override
    public long getCreationTime() {
        try (BatchContext context = this.manager.getSessionManager().getBatcher().resumeBatch(this.batch)) {
            return this.entry.getKey().getMetaData().getCreationTime().getTime();
        }
    }

    @Override
    public long getLastAccessedTime() {
        try (BatchContext context = this.manager.getSessionManager().getBatcher().resumeBatch(this.batch)) {
            return this.entry.getKey().getMetaData().getLastAccessedTime().getTime();
        }
    }

    @Override
    public int getMaxInactiveInterval() {
        try (BatchContext context = this.manager.getSessionManager().getBatcher().resumeBatch(this.batch)) {
            return (int) this.entry.getKey().getMetaData().getMaxInactiveInterval(TimeUnit.SECONDS);
        }
    }

    @Override
    public void setMaxInactiveInterval(int interval) {
        try (BatchContext context = this.manager.getSessionManager().getBatcher().resumeBatch(this.batch)) {
            this.entry.getKey().getMetaData().setMaxInactiveInterval(interval, TimeUnit.SECONDS);
        }
    }

    @Override
    public Set<String> getAttributeNames() {
        try (BatchContext context = this.manager.getSessionManager().getBatcher().resumeBatch(this.batch)) {
            return this.entry.getKey().getAttributes().getAttributeNames();
        }
    }

    @Override
    public Object getAttribute(String name) {
        Session<LocalSessionContext> session = this.entry.getKey();
        try (BatchContext context = this.manager.getSessionManager().getBatcher().resumeBatch(this.batch)) {
            if (AUTHENTICATED_SESSION_ATTRIBUTE_NAME.equals(name)) {
                Account account = (Account) session.getAttributes().getAttribute(name);
                return (account != null) ? new AuthenticatedSession(account, HttpServletRequest.FORM_AUTH) : session.getLocalContext().getAuthenticatedSession();
            }
            return session.getAttributes().getAttribute(name);
        }
    }

    @Override
    public Object setAttribute(String name, Object value) {
        if (value == null) {
            return this.removeAttribute(name);
        }
        Session<LocalSessionContext> session = this.entry.getKey();
        try (BatchContext context = this.manager.getSessionManager().getBatcher().resumeBatch(this.batch)) {
            if (AUTHENTICATED_SESSION_ATTRIBUTE_NAME.equals(name)) {
                AuthenticatedSession authSession = (AuthenticatedSession) value;
                // If using FORM authentication, we store the corresponding Account in a session attribute
                if (authSession.getMechanism().equals(HttpServletRequest.FORM_AUTH)) {
                    Account account = (Account) session.getAttributes().setAttribute(name, authSession.getAccount());
                    return (account != null) ? new AuthenticatedSession(account, HttpServletRequest.FORM_AUTH) : null;
                }
                // Otherwise we store the whole AuthenticatedSession in the local context
                LocalSessionContext localContext = session.getLocalContext();
                AuthenticatedSession old = localContext.getAuthenticatedSession();
                localContext.setAuthenticatedSession(authSession);
                return old;
            }
            if (!(value instanceof Serializable)) {
                throw new IllegalArgumentException(new NotSerializableException(value.getClass().getName()));
            }
            Object old = session.getAttributes().setAttribute(name, value);
            if (old == null) {
                this.manager.getSessionListeners().attributeAdded(this, name, value);
            } else if (old != value) {
                this.manager.getSessionListeners().attributeUpdated(this, name, value, old);
            }
            return old;
        }
    }

    @Override
    public Object removeAttribute(String name) {
        Session<LocalSessionContext> session = this.entry.getKey();
        try (BatchContext context = this.manager.getSessionManager().getBatcher().resumeBatch(this.batch)) {
            if (AUTHENTICATED_SESSION_ATTRIBUTE_NAME.equals(name)) {
                Account account = (Account) session.getAttributes().removeAttribute(name);
                if (account != null) {
                    return new AuthenticatedSession(account, HttpServletRequest.FORM_AUTH);
                }
                LocalSessionContext localContext = session.getLocalContext();
                AuthenticatedSession old = localContext.getAuthenticatedSession();
                localContext.setAuthenticatedSession(null);
                return old;
            }
            Object old = session.getAttributes().removeAttribute(name);
            if (old != null) {
                this.manager.getSessionListeners().attributeRemoved(this, name, old);
            }
            return old;
        }
    }

    @Override
    public void invalidate(HttpServerExchange exchange) {
        Map.Entry<Session<LocalSessionContext>, SessionConfig> entry = this.entry;
        Session<LocalSessionContext> session = entry.getKey();
        try (BatchContext context = this.manager.getSessionManager().getBatcher().resumeBatch(this.batch)) {
            this.manager.getSessionListeners().sessionDestroyed(this, exchange, SessionDestroyedReason.INVALIDATED);
            session.invalidate();
            if (exchange != null) {
                String id = session.getId();
                entry.getValue().clearSession(exchange, id);
            }
            this.batch.close();
        }
    }

    @Override
    public String changeSessionId(HttpServerExchange exchange, SessionConfig config) {
        Session<LocalSessionContext> oldSession = this.entry.getKey();
        SessionManager<LocalSessionContext, Batch> manager = this.manager.getSessionManager();
        String id = manager.createIdentifier();
        try (BatchContext context = this.manager.getSessionManager().getBatcher().resumeBatch(this.batch)) {
            Session<LocalSessionContext> newSession = manager.createSession(id);
            for (String name: oldSession.getAttributes().getAttributeNames()) {
                newSession.getAttributes().setAttribute(name, oldSession.getAttributes().getAttribute(name));
            }
            newSession.getMetaData().setMaxInactiveInterval(oldSession.getMetaData().getMaxInactiveInterval(TimeUnit.MILLISECONDS), TimeUnit.MILLISECONDS);
            newSession.getMetaData().setLastAccessedTime(oldSession.getMetaData().getLastAccessedTime());
            newSession.getLocalContext().setAuthenticatedSession(oldSession.getLocalContext().getAuthenticatedSession());
            config.setSessionId(exchange, id);
            this.entry = new SimpleImmutableEntry<>(newSession, config);
            oldSession.invalidate();
            return id;
        }
    }
}
