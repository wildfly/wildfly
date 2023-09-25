/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.infinispan.listener;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;

import org.infinispan.notifications.cachelistener.event.CacheEntryEvent;

/**
 * Generic non-blocking event listener that delegates to a non-blocking event consumer.
 * @author Paul Ferraro
 */
public class NonBlockingCacheEventListener<K, V> implements Function<CacheEntryEvent<K, V>, CompletionStage<Void>>, Consumer<CacheEntryEvent<K, V>> {

    private final BiConsumer<K, V> consumer;

    public NonBlockingCacheEventListener(Consumer<K> consumer) {
        this((key, value) -> consumer.accept(key));
    }

    public NonBlockingCacheEventListener(BiConsumer<K, V> consumer) {
        this.consumer = consumer;
    }

    @Override
    public CompletionStage<Void> apply(CacheEntryEvent<K, V> event) {
        try {
            this.accept(event);
            return CompletableFuture.completedStage(null);
        } catch (RuntimeException | Error e) {
            return CompletableFuture.failedStage(e);
        }
    }

    @Override
    public void accept(CacheEntryEvent<K, V> event) {
        this.consumer.accept(event.getKey(), event.getValue());
    }
}
