/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2019, Red Hat, Inc., and individual contributors
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
package org.wildfly.clustering.ejb.infinispan;

import java.time.Duration;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.wildfly.clustering.ee.Batch;
import org.wildfly.clustering.ee.Batcher;
import org.wildfly.clustering.ee.cache.tx.TransactionBatch;
import org.wildfly.clustering.ejb.infinispan.logging.InfinispanEjbLogger;
import org.wildfly.clustering.infinispan.spi.distribution.Locality;

/**
 * Schedules a bean for expiration.
 *
 * @author Paul Ferraro
 * @author Flavia Rainone
 *
 * @param <I> the bean identifier type
 * @param <T> the bean type
 */
public class NonClusteredBeanExpirationScheduler<I, T> implements Scheduler<I> {
    private final Batcher<TransactionBatch> batcher;
    private final BeanRemover<I, T> remover;
    private final ExpirationConfiguration<T> expiration;
    private final ExpirationTracker<I> expirationTracker;
    private volatile Future<?> expireTask;

    NonClusteredBeanExpirationScheduler(Batcher<TransactionBatch> batcher, BeanRemover<I, T> remover, ExpirationConfiguration<T> expiration) {
        this.batcher = batcher;
        this.remover = remover;
        this.expiration = expiration;
        this.expirationTracker = expiration.getTimeout().isNegative()? null : new ExpirationTracker<>(expiration.getTimeout());
    }

    @Override
    public void prepareRescheduling(I id) {
        if (expirationTracker != null) {
            expirationTracker.invalidateExpiration(id);
        }
    }

    @Override
    public void schedule(I id) {
        if (expirationTracker != null) {
            expirationTracker.trackExpiration(id);
            if (expireTask == null) {
                synchronized (this) {
                    if (expireTask == null) {
                        Runnable task = new ExpirationTask();
                        Duration timeout = this.expiration.getTimeout();
                        InfinispanEjbLogger.ROOT_LOGGER.tracef("Scheduling stateful session bean %s to expire in %s", id, timeout);
                        expireTask = this.expiration.getExecutor().schedule(task, timeout.toMillis(), TimeUnit.MILLISECONDS);
                    }
                }
            }
        }
    }

    @Override
    public void schedule(I id, ImmutableBeanEntry<I> entry) {
        schedule(id);
    }

    @Override
    public void cancel(I id) {
        this.expirationTracker.forget(id);
    }

    @Override
    public void cancel(Locality locality) {
        for (I id: expirationTracker.getTrackedIds()) {
            if (Thread.currentThread().isInterrupted()) break;
            if (!locality.isLocal(id)) {
                this.cancel(id);
            }
        }
    }

    @Override
    public void close() {
        final Future<?> task;
        synchronized (this) {
            if (expireTask == null) {
                return;
            }
            task = expireTask;
            expireTask = null;
        }
        task.cancel(false);
        if (!task.isDone()) {
            try {
                task.get();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } catch (ExecutionException e) {
                // Ignore
            }
        }
    }

    private class ExpirationTask implements Runnable {

        @Override
        public void run() {
            I sessionId = null;
            boolean removed = true;
            final long currentTime = System.currentTimeMillis();
            while (removed && (sessionId = expirationTracker.getExpiredId(currentTime)) != null) {
                InfinispanEjbLogger.ROOT_LOGGER.tracef("Expiring stateful session bean %s", sessionId);
                removed = false;
                try (Batch batch = NonClusteredBeanExpirationScheduler.this.batcher.createBatch()) {
                    try {
                        removed = NonClusteredBeanExpirationScheduler.this.remover.remove(sessionId, NonClusteredBeanExpirationScheduler.this.expiration.getRemoveListener());
                    } catch (Throwable e) {
                        InfinispanEjbLogger.ROOT_LOGGER.failedToExpireBean(e, sessionId);
                        batch.discard();
                    }
                }

            }
            if (!removed) {
                // if bean failed to expire, likely due to a lock timeout, just reschedule it
                expirationTracker.retryExpiration(sessionId);
            }
            final long nextExpirationInMillis = expirationTracker.getNextExpirationInMillis();
            synchronized (NonClusteredBeanExpirationScheduler.this) {
                // check for expireTask being not null as well, because if
                // expireTask is null, it means close() has been invoked
                if (nextExpirationInMillis != -1 && expireTask != null) {
                    expireTask = expiration.getExecutor().schedule(this, nextExpirationInMillis - System.currentTimeMillis(), TimeUnit.MILLISECONDS);
                } else {
                    expireTask = null;
                }
            }

        }
    }
}
