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

import io.undertow.UndertowLogger;
import io.undertow.security.api.AuthenticatedSessionManager.AuthenticatedSession;
import io.undertow.server.HttpServerExchange;
import io.undertow.server.session.SessionConfig;
import io.undertow.server.session.SessionListener.SessionDestroyedReason;
import io.undertow.servlet.handlers.security.CachedAuthenticatedSessionHandler;

import java.time.Duration;
import java.util.AbstractMap.SimpleImmutableEntry;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

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
    // These mechanisms can auto-reauthenticate and thus use local context (instead of replicating)
    private static final Set<String> AUTO_REAUTHENTICATING_MECHANISMS = new HashSet<>(Arrays.asList(HttpServletRequest.BASIC_AUTH, HttpServletRequest.DIGEST_AUTH, HttpServletRequest.CLIENT_CERT_AUTH));

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
        if (this.entry.getKey().isValid()) {
            try (BatchContext context = this.manager.getSessionManager().getBatcher().resumeBatch(this.batch)) {
                this.entry.getKey().close();
                this.batch.close();
            } catch (Throwable e) {
                // Don't propagate exceptions at the stage, since response was alread committed
                UndertowLogger.REQUEST_LOGGER.warn(e.getLocalizedMessage(), e);
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
            return this.entry.getKey().getMetaData().getCreationTime().toEpochMilli();
        }
    }

    @Override
    public long getLastAccessedTime() {
        try (BatchContext context = this.manager.getSessionManager().getBatcher().resumeBatch(this.batch)) {
            return this.entry.getKey().getMetaData().getLastAccessedTime().toEpochMilli();
        }
    }

    @Override
    public int getMaxInactiveInterval() {
        try (BatchContext context = this.manager.getSessionManager().getBatcher().resumeBatch(this.batch)) {
            return (int) this.entry.getKey().getMetaData().getMaxInactiveInterval().getSeconds();
        }
    }

    @Override
    public void setMaxInactiveInterval(int interval) {
        try (BatchContext context = this.manager.getSessionManager().getBatcher().resumeBatch(this.batch)) {
            this.entry.getKey().getMetaData().setMaxInactiveInterval(Duration.ofSeconds(interval));
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
                AuthenticatedSession auth = (AuthenticatedSession) session.getAttributes().getAttribute(name);
                return (auth != null) ? auth : session.getLocalContext().getAuthenticatedSession();
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
                AuthenticatedSession auth = (AuthenticatedSession) value;
                return AUTO_REAUTHENTICATING_MECHANISMS.contains(auth.getMechanism()) ? this.setLocalContext(auth) : session.getAttributes().setAttribute(name, new ImmutableAuthenticatedSession(auth));
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
                AuthenticatedSession auth = (AuthenticatedSession) session.getAttributes().removeAttribute(name);
                return (auth != null) ? auth : this.setLocalContext(null);
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
        // Invoke listeners outside of the context of the batch associated with this session
        this.manager.getSessionListeners().sessionDestroyed(this, exchange, SessionDestroyedReason.INVALIDATED);
        Map.Entry<Session<LocalSessionContext>, SessionConfig> entry = this.entry;
        Session<LocalSessionContext> session = entry.getKey();
        try (BatchContext context = this.manager.getSessionManager().getBatcher().resumeBatch(this.batch)) {
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
            newSession.getMetaData().setMaxInactiveInterval(oldSession.getMetaData().getMaxInactiveInterval());
            newSession.getMetaData().setLastAccessedTime(oldSession.getMetaData().getLastAccessedTime());
            newSession.getLocalContext().setAuthenticatedSession(oldSession.getLocalContext().getAuthenticatedSession());
            config.setSessionId(exchange, id);
            this.entry = new SimpleImmutableEntry<>(newSession, config);
            oldSession.invalidate();
        }
        // Invoke listeners outside of the context of the batch associated with this session
        this.manager.getSessionListeners().sessionIdChanged(this, oldSession.getId());
        return id;
    }

    private AuthenticatedSession setLocalContext(AuthenticatedSession auth) {
        LocalSessionContext localContext = this.entry.getKey().getLocalContext();
        AuthenticatedSession old = localContext.getAuthenticatedSession();
        localContext.setAuthenticatedSession(auth);
        return old;
    }
}
