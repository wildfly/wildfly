/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2011, Red Hat, Inc., and individual contributors
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
package org.wildfly.clustering.infinispan.spi.service;

import java.util.function.Function;
import java.util.function.Supplier;

import org.infinispan.Cache;
import org.infinispan.manager.EmbeddedCacheManager;
import org.jboss.msc.service.Service;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceTarget;
import org.jboss.msc.value.InjectedValue;
import org.wildfly.clustering.service.AsynchronousServiceBuilder;
import org.wildfly.clustering.service.Builder;
import org.wildfly.clustering.service.SuppliedValueService;

/**
 * Service that provides a cache and handles its lifecycle
 * @author Paul Ferraro
 * @param <K> the cache key type
 * @param <V> the cache value type
 */
public class CacheBuilder<K, V> implements Builder<Cache<K, V>> {

    private final InjectedValue<EmbeddedCacheManager> container = new InjectedValue<>();
    private final String containerName;
    private final String cacheName;

    public CacheBuilder(String containerName, String cacheName) {
        this.containerName = containerName;
        this.cacheName = cacheName;
    }

    @Override
    public ServiceName getServiceName() {
        return CacheServiceName.CACHE.getServiceName(this.containerName, this.cacheName);
    }

    @Override
    public ServiceBuilder<Cache<K, V>> build(ServiceTarget target) {
        Supplier<Cache<K, V>> supplier = () -> {
            Cache<K, V> cache = this.container.getValue().getCache(this.cacheName);
            cache.start();
            return cache;
        };
        Service<Cache<K, V>> service = new SuppliedValueService<>(Function.identity(), supplier, Cache::stop);
        return new AsynchronousServiceBuilder<>(this.getServiceName(), service).build(target)
                .addDependency(CacheContainerServiceName.CACHE_CONTAINER.getServiceName(this.containerName), EmbeddedCacheManager.class, this.container)
                .addDependency(CacheServiceName.CONFIGURATION.getServiceName(this.containerName, this.cacheName))
                .setInitialMode(ServiceController.Mode.ON_DEMAND);
    }
}