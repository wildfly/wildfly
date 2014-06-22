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

import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.jboss.as.clustering.infinispan.distribution.Locality;
import org.wildfly.clustering.ejb.Batch;
import org.wildfly.clustering.ejb.Batcher;
import org.wildfly.clustering.ejb.Time;
import org.wildfly.clustering.ejb.infinispan.logging.InfinispanEjbLogger;

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
    final Map<I, Future<?>> expirationFutures = Collections.synchronizedMap(new HashMap<I, Future<?>>());
    final Batcher batcher;
    final BeanRemover<I, T> remover;
    final ExpirationConfiguration<T> expiration;

    public BeanExpirationScheduler(Batcher batcher, BeanRemover<I, T> remover, ExpirationConfiguration<T> expiration) {
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
            ExpirationTask task = new ExpirationTask(id);
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
        synchronized (this.expirationFutures) {
            Iterator<Map.Entry<I, Future<?>>> entries = this.expirationFutures.entrySet().iterator();
            while (entries.hasNext()) {
                Map.Entry<I, Future<?>> entry = entries.next();
                I id = entry.getKey();
                if (!locality.isLocal(id)) {
                    entry.getValue().cancel(false);
                    entries.remove();
                }
            }
        }
    }

    @Override
    public void close() {
        for (Future<?> future: this.expirationFutures.values()) {
            future.cancel(false);
        }
        for (Future<?> future: this.expirationFutures.values()) {
            if (!future.isCancelled() && !future.isDone()) {
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

        public ExpirationTask(I id) {
            this.id = id;
        }

        @Override
        public void run() {
            synchronized (this) {
                BeanExpirationScheduler.this.expirationFutures.remove(this.id);
            }
            InfinispanEjbLogger.ROOT_LOGGER.tracef("Expiring stateful session bean %s", this.id);
            Batch batch = BeanExpirationScheduler.this.batcher.startBatch();
            boolean success = false;
            try {
                BeanExpirationScheduler.this.remover.remove(this.id, BeanExpirationScheduler.this.expiration.getRemoveListener());
                success = true;
            } finally {
                if (success) {
                    batch.close();
                } else {
                    batch.discard();
                }
            }
        }
    }
}
