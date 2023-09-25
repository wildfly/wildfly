/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.extension.clustering.server.provider;

import org.kohsuke.MetaInfServices;
import org.wildfly.clustering.server.service.DistributedCacheServiceConfiguratorProvider;

/**
 * Provides the requisite builders for a clustered {@link ServiceProviderRegistrationFactory}.
 * @author Paul Ferraro
 */
@MetaInfServices(DistributedCacheServiceConfiguratorProvider.class)
public class CacheServiceProviderRegistryServiceConfiguratorProvider extends ServiceProviderRegistryServiceConfiguratorProvider implements DistributedCacheServiceConfiguratorProvider {

    public CacheServiceProviderRegistryServiceConfiguratorProvider() {
        super(CacheServiceProviderRegistryServiceConfigurator::new);
    }
}
