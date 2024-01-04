/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.extension.clustering.server.provider;

import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

import org.jboss.as.clustering.controller.CapabilityServiceConfigurator;
import org.jboss.as.controller.capability.CapabilityServiceSupport;
import org.jboss.msc.Service;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceTarget;
import org.wildfly.clustering.group.Group;
import org.wildfly.clustering.provider.ServiceProviderRegistry;
import org.wildfly.clustering.server.infinispan.provider.LocalServiceProviderRegistry;
import org.wildfly.clustering.server.service.ClusteringCacheRequirement;
import org.wildfly.clustering.service.FunctionalService;
import org.wildfly.clustering.service.ServiceConfigurator;
import org.wildfly.clustering.service.ServiceSupplierDependency;
import org.wildfly.clustering.service.SimpleServiceNameProvider;
import org.wildfly.clustering.service.SupplierDependency;

/**
 * Builds a non-clustered {@link ServiceProviderRegistrationFactory} service.
 * @author Paul Ferraro
 */
public class LocalServiceProviderRegistryServiceConfigurator<T> extends SimpleServiceNameProvider implements CapabilityServiceConfigurator, Supplier<ServiceProviderRegistry<T>> {

    private final String containerName;
    private final String cacheName;

    private volatile SupplierDependency<Group> group;

    public LocalServiceProviderRegistryServiceConfigurator(ServiceName name, String containerName, String cacheName) {
        super(name);
        this.containerName = containerName;
        this.cacheName = cacheName;
    }

    @Override
    public ServiceProviderRegistry<T> get() {
        return new LocalServiceProviderRegistry<>(this.group.get());
    }

    @Override
    public ServiceConfigurator configure(CapabilityServiceSupport support) {
        this.group = new ServiceSupplierDependency<>(ClusteringCacheRequirement.GROUP.getServiceName(support, this.containerName, this.cacheName));
        return this;
    }

    @Override
    public ServiceBuilder<?> build(ServiceTarget target) {
        ServiceBuilder<?> builder = target.addService(this.getServiceName());
        Consumer<ServiceProviderRegistry<T>> registry = this.group.register(builder).provides(this.getServiceName());
        Service service = new FunctionalService<>(registry, Function.identity(), this);
        return builder.setInstance(service).setInitialMode(ServiceController.Mode.ON_DEMAND);
    }
}
