/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2019, Red Hat, Inc., and individual contributors
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

package org.wildfly.clustering.web.infinispan.routing;

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
import org.wildfly.clustering.infinispan.spi.InfinispanCacheRequirement;
import org.wildfly.clustering.registry.Registry;
import org.wildfly.clustering.service.AsyncServiceConfigurator;
import org.wildfly.clustering.service.CompositeDependency;
import org.wildfly.clustering.service.FunctionalService;
import org.wildfly.clustering.service.ServiceConfigurator;
import org.wildfly.clustering.service.ServiceSupplierDependency;
import org.wildfly.clustering.service.SupplierDependency;
import org.wildfly.clustering.spi.ClusteringCacheRequirement;
import org.wildfly.clustering.spi.NodeFactory;
import org.wildfly.clustering.web.WebDeploymentConfiguration;
import org.wildfly.clustering.web.cache.routing.RouteLocatorServiceNameProvider;
import org.wildfly.clustering.web.infinispan.session.InfinispanSessionManagementConfiguration;
import org.wildfly.clustering.web.routing.RouteLocator;

/**
 * Configures a service providing a primary owner route locator.
 * @author Paul Ferraro
 */
public class PrimaryOwnerRouteLocatorServiceConfigurator extends RouteLocatorServiceNameProvider implements CapabilityServiceConfigurator, PrimaryOwnerRouteLocatorConfiguration, Supplier<RouteLocator> {

    private final InfinispanSessionManagementConfiguration managementConfiguration;
    private final WebDeploymentConfiguration deploymentConfiguration;

    private volatile SupplierDependency<Registry<String, Void>> registry;
    private volatile SupplierDependency<Cache<GroupedKey<String>, ?>> cache;
    private volatile SupplierDependency<NodeFactory<Address>> factory;

    public PrimaryOwnerRouteLocatorServiceConfigurator(InfinispanSessionManagementConfiguration managementConfiguration, WebDeploymentConfiguration deploymentConfiguration) {
        super(deploymentConfiguration);
        this.managementConfiguration = managementConfiguration;
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
        this.registry = new ServiceSupplierDependency<>(ClusteringCacheRequirement.REGISTRY.getServiceName(support, this.managementConfiguration.getContainerName(), this.deploymentConfiguration.getServerName()));
        this.factory = new ServiceSupplierDependency<>(ClusteringCacheRequirement.GROUP.getServiceName(support, this.managementConfiguration.getContainerName(), this.deploymentConfiguration.getServerName()));
        this.cache = new ServiceSupplierDependency<>(InfinispanCacheRequirement.CACHE.getServiceName(support, this.managementConfiguration.getContainerName(), this.deploymentConfiguration.getDeploymentName()));
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
