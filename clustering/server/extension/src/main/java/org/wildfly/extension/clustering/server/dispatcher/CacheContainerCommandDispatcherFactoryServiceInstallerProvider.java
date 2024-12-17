/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.extension.clustering.server.dispatcher;

import org.kohsuke.MetaInfServices;
import org.wildfly.clustering.server.GroupMember;
import org.wildfly.clustering.server.dispatcher.CommandDispatcherFactory;
import org.wildfly.clustering.server.service.CacheContainerServiceInstallerProvider;
import org.wildfly.clustering.server.service.ClusteringServiceDescriptor;
import org.wildfly.extension.clustering.server.BiUnaryServiceInstallerProvider;
import org.wildfly.extension.clustering.server.ChannelJndiNameFactory;

/**
 * @author Paul Ferraro
 */
@MetaInfServices(CacheContainerServiceInstallerProvider.class)
public class CacheContainerCommandDispatcherFactoryServiceInstallerProvider<M extends GroupMember> extends BiUnaryServiceInstallerProvider<CommandDispatcherFactory<GroupMember>> implements CacheContainerServiceInstallerProvider {

    public CacheContainerCommandDispatcherFactoryServiceInstallerProvider() {
        super(ClusteringServiceDescriptor.COMMAND_DISPATCHER_FACTORY, CacheContainerCommandDispatcherFactoryServiceInstallerFactory.INSTANCE, ChannelJndiNameFactory.COMMAND_DISPATCHER_FACTORY);
    }
}
