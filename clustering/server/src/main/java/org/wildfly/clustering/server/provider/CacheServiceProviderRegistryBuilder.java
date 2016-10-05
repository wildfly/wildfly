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

import org.infinispan.Cache;
import org.jboss.as.controller.capability.CapabilityServiceSupport;
import org.jboss.msc.service.Service;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceTarget;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StopContext;
import org.jboss.msc.value.InjectedValue;
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
import org.wildfly.clustering.spi.CacheGroupServiceName;
import org.wildfly.clustering.spi.GroupServiceName;

/**
 * Builds a clustered {@link ServiceProviderRegistrationFactory} service.
 * @author Paul Ferraro
 */
public class CacheServiceProviderRegistryBuilder<T> extends ServiceProviderRegistryServiceNameProvider implements Builder<ServiceProviderRegistry<T>>, Service<ServiceProviderRegistry<T>>, CacheServiceProviderRegistryConfiguration<T> {

    private final InjectedValue<CommandDispatcherFactory> dispatcherFactory = new InjectedValue<>();
    private final InjectedValue<Group> group = new InjectedValue<>();
    @SuppressWarnings("rawtypes")
    private final InjectedValue<Cache> cache = new InjectedValue<>();
    private final CapabilityServiceSupport support;

    private volatile CacheServiceProviderRegistry<T> factory = null;

    /**
     * @param containerName
     * @param cacheName
     */
    public CacheServiceProviderRegistryBuilder(CapabilityServiceSupport support, String containerName, String cacheName) {
        super(containerName, cacheName);
        this.support = support;
    }

    @Override
    public ServiceBuilder<ServiceProviderRegistry<T>> build(ServiceTarget target) {
        return new AsynchronousServiceBuilder<>(this.getServiceName(), this).build(target)
                .addDependency(InfinispanCacheRequirement.CACHE.getServiceName(this.support, this.containerName, this.cacheName), Cache.class, this.cache)
                .addDependency(CacheGroupServiceName.GROUP.getServiceName(this.containerName, this.cacheName), Group.class, this.group)
                .addDependency(GroupServiceName.COMMAND_DISPATCHER.getServiceName(this.containerName), CommandDispatcherFactory.class, this.dispatcherFactory)
                .setInitialMode(ServiceController.Mode.ON_DEMAND);
    }

    @Override
    public ServiceProviderRegistry<T> getValue() {
        return new ServiceProviderRegistrationFactoryAdapter<>(this.factory);
    }

    @Override
    public void start(StartContext context) {
        this.factory = new CacheServiceProviderRegistry<>(this);
    }

    @Override
    public void stop(StopContext context) {
        this.factory.close();
        this.factory = null;
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
