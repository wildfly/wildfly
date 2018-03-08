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
package org.wildfly.clustering.server.provider;

import java.util.Set;
import java.util.function.Supplier;

import org.infinispan.Cache;
import org.jboss.as.clustering.controller.CapabilityServiceBuilder;
import org.jboss.as.clustering.function.Consumers;
import org.jboss.as.clustering.function.Functions;
import org.jboss.as.controller.capability.CapabilityServiceSupport;
import org.jboss.msc.service.Service;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceTarget;
import org.wildfly.clustering.dispatcher.CommandDispatcherFactory;
import org.wildfly.clustering.ee.Batch;
import org.wildfly.clustering.ee.Batcher;
import org.wildfly.clustering.ee.infinispan.InfinispanBatcher;
import org.wildfly.clustering.group.Group;
import org.wildfly.clustering.group.Node;
import org.wildfly.clustering.infinispan.spi.InfinispanCacheRequirement;
import org.wildfly.clustering.provider.ServiceProviderRegistry;
import org.wildfly.clustering.service.AsynchronousServiceBuilder;
import org.wildfly.clustering.service.Builder;
import org.wildfly.clustering.service.CompositeDependency;
import org.wildfly.clustering.service.InjectedValueDependency;
import org.wildfly.clustering.service.SuppliedValueService;
import org.wildfly.clustering.service.ValueDependency;
import org.wildfly.clustering.spi.ClusteringCacheRequirement;
import org.wildfly.clustering.spi.ClusteringRequirement;

/**
 * Builds a clustered {@link ServiceProviderRegistrationFactory} service.
 * @author Paul Ferraro
 */
public class CacheServiceProviderRegistryBuilder<T> implements CapabilityServiceBuilder<ServiceProviderRegistry<T>>, CacheServiceProviderRegistryConfiguration<T>, Supplier<CacheServiceProviderRegistry<T>> {

    private final ServiceName name;
    private final String containerName;
    private final String cacheName;

    private volatile ValueDependency<CommandDispatcherFactory> dispatcherFactory;
    private volatile ValueDependency<Group> group;
    private volatile ValueDependency<Cache<T, Set<Node>>> cache;

    public CacheServiceProviderRegistryBuilder(ServiceName name, String containerName, String cacheName) {
        this.name = name;
        this.containerName = containerName;
        this.cacheName = cacheName;
    }

    @Override
    public CacheServiceProviderRegistry<T> get() {
        return new CacheServiceProviderRegistry<>(this);
    }

    @Override
    public ServiceName getServiceName() {
        return this.name;
    }

    @SuppressWarnings("unchecked")
    @Override
    public Builder<ServiceProviderRegistry<T>> configure(CapabilityServiceSupport support) {
        this.cache = new InjectedValueDependency<>(InfinispanCacheRequirement.CACHE.getServiceName(support, this.containerName, this.cacheName), (Class<Cache<T, Set<Node>>>) (Class<?>) Cache.class);
        this.dispatcherFactory = new InjectedValueDependency<>(ClusteringRequirement.COMMAND_DISPATCHER_FACTORY.getServiceName(support, this.containerName), CommandDispatcherFactory.class);
        this.group = new InjectedValueDependency<>(ClusteringCacheRequirement.GROUP.getServiceName(support, this.containerName, this.cacheName), Group.class);
        return this;
    }

    @Override
    public ServiceBuilder<ServiceProviderRegistry<T>> build(ServiceTarget target) {
        Service<ServiceProviderRegistry<T>> service = new SuppliedValueService<>(Functions.identity(), this, Consumers.close());
        ServiceBuilder<ServiceProviderRegistry<T>> builder = new AsynchronousServiceBuilder<>(this.name, service).build(target).setInitialMode(ServiceController.Mode.ON_DEMAND);
        return new CompositeDependency(this.cache, this.dispatcherFactory, this.group).register(builder);
    }

    @Override
    public Object getId() {
        return this.getServiceName();
    }

    @Override
    public Group getGroup() {
        return this.group.getValue();
    }

    @Override
    public Cache<T, Set<Node>> getCache() {
        return this.cache.getValue();
    }

    @Override
    public CommandDispatcherFactory getCommandDispatcherFactory() {
        return this.dispatcherFactory.getValue();
    }

    @Override
    public Batcher<? extends Batch> getBatcher() {
        return new InfinispanBatcher(this.getCache());
    }
}
