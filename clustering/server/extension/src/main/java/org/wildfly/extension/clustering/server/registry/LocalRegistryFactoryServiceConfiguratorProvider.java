/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.extension.clustering.server.registry;

import org.kohsuke.MetaInfServices;
import org.wildfly.clustering.registry.RegistryFactory;
import org.wildfly.clustering.server.service.LocalCacheServiceConfiguratorProvider;
import org.wildfly.clustering.server.service.registry.LocalRegistryServiceConfiguratorProvider;

/**
 * Provides the requisite builders for a non-clustered {@link RegistryFactory}.
 * @author Paul Ferraro
 */
@MetaInfServices({ LocalCacheServiceConfiguratorProvider.class, LocalRegistryServiceConfiguratorProvider.class })
public class LocalRegistryFactoryServiceConfiguratorProvider extends RegistryFactoryServiceConfiguratorProvider implements LocalRegistryServiceConfiguratorProvider {

    public LocalRegistryFactoryServiceConfiguratorProvider() {
        super(LocalRegistryFactoryServiceConfigurator::new);
    }
}
