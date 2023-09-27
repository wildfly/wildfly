/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.clustering.singleton.server;

import org.kohsuke.MetaInfServices;
import org.wildfly.clustering.server.service.LocalCacheServiceConfiguratorProvider;

/**
 * Provides the requisite service configurators for installing a service providing a local {@link org.wildfly.clustering.singleton.service.SingletonServiceConfiguratorFactory}.
 * @author Paul Ferraro
 */
@MetaInfServices(LocalCacheServiceConfiguratorProvider.class)
public class LocalSingletonServiceConfiguratorFactoryServiceConfiguratorProvider extends SingletonServiceConfiguratorFactoryServiceConfiguratorProvider implements LocalCacheServiceConfiguratorProvider {

    public LocalSingletonServiceConfiguratorFactoryServiceConfiguratorProvider() {
        super(LocalSingletonServiceConfiguratorFactoryServiceConfigurator::new);
    }
}
