/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.extension.clustering.server.registry;

import java.util.Map;

import org.infinispan.Cache;
import org.infinispan.remoting.transport.Address;
import org.jboss.as.controller.capability.CapabilityServiceSupport;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceName;
import org.wildfly.clustering.infinispan.service.InfinispanCacheRequirement;
import org.wildfly.clustering.registry.Registry;
import org.wildfly.clustering.registry.RegistryFactory;
import org.wildfly.clustering.server.group.Group;
import org.wildfly.clustering.server.infinispan.registry.CacheRegistry;
import org.wildfly.clustering.server.infinispan.registry.CacheRegistryConfiguration;
import org.wildfly.clustering.server.infinispan.registry.LocalRegistry;
import org.wildfly.clustering.server.service.ClusteringCacheRequirement;
import org.wildfly.clustering.service.CompositeDependency;
import org.wildfly.clustering.service.ServiceConfigurator;
import org.wildfly.clustering.service.ServiceSupplierDependency;
import org.wildfly.clustering.service.SupplierDependency;

/**
 * Builds a clustered {@link RegistryFactory}.
 * @author Paul Ferraro
 */
public class CacheRegistryFactoryServiceConfigurator<K, V> extends FunctionalRegistryFactoryServiceConfigurator<K, V> implements CacheRegistryConfiguration<K, V> {

    private final String containerName;
    private final String cacheName;

    private volatile SupplierDependency<Group<Address>> group;
    private volatile SupplierDependency<Cache<?, ?>> cache;

    public CacheRegistryFactoryServiceConfigurator(ServiceName name, String containerName, String cacheName) {
        super(name);
        this.containerName = containerName;
        this.cacheName = cacheName;
    }

    @Override
    public Registry<K, V> apply(Map.Entry<K, V> entry, Runnable closeTask) {
        Cache<?, ?> cache = this.cache.get();
        return cache.getCacheConfiguration().clustering().cacheMode().isClustered() ? new CacheRegistry<>(this, entry, closeTask) : new LocalRegistry<>(this.group.get(), entry, closeTask);
    }

    @Override
    public ServiceConfigurator configure(CapabilityServiceSupport support) {
        this.cache = new ServiceSupplierDependency<>(InfinispanCacheRequirement.CACHE.getServiceName(support, this.containerName, this.cacheName));
        this.group = new ServiceSupplierDependency<>(ClusteringCacheRequirement.GROUP.getServiceName(support, this.containerName, this.cacheName));
        return this;
    }

    @Override
    public <T> ServiceBuilder<T> register(ServiceBuilder<T> builder) {
        return new CompositeDependency(this.cache, this.group).register(builder);
    }

    @Override
    public Group<Address> getGroup() {
        return this.group.get();
    }

    @SuppressWarnings("unchecked")
    @Override
    public <KK, VV> Cache<KK, VV> getCache() {
        return (Cache<KK, VV>) this.cache.get();
    }
}
