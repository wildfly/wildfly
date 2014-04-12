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

import java.util.Map;

import org.infinispan.Cache;
import org.infinispan.distribution.DistributionManager;
import org.infinispan.remoting.transport.Address;
import org.jboss.as.clustering.infinispan.CacheContainer;
import org.jboss.msc.inject.Injector;
import org.jboss.msc.service.AbstractService;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceTarget;
import org.jboss.msc.value.InjectedValue;
import org.wildfly.clustering.group.Node;
import org.wildfly.clustering.group.NodeFactory;
import org.wildfly.clustering.registry.Registry;
import org.wildfly.clustering.web.session.RouteLocator;

/**
 * Service providing a {@link RouteLocator}.
 * Uses Infinispan's {@link DistributionManager} to determine the best node (i.e. the primary lock owner) to handle a given session.
 * The {@link Address} is then converted to a route using a {@link Registry}, which maps the route identifier per node.
 * @author Paul Ferraro
 */
public class RouteLocatorService extends AbstractService<RouteLocator> implements RouteLocator {

    public static final ServiceName REGISTRY_SERVICE_NAME = ServiceName.JBOSS.append("clustering", "registry", InfinispanSessionManagerFactoryBuilder.DEFAULT_CACHE_CONTAINER, CacheContainer.DEFAULT_CACHE_ALIAS);
    public static final ServiceName NODE_FACTORY_SERVICE_NAME = ServiceName.JBOSS.append("clustering", "nodes", InfinispanSessionManagerFactoryBuilder.DEFAULT_CACHE_CONTAINER, CacheContainer.DEFAULT_CACHE_ALIAS, "entry");

    @SuppressWarnings({ "unchecked", "rawtypes", "deprecation" })
    public static ServiceBuilder<RouteLocator> build(ServiceTarget target, ServiceName name, ServiceName deploymentServiceName) {
        RouteLocatorService service = new RouteLocatorService();
        return target.addService(name, service)
                // These services are only available if the cache is non-local.
                .addDependency(ServiceBuilder.DependencyType.OPTIONAL, NODE_FACTORY_SERVICE_NAME, NodeFactory.class, (Injector) service.nodeFactory)
                .addDependency(ServiceBuilder.DependencyType.OPTIONAL, REGISTRY_SERVICE_NAME, Registry.class, service.registry)
                .addDependency(RouteRegistryEntryProviderService.SERVICE_NAME, RouteRegistryEntryProvider.class, service.provider)
                .addDependency(getCacheServiceName(deploymentServiceName), Cache.class, service.cache)
        ;
    }

    public static ServiceName getCacheServiceName(ServiceName deploymentServiceName) {
        return deploymentServiceName.append("web", "cache");
    }

    private final InjectedValue<NodeFactory<Address>> nodeFactory = new InjectedValue<>();
    private final InjectedValue<Registry<String, Void>> registry = new InjectedValue<>();
    private final InjectedValue<RouteRegistryEntryProvider> provider = new InjectedValue<>();
    private final InjectedValue<Cache<String, ?>> cache = new InjectedValue<>();

    private RouteLocatorService() {
        // Hide
    }

    @Override
    public RouteLocator getValue() {
        return this;
    }

    @Override
    public String locate(String sessionId) {
        Registry<String, Void> registry = this.registry.getOptionalValue();
        if (registry == null) {
            // Return local route
            return this.provider.getValue().getKey();
        }
        Map.Entry<String, Void> entry = null;
        Address location = this.locatePrimaryOwner(sessionId);
        if (location != null) {
            NodeFactory<Address> nodeFactory = this.nodeFactory.getOptionalValue();
            if (nodeFactory != null) {
                Node node = nodeFactory.createNode(location);
                entry = registry.getEntry(node);
            }
        }
        if (entry == null) {
            // Accommodate mod_cluster's lazy route auto-generation
            entry = registry.getLocalEntry();
        }
        return (entry != null) ? entry.getKey() : null;
    }

    private Address locatePrimaryOwner(String sessionId) {
        Cache<?, ?> cache = this.cache.getValue();
        DistributionManager dist = cache.getAdvancedCache().getDistributionManager();
        return (dist != null) ? dist.getPrimaryLocation(sessionId) : cache.getCacheManager().getAddress();
    }
}
