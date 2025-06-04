/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.clustering.web.undertow.session;

import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.StampedLock;

import io.undertow.UndertowMessages;
import io.undertow.server.HttpServerExchange;
import io.undertow.server.session.SessionConfig;
import io.undertow.server.session.SessionListener;
import io.undertow.server.session.SessionListeners;
import io.undertow.server.session.SessionManagerStatistics;
import io.undertow.util.AttachmentKey;

import org.wildfly.clustering.cache.batch.Batch;
import org.wildfly.clustering.function.Consumer;
import org.wildfly.clustering.session.IdentifierMarshaller;
import org.wildfly.clustering.session.Session;
import org.wildfly.clustering.session.SessionManager;
import org.wildfly.clustering.web.undertow.UndertowIdentifierSerializerProvider;
import org.wildfly.clustering.web.undertow.logging.UndertowClusteringLogger;

/**
 * Adapts a distributable {@link SessionManager} to an Undertow {@link io.undertow.server.session.SessionManager}.
 * @author Paul Ferraro
 */
public class DistributableSessionManager implements UndertowSessionManager {

    private static final IdentifierMarshaller IDENTIFIER_MARSHALLER = new UndertowIdentifierSerializerProvider().getMarshaller();

    private final AttachmentKey<io.undertow.server.session.Session> key = AttachmentKey.create(io.undertow.server.session.Session.class);
    private final String deploymentName;
    private final SessionListeners listeners;
    private final SessionManager<Map<String, Object>> manager;
    private final RecordableSessionManagerStatistics statistics;
    private final StampedLock lifecycleLock = new StampedLock();
    private final AtomicLong lifecycleStamp = new AtomicLong(0L);

    // Matches io.undertow.server.session.InMemorySessionManager
    private volatile int defaultSessionTimeout = 30 * 60;

    public DistributableSessionManager(DistributableSessionManagerConfiguration config) {
        this.deploymentName = config.getDeploymentName();
        this.manager = config.getSessionManager();
        this.listeners = config.getSessionListeners();
        this.statistics = config.getStatistics();
    }

    @Override
    public SessionListeners getSessionListeners() {
        return this.listeners;
    }

    @Override
    public SessionManager<Map<String, Object>> getSessionManager() {
        return this.manager;
    }

    @Override
    public synchronized void start() {
        long stamp = this.lifecycleStamp.getAndSet(0L);
        if (StampedLock.isWriteLockStamp(stamp)) {
            this.lifecycleLock.unlockWrite(stamp);
        }
        this.manager.start();
        if (this.statistics != null) {
            this.statistics.reset();
        }
    }

    @Override
    public synchronized void stop() {
        try {
            this.lifecycleStamp.set(this.lifecycleLock.tryWriteLock(60, TimeUnit.SECONDS));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        this.manager.stop();
    }

    private Consumer<HttpServerExchange> getSessionCloseTask() {
        StampedLock lock = this.lifecycleLock;
        long stamp = lock.tryReadLock();
        if (!StampedLock.isReadLockStamp(stamp)) {
            throw UndertowClusteringLogger.ROOT_LOGGER.sessionManagerStopped();
        }
        AttachmentKey<io.undertow.server.session.Session> key = this.key;
        AtomicLong stampRef = new AtomicLong(stamp);
        return new Consumer<>() {
            @Override
            public void accept(HttpServerExchange exchange) {
                try {
                    // Ensure we only unlock once.
                    long stamp = stampRef.getAndSet(0L);
                    if (StampedLock.isReadLockStamp(stamp)) {
                        lock.unlockRead(stamp);
                    }
                } finally {
                    if (exchange != null) {
                        exchange.removeAttachment(key);
                    }
                }
            }
        };
    }

    @Override
    public io.undertow.server.session.Session createSession(HttpServerExchange exchange, SessionConfig config) {
        if (config == null) {
            throw UndertowMessages.MESSAGES.couldNotFindSessionCookieConfig();
        }
        if (exchange.isResponseStarted()) { // Should match the condition in io.undertow.servlet.spec.HttpServletResponseImpl#isCommitted()
            // Return single-use session to be garbage collected at the end of the request
            io.undertow.server.session.Session session = new OrphanSession(this, this.manager.getIdentifierFactory().get());
            session.setMaxInactiveInterval(this.defaultSessionTimeout);
            return session;
        }

        String requestedId = config.findSessionId(exchange);

        boolean close = true;
        Consumer<HttpServerExchange> closeTask = this.getSessionCloseTask();
        try {
            String id = (requestedId == null) ? this.manager.getIdentifierFactory().get() : requestedId;

            // Batch will be closed by Session.close();
            Batch batch = this.manager.getBatchFactory().get();
            try {
                Session<Map<String, Object>> session = this.manager.createSession(id);
                if (session == null) {
                    throw UndertowClusteringLogger.ROOT_LOGGER.sessionAlreadyExists(id);
                }
                // Apply session ID encoding
                config.setSessionId(exchange, id);

                io.undertow.server.session.Session result = new DistributableSession(this, session, config, batch.suspend(), closeTask, this.statistics);
                this.listeners.sessionCreated(result, exchange);
                if (this.statistics != null) {
                    this.statistics.record(session.getMetaData());
                }
                exchange.putAttachment(this.key, result);
                close = false;
                return result;
            } catch (RuntimeException | Error e) {
                batch.discard();
                throw e;
            } finally {
                if (close) {
                    batch.close();
                }
            }
        } finally {
            if (close) {
                closeTask.accept(exchange);
            }
        }
    }

    @Override
    public io.undertow.server.session.Session getSession(HttpServerExchange exchange, SessionConfig config) {
        if (exchange != null) {
            io.undertow.server.session.Session attachedSession = exchange.getAttachment(this.key);
            if (attachedSession != null) {
                return attachedSession;
            }
        }

        if (config == null) {
            throw UndertowMessages.MESSAGES.couldNotFindSessionCookieConfig();
        }

        String id = config.findSessionId(exchange);
        if (id == null) {
            return null;
        }

        // If requested id contains invalid characters, then session cannot exist and would otherwise cause session lookup to fail
        if (!IDENTIFIER_MARSHALLER.validate(id)) {
            return null;
        }

        boolean close = true;
        Consumer<HttpServerExchange> closeTask = this.getSessionCloseTask();
        try {
            Batch batch = this.manager.getBatchFactory().get();
            try {
                Session<Map<String, Object>> session = this.manager.findSession(id);
                if ((session == null) || !session.isValid() || session.getMetaData().isExpired()) {
                    return null;
                }
                // Update session ID encoding
                config.setSessionId(exchange, id);

                io.undertow.server.session.Session result = new DistributableSession(this, session, config, batch.suspend(), closeTask, this.statistics);
                if (exchange != null) {
                    exchange.putAttachment(this.key, result);
                }
                close = false;
                return result;
            } catch (RuntimeException | Error e) {
                batch.discard();
                throw e;
            } finally {
                if (close) {
                    batch.close();
                }
            }
        } finally {
            if (close) {
                closeTask.accept(exchange);
            }
        }
    }

    @Override
    public void registerSessionListener(SessionListener listener) {
        this.listeners.addSessionListener(listener);
    }

    @Override
    public void removeSessionListener(SessionListener listener) {
        this.listeners.removeSessionListener(listener);
    }

    @Override
    public void setDefaultSessionTimeout(int timeout) {
        this.defaultSessionTimeout = timeout;
    }

    @Override
    public Set<String> getTransientSessions() {
        // We are a distributed session manager, so none of our sessions are transient
        return Collections.emptySet();
    }

    @Override
    public Set<String> getActiveSessions() {
        return this.manager.getStatistics().getActiveSessions();
    }

    @Override
    public Set<String> getAllSessions() {
        return this.manager.getStatistics().getSessions();
    }

    @Override
    public io.undertow.server.session.Session getSession(String sessionId) {
        // If requested id contains invalid characters, then session cannot exist and would otherwise cause session lookup to fail
        if (!IDENTIFIER_MARSHALLER.validate(sessionId)) {
            return null;
        }
        Session<Map<String, Object>> session = this.manager.getDetachedSession(sessionId);
        return session.isValid() ? new DistributableSession(this, session, new SimpleSessionConfig(sessionId), Batch.factory().get().suspend(), Consumer.empty(), null) : null;
    }

    @Override
    public String getDeploymentName() {
        return this.deploymentName;
    }

    @Override
    public SessionManagerStatistics getStatistics() {
        return this.statistics;
    }

    @Override
    public boolean equals(Object object) {
        if (!(object instanceof DistributableSessionManager)) return false;
        DistributableSessionManager manager = (DistributableSessionManager) object;
        return this.deploymentName.equals(manager.getDeploymentName());
    }

    @Override
    public int hashCode() {
        return this.deploymentName.hashCode();
    }

    @Override
    public String toString() {
        return this.deploymentName;
    }
}
