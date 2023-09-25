/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.extension.clustering.server.dispatcher;

import org.kohsuke.MetaInfServices;
import org.wildfly.clustering.server.service.DistributedGroupServiceConfiguratorProvider;

/**
 * Provides the requisite builders for creating a channel-based {@link org.wildfly.clustering.dispatcher.CommandDispatcherFactory}.
 * @author Paul Ferraro
 */
@MetaInfServices(DistributedGroupServiceConfiguratorProvider.class)
public class ChannelCommandDispatcherFactoryServiceConfiguratorProvider extends CommandDispatcherFactoryServiceConfiguratorProvider implements DistributedGroupServiceConfiguratorProvider {

    public ChannelCommandDispatcherFactoryServiceConfiguratorProvider() {
        super(ChannelCommandDispatcherFactoryServiceConfigurator::new);
    }
}
