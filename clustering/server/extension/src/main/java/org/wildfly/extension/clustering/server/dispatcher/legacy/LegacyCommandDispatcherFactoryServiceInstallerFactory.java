/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.extension.clustering.server.dispatcher.legacy;

import java.util.function.Function;

import org.jboss.as.controller.ServiceNameFactory;
import org.wildfly.clustering.server.GroupMember;
import org.wildfly.clustering.server.dispatcher.CommandDispatcherFactory;
import org.wildfly.clustering.server.service.ClusteringServiceDescriptor;
import org.wildfly.clustering.server.service.LegacyClusteringServiceDescriptor;
import org.wildfly.subsystem.service.ServiceDependency;
import org.wildfly.subsystem.service.ServiceInstaller;

/**
 * @author Paul Ferraro
 */
@Deprecated
public class LegacyCommandDispatcherFactoryServiceInstallerFactory<M extends GroupMember, F extends CommandDispatcherFactory<M>> implements Function<String, ServiceInstaller> {

    private final Class<F> commandDispatcherFactoryType;
    private final Function<F, org.wildfly.clustering.dispatcher.CommandDispatcherFactory> wrapper;

    LegacyCommandDispatcherFactoryServiceInstallerFactory(Class<F> commandDispatcherFactoryType, Function<F, org.wildfly.clustering.dispatcher.CommandDispatcherFactory> wrapper) {
        this.commandDispatcherFactoryType = commandDispatcherFactoryType;
        this.wrapper = wrapper;
    }

    @Override
    public ServiceInstaller apply(String name) {
        ServiceDependency<F> commandDispatcherFactory = ServiceDependency.on(ClusteringServiceDescriptor.COMMAND_DISPATCHER_FACTORY, name).map(this.commandDispatcherFactoryType::cast);
        return ServiceInstaller.builder(this.wrapper, commandDispatcherFactory)
                .provides(ServiceNameFactory.resolveServiceName(LegacyClusteringServiceDescriptor.COMMAND_DISPATCHER_FACTORY, name))
                .requires(commandDispatcherFactory)
                .build();
    }
}
