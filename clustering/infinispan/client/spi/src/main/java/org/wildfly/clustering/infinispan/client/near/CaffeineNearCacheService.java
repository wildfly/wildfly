/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.infinispan.client.near;

import java.util.function.BiConsumer;
import java.util.function.Supplier;

import org.infinispan.client.hotrod.MetadataValue;
import org.infinispan.client.hotrod.configuration.NearCacheConfiguration;
import org.infinispan.client.hotrod.configuration.NearCacheMode;
import org.infinispan.client.hotrod.event.impl.ClientListenerNotifier;
import org.infinispan.client.hotrod.near.NearCache;
import org.infinispan.client.hotrod.near.NearCacheService;

import com.github.benmanes.caffeine.cache.Cache;

/**
 * Near cache service that constructs its near cache using a generic factory.
 * @author Paul Ferraro
 */
public class CaffeineNearCacheService<K, V> extends NearCacheService<K, V> {
    private final Supplier<Cache<K, MetadataValue<V>>> factory;

    public CaffeineNearCacheService(Supplier<Cache<K, MetadataValue<V>>> factory, ClientListenerNotifier listenerNotifier) {
        super(new NearCacheConfiguration(NearCacheMode.INVALIDATED, 0, false), listenerNotifier);
        this.factory = factory;
    }

    @Override
    protected NearCache<K, V> createNearCache(NearCacheConfiguration config, BiConsumer<K, MetadataValue<V>> removedConsumer) {
        return new CaffeineNearCache<>(this.factory.get());
    }
}
