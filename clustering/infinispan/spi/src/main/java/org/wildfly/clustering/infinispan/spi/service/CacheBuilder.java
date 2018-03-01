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

import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

import org.infinispan.Cache;
import org.jboss.as.clustering.controller.CapabilityServiceBuilder;
import org.jboss.as.controller.capability.CapabilityServiceSupport;
import org.jboss.msc.service.Service;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceTarget;
import org.wildfly.clustering.infinispan.spi.CacheContainer;
import org.wildfly.clustering.infinispan.spi.InfinispanCacheRequirement;
import org.wildfly.clustering.infinispan.spi.InfinispanRequirement;
import org.wildfly.clustering.service.AsynchronousServiceBuilder;
import org.wildfly.clustering.service.Builder;
import org.wildfly.clustering.service.CompositeDependency;
import org.wildfly.clustering.service.Dependency;
import org.wildfly.clustering.service.InjectedValueDependency;
import org.wildfly.clustering.service.SimpleDependency;
import org.wildfly.clustering.service.SuppliedValueService;
import org.wildfly.clustering.service.ValueDependency;

/**
 * Service that provides a cache and handles its lifecycle
 * @author Paul Ferraro
 * @param <K> the cache key type
 * @param <V> the cache value type
 */
public class CacheBuilder<K, V> implements CapabilityServiceBuilder<Cache<K, V>>, Supplier<Cache<K, V>>, Consumer<Cache<K, V>> {

    private final ServiceName name;
    private final String containerName;
    private final String cacheName;

    private volatile ValueDependency<CacheContainer> container;
    private volatile Dependency configuration;

    public CacheBuilder(ServiceName name, String containerName, String cacheName) {
        this.name = name;
        this.containerName = containerName;
        this.cacheName = cacheName;
    }

    @Override
    public Cache<K, V> get() {
        Cache<K, V> cache = this.container.getValue().getCache(this.cacheName);
        cache.start();
        return cache;
    }

    @Override
    public void accept(Cache<K, V> cache) {
        cache.stop();
    }

    @Override
    public ServiceName getServiceName() {
        return this.name;
    }

    @Override
    public Builder<Cache<K, V>> configure(CapabilityServiceSupport support) {
        this.container = new InjectedValueDependency<>(InfinispanRequirement.CONTAINER.getServiceName(support, this.containerName), CacheContainer.class);
        this.configuration = new SimpleDependency(InfinispanCacheRequirement.CONFIGURATION.getServiceName(support, this.containerName, this.cacheName));
        return this;
    }

    @Override
    public ServiceBuilder<Cache<K, V>> build(ServiceTarget target) {
        Service<Cache<K, V>> service = new SuppliedValueService<>(Function.identity(), this, this);
        ServiceBuilder<Cache<K, V>> builder = new AsynchronousServiceBuilder<>(this.getServiceName(), service).build(target).setInitialMode(ServiceController.Mode.ON_DEMAND);
        return new CompositeDependency(this.configuration, this.container).register(builder);
    }
}