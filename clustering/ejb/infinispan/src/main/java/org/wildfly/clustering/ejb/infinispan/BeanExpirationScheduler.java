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
package org.wildfly.clustering.ejb.infinispan;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.wildfly.clustering.ee.Batch;
import org.wildfly.clustering.ee.Batcher;
import org.wildfly.clustering.ee.infinispan.TransactionBatch;
import org.wildfly.clustering.ejb.Time;
import org.wildfly.clustering.ejb.infinispan.logging.InfinispanEjbLogger;
import org.wildfly.clustering.infinispan.spi.distribution.Locality;

/**
 * Schedules a bean for expiration.
 *
 * @author Paul Ferraro
 *
 * @param <G> the group identifier type
 * @param <I> the bean identifier type
 * @param <T> the bean type
 */
public class BeanExpirationScheduler<G, I, T> implements Scheduler<I> {
    final Map<I, Future<?>> expirationFutures = new ConcurrentHashMap<>();
    final Batcher<TransactionBatch> batcher;
    final BeanRemover<I, T> remover;
    final ExpirationConfiguration<T> expiration;

    public BeanExpirationScheduler(Batcher<TransactionBatch> batcher, BeanRemover<I, T> remover, ExpirationConfiguration<T> expiration) {
        this.batcher = batcher;
        this.remover = remover;
        this.expiration = expiration;
    }

    @Override
    public void schedule(I id) {
        Time timeout = this.expiration.getTimeout();
        long value = timeout.getValue();
        if (value >= 0) {
            TimeUnit unit = timeout.getUnit();
            InfinispanEjbLogger.ROOT_LOGGER.tracef("Scheduling stateful session bean %s to expire in %d %s", id, value, unit);
            Runnable task = new ExpirationTask(id);
            // Make sure the expiration future map insertion happens before map removal (during task execution).
            synchronized (task) {
                this.expirationFutures.put(id, this.expiration.getExecutor().schedule(task, value, unit));
            }
        }
    }

    @Override
    public void cancel(I id) {
        Future<?> future = this.expirationFutures.remove(id);
        if (future != null) {
            future.cancel(false);
        }
    }

    @Override
    public void cancel(Locality locality) {
        for (I id: this.expirationFutures.keySet()) {
            if (Thread.currentThread().isInterrupted()) break;
            if (!locality.isLocal(id)) {
                this.cancel(id);
            }
        }
    }

    @Override
    public void close() {
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
        private final I id;

        ExpirationTask(I id) {
            this.id = id;
        }

        @Override
        public void run() {
            InfinispanEjbLogger.ROOT_LOGGER.tracef("Expiring stateful session bean %s", this.id);
            try (Batch batch = BeanExpirationScheduler.this.batcher.createBatch()) {
                try {
                    BeanExpirationScheduler.this.remover.remove(this.id, BeanExpirationScheduler.this.expiration.getRemoveListener());
                } catch (Throwable e) {
                    InfinispanEjbLogger.ROOT_LOGGER.failedToExpireBean(e, this.id);
                    batch.discard();
                }
            } finally {
                synchronized (this) {
                    BeanExpirationScheduler.this.expirationFutures.remove(this.id);
                }
            }
        }
    }
}
