/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.clustering.web.undertow.session;

import java.util.Collections;
import java.util.Map;
import java.util.Objects;
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

import org.jboss.logging.Logger;
import org.wildfly.clustering.cache.batch.Batch;
import org.wildfly.clustering.cache.batch.SuspendedBatch;
import org.wildfly.clustering.context.Context;
import org.wildfly.clustering.function.Consumer;
import org.wildfly.clustering.function.Function;
import org.wildfly.clustering.function.Predicate;
import org.wildfly.clustering.function.Supplier;
import org.wildfly.clustering.function.UnaryOperator;
import org.wildfly.clustering.session.IdentifierMarshaller;
import org.wildfly.clustering.session.Session;
import org.wildfly.clustering.session.SessionManager;
import org.wildfly.clustering.session.SessionMetaData;
import org.wildfly.clustering.web.undertow.UndertowIdentifierSerializerProvider;
import org.wildfly.clustering.web.undertow.logging.UndertowClusteringLogger;

/**
 * Adapts a distributable {@link SessionManager} to an Undertow {@link io.undertow.server.session.SessionManager}.
 * @author Paul Ferraro
 */
public class DistributableSessionManager implements UndertowSessionManager {
    private static final Logger LOGGER = Logger.getLogger(DistributableSessionManager.class);
    private static final IdentifierMarshaller IDENTIFIER_MARSHALLER = new UndertowIdentifierSerializerProvider().getMarshaller();
    private static final Predicate<String> REQUESTED_IDENTIFIER = Objects::nonNull;
    // If requested session id contains invalid characters, then session cannot exist
    private static final Predicate<String> VALID_IDENTIFIER = REQUESTED_IDENTIFIER.and(IDENTIFIER_MARSHALLER::validate);
    private static final Predicate<Session<Map<String, Object>>> EXISTING_SESSION = Objects::nonNull;
    private static final Predicate<Session<Map<String, Object>>> VALID_SESSION = EXISTING_SESSION.and(Session::isValid);
    // If session exists, verify that it was not invalidated by a concurrent thread
    private static final UnaryOperator<Session<Map<String, Object>>> VALIDATE_SESSION = new UnaryOperator<>() {
        @Override
        public Session<Map<String, Object>> apply(Session<Map<String, Object>> session) {
            if (!VALID_SESSION.test(session)) {
                Consumer.close().accept(session);
                return null;
            }
            return session;
        }
    };
    private static final UnaryOperator<Session<Map<String, Object>>> REQUIRE_SESSION = UnaryOperator.<Session<Map<String, Object>>>identity().orDefault(EXISTING_SESSION, () -> {
        throw new IllegalStateException();
    });

    private final AttachmentKey<io.undertow.server.session.Session> key = AttachmentKey.create(io.undertow.server.session.Session.class);
    private final String deploymentName;
    private final SessionListeners listeners;
    private final SessionManager<Map<String, Object>> manager;
    private final RecordableSessionManagerStatistics statistics;
    private final StampedLock lifecycleLock = new StampedLock();
    private final AtomicLong lifecycleStamp = new AtomicLong(0L);
    private final Supplier<Map.Entry<SuspendedBatch, Consumer<HttpServerExchange>>> batchEntryFactory = this::createBatchEntry;
    private final Function<String, Session<Map<String, Object>>> createSession;
    private final Function<String, Session<Map<String, Object>>> findSession;
    private final Function<String, Session<Map<String, Object>>> getDetachedSession;

    // Matches io.undertow.server.session.InMemorySessionManager
    private volatile int defaultSessionTimeout = 30 * 60;

    public DistributableSessionManager(DistributableSessionManagerConfiguration config) {
        this.deploymentName = config.getDeploymentName();
        this.manager = config.getSessionManager();
        this.listeners = config.getSessionListeners();
        this.statistics = config.getStatistics();

        Function<String, Session<Map<String, Object>>> createSession = this.manager::createSession;
        this.createSession = createSession.withDefault(VALID_IDENTIFIER, this.manager.getIdentifierFactory()).andThen(REQUIRE_SESSION);

        Function<String, Session<Map<String, Object>>> findSession = this.manager::findSession;
        this.findSession = findSession.orDefault(VALID_IDENTIFIER, Supplier.of(null)).andThen(VALIDATE_SESSION);

        Function<String, Session<Map<String, Object>>> getDetachedSession = this.manager::getDetachedSession;
        this.getDetachedSession = getDetachedSession.orDefault(VALID_IDENTIFIER, Supplier.of(null)).andThen(VALIDATE_SESSION);
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
    public void start() {
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
    public void stop() {
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
        if (exchange.isResponseStarted()) { // Should match the condition in io.undertow.servlet.spec.HttpServletResponseImpl#isCommitted()
            // Return single-use session to be garbage collected at the end of the request
            io.undertow.server.session.Session session = new OrphanSession(this, this.manager.getIdentifierFactory().get());
            session.setMaxInactiveInterval(this.defaultSessionTimeout);
            return session;
        }

        io.undertow.server.session.Session session = this.getSession(exchange, config, this.batchEntryFactory, this.createSession);
        try {
            this.listeners.sessionCreated(session, exchange);
        } catch (RuntimeException | Error e) {
            LOGGER.warn(e.getLocalizedMessage(), e);
        }
        return session;
    }

    @Override
    public io.undertow.server.session.Session getSession(HttpServerExchange exchange, SessionConfig config) {
        // Handle redundant calls to getSession(...)
        if (exchange != null) {
            io.undertow.server.session.Session session = exchange.getAttachment(this.key);
            if (session != null) {
                return session;
            }
        }
        return this.getSession(exchange, config, this.batchEntryFactory, this.findSession);
    }

    private io.undertow.server.session.Session getSession(HttpServerExchange exchange, SessionConfig config, Supplier<Map.Entry<SuspendedBatch, Consumer<HttpServerExchange>>> batchEntryFactory, Function<String, Session<Map<String, Object>>> sessionFactory) {
        if (config == null) {
            throw UndertowMessages.MESSAGES.couldNotFindSessionCookieConfig();
        }
        Map.Entry<SuspendedBatch, Consumer<HttpServerExchange>> entry = batchEntryFactory.get();
        SuspendedBatch suspendedBatch = entry.getKey();
        Consumer<HttpServerExchange> closeTask = entry.getValue();
        try (Context<Batch> context = suspendedBatch.resumeWithContext()) {
            Session<Map<String, Object>> session = sessionFactory.apply(config.findSessionId(exchange));
            if (session == null) {
                return close(context, closeTask);
            }
            // Apply session ID encoding
            config.setSessionId(exchange, session.getId());
            if (this.statistics != null) {
                SessionMetaData metaData = session.getMetaData();
                if (metaData.isNew()) {
                    this.statistics.record(metaData);
                }
            }
            DistributableSession result = new DistributableSession(this, session, config, suspendedBatch, closeTask, this.statistics);
            if (exchange != null) {
                exchange.putAttachment(this.key, result);
            }
            return result;
        } catch (RuntimeException | Error e) {
            try (Context<Batch> context = suspendedBatch.resumeWithContext()) {
                rollback(context, closeTask);
            }
            throw e;
        }
    }

    private static void rollback(java.util.function.Supplier<Batch> batchProvider, Consumer<HttpServerExchange> closeTask) {
        close(batchProvider, Batch::discard, closeTask);
    }

    private static io.undertow.server.session.Session close(java.util.function.Supplier<Batch> batchProvider, Consumer<HttpServerExchange> closeTask) {
        close(batchProvider, Consumer.empty(), closeTask);
        return null;
    }

    private static void close(java.util.function.Supplier<Batch> batchProvider, Consumer<Batch> batchTask, Consumer<HttpServerExchange> closeTask) {
        try (Batch batch = batchProvider.get()) {
            batchTask.accept(batch);
        } catch (RuntimeException | Error e) {
            LOGGER.error(e.getLocalizedMessage(), e);
        } finally {
            closeTask.accept(null);
        }
    }

    private Map.Entry<SuspendedBatch, Consumer<HttpServerExchange>> createBatchEntry() {
        Consumer<HttpServerExchange> closeTask = this.getSessionCloseTask();
        try {
            return Map.entry(this.manager.getBatchFactory().get().suspend(), closeTask);
        } catch (RuntimeException | Error e) {
            closeTask.accept(null);
            throw e;
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
        try (Batch batch = this.manager.getBatchFactory().get()) {
            Session<Map<String, Object>> session = this.getDetachedSession.apply(sessionId);
            return (session != null) ? new DetachedDistributableSession(this, session, this.statistics) : null;
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
