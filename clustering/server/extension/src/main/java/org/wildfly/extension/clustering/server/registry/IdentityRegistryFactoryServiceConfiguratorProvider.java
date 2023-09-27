/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.extension.clustering.server.registry;

import java.util.List;

import org.jboss.as.clustering.controller.IdentityCapabilityServiceConfigurator;
import org.jboss.as.controller.OperationContext;
import org.jboss.msc.service.ServiceName;
import org.kohsuke.MetaInfServices;
import org.wildfly.clustering.server.service.ClusteringCacheRequirement;
import org.wildfly.clustering.server.service.IdentityCacheServiceConfiguratorProvider;
import org.wildfly.clustering.service.ServiceConfigurator;
import org.wildfly.common.iteration.CompositeIterable;
import org.wildfly.extension.clustering.server.IdentityCacheRequirementServiceConfiguratorProvider;

/**
 * @author Paul Ferraro
 */
@MetaInfServices(IdentityCacheServiceConfiguratorProvider.class)
public class IdentityRegistryFactoryServiceConfiguratorProvider extends IdentityCacheRequirementServiceConfiguratorProvider {

    public IdentityRegistryFactoryServiceConfiguratorProvider() {
        super(ClusteringCacheRequirement.REGISTRY_FACTORY);
    }

    @Override
    public Iterable<ServiceConfigurator> getServiceConfigurators(OperationContext context, String containerName, String cacheName, String targetCacheName) {
        Iterable<ServiceConfigurator> configurators = super.getServiceConfigurators(context, containerName, cacheName, targetCacheName);
        ServiceName registryServiceName = ClusteringCacheRequirement.REGISTRY.getServiceName(context, containerName, cacheName);
        ServiceName registryEntryServiceName = ClusteringCacheRequirement.REGISTRY_ENTRY.getServiceName(context, containerName, cacheName);

        ServiceConfigurator registryConfigurator = new IdentityCapabilityServiceConfigurator<>(registryServiceName, ClusteringCacheRequirement.REGISTRY, containerName, targetCacheName).configure(context);
        ServiceConfigurator registryEntryConfigurator = new IdentityCapabilityServiceConfigurator<>(registryEntryServiceName, ClusteringCacheRequirement.REGISTRY_ENTRY, containerName, targetCacheName).configure(context);
        return new CompositeIterable<>(configurators, List.of(registryConfigurator, registryEntryConfigurator));
    }
}
