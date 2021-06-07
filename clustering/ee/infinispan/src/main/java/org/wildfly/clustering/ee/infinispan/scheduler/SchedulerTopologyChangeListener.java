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

import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import org.infinispan.Cache;
import org.infinispan.distribution.ch.ConsistentHash;
import org.infinispan.notifications.Listener;
import org.infinispan.notifications.cachelistener.annotation.TopologyChanged;
import org.infinispan.notifications.cachelistener.event.TopologyChangedEvent;
import org.infinispan.remoting.transport.Address;
import org.infinispan.util.concurrent.CompletableFutures;
import org.jboss.as.clustering.context.DefaultExecutorService;
import org.jboss.as.clustering.context.DefaultThreadFactory;
import org.wildfly.clustering.ee.Key;
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
    private final AtomicReference<Future<?>> scheduleTaskFuture = new AtomicReference<>();
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

    @TopologyChanged
    public CompletionStage<Void> topologyChanged(TopologyChangedEvent<K, V> event) {
        Address address = event.getCache().getCacheManager().getAddress();
        ConsistentHash oldHash = event.getWriteConsistentHashAtStart();
        Set<Integer> oldSegments = oldHash.getPrimarySegmentsForOwner(address);
        ConsistentHash newHash = event.getWriteConsistentHashAtEnd();
        Set<Integer> newSegments = newHash.getPrimarySegmentsForOwner(address);
        if (event.isPre()) {
            // If there are segments that we no longer own, then run cancellation task
            if (!newSegments.containsAll(oldSegments)) {
                Future<?> future = this.scheduleTaskFuture.getAndSet(null);
                if (future != null) {
                    future.cancel(true);
                }
                return CompletableFuture.runAsync(() -> this.cancelTask.accept(new ConsistentHashLocality(event.getCache(), newHash)), this.executor);
            }
        } else {
            // If we have newly owned segments, then run schedule task
            if (!oldSegments.containsAll(newSegments)) {
                Locality oldLocality = new ConsistentHashLocality(event.getCache(), oldHash);
                Locality newLocality = new ConsistentHashLocality(event.getCache(), newHash);
                try {
                    Future<?> future = this.scheduleTaskFuture.getAndSet(this.executor.submit(() -> this.scheduleTask.accept(oldLocality, newLocality)));
                    if (future != null) {
                        future.cancel(true);
                    }
                } catch (RejectedExecutionException e) {
                    // Executor was shutdown
                }
            }
        }
        return CompletableFutures.completedNull();
    }
}
