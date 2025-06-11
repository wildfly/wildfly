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

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

import jakarta.servlet.http.HttpServletRequest;

import org.wildfly.clustering.cache.batch.Batch;
import org.wildfly.clustering.cache.batch.BatchContext;
import org.wildfly.clustering.cache.batch.SuspendedBatch;
import org.wildfly.clustering.session.Session;
import org.wildfly.clustering.session.SessionManager;
import org.wildfly.clustering.session.SessionMetaData;
import org.wildfly.clustering.web.undertow.logging.UndertowClusteringLogger;

/**
 * Adapts a distributable {@link Session} to an Undertow {@link io.undertow.server.session.Session}.
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

    private volatile Map.Entry<Session<Map<String, Object>>, SessionConfig> entry;

    public DistributableSession(UndertowSessionManager manager, Session<Map<String, Object>> session, SessionConfig config, SuspendedBatch suspendedBatch, Consumer<HttpServerExchange> closeTask, RecordableSessionManagerStatistics statistics) {
        this.manager = manager;
        this.entry = Map.entry(session, config);
        this.suspendedBatch = suspendedBatch;
        this.closeTask = new AtomicReference<>(closeTask);
        this.startTime = session.getMetaData().isNew() ? session.getMetaData().getCreationTime() : Instant.now();
        this.statistics = statistics;
    }

    @Override
    public io.undertow.server.session.SessionManager getSessionManager() {
        return this.manager;
    }

    @Override
    public void requestDone(HttpServerExchange exchange) {
        Consumer<HttpServerExchange> closeTask = this.closeTask.getAndSet(null);
        if (closeTask != null) {
            try (Batch batch = this.suspendedBatch.resume()) {
                // Ensure session is closed, even if invalid
                try (Session<Map<String, Object>> session = this.entry.getKey()) {
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
                this.entry = Map.entry(this.entry.getKey(), new SimpleSessionConfig(this.entry.getKey().getId()));
                closeTask.accept(exchange);
            }
        }
    }

    @Override
    public String getId() {
        return this.entry.getKey().getId();
    }

    @Override
    public long getCreationTime() {
        Session<Map<String, Object>> session = this.entry.getKey();
        try {
            return this.entry.getKey().getMetaData().getCreationTime().toEpochMilli();
        } catch (IllegalStateException e) {
            this.closeIfInvalid(null, session);
            throw e;
        }
    }

    @Override
    public long getLastAccessedTime() {
        Session<Map<String, Object>> session = this.entry.getKey();
        try {
            SessionMetaData metaData = session.getMetaData();
            return Optional.ofNullable(metaData.getLastAccessStartTime()).orElseGet(metaData::getCreationTime).toEpochMilli();
        } catch (IllegalStateException e) {
            this.closeIfInvalid(null, session);
            throw e;
        }
    }

    @Override
    public int getMaxInactiveInterval() {
        Session<Map<String, Object>> session = this.entry.getKey();
        try {
            return (int) session.getMetaData().getTimeout().getSeconds();
        } catch (IllegalStateException e) {
            this.closeIfInvalid(null, session);
            throw e;
        }
    }

    @Override
    public void setMaxInactiveInterval(int interval) {
        Session<Map<String, Object>> session = this.entry.getKey();
        try {
            session.getMetaData().setTimeout(Duration.ofSeconds(interval));
        } catch (IllegalStateException e) {
            this.closeIfInvalid(null, session);
            throw e;
        }
    }

    @Override
    public Set<String> getAttributeNames() {
        Session<Map<String, Object>> session = this.entry.getKey();
        try {
            return session.getAttributes().keySet();
        } catch (IllegalStateException e) {
            this.closeIfInvalid(null, session);
            throw e;
        }
    }

    @Override
    public Object getAttribute(String name) {
        Session<Map<String, Object>> session = this.entry.getKey();
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
            this.closeIfInvalid(null, session);
            throw e;
        }
    }

    @Override
    public Object setAttribute(String name, Object value) {
        if (value == null) {
            return this.removeAttribute(name);
        }
        Session<Map<String, Object>> session = this.entry.getKey();
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
            this.closeIfInvalid(null, session);
            throw e;
        }
    }

    @Override
    public Object removeAttribute(String name) {
        Session<Map<String, Object>> session = this.entry.getKey();
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
            this.closeIfInvalid(null, session);
            throw e;
        }
    }

    @Override
    public void invalidate(HttpServerExchange exchange) {
        Map.Entry<Session<Map<String, Object>>, SessionConfig> entry = this.entry;
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
            try (Batch batch = this.suspendedBatch.resume()) {
                try (Session<Map<String, Object>> validSession = session) {
                    session.invalidate();
                } finally {
                    if (exchange != null) {
                        String id = session.getId();
                        entry.getValue().clearSession(exchange, id);
                    }
                }
            } finally {
                closeTask.accept(exchange);
            }
        }
    }

    @Override
    public String changeSessionId(HttpServerExchange exchange, SessionConfig config) {
        Session<Map<String, Object>> oldSession = this.entry.getKey();
        SessionManager<Map<String, Object>> manager = this.manager.getSessionManager();
        String id = manager.getIdentifierFactory().get();
        try (BatchContext<Batch> context = this.suspendedBatch.resumeWithContext()) {
            Session<Map<String, Object>> newSession = manager.createSession(id);
            try {
                newSession.getAttributes().putAll(oldSession.getAttributes());
                SessionMetaData oldMetaData = oldSession.getMetaData();
                SessionMetaData newMetaData = newSession.getMetaData();
                newMetaData.setTimeout(oldMetaData.getTimeout());
                Instant lastAccessStartTime = oldMetaData.getLastAccessStartTime();
                Instant lastAccessEndTime = oldMetaData.getLastAccessEndTime();
                if ((lastAccessStartTime != null) && (lastAccessEndTime != null)) {
                    newMetaData.setLastAccess(oldSession.getMetaData().getLastAccessStartTime(), oldSession.getMetaData().getLastAccessEndTime());
                }
                newSession.getContext().putAll(oldSession.getContext());
                oldSession.invalidate();
                config.setSessionId(exchange, id);
                this.entry = Map.entry(newSession, config);
            } catch (IllegalStateException e) {
                this.closeIfInvalid(exchange, oldSession);
                newSession.invalidate();
                throw e;
            }
        }
        if (!oldSession.isValid()) {
            // Invoke listeners outside of the context of the batch associated with this session
            this.manager.getSessionListeners().sessionIdChanged(this, oldSession.getId());
        }
        return id;
    }

    private void closeIfInvalid(HttpServerExchange exchange, Session<Map<String, Object>> session) {
        if (!session.isValid()) {
            // If session was invalidated by a concurrent request, Undertow will not trigger Session.requestDone(...), so we need to close the session here
            Consumer<HttpServerExchange> closeTask = this.closeTask.getAndSet(null);
            if (closeTask != null) {
                try {
                    session.close();
                } finally {
                    // Ensure close task is run
                    closeTask.accept(exchange);
                }
            }
        }
    }
}
