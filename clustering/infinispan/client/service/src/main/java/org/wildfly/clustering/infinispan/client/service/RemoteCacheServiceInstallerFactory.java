/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.infinispan.client.service;

import java.util.List;
import java.util.function.Function;
import java.util.function.Supplier;

import org.infinispan.client.hotrod.RemoteCache;
import org.wildfly.clustering.infinispan.client.RemoteCacheContainer;
import org.wildfly.clustering.server.service.BinaryServiceConfiguration;
import org.wildfly.subsystem.service.ServiceDependency;
import org.wildfly.subsystem.service.ServiceInstaller;

/**
 * Installs a service providing a dynamically created remote-cache with a custom near-cache factory with auto-managed lifecycle.
 * @author Paul Ferraro
 * @param K cache key
 * @param V cache value
 */
public enum RemoteCacheServiceInstallerFactory implements Function<BinaryServiceConfiguration, ServiceInstaller> {
    INSTANCE;

    @Override
    public ServiceInstaller apply(BinaryServiceConfiguration configuration) {
        ServiceDependency<RemoteCacheContainer> container = configuration.getServiceDependency(HotRodServiceDescriptor.REMOTE_CACHE_CONTAINER);
        String cacheName = configuration.getChildName();
        Supplier<RemoteCache<?, ?>> cache = new Supplier<>() {
            @Override
            public RemoteCache<?, ?> get() {
                return container.get().getCache(cacheName);
            }
        };
        return ServiceInstaller.builder(cache).blocking()
                .onStart(RemoteCache::start)
                .onStop(RemoteCache::stop)
                .provides(configuration.resolveServiceName(HotRodServiceDescriptor.REMOTE_CACHE))
                .requires(List.of(container, configuration.getServiceDependency(HotRodServiceDescriptor.REMOTE_CACHE_CONFIGURATION)))
                .build();
    }
}
