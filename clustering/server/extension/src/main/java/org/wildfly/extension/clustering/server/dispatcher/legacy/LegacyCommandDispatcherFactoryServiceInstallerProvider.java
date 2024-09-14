/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.extension.clustering.server.dispatcher.legacy;

import java.util.function.Function;

import org.wildfly.clustering.server.service.LegacyClusteringServiceDescriptor;
import org.wildfly.extension.clustering.server.LegacyChannelJndiNameFactory;
import org.wildfly.extension.clustering.server.UnaryServiceInstallerProvider;
import org.wildfly.subsystem.service.ServiceInstaller;

/**
 * @author Paul Ferraro
 */
@Deprecated
public class LegacyCommandDispatcherFactoryServiceInstallerProvider extends UnaryServiceInstallerProvider<org.wildfly.clustering.dispatcher.CommandDispatcherFactory> {

    LegacyCommandDispatcherFactoryServiceInstallerProvider(Function<String, ServiceInstaller> installerFactory) {
        super(LegacyClusteringServiceDescriptor.COMMAND_DISPATCHER_FACTORY, installerFactory, LegacyChannelJndiNameFactory.COMMAND_DISPATCHER_FACTORY);
    }
}
