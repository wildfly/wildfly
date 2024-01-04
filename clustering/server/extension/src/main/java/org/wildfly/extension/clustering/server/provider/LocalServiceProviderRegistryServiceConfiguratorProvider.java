/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.extension.clustering.server.provider;

import org.kohsuke.MetaInfServices;
import org.wildfly.clustering.server.service.LocalCacheServiceConfiguratorProvider;

/**
 * Provides the requisite builders for a non-clustered {@link ServiceProviderRegistrationFactory}.
 * @author Paul Ferraro
 */
@MetaInfServices(LocalCacheServiceConfiguratorProvider.class)
public class LocalServiceProviderRegistryServiceConfiguratorProvider extends ServiceProviderRegistryServiceConfiguratorProvider implements LocalCacheServiceConfiguratorProvider {

    public LocalServiceProviderRegistryServiceConfiguratorProvider() {
        super(LocalServiceProviderRegistryServiceConfigurator::new);
    }
}
