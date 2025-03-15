/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.extension.clustering.server.dispatcher;

import java.util.function.Function;

import org.wildfly.clustering.server.GroupMember;
import org.wildfly.clustering.server.dispatcher.CommandDispatcherFactory;
import org.wildfly.clustering.server.service.ClusteringServiceDescriptor;
import org.wildfly.extension.clustering.server.ChannelJndiNameFactory;
import org.wildfly.extension.clustering.server.UnaryServiceInstallerProvider;
import org.wildfly.subsystem.service.ServiceInstaller;

/**
 * @author Paul Ferraro
 */
public class CommandDispatcherFactoryServiceInstallerProvider extends UnaryServiceInstallerProvider<CommandDispatcherFactory<GroupMember>> {

    public CommandDispatcherFactoryServiceInstallerProvider(Function<String, ServiceInstaller> installerFactory) {
        super(ClusteringServiceDescriptor.COMMAND_DISPATCHER_FACTORY, installerFactory, ChannelJndiNameFactory.COMMAND_DISPATCHER_FACTORY);
    }
}
