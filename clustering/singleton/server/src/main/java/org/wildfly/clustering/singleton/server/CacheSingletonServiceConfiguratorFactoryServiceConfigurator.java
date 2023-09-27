/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.singleton.server;

import java.util.function.Consumer;

import org.jboss.as.clustering.controller.CapabilityServiceConfigurator;
import org.jboss.as.controller.capability.CapabilityServiceSupport;
import org.jboss.msc.Service;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceTarget;
import org.wildfly.clustering.server.service.ClusteringCacheRequirement;
import org.wildfly.clustering.server.service.ClusteringRequirement;
import org.wildfly.clustering.service.ServiceConfigurator;
import org.wildfly.clustering.service.SimpleServiceNameProvider;
import org.wildfly.clustering.singleton.service.SingletonServiceConfiguratorFactory;

/**
 * Configures a service providing a distributed {@link org.wildfly.clustering.singleton.service.SingletonServiceConfiguratorFactory}.
 * @author Paul Ferraro
 */
public class CacheSingletonServiceConfiguratorFactoryServiceConfigurator extends SimpleServiceNameProvider implements CapabilityServiceConfigurator, DistributedSingletonServiceConfiguratorFactoryContext {

    private final String containerName;
    private final String cacheName;

    private volatile ServiceName registryServiceName;
    private volatile ServiceName dispatcherFactoryServiceName;

    public CacheSingletonServiceConfiguratorFactoryServiceConfigurator(ServiceName name, String containerName, String cacheName) {
        super(name);
        this.containerName = containerName;
        this.cacheName = cacheName;
    }

    @Override
    public ServiceConfigurator configure(CapabilityServiceSupport support) {
        this.registryServiceName = ClusteringCacheRequirement.SERVICE_PROVIDER_REGISTRY.getServiceName(support, this.containerName, this.cacheName);
        this.dispatcherFactoryServiceName = ClusteringRequirement.COMMAND_DISPATCHER_FACTORY.getServiceName(support, this.containerName);
        return this;
    }

    @Override
    public ServiceBuilder<?> build(ServiceTarget target) {
        ServiceBuilder<?> builder = target.addService(this.getServiceName());
        Consumer<SingletonServiceConfiguratorFactory> factory = builder.provides(this.getServiceName());
        @SuppressWarnings("deprecation")
        Service service = Service.newInstance(factory, new DistributedSingletonServiceBuilderFactory(this));
        return builder.setInstance(service);
    }

    @Override
    public ServiceName getServiceProviderRegistryServiceName() {
        return this.registryServiceName;
    }

    @Override
    public ServiceName getCommandDispatcherFactoryServiceName() {
        return this.dispatcherFactoryServiceName;
    }
}
