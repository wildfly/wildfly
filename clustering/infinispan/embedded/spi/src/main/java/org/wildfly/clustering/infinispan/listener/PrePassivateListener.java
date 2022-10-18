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

package org.wildfly.clustering.infinispan.listener;

import java.util.concurrent.CompletionStage;
import java.util.function.BiConsumer;
import java.util.function.Predicate;

import org.infinispan.Cache;
import org.infinispan.notifications.Listener;
import org.infinispan.notifications.cachelistener.annotation.CacheEntryPassivated;
import org.infinispan.notifications.cachelistener.event.CacheEntryPassivatedEvent;
import org.infinispan.notifications.cachelistener.filter.CacheEventFilter;
import org.infinispan.util.concurrent.BlockingManager;

/**
 * Generic non-blocking passivation listener that consumes a passivation event.
 * @author Paul Ferraro
 */
public class PrePassivateListener<K, V> extends LifecycleListenerRegistration {

    public PrePassivateListener(Cache<K, V> cache, BiConsumer<K, V> consumer) {
        super(new ListenerLifecycle<>(cache, new PrePassivate<>(cache, consumer)));
    }

    public PrePassivateListener(Cache<K, V> cache, BiConsumer<K, V> consumer, Predicate<? super K> keyPredicate) {
        super(new ListenerLifecycle<>(cache, new PrePassivate<>(cache, consumer), keyPredicate));
    }

    public PrePassivateListener(Cache<K, V> cache, BiConsumer<K, V> consumer, CacheEventFilter<? super K, ? super V> filter) {
        super(new ListenerLifecycle<>(cache, new PrePassivate<>(cache, consumer), filter));
    }

    @Listener(observation = Listener.Observation.PRE)
    private static class PrePassivate<K, V> {
        private final BlockingManager blocking;
        private final BiConsumer<K, V> consumer;

        @SuppressWarnings("deprecation")
        PrePassivate(Cache<K, V> cache, BiConsumer<K, V> consumer) {
            this.blocking = cache.getCacheManager().getGlobalComponentRegistry().getComponent(BlockingManager.class);
            this.consumer = consumer;
        }

        @CacheEntryPassivated
        public CompletionStage<Void> prePassivate(CacheEntryPassivatedEvent<K, V> event) {
            return this.blocking.runBlocking(() -> this.consumer.accept(event.getKey(), event.getValue()), event.getSource());
        }
    }
}
