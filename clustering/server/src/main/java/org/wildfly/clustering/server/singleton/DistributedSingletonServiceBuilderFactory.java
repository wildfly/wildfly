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
package org.wildfly.clustering.server.singleton;

import org.jboss.msc.service.Service;
import org.jboss.msc.service.ServiceName;
import org.wildfly.clustering.dispatcher.CommandDispatcherFactory;
import org.wildfly.clustering.provider.ServiceProviderRegistry;
import org.wildfly.clustering.service.InjectedValueDependency;
import org.wildfly.clustering.service.SupplierDependency;
import org.wildfly.clustering.singleton.SingletonServiceBuilder;
import org.wildfly.clustering.singleton.SingletonServiceBuilderFactory;

/**
 * Service for building {@link DistributedSingletonServiceBuilder} instances.
 * @author Paul Ferraro
 */
public class DistributedSingletonServiceBuilderFactory implements SingletonServiceBuilderFactory, DistributedSingletonServiceConfiguratorContext {

    private final DistributedSingletonServiceConfiguratorFactoryContext context;

    public DistributedSingletonServiceBuilderFactory(DistributedSingletonServiceConfiguratorFactoryContext context) {
        this.context = context;
    }

    @Override
    public <T> SingletonServiceBuilder<T> createSingletonServiceBuilder(ServiceName name, Service<T> service) {
        return this.createSingletonServiceBuilder(name, service, null);
    }

    @Override
    public <T> SingletonServiceBuilder<T> createSingletonServiceBuilder(ServiceName name, Service<T> primaryService, Service<T> backupService) {
        return new DistributedSingletonServiceBuilder<>(this, name, primaryService, backupService);
    }

    @Override
    public SupplierDependency<ServiceProviderRegistry<ServiceName>> getServiceProviderRegistryDependency() {
        return new InjectedValueDependency<>(this.context.getServiceProviderRegistryServiceName(), (Class<ServiceProviderRegistry<ServiceName>>) (Class<?>) ServiceProviderRegistry.class);
    }

    @Override
    public SupplierDependency<CommandDispatcherFactory> getCommandDispatcherFactoryDependency() {
        return new InjectedValueDependency<>(this.context.getCommandDispatcherFactoryServiceName(), CommandDispatcherFactory.class);
    }
}
