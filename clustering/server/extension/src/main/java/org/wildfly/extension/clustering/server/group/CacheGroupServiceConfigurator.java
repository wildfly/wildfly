/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.extension.clustering.server.group;

import java.util.function.Consumer;
import java.util.function.Supplier;

import org.infinispan.Cache;
import org.jboss.as.clustering.controller.CapabilityServiceConfigurator;
import org.jboss.as.clustering.function.Consumers;
import org.jboss.as.clustering.function.Functions;
import org.jboss.as.controller.capability.CapabilityServiceSupport;
import org.jboss.as.server.ServerEnvironment;
import org.jboss.as.server.ServerEnvironmentService;
import org.jboss.msc.Service;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceTarget;
import org.jgroups.Address;
import org.wildfly.clustering.group.Group;
import org.wildfly.clustering.infinispan.service.InfinispanCacheRequirement;
import org.wildfly.clustering.server.NodeFactory;
import org.wildfly.clustering.server.infinispan.group.AutoCloseableGroup;
import org.wildfly.clustering.server.infinispan.group.CacheGroup;
import org.wildfly.clustering.server.infinispan.group.CacheGroupConfiguration;
import org.wildfly.clustering.server.infinispan.group.LocalGroup;
import org.wildfly.clustering.server.service.ClusteringRequirement;
import org.wildfly.clustering.service.AsyncServiceConfigurator;
import org.wildfly.clustering.service.CompositeDependency;
import org.wildfly.clustering.service.FunctionalService;
import org.wildfly.clustering.service.ServiceConfigurator;
import org.wildfly.clustering.service.ServiceSupplierDependency;
import org.wildfly.clustering.service.SimpleServiceNameProvider;
import org.wildfly.clustering.service.SupplierDependency;

/**
 * Builds a {@link Group} service for a cache.
 * @author Paul Ferraro
 */
public class CacheGroupServiceConfigurator extends SimpleServiceNameProvider implements CapabilityServiceConfigurator, CacheGroupConfiguration, Supplier<AutoCloseableGroup<?>> {

    private final String containerName;
    private final String cacheName;
    private final SupplierDependency<ServerEnvironment> environment;

    private volatile SupplierDependency<Cache<?, ?>> cache;
    private volatile SupplierDependency<NodeFactory<Address>> factory;

    public CacheGroupServiceConfigurator(ServiceName name, String containerName, String cacheName) {
        super(name);
        this.containerName = containerName;
        this.cacheName = cacheName;
        this.environment = new ServiceSupplierDependency<>(ServerEnvironmentService.SERVICE_NAME);
    }

    @Override
    public AutoCloseableGroup<?> get() {
        Cache<?, ?> cache = this.cache.get();
        return cache.getCacheConfiguration().clustering().cacheMode().isClustered() ? new CacheGroup(this) : new LocalGroup(this.environment.get().getNodeName(), this.containerName);
    }

    @Override
    public ServiceConfigurator configure(CapabilityServiceSupport support) {
        this.cache = new ServiceSupplierDependency<>(InfinispanCacheRequirement.CACHE.getServiceName(support, this.containerName, this.cacheName));
        this.factory = new ServiceSupplierDependency<>(ClusteringRequirement.GROUP.getServiceName(support, this.containerName));
        return this;
    }

    @Override
    public ServiceBuilder<?> build(ServiceTarget target) {
        ServiceName name = this.getServiceName();
        ServiceBuilder<?> builder = new AsyncServiceConfigurator(name).build(target);
        Consumer<Group> group = new CompositeDependency(this.cache, this.factory, this.environment).register(builder).provides(name);
        Service service = new FunctionalService<>(group, Functions.identity(), this, Consumers.close());
        return builder.setInstance(service).setInitialMode(ServiceController.Mode.ON_DEMAND);
    }

    @SuppressWarnings("unchecked")
    @Override
    public <K, V> Cache<K, V> getCache() {
        return (Cache<K, V>) this.cache.get();
    }

    @Override
    public NodeFactory<Address> getMemberFactory() {
        return this.factory.get();
    }
}
