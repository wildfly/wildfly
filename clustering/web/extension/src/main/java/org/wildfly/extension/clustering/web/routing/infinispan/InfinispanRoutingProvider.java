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

package org.wildfly.extension.clustering.web.routing.infinispan;

import java.util.List;

import org.jboss.as.clustering.controller.CapabilityServiceConfigurator;
import org.jboss.as.controller.ServiceNameFactory;
import org.wildfly.clustering.infinispan.service.CacheServiceConfigurator;
import org.wildfly.clustering.infinispan.service.InfinispanCacheRequirement;
import org.wildfly.clustering.infinispan.service.TemplateConfigurationServiceConfigurator;
import org.wildfly.clustering.server.service.ProvidedCacheServiceConfigurator;
import org.wildfly.clustering.server.service.group.DistributedCacheGroupServiceConfiguratorProvider;
import org.wildfly.clustering.server.service.registry.DistributedRegistryServiceConfiguratorProvider;
import org.wildfly.clustering.service.SupplierDependency;
import org.wildfly.clustering.web.service.routing.RoutingProvider;
import org.wildfly.extension.clustering.web.routing.LocalRouteServiceConfigurator;

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
    public Iterable<CapabilityServiceConfigurator> getServiceConfigurators(String serverName, SupplierDependency<String> route) {
        String containerName = this.config.getContainerName();
        String cacheName = this.config.getCacheName();

        CapabilityServiceConfigurator localRouteConfigurator = new LocalRouteServiceConfigurator(serverName, route);
        CapabilityServiceConfigurator registryEntryConfigurator = new RouteRegistryEntryProviderServiceConfigurator(containerName, serverName);
        CapabilityServiceConfigurator configurationConfigurator = new TemplateConfigurationServiceConfigurator(ServiceNameFactory.parseServiceName(InfinispanCacheRequirement.CONFIGURATION.getName()).append(containerName, serverName), containerName, serverName, cacheName, this.config);
        CapabilityServiceConfigurator cacheConfigurator = new CacheServiceConfigurator<>(ServiceNameFactory.parseServiceName(InfinispanCacheRequirement.CACHE.getName()).append(containerName, serverName), containerName, serverName);
        CapabilityServiceConfigurator groupConfigurator = new ProvidedCacheServiceConfigurator<>(DistributedCacheGroupServiceConfiguratorProvider.class, containerName, serverName);
        CapabilityServiceConfigurator registryConfigurator = new ProvidedCacheServiceConfigurator<>(DistributedRegistryServiceConfiguratorProvider.class, containerName, serverName);
        return List.of(localRouteConfigurator, registryEntryConfigurator, configurationConfigurator, cacheConfigurator, registryConfigurator, groupConfigurator);
    }
}
