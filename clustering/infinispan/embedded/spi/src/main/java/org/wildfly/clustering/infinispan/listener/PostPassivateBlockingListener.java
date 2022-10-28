/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2022, Red Hat, Inc., and individual contributors
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
import java.util.function.Consumer;

import org.infinispan.Cache;
import org.infinispan.commons.util.concurrent.CompletableFutures;
import org.infinispan.notifications.Listener;
import org.infinispan.notifications.cachelistener.annotation.CacheEntryPassivated;
import org.infinispan.notifications.cachelistener.event.CacheEntryEvent;
import org.infinispan.notifications.cachelistener.event.CacheEntryPassivatedEvent;

/**
 * Generic non-blocking post-passivation listener that delegates to a blocking consumer.
 * @author Paul Ferraro
 */
@Listener(observation = Listener.Observation.POST)
public class PostPassivateBlockingListener<K, V> extends CacheEventListenerRegistrar<K, V> {

    private Consumer<CacheEntryEvent<K, V>> listener;

    public PostPassivateBlockingListener(Cache<K, V> cache, Consumer<K> listener) {
        super(cache);
        this.listener = new BlockingCacheEventListener<>(cache, listener);
    }

    @CacheEntryPassivated
    public CompletionStage<Void> postPassivate(CacheEntryPassivatedEvent<K, V> event) {
        this.listener.accept(event);
        return CompletableFutures.completedNull();
    }
}
