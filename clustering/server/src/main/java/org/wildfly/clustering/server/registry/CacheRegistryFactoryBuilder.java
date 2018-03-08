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
import java.util.function.BiFunction;

import org.infinispan.Cache;
import org.infinispan.remoting.transport.Address;
import org.jboss.as.clustering.controller.CapabilityServiceBuilder;
import org.jboss.as.controller.capability.CapabilityServiceSupport;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceTarget;
import org.jboss.msc.service.ValueService;
import org.jboss.msc.value.Value;
import org.wildfly.clustering.ee.Batch;
import org.wildfly.clustering.ee.Batcher;
import org.wildfly.clustering.ee.infinispan.InfinispanBatcher;
import org.wildfly.clustering.group.Node;
import org.wildfly.clustering.infinispan.spi.InfinispanCacheRequirement;
import org.wildfly.clustering.registry.Registry;
import org.wildfly.clustering.registry.RegistryFactory;
import org.wildfly.clustering.server.group.Group;
import org.wildfly.clustering.service.Builder;
import org.wildfly.clustering.service.CompositeDependency;
import org.wildfly.clustering.service.InjectedValueDependency;
import org.wildfly.clustering.service.ValueDependency;
import org.wildfly.clustering.spi.ClusteringCacheRequirement;

/**
 * Builds a clustered {@link RegistryFactory}.
 * @author Paul Ferraro
 */
public class CacheRegistryFactoryBuilder<K, V> implements CapabilityServiceBuilder<RegistryFactory<K, V>>, CacheRegistryConfiguration<K, V>, Value<RegistryFactory<K, V>>, BiFunction<Map.Entry<K, V>, Runnable, Registry<K, V>> {

    private final ServiceName name;
    private final String containerName;
    private final String cacheName;

    private volatile ValueDependency<Group<Address>> group;
    private volatile ValueDependency<Cache<Node, Map.Entry<K, V>>> cache;

    public CacheRegistryFactoryBuilder(ServiceName name, String containerName, String cacheName) {
        this.name = name;
        this.containerName = containerName;
        this.cacheName = cacheName;
    }

    @Override
    public RegistryFactory<K, V> getValue() {
        return new FunctionalRegistryFactory<>(this);
    }

    @Override
    public Registry<K, V> apply(Map.Entry<K, V> entry, Runnable closeTask) {
        return new CacheRegistry<>(this, entry, closeTask);
    }

    @Override
    public ServiceName getServiceName() {
        return this.name;
    }

    @SuppressWarnings("unchecked")
    @Override
    public Builder<RegistryFactory<K, V>> configure(CapabilityServiceSupport support) {
        this.cache = new InjectedValueDependency<>(InfinispanCacheRequirement.CACHE.getServiceName(support, this.containerName, this.cacheName), (Class<Cache<Node, Map.Entry<K, V>>>) (Class<?>) Cache.class);
        this.group = new InjectedValueDependency<>(ClusteringCacheRequirement.GROUP.getServiceName(support, this.containerName, this.cacheName), (Class<Group<Address>>) (Class<?>) Group.class);
        return this;
    }

    @Override
    public ServiceBuilder<RegistryFactory<K, V>> build(ServiceTarget target) {
        ServiceBuilder<RegistryFactory<K, V>> builder = target.addService(this.name, new ValueService<>(this)).setInitialMode(ServiceController.Mode.ON_DEMAND);
        return new CompositeDependency(this.cache, this.group).register(builder);
    }

    @Override
    public Batcher<? extends Batch> getBatcher() {
        return new InfinispanBatcher(this.getCache());
    }

    @Override
    public Group<Address> getGroup() {
        return this.group.getValue();
    }

    @Override
    public Cache<Node, Map.Entry<K, V>> getCache() {
        return this.cache.getValue();
    }
}
