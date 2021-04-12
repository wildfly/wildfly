/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2021, Red Hat, Inc., and individual contributors
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

package org.wildfly.clustering.infinispan.spi.listener;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.Executor;
import java.util.concurrent.RejectedExecutionException;
import java.util.function.BiConsumer;

import org.infinispan.notifications.Listener;
import org.infinispan.notifications.cachelistener.annotation.CacheEntryPassivated;
import org.infinispan.notifications.cachelistener.event.CacheEntryPassivatedEvent;
import org.infinispan.util.concurrent.CompletableFutures;

/**
 * Generic non-blocking passivation listener that consumes a passivation event.
 * @author Paul Ferraro
 */
@Listener(observation = Listener.Observation.PRE)
public class PrePassivateListener<K, V> {
    private final BiConsumer<K, V> consumer;
    private final Executor executor;

    public PrePassivateListener(BiConsumer<K, V> consumer, Executor executor) {
        this.consumer = consumer;
        this.executor = executor;
    }

    @CacheEntryPassivated
    public CompletionStage<Void> passivated(CacheEntryPassivatedEvent<K, V> event) {
        if (event.isPre()) {
            try {
                return CompletableFuture.runAsync(() -> this.consumer.accept(event.getKey(), event.getValue()), this.executor);
            } catch (RejectedExecutionException e) {
                // Executor was shutdown
            }
        }
        return CompletableFutures.completedNull();
    }
}
