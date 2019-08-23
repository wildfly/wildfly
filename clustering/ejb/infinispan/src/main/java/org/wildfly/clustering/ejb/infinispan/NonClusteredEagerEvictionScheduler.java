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
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

import org.wildfly.clustering.ejb.infinispan.logging.InfinispanEjbLogger;
import org.wildfly.clustering.infinispan.spi.distribution.Locality;

/**
 * Scheduler for eager eviction of a bean.
 * @author Paul Ferraro
 * @author Flavia Rainone
 */
public class NonClusteredEagerEvictionScheduler<I, T> implements Scheduler<I>, BeanGroupEvictor<I>, Consumer<I> {

    private final BeanFactory<I, T> factory;
    private final ScheduledExecutorService executor;
    private final Duration idleTimeout;
    private final ExpirationTracker<I> expirationTracker;
    private final Map<I, I> groupIdMap = new HashMap<>();
    private volatile Future<?> evictionTask;

    private final BeanGroupEvictor<I> evictor;

    NonClusteredEagerEvictionScheduler(BeanFactory<I, T> factory, BeanGroupEvictor<I> evictor, ScheduledExecutorService executor, Duration idleTimeout) {
        this.factory = factory;
        this.executor = executor;
        this.idleTimeout = idleTimeout;
        this.expirationTracker = new ExpirationTracker<>(idleTimeout);
        this.evictor = evictor;
    }

    @Override
    public void accept(I id) {
        this.expirationTracker.forget(id);
    }

    @Override
    public void schedule(I id) {
        BeanEntry<I> entry = this.factory.findValue(id);
        if (entry != null) {
            InfinispanEjbLogger.ROOT_LOGGER.tracef("Scheduling stateful session bean %s to passivate in %s", id, this.idleTimeout);
            expirationTracker.trackExpiration(id);
            if (!groupIdMap.containsKey(id)) {
                groupIdMap.put(id, entry.getGroupId());
            }
            // Make sure the map insertion happens before map removal (during task execution).
            if (evictionTask == null)
            synchronized (this) {
                if (evictionTask == null) {
                    Runnable task = new EvictionTask();
                    evictionTask = this.executor.schedule(task, this.idleTimeout.toMillis(), TimeUnit.MILLISECONDS);
                }
            }
        }
    }

    @Override
    public void schedule(I id, ImmutableBeanEntry<I> entry) {
        schedule(id);
    }

    @Override
    public void prepareRescheduling(I id) {
        expirationTracker.invalidateExpiration(id);
    }

    @Override
    public void cancel(I id) {
        expirationTracker.forget(id);
        groupIdMap.remove(id);
    }

    @Override
    public void cancel(Locality locality) {
        for (I id: this.expirationTracker.getTrackedIds()) {
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
            if (this.evictionTask == null) {
                return;
            }
            task = evictionTask;

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

    @Override
    public void evict(I id) {
        InfinispanEjbLogger.ROOT_LOGGER.tracef("Passivating stateful session bean %s", id);
        this.evictor.evict(id);
    }

    private class EvictionTask implements Runnable {

        @Override
        public void run() {
            I sessionId;
            final long currentTime = System.currentTimeMillis();
            while ((sessionId = expirationTracker.getExpiredId(currentTime)) != null) {
                InfinispanEjbLogger.ROOT_LOGGER.tracef("Evicting stateful session bean %s", sessionId);
                I groupId = groupIdMap.remove(sessionId);
                try {
                    evict(groupId);
                } finally {
                    synchronized (NonClusteredEagerEvictionScheduler.this) {
                        accept(sessionId);
                    }
                }
            }
            final long nextExpirationInMillis = expirationTracker.getNextExpirationInMillis();
            synchronized (NonClusteredEagerEvictionScheduler.this) {
                if (nextExpirationInMillis != -1) {
                    evictionTask = executor.schedule(this, nextExpirationInMillis - System.currentTimeMillis(), TimeUnit.MILLISECONDS);
                } else {
                    evictionTask = null;
                }
            }

        }
    }
}
