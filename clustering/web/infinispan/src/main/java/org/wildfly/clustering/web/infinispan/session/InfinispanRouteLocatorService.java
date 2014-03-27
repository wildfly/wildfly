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
import org.jboss.as.clustering.infinispan.CacheContainer;
import org.jboss.msc.service.Service;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceTarget;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StopContext;
import org.jboss.msc.value.InjectedValue;
import org.wildfly.clustering.group.NodeFactory;
import org.wildfly.clustering.registry.Registry;
import org.wildfly.clustering.web.session.RouteLocator;

/**
 * Service providing an Infinispan-based {@link RouteLocator}.
 * @author Paul Ferraro
 */
public class InfinispanRouteLocatorService implements Service<RouteLocator>, InfinispanRouteLocatorConfiguration {

    public static final ServiceName REGISTRY_SERVICE_NAME = ServiceName.JBOSS.append("clustering", "registry", InfinispanSessionManagerFactoryBuilder.DEFAULT_CACHE_CONTAINER, CacheContainer.DEFAULT_CACHE_ALIAS);
    public static final ServiceName NODE_FACTORY_SERVICE_NAME = ServiceName.JBOSS.append("clustering", "nodes", InfinispanSessionManagerFactoryBuilder.DEFAULT_CACHE_CONTAINER, CacheContainer.DEFAULT_CACHE_ALIAS);

    public static ServiceBuilder<RouteLocator> build(ServiceTarget target, ServiceName name, ServiceName deploymentServiceName) {
        InfinispanRouteLocatorService service = new InfinispanRouteLocatorService();
        return target.addService(name, service)
                .addDependency(NODE_FACTORY_SERVICE_NAME, NodeFactory.class, service.factory)
                .addDependency(REGISTRY_SERVICE_NAME, Registry.class, service.registry)
                .addDependency(getCacheServiceName(deploymentServiceName), Cache.class, service.cache)
        ;
    }

    public static ServiceName getCacheServiceName(ServiceName deploymentServiceName) {
        return deploymentServiceName.append("web", "cache");
    }

    @SuppressWarnings("rawtypes")
    private final InjectedValue<NodeFactory> factory = new InjectedValue<>();
    @SuppressWarnings("rawtypes")
    private final InjectedValue<Registry> registry = new InjectedValue<>();
    @SuppressWarnings("rawtypes")
    private final InjectedValue<Cache> cache = new InjectedValue<>();

    private volatile RouteLocator locator = null;

    private InfinispanRouteLocatorService() {
        // Hide
    }

    @Override
    public RouteLocator getValue() {
        return this.locator;
    }

    @Override
    public void start(StartContext context) {
        this.locator = new InfinispanRouteLocator(this);
    }

    @Override
    public void stop(StopContext context) {
        this.locator = null;
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
