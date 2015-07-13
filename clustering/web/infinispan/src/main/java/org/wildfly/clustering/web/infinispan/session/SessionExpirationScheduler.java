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
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

import org.jboss.threads.JBossThreadFactory;
import org.wildfly.clustering.ee.Batch;
import org.wildfly.clustering.ee.Batcher;
import org.wildfly.clustering.ee.infinispan.Remover;
import org.wildfly.clustering.ee.infinispan.TransactionBatch;
import org.wildfly.clustering.infinispan.spi.distribution.Locality;
import org.wildfly.clustering.web.infinispan.logging.InfinispanWebLogger;
import org.wildfly.clustering.web.session.ImmutableSession;

/**
 * Session expiration scheduler that eagerly expires sessions as soon as they are eligible.
 * If/When Infinispan implements expiration notifications (ISPN-694), this will be obsolete.
 * @author Paul Ferraro
 */
public class SessionExpirationScheduler implements Scheduler {

    final Map<String, Future<?>> expirationFutures = new ConcurrentHashMap<>();
    final Batcher<TransactionBatch> batcher;
    final Remover<String> remover;
    private final ScheduledExecutorService executor;

    public SessionExpirationScheduler(Batcher<TransactionBatch> batcher, Remover<String> remover) {
        this(batcher, remover, createScheduledExecutor(createThreadFactory()));
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

    public SessionExpirationScheduler(Batcher<TransactionBatch> batcher, Remover<String> remover, ScheduledExecutorService executor) {
        this.batcher = batcher;
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
    public void schedule(ImmutableSession session) {
        long timeout = session.getMetaData().getMaxInactiveInterval(TimeUnit.MILLISECONDS);
        if (timeout > 0) {
            long lastAccessed = session.getMetaData().getLastAccessedTime().getTime();
            long delay = Math.max(lastAccessed + timeout - System.currentTimeMillis(), 0);
            String id = session.getId();
            Runnable task = new ExpirationTask(id);
            InfinispanWebLogger.ROOT_LOGGER.tracef("Session %s will expire in %d ms", id, timeout);
            synchronized (task) {
                this.expirationFutures.put(id, this.executor.schedule(task, delay, TimeUnit.MILLISECONDS));
            }
        }
    }

    @Override
    public void cancel(Locality locality) {
        for (String sessionId: this.expirationFutures.keySet()) {
            if (!locality.isLocal(sessionId)) {
                this.cancel(sessionId);
            }
        }
    }

    @Override
    public void close() {
        this.executor.shutdown();
        for (Future<?> future: this.expirationFutures.values()) {
            future.cancel(false);
        }
        for (Future<?> future: this.expirationFutures.values()) {
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

        public ExpirationTask(String id) {
            this.id = id;
        }

        @Override
        public void run() {
            InfinispanWebLogger.ROOT_LOGGER.tracef("Expiring session %s", this.id);
            try {
                Batch batch = SessionExpirationScheduler.this.batcher.createBatch();
                boolean success = false;
                try {
                    SessionExpirationScheduler.this.remover.remove(this.id);
                    success = true;
                } catch (Throwable e) {
                    InfinispanWebLogger.ROOT_LOGGER.failedToExpireSession(e, this.id);
                } finally {
                    if (success) {
                        batch.close();
                    } else {
                        batch.discard();
                    }
                }
            } finally {
                synchronized (this) {
                    SessionExpirationScheduler.this.expirationFutures.remove(this.id);
                }
            }
        }
    }
}
