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

import java.security.AccessController;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

import org.jboss.as.clustering.concurrent.Scheduler;
import org.jboss.threads.JBossThreadFactory;
import org.wildfly.clustering.ejb.Batch;
import org.wildfly.clustering.ejb.Batcher;
import org.wildfly.clustering.ejb.Bean;
import org.wildfly.clustering.ejb.Time;
import org.wildfly.security.manager.GetAccessControlContextAction;

/**
 * Schedules a bean for expiration.
 *
 * @author Paul Ferraro
 *
 * @param <G> the group identifier type
 * @param <I> the bean identifier type
 * @param <T> the bean type
 */
public class BeanExpirationScheduler<G, I, T> implements Scheduler<Bean<G, I, T>> {
    final Map<I, Future<?>> expirationFutures = new ConcurrentHashMap<>();
    final Batcher batcher;
    final BeanRemover<I, T> remover;
    final ExpirationConfiguration<T> expiration;
    private final ScheduledExecutorService executor;

    public BeanExpirationScheduler(Batcher batcher, BeanRemover<I, T> remover, ExpirationConfiguration<T> expiration) {
        this(batcher, remover, expiration, Executors.newSingleThreadScheduledExecutor(createThreadFactory()));
    }

    private static ThreadFactory createThreadFactory() {
        return new JBossThreadFactory(new ThreadGroup(BeanExpirationScheduler.class.getSimpleName()), Boolean.FALSE, null, "%G - %t", null, null, AccessController.doPrivileged(GetAccessControlContextAction.getInstance()));
    }

    public BeanExpirationScheduler(Batcher batcher, BeanRemover<I, T> remover, ExpirationConfiguration<T> expiration, ScheduledExecutorService executor) {
        this.batcher = batcher;
        this.remover = remover;
        this.expiration = expiration;
        this.executor = executor;
    }

    @Override
    public void schedule(Bean<G, I, T> bean) {
        Time timeout = this.expiration.getTimeout();
        long value = timeout.getValue();
        if (value > 0) {
            TimeUnit unit = timeout.getUnit();
            I id = bean.getId();
            InfinispanEjbLogger.ROOT_LOGGER.tracef("Scheduling stateful session bean %s to expire in %d %s", id, value, unit);
            this.expirationFutures.put(id, this.executor.schedule(new ExpirationTask(id), value, unit));
        }
    }

    @Override
    public void cancel(Bean<G, I, T> group) {
        Future<?> future = this.expirationFutures.remove(group.getId());
        if (future != null) {
            future.cancel(false);
        }
    }

    @Override
    public void close() {
        this.executor.shutdown();
    }

    private class ExpirationTask implements Runnable {
        private final I id;

        public ExpirationTask(I id) {
            this.id = id;
        }

        @Override
        public void run() {
            BeanExpirationScheduler.this.expirationFutures.remove(this.id);
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
