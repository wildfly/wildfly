/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2013, Red Hat, Inc., and individual contributors
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
package org.wildfly.clustering.server.registry;

import java.util.Map;

import org.infinispan.Cache;
import org.infinispan.remoting.transport.Address;
import org.jboss.as.controller.capability.CapabilityServiceSupport;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceName;
import org.wildfly.clustering.ee.Batch;
import org.wildfly.clustering.ee.Batcher;
import org.wildfly.clustering.ee.infinispan.InfinispanBatcher;
import org.wildfly.clustering.infinispan.spi.InfinispanCacheRequirement;
import org.wildfly.clustering.registry.Registry;
import org.wildfly.clustering.registry.RegistryFactory;
import org.wildfly.clustering.server.group.Group;
import org.wildfly.clustering.service.CompositeDependency;
import org.wildfly.clustering.service.ServiceConfigurator;
import org.wildfly.clustering.service.ServiceSupplierDependency;
import org.wildfly.clustering.service.SupplierDependency;
import org.wildfly.clustering.spi.ClusteringCacheRequirement;

/**
 * Builds a clustered {@link RegistryFactory}.
 * @author Paul Ferraro
 */
public class CacheRegistryFactoryServiceConfigurator<K, V> extends FunctionalRegistryFactoryServiceConfigurator<K, V> implements CacheRegistryConfiguration<K, V> {

    private final String containerName;
    private final String cacheName;

    private volatile SupplierDependency<Group<Address>> group;
    private volatile SupplierDependency<Cache<Address, Map.Entry<K, V>>> cache;

    public CacheRegistryFactoryServiceConfigurator(ServiceName name, String containerName, String cacheName) {
        super(name);
        this.containerName = containerName;
        this.cacheName = cacheName;
    }

    @Override
    public Registry<K, V> apply(Map.Entry<K, V> entry, Runnable closeTask) {
        return new CacheRegistry<>(this, entry, closeTask);
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
    public Batcher<? extends Batch> getBatcher() {
        return new InfinispanBatcher(this.getCache());
    }

    @Override
    public Group<Address> getGroup() {
        return this.group.get();
    }

    @Override
    public Cache<Address, Map.Entry<K, V>> getCache() {
        return this.cache.get();
    }
}
