/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.extension.clustering.ejb;

import java.util.List;

import org.jboss.as.clustering.controller.CapabilityServiceConfigurator;
import org.jboss.as.network.ClientMapping;
import org.wildfly.clustering.ejb.infinispan.network.ClientMappingsRegistryEntryServiceConfigurator;
import org.wildfly.clustering.ejb.remote.ClientMappingsRegistryProvider;
import org.wildfly.clustering.server.service.ProvidedCacheServiceConfigurator;
import org.wildfly.clustering.server.service.group.LocalCacheGroupServiceConfiguratorProvider;
import org.wildfly.clustering.server.service.registry.LocalRegistryServiceConfiguratorProvider;
import org.wildfly.clustering.service.SupplierDependency;

/**
 * A local client mappings registry provider implementation.
 *
 * @author Paul Ferraro
 * @author Richard Achmatowicz
 */
public class LocalClientMappingsRegistryProvider implements ClientMappingsRegistryProvider {
    static final String NAME = "ejb";

    @Override
    public Iterable<CapabilityServiceConfigurator> getServiceConfigurators(String connectorName, SupplierDependency<List<ClientMapping>> clientMappings) {
        CapabilityServiceConfigurator registryEntryConfigurator = new ClientMappingsRegistryEntryServiceConfigurator(NAME, connectorName, clientMappings);
        CapabilityServiceConfigurator registryConfigurator = new ProvidedCacheServiceConfigurator<>(LocalRegistryServiceConfiguratorProvider.class, NAME, connectorName);
        CapabilityServiceConfigurator groupConfigurator = new ProvidedCacheServiceConfigurator<>(LocalCacheGroupServiceConfiguratorProvider.class, NAME, connectorName);
        return List.of(registryEntryConfigurator, registryConfigurator, groupConfigurator);
    }
}
