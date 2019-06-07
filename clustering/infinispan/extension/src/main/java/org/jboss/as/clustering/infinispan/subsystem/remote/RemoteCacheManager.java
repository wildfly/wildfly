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

package org.jboss.as.clustering.infinispan.subsystem.remote;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

import org.infinispan.client.hotrod.RemoteCache;
import org.infinispan.client.hotrod.configuration.Configuration;
import org.infinispan.client.hotrod.configuration.NearCacheConfiguration;
import org.infinispan.client.hotrod.event.impl.ClientListenerNotifier;
import org.infinispan.client.hotrod.near.NearCacheService;
import org.wildfly.clustering.infinispan.client.RemoteCacheContainer;

/**
 * Extends Infinispan's {@link org.infinispan.client.hotrod.RemoteCacheManager}
 * to provide a name and mechanism for overriding near cache behavior per remote cache.
 * @author Paul Ferraro
 */
public class RemoteCacheManager extends org.infinispan.client.hotrod.RemoteCacheManager implements RemoteCacheContainer {
    // Workaround for ISPN-10248, used to capture cache name for use by createNearCacheService(...)
    private static final ThreadLocal<String> CURRENT_CACHE_NAME = new ThreadLocal<>();

    private final Map<String, Function<ClientListenerNotifier, NearCacheService<?, ?>>> nearCacheFactories = new ConcurrentHashMap<>();
    private final String name;

    public RemoteCacheManager(String name, Configuration configuration) {
        super(configuration, false);
        this.name = name;
    }

    @Override
    public String getName() {
        return this.name;
    }

    @Override
    public <K, V> RemoteCache<K, V> getCache(String cacheName) {
        // Workaround for ISPN-10248, capture cache name for use by createNearCacheService(...)
        CURRENT_CACHE_NAME.set(cacheName);
        try {
            return super.getCache(cacheName);
        } finally {
            CURRENT_CACHE_NAME.remove();
        }
    }

    @SuppressWarnings("unchecked")
    @Override
    public <K, V> NearCacheRegistration registerNearCacheFactory(String cacheName, Function<ClientListenerNotifier, NearCacheService<K, V>> factory) {
        Map<String, Function<ClientListenerNotifier, NearCacheService<?, ?>>> factories = this.nearCacheFactories;
        factories.put(cacheName, (Function<ClientListenerNotifier, NearCacheService<?, ?>>) (Function<?, ?>) factory);
        return new NearCacheRegistration() {
            @Override
            public void close() {
                factories.remove(cacheName);
            }
        };
    }

    @Override
    protected <K, V> NearCacheService<K, V> createNearCacheService(NearCacheConfiguration config) {
        String cacheName = CURRENT_CACHE_NAME.get();
        @SuppressWarnings("unchecked")
        Function<ClientListenerNotifier, NearCacheService<K, V>> factory = (cacheName != null) ? (Function<ClientListenerNotifier, NearCacheService<K, V>>) (Function<?, ?>) this.nearCacheFactories.get(cacheName) : null;
        return (factory != null) ? factory.apply(this.listenerNotifier) : super.createNearCacheService(config);
    }
}
