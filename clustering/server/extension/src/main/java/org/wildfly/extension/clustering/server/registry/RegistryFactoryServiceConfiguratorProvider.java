/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.extension.clustering.server.registry;

import java.util.List;

import org.jboss.as.controller.capability.CapabilityServiceSupport;
import org.jboss.msc.service.ServiceName;
import org.wildfly.clustering.registry.RegistryFactory;
import org.wildfly.clustering.server.service.CacheCapabilityServiceConfiguratorFactory;
import org.wildfly.clustering.server.service.ClusteringCacheRequirement;
import org.wildfly.clustering.service.ServiceConfigurator;
import org.wildfly.common.iteration.CompositeIterable;
import org.wildfly.extension.clustering.server.CacheJndiNameFactory;
import org.wildfly.extension.clustering.server.CacheRequirementServiceConfiguratorProvider;

/**
 * Provides the requisite builders for a clustered {@link RegistryFactory} created from the specified factory.
 * @author Paul Ferraro
 */
public class RegistryFactoryServiceConfiguratorProvider extends CacheRequirementServiceConfiguratorProvider<RegistryFactory<Object, Object>> {

    protected RegistryFactoryServiceConfiguratorProvider(CacheCapabilityServiceConfiguratorFactory<RegistryFactory<Object, Object>> factory) {
        super(ClusteringCacheRequirement.REGISTRY_FACTORY, factory, CacheJndiNameFactory.REGISTRY_FACTORY);
    }

    @Override
    public Iterable<ServiceConfigurator> getServiceConfigurators(CapabilityServiceSupport support, String containerName, String cacheName) {
        Iterable<ServiceConfigurator> configurators = super.getServiceConfigurators(support, containerName, cacheName);
        ServiceName name = ClusteringCacheRequirement.REGISTRY.getServiceName(support, containerName, cacheName);
        ServiceConfigurator configurator = new RegistryServiceConfigurator<>(name, containerName, cacheName).configure(support);
        return new CompositeIterable<>(List.of(configurator), configurators);
    }
}
