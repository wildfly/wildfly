/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
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
