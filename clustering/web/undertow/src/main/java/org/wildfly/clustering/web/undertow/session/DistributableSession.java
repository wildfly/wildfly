/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.clustering.web.undertow.session;

import io.undertow.security.api.AuthenticatedSessionManager.AuthenticatedSession;
import io.undertow.server.HttpServerExchange;
import io.undertow.server.session.SessionConfig;
import io.undertow.server.session.SessionListener.SessionDestroyedReason;
import io.undertow.servlet.handlers.security.CachedAuthenticatedSessionHandler;
import jakarta.servlet.http.HttpServletRequest;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

import org.wildfly.clustering.cache.batch.Batch;
import org.wildfly.clustering.cache.batch.SuspendedBatch;
import org.wildfly.clustering.context.Context;
import org.wildfly.clustering.session.Session;
import org.wildfly.clustering.session.SessionManager;
import org.wildfly.clustering.session.SessionMetaData;
import org.wildfly.clustering.web.undertow.logging.UndertowClusteringLogger;

/**
 * A distributable {@link io.undertow.server.session.Session} suitable for use by a request thread.
 * @author Paul Ferraro
 */
public class DistributableSession implements io.undertow.server.session.Session {
    // These mechanisms can auto-reauthenticate and thus use local context (instead of replicating)
    private static final Set<String> AUTO_REAUTHENTICATING_MECHANISMS = Set.of(HttpServletRequest.BASIC_AUTH, HttpServletRequest.DIGEST_AUTH, HttpServletRequest.CLIENT_CERT_AUTH);
    static final String WEB_SOCKET_CHANNELS_ATTRIBUTE = "io.undertow.websocket.current-connections";
    private static final Set<String> LOCAL_CONTEXT_ATTRIBUTES = Set.of(WEB_SOCKET_CHANNELS_ATTRIBUTE);

    private final UndertowSessionManager manager;
    private final SuspendedBatch suspendedBatch;
    private final AtomicReference<Consumer<HttpServerExchange>> closeTask;
    private final Instant startTime;
    private final RecordableSessionManagerStatistics statistics;
    private final AtomicReference<Map.Entry<Session<Map<String, Object>>, SessionConfig>> reference;

    public DistributableSession(UndertowSessionManager manager, Session<Map<String, Object>> session, SessionConfig config, SuspendedBatch suspendedBatch, Consumer<HttpServerExchange> closeTask, RecordableSessionManagerStatistics statistics) {
        this(manager, new AtomicReference<>(Map.entry(session, config)), suspendedBatch, closeTask, statistics);
    }

    private DistributableSession(UndertowSessionManager manager, AtomicReference<Map.Entry<Session<Map<String, Object>>, SessionConfig>> reference, SuspendedBatch suspendedBatch, Consumer<HttpServerExchange> closeTask, RecordableSessionManagerStatistics statistics) {
        this.manager = manager;
        this.reference = reference;
        this.suspendedBatch = suspendedBatch;
        this.closeTask = new AtomicReference<>(closeTask);
        Session<Map<String, Object>> session = reference.get().getKey();
        this.startTime = session.getMetaData().getLastAccessTime().isEmpty() ? session.getMetaData().getCreationTime() : Instant.now();
        this.statistics = statistics;
    }

    @Override
    public void requestDone(HttpServerExchange exchange) {
        Consumer<HttpServerExchange> closeTask = this.closeTask.getAndSet(null);
        if (closeTask != null) {
            Session<Map<String, Object>> currentSession = this.reference.get().getKey();
            try (Context<Batch> context = this.suspendedBatch.resumeWithContext()) {
                try (Batch batch = context.get()) {
                    // Ensure session is closed, even if invalid
                    try (Session<Map<String, Object>> session = currentSession) {
                        if (session.isValid()) {
                            // According to §7.6 of the servlet specification:
                            // The session is considered to be accessed when a request that is part of the session is first handled by the servlet container.
                            session.getMetaData().setLastAccess(this.startTime, Instant.now());
                        }
                    }
                } catch (Throwable e) {
                    // Don't propagate exceptions at the stage, since response was already committed
                    UndertowClusteringLogger.ROOT_LOGGER.warn(e.getLocalizedMessage(), e);
                } finally {
                    this.reference.set(Map.entry(currentSession, new SimpleSessionConfig(currentSession.getId())));
                    closeTask.accept(exchange);
                }
            }
        }
    }

    @Override
    public UndertowSessionManager getSessionManager() {
        return this.manager;
    }

    @Override
    public String getId() {
        return this.reference.get().getKey().getId();
    }

    @Override
    public long getCreationTime() {
        Session<Map<String, Object>> session = this.reference.get().getKey();
        try {
            return session.getMetaData().getCreationTime().toEpochMilli();
        } catch (IllegalStateException e) {
            this.closeIfInvalid(session, null);
            throw e;
        }
    }

    @Override
    public long getLastAccessedTime() {
        Session<Map<String, Object>> session = this.reference.get().getKey();
        try {
            SessionMetaData metaData = this.reference.get().getKey().getMetaData();
            return metaData.getLastAccessStartTime().orElseGet(metaData::getCreationTime).toEpochMilli();
        } catch (IllegalStateException e) {
            this.closeIfInvalid(session, null);
            throw e;
        }
    }

    @Override
    public int getMaxInactiveInterval() {
        Session<Map<String, Object>> session = this.reference.get().getKey();
        try {
            return this.reference.get().getKey().getMetaData().getMaxIdle().map(Duration::getSeconds).orElse(0L).intValue();
        } catch (IllegalStateException e) {
            this.closeIfInvalid(session, null);
            throw e;
        }
    }

    @Override
    public void setMaxInactiveInterval(int interval) {
        Session<Map<String, Object>> session = this.reference.get().getKey();
        try {
            this.reference.get().getKey().getMetaData().setMaxIdle((interval > 0) ? Duration.ofSeconds(interval) : null);
        } catch (IllegalStateException e) {
            this.closeIfInvalid(session, null);
            throw e;
        }
    }

    @Override
    public Set<String> getAttributeNames() {
        Session<Map<String, Object>> session = this.reference.get().getKey();
        try {
            return this.reference.get().getKey().getAttributes().keySet();
        } catch (IllegalStateException e) {
            this.closeIfInvalid(session, null);
            throw e;
        }
    }

    @Override
    public Object getAttribute(String name) {
        Session<Map<String, Object>> session = this.reference.get().getKey();
        try {
            if (CachedAuthenticatedSessionHandler.ATTRIBUTE_NAME.equals(name)) {
                AuthenticatedSession auth = (AuthenticatedSession) session.getAttributes().get(name);
                return (auth != null) ? auth : session.getContext().get(name);
            }
            if (LOCAL_CONTEXT_ATTRIBUTES.contains(name)) {
                return session.getContext().get(name);
            }
            return session.getAttributes().get(name);
        } catch (IllegalStateException e) {
            this.closeIfInvalid(session, null);
            throw e;
        }
    }

    @Override
    public Object setAttribute(String name, Object value) {
        if (value == null) {
            return this.removeAttribute(name);
        }
        Session<Map<String, Object>> session = this.reference.get().getKey();
        try {
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
        } catch (IllegalStateException e) {
            this.closeIfInvalid(session, null);
            throw e;
        }
    }

    @Override
    public Object removeAttribute(String name) {
        Session<Map<String, Object>> session = this.reference.get().getKey();
        try {
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
        } catch (IllegalStateException e) {
            this.closeIfInvalid(session, null);
            throw e;
        }
    }

    @Override
    public void invalidate(HttpServerExchange exchange) {
        Map.Entry<Session<Map<String, Object>>, SessionConfig> entry = this.reference.get();
        Session<Map<String, Object>> session = entry.getKey();
        if (session.isValid()) {
            // Invoke listeners outside of the context of the batch associated with this session
            // Trigger attribute listeners
            this.manager.getSessionListeners().sessionDestroyed(this, exchange, SessionDestroyedReason.INVALIDATED);

            for (Map.Entry<String, Object> attributesEntry : session.getAttributes().entrySet()) {
                this.manager.getSessionListeners().attributeRemoved(this, attributesEntry.getKey(), attributesEntry.getValue());
            }
            if (this.statistics != null) {
                this.statistics.getInactiveSessionRecorder().record(session.getMetaData());
            }
        }
        Consumer<HttpServerExchange> closeTask = this.closeTask.getAndSet(null);
        if (closeTask != null) {
            try (Context<Batch> context = this.suspendedBatch.resumeWithContext()) {
                try (Batch batch = context.get()) {
                    try (Session<Map<String, Object>> validSession = session) {
                        session.invalidate();
                    } finally {
                        if (exchange != null) {
                            entry.getValue().clearSession(exchange, session.getId());
                        }
                    }
                } finally {
                    closeTask.accept(exchange);
                }
            }
        }
    }

    @Override
    public String changeSessionId(HttpServerExchange exchange, SessionConfig config) {
        SessionManager<Map<String, Object>> manager = this.manager.getSessionManager();
        String id = manager.getIdentifierFactory().get();
        Session<Map<String, Object>> currentSession = this.reference.get().getKey();
        try {
            SessionMetaData currentMetaData = currentSession.getMetaData();
            Map<String, Object> currentAttributes = currentSession.getAttributes();
            try (Context<Batch> context = this.suspendedBatch.resumeWithContext()) {
                Session<Map<String, Object>> newSession = manager.createSession(id);
                try {
                    newSession.getAttributes().putAll(currentAttributes);
                    SessionMetaData newMetaData = newSession.getMetaData();
                    currentMetaData.getMaxIdle().ifPresent(newMetaData::setMaxIdle);
                    currentMetaData.getLastAccess().ifPresent(newMetaData::setLastAccess);
                    newSession.getContext().putAll(currentSession.getContext());
                    currentSession.invalidate();
                    config.setSessionId(exchange, id);
                    this.reference.set(Map.entry(newSession, config));
                } catch (IllegalStateException e) {
                    newSession.invalidate();
                    throw e;
                }
            }
            if (!currentSession.isValid()) {
                // Invoke listeners outside of the context of the batch associated with this session
                this.manager.getSessionListeners().sessionIdChanged(this, currentSession.getId());
            }
        } catch (IllegalStateException e) {
            this.closeIfInvalid(currentSession, exchange);
            throw e;
        }
        return id;
    }

    /*
     * New method in io.undertow.server.session.Session that can add the @Override annotation when Undertow is upgraded
     */
    public boolean isInvalid() {
        return !this.reference.get().getKey().isValid();
    }

    /*
     * New method in io.undertow.server.session.Session that can add the @Override annotation when Undertow is upgraded
     */
    public io.undertow.server.session.Session detach() {
        return this.manager.getSession(this.getId());
    }

    private void closeIfInvalid(Session<Map<String, Object>> session, HttpServerExchange exchange) {
        // If session was invalidated by a concurrent request, Undertow will not trigger Session.requestDone(...), so we need to close the session here
        if (!session.isValid()) {
            Consumer<HttpServerExchange> closeTask = this.closeTask.getAndSet(null);
            if (closeTask != null) {
                try (Context<Batch> context = this.suspendedBatch.resumeWithContext()) {
                    try (Batch batch = context.get()) {
                        session.close();
                    }
                } finally {
                    // Ensure close task is run
                    closeTask.accept(exchange);
                }
            }
        }
    }
}
