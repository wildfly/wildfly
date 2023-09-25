/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.extension.clustering.server.provider;

import org.wildfly.clustering.provider.ServiceProviderRegistry;
import org.wildfly.clustering.server.service.CacheCapabilityServiceConfiguratorFactory;
import org.wildfly.clustering.server.service.ClusteringCacheRequirement;
import org.wildfly.extension.clustering.server.CacheJndiNameFactory;
import org.wildfly.extension.clustering.server.CacheRequirementServiceConfiguratorProvider;

/**
 * Provides the requisite builders for a {@link ServiceProviderRegistrationFactory} created from the specified factory.
 * @author Paul Ferraro
 */
public class ServiceProviderRegistryServiceConfiguratorProvider extends CacheRequirementServiceConfiguratorProvider<ServiceProviderRegistry<Object>> {

    public ServiceProviderRegistryServiceConfiguratorProvider(CacheCapabilityServiceConfiguratorFactory<ServiceProviderRegistry<Object>> factory) {
        super(ClusteringCacheRequirement.SERVICE_PROVIDER_REGISTRY, factory, CacheJndiNameFactory.SERVICE_PROVIDER_REGISTRY);
    }
}
