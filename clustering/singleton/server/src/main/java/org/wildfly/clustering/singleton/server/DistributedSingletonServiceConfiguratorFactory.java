/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.singleton.server;

import org.jboss.msc.service.ServiceName;
import org.wildfly.clustering.dispatcher.CommandDispatcherFactory;
import org.wildfly.clustering.provider.ServiceProviderRegistry;
import org.wildfly.clustering.service.ServiceSupplierDependency;
import org.wildfly.clustering.service.SupplierDependency;
import org.wildfly.clustering.singleton.service.SingletonServiceConfigurator;
import org.wildfly.clustering.singleton.service.SingletonServiceConfiguratorFactory;

/**
 * Factory for creating distributed {@link SingletonServiceConfigurator} instances.
 * @author Paul Ferraro
 */
public class DistributedSingletonServiceConfiguratorFactory implements SingletonServiceConfiguratorFactory, DistributedSingletonServiceConfiguratorContext {

    private final DistributedSingletonServiceConfiguratorFactoryContext context;

    public DistributedSingletonServiceConfiguratorFactory(DistributedSingletonServiceConfiguratorFactoryContext context) {
        this.context = context;
    }

    @Override
    public SingletonServiceConfigurator createSingletonServiceConfigurator(ServiceName name) {
        return new DistributedSingletonServiceConfigurator(name, this);
    }

    @Override
    public SupplierDependency<ServiceProviderRegistry<ServiceName>> getServiceProviderRegistryDependency() {
        return new ServiceSupplierDependency<>(this.context.getServiceProviderRegistryServiceName());
    }

    @Override
    public SupplierDependency<CommandDispatcherFactory> getCommandDispatcherFactoryDependency() {
        return new ServiceSupplierDependency<>(this.context.getCommandDispatcherFactoryServiceName());
    }
}
