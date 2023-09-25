/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.extension.clustering.server.registry;

import org.kohsuke.MetaInfServices;
import org.wildfly.clustering.registry.RegistryFactory;
import org.wildfly.clustering.server.service.DistributedCacheServiceConfiguratorProvider;
import org.wildfly.clustering.server.service.registry.DistributedRegistryServiceConfiguratorProvider;

/**
 * Provides the requisite builders for a clustered {@link RegistryFactory}.
 * @author Paul Ferraro
 */
@MetaInfServices({ DistributedCacheServiceConfiguratorProvider.class, DistributedRegistryServiceConfiguratorProvider.class })
public class CacheRegistryFactoryServiceConfiguratorProvider extends RegistryFactoryServiceConfiguratorProvider implements DistributedRegistryServiceConfiguratorProvider {

    public CacheRegistryFactoryServiceConfiguratorProvider() {
        super(CacheRegistryFactoryServiceConfigurator::new);
    }
}
