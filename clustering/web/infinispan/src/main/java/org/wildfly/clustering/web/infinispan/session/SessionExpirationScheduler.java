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
package org.wildfly.clustering.web.infinispan.session;

import java.security.AccessController;
import java.security.PrivilegedAction;
import java.time.Duration;
import java.time.Instant;
import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

import org.jboss.threads.JBossThreadFactory;
import org.wildfly.clustering.ee.Batch;
import org.wildfly.clustering.ee.Batcher;
import org.wildfly.clustering.ee.Remover;
import org.wildfly.clustering.ee.cache.tx.TransactionBatch;
import org.wildfly.clustering.infinispan.spi.distribution.Locality;
import org.wildfly.clustering.web.cache.session.ImmutableSessionMetaDataFactory;
import org.wildfly.clustering.web.infinispan.logging.InfinispanWebLogger;
import org.wildfly.clustering.web.session.ImmutableSessionMetaData;
import org.wildfly.clustering.web.session.SessionExpirationListener;

/**
 * Session expiration scheduler that eagerly expires sessions as soon as they are eligible.
 * If/When Infinispan implements expiration notifications (ISPN-694), this will be obsolete.
 * @author Paul Ferraro
 */
public class SessionExpirationScheduler<MV> implements Scheduler {

    final Collection<SessionExpirationListener> listeners = new CopyOnWriteArraySet<>();
    final Map<String, Future<?>> expirationFutures = new ConcurrentHashMap<>();
    final Batcher<TransactionBatch> batcher;
    final Remover<String> remover;
    private final ImmutableSessionMetaDataFactory<MV> metaDataFactory;
    private final ScheduledExecutorService executor;

    public SessionExpirationScheduler(Batcher<TransactionBatch> batcher, ImmutableSessionMetaDataFactory<MV> metaDataFactory, Remover<String> remover) {
        this(batcher, metaDataFactory, remover, createScheduledExecutor(createThreadFactory()));
    }

    private static ThreadFactory createThreadFactory() {
        return AccessController.doPrivileged(new PrivilegedAction<ThreadFactory>() {
            @Override
            public ThreadFactory run() {
                return new JBossThreadFactory(new ThreadGroup(SessionExpirationScheduler.class.getSimpleName()), Boolean.FALSE, null, "%G - %t", null, null);
            }
        });
    }

    private static ScheduledExecutorService createScheduledExecutor(ThreadFactory factory) {
        ScheduledThreadPoolExecutor executor = new ScheduledThreadPoolExecutor(1, factory);
        executor.setRemoveOnCancelPolicy(true);
        executor.setExecuteExistingDelayedTasksAfterShutdownPolicy(false);
        return executor;
    }

    public SessionExpirationScheduler(Batcher<TransactionBatch> batcher, ImmutableSessionMetaDataFactory<MV> metaDataFactory, Remover<String> remover, ScheduledExecutorService executor) {
        this.batcher = batcher;
        this.metaDataFactory = metaDataFactory;
        this.remover = remover;
        this.executor = executor;
    }

    @Override
    public void cancel(String sessionId) {
        Future<?> future = this.expirationFutures.remove(sessionId);
        if (future != null) {
            future.cancel(false);
        }
    }

    @Override
    public void schedule(String sessionId) {
        MV value = this.metaDataFactory.findValue(sessionId);
        if (value != null) {
            ImmutableSessionMetaData metaData = this.metaDataFactory.createImmutableSessionMetaData(sessionId, value);
            this.schedule(sessionId, metaData);
        }
    }

    @Override
    public void schedule(String sessionId, ImmutableSessionMetaData metaData) {
        Duration maxInactiveInterval = metaData.getMaxInactiveInterval();
        if (!maxInactiveInterval.isZero()) {
            Instant lastAccessed = metaData.getLastAccessedTime();
            Duration delay = Duration.between(Instant.now(), lastAccessed.plus(maxInactiveInterval));
            Runnable task = new ExpirationTask(sessionId);
            long millis = !delay.isNegative() ? delay.toMillis() : 0;
            InfinispanWebLogger.ROOT_LOGGER.tracef("Session %s will expire in %d ms", sessionId, millis);
            synchronized (task) {
                this.expirationFutures.put(sessionId, this.executor.schedule(task, millis, TimeUnit.MILLISECONDS));
            }
        }
    }

    @Override
    public void cancel(Locality locality) {
        for (String sessionId : this.expirationFutures.keySet()) {
            if (Thread.currentThread().isInterrupted()) break;
            if (!locality.isLocal(sessionId)) {
                this.cancel(sessionId);
            }
        }
    }

    @Override
    public void close() {
        this.executor.shutdown();
        for (Future<?> future : this.expirationFutures.values()) {
            future.cancel(true);
        }
        for (Future<?> future : this.expirationFutures.values()) {
            if (!future.isDone()) {
                try {
                    future.get();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } catch (ExecutionException e) {
                    // Ignore
                }
            }
        }
        this.expirationFutures.clear();
    }

    private class ExpirationTask implements Runnable {
        private final String id;

        ExpirationTask(String id) {
            this.id = id;
        }

        @Override
        public void run() {
            InfinispanWebLogger.ROOT_LOGGER.tracef("Expiring session %s", this.id);
            try (Batch batch = SessionExpirationScheduler.this.batcher.createBatch()) {
                try {
                    SessionExpirationScheduler.this.remover.remove(this.id);
                } catch (Throwable e) {
                    InfinispanWebLogger.ROOT_LOGGER.failedToExpireSession(e, this.id);
                    batch.discard();
                }
            } finally {
                synchronized (this) {
                    SessionExpirationScheduler.this.expirationFutures.remove(this.id);
                }
            }
        }
    }
}
