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

package org.wildfly.clustering.server.registry;

import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

import org.jboss.as.clustering.controller.CapabilityServiceConfigurator;
import org.jboss.as.clustering.function.Consumers;
import org.jboss.as.controller.capability.CapabilityServiceSupport;
import org.jboss.msc.Service;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceTarget;
import org.wildfly.clustering.registry.Registry;
import org.wildfly.clustering.registry.RegistryFactory;
import org.wildfly.clustering.service.AsyncServiceConfigurator;
import org.wildfly.clustering.service.CompositeDependency;
import org.wildfly.clustering.service.FunctionalService;
import org.wildfly.clustering.service.ServiceConfigurator;
import org.wildfly.clustering.service.ServiceSupplierDependency;
import org.wildfly.clustering.service.SimpleServiceNameProvider;
import org.wildfly.clustering.service.SupplierDependency;
import org.wildfly.clustering.spi.ClusteringCacheRequirement;

/**
 * Builds a {@link Registry} service.
 * @author Paul Ferraro
 */
public class RegistryServiceConfigurator<K, V> extends SimpleServiceNameProvider implements CapabilityServiceConfigurator, Supplier<Registry<K, V>> {

    private final String containerName;
    private final String cacheName;

    private volatile SupplierDependency<RegistryFactory<K, V>> factory;
    private volatile SupplierDependency<Map.Entry<K, V>> entry;

    public RegistryServiceConfigurator(ServiceName name, String containerName, String cacheName) {
        super(name);
        this.containerName = containerName;
        this.cacheName = cacheName;
    }

    @Override
    public Registry<K, V> get() {
        return this.factory.get().createRegistry(this.entry.get());
    }

    @Override
    public ServiceConfigurator configure(CapabilityServiceSupport support) {
        this.factory = new ServiceSupplierDependency<>(ClusteringCacheRequirement.REGISTRY_FACTORY.getServiceName(support, this.containerName, this.cacheName));
        this.entry = new ServiceSupplierDependency<>(ClusteringCacheRequirement.REGISTRY_ENTRY.getServiceName(support, this.containerName, this.cacheName));
        return this;
    }

    @Override
    public ServiceBuilder<?> build(ServiceTarget target) {
        ServiceBuilder<?> builder = new AsyncServiceConfigurator(this.getServiceName()).build(target);
        Consumer<Registry<K, V>> registry = new CompositeDependency(this.factory, this.entry).register(builder).provides(this.getServiceName());
        Service service = new FunctionalService<>(registry, Function.identity(), this, Consumers.close());
        return builder.setInstance(service).setInitialMode(ServiceController.Mode.ON_DEMAND);
    }
}
