/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.extension.clustering.server.dispatcher;

import org.kohsuke.MetaInfServices;
import org.wildfly.clustering.server.GroupMember;
import org.wildfly.clustering.server.dispatcher.CommandDispatcherFactory;
import org.wildfly.clustering.server.service.ClusteringServiceDescriptor;
import org.wildfly.clustering.server.service.DefaultChannelServiceInstallerProvider;
import org.wildfly.extension.clustering.server.DefaultUnaryServiceInstallerProvider;
import org.wildfly.extension.clustering.server.ChannelJndiNameFactory;

/**
 * @author Paul Ferraro
 */
@MetaInfServices(DefaultChannelServiceInstallerProvider.class)
public class DefaultCommandDispatcherFactoryServiceInstallerProvider extends DefaultUnaryServiceInstallerProvider<CommandDispatcherFactory<GroupMember>> implements DefaultChannelServiceInstallerProvider {

    public DefaultCommandDispatcherFactoryServiceInstallerProvider() {
        super(ClusteringServiceDescriptor.COMMAND_DISPATCHER_FACTORY, ChannelJndiNameFactory.COMMAND_DISPATCHER_FACTORY);
    }
}
