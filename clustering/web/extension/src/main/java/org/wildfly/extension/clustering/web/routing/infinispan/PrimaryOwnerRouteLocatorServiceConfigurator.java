/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.extension.clustering.web.routing.infinispan;

import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

import org.infinispan.Cache;
import org.infinispan.remoting.transport.Address;
import org.jboss.as.clustering.controller.CapabilityServiceConfigurator;
import org.jboss.as.controller.capability.CapabilityServiceSupport;
import org.jboss.msc.Service;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceTarget;
import org.wildfly.clustering.ee.infinispan.GroupedKey;
import org.wildfly.clustering.ee.infinispan.InfinispanCacheConfiguration;
import org.wildfly.clustering.infinispan.service.InfinispanCacheRequirement;
import org.wildfly.clustering.registry.Registry;
import org.wildfly.clustering.server.NodeFactory;
import org.wildfly.clustering.server.service.ClusteringCacheRequirement;
import org.wildfly.clustering.service.AsyncServiceConfigurator;
import org.wildfly.clustering.service.CompositeDependency;
import org.wildfly.clustering.service.FunctionalService;
import org.wildfly.clustering.service.ServiceConfigurator;
import org.wildfly.clustering.service.ServiceSupplierDependency;
import org.wildfly.clustering.service.SupplierDependency;
import org.wildfly.clustering.web.WebDeploymentConfiguration;
import org.wildfly.clustering.web.infinispan.routing.PrimaryOwnerRouteLocator;
import org.wildfly.clustering.web.infinispan.routing.PrimaryOwnerRouteLocatorConfiguration;
import org.wildfly.clustering.web.routing.RouteLocator;
import org.wildfly.extension.clustering.web.routing.RouteLocatorServiceNameProvider;

/**
 * Configures a service providing a primary owner route locator.
 * @author Paul Ferraro
 */
public class PrimaryOwnerRouteLocatorServiceConfigurator extends RouteLocatorServiceNameProvider implements CapabilityServiceConfigurator, PrimaryOwnerRouteLocatorConfiguration, Supplier<RouteLocator> {

    private final InfinispanCacheConfiguration configuration;
    private final WebDeploymentConfiguration deploymentConfiguration;

    private volatile SupplierDependency<Registry<String, Void>> registry;
    private volatile SupplierDependency<Cache<GroupedKey<String>, ?>> cache;
    private volatile SupplierDependency<NodeFactory<Address>> factory;

    public PrimaryOwnerRouteLocatorServiceConfigurator(InfinispanCacheConfiguration configuration, WebDeploymentConfiguration deploymentConfiguration) {
        super(deploymentConfiguration);
        this.configuration = configuration;
        this.deploymentConfiguration = deploymentConfiguration;
    }

    @Override
    public RouteLocator get() {
        return new PrimaryOwnerRouteLocator(this);
    }

    @Override
    public ServiceBuilder<?> build(ServiceTarget target) {
        ServiceName name = this.getServiceName();
        ServiceBuilder<?> builder = new AsyncServiceConfigurator(name).build(target);
        Consumer<RouteLocator> locator = new CompositeDependency(this.registry, this.cache, this.factory).register(builder).provides(name);
        Service service = new FunctionalService<>(locator, Function.identity(), this);
        return builder.setInstance(service).setInitialMode(ServiceController.Mode.ON_DEMAND);
    }

    @Override
    public ServiceConfigurator configure(CapabilityServiceSupport support) {
        this.registry = new ServiceSupplierDependency<>(ClusteringCacheRequirement.REGISTRY.getServiceName(support, this.configuration.getContainerName(), this.deploymentConfiguration.getServerName()));
        this.factory = new ServiceSupplierDependency<>(ClusteringCacheRequirement.GROUP.getServiceName(support, this.configuration.getContainerName(), this.deploymentConfiguration.getServerName()));
        this.cache = new ServiceSupplierDependency<>(InfinispanCacheRequirement.CACHE.getServiceName(support, this.configuration.getContainerName(), this.deploymentConfiguration.getDeploymentName()));
        return this;
    }

    @Override
    public Registry<String, Void> getRegistry() {
        return this.registry.get();
    }

    @Override
    public Cache<GroupedKey<String>, ?> getCache() {
        return this.cache.get();
    }

    @Override
    public NodeFactory<Address> getMemberFactory() {
        return this.factory.get();
    }
}
