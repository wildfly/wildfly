/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.clustering.singleton.server;

import org.jboss.as.clustering.msc.InjectedValueDependency;
import org.jboss.msc.service.Service;
import org.jboss.msc.service.ServiceName;
import org.wildfly.clustering.provider.ServiceProviderRegistry;
import org.wildfly.clustering.server.dispatcher.CommandDispatcherFactory;
import org.wildfly.clustering.service.SupplierDependency;
import org.wildfly.clustering.singleton.SingletonServiceBuilder;
import org.wildfly.clustering.singleton.SingletonServiceBuilderFactory;

/**
 * Factory for creating distributed {@link SingletonServiceBuilder} instances.
 * @author Paul Ferraro
 */
@Deprecated
public class DistributedSingletonServiceBuilderFactory extends DistributedSingletonServiceConfiguratorFactory implements SingletonServiceBuilderFactory {

    private final DistributedSingletonServiceConfiguratorContext context;

    public DistributedSingletonServiceBuilderFactory(DistributedSingletonServiceConfiguratorFactoryContext context) {
        super(context);
        this.context = new LegacyDistributedSingletonServiceConfiguratorContext(context);
    }

    @Override
    public <T> SingletonServiceBuilder<T> createSingletonServiceBuilder(ServiceName name, Service<T> service) {
        return this.createSingletonServiceBuilder(name, service, null);
    }

    @Override
    public <T> SingletonServiceBuilder<T> createSingletonServiceBuilder(ServiceName name, Service<T> primaryService, Service<T> backupService) {
        return new DistributedSingletonServiceBuilder<>(name, primaryService, backupService, this.context);
    }

    private class LegacyDistributedSingletonServiceConfiguratorContext implements DistributedSingletonServiceConfiguratorContext {
        private final DistributedSingletonServiceConfiguratorFactoryContext context;

        LegacyDistributedSingletonServiceConfiguratorContext(DistributedSingletonServiceConfiguratorFactoryContext context) {
            this.context = context;
        }

        @SuppressWarnings("unchecked")
        @Override
        public SupplierDependency<ServiceProviderRegistry<ServiceName>> getServiceProviderRegistryDependency() {
            return new InjectedValueDependency<>(this.context.getServiceProviderRegistryServiceName(), (Class<ServiceProviderRegistry<ServiceName>>) (Class<?>) ServiceProviderRegistry.class);
        }

        @Override
        public SupplierDependency<CommandDispatcherFactory> getCommandDispatcherFactoryDependency() {
            return new InjectedValueDependency<>(this.context.getCommandDispatcherFactoryServiceName(), CommandDispatcherFactory.class);
        }
    }
}
