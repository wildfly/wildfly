/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.clustering.infinispan.service;

import java.util.List;
import java.util.function.Function;

import org.infinispan.AdvancedCache;
import org.infinispan.Cache;
import org.infinispan.cache.impl.AbstractDelegatingAdvancedCache;
import org.infinispan.manager.EmbeddedCacheManager;
import org.jboss.as.controller.RequirementServiceTarget;
import org.jboss.msc.service.ServiceController;
import org.wildfly.clustering.server.service.BinaryServiceConfiguration;
import org.wildfly.subsystem.service.ServiceDependency;
import org.wildfly.subsystem.service.ServiceInstaller;

/**
 * Service that provides a cache and handles its lifecycle
 * @author Paul Ferraro
 * @param <K> the cache key type
 * @param <V> the cache value type
 */
public class CacheServiceInstaller implements ServiceInstaller {

    private final BinaryServiceConfiguration configuration;

    public CacheServiceInstaller(BinaryServiceConfiguration configuration) {
        this.configuration = configuration;
    }

    @Override
    public ServiceController<?> install(RequirementServiceTarget target) {
        String cacheName = this.configuration.getChildName();
        ServiceDependency<Cache<?, ?>> cache = this.configuration.getServiceDependency(InfinispanServiceDescriptor.CACHE_CONTAINER).map(new Function<>() {
            @Override
            public Cache<?, ?> apply(EmbeddedCacheManager container) {
                return container.getCache(cacheName);
            }
        });
        return ServiceInstaller.builder(ManagedCache::new, cache).blocking()
                .provides(this.configuration.resolveServiceName(InfinispanServiceDescriptor.CACHE))
                .requires(List.of(cache, this.configuration.getServiceDependency(InfinispanServiceDescriptor.CACHE_CONFIGURATION)))
                .onStart(Cache::start)
                .onStop(Cache::stop)
                .build()
                .install(target);
    }

    private static class ManagedCache<K, V> extends AbstractDelegatingAdvancedCache<K, V> {

        ManagedCache(Cache<K, V> cache) {
            this(cache.getAdvancedCache());
        }

        private ManagedCache(AdvancedCache<K, V> cache) {
            super(cache);
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
