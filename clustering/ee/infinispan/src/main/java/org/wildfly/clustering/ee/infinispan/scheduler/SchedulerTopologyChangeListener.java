/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2020, Red Hat, Inc., and individual contributors
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

package org.wildfly.clustering.ee.infinispan.scheduler;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import org.infinispan.Cache;
import org.infinispan.distribution.ch.ConsistentHash;
import org.infinispan.notifications.Listener;
import org.infinispan.notifications.cachelistener.annotation.DataRehashed;
import org.infinispan.notifications.cachelistener.annotation.TopologyChanged;
import org.infinispan.notifications.cachelistener.event.DataRehashedEvent;
import org.infinispan.notifications.cachelistener.event.TopologyChangedEvent;
import org.jboss.as.clustering.context.DefaultExecutorService;
import org.jboss.as.clustering.context.DefaultThreadFactory;
import org.wildfly.clustering.ee.cache.Key;
import org.wildfly.clustering.infinispan.spi.distribution.ConsistentHashLocality;
import org.wildfly.clustering.infinispan.spi.distribution.Locality;
import org.wildfly.security.manager.WildFlySecurityManager;

/**
 * @author Paul Ferraro
 */
@Listener
public class SchedulerTopologyChangeListener<I, K extends Key<I>, V> implements SchedulerListener {

    private final Cache<K, V> cache;
    private final ExecutorService executor = Executors.newSingleThreadExecutor(new DefaultThreadFactory(SchedulerTopologyChangeListener.class));
    private final AtomicReference<Future<?>> rehashFuture = new AtomicReference<>();
    private final AtomicInteger rehashTopology = new AtomicInteger();
    private final Consumer<Locality> cancelTask;
    private final BiConsumer<Locality, Locality> scheduleTask;

    public SchedulerTopologyChangeListener(Cache<K, V> cache, Scheduler<I, ?> scheduler, BiConsumer<Locality, Locality> scheduleTask) {
        this(cache, scheduler::cancel, scheduleTask);
    }

    public SchedulerTopologyChangeListener(Cache<K, V> cache, Consumer<Locality> cancelTask, BiConsumer<Locality, Locality> scheduleTask) {
        this.cache = cache;
        this.cancelTask = cancelTask;
        this.scheduleTask = scheduleTask;
        this.cache.addListener(this);
    }

    @Override
    public void close() {
        this.cache.removeListener(this);
        WildFlySecurityManager.doUnchecked(this.executor, DefaultExecutorService.SHUTDOWN_NOW_ACTION);
        try {
            this.executor.awaitTermination(this.cache.getCacheConfiguration().transaction().cacheStopTimeout(), TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    @DataRehashed
    public void dataRehashed(DataRehashedEvent<K, V> event) {
        try {
            if (event.isPre()) {
                this.rehashTopology.set(event.getNewTopologyId());
                this.cancel(event.getCache(), event.getConsistentHashAtEnd());
            } else {
                this.rehashTopology.compareAndSet(event.getNewTopologyId(), 0);
                this.schedule(event.getCache(), event.getConsistentHashAtStart(), event.getConsistentHashAtEnd());
            }
        } catch (RejectedExecutionException e) {
            // Executor was shutdown
        }
    }

    @TopologyChanged
    public void topologyChanged(TopologyChangedEvent<K, V> event) {
        if (!event.isPre()) {
            // If this topology change has no corresponding rehash event, we must reschedule expirations as primary ownership may have changed
            if (this.rehashTopology.get() != event.getNewTopologyId()) {
                this.schedule(event.getCache(), event.getReadConsistentHashAtStart(), event.getWriteConsistentHashAtEnd());
            }
        }
    }

    private void cancel(Cache<K, V> cache, ConsistentHash hash) {
        Future<?> future = this.rehashFuture.getAndSet(null);
        if (future != null) {
            future.cancel(true);
        }
        this.executor.submit(() -> this.cancelTask.accept(new ConsistentHashLocality(cache, hash)));
    }

    private void schedule(Cache<K, V> cache, ConsistentHash oldHash, ConsistentHash newHash) {
        // Skip rescheduling if we do not own any segments
        if (!newHash.getPrimarySegmentsForOwner(cache.getCacheManager().getAddress()).isEmpty()) {
            Future<?> future = this.rehashFuture.getAndSet(null);
            if (future != null) {
                future.cancel(true);
            }
            // For non-transactional invalidation-caches, where all keys hash to a single member, always schedule
            Locality oldLocality = new ConsistentHashLocality(cache, oldHash);
            Locality newLocality = new ConsistentHashLocality(cache, newHash);
            try {
                this.rehashFuture.compareAndSet(null, this.executor.submit(() -> this.scheduleTask.accept(oldLocality, newLocality)));
            } catch (RejectedExecutionException e) {
                // Executor was shutdown
            }
        }
    }
}
