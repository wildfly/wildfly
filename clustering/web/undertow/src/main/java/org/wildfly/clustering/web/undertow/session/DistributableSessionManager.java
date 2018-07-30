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

import io.undertow.UndertowMessages;
import io.undertow.server.HttpServerExchange;
import io.undertow.server.session.SessionConfig;
import io.undertow.server.session.SessionListener;
import io.undertow.server.session.SessionListeners;
import io.undertow.server.session.SessionManagerStatistics;
import io.undertow.util.AttachmentKey;

import java.time.Duration;
import java.util.Collections;
import java.util.OptionalLong;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.StampedLock;
import java.util.function.Consumer;

import org.wildfly.clustering.ee.Batch;
import org.wildfly.clustering.ee.Batcher;
import org.wildfly.clustering.web.IdentifierSerializer;
import org.wildfly.clustering.web.session.ImmutableSession;
import org.wildfly.clustering.web.session.Session;
import org.wildfly.clustering.web.session.SessionManager;
import org.wildfly.clustering.web.undertow.UndertowIdentifierSerializerProvider;
import org.wildfly.clustering.web.undertow.logging.UndertowClusteringLogger;

/**
 * Adapts a distributable {@link SessionManager} to an Undertow {@link io.undertow.server.session.SessionManager}.
 * @author Paul Ferraro
 */
public class DistributableSessionManager implements UndertowSessionManager {

    private static final IdentifierSerializer IDENTIFIER_SERIALIZER = new UndertowIdentifierSerializerProvider().getSerializer();

    private final AttachmentKey<io.undertow.server.session.Session> key = AttachmentKey.create(io.undertow.server.session.Session.class);
    private final String deploymentName;
    private final SessionListeners listeners;
    private final SessionManager<LocalSessionContext, Batch> manager;
    private final RecordableSessionManagerStatistics statistics;
    private final StampedLock lifecycleLock = new StampedLock();

    // Guarded by this
    private OptionalLong lifecycleStamp = OptionalLong.empty();

    public DistributableSessionManager(String deploymentName, SessionManager<LocalSessionContext, Batch> manager, SessionListeners listeners, RecordableSessionManagerStatistics statistics) {
        this.deploymentName = deploymentName;
        this.manager = manager;
        this.listeners = listeners;
        this.statistics = statistics;
    }

    @Override
    public SessionListeners getSessionListeners() {
        return this.listeners;
    }

    @Override
    public SessionManager<LocalSessionContext, Batch> getSessionManager() {
        return this.manager;
    }

    @Override
    public synchronized void start() {
        this.lifecycleStamp.ifPresent(stamp -> this.lifecycleLock.unlock(stamp));
        this.manager.start();
        if (this.statistics != null) {
            this.statistics.reset();
        }
    }

    @Override
    public synchronized void stop() {
        if (!this.lifecycleStamp.isPresent()) {
            try {
                long stamp = this.lifecycleLock.tryWriteLock(this.manager.getDefaultMaxInactiveInterval().getSeconds(), TimeUnit.SECONDS);
                if (stamp != 0) {
                    this.lifecycleStamp = OptionalLong.of(stamp);
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            this.manager.stop();
        }
    }

    private Consumer<HttpServerExchange> getSessionCloseTask() {
        StampedLock lock = this.lifecycleLock;
        long stamp = lock.tryReadLock();
        if (stamp == 0L) {
            throw UndertowClusteringLogger.ROOT_LOGGER.sessionManagerStopped();
        }
        AttachmentKey<io.undertow.server.session.Session> key = this.key;
        AtomicLong stampRef = new AtomicLong(stamp);
        return new Consumer<HttpServerExchange>() {
            @Override
            public void accept(HttpServerExchange exchange) {
                try {
                    // Ensure we only unlock once.
                    long stamp = stampRef.getAndSet(0L);
                    if (stamp != 0L) {
                        lock.unlock(stamp);
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

        String requestedId = config.findSessionId(exchange);
        String id = (requestedId == null) ? this.manager.createIdentifier() : requestedId;

        Consumer<HttpServerExchange> closeTask = this.getSessionCloseTask();
        boolean close = true;
        try {
            Batcher<Batch> batcher = this.manager.getBatcher();
            // Batch will be closed by Session.close();
            Batch batch = batcher.createBatch();
            try {
                Session<LocalSessionContext> session = this.manager.createSession(id);
                if (session == null) {
                    throw UndertowClusteringLogger.ROOT_LOGGER.sessionAlreadyExists(id);
                }
                if (requestedId == null) {
                    config.setSessionId(exchange, id);
                }

                io.undertow.server.session.Session result = new DistributableSession(this, session, config, batcher.suspendBatch(), closeTask);
                this.listeners.sessionCreated(result, exchange);
                if (this.statistics != null) {
                    this.statistics.record(result);
                }
                close = false;
                exchange.putAttachment(this.key, result);
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
        if (!IDENTIFIER_SERIALIZER.validate(id)) {
            return null;
        }

        Consumer<HttpServerExchange> closeTask = this.getSessionCloseTask();
        boolean close = true;
        try {
            Batcher<Batch> batcher = this.manager.getBatcher();
            Batch batch = batcher.createBatch();
            try {
                Session<LocalSessionContext> session = this.manager.findSession(id);
                if (session == null) {
                    return null;
                }

                io.undertow.server.session.Session result = new DistributableSession(this, session, config, batcher.suspendBatch(), closeTask);
                close = false;
                if (exchange != null) {
                    exchange.putAttachment(this.key, result);
                }
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
        this.manager.setDefaultMaxInactiveInterval(Duration.ofSeconds(timeout));
    }

    @Override
    public Set<String> getTransientSessions() {
        // We are a distributed session manager, so none of our sessions are transient
        return Collections.emptySet();
    }

    @Override
    public Set<String> getActiveSessions() {
        return this.manager.getActiveSessions();
    }

    @Override
    public Set<String> getAllSessions() {
        return this.manager.getLocalSessions();
    }

    @Override
    public io.undertow.server.session.Session getSession(String sessionId) {
        // If requested id contains invalid characters, then session cannot exist and would otherwise cause session lookup to fail
        if (!IDENTIFIER_SERIALIZER.validate(sessionId)) {
            return null;
        }
        try (Batch batch = this.manager.getBatcher().createBatch()) {
            try {
                ImmutableSession session = this.manager.viewSession(sessionId);
                return (session != null) ? new DistributableImmutableSession(this, session) : null;
            } catch (RuntimeException | Error e) {
                batch.discard();
                throw e;
            }
        }
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
