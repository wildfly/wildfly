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
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceTarget;
import org.jboss.msc.service.ValueService;
import org.jboss.msc.value.InjectedValue;
import org.jboss.msc.value.Value;
import org.wildfly.clustering.group.NodeFactory;
import org.wildfly.clustering.registry.Registry;
import org.wildfly.clustering.service.Builder;
import org.wildfly.clustering.spi.CacheGroupServiceName;
import org.wildfly.clustering.web.session.RouteLocator;

/**
 * Service providing an Infinispan-based {@link RouteLocator}.
 * @author Paul Ferraro
 */
public class InfinispanRouteLocatorBuilder implements Builder<RouteLocator>, Value<RouteLocator>, InfinispanRouteLocatorConfiguration {

    private static ServiceName getServiceName(String deploymentName) {
        return ServiceName.JBOSS.append("clustering", "web", "locator", deploymentName);
    }

    public static ServiceName getCacheServiceAlias(String deploymentName) {
        return getServiceName(deploymentName).append("cache");
    }

    private final String deploymentName;

    @SuppressWarnings("rawtypes")
    private final InjectedValue<NodeFactory> factory = new InjectedValue<>();
    @SuppressWarnings("rawtypes")
    private final InjectedValue<Registry> registry = new InjectedValue<>();
    @SuppressWarnings("rawtypes")
    private final InjectedValue<Cache> cache = new InjectedValue<>();

    public InfinispanRouteLocatorBuilder(String deploymentName) {
        this.deploymentName = deploymentName;
    }

    @Override
    public ServiceName getServiceName() {
        return getServiceName(this.deploymentName);
    }

    @Override
    public ServiceBuilder<RouteLocator> build(ServiceTarget target) {
        return target.addService(this.getServiceName(), new ValueService<>(this))
                .addDependency(CacheGroupServiceName.NODE_FACTORY.getServiceName(InfinispanSessionManagerFactoryBuilder.DEFAULT_CACHE_CONTAINER), NodeFactory.class, this.factory)
                .addDependency(CacheGroupServiceName.REGISTRY.getServiceName(InfinispanSessionManagerFactoryBuilder.DEFAULT_CACHE_CONTAINER), Registry.class, this.registry)
                .addDependency(getCacheServiceAlias(this.deploymentName), Cache.class, this.cache)
                .setInitialMode(ServiceController.Mode.ON_DEMAND)
        ;
    }

    @Override
    public RouteLocator getValue() {
        return new InfinispanRouteLocator(this);
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
    public NodeFactory<Address> getNodeFactory() {
        return this.factory.getValue();
    }
}
