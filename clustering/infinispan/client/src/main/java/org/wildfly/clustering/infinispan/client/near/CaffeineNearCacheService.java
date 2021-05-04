/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2019, Red Hat, Inc., and individual contributors
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
 * Workaround for ISPN-10248, {@link NearCache} is package protected.
 * To be refactored into org.wildfly.clustering.infinispan.client.near package once Infinispan increases visibility of {@link NearCache}.
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
