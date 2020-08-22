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

package org.wildfly.clustering.infinispan.client.manager;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

import javax.transaction.TransactionManager;

import org.infinispan.client.hotrod.RemoteCache;
import org.infinispan.client.hotrod.configuration.Configuration;
import org.infinispan.client.hotrod.configuration.NearCacheConfiguration;
import org.infinispan.client.hotrod.configuration.TransactionMode;
import org.infinispan.client.hotrod.event.impl.ClientListenerNotifier;
import org.infinispan.client.hotrod.near.NearCacheService;
import org.wildfly.clustering.Registrar;
import org.wildfly.clustering.infinispan.client.RegisteredRemoteCache;
import org.wildfly.clustering.infinispan.client.RemoteCacheContainer;

/**
 * Extends Infinispan's {@link org.infinispan.client.hotrod.RemoteCacheManager}
 * to provide a name and mechanism for overriding near cache behavior per remote cache.
 * @author Paul Ferraro
 */
public class RemoteCacheManager extends org.infinispan.client.hotrod.RemoteCacheManager implements RemoteCacheContainer {

    private final Map<String, Function<ClientListenerNotifier, NearCacheService<?, ?>>> nearCacheFactories = new ConcurrentHashMap<>();
    private final String name;
    private final Registrar<String> registrar;

    public RemoteCacheManager(String name, Configuration configuration, Registrar<String> registrar) {
        super(configuration, false);
        this.name = name;
        this.registrar = registrar;
    }

    @Override
    public String getName() {
        return this.name;
    }

    @Override
    public <K, V> RemoteCache<K, V> getCache(String cacheName, TransactionMode transactionMode, TransactionManager transactionManager) {
        return this.getCache(cacheName, this.getConfiguration().forceReturnValues(), transactionMode, transactionManager);
    }

    @Override
    public <K, V> RemoteCache<K, V> getCache(String cacheName, boolean forceReturnValue, TransactionMode transactionMode, TransactionManager transactionManager) {
        return new RegisteredRemoteCache<>(this, super.getCache(cacheName, forceReturnValue, transactionMode, transactionManager), this.registrar);
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
    protected <K, V> NearCacheService<K, V> createNearCacheService(String cacheName, NearCacheConfiguration config) {
        @SuppressWarnings("unchecked")
        Function<ClientListenerNotifier, NearCacheService<K, V>> factory = (cacheName != null) ? (Function<ClientListenerNotifier, NearCacheService<K, V>>) (Function<?, ?>) this.nearCacheFactories.get(cacheName) : null;
        return (factory != null) ? factory.apply(this.listenerNotifier) : super.createNearCacheService(cacheName, config);
    }
}
