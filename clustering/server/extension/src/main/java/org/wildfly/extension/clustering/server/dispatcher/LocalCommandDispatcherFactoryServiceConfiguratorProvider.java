/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.extension.clustering.server.dispatcher;

import org.kohsuke.MetaInfServices;
import org.wildfly.clustering.server.service.LocalGroupServiceConfiguratorProvider;

/**
 * Provides the requisite builders for creating a non-clustered {@link org.wildfly.clustering.dispatcher.CommandDispatcherFactory}.
 * @author Paul Ferraro
 */
@MetaInfServices(LocalGroupServiceConfiguratorProvider.class)
public class LocalCommandDispatcherFactoryServiceConfiguratorProvider extends CommandDispatcherFactoryServiceConfiguratorProvider implements LocalGroupServiceConfiguratorProvider {

    public LocalCommandDispatcherFactoryServiceConfiguratorProvider() {
        super(LocalCommandDispatcherFactoryServiceConfigurator::new);
    }
}
