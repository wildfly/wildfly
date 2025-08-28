/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.clustering.web.undertow.session;

import io.undertow.server.HttpServerExchange;
import io.undertow.server.session.SessionConfig;
import io.undertow.server.session.SessionListener.SessionDestroyedReason;

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
public class DistributableSession extends AbstractSession {

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
        super(manager, () -> reference.get().getKey());
        this.manager = manager;
        this.reference = reference;
        this.suspendedBatch = suspendedBatch;
        this.closeTask = new AtomicReference<>(closeTask);
        Session<Map<String, Object>> session = reference.get().getKey();
        this.startTime = session.getMetaData().isNew() ? session.getMetaData().getCreationTime() : Instant.now();
        this.statistics = statistics;
    }

    @Override
    public void requestDone(HttpServerExchange exchange) {
        Consumer<HttpServerExchange> closeTask = this.closeTask.getAndSet(null);
        if (closeTask != null) {
            Session<Map<String, Object>> currentSession = this.get();
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
    public long getCreationTime() {
        try {
            return super.getCreationTime();
        } catch (IllegalStateException e) {
            this.closeIfInvalid(null);
            throw e;
        }
    }

    @Override
    public long getLastAccessedTime() {
        try {
            return super.getLastAccessedTime();
        } catch (IllegalStateException e) {
            this.closeIfInvalid(null);
            throw e;
        }
    }

    @Override
    public int getMaxInactiveInterval() {
        try {
            return super.getMaxInactiveInterval();
        } catch (IllegalStateException e) {
            this.closeIfInvalid(null);
            throw e;
        }
    }

    @Override
    public void setMaxInactiveInterval(int interval) {
        try {
            super.setMaxInactiveInterval(interval);
        } catch (IllegalStateException e) {
            this.closeIfInvalid(null);
            throw e;
        }
    }

    @Override
    public Set<String> getAttributeNames() {
        try {
            return super.getAttributeNames();
        } catch (IllegalStateException e) {
            this.closeIfInvalid(null);
            throw e;
        }
    }

    @Override
    public Object getAttribute(String name) {
        try {
            return super.getAttribute(name);
        } catch (IllegalStateException e) {
            this.closeIfInvalid(null);
            throw e;
        }
    }

    @Override
    public Object setAttribute(String name, Object value) {
        try {
            return super.setAttribute(name, value);
        } catch (IllegalStateException e) {
            this.closeIfInvalid(null);
            throw e;
        }
    }

    @Override
    public Object removeAttribute(String name) {
        try {
            return super.removeAttribute(name);
        } catch (IllegalStateException e) {
            this.closeIfInvalid(null);
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
        Session<Map<String, Object>> currentSession = this.get();
        SessionManager<Map<String, Object>> manager = this.manager.getSessionManager();
        String id = manager.getIdentifierFactory().get();
        try (Context<Batch> context = this.suspendedBatch.resumeWithContext()) {
            Session<Map<String, Object>> newSession = manager.createSession(id);
            try {
                newSession.getAttributes().putAll(currentSession.getAttributes());
                SessionMetaData oldMetaData = currentSession.getMetaData();
                SessionMetaData newMetaData = newSession.getMetaData();
                newMetaData.setTimeout(oldMetaData.getTimeout());
                Instant lastAccessStartTime = oldMetaData.getLastAccessStartTime();
                Instant lastAccessEndTime = oldMetaData.getLastAccessEndTime();
                if ((lastAccessStartTime != null) && (lastAccessEndTime != null)) {
                    newMetaData.setLastAccess(currentSession.getMetaData().getLastAccessStartTime(), currentSession.getMetaData().getLastAccessEndTime());
                }
                newSession.getContext().putAll(currentSession.getContext());
                currentSession.invalidate();
                config.setSessionId(exchange, id);
                this.reference.set(Map.entry(newSession, config));
            } catch (IllegalStateException e) {
                this.closeIfInvalid(exchange);
                newSession.invalidate();
                throw e;
            }
        }
        if (!currentSession.isValid()) {
            // Invoke listeners outside of the context of the batch associated with this session
            this.manager.getSessionListeners().sessionIdChanged(this, currentSession.getId());
        }
        return id;
    }

    private void closeIfInvalid(HttpServerExchange exchange) {
        // If session was invalidated by a concurrent request, Undertow will not trigger Session.requestDone(...), so we need to close the session here
        Session<Map<String, Object>> session = this.get();
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
