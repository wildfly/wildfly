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
import io.undertow.server.HttpServerExchange;
import io.undertow.server.session.SessionConfig;
import io.undertow.server.session.SessionListener.SessionDestroyedReason;
import io.undertow.servlet.handlers.security.CachedAuthenticatedSessionHandler;

import java.time.Duration;
import java.time.Instant;
import java.util.AbstractMap.SimpleImmutableEntry;
import java.util.function.Consumer;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.servlet.http.HttpServletRequest;

import org.wildfly.clustering.ee.Batch;
import org.wildfly.clustering.ee.BatchContext;
import org.wildfly.clustering.ee.Batcher;
import org.wildfly.clustering.web.session.ImmutableSessionAttributes;
import org.wildfly.clustering.web.session.Session;
import org.wildfly.clustering.web.session.SessionManager;
import org.wildfly.clustering.web.undertow.logging.UndertowClusteringLogger;

/**
 * Adapts a distributable {@link Session} to an Undertow {@link io.undertow.server.session.Session}.
 * @author Paul Ferraro
 */
public class DistributableSession implements io.undertow.server.session.Session {
    // These mechanisms can auto-reauthenticate and thus use local context (instead of replicating)
    private static final Set<String> AUTO_REAUTHENTICATING_MECHANISMS = new HashSet<>(Arrays.asList(HttpServletRequest.BASIC_AUTH, HttpServletRequest.DIGEST_AUTH, HttpServletRequest.CLIENT_CERT_AUTH));
    static final String WEB_SOCKET_CHANNELS_ATTRIBUTE = "io.undertow.websocket.current-connections";
    static final String IDENTITY_CONTAINER_ATTRIBUTE = "org.wildfly.elytron.web.undertow.server.servlet.ServletSecurityContextImpl$IdentityContainer";
    private static final Set<String> LOCAL_CONTEXT_ATTRIBUTES = new HashSet<>(Arrays.asList(WEB_SOCKET_CHANNELS_ATTRIBUTE, IDENTITY_CONTAINER_ATTRIBUTE));

    private final UndertowSessionManager manager;
    private final Batch batch;
    private final Consumer<HttpServerExchange> closeTask;
    private final Instant startTime;

    private volatile Map.Entry<Session<Map<String, Object>>, SessionConfig> entry;

    public DistributableSession(UndertowSessionManager manager, Session<Map<String, Object>> session, SessionConfig config, Batch batch, Consumer<HttpServerExchange> closeTask) {
        this.manager = manager;
        this.entry = new SimpleImmutableEntry<>(session, config);
        this.batch = batch;
        this.closeTask = closeTask;
        this.startTime = session.getMetaData().isNew() ? session.getMetaData().getCreationTime() : Instant.now();
    }

    @Override
    public io.undertow.server.session.SessionManager getSessionManager() {
        return this.manager;
    }

    @Override
    public void requestDone(HttpServerExchange exchange) {
        try {
            Session<Map<String, Object>> session = this.entry.getKey();
            if (session.isValid()) {
                Batcher<Batch> batcher = this.manager.getSessionManager().getBatcher();
                try (BatchContext context = batcher.resumeBatch(this.batch)) {
                    // If batch was discarded, close it
                    if (this.batch.getState() == Batch.State.DISCARDED) {
                        this.batch.close();
                    }
                    // If batch is closed, close session in a new batch
                    try (Batch batch = (this.batch.getState() == Batch.State.CLOSED) ? batcher.createBatch() : this.batch) {
                        // According to §7.6 of the servlet specification:
                        // The session is considered to be accessed when a request that is part of the session is first handled by the servlet container.
                        session.getMetaData().setLastAccess(this.startTime, Instant.now());
                        session.close();
                    }
                } catch (Throwable e) {
                    // Don't propagate exceptions at the stage, since response was already committed
                    UndertowClusteringLogger.ROOT_LOGGER.warn(e.getLocalizedMessage(), e);
                }
            } else if (this.batch.getState() != Batch.State.CLOSED) {
                Batcher<Batch> batcher = this.manager.getSessionManager().getBatcher();
                try (BatchContext context = batcher.resumeBatch(this.batch)) {
                    // Ensure batch is closed.
                    this.batch.close();
                }
            }
        } finally {
            this.closeTask.accept(exchange);
        }
    }

    @Override
    public String getId() {
        return this.entry.getKey().getId();
    }

    @Override
    public long getCreationTime() {
        Session<Map<String, Object>> session = this.entry.getKey();
        this.validate(session);
        try (BatchContext context = this.resumeBatch()) {
            return session.getMetaData().getCreationTime().toEpochMilli();
        }
    }

    @Override
    public long getLastAccessedTime() {
        Session<Map<String, Object>> session = this.entry.getKey();
        this.validate(session);
        try (BatchContext context = this.resumeBatch()) {
            return session.getMetaData().getLastAccessStartTime().toEpochMilli();
        }
    }

    @Override
    public int getMaxInactiveInterval() {
        Session<Map<String, Object>> session = this.entry.getKey();
        this.validate(session);
        try (BatchContext context = this.resumeBatch()) {
            return (int) session.getMetaData().getMaxInactiveInterval().getSeconds();
        }
    }

    @Override
    public void setMaxInactiveInterval(int interval) {
        Session<Map<String, Object>> session = this.entry.getKey();
        this.validate(session);
        try (BatchContext context = this.resumeBatch()) {
            session.getMetaData().setMaxInactiveInterval(Duration.ofSeconds(interval));
        }
    }

    @Override
    public Set<String> getAttributeNames() {
        Session<Map<String, Object>> session = this.entry.getKey();
        this.validate(session);
        try (BatchContext context = this.resumeBatch()) {
            return session.getAttributes().getAttributeNames();
        }
    }

    @Override
    public Object getAttribute(String name) {
        Session<Map<String, Object>> session = this.entry.getKey();
        this.validate(session);
        try (BatchContext context = this.resumeBatch()) {
            if (CachedAuthenticatedSessionHandler.ATTRIBUTE_NAME.equals(name)) {
                AuthenticatedSession auth = (AuthenticatedSession) session.getAttributes().getAttribute(name);
                return (auth != null) ? auth : session.getLocalContext().get(name);
            }
            if (LOCAL_CONTEXT_ATTRIBUTES.contains(name)) {
                return session.getLocalContext().get(name);
            }
            return session.getAttributes().getAttribute(name);
        }
    }

    @Override
    public Object setAttribute(String name, Object value) {
        if (value == null) {
            return this.removeAttribute(name);
        }
        Session<Map<String, Object>> session = this.entry.getKey();
        this.validate(session);
        try (BatchContext context = this.resumeBatch()) {
            if (CachedAuthenticatedSessionHandler.ATTRIBUTE_NAME.equals(name)) {
                AuthenticatedSession auth = (AuthenticatedSession) value;
                return AUTO_REAUTHENTICATING_MECHANISMS.contains(auth.getMechanism()) ? session.getLocalContext().put(name, auth) : session.getAttributes().setAttribute(name, auth);
            }
            if (LOCAL_CONTEXT_ATTRIBUTES.contains(name)) {
                return session.getLocalContext().put(name, value);
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
        Session<Map<String, Object>> session = this.entry.getKey();
        this.validate(session);
        try (BatchContext context = this.resumeBatch()) {
            if (CachedAuthenticatedSessionHandler.ATTRIBUTE_NAME.equals(name)) {
                AuthenticatedSession auth = (AuthenticatedSession) session.getAttributes().removeAttribute(name);
                return (auth != null) ? auth : session.getLocalContext().remove(name);
            }
            if (LOCAL_CONTEXT_ATTRIBUTES.contains(name)) {
                return session.getLocalContext().remove(name);
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
        Map.Entry<Session<Map<String, Object>>, SessionConfig> entry = this.entry;
        Session<Map<String, Object>> session = entry.getKey();
        this.validate(exchange, session);
        this.validateBatch(session);
        // Invoke listeners outside of the context of the batch associated with this session
        this.manager.getSessionListeners().sessionDestroyed(this, exchange, SessionDestroyedReason.INVALIDATED);
        try (BatchContext context = this.resumeBatch()) {
            // Trigger attribute listeners
            ImmutableSessionAttributes attributes = session.getAttributes();
            for (String name : attributes.getAttributeNames()) {
                Object value = attributes.getAttribute(name);
                this.manager.getSessionListeners().attributeRemoved(this, name, value);
            }
            session.invalidate();
            if (exchange != null) {
                String id = session.getId();
                entry.getValue().clearSession(exchange, id);
            }
            this.batch.close();
        } finally {
            this.closeTask.accept(exchange);
        }
    }

    @Override
    public String changeSessionId(HttpServerExchange exchange, SessionConfig config) {
        Session<Map<String, Object>> oldSession = this.entry.getKey();
        this.validate(exchange, oldSession);
        SessionManager<Map<String, Object>, Batch> manager = this.manager.getSessionManager();
        String id = manager.createIdentifier();
        try (BatchContext context = this.resumeBatch()) {
            Session<Map<String, Object>> newSession = manager.createSession(id);
            for (String name: oldSession.getAttributes().getAttributeNames()) {
                newSession.getAttributes().setAttribute(name, oldSession.getAttributes().getAttribute(name));
            }
            newSession.getMetaData().setMaxInactiveInterval(oldSession.getMetaData().getMaxInactiveInterval());
            newSession.getMetaData().setLastAccess(oldSession.getMetaData().getLastAccessStartTime(), oldSession.getMetaData().getLastAccessEndTime());
            newSession.getLocalContext().putAll(oldSession.getLocalContext());
            config.setSessionId(exchange, id);
            this.entry = new SimpleImmutableEntry<>(newSession, config);
            oldSession.invalidate();
        }
        // Invoke listeners outside of the context of the batch associated with this session
        this.manager.getSessionListeners().sessionIdChanged(this, oldSession.getId());
        return id;
    }

    private void validate(Session<Map<String, Object>> session) {
        this.validate(null, session);
    }

    private void validate(HttpServerExchange exchange, Session<Map<String, Object>> session) {
        if (!session.isValid()) {
            // Workaround for UNDERTOW-1402
            // Ensure close task is run before throwing exception
            this.closeTask.accept(exchange);
            throw UndertowClusteringLogger.ROOT_LOGGER.sessionIsInvalid(session.getId());
        }
    }

    private void validateBatch(Session<Map<String, Object>> session) {
        if (this.batch.getState() == Batch.State.CLOSED) {
            throw UndertowClusteringLogger.ROOT_LOGGER.batchIsAlreadyClosed(session.getId());
        }
    }

    private BatchContext resumeBatch() {
        Batch batch = (this.batch.getState() != Batch.State.CLOSED) ? this.batch : null;
        return this.manager.getSessionManager().getBatcher().resumeBatch(batch);
    }
}
