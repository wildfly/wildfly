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
package org.wildfly.clustering.web.infinispan.session;

import org.infinispan.Cache;
import org.infinispan.remoting.transport.Address;
import org.jboss.as.clustering.controller.CapabilityServiceBuilder;
import org.jboss.as.controller.capability.CapabilityServiceSupport;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceTarget;
import org.jboss.msc.service.ValueService;
import org.jboss.msc.value.Value;
import org.wildfly.clustering.infinispan.spi.InfinispanCacheRequirement;
import org.wildfly.clustering.registry.Registry;
import org.wildfly.clustering.service.Builder;
import org.wildfly.clustering.service.CompositeDependency;
import org.wildfly.clustering.service.InjectedValueDependency;
import org.wildfly.clustering.service.ValueDependency;
import org.wildfly.clustering.spi.ClusteringCacheRequirement;
import org.wildfly.clustering.spi.NodeFactory;
import org.wildfly.clustering.web.session.RouteLocator;

/**
 * Service providing an Infinispan-based {@link RouteLocator}.
 * @author Paul Ferraro
 */
public class InfinispanRouteLocatorBuilder implements CapabilityServiceBuilder<RouteLocator>, InfinispanRouteLocatorConfiguration, Value<RouteLocator> {

    private final String containerName = InfinispanSessionManagerFactoryBuilder.DEFAULT_CACHE_CONTAINER;
    private final String serverName;
    private final String deploymentName;

    @SuppressWarnings("rawtypes")
    private volatile ValueDependency<NodeFactory> factory;
    @SuppressWarnings("rawtypes")
    private volatile ValueDependency<Registry> registry;
    @SuppressWarnings("rawtypes")
    private volatile ValueDependency<Cache> cache;

    public InfinispanRouteLocatorBuilder(String serverName, String deploymentName) {
        this.serverName = serverName;
        this.deploymentName = deploymentName;
    }

    @Override
    public RouteLocator getValue() {
        return new InfinispanRouteLocator(this);
    }

    @Override
    public ServiceName getServiceName() {
        return ServiceName.JBOSS.append("clustering", "web", "locator", this.deploymentName);
    }

    @Override
    public Builder<RouteLocator> configure(CapabilityServiceSupport support) {
        this.factory = new InjectedValueDependency<>(ClusteringCacheRequirement.GROUP.getServiceName(support, this.containerName, this.serverName), NodeFactory.class);
        this.registry = new InjectedValueDependency<>(ClusteringCacheRequirement.REGISTRY.getServiceName(support, this.containerName, this.serverName), Registry.class);
        this.cache = new InjectedValueDependency<>(InfinispanCacheRequirement.CACHE.getServiceName(support, this.containerName, this.deploymentName), Cache.class);
        return this;
    }

    @Override
    public ServiceBuilder<RouteLocator> build(ServiceTarget target) {
        ServiceBuilder<RouteLocator> builder = target.addService(this.getServiceName(), new ValueService<>(this)).setInitialMode(ServiceController.Mode.ON_DEMAND);
        return new CompositeDependency(this.factory, this.registry, this.cache).register(builder);
    }

    @Override
    public Cache<String, ?> getCache() {
        return this.cache.getValue();
    }

    @Override
    public Registry<String, Void> getRegistry() {
        return this.registry.getValue();
    }

    @Override
    public NodeFactory<Address> getMemberFactory() {
        return this.factory.getValue();
    }
}
