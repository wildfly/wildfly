/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.clustering.infinispan.service;

import java.util.List;
import java.util.function.Function;
import java.util.function.Supplier;

import org.infinispan.AdvancedCache;
import org.infinispan.Cache;
import org.infinispan.cache.impl.AbstractDelegatingAdvancedCache;
import org.infinispan.manager.EmbeddedCacheManager;
import org.wildfly.clustering.server.service.BinaryServiceConfiguration;
import org.wildfly.subsystem.service.ServiceDependency;
import org.wildfly.subsystem.service.ServiceInstaller;

/**
 * Service that provides a cache and handles its lifecycle
 * @author Paul Ferraro
 * @param <K> the cache key type
 * @param <V> the cache value type
 */
public enum CacheServiceInstallerFactory implements Function<BinaryServiceConfiguration, ServiceInstaller> {
    INSTANCE;

    @SuppressWarnings("unchecked")
    @Override
    public ServiceInstaller apply(BinaryServiceConfiguration configuration) {
        ServiceDependency<EmbeddedCacheManager> container = configuration.getServiceDependency(InfinispanServiceDescriptor.CACHE_CONTAINER);
        String cacheName = configuration.getChildName();
        Supplier<Cache<?, ?>> cache = new Supplier<>() {
            @Override
            public Cache<?, ?> get() {
                return container.get().getCache(cacheName);
            }
        };
        return ServiceInstaller.builder(ManagedCache::new, cache).blocking()
                .provides(configuration.resolveServiceName(InfinispanServiceDescriptor.CACHE))
                .requires(List.of(container, configuration.getServiceDependency(InfinispanServiceDescriptor.CACHE_CONFIGURATION)))
                .onStart(Cache::start)
                .onStop(Cache::stop)
                .build();
    }

    private static class ManagedCache<K, V> extends AbstractDelegatingAdvancedCache<K, V> {

        ManagedCache(Cache<K, V> cache) {
            this(cache.getAdvancedCache());
        }

        private ManagedCache(AdvancedCache<K, V> cache) {
            super(cache.getAdvancedCache());
        }

        @SuppressWarnings({ "unchecked" })
        @Override
        public AdvancedCache rewrap(AdvancedCache delegate) {
            return new ManagedCache<>(delegate);
        }

        @Override
        public void start() {
            // No-op.  Lifecycle managed by container.
        }

        @Override
        public void stop() {
            // No-op.  Lifecycle managed by container.
        }
    }
}