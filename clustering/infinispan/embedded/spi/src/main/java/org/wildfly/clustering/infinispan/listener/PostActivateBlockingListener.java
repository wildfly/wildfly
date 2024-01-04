/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.infinispan.listener;

import java.util.concurrent.CompletionStage;
import java.util.function.BiConsumer;
import java.util.function.Function;

import org.infinispan.Cache;
import org.infinispan.notifications.Listener;
import org.infinispan.notifications.cachelistener.annotation.CacheEntryActivated;
import org.infinispan.notifications.cachelistener.event.CacheEntryActivatedEvent;
import org.infinispan.notifications.cachelistener.event.CacheEntryEvent;

/**
 * Generic non-blocking post-activation listener that delegates to a blocking consumer.
 * @author Paul Ferraro
 */
@Listener(observation = Listener.Observation.POST)
public class PostActivateBlockingListener<K, V> extends CacheEventListenerRegistrar<K, V> {

    private final Function<CacheEntryEvent<K, V>, CompletionStage<Void>> listener;

    public PostActivateBlockingListener(Cache<K, V> cache, BiConsumer<K, V> consumer) {
        super(cache);
        this.listener = new BlockingCacheEventListener<>(cache, consumer);
    }

    @CacheEntryActivated
    public CompletionStage<Void> postActivate(CacheEntryActivatedEvent<K, V> event) {
        return this.listener.apply(event);
    }
}
