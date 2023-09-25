/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.infinispan.listener;

import java.util.concurrent.CompletionStage;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import org.infinispan.Cache;
import org.infinispan.notifications.cachelistener.event.CacheEntryEvent;
import org.infinispan.util.concurrent.BlockingManager;

/**
 * Generic non-blocking event listener that delegates to a blocking event consumer.
 * @author Paul Ferraro
 */
public class BlockingCacheEventListener<K, V> extends NonBlockingCacheEventListener<K, V> {

    private final BlockingManager blocking;
    private final String name;

    public BlockingCacheEventListener(Cache<K, V> cache, Consumer<K> consumer) {
        this(cache, (key, value) -> consumer.accept(key), consumer.getClass());
    }

    public BlockingCacheEventListener(Cache<K, V> cache, BiConsumer<K, V> consumer) {
        this(cache, consumer, consumer.getClass());
    }

    @SuppressWarnings("deprecation")
    private BlockingCacheEventListener(Cache<K, V> cache, BiConsumer<K, V> consumer, Class<?> consumerClass) {
        super(consumer);
        this.blocking = cache.getCacheManager().getGlobalComponentRegistry().getComponent(BlockingManager.class);
        this.name = consumerClass.getName();
    }

    @Override
    public CompletionStage<Void> apply(CacheEntryEvent<K, V> event) {
        return this.blocking.runBlocking(() -> super.accept(event), this.name);
    }

    @Override
    public void accept(CacheEntryEvent<K, V> event) {
        this.blocking.asExecutor(this.name).execute(() -> super.accept(event));
    }
}
