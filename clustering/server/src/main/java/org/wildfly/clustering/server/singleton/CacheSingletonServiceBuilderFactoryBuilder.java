/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2014, Red Hat, Inc., and individual contributors
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

package org.wildfly.clustering.server.singleton;

import java.util.function.Supplier;

import org.jboss.as.clustering.controller.CapabilityServiceBuilder;
import org.jboss.as.controller.capability.CapabilityServiceSupport;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceTarget;
import org.jboss.msc.service.ValueService;
import org.jboss.msc.value.Value;
import org.wildfly.clustering.dispatcher.CommandDispatcherFactory;
import org.wildfly.clustering.provider.ServiceProviderRegistry;
import org.wildfly.clustering.service.Builder;
import org.wildfly.clustering.service.InjectedValueDependency;
import org.wildfly.clustering.service.ValueDependency;
import org.wildfly.clustering.singleton.SingletonServiceBuilderFactory;
import org.wildfly.clustering.spi.ClusteringCacheRequirement;
import org.wildfly.clustering.spi.ClusteringRequirement;

/**
 * Builds a clustered {@link SingletonServiceBuilderFactory}.
 * @author Paul Ferraro
 */
public class CacheSingletonServiceBuilderFactoryBuilder implements CapabilityServiceBuilder<SingletonServiceBuilderFactory>, DistributedSingletonServiceBuilderContext, Value<SingletonServiceBuilderFactory> {

    private final ServiceName name;
    private final String containerName;
    private final String cacheName;

    private volatile Supplier<ValueDependency<ServiceProviderRegistry<ServiceName>>> registry;
    private volatile Supplier<ValueDependency<CommandDispatcherFactory>> dispatcherFactory;

    public CacheSingletonServiceBuilderFactoryBuilder(ServiceName name, String containerName, String cacheName) {
        this.name = name;
        this.containerName = containerName;
        this.cacheName = cacheName;
    }

    @Override
    public SingletonServiceBuilderFactory getValue() {
        return new DistributedSingletonServiceBuilderFactory(this);
    }

    @Override
    public ServiceName getServiceName() {
        return this.name;
    }

    @SuppressWarnings("unchecked")
    @Override
    public Builder<SingletonServiceBuilderFactory> configure(CapabilityServiceSupport support) {
        this.registry = () -> new InjectedValueDependency<>(ClusteringCacheRequirement.SERVICE_PROVIDER_REGISTRY.getServiceName(support, this.containerName, this.cacheName), (Class<ServiceProviderRegistry<ServiceName>>) (Class<?>) ServiceProviderRegistry.class);
        this.dispatcherFactory = () -> new InjectedValueDependency<>(ClusteringRequirement.COMMAND_DISPATCHER_FACTORY.getServiceName(support, this.containerName), CommandDispatcherFactory.class);
        return this;
    }

    @Override
    public ServiceBuilder<SingletonServiceBuilderFactory> build(ServiceTarget target) {
        return target.addService(this.name, new ValueService<>(this));
    }

    @Override
    public ValueDependency<ServiceProviderRegistry<ServiceName>> getServiceProviderRegistryDependency() {
        return this.registry.get();
    }

    @Override
    public ValueDependency<CommandDispatcherFactory> getCommandDispatcherFactoryDependency() {
        return this.dispatcherFactory.get();
    }
}
