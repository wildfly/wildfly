/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2018, Red Hat, Inc., and individual contributors
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

import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.ServiceLoader;

import org.jboss.as.clustering.controller.CapabilityServiceConfigurator;
import org.jboss.as.controller.ServiceNameFactory;
import org.wildfly.clustering.infinispan.spi.InfinispanCacheRequirement;
import org.wildfly.clustering.infinispan.spi.service.CacheServiceConfigurator;
import org.wildfly.clustering.infinispan.spi.service.TemplateConfigurationServiceConfigurator;
import org.wildfly.clustering.service.ServiceNameRegistry;
import org.wildfly.clustering.service.SupplierDependency;
import org.wildfly.clustering.spi.CacheServiceConfiguratorProvider;
import org.wildfly.clustering.spi.ClusteringCacheRequirement;
import org.wildfly.clustering.spi.DistributedCacheServiceConfiguratorProvider;
import org.wildfly.clustering.web.cache.routing.LocalRouteServiceConfigurator;
import org.wildfly.clustering.web.routing.RoutingProvider;

/**
 * Routing provider backed by an Infinispan cache.
 * @author Paul Ferraro
 */
public class InfinispanRoutingProvider implements RoutingProvider {

    private final InfinispanRoutingConfiguration config;

    public InfinispanRoutingProvider(InfinispanRoutingConfiguration config) {
        this.config = config;
    }

    @Override
    public Collection<CapabilityServiceConfigurator> getServiceConfigurators(String serverName, SupplierDependency<String> route) {
        String containerName = this.config.getContainerName();
        String cacheName = this.config.getCacheName();

        List<CapabilityServiceConfigurator> builders = new LinkedList<>();

        builders.add(new LocalRouteServiceConfigurator(serverName, route));
        builders.add(new RouteRegistryEntryProviderServiceConfigurator(containerName, serverName));
        builders.add(new TemplateConfigurationServiceConfigurator(ServiceNameFactory.parseServiceName(InfinispanCacheRequirement.CONFIGURATION.resolve(containerName, serverName)), containerName, serverName, cacheName, this.config));
        builders.add(new CacheServiceConfigurator<>(ServiceNameFactory.parseServiceName(InfinispanCacheRequirement.CACHE.resolve(containerName, serverName)), containerName, serverName));
        ServiceNameRegistry<ClusteringCacheRequirement> registry = requirement -> ServiceNameFactory.parseServiceName(requirement.resolve(containerName, serverName));
        for (CacheServiceConfiguratorProvider provider : ServiceLoader.load(DistributedCacheServiceConfiguratorProvider.class, DistributedCacheServiceConfiguratorProvider.class.getClassLoader())) {
            builders.addAll(provider.getServiceConfigurators(registry, containerName, serverName));
        }

        return builders;
    }
}
