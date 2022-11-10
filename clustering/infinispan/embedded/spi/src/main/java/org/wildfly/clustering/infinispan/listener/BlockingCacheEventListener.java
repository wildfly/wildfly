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
