/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.extension.clustering.server.dispatcher.legacy;

import org.kohsuke.MetaInfServices;
import org.wildfly.clustering.server.service.DefaultChannelServiceInstallerProvider;
import org.wildfly.clustering.server.service.LegacyClusteringServiceDescriptor;
import org.wildfly.extension.clustering.server.DefaultUnaryServiceInstallerProvider;
import org.wildfly.extension.clustering.server.LegacyChannelJndiNameFactory;

/**
 * @author Paul Ferraro
 */
@Deprecated
@MetaInfServices(DefaultChannelServiceInstallerProvider.class)
public class DefaultLegacyCommandDispatcherFactoryServiceInstallerProvider extends DefaultUnaryServiceInstallerProvider<org.wildfly.clustering.dispatcher.CommandDispatcherFactory> implements DefaultChannelServiceInstallerProvider {

    public DefaultLegacyCommandDispatcherFactoryServiceInstallerProvider() {
        super(LegacyClusteringServiceDescriptor.COMMAND_DISPATCHER_FACTORY, LegacyChannelJndiNameFactory.COMMAND_DISPATCHER_FACTORY);
    }
}
