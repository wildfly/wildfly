/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.clustering.singleton.server;

import org.kohsuke.MetaInfServices;
import org.wildfly.clustering.server.service.DistributedCacheServiceConfiguratorProvider;

/**
 * Provides the requisite service configurators for installing a service providing a distributed {@link org.wildfly.clustering.singleton.service.SingletonServiceConfiguratorFactory}.
 * @author Paul Ferraro
 */
@MetaInfServices(DistributedCacheServiceConfiguratorProvider.class)
public class CacheSingletonServiceConfiguratorFactoryServiceConfiguratorProvider extends SingletonServiceConfiguratorFactoryServiceConfiguratorProvider implements DistributedCacheServiceConfiguratorProvider {

    public CacheSingletonServiceConfiguratorFactoryServiceConfiguratorProvider() {
        super(CacheSingletonServiceConfiguratorFactoryServiceConfigurator::new);
    }
}
