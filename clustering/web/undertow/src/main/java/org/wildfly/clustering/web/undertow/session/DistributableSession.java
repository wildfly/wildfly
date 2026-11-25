/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.clustering.web.undertow.session;

import io.undertow.server.HttpServerExchange;
import io.undertow.server.session.SessionConfig;

import java.time.Instant;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

import org.wildfly.clustering.function.Consumer;
import org.wildfly.clustering.function.UnaryOperator;
import org.wildfly.clustering.server.util.BlockingReference;
import org.wildfly.clustering.session.ImmutableSession;
import org.wildfly.clustering.session.Session;
import org.wildfly.clustering.session.SessionManager;
import org.wildfly.clustering.session.SessionMetaData;
import org.wildfly.clustering.web.undertow.logging.UndertowClusteringLogger;

/**
 * A distributable {@link io.undertow.server.session.Session} suitable for use by a request thread.
 * @author Paul Ferraro
 */
public class DistributableSession extends AbstractDistributableSession {
    private final UndertowSessionManager manager;
    private final AtomicReference<Consumer<HttpServerExchange>> closeTask;
    private final Instant startTime;
    private final BlockingReference<Session<Map<String, Object>>> reference;

    public DistributableSession(UndertowSessionManager manager, Session<Map<String, Object>> session, Consumer<HttpServerExchange> closeTask) {
        this(manager, BlockingReference.of(session), closeTask, session.getMetaData().getLastAccessStartTime().isEmpty() ? session.getMetaData().getCreationTime() : Instant.now());
    }

    private DistributableSession(UndertowSessionManager manager, BlockingReference<Session<Map<String, Object>>> reference, Consumer<HttpServerExchange> closeTask, Instant startTime) {
        super(manager, reference);
        this.manager = manager;
        this.reference = reference;
        this.closeTask = new AtomicReference<>(closeTask);
        this.startTime = startTime;
    }

    @Override
    public void requestDone(HttpServerExchange exchange) {
        // Guard against duplicate calls
        Consumer<HttpServerExchange> closeTask = this.closeTask.getAndSet(null);
        if (closeTask != null) {
            try {
                this.reference.getReader().read(completeSession -> {
                    // Session must be closed, even if invalid
                    try (Session<?> session = completeSession) {
                        if (session.isValid()) {
                            // According to §7.6 of the servlet specification:
                            // The session is considered to be accessed when a request that is part of the session is first handled by the servlet container.
                            session.getMetaData().setLastAccess(this.startTime, Instant.now());
                        }
                    } catch (Throwable e) {
                        // Don't propagate exceptions at the stage, since response was already committed
                        UndertowClusteringLogger.ROOT_LOGGER.warn(e.getLocalizedMessage(), e);
                    }
                });
            } finally {
                closeTask.accept(exchange);
            }
        }
    }

    @Override
    public void invalidate(HttpServerExchange exchange) {
        try {
            super.invalidate(exchange);
        } finally {
            // Undertow does not trigger requestDone(...) for invalidated sessions
            // Therefore session must be closed here
            this.close(exchange);
        }
    }

    @Override
    public String changeSessionId(HttpServerExchange exchange, SessionConfig config) {
        SessionManager<Map<String, Object>> manager = this.manager.getSessionManager();
        String newId = manager.getIdentifierFactory().get();
        // Capture old session ID, required by listener
        AtomicReference<String> oldId = new AtomicReference<>();
        String currentId = this.reference.getWriter(ImmutableSession.VALID).updateAndGet(new UnaryOperator<>() {
            @Override
            public Session<Map<String, Object>> apply(Session<Map<String, Object>> currentSession) {
                oldId.setPlain(currentSession.getId());
                SessionMetaData currentMetaData = currentSession.getMetaData();
                Map<String, Object> currentAttributes = currentSession.getAttributes();
                Session<Map<String, Object>> newSession = manager.createSession(newId);
                try {
                    newSession.getAttributes().putAll(currentAttributes);
                    SessionMetaData newMetaData = newSession.getMetaData();
                    currentMetaData.getMaxIdle().ifPresent(newMetaData::setMaxIdle);
                    currentMetaData.getLastAccess().ifPresent(newMetaData::setLastAccess);
                    newSession.getContext().putAll(currentSession.getContext());
                    currentSession.invalidate();
                    return newSession;
                } catch (RuntimeException | Error e) {
                    newSession.invalidate();
                    throw e;
                } finally {
                    Consumer.close().accept(newSession.isValid() ? currentSession : newSession);
                }
            }
        }).getId();
        if (currentId.equals(newId)) {
            // Implies that write operation was successful
            config.setSessionId(exchange, newId);

            this.manager.getSessionListeners().sessionIdChanged(this, oldId.getPlain());
        } else {
            // Implies that session was not valid
            this.close(exchange);
            throw UndertowClusteringLogger.ROOT_LOGGER.sessionIsInvalid(currentId);
        }
        return currentId;
    }

    @Override
    public long getCreationTime() {
        try {
            return super.getCreationTime();
        } catch (IllegalStateException e) {
            this.close(null);
            throw e;
        }
    }

    @Override
    public long getLastAccessedTime() {
        try {
            return super.getLastAccessedTime();
        } catch (IllegalStateException e) {
            this.close(null);
            throw e;
        }
    }

    @Override
    public int getMaxInactiveInterval() {
        try {
            return super.getMaxInactiveInterval();
        } catch (IllegalStateException e) {
            this.close(null);
            throw e;
        }
    }

    @Override
    public void setMaxInactiveInterval(int interval) {
        try {
            super.setMaxInactiveInterval(interval);
        } catch (IllegalStateException e) {
            this.close(null);
            throw e;
        }
    }

    @Override
    public Set<String> getAttributeNames() {
        try {
            return super.getAttributeNames();
        } catch (IllegalStateException e) {
            this.close(null);
            throw e;
        }
    }

    @Override
    public Object getAttribute(String name) {
        try {
            return super.getAttribute(name);
        } catch (IllegalStateException e) {
            this.close(null);
            throw e;
        }
    }

    @Override
    public Object setAttribute(String name, Object value) {
        try {
            return super.setAttribute(name, value);
        } catch (IllegalStateException e) {
            this.close(null);
            throw e;
        }
    }

    @Override
    public Object removeAttribute(String name) {
        try {
            return super.removeAttribute(name);
        } catch (IllegalStateException e) {
            this.close(null);
            throw e;
        }
    }

    private void close(HttpServerExchange exchange) {
        Consumer<HttpServerExchange> closeTask = this.closeTask.getAndSet(null);
        if (closeTask != null) {
            try {
                this.reference.getReader().read(Consumer.close());
            } finally {
                // Ensure close task is run
                closeTask.accept(exchange);
            }
        }
    }
}
