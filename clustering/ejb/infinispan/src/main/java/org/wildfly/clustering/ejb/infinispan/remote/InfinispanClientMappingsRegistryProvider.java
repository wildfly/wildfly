/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.ejb.infinispan.remote;

import java.util.List;

import org.jboss.as.clustering.controller.CapabilityServiceConfigurator;
import org.jboss.as.controller.ServiceNameFactory;
import org.jboss.as.network.ClientMapping;
import org.wildfly.clustering.ejb.infinispan.network.ClientMappingsRegistryEntryServiceConfigurator;
import org.wildfly.clustering.ejb.remote.ClientMappingsRegistryProvider;
import org.wildfly.clustering.infinispan.service.CacheServiceConfigurator;
import org.wildfly.clustering.infinispan.service.InfinispanCacheRequirement;
import org.wildfly.clustering.infinispan.service.TemplateConfigurationServiceConfigurator;
import org.wildfly.clustering.server.service.ProvidedCacheServiceConfigurator;
import org.wildfly.clustering.server.service.group.DistributedCacheGroupServiceConfiguratorProvider;
import org.wildfly.clustering.server.service.registry.DistributedRegistryServiceConfiguratorProvider;
import org.wildfly.clustering.service.SupplierDependency;

/**
 * The non-legacy version of the client mappings registry provider, used when the distributable-ejb subsystem is present.
 *
 * @author Paul Ferraro
 * @author Richard Achmatowicz
 */
public class InfinispanClientMappingsRegistryProvider implements ClientMappingsRegistryProvider {

    private final String containerName ;
    private final String cacheName;

    /**
     * Creates an instance of the Infinispan-based client mappings registry provider, for local or distribute use, based on a cache-service abstraction.
     *
     * @param containerName name of the existing cache container to use, must be defined in the Infinispan subsystem.
     * @param cacheName name of the existing cache configuration to use, must be defined in the Infinispan subsystem.
     */
    public InfinispanClientMappingsRegistryProvider(final String containerName, final String cacheName) {
        this.containerName = containerName;
        this.cacheName = cacheName;
    }

    @Override
    public Iterable<CapabilityServiceConfigurator> getServiceConfigurators(String connectorName, SupplierDependency<List<ClientMapping>> clientMappings) {
        CapabilityServiceConfigurator configurationConfigurator = new TemplateConfigurationServiceConfigurator(ServiceNameFactory.parseServiceName(InfinispanCacheRequirement.CONFIGURATION.getName()).append(this.containerName, connectorName), this.containerName, connectorName, this.cacheName);
        CapabilityServiceConfigurator cacheConfigurator = new CacheServiceConfigurator<>(ServiceNameFactory.parseServiceName(InfinispanCacheRequirement.CACHE.getName()).append(this.containerName, connectorName), this.containerName, connectorName);
        CapabilityServiceConfigurator registryEntryConfigurator = new ClientMappingsRegistryEntryServiceConfigurator(this.containerName, connectorName, clientMappings);
        CapabilityServiceConfigurator groupConfigurator = new ProvidedCacheServiceConfigurator<>(DistributedCacheGroupServiceConfiguratorProvider.class, this.containerName, connectorName);
        CapabilityServiceConfigurator registryConfigurator = new ProvidedCacheServiceConfigurator<>(DistributedRegistryServiceConfiguratorProvider.class, this.containerName, connectorName);
        return List.of(configurationConfigurator, cacheConfigurator, registryEntryConfigurator, registryConfigurator, groupConfigurator);
    }
}
